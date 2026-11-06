package com.marat.albion.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.components.FitText
import com.marat.albion.ui.components.SettingsShortcut
import com.marat.albion.ui.components.SignalLabel
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.theme.AnimatedNumber
import java.util.Locale

/** The introduction stays in the daily overview at every window and font size. */
@Composable
internal fun HomeHero(state: HomeState, date: String, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SignalLabel(date.uppercase(Locale.forLanguageTag("ru")), Modifier.weight(1f))
            SettingsShortcut(onSettings)
        }
        HeroTitle(Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(8.dp))
        DailyProgress(state)
    }
}

@Composable
private fun HeroTitle(modifier: Modifier) {
    val style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp, lineHeight = 42.sp)
    FitText("ALBION", modifier, style = style,
        minSize = 20.sp)
}

@Composable
private fun DailyProgress(state: HomeState) {
    val progress = if (state.dailyGoal > 0) state.answersToday.toFloat() / state.dailyGoal else 0f
    val status = if (state.answersToday >= state.dailyGoal) "ВЫПОЛНЕНО" else "В ПРОЦЕССЕ"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth / LocalDensity.current.fontScale < 300.dp) {
            Column(Modifier.fillMaxWidth()) {
                GoalCount(state)
                Spacer(Modifier.height(10.dp))
                SignalLabel(status)
                Spacer(Modifier.height(8.dp))
                ThinProgress(progress, height = 2.dp)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { GoalCount(state) }
                Column(horizontalAlignment = Alignment.End) {
                    SignalLabel(status)
                    Spacer(Modifier.height(8.dp))
                    ThinProgress(progress, Modifier.width(76.dp), height = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun GoalCount(state: HomeState) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedNumber(state.answersToday, MaterialTheme.typography.titleLarge)
            Text(" / ${state.dailyGoal}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("цель дня", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
