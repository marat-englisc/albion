package com.marat.albion.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.marat.albion.data.content.Deck
import com.marat.albion.ui.browse.BrowseScreen
import com.marat.albion.ui.browse.CardDetailScreen
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.lerpColor
import com.marat.albion.ui.deck.DeckScreen
import com.marat.albion.ui.games.Game
import com.marat.albion.ui.games.GameHost
import com.marat.albion.ui.games.TopicQuizHost
import com.marat.albion.ui.games.GamesScreen
import com.marat.albion.ui.grammar.GrammarScreen
import com.marat.albion.ui.grammar.TopicScreen
import com.marat.albion.ui.home.HomeScreen
import com.marat.albion.ui.library.LibraryScreen
import com.marat.albion.ui.library.ReaderScreen
import com.marat.albion.ui.settings.SettingsScreen
import com.marat.albion.ui.stats.StatsScreen
import com.marat.albion.ui.study.StudyScreen
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import kotlinx.serialization.Serializable

// ---- ключи навигации ----

sealed interface TopLevel : NavKey

@Serializable data object HomeRoute : TopLevel
@Serializable data object GrammarRoute : TopLevel
@Serializable data object LibraryRoute : TopLevel
@Serializable data object GamesRoute : TopLevel
@Serializable data object StatsRoute : TopLevel
@Serializable data object SettingsRoute : TopLevel

@Serializable data class DeckRoute(val deck: Deck) : NavKey
@Serializable data class StudyRoute(val deck: Deck) : NavKey
@Serializable data class BrowseRoute(val deck: Deck) : NavKey
@Serializable data class CardRoute(val cardId: String) : NavKey
@Serializable data class TopicRoute(val topicId: String) : NavKey
@Serializable data class TopicQuizRoute(val topic: Int, val topicId: String, val title: String) : NavKey
@Serializable data class GameRoute(val game: Game) : NavKey
@Serializable data class ReaderRoute(val bookId: String) : NavKey

private data class Tab(val route: TopLevel, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(HomeRoute, "Карточки", Icons.Outlined.Layers),
    Tab(GrammarRoute, "Учебник", Icons.AutoMirrored.Outlined.MenuBook),
    Tab(LibraryRoute, "Библиотека", Icons.Outlined.AutoStories),
    Tab(GamesRoute, "Игры", Icons.Outlined.SportsEsports),
    Tab(StatsRoute, "Прогресс", Icons.Outlined.Insights),
    Tab(SettingsRoute, "Настройки", Icons.Outlined.Tune),
)

