package com.marat.albion.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "albion_settings")

enum class NewCardOrder(val title: String) {
    LEVEL("По уровню CEFR"), FILE("Как в таблице"), RANDOM("Случайно")
}

enum class ThemeMode(val title: String) {
    DARK("Тёмная"), LIGHT("Светлая"), SYSTEM("Как в системе")
}

data class Settings(
    val desiredRetention: Float = 0.9f,
    val newPerDay: Int = 20,
    val reviewsPerDay: Int = 200,
    val newOrder: NewCardOrder = NewCardOrder.LEVEL,
    val autoplayFront: Boolean = true,
    val autoplayBack: Boolean = false,
    val showIpaOnFront: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    /** Громкость озвучки 0…1. */
    val playbackVolume: Float = 1.0f,
    val grammarTextScale: Float = 1.0f,
    /** Час, с которого начинается новый учебный день (как в Anki). */
    val dayStartHour: Int = 4,
    val lastGrammarTopic: String? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Показывать русское значение на обороте карточек. */
    val showRussian: Boolean = true,
    /** Дневная цель — ответов в день по всем колодам. */
    val dailyGoal: Int = 30,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    /** Напоминать, только если цель ещё не выполнена или есть карточки к повторению. */
    val reminderOnlyIfNeeded: Boolean = true,
    /** Персональные параметры FSRS (21 число через запятую) или null — стандартные. */
    val fsrsParams: String? = null,
    val fsrsUpdatedAt: Long = 0,
    val fsrsReviews: Int = 0,
    val fsrsImprovement: Float = 0f,
    /** Масштаб текста в читалке. */
    val readerTextScale: Float = 1.0f,
    /** Подчёркивать в книгах слова из начатых карточек. */
    val readerMarkWords: Boolean = true,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val retention = floatPreferencesKey("desired_retention")
        val newPerDay = intPreferencesKey("new_per_day")
        val reviewsPerDay = intPreferencesKey("reviews_per_day")
        val newOrder = stringPreferencesKey("new_order")
        val autoplayFront = booleanPreferencesKey("autoplay_front")
        val autoplayBack = booleanPreferencesKey("autoplay_back")
        val showIpaFront = booleanPreferencesKey("show_ipa_front")
        val speed = floatPreferencesKey("playback_speed")
        val volume = floatPreferencesKey("playback_volume")
        val grammarScale = floatPreferencesKey("grammar_text_scale")
        val dayStart = intPreferencesKey("day_start_hour")
        val lastTopic = stringPreferencesKey("last_grammar_topic")
        val contentVersion = stringPreferencesKey("content_version")
        val dictionaryVersion = intPreferencesKey("dictionary_version")
        val themeMode = stringPreferencesKey("theme_mode")
        val showRussian = booleanPreferencesKey("show_russian")
        val dailyGoal = intPreferencesKey("daily_goal")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val reminderOnlyIfNeeded = booleanPreferencesKey("reminder_only_if_needed")
        val fsrsParams = stringPreferencesKey("fsrs_params")
        val fsrsUpdatedAt = androidx.datastore.preferences.core.longPreferencesKey("fsrs_updated_at")
        val fsrsReviews = intPreferencesKey("fsrs_reviews")
        val fsrsImprovement = floatPreferencesKey("fsrs_improvement")
        val readerScale = floatPreferencesKey("reader_text_scale")
        val readerMarkWords = booleanPreferencesKey("reader_mark_words")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            desiredRetention = p[Keys.retention] ?: 0.9f,
            newPerDay = p[Keys.newPerDay] ?: 20,
            reviewsPerDay = p[Keys.reviewsPerDay] ?: 200,
            newOrder = p[Keys.newOrder]?.let { runCatching { NewCardOrder.valueOf(it) }.getOrNull() } ?: NewCardOrder.LEVEL,
            autoplayFront = p[Keys.autoplayFront] ?: true,
            autoplayBack = p[Keys.autoplayBack] ?: false,
            showIpaOnFront = p[Keys.showIpaFront] ?: true,
            playbackSpeed = p[Keys.speed] ?: 1.0f,
            playbackVolume = p[Keys.volume] ?: 1.0f,
            grammarTextScale = p[Keys.grammarScale] ?: 1.0f,
            dayStartHour = p[Keys.dayStart] ?: 4,
            lastGrammarTopic = p[Keys.lastTopic],
            themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            showRussian = p[Keys.showRussian] ?: true,
            dailyGoal = p[Keys.dailyGoal] ?: 30,
            reminderEnabled = p[Keys.reminderEnabled] ?: false,
            reminderHour = p[Keys.reminderHour] ?: 19,
            reminderMinute = p[Keys.reminderMinute] ?: 0,
            reminderOnlyIfNeeded = p[Keys.reminderOnlyIfNeeded] ?: true,
            fsrsParams = p[Keys.fsrsParams],
            fsrsUpdatedAt = p[Keys.fsrsUpdatedAt] ?: 0,
            fsrsReviews = p[Keys.fsrsReviews] ?: 0,
            fsrsImprovement = p[Keys.fsrsImprovement] ?: 0f,
            readerTextScale = p[Keys.readerScale] ?: 1.0f,
            readerMarkWords = p[Keys.readerMarkWords] ?: true,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setRetention(v: Float) = edit { it[Keys.retention] = v }
    suspend fun setNewPerDay(v: Int) = edit { it[Keys.newPerDay] = v }
    suspend fun setReviewsPerDay(v: Int) = edit { it[Keys.reviewsPerDay] = v }
    suspend fun setNewOrder(v: NewCardOrder) = edit { it[Keys.newOrder] = v.name }
    suspend fun setAutoplayFront(v: Boolean) = edit { it[Keys.autoplayFront] = v }
    suspend fun setAutoplayBack(v: Boolean) = edit { it[Keys.autoplayBack] = v }
    suspend fun setShowIpaOnFront(v: Boolean) = edit { it[Keys.showIpaFront] = v }
    suspend fun setPlaybackSpeed(v: Float) = edit { it[Keys.speed] = v }
    suspend fun setPlaybackVolume(v: Float) = edit { it[Keys.volume] = v }
    suspend fun setGrammarTextScale(v: Float) = edit { it[Keys.grammarScale] = v }
    suspend fun setDayStartHour(v: Int) = edit { it[Keys.dayStart] = v }
    suspend fun setLastGrammarTopic(v: String) = edit { it[Keys.lastTopic] = v }
    suspend fun setThemeMode(v: ThemeMode) = edit { it[Keys.themeMode] = v.name }
    suspend fun setShowRussian(v: Boolean) = edit { it[Keys.showRussian] = v }
    suspend fun setDailyGoal(v: Int) = edit { it[Keys.dailyGoal] = v }
    suspend fun setReminderEnabled(v: Boolean) = edit { it[Keys.reminderEnabled] = v }
    suspend fun setReminderTime(hour: Int, minute: Int) = edit { it[Keys.reminderHour] = hour; it[Keys.reminderMinute] = minute }
    suspend fun setReminderOnlyIfNeeded(v: Boolean) = edit { it[Keys.reminderOnlyIfNeeded] = v }

    suspend fun setReaderTextScale(v: Float) = edit { it[Keys.readerScale] = v }
    suspend fun setReaderMarkWords(v: Boolean) = edit { it[Keys.readerMarkWords] = v }

    suspend fun setFsrsParams(params: String?, reviews: Int, improvement: Float) = edit {
        if (params == null) {
            it.remove(Keys.fsrsParams); it.remove(Keys.fsrsUpdatedAt); it.remove(Keys.fsrsReviews); it.remove(Keys.fsrsImprovement)
        } else {
            it[Keys.fsrsParams] = params
            it[Keys.fsrsUpdatedAt] = System.currentTimeMillis()
            it[Keys.fsrsReviews] = reviews
            it[Keys.fsrsImprovement] = improvement
        }
    }

    suspend fun contentVersion(): String? = context.dataStore.data.first()[Keys.contentVersion]
    suspend fun setContentVersion(v: String) = edit { it[Keys.contentVersion] = v }
    suspend fun dictionaryVersion(): Int = context.dataStore.data.first()[Keys.dictionaryVersion] ?: 0
    suspend fun setDictionaryVersion(v: Int) = edit { it[Keys.dictionaryVersion] = v }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
