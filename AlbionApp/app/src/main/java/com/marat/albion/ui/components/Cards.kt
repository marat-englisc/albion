package com.marat.albion.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.LocalFeedback

@Composable
fun BoundVolume(modifier: Modifier = Modifier, compact: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun CatalogueTab(label: String, modifier: Modifier = Modifier) {
    Text(label, modifier, style = MaterialTheme.typography.labelMedium)
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
    Card(onClick = { feedback.selection(); onClick() }, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            if (progress != null && progress > 0f) {
                Spacer(Modifier.height(8.dp))
                ThinProgress(progress)
            }
        }
    }
}

@Composable
fun FolioDivider(modifier: Modifier = Modifier, label: String? = null) {
    if (label != null) Text(label, modifier, style = MaterialTheme.typography.labelMedium)
    else HorizontalDivider(modifier)
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
