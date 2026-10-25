package com.marat.albion.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
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

private data class Tab(val route: TopLevel, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(HomeRoute, "Карточки", Icons.Outlined.Layers),
    Tab(GrammarRoute, "Учебник", Icons.AutoMirrored.Outlined.MenuBook),
    Tab(LibraryRoute, "Чтение", Icons.Outlined.AutoStories),
    Tab(GamesRoute, "Практика", Icons.Outlined.SportsEsports),
    Tab(StatsRoute, "Прогресс", Icons.Outlined.Insights),
)

// Новый экран въезжает справа с лёгким пружинным «доездом», старый отступает в глубину.
private fun forward(): ContentTransform =
    (slideInHorizontally(spring(dampingRatio = 0.86f, stiffness = 520f)) { (it * 0.14f).toInt() } +
        fadeIn(tween(200)) +
        scaleIn(tween(300, easing = Easing.Emphasized), initialScale = 0.97f)) togetherWith
        (fadeOut(tween(160)) + scaleOut(tween(300, easing = Easing.Emphasized), targetScale = 0.96f))

private fun backward(): ContentTransform =
    (fadeIn(tween(220)) + scaleIn(tween(300, easing = Easing.Emphasized), initialScale = 0.96f)) togetherWith
        (slideOutHorizontally(tween(260, easing = Easing.Emphasized)) { (it * 0.16f).toInt() } +
            fadeOut(tween(180)) +
            scaleOut(tween(260, easing = Easing.Emphasized), targetScale = 0.97f))

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
                    NavEntry(route, contentKey = "$tab:$route", metadata = original.metadata + paneMetadata(route)) {
                        CenteredColumn(maxWidthFor(route)) { original.Content() }
                    }
                },
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Ink.Bg)) {
    // На планшете вкладки — боковой панелью слева, на телефоне — снизу.
    val wide = widthClassOf(maxWidth) != WidthClass.Compact
    // Ещё шире — список и открытая из него страница рядом (учебник, все карточки колоды).
    val listDetail = maxWidth >= ListDetailMinWidth
    Scaffold(
        containerColor = Ink.Bg,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBar && !wide,
                enter = fadeIn(tween(180)) + slideInVertically(tween(220, easing = Easing.Emphasized)) { it },
                exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { it },
            ) {
                BottomBar(navigator.selectedTab, navigator::selectTab)
            }
        },
    ) { padding ->
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        val rail = showBar && wide
        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = rail,
                enter = fadeIn(tween(180)) + slideInHorizontally(tween(220, easing = Easing.Emphasized)) { -it },
                exit = fadeOut(tween(120)) + slideOutHorizontally(tween(180)) { -it },
            ) {
                SideRail(navigator.selectedTab, navigator::selectTab)
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                NavDisplay(
                    entries = entries.getValue(navigator.selectedTab),
                    onBack = { navigator.back() },
                    sceneStrategies = remember(listDetail) { listOf(ListDetailSceneStrategy(listDetail)) },
                    modifier = Modifier.padding(bottom).consumeWindowInsets(bottom)
                        // Без нижней панели вкладок разделы сами отступают от системной навигации.
                        .then(if (rail) Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)) else Modifier),
                    transitionSpec = { forward() },
                    popTransitionSpec = { backward() },
                    predictivePopTransitionSpec = { backward() },
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

/** Значок вкладки: при выборе подпрыгивает с лёгким «перелётом», как фишка на игровом поле. */
@Composable
private fun TabIcon(icon: ImageVector, selected: Boolean, description: String?) {
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            bounce.snapTo(0.78f)
            bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    val emphasis by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "tabTint")
    Icon(
        icon, description,
        tint = lerpColor(Ink.Muted, Ink.Accent, emphasis),
        modifier = Modifier.size(24.dp).graphicsLayer {
            scaleX = bounce.value; scaleY = bounce.value
            translationY = (1f - bounce.value) * 18f
        },
    )
}

/** Боковая панель вкладок для планшета: мягкая «капля» перетекает к выбранной вкладке. */
@Composable
private fun SideRail(selected: TopLevel, onSelect: (TopLevel) -> Unit) {
    val index = tabs.indexOfFirst { it.route == selected }.coerceAtLeast(0)
    val labelStyle = AlbionType.eyebrow.copy(letterSpacing = 0.sp, fontSize = 12.sp, lineHeight = 16.sp)
    val itemHeight = 84.dp
    val indicatorY by animateDpAsState(
        itemHeight * index + (itemHeight - 32.dp - 22.dp) / 2,
        spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "railIndicator",
    )
    Row(
        Modifier.fillMaxHeight().background(Ink.Bg)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom)),
    ) {
        Box(Modifier.width(96.dp).fillMaxHeight().windowInsetsPadding(WindowInsets.statusBars).padding(top = 24.dp)) {
            Box(Modifier.offset { IntOffset(((96.dp - 60.dp) / 2).roundToPx(), indicatorY.roundToPx()) }
                .width(60.dp).height(32.dp).background(Ink.AccentSoft, RoundedCornerShape(16.dp)))
            Column(Modifier.fillMaxWidth().selectableGroup()) {
                tabs.forEachIndexed { i, tab ->
                    val isSelected = i == index
                    val emphasis by animateFloatAsState(if (isSelected) 1f else 0f, tween(180), label = "railTab")
                    Column(
                        Modifier.fillMaxWidth().height(itemHeight)
                            .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab.route) })
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(Modifier.width(60.dp).height(32.dp), contentAlignment = Alignment.Center) {
                            TabIcon(tab.icon, isSelected, null)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(tab.label, style = labelStyle.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                            maxLines = 1, softWrap = false, textAlign = TextAlign.Center, color = lerpColor(Ink.Muted, Ink.Fg, emphasis))
                    }
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Ink.Line))
    }
}

