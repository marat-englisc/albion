package com.marat.albion.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** Native bookcloth, spine bands and gilt rules; the text remains ordinary accessible Compose. */
@Composable
fun BoundVolume(modifier: Modifier = Modifier, compact: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val crimson = Ink.Accent
    val gold = Ink.Gold
    val ivory = Ink.Ivory
    Column(modifier.clip(RoundedCornerShape(4.dp)).drawWithCache {
        val random = Random(3927)
        val fibres = List((size.width * size.height / 1350f).toInt().coerceAtMost(2600)) {
            Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
        }
        val spine = 19.dp.toPx()
        val edge = 5.dp.toPx()
        onDrawBehind {
            drawRect(crimson)
            drawRect(Color(0xFF66172C), size = Size(spine, size.height))
            drawLine(gold.copy(alpha = 0.35f), Offset(spine, 0f), Offset(spine, size.height), 0.5.dp.toPx())
            fibres.forEach { p -> drawLine(ivory.copy(alpha = 0.045f), p, p + Offset(1.5f, 0.45f), 0.7f) }
            for (y in listOf(24.dp.toPx(), size.height - 32.dp.toPx())) {
                repeat(3) { i ->
                    drawLine(gold.copy(alpha = 0.9f), Offset(5.dp.toPx(), y + i * 4.dp.toPx()),
                        Offset(spine - 5.dp.toPx(), y + i * 4.dp.toPx()), 1.4.dp.toPx())
                }
            }
            for (inset in listOf(0f, 3.dp.toPx())) {
                val left = spine + 7.dp.toPx() + inset
                val top = 7.dp.toPx() + inset
                drawRect(gold.copy(alpha = if (inset == 0f) 0.7f else 0.4f), Offset(left, top),
                    Size((size.width - left - 13.dp.toPx() - inset).coerceAtLeast(0f), (size.height - top * 2).coerceAtLeast(0f)),
                    style = Stroke(0.5.dp.toPx()))
            }
            repeat(2) { i ->
                val x = size.width - edge - i * 3.dp.toPx()
                drawLine(ivory.copy(alpha = 0.8f), Offset(x, 0f), Offset(x, size.height), 0.75.dp.toPx())
            }
        }
    }.padding(start = if (compact) 35.dp else 42.dp, end = 23.dp, top = if (compact) 17.dp else 23.dp,
        bottom = if (compact) 17.dp else 23.dp), content = content)
}

/** A postal cancellation made from typography and thin curves, not a raster illustration. */
@Composable
fun LondonPostmark(modifier: Modifier = Modifier, date: Date = Date()) {
    val label = remember(date.time / 86_400_000L) { SimpleDateFormat("dd MMM yyyy", Locale.UK).format(date).uppercase(Locale.UK) }
    val ink = Ink.Muted.copy(alpha = 0.55f)
    Canvas(modifier.size(116.dp, 65.dp).clearAndSetSemantics {}) {
        val r = 27.dp.toPx()
        val c = Offset(size.width - r - 2.dp.toPx(), size.height / 2)
        drawCircle(ink, r, c, style = Stroke(0.55.dp.toPx()))
        repeat(4) { row ->
            val base = size.height / 2 - 13.dp.toPx() + row * 8.dp.toPx()
            val wave = Path().apply {
                moveTo(0f, base)
                cubicTo(19.dp.toPx(), base - 8.dp.toPx(), 27.dp.toPx(), base + 9.dp.toPx(), c.x - r + 2.dp.toPx(), base)
            }
            drawPath(wave, ink, style = Stroke(0.55.dp.toPx()))
        }
        val canvas = drawContext.canvas.nativeCanvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink.toArgb(); textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = 7.dp.toPx(); isSubpixelText = true
        }
        canvas.save()
        canvas.rotate(-15f, c.x, c.y)
        canvas.drawText("L O N D O N", c.x, c.y - 10.dp.toPx(), paint)
        paint.textSize = 6.5.dp.toPx()
        canvas.drawText(label, c.x, c.y + 2.dp.toPx(), paint)
        canvas.drawText("E N G L A N D", c.x, c.y + 14.dp.toPx(), paint)
        canvas.restore()
    }
}

