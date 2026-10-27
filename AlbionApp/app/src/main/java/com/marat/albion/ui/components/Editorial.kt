package com.marat.albion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Ink

/** A quiet, bordered surface for the primary content of a screen. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    color: Color = Ink.Raised,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Column(modifier.background(color, shape).border(1.dp, Ink.Line, shape).padding(contentPadding), content = content)
}

/** Compact metric used across daily study, deck summaries and statistics. */
@Composable
fun StatPill(
    value: Int?,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String = "",
    tint: Color = Ink.Raised,
    valueColor: Color = if (value == null || value == 0) Ink.Muted else Ink.Fg,
) {
    Column(modifier.background(tint, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 14.dp)) {
        if (value == null) Text("—", style = AlbionType.numberSmall, color = Ink.Faint)
        else AnimatedNumber(value, AlbionType.numberSmall, color = valueColor, suffix = suffix)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
    }
}
