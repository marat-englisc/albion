package com.marat.albion.ui.deck

import com.marat.albion.ui.components.peekingFinn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.repo.DeckSummary
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.components.TopBar
import com.marat.albion.ui.nav.BrowseRoute
import com.marat.albion.ui.nav.LibraryRoute
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.StudyRoute
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun DeckScreen(deck: Deck, navigator: Navigator) {
    val c = LocalAppContainer.current
    val summaryFlow = remember(deck) { c.study.observeSummaries().map { list -> list.first { it.deck == deck } } }
    val summary by summaryFlow.collectAsStateWithLifecycle(DeckSummary(deck))
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val reveal = rememberReveal()

    Column(Modifier.fillMaxSize().background(Ink.Bg)) {
        TopBar(onBack = navigator::back) {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreHoriz, "Ещё", tint = Ink.Fg) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Ink.Elevated) {
                    DropdownMenuItem(text = { Text("Сбросить прогресс колоды") }, onClick = { menu = false; confirmReset = true })
                }
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 24.dp),
        ) deckContent@{
            Column(Modifier.reveal(reveal, 0)) {
                Eyebrow("Колода · %02d".format(deck.ordinal + 1))
                Spacer(Modifier.height(12.dp))
                Text(deck.title, style = MaterialTheme.typography.displayMedium, color = Ink.Fg)
                Spacer(Modifier.height(8.dp))
                Text(deck.subtitle, style = MaterialTheme.typography.bodyLarge, color = Ink.Muted)
            }

            if (deck == Deck.BOOK_WORDS && summary.total == 0) {
                Spacer(Modifier.height(24.dp))
                Text("Добавьте первое слово", style = MaterialTheme.typography.headlineSmall, color = Ink.Fg)
                Spacer(Modifier.height(10.dp))
                Text("Откройте книгу, коснитесь слова и нажмите «Добавить в “Слова из книг”». " +
                    "Перевод и пример можно поправить перед сохранением.",
                    style = MaterialTheme.typography.bodyLarge, color = Ink.Muted)
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Открыть библиотеку", onClick = { navigator.switchTab(LibraryRoute) },
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
                return@deckContent
            }

            Row(Modifier.padding(top = 36.dp).reveal(reveal, 1)) {
                Metric(summary.due, "к повторению", Modifier.weight(1f))
                Metric(summary.newAvailable, "новых сегодня", Modifier.weight(1f))
            }
            // Над кнопкой выглядывает Финн — оставляем ему место, чтобы не заслонить подписи.
            Spacer(Modifier.height(if (summary.toStudy > 0) 52.dp else 28.dp))
            PrimaryButton(
                text = if (summary.toStudy > 0) "Начать занятие" else "На сегодня всё",
                onClick = { navigator.open(StudyRoute(deck)) },
                enabled = summary.toStudy > 0,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                // Финн машет из-за кнопки — занятие ждёт.
                modifier = Modifier.reveal(reveal, 2).then(
                    if (summary.toStudy > 0) Modifier.peekingFinn(at = 0.86f, height = 54.dp) else Modifier
                ),
            )
            Spacer(Modifier.height(12.dp))
            GhostButton(
                "Все карточки и поиск",
                onClick = { navigator.open(BrowseRoute(deck)) },
                icon = Icons.Rounded.Search,
                modifier = Modifier.reveal(reveal, 3),
            )

            Column(Modifier.reveal(reveal, 4)) {
                SectionHeader("Прогресс", Modifier.padding(top = 28.dp), trailing = "${(summary.progress * 100).toInt()}%")
                Spacer(Modifier.height(8.dp))
                ThinProgress(summary.progress)
                Spacer(Modifier.height(24.dp))
                Row {
                    Metric(summary.total, "всего", Modifier.weight(1f), small = true)
                    Metric(summary.total - summary.started, "не начато", Modifier.weight(1f), small = true)
                }
                Spacer(Modifier.height(20.dp))
                Hairline()
                Spacer(Modifier.height(20.dp))
                Row {
                    Metric(summary.learning, "изучаются", Modifier.weight(1f), small = true)
                    Metric(summary.mature, "выучено · 21+ дн", Modifier.weight(1f), small = true)
                }
            }

            Column(Modifier.reveal(reveal, 5)) {
                SectionHeader("Как это работает", Modifier.padding(top = 28.dp))
                Text(
                    "Посмотрите на лицевую сторону и вспомните значение. Переверните карточку и честно оцените ответ — " +
                        "FSRS рассчитает момент, когда вы начнёте забывать, и покажет карточку именно тогда.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.Muted,
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Ink.Elevated,
            title = { Text("Сбросить прогресс?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Все карточки колоды «${deck.title}» снова станут новыми, статистика занятий сохранится.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch { c.study.resetDeck(deck) }
                }) { Text("Сбросить", color = Ink.Fg) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена", color = Ink.Muted) } },
        )
    }
}

@Composable
private fun Metric(value: Int, label: String, modifier: Modifier = Modifier, small: Boolean = false) {
    Column(modifier) {
        AnimatedNumber(value, if (small) AlbionType.numberSmall else AlbionType.number, color = if (value == 0) Ink.Faint else Ink.Fg)
        Spacer(Modifier.height(2.dp))
        Eyebrow(label)
    }
}
