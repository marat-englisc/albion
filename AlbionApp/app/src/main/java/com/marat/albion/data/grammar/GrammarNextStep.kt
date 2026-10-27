package com.marat.albion.data.grammar

import com.marat.albion.data.db.GrammarProgressEntity

/** Reading, confirming mastery and scheduled review are different learning actions. */
object GrammarNextStep {
    enum class Kind { Read, Check, Review }
    data class Step(val topic: Topic, val kind: Kind, val reason: String) {
        val label get() = when (kind) { Kind.Read -> "Прочитать"; Kind.Check -> "Закрепить"; Kind.Review -> "Повторить" }
        val action get() = when (kind) { Kind.Read -> "Читать урок"; Kind.Check -> "Проверить знания"; Kind.Review -> "Повторить тему" }
    }

    fun recommend(
        book: GrammarBook, progress: Map<String, GrammarProgressEntity>, quizTopics: Set<Int>,
        now: Long, level: String? = null, lastId: String? = null,
    ): Step? {
        val due = GrammarReview.scheduled(book, progress, quizTopics)
            .firstOrNull { it.dueAt <= now && (level == null || it.topic.level == level) }
        if (due != null) return Step(due.topic, Kind.Review, "Подошёл срок повторения. Короткая проверка поможет сохранить знания.")
        // A read lesson without a quiz must not block the rest of the course or gain a mastery badge.
        val covered = book.lessons.filter { topic ->
            val p = progress[topic.id]
            p?.masteredAt != null || (p?.readAt != null && topic.number !in quizTopics)
        }.map { it.id }.toSet()
        val topic = GrammarLearningPath.recommend(book.topics, covered, level, lastId) ?: return null
        val p = progress[topic.id]
        return if (p?.readAt != null && topic.number in quizTopics) {
            Step(topic, Kind.Check, if (p.quizAt == null) "Урок прочитан. Проверьте знания, чтобы подтвердить освоение."
                else "Последняя проверка пока не подтвердила освоение. Разберите правило и попробуйте снова.")
        } else Step(topic, Kind.Read, when {
            topic.number !in quizTopics -> "Изучите правило и примеры. После чтения можно перейти к следующей теме."
            p?.quizAt != null -> "Освоение пока не подтверждено. Вернитесь к правилу и разберите примеры перед новой попыткой."
            topic.id == lastId && p?.openedAt != null -> "Последний открытый урок. Продолжите знакомство с правилом и примерами."
            else -> "Начните с правила и примеров, затем закрепите их в короткой проверке."
        })
    }
}
