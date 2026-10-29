package com.marat.albion.ui.nav

import com.marat.albion.ui.theme.paperSurface
import com.marat.albion.ui.components.AlbionIcons
import androidx.activity.compose.BackHandler
import com.marat.albion.ui.theme.QuietAnimatedVisibility as AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import com.marat.albion.ui.components.CenteredColumn
import com.marat.albion.ui.components.DashboardWidth
import com.marat.albion.ui.components.FocusWidth
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline

import com.marat.albion.ui.components.ReadableWidth
import com.marat.albion.ui.components.WidthClass
import com.marat.albion.ui.components.widthClassOf
import com.marat.albion.ui.components.lerpColor
import com.marat.albion.ui.components.softShadow
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
import com.marat.albion.ui.theme.LocalReducedMotion
import com.marat.albion.ui.theme.LocalFeedback

private data class Tab(val route: TopLevel, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(HomeRoute, "Сегодня", AlbionIcons.Layers),
    Tab(GrammarRoute, "Учебник", AlbionIcons.MenuBook),
    Tab(LibraryRoute, "Чтение", AlbionIcons.AutoStories),
    Tab(GamesRoute, "Практика", AlbionIcons.EditNote),
    Tab(StatsRoute, "Прогресс", AlbionIcons.Insights),
)

// A short page movement and dissolve, without spring or depth effects.
private fun forward(reduced: Boolean): ContentTransform =
    if (reduced) ContentTransform(androidx.compose.animation.EnterTransition.None, androidx.compose.animation.ExitTransition.None, sizeTransform = null) else (fadeIn(tween(220)) + slideInHorizontally(tween(280, easing = Easing.Standard)) { if (reduced) 0 else it / 22 }) togetherWith fadeOut(tween(160))
private fun backward(reduced: Boolean): ContentTransform =
    if (reduced) ContentTransform(androidx.compose.animation.EnterTransition.None, androidx.compose.animation.ExitTransition.None, sizeTransform = null) else fadeIn(tween(200)) togetherWith (fadeOut(tween(160)) + slideOutHorizontally(tween(240, easing = Easing.Standard)) { if (reduced) 0 else it / 22 })

@Composable
fun AlbionNavigation() {
    val reduced = LocalReducedMotion.current
    val feedback = LocalFeedback.current
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
        entry<GameRoute> { GameHost(it.game, navigator, it.deck, it.onlyMistakes, it.count) }
        entry<LibraryRoute> { LibraryScreen(navigator) }
        entry<ReaderRoute> { ReaderScreen(it.bookId, navigator) }
        entry<StatsRoute> { StatsScreen(navigator) }
        entry<SettingsRoute> { SettingsScreen(navigator::back) }
        entry<DeckRoute> { DeckScreen(it.deck, navigator) }
        entry<StudyRoute> { StudyScreen(it.deck, navigator, it.answerLimit) }
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
                    NavEntry(route, contentKey = "$tab:$route", metadata = original.metadata + paneMetadata(route)) {
                        CenteredColumn(maxWidthFor(route)) { original.Content() }
                    }
                },
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().paperSurface()) {
    // На планшете вкладки — боковой панелью слева, на телефоне — снизу.
    val wide = widthClassOf(maxWidth) != WidthClass.Compact
    // Ещё шире — список и открытая из него страница рядом (учебник, все карточки колоды).
    val listDetail = maxWidth >= ListDetailMinWidth && LocalDensity.current.fontScale <= 1.3f
    Scaffold(
        containerColor = Ink.Bg,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBar && !wide,
                enter = fadeIn(tween(180)) + slideInVertically(tween(220, easing = Easing.Standard)) { if (reduced) 0 else it / 3 },
                exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { if (reduced) 0 else it / 3 },
            ) {
                BottomBar(navigator.selectedTab) { feedback.selection(); navigator.selectTab(it) }
            }
        },
    ) { padding ->
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        val rail = showBar && wide
        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = rail,
                enter = fadeIn(tween(180)) + slideInHorizontally(tween(220, easing = Easing.Standard)) { if (reduced) 0 else -it / 3 },
                exit = fadeOut(tween(120)) + slideOutHorizontally(tween(180)) { if (reduced) 0 else -it / 3 },
            ) {
                SideRail(navigator.selectedTab) { feedback.selection(); navigator.selectTab(it) }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                NavDisplay(
                    entries = entries.getValue(navigator.selectedTab),
                    onBack = { navigator.back() },
                    sceneStrategies = remember(listDetail) { listOf(ListDetailSceneStrategy(listDetail)) },
                    modifier = Modifier.padding(bottom).consumeWindowInsets(bottom)
                        // Без нижней панели вкладок разделы сами отступают от системной навигации.
                        .then(if (rail) Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)) else Modifier),
                    transitionSpec = { forward(reduced) },
                    popTransitionSpec = { backward(reduced) },
                    predictivePopTransitionSpec = { backward(reduced) },
                )
                // Прокручиваемый контент не должен просвечивать под статус-баром.
                Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(Ink.Bg.copy(alpha = 0.94f)))
            }
        }
    }
    }
}

/** Ширина колонки экрана: книга — на всю ширину, задания — уже, списки и текст — колонкой для чтения. */
private fun maxWidthFor(route: NavKey): Dp = when (route) {
    is ReaderRoute -> Dp.Infinity
    // Главная на планшете делится на две колонки: сегодня и колоды.
    HomeRoute -> DashboardWidth
    is StudyRoute, is GameRoute, is TopicQuizRoute -> FocusWidth
    else -> ReadableWidth
}

