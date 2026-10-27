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
            val foregrounds = listOf(palette.fg, palette.text, palette.soft, palette.muted, palette.faint,
                palette.successText, palette.accentText, palette.errorText, palette.goldText)
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
                palette.onAccent to palette.accentDeep,
                palette.onError to palette.error,
                palette.onError to palette.errorDeep,
                palette.onWarm to palette.warm,
                palette.onWarm to palette.warmDeep,
                palette.text to palette.accentSoft,
                palette.accentText to palette.accentSoft,
                palette.text to palette.errorSoft,
                palette.errorText to palette.errorSoft,
                palette.text to palette.warmSoft,
                palette.warmText to palette.warmSoft,
                palette.warmText to palette.bg,
                palette.warmText to palette.raised,
                palette.goldText to palette.goldSoft,
                palette.text to palette.goldSoft,
                palette.muted to palette.accentSoft,
                palette.muted to palette.errorSoft,
                palette.muted to palette.warmSoft,
                palette.muted to palette.goldSoft,
            )
            for ((foreground, background) in pairs) {
                val c = contrast(foreground, background)
                assertTrue("${palette.isDark}: $foreground on $background has contrast $c", c >= 4.5f)
            }
        }
    }

    @Test fun successLabelsRemainReadableOnFeedbackCards() {
        for (palette in listOf(DarkPalette, LightPalette)) {
            for (background in listOf(palette.raised, palette.surface, palette.successSoft)) {
                val c = contrast(palette.successText, background)
                assertTrue("Success label on $background has contrast $c", c >= 4.5f)
            }
        }
    }
}
