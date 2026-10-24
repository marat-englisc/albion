package com.marat.albion.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Классы ширины окна, как в Material: телефон, планшет вертикально / раскладушка, планшет горизонтально. */
enum class WidthClass { Compact, Medium, Expanded }

fun widthClassOf(width: Dp): WidthClass = when {
    width < 600.dp -> WidthClass.Compact
    width < 840.dp -> WidthClass.Medium
    else -> WidthClass.Expanded
}

/** Самая широкая колонка, в которой удобно читать списки и текст. */
val ReadableWidth = 760.dp

/** Колонка для одного задания: карточка, мини-игра, тест. */
val FocusWidth = 680.dp

/**
 * На широком экране экран остаётся колонкой по центру: строки не растягиваются
 * на всю ширину планшета. На телефоне ограничение ни на что не влияет.
 */
@Composable
fun CenteredColumn(maxWidth: Dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxSize()) { content() }
    }
}
