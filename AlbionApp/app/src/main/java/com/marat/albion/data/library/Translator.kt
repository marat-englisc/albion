package com.marat.albion.data.library

import android.content.Context
import com.marat.albion.data.translation.LlamaEngine
import com.marat.albion.data.translation.TranslationModels
import com.marat.albion.data.translation.TranslationProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import java.util.Collections

/** Offline EN → RU through Qwen3.5; dictionary lookup remains independent of the model. */
class Translator(context: Context, private val scope: CoroutineScope,
                 /** Shared with conversation practice: one Qwen3.5 in memory at a time. */
                 val engine: LlamaEngine = LlamaEngine(context.applicationContext, scope)) {
    val models = TranslationModels(context.applicationContext, scope)
    val model = models.state
    val loading = engine.loading
    private val cache: MutableMap<String, String> = Collections.synchronizedMap(object : LinkedHashMap<String, String>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 200
    })
    /** Restore downloads, never silently download hundreds of megabytes or load weights at startup. */
    fun preload() { scope.launch { models.refresh() } }
    fun releaseMemory() { scope.launch { engine.unload() } }
    suspend fun unload() = engine.unload()

    fun translations(text: String, context: String = ""): Flow<String> = flow {
        val prompt = TranslationProtocol.prompt(text, context)
        val (file, identity) = models.activeModel()
            ?: error("Установите модель Qwen3.5 для перевода предложений. Словарь уже доступен.")
        val key = identity + "\n" + prompt
        cache[key]?.let { emit(it); return@flow }
        var complete = ""
        engine.generate(file, prompt).collect { raw ->
            complete = TranslationProtocol.clean(raw)
            if (complete.isNotBlank()) emit(complete)
        }
        check(complete.isNotBlank()) { "Модель вернула пустой перевод. Попробуйте другой фрагмент." }
        cache[key] = complete
    }
    suspend fun translate(text: String, context: String = ""): String = translations(text, context).last()
}
