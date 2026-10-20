package com.marat.albion.data.grammar

data class GrammarQuizResult(val correct: Int, val total: Int, val fullAttempt: Boolean) {
    init {
        require(total >= 0 && correct in 0..total)
    }

    val updatesMastery: Boolean get() = fullAttempt && total > 0
    val passed: Boolean get() = updatesMastery && correct.toLong() * 10 >= total.toLong() * 7
}

/** Полный тест и повтор ошибок — разные попытки. Каждый вопрос учитывается один раз. */
class GrammarQuizSession(taskIds: List<String>, private val fullAttempt: Boolean) {
    private val expected = taskIds.toSet()
    private val answers = LinkedHashMap<String, Boolean>()

    init { require(expected.size == taskIds.size) }

    fun record(taskId: String, correct: Boolean): Boolean {
        if (taskId !in expected || taskId in answers) return false
        answers[taskId] = correct
        return true
    }

    fun answered(taskId: String): Boolean = taskId in answers

    fun result(): GrammarQuizResult? =
        if (expected.isNotEmpty() && answers.size == expected.size) {
            GrammarQuizResult(answers.values.count { it }, expected.size, fullAttempt)
        } else null
}
