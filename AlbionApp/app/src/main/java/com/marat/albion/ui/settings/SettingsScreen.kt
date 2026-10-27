package com.marat.albion.ui.settings

import com.marat.albion.ui.theme.paperSurface
import com.marat.albion.ui.components.AlbionIcons
import androidx.compose.animation.core.tween

import com.marat.albion.ui.components.gutter
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import com.marat.albion.ui.components.lerpColor
import com.marat.albion.ui.theme.Palette
import com.marat.albion.ui.theme.pressScale
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

    Column(Modifier.fillMaxSize().paperSurface()) {
        TopBar(onBack = onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = gutter).padding(bottom = 32.dp),
        ) {

            Box(Modifier.fillMaxWidth().reveal(reveal, 0)) {
                ScreenHeader("Настройки", includeStatusBarInsets = false)
            }

            Column(Modifier.reveal(reveal, 1)) {
                SectionHeader("Оформление", Modifier.padding(top = 16.dp))
                Column(Modifier.padding(vertical = 14.dp)) {
                    Text("Тема", style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ThemeTile(mode, s.themeMode == mode, Modifier.weight(1f)) { scope.launch { repo.setThemeMode(mode) } }
                        }
                    }
                Text("Светлая — тёплая бумага, тёмная — глубокий графит. Бордовые акценты и тонкие золотые детали.",
                        style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
                        modifier = Modifier.padding(top = 12.dp))
                }
                Hairline()
            }

            Column(Modifier.reveal(reveal, 2)) {
                SectionHeader("Звук и ощущения")
                SwitchSetting("Звук завершения занятия", s.interfaceSounds) { scope.launch { repo.setInterfaceSounds(it) } }
                Text("Тихое фортепианное созвучие в конце занятия. Учитывает беззвучный режим телефона.",
                    style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                SwitchSetting("Тактильный отклик", s.hapticsEnabled) { scope.launch { repo.setHapticsEnabled(it) } }
                SwitchSetting("Меньше движения", s.reduceMotion) { scope.launch { repo.setReduceMotion(it) } }
                Text("Спокойные переходы без перемещения и переворота карточек.",
                    style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                Hairline(Modifier.padding(top = 16.dp))
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
            containerColor = Ink.Raised,
            title = { Text("Сбросить прогресс учебника?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Отметки «изучено» и закладки будут удалены.") },
            confirmButton = {
                TextButton(onClick = { confirmGrammarReset = false; scope.launch { c.grammar.resetProgress() } }) { Text("Сбросить", color = Ink.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmGrammarReset = false }) { Text("Отмена", color = Ink.Muted) } },
        )
    }
}

/**
 * Плитка темы — маленькое превью экрана в её цветах: строки текста, пилюля прогресса и кнопка.
 * «Как в системе» показывает обе половины. Выбранная тема получает crimson-рамку.
 */
@Composable
private fun ThemeTile(mode: ThemeMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val lift by animateFloatAsState(if (selected) 1f else 0f, tween(180), label = "tile")
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .pressScale(interaction, 0.95f)
            .selectable(selected = selected, role = Role.RadioButton, interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().height(96.dp)
                .border(if (selected) 2.dp else 1.dp, lerpColor(Ink.LineStrong, Ink.Accent, lift), shape)
                .padding(4.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            when (mode) {
                ThemeMode.LIGHT -> ThemePreview(paletteFor(false), Modifier.fillMaxSize())
                ThemeMode.DARK -> ThemePreview(paletteFor(true), Modifier.fillMaxSize())
                ThemeMode.SYSTEM -> Row(Modifier.fillMaxSize()) {
                    ThemePreview(paletteFor(false), Modifier.weight(1f).fillMaxHeight())
                    ThemePreview(paletteFor(true), Modifier.weight(1f).fillMaxHeight())
                }
            }
            if (lift > 0.01f) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp)
                        .graphicsLayer { scaleX = lift; scaleY = lift }
                        .background(Ink.Accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(AlbionIcons.Check, null, tint = Ink.OnAccent, modifier = Modifier.size(14.dp)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(mode.title, style = MaterialTheme.typography.labelLarge, color = if (selected) Ink.Fg else Ink.Muted, textAlign = TextAlign.Center)
    }
}

/** Миниатюра экрана в цветах [p]. */
@Composable
private fun ThemePreview(p: Palette, modifier: Modifier) {
    Canvas(modifier.background(p.bg)) {
        val u = size.width / 10f
        val r = CornerRadius(u * 0.5f)
        drawRoundRect(p.fg, Offset(u, u * 1.2f), Size(u * 4.5f, u * 0.9f), r)
        drawRoundRect(p.muted, Offset(u, u * 2.6f), Size(u * 6.5f, u * 0.55f), r)
        drawRoundRect(p.raised, Offset(u, u * 3.8f), Size(size.width - u * 2, u * 2.4f), CornerRadius(u))
        drawRoundRect(p.high, Offset(u * 1.6f, u * 4.6f), Size(size.width - u * 3.2f, u * 0.6f), r)
        drawRoundRect(p.accent, Offset(u * 1.6f, u * 4.6f), Size((size.width - u * 3.2f) * 0.6f, u * 0.6f), r)
        drawRoundRect(p.accent, Offset(u, size.height - u * 2.4f), Size(size.width - u * 2, u * 1.4f), r)
        drawCircle(p.gold, u * 0.45f, Offset(size.width - u * 1.6f, u * 1.6f))
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
                selectedColor = Ink.Accent,
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
            Text(
                value, style = MaterialTheme.typography.titleMedium, color = Ink.AccentText,
                modifier = Modifier.background(Ink.AccentSoft, CircleShape).padding(horizontal = 10.dp, vertical = 2.dp),
            )
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
                checkedTrackColor = Ink.Accent,
                checkedThumbColor = Ink.OnAccent,
                checkedBorderColor = Ink.Accent,
                uncheckedTrackColor = Ink.Surface,
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
