package com.marat.albion.ui.games

import com.marat.albion.ui.components.PeekFrom
import com.marat.albion.ui.components.peekingFinn
import com.marat.albion.ui.components.FinnPeek
import com.marat.albion.ui.components.Finn
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.marat.albion.ui.components.Panel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.theme.Serif
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.FeatureHelp
import com.marat.albion.ui.components.ScreenHeader
import com.marat.albion.ui.components.SettingsShortcut
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.nav.GameRoute
import com.marat.albion.ui.nav.Navigator
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
            // Финн заглядывает в «Практику» из-за правого края экрана, на уровне заголовка.
            Box(Modifier.fillMaxWidth().reveal(reveal, 0)
                .peekingFinn(FinnPeek.SIDE, from = PeekFrom.InEnd, at = 0.86f, height = 88.dp, outset = 24.dp)) {
                ScreenHeader("Практика", eyebrow = "Закрепляйте изученное", trailing = {
                    Row {
                        FeatureHelp("practice", "Как заниматься", "Выберите упражнение, колоду или уровень и длину занятия.\n\nОшибки сохраняются: сложные слова и задания будут встречаться чаще.\n\nПрактика учитывается в статистике, расписание повторения карточек остаётся прежним.", showOnFirstUse = false, mascot = Finn.HINT)
                        SettingsShortcut(navigator::openSettings)
                    }
                })
            }
        }
        item { SectionHeader("Упражнения", Modifier.padding(top = 8.dp).reveal(reveal, 1), trailing = "%02d".format(Game.entries.size)) }
        itemsIndexed(Game.entries) { i, game ->
            GameRow(game, Modifier.reveal(reveal, 2 + i)) { navigator.open(GameRoute(game)) }
        }
    }
}

@Composable
private fun GameRow(game: Game, modifier: Modifier, onClick: () -> Unit) {
    // Упражнения чередуют оттенки шарфа и клюва — список читается как игровое поле.
    val warm = game.ordinal % 2 == 1
    Panel(modifier.fillMaxWidth().padding(vertical = 5.dp), onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(if (warm) Ink.WarmSoft else Ink.AccentSoft, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(game.icon, null, tint = if (warm) Ink.Warm else Ink.Accent, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Serif, fontSize = 20.sp, lineHeight = 26.sp), color = Ink.Fg)
                Spacer(Modifier.height(2.dp))
                Text(game.tagline, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = Ink.Faint)
        }
    }
}
