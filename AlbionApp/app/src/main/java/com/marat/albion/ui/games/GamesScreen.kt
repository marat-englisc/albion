package com.marat.albion.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.ScreenHeader
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.nav.GameRoute
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.pressScale
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal

@Composable
fun GamesScreen(navigator: Navigator) {
    val reveal = rememberReveal()
    LazyColumn(
        Modifier.fillMaxSize().background(Ink.Bg),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        item {
            Box(Modifier.reveal(reveal, 0)) {
                ScreenHeader("Игры", eyebrow = "Практика", subtitle = "Тренировки в свободном режиме — расписание FSRS они не меняют.")
            }
        }
        item { SectionHeader("Мини-игры", Modifier.padding(top = 16.dp).reveal(reveal, 1), trailing = "%02d".format(Game.entries.size)) }
        itemsIndexed(Game.entries) { i, game ->
            GameRow(i + 1, game, Modifier.reveal(reveal, 2 + i)) { navigator.open(GameRoute(game)) }
        }
    }
}

@Composable
private fun GameRow(index: Int, game: Game, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.fillMaxWidth().pressScale(interaction, 0.98f).clickable(interaction, indication = null, onClick = onClick)) {
        Row(Modifier.padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("%02d".format(index), style = AlbionType.index, modifier = Modifier.width(36.dp))
            Column(Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.headlineSmall, color = Ink.Fg)
                Spacer(Modifier.height(4.dp))
                Text(game.tagline, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
            }
            Box(Modifier.size(44.dp).border(1.dp, Ink.LineStrong, CircleShape), contentAlignment = Alignment.Center) {
                Icon(game.icon, null, tint = Ink.Fg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = Ink.Faint)
        }
        Hairline()
    }
}
