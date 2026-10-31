package com.marat.albion

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.ui.nav.AlbionNavigation
import com.marat.albion.ui.theme.AlbionTheme
import com.marat.albion.ui.theme.AlbionExperience
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Интерфейс адаптируется к повороту телефона, планшета и раскрытой раскладушки.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val container = (application as AlbionApp).container
        val initial = runBlocking { container.settings.current() }
        enableEdgeToEdge()

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initial)
            CompositionLocalProvider(LocalAppContainer provides container) {
                AlbionTheme {
                    AlbionExperience(settings) {
                        AlbionNavigation()
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
