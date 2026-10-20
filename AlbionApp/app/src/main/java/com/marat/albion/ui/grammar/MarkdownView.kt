package com.marat.albion.ui.grammar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.data.grammar.MdBlock
import com.marat.albion.data.grammar.MdCallout
import com.marat.albion.data.grammar.MdCode
import com.marat.albion.data.grammar.MdDiagram
import com.marat.albion.data.grammar.MdHeading
import com.marat.albion.data.grammar.MdList
import com.marat.albion.data.grammar.MdParagraph
import com.marat.albion.data.grammar.MdQuote
import com.marat.albion.data.grammar.MdRule
import com.marat.albion.data.grammar.MdTable
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Serif

/** Настройки отрисовки: масштаб текста и обработчик вики-ссылок. */
class MdEnv(val scale: Float, val onWikiLink: (String) -> Unit)

val LocalMdEnv = staticCompositionLocalOf { MdEnv(1f) {} }



internal operator fun TextUnit.times(f: Float): TextUnit = (value * f).sp

@Composable
private fun bodyStyle(): TextStyle {
    val s = LocalMdEnv.current.scale
    return MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp * s, lineHeight = 26.sp * s, color = Ink.Text)
}

/** Один верхнеуровневый блок — удобно класть по одному в LazyColumn. */
@Composable
fun MdBlockView(block: MdBlock, modifier: Modifier = Modifier) {
    when (block) {
        is MdHeading -> Heading(block, modifier)
        is MdParagraph -> InlineText(block.text, modifier)
        is MdList -> ListView(block, modifier)
        is MdTable -> TableView(block, modifier)
        is MdCallout -> CalloutView(block, modifier)
        is MdQuote -> QuoteView(block, modifier)
        is MdCode -> CodeView(block, modifier)
        is MdDiagram -> FlowDiagram(block.graph, modifier)
        MdRule -> Hairline(modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun MdBlocks(blocks: List<MdBlock>, modifier: Modifier = Modifier, spacing: Dp = 12.dp) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        blocks.forEach { MdBlockView(it) }
    }
}

@Composable
private fun Heading(h: MdHeading, modifier: Modifier) {
    val scale = LocalMdEnv.current.scale
    when (h.level) {
        1, 2 -> Column(modifier.padding(top = 22.dp, bottom = 2.dp)) {
            Box(Modifier.width(24.dp).height(1.dp).background(Ink.Fg))
            Spacer(Modifier.height(14.dp))
            InlineText(h.text, style = TextStyle(fontFamily = Serif, fontSize = 25.sp * scale, lineHeight = 31.sp * scale, color = Ink.Fg, letterSpacing = (-0.3).sp))
        }
        3 -> InlineText(h.text.uppercase(), modifier.padding(top = 10.dp), style = AlbionType.eyebrow.copy(color = Ink.Soft, fontSize = 13.sp * scale))
        else -> InlineText(h.text, modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp * scale, color = Ink.Fg))
    }
}

@Composable
fun InlineText(text: String, modifier: Modifier = Modifier, style: TextStyle = bodyStyle()) {
    val env = LocalMdEnv.current
    val uri = LocalUriHandler.current
    val annotated = remember(text, style.color) {
        inlineMarkdown(text, onWiki = env.onWikiLink, onUrl = { runCatching { uri.openUri(it) } })
    }
    Text(annotated, modifier = modifier, style = style)
}

/**
 * Инлайн-разметка Obsidian → AnnotatedString:
 * **жирный**, *курсив*, ~~зачёркнутый~~, ==выделение==, `код`, [[ссылка|текст]], [текст](url), <br>.
 */
