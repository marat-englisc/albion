package com.marat.albion.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.ui.theme.Ink
import kotlinx.coroutines.launch

/** Инструкция один раз при знакомстве с разделом, затем — по кнопке справки. */
@Composable
fun FeatureHelp(key: String, title: String, text: String, showOnFirstUse: Boolean = true, mascot: Finn? = null) {
    val c = LocalAppContainer.current
    val viewed by c.settings.viewedHelp.collectAsStateWithLifecycle(null)
    var expanded by rememberSaveable(key) { mutableStateOf(false) }
    var presented by rememberSaveable(key) { mutableStateOf(false) }

    LaunchedEffect(viewed, key) {
        val known = viewed ?: return@LaunchedEffect
        if (showOnFirstUse && key !in known && !presented) {
            presented = true
            expanded = true
        }
    }

    fun dismiss() {
        expanded = false
        // Запись завершается, даже если пользователь сразу уходит с экрана.
        c.appScope.launch { c.settings.markHelpViewed(key) }
    }

    RoundIconButton(Icons.AutoMirrored.Outlined.HelpOutline, "Справка: $title", onClick = { presented = true; expanded = true }, spin = -20f)
    if (expanded) {
        AlertDialog(
            onDismissRequest = ::dismiss,
            containerColor = Ink.Raised,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            // Финн «переходит» со страницы в диалог и показывает, чем здесь занимаются.
            icon = mascot?.let { { Mascot(it, 104.dp) } },
            title = { Text(title, style = MaterialTheme.typography.headlineSmall, color = Ink.Fg) },
            text = {
                Text(text, style = MaterialTheme.typography.bodyMedium, color = Ink.Text,
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()))
            },
            confirmButton = { PrimaryButton("Понятно", onClick = ::dismiss) },
        )
    }
}
