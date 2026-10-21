package com.marat.albion

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.audio.AudioPlayer
import com.marat.albion.data.content.Deck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/** Настоящий ExoPlayer и управляемые события TTS: проверяется состояние движков после stop. */
@RunWith(AndroidJUnit4::class)
class AudioPlayerTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var audio: AudioPlayer
    private lateinit var longFile: File
    private lateinit var shortFile: File
    private var media: ExoPlayer? = null
    private var mediaCreations = 0
    private val engines = mutableListOf<ControlledTts>()
    private val initializers = mutableListOf<TextToSpeech.OnInitListener>()

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val context = instrumentation.targetContext
        onMain {
            audio = AudioPlayer(context, createPlayer = { ctx ->
                mediaCreations++
                ExoPlayer.Builder(ctx).setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                    false,
                ).build().also { media = it }
            }, createTts = { ctx, listener ->
                initializers += listener
                ControlledTts(ctx).also { engines += it }
            })
        }
        longFile = wav(context, "long", 5_000)
        shortFile = wav(context, "short", 450)
    }

    @After
    fun tearDown() {
        onMain { audio.release() }
        scenario.close()
        longFile.delete()
        shortFile.delete()
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<T>()
        instrumentation.runOnMainSync { value.set(block()) }
        return value.get()
    }

    private suspend fun awaitReady() = withTimeout(5_000) {
        while (onMain { media?.playbackState } != Player.STATE_READY) delay(10)
    }

    private suspend fun awaitKey(key: String?) = withTimeout(5_000) { audio.playing.first { it == key } }

    private fun initialize(index: Int = initializers.lastIndex, status: Int = TextToSpeech.SUCCESS) {
        onMain { initializers[index].onInit(status) }
        instrumentation.waitForIdleSync()
    }

    private fun assertStopped() {
        assertNull(audio.playing.value)
        assertEquals(Player.STATE_IDLE, onMain { media!!.playbackState })
        assertFalse(onMain { media!!.isPlaying })
    }

    @Test fun stopAndReleaseDoNotCreateUnusedEngines() {
        onMain { audio.stop(); audio.release(); audio.stop() }
        assertEquals(0, mediaCreations)
        assertTrue(engines.isEmpty())
        assertNull(audio.playing.value)
    }

    @Test fun replayKeepsTheNewKeyAndStopStopsTheActualPlayer(): Unit = runBlocking {
        onMain { audio.playFile(longFile, "first") }
        awaitReady()
        onMain { audio.playFile(longFile, "second") }
        assertEquals("second", audio.playing.value)
        awaitReady()
        onMain { audio.stop() }
        assertStopped()
        delay(350)
        assertStopped()
    }

    @Test fun replayingBundledMp3CanBeStopped(): Unit = runBlocking {
        onMain { audio.play(Deck.WORDS, "son.mp3", "son", "first", rate = 0.25f) }
        awaitReady()
        onMain { audio.play(Deck.WORDS, "son.mp3", "son", "second", rate = 0.25f) }
        assertEquals("second", audio.playing.value)
        awaitReady()
        onMain { audio.stop() }
        assertStopped()
    }

    @Test fun naturalCompletionClearsThePlayingKey(): Unit = runBlocking {
        onMain { audio.playFile(shortFile, "short") }
        awaitKey(null)
        assertEquals(Player.STATE_ENDED, onMain { media!!.playbackState })
    }

    @Test fun stopDuringLoadingCancelsTheTtsFallback(): Unit = runBlocking {
        onMain {
            audio.play(Deck.WORDS, "missing-stop-test.mp3", "fallback", "missing")
            audio.stop()
        }
        delay(350)
        assertStopped()
        assertTrue(engines.isEmpty())
    }

    @Test fun missingMp3StillFallsBackToSpeech(): Unit = runBlocking {
        onMain { audio.play(Deck.WORDS, "missing-fallback-test.mp3", "fallback", "missing") }
        withTimeout(5_000) { while (onMain { engines.isEmpty() }) delay(10) }
        initialize()
        assertEquals(listOf("fallback"), engines.single().spoken.map { it.first })
        assertEquals("missing", audio.playing.value)
        onMain { audio.stop() }
        assertStopped()
    }

    @Test fun stopCancelsDelayedAutoplayBeforeTheEngineExists(): Unit = runBlocking {
        val autoplay = async(Dispatchers.Main) {
            audio.playDelayed(Deck.WORDS, "son.mp3", "son", "automatic", delayMs = 150)
        }
        withContext(Dispatchers.Main) { audio.stop() }
        autoplay.await()
        assertEquals(0, mediaCreations)
        assertNull(audio.playing.value)
    }

    @Test fun manualPlaybackReplacesDelayedAutoplay(): Unit = runBlocking {
        val autoplay = async(Dispatchers.Main) {
            audio.playDelayed(Deck.WORDS, "son.mp3", "son", "automatic", delayMs = 150)
        }
        withContext(Dispatchers.Main) { audio.playFile(longFile, "manual") }
        autoplay.await()
        assertEquals("manual", audio.playing.value)
        awaitReady()
    }

    @Test fun delayedAutoplayRunsWhenItHasNotBeenCancelled(): Unit = runBlocking {
        withContext(Dispatchers.Main) {
            audio.playDelayed(Deck.WORDS, "son.mp3", "son", "automatic", delayMs = 50)
        }
        assertEquals("automatic", audio.playing.value)
        awaitReady()
    }

    @Test fun stopCancelsSpeechWaitingForInitialization() {
        onMain { audio.speak("cancel this", "queued"); audio.stop() }
        initialize()
        assertTrue(engines.single().spoken.isEmpty())
        assertNull(audio.playing.value)
        assertEquals(0, mediaCreations)
    }

    @Test fun mediaReplacesQueuedSpeechAndLateInitializationDoesNotSpeak(): Unit = runBlocking {
        onMain { audio.speak("old queued speech", "queued"); audio.playFile(longFile, "media") }
        initialize()
        awaitReady()
        assertTrue(engines.single().spoken.isEmpty())
        assertEquals("media", audio.playing.value)
    }

    @Test fun failedOldTtsInitializationDoesNotClearTheNewMediaKey(): Unit = runBlocking {
        onMain { audio.speak("old speech", "queued"); audio.playFile(longFile, "media") }
        initialize(status = TextToSpeech.ERROR)
        awaitReady()
        assertEquals("media", audio.playing.value)
    }

    @Test fun speechStopsMediaAndKeepsItsKeyUntilCompletion(): Unit = runBlocking {
        onMain { audio.speak("prepare engine", "prepare") }
        initialize()
        onMain { audio.playFile(longFile, "media") }
        awaitReady()
        onMain { audio.speak("new speech", "speech") }
        assertEquals(Player.STATE_IDLE, onMain { media!!.playbackState })
        assertEquals("speech", audio.playing.value)
        assertEquals("new speech", engines.single().spoken.last().first)
        val before = engines.single().stops
        onMain { audio.stop() }
        assertNull(audio.playing.value)
        assertTrue(engines.single().stops > before)
    }

    @Test fun lateTtsCompletionCannotClearANewerUtterance() {
        onMain { audio.speak("first", "first") }
        initialize()
        val engine = engines.single()
        val firstId = engine.spoken.last().second
        onMain { audio.speak("second", "second") }
        val secondId = engine.spoken.last().second
        assertNotEquals(firstId, secondId)
        engine.progress!!.onDone(firstId)
        engine.progress!!.onStop(firstId, true)
        instrumentation.waitForIdleSync()
        assertEquals("second", audio.playing.value)
        engine.progress!!.onDone(secondId)
        instrumentation.waitForIdleSync()
        assertNull(audio.playing.value)
    }

    @Test fun releasedEngineCannotSpeakWhenItsInitializationArrivesLate() {
        onMain { audio.speak("old", "old"); audio.release(); audio.speak("new", "new") }
        initialize(index = 0)
        assertTrue(engines[0].spoken.isEmpty())
        assertTrue(engines[1].spoken.isEmpty())
        assertEquals("new", audio.playing.value)
        initialize(index = 1)
        assertEquals(listOf("new"), engines[1].spoken.map { it.first })
    }

    @Test fun comparisonPlaysTheSecondFileAfterNaturalCompletion(): Unit = runBlocking {
        onMain { audio.playFile(shortFile, "original") }
        val comparison = async(Dispatchers.Main) { audio.playFileAfterCurrent(longFile, "mine") }
        awaitKey("mine")
        comparison.await()
        onMain { audio.stop() }
        assertStopped()
    }

    @Test fun stopCancelsThePendingSecondFile(): Unit = runBlocking {
        onMain { audio.playFile(longFile, "original") }
        val comparison = async(Dispatchers.Main) { audio.playFileAfterCurrent(longFile, "mine") }
        withContext(Dispatchers.Main) { audio.stop() }
        comparison.await()
        assertStopped()
    }

    @Test fun stopDuringTheComparisonPausePreventsRestart(): Unit = runBlocking {
        onMain { audio.playFile(shortFile, "original") }
        val comparison = async(Dispatchers.Main) { audio.playFileAfterCurrent(longFile, "mine") }
        awaitKey(null)
        onMain { audio.stop() }
        comparison.await()
        assertStopped()
    }

    private class ControlledTts(context: Context) : TextToSpeech(context, null) {
        val spoken = mutableListOf<Pair<String, String>>()
        var stops = 0
        var progress: UtteranceProgressListener? = null
        override fun setLanguage(loc: Locale): Int = SUCCESS
        override fun setSpeechRate(speechRate: Float): Int = SUCCESS
        override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener): Int {
            progress = listener
            return SUCCESS
        }
        override fun speak(text: CharSequence, queueMode: Int, params: Bundle?, utteranceId: String): Int {
            spoken += text.toString() to utteranceId
            return SUCCESS
        }
        override fun stop(): Int { stops++; return SUCCESS }
    }

    private fun wav(context: Context, name: String, durationMs: Int): File {
        val file = File(context.cacheDir, "audio-stop-$name.wav")
        val rate = 16_000
        val dataSize = rate * durationMs / 1_000 * 2
        RandomAccessFile(file, "rw").use { out ->
            out.setLength(0)
            fun int(value: Int) { repeat(4) { out.write(value shr (it * 8) and 0xff) } }
            fun short(value: Int) { repeat(2) { out.write(value shr (it * 8) and 0xff) } }
            out.writeBytes("RIFF"); int(36 + dataSize); out.writeBytes("WAVEfmt ")
            int(16); short(1); short(1); int(rate); int(rate * 2); short(2); short(16)
            out.writeBytes("data"); int(dataSize)
            out.write(ByteArray(dataSize))
        }
        return file
    }
}
