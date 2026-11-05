package com.marat.albion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import com.marat.albion.ui.components.PaneBottomSheet as ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** A visible close action and a bounded, scrollable body, including at large text sizes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollableSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        CompositionLocalProvider(LocalDensity provides density) {
            Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = gutter)) {
                Box(Modifier.padding(bottom = 8.dp)) { SheetHeader(title, onDismiss) }
                Column(
                    Modifier.weight(1f, fill = false).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState())
                        .padding(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content,
                )
            }
        }
    }
}
