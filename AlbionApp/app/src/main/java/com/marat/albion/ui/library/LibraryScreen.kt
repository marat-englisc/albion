package com.marat.albion.ui.library

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
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
    LazyVerticalGrid(
        columns = GridCells.Adaptive(136.dp),
        state = shelfState,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader("Библиотека", eyebrow = "03 / READING SPACE", compact = true, trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = launchPicker, enabled = !importing, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp).semantics { contentDescription = if (importing) "Открываю книгу" else "Добавить книгу" }) {
                        Box(contentAlignment = Alignment.Center) {
                            if (importing) androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                            else Icon(AlbionIcons.Add, null, Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    SettingsShortcut(navigator::openSettings)
                }
            })
        }
        if (importError != null) item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                FieldError(importError)
                TextButton(onClick = launchPicker) { Text("Выбрать другой файл") }
            }
        }
        if (continueBook != null && query.isBlank() && filter == "Все") item(span = { GridItemSpan(maxLineSpan) }) {
            ContinueReading(continueBook, Modifier.appear(2)) { navigator.open(ReaderRoute(continueBook.book.id)) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.appear(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.marat.albion.ui.browse.SearchField(query, { query = it }, "Поиск книги")
                OptionPicker("Статус чтения", filters, filters.indexOf(filter), { filter = filters[it] })
                val sorts = listOf("Недавние", "По названию", "По прогрессу")
                OptionPicker("Порядок книг", sorts, sorts.indexOf(sort), { sort = sorts[it] })
            }
        }
        if (books == null) item(span = { GridItemSpan(maxLineSpan) }) { LoadingState("Загружаю библиотеку") }
        val rows = if (query.isBlank() && filter == "Все") shown.filter { it.book.id != continueBook?.book?.id } else shown
        if (books != null && shown.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(if (list.isEmpty()) "Полка пока пустая" else "Книги не найдены",
                    if (list.isEmpty()) "Добавьте книгу: TXT, EPUB или PDF. Слова из неё можно переводить и сохранять." else "Измените запрос или сбросьте фильтр.")
                if (list.isNotEmpty()) TextButton(onClick = { query = ""; filter = "Все"; scope.launch { shelfState.scrollToItem(0) } }) { Text("Показать все книги") }
            }
        }
        if (rows.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
                Text(if (continueBook != null && query.isBlank() && filter == "Все") "Другие книги" else "Книги", Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge)
                Text("${rows.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (rows.any { it.book.source == "user" }) item(span = { GridItemSpan(maxLineSpan) }) {
            com.marat.albion.ui.components.CoachMark("library.longpress", "Удерживайте обложку своей книги, чтобы удалить её")
        }
        items(rows, key = { it.book.id }) { b ->
            BookTile(b, Modifier.animateItem(), { navigator.open(ReaderRoute(b.book.id)) }, if (b.book.source == "user") ({ toDelete = b }) else null)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 12.dp)) {
                GhostButton("Добавить книгу", launchPicker, icon = AlbionIcons.Add)
                Text("TXT · EPUB · PDF", Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Disclosure("Офлайн-перевод", model.summary) { TranslationModelControls() }
                Disclosure("Подсказки по чтению", "Перевод касанием, выделение и заметки", icon = AlbionIcons.HelpOutline) {
                    Text("Коснитесь слова — откроется перевод. Долгое нажатие выбирает предложение, протяжка — любой фрагмент. " +
                        "В панели перевода можно выбрать значение и сохранить слово в карточки. В меню читалки — поиск, закладки, заметки и оформление. " +
                        "Удерживайте обложку своей книги, чтобы удалить её.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    toDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Удалить книгу?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("«${b.book.title}», закладки, выделения и заметки к ней будут удалены из библиотеки.") },
            confirmButton = {
                TextButton(onClick = { toDelete = null; scope.launch { c.library.delete(b.book.id) } }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        )
    }
}

/** «Продолжить чтение»: инверсный блок с обложкой, процентом и кнопкой. */
@Composable
private fun ContinueReading(item: LibraryBook, modifier: Modifier, onOpen: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    com.marat.albion.ui.components.BoundVolume(modifier.fillMaxWidth().pressScale(interaction, 0.98f).clip(MaterialTheme.shapes.extraLarge)
        .clickable(interaction, null, onClickLabel = "Читать", onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BookCover(item.book.title, Modifier.size(width = 76.dp, height = 112.dp), inverse = true)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Продолжить", style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.7f))
                Spacer(Modifier.height(4.dp))
                Text(item.book.title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (item.book.author.isNotBlank()) Text(item.book.author, style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f), maxLines = 1)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ThinProgress(item.progress, Modifier.weight(1f), color = LocalContentColor.current,
                        track = LocalContentColor.current.copy(alpha = 0.18f), height = 4.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("${(item.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        PrimaryButton("Читать", onOpen, trailingIcon = AlbionIcons.ArrowForward, inverse = true)
    }
}

/** Книга на полке: обложка 2:3, название и прогресс. Долгое нажатие на свою книгу — удаление. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BookTile(item: LibraryBook, modifier: Modifier, onOpen: () -> Unit, onDelete: (() -> Unit)?) {
    val interaction = remember { MutableInteractionSource() }
    val feedback = com.marat.albion.ui.theme.LocalFeedback.current
    var menu by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().pressScale(interaction, 0.95f)
        .combinedClickable(interaction, indication = null, onClick = onOpen,
            onLongClick = onDelete?.let { { feedback.reveal(); menu = true } }, onLongClickLabel = onDelete?.let { "Удалить книгу" })) {
        Box {
            BookCover(item.book.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
            if (item.book.level.isNotBlank()) LevelBadge(item.book.level, Modifier.align(Alignment.TopEnd).padding(8.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape))
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Удалить") }, onClick = { menu = false; onDelete?.invoke() },
                    leadingIcon = { Icon(AlbionIcons.Delete, null, Modifier.size(18.dp)) })
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(item.book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (item.book.author.isNotBlank()) Text(item.book.author, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (item.progress > 0f) {
            Spacer(Modifier.height(8.dp))
            ThinProgress(item.progress, height = 3.dp)
        }
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
        // На миниатюре сфера меньше и выше, чтобы не заходить под название.
        if (thumbnail) OrbitalField(Modifier.fillMaxWidth(.56f).aspectRatio(1f).align(Alignment.TopCenter).padding(top = 10.dp), color = fg, animated = false)
        else OrbitalField(Modifier.fillMaxWidth().aspectRatio(1f).align(Alignment.Center).padding(8.dp), color = fg, animated = false)
        if (maxWidth >= 100.dp) SignalLabel("ALBION / READ", Modifier.align(Alignment.TopStart).padding(14.dp), color = fg.copy(alpha = .6f))
        // Полупрозрачная плашка на акцентной плите легла бы вторым слоем и дала полосу — подложка только у непрозрачной обложки.
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().then(if (inverse) Modifier else Modifier.background(bg)).padding(if (thumbnail) 10.dp else 14.dp)) {
            Box(Modifier.width(24.dp).height(1.dp).background(fg.copy(alpha = .5f)))
            Spacer(Modifier.height(if (thumbnail) 6.dp else 8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = fg, maxLines = if (thumbnail) 2 else 4, overflow = TextOverflow.Ellipsis)
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
