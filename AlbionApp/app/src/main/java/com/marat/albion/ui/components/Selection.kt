package com.marat.albion.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/** Строка списка, чья страница открыта рядом (учебник, карточки на планшете). */
@Composable
fun Modifier.selectedRow(selected: Boolean): Modifier =
    if (!selected) this
    else semantics { this.selected = true }.background(MaterialTheme.colorScheme.secondaryContainer)
