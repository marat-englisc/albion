package com.marat.albion.ui.library

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.marat.albion.ui.theme.paperSurface
import com.marat.albion.ui.components.AlbionIcons
import com.marat.albion.ui.components.gutter
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.style.TextAlign
import com.marat.albion.ui.components.FolioDivider
import com.marat.albion.ui.components.RibbonBookmark
import com.marat.albion.ui.components.romanNumeral
import com.marat.albion.ui.theme.headingFamily
import com.marat.albion.ui.theme.LocalReducedMotion
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import com.marat.albion.data.db.HighlightEntity
import com.marat.albion.data.library.WordMarker
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import com.marat.albion.AppContainer
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.db.BookmarkEntity
import com.marat.albion.data.library.BookContent
import com.marat.albion.data.library.BookPosition
import com.marat.albion.data.library.BookWordSource
import com.marat.albion.data.content.Deck
import com.marat.albion.ui.nav.DeckRoute
import com.marat.albion.data.settings.Settings
import com.marat.albion.ui.components.AlbionLogo
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.components.ThinSlider
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Reading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** Высота нижней строки читалки (вместе с разделительной линией). */
private val FOOTER_HEIGHT = 77.dp

/** Выделение на странице: блок и диапазон символов внутри абзаца. */
data class Selection(val block: Int, val start: Int, val end: Int, val isWord: Boolean)

class ReaderViewModel(private val c: AppContainer, val bookId: String) : ViewModel() {
    var book by mutableStateOf<BookContent?>(null)
        private set
    var title by mutableStateOf("")
        private set
    var pages by mutableStateOf<List<Page>>(emptyList())
        private set
    var pagesKey by mutableStateOf("")
        private set
    var startPosition by mutableStateOf<BookPosition?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set

    val bookmarks = c.library.observeBookmarks(bookId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val highlights = c.library.observeHighlights(bookId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Подсветка слов из начатых карточек; пересоздаётся, когда меняется прогресс. */
    var marker by mutableStateOf<WordMarker?>(null)
        private set
    private var marks = HashMap<Long, List<IntRange>>()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val forms = c.dictionary.irregularForms()
            c.library.observeStudiedTerms().distinctUntilChanged().collect { terms ->
                val m = WordMarker(terms, forms)
                withContext(Dispatchers.Main) { marks = HashMap(); marker = m }
            }
        }
    }

    /** Диапазоны подсвечиваемых слов в абзаце (кэшируются). */
    fun marks(chapter: Int, paragraph: Int, text: String): List<IntRange> {
        val m = marker ?: return emptyList()
        return marks.getOrPut((chapter.toLong() shl 32) or paragraph.toLong()) { m.ranges(text) }
    }

    fun addHighlight(chapter: Int, paragraph: Int, start: Int, end: Int, text: String, note: String) {
        viewModelScope.launch { c.library.addHighlight(bookId, chapter, paragraph, start, end, text, note) }
    }

    fun updateNote(id: Long, note: String) {
        viewModelScope.launch { c.library.updateHighlightNote(id, note) }
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch { c.library.deleteHighlight(id) }
    }

    init {
        viewModelScope.launch {
            title = c.library.book(bookId)?.title.orEmpty()
            book = c.library.content(bookId)
            startPosition = c.library.position(bookId) ?: BookPosition(0, 0, 0)
            loaded = true
        }
    }

    fun setPages(key: String, list: List<Page>) {
        pagesKey = key
        pages = list
    }

    fun savePosition(pos: BookPosition, progress: Float) {
        startPosition = pos
        viewModelScope.launch { c.library.savePosition(bookId, pos, progress) }
    }

