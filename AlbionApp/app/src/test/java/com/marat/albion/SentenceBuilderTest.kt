package com.marat.albion

import com.marat.albion.data.games.SentencePuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceBuilderTest {

    @Test
    fun splitsWordsAndKeepsEndingApart() {
        val p = SentencePuzzle.from("She works in a small office.")!!
        assertEquals(listOf("she", "works", "in", "a", "small", "office"), p.tokens)
        assertEquals(".", p.ending)
        assertEquals("She works in a small office.", p.original)
    }

    @Test
    fun keepsCapitalForNamesAndI() {
        assertEquals("London", SentencePuzzle.from("London is very busy today.")!!.tokens.first())
        assertEquals("I", SentencePuzzle.from("I need a pen to sign this form.")!!.tokens.first())
    }

    @Test
    fun checksOrderIgnoringCaseAndDuplicates() {
        val p = SentencePuzzle.from("The cat saw the dog?")!!
        assertEquals("?", p.ending)
        assertTrue(p.isCorrect(listOf("the", "cat", "saw", "The", "dog")))
        assertFalse(p.isCorrect(listOf("the", "dog", "saw", "the", "cat")))
        assertEquals(setOf(1, 4), p.wrongPositions(listOf("the", "dog", "saw", "the", "cat")))
    }

    @Test
    fun rejectsUnsuitableSentences() {
        assertNull(SentencePuzzle.from("Too short."))
        assertNull(SentencePuzzle.from("He said \"hello\" to everyone in the room."))
        assertNull(SentencePuzzle.from("One two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen."))
        assertEquals("Fine example here now.", SentencePuzzle.fromExamples(listOf("Bad.", "Fine example here now."))!!.original)
    }

    @Test
    fun keepsIdiomAsOneTileAndTrimsLongExamples() {
        val p = SentencePuzzle.from(
            "Missing the train was a blessing in disguise: I met an old friend while waiting for the next one.",
            chunk = "a blessing in disguise",
        )!!
        assertEquals("Missing the train was a blessing in disguise.", p.original)
        assertEquals(listOf("missing", "the", "train", "was", "a blessing in disguise"), p.tokens)
        // Часть с идиомой слишком короткая, а без идиомы пример бесполезен — пропускаем его.
        assertNull(SentencePuzzle.from("We caught the last train with seconds to spare. It was a close call.", chunk = "a close call"))
        assertEquals("We caught the last train with seconds to spare.", SentencePuzzle.from("We caught the last train with seconds to spare. It was a close call.")!!.original)
        // Короткая часть с идиомой не отбрасывается в пользу части без неё.
        val r = SentencePuzzle.from("Don't make a mountain out of a molehill; it's only a tiny scratch.", chunk = "make a mountain out of a molehill")!!
        assertTrue(r.tokens.any { it.startsWith("make a mountain out of a molehill") })
    }
}
