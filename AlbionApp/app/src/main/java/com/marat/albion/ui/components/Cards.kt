package com.marat.albion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.marat.albion.ui.theme.Hero
import com.marat.albion.ui.theme.Heritage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp

/**
 * Акцентная плита: зелёный переплёт с латунным тиснением, одинаковый в обеих темах.
 * Внутри действует локальная схема — кремовый текст и «инверсные» кнопки. Одна на экран.
 */
@Composable
fun BoundVolume(modifier: Modifier = Modifier, compact: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(if (compact) 20.dp else 24.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp), content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val onHero = Hero.Content
    val heroScheme = scheme.copy(
        primary = onHero, onPrimary = Hero.Deep,
        background = onHero, onBackground = Hero.Deep,
        surface = Hero.Base, onSurface = onHero, onSurfaceVariant = onHero,
        surfaceContainerLow = Hero.Base,
        inverseSurface = Hero.Base, inverseOnSurface = onHero,
        surfaceContainerHigh = onHero.copy(alpha = .16f), outline = onHero, outlineVariant = onHero.copy(alpha = .22f),
    )
    Surface(modifier, shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent, contentColor = onHero,
        border = BorderStroke(.75.dp, Heritage.Champagne.copy(alpha = .65f))) {
        MaterialTheme(colorScheme = heroScheme) {
            CompositionLocalProvider(LocalContentColor provides onHero) {
                Column(Modifier.background(Hero.Gradient).drawBehind {
                    // Мягкий свет на коже переплёта.
                    drawRect(Brush.radialGradient(listOf(Hero.Glow, Color.Transparent),
                        center = Offset(size.width * .95f, 0f), radius = size.maxDimension * .7f))
                }.constructionLines(Heritage.Champagne).engravedSurface(Heritage.Champagne).paperGrain(Heritage.Champagne)
                    .folioFrame(Heritage.Champagne, embellished = true).padding(contentPadding),
                    verticalArrangement = verticalArrangement, content = content)
            }
        }
    }
}

@Composable
fun CatalogueTab(label: String, modifier: Modifier = Modifier) {
    Eyebrow(label, modifier)
}

/** Карточка продолжения: подпись, заголовок, описание, стрелка в круге и прогресс. */
@Composable
fun CatalogueCard(
    label: String,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ribbon: Boolean = false,
    progress: Float? = null,
    compact: Boolean = false,
) {
    Panel(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Eyebrow(label)
                Spacer(Modifier.height(6.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            ArrowBadge()
        }
        if (progress != null && progress > 0f) {
            Spacer(Modifier.height(16.dp))
            ThinProgress(progress)
        }
    }
}

/** Стрелка «перейти» в круге. */
@Composable
fun ArrowBadge(modifier: Modifier = Modifier) {
    Box(modifier.size(36.dp).background(MaterialTheme.colorScheme.background, CircleShape)
        .medallionRim(MaterialTheme.colorScheme.secondary)
        .border(.75.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape), contentAlignment = Alignment.Center) {
        Icon(AlbionIcons.ArrowForward, null, Modifier.size(18.dp))
    }
}

@Composable
fun FolioDivider(modifier: Modifier = Modifier, label: String? = null) {
    if (label != null) Eyebrow(label, modifier) else
        Box(modifier.fillMaxWidth().height(1.dp).folioRule(MaterialTheme.colorScheme.outline))
}

fun romanNumeral(value: Int): String {
    if (value !in 1..3999) return value.toString()
    var rest = value
    return buildString {
        for ((number, glyph) in listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
            50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")) {
            while (rest >= number) { append(glyph); rest -= number }
        }
    }
}
