package com.marat.albion

import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.translation.*
import com.marat.albion.ui.library.TranslationModelControls
import com.marat.albion.ui.components.Panel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.AlbionTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LlamaTranslationTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val c get() = (instrumentation.targetContext.applicationContext as AlbionApp).container

    @Test fun modelControlsShowInstalledAndMissingChoices(): Unit = runBlocking {
        c.translator.models.refresh()
        c.translator.models.select("0.8B")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CompositionLocalProvider(LocalAppContainer provides c) {
                    AlbionTheme(darkTheme = true) {
                        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                            .verticalScroll(rememberScrollState()).padding(20.dp)) {
                            Panel { TranslationModelControls() }
                        }
                    }
                }
            } }
            val anchor = await { it.contentDescription?.toString() == "Модель Qwen3.5" }
            click(anchor)
            click(await { it.text?.toString() == QWEN_MODELS.last().title })
            withTimeout(10_000) { c.translator.model.first { it.selected == "2B" } }
            await { it.text?.toString() == "Загрузить модель" }
            SystemClock.sleep(400)
            capture("model-controls-missing-2b")
            click(await { it.contentDescription?.toString() == "Модель Qwen3.5" })
            click(await { it.text?.toString() == QWEN_MODELS.first().title })
            withTimeout(10_000) { c.translator.model.first { it.selected == "0.8B" } }
            capture("model-controls-08b")
        }
    }

    @Test fun primary2BTranslatesProseAndResolvesWordContext(): Unit = runBlocking {
        val path = InstrumentationRegistry.getArguments().getString("primaryModelPath")
        assumeTrue("Pass -e primaryModelPath with a real Qwen3.5-2B model", path != null)
        val fixture = File(requireNotNull(path))
        assertEquals(QWEN_MODELS.last().bytes, fixture.length())
        val engine = LlamaEngine(instrumentation.targetContext, c.appScope)
        try {
            val prose = withTimeout(180_000) { engine.generate(fixture, TranslationProtocol.prompt("The book is on the table.")).last() }
            Log.i("LlamaTranslationTest", "Qwen3.5-2B prose -> $prose")
            assertRussian(prose)
            assertTrue(prose.contains("книг", true) && prose.contains("стол", true))
            val word = withTimeout(180_000) { engine.generate(fixture,
                TranslationProtocol.prompt("bank", "We sat on the bank of the river and watched the boats.")).last() }
            Log.i("LlamaTranslationTest", "Qwen3.5-2B contextual bank -> $word")
            assertTrue("Word context was ignored: $word", word.contains("берег", true))
        } finally { engine.unload() }
    }

    /** Opt-in real weights, never a mock or a model download during a normal CI run. */
    @Test fun realQwenImportsTranslatesAndCancels(): Unit = runBlocking {
        val path = InstrumentationRegistry.getArguments().getString("modelPath")
        assumeTrue("Pass -e modelPath with the Qwen3.5 GGUF fixture", path != null)
        val fixture = File(requireNotNull(path))
        assertTrue("Missing real model fixture", fixture.isFile)
        c.importer.importIfNeeded()
        c.translator.models.refresh()
        c.translator.models.importFrom(Uri.fromFile(fixture))
        withTimeout(90_000) { c.translator.model.first { it.phase == ModelState.Ready && it.selected == "custom" } }
        val original = "The book is on the table."
        val translated = withTimeout(120_000) { c.translator.translate(original) }
        Log.i("LlamaTranslationTest", "$original -> $translated")
        assertRussian(translated)
        assertTrue(translated.contains("книг", true))
        assertTrue(translated.contains("стол", true))
        assertEquals(translated, withTimeout(2000) { c.translator.translate(original) })

        val request = launch(Dispatchers.Default) {
            c.translator.translations("She walked slowly through the forest and listened to the birds. ".repeat(20))
                .collect { }
        }
        delay(300)
        withTimeout(20_000) { request.cancelAndJoin() }
        assertRussian(withTimeout(120_000) { c.translator.translate("Good morning!") })
        c.translator.unload()

        val engine = LlamaEngine(instrumentation.targetContext, c.appScope)
        try {
            try {
                withTimeout(90_000) { engine.generate(fixture, "hello ".repeat(3000)).last() }
                fail("Oversized context was silently truncated")
            } catch (expected: IllegalStateException) { assertTrue(expected.message!!.contains("too long")) }
            val unicode = withTimeout(120_000) { engine.generate(fixture, TranslationProtocol.prompt("Hello, world! 🌍")).last() }
            assertRussian(unicode)
            assertFalse(unicode.contains('\uFFFD'))
        } finally { engine.unload() }

        c.translator.unload()
    }

    @Test fun systemDownloadIsVerifiedAndCanBeRestored(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("downloadModel") == "true")
        val models = c.translator.models
        models.refresh()
        models.select("0.8B")
        models.setAllowMobile(true)
        models.downloadSelected()
        assertTrue(models.state.value.phase in setOf(ModelState.Downloading, ModelState.Ready))
        // The refresh path also restores the persisted system download after app startup.
        models.refresh()
        val final = withTimeout(300_000) { models.state.first { it.phase in setOf(ModelState.Ready, ModelState.Failed, ModelState.Unavailable) } }
        assertEquals(final.message, ModelState.Ready, final.phase)
        assertEquals(QWEN_MODELS.first().bytes, models.activeModel()!!.first.length())
        assertRussian(withTimeout(120_000) { c.translator.translate("Thank you for your help.") })
        c.translator.unload()
    }
    private fun assertRussian(text: String) {
        assertTrue("No Russian translation: $text", text.any { it in 'А'..'я' })
        assertFalse(text.contains("<think>"))
        assertFalse(text.contains("<|"))
    }
    private fun click(node: AccessibilityNodeInfo) {
        var target = node
        while (!target.isClickable) target = target.parent ?: error("Missing action target")
        assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync(); SystemClock.sleep(300)
    }
    private fun await(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 30_000
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isVisibleToUser && predicate(node)) return node
            for (i in 0 until node.childCount) find(node.getChild(i))?.let { return it }
            return null
        }
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.uiAutomation.clearCache()
            find(instrumentation.uiAutomation.rootInActiveWindow)?.let { return it }
            SystemClock.sleep(100)
        }
        error("Reader translation did not appear")
    }
    private fun capture(name: String) {
        val image = instrumentation.uiAutomation.takeScreenshot()!!
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "qwen-preview").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
