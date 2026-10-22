package com.marat.albion

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.audio.AudioDecoder
import com.marat.albion.data.audio.ProsodyAnalyzer
import com.marat.albion.data.audio.ShadowReport
import com.marat.albion.data.content.Deck
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.games.MelodyChart
import com.marat.albion.ui.games.preparePitchContour
import com.marat.albion.ui.theme.AlbionTheme
import com.marat.albion.ui.theme.Ink
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class ShadowingMelodyUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun bundledSpeechProducesReadableContours() {
        val context = instrumentation.targetContext
        assumeTrue(context.assets.list("content/audio/words")?.isNotEmpty() == true)
        val clips = listOf(Deck.WORDS to "abandon.mp3", Deck.WORDS to "absolutely.mp3", Deck.IDIOMS to "a piece of cake.mp3")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for ((deck, file) in clips) {
                val audio = AudioDecoder.decodeCardAudio(context, deck, file) ?: error("Не декодируется $file")
                val prosody = ProsodyAnalyzer.analyze(audio) ?: error("Нет анализа $file")
                val contour = preparePitchContour(prosody.pitch, prosody.energy)
                android.util.Log.i("MelodyCheck", "$file: ${prosody.voiced}/${prosody.frames} voiced, segments=${contour.segments.map { it.size }}")
                show(scenario, ProsodyAnalyzer.compare(prosody, prosody), false, 1f)
                awaitText(if (contour.available) "Диктор" else "В записи диктора мало участков с различимым тоном. Сравните интонацию на слух.")
                capture(file.removeSuffix(".mp3"))
                assertTrue("Недостаточно мелодики в $file: ${prosody.voiced}/${prosody.frames}", contour.available)
            }
        }
    }

    @Test fun chartAndMissingPitchAreReadableOnCompactScreens() {
        val pitch = FloatArray(160) { i ->
            if (i in 40..46 || i in 90..112) Float.NaN else (3 * sin(i / 22.0)).toFloat()
        }
        val report = ShadowReport(74, 72, 49, 100, 1f, emptyList(), pitch,
            FloatArray(160) { if (pitch[it].isFinite()) pitch[it] * 0.7f - 0.4f else Float.NaN },
            FloatArray(160) { 1f }, FloatArray(160) { 1f })
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for ((name, dark, fontScale) in listOf(Triple("light", false, 1f), Triple("large-font", false, 1.5f), Triple("dark", true, 1f))) {
                show(scenario, report, dark, fontScale)
                awaitText("Диктор")
                awaitText("Вы")
                awaitText("Выше")
                awaitText("Ниже")
                awaitText("Начало")
                awaitText("Конец")
                assertTrue(hasChart())
                capture(name)
            }
            val missing = FloatArray(160) { Float.NaN }
            show(scenario, report.copy(minePitch = missing), false, 1f)
            awaitText("Не удалось уверенно определить тон вашей записи. Попробуйте записать ещё раз ближе к микрофону.")
            assertTrue(hasChart()) // Мелодика диктора остаётся доступна.
            assertFalse(hasText("Вы"))
            capture("missing-recording")

            val sparse = FloatArray(160) { if (it % 35 in 0..2) 2f else Float.NaN }
            show(scenario, report.copy(originalPitch = sparse, minePitch = missing), false, 1.5f)
            awaitText("В записи диктора мало участков с различимым тоном. Сравните интонацию на слух.")
            assertFalse(hasChart())
            capture("sparse")
        }
    }

    private fun show(scenario: ActivityScenario<MainActivity>, report: ShadowReport, dark: Boolean, scale: Float) {
        scenario.onActivity { activity ->
            activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                    AlbionTheme(dark = dark) {
                        Box(Modifier.fillMaxSize().background(Ink.Bg), contentAlignment = Alignment.TopCenter) {
                            // 320 dp экран, 24 dp внешние отступы и 18 dp внутри карточки.
                            Column(Modifier.width(320.dp).padding(horizontal = 42.dp, vertical = 40.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Eyebrow("Мелодика")
                                MelodyChart(report)
                            }
                        }
                    }
                }
            }
        }
        instrumentation.waitForIdleSync()
        // Обновляются и композиция, и snapshot-палитра темы, затем кадр попадает на экран.
        SystemClock.sleep(300)
        instrumentation.waitForIdleSync()
    }

    private fun hasText(text: String) = find(instrumentation.uiAutomation.rootInActiveWindow) { it.text?.toString() == text }
    private fun hasChart() = find(instrumentation.uiAutomation.rootInActiveWindow) {
        it.contentDescription?.toString()?.startsWith("Мелодика: подъёмы") == true
    }
    private fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): Boolean {
        if (node == null) return false
        if (predicate(node)) return true
        return (0 until node.childCount).any { find(node.getChild(it), predicate) }
    }
    private fun awaitText(text: String) {
        val until = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < until) {
            if (hasText(text)) return
            SystemClock.sleep(50)
        }
        error("Не найдена подпись: $text")
    }
    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureMelody") != "true") return
        instrumentation.waitForIdleSync()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "melody-check").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Нет снимка экрана")
        if (name == "dark" || name == "light") {
            val expected = if (name == "dark") android.graphics.Color.BLACK else android.graphics.Color.WHITE
            assertTrue("Тема снимка $name", bitmap.getPixel(bitmap.width / 2, bitmap.height * 3 / 4) == expected)
        }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
