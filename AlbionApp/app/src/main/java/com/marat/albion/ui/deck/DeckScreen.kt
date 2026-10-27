package com.marat.albion.ui.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import com.marat.albion.ui.components.StatPill
import com.marat.albion.ui.components.gutter
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
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.components.TopBar
import com.marat.albion.ui.nav.BrowseRoute
import com.marat.albion.ui.nav.LibraryRoute
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.StudyRoute
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
                com.marat.albion.ui.components.RoundIconButton(Icons.Rounded.MoreHoriz, "Ещё", { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Ink.Raised) {
                    DropdownMenuItem(text = { Text("Сбросить прогресс колоды") }, onClick = { menu = false; confirmReset = true })
                }
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = gutter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 24.dp),
        ) deckContent@{
            Column(Modifier.reveal(reveal, 0)) {
                Eyebrow("Колода · %02d".format(deck.ordinal + 1))
                Spacer(Modifier.height(12.dp))
                Text(deck.title, style = if (com.marat.albion.ui.components.LocalNarrow.current) MaterialTheme.typography.displaySmall
                    else MaterialTheme.typography.displayMedium, color = Ink.Fg)
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

            // Те же «камешки» с цифрами, что на главной и в «Прогрессе».
            Row(Modifier.padding(top = 28.dp).height(IntrinsicSize.Min).reveal(reveal, 1), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(summary.due, "к повторению", Modifier.weight(1f).fillMaxHeight(), tint = Ink.WarmSoft,
                    valueColor = if (summary.due == 0) Ink.Faint else Ink.Warm)
                StatPill(summary.newAvailable, "новых сегодня", Modifier.weight(1f).fillMaxHeight(), tint = Ink.AccentSoft)
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = if (summary.toStudy > 0) "Начать занятие" else "На сегодня всё",
                onClick = { navigator.open(StudyRoute(deck)) },
                enabled = summary.toStudy > 0,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,

                modifier = Modifier.reveal(reveal, 2),
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
                Spacer(Modifier.height(16.dp))
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(summary.total, "всего", Modifier.weight(1f).fillMaxHeight())
                    StatPill(summary.total - summary.started, "не начато", Modifier.weight(1f).fillMaxHeight())
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(summary.learning, "изучаются", Modifier.weight(1f).fillMaxHeight())
                    StatPill(summary.mature, "выучено · 21+ дн", Modifier.weight(1f).fillMaxHeight())
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
            containerColor = Ink.Raised,
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
