package com.marat.albion.ui.stats

import com.marat.albion.ui.components.gutter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marat.albion.ui.components.HeroCard
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.EmptyState
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.DayCount
import com.marat.albion.data.repo.Stats
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.Panel
import com.marat.albion.ui.components.ScreenHeader
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.SettingsShortcut
import com.marat.albion.ui.games.Game
import com.marat.albion.ui.library.pluralRu
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.components.StatPill
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(navigator: Navigator) {
    val c = LocalAppContainer.current
    val stats by remember { c.study.observeStats() }.collectAsStateWithLifecycle(null)
    val adviceVm = viewModel { StudyAdviceViewModel(c) }
    val advice by adviceVm.advice.collectAsStateWithLifecycle()
    val reveal = rememberReveal()

    Column(
        Modifier.fillMaxSize().background(Ink.Bg).verticalScroll(rememberScrollState())
            .padding(horizontal = gutter).padding(bottom = 32.dp),
    ) {
        Box(Modifier.reveal(reveal, 0)) { ScreenHeader("Прогресс", eyebrow = "Карточки и практика", compact = true, trailing = { SettingsShortcut(navigator::openSettings) }) }
        val s = stats ?: return@Column

        advice?.let { next ->
            HeroCard(Modifier.fillMaxWidth().padding(top = 12.dp), contentPadding = PaddingValues(18.dp)) {
                Eyebrow("Следующее занятие")
                Spacer(Modifier.height(9.dp))
                Text(next.title, style = MaterialTheme.typography.headlineSmall, color = Ink.Fg)
                Spacer(Modifier.height(8.dp))
                Text(next.reason, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                Spacer(Modifier.height(14.dp))
                PrimaryButton(next.action, onClick = { navigator.open(next.destination) })
            }
        }

        // в день рекорда — стоит на карточке достижения.
        val milestone = streakMilestone(s.streak)

        Column(Modifier.padding(top = if (milestone == null) 28.dp else 16.dp).reveal(reveal, 1)
            ) {
            if (milestone != null) {
                Panel(Modifier.padding(bottom = 16.dp), color = Ink.GoldSoft) {
                    Eyebrow("Достижение", color = Ink.GoldText)
                    Spacer(Modifier.height(8.dp))
                    Text(milestone.first, style = MaterialTheme.typography.headlineSmall, color = Ink.Fg)
                    Spacer(Modifier.height(8.dp))
                    Text(milestone.second, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(s.streak, "дней подряд", Modifier.weight(1f))
                Tile(s.answersToday, "ответов сегодня", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Hairline()
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(s.retention?.let { (it * 100).toInt() }, "вспомнили при повторении · 30 дней", Modifier.weight(1f), suffix = "%")
                Tile((s.timeTodayMs / 60000).toInt(), "минут сегодня", Modifier.weight(1f))
            }
        }

        Column(Modifier.reveal(reveal, 2)) {
            SectionHeader("Активность · 30 дней", trailing = "Σ ${days(s.daily, s.today - 29, s.today).sum()}")
            BarChart(days(s.daily, s.today - 29, s.today), startDay = s.today - 29)
            Text("Ответы в карточках и упражнениях", style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
                modifier = Modifier.padding(top = 8.dp))
        }

        Column(Modifier.reveal(reveal, 3)) {
            GameBreakdown(s)
        }

        Column(Modifier.reveal(reveal, 4)) {
            SectionHeader("Прогноз", trailing = "14 дн")
            BarChart(days(s.forecast, s.today, s.today + 13), startDay = s.today, forecast = true)
        }

        Column(Modifier.reveal(reveal, 5)) {
            SectionHeader("Колоды")
            DeckBreakdown(s)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Всего ответов: ${s.totalReviews + s.totalGameAnswers} · карточки: ${s.totalReviews}, практика: ${s.totalGameAnswers}. " +
                "Удержание FSRS — доля повторений карточек, на которые вы ответили не «Снова».",
            style = MaterialTheme.typography.bodySmall,
            color = Ink.Faint,
        )
    }
}

@Composable
private fun GameBreakdown(s: Stats) {
    SectionHeader("Практика", trailing = "${s.totalGameAnswers} ${pluralRu(s.totalGameAnswers, "ответ", "ответа", "ответов")}")
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatPill(s.gameAnswersToday, "ответов сегодня", Modifier.weight(1f))
        StatPill(s.gameAccuracy?.let { (it * 100).toInt() }, "верных ответов", Modifier.weight(1f), suffix = "%", tint = Ink.AccentSoft)
    }
    Spacer(Modifier.height(18.dp))
    val totals = s.games.associateBy { it.game }
    Game.entries.forEach { game ->
        val result = totals[game.name]
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text(game.title, style = MaterialTheme.typography.titleSmall, color = Ink.Text)
            Spacer(Modifier.height(4.dp))
            Text(
                if (result == null || result.answers == 0) "Пока нет ответов"
                else "${result.correct} из ${result.answers} верно · ${(result.accuracy!! * 100).toInt()}% · ошибок: ${result.wrong}",
                style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
            )
        }
        Hairline()
    }
    Text(
        "Здесь учтены упражнения и проверки тем.",
        style = MaterialTheme.typography.bodySmall, color = Ink.Muted, modifier = Modifier.padding(top = 12.dp),
    )
}

private fun days(source: List<DayCount>, from: Long, to: Long): List<Int> {
    val map = source.associate { it.day to it.count }
    return (from..to).map { map[it] ?: 0 }
}

@Composable
private fun Tile(value: Int?, label: String, modifier: Modifier, suffix: String = "") {
    Column(modifier) {
        if (value == null) Text("—", style = AlbionType.numberSmall, color = Ink.Faint)
        else AnimatedNumber(value, AlbionType.numberSmall, color = if (value == 0) Ink.Faint else Ink.Fg, suffix = suffix)
        Spacer(Modifier.height(2.dp))
        Text(label.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
            modifier = Modifier.padding(end = 12.dp))
    }
}

/** Один график — одна область касания: узкие столбики не перекрывают соседние touch targets. */
@Composable
private fun BarChart(values: List<Int>, startDay: Long, forecast: Boolean = false) {
    if (values.none { it > 0 }) {
        Panel(contentPadding = PaddingValues(16.dp)) {
            Text(if (forecast) "Пока нет запланированных повторений" else "История появится после первого занятия",
                style = MaterialTheme.typography.titleSmall, color = Ink.Text)
            Spacer(Modifier.height(6.dp))
            Text(if (forecast) "Начните изучать карточки — здесь появится расписание."
                else "Здесь будут видны ответы по дням за последние 30 дней.",
                style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        }
        return
    }
    val max = values.maxOrNull()!!.coerceAtLeast(1)
    var selectedIndex by rememberSaveable(startDay) { mutableIntStateOf(if (forecast) 0 else values.lastIndex) }
    val selected = selectedIndex.coerceIn(values.indices)
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru")) }
    val date = LocalDate.ofEpochDay(startDay + selected).format(formatter)
    val selectionLabel = "$date · ${values[selected]} ${if (forecast) "к повторению" else pluralRu(values[selected], "ответ", "ответа", "ответов") }"
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        grow.animateTo(1f, tween(300, easing = Easing.Emphasized))
    }
    Text(selectionLabel, style = MaterialTheme.typography.titleSmall, color = Ink.Fg)
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth().height(104.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (forecast) "График предстоящих повторений" else "График активности"
                stateDescription = selectionLabel
                customActions = listOf(
                    CustomAccessibilityAction("Предыдущий день") { selectedIndex = (selected - 1).coerceAtLeast(0); true },
                    CustomAccessibilityAction("Следующий день") { selectedIndex = (selected + 1).coerceAtMost(values.lastIndex); true },
                )
            }
            .pointerInput(values) {
                detectTapGestures { offset ->
                    selectedIndex = (offset.x / size.width * values.size).toInt().coerceIn(values.indices)
                }
            },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            Box(Modifier.weight(1f).fillMaxHeight().background(if (i == selected) Ink.Surface else Color.Transparent, RoundedCornerShape(4.dp)), contentAlignment = Alignment.BottomCenter) {
                val h = if (v == 0) 0.015f else (v.toFloat() / max).coerceAtLeast(0.04f)
                val color = when {
                    v == 0 -> Ink.Line
                    forecast && i == selected -> Ink.Warm
                    forecast -> Ink.Gold.copy(alpha = 0.55f)
                    i == selected -> Ink.Accent
                    else -> Ink.Accent.copy(alpha = 0.55f)
                }
                Box(Modifier.fillMaxWidth().fillMaxHeight(h * grow.value).background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 1.dp, bottomEnd = 1.dp)))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row {
        Text(if (forecast) "сегодня" else "29 дней назад", style = MaterialTheme.typography.labelSmall, color = Ink.Faint, modifier = Modifier.weight(1f))
        Text("макс. $max", style = MaterialTheme.typography.labelSmall, color = Ink.Faint, modifier = Modifier.weight(1f))
        Text(if (forecast) "+13 дней" else "сегодня", style = MaterialTheme.typography.labelSmall, color = Ink.Faint)
    }
    Text("Коснитесь столбика, чтобы выбрать день", style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
        modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun DeckBreakdown(s: Stats) {
    val byId = s.counts.associateBy { it.deck }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Deck.entries.forEach { deck ->
            val c = byId[deck.id] ?: return@forEach
            val young = (c.started - c.learning - c.mature).coerceAtLeast(0)
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(deck.title, style = MaterialTheme.typography.titleSmall, color = Ink.Text, modifier = Modifier.weight(1f))
                    Text("${c.started} / ${c.total}", style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Ink.High)) {
                    val total = c.total.coerceAtLeast(1).toFloat()
                    if (c.learning > 0) Box(Modifier.weight(c.learning / total).fillMaxHeight().background(Ink.Gold))
                    if (young > 0) Box(Modifier.weight(young / total).fillMaxHeight().background(Ink.Accent.copy(alpha = 0.5f)))
                    if (c.mature > 0) Box(Modifier.weight(c.mature / total).fillMaxHeight().background(Ink.Accent))
                    val rest = (c.total - c.started) / total
                    if (rest > 0f) Spacer(Modifier.weight(rest))
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Legend(Ink.Gold, "Учу ${c.learning}")
                    Legend(Ink.Accent.copy(alpha = 0.5f), "Повтор $young")
                    Legend(Ink.Accent, "Выучено ${c.mature}")
                    Legend(Ink.High, "Новые ${(c.total - c.started).coerceAtLeast(0)}")
                }
            }
        }
    }
    if (s.counts.isEmpty()) Text("Материалы ещё не загружены.", style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(4.dp).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
    }
}

/** Подпись к серии дней: с недели без пропусков. */
private fun streakMilestone(streak: Int): Pair<String, String>? = when {
    streak >= 100 -> "Сто дней подряд" to "Английский стал привычкой. Это лучшее, что можно для него сделать."
    streak >= 30 -> "Месяц без пропусков" to "Интервалы FSRS растут, а повторения занимают всё меньше времени."
    streak >= 7 -> "Неделя без пропусков" to "Короткие ежедневные занятия запоминаются лучше редких длинных."
    else -> null
}
