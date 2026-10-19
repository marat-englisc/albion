package com.marat.albion.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Upsert
    suspend fun upsert(cards: List<CardEntity>)

    @Query("SELECT id FROM cards WHERE deck = :deck")
    suspend fun idsOf(deck: String): List<String>

    @Query("DELETE FROM cards WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun get(id: String): CardEntity?

    @Query("SELECT * FROM cards WHERE id = :id")
    fun observe(id: String): Flow<CardEntity?>

    @Query("SELECT COUNT(*) FROM cards")
    fun observeTotal(): Flow<Int>

    @Query(
        """
        SELECT c.* FROM cards c LEFT JOIN review_state r ON r.card_id = c.id
        WHERE c.deck = :deck AND r.card_id IS NULL
        ORDER BY
          CASE WHEN :byLevel THEN
            CASE c.level WHEN 'A1' THEN 1 WHEN 'A2' THEN 2 WHEN 'B1' THEN 3 WHEN 'B2' THEN 4
                         WHEN 'C1' THEN 5 WHEN 'C2' THEN 6 ELSE 7 END
          ELSE 0 END,
          CASE WHEN :random THEN RANDOM() ELSE c.position END
        LIMIT :limit
        """
    )
    suspend fun newCards(deck: String, limit: Int, byLevel: Boolean, random: Boolean): List<CardEntity>

    /** filter: 0 — все, 1 — новые, 2 — изучаются, 3 — на повторении, 4 — к повторению сейчас. */
    @Query(
        """
        SELECT c.* FROM cards c LEFT JOIN review_state r ON r.card_id = c.id
        WHERE c.deck = :deck
          AND (:query = '' OR c.term LIKE '%' || :query || '%' OR c.meaning_ru LIKE '%' || :query || '%')
          AND (:filter = 0
               OR (:filter = 1 AND r.card_id IS NULL)
               OR (:filter = 2 AND r.state IN (1, 3))
               OR (:filter = 3 AND r.state = 2)
               OR (:filter = 4 AND r.due <= :now))
        ORDER BY CASE WHEN :query != '' AND c.term LIKE :query || '%' THEN 0 ELSE 1 END,
                 c.term COLLATE NOCASE, c.position
        """
    )
    fun browse(deck: String, query: String, filter: Int, now: Long): Flow<List<CardEntity>>

    @Query(
        """
        SELECT c.deck AS deck,
               COUNT(*) AS total,
               COUNT(r.card_id) AS started,
               COALESCE(SUM(CASE WHEN r.state = 2 AND r.due <= :now THEN 1 ELSE 0 END), 0) AS due,
               COALESCE(SUM(CASE WHEN r.state IN (1, 3) AND r.due <= :now + 1200000 THEN 1 ELSE 0 END), 0) AS learningDue,
               COALESCE(SUM(CASE WHEN r.state IN (1, 3) THEN 1 ELSE 0 END), 0) AS learning,
               COALESCE(SUM(CASE WHEN r.state = 2 AND r.stability >= 21 THEN 1 ELSE 0 END), 0) AS mature
        FROM cards c LEFT JOIN review_state r ON r.card_id = c.id
        GROUP BY c.deck
        """
    )
    fun observeCounts(now: Long): Flow<List<DeckCounts>>

    /**
     * Случайные карточки для мини-игр: сначала уже начатые (их полезнее закреплять),
     * затем новые. [withAudio] — только карточки с озвучкой.
     */
    @Query(
        """
        SELECT c.* FROM cards c
        LEFT JOIN review_state r ON r.card_id = c.id
        LEFT JOIN game_card_stats g ON g.card_id = c.id AND g.game = :game
        WHERE c.deck = :deck AND (:withAudio = 0 OR c.audio IS NOT NULL)
          AND (:onlyMistakes = 0 OR (g.wrong > 0 AND g.streak < 2))
        ORDER BY CASE
            WHEN g.wrong > 0 AND g.streak < 2 THEN 0
            WHEN r.card_id IS NOT NULL THEN 1
            ELSE 2 END,
          RANDOM()
        LIMIT :limit
        """
    )
    suspend fun gamePool(deck: String, game: String, limit: Int, withAudio: Boolean, onlyMistakes: Boolean): List<CardEntity>

    /** Сколько слов колоды числятся ошибками в игре. */
    @Query(
        """
        SELECT COUNT(*) FROM game_card_stats g JOIN cards c ON c.id = g.card_id
        WHERE g.game = :game AND c.deck = :deck AND g.wrong > 0 AND g.streak < 2
          AND (:withAudio = 0 OR c.audio IS NOT NULL)
        """
    )
    fun observeGameMistakes(deck: String, game: String, withAudio: Boolean): Flow<Int>

    @Query("SELECT * FROM game_card_stats WHERE card_id = :cardId AND game = :game")
    suspend fun gameStat(cardId: String, game: String): GameCardStatEntity?

    @Upsert
    suspend fun upsertGameStat(stat: GameCardStatEntity)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review_state WHERE card_id = :id")
    suspend fun state(id: String): ReviewStateEntity?

    @Query("SELECT * FROM review_state WHERE card_id = :id")
    fun observeState(id: String): Flow<ReviewStateEntity?>

    @Upsert
    suspend fun upsertState(state: ReviewStateEntity)

    @Query("DELETE FROM review_state WHERE card_id = :id")
    suspend fun deleteState(id: String)

    @Query("DELETE FROM review_state WHERE card_id NOT IN (SELECT id FROM cards)")
    suspend fun deleteOrphanStates()

    @Insert
    suspend fun insertLog(log: ReviewLogEntity): Long

    @Query("SELECT * FROM review_log WHERE id = :id")
    suspend fun log(id: Long): ReviewLogEntity?

    @Query("DELETE FROM review_log WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query(
        """
        SELECT c.* FROM cards c JOIN review_state r ON r.card_id = c.id
        WHERE c.deck = :deck AND r.state IN (1, 3) AND r.due <= :until
        ORDER BY r.due
        """
    )
    suspend fun learningDue(deck: String, until: Long): List<CardEntity>

    @Query(
        """
        SELECT c.* FROM cards c JOIN review_state r ON r.card_id = c.id
        WHERE c.deck = :deck AND r.state = 2 AND r.due <= :now
        ORDER BY r.due
        LIMIT :limit
        """
    )
    suspend fun reviewDue(deck: String, now: Long, limit: Int): List<CardEntity>

    /** Слова, на которые отвечали с [since], — чтобы не показывать сегодня их другие значения. */
    @Query("SELECT DISTINCT c.term FROM review_log l JOIN cards c ON c.id = l.card_id WHERE l.deck = :deck AND l.reviewed_at >= :since")
    suspend fun termsAnsweredSince(deck: String, since: Long): List<String>

    @Query("SELECT COUNT(*) FROM review_log WHERE deck = :deck AND state_before = :stateBefore AND reviewed_at >= :since")
    suspend fun countSince(deck: String, stateBefore: Int, since: Long): Int

    @Query("SELECT COUNT(*) FROM review_log WHERE reviewed_at >= :since")
    fun observeReviewsSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(DISTINCT card_id) FROM review_log WHERE reviewed_at >= :since AND state_before = 0")
    fun observeNewSince(since: Long): Flow<Int>

    /** Повторения по «учебным дням». День = floor((t + shift) / сутки), shift учитывает пояс и час начала дня. */
    @Query(
        """
        SELECT ((reviewed_at + :shift) / 86400000) AS day, COUNT(*) AS count
        FROM review_log WHERE reviewed_at >= :since
        GROUP BY day ORDER BY day
        """
    )
    fun observeDaily(since: Long, shift: Long): Flow<List<DayCount>>

    @Query("SELECT DISTINCT ((reviewed_at + :shift) / 86400000) AS day FROM review_log ORDER BY day DESC LIMIT 1000")
    fun observeActiveDays(shift: Long): Flow<List<Long>>

    @Query(
        """
        SELECT ((due + :shift) / 86400000) AS day, COUNT(*) AS count
        FROM review_state WHERE due < :until
        GROUP BY day ORDER BY day
        """
    )
    fun observeForecast(until: Long, shift: Long): Flow<List<DayCount>>

    @Query("SELECT COUNT(*) FROM review_log WHERE state_before = 2 AND reviewed_at >= :since")
    fun observeReviewAnswers(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_log WHERE state_before = 2 AND rating > 1 AND reviewed_at >= :since")
    fun observeReviewPassed(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_log")
    fun observeTotalReviews(): Flow<Int>

    /** Весь журнал — для подстройки параметров FSRS. */
    @Query("SELECT card_id AS cardId, rating, reviewed_at AS time FROM review_log ORDER BY card_id, reviewed_at")
    suspend fun allEvents(): List<com.marat.albion.data.fsrs.ReviewEvent>

    @Query("SELECT COUNT(*) FROM review_log WHERE reviewed_at >= :since")
    suspend fun countAllSince(since: Long): Int

    @Query("SELECT COALESCE(SUM(duration_ms), 0) FROM review_log WHERE reviewed_at >= :since")
    fun observeTimeSince(since: Long): Flow<Long>

    @Transaction
    suspend fun resetDeck(deck: String) {
        deleteStates(deck)
        deleteLogs(deck)
    }

    @Query("DELETE FROM review_state WHERE deck = :deck")
    suspend fun deleteStates(deck: String)

    @Query("DELETE FROM review_log WHERE deck = :deck")
    suspend fun deleteLogs(deck: String)
}

@Dao
interface GrammarDao {
    @Query("SELECT * FROM grammar_progress")
    fun observeAll(): Flow<List<GrammarProgressEntity>>

    @Query("SELECT * FROM grammar_progress WHERE topic_id = :id")
    suspend fun get(id: String): GrammarProgressEntity?

    @Query("SELECT * FROM grammar_progress WHERE topic_id = :id")
    fun observe(id: String): Flow<GrammarProgressEntity?>

    @Upsert
    suspend fun upsert(entity: GrammarProgressEntity)

    @Query("DELETE FROM grammar_progress")
    suspend fun clear()
}

@Dao
interface GrammarTaskDao {
    @Upsert
    suspend fun upsert(tasks: List<GrammarTaskEntity>)

    @Query("SELECT id FROM grammar_tasks")
    suspend fun ids(): List<String>

    @Query("DELETE FROM grammar_tasks WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query("SELECT COUNT(*) FROM grammar_tasks WHERE kind = :kind")
    fun observeCount(kind: String): Flow<Int>

    /**
     * Задания для игры: сначала те, где чаще ошибались, затем новые, затем остальные;
     * внутри групп — случайно. [level] = "" — все уровни.
     */
    @Query(
        """
        SELECT t.* FROM grammar_tasks t LEFT JOIN grammar_task_stats s ON s.task_id = t.id
        WHERE t.kind = :kind AND (:level = '' OR t.level = :level)
        ORDER BY CASE
            WHEN s.task_id IS NOT NULL AND s.wrong > s.correct THEN 0
            WHEN s.task_id IS NULL THEN 1
            ELSE 2 END,
          RANDOM()
        LIMIT :limit
        """
    )
    suspend fun pool(kind: String, level: String, limit: Int): List<GrammarTaskEntity>

    /** Все задания одной темы учебника: сначала те, где ошибались, внутри — случайно. */
    @Query(
        """
        SELECT t.* FROM grammar_tasks t LEFT JOIN grammar_task_stats s ON s.task_id = t.id
        WHERE t.topic = :topic
        ORDER BY CASE WHEN s.task_id IS NOT NULL AND s.wrong > s.correct THEN 0 ELSE 1 END, RANDOM()
        LIMIT :limit
        """
    )
    suspend fun topicPool(topic: Int, limit: Int): List<GrammarTaskEntity>

    @Query("SELECT COUNT(*) FROM grammar_tasks WHERE topic = :topic")
    fun observeTopicCount(topic: Int): Flow<Int>

    @Query("SELECT * FROM grammar_task_stats WHERE task_id = :id")
    suspend fun stat(id: String): GrammarTaskStatEntity?

    @Upsert
    suspend fun upsertStat(stat: GrammarTaskStatEntity)
}

@Dao
interface LibraryDao {
    @Upsert
    suspend fun upsertBook(book: BookEntity)

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun book(id: String): BookEntity?

    @Query("SELECT * FROM books ORDER BY added_at")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM reading_positions")
    fun observePositions(): Flow<List<ReadingPositionEntity>>

    @Query("SELECT * FROM reading_positions WHERE book_id = :id")
    suspend fun position(id: String): ReadingPositionEntity?

    @Upsert
    suspend fun upsertPosition(position: ReadingPositionEntity)

    @Query("SELECT * FROM bookmarks WHERE book_id = :id ORDER BY chapter, paragraph, `offset`")
    fun observeBookmarks(id: String): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("SELECT * FROM highlights WHERE book_id = :id ORDER BY chapter, paragraph, start")
    fun observeHighlights(id: String): Flow<List<HighlightEntity>>

    @Insert
    suspend fun insertHighlight(h: HighlightEntity): Long

    @Query("UPDATE highlights SET note = :note WHERE id = :id")
    suspend fun updateHighlightNote(id: Long, note: String)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Long)

    @Query("DELETE FROM highlights WHERE book_id = :id")
    suspend fun deleteHighlights(id: String)

    @Transaction
    suspend fun deleteBook(id: String) {
        deleteBookRow(id)
        deletePosition(id)
        deleteBookmarks(id)
        deleteHighlights(id)
    }

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookRow(id: String)

    @Query("DELETE FROM reading_positions WHERE book_id = :id")
    suspend fun deletePosition(id: String)

    @Query("DELETE FROM bookmarks WHERE book_id = :id")
    suspend fun deleteBookmarks(id: String)
}

/** Карточки по словам — для словарной справки в читалке. */
@Dao
interface LookupDao {
    @Query("SELECT * FROM cards WHERE lower(term) IN (:terms) ORDER BY CASE deck WHEN 'words' THEN 0 WHEN 'irregular_verbs' THEN 1 ELSE 2 END, position LIMIT 3")
    suspend fun findByTerms(terms: List<String>): List<CardEntity>

    /** Термины начатых, но ещё не выученных карточек (стабильность меньше 21 дня) — для подсветки в книгах. */
    @Query(
        """
        SELECT c.term FROM cards c JOIN review_state r ON r.card_id = c.id
        WHERE NOT (r.state = 2 AND r.stability >= 21)
        """
    )
    fun observeStudiedTerms(): Flow<List<String>>

    @Query("SELECT extra FROM cards WHERE deck = 'irregular_verbs' AND extra IS NOT NULL")
    suspend fun irregularForms(): List<String>
}

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary WHERE term IN (:terms)")
    suspend fun find(terms: List<String>): List<DictionaryEntity>

    @Query("SELECT COUNT(*) FROM dictionary")
    suspend fun count(): Int

    @Query("DELETE FROM dictionary")
    suspend fun clear()

    @Insert
    suspend fun insert(rows: List<DictionaryEntity>)
}