fun inlineMarkdown(text: String, onWiki: (String) -> Unit, onUrl: (String) -> Unit): AnnotatedString = buildAnnotatedString {
    var bold = false
    var italic = false
    var strike = false
    var mark = false
    val linkStyles = TextLinkStyles(SpanStyle(color = Ink.Fg, textDecoration = TextDecoration.Underline))

    fun style() = SpanStyle(
        fontWeight = if (bold) FontWeight.SemiBold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        textDecoration = if (strike) TextDecoration.LineThrough else null,
        background = if (mark) Ink.High else Color.Unspecified,
        color = if (bold) Ink.Fg else Color.Unspecified,
    )

    val sb = StringBuilder()
    fun flush() { if (sb.isNotEmpty()) { withStyle(style()) { append(sb.toString()) }; sb.setLength(0) } }

    var i = 0
    while (i < text.length) {
        val ch = text[i]
        when {
            ch == '\\' && i + 1 < text.length && text[i + 1] in "\\`*_[]()#|~=<>!-." -> { sb.append(text[i + 1]); i += 2 }

            ch == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end < 0) { sb.append(ch); i++ } else {
                    flush()
                    withStyle(SpanStyle(color = Ink.Fg, background = Ink.Code, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium)) {
                        append(" "); append(text.substring(i + 1, end)); append(" ")
                    }
                    i = end + 1
                }
            }

            text.startsWith("[[", i) -> {
                val end = text.indexOf("]]", i + 2)
                if (end < 0) { sb.append(ch); i++ } else {
                    flush()
                    val inner = text.substring(i + 2, end)
                    val target = inner.substringBefore('|').trim()
                    val alias = inner.substringAfter('|', inner.substringAfter('#', target)).trim()
                    withLink(LinkAnnotation.Clickable(target, linkStyles) { onWiki(target) }) { withStyle(style()) { append(alias) } }
                    i = end + 2
                }
            }

            ch == '[' -> {
                val close = text.indexOf("](", i + 1)
                val end = if (close > 0) text.indexOf(')', close + 2) else -1
                if (close < 0 || end < 0 || text.substring(i + 1, close).contains('\n')) { sb.append(ch); i++ } else {
                    flush()
                    val label = text.substring(i + 1, close)
                    val url = text.substring(close + 2, end).trim()
                    withLink(LinkAnnotation.Clickable(url, linkStyles) { onUrl(url) }) { withStyle(style()) { append(label) } }
                    i = end + 1
                }
            }

            text.startsWith("**", i) || text.startsWith("__", i) -> { flush(); bold = !bold; i += 2 }
            text.startsWith("~~", i) -> { flush(); strike = !strike; i += 2 }
            text.startsWith("==", i) -> { flush(); mark = !mark; i += 2 }

            ch == '*' || (ch == '_' && (i == 0 || !text[i - 1].isLetterOrDigit()) || (ch == '_' && italic)) -> {
                val next = text.getOrNull(i + 1)
                if (!italic && (next == null || next == ' ')) { sb.append(ch); i++ } else { flush(); italic = !italic; i++ }
            }

            text.startsWith("<br", i, ignoreCase = true) && text.indexOf('>', i) > 0 -> {
                sb.append('\n'); i = text.indexOf('>', i) + 1
            }

            else -> { sb.append(ch); i++ }
        }
    }
    flush()
}

// ---------------------------------------------------------------- списки

