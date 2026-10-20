package com.marat.albion

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.marat.albion.data.db.GrammarTaskEntity
import com.marat.albion.data.grammar.GrammarPrompt
import com.marat.albion.ui.games.GAP_MARK
import com.marat.albion.ui.games.promptWithGap
import com.marat.albion.ui.grammar.inlineMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarGapTest {
    @Test
    fun fillsEachGapInTheFourAffectedExercises() {
        val cases = listOf(
            Triple("___ you ever ___ sushi?", "Have / eaten", "Have you ever eaten sushi?"),
            Triple("I ___ never ___ such a beautiful place before that trip.", "had / seen",
                "I had never seen such a beautiful place before that trip."),
            Triple("When I got to the station, the train ___ already ___.", "had / left",
                "When I got to the station, the train had already left."),
            Triple("The tickets ___ already ___.", "have / been sold", "The tickets have already been sold."),
        )
        cases.forEach { (prompt, answer, expected) ->
            assertEquals(expected, GrammarPrompt(prompt).complete(answer))
            val rendered = promptWithGap(prompt, answer)
            assertEquals(expected, rendered.text)
            val underlined = rendered.spanStyles.filter { it.item.textDecoration == TextDecoration.Underline }
                .map { rendered.text.substring(it.start, it.end) }
            assertEquals(answer.split(" / "), underlined)
        }
    }

    @Test
    fun choiceAnswerRemainsACompleteCombinationInTheResults() {
        val task = task("___ you ever ___ sushi?", "Have / eaten", "choice")
        assertEquals(listOf("Have / eaten"), task.acceptedAnswers)
        assertEquals("Have you ever eaten sushi?", task.correctAnswerText)
    }

    @Test
    fun oneGapKeepsItsMultiwordAnswerAndInputAlternatives() {
        val task = task("She ___ to London.", "has gone / 's gone")
        assertEquals(listOf("has gone", "'s gone"), task.acceptedAnswers)
        assertEquals("She has gone to London.", task.correctAnswerText)
        assertEquals("She's gone to London.", promptWithGap(task.prompt, "'s gone").text)
        assertEquals("She's gone to London.", GrammarPrompt(task.prompt).complete("'s gone"))
        assertEquals("They’ve arrived.", promptWithGap("They ___ arrived.", "’ve").text)
    }

    @Test
    fun inputAlternativesForSeveralGapsAreSeparateCombinations() {
        val task = task("___ you ___ sushi?", " Have/eaten \nDid / eat")
        assertEquals(listOf("Have / eaten", "Did / eat"), task.acceptedAnswers)
        assertEquals("Have you eaten sushi?", task.correctAnswerText)
        assertEquals(listOf("Have", "eaten"), GrammarPrompt(task.prompt).answerParts("Have /eaten"))
    }

    @Test
    fun missingOrExtraPartsAreNeverRepeatedOrSilentlyDiscarded() {
        val prompt = "___ you ever ___ sushi?"
        val parsed = GrammarPrompt(prompt)
        for (answer in listOf("", "Have", "Have / ", " / eaten", "Have / eaten / too")) {
            assertNull(answer, parsed.complete(answer))
            assertEquals("$GAP_MARK you ever $GAP_MARK sushi?", promptWithGap(prompt, answer).text)
        }
        assertEquals("$GAP_MARK you ever $GAP_MARK sushi?", promptWithGap(prompt, null).text)
    }

    @Test
    fun supportsThreeGapsAndLongerUnderscoreRuns() {
        val parsed = GrammarPrompt("___ / ______ / ___")
        assertEquals(3, parsed.gapCount)
        assertEquals("one / two / three", promptWithGap("___ / ______ / ___", "one/two/three").text)
        assertEquals("They went home.", promptWithGap("They ______ home.", "went").text)
    }

    @Test
    fun questionsWithoutGapsKeepTheirOriginalTextAndAnswer() {
        val task = task("Choose the correct sentence.", "I live in London.", "choice")
        assertEquals("I live in London.", task.correctAnswerText)
        assertEquals(task.prompt, promptWithGap(task.prompt, task.answer).text)
    }

    @Test
    fun markdownKeepsLiteralGapsWithoutChangingTheFollowingTextStyle() {
        for (text in listOf("I ___ (lose) my keys.", "___ you ever ___ sushi?", "Fill ___, then ___.", "___", "______")) {
            val rendered = inlineMarkdown(text, {}, {})
            assertEquals(text, rendered.text)
            assertTrue(rendered.spanStyles.all { it.item.fontWeight == null && it.item.fontStyle == null })
        }
        assertEquals("Fill ___ here", inlineMarkdown("**Fill ___ here**", {}, {}).text)
    }

    @Test
    fun markdownStillSupportsUnderscoreEmphasisAndEscapedGaps() {
        val bold = inlineMarkdown("__word__", {}, {})
        assertEquals("word", bold.text)
        assertTrue(bold.spanStyles.any { it.item.fontWeight == FontWeight.SemiBold })
        val both = inlineMarkdown("___word___", {}, {})
        assertEquals("word", both.text)
        assertTrue(both.spanStyles.any { it.item.fontWeight == FontWeight.SemiBold && it.item.fontStyle == FontStyle.Italic })
        assertEquals("___", inlineMarkdown("\\_\\_\\_", {}, {}).text)
    }

    private fun task(prompt: String, answer: String, kind: String = "input") = GrammarTaskEntity(
        id = "test", position = 0, level = "A2", topic = 33, topicTitle = "",
        kind = kind, prompt = prompt, options = "", answer = answer, explanation = "",
    )
}
