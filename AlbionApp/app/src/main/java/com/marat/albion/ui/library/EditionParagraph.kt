package com.marat.albion.ui.library

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import kotlin.math.max

/** One logical paragraph, even when its opening lines wrap around an initial. */
data class EditionTextPart(val start: Int, val layout: TextLayoutResult, val origin: Offset)

class EditionParagraph(
    val parts: List<EditionTextPart>,
    val height: Int,
    private val initialHeight: Int = 0,
    val initialFrame: Rect? = null,
) {
    fun offsetAt(position: Offset): Int {
        // The ornament and the letter share the first character's dictionary/selection target.
        if (initialFrame?.contains(position) == true) return 0
        val part = parts.lastOrNull { position.y >= it.origin.y && position.x >= it.origin.x }
            ?: parts.first()
        return part.start + part.layout.getOffsetForPosition(position - part.origin)
    }

    /** Shared by pagination and rendering so the initial never hides a line or loses text. */
    fun offsetThatFits(height: Int): Int {
        if (height < initialHeight) return 0
        var end = 0
        parts.forEach { part ->
            for (line in 0 until part.layout.lineCount) {
                if (part.origin.y + part.layout.getLineBottom(line) <= height) {
                    end = max(end, part.start + part.layout.getLineEnd(line, visibleEnd = false))
                }
            }
        }
        return end
    }
}

/** The exact same text layout is used to split pages, draw glyphs and map taps/selections. */
fun measureEditionParagraph(
    measurer: TextMeasurer,
    text: AnnotatedString,
    body: TextStyle,
    width: Int,
    dropCap: Boolean,
): EditionParagraph {
    val full = measurer.measure(text, body, constraints = Constraints(maxWidth = width.coerceAtLeast(1)))
    return EditionParagraph(listOf(EditionTextPart(0, full, Offset.Zero)), full.size.height)
}

fun readingChapterTitle(title: String): String = title.replace(
    Regex("^(?:chapter|глава)\\s+(?:[0-9]+|[IVXLCDM]+)[.:—–-]?\\s+", RegexOption.IGNORE_CASE), "",
).ifBlank { title }
