package com.marat.albion.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.Ink

/** One disclosure pattern for secondary information, usable without animation or gestures. */
@Composable
fun Disclosure(
    title: String,
    summary: String? = null,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .semantics { stateDescription = if (expanded) "Развёрнуто" else "Свёрнуто" }
            .clickable(role = Role.Button) { expanded = !expanded }
            .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
                if (!summary.isNullOrBlank() && !expanded) Text(summary,
                    style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
            }
            Icon(if (expanded) AlbionIcons.KeyboardArrowDown else AlbionIcons.ArrowForward,
                null, tint = Ink.AccentText, modifier = Modifier.padding(start = 12.dp).size(20.dp))
        }
        if (expanded) Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), content = content)
        Hairline()
    }
}

@Composable
fun LoadingState(label: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp).semantics(mergeDescendants = true) {
        liveRegion = LiveRegionMode.Polite
    }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(28.dp), color = Ink.Accent, strokeWidth = 2.dp)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
    }
}

@Composable
fun FieldError(message: String?, modifier: Modifier = Modifier) {
    if (message != null) Text(message, modifier.fillMaxWidth().padding(top = 6.dp).semantics {
        liveRegion = LiveRegionMode.Polite; error(message)
    }, style = MaterialTheme.typography.bodySmall, color = Ink.Error)
}

@Composable
fun AppSettingsButton() {
    val context = LocalContext.current
    TextButton(onClick = {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")))
    }) { Text("Открыть настройки Albion", color = Ink.AccentText) }
}

@Composable
fun SheetHeader(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge, color = Ink.Fg)
        RoundIconButton(AlbionIcons.Close, "Закрыть: $title", onClose)
    }
}

fun highlightedQuery(text: String, query: String): AnnotatedString = buildAnnotatedString {
    append(text)
    val term = query.trim()
    if (term.isNotEmpty()) {
        var start = text.indexOf(term, ignoreCase = true)
        while (start >= 0) {
            addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, background = Ink.GoldSoft,
                color = Ink.Fg), start, start + term.length)
            start = text.indexOf(term, start + term.length, ignoreCase = true)
        }
    }
}

@Composable
fun SheetHandle() { Hairline(Modifier.padding(top = 10.dp, bottom = 6.dp).width(36.dp), color = Ink.LineStrong) }
