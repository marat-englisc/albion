package com.marat.albion.data.library

import androidx.room.withTransaction
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.AlbionDatabase
import com.marat.albion.data.db.CardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

@Serializable
data class BookWordSource(val bookId: String, val title: String, val chapter: String)

data class BookWordDraft(
    val term: String,
    val meaning: String,
    val example: String,
    val source: BookWordSource,
    val ipa: String = "",
    val pos: String = "word",
    val meaningEn: String = "",
    val level: String? = null,
)

data class SavedBookWord(val card: CardEntity, val added: Boolean)

/** Личные карточки живут в той же таблице и используют обычную очередь FSRS. */
class BookWordsRepository(private val db: AlbionDatabase, private val json: Json) {
    fun observeTerm(term: String): Flow<List<CardEntity>> =
        db.cards().observeWithPrefix(Deck.BOOK_WORDS.id, termPrefix(term))

    suspend fun remove(cardId: String): Boolean = db.withTransaction {
        val card = db.cards().get(cardId) ?: return@withTransaction false
        if (card.deck != Deck.BOOK_WORDS.id) return@withTransaction false
        db.reviews().deleteState(cardId)
        db.reviews().deleteCardLogs(cardId)
        db.cards().delete(listOf(cardId))
        true
    }

    suspend fun add(draft: BookWordDraft): SavedBookWord {
        val term = clean(draft.term)
        val meaning = clean(draft.meaning)
        require(term.isNotBlank() && term.length <= 200) { "Введите слово или выражение до 200 символов" }
        require(meaning.isNotBlank() && meaning.length <= 2000) { "Введите перевод до 2000 символов" }
        require(draft.example.length <= 4000) { "Сократите пример до 4000 символов" }
        val id = termPrefix(term) + digest(key(meaning).trimEnd('.', '!', '?', ';'))
        return db.withTransaction {
            val existing = db.cards().get(id)
            if (existing != null) return@withTransaction SavedBookWord(existing, false)
            val card = CardEntity(
                id = id, deck = Deck.BOOK_WORDS.id, position = db.cards().nextPosition(Deck.BOOK_WORDS.id),
                term = term, ipa = draft.ipa, pos = draft.pos.ifBlank { "word" }, level = draft.level,
                meaningEn = draft.meaningEn, meaningRu = meaning,
                example1 = draft.example.trim(), example2 = "", audio = null,
                extra = json.encodeToString(BookWordSource.serializer(), draft.source),
            )
            val added = db.cards().insertIfMissing(card) != -1L
            SavedBookWord(if (added) card else requireNotNull(db.cards().get(id)), added)
        }
    }

    companion object {
        fun source(card: CardEntity): BookWordSource? =
            if (card.deck != Deck.BOOK_WORDS.id) null
            else card.extra?.let { runCatching { Json.decodeFromString(BookWordSource.serializer(), it) }.getOrNull() }

        fun key(text: String): String = clean(Normalizer.normalize(text, Normalizer.Form.NFC)).lowercase(Locale.ROOT)

        private fun clean(text: String) = text.trim().replace('’', '\'').replace('‘', '\'').replace(Regex("\\s+"), " ")
        private fun termPrefix(term: String) = "${Deck.BOOK_WORDS.id}:${digest(key(term))}:"
        private fun digest(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
