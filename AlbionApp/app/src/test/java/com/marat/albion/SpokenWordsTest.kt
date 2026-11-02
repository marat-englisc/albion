package com.marat.albion

import com.marat.albion.data.speech.HeardWord
import com.marat.albion.data.speech.SpokenWords
import com.marat.albion.data.speech.WordVerdict
import org.junit.Assert.*
import org.junit.Test

class SpokenWordsTest {
    private fun word(text: String, confidence: Float = 0.9f) = HeardWord(text, 0, 10, confidence)

    @Test fun tokensMergeIntoWordsWithTimingAndConfidence() {
        val raw = "0\t20\t0.9000\t Pick\n20\t35\t0.8000\t it\n35\t50\t0.4000\t a\n50\t60\t0.9000\tp\n60\t61\t0.1000\t.\n"
        val words = SpokenWords.parse(raw)
        assertEquals(listOf("Pick", "it", "ap"), words.map { it.text })
        assertEquals(35, words[2].start)
        assertEquals(60, words[2].end)
        // Пунктуация не снижает уверенность; части слова дают среднее геометрическое.
        assertEquals(0.6f, words[2].confidence, 0.001f)
    }

    @Test fun noiseMarksAreDropped() {
        val raw = "0\t5\t0.9\t [\n5\t9\t0.9\tBL\n9\t12\t0.9\tANK_AUDIO\n12\t13\t0.9\t]\n" +
            "20\t30\t0.9\t (\n30\t40\t0.9\twind\n40\t50\t0.9\t blowing)\n50\t60\t0.95\t Hello\n"
        assertEquals(listOf("Hello"), SpokenWords.parse(raw).map { it.text })
    }

    @Test fun eachExpectedWordGetsAVerdict() {
        val check = SpokenWords.check(listOf("pick", "it", "up"), listOf(word("Pick"), word("ap")))
        assertEquals(listOf(WordVerdict.CLEAR, WordVerdict.MISSING, WordVerdict.MISHEARD), check.words.map { it.verdict })
        assertEquals("ap", check.words[2].heard?.text)
        assertEquals(1, check.recognized)
    }

    @Test fun lowConfidenceIsUnclearAndExtraWordsAreIgnored() {
        val check = SpokenWords.check(listOf("miss", "the", "boat"), listOf(word("Miss"), word("the", 0.3f), word("boat"), word("uh")))
        assertEquals(listOf(WordVerdict.CLEAR, WordVerdict.UNCLEAR, WordVerdict.CLEAR), check.words.map { it.verdict })
        assertFalse(check.allClear)
    }

    @Test fun britishSpellingContractionsAndHomophonesOfTheSpeakerMatch() {
        assertEquals(WordVerdict.CLEAR, SpokenWords.check(listOf("colour"), listOf(word("color"))).words.single().verdict)
        assertEquals(WordVerdict.CLEAR, SpokenWords.check(listOf("realise"), listOf(word("realize"))).words.single().verdict)
        assertEquals(WordVerdict.CLEAR, SpokenWords.check(listOf("don't"), listOf(word("Dont"))).words.single().verdict)
        assertEquals(WordVerdict.MISHEARD, SpokenWords.check(listOf("read"), listOf(word("red"))).words.single().verdict)
        assertEquals(WordVerdict.CLEAR, SpokenWords.check(listOf("read"), listOf(word("red")), listOf(setOf("red"))).words.single().verdict)
    }

    @Test fun wordAtFindsTheSpanOrTheNearestWord() {
        val words = listOf(HeardWord("pick", 0, 20, 1f), HeardWord("up", 30, 50, 1f))
        assertEquals("pick", SpokenWords.wordAt(words, 10)?.text)
        assertEquals("up", SpokenWords.wordAt(words, 27)?.text)
        assertEquals("up", SpokenWords.wordAt(words, 80)?.text)
    }
}
