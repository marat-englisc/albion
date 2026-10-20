package com.marat.albion.ui.theme

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class TextContrastTest {
    @Test fun textRemainsReadableOnEveryThemeSurface() {
        for (palette in listOf(DarkPalette, LightPalette)) {
            val backgrounds = listOf(palette.bg, palette.raised, palette.surface, palette.elevated, palette.high)
            val foregrounds = listOf(palette.fg, palette.text, palette.soft, palette.muted, palette.faint)
            for (background in backgrounds) for (foreground in foregrounds) {
                val a = foreground.luminance()
                val b = background.luminance()
                val contrast = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
                assertTrue("${palette.isDark}: $foreground on $background has contrast $contrast", contrast >= 4.5f)
            }
        }
    }
}
