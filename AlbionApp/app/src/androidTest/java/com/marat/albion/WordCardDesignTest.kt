package com.marat.albion

import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.ui.browse.BrowseScreen
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.primaryRoutes
import com.marat.albion.ui.study.*
import com.marat.albion.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WordCardDesignTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val c get() = (instrumentation.targetContext.applicationContext as AlbionApp).container
    private val silentAudio = object : CardAudio {
        override val playingKey: String? = null
        override fun play(deck: Deck, file: String?, text: String, key: String) = Unit
    }

    @Test fun aboutHasOneListRowAndEveryMeaningIsReadableOnItsCard(): Unit = runBlocking {
        c.importer.importIfNeeded()
        val about = c.study.browse(Deck.WORDS, "about", 0).first().first { it.term == "about" }
        assertTrue("Bundled meanings were not combined", about.senses.size >= 3)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val nav = Navigator(primaryRoutes.associateWith { NavBackStack<NavKey>(it) }, mutableIntStateOf(0))
            show(scenario, "list") { BrowseScreen(Deck.WORDS, nav) }
            val search = await { it.isEditable }
            assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "about")
            }))
            await { it.text?.toString() == "Все значения · ${about.senses.size}" }
            SystemClock.sleep(300)
            assertEquals(1, matching(instrumentation.uiAutomation.rootInActiveWindow) {
                !it.isEditable && it.text?.toString() == "about"
            }.size)
            capture("about-list")
            showCard(scenario, about, false, 1f)
            await { it.text?.toString() == "Все значения · ${about.senses.size}".uppercase() }
            capture("about-front")
            showCard(scenario, about, true, 1f)
            capture("about-back")
            for ((index, sense) in about.senses.withIndex()) {
                scrollTo { it.text?.toString() == sense.meaningRu }
                capture("about-meaning-${index + 1}")
            }
            for (scale in listOf(1.5f, 2f)) {
                showCard(scenario, about, false, scale)
                await { it.text?.toString() == "about" }
                capture("about-front-$scale")
                showCard(scenario, about, true, scale)
                for (sense in about.senses) scrollTo { it.text?.toString() == sense.meaningRu }
                capture("about-back-$scale")
            }
            for (scale in listOf(1.5f, 2f)) {
                show(scenario, "list-$scale") {
                    val density = LocalDensity.current.density
                    CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                        Box(Modifier.width(320.dp).fillMaxHeight()) { BrowseScreen(Deck.WORDS, nav) }
                    }
                }
                assertTrue(await { it.isEditable }.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "about")
                }))
                await { it.text?.toString() == "Все значения · ${about.senses.size}" }
                capture("about-list-$scale")
                var expand = await { it.text?.toString() == "Все значения · ${about.senses.size}" }
                while (!expand.isClickable) expand = expand.parent ?: error("No expand action")
                assertTrue(expand.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                instrumentation.waitForIdleSync(); SystemClock.sleep(300)
                scrollTo { it.text?.toString() == about.senses.last().meaningRu }
                capture("about-list-$scale-last")
            }
        }
    }

    private fun showCard(scenario: ActivityScenario<MainActivity>, card: CardEntity, back: Boolean, scale: Float) {
        show(scenario, "$back-$scale") {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                Box(Modifier.width(320.dp).fillMaxHeight().background(Ink.Bg).verticalScroll(rememberScrollState())) {
                    if (back) CardBack(card, null, silentAudio) else CardFront(card, null, silentAudio, true)
                }
            }
        }
    }

    private fun show(scenario: ActivityScenario<MainActivity>, name: String, content: @Composable () -> Unit) {
        val ready = CountDownLatch(1)
        scenario.onActivity { activity -> activity.setContent {
            CompositionLocalProvider(LocalAppContainer provides c) {
                key(name) {
                    AlbionTheme { Box(Modifier.fillMaxSize().background(Ink.Bg).windowInsetsPadding(WindowInsets.systemBars)) { content() } }
                    SideEffect { ready.countDown() }
                }
            }
        } }
        assertTrue(ready.await(10, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync(); SystemClock.sleep(400)
        if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.clearCache()
    }

    private fun matching(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
        if (node == null) return emptyList()
        return (if (node.isVisibleToUser && predicate(node)) listOf(node) else emptyList()) +
            (0 until node.childCount).flatMap { matching(node.getChild(it), predicate) }
    }
    private fun await(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val until = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < until) {
            if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.clearCache()
            matching(instrumentation.uiAutomation.rootInActiveWindow, predicate).firstOrNull()?.let { return it }
            SystemClock.sleep(100)
        }
        error("Expected word or meaning did not appear")
    }
    private fun scrollTo(predicate: (AccessibilityNodeInfo) -> Boolean) {
        repeat(18) {
            if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.clearCache()
            if (matching(instrumentation.uiAutomation.rootInActiveWindow, predicate).isNotEmpty()) return
            val metrics = instrumentation.targetContext.resources.displayMetrics
            val x = 150 * metrics.density
            val y = metrics.heightPixels * .7f
            val end = metrics.heightPixels * .3f
            val down = SystemClock.uptimeMillis()
            fun event(action: Int, at: Float) {
                val e = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, at, 0)
                instrumentation.uiAutomation.injectInputEvent(e, true); e.recycle()
            }
            event(MotionEvent.ACTION_DOWN, y)
            for (i in 1..16) { SystemClock.sleep(15); event(MotionEvent.ACTION_MOVE, y + (end - y) * i / 16) }
            event(MotionEvent.ACTION_UP, end)
            instrumentation.waitForIdleSync(); SystemClock.sleep(250)
        }
        await(predicate)
    }
    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureWordCards") != "true") return
        instrumentation.waitForIdleSync(); SystemClock.sleep(250)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()!!
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "word-cards").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