@Composable
private fun ListView(list: MdList, modifier: Modifier) {
    val body = bodyStyle()
    val lineHeightDp = with(LocalDensity.current) { body.lineHeight.toDp() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        list.items.forEachIndexed { index, item ->
            Row {
                Box(Modifier.width(if (list.ordered) 30.dp else 20.dp).height(lineHeightDp), contentAlignment = Alignment.CenterStart) {
                    when {
                        item.checked != null -> Icon(
                            if (item.checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                            null, tint = Ink.Faint, modifier = Modifier.size(18.dp),
                        )
                        list.ordered -> Text("${list.start + index}", style = AlbionType.index.copy(color = Ink.Soft, fontSize = 16.sp * LocalMdEnv.current.scale))
                        else -> Box(Modifier.size(4.dp).background(Ink.Fg))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.blocks.forEach { MdBlockView(it) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- таблицы

/**
 * Адаптивная таблица без горизонтальной прокрутки:
 * 1) всё помещается одной строкой — колонки растягиваются на всю ширину;
 * 2) помещаются самые длинные слова — колонки ужимаются, текст переносится;
 * 3) иначе — каждая строка становится карточкой «заголовок колонки → значение».
 */
@Composable
private fun TableView(table: MdTable, modifier: Modifier) {
    val scale = LocalMdEnv.current.scale
    val cellStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp * scale, lineHeight = 20.sp * scale, color = Ink.Text)
    val headStyle = cellStyle.copy(fontWeight = FontWeight.SemiBold, color = Ink.Fg)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val cellPadding = 10.dp

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = constraints.maxWidth.toFloat()
        val layout = remember(table, scale, available) {
            val pad = with(density) { (cellPadding * 2 + 1.dp).toPx() }
            val columns = table.header.size
            val minW = FloatArray(columns)
            val prefW = FloatArray(columns)
            (listOf(table.header) + table.rows).forEachIndexed { r, row ->
                val style = if (r == 0) headStyle else cellStyle
                for (c in 0 until columns) {
                    val text = stripInline(row.getOrElse(c) { "" })
                    if (text.isBlank()) continue
                    val line = measurer.measure(text, style, maxLines = 1, softWrap = false).size.width + pad
                    val word = text.split(' ', '/').filter { it.isNotBlank() }
                        .maxOf { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width } + pad
                    prefW[c] = maxOf(prefW[c], minOf(line, available * 0.75f))
                    minW[c] = maxOf(minW[c], word)
                }
            }
            for (c in 0 until columns) {
                minW[c] = maxOf(minW[c], pad + 24f)
                prefW[c] = maxOf(prefW[c], minW[c])
            }
            val sumPref = prefW.sum()
            val sumMin = minW.sum()
            when {
                sumPref <= available -> prefW.map { it * available / sumPref }
                sumMin <= available -> {
                    val extra = available - sumMin
                    val slack = (0 until columns).map { prefW[it] - minW[it] }
                    val slackSum = slack.sum().coerceAtLeast(1f)
                    (0 until columns).map { minW[it] + extra * slack[it] / slackSum }
                }
                else -> null
            }
        }

        if (layout == null) {
            TableAsCards(table, headStyle, cellStyle)
        } else {
            val widths = layout.map { with(density) { it.toDp() } }
            Column(Modifier.border(1.dp, Ink.Line, RoundedCornerShape(4.dp))) {
                TableRow(table.header, widths, headStyle, Ink.Elevated, cellPadding)
                table.rows.forEachIndexed { r, row ->
                    Hairline()
                    TableRow(row, widths, cellStyle, if (r % 2 == 0) Ink.Bg else Ink.Raised, cellPadding)
                }
            }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, widths: List<Dp>, style: TextStyle, bg: Color, padding: Dp) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(bg)) {
        cells.forEachIndexed { i, cell ->
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(Ink.Line))
            val w = widths.getOrElse(i) { 0.dp } - 1.dp
            Box(
                Modifier.then(if (i == cells.lastIndex) Modifier.weight(1f) else Modifier.width(w))
                    .padding(horizontal = padding, vertical = 9.dp),
            ) { InlineText(cell, style = style) }
        }
    }
}

/** Узкий экран и широкая таблица: строка → карточка, первая ячейка — заголовок. */
@Composable
private fun TableAsCards(table: MdTable, headStyle: TextStyle, cellStyle: TextStyle) {
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = Ink.Faint)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        table.rows.forEach { row ->
            Column(
                Modifier.fillMaxWidth().border(1.dp, Ink.Line, RoundedCornerShape(4.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val title = row.firstOrNull().orEmpty()
                if (title.isNotBlank()) {
                    if (table.header.firstOrNull().orEmpty().isNotBlank()) InlineText(table.header[0], style = labelStyle)
                    InlineText(title, style = headStyle.copy(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = headStyle.fontSize * 1.2f))
                }
                for (c in 1 until table.header.size) {
                    val value = row.getOrElse(c) { "" }
                    if (value.isBlank()) continue
                    Column {
                        if (table.header[c].isNotBlank()) InlineText(table.header[c], style = labelStyle)
                        InlineText(value, style = cellStyle)
                    }
                }
            }
        }
    }
}

private val INLINE_MARKS = Regex("\\*\\*|__|`|\\[\\[|]]|==")
private fun stripInline(s: String) = s.replace(INLINE_MARKS, "").replace(Regex("[^|\\s]+\\|"), "")

// ---------------------------------------------------------------- callout

/** Типы блоков различаются оформлением, а не цветом. */
private enum class Frame { Box, Rule, Filled, Plain }

private data class CalloutLook(val icon: ImageVector, val frame: Frame)

private fun look(kind: String): CalloutLook = when (kind) {
    "abstract", "summary", "tldr" -> CalloutLook(Icons.Outlined.Info, Frame.Box)
    "tip", "hint" -> CalloutLook(Icons.Outlined.Lightbulb, Frame.Rule)
    "warning", "caution", "attention", "danger", "error" -> CalloutLook(Icons.Outlined.ReportProblem, Frame.Filled)
    "success", "check", "done" -> CalloutLook(Icons.Outlined.TaskAlt, Frame.Plain)
    "example" -> CalloutLook(Icons.Outlined.Route, Frame.Plain)
    "quote", "cite" -> CalloutLook(Icons.Rounded.FormatQuote, Frame.Rule)
    else -> CalloutLook(Icons.Outlined.Info, Frame.Rule)
}

@Composable
private fun CalloutView(c: MdCallout, modifier: Modifier) {
    val l = look(c.kind)
    var open by rememberSaveable(c.title, c.kind) { mutableStateOf(c.initiallyExpanded) }
    val arrow by animateFloatAsState(if (open) 180f else 0f, tween(320, easing = Easing.Emphasized), label = "chevron")
    val scale = LocalMdEnv.current.scale
    val shape = RoundedCornerShape(4.dp)
    val container = when (l.frame) {
        Frame.Box -> modifier.fillMaxWidth().clip(shape).background(Ink.Raised).border(1.dp, Ink.Fg.copy(alpha = 0.85f), shape)
        Frame.Filled -> modifier.fillMaxWidth().clip(shape).background(Ink.Elevated)
        Frame.Plain -> modifier.fillMaxWidth().clip(shape).border(1.dp, Ink.LineStrong, shape)
        Frame.Rule -> modifier.fillMaxWidth()
    }
    Row(container.height(IntrinsicSize.Min)) {
        if (l.frame == Frame.Rule) Box(Modifier.width(2.dp).fillMaxHeight().background(Ink.Soft))
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth()
                    .then(if (c.foldable) Modifier.clickable { open = !open } else Modifier)
                    .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = if (open && c.blocks.isNotEmpty()) 8.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(l.icon, null, tint = Ink.Fg, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(10.dp))
                InlineText(c.title.uppercase(), Modifier.weight(1f), style = AlbionType.eyebrow.copy(color = Ink.Fg, fontSize = 13.sp * scale))
                if (c.foldable) Icon(Icons.Rounded.ExpandMore, if (open) "Свернуть" else "Развернуть", tint = Ink.Fg, modifier = Modifier.rotate(arrow))
            }
            AnimatedVisibility(
                open,
                enter = expandVertically(tween(380, easing = Easing.Emphasized)) + fadeIn(tween(300, delayMillis = 80)),
                exit = shrinkVertically(tween(300, easing = Easing.Emphasized)) + fadeOut(tween(150)),
            ) {
                MdBlocks(c.blocks, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), spacing = 8.dp)
            }
        }
    }
}

@Composable
private fun QuoteView(q: MdQuote, modifier: Modifier) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(Ink.Faint))
        Spacer(Modifier.width(14.dp))
        MdBlocks(q.blocks, Modifier.weight(1f), spacing = 8.dp)
    }
}

@Composable
private fun CodeView(code: MdCode, modifier: Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Ink.Code).padding(14.dp)) {
        Text(code.code, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Ink.Text))
    }
}

/** Предоставляет окружение Markdown для поддерева. */
@Composable
fun ProvideMdEnv(env: MdEnv, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMdEnv provides env, content = content)
}
