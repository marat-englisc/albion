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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    var importError by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<LibraryBook?>(null) }
    val reveal = rememberReveal()

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

    Column(Modifier.fillMaxSize().paperSurface()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 32.dp)) {
            item {
                Box(Modifier.reveal(reveal, 0)) {
                    ScreenHeader(
                        "Библиотека", eyebrow = "Чтение с переводом",
                        trailing = {
                            Row {
                                FeatureHelp("library", "Как читать", text =
                                    "Коснитесь слова, чтобы увидеть перевод и справку из карточек.\n\nУдерживайте палец на тексте для перевода предложения. Протяните палец, чтобы выбрать любой фрагмент.\n\nВ окне перевода можно выделить текст и добавить заметку. Закладки, заметки и размер текста доступны в панели читалки.\n\nСловарь работает сразу без интернета. Для перевода предложений приложение один раз скачает языковую модель, затем перевод тоже работает офлайн.")
                                SettingsShortcut(navigator::openSettings)
                            }
                        },
                    )
                }
            }

            val list = books.orEmpty()
            val continueBook = list.firstOrNull { it.lastRead != null && it.progress < 0.999f }
            if (continueBook != null) {
                item {
                    Column(Modifier.padding(top = 16.dp).reveal(reveal, 1)) {
                        PrimaryButton(
                            text = "Продолжить · ${continueBook.book.title}",
                            onClick = { navigator.open(ReaderRoute(continueBook.book.id)) },
                            trailingIcon = AlbionIcons.ArrowForward,

                            modifier = Modifier,
                        )
                    }
                }
            }

            item { SectionHeader("Книги", Modifier.padding(top = 12.dp).reveal(reveal, 2), trailing = "%02d".format(list.size)) }
            if (books != null && list.isEmpty()) item {
                EmptyState("Полка пока пустая", "Добавьте книгу в TXT, EPUB или PDF — слова из неё можно сразу переводить и сохранять.",
                    Modifier.reveal(reveal, 3))
            }
            itemsIndexed(list, key = { _, b -> b.book.id }) { i, b ->
                BookRow(
                    item = b,
                    modifier = Modifier.animateItem().reveal(reveal, 3 + i),
                    onOpen = { navigator.open(ReaderRoute(b.book.id)) },
                    onDelete = if (b.book.source == "user") ({ toDelete = b }) else null,
                )
            }

            item {
                Column(Modifier.padding(top = 20.dp).reveal(reveal, 4 + list.size)) {

                    if (list.isNotEmpty() && list.none { it.book.source == "user" }) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {

                            Column(Modifier.weight(1f)) {
                                Text("Читайте то, что нравится", style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
                                Spacer(Modifier.height(4.dp))
                                Text("Добавьте свою книгу — перевод слов и «Мои слова» работают в ней так же.",
                                    style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                            }
                        }
                    }
                    GhostButton(if (importing) "Открываю…" else "Добавить книгу · TXT, EPUB, PDF", onClick = {
                        if (!importing) picker.launch(arrayOf("text/plain", "text/markdown", "application/epub+zip", "application/pdf", "application/octet-stream"))
                    }, icon = AlbionIcons.Add)
                    importError?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Ink.Fg)
                    }
                }
            }

            item {
                Column(Modifier.padding(top = 8.dp).reveal(reveal, 5 + list.size)) {
                    SectionHeader("Офлайн-перевод")
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        if (model == ModelState.Missing || model == ModelState.Failed) {
                            Box(Modifier.size(40.dp).background(Ink.WarmSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(AlbionIcons.CloudOff, null, tint = Ink.Warm, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            modifier = Modifier.weight(1f),
                            text = when (model) {
                                ModelState.Ready -> "Слова и предложения готовы к переводу."
                                ModelState.Downloading -> "Словарь готов · загружается перевод предложений…"
                                ModelState.Failed -> "Словарь готов · перевод предложений пока недоступен."
                                else -> "Словарь готов · для загрузки перевода предложений нужен интернет."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Ink.Muted,
                        )
                    }
                    if (model == ModelState.Failed) {
                        Text(
                            "Повторить загрузку  →",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ink.Fg,
                            modifier = Modifier.clickable { scope.launch { c.translator.ensureModel() } }.padding(vertical = 12.dp),
                        )
                    }
                }
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
    Column(modifier.fillMaxWidth().pressScale(interaction, 0.985f).clickable(interaction, indication = null, onClick = onOpen)) {
        Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(item.book.title, Modifier.size(width = 64.dp, height = 92.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.book.title, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = headingFamily(item.book.title)), color = Ink.Fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOf(item.book.author, "${item.book.chapters} ${pluralRu(item.book.chapters, "глава", "главы", "глав")}").filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink.Muted,
                        maxLines = 1,
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
        modifier.background(cover, RoundedCornerShape(2.dp))

            .drawBehind {
                val gold = Color(0xFFC6A66D)
                drawRect(gold.copy(alpha = 0.55f), size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                val inset = 5.dp.toPx()
                drawRect(gold.copy(alpha = 0.65f), androidx.compose.ui.geometry.Offset(inset, inset),
                    androidx.compose.ui.geometry.Size((size.width - 2 * inset).coerceAtLeast(0f), (size.height - 2 * inset).coerceAtLeast(0f)),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(0.5.dp.toPx()))
            }
            .padding(start = 10.dp, end = 9.dp, top = 10.dp, bottom = 10.dp),
    ) {
        // Шрифт — такой, чтобы самое длинное слово названия не переносилось посередине.
        val measurer = androidx.compose.ui.text.rememberTextMeasurer()
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        val longest = remember(title) { title.split(' ').maxByOrNull { it.length }.orEmpty() }
        val fontSize = remember(title, widthPx, fontScale) {
            (10 downTo 7).firstOrNull { size ->
                measurer.measure(longest, TextStyle(fontFamily = Serif, fontSize = size.sp)).size.width <= widthPx
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
                        val measured = measurer.measure(monogram, TextStyle(fontFamily = Serif, fontSize = size.sp))
                        measured.size.width <= widthPx && measured.size.height <= heightPx
                    } ?: 6
                }
                Text(monogram, style = TextStyle(fontFamily = Serif, fontSize = monogramSize.sp), color = ink,
                    maxLines = 1, softWrap = false)
            } else {
                Text(
                    title,
                    style = TextStyle(fontFamily = Serif, fontSize = fontSize.sp, lineHeight = (fontSize * 1.2f).sp),
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
