package com.marat.albion

import com.marat.albion.data.grammar.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class GrammarContentsTest {
    private val lesson = Topic("001 - Lesson", 1, "Lesson", "A1", TopicKind.Lesson)

    @Test fun ordinaryListsAndUnknownSectionsArePreserved() {
        val doc = MarkdownParser.parse("""
            # Course
            ## How to study
            1. Read the rule.
            2. Open [[001 - Lesson]].
            ## New section
            - A normal list.
            - [[001 - Lesson]]
            ## Chapter
            - [ ] [[001 - Lesson|Lesson]]
            ## Routes
            > [!example]- A1
            > - [[001 - Lesson]]
        """.trimIndent())
        val result = GrammarContents.extract(doc, listOf(lesson))
        assertEquals(listOf(Chapter("Chapter", listOf(lesson))), result.chapters)
        val expected = doc.blocks.filterNot { it is MdHeading && it.text == "Chapter" || it is MdList && it.items.first().checked != null }
        assertEquals(expected, result.intro.blocks)
    }

    @Test fun mixedOrUnresolvedChecklistsAreNeverSilentlyDiscarded() {
        val doc = MarkdownParser.parse("""
            ## Reading plan
            - [ ] [[001 - Lesson]]
            - [ ] Write a sentence.
            ## Later
            - [ ] [[Missing lesson]]
        """.trimIndent())
        val result = GrammarContents.extract(doc, listOf(lesson))
        assertTrue(result.chapters.isEmpty())
        assertEquals(doc, result.intro)
    }

    @Test fun chapterDescriptionsAndMultipleListsAreKept() {
        val doc = MarkdownParser.parse("""
            ## Chapter
            Introduction to this chapter.
            - [ ] [[001 - Lesson]]

            More advice.
            - [x] [[001 - Lesson]]
        """.trimIndent())
        val result = GrammarContents.extract(doc, listOf(lesson))
        assertEquals(listOf(lesson), result.chapters.single().topics)
        assertEquals(listOf(MdHeading(2, "Chapter"), MdParagraph("Introduction to this chapter."), MdParagraph("More advice.")), result.intro.blocks)
    }

    @Test fun actualContentsKeepAllInstructionsAndFiveLevelGroups() {
        val dir = File("../../grammar").canonicalFile
        assumeTrue(dir.isDirectory)
        val topics = dir.listFiles().orEmpty().filter { it.extension == "md" }.map { file ->
            val id = file.nameWithoutExtension
            val number = id.take(3).toIntOrNull()
            Topic(id, number, id, null, when {
                id.startsWith("Справочник") -> TopicKind.Reference
                number == 0 -> TopicKind.Contents
                else -> TopicKind.Lesson
            })
        }
        val doc = MarkdownParser.parse(File(dir, "000 - Оглавление.md").readText())
        val result = GrammarContents.extract(doc, topics)
        val extracted = result.chapters.flatMap { it.topics }
        assertEquals(144, extracted.size)
        assertEquals(144, extracted.map { it.id }.toSet().size)
        val blocks = result.intro.blocks
        val study = blocks.indexOf(MdHeading(2, "Как заниматься"))
        assertTrue(study >= 0)
        val steps = blocks.drop(study + 1).first() as MdList
        assertTrue(steps.ordered)
        assertEquals(5, steps.items.size)
        assertTrue(blocks.contains(MdHeading(2, "Темы по уровням")))
        val routes = blocks.filterIsInstance<MdCallout>().filter { it.title.startsWith("Темы ") }
        assertEquals(listOf(24, 32, 35, 34, 19), routes.map { (it.blocks.single() as MdList).items.size })
        assertTrue(routes.all { it.foldable && !it.initiallyExpanded })
        assertEquals(144, routes.sumOf { (it.blocks.single() as MdList).items.size })
        assertTrue(blocks.contains(MdHeading(2, "Справочники")))
        assertTrue(blocks.filterIsInstance<MdParagraph>().any { "[[000 - Источники]]" in it.text })
    }
}
