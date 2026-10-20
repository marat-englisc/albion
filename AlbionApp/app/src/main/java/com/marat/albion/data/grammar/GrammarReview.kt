package com.marat.albion.data.grammar

import com.marat.albion.data.db.GrammarProgressEntity

/** Локальный план повторения тем. Изменяется только после законченного полного теста. */
object GrammarReview {
    private const val DAY = 86_400_000L
    private val intervals = listOf(1, 3, 7, 14, 30, 60)

    data class Plan(val stage: Int, val dueAt: Long)
    data class Item(val topic: Topic, val dueAt: Long)

    /** Результаты, сохранённые до появления плана, тоже попадают в очередь. */
    fun plan(progress: GrammarProgressEntity?): Plan? {
        val quizAt = progress?.quizAt ?: return null
        val stage = if (progress.masteredAt != null) progress.reviewStage.coerceIn(1, intervals.size) else 0
        return Plan(stage, progress.reviewAt ?: (quizAt + DAY))
    }

    fun afterQuiz(previous: GrammarProgressEntity?, passed: Boolean, now: Long): Plan {
        if (!passed) return Plan(0, now + DAY)
        val old = plan(previous)
        // Досрочный успешный тест не отодвигает повторение и не ускоряет рост интервала.
        if (old != null && old.stage > 0 && now < old.dueAt) return old
        val stage = ((old?.stage ?: 0) + 1).coerceIn(1, intervals.size)
        return Plan(stage, now + intervals[stage - 1] * DAY)
    }

    fun scheduled(book: GrammarBook, progress: Map<String, GrammarProgressEntity>, quizTopics: Set<Int>): List<Item> =
        book.lessons.mapNotNull { topic ->
            if (topic.number !in quizTopics) return@mapNotNull null
            plan(progress[topic.id])?.let { Item(topic, it.dueAt) }
        }.sortedWith(compareBy<Item> { it.dueAt }.thenBy { it.topic.number })
}
