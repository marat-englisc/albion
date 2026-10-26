package com.marat.albion.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marat.albion.data.audio.ShadowReport
import com.marat.albion.ui.theme.Ink
import kotlin.math.abs
import kotlin.math.ceil

/** Общая шкала относительной высоты; время записи уже сопоставлено с диктором. */
@Composable
internal fun MelodyChart(report: ShadowReport) {
    val original = remember(report) { preparePitchContour(report.originalPitch, report.originalEnergy) }
    val mine = remember(report) { preparePitchContour(report.minePitch, report.mineEnergy) }
    // Диктор — сплошная линия цвета шарфа, вы — тёплый пунктир цвета клюва.
    val fg = Ink.Accent
    val muted = Ink.Beak
    val grid = Ink.Line
    val center = Ink.LineStrong
    val band = Ink.Surface

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (original.available || mine.available) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (original.available) MelodyLegend(dashed = false, label = "Диктор")
                if (mine.available) MelodyLegend(dashed = true, label = "Вы")
            }
            Row(
                Modifier.fillMaxWidth().background(band, RoundedCornerShape(16.dp)).padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    Modifier.height(144.dp).padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Выше", style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
                    Text("Ниже", style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
                }
                Column(Modifier.weight(1f)) {
                    Canvas(Modifier.fillMaxWidth().height(144.dp).semantics {
                        contentDescription = "Мелодика: подъёмы и спады тона от начала к концу фразы"
                    }) {
                        val points = (original.segments + mine.segments).flatten()
                        val limit = maxOf(4f, ceil((points.maxOfOrNull { abs(it.semitones) } ?: 4f) / 2f) * 2f)
                        val frames = maxOf(report.originalPitch.size, report.minePitch.size, 2)
                        val inset = 4.dp.toPx()
                        val width = (size.width - 2 * inset).coerceAtLeast(0f)
                        val height = size.height - 2 * inset
                        fun x(frame: Int) = inset + frame.toFloat() / (frames - 1) * width
                        fun y(semitones: Float) = inset + (1f - semitones / limit) * height / 2

                        for (level in listOf(-1f, 0f, 1f)) {
                            val yy = y(level * limit)
                            drawLine(
                                if (level == 0f) center else grid, Offset(inset, yy), Offset(size.width - inset, yy), 1.dp.toPx(),
                                pathEffect = if (level == 0f) null else PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
                            )
                        }
                        fun drawContour(contour: PitchContour, dashed: Boolean) {
                            for (segment in contour.segments) {
                                val path = Path()
                                path.moveTo(x(segment.first().frame), y(segment.first().semitones))
                                // Контрольные точки лежат между соседними измерениями: кривая
                                // не создаёт новых пиков и не пересекает настоящие разрывы.
                                for (i in 1 until segment.size) {
                                    val a = segment[i - 1]
                                    val b = segment[i]
                                    val midX = (x(a.frame) + x(b.frame)) / 2
                                    path.cubicTo(midX, y(a.semitones), midX, y(b.semitones), x(b.frame), y(b.semitones))
                                }
                                drawPath(
                                    path, if (dashed) muted else fg,
                                    style = Stroke(
                                        width = (if (dashed) 2.5.dp else 3.dp).toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                                        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null,
                                    ),
                                )
                            }
                        }
                        drawContour(mine, dashed = true)
                        drawContour(original, dashed = false)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Начало", style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
                        Text("Конец", style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
                    }
                }
            }
            Text(
                "Середина — обычный тон каждого голоса. Разрывы — паузы и звуки без тона.",
                style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
            )
        }
        if (!original.available) {
            Text(
                "В записи диктора мало участков с различимым тоном. Сравните интонацию на слух.",
                style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
            )
        }
        if (!mine.available) {
            Text(
                "Не удалось уверенно определить тон вашей записи. Попробуйте записать ещё раз ближе к микрофону.",
                style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
            )
        }
    }
}

@Composable
private fun MelodyLegend(dashed: Boolean, label: String) {
    val color = if (dashed) Ink.Beak else Ink.Accent
    Row(
        Modifier.background(Ink.Surface, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(width = 24.dp, height = 8.dp)) {
            drawLine(
                color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Ink.Soft)
    }
}
