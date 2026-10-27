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
    Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Ваш маршрут", Modifier.weight(1f), color = Ink.GoldText)
            Spacer(Modifier.width(12.dp))
            Text("$mastered / $total", style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().drawBehind {
            val half = size.width / stages.size.coerceAtLeast(1) / 2
            drawLine(Ink.Gold.copy(alpha = .7f), Offset(half, 26.dp.toPx()),
                Offset(size.width - half, 26.dp.toPx()), .65.dp.toPx())
        }) {
            stages.forEachIndexed { index, stage ->
                val done = stage.topics.count { progress[it.id]?.masteredAt != null }
                val selected = active == stage.level
                Column(Modifier.weight(1f).selectable(selected, role = Role.Tab, onClick = {
                    feedback.selection(); onSelect(stage.level)
                }).semantics { stateDescription = "Освоено $done из ${stage.topics.size} тем" }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(44.dp).background(Ink.Bg, CircleShape).drawBehind {
                        drawCircle(if (selected) Ink.Accent else Ink.Raised, size.minDimension / 2 - 3.dp.toPx())
                        drawCircle(if (selected) Ink.Accent else Ink.LineStrong, size.minDimension / 2 - 1.dp.toPx(),
                            style = Stroke(.65.dp.toPx()))
                        if (done > 0) drawArc(Ink.Gold, -90f, 360f * done / stage.topics.size, false,
                            topLeft = Offset(1.dp.toPx(), 1.dp.toPx()), size = size.copy(width = size.width - 2.dp.toPx(), height = size.height - 2.dp.toPx()),
                            style = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round))
                    }, contentAlignment = Alignment.Center) {
                        Text(stage.level, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                            color = if (selected) Ink.OnAccent else Ink.Fg, maxLines = 1, softWrap = false)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(romanNumeral(index + 1), style = AlbionType.index.copy(fontSize = 13.sp),
                        color = if (selected) Ink.Accent else Ink.Muted, modifier = Modifier.clearAndSetSemantics {})
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ThinProgress(if (total == 0) 0f else mastered.toFloat() / total, color = Ink.GoldText)
    }
}

