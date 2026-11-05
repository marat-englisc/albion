package com.marat.albion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Consequences scroll together, while both actions remain reachable at large font scales. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestructiveConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Удалить",
    busy: Boolean = false,
    busyLabel: String = "Удаляем…",
    errorMessage: String? = null,
) {
    BasicAlertDialog(onDismissRequest = { if (!busy) onDismiss() }) {
        ConstrainPopupToPane()
        BoxWithConstraints(Modifier.padding(horizontal = 16.dp)) {
            val scheme = MaterialTheme.colorScheme
            val measurer = rememberTextMeasurer()
            val titleStyle = MaterialTheme.typography.headlineSmall
            val longestWord = title.split(Regex("\\s+")).maxOfOrNull {
                measurer.measure(it, titleStyle, softWrap = false).size.width
            } ?: 0
            val horizontalInset = with(LocalDensity.current) {
                ((maxWidth - longestWord.toDp()) / 2).coerceIn(8.dp, 20.dp)
            }
            Surface(Modifier.fillMaxWidth().heightIn(max = (maxHeight - 24.dp).coerceAtLeast(0.dp)),
                shape = MaterialTheme.shapes.extraLarge, color = scheme.surfaceContainerHigh) {
                Column(Modifier.engravedSurface(scheme.secondary).paperGrain(scheme.outline)
                    .folioFrame(scheme.outlineVariant).padding(horizontal = horizontalInset, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(title, Modifier.semantics { heading() }, style = titleStyle,
                            color = scheme.onSurface)
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        FieldError(errorMessage)
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val label = if (busy) busyLabel else confirmLabel
                        val style = MaterialTheme.typography.labelLarge
                        val needed = with(LocalDensity.current) {
                            // Both buttons receive equal width, so each must fit the wider label.
                            maxOf(measurer.measure(label, style, softWrap = false).size.width,
                                measurer.measure("Отмена", style, softWrap = false).size.width).toDp() * 2 + 72.dp
                        }
                        val cancel: @Composable (Modifier) -> Unit = { modifier ->
                            TextButton(onClick = onDismiss, enabled = !busy, modifier = modifier.heightIn(min = 48.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = scheme.onSurfaceVariant)) {
                                Text("Отмена", Modifier.weight(1f), style = style, textAlign = TextAlign.Center)
                            }
                        }
                        val confirm: @Composable (Modifier) -> Unit = { modifier ->
                            TextButton(onClick = onConfirm, enabled = !busy, modifier = modifier.heightIn(min = 48.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = scheme.error)) {
                                Text(label, Modifier.weight(1f), style = style, textAlign = TextAlign.Center)
                            }
                        }
                        if (needed <= maxWidth) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            cancel(Modifier.weight(1f)); confirm(Modifier.weight(1f))
                        } else Column {
                            confirm(Modifier.fillMaxWidth()); cancel(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
