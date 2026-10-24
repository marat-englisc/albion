package com.marat.albion.ui.nav

import androidx.compose.runtime.MutableIntState
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.marat.albion.data.content.Deck
import com.marat.albion.ui.games.Game
import kotlinx.serialization.Serializable

sealed interface TopLevel : NavKey
@Serializable data object HomeRoute : TopLevel
@Serializable data object GrammarRoute : TopLevel
@Serializable data object LibraryRoute : TopLevel
@Serializable data object GamesRoute : TopLevel
@Serializable data object StatsRoute : TopLevel
@Serializable data object SettingsRoute : NavKey

@Serializable data class DeckRoute(val deck: Deck) : NavKey
/** [deck] = null — общее занятие по всем колодам. */
@Serializable data class StudyRoute(val deck: Deck? = null) : NavKey
@Serializable data class BrowseRoute(val deck: Deck) : NavKey
@Serializable data class CardRoute(val cardId: String) : NavKey
@Serializable data class TopicRoute(val topicId: String, val fromQuiz: Boolean = false) : NavKey
@Serializable data class TopicQuizRoute(val topic: Int, val topicId: String, val title: String) : NavKey
@Serializable data class GameRoute(val game: Game) : NavKey
@Serializable data class ReaderRoute(val bookId: String) : NavKey

internal val primaryRoutes = listOf(HomeRoute, GrammarRoute, LibraryRoute, GamesRoute, StatsRoute)

/** У каждого раздела свой стек: переключение вкладок не удаляет экраны и их состояние. */
class Navigator internal constructor(
    internal val stacks: Map<TopLevel, NavBackStack<NavKey>>,
    private val selectedIndex: MutableIntState,
) {
    val selectedTab: TopLevel get() = primaryRoutes[selectedIndex.intValue.coerceIn(primaryRoutes.indices)]
    val backStack: NavBackStack<NavKey> get() = stacks.getValue(selectedTab)

    fun openSettings() = open(SettingsRoute)

    fun open(route: NavKey) {
        if (route is TopLevel) {
            switchTab(route)
            popToRoot()
            return
        }
        val existing = backStack.indexOf(route)
        if (existing >= 0) {
            while (backStack.lastIndex > existing) backStack.removeAt(backStack.lastIndex)
        } else backStack.add(route)
    }

    /** Страница, открытая из списка, — на планшете она видна рядом, и в списке её строка подсвечена. */
    val shownDetail: NavKey? get() = backStack.lastOrNull()?.takeIf { paneOf(it) == Pane.Detail }

    /**
     * Открывает страницу из списка (тему из оглавления, карточку из списка колоды).
     * На планшете страница стоит рядом со списком, поэтому новая заменяет прежнюю, а не копится в истории.
     * На телефоне над списком страниц нет, и это обычный переход.
     */
    fun openDetail(route: NavKey) {
        while (backStack.size > 1 && backStack.last().let { paneOf(it) == Pane.Detail }) backStack.removeAt(backStack.lastIndex)
        open(route)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        else if (selectedTab != HomeRoute) switchTab(HomeRoute)
    }

    fun replace(route: NavKey) {
        if (route is TopLevel) { open(route); return }
        if (route == backStack.lastOrNull()) return
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        open(route)
    }

    fun switchTab(tab: TopLevel) {
        selectedIndex.intValue = primaryRoutes.indexOf(tab)
    }

    /** Повторное нажатие выбранной вкладки возвращает в начало этого раздела. */
    fun selectTab(tab: TopLevel) {
        if (tab == selectedTab) popToRoot()
        else switchTab(tab)
    }

    private fun popToRoot() {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}
