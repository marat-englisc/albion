package com.marat.albion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.AnimatedNumber

data class StatMetric(val value: Int?, val label: String, val suffix: String = "")

/** Keep whole words and the full value at the user's font size, including four-digit totals. */
@Composable
fun metricMinWidth(metrics: List<StatMetric>, valueStyle: TextStyle, labelStyle: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val width = metrics.maxOf { metric ->
        maxOf(
            measurer.measure(metric.value?.let { "$it${metric.suffix}" } ?: "—", valueStyle).size.width,
            metric.label.split(Regex("\\s+")).maxOf { measurer.measure(it, labelStyle).size.width },
        )
    }
    return with(LocalDensity.current) { width.toDp() }
}

@Composable
fun AdaptivePair(
    minItemWidth: Dp,
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (minItemWidth * 2 + spacing <= maxWidth) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                first(Modifier.weight(1f).fillMaxHeight())
                second(Modifier.weight(1f).fillMaxHeight())
            }
        } else Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun StatPair(first: StatMetric, second: StatMetric, modifier: Modifier = Modifier) {
    val minWidth = metricMinWidth(listOf(first, second), MaterialTheme.typography.displaySmall, MaterialTheme.typography.bodySmall) + 40.dp
    AdaptivePair(minWidth, modifier,
        first = { StatPill(first.value, first.label, it, first.suffix) },
        second = { StatPill(second.value, second.label, it, second.suffix) })
}

@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Panel(modifier, contentPadding = contentPadding, content = content)
}

/** Показатель: крупная «катящаяся» цифра акцентным шрифтом и подпись. */
@Composable
fun StatPill(
    value: Int?,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String = "",
    tint: Color = Color.Unspecified,
    valueColor: Color = Color.Unspecified,
) {
    Panel(modifier) {
        if (value == null) Text("—", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else AnimatedNumber(value, MaterialTheme.typography.displaySmall, suffix = suffix)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
