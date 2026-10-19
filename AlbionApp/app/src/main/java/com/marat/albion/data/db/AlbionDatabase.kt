package com.marat.albion.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CardEntity::class, ReviewStateEntity::class, ReviewLogEntity::class, GrammarProgressEntity::class,
        GrammarTaskEntity::class, GrammarTaskStatEntity::class,
        BookEntity::class, ReadingPositionEntity::class, BookmarkEntity::class, DictionaryEntity::class, HighlightEntity::class, GameCardStatEntity::class,
    ],
    version = 6,
    exportSchema = true,
    // v2: грамматические задания; v3: библиотека; v4: офлайн-словарь; v5: выделения в книгах; v6: ошибки в мини-играх. Прогресс карточек сохраняется.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6)],
)
abstract class AlbionDatabase : RoomDatabase() {
    abstract fun cards(): CardDao
    abstract fun reviews(): ReviewDao
    abstract fun grammar(): GrammarDao
    abstract fun grammarTasks(): GrammarTaskDao
    abstract fun library(): LibraryDao
    abstract fun lookup(): LookupDao
    abstract fun dictionary(): DictionaryDao

    companion object {
        fun create(context: Context): AlbionDatabase =
            Room.databaseBuilder(context, AlbionDatabase::class.java, "albion.db").build()
    }
}
