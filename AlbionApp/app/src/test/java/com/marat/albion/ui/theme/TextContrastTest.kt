package com.marat.albion.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class TextContrastTest {
    private fun contrast(foreground: Color, background: Color): Float {
        val a = foreground.luminance()
        val b = background.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }

    @Test fun textRemainsReadableOnEveryThemeSurface() {
        for (palette in listOf(DarkPalette, LightPalette)) {
            val backgrounds = listOf(palette.bg, palette.raised, palette.surface, palette.elevated, palette.high)
            val foregrounds = listOf(palette.fg, palette.text, palette.soft, palette.muted, palette.faint, palette.success, palette.accent)
            for (background in backgrounds) for (foreground in foregrounds) {
                val c = contrast(foreground, background)
                assertTrue("${palette.isDark}: $foreground on $background has contrast $c", c >= 4.5f)
            }
        }
    }

    @Test fun accentFillsKeepTheirLabelsReadable() {
        for (palette in listOf(DarkPalette, LightPalette)) {
            val pairs = listOf(
                palette.onAccent to palette.accent,
                palette.onWarm to palette.warm,
                palette.text to palette.accentSoft,
                palette.accent to palette.accentSoft,
                palette.text to palette.warmSoft,
                palette.warm to palette.warmSoft,
                palette.warm to palette.bg,
                palette.warm to palette.raised,
            )
            for ((foreground, background) in pairs) {
                val c = contrast(foreground, background)
                assertTrue("${palette.isDark}: $foreground on $background has contrast $c", c >= 4.5f)
            }
        }
    }
}
