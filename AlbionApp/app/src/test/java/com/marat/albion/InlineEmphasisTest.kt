package com.marat.albion

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.marat.albion.ui.grammar.inlineMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineEmphasisTest {
    @Test
    fun unpairedUnderscoresAndAsterisksRemainLiteral() {
        for (text in listOf("__unclosed", "_unclosed", "**unclosed", "*unclosed", "closed__", "__ spaced __", "_ spaced _")) {
            val rendered = render(text)
            assertEquals(text, rendered.text)
            assertTrue(rendered.spanStyles.all { it.item.fontWeight == null && it.item.fontStyle == null })
        }
    }

    @Test
    fun underscoresInsideWordsAreLiteralEvenInsideItalicText() {
        assertEquals("file_name__suffix a_b_c", render("file_name__suffix a_b_c").text)
        val text = render("*file_name and word_value* end")
        assertEquals("file_name and word_value end", text.text)
        assertEquals(FontStyle.Italic, styleAt(text, "file").fontStyle)
        assertEquals(null, styleAt(text, "end").fontStyle)
    }

    @Test
    fun underscoreAndAsteriskMarkersMustMatch() {
        assertEquals("_one* two", render("_one* two").text)
        assertEquals("__one** two", render("__one** two").text)
        val mixed = render("*one _two* three_")
        assertEquals("one _two three_", mixed.text)
        assertEquals(FontStyle.Italic, styleAt(mixed, "one").fontStyle)
        assertEquals(null, styleAt(mixed, "three").fontStyle)
    }

    @Test
    fun nestedEmphasisRestoresTheOuterStyle() {
        val text = render("__one _two_ three__ end")
        assertEquals("one two three end", text.text)
        assertEquals(FontWeight.SemiBold, styleAt(text, "one").fontWeight)
        assertEquals(FontStyle.Italic, styleAt(text, "two").fontStyle)
        assertEquals(FontWeight.SemiBold, styleAt(text, "three").fontWeight)
        assertEquals(null, styleAt(text, "three").fontStyle)
        assertEquals(null, styleAt(text, "end").fontWeight)
        val stars = render("*one **two*** end")
        assertEquals("one two end", stars.text)
        assertEquals(FontWeight.SemiBold, styleAt(stars, "two").fontWeight)
        assertEquals(FontStyle.Italic, styleAt(stars, "two").fontStyle)
    }

    @Test
    fun unicodeWordsAndPunctuationRespectTheBoundaries() {
        val text = render("«_слово_» __другое__")
        assertEquals("«слово» другое", text.text)
        assertEquals(FontStyle.Italic, styleAt(text, "слово").fontStyle)
        assertEquals(FontWeight.SemiBold, styleAt(text, "другое").fontWeight)
        assertEquals("имя_файла", render("имя_файла").text)
    }

    @Test
    fun literalGapsAreNotPairedAcrossOtherGaps() {
        for (text in listOf("(___) and (___)", "I ___ (lose) my keys.", "word___ and ___word", "__ and __", "___, then ___.")) {
            assertEquals(text, render(text).text)
        }
    }

    @Test
    fun escapedMarkersCodeAndLinkTargetsDoNotOpenEmphasis() {
        assertEquals("__word__", render("\\_\\_word\\_\\_").text)
        val code = render("__before `x_y` after__ end")
        assertTrue(code.text.contains("x_y"))
        assertEquals(FontWeight.SemiBold, styleAt(code, "after").fontWeight)
        assertEquals(null, styleAt(code, "end").fontWeight)
        assertEquals("label end", render("[label](https://example.org/a_b) end").text)
        assertEquals("alias end", render("[[file_name|alias]] end").text)
    }

    private fun render(text: String) = inlineMarkdown(text, {}, {})
    private fun styleAt(text: AnnotatedString, word: String) =
        text.spanStyles.single { text.text.indexOf(word) in it.start until it.end }.item
}
