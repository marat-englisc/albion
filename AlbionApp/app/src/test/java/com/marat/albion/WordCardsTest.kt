package com.marat.albion

import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.db.ReviewStateEntity
import com.marat.albion.data.repo.WordCards
import org.junit.Assert.*
import org.junit.Test

class WordCardsTest {
    private fun card(id: String, term: String = "about", position: Int = 0) =
        CardEntity(id, "words", position, term, "/əˈbaʊt/", "preposition", "A1",
            "definition $id", "перевод $id", "example $id", "", null, null)

    @Test fun everyMeaningKeepsItsExamplesAndStableSourceId() {
        val first = card("first")
        val second = card("second", " ABOUT ", 1).copy(pos = "adverb", audio = "about.mp3")
        val combined = WordCards.group(listOf(second, first)).single()
        assertEquals("first", combined.id)
        assertEquals("about", combined.term)
        assertEquals(listOf(first, second), WordCards.members(combined))
        assertEquals("перевод first; перевод second", combined.meaningRu)
        assertEquals("definition first; definition second", combined.meaningEn)
        assertEquals("about.mp3", combined.audio)
        assertEquals("example second", combined.senses[1].example1)
    }

    @Test fun collectionsAndDifferentExpressionsRemainIndependent() {
        val rows = listOf(card("a"), card("b").copy(deck = "idioms"), card("c", "about to"))
        assertEquals(3, WordCards.group(rows).size)
        assertEquals(rows, WordCards.group(rows))
    }

    @Test fun latestExistingProgressBecomesTheSharedScheduleAndCanonicalWinsTies() {
        val combined = WordCards.combine(listOf(card("a"), card("b", position = 1)))
        fun state(id: String, last: Long) = ReviewStateEntity(id, "words", 2, 0, 8.0, 4.0, 1000, last, 5, 0)
        val older = state("a", 10)
        val latest = state("b", 20)
        assertEquals(latest, WordCards.state(combined, mapOf("a" to older, "b" to latest)))
        val shared = older.copy(lastReview = 20)
        assertEquals(shared, WordCards.state(combined, mapOf("a" to shared, "b" to latest)))
        assertNull(WordCards.state(combined, emptyMap()))
    }
}
