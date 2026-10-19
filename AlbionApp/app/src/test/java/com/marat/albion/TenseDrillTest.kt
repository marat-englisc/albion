package com.marat.albion

import com.marat.albion.data.content.FormVariant
import com.marat.albion.data.content.IrregularData
import com.marat.albion.data.content.VerbForm
import com.marat.albion.data.games.TenseDrill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TenseDrillTest {

    private fun verb(v1: List<String>, v2: List<String>, v3: List<String>, ex: List<List<String>>) = IrregularData(
        listOf(v1, v2, v3).mapIndexed { i, forms -> VerbForm("f$i", forms.map { FormVariant(it, "/x/", emptyList()) }, ex[i]) }
    )

    @Test
    fun buildsGapAndRule() {
        val arise = verb(
            listOf("arise"), listOf("arose"), listOf("arisen"),
            listOf(
                listOf("Please contact us if any questions arise."),
                listOf("A problem arose during the final inspection."),
                listOf("Several questions have arisen since our last meeting.", "The dispute had arisen before we joined."),
            ),
        )
        val tasks = TenseDrill.tasks(arise)
        assertEquals(4, tasks.size)
        val perfect = tasks.first { it.answer == "arisen" && it.before.endsWith("have ") }
        assertEquals("Several questions have ", perfect.before)
        assertEquals(" since our last meeting.", perfect.after)
        assertTrue(perfect.rule.startsWith("Present Perfect"))
        assertTrue(tasks.first { it.before.contains("had") }.rule.startsWith("Past Perfect"))
        assertTrue(tasks.first { it.formIndex == 1 }.rule.startsWith("Past Simple"))
        assertEquals(setOf("arise", "arose", "arisen", "arised"), tasks.first().options.toSet())
    }

    @Test
    fun rulesForInfinitive() {
        assertTrue(TenseDrill.rule("I want to ", 0).startsWith("После to"))
        assertTrue(TenseDrill.rule("She didn't ", 0).startsWith("После do"))
        assertTrue(TenseDrill.rule("You must never ", 0).startsWith("После модального"))
        assertTrue(TenseDrill.rule("", 0).startsWith("Повелительное"))
        assertTrue(TenseDrill.rule("The bill was ", 2).startsWith("Пассив"))
    }

    @Test
    fun beKeepsWasAndWereApartAndSkipsIdenticalForms() {
        val be = verb(listOf("be"), listOf("was", "were"), listOf("been"), listOf(listOf(), listOf("We were tired after the walk."), listOf()))
        val t = TenseDrill.tasks(be).single()
        assertEquals(listOf("were"), t.accepted)
        assertTrue("was" in t.options && "were" in t.options)
        val cut = verb(listOf("cut"), listOf("cut"), listOf("cut"), listOf(listOf("I cut it."), listOf(), listOf()))
        assertTrue(TenseDrill.tasks(cut).isEmpty())
        val learn = verb(listOf("learn"), listOf("learnt", "learned"), listOf("learnt", "learned"), listOf(listOf(), listOf("She learned fast."), listOf()))
        assertEquals(listOf("learned", "learnt"), TenseDrill.tasks(learn).single().accepted)
        assertEquals("cried", TenseDrill.regularized("cry"))
        assertEquals("writed", TenseDrill.regularized("write"))
    }
}
