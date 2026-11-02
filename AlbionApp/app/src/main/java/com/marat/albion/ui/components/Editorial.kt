package com.marat.albion.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.AnimatedNumber

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
