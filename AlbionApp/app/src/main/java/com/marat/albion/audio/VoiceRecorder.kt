package com.marat.albion.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.marat.albion.data.audio.ProsodyAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * Запись голоса для шэдоуинга: несжатый звук 16 кГц (для анализа мелодики) + WAV-файл
 * в кэше для прослушивания. Пока идёт запись, [level] (0…1) отдаёт громкость.
 * Запись останавливается сама, когда вы замолчали после фразы, или через [start] maxMs.
 * Файлы хранятся только до конца занятия.
 */
class VoiceRecorder(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null
    /** Флаг остановки текущей записи; у каждой записи свой, чтобы новая не «оживляла» прежнюю. */
    private var stopFlag = AtomicBoolean(true)

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    /** Последняя запись: моно 16 кГц, −1…1. */
    var samples: FloatArray? = null
        private set

    fun fileFor(key: String): File {
        val dir = File(context.cacheDir, "shadowing").apply { mkdirs() }
        return File(dir, key.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".wav")
    }

    /**
     * Начинает запись в [file]. [onStopped] вызывается на главном потоке, когда запись
     * завершилась — сама (тишина, лимит) или по [stop].
     */
    @SuppressLint("MissingPermission") // доступ к микрофону проверяет экран перед вызовом
    fun start(file: File, maxMs: Long = 10_000, onStopped: () -> Unit = {}): Boolean {
        stop()
        val rate = ProsodyAnalyzer.RATE
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return false
        val record = runCatching {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, rate / 5))
        }.getOrNull() ?: return false
        if (record.state != AudioRecord.STATE_INITIALIZED) { record.release(); return false }

        val stopped = AtomicBoolean(false)
        stopFlag = stopped
        samples = null
        _recording.value = true
        job = scope.launch {
            try {
                val pcm = try {
                    withContext(Dispatchers.IO) { capture(record, rate, maxMs, stopped) }
                } finally {
                    // Микрофон освобождаем всегда — и при отмене (уход с экрана посреди записи).
                    record.release()
                }
                samples = FloatArray(pcm.size) { pcm[it] / 32768f }
                withContext(Dispatchers.IO) { writeWav(file, pcm, rate) }
                onStopped()
            } finally {
                // Поздно завершившаяся прежняя запись не гасит индикатор новой.
                if (stopFlag === stopped) {
                    _recording.value = false
                    _level.value = 0f
                }
            }
        }
        return true
    }

    /** Чтение с микрофона до тишины после речи, лимита или [stop]. */
    private fun capture(record: AudioRecord, rate: Int, maxMs: Long, stopped: AtomicBoolean): ShortArray {
        val out = ShortArrayBuilder(rate * 4)
        val chunk = ShortArray(rate / 50) // 20 мс
        val maxSamples = rate * maxMs / 1000
        // Запасной лимит по часам: если микрофон перестал отдавать звук, запись всё равно закончится.
        val deadline = SystemClock.elapsedRealtime() + maxMs + 1_000
        var noise = -1f
        var speechMs = 0
        var silenceMs = 0
        runCatching { record.startRecording() }.onFailure { return ShortArray(0) }
        try {
            while (!stopped.get() && out.size < maxSamples && SystemClock.elapsedRealtime() < deadline) {
                val read = record.read(chunk, 0, chunk.size)
                // Отрицательное значение — ошибка (микрофон занят, устройство отключено): дальше читать бессмысленно.
                if (read < 0) break
                if (read == 0) continue
                out.append(chunk, read)
                var s = 0.0
                for (k in 0 until read) s += chunk[k] * chunk[k].toDouble()
                val rms = (sqrt(s / read) / 32768).toFloat()
                // Сжимаем динамику, чтобы тихая речь тоже была видна.
                _level.value = sqrt(rms * 4).coerceIn(0f, 1f)
                // Уровень шума — по первым 200 мс; речь — заметно громче шума.
                if (out.size <= rate / 5) { noise = maxOf(noise, rms); continue }
                val loud = rms > maxOf(0.012f, noise * 3f)
                if (loud) { speechMs += 20; silenceMs = 0 } else silenceMs += 20
                if (speechMs >= 300 && silenceMs >= 1_100) break
            }
        } finally {
            runCatching { record.stop() }
        }
        return out.toArray()
    }

    fun stop() {
        stopFlag.set(true)
    }

    fun clear() {
        stop()
        job?.cancel()
        File(context.cacheDir, "shadowing").listFiles()?.forEach { it.delete() }
    }

    private fun writeWav(file: File, pcm: ShortArray, rate: Int) {
        file.delete()
        RandomAccessFile(file, "rw").use { f ->
            val dataLen = pcm.size * 2
            fun int(v: Int) { f.write(v and 0xff); f.write(v shr 8 and 0xff); f.write(v shr 16 and 0xff); f.write(v shr 24 and 0xff) }
            fun short(v: Int) { f.write(v and 0xff); f.write(v shr 8 and 0xff) }
            f.writeBytes("RIFF"); int(36 + dataLen); f.writeBytes("WAVE")
            f.writeBytes("fmt "); int(16); short(1); short(1); int(rate); int(rate * 2); short(2); short(16)
            f.writeBytes("data"); int(dataLen)
            val bytes = ByteArray(dataLen)
            for (k in pcm.indices) { bytes[2 * k] = (pcm[k].toInt() and 0xff).toByte(); bytes[2 * k + 1] = (pcm[k].toInt() shr 8).toByte() }
            f.write(bytes)
        }
    }

    private class ShortArrayBuilder(capacity: Int) {
        private var data = ShortArray(capacity)
        var size = 0
            private set

        fun append(src: ShortArray, count: Int) {
            if (size + count > data.size) data = data.copyOf(maxOf(data.size * 2, size + count))
            System.arraycopy(src, 0, data, size, count)
            size += count
        }

        fun toArray() = data.copyOf(size)
    }
}