    fun toggleBookmark(page: Page, next: Page?, snippet: String) {
        val existing = bookmarks.value.filter { b ->
            val p = BookPosition(b.chapter, b.paragraph, b.offset)
            p >= page.start && (next == null || p < next.start)
        }
        viewModelScope.launch {
            if (existing.isNotEmpty()) existing.forEach { c.library.deleteBookmark(it.id) }
            else c.library.addBookmark(bookId, page.start, snippet)
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { c.library.deleteBookmark(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(bookId: String, navigator: Navigator) {
    val c = LocalAppContainer.current
    val vm = viewModel(key = "reader-$bookId") { ReaderViewModel(c, bookId) }
    val settings by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val highlights by vm.highlights.collectAsStateWithLifecycle()
    var showHighlights by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var sizeMenu by remember { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<Selection?>(null) }
    var selectionPage by remember { mutableStateOf(-1) }

    val scale = settings.readerTextScale
    val body = TextStyle(fontFamily = Reading, fontSize = (18 * scale).sp, lineHeight = (29 * scale).sp, color = Ink.Text)
    val heading = TextStyle(fontFamily = headingFamily(vm.title), fontSize = (32 * scale).sp, lineHeight = (39 * scale).sp, color = Ink.Fg, textAlign = TextAlign.Center)
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val density = LocalDensity.current
    val book = vm.book
    val reduced = LocalReducedMotion.current

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().paperSurface()) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).heightIn(min = 64.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            com.marat.albion.ui.components.RoundIconButton(AlbionIcons.ArrowBack, "Назад", navigator::back)
            Text("ALBION / THE READING ROOM", style = AlbionType.eyebrow.copy(fontSize = 10.sp, lineHeight = 16.sp, letterSpacing = 1.sp),
                color = Ink.Muted, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
            com.marat.albion.ui.components.RoundIconButton(AlbionIcons.List, "Оглавление", { showToc = true })
            Box {
                com.marat.albion.ui.components.RoundIconButton(AlbionIcons.MoreHoriz, "Меню чтения", { sizeMenu = true })
                DropdownMenu(expanded = sizeMenu, onDismissRequest = { sizeMenu = false }, containerColor = Ink.Raised) {
                    DropdownMenuItem(text = { Text("Закладки") }, leadingIcon = { Icon(AlbionIcons.Bookmarks, null) },
                        onClick = { sizeMenu = false; showBookmarks = true })
                    DropdownMenuItem(text = { Text("Выделения и заметки") }, leadingIcon = { Icon(AlbionIcons.BorderColor, null) },
                        onClick = { sizeMenu = false; showHighlights = true })
                    Hairline()
                    Column(Modifier.width(240.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Eyebrow("Размер текста · ${(scale * 100).toInt()}%")
                        ThinSlider(
                            value = scale,
                            onValueChange = { v -> scope.launch { c.settings.setReaderTextScale(v) } },
                            valueRange = 0.8f..1.5f,
                            steps = 13,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Мои слова", style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
                                Text("пунктир под словами из карточек, которые вы учите", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                            }
                            Switch(
                                checked = settings.readerMarkWords,
                                onCheckedChange = { v -> scope.launch { c.settings.setReaderMarkWords(v) } },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Ink.Fg, checkedThumbColor = Ink.Bg,
                                    uncheckedTrackColor = Ink.Line, uncheckedThumbColor = Ink.Muted, uncheckedBorderColor = Ink.LineStrong,
                                ),
                            )
                        }
                    }
                }
            }
        }

        Hairline(color = Ink.Gold.copy(alpha = 0.65f))

        if (!vm.loaded || book == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (vm.loaded) Text("Книга не найдена", color = Ink.Muted) else AlbionLogo(size = 36.dp, color = Ink.Faint)
            }
            return@Column
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val padH = 26.dp
            val padV = 20.dp
            // Широкий горизонтальный экран — разворот из двух страниц, как в бумажной книге.
            val columns = if (maxWidth >= 840.dp && maxWidth > maxHeight) 2 else 1
            val columnGap = 56.dp
            val widthPx = with(density) { ((maxWidth - padH * 2 - columnGap * (columns - 1)) / columns).roundToPx() }
            // Нижняя строка (глава, номер страницы, закладка) живёт в той же области — вычитаем её высоту.
            val navInset = WindowInsets.navigationBars.getBottom(density)
            val footerPx = with(density) { FOOTER_HEIGHT.roundToPx() } + navInset
            val heightPx = with(density) { (maxHeight - padV * 2).roundToPx() } - footerPx
            val gapPx = with(density) { (12 * scale).dp.roundToPx() }
            val headingGapPx = with(density) { (24 * scale).dp.roundToPx() }
            val headingDecoration = 42.dp
            val decorationPx = with(density) { headingDecoration.roundToPx() }
            val dropCaps = maxWidth / columns >= 360.dp && density.fontScale <= 1.3f && scale <= 1.2f
            val key = "reading-room-initial-v2:$widthPx:$heightPx:$scale:$columns:$dropCaps:${density.fontScale}"

            LaunchedEffect(key, book) {
                if (vm.pagesKey == key && vm.pages.isNotEmpty()) return@LaunchedEffect
                val list = withContext(Dispatchers.Default) {
                    Paginator.paginate(book, measurer, widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), body, heading, gapPx, headingGapPx,
                        edition = true, dropCaps = dropCaps, headingDecoration = decorationPx)
                }
                vm.setPages(key, list)
            }

            val pages = vm.pages
            if (pages.isEmpty() || vm.pagesKey != key) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    AlbionLogo(size = 36.dp, color = Ink.Faint)
                    Spacer(Modifier.height(14.dp))
                    Eyebrow("Готовлю страницы")
                }
                return@BoxWithConstraints
            }

            // Пейджер листает развороты: при columns = 1 разворот — это одна страница.
            val spreads = (pages.size + columns - 1) / columns
            val initial = remember(key) { Paginator.pageOf(pages, vm.startPosition ?: BookPosition(0, 0, 0)) / columns }
            // Новое разбиение (другой шрифт или размер окна) — новый пейджер, открытый на том же месте.
            val pager = remember(key) { PagerState(currentPage = initial) { spreads } }

            // Сохраняем место чтения при перелистывании.
            LaunchedEffect(pager, pages) {
                snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { i ->
                    val page = pages.getOrNull(i * columns) ?: return@collect
                    vm.savePosition(page.start, Paginator.progressOf(book, page.start, isLastPage = i == spreads - 1))
                    if (selectionPage / columns != i) selection = null
                }
            }

            // Переход по оглавлению/закладке.
            var jumpTo by remember { mutableStateOf<BookPosition?>(null) }
            LaunchedEffect(jumpTo) {
                jumpTo?.let {
                    val target = Paginator.pageOf(pages, it) / columns
                    if (reduced) pager.scrollToPage(target) else pager.animateScrollToPage(target)
                    jumpTo = null
                }
            }

            Column(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 1,
                ) { index ->
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            // Смещение читаем на этапе отрисовки: при листании страницы не перекомпоновываются каждый кадр.
                            val offset = pager.currentPageOffsetFraction + (pager.currentPage - index)
                            // Уходящая страница слегка уменьшается и тускнеет — ощущение глубины.
                            val f = abs(offset).coerceIn(0f, 1f)
                            val s = if (reduced) 1f else 1f - 0.012f * f
                            scaleX = s; scaleY = s
                            alpha = if (reduced) 1f else 1f - 0.22f * f
                        }.padding(horizontal = padH, vertical = padV),
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(columnGap)) {
                            for (column in 0 until columns) {
                                val pageIndex = index * columns + column
                                Box(Modifier.weight(1f).fillMaxHeight()) {
                                    val page = pages.getOrNull(pageIndex) ?: return@Box
                                    PageView(
                                        book = book,
                                        page = page,
                                        body = body,
                                        heading = heading,
                                        gap = (12 * scale).dp,
                                        headingGap = (24 * scale).dp,
                                        headingDecoration = headingDecoration, dropCaps = dropCaps,
                                        selection = if (selectionPage == pageIndex) selection else null,
                                        onSelect = { sel -> selectionPage = pageIndex; selection = sel },
                                        highlights = highlights,
                                        marks = if (settings.readerMarkWords && vm.marker != null) vm::marks else null,
                                    )
                                }
                            }
                        }
                    }
                }

                // Нижняя строка: глава, номер страницы, закладка (на развороте — для обеих страниц).
                val firstPage = pager.currentPage * columns
                val lastPage = minOf(firstPage + columns, pages.size) - 1
                val current = pages.getOrNull(firstPage)
                val next = pages.getOrNull(lastPage + 1)
                val marked = current != null && bookmarks.any { b ->
                    val p = BookPosition(b.chapter, b.paragraph, b.offset)
                    p >= current.start && (next == null || p < next.start)
                }
                FolioDivider(Modifier.height(16.dp))
                Hairline(color = Ink.Gold.copy(alpha = 0.65f))
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(FOOTER_HEIGHT - 17.dp).padding(start = 24.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            current?.let { "ALBION / LONDON / %03d".format(it.blocks.first().chapter + 1) }.orEmpty(),
                            style = AlbionType.eyebrow.copy(letterSpacing = 1.sp),
                            color = Ink.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        ThinProgress((lastPage + 1f) / pages.size, track = Ink.Line)
                    }
                    Spacer(Modifier.width(14.dp))
                    val pageLabel = if (lastPage > firstPage) "${firstPage + 1}–${lastPage + 1}" else "${firstPage + 1}"
                    Text("$pageLabel / ${pages.size}", style = MaterialTheme.typography.bodySmall.copy(color = Ink.Soft))
                    IconButton(onClick = {
                        current?.let { page ->
                            val snippet = page.blocks.firstOrNull { !it.isHeading }?.let { b ->
                                book.chapters[b.chapter].paragraphs[b.paragraph].substring(b.start, b.end)
                            } ?: book.chapters[page.blocks.first().chapter].title
                            vm.toggleBookmark(page, next, snippet)
                        }
                    }) {
                        Icon(if (marked) AlbionIcons.Bookmark else AlbionIcons.BookmarkBorder, "Закладка", tint = Ink.Accent)
                    }
                }
            }

            if (showToc) {
                ListSheet(title = "Оглавление", onDismiss = { showToc = false }) {
                    itemsIndexed(book.chapters) { i, ch ->
                        SheetRow(primary = ch.title, secondary = "Страница ${pages.indexOfFirst { it.blocks.first().chapter == i } + 1}") {
                            showToc = false
                            jumpTo = BookPosition(i, 0, 0)
                        }
                    }
                }
            }
            if (showHighlights) {
                ListSheet(title = "Выделения и заметки", onDismiss = { showHighlights = false }) {
                    if (highlights.isEmpty()) {
                        item {
                            Text(
                                "Пока пусто. Коснитесь слова или выделите фрагмент (долгое нажатие и протяжка), " +
                                    "затем нажмите «Выделить» или «Заметка».",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink.Muted,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                    itemsIndexed(highlights, key = { _, h -> h.id }) { _, h ->
                        HighlightRow(
                            h,
                            page = Paginator.pageOf(pages, BookPosition(h.chapter, h.paragraph, h.start)) + 1,
                            chapter = book.chapters.getOrNull(h.chapter)?.title.orEmpty(),
                            onOpen = { showHighlights = false; jumpTo = BookPosition(h.chapter, h.paragraph, h.start) },
                            onDelete = { vm.deleteHighlight(h.id) },
                        )
                    }
                }
            }
            if (showBookmarks) {
                ListSheet(title = "Закладки", onDismiss = { showBookmarks = false }) {
                    if (bookmarks.isEmpty()) {
                        item {
                            Text(
                                "Пока пусто. Нажмите на значок закладки внизу страницы, чтобы отметить место.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink.Muted,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                    itemsIndexed(bookmarks, key = { _, b -> b.id }) { _, b ->
                        BookmarkRow(b, page = Paginator.pageOf(pages, BookPosition(b.chapter, b.paragraph, b.offset)) + 1, chapter = book.chapters.getOrNull(b.chapter)?.title.orEmpty(),
                            onOpen = { showBookmarks = false; jumpTo = BookPosition(b.chapter, b.paragraph, b.offset) },
                            onDelete = { vm.deleteBookmark(b.id) },
                        )
                    }
                }
            }
        }
    }

    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = FOOTER_HEIGHT, start = 12.dp, end = 12.dp)) { data ->
        Snackbar(data, containerColor = Ink.Fg, contentColor = Ink.Bg, actionColor = Ink.Bg,
            dismissActionContentColor = Ink.Bg)
    }
    }

    // Окно перевода для выделенного слова или фрагмента.
    val sel = selection
    val pages = vm.pages
    if (sel != null && book != null && selectionPage in pages.indices) {
        val block = pages[selectionPage].blocks.getOrNull(sel.block)
        if (block != null && !block.isHeading) {
            val paragraph = book.chapters[block.chapter].paragraphs[block.paragraph]
            val absStart = block.start + sel.start
            val absEnd = block.start + sel.end
            val text = paragraph.substring(absStart, absEnd).trim()
            if (text.isNotEmpty()) {
                // Выделение, которого касается выбранный фрагмент, — чтобы показать заметку.
                val existing = highlights.firstOrNull { h ->
                    h.chapter == block.chapter && h.paragraph == block.paragraph && h.start < absEnd && absStart < h.end
                }
                TranslationSheet(
                    text = text,
                    isWord = sel.isWord,
                    onDismiss = { selection = null },
                    highlight = existing,
                    onHighlight = { note -> vm.addHighlight(block.chapter, block.paragraph, absStart, absEnd, text, note) },
                    onNote = { h, note -> vm.updateNote(h.id, note) },
                    onDeleteHighlight = { h -> vm.deleteHighlight(h.id) },
                    bookSource = BookWordSource(bookId, vm.title, book.chapters[block.chapter].title),
                    bookExample = paragraph.substring(
                        sentenceAt(paragraph, absStart).first,
                        sentenceAt(paragraph, (absEnd - 1).coerceAtLeast(absStart)).last + 1,
                    ).trim(),
                    onOpenBookWords = { selection = null; navigator.open(DeckRoute(Deck.BOOK_WORDS)) },
                    onBookWordSaved = { saved ->
                        selection = null
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val result = snackbar.showSnackbar(
                                if (saved.added) "«${saved.card.term}» — в «Слова из книг»" else "Это значение уже есть в «Слова из книг».",
                                actionLabel = if (saved.added) "Отменить" else null,
                                withDismissAction = true, duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                try {
                                    if (!c.bookWords.undoAdd(saved)) snackbar.showSnackbar(
                                        "Отмена уже недоступна. Карточка осталась в колоде.",
                                        withDismissAction = true, duration = SnackbarDuration.Long)
                                } catch (e: CancellationException) { throw e }
                                catch (e: Exception) { snackbar.showSnackbar("Не удалось отменить сохранение. Карточка осталась в колоде.", withDismissAction = true) }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PageView(
    book: BookContent,
    page: Page,
    body: TextStyle,
    heading: TextStyle,
    gap: androidx.compose.ui.unit.Dp,
    headingGap: androidx.compose.ui.unit.Dp,
    headingDecoration: androidx.compose.ui.unit.Dp,
    dropCaps: Boolean,
    selection: Selection?,
    onSelect: (Selection?) -> Unit,
    highlights: List<HighlightEntity>,
    marks: ((Int, Int, String) -> List<IntRange>)?,
) {
    Column(Modifier.fillMaxWidth()) {
        page.blocks.forEachIndexed { i, block ->
            if (block.isHeading) {
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().height(headingDecoration), contentAlignment = Alignment.Center) {
                            FolioDivider(label = romanNumeral(block.chapter + 1))
                        }
                        Text(readingChapterTitle(book.chapters[block.chapter].title), style = heading, modifier = Modifier.fillMaxWidth())
                    }
                    RibbonBookmark(Modifier.align(Alignment.TopEnd))
                }
                Spacer(Modifier.height(headingGap))
            } else {
                if (i > 0 && !page.blocks[i - 1].isHeading) Spacer(Modifier.height(gap))
                val paragraph = book.chapters[block.chapter].paragraphs[block.paragraph]
                val text = paragraph.substring(block.start, block.end)
                // Выделения и подсвеченные слова — в координатах этого куска абзаца.
                val marked = highlights.filter { it.chapter == block.chapter && it.paragraph == block.paragraph }
                    .mapNotNull { h -> clip(h.start, h.end, block.start, block.end) }
                val words = marks?.invoke(block.chapter, block.paragraph, paragraph).orEmpty()
                    .mapNotNull { r -> clip(r.first, r.last + 1, block.start, block.end) }
                SelectableParagraph(
                    text = text,
                    style = body,
                    dropCap = dropCaps && block.paragraph == 0 && block.start == 0,
                    selection = selection?.takeIf { it.block == i },
                    onSelect = { range, isWord -> onSelect(range?.let { Selection(i, it.first, it.last + 1, isWord) }) },
                    highlights = marked,
                    marks = words,
                )
            }
        }
    }
}

/**
 * Абзац с жестами: касание — слово, долгое нажатие — предложение,
 * долгое нажатие с протягиванием — произвольный фрагмент.
 */
@Composable
private fun SelectableParagraph(
    text: String,
    style: TextStyle,
    dropCap: Boolean = false,
    selection: Selection?,
    onSelect: (IntRange?, Boolean) -> Unit,
    highlights: List<IntRange> = emptyList(),
    marks: List<IntRange> = emptyList(),
) {
    val marker = Ink.Fg.copy(alpha = 0.10f)
    val dots = Ink.Muted
    val density = LocalDensity.current
    // Читаем списки через состояние: выделения приходят из БД позже первого кадра,
    // и отрисовка должна обновиться сама.
    val highlightRanges by rememberUpdatedState(highlights)
    val markRanges by rememberUpdatedState(marks)
    var dragRange by remember { mutableStateOf<IntRange?>(null) }
    val shown = dragRange ?: selection?.let { it.start until it.end }

    val annotated: AnnotatedString = remember(text, shown, style.color) {
        buildAnnotatedString {
            append(text)
            shown?.let { r ->
                val s = r.first.coerceIn(0, text.length)
                val e = (r.last + 1).coerceIn(s, text.length)
                addStyle(SpanStyle(background = Ink.High, color = Ink.Fg, textDecoration = TextDecoration.Underline), s, e)
            }
        }
    }

    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth
        val layout = remember(annotated, style, width, dropCap) { measureEditionParagraph(measurer, annotated, style, width, dropCap) }
        val currentLayout by rememberUpdatedState(layout)
        Canvas(Modifier.fillMaxWidth().height(with(density) { layout.height.toDp() })
            .semantics { this.text = AnnotatedString(text) }
            .drawBehind {
                fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMarkers(part: EditionTextPart) {
                    val from = part.start
                    val until = from + part.layout.layoutInput.text.length
                    translate(part.origin.x, part.origin.y) {
                        highlightRanges.forEach { range ->
                            val first = maxOf(range.first, from); val end = minOf(range.last + 1, until)
                            if (end > first) drawPath(part.layout.getPathForRange(first-from, end-from), marker)
                        }
                        val offsetY = 3.dp.toPx()
                        val stroke = 1.3.dp.toPx()
                        val dash = PathEffect.dashPathEffect(floatArrayOf(stroke * 2, stroke * 2))
                        markRanges.forEach { range ->
                            val first = maxOf(range.first, from); val end = minOf(range.last + 1, until)
                            if (end > first) {
                                val firstLine = part.layout.getLineForOffset(first-from)
                                val lastLine = part.layout.getLineForOffset(end-from-1)
                                for (line in firstLine..lastLine) {
                                    val x1 = if (line == firstLine) part.layout.getHorizontalPosition(first-from, true) else part.layout.getLineLeft(line)
                                    val endX = if (line == lastLine) part.layout.getHorizontalPosition(end-from, true) else part.layout.getLineRight(line)
                                    val x2 = if (endX <= x1) part.layout.getLineRight(line) else endX
                                    val y = part.layout.getLineBaseline(line) + offsetY
                                    drawLine(dots, Offset(x1,y), Offset(x2,y), stroke, pathEffect = dash)
                                }
                            }
                        }
                    }
                }
                layout.parts.forEach { part ->
                    val frame = layout.initialFrame
                    if (part.start == 0 && frame != null) {
                        clipRect(frame.left, frame.top, frame.right, frame.bottom) { drawMarkers(part) }
                    } else drawMarkers(part)
                }
            }.pointerInput(text) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var timedOut = false
                    val up = try {
                        withTimeout(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation() }
                    } catch (e: PointerEventTimeoutCancellationException) {
                        timedOut = true
                        null
                    }
                    val l = currentLayout
                    if (!timedOut) {
                        // Короткое касание; null — жест забрал пейджер (листание).
                        if (up != null) {
                            val o = l.offsetAt(up.position)
                            onSelect(wordAt(text, o), true)
                        }
                        return@awaitEachGesture
                    }
                    // Долгое нажатие: предложение или протягивание.
                    val anchor = l.offsetAt(down.position)
                    val anchorWord = wordAt(text, anchor)
                    dragRange = sentenceAt(text, anchor)
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                        if (moved && anchorWord != null) {
                            val cur = wordAt(text, l.offsetAt(change.position)) ?: continue
                            dragRange = minOf(anchorWord.first, cur.first)..maxOf(anchorWord.last, cur.last)
                        }
                    }
                    val result = dragRange
                    dragRange = null
                    val isWord = result != null && anchorWord != null && result == anchorWord
                    onSelect(result, isWord)
                }
            },
        ) {
            layout.parts.forEach { part ->
                val frame = layout.initialFrame
                if (part.start == 0 && frame != null) {
                    clipRect(frame.left, frame.top, frame.right, frame.bottom) {
                        drawText(part.layout, topLeft = part.origin)
                    }
                } else drawText(part.layout, topLeft = part.origin)
            }
            layout.initialFrame?.let { drawInitialOrnament(it) }
        }
    }
}