@Composable
fun RibbonBookmark(modifier: Modifier = Modifier, width: Dp = 16.dp, height: Dp = 39.dp, color: Color = Ink.Accent) {
    Canvas(modifier.size(width, height).clearAndSetSemantics {}) {
        drawPath(Path().apply {
            moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height)
            lineTo(size.width / 2, size.height - size.width * 0.32f); lineTo(0f, size.height); close()
        }, color)
        drawLine(Ink.Ivory.copy(alpha = 0.18f), Offset(1.dp.toPx(), 0f), Offset(1.dp.toPx(), size.height - 1.dp.toPx()), 0.5.dp.toPx())
    }
}

@Composable
fun CatalogueTab(label: String, modifier: Modifier = Modifier) {
    val fill = Ink.Surface
    val line = Ink.Line
    Text(label.uppercase(Locale.ROOT), modifier.drawWithCache {
        val shape = Path().apply {
            moveTo(0f, size.height); lineTo(0f, 3.dp.toPx()); quadraticTo(0f, 0f, 3.dp.toPx(), 0f)
            lineTo(size.width - 12.dp.toPx(), 0f); lineTo(size.width, size.height); close()
        }
        onDrawBehind { drawPath(shape, fill); drawPath(shape, line, style = Stroke(0.6.dp.toPx())) }
    }.padding(start = 12.dp, end = 23.dp, top = 6.dp, bottom = 5.dp),
        style = AlbionType.eyebrow.copy(fontSize = 12.sp, letterSpacing = 0.6.sp), color = Ink.Soft)
}

@Composable
fun CatalogueCard(
    label: String,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ribbon: Boolean = false,
    progress: Float? = null,
    compact: Boolean = false,
) {
    val feedback = LocalFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.fillMaxWidth().pressScale(interaction, 0.995f).clickable(
        interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, role = Role.Button,
        onClick = { feedback.selection(); onClick() },
    )) {
        CatalogueTab(label, Modifier.padding(start = 8.dp))
        Box(Modifier.fillMaxWidth().background(Ink.Raised, RoundedCornerShape(3.dp))
            .border(0.6.dp, Ink.Line, RoundedCornerShape(3.dp))) {
            if (ribbon) RibbonBookmark(Modifier.align(Alignment.TopEnd).padding(end = 18.dp))
            Column(Modifier.fillMaxWidth().padding(start = 17.dp, end = if (ribbon) 42.dp else 17.dp, top = if (compact) 12.dp else 17.dp, bottom = if (compact) 12.dp else 17.dp)) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = headingFamily(title), fontSize = if (compact) 21.sp else 25.sp, lineHeight = if (compact) 25.sp else 30.sp),
                        color = Ink.Fg, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(14.dp))
                    Icon(AlbionIcons.ArrowForward, null, tint = Ink.Fg, modifier = Modifier.size(19.dp))
                }
                if (progress != null && progress > 0f) {
                    Spacer(Modifier.height(13.dp)); ThinProgress(progress, color = Ink.GoldText)
                }
            }
        }
    }
}

@Composable
fun FolioDivider(modifier: Modifier = Modifier, label: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Box(Modifier.width(30.dp).height(0.5.dp).background(Ink.Gold))
        Spacer(Modifier.width(15.dp))
        if (label != null) Text(label, style = MaterialTheme.typography.labelMedium.copy(fontFamily = Brand, fontWeight = FontWeight.SemiBold), color = Ink.GoldText)
        else Canvas(Modifier.size(36.dp, 8.dp).clearAndSetSemantics {}) {
            repeat(3) { i ->
                val x = (6 + i * 12).dp.toPx(); val y = size.height / 2; val r = 2.dp.toPx()
                drawPath(Path().apply { moveTo(x, y-r); lineTo(x+r, y); lineTo(x, y+r); lineTo(x-r, y); close() }, Ink.Gold)
            }
        }
        Spacer(Modifier.width(15.dp))
        Box(Modifier.width(30.dp).height(0.5.dp).background(Ink.Gold))
    }
}

@Composable
fun AccessionStamp(number: String, modifier: Modifier = Modifier) {
    Text("ALBION\nLONDON\n$number", modifier.clearAndSetSemantics {}.border(0.5.dp, Ink.LineStrong).padding(horizontal = 10.dp, vertical = 6.dp),
        style = AlbionType.eyebrow.copy(fontSize = 9.sp, lineHeight = 14.sp, letterSpacing = 1.5.sp),
        color = Ink.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

fun romanNumeral(value: Int): String {
    if (value !in 1..3999) return value.toString()
    var rest = value
    return buildString {
        for ((number, glyph) in listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
            50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")) {
            while (rest >= number) { append(glyph); rest -= number }
        }
    }
}
