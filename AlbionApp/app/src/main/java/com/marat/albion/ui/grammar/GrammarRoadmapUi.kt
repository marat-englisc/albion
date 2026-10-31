package com.marat.albion.ui.grammar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.selected
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.data.db.GrammarProgressEntity
import com.marat.albion.data.grammar.GrammarBook
import com.marat.albion.data.grammar.GrammarNextStep
import com.marat.albion.data.grammar.GrammarReview
import com.marat.albion.data.grammar.Topic
import com.marat.albion.ui.components.*
import com.marat.albion.ui.theme.*

internal data class GrammarStage(val level: String, val name: String, val title: String, val topics: List<Topic>)

internal fun grammarStages(book: GrammarBook): List<GrammarStage> = listOf(
    Triple("A1", "Foundations", "Основы английского"),
    Triple("A2", "Everyday English", "Английский на каждый день"),
    Triple("B1", "An Independent Voice", "Самостоятельная речь"),
    Triple("B2", "Precision & Fluency", "Точность и свобода"),
    Triple("C1", "The Art of Expression", "Искусство выражать мысли"),
).map { (level, name, title) -> GrammarStage(level, name, title, book.learningOrder.filter { it.level == level }) }
    .filter { it.topics.isNotEmpty() }

@Composable
internal fun GrammarAtlas(
    stages: List<GrammarStage>, active: String?, progress: Map<String, GrammarProgressEntity>,
    total: Int, mastered: Int, onSelect: (String) -> Unit,
) {
    val feedback = LocalFeedback.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stages.forEach { stage ->
                val done = stage.topics.count { progress[it.id]?.masteredAt != null }
                val selected = active == stage.level
                Box(Modifier.weight(1f).heightIn(min = 48.dp)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                    .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    .selectable(selected, role = Role.Tab, onClick = { feedback.selection(); onSelect(stage.level) })
                    .semantics { stateDescription = "Освоено $done из ${stage.topics.size} тем" }, contentAlignment = Alignment.Center) {
                    Text(stage.level, style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        active?.let { label -> stages.firstOrNull { it.level == label }?.let { stage ->
            Text("${stage.level} · освоено ${stage.topics.count { progress[it.id]?.masteredAt != null }} из ${stage.topics.size} · всего $mastered из $total",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        } }
    }
}

@Composable
internal fun GrammarNextCard(step: GrammarNextStep.Step, onClick: () -> Unit, onRead: () -> Unit) {
    Column(Modifier.padding(top = 8.dp, bottom = 2.dp)) {
        Text("Следующий шаг · ${step.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(3.dp))
            .border(.65.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Урок %03d · %s".format(step.topic.number, step.topic.level.orEmpty()),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(step.topic.title, style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 12.dp))
                Spacer(Modifier.height(6.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onClick) {
                        Text(step.action, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Icon(AlbionIcons.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    if (step.kind != GrammarNextStep.Kind.Read) {
                        TextButton(onClick = onRead) { Text("Читать урок", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GrammarViewTabs(selected: Int, due: Int, onSelect: (Int) -> Unit) {
    val feedback = LocalFeedback.current
    val labels = listOf("Маршрут", "Закладки", "Повторить · $due")
    if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) {
        var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        Box(Modifier.fillMaxWidth()) {
            TextButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("${labels[selected]} ▾", Modifier.weight(1f), textAlign = TextAlign.Start)
            }
            androidx.compose.material3.DropdownMenu(open, { open = false }) {
                labels.forEachIndexed { index, label -> androidx.compose.material3.DropdownMenuItem(text = { Text(label) },
                    modifier = Modifier.semantics { this.selected = selected == index }, onClick = { open = false; onSelect(index) }) }
            }
        }
        return
    }
    FlowRow(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEachIndexed { index, label ->
            Box(Modifier.widthIn(min = 80.dp).heightIn(min = 48.dp).selectable(selected == index, role = Role.Tab,
                onClick = { feedback.selection(); onSelect(index) }).padding(horizontal = 12.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
internal fun GrammarStageHeading(stage: GrammarStage, index: Int, done: Int, read: Int) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp)) {
        Text(stage.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text("Прочитано $read · освоено $done из ${stage.topics.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun GrammarRouteLesson(
    topic: Topic, progress: GrammarProgressEntity?, current: Boolean, selected: Boolean,
    first: Boolean, last: Boolean, chapter: String?, now: Long, onClick: () -> Unit,
) {
    val mastered = progress?.masteredAt != null
    val read = progress?.readAt != null
    val due = GrammarReview.plan(progress)?.dueAt?.let { it <= now } == true
    val status = when {
        due -> "К повторению"
        mastered -> "Освоено"
        current && read -> "Проверьте знания"
        current -> "Ваш следующий шаг"
        progress?.quizAt != null -> "Проверка: ${progress.quizCorrect} из ${progress.quizTotal}"
        read -> "Прочитано"
        else -> "Изучить тему"
    }
    val feedback = LocalFeedback.current
    val emphasis = current || selected
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(Modifier.fillMaxWidth().heightIn(min = 106.dp).drawBehind {
        val x = 22.dp.toPx(); val y = 32.dp.toPx()
        if (!first) drawLine(line, Offset(x, 0f), Offset(x, y), .75.dp.toPx())
        if (!last) drawLine(line, Offset(x, y), Offset(x, size.height), .75.dp.toPx(),
            pathEffect = if (mastered) null else PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
    }.clickable(role = Role.Button, onClick = { feedback.selection(); onClick() })
        .semantics(mergeDescendants = true) { stateDescription = status }) {
        Box(Modifier.padding(top = 10.dp).size(44.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp).background(if (emphasis) MaterialTheme.colorScheme.primary else if (mastered) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background, CircleShape)
                .border(if (emphasis) 1.dp else .65.dp, if (emphasis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, CircleShape), contentAlignment = Alignment.Center) {
                if (mastered) Icon(AlbionIcons.Check, null, tint = if (emphasis) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(17.dp))
                else Text(topic.number.toString(), style = MaterialTheme.typography.labelSmall,
                    color = if (emphasis) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f).padding(bottom = 12.dp)
            .background(if (emphasis) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(3.dp))
            .then(if (emphasis) Modifier.border(.65.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp)) else Modifier)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(status, style = MaterialTheme.typography.labelSmall,
                    color = if (emphasis || due) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (progress?.bookmarked == true) Icon(AlbionIcons.Bookmark, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(5.dp))
            Text(topic.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            if (chapter != null) {
                Spacer(Modifier.height(5.dp))
                Text(chapter, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!due) GrammarReview.plan(progress)?.let { plan ->
                Spacer(Modifier.height(4.dp))
                Text("Повторение: ${grammarReviewDate(plan.dueAt, now)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
