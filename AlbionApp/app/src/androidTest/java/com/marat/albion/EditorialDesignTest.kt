package com.marat.albion

import android.app.UiModeManager
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.content.ImportStatus
import com.marat.albion.ui.intro.IntroScreen
import com.marat.albion.ui.theme.AlbionTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Reading Room navigation and rendering stay ivory even with Android night mode enabled. */
@RunWith(AndroidJUnit4::class)
class EditorialDesignTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val c get() = (instrumentation.targetContext.applicationContext as AlbionApp).container

    @Test fun primaryScreensAndSensoryPreferencesWorkInReadingRoom(): Unit = runBlocking {
        c.importer.importIfNeeded()
        val original = c.settings.current()
        val manager = instrumentation.targetContext.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val originalNight = manager.nightMode
        try {
            for (night in listOf(false, true)) {
                shell("cmd uimode night ${if (night) "yes" else "no"}")
                val edition = if (night) "system-night" else "reading-room"
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    await { it.text?.toString()?.startsWith("Начать · ") == true || it.text?.toString() == "Открыть библиотеку" }
                    assertIvoryBackground()
                    capture("home-$edition")
                    click { it.text?.toString() == "Учебник" && it.isVisibleToUser }
                    await { it.isEditable }
                    capture("grammar-$edition")
                    click { it.text?.toString() in setOf("Читать урок", "Сначала открыть правило") }
                    await { it.contentDescription?.toString() == "Размер текста" }
                    capture("lesson-$edition")
                    click { it.contentDescription?.toString() == "Назад" }
                    await { it.isEditable }
                    click { it.text?.toString() == "Чтение" }
                    dismissHelp()
                    await { it.text?.toString() == "Библиотека" }
                    capture("library-$edition")
                    click { it.text?.toString() == "The Keeper of Gull Island" }
                    await { it.contentDescription?.toString() == "Оглавление" }
                    capture("reader-$edition")
                    click { it.contentDescription?.toString() == "Оглавление" }
                    click { it.text?.toString() == "Chapter 6. The Light" }
                    await { it.text?.toString() == "The Light" }
                    capture("chapter-opening-$edition")
                    click { it.contentDescription?.toString() == "Меню чтения" }
                    await { it.text?.toString() == "Выделения и заметки" }
                    capture("reader-menu-$edition")
                    instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                    click { it.contentDescription?.toString() == "Назад" }
                    await { it.text?.toString() == "Библиотека" }
                    click { it.text?.toString() == "Практика" }
                    await { it.text?.toString() == "Словарный запас".uppercase() }
                    capture("practice-$edition")
                    click { it.text?.toString() == "Прогресс" }
                    await { it.text?.toString() == "КАРТОЧКИ И ПРАКТИКА" }
                    capture("progress-$edition")
                    click { it.contentDescription?.toString() == "Настройки" }
                    await { it.text?.toString() == "ЗВУК И ОЩУЩЕНИЯ" }
                    assertNull(find(instrumentation.uiAutomation.rootInActiveWindow) { it.text?.toString() in setOf("Тема", "Тёмная", "Как в системе") })
                    capture("settings-$edition")
                    click { it.contentDescription?.toString() == "Назад" }
                    await { it.text?.toString() == "КАРТОЧКИ И ПРАКТИКА" }
                    click { it.text?.toString() == "Карточки" }
                    click { it.text?.toString()?.startsWith("Начать · ") == true }
                    await { it.text?.toString() == "Показать ответ" }
                    capture("study-$edition")
                    click { it.text?.toString() == "Показать ответ" }
                    await { it.text?.toString() == "Хорошо" }
                    capture("answer-$edition")
                    click { it.contentDescription?.toString() == "Закрыть" }
                    scenario.onActivity { activity ->
                        activity.setContent {
                            AlbionTheme {
                                IntroScreen(ImportStatus.Checking, full = false, onFinished = {})
                            }
                        }
                    }
                    await { it.text?.toString() == "Проверяю материалы" }
                    capture("launch-$edition")
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
            shell("cmd uimode night ${when (originalNight) { UiModeManager.MODE_NIGHT_YES -> "yes"; UiModeManager.MODE_NIGHT_AUTO -> "auto"; else -> "no" }}")
            c.settings.setInterfaceSounds(original.interfaceSounds)
            c.settings.setHapticsEnabled(original.hapticsEnabled)
            c.settings.setReduceMotion(original.reduceMotion)
        }
    }

    private fun shell(command: String) {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        SystemClock.sleep(350)
    }
    private fun assertIvoryBackground() {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
        val color = bitmap.getPixel(6, 280)
        assertTrue("Application followed system dark mode", android.graphics.Color.red(color) > 230 &&
            android.graphics.Color.green(color) > 220 && android.graphics.Color.blue(color) > 205)
        bitmap.recycle()
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
