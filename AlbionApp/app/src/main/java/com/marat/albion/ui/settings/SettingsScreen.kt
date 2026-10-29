package com.marat.albion.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.ImportStatus
import com.marat.albion.data.settings.NewCardOrder
import com.marat.albion.data.settings.Settings
import com.marat.albion.ui.components.*
import com.marat.albion.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val c = LocalAppContainer.current
    val s by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val status by c.importer.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val repo = c.settings
    var reset by rememberSaveable { mutableIntStateOf(0) }
    var resetBusy by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(status) { version = repo.contentVersion() }
    Column(Modifier.fillMaxSize().paperSurface()) {
        TopBar(onBack, "Настройки")
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = gutter).padding(bottom = 24.dp)) {
            Disclosure("Занятия и цель", "${s.dailyGoal} ответов в день") {
                GoalReminderSection(s)
                SliderSetting("Новых карточек в день", "${s.newPerDay}",
                    "На каждую колоду. Для шести колод — до ${s.newPerDay * 6} новых карточек в день.",
                    s.newPerDay.toFloat(), 0f..100f, 19) { scope.launch { repo.setNewPerDay((it / 5).roundToInt() * 5) } }
                SliderSetting("Повторений в день", "${s.reviewsPerDay}", "Для каждой колоды отдельно.",
                    s.reviewsPerDay.toFloat(), 50f..1000f, 18) { scope.launch { repo.setReviewsPerDay((it / 50).roundToInt() * 50) } }
                Disclosure("Порядок новых карточек", s.newOrder.title) {
                    Column(Modifier.selectableGroup()) { NewCardOrder.entries.forEach { order ->
                        RadioSettingOption(order.title, s.newOrder == order) { scope.launch { repo.setNewOrder(order) } }
                    } }
                }
            }
            Disclosure("Карточки и озвучка", "Перевод, транскрипция и воспроизведение") {
                SwitchSetting("Озвучивать слово при показе", s.autoplayFront) { scope.launch { repo.setAutoplayFront(it) } }
                SwitchSetting("Озвучивать при перевороте", s.autoplayBack) { scope.launch { repo.setAutoplayBack(it) } }
                SwitchSetting("Транскрипция на лицевой стороне", s.showIpaOnFront) { scope.launch { repo.setShowIpaOnFront(it) } }
                SwitchSetting("Русский перевод на обороте карточек", s.showRussian) { scope.launch { repo.setShowRussian(it) } }
                SwitchSetting("Русский перевод в словарном списке", s.browseShowRussian) { scope.launch { repo.setBrowseShowRussian(it) } }
                SliderSetting("Громкость озвучки", "${(s.playbackVolume * 100).roundToInt()}%",
                    "Относительно громкости медиа на телефоне.", s.playbackVolume, 0f..1f, 9) {
                    c.audio.setVolume(it); scope.launch { repo.setPlaybackVolume(it) }
                }
                SliderSetting("Скорость озвучки", "×%.1f".format(s.playbackSpeed), null, s.playbackSpeed, 0.5f..1.5f, 9) {
                    c.audio.setSpeed(it); scope.launch { repo.setPlaybackSpeed(it) }
                }
                ActionRow("Прослушать пример") { c.audio.speak("A little, every day.", "settings-preview") }
            }
            Disclosure("Чтение и учебник", "Размер текста, интервал и поля") {
                SliderSetting("Текст учебника", "${(s.grammarTextScale * 100).roundToInt()}%", null,
                    s.grammarTextScale, 0.85f..1.4f, 10) { scope.launch { repo.setGrammarTextScale(it) } }
                Text("Английский — маленькими шагами каждый день.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Reading, fontSize = (17 * s.grammarTextScale).sp))
                SliderSetting("Текст книги", "${(s.readerTextScale * 100).roundToInt()}%", null,
                    s.readerTextScale, 0.8f..1.5f, 13) { scope.launch { repo.setReaderTextScale(it) } }
                SliderSetting("Межстрочный интервал", "${(s.readerLineSpacing * 100).roundToInt()}%", null,
                    s.readerLineSpacing, 0.85f..1.3f, 8) { scope.launch { repo.setReaderLineSpacing(it) } }
                SliderSetting("Поля книги", "${s.readerMargin} dp", null, s.readerMargin.toFloat(), 16f..40f, 11) {
                    scope.launch { repo.setReaderMargin(it.roundToInt()) }
                }
                Text("A little, every day. A good book is always a good companion.", Modifier.padding(horizontal = s.readerMargin.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Reading, fontSize = (18 * s.readerTextScale).sp,
                        lineHeight = (29 * s.readerTextScale * s.readerLineSpacing).sp))
                SwitchSetting("Подчёркивать изучаемые слова в книге", s.readerMarkWords) { scope.launch { repo.setReaderMarkWords(it) } }
            }
            Disclosure("Звук и доступность", "Тактильный отклик и движение") {
                SwitchSetting("Звук завершения занятия", s.interfaceSounds) { scope.launch { repo.setInterfaceSounds(it) } }
                Text("Тихое созвучие с учётом беззвучного режима телефона.", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                SwitchSetting("Тактильный отклик", s.hapticsEnabled) { scope.launch { repo.setHapticsEnabled(it) } }
                SwitchSetting("Меньше движения", s.reduceMotion) { scope.launch { repo.setReduceMotion(it) } }
            }
            Disclosure("Расширенные настройки повторения", "Память, начало дня и персональная подстройка") {
                SliderSetting("Желаемое удержание", "${(s.desiredRetention * 100).roundToInt()}%",
                    "Вероятность вспомнить карточку при показе. Рекомендуется 90%.", s.desiredRetention, 0.80f..0.97f, 16) {
                    scope.launch { repo.setRetention((it * 100).roundToInt() / 100f) }
                }
                SliderSetting("Начало учебного дня", "%02d:00".format(s.dayStartHour),
                    "Занятия до этого часа относятся к предыдущему дню.", s.dayStartHour.toFloat(), 0f..8f, 7) {
                    scope.launch { repo.setDayStartHour(it.roundToInt()) }
                }
                FsrsTuningBlock(s)
            }
            Disclosure("Материалы и данные", "Состояние курса и управление прогрессом") {
                when (val st = status) {
                    ImportStatus.Checking -> LoadingState("Проверяем материалы")
                    is ImportStatus.Importing -> LoadingState("Подготавливаем ${st.deck.title} · ${st.index}/${st.total}")
                    is ImportStatus.Failed -> {
                        Text("Не удалось подготовить материалы.", color = Ink.Error)
                        ActionRow("Повторить подготовку") { c.appScope.launch { c.importer.importIfNeeded(force = true) } }
                    }
                    ImportStatus.Empty -> Text("Материалы недоступны. Установите полную версию приложения.", color = Ink.Muted)
                    ImportStatus.Ready -> Text("Учебные материалы готовы", style = MaterialTheme.typography.bodyLarge, color = Ink.Success)
                }
                Disclosure("Сведения о материалах", "Версия курса и обслуживание") {
                    Text("Версия материалов: ${version ?: "не определена"}", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                    Text("Материалы обновляются вместе с приложением. Прогресс сохраняется.", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                    ActionRow("Повторно проверить материалы") { c.appScope.launch { c.importer.importIfNeeded(force = true) } }
                }
                ActionRow("Сбросить только отметки прочитанного") { resetError = null; reset = 1 }
                ActionRow("Удалить весь прогресс учебника") { resetError = null; reset = 2 }
            }
            Text("Albion 1.0", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        }
    }
    if (reset != 0) AlertDialog(onDismissRequest = { if (!resetBusy) reset = 0 }, containerColor = Ink.Raised,
        title = { Text(if (reset == 1) "Сбросить отметки чтения?" else "Удалить весь прогресс учебника?") },
        text = { Column {
            Text(if (reset == 1) "Будут сняты только отметки «Прочитано». Освоение, закладки, результаты проверок и план повторения сохранятся."
                else "Будут удалены отметки чтения и освоения, закладки, последние результаты проверок и расписание повторения тем. Это действие нельзя отменить.")
            FieldError(resetError)
        } },
        confirmButton = { TextButton(enabled = !resetBusy, onClick = {
            resetBusy = true
            scope.launch {
                try { if (reset == 1) c.grammar.resetReadMarks() else c.grammar.resetProgress(); reset = 0 }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { resetError = "Не удалось выполнить сброс. Попробуйте ещё раз." }
                finally { resetBusy = false }
            }
        }) { Text(if (resetBusy) "Сохраняем…" else if (reset == 1) "Сбросить отметки" else "Удалить прогресс", color = Ink.Error) } },
        dismissButton = { TextButton(enabled = !resetBusy, onClick = { reset = 0 }) { Text("Отмена") } })
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
            Text(
                value, style = MaterialTheme.typography.titleMedium, color = Ink.AccentText,
                modifier = Modifier.background(Ink.AccentSoft, CircleShape).padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
        val step = (range.endInclusive - range.start) / (steps + 1)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = current > range.start, onClick = { onChange((current - step).coerceAtLeast(range.start)) },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = "Уменьшить: $title" }) { Text("−") }
        ThinSlider(
            value = current.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.weight(1f).semantics { contentDescription = title; stateDescription = value },
        )
            TextButton(enabled = current < range.endInclusive, onClick = { onChange((current + step).coerceAtMost(range.endInclusive)) },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = "Увеличить: $title" }) { Text("+") }
        }
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
    }
}

@Composable
internal fun SwitchSetting(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = Ink.Text, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
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
