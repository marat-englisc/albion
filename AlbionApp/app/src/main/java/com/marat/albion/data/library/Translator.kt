package com.marat.albion.data.library

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class ModelState { Unknown, Missing, Downloading, Ready, Failed }

/**
 * Перевод фраз EN → RU на устройстве через Google ML Kit. Бесплатно и без лимитов.
 *
 * Google не разрешает класть модель (~30 МБ) внутрь APK, поэтому приложение само скачивает её
 * в фоне при первом запуске — как только появляется сеть, без участия пользователя.
 * Перевод отдельных слов от модели не зависит: для них есть встроенный словарь [Dictionary].
 */
class Translator(private val context: Context, private val scope: CoroutineScope) {
    // Клиенты создаются лениво: приложение запускается, даже если с ML Kit что-то не так.
    private val client by lazy {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.RUSSIAN)
                .build()
        )
    }
    private val russian by lazy { TranslateRemoteModel.Builder(TranslateLanguage.RUSSIAN).build() }
    private val models by lazy { RemoteModelManager.getInstance() }
    private val mutex = Mutex()
    private val cache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 500
    }
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _model = MutableStateFlow(ModelState.Unknown)
    val model: StateFlow<ModelState> = _model.asStateFlow()

    suspend fun refresh() {
        if (_model.value == ModelState.Downloading) return
        _model.value = runCatching { if (models.isModelDownloaded(russian).await()) ModelState.Ready else ModelState.Missing }
            .getOrDefault(ModelState.Missing)
    }

    /**
     * Фоновая подготовка при запуске приложения: если модели нет — скачиваем сразу,
     * а без сети ждём её появления и пробуем снова.
     */
    fun preload() {
        scope.launch {
            refresh()
            if (_model.value == ModelState.Ready) return@launch
            if (!ensureModel()) watchNetwork()
        }
    }

    private fun watchNetwork() {
        if (networkCallback != null) return
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val self = this
                scope.launch {
                    if (ensureModel()) {
                        runCatching { cm.unregisterNetworkCallback(self) }
                        networkCallback = null
                    }
                }
            }
        }
        networkCallback = callback
        runCatching { cm.registerDefaultNetworkCallback(callback) }.onFailure { networkCallback = null }
    }

    /** Скачивает модель, если её ещё нет. true — можно переводить. */
    suspend fun ensureModel(): Boolean = mutex.withLock {
        if (_model.value == ModelState.Ready) return true
        _model.value = ModelState.Downloading
        return runCatching { client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await() }
            .fold(
                onSuccess = { _model.value = ModelState.Ready; true },
                onFailure = { _model.value = ModelState.Failed; false },
            )
    }

    /** Ждёт окончания фоновой загрузки, если она идёт. */
    suspend fun awaitDownload(): Boolean =
        model.first { it != ModelState.Downloading && it != ModelState.Unknown } == ModelState.Ready

    suspend fun translate(text: String): String {
        val key = text.trim()
        cache[key]?.let { return it }
        val ready = when (_model.value) {
            ModelState.Ready -> true
            ModelState.Downloading, ModelState.Unknown -> awaitDownload() || ensureModel()
            else -> ensureModel()
        }
        if (!ready) error("Модель перевода фраз ещё не загружена — она скачается сама, как только появится интернет")
        val result = runCatching { client.translate(key).await() }.getOrElse { error("Не удалось перевести фразу") }
        cache[key] = result
        return result
    }

    suspend fun deleteModel() {
        runCatching { models.deleteDownloadedModel(russian).await() }
        cache.clear()
        _model.value = ModelState.Missing
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
