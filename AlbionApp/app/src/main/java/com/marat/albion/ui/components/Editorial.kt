package com.marat.albion.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier) { Column(Modifier.padding(contentPadding), content = content) }
}

@Composable
fun StatPill(
    value: Int?,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String = "",
    tint: Color = Color.Unspecified,
    valueColor: Color = Color.Unspecified,
) {
    Column(modifier.padding(8.dp)) {
        Text(value?.let { "$it$suffix" } ?: "—", style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