/**
 * Нижняя панель — плавающая «пилюля» над страницей. За выбранной вкладкой пружинисто
 * перетекает мягкая капля цвета шарфа, значок подпрыгивает.
 */
@Composable
private fun BottomBar(selected: TopLevel, onSelect: (TopLevel) -> Unit) {
    val index = tabs.indexOfFirst { it.route == selected }.coerceAtLeast(0)
    val labelStyle = AlbionType.eyebrow.copy(letterSpacing = 0.sp, fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold)
    val textMeasurer = rememberTextMeasurer()
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier.background(Ink.Bg).windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 10.dp),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().softShadow(shape, 10.dp).background(Ink.Raised, shape).border(1.dp, Ink.Line, shape)
        ) {
            val tabWidth = maxWidth / tabs.size
            val availablePx = with(LocalDensity.current) { (tabWidth - 2.dp).roundToPx() }
            // Полные названия не разрываем внутри слов и не уменьшаем системный шрифт.
            val needsMenu = tabs.any { textMeasurer.measure(it.label, labelStyle, softWrap = false).size.width > availablePx }
            if (needsMenu) {
                NavigationMenu(selected, onSelect)
                return@BoxWithConstraints
            }
            val blobWidth = 56.dp.coerceAtMost(tabWidth)
            val indicatorX by animateDpAsState(
                tabWidth * index + (tabWidth - blobWidth) / 2,
                spring(dampingRatio = 0.62f, stiffness = 420f),
                label = "indicator",
            )
            Box(Modifier.offset { IntOffset(indicatorX.roundToPx(), 8.dp.roundToPx()) }
                .width(blobWidth).height(32.dp).background(Ink.AccentSoft, RoundedCornerShape(16.dp)))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup()) {
                tabs.forEachIndexed { i, tab ->
                    val isSelected = i == index
                    val emphasis by animateFloatAsState(if (isSelected) 1f else 0f, tween(180), label = "tab")
                    Column(
                        Modifier.weight(1f).fillMaxHeight().heightIn(min = 68.dp)
                            .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab.route) })
                            // Подпись выше нижних скруглений капсулы, включая экран 320 dp.
                            .padding(start = 1.dp, end = 1.dp, top = 8.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Box(Modifier.width(blobWidth).height(32.dp), contentAlignment = Alignment.Center) {
                            TabIcon(tab.icon, isSelected, null)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(tab.label, style = labelStyle, maxLines = 1, softWrap = false,
                            textAlign = TextAlign.Center, color = lerpColor(Ink.Muted, Ink.Fg, emphasis))
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
        Icon(current.icon, null, tint = Ink.Accent, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(current.label, style = AlbionType.eyebrow.copy(letterSpacing = 0.sp, fontSize = 14.sp,
                lineHeight = 20.sp, fontWeight = FontWeight.SemiBold), color = Ink.Fg)
            Text("Все разделы", style = AlbionType.eyebrow.copy(letterSpacing = 0.sp, fontSize = 12.sp,
                lineHeight = 16.sp), color = Ink.Muted)
        }
        Icon(Icons.Rounded.Menu, null, tint = Ink.Accent, modifier = Modifier.size(24.dp))
    }
    if (open) ModalBottomSheet(
        onDismissRequest = { open = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink.Raised, contentColor = Ink.Text, scrimColor = Ink.Bg.copy(alpha = 0.4f),
        dragHandle = { Hairline(Modifier.padding(top = 10.dp, bottom = 6.dp).width(36.dp), color = Ink.LineStrong) },
    ) {
        CompositionLocalProvider(LocalDensity provides density) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                .navigationBarsPadding().padding(bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("Разделы", Modifier.weight(1f))
                    IconButton(onClick = { open = false }) {
                        Icon(Icons.Rounded.Close, "Закрыть разделы", tint = Ink.Muted)
                    }
                }
                Column(Modifier.selectableGroup()) {
                    tabs.forEach { tab ->
                        val active = tab.route == selected
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .background(if (active) Ink.AccentSoft else Ink.Raised, RoundedCornerShape(16.dp))
                            .selectable(selected = active, role = Role.Tab, onClick = {
                                open = false
                                onSelect(tab.route)
                            }).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(tab.icon, null, tint = if (active) Ink.Accent else Ink.Muted, modifier = Modifier.size(24.dp))
                            Text(tab.label, modifier = Modifier.weight(1f), style = AlbionType.eyebrow.copy(
                                letterSpacing = 0.sp, fontSize = 16.sp, lineHeight = 22.sp), color = Ink.Fg)
                            if (active) Icon(Icons.Rounded.Check, null, tint = Ink.Accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
