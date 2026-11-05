package com.marat.albion.ui.library

import com.marat.albion.ui.components.DestructiveConfirmationDialog
import com.marat.albion.ui.theme.quietItem
import com.marat.albion.ui.components.QuietCircularProgressIndicator as CircularProgressIndicator
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.marat.albion.ui.theme.pressScale
import com.marat.albion.ui.theme.appear
import com.marat.albion.ui.components.Disclosure
import com.marat.albion.ui.components.FieldError
import com.marat.albion.ui.components.LoadingState
import com.marat.albion.ui.components.RoundIconButton
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import com.marat.albion.ui.components.AlbionIcons
import com.marat.albion.ui.components.gutter
import com.marat.albion.ui.components.EmptyState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import com.marat.albion.ui.components.PaneDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextField
import com.marat.albion.ui.components.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.library.LibraryBook
import com.marat.albion.ui.components.CatalogueCard
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.FeatureHelp
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.LevelBadge
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.ScreenHeader
import com.marat.albion.ui.components.SettingsShortcut
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.ReaderRoute
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun LibraryScreen(navigator: Navigator) {
    val c = LocalAppContainer.current
    val books by remember { c.library.observeLibrary() }.collectAsStateWithLifecycle(null)
    val model by c.translator.model.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val shelfState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    var importError by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<LibraryBook?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Все") }
    var sort by rememberSaveable { mutableStateOf("Недавние") }
    val list = books.orEmpty()
    val continueBook = list.firstOrNull { it.lastRead != null && it.progress < 0.999f }
    val shown = list.filter { b ->
        (query.isBlank() || b.book.title.contains(query.trim(), true) || b.book.author.contains(query.trim(), true)) && when(filter) {
            "Не начаты" -> b.lastRead == null
            "Читаю" -> b.lastRead != null && b.progress < .999f
            "Прочитаны" -> b.progress >= .999f
            else -> true
        }
    }.let { rows -> when(sort) {
        "По названию" -> rows.sortedBy { it.book.title.lowercase() }
        "По прогрессу" -> rows.sortedByDescending { it.progress }
        else -> rows
    } }


    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !importing) {
            importing = true
            importError = null
            scope.launch {
                try {
                    val book = c.library.import(uri)
                    navigator.open(ReaderRoute(book.id))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    importError = e.message ?: "Не удалось открыть книгу"
                } finally { importing = false }
            }
        }
    }

    val launchPicker = { picker.launch(arrayOf("text/plain", "text/markdown", "application/epub+zip", "application/pdf", "application/octet-stream")) }
    val filters = listOf("Все", "Читаю", "Не начаты", "Прочитаны")
    val controls: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit = {
        item(key = "library-filters", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.appear(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionPicker("Статус чтения", filters, filters.indexOf(filter), { filter = filters[it] })
                val sorts = listOf("Недавние", "По названию", "По прогрессу")
                OptionPicker("Порядок книг", sorts, sorts.indexOf(sort), { sort = sorts[it] })
            }
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(136.dp),
        state = shelfState,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader("Библиотека", eyebrow = "03 / КНИГИ", compact = true, trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = launchPicker, enabled = !importing, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp).semantics { contentDescription = if (importing) "Открываю книгу" else "Добавить книгу" }) {
                        Box(contentAlignment = Alignment.Center) {
                            if (importing) com.marat.albion.ui.components.QuietCircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                            else Icon(AlbionIcons.Add, null, Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    SettingsShortcut(navigator::openSettings)
                }
            })
        }
        item(key = "book-search", span = { GridItemSpan(maxLineSpan) }) {
            com.marat.albion.ui.browse.SearchField(query, { query = it }, "Поиск книги")
        }
        if (importError != null) item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                FieldError(importError)
                TextButton(onClick = launchPicker) { Text("Выбрать другой файл") }
            }
        }
        if (continueBook != null && query.isBlank() && filter == "Все") item(span = { GridItemSpan(maxLineSpan) }) {
            ContinueReading(continueBook, Modifier.appear(2),
                onDelete = if (continueBook.book.source == "user") ({ toDelete = continueBook }) else null,
                onOpen = { navigator.open(ReaderRoute(continueBook.book.id)) })
        }
        if (query.isBlank()) controls()
        if (books == null) item(span = { GridItemSpan(maxLineSpan) }) { LoadingState("Загружаю библиотеку") }
        val rows = if (query.isBlank() && filter == "Все") shown.filter { it.book.id != continueBook?.book?.id } else shown
        if (books != null && shown.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(if (list.isEmpty()) "Полка пока пустая" else "Книги не найдены",
                    if (list.isEmpty()) "Добавьте книгу: TXT, EPUB или PDF. Слова из неё можно переводить и сохранять." else "Измените запрос или сбросьте фильтр.")
                if (list.isNotEmpty()) TextButton(onClick = { query = ""; filter = "Все"; scope.launch { shelfState.scrollToItem(0) } }) { Text("Показать все книги") }
            }
        }
        if (rows.isNotEmpty() && query.isBlank()) item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
                Text(if (continueBook != null && query.isBlank() && filter == "Все") "Другие книги" else "Книги", Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge)
                Text("${rows.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(rows, key = { it.book.id }, span = { if (query.isBlank()) GridItemSpan(1) else GridItemSpan(maxLineSpan) }) { b ->
            val onDelete: (() -> Unit)? = if (b.book.source == "user") ({ toDelete = b }) else null
            if (query.isBlank()) BookTile(b, quietItem(), { navigator.open(ReaderRoute(b.book.id)) }, onDelete)
            else BookSearchRow(b, quietItem(), { navigator.open(ReaderRoute(b.book.id)) }, onDelete)
        }
        if (query.isNotBlank()) controls()
        item(span = { GridItemSpan(maxLineSpan) }) {
            // Добавление книги — кнопкой «+» в шапке; здесь вторая такая же кнопка не нужна.
            Column(Modifier.padding(top = 12.dp)) {
                Disclosure("Офлайн-перевод", model.summary) { TranslationModelControls() }
                Disclosure("Подсказки по чтению", "Перевод касанием, выделение и заметки", icon = AlbionIcons.HelpOutline) {
                    Text("Коснитесь слова — откроется перевод. Долгое нажатие выбирает предложение, протяжка — любой фрагмент. " +
                        "В панели перевода можно выбрать значение и сохранить слово в карточки. В меню читалки — поиск, закладки, заметки и оформление. " +
                        "Откройте меню действий рядом со своей книгой, чтобы удалить её вместе с заметками.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    toDelete?.let { b ->
        DestructiveConfirmationDialog(
            title = "Удалить книгу?",
            message = "«${b.book.title}», закладки, выделения и заметки к ней будут удалены из библиотеки. Отменить удаление нельзя.",
            onConfirm = { toDelete = null; scope.launch { c.library.delete(b.book.id) } },
            onDismiss = { toDelete = null },
        )
    }
}

/** «Продолжить чтение»: инверсный блок с обложкой, процентом и кнопкой. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ContinueReading(item: LibraryBook, modifier: Modifier, onDelete: (() -> Unit)? = null, onOpen: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val measurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.titleLarge
    val labelStyle = MaterialTheme.typography.labelMedium
    val minTextWidth = with(LocalDensity.current) {
        maxOf(measurer.measure("Продолжить", labelStyle).size.width + if (onDelete != null) 48.dp.roundToPx() else 0,
            item.book.title.split(Regex("\\s+")).maxOf { measurer.measure(it, titleStyle).size.width }).toDp()
    }.coerceAtLeast(132.dp)
    com.marat.albion.ui.components.BoundVolume(modifier.fillMaxWidth().pressScale(interaction, 0.98f).clip(MaterialTheme.shapes.extraLarge)
        .combinedClickable(interaction, indication = null, onClickLabel = "Читать", onClick = onOpen,
            onLongClick = onDelete?.let { { menu = true } }, onLongClickLabel = onDelete?.let { "Действия с книгой" })) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val description: @Composable () -> Unit = {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Продолжить", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current)
                        if (onDelete != null) BookActionsMenu(item.book.title, menu, { menu = it }, onDelete,
                            tint = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(item.book.title, style = MaterialTheme.typography.titleLarge)
                    if (item.book.author.isNotBlank()) Text(item.book.author, style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ThinProgress(item.progress, Modifier.weight(1f), color = LocalContentColor.current,
                            track = LocalContentColor.current.copy(alpha = 0.18f), height = 4.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("${(item.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (maxWidth >= 76.dp + 18.dp + minTextWidth) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookCover(item.book.title, Modifier.size(width = 76.dp, height = 112.dp), inverse = true)
                    Spacer(Modifier.width(18.dp))
                    Box(Modifier.weight(1f)) { description() }
                }
            } else {
                description()
            }
        }
        PrimaryButton("Читать", onOpen, trailingIcon = AlbionIcons.ArrowForward, inverse = true)
    }
}

/** Книга на полке: обложка, название и доступное меню для своей книги. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BookTile(item: LibraryBook, modifier: Modifier, onOpen: () -> Unit, onDelete: (() -> Unit)?) {
    val interaction = remember { MutableInteractionSource() }
    val feedback = com.marat.albion.ui.theme.LocalFeedback.current
    var menu by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().pressScale(interaction, 0.95f)
        .combinedClickable(interaction, indication = null, onClick = onOpen,
            onLongClick = onDelete?.let { { feedback.reveal(); menu = true } }, onLongClickLabel = onDelete?.let { "Действия с книгой" })) {
        Box {
            BookCover(item.book.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
            if (item.book.level.isNotBlank()) LevelBadge(item.book.level, Modifier.align(Alignment.TopEnd).padding(8.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape))
        }
        if (onDelete != null) UserBookToolbar(item.book.title, menu, { menu = it }, onDelete)
        else Spacer(Modifier.height(10.dp))
        Text(item.book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (item.book.author.isNotBlank()) Text(item.book.author, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (item.progress > 0f) {
            Spacer(Modifier.height(8.dp))
            ThinProgress(item.progress, height = 3.dp)
        }
    }
}

/** One visible action target for a user book, in the shelf, search and continuation. */
@Composable
private fun BookActionsMenu(
    title: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onDelete: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box {
        RoundIconButton(AlbionIcons.MoreHoriz, "Действия с книгой «$title»", { onExpandedChange(true) }, tint = tint)
        DropdownMenu(expanded, { onExpandedChange(false) }) {
            DropdownMenuItem(
                text = { Text("Удалить книгу") },
                onClick = { onExpandedChange(false); onDelete() },
                leadingIcon = { Icon(AlbionIcons.Delete, null, Modifier.size(20.dp)) },
                modifier = Modifier.heightIn(min = 52.dp),
                colors = androidx.compose.material3.MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error, leadingIconColor = MaterialTheme.colorScheme.error),
            )
        }
    }
}

@Composable
private fun UserBookToolbar(title: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Своя книга", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        BookActionsMenu(title, expanded, onExpandedChange, onDelete)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BookSearchRow(item: LibraryBook, modifier: Modifier, onOpen: () -> Unit, onDelete: (() -> Unit)?) {
    var menu by remember { mutableStateOf(false) }
    Panel(modifier.fillMaxWidth().combinedClickable(onClick = onOpen,
        onLongClick = onDelete?.let { { menu = true } },
        onLongClickLabel = onDelete?.let { "Действия с книгой" }, role = androidx.compose.ui.semantics.Role.Button),
        contentPadding = PaddingValues(14.dp)) {
        if (onDelete != null) UserBookToolbar(item.book.title, menu, { menu = it }, onDelete)
        Text(item.book.title, style = MaterialTheme.typography.titleMedium)
        if (item.book.author.isNotBlank()) Text(item.book.author, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (item.book.level.isNotBlank()) Text(item.book.level, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Типографская обложка: тонированная плашка, название акцентным шрифтом и тонкая линия-корешок. */
@Composable
fun BookCover(title: String, modifier: Modifier = Modifier, inverse: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val tones = listOf(scheme.surfaceContainerHighest, scheme.surfaceContainerHigh, scheme.secondaryContainer)
    val bg = if (inverse) scheme.inverseOnSurface.copy(alpha = 0.12f) else tones[Math.floorMod(title.hashCode(), tones.size)]
    val fg = if (inverse) scheme.inverseOnSurface else scheme.onSurface
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.clearAndSetSemantics {}.clip(MaterialTheme.shapes.medium)
        .background(bg).border(.75.dp, fg.copy(alpha = .16f), MaterialTheme.shapes.medium)) {
        val thumbnail = maxWidth < 100.dp
        // Миниатюра остаётся декоративной: полное название уже есть рядом с обложкой.
        if (thumbnail) OrbitalField(Modifier.fillMaxWidth(.56f).aspectRatio(1f).align(Alignment.TopCenter).padding(top = 10.dp), color = fg, animated = false)
        else OrbitalField(Modifier.fillMaxWidth().aspectRatio(1f).align(Alignment.Center).padding(8.dp), color = fg, animated = false)
        if (maxWidth >= 100.dp) SignalLabel("ALBION / READ", Modifier.align(Alignment.TopStart).padding(14.dp), color = fg.copy(alpha = .6f))
        // Полупрозрачная плашка на акцентной плите легла бы вторым слоем и дала полосу — подложка только у непрозрачной обложки.
        if (!thumbnail) Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().then(if (inverse) Modifier else Modifier.background(bg)).padding(14.dp)) {
            Box(Modifier.width(24.dp).height(1.dp).background(fg.copy(alpha = .5f)))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = fg, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

fun pluralRu(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> one
        m10 in 2..4 && m100 !in 12..14 -> few
        else -> many
    }
}
