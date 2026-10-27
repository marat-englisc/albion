package com.marat.albion

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.audio.ShadowReport
import com.marat.albion.ui.components.MeasureNarrow
import com.marat.albion.ui.components.gutter
import com.marat.albion.ui.games.Game
import com.marat.albion.ui.games.GameSummary
import com.marat.albion.ui.games.Mistake
import com.marat.albion.ui.games.ReportCard
import com.marat.albion.ui.theme.AlbionTheme
import com.marat.albion.ui.theme.Ink
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.sin

/**
 * Снимки экранов игр, до которых не добраться без микрофона и целой игры: разбор шэдоуинга
 * с графиком мелодики и итог. Узкий (320 dp), обычный и широкий экраны, крупный шрифт, тёмная тема.
 * Снимки пишутся только с аргументом `captureCompact=true`.
 */
@RunWith(AndroidJUnit4::class)
class GameUiGalleryTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val variants = listOf(
        Triple(320, 1f, false), Triple(360, 1.3f, false), Triple(411, 1f, true), Triple(320, 1.5f, true),
    )

    @Test fun shadowingReportAdaptsToWidthAndFont() {
        val pitch = FloatArray(160) { i -> if (i in 40..46 || i in 90..112) Float.NaN else (3 * sin(i / 22.0)).toFloat() }
        val mine = FloatArray(160) { if (pitch[it].isFinite()) pitch[it] * 0.7f - 0.4f else Float.NaN }
        val report = ShadowReport(
            74, 72, 49, 100, 1.3f,
            listOf("Голос в конце фразы опускается раньше, чем у диктора.", "Говорите чуть быстрее."),
            pitch, mine, FloatArray(160) { 1f }, FloatArray(160) { 1f },
        )
        val low = report.copy(overall = 41, intonation = 35, rhythm = 52, tempo = 30, tempoRatio = 0.8f)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for ((width, scale, dark) in variants) for ((tag, r) in listOf("good" to report, "low" to low)) {
                show(scenario, "report-$tag-$width-$scale-$dark", width, scale, dark) {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)
                        .verticalScroll(rememberScrollState()).padding(horizontal = gutter)) { ReportCard(r) }
                }
            }
        }
    }

    @Test fun summaryFitsNarrowScreens() {
        val mistakes = listOf(
            Mistake("Используется перед исчисляемым существительным", "abaut", "about"),
            Mistake("arise — возникать", "arised · arised", "arose · arisen"),
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for ((width, scale, dark) in variants) {
                show(scenario, "summary-$width-$scale-$dark", width, scale, dark) {
                    GameSummary(Game.TYPE_ANSWER, 7, 10, mistakes, {}, {}, {})
                }
            }
        }
    }

    private fun show(scenario: ActivityScenario<MainActivity>, name: String, width: Int, scale: Float, dark: Boolean,
                     content: @Composable () -> Unit) {
        scenario.onActivity { activity ->
            activity.setContent {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                    key(name) {
                        AlbionTheme {
                            Box(Modifier.fillMaxSize().background(Ink.Bg)) {
                                MeasureNarrow(Modifier.width(width.dp).height(760.dp)) { content() }
                            }
                        }
                    }
                }
            }
        }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(2600)
        capture(name)
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureCompact") != "true") return
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "gallery").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Нет снимка экрана")
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
