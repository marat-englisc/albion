package com.marat.albion.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.DayCount
import com.marat.albion.data.repo.Stats
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.ScreenHeader
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.SettingsShortcut
import com.marat.albion.ui.games.Game
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal
import kotlinx.coroutines.delay

@Composable
fun StatsScreen(navigator: Navigator) {
    val c = LocalAppContainer.current
    val stats by remember { c.study.observeStats() }.collectAsStateWithLifecycle(null)
    val reveal = rememberReveal()

    Column(
        Modifier.fillMaxSize().background(Ink.Bg).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 32.dp),
    ) {
        Box(Modifier.reveal(reveal, 0)) { ScreenHeader("Прогресс", eyebrow = "Карточки и игры", trailing = { SettingsShortcut(navigator::openSettings) }) }
        val s = stats ?: return@Column

        Column(Modifier.padding(top = 28.dp).reveal(reveal, 1)) {
            Row {
                Tile(s.streak, "дней подряд", Modifier.weight(1f))
                Tile(s.answersToday, "ответов сегодня", Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Hairline()
            Spacer(Modifier.height(20.dp))
            Row {
                Tile(s.retention?.let { (it * 100).toInt() }, "удержание FSRS · 30 дн", Modifier.weight(1f), suffix = "%")
                Tile((s.timeTodayMs / 60000).toInt(), "минут сегодня", Modifier.weight(1f))
            }
        }

        Column(Modifier.reveal(reveal, 2)) {
            SectionHeader("Активность · 30 дней", trailing = "Σ ${days(s.daily, s.today - 29, s.today).sum()}")
            BarChart(days(s.daily, s.today - 29, s.today), labels = "30 дн назад" to "сегодня")
            Text("Ответы в карточках и играх", style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
                modifier = Modifier.padding(top = 8.dp))
        }

        Column(Modifier.reveal(reveal, 3)) {
            GameBreakdown(s)
        }

        Column(Modifier.reveal(reveal, 4)) {
            SectionHeader("Прогноз", trailing = "14 дн")
            BarChart(days(s.forecast, s.today, s.today + 13), labels = "сегодня" to "+13 дн", hollow = true)
        }

        Column(Modifier.reveal(reveal, 5)) {
            SectionHeader("Колоды")
            DeckBreakdown(s)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Всего ответов: ${s.totalReviews + s.totalGameAnswers} · карточки: ${s.totalReviews}, игры: ${s.totalGameAnswers}. " +
                "Удержание FSRS — доля повторений карточек, на которые вы ответили не «Снова».",
            style = MaterialTheme.typography.bodySmall,
            color = Ink.Faint,
        )
    }
}

@Composable
private fun GameBreakdown(s: Stats) {
    SectionHeader("Игры", trailing = "${s.totalGameAnswers} ответов")
    Spacer(Modifier.height(16.dp))
    Row {
        Tile(s.gameAnswersToday, "ответов сегодня", Modifier.weight(1f))
        Tile(s.gameAccuracy?.let { (it * 100).toInt() }, "верных ответов", Modifier.weight(1f), suffix = "%")
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
        "Суммарные результаты включают прежние игры и проверки тем. " +
            "История игр по дням и время занятий записываются с этого обновления. Расписание FSRS игры не меняют.",
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
        if (value == null) Text("—", style = AlbionType.number, color = Ink.Faint)
        else AnimatedNumber(value, AlbionType.number, color = if (value == 0) Ink.Faint else Ink.Fg, suffix = suffix)
        Spacer(Modifier.height(2.dp))
        Eyebrow(label)
    }
}

/** Столбики вырастают каскадом слева направо. */
@Composable
private fun BarChart(values: List<Int>, labels: Pair<String, String>, hollow: Boolean = false) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(150)
        grow.animateTo(1f, tween(1100, easing = Easing.Emphasized))
    }
    Row(
        Modifier.fillMaxWidth().height(132.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            val local = ((grow.value * (values.size + 6) - i) / 6f).coerceIn(0f, 1f)
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                val h = if (v == 0) 0.015f else (v.toFloat() / max).coerceAtLeast(0.04f)
                val color = when {
                    v == 0 -> Ink.Line
                    hollow -> Ink.Muted
                    i == values.lastIndex -> Ink.Fg
                    else -> Ink.Soft
                }
                Box(Modifier.fillMaxWidth().fillMaxHeight(h * local).background(color))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row {
        Text(labels.first, style = MaterialTheme.typography.labelSmall, color = Ink.Faint, modifier = Modifier.weight(1f))
        Text("max $max", style = MaterialTheme.typography.labelSmall, color = Ink.Faint, modifier = Modifier.weight(1f))
        Text(labels.second, style = MaterialTheme.typography.labelSmall, color = Ink.Faint)
    }
}

@Composable
private fun DeckBreakdown(s: Stats) {
    val byId = s.counts.associateBy { it.deck }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Legend(Ink.Muted, "учу")
        Legend(Ink.Soft, "повтор")
        Legend(Ink.Fg, "выучено")
        Legend(Ink.Line, "новые")
    }
    Spacer(Modifier.height(18.dp))
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
                Row(Modifier.fillMaxWidth().height(4.dp).background(Ink.Line)) {
                    val total = c.total.coerceAtLeast(1).toFloat()
                    if (c.learning > 0) Box(Modifier.weight(c.learning / total).fillMaxHeight().background(Ink.Muted))
                    if (young > 0) Box(Modifier.weight(young / total).fillMaxHeight().background(Ink.Soft))
                    if (c.mature > 0) Box(Modifier.weight(c.mature / total).fillMaxHeight().background(Ink.Fg))
                    val rest = (c.total - c.started) / total
                    if (rest > 0f) Spacer(Modifier.weight(rest))
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
