package com.marat.albion.audio

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.marat.albion.data.content.ContentImporter
import com.marat.albion.data.content.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Озвучка карточек: mp3 из assets/content/audio через Media3 ExoPlayer.
 * Если файла нет (или материалы не вшиты), произносит текст системным TTS (en-GB).
 * Вызывать с главного потока.
 */
class AudioPlayer(private val context: Context) {

    private val _playing = MutableStateFlow<String?>(null)
    /** Ключ звучащего сейчас фрагмента — чтобы подсветить нужную кнопку. */
    val playing: StateFlow<String?> = _playing.asStateFlow()

    private var speed = 1f
    private var volume = 1f
    private var pendingFallback: (() -> Unit)? = null

    private var playerCreated = false

    private val player: ExoPlayer by lazy {
        playerCreated = true
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) _playing.value = null
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _playing.value = null
                        pendingFallback?.invoke()
                    }
                })
            }
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsQueued: String? = null

    fun setSpeed(value: Float) {
        speed = value
    }

    /** Громкость озвучки 0…1 относительно системной громкости медиа. */
    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        if (playerCreated) player.volume = volume
    }

    /**
     * Проигрывает файл [file] колоды [deck]; при неудаче — TTS для [text].
     * [rate] — множитель к скорости из настроек (0.7 — медленный повтор в шэдоуинге).
     */
    fun play(deck: Deck, file: String?, text: String, key: String = "${deck.id}:$text", rate: Float = 1f) {
        if (file == null) {
            speak(text, key); return
        }
        val path = "${ContentImporter.AUDIO}/${deck.audioDir}/$file"
        val uri = Uri.parse("asset:///" + Uri.encode(path, "/"))
        tts?.stop()
        pendingFallback = { speak(text, key) }
        _playing.value = key
        player.run {
            stop()
            setMediaItem(MediaItem.fromUri(uri))
            setPlaybackSpeed(speed * rate)
            volume = this@AudioPlayer.volume
            prepare()
            play()
        }
    }

    /** Проигрывает собственную запись пользователя (шэдоуинг) — всегда в обычном темпе. */
    fun playFile(file: java.io.File, key: String) {
        if (!file.exists()) return
        tts?.stop()
        pendingFallback = null
        _playing.value = key
        player.run {
            stop()
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            setPlaybackSpeed(1f)
            volume = 1f
            prepare()
            play()
        }
    }

    fun speak(text: String, key: String = text) {
        if (text.isBlank()) return
        _playing.value = key
        val engine = tts
        if (engine == null) {
            ttsQueued = text
            tts = TextToSpeech(context.applicationContext) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    tts?.language = Locale.UK
                    ttsQueued?.let { speakNow(it) }
                } else {
                    _playing.value = null
                }
                ttsQueued = null
            }
        } else if (ttsReady) {
            speakNow(text)
        } else {
            ttsQueued = text
        }
    }

    private fun speakNow(text: String) {
        tts?.setSpeechRate(speed * 0.95f)
        val params = android.os.Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume) }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "albion")
        // TTS не сообщает окончание без listener — гасим индикатор чуть позже.
        _playing.value = null
    }

    fun stop() {
        if (_playing.value != null) runCatching { player.stop() }
        tts?.stop()
        _playing.value = null
    }

    fun release() {
        runCatching { player.release() }
        tts?.shutdown()
        tts = null
    }
}
