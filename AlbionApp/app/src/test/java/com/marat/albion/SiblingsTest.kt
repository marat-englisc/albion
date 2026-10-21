package com.marat.albion

import com.marat.albion.data.db.StudyCandidate
import com.marat.albion.data.repo.Siblings
import org.junit.Assert.assertEquals
import org.junit.Test

class SiblingsTest {

    private fun card(id: String, term: String) =
        StudyCandidate(id, term)

    @Test
    fun oneMeaningPerWordPerDay() {
        val learning = listOf(card("l1", "give up#1"), card("t1", "take off"))
        val reviews = listOf(card("r1", "Take off"), card("r2", "look after"), card("r3", "look after"), card("r4", "put up"))
        val news = listOf(card("n1", "look after"), card("n2", "put up"), card("n3", "set off"), card("n4", "set off"), card("n5", "run out"), card("n6", "carry on"))
        val (r, n) = Siblings.select(learning, reviews, news, answeredToday = setOf("run out"), reviewLimit = 10, newLimit = 10)
        // take off уже в учебных шагах, второе значение look after — дубль.
        assertEquals(listOf("r2", "r4"), r.map { it.id })
        // look after и put up уже сегодня в повторениях, run out отвечали сегодня, set off — один раз.
        assertEquals(listOf("n3", "n6"), n.map { it.id })
    }

    @Test
    fun limitsAreAppliedAfterRemovingSiblings() {
        val news = listOf(card("a1", "a"), card("a2", "a"), card("b", "b"), card("c", "c"))
        val (_, n) = Siblings.select(emptyList(), emptyList(), news, emptySet(), reviewLimit = 0, newLimit = 2)
        assertEquals(listOf("a1", "b"), n.map { it.id })
    }
}
