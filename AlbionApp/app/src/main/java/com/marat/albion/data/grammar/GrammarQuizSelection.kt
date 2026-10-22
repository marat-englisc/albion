package com.marat.albion.data.grammar

import com.marat.albion.data.db.GrammarTaskEntity

/** Новая проверка использует часть банка, сохраняя выбор и самостоятельный ввод. */
object GrammarQuizSelection {
    const val SIZE = 8

    fun select(pool: List<GrammarTaskEntity>): List<GrammarTaskEntity> {
        val unique = pool.distinctBy { it.id }
        val chosen = (unique.filter { it.kind == "choice" }.take(SIZE / 2) +
            unique.filter { it.kind == "input" }.take(SIZE / 2)).toMutableList()
        val ids = chosen.map { it.id }.toSet()
        chosen += unique.filter { it.id !in ids }.take((SIZE - chosen.size).coerceAtLeast(0))
        // Порядок DAO уже учитывает ошибки и перемешивает равные по приоритету задания.
        val selectedIds = chosen.map { it.id }.toSet()
        return unique.filter { it.id in selectedIds }
    }
}
