package com.marat.albion

import com.marat.albion.data.games.AnswerCheck
import com.marat.albion.data.games.Verdict
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckTest {

    @Test
    fun ignoresCaseSpacesAndApostrophes() {
        assertEquals(Verdict.Correct, AnswerCheck.check("  Give  UP ", listOf("give up")))
        assertEquals(Verdict.Correct, AnswerCheck.check("Bob’s your uncle!", listOf("Bob's your uncle")))
    }

    @Test
    fun smallTyposAreAlmost() {
        assertEquals(Verdict.Typo, AnswerCheck.check("abandn", listOf("abandon")))
        assertEquals(Verdict.Typo, AnswerCheck.check("acomodation", listOf("accommodation")))
        // В коротких словах опечатка не прощается.
        assertEquals(Verdict.Wrong, AnswerCheck.check("cat", listOf("car")))
        assertEquals(Verdict.Wrong, AnswerCheck.check("", listOf("car")))
        assertEquals(Verdict.Wrong, AnswerCheck.check("window", listOf("abandon")))
    }

    @Test
    fun irregularFormsAcceptAnyVariant() {
        val variants = listOf("abided", "abode")
        assertEquals(Verdict.Correct, AnswerCheck.checkForm("abode", variants))
        assertEquals(Verdict.Correct, AnswerCheck.checkForm("abided / abode", variants))
        assertEquals(Verdict.Correct, AnswerCheck.checkForm("Abided, abode", variants))
        assertEquals(Verdict.Wrong, AnswerCheck.checkForm("abidded", variants))
        assertEquals(Verdict.Wrong, AnswerCheck.checkForm("forgot", listOf("forgotten")))
        assertEquals(Verdict.Wrong, AnswerCheck.checkForm("abided / ", variants))
        assertEquals(Verdict.Wrong, AnswerCheck.checkForm("abided / abidden", listOf("abided")))
        assertEquals(Verdict.Wrong, AnswerCheck.checkForm("", variants))
    }

    @Test
    fun similarityForSpeech() {
        assertEquals(1f, AnswerCheck.similarity("Accommodation.", "accommodation"), 0.001f)
        assertTrue(AnswerCheck.similarity("acommodation", "accommodation") > 0.9f)
        assertTrue(AnswerCheck.similarity("banana", "accommodation") < 0.5f)
    }

    @Test
    fun mistakeMaskMarksMissingLetters() {
        val mask = AnswerCheck.mistakeMask("acomodation", "accommodation")
        assertEquals(2, mask.count { it })
        assertArrayEquals(booleanArrayOf(false, false, false), AnswerCheck.mistakeMask("cat", "cat"))
        // Пробелы не подчёркиваются.
        assertTrue(!AnswerCheck.mistakeMask("", "give up")[4])
    }

    @Test
    fun idiomsAcceptPlaceholdersAndRealFillers() {
        val t = "drive someone up the wall"
        assertEquals(Verdict.Correct, AnswerCheck.checkTerm("drive someone up the wall", t))
        assertEquals(Verdict.Correct, AnswerCheck.checkTerm("drive sb up the wall", t))
        assertEquals(Verdict.Correct, AnswerCheck.checkTerm("Drive me up the wall", t))
        assertEquals(Verdict.Correct, AnswerCheck.checkTerm("drive my little brother up the wall", t))
        assertEquals(Verdict.Typo, AnswerCheck.checkTerm("drive me up the wal", t))
        assertEquals(Verdict.Wrong, AnswerCheck.checkTerm("drive up the wall", t))
        assertEquals(Verdict.Wrong, AnswerCheck.checkTerm("drive me crazy", t))

        val eye = "catch someone's eye"
        assertEquals(Verdict.Correct, AnswerCheck.checkTerm("catch someone's eye", eye))
        assertEquals(Verdict.Correct, AnswerCheck.checkTerm("catch his eye", eye))
        assertEquals(Verdict.Correct, AnswerCheck.checkTerm("catch John’s eye", eye))
        assertEquals(Verdict.Correct, AnswerCheck.checkTerm("catch my boss's eye", eye))
        assertEquals(Verdict.Wrong, AnswerCheck.checkTerm("catch the eye", eye))

        assertEquals(Verdict.Correct, AnswerCheck.checkTerm("cut him some slack", "cut someone some slack"))
        // Без подстановок — как раньше.
        assertEquals(Verdict.Typo, AnswerCheck.checkTerm("beautifull", "beautiful"))
    }
}
