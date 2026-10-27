package com.marat.albion.ui.theme

import android.content.res.Resources
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import com.marat.albion.R

/** The native face and Compose face must agree when fitting an initial by its visible outline. */
internal data class InitialFont(val typeface: Typeface) {
    val family = FontFamily(typeface)
}

internal object InitialFonts {
    private var latin: InitialFont? = null
    private var cyrillic: InitialFont? = null
    var reading: Typeface? = null
        private set

    fun initialize(resources: Resources) {
        if (latin != null) return
        latin = InitialFont(resources.getFont(R.font.unifraktur_maguntia))
        cyrillic = InitialFont(resources.getFont(R.font.cormorant_garamond_variable))
        reading = resources.getFont(R.font.literata_variable)
    }

    fun forLetter(letter: Char): InitialFont? = when (letter) {
        in 'A'..'Z', in 'a'..'z', in '\u00c0'..'\u024f' -> latin
        in '\u0400'..'\u052f' -> cyrillic
        else -> null
    }
}
