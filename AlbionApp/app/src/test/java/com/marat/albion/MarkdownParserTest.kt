package com.marat.albion

import com.marat.albion.data.grammar.MarkdownParser
import com.marat.albion.data.grammar.MdBlock
import com.marat.albion.data.grammar.MdCallout
import com.marat.albion.data.grammar.MdCode
import com.marat.albion.data.grammar.MdDiagram
import com.marat.albion.data.grammar.MdHeading
import com.marat.albion.data.grammar.MdList
import com.marat.albion.data.grammar.MdParagraph
import com.marat.albion.data.grammar.MdQuote
import com.marat.albion.data.grammar.MdTable
import com.marat.albion.data.grammar.MermaidParser
import com.marat.albion.data.grammar.NodeShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class MarkdownParserTest {
    private val grammarDir = File("../../grammar").canonicalFile

    @Test
    fun parsesCalloutsListsAndTables() {
        val doc = MarkdownParser.parse(
            """
            ---
            level: A2
            order: 33
            ---

            # Title

            > [!abstract] Коротко
            > - one `code`
            > - two

            1. **Bold** text.${"  "}
               **Разбор:** continuation
            2. Second

            | A | B |
            | --- | --- |
            | [[x\|y]] | 2 |

            > [!success]- Ответы
            > 1. **ok**
            """.trimIndent()
        )
        assertEquals("A2", doc.frontmatter["level"])
        assertEquals("Title", doc.title)
        val callout = doc.blocks[0] as MdCallout
        assertEquals("abstract", callout.kind)
        assertTrue(callout.blocks[0] is MdList)
        val list = doc.blocks[1] as MdList
        assertTrue(list.ordered)
        assertEquals(2, list.items.size)
        assertEquals("**Bold** text.\n**Разбор:** continuation", (list.items[0].blocks[0] as MdParagraph).text)
        val table = doc.blocks[2] as MdTable
        assertEquals(listOf("[[x|y]]", "2"), table.rows[0])
        val answers = doc.blocks[3] as MdCallout
        assertTrue(answers.foldable && !answers.initiallyExpanded)
    }

    @Test
    fun parsesMermaid() {
        val g = MermaidParser.parse(
            """
            flowchart TD
                A["Что хочу сказать?"] --> B{"Обычно?"}
                B -->|Да| C["Present Simple"]
                B -->|Нет| D{"Сейчас?"}
                D -->|Да| E["Continuous"]
                E --- C
            """.trimIndent()
        )
        assertNotNull(g)
        g!!
        assertEquals(5, g.nodes.size)
        assertEquals(NodeShape.Decision, g.node("B").shape)
        assertEquals("Что хочу сказать?", g.node("A").label)
        assertEquals(listOf("Да", "Нет"), g.outgoing("B").map { it.label })
        assertEquals(5, g.edges.size)
    }

    /** Все темы учебника разбираются без «сырых» остатков разметки. */
    @Test
    fun parsesWholeGrammarBook() {
        val files = grammarDir.listFiles { f -> f.extension == "md" }?.toList().orEmpty()
        assumeTrue("нет папки grammar", files.isNotEmpty())
        val ids = files.map { it.nameWithoutExtension }.toSet()
        var diagrams = 0
        var tables = 0
        val brokenLinks = mutableSetOf<String>()
        val link = Regex("\\[\\[([^\\]|#\\\\]+)")
        for (f in files) {
            val text = f.readText()
            val doc = MarkdownParser.parse(text)
            assertNotNull("${f.name}: нет заголовка", doc.title)
            val all = flatten(doc.blocks)
            assertTrue("${f.name}: mermaid не разобран", all.none { it is MdCode && it.lang == "mermaid" })
            assertTrue("${f.name}: callout не распознан", all.none { it is MdParagraph && it.text.startsWith("[!") })
            all.filterIsInstance<MdTable>().forEach { t ->
                assertTrue("${f.name}: таблица", t.rows.all { it.size == t.header.size })
            }
            diagrams += all.count { it is MdDiagram }
            tables += all.count { it is MdTable }
            link.findAll(text).forEach { m -> if (m.groupValues[1].trim() !in ids) brokenLinks += m.groupValues[1] }
            if (f.name.first().isDigit() && !f.name.startsWith("000")) {
                assertTrue("${f.name}: нет блока «Коротко»", doc.blocks.any { it is MdCallout && it.kind == "abstract" })
                assertNotNull("${f.name}: нет level", doc.frontmatter["level"])
            }
        }
        println("Тем: ${files.size}, схем: $diagrams, таблиц: $tables, битых ссылок: ${brokenLinks.size} ${brokenLinks.take(5)}")
        assertTrue(brokenLinks.isEmpty())
    }

    @Test
    fun contentsHasChapters() {
        val f = File(grammarDir, "000 - Оглавление.md")
        assumeTrue(f.exists())
        val doc = MarkdownParser.parse(f.readText())
        val chapters = doc.blocks.filterIsInstance<MdHeading>().filter { it.level == 2 }.map { it.text }
        assertTrue(chapters.contains("Основа предложения"))
        assertTrue(chapters.contains("Справочники"))
    }

    private fun flatten(blocks: List<MdBlock>): List<MdBlock> = blocks.flatMap { b ->
        listOf(b) + when (b) {
            is MdCallout -> flatten(b.blocks)
            is MdQuote -> flatten(b.blocks)
            is MdList -> b.items.flatMap { flatten(it.blocks) }
            else -> emptyList()
        }
    }
}