@Composable
internal fun GrammarNextCard(step: GrammarNextStep.Step, onClick: () -> Unit, onRead: () -> Unit) {
    Column(Modifier.padding(top = 18.dp, bottom = 2.dp)) {
        CatalogueTab("Следующий шаг · ${step.label}", Modifier.padding(start = 7.dp))
        Box(Modifier.fillMaxWidth().background(Ink.Raised, RoundedCornerShape(3.dp))
            .border(.65.dp, Ink.Line, RoundedCornerShape(3.dp))) {
            RibbonBookmark(Modifier.align(Alignment.TopEnd).padding(end = 16.dp), width = 11.dp, height = 25.dp)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Урок %03d · %s".format(step.topic.number, step.topic.level.orEmpty()),
                    style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
                Spacer(Modifier.height(8.dp))
                Text(step.topic.title, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = headingFamily(step.topic.title), fontSize = 23.sp, lineHeight = 27.sp),
                    color = Ink.Fg, modifier = Modifier.padding(end = 12.dp))
                Spacer(Modifier.height(6.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onClick) {
                        Text(step.action, color = Ink.AccentText)
                        Spacer(Modifier.width(10.dp))
                        Icon(AlbionIcons.ArrowForward, null, tint = Ink.AccentText, modifier = Modifier.size(18.dp))
                    }
                    if (step.kind != GrammarNextStep.Kind.Read) {
                        TextButton(onClick = onRead) { Text("Читать урок", color = Ink.Soft) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GrammarViewTabs(selected: Int, due: Int, onSelect: (Int) -> Unit) {
    val feedback = LocalFeedback.current
    FlowRow(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Маршрут", "Закладки", "Повторить · $due").forEachIndexed { index, label ->
            Box(Modifier.widthIn(min = 80.dp).heightIn(min = 48.dp).selectable(selected == index, role = Role.Tab,
                onClick = { feedback.selection(); onSelect(index) }).drawBehind {
                    if (selected == index) drawLine(Ink.Accent, Offset(10.dp.toPx(), size.height - 1.dp.toPx()),
                        Offset(size.width - 10.dp.toPx(), size.height - 1.dp.toPx()), 1.5.dp.toPx())
                }.padding(horizontal = 12.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = if (selected == index) Ink.AccentText else Ink.Muted, textAlign = TextAlign.Center,
                    maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
internal fun GrammarStageHeading(stage: GrammarStage, index: Int, done: Int, read: Int) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${romanNumeral(index + 1)} / ${stage.level}", style = AlbionType.index, color = Ink.GoldText)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f).height(.5.dp).background(Ink.Gold.copy(alpha = .65f)))
        }
        Spacer(Modifier.height(12.dp))
        Text(stage.name, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Brand, fontSize = 30.sp, lineHeight = 35.sp), color = Ink.Fg)
        Text(stage.title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = Cyrillic, fontSize = 22.sp, lineHeight = 27.sp), color = Ink.Soft)
        Spacer(Modifier.height(9.dp))
        Text("Освоено $done из ${stage.topics.size} · прочитано $read", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        Spacer(Modifier.height(12.dp))
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
    Row(Modifier.fillMaxWidth().heightIn(min = 106.dp).drawBehind {
        val x = 22.dp.toPx(); val y = 32.dp.toPx()
        if (!first) drawLine(Ink.Gold.copy(alpha = .7f), Offset(x, 0f), Offset(x, y), .75.dp.toPx())
        if (!last) drawLine(Ink.Gold.copy(alpha = .7f), Offset(x, y), Offset(x, size.height), .75.dp.toPx(),
            pathEffect = if (mastered) null else PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
    }.clickable(role = Role.Button, onClick = { feedback.selection(); onClick() })
        .semantics(mergeDescendants = true) { stateDescription = status }) {
        Box(Modifier.padding(top = 10.dp).size(44.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp).background(if (emphasis) Ink.Accent else if (mastered) Ink.GoldSoft else Ink.Bg, CircleShape)
                .border(if (emphasis) 1.dp else .65.dp, if (emphasis) Ink.Accent else Ink.Gold, CircleShape), contentAlignment = Alignment.Center) {
                if (mastered) Icon(AlbionIcons.Check, null, tint = if (emphasis) Ink.OnAccent else Ink.GoldText, modifier = Modifier.size(17.dp))
                else Text(topic.number.toString(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (emphasis) Ink.OnAccent else Ink.Muted, maxLines = 1, softWrap = false)
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f).padding(bottom = 12.dp)
            .background(if (emphasis) Ink.Raised else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(3.dp))
            .then(if (emphasis) Modifier.border(.65.dp, if (selected) Ink.Accent else Ink.Gold, RoundedCornerShape(3.dp)) else Modifier)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(status, style = MaterialTheme.typography.labelSmall,
                    color = if (emphasis || due) Ink.AccentText else Ink.Muted, modifier = Modifier.weight(1f))
                if (progress?.bookmarked == true) Icon(AlbionIcons.Bookmark, null, tint = Ink.Accent, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(5.dp))
            Text(topic.title, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = headingFamily(topic.title), fontSize = 24.sp, lineHeight = 28.sp), color = Ink.Fg)
            if (chapter != null) {
                Spacer(Modifier.height(5.dp))
                Text(chapter, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Ink.Muted)
            }
            if (!due) GrammarReview.plan(progress)?.let { plan ->
                Spacer(Modifier.height(4.dp))
                Text("Повторение: ${grammarReviewDate(plan.dueAt, now)}", style = MaterialTheme.typography.bodySmall, color = Ink.Soft)
            }
        }
    }
}
