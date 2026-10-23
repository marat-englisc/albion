package com.marat.albion

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.data.settings.ThemeMode
import com.marat.albion.data.settings.ThemeStyle
import com.marat.albion.ui.intro.IntroScreen
import com.marat.albion.ui.nav.AlbionNavigation
import com.marat.albion.ui.theme.AlbionTheme
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.paletteFor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as AlbionApp).container
        // Тему читаем до первого кадра, чтобы светлая тема не «моргала» тёмной.
        val initial = runBlocking { container.settings.current() }
        val systemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        Ink.palette = paletteFor(isDark(initial.themeMode, systemDark), initial.themeStyle == ThemeStyle.SOFT)

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initial)
            val dark = isDark(settings.themeMode, isSystemInDarkTheme())
            val soft = settings.themeStyle == ThemeStyle.SOFT
            val palette = paletteFor(dark, soft)

            // Значки статус-бара и навигации — светлые на тёмной теме, тёмные на светлой.
            DisposableEffect(palette) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(palette.bg.toArgb()))
                onDispose {}
            }

            CompositionLocalProvider(LocalAppContainer provides container) {
                AlbionTheme(dark = dark, soft = soft) {
                    // Заставка — только при холодном старте; приложение под ней
                    // начинает рисоваться в момент ухода заставки, чтобы каскадные
                    // анимации главного экрана совпали с её исчезновением.
                    var introVisible by rememberSaveable { mutableStateOf(true) }
                    val status by container.importer.status.collectAsStateWithLifecycle()
                    Box(Modifier.fillMaxSize().background(Ink.Bg)) {
                        if (!introVisible) AlbionNavigation()
                        AnimatedVisibility(
                            visible = introVisible,
                            enter = androidx.compose.animation.EnterTransition.None,
                            exit = fadeOut(tween(180, easing = Easing.Emphasized)) +
                                scaleOut(tween(220, easing = Easing.EmphasizedAccelerate), targetScale = 1.04f),
                        ) {
                            IntroScreen(status = status, full = !initial.introSeen, onFinished = {
                                introVisible = false
                                if (!initial.introSeen) container.appScope.launch { container.settings.markIntroSeen() }
                            })
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        (application as AlbionApp).container.audio.stop()
    }

    private fun isDark(mode: ThemeMode, systemDark: Boolean) = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
}
