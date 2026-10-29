package com.marat.albion.ui.library

import com.marat.albion.ui.theme.paperSurface
import com.marat.albion.ui.components.AlbionIcons
import com.marat.albion.ui.components.gutter
import com.marat.albion.ui.components.EmptyState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.FlowRow
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
import com.marat.albion.data.library.ModelState
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
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Serif
import com.marat.albion.ui.theme.headingFamily
import com.marat.albion.ui.theme.pressScale
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun LibraryScreen(navigator: Navigator) {
    val c = LocalAppContainer.current
    val books by remember { c.library.observeLibrary() }.collectAsStateWithLifecycle(null)
    val model by c.translator.model.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val shelfState = rememberLazyListState()
    var importError by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<LibraryBook?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Все") }
    var sort by rememberSaveable { mutableStateOf("Недавние") }
    var filterMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
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
    Column(Modifier.fillMaxSize().paperSurface()) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars).padding(horizontal = gutter, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Библиотека", Modifier.weight(1f), style = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
            SettingsShortcut(navigator::openSettings)
        }
        LazyColumn(Modifier.fillMaxSize(), state = shelfState, contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 32.dp)) {
            item {
                PrimaryButton(if (importing) "Открываю книгу…" else "Добавить", onClick = launchPicker, enabled = !importing, icon = AlbionIcons.Add)
                Text("TXT · EPUB · PDF", style = MaterialTheme.typography.bodySmall,
                    color = Ink.Muted, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                FieldError(importError)
                if (importError != null) TextButton(onClick = launchPicker) { Text("Выбрать другой файл") }
                OutlinedTextField(query, { query = it }, label = { Text("Поиск книги") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        TextButton(onClick = { filterMenu = true }) { Text("Статус: $filter ▾") }
                        DropdownMenu(filterMenu, { filterMenu = false }, containerColor = Ink.Raised) {
                            listOf("Все", "Не начаты", "Читаю", "Прочитаны").forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { filter = option; filterMenu = false })
                            }
                        }
                    }
                    Box {
                        TextButton(onClick = { sortMenu = true }) { Text("Порядок: $sort ▾") }
                        DropdownMenu(sortMenu, { sortMenu = false }, containerColor = Ink.Raised) {
                            listOf("Недавние", "По названию", "По прогрессу").forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { sort = option; sortMenu = false })
                            }
                        }
                    }
                }
            }
            if (books == null) item { LoadingState("Загружаю библиотеку") }
            if (continueBook != null && query.isBlank() && filter == "Все") item {
                CatalogueCard(label = "Продолжить", title = continueBook.book.title, compact = true,
                    description = "Прочитано ${(continueBook.progress * 100).toInt()}%", progress = continueBook.progress,
                    onClick = { navigator.open(ReaderRoute(continueBook.book.id)) })
            }
            val rows = if (query.isBlank() && filter == "Все") shown.filter { it.book.id != continueBook?.book?.id } else shown
            if (books != null && shown.isEmpty()) item {
                EmptyState(if (list.isEmpty()) "Полка пока пустая" else "Книги не найдены",
                    if (list.isEmpty()) "Добавьте книгу: слова из неё можно переводить и сохранять." else "Измените запрос или сбросьте фильтр.")
                if (list.isNotEmpty()) TextButton(onClick = { query = ""; filter = "Все"; scope.launch { shelfState.scrollToItem(0) } }) { Text("Показать все книги") }
            }
            if (rows.isNotEmpty()) item { SectionHeader(if (continueBook != null && query.isBlank() && filter == "Все") "Другие книги" else "Книги", trailing = "${rows.size}") }
            itemsIndexed(rows, key = { _, b -> b.book.id }) { _, b ->
                BookRow(b, Modifier, { navigator.open(ReaderRoute(b.book.id)) }, if (b.book.source == "user") ({ toDelete = b }) else null)
            }
            item {
                Disclosure("Офлайн-перевод", when(model) {
                    ModelState.Ready -> "Слова и предложения готовы"
                    ModelState.Downloading -> "Загружаю перевод предложений…"
                    else -> "Словарь готов · перевод предложений можно загрузить"
                }) {
                    Text("Словарь работает без интернета. Для перевода предложений один раз загрузите языковую модель.", style = MaterialTheme.typography.bodyMedium)
                    if (model == ModelState.Failed) FieldError("Не удалось загрузить модель. Проверьте интернет и свободное место.")
                    if (model == ModelState.Missing || model == ModelState.Failed) TextButton(onClick = { scope.launch { c.translator.ensureModel() } }) {
                        Text(if (model == ModelState.Failed) "Повторить загрузку" else "Загрузить перевод предложений")
                    }
                }
                FeatureHelp("library", "Как читать", text = "Коснитесь слова для перевода. Долгое нажатие выбирает предложение, протяжка — фрагмент. В панели перевода можно уточнить выделение и выбрать значение для сохранения. Меню читалки содержит поиск, закладки, заметки и оформление.")
            }
        }
    }

    toDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = Ink.Raised,
            title = { Text("Удалить книгу?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("«${b.book.title}», закладки, выделения и заметки к ней будут удалены из библиотеки.") },
            confirmButton = {
                TextButton(onClick = { toDelete = null; scope.launch { c.library.delete(b.book.id) } }) { Text("Удалить", color = Ink.Error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Отмена", color = Ink.Muted) } },
        )
    }
}

@Composable
private fun BookRow(item: LibraryBook, modifier: Modifier, onOpen: () -> Unit, onDelete: (() -> Unit)?) {
    val interaction = remember { MutableInteractionSource() }
    var menu by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().pressScale(interaction, 0.985f).clickable(interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onOpen)) {
        Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(item.book.title, Modifier.size(width = 64.dp, height = 92.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.book.title, style = MaterialTheme.typography.titleLarge.copy(fontFamily = headingFamily(item.book.title)), color = Ink.Fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOf(item.book.author, "${item.book.chapters} ${pluralRu(item.book.chapters, "глава", "главы", "глав")}").filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink.Muted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.book.level.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        LevelBadge(item.book.level)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ThinProgress(item.progress, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    Text("${(item.progress * 100).toInt()}%", style = AlbionType.index)
                }
            }
            if (onDelete != null) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(AlbionIcons.MoreHoriz, "Ещё", tint = Ink.Muted) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Ink.Raised) {
                        DropdownMenuItem(text = { Text("Удалить") }, onClick = { menu = false; onDelete() })
                    }
                }
            }
        }
        Hairline()
    }
}

