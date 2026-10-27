package com.marat.albion

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.settings.ThemeMode
import com.marat.albion.data.content.ImportStatus
import com.marat.albion.ui.intro.IntroScreen
import com.marat.albion.ui.theme.AlbionTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Actual app navigation and script rendering in both palettes; opt-in review screenshots. */
@RunWith(AndroidJUnit4::class)
class EditorialDesignTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val c get() = (instrumentation.targetContext.applicationContext as AlbionApp).container

    @Test fun primaryScreensAndSensoryPreferencesWorkInBothThemes(): Unit = runBlocking {
        c.importer.importIfNeeded()
        val original = c.settings.current()
        try {
            for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                c.settings.setThemeMode(mode)
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    await { it.text?.toString()?.startsWith("Начать · ") == true || it.text?.toString() == "Открыть библиотеку" }
                    capture("home-${mode.name.lowercase()}")
                    click { it.text?.toString() == "Учебник" && it.isVisibleToUser }
                    await { it.isEditable }
                    capture("grammar-${mode.name.lowercase()}")
                    click { it.text?.toString() in setOf("Читать урок", "Сначала открыть правило") }
                    await { it.contentDescription?.toString() == "Размер текста" }
                    capture("lesson-${mode.name.lowercase()}")
                    click { it.contentDescription?.toString() == "Назад" }
                    await { it.isEditable }
                    click { it.text?.toString() == "Чтение" }
                    dismissHelp()
                    await { it.text?.toString() == "Библиотека" }
                    capture("library-${mode.name.lowercase()}")
                    click { it.text?.toString() == "The Keeper of Gull Island" }
                    await { it.contentDescription?.toString() == "Оглавление" }
                    capture("reader-${mode.name.lowercase()}")
                    click { it.contentDescription?.toString() == "Назад" }
                    await { it.text?.toString() == "Библиотека" }
                    click { it.text?.toString() == "Практика" }
                    await { it.text?.toString() == "Словарный запас".uppercase() }
                    capture("practice-${mode.name.lowercase()}")
                    click { it.text?.toString() == "Прогресс" }
                    await { it.text?.toString() == "КАРТОЧКИ И ПРАКТИКА" }
                    capture("progress-${mode.name.lowercase()}")
                    click { it.contentDescription?.toString() == "Настройки" }
                    await { it.text?.toString() == "Тема" }
                    capture("settings-${mode.name.lowercase()}")
                    click { it.contentDescription?.toString() == "Назад" }
                    await { it.text?.toString() == "КАРТОЧКИ И ПРАКТИКА" }
                    click { it.text?.toString() == "Карточки" }
                    click { it.text?.toString()?.startsWith("Начать · ") == true }
                    await { it.text?.toString() == "Показать ответ" }
                    capture("study-${mode.name.lowercase()}")
                    click { it.text?.toString() == "Показать ответ" }
                    await { it.text?.toString() == "Хорошо" }
                    capture("answer-${mode.name.lowercase()}")
                    click { it.contentDescription?.toString() == "Закрыть" }
                    scenario.onActivity { activity ->
                        activity.setContent {
                            AlbionTheme(dark = mode == ThemeMode.DARK) {
                                IntroScreen(ImportStatus.Checking, full = false, onFinished = {})
                            }
                        }
                    }
                    await { it.text?.toString() == "Проверяю материалы" }
                    capture("launch-${mode.name.lowercase()}")
                }
            }
            c.settings.setInterfaceSounds(false)
            c.settings.setHapticsEnabled(false)
            c.settings.setReduceMotion(true)
            val saved = c.settings.current()
            assertTrue(!saved.interfaceSounds && !saved.hapticsEnabled && saved.reduceMotion)
            ActivityScenario.launch(MainActivity::class.java).use {
                click { it.text?.toString()?.startsWith("Начать · ") == true }
                click { it.text?.toString() == "Показать ответ" }
                await { it.text?.toString() == "Хорошо" }
                capture("study-reduced-motion")
            }
        } finally {
            c.settings.setThemeMode(original.themeMode)
            c.settings.setInterfaceSounds(original.interfaceSounds)
            c.settings.setHapticsEnabled(original.hapticsEnabled)
            c.settings.setReduceMotion(original.reduceMotion)
        }
    }

    private fun dismissHelp() {
        SystemClock.sleep(400)
        find(instrumentation.uiAutomation.rootInActiveWindow) { it.text?.toString() == "Понятно" }?.let(::activate)
    }
    private fun click(predicate: (AccessibilityNodeInfo) -> Boolean) = activate(await(predicate))
    private fun activate(node: AccessibilityNodeInfo) {
        var target = node
        while (!target.isClickable) target = target.parent ?: error("Missing clickable parent")
        assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
        SystemClock.sleep(500)
    }
    private fun await(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val until = SystemClock.uptimeMillis() + 20_000
        while (SystemClock.uptimeMillis() < until) {
            find(instrumentation.uiAutomation.rootInActiveWindow, predicate)?.let { return it }
            SystemClock.sleep(100)
        }
        error("Expected UI element did not appear")
    }
    private fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isVisibleToUser && predicate(node)) return node
        for (i in 0 until node.childCount) find(node.getChild(i), predicate)?.let { return it }
        return null
    }
    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureEditorial") != "true") return
        instrumentation.waitForIdleSync()
        SystemClock.sleep(450)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "editorial").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
