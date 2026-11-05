package com.marat.albion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.selectedBorder
import com.marat.albion.ui.theme.selectedContainer

@Composable
fun Modifier.selectionSurface(selected: Boolean, shape: Shape = MaterialTheme.shapes.medium): Modifier =
    if (!selected) this else background(MaterialTheme.colorScheme.selectedContainer, shape)
        .engravedSurface(MaterialTheme.colorScheme.selectedBorder)
        .border(1.dp, MaterialTheme.colorScheme.selectedBorder, shape)

/** Строка списка, чья страница открыта рядом (учебник, карточки на планшете). */
@Composable
fun Modifier.selectedRow(selected: Boolean): Modifier =
    semantics { this.selected = selected }.selectionSurface(selected)
