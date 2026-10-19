package com.marat.albion.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SpeechState {
    data object Idle : SpeechState
    data class Listening(val level: Float, val partial: String) : SpeechState
    data object Processing : SpeechState
    /** Варианты распознавания, от наиболее вероятного. */
    data class Heard(val variants: List<String>) : SpeechState
    data class Failed(val message: String) : SpeechState
}

/**
 * Распознавание речи встроенным сервисом Android (бесплатно, без ключей).
 * Просим британский английский и офлайн-режим: если на телефоне скачан
 * языковой пакет, всё работает без интернета. Создавать и вызывать — на главном потоке.
 */
class SpeechChecker(private val context: Context) {

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    /** Язык и режим попытки: офлайн en-GB → онлайн en-GB → онлайн en-US. */
    private data class Attempt(val language: String, val offline: Boolean)

    private val attempts = listOf(Attempt("en-GB", true), Attempt("en-GB", false), Attempt("en-US", false))

    fun listen() = listen(0)

    private fun listen(attempt: Int) {
        cancel()
        if (!isAvailable) {
            _state.value = SpeechState.Failed("На устройстве нет сервиса распознавания речи")
            return
        }
        val config = attempts[attempt]
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _state.value = SpeechState.Listening(0f, "") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                val s = _state.value
                // rmsdB обычно в диапазоне −2…10 дБ.
                val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                if (s is SpeechState.Listening) _state.value = s.copy(level = level)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _state.value = SpeechState.Processing }
            override fun onError(error: Int) {
                // 11 — нет связи с сервером, 12 — язык не поддерживается, 13 — языковой пакет не скачан: пробуем следующий вариант.
                if ((error == 11 || error == 12 || error == 13) && attempt + 1 < attempts.size) {
                    listen(attempt + 1)
                    return
                }
                _state.value = SpeechState.Failed(
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Не расслышал — попробуйте ещё раз"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет доступа к микрофону"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER, 11 ->
                            "Нужен интернет или офлайн-пакет английского языка в настройках Google"
                        SpeechRecognizer.ERROR_AUDIO -> "Ошибка микрофона"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознавание занято, попробуйте ещё раз"
                        12, 13 -> "Английский не установлен для распознавания. Скачайте его: Настройки → Google → Распознавание речи"
                        else -> "Ошибка распознавания ($error)"
                    }
                )
            }
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                _state.value = if (list.isEmpty()) SpeechState.Failed("Не расслышал — попробуйте ещё раз") else SpeechState.Heard(list)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                val s = _state.value
                if (s is SpeechState.Listening && text.isNotBlank()) _state.value = s.copy(partial = text)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, config.language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, config.offline)
        }
        _state.value = SpeechState.Listening(0f, "")
        r.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun reset() {
        cancel()
        _state.value = SpeechState.Idle
    }

    private fun cancel() {
        recognizer?.let { runCatching { it.cancel() }; it.destroy() }
        recognizer = null
    }

    fun release() = cancel()
}
