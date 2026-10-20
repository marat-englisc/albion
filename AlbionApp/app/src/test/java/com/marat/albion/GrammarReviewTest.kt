package com.marat.albion

import com.marat.albion.data.db.GrammarProgressEntity
import com.marat.albion.data.grammar.*
import org.junit.Assert.*
import org.junit.Test

class GrammarReviewTest {
    private val day = 86_400_000L
    private val now = 1_000L * day
    private fun progress(stage: Int, dueAt: Long, passed: Boolean = true) = GrammarProgressEntity(
        "topic", readAt = null, bookmarked = false, openedAt = null,
        masteredAt = if (passed) now - day else null, quizAt = now - day,
        quizCorrect = if (passed) 7 else 6, quizTotal = 10, reviewStage = stage, reviewAt = dueAt,
    )

    @Test fun readingAndBookmarksAloneDoNotScheduleTests() {
        assertNull(GrammarReview.plan(null))
        assertNull(GrammarReview.plan(GrammarProgressEntity("topic", now, true, now)))
    }

    @Test fun firstSuccessAndFailureAreRepeatedInOneDay() {
        assertEquals(GrammarReview.Plan(1, now + day), GrammarReview.afterQuiz(null, true, now))
        assertEquals(GrammarReview.Plan(0, now + day), GrammarReview.afterQuiz(null, false, now))
    }

    @Test fun dueSuccessesFollowIntervalsAndCapAtSixtyDays() {
        var p: GrammarProgressEntity? = null
        var time = now
        listOf(1, 3, 7, 14, 30, 60, 60).forEachIndexed { i, days ->
            val plan = GrammarReview.afterQuiz(p, true, time)
            assertEquals((i + 1).coerceAtMost(6), plan.stage)
            assertEquals(time + days * day, plan.dueAt)
            p = progress(plan.stage, plan.dueAt).copy(quizAt = time)
            time = plan.dueAt
        }
    }

    @Test fun earlySuccessKeepsTheScheduledDateAndStage() {
        val p = progress(4, now + day)
        assertEquals(GrammarReview.Plan(4, now + day), GrammarReview.afterQuiz(p, true, now))
    }

    @Test fun lateSuccessSchedulesFromActualCompletionTime() {
        assertEquals(GrammarReview.Plan(3, now + 7 * day), GrammarReview.afterQuiz(progress(2, now - 20 * day), true, now))
    }

    @Test fun failureResetsTheStageAndNextSuccessStartsAtOneDay() {
        val failed = GrammarReview.afterQuiz(progress(6, now + day), false, now)
        assertEquals(GrammarReview.Plan(0, now + day), failed)
        assertEquals(GrammarReview.Plan(1, now + day), GrammarReview.afterQuiz(progress(0, now + day, false), true, now))
    }

    @Test fun legacyFullResultsJoinTheQueueWithoutChangingReadingFlags() {
        val legacy = progress(0, now).copy(reviewAt = null)
        assertEquals(GrammarReview.Plan(1, now), GrammarReview.plan(legacy))
        assertEquals(GrammarReview.Plan(0, now), GrammarReview.plan(legacy.copy(masteredAt = null)))
        assertEquals(GrammarReview.Plan(2, now + 3 * day), GrammarReview.afterQuiz(legacy, true, now))
    }

    @Test fun queueContainsOnlyAvailableLessonTestsAndOrdersByDueDate() {
        fun topic(number: Int, kind: TopicKind = TopicKind.Lesson) = Topic("$number", number, "Topic $number", "A1", kind)
        val topics = listOf(topic(1), topic(2), topic(3), topic(4), topic(5, TopicKind.Reference))
        val book = GrammarBook(topics, emptyList(), emptyList(), null)
        val saved = topics.associate { it.id to progress(1, now + it.number!! * day).copy(topicId = it.id) }.toMutableMap()
        saved["2"] = progress(0, now - day, false).copy(topicId = "2")
        saved["3"] = GrammarProgressEntity("3", now, true, now)
        val queue = GrammarReview.scheduled(book, saved, setOf(1, 2, 3, 5))
        assertEquals(listOf("2", "1"), queue.map { it.topic.id })
        assertEquals(listOf("2"), queue.filter { it.dueAt <= now }.map { it.topic.id })
    }
}
