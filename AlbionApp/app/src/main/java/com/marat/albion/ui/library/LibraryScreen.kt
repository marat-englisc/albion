package com.marat.albion.ui.library

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
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

    Column(Modifier.fillMaxSize().background(Ink.Bg)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            item {
                Box(Modifier.reveal(reveal, 0)) {
                    ScreenHeader(
                        "Библиотека", eyebrow = "Чтение с переводом",
                        subtitle = "Коснитесь слова — увидите перевод и справку из ваших карточек. Удерживайте палец на тексте — переведётся " +
                            "предложение; протяните — выделите любой фрагмент.",
                        trailing = { SettingsShortcut(navigator::openSettings) },
                    )
                }
            }

            val list = books.orEmpty()
            val continueBook = list.firstOrNull { it.lastRead != null && it.progress < 0.999f }
            if (continueBook != null) {
                item {
                    Column(Modifier.padding(top = 28.dp).reveal(reveal, 1)) {
                        PrimaryButton(
                            text = "Продолжить · ${continueBook.book.title}",
                            onClick = { navigator.open(ReaderRoute(continueBook.book.id)) },
                            trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                        )
                    }
                }
            }

            item { SectionHeader("Книги", Modifier.padding(top = 24.dp).reveal(reveal, 2), trailing = "%02d".format(list.size)) }
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
                    GhostButton(if (importing) "Открываю…" else "Добавить книгу · TXT, EPUB, PDF", onClick = {
                        if (!importing) picker.launch(arrayOf("text/plain", "text/markdown", "application/epub+zip", "application/pdf", "application/octet-stream"))
                    }, icon = Icons.Rounded.Add)
                    importError?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Ink.Fg)
                    }
                }
            }

            item {
                Column(Modifier.padding(top = 8.dp).reveal(reveal, 5 + list.size)) {
                    SectionHeader("Перевод на устройстве")
                    Text(
                        "Слова переводит встроенный словарь EN → RU (62 тыс. статей, Викисловарь) — сразу и без интернета. " +
                            when (model) {
                                ModelState.Ready -> "Модель перевода фраз Google ML Kit загружена: предложения тоже переводятся офлайн."
                                ModelState.Downloading -> "Модель перевода фраз Google ML Kit загружается в фоне…"
                                else -> "Модель перевода фраз Google ML Kit скачается сама, как только появится интернет."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink.Muted,
                    )
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
            containerColor = Ink.Elevated,
            title = { Text("Удалить книгу?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("«${b.book.title}», закладки, выделения и заметки к ней будут удалены из библиотеки.") },
            confirmButton = {
                TextButton(onClick = { toDelete = null; scope.launch { c.library.delete(b.book.id) } }) { Text("Удалить", color = Ink.Fg) }
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
            BookCover(item.book.title, Modifier.size(width = 58.dp, height = 84.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.book.title, style = MaterialTheme.typography.headlineSmall, color = Ink.Fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreHoriz, "Ещё", tint = Ink.Muted) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Ink.Elevated) {
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
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier.background(Ink.Fg, RoundedCornerShape(2.dp)).padding(7.dp),
    ) {
        // Шрифт — такой, чтобы самое длинное слово названия не переносилось посередине.
        val measurer = androidx.compose.ui.text.rememberTextMeasurer()
        val widthPx = constraints.maxWidth
        val longest = remember(title) { title.split(' ').maxByOrNull { it.length }.orEmpty() }
        val fontSize = remember(title, widthPx) {
            (10 downTo 7).firstOrNull { size ->
                measurer.measure(longest, TextStyle(fontFamily = Serif, fontSize = size.sp)).size.width <= widthPx
            }
        }
        val small = maxWidth < 44.dp || fontSize == null
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.width(14.dp).height(1.dp).background(Ink.Bg))
            if (small) {
                val monogram = title.split(' ')
                    .filter { it.isNotBlank() && it.lowercase() !in setOf("the", "a", "an", "of", "and") }
                    .take(2).joinToString("") { it.first().uppercase() }
                Text(monogram, style = TextStyle(fontFamily = Serif, fontSize = 18.sp), color = Ink.Bg)
            } else {
                Text(
                    title,
                    style = TextStyle(fontFamily = Serif, fontSize = (fontSize ?: 10).sp, lineHeight = ((fontSize ?: 10) * 1.2f).sp),
                    color = Ink.Bg,
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
