package com.marat.albion.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** История ответов для календаря активности; накопленные итоги остаются в прежних таблицах. */
@Entity(tableName = "game_answer_log", indices = [Index("answered_at")])
data class GameAnswerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val game: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    val correct: Boolean,
    @ColumnInfo(name = "answered_at") val answeredAt: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
)

data class GameTotals(val game: String, val correct: Int, val wrong: Int) {
    val answers get() = correct + wrong
    val accuracy get() = if (answers == 0) null else correct.toFloat() / answers
}

@Dao
interface GameActivityDao {
    @Insert
    suspend fun insert(answer: GameAnswerLogEntity)

    /** Сохраняем в сводке также ответы, сделанные до появления журнала по дням. */
    @Query("""
        SELECT game, SUM(correct) AS correct, SUM(wrong) AS wrong FROM (
            SELECT game, correct, wrong FROM game_card_stats
            UNION ALL
            SELECT CASE WHEN t.kind = 'input' THEN 'GRAMMAR_GAP' ELSE 'GRAMMAR_CHOICE' END AS game,
                   s.correct, s.wrong
            FROM grammar_task_stats s JOIN grammar_tasks t ON t.id = s.task_id
        ) GROUP BY game
    """)
    fun observeTotals(): Flow<List<GameTotals>>

    @Query("""
        SELECT ((answered_at + :shift) / 86400000) AS day, COUNT(*) AS count
        FROM game_answer_log WHERE answered_at >= :since GROUP BY day ORDER BY day
    """)
    fun observeDaily(since: Long, shift: Long): Flow<List<DayCount>>

    @Query("SELECT DISTINCT ((answered_at + :shift) / 86400000) AS day FROM game_answer_log ORDER BY day DESC LIMIT 1000")
    fun observeActiveDays(shift: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM game_answer_log WHERE answered_at >= :since")
    fun observeAnswersSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM game_answer_log WHERE answered_at >= :since")
    suspend fun countAnswersSince(since: Long): Int

    @Query("SELECT COALESCE(SUM(duration_ms), 0) FROM game_answer_log WHERE answered_at >= :since")
    fun observeTimeSince(since: Long): Flow<Long>
}
