package com.marat.albion.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marat.albion.data.repo.DeckSummary
import com.marat.albion.ui.components.AlbionIcons
import com.marat.albion.ui.components.Panel
import com.marat.albion.ui.components.ThinProgress
import androidx.compose.foundation.border

/** Пункт дневного плана: значок (галочка, когда выполнено), заголовок, пояснение и стрелка. */
@Composable
internal fun PlanRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    done: Boolean,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Panel(modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(if (done) scheme.tertiary else scheme.background, MaterialTheme.shapes.small)
                .border(.75.dp, scheme.outlineVariant, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(if (done) AlbionIcons.Check else icon, null, Modifier.size(22.dp), tint = if (done) scheme.onTertiary else scheme.onSurface)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (progress != null && progress > 0f) {
                    Spacer(Modifier.height(8.dp))
                    ThinProgress(progress, height = 4.dp)
                }
            }
            Spacer(Modifier.width(10.dp))
            Icon(AlbionIcons.ArrowForward, null, Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
        }
    }
}

/** Строка колоды: сколько ждёт сегодня (крупно), название, прогресс и стрелка. */
@Composable
internal fun DeckRow(summary: DeckSummary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Panel(modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Число карточек на сегодня; расшифровка — в строке под названием («5 повторить · 15 новых»).
            Column(Modifier.size(56.dp).background(scheme.background, MaterialTheme.shapes.small)
                .border(.75.dp, scheme.outlineVariant, MaterialTheme.shapes.small)
                .semantics { contentDescription = "На сегодня: ${summary.toStudy}" },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("${summary.toStudy}", style = MaterialTheme.typography.titleLarge,
                    color = if (summary.toStudy > 0) scheme.onSurface else scheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(summary.deck.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(when {
                    summary.total == 0 -> summary.deck.subtitle
                    summary.toStudy == 0 -> "На сегодня всё · ${summary.started} из ${summary.total} начато"
                    else -> listOfNotNull(
                        summary.due.takeIf { it > 0 }?.let { "$it повторить" },
                        summary.newAvailable.takeIf { it > 0 }?.let { "$it ${com.marat.albion.ui.library.pluralRu(it, "новая", "новые", "новых")}" },
                    ).joinToString(" · ")
                }, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (summary.started > 0) {
                    Spacer(Modifier.height(8.dp))
                    ThinProgress(summary.progress, height = 4.dp)
                }
            }
            Spacer(Modifier.width(10.dp))
            Icon(AlbionIcons.ArrowForward, null, Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
        }
    }
}
