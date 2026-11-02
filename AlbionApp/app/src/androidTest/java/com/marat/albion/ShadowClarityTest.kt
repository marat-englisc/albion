package com.marat.albion

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.audio.AudioDecoder
import com.marat.albion.data.audio.ProsodyAnalyzer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.speech.SpokenWords
import com.marat.albion.data.speech.WordVerdict
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Слова с таймингами из настоящего Whisper на записи диктора. Нужна установленная модель распознавания. */
@RunWith(AndroidJUnit4::class)
class ShadowClarityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val c get() = (context.applicationContext as AlbionApp).container

    @Test fun speakerRecordingIsHeardWordByWordWithTiming(): Unit = runBlocking {
        c.speech.models.refresh()
        assumeTrue("Whisper не установлен", c.speech.models.state.value.ready)
        for ((deck, phrase) in listOf(Deck.PHRASAL to "break down", Deck.IDIOMS to "miss the boat")) {
            val pcm = requireNotNull(AudioDecoder.decodeCardAudio(context, deck, "$phrase.mp3")) { "Нет записи $phrase" }
            val words = withTimeout(120_000) { c.speech.words(pcm) }
            Log.i("ShadowClarityTest", "$phrase -> " + words.joinToString { "${it.text}[${it.start}-${it.end} p=${"%.2f".format(it.confidence)}]" })
            val check = SpokenWords.check(SpokenWords.split(phrase), words)
            assertTrue(check.words.toString(), check.words.all { it.verdict == WordVerdict.CLEAR })
            val frames = pcm.size / (ProsodyAnalyzer.RATE / 100)
            words.zipWithNext().forEach { (a, b) -> assertTrue("$a / $b", a.start <= b.start) }
            words.forEach { assertTrue("$it", it.start <= it.end && it.end <= frames + 100 && it.confidence in 0f..1f) }
        }
    }
}
