package com.marat.albion.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.Ink

/**
 * Строка списка, чья страница открыта рядом (учебник, карточки на планшете):
 * плашка и черта слева — как у выбранной вкладки на боковой панели.
 */
fun Modifier.selectedRow(selected: Boolean): Modifier =
    if (!selected) this
    else semantics { this.selected = true }
        .background(Ink.High)
        .drawBehind { drawRect(Ink.Fg, Offset.Zero, Size(2.dp.toPx(), size.height)) }
