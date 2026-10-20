package com.marat.albion

import com.marat.albion.data.content.AudioIndex
import com.marat.albion.data.content.CollectionParser
import com.marat.albion.data.content.Deck
import com.marat.albion.data.content.IrregularData
import com.marat.albion.data.content.XlsxReader
import com.marat.albion.data.grammar.GrammarPrompt
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Проверка на настоящих материалах из папки над проектом (collections/, audio/).
 * Если материалов нет — тест пропускается.
 */
class ContentParsingTest {
    private val root = File("../..").canonicalFile
    private val json = Json
    private val parser = CollectionParser(json)

    private fun rows(deck: Deck): List<List<String>> {
        val f = File(root, "collections/${deck.file}")
        assumeTrue("нет ${f.path}", f.exists())
        return f.inputStream().use(XlsxReader::read)
    }

    private fun audio(deck: Deck) = AudioIndex(File(root, "audio/${deck.audioDir}").list()?.toList().orEmpty())

    @Test
    fun columnIndex() {
        assertEquals(0, XlsxReader.columnIndex("A1"))
        assertEquals(25, XlsxReader.columnIndex("Z10"))
        assertEquals(27, XlsxReader.columnIndex("AB3"))
    }

    @Test
    fun simpleDecksParseWithAudio() {
        for (deck in Deck.entries - Deck.IRREGULAR) {
            val r = rows(deck)
            val cards = parser.parseCards(deck, r, audio(deck))
            val withoutAudio = cards.filter { it.audio == null }
            println("${deck.id}: ${cards.size} карточек, без озвучки ${withoutAudio.size} ${withoutAudio.take(5).map { it.term }}")
            assertEquals(r.drop(1).count { it.firstOrNull()?.isNotBlank() == true }, cards.size)
            assertEquals("id уникальны", cards.size, cards.map { it.id }.toSet().size)
            assertTrue(cards.all { it.meaningEn.isNotBlank() && it.meaningRu.isNotBlank() && it.example1.isNotBlank() })
            assertTrue(cards.all { it.ipa.startsWith("/") })
            assertTrue("озвучка для всех", withoutAudio.isEmpty())
            if (deck == Deck.WORDS) {
                assertTrue(cards.all { it.level in setOf("A1", "A2", "B1", "B2", "C1", "C2") })
                assertTrue(cards.none { it.pos == deck.kind })
            } else {
                assertTrue(cards.all { it.pos == deck.kind })
            }
        }
    }

    @Test
    fun homonymsGetTheirOwnAudio() {
        val words = parser.parseCards(Deck.WORDS, rows(Deck.WORDS), audio(Deck.WORDS))
        fun audioOf(term: String) = words.filter { it.term == term }.map { it.audio }.toSet()
        println("March=${audioOf("March")} march=${audioOf("march")} may=${audioOf("may")} May=${audioOf("May")} it=${audioOf("it")}")
        if (words.any { it.term == "March" }) assertEquals(setOf("March (month).mp3"), audioOf("March"))
        if (words.any { it.term == "may" }) assertEquals(setOf("may (modal verb).mp3"), audioOf("may"))
        if (words.any { it.term == "it" }) assertEquals(setOf("it (pronoun).mp3"), audioOf("it"))
    }

    @Test
    fun grammarTasksAreConsistent() {
        val f = File(root, "collections/grammar_tasks.xlsx")
        assumeTrue("нет ${f.path}", f.exists())
        val tasks = parser.parseGrammarTasks(f.inputStream().use(XlsxReader::read))
        val topics = File(root, "grammar").list().orEmpty()
            .mapNotNull { Regex("^(\\d{3}) - ").find(it)?.groupValues?.get(1)?.toInt() }.toSet()
        println("Грамматических заданий: ${tasks.size}, выбор ${tasks.count { it.kind == "choice" }}, ввод ${tasks.count { it.kind == "input" }}")
        assertTrue(tasks.size >= 700)
        assertEquals("id уникальны", tasks.size, tasks.map { it.id }.toSet().size)
        tasks.forEach { t ->
            assertTrue("${t.id}: уровень", t.level in setOf("A1", "A2", "B1", "B2", "C1"))
            assertTrue("${t.id}: тема ${t.topic}", t.topic in topics)
            assertTrue("${t.id}: пояснение", t.explanation.isNotBlank())
            if (t.kind == "choice") {
                assertTrue("${t.id}: ответ среди вариантов", t.answer in t.optionList)
                assertTrue("${t.id}: 3–4 варианта", t.optionList.size in 3..4)
            } else {
                assertTrue("${t.id}: есть пропуск", "___" in t.prompt)
                assertTrue("${t.id}: есть ответ", t.acceptedAnswers.isNotEmpty())
            }
            val prompt = GrammarPrompt(t.prompt)
            if (prompt.gapCount > 0) {
                val answers = if (t.kind == "choice") t.optionList else t.acceptedAnswers
                answers.forEach { answer ->
                    assertNotNull("${t.id}: $answer не соответствует ${prompt.gapCount} пропускам", prompt.answerParts(answer))
                }
            }
        }
    }

