package com.marat.albion.ui.nav

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.marat.albion.ui.browse.BrowseScreen
import com.marat.albion.ui.browse.CardDetailScreen
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.lerpColor
import com.marat.albion.ui.deck.DeckScreen
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

private data class Tab(val route: TopLevel, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(HomeRoute, "Карточки", Icons.Outlined.Layers),
    Tab(GrammarRoute, "Учебник", Icons.AutoMirrored.Outlined.MenuBook),
    Tab(LibraryRoute, "Чтение", Icons.Outlined.AutoStories),
    Tab(GamesRoute, "Практика", Icons.Outlined.SportsEsports),
    Tab(StatsRoute, "Прогресс", Icons.Outlined.Insights),
)

// Переход вглубь: новый экран въезжает справа и «приближается», старый уходит в глубину.
private fun forward(): ContentTransform =
    (slideInHorizontally(tween(220, easing = Easing.Emphasized)) { (it * 0.08f).toInt() } +
        fadeIn(tween(180)) +
        scaleIn(tween(220, easing = Easing.Emphasized), initialScale = 0.985f)) togetherWith
        (fadeOut(tween(140)) + scaleOut(tween(220, easing = Easing.Emphasized), targetScale = 0.98f))

private fun backward(): ContentTransform =
    (fadeIn(tween(180)) + scaleIn(tween(220, easing = Easing.Emphasized), initialScale = 0.98f)) togetherWith
        (slideOutHorizontally(tween(220, easing = Easing.Emphasized)) { (it * 0.1f).toInt() } +
            fadeOut(tween(160)) +
            scaleOut(tween(220, easing = Easing.Emphasized), targetScale = 0.985f))

@Composable
fun AlbionNavigation() {
    val stacks = primaryRoutes.associateWith { route -> key(route) { rememberNavBackStack(route) } }
    val selectedIndex = rememberSaveable { mutableIntStateOf(0) }
    val navigator = remember(stacks, selectedIndex) { Navigator(stacks, selectedIndex) }
    val backStack = navigator.backStack
    val current = backStack.lastOrNull()
    val showBar = current !is StudyRoute && current !is ReaderRoute && current !is GameRoute &&
        current !is TopicQuizRoute && current != SettingsRoute
    BackHandler(enabled = backStack.size == 1 && navigator.selectedTab != HomeRoute) { navigator.back() }

    val provider = entryProvider<NavKey> {
        entry<HomeRoute> { HomeScreen(navigator) }
        entry<GrammarRoute> { GrammarScreen(navigator) }
        entry<GamesRoute> { GamesScreen(navigator) }
        entry<GameRoute> { GameHost(it.game, navigator) }
        entry<LibraryRoute> { LibraryScreen(navigator) }
        entry<ReaderRoute> { ReaderScreen(it.bookId, navigator) }
        entry<StatsRoute> { StatsScreen(navigator) }
        entry<SettingsRoute> { SettingsScreen(navigator::back) }
        entry<DeckRoute> { DeckScreen(it.deck, navigator) }
        entry<StudyRoute> { StudyScreen(it.deck, navigator) }
        entry<BrowseRoute> { BrowseScreen(it.deck, navigator) }
        entry<CardRoute> { CardDetailScreen(it.cardId, navigator) }
        entry<TopicRoute> { TopicScreen(it.topicId, navigator, fromQuiz = it.fromQuiz) }
        entry<TopicQuizRoute> { TopicQuizHost(it.topic, it.topicId, it.title, navigator) }
    }
    // Декораторы всех стеков остаются в композиции, даже когда отображается другой раздел.
    val entries = stacks.mapValues { (tab, stack) ->
        key(tab) {
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = { route ->
                    val original = provider(route)
                    NavEntry(route, contentKey = "$tab:$route", metadata = original.metadata) { original.Content() }
                },
            )
        }
    }

    Scaffold(
        containerColor = Ink.Bg,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = fadeIn(tween(180)) + slideInVertically(tween(220, easing = Easing.Emphasized)) { it },
                exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { it },
            ) {
                BottomBar(navigator.selectedTab, navigator::selectTab)
            }
        },
    ) { padding ->
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        Box(Modifier.fillMaxSize()) {
            NavDisplay(
                entries = entries.getValue(navigator.selectedTab),
                onBack = { navigator.back() },
                modifier = Modifier.padding(bottom).consumeWindowInsets(bottom),
                transitionSpec = { forward() },
                popTransitionSpec = { backward() },
                predictivePopTransitionSpec = { backward() },
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
    val labelStyle = AlbionType.eyebrow.copy(letterSpacing = 0.sp, fontSize = 12.sp, lineHeight = 16.sp)
    val textMeasurer = rememberTextMeasurer()
    val labelWidth = with(LocalDensity.current) {
        tabs.maxOf { textMeasurer.measure(it.label, labelStyle.copy(fontWeight = FontWeight.SemiBold), softWrap = false).size.width }.toDp() + 12.dp
    }
    Column(Modifier.background(Ink.Bg).windowInsetsPadding(WindowInsets.navigationBars)) {
        Hairline()
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / tabs.size
            val compact = tabWidth < labelWidth
            val selectedWidth = if (compact) {
                labelWidth.coerceAtMost(maxWidth - 48.dp * (tabs.size - 1)).coerceAtLeast(tabWidth)
            } else tabWidth
            val otherWidth = (maxWidth - selectedWidth) / (tabs.size - 1)
            val indicatorX by animateDpAsState(
                otherWidth * index + (selectedWidth - 28.dp) / 2,
                spring(dampingRatio = 0.72f, stiffness = 380f),
                label = "indicator",
            )
            Box(Modifier.offset(x = indicatorX).width(28.dp).height(2.dp).background(Ink.Fg))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup()) {
                tabs.forEachIndexed { i, tab ->
                    val isSelected = i == index
                    val emphasis by animateFloatAsState(if (isSelected) 1f else 0f, tween(180), label = "tab")
                    Column(
                        Modifier.width(if (isSelected) selectedWidth else otherWidth).fillMaxHeight().heightIn(min = 80.dp)
                            .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab.route) })
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        val tint = lerpColor(Ink.Muted, Ink.Fg, emphasis)
                        Box(Modifier.width(48.dp).height(28.dp).background(if (isSelected) Ink.High else Ink.Bg, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(tab.icon, if (compact && !isSelected) tab.label else null, tint = tint, modifier = Modifier.size(24.dp))
                        }
                        if (!compact || isSelected) {
                            Spacer(Modifier.height(6.dp))
                            Text(tab.label, style = labelStyle.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                                maxLines = 1, softWrap = false, textAlign = TextAlign.Center, color = tint)
                        }
                    }
                }
            }
        }
    }
}