/** Типографская обложка: название антиквой на «чернильной» плашке; на маленькой — монограмма. */
@Composable
fun BookCover(title: String, modifier: Modifier = Modifier) {

    val (cover, ink) = when (Math.floorMod(title.hashCode(), 3)) {
        0 -> Ink.Accent to Ink.OnAccent
        1 -> Color(0xFF24272B) to Ink.Ivory
        else -> Color(0xFF861F39) to Ink.Ivory
    }
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier.clearAndSetSemantics {}.background(cover, RoundedCornerShape(2.dp))

            .drawBehind {
                val gold = Color(0xFFC6A66D)
                val spine = 7.dp.toPx()
                drawRect(Color(0xFF66172C), size = androidx.compose.ui.geometry.Size(spine, size.height))
                for (baseY in listOf(11.dp.toPx(), size.height - 17.dp.toPx())) repeat(3) { i ->
                    val y = baseY + i * 2.5.dp.toPx()
                    drawLine(gold, androidx.compose.ui.geometry.Offset(1.5.dp.toPx(), y),
                        androidx.compose.ui.geometry.Offset(spine - 1.5.dp.toPx(), y), 0.7.dp.toPx())
                }
                val inset = 9.dp.toPx()
                drawRect(gold.copy(alpha = 0.65f), androidx.compose.ui.geometry.Offset(inset, inset),
                    androidx.compose.ui.geometry.Size((size.width - 2 * inset).coerceAtLeast(0f), (size.height - 2 * inset).coerceAtLeast(0f)),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(0.5.dp.toPx()))
            }
            .padding(start = 13.dp, end = 11.dp, top = 12.dp, bottom = 12.dp),
    ) {
        // Шрифт — такой, чтобы самое длинное слово названия не переносилось посередине.
        val measurer = androidx.compose.ui.text.rememberTextMeasurer()
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        val longest = remember(title) { title.split(' ').maxByOrNull { it.length }.orEmpty() }
        val fontSize = remember(title, widthPx, fontScale) {
            (10 downTo 7).firstOrNull { size ->
                measurer.measure(longest, TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Serif, fontSize = size.sp)).size.width <= widthPx
            }
        }
        val small = maxWidth < 44.dp || fontSize == null
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.width(14.dp).height(2.dp).background(ink.copy(alpha = 0.6f), RoundedCornerShape(1.dp)))
            if (small) {
                val monogram = title.split(' ')
                    .filter { it.isNotBlank() && it.lowercase() !in setOf("the", "a", "an", "of", "and") }
                    .firstOrNull()?.firstOrNull()?.uppercase().orEmpty()
                val monogramSize = remember(monogram, widthPx, heightPx, fontScale) {
                    (18 downTo 6).firstOrNull { size ->
                        val measured = measurer.measure(monogram, TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Serif, fontSize = size.sp))
                        measured.size.width <= widthPx && measured.size.height <= heightPx
                    } ?: 6
                }
                Text(monogram, style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Serif, fontSize = monogramSize.sp), color = ink,
                    maxLines = 1, softWrap = false)
            } else {
                Text(
                    title,
                    style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Serif, fontSize = fontSize.sp, lineHeight = (fontSize * 1.2f).sp),
                    color = ink,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                )
            }
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