/** Selected tabs change tone without displacing the icon. */
@Composable
private fun TabIcon(icon: ImageVector, selected: Boolean, description: String?) {
    val tone by androidx.compose.animation.animateColorAsState(if (selected) Ink.AccentText else Ink.Muted, tween(if (LocalReducedMotion.current) 0 else 180), label = "tabTint")
    Icon(icon, description, tint = tone, modifier = Modifier.size(23.dp))
}

@Composable
private fun SideRail(selected: TopLevel, onSelect: (TopLevel) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val railWidth = if (fontScale > 1.3f) 144.dp else 104.dp
    Row(Modifier.fillMaxHeight().paperSurface().windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom))) {
        Column(Modifier.width(railWidth).fillMaxHeight().windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState()).padding(top = 12.dp).selectableGroup()) {
            tabs.forEach { tab ->
                val active = tab.route == selected
                Column(Modifier.fillMaxWidth().heightIn(min = 80.dp)
                    .background(if (active) Ink.AccentSoft else Ink.Raised)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab.route) })
                    .padding(horizontal = 6.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TabIcon(tab.icon, active, null)
                    Spacer(Modifier.height(8.dp))
                    Text(tab.label, style = AlbionType.eyebrow.copy(letterSpacing = 0.sp, fontSize = 12.sp, lineHeight = 17.sp),
                        textAlign = TextAlign.Center, color = if (active) Ink.AccentText else Ink.Muted)
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Ink.Line))
    }
}

/** Quiet full-width navigation, with a fine crimson selection rule. */
@Composable
private fun BottomBar(selected: TopLevel, onSelect: (TopLevel) -> Unit) {
    val index = tabs.indexOfFirst { it.route == selected }.coerceAtLeast(0)
    val labelStyle = AlbionType.eyebrow.copy(letterSpacing = 0.sp, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
    val textMeasurer = rememberTextMeasurer()
    Column(Modifier.background(Ink.Raised).windowInsetsPadding(WindowInsets.navigationBars)) {
        Hairline()
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / tabs.size
            val availablePx = with(LocalDensity.current) { (tabWidth - 4.dp).roundToPx() }
            val needsMenu = tabs.any { textMeasurer.measure(it.label, labelStyle, softWrap = false).size.width > availablePx }
            if (needsMenu) {
                NavigationMenu(selected, onSelect)
                return@BoxWithConstraints
            }
            val markerWidth = 28.dp.coerceAtMost(tabWidth)
            val indicatorX by animateDpAsState(tabWidth * index + (tabWidth - markerWidth) / 2,
                tween(if (LocalReducedMotion.current) 0 else 220, easing = Easing.Standard), label = "indicator")
            Box(Modifier.offset { IntOffset(indicatorX.roundToPx(), 0) }.width(markerWidth).height(2.dp).background(Ink.Gold))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup()) {
                tabs.forEachIndexed { i, tab ->
                    val active = i == index
                    Column(Modifier.weight(1f).fillMaxHeight().heightIn(min = 74.dp)
                        .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab.route) })
                        .padding(horizontal = 2.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        TabIcon(tab.icon, active, null)
                        Spacer(Modifier.height(7.dp))
                        Text(tab.label, style = labelStyle.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                            maxLines = 1, softWrap = false, textAlign = TextAlign.Center, color = if (active) Ink.AccentText else Ink.Muted)
                    }
                }
            }
        }
    }
}

/** При крупном шрифте список сохраняет полные названия и оставляет место самой странице. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationMenu(selected: TopLevel, onSelect: (TopLevel) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val current = tabs.first { it.route == selected }
    val density = LocalDensity.current
    Row(Modifier.fillMaxWidth().heightIn(min = 68.dp)
        .clickable(role = Role.Button, onClick = { open = true }).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(current.icon, null, tint = Ink.AccentText, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(current.label, style = AlbionType.eyebrow.copy(letterSpacing = 0.sp, fontSize = 14.sp,
                lineHeight = 20.sp, fontWeight = FontWeight.SemiBold), color = Ink.Fg)
            Text("Все разделы", style = AlbionType.eyebrow.copy(letterSpacing = 0.sp, fontSize = 12.sp,
                lineHeight = 16.sp), color = Ink.Muted)
        }
        Icon(AlbionIcons.Menu, null, tint = Ink.AccentText, modifier = Modifier.size(24.dp))
    }
    if (open) ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = { com.marat.albion.ui.components.SheetHandle() },
        onDismissRequest = { open = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink.Raised, contentColor = Ink.Text, scrimColor = Ink.Fg.copy(alpha = 0.32f),
    ) {
        CompositionLocalProvider(LocalDensity provides density) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                .navigationBarsPadding().padding(bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("Разделы", Modifier.weight(1f))
                    IconButton(onClick = { open = false }) {
                        Icon(AlbionIcons.Close, "Закрыть разделы", tint = Ink.Muted)
                    }
                }
                Column(Modifier.selectableGroup()) {
                    tabs.forEach { tab ->
                        val active = tab.route == selected
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .background(if (active) Ink.AccentSoft else Ink.Raised, RoundedCornerShape(8.dp))
                            .selectable(selected = active, role = Role.Tab, onClick = {
                                open = false
                                onSelect(tab.route)
                            }).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(tab.icon, null, tint = if (active) Ink.AccentText else Ink.Muted, modifier = Modifier.size(24.dp))
                            Text(tab.label, modifier = Modifier.weight(1f), style = AlbionType.eyebrow.copy(
                                letterSpacing = 0.sp, fontSize = 16.sp, lineHeight = 22.sp), color = Ink.Fg)
                            if (active) Icon(AlbionIcons.Check, null, tint = Ink.AccentText, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
