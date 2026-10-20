package com.marat.albion

import android.os.Bundle
import android.os.SystemClock
import android.graphics.Rect
import android.view.MotionEvent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Проверяет реальную композицию, декораторы стеков и сохранение состояния Activity. */
@RunWith(AndroidJUnit4::class)
class NavigationUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun searchSurvivesTabSwitchAndSettingsRoundTrip() {
        ActivityScenario.launch(MainActivity::class.java).use {
            clickText("Учебник")
            setSearch("articles")
            clickText("Чтение")
            clickText("Учебник")
            awaitNode { it.isEditable && it.text?.toString() == "articles" }
            clickDescription("Настройки")
            awaitNode { it.text?.toString() == "Тема" }
            clickDescription("Назад")
            awaitNode { it.isEditable && it.text?.toString() == "articles" }
        }
    }

    @Test fun inactiveTabStateSurvivesActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            clickText("Учебник")
            setSearch("present")
            clickText("Прогресс")
            awaitNode { it.text?.toString() == "СТАТИСТИКА FSRS" }
            scenario.recreate()
            awaitNode { it.text?.toString() == "СТАТИСТИКА FSRS" }
            clickText("Учебник")
            awaitNode { it.isEditable && it.text?.toString() == "present" }
        }
    }

    @Test fun lessonSurvivesSwitchAndReselectReturnsToSectionRoot() {
        ActivityScenario.launch(MainActivity::class.java).use {
            clickText("Учебник")
            click(awaitNode { it.text?.toString()?.matches(Regex("\\d+\\. .+")) == true })
            awaitNode { it.contentDescription?.toString() == "Размер текста" }
            clickText("Чтение")
            clickText("Учебник")
            awaitNode { it.contentDescription?.toString() == "Размер текста" }
            clickText("Учебник")
            awaitNode { it.isEditable }
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            awaitNode { it.text?.toString() == "ALBION" }
        }
    }

    private fun setSearch(query: String) {
        val field = awaitNode { it.isEditable }
        assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
        }))
        awaitNode { it.isEditable && it.text?.toString() == query }
    }

    private fun clickText(text: String) = click(awaitNode { it.text?.toString() == text })
    private fun clickDescription(text: String) = click(awaitNode { it.contentDescription?.toString() == text })

    private fun click(node: AccessibilityNodeInfo) {
        val bounds = Rect().also(node::getBoundsInScreen)
        val downTime = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
                bounds.exactCenterX(), bounds.exactCenterY(), 0)
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
        instrumentation.waitForIdleSync()
        // NavDisplay завершает переход после того, как главный поток уже освободился.
        SystemClock.sleep(650)
    }

    private fun awaitNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 30_000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.uiAutomation.rootInActiveWindow?.let { root ->
                find(root, predicate)?.let { return it }
            }
            SystemClock.sleep(100)
        }
        error("Expected UI node was not found within 30 seconds")
    }

    private fun find(node: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && predicate(node)) return node
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { find(it, predicate)?.let { found -> return found } }
        }
        return null
    }
}
