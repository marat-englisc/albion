package com.marat.albion.data.repo

import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.db.ReviewStateEntity
import java.util.Locale

/** One word per collection, without rewriting source IDs or historical answers. */
object WordCards {
    fun key(term: String) = term.trim().lowercase(Locale.ROOT)
    fun members(card: CardEntity) = card.senses.ifEmpty { listOf(card) }

    fun group(rows: List<CardEntity>): List<CardEntity> = rows
        .groupBy { it.deck to key(it.term) }.values.map(::combine)

    fun combine(rows: List<CardEntity>): CardEntity {
        require(rows.isNotEmpty())
        val ordered = rows.sortedWith(compareBy<CardEntity> { it.position }.thenBy { it.id })
        if (ordered.size == 1) return ordered.first()
        fun distinctText(value: (CardEntity) -> String) = ordered.map(value).filter { it.isNotBlank() }.distinct()
        return ordered.first().copy(
            term = ordered.first().term.trim(),
            pos = distinctText { it.pos }.joinToString(" · "),
            ipa = distinctText { it.ipa }.joinToString(" · "),
            meaningEn = distinctText { it.meaningEn }.map { it.trim().trimEnd('.', ';') }.distinct().joinToString("; "),
            meaningRu = distinctText { it.meaningRu }.map { it.trim().trimEnd('.', ';') }.distinct().joinToString("; "),
            audio = ordered.firstNotNullOfOrNull { it.audio },
        ).apply { senses = ordered }
    }

    /** Continue from the last studied meaning. Future answers share this word's schedule. */
    fun state(card: CardEntity, states: Map<String, ReviewStateEntity>): ReviewStateEntity? =
        members(card).mapNotNull { states[it.id] }
            .maxWithOrNull(compareBy<ReviewStateEntity> { it.lastReview }
                .thenBy { if (it.cardId == card.id) 1 else 0 }.thenBy { it.reps })
}
