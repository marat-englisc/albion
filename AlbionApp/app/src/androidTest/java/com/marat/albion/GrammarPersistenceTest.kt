package com.marat.albion

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.db.AlbionDatabase
import com.marat.albion.data.grammar.GrammarQuizResult
import com.marat.albion.data.grammar.GrammarRepository
import com.marat.albion.data.grammar.GrammarReview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrammarPersistenceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun clearingOnlyReadMarksPreservesMasteryQuizBookmarksAndReviewPlan() = runBlocking {
        val context = instrumentation.targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AlbionDatabase::class.java).build()
        try {
            val repo = GrammarRepository(context.assets, db)
            repo.markOpened("safe-reset")
            repo.setRead("safe-reset", true)
            repo.toggleBookmark("safe-reset")
            repo.recordQuiz("safe-reset", GrammarQuizResult(9, 10, true))
            val before = db.grammar().get("safe-reset")!!
            repo.resetReadMarks()
            assertEquals(before.copy(readAt = null), db.grammar().get("safe-reset"))
            repo.resetProgress()
            assertNull(db.grammar().get("safe-reset"))
        } finally { db.close() }
    }

    @Test
    fun readingRetryAndBookmarkChangesCannotAwardMastery() = runBlocking {
        val context = instrumentation.targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AlbionDatabase::class.java).build()
        try {
            val repo = GrammarRepository(context.assets, db)
            repo.markOpened("topic")
            repo.setRead("topic", true)
            repo.toggleBookmark("topic")
            val read = db.grammar().get("topic")!!
            assertNotNull(read.readAt)
            assertNull(read.masteredAt)
            repo.recordQuiz("topic", GrammarQuizResult(2, 2, false))
            assertEquals(read, db.grammar().get("topic"))
            repo.recordQuiz("topic", GrammarQuizResult(0, 0, true))
            assertEquals(read, db.grammar().get("topic"))

            repo.recordQuiz("topic", GrammarQuizResult(7, 10, true))
            val passed = db.grammar().get("topic")!!
            assertNotNull(passed.masteredAt)
            assertEquals(7, passed.quizCorrect)
            assertEquals(10, passed.quizTotal)
            assertEquals(read.readAt, passed.readAt)
            assertTrue(passed.bookmarked)

            repo.setRead("topic", false)
            repo.markOpened("topic")
            repo.toggleBookmark("topic")
            assertEquals(passed.masteredAt, db.grammar().get("topic")!!.masteredAt)

            repo.recordQuiz("topic", GrammarQuizResult(6, 10, true))
            val failed = db.grammar().get("topic")!!
            assertNull(failed.masteredAt)
            assertEquals(6, failed.quizCorrect)
            repo.recordQuiz("topic", GrammarQuizResult(4, 4, false))
            assertEquals(failed, db.grammar().get("topic"))
        } finally { db.close() }
    }

    @Test
    fun concurrentUpdatesPreserveOtherProgressFields() = runBlocking {
        val context = instrumentation.targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AlbionDatabase::class.java).build()
        try {
            val repo = GrammarRepository(context.assets, db)
            listOf(
                async { repo.markOpened("topic") },
                async { repo.setRead("topic", true) },
                async { repo.toggleBookmark("topic") },
                async { repo.recordQuiz("topic", GrammarQuizResult(7, 10, true)) },
            ).awaitAll()
            val p = db.grammar().get("topic")!!
            assertNotNull(p.openedAt)
            assertNotNull(p.readAt)
            assertNotNull(p.masteredAt)
            assertEquals(7, p.quizCorrect)
            assertTrue(p.bookmarked)
            assertEquals(1, p.reviewStage)
            assertEquals(p.quizAt!! + 86_400_000L, p.reviewAt)
        } finally { db.close() }
    }

    @Test
    fun fullTestsPersistThePlanWhileReadingAndMistakeRetriesLeaveItAlone() = runBlocking {
        val context = instrumentation.targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AlbionDatabase::class.java).build()
        val day = 86_400_000L
        var time = 1_000L * day
        try {
            val repo = GrammarRepository(context.assets, db, now = { time })
            repo.recordQuiz("topic", GrammarQuizResult(7, 10, true))
            val first = db.grammar().get("topic")!!
            assertEquals(1, first.reviewStage)
            assertEquals(time + day, first.reviewAt)

            time += 1_000
            repo.recordQuiz("topic", GrammarQuizResult(10, 10, true))
            assertEquals(first.reviewAt, db.grammar().get("topic")!!.reviewAt)
            assertEquals(1, db.grammar().get("topic")!!.reviewStage)
            repo.setRead("topic", true)
            repo.toggleBookmark("topic")
            val beforeRetry = db.grammar().get("topic")!!
            repo.recordQuiz("topic", GrammarQuizResult(3, 3, false))
            assertEquals(beforeRetry, db.grammar().get("topic"))

            time = first.reviewAt!!
            repo.recordQuiz("topic", GrammarQuizResult(8, 10, true))
            val second = db.grammar().get("topic")!!
            assertEquals(2, second.reviewStage)
            assertEquals(time + 3 * day, second.reviewAt)
            assertEquals(beforeRetry.readAt, second.readAt)
            assertTrue(second.bookmarked)

            repo.recordQuiz("topic", GrammarQuizResult(6, 10, true))
            val failed = db.grammar().get("topic")!!
            assertEquals(0, failed.reviewStage)
            assertEquals(time + day, failed.reviewAt)
            assertNull(failed.masteredAt)
            repo.resetProgress()
            assertNull(db.grammar().get("topic"))
        } finally { db.close() }
    }

    @Test
    fun migrationFromVersionSixKeepsOldMarksAsRead() = runBlocking {
        val context = instrumentation.targetContext
        val name = createLegacyDatabase(6, "INSERT INTO grammar_progress (topic_id, read_at, bookmarked, opened_at) VALUES ('legacy', 100, 1, 200)")
        val db = Room.databaseBuilder(context, AlbionDatabase::class.java, name).build()
        try {
            val p = db.grammar().get("legacy")!!
            assertEquals(100L, p.readAt)
            assertEquals(200L, p.openedAt)
            assertTrue(p.bookmarked)
            assertNull(p.masteredAt)
            assertNull(p.quizAt)
            assertNull(p.quizCorrect)
            assertNull(p.quizTotal)
            assertEquals(0, p.reviewStage)
            assertNull(p.reviewAt)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migrationFromVersionSevenPreservesScoresAndRestoresTheReviewPlan() = runBlocking {
        val context = instrumentation.targetContext
        val name = createLegacyDatabase(7, "INSERT INTO grammar_progress (topic_id, read_at, bookmarked, opened_at, mastered_at, quiz_at, quiz_correct, quiz_total) VALUES ('legacy', 100, 1, 200, 300, 300, 7, 10)")
        val db = Room.databaseBuilder(context, AlbionDatabase::class.java, name).build()
        try {
            val p = db.grammar().get("legacy")!!
            assertEquals(100L, p.readAt)
            assertEquals(200L, p.openedAt)
            assertTrue(p.bookmarked)
            assertEquals(300L, p.masteredAt)
            assertEquals(300L, p.quizAt)
            assertEquals(7, p.quizCorrect)
            assertEquals(10, p.quizTotal)
            assertEquals(GrammarReview.Plan(1, 300L + 86_400_000L), GrammarReview.plan(p))
            GrammarRepository(context.assets, db, now = { 3 * 86_400_000L }).recordQuiz("legacy", GrammarQuizResult(8, 10, true))
            assertEquals(2, db.grammar().get("legacy")!!.reviewStage)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    private fun createLegacyDatabase(version: Int, insertSql: String): String {
        val name = "grammar-migration-${System.nanoTime()}.db"
        val file = instrumentation.targetContext.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val schema = instrumentation.context.assets.open("com.marat.albion.data.db.AlbionDatabase/$version.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val tables = schema.getJSONArray("entities")
            for (i in 0 until tables.length()) {
                val table = tables.getJSONObject(i)
                val tableName = table.getString("tableName")
                old.execSQL(table.getString("createSql").replace("\${TABLE_NAME}", tableName))
                val indices = table.optJSONArray("indices")
                for (j in 0 until (indices?.length() ?: 0)) {
                    old.execSQL(indices!!.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tableName))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL(insertSql)
            old.version = version
        }
        return name
    }
}
