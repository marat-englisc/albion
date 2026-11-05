package com.marat.albion.data.library

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

enum class BookTranslationPhase { Preparing, Ready, Failed }

data class BookTranslationStatus(
    val phase: BookTranslationPhase = BookTranslationPhase.Preparing,
    val message: String? = null,
) {
    val ready get() = phase == BookTranslationPhase.Ready
    val summary get() = when (phase) {
        BookTranslationPhase.Preparing -> "Подготавливаю офлайн-перевод…"
        BookTranslationPhase.Ready -> "Английский → русский · работает офлайн"
        BookTranslationPhase.Failed -> "Не удалось подготовить перевод"
    }
}

/** Kept separate from Qwen: reading never loads or depends on the conversation model. */
class BookTranslator internal constructor(
    private val scope: CoroutineScope,
    private val backend: BookTranslationBackend,
) {
    constructor(scope: CoroutineScope) : this(scope, MlKitBookTranslationBackend())

    private val preparation = Mutex()
    private val translation = Mutex()
    private val _state = MutableStateFlow(BookTranslationStatus())
    val state = _state.asStateFlow()
    private val cache = object : LinkedHashMap<String, String>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 200
    }

    /** ML Kit keeps downloaded language models across app restarts and updates. */
    fun preload() {
        scope.launch {
            try { prepare() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* The state exposes the error and the reader can retry. */ }
        }
    }

    suspend fun prepare() = preparation.withLock {
        if (_state.value.ready) return@withLock
        _state.value = BookTranslationStatus()
        try {
            backend.prepare()
            _state.value = BookTranslationStatus(BookTranslationPhase.Ready)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val message = "Не удалось загрузить языковую модель. Проверьте подключение к интернету и свободное место, затем повторите попытку."
            _state.value = BookTranslationStatus(BookTranslationPhase.Failed, message)
            throw IllegalStateException(message, error)
        }
    }

    suspend fun translate(text: String): String {
        val selected = text.trim()
        require(selected.isNotEmpty()) { "Выберите слово или фрагмент для перевода." }
        require(selected.length <= MAX_SELECTION) { "Выделите более короткий фрагмент: до $MAX_SELECTION символов." }
        prepare()
        return translation.withLock {
            cache[selected]?.let { return@withLock it }
            val result = try { backend.translate(selected).trim() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                throw IllegalStateException("Не удалось перевести фрагмент. Попробуйте ещё раз или выберите более короткий текст.", error)
            }
            check(result.isNotEmpty()) { "Перевод получился пустым. Попробуйте другой фрагмент." }
            check(!selected.any { it in 'a'..'z' || it in 'A'..'Z' } || !result.equals(selected, ignoreCase = true)) {
                "Для этого фрагмента нет автоперевода. Выберите словарное значение или укажите свой перевод."
            }
            cache[selected] = result
            result
        }
    }

    companion object {
        const val MAX_SELECTION = 1_800
    }
}

internal interface BookTranslationBackend {
    suspend fun prepare()
    suspend fun translate(text: String): String
}

private class MlKitBookTranslationBackend : BookTranslationBackend {
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.RUSSIAN)
        .build()

    // Clients are closed after every operation; only ML Kit's on-disk model is retained.
    override suspend fun prepare() {
        Translation.getClient(options).use { client ->
            client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        }
    }

    override suspend fun translate(text: String): String = Translation.getClient(options).use { client ->
        client.translate(text).await()
    }
}
