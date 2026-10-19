package com.marat.albion

import com.marat.albion.data.library.WordMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordMarkerTest {

    private fun marked(m: WordMarker, text: String) = m.ranges(text).map { text.substring(it.first, it.last + 1) }

    @Test
    fun findsWordsAndForms() {
        val m = WordMarker(listOf("study", "weather", "cottage"), mapOf("went" to "go"))
        assertEquals(listOf("studied", "weather", "cottages"), marked(m, "She studied the weather near the cottages."))
    }

    @Test
    fun findsPhrasesWithInflectionAndPlaceholders() {
        val m = WordMarker(listOf("give up", "drive someone up the wall", "make up one's mind", "a blessing in disguise"), mapOf("gave" to "give", "drove" to "drive"))
        assertEquals(listOf("gave up"), marked(m, "He finally gave up smoking."))
        assertEquals(listOf("drove me up the wall"), marked(m, "The noise drove me up the wall."))
        assertEquals(listOf("make up her mind"), marked(m, "She couldn't make up her mind."))
        assertEquals(listOf("a blessing in disguise"), marked(m, "It was a blessing in disguise, really."))
    }

    @Test
    fun ignoresStopWordsAndMissing() {
        val m = WordMarker(listOf("a", "the", "go", "beautiful"))
        assertEquals(listOf("beautiful"), marked(m, "Go to the beautiful park."))
        assertTrue(WordMarker(emptyList()).ranges("Anything").isEmpty())
    }
}
