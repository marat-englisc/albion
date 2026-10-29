package com.marat.albion.ui.library

import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.theme.InitialFonts
import com.marat.albion.ui.theme.Ink
import kotlin.math.ceil
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
    val font = if (dropCap && text.isNotEmpty()) InitialFonts.forLetter(text[0]) else null
    if (font == null || text.length < 4 || full.lineCount < 2) {
        return EditionParagraph(listOf(EditionTextPart(0, full, Offset.Zero)), full.size.height)
    }
    val density = full.layoutInput.density
    val bodySize = with(density) { body.fontSize.toPx() }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = InitialFonts.reading
        textSize = bodySize
    }
    val bounds = AndroidRect()
    paint.setFontVariationSettings("'wght' ${body.fontWeight?.weight ?: 400}")
    paint.getTextBounds("H", 0, 1, bounds)
    val capTop = (full.firstBaseline + bounds.top).coerceAtLeast(0f)
    val frameBottom = full.getLineBaseline(1)
    val padding = bodySize * 0.18f
    val frameTop = (capTop - padding).coerceAtLeast(0f)
    val inkHeight = frameBottom - frameTop - padding * 2
    paint.typeface = font.typeface
    paint.setFontVariationSettings(null)
    paint.textSize = 100f
    val letter = text[0].toString()
    paint.getTextBounds(letter, 0, 1, bounds)
    if (bounds.isEmpty) return EditionParagraph(listOf(EditionTextPart(0, full, Offset.Zero)), full.size.height)
    val initialSizePx = 100f * inkHeight / bounds.height()
    paint.textSize = initialSizePx
    paint.getTextBounds(letter, 0, 1, bounds)
    val frameWidth = ceil(bounds.width() + padding * 2)
    val frame = Rect(0f, frameTop, frameWidth, frameBottom)
    val initial = measurer.measure(text.subSequence(0, 1), body.copy(
        fontFamily = font.family, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Normal,
        fontSize = with(density) { initialSizePx.toSp() }, lineHeight = with(density) { initialSizePx.toSp() },
        letterSpacing = 0.sp, color = Ink.Accent,
    ))
    val initialOrigin = Offset(padding - bounds.left, frameTop + padding - bounds.top - initial.firstBaseline)
    val gutter = ceil(bodySize * 0.32f).toInt()
    val indent = frameWidth.toInt() + gutter
    if (width - indent < width * 0.55f) return EditionParagraph(listOf(EditionTextPart(0, full, Offset.Zero)), full.size.height)
    val narrow = Constraints(maxWidth = width - indent)
    val tail = text.subSequence(1, text.length)
    val probe = measurer.measure(tail, body, constraints = narrow)
    val leadEnd = 1 + probe.getLineEnd(minOf(1, probe.lineCount - 1), visibleEnd = false)
    val lead = measurer.measure(text.subSequence(1, leadEnd), body, constraints = narrow)
    // Only the visible frame and body lines reserve height. A display font's ascenders,
    // descenders and line padding must not push the following text down or prolong its indent.
    val leadingHeight = max(ceil(frame.bottom).toInt(), lead.size.height)
    val parts = mutableListOf(EditionTextPart(0, initial, initialOrigin), EditionTextPart(1, lead, Offset(indent.toFloat(), 0f)))
    var totalHeight = leadingHeight
    if (leadEnd < text.length) {
        val rest = measurer.measure(text.subSequence(leadEnd, text.length), body, constraints = Constraints(maxWidth = width))
        // Separate text layouts trim their first/last line leading. Align baselines, rather
        // than stacking their boxes, to preserve the book's exact line rhythm at the join.
        val restY = probe.getLineBaseline(2) - rest.firstBaseline
        parts += EditionTextPart(leadEnd, rest, Offset(0f, restY))
        totalHeight = ceil(restY + rest.size.height).toInt()
    }
    return EditionParagraph(parts, totalHeight, leadingHeight, frame)
}

fun readingChapterTitle(title: String): String = title.replace(
    Regex("^(?:chapter|глава)\\s+(?:[0-9]+|[IVXLCDM]+)[.:—–-]?\\s+", RegexOption.IGNORE_CASE), "",
).ifBlank { title }
