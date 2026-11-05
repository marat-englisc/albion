package com.marat.albion.ui.library

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.theme.AlbionFonts
import com.marat.albion.ui.theme.Hero
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/** One logical paragraph, even when its opening lines wrap around an initial. */
data class EditionTextPart(val start: Int, val layout: TextLayoutResult, val origin: Offset)

class EditionParagraph(
    val parts: List<EditionTextPart>,
    val height: Int,
    private val initialHeight: Int = 0,
    val initialFrame: Rect? = null,
    private val initialOffset: Int = 0,
) {
    fun offsetAt(position: Offset): Int {
        // The ornament and the letter share the first character's dictionary/selection target.
        if (initialFrame?.contains(position) == true) return initialOffset
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
    initialColor: Color = Hero.Content,
): EditionParagraph {
    val full = measurer.measure(text, body, constraints = Constraints(maxWidth = width.coerceAtLeast(1)))
    val plain = EditionParagraph(listOf(EditionTextPart(0, full, Offset.Zero)), full.size.height)
    if (!dropCap || text.isEmpty()) return plain

    // Keep opening quotation marks, accents and UTF-16 offsets in the original paragraph.
    val letter = text.text.indexOfFirst(Char::isLetter)
    if (letter !in 0..3 || text.text.take(letter).any { !it.isWhitespace() && it !in "\"'“‘«(—" }) return plain
    val end = BreakIterator.getCharacterInstance(Locale.ROOT).run {
        setText(text.text)
        following(letter)
    }
    if (end <= letter) return plain
    val frameHeight = measurer.measure("Ag\nAg\nAg", body,
        constraints = Constraints(maxWidth = width), maxLines = 3).size.height
    val lineHeight = frameHeight / 3f
    if (lineHeight <= 0f || width < lineHeight * 7f) return plain

    val selected = text.spanStyles.any {
        it.start < end && it.end > letter && it.item.background != Color.Unspecified && it.item.background.alpha > 0f
    }
    val initialText = buildAnnotatedString {
        append(text.subSequence(0, end))
        if (letter > 0) addStyle(SpanStyle(fontSize = body.fontSize, fontFamily = AlbionFonts.Reading), 0, letter)
    }
    var initialStyle = body.copy(
        fontFamily = AlbionFonts.Display, fontWeight = FontWeight.Medium,
        fontSize = body.fontSize * 3.2f, lineHeight = body.fontSize * 3.6f,
        letterSpacing = 0.sp, textAlign = TextAlign.Center,
        color = if (selected) body.color else initialColor,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    var initial = measurer.measure(initialText, initialStyle, softWrap = false, maxLines = 1)
    // Literata's ascenders and descenders exceed its nominal size. Fit its measured metrics,
    // including accents, inside the ornament rather than clipping the capital or hiding it.
    val availableHeight = frameHeight - lineHeight * .35f
    repeat(2) {
        if (initial.size.height > availableHeight) {
            val fit = availableHeight / initial.size.height * .99f
            initialStyle = initialStyle.copy(fontSize = initialStyle.fontSize * fit, lineHeight = initialStyle.lineHeight * fit)
            initial = measurer.measure(initialText, initialStyle, softWrap = false, maxLines = 1)
        }
    }
    val frameWidth = ceil(max(initial.size.width + lineHeight * .6f, lineHeight * 2.4f)).toInt()
    val gap = ceil(lineHeight * .35f).toInt()
    if (frameWidth + gap > width * .55f || initial.size.height > availableHeight + 1f) return plain
    val frame = Rect(0f, 0f, frameWidth.toFloat(), frameHeight.toFloat())
    val parts = mutableListOf(EditionTextPart(0, initial,
        Offset((frameWidth - initial.size.width) / 2f, (frameHeight - initial.size.height) / 2f)))
    var height = frameHeight

    if (end < text.length) {
        val sideWidth = width - frameWidth - gap
        // Greedy wrapping gives the same line ends when the paginator later measures a prefix.
        val sideStyle = body.copy(lineBreak = LineBreak.Simple)
        val side = measurer.measure(text.subSequence(end, text.length), sideStyle,
            constraints = Constraints(maxWidth = sideWidth), maxLines = 3, overflow = TextOverflow.Clip)
        val sideEnd = end + side.getLineEnd(side.lineCount - 1, visibleEnd = false)
        if (sideEnd <= end) return plain
        val opening = measurer.measure(text.subSequence(end, sideEnd), sideStyle,
            constraints = Constraints(maxWidth = sideWidth), maxLines = 3, overflow = TextOverflow.Clip)
        parts += EditionTextPart(end, opening, Offset((frameWidth + gap).toFloat(), 0f))
        height = max(frameHeight, opening.size.height)
        if (sideEnd < text.length) {
            val rest = measurer.measure(text.subSequence(sideEnd, text.length), body,
                constraints = Constraints(maxWidth = width))
            parts += EditionTextPart(sideEnd, rest, Offset(0f, height.toFloat()))
            return EditionParagraph(parts, height + rest.size.height, height, frame, letter)
        }
    }
    return EditionParagraph(parts, height, height, frame, letter)
}

fun readingChapterTitle(title: String): String = title.replace(
    Regex("^(?:chapter|глава)\\s+(?:[0-9]+|[IVXLCDM]+)[.:—–-]?\\s+", RegexOption.IGNORE_CASE), "",
).ifBlank { title }
