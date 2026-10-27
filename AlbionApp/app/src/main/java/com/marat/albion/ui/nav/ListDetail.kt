package com.marat.albion.ui.nav

import com.marat.albion.ui.theme.paperSurface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.marat.albion.ui.theme.Ink

/** Роль экрана в двухпанельной вёрстке планшета. */
internal enum class Pane { List, Detail }

private const val PANE_KEY = "albion.pane"
private const val PLACEHOLDER_KEY = "albion.placeholder"

/** Какие экраны — список, а какие — его подробности. Остальные всегда занимают весь экран. */
internal fun paneOf(route: NavKey): Pane? = when (route) {
    GrammarRoute, is BrowseRoute -> Pane.List
    is TopicRoute, is CardRoute -> Pane.Detail
    else -> null
}

/** Метаданные записи навигации: роль панели и подсказка для пустой правой панели. */
internal fun paneMetadata(route: NavKey): Map<String, Any> = when (paneOf(route)) {
    Pane.List -> mapOf(
        PANE_KEY to Pane.List,
        PLACEHOLDER_KEY to if (route is BrowseRoute) "Выберите карточку в списке" else "Выберите тему в оглавлении",
    )
    Pane.Detail -> mapOf(PANE_KEY to Pane.Detail)
    null -> emptyMap()
}

private val NavEntry<*>.pane get() = metadata[PANE_KEY] as? Pane

/**
 * На широком экране список и открытая из него страница показываются рядом:
 * оглавление учебника слева, тема справа. Стек навигации тот же, что и на телефоне, —
 * меняется только то, как он нарисован, поэтому «Назад» и сохранение состояния работают как обычно.
 */
internal class ListDetailSceneStrategy(private val enabled: Boolean) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        if (!enabled) return null
        val last = entries.lastOrNull() ?: return null
        return when (last.pane) {
            Pane.List -> ListDetailScene(list = last, detail = null, previousEntries = entries.dropLast(1))
            Pane.Detail -> {
                // Справа — последняя страница; между ней и списком могут лежать только другие страницы.
                val listIndex = entries.indexOfLast { it.pane == Pane.List }
                if (listIndex < 0 || entries.subList(listIndex + 1, entries.size).any { it.pane != Pane.Detail }) return null
                ListDetailScene(list = entries[listIndex], detail = last, previousEntries = entries.dropLast(1))
            }
            null -> null
        }
    }
}

private data class ListDetailScene(
    val list: NavEntry<NavKey>,
    val detail: NavEntry<NavKey>?,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    // Ключ — список: смена темы справа не перерисовывает весь экран переходом.
    override val key: Any = "list-detail:${list.contentKey}"
    override val entries: List<NavEntry<NavKey>> = listOfNotNull(list, detail)
    override val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(LIST_PANE_WIDTH).fillMaxHeight()) {
                CompositionLocalProvider(LocalBesideDetail provides true) { list.Content() }
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(Ink.Line))
            AnimatedContent(
                targetState = detail,
                contentKey = { it?.contentKey },
                transitionSpec = { fadeIn(tween(200, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = "detail",
            ) { entry ->
                if (entry != null) entry.Content()
                else DetailPlaceholder(list.metadata[PLACEHOLDER_KEY] as? String ?: "")
            }
        }
    }
}

@Composable
private fun DetailPlaceholder(text: String) {
    Box(Modifier.fillMaxSize().paperSurface().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Ink.Faint, textAlign = TextAlign.Center)
    }
}

private val LIST_PANE_WIDTH = 380.dp

/** true — список нарисован рядом с открытой страницей: ему стоит держать её строку на виду. */
val LocalBesideDetail = staticCompositionLocalOf { false }

/** С какой ширины окна список и страница помещаются рядом без тесноты. */
internal val ListDetailMinWidth = 1000.dp