private fun isWordChar(c: Char) = c.isLetterOrDigit() || c == '\'' || c == '’' || c == '-'

/** Слово вокруг позиции (без кавычек и тире по краям). */
fun wordAt(text: String, offset: Int): IntRange? {
    if (text.isEmpty()) return null
    var i = offset.coerceIn(0, text.length - 1)
    if (!isWordChar(text[i]) && i > 0 && isWordChar(text[i - 1])) i--
    if (!isWordChar(text[i])) return null
    var s = i
    var e = i
    while (s > 0 && isWordChar(text[s - 1])) s--
    while (e < text.length - 1 && isWordChar(text[e + 1])) e++
    while (s <= e && !text[s].isLetterOrDigit()) s++
    while (e >= s && !text[e].isLetterOrDigit()) e--
    return if (s <= e) s..e else null
}

/** Предложение вокруг позиции — по точке, вопросительному и восклицательному знакам. */
fun sentenceAt(text: String, offset: Int): IntRange {
    if (text.isEmpty()) return 0..0
    val o = offset.coerceIn(0, text.length - 1)
    var s = o
    while (s > 0) {
        val prev = text[s - 1]
        if ((prev == '.' || prev == '!' || prev == '?' || prev == '…') && (s >= text.length || text[s] == ' ')) break
        s--
    }
    while (s < o && text[s] == ' ') s++
    var e = o
    while (e < text.length - 1) {
        val ch = text[e]
        if ((ch == '.' || ch == '!' || ch == '?' || ch == '…') && (e + 1 >= text.length || text[e + 1] == ' ' || text[e + 1] == '"' || text[e + 1] == '”')) break
        e++
    }
    while (e + 1 < text.length && (text[e + 1] == '"' || text[e + 1] == '”' || text[e + 1] == '’')) e++
    return s..e
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListSheet(title: String, onDismiss: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink.Raised,
        dragHandle = { Hairline(Modifier.padding(top = 10.dp, bottom = 6.dp).width(36.dp), color = Ink.LineStrong) },
    ) {
        Column(Modifier.padding(horizontal = gutter).navigationBarsPadding()) {
            Eyebrow(title, Modifier.padding(vertical = 10.dp))
            LazyColumn(Modifier.fillMaxWidth().padding(bottom = 16.dp), content = content)
        }
    }
}

