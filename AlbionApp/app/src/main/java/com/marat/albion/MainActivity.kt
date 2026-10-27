package com.marat.albion

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.ui.intro.IntroScreen
import com.marat.albion.ui.nav.AlbionNavigation
import com.marat.albion.ui.theme.AlbionTheme
import com.marat.albion.ui.theme.AlbionExperience
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.initializeTypography
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeTypography(resources)
        // Телефон — только вертикально; планшет и раскрытая раскладушка — в любой ориентации.
        // Размер пересчитывается при каждом пересоздании, в том числе после складывания.
        requestedOrientation = if (resources.configuration.smallestScreenWidthDp < 600) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val container = (application as AlbionApp).container
        val initial = runBlocking { container.settings.current() }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Ink.Fg.toArgb()),
        )
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Ink.Bg.toArgb()))

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initial)
            CompositionLocalProvider(LocalAppContainer provides container) {
                AlbionTheme {
                    AlbionExperience(settings) {
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
                                exit = fadeOut(tween(240, easing = Easing.Standard)),
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
    }

    override fun onStop() {
        super.onStop()
        (application as AlbionApp).container.audio.stop()
    }

}