    @Test
    fun irregularVerbsHaveAllForms() {
        val cards = parser.parseIrregular(rows(Deck.IRREGULAR), audio(Deck.IRREGULAR))
        assertTrue(cards.size > 200)
        val data = cards.associate { it.term to json.decodeFromString(IrregularData.serializer(), it.extra!!) }
        data.forEach { (term, d) ->
            assertEquals(term, 3, d.forms.size)
            d.forms.forEach { f ->
                assertTrue("$term ${f.label}", f.variants.isNotEmpty())
                f.variants.forEach { v ->
                    assertTrue("$term ${v.text} ipa", v.ipa.startsWith("/"))
                    assertTrue("$term ${v.text} audio", v.audio.isNotEmpty())
                }
                assertEquals("$term ${f.label} примеры", 3, f.examples.size)
            }
        }
        val read = data.getValue("read")
        assertEquals(listOf("read.mp3"), read.forms[0].variants[0].audio)
        assertEquals(listOf("read (past).mp3"), read.forms[1].variants[0].audio)
        data["eat"]?.let { println("eat V2: ${it.forms[1].variants}") }
        val abide = data.getValue("abide")
        assertEquals(listOf("abided", "abode"), abide.forms[1].variants.map { it.text })
        assertEquals("/əˈbəʊd/", abide.forms[1].variants[1].ipa)
    }

    @Test
    fun mostCardsHaveSentenceForBuilder() {
        for (deck in listOf(Deck.WORDS, Deck.PHRASAL, Deck.IDIOMS, Deck.COLLOCATIONS)) {
            val cards = parser.parseCards(deck, rows(deck), audio(deck))
            val chunk = deck == Deck.IDIOMS || deck == Deck.COLLOCATIONS
            val ok = cards.count { c -> com.marat.albion.data.games.SentencePuzzle.fromExamples(listOf(c.example1, c.example2), c.term.takeIf { chunk }) != null }
            println("«Собери предложение» · ${deck.title}: $ok из ${cards.size}")
            assertTrue("${deck.title}: подходящих примеров слишком мало ($ok из ${cards.size})", ok * 10 >= cards.size * 7)
        }
    }

    @Test
    fun tenseDrillCoversIrregularVerbs() {
        val cards = parser.parseIrregular(rows(Deck.IRREGULAR), audio(Deck.IRREGULAR))
        val all = cards.map { it.term to com.marat.albion.data.games.TenseDrill.tasks(json.decodeFromString(IrregularData.serializer(), it.extra!!)) }
        val verbsWithTasks = all.count { it.second.isNotEmpty() }
        val tasks = all.flatMap { it.second }
        println("Тренажёр времён: ${tasks.size} заданий, глаголов ${verbsWithTasks} из ${cards.size}")
        println("без заданий (одинаковые формы): " + all.filter { it.second.isEmpty() }.joinToString { it.first })
        assertTrue(verbsWithTasks * 10 >= cards.size * 8)
        tasks.forEach { t ->
            assertTrue("${t.sentence}: ${t.options}", t.answer in t.options)
            assertTrue("${t.sentence}: ${t.options}", t.options.size >= 2)
            assertTrue(t.accepted.any { it.equals(t.answer, ignoreCase = true) })
        }
        val byForm = tasks.groupBy { it.formIndex }.mapValues { it.value.size }
        println("по формам: $byForm; Present Perfect: ${tasks.count { it.rule.startsWith("Present Perfect") }}, пассив: ${tasks.count { it.rule.startsWith("Пассив") }}")
        all.firstOrNull { it.first == "be" }?.second?.first { it.formIndex == 1 }?.let { assertEquals(1, it.accepted.size) }
    }
}
