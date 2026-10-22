package com.marat.albion

import com.marat.albion.data.db.GrammarTaskEntity
import com.marat.albion.data.grammar.GrammarQuizSelection
import com.marat.albion.data.grammar.GrammarQuizSession
import org.junit.Assert.*
import org.junit.Test

class GrammarQuizSelectionTest {
    private fun task(n: Int, kind: String) = GrammarTaskEntity("q$n", n, "B1", 34, "Perfect", kind, "Question", "", "answer", "why")

    @Test fun mixedAttemptIsCompleteOnlyAfterEverySelectedQuestion() {
        val pool = (1..8).map { task(it, "choice") } + (9..16).map { task(it, "input") }
        val selected = GrammarQuizSelection.select(pool)
        assertEquals(8, selected.size)
        assertEquals(4, selected.count { it.kind == "input" })
        assertNotEquals(selected.map { it.id }.toSet(), GrammarQuizSelection.select(pool.reversed()).map { it.id }.toSet())
        val session = GrammarQuizSession(selected.map { it.id }, true)
        selected.dropLast(1).forEach { session.record(it.id, true) }
        assertNull(session.result())
        session.record(selected.last().id, true)
        assertTrue(session.result()!!.passed)
    }

    @Test fun shortOrSingleKindPoolsRemainUsableAndNeverRepeatQuestions() {
        val short = listOf(task(1, "input"), task(2, "input"))
        assertEquals(short, GrammarQuizSelection.select(short + short))
        assertEquals(8, GrammarQuizSelection.select((1..12).map { task(it, "choice") }).size)
        assertTrue(GrammarQuizSelection.select(emptyList()).isEmpty())
    }
}