/** Действия навигации, которые получают экраны. */
class Navigator(private val backStack: NavBackStack<NavKey>) {
    fun open(route: NavKey) {
        backStack.add(route)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /** Заменить текущий экран (например, соседняя тема учебника). */
    fun replace(route: NavKey) {
        if (backStack.isNotEmpty()) backStack[backStack.lastIndex] = route else backStack.add(route)
    }

    /** Переключение вкладки: «Карточки» всегда остаются корнем стека. */
    fun switchTab(tab: TopLevel) {
        backStack.clear()
        backStack.add(HomeRoute)
        if (tab != HomeRoute) backStack.add(tab)
    }
}

// Переход вглубь: новый экран въезжает справа и «приближается», старый уходит в глубину.
private fun forward(): ContentTransform =
    (slideInHorizontally(tween(480, easing = Easing.Emphasized)) { (it * 0.18f).toInt() } +
        fadeIn(tween(320, delayMillis = 60)) +
        scaleIn(tween(480, easing = Easing.Emphasized), initialScale = 0.96f)) togetherWith
        (fadeOut(tween(220)) + scaleOut(tween(480, easing = Easing.Emphasized), targetScale = 0.94f))

private fun backward(): ContentTransform =
    (fadeIn(tween(320, delayMillis = 80)) + scaleIn(tween(480, easing = Easing.Emphasized), initialScale = 0.94f)) togetherWith
        (slideOutHorizontally(tween(420, easing = Easing.Emphasized)) { (it * 0.2f).toInt() } +
            fadeOut(tween(260)) +
            scaleOut(tween(420, easing = Easing.Emphasized), targetScale = 0.97f))

@Composable
fun AlbionNavigation() {
    val backStack = rememberNavBackStack(HomeRoute)
    val navigator = remember(backStack) { Navigator(backStack) }
    val current = backStack.lastOrNull()
    val showBar = current is TopLevel

    Scaffold(
        containerColor = Ink.Bg,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = fadeIn(tween(300)) + slideInVertically(tween(420, easing = Easing.Emphasized)) { it },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(260)) { it },
            ) {
                BottomBar(current as? TopLevel ?: HomeRoute) { navigator.switchTab(it) }
            }
        },
    ) { padding ->
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        Box(Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                onBack = { navigator.back() },
                modifier = Modifier.padding(bottom).consumeWindowInsets(bottom),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = { forward() },
                popTransitionSpec = { backward() },
                predictivePopTransitionSpec = { backward() },
                entryProvider = entryProvider {
                    entry<HomeRoute> { HomeScreen(navigator) }
                    entry<GrammarRoute> { GrammarScreen(navigator) }
                    entry<GamesRoute> { GamesScreen(navigator) }
                    entry<GameRoute> { GameHost(it.game, navigator) }
                    entry<LibraryRoute> { LibraryScreen(navigator) }
                    entry<ReaderRoute> { ReaderScreen(it.bookId, navigator) }
                    entry<StatsRoute> { StatsScreen() }
                    entry<SettingsRoute> { SettingsScreen() }
                    entry<DeckRoute> { DeckScreen(it.deck, navigator) }
                    entry<StudyRoute> { StudyScreen(it.deck, navigator) }
                    entry<BrowseRoute> { BrowseScreen(it.deck, navigator) }
                    entry<CardRoute> { CardDetailScreen(it.cardId, navigator) }
                    entry<TopicRoute> { TopicScreen(it.topicId, navigator) }
                    entry<TopicQuizRoute> { TopicQuizHost(it.topic, it.topicId, it.title, navigator) }
                },
            )
            // Прокручиваемый контент не должен просвечивать под статус-баром.
            Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(Ink.Bg.copy(alpha = 0.94f)))
        }
    }
}

/** Нижняя панель: тонкая белая черта пружинисто скользит к выбранной вкладке. */
@Composable
private fun BottomBar(selected: TopLevel, onSelect: (TopLevel) -> Unit) {
    val index = tabs.indexOfFirst { it.route == selected }.coerceAtLeast(0)
    Column(Modifier.background(Ink.Bg).windowInsetsPadding(WindowInsets.navigationBars)) {
        Hairline()
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / tabs.size
            val indicatorX by animateDpAsState(
                tabWidth * index + (tabWidth - 28.dp) / 2,
                spring(dampingRatio = 0.72f, stiffness = 380f),
                label = "indicator",
            )
            Box(Modifier.offset(x = indicatorX).width(28.dp).height(2.dp).background(Ink.Fg))
            Row(Modifier.fillMaxWidth().height(64.dp)) {
                tabs.forEachIndexed { i, tab ->
                    val isSelected = i == index
                    val emphasis by animateFloatAsState(if (isSelected) 1f else 0f, tween(320), label = "tab")
                    Column(
                        Modifier.weight(1f).fillMaxSize()
                            .clickable(remember { MutableInteractionSource() }, indication = null) { if (!isSelected) onSelect(tab.route) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val tint = lerpColor(Ink.Faint, Ink.Fg, emphasis)
                        Icon(tab.icon, null, tint = tint, modifier = Modifier.size(22.dp).offset(y = (-2 * emphasis).dp))
                        Spacer(Modifier.height(5.dp))
                        Text(tab.label.uppercase(), style = AlbionType.eyebrow.copy(letterSpacing = 0.5.sp, fontSize = 9.sp), maxLines = 1, softWrap = false, color = tint)
                    }
                }
            }
        }
    }
}
