package com.marat.albion.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.ImportStatus
import com.marat.albion.data.settings.NewCardOrder
import com.marat.albion.data.settings.Settings
import com.marat.albion.data.settings.ThemeMode
import com.marat.albion.data.settings.ThemeStyle
import com.marat.albion.ui.components.AlbionLogo
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.ThinSlider
import com.marat.albion.ui.components.ScreenHeader
import com.marat.albion.ui.components.TopBar
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.paletteFor
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val c = LocalAppContainer.current
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val status by c.importer.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmGrammarReset by remember { mutableStateOf(false) }
    val repo = c.settings
    val reveal = rememberReveal()

    Column(Modifier.fillMaxSize().background(Ink.Bg)) {
        TopBar(onBack = onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        ) {
            Box(Modifier.reveal(reveal, 0)) { ScreenHeader("Настройки", includeStatusBarInsets = false) }

            Column(Modifier.reveal(reveal, 1)) {
                SectionHeader("Оформление", Modifier.padding(top = 16.dp))
                Column(Modifier.padding(vertical = 14.dp)) {
                    Text("Тема", style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.fillMaxWidth().selectableGroup()) {
                        ThemeMode.entries.forEach { mode ->
                            RadioSettingOption(mode.title, s.themeMode == mode) { scope.launch { repo.setThemeMode(mode) } }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Палитра", style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.fillMaxWidth().selectableGroup()) {
                        ThemeStyle.entries.forEach { style ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                    .selectable(selected = s.themeStyle == style, role = Role.RadioButton,
                                        onClick = { scope.launch { repo.setThemeStyle(style) } })
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = s.themeStyle == style, onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = Ink.Fg, unselectedColor = Ink.Muted))
                                Spacer(Modifier.width(12.dp))
                                Text(style.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                    color = if (s.themeStyle == style) Ink.Fg else Ink.Soft)
                                Spacer(Modifier.width(12.dp))
                                listOf(false, true).forEach { dark ->
                                    val preview = paletteFor(dark, style == ThemeStyle.SOFT)
                                    Box(Modifier.padding(start = 6.dp).size(22.dp)
                                        .background(preview.bg, CircleShape).border(1.dp, Ink.LineStrong, CircleShape))
                                }
                            }
                        }
                    }
                    Text("«Бумага» в светлой теме, «Графит» — в тёмной.",
                        style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
                        modifier = Modifier.padding(top = 8.dp))
                }
                Hairline()
            }

            Column(Modifier.reveal(reveal, 2)) { GoalReminderSection(s) }

            Column(Modifier.reveal(reveal, 2)) {
                SectionHeader("Повторение · FSRS")
                SliderSetting(
                    "Желаемое удержание", "${(s.desiredRetention * 100).roundToInt()}%",
                    "Вероятность вспомнить карточку в момент показа. Выше — чаще повторения. Рекомендуется 90%.",
                    s.desiredRetention, 0.80f..0.97f, 16,
                ) { scope.launch { repo.setRetention((it * 100).roundToInt() / 100f) } }
                SliderSetting("Новых карточек в день", "${s.newPerDay}", "Для каждой колоды отдельно.", s.newPerDay.toFloat(), 0f..100f, 19) {
                    scope.launch { repo.setNewPerDay((it / 5).roundToInt() * 5) }
                }
                SliderSetting("Повторений в день", "${s.reviewsPerDay}", null, s.reviewsPerDay.toFloat(), 50f..1000f, 18) {
                    scope.launch { repo.setReviewsPerDay((it / 50).roundToInt() * 50) }
                }
                Column(Modifier.padding(vertical = 16.dp)) {
                    Text("Порядок новых карточек", style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.fillMaxWidth().selectableGroup()) {
                        NewCardOrder.entries.forEach { order ->
                            RadioSettingOption(order.title, s.newOrder == order) { scope.launch { repo.setNewOrder(order) } }
                        }
                    }
                }
                Hairline()
                SliderSetting(
                    "Начало нового дня", "%02d:00".format(s.dayStartHour),
                    "Ночные занятия до этого часа засчитываются в предыдущий день.",
                    s.dayStartHour.toFloat(), 0f..8f, 7,
                ) { scope.launch { repo.setDayStartHour(it.roundToInt()) } }
                FsrsTuningBlock(s)
            }

            Column(Modifier.reveal(reveal, 3)) {
                SectionHeader("Карточки и озвучка")
                SwitchSetting("Озвучивать слово при показе", s.autoplayFront) { scope.launch { repo.setAutoplayFront(it) } }
                SwitchSetting("Озвучивать при перевороте", s.autoplayBack) { scope.launch { repo.setAutoplayBack(it) } }
                SwitchSetting("Транскрипция на лицевой стороне", s.showIpaOnFront) { scope.launch { repo.setShowIpaOnFront(it) } }
                SwitchSetting("Русское значение на обороте", s.showRussian) { scope.launch { repo.setShowRussian(it) } }
                SliderSetting(
                    "Громкость озвучки", if (s.playbackVolume == 0f) "без звука" else "${(s.playbackVolume * 100).roundToInt()}%",
                    "Относительно громкости медиа на телефоне.",
                    s.playbackVolume, 0f..1f, 9,
                ) {
                    val v = (it * 10).roundToInt() / 10f
                    c.audio.setVolume(v)
                    scope.launch { repo.setPlaybackVolume(v) }
                }
                SliderSetting("Скорость озвучки", "×%.1f".format(s.playbackSpeed), null, s.playbackSpeed, 0.5f..1.5f, 9) {
                    val v = (it * 10).roundToInt() / 10f
                    c.audio.setSpeed(v)
                    scope.launch { repo.setPlaybackSpeed(v) }
                }
            }

            Column(Modifier.reveal(reveal, 4)) {
                SectionHeader("Учебник")
                SliderSetting("Размер текста", "${(s.grammarTextScale * 100).roundToInt()}%", null, s.grammarTextScale, 0.85f..1.4f, 10) {
                    scope.launch { repo.setGrammarTextScale(it) }
                }
                ActionRow("Сбросить отметки прочитанного") { confirmGrammarReset = true }
            }

            Column(Modifier.reveal(reveal, 5)) {
                SectionHeader("Материалы")
                Text(
                    when (val st = status) {
                        ImportStatus.Checking -> "Проверка…"
                        is ImportStatus.Importing -> "Загрузка: ${st.deck.title} (${st.index}/${st.total})"
                        ImportStatus.Ready -> "Материалы загружены"
                        ImportStatus.Empty -> "В сборке нет материалов"
                        is ImportStatus.Failed -> "Ошибка: ${st.message}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink.Text,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Таблицы, озвучка и учебник вшиваются в приложение при сборке. После обновления таблиц пересоберите приложение — " +
                        "изменения подтянутся автоматически, прогресс сохранится.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.Muted,
                )
                Spacer(Modifier.height(8.dp))
                ActionRow("Перечитать таблицы сейчас") { c.appScope.launch { c.importer.importIfNeeded(force = true) } }
            }

            Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AlbionLogo(size = 28.dp, color = Ink.Faint)
                Spacer(Modifier.height(10.dp))
                Eyebrow("Albion 1.0 · FSRS-6", color = Ink.Faint)
            }
        }
    }
    if (confirmGrammarReset) {
        AlertDialog(
            onDismissRequest = { confirmGrammarReset = false },
            containerColor = Ink.Elevated,
            title = { Text("Сбросить прогресс учебника?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Отметки «изучено» и закладки будут удалены.") },
            confirmButton = {
                TextButton(onClick = { confirmGrammarReset = false; scope.launch { c.grammar.resetProgress() } }) { Text("Сбросить", color = Ink.Fg) }
            },
            dismissButton = { TextButton(onClick = { confirmGrammarReset = false }) { Text("Отмена", color = Ink.Muted) } },
        )
    }
}

@Composable
private fun RadioSettingOption(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Ink.Fg,
                unselectedColor = Ink.Muted,
            ),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Ink.Fg else Ink.Soft,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun SliderSetting(
    title: String,
    value: String,
    hint: String?,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Ink.Text, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
        }
        ThinSlider(
            value = current.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
        )
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
    }
}

@Composable
internal fun SwitchSetting(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = Ink.Text, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Ink.Fg,
                checkedThumbColor = Ink.Bg,
                uncheckedTrackColor = Ink.Bg,
                uncheckedThumbColor = Ink.Muted,
                uncheckedBorderColor = Ink.LineStrong,
            ),
        )
    }
}

@Composable
internal fun ActionRow(title: String, onClick: () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink.Fg,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        )
        Hairline()
    }
}
