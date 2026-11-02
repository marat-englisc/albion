package com.marat.albion.ui.grammar

import androidx.compose.ui.draw.clip
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
                val fill by androidx.compose.animation.animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh, label = "stage")
                Column(Modifier.weight(1f).heightIn(min = 64.dp)
                    .background(fill, MaterialTheme.shapes.small)
                    .clip(MaterialTheme.shapes.small)
                    .selectable(selected, role = Role.Tab, onClick = { feedback.selection(); onSelect(stage.level) })
                    .semantics { stateDescription = "Освоено $done из ${stage.topics.size} тем" }
                    .padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(stage.level, style = MaterialTheme.typography.titleMedium, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    Text("$done / ${stage.topics.size}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = AlbionFonts.Tech),
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        // Освоение по уровню уже на плитках, по этапу — в заголовке маршрута; отдельная строка повторяла их.
    }
}

/** Следующий шаг по учебнику — инверсный акцент экрана: урок крупно и одно главное действие. */
@Composable
internal fun GrammarNextCard(step: GrammarNextStep.Step, onClick: () -> Unit, onRead: () -> Unit) {
    com.marat.albion.ui.components.BoundVolume(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text("Следующий шаг · ${step.label}", style = MaterialTheme.typography.labelMedium,
            color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.7f))
        Text("Урок %d · %s".format(step.topic.number, step.topic.level.orEmpty()), style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.7f))
        Text(step.topic.title, style = MaterialTheme.typography.headlineSmall)
        com.marat.albion.ui.components.PrimaryButton(step.action, onClick, trailingIcon = AlbionIcons.ArrowForward, inverse = true)
        if (step.kind != GrammarNextStep.Kind.Read) {
            val onInk = MaterialTheme.colorScheme.inverseOnSurface
            TextButton(onClick = onRead, modifier = Modifier.fillMaxWidth()) { Text("Читать урок", color = onInk) }
        }
    }
}

@Composable
internal fun GrammarViewPicker(selected: Int, due: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Маршрут", "Закладки", "Повторить · $due")
    OptionPicker("Раздел учебника", labels, selected, onSelect, Modifier.padding(top = 8.dp))
}

@Composable
internal fun GrammarStageHeading(stage: GrammarStage, index: Int, done: Int, read: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stage.level, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(stage.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("Прочитано $read · освоено $done из ${stage.topics.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
    Row(Modifier.fillMaxWidth().heightIn(min = 92.dp).drawBehind {
        val x = 22.dp.toPx(); val y = 32.dp.toPx()
        if (!first) drawLine(line, Offset(x, 0f), Offset(x, y), .75.dp.toPx())
        if (!last) drawLine(line, Offset(x, y), Offset(x, size.height), .75.dp.toPx(),
            pathEffect = if (mastered) null else PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
    }.clickable(role = Role.Button, onClick = { feedback.selection(); onClick() })
        .semantics(mergeDescendants = true) { stateDescription = status }) {
        Box(Modifier.padding(top = 10.dp).size(44.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp).background(if (emphasis) MaterialTheme.colorScheme.primary else if (mastered) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
                .border(1.dp, if (emphasis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                if (mastered) Icon(AlbionIcons.Check, null, tint = if (emphasis) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(17.dp))
                else Text(topic.number.toString(), style = MaterialTheme.typography.labelSmall,
                    color = if (emphasis) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f).padding(bottom = 12.dp)
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else if (emphasis) MaterialTheme.colorScheme.surfaceContainerLow else androidx.compose.ui.graphics.Color.Transparent, MaterialTheme.shapes.medium)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(status, style = MaterialTheme.typography.labelSmall,
                    color = if (emphasis || due) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (progress?.bookmarked == true) Icon(AlbionIcons.Bookmark, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(5.dp))
            Text(topic.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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
