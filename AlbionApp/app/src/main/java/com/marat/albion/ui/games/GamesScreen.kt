package com.marat.albion.ui.games

import com.marat.albion.ui.theme.paperSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.settings.PracticeOptions
import com.marat.albion.ui.components.*
import com.marat.albion.ui.nav.GameRoute
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.theme.Ink

private val practiceGroups = listOf(
    "Словарный запас" to listOf(Game.TYPE_ANSWER, Game.IRREGULAR),
    "Грамматика" to listOf(Game.GRAMMAR_CHOICE, Game.GRAMMAR_GAP, Game.SENTENCE, Game.TENSES),
    "Речь и слух" to listOf(Game.PRONUNCIATION, Game.SHADOWING),
)

@Composable
fun GamesScreen(navigator: Navigator) {
    val settings = LocalAppContainer.current.settings
    val lastName by settings.lastPractice.collectAsStateWithLifecycle(null)
    val lastGame = Game.entries.firstOrNull { it.name == lastName }
    val featured = lastGame ?: Game.TYPE_ANSWER
    val defaultCount = if (featured == Game.IRREGULAR) 20 else 10
    val options by remember(featured) { settings.practiceOptions(featured.name, defaultCount) }
        .collectAsStateWithLifecycle(PracticeOptions(count = defaultCount))

    LazyColumn(
        Modifier.fillMaxSize().paperSurface(),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 32.dp),
    ) {
        item {
            ScreenHeader("Практика", eyebrow = "The Practice Room", compact = true, trailing = {
                Row {
                    FeatureHelp("practice", "Как заниматься",
                        "Выберите навык, затем колоду или уровень и длину занятия. Параметры каждого упражнения сохраняются.\n\nОшибки сохраняются: сложные слова и задания будут встречаться чаще.\n\nПрактика учитывается в дневной цели и статистике; расписание повторения карточек остаётся прежним.", showOnFirstUse = false)
                    SettingsShortcut(navigator::openSettings)
                }
            })
        }
        item {
            val scope = if (featured.isGrammar) options.level.ifEmpty { "Все уровни" }
                    else (featured.decks.firstOrNull { it.id == options.deckId } ?: featured.decks.first()).title
                val length = if (options.count == Int.MAX_VALUE) "вся колода" else "до ${options.count} заданий"
                val details = listOfNotNull(scope.takeUnless { it == featured.title }, length).joinToString(" · ")
                CatalogueCard(if (lastGame != null) "Продолжить практику" else "Первое занятие", featured.title,
                    if (lastGame != null) details else "Вспомните слово и запишите ответ",
                    onClick = { navigator.open(GameRoute(featured)) }, ribbon = true,
                    modifier = Modifier.padding(top = 14.dp, bottom = 12.dp))
        }
        practiceGroups.forEach { (title, games) ->
            item(key = title) { SectionHeader(title, Modifier.padding(top = 10.dp)) }
            items(games, key = { it.name }) { game ->
                PracticeModeCard(game, lastGame == game) { navigator.open(GameRoute(game)) }
            }
        }
    }
}
