package com.marat.albion.ui.components

import com.marat.albion.ui.theme.selectedContent
import com.marat.albion.ui.theme.controlBorder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.LocalFeedback

/** One visible value for a secondary choice; the complete options stay in an anchored menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionPicker(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    val current = selected.coerceIn(options.indices)
    var expanded by remember { mutableStateOf(false) }
    val feedback = LocalFeedback.current
    val scheme = MaterialTheme.colorScheme

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { feedback.selection(); expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .semantics { contentDescription = label },
            shape = MaterialTheme.shapes.medium,
            color = scheme.surfaceContainerLow,
            border = BorderStroke(if (expanded) 2.dp else 1.dp, if (expanded) scheme.primary else scheme.controlBorder),
        ) {
            Row(Modifier.paperGrain(scheme.outline).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    Text(options[current], style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                }
                DisclosureIndicator(expanded)
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = scheme.surfaceContainer,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, scheme.controlBorder),
        ) {
            ConstrainPopupToPane()
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        expanded = false
                        if (index != current) feedback.selection()
                        onSelect(index)
                    },
                    trailingIcon = {
                        if (index == current) Icon(AlbionIcons.Check, null, Modifier.size(20.dp))
                        else Spacer(Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .selectionSurface(index == current, MaterialTheme.shapes.extraSmall)
                        .semantics { this.selected = index == current },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    colors = MenuDefaults.itemColors(
                        textColor = if (index == current) scheme.selectedContent else scheme.onSurface,
                        trailingIconColor = if (index == current) scheme.selectedContent else scheme.onSurface),
                )
            }
        }
    }
}