@Composable
private fun SheetRow(primary: String, secondary: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(primary, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Reading), color = Ink.Fg, modifier = Modifier.weight(1f))
            Text(secondary, style = AlbionType.index)
        }
        Hairline()
    }
}

/** Часть диапазона [start, end) абзаца, попавшая в кусок [from, to), — в координатах куска. */
private fun clip(start: Int, end: Int, from: Int, to: Int): IntRange? {
    val s = maxOf(start, from) - from
    val e = minOf(end, to) - from
    return if (s < e) s until e else null
}

@Composable
private fun HighlightRow(h: HighlightEntity, page: Int, chapter: String, onOpen: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Eyebrow("$chapter · стр. $page")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(Ink.Fg))
                    Spacer(Modifier.width(10.dp))
                    Text(h.text, style = TextStyle(fontFamily = Reading, fontSize = 17.sp, lineHeight = 24.sp), color = Ink.Fg, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
                if (h.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(h.note, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
                }
            }
            IconButton(onClick = onDelete) { Icon(AlbionIcons.Close, "Удалить выделение", tint = Ink.Muted) }
        }
        Hairline()
    }
}

@Composable
private fun BookmarkRow(b: BookmarkEntity, page: Int, chapter: String, onOpen: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Eyebrow("$chapter · стр. $page")
                Spacer(Modifier.height(6.dp))
                Text(b.snippet, style = MaterialTheme.typography.bodyMedium, color = Ink.Text, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onDelete) { Icon(AlbionIcons.Close, "Удалить закладку", tint = Ink.Muted) }
        }
        Hairline()
    }
}
