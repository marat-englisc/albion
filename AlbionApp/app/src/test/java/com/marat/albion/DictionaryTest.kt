package com.marat.albion

import com.marat.albion.data.library.wordCandidates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DictionaryTest {

    @Test
    fun bundledDictionaryIsWellFormed() {
        val f = File("src/main/assets/dictionary/eng-rus.tsv")
        val rows = f.readLines().map { it.split('\t') }
        assertTrue("слишком мало статей: ${rows.size}", rows.size > 50_000)
        assertTrue(rows.all { it.size == 3 && it[0].isNotBlank() && it[2].isNotBlank() })
        assertTrue(rows.none { "[[" in it[2] })
        val terms = rows.map { it[0].lowercase() }.toSet()
        listOf("run", "house", "beautiful", "give up", "break the ice", "look forward to").forEach {
            assertTrue("нет слова $it", it in terms)
        }
    }

    @Test
    fun candidatesCoverCommonInflections() {
        assertTrue("study" in wordCandidates("studied"))
        assertTrue("stop" in wordCandidates("stopped"))
        assertTrue("make" in wordCandidates("making"))
        assertTrue("cat" in wordCandidates("Cats"))
        assertTrue("happy" in wordCandidates("happier"))
        assertTrue("children" in wordCandidates("children’s"))
        assertEquals("dog", wordCandidates("“dog”").first())
        assertTrue(wordCandidates("a").isEmpty())
    }
}
