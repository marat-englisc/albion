package com.marat.albion.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.marat.albion.ui.theme.QuietAnimatedVisibility as AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.fsrs.OptimizeOutcome
import com.marat.albion.data.repo.TuningState
import com.marat.albion.data.settings.Settings
import com.marat.albion.notify.Reminders
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.theme.Ink
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/** Раздел «Цель и напоминания». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalReminderSection(s: Settings) {
    val c = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickTime by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    var canNotify by remember { mutableStateOf(Reminders.canNotify(context)) }

    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        canNotify = Reminders.canNotify(context)
        if (granted) scope.launch { c.settings.setReminderEnabled(true) }
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        canNotify = Reminders.canNotify(context)
        if (denied && canNotify) {
            denied = false
            scope.launch { c.settings.setReminderEnabled(true) }
        }
    }
    SliderSetting(
        "Дневная цель", "${s.dailyGoal} ${plural(s.dailyGoal)}",
        "Ответы в карточках и практике за учебный день. Прогресс виден на главном экране.",
        s.dailyGoal.toFloat(), 10f..200f, 18,
    ) { scope.launch { c.settings.setDailyGoal(((it / 10).roundToInt() * 10).coerceIn(10, 200)) } }

    SwitchSetting("Ежедневное напоминание", s.reminderEnabled && canNotify) { on ->
        when {
            !on -> scope.launch { c.settings.setReminderEnabled(false) }
            Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context) -> notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> scope.launch { c.settings.setReminderEnabled(true) }
        }
    }
    if ((denied || s.reminderEnabled) && !canNotify) Column {
        Text(
            "Без разрешения на уведомления напомнить не получится. Его можно выдать в настройках телефона → Приложения → Albion.",
            style = MaterialTheme.typography.bodySmall,
            color = Ink.Muted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        com.marat.albion.ui.components.AppSettingsButton()
    }
    if (s.reminderEnabled) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { pickTime = true }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Время", style = MaterialTheme.typography.bodyLarge, color = Ink.Text, modifier = Modifier.weight(1f))
                Text("%02d:%02d".format(s.reminderHour, s.reminderMinute), style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
            }
            SwitchSetting("Только если есть что повторить", s.reminderOnlyIfNeeded) {
                scope.launch { c.settings.setReminderOnlyIfNeeded(it) }
            }
            Text(
                "Напомню, если цель ещё не выполнена или карточки ждут повторения.",
                style = MaterialTheme.typography.bodySmall,
                color = Ink.Muted,
            )
            Spacer(Modifier.height(6.dp))
            ActionRow("Прислать пробное напоминание") {
                scope.launch {
                    val (answered, due) = c.study.todayStatus()
                    val (title, text) = Reminders.message(answered, s.dailyGoal, due, onlyIfNeeded = false)
                        ?: ("Albion" to "Так будет выглядеть напоминание")
                    Reminders.show(context, title, text)
                }
            }
        }
    }
    Hairline()

    if (pickTime) {
        val state = rememberTimePickerState(s.reminderHour, s.reminderMinute, is24Hour = true)
        var clock by rememberSaveable { mutableStateOf(false) }
        var hours by rememberSaveable { mutableStateOf("%02d".format(s.reminderHour)) }
        var minutes by rememberSaveable { mutableStateOf("%02d".format(s.reminderMinute)) }
        val validHours = (hours.toIntOrNull() ?: -1) in 0..23
        val validMinutes = (minutes.toIntOrNull() ?: -1) in 0..59
        val valid = clock || (validHours && validMinutes)
        val offerClock = androidx.compose.ui.platform.LocalDensity.current.fontScale <= 1.3f
        androidx.compose.runtime.LaunchedEffect(offerClock) {
            if (!offerClock && clock) { clock = false; hours = "%02d".format(state.hour); minutes = "%02d".format(state.minute) }
        }
        AlertDialog(
            onDismissRequest = { pickTime = false },
            containerColor = Ink.Raised,
            title = { Eyebrow("Время напоминания", color = Ink.Soft) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                if (!clock) {
                    val fields: @Composable (Modifier, Modifier) -> Unit = { hourModifier, minuteModifier ->
                        OutlinedTextField(hours, { value ->
                            hours = value.filter(Char::isDigit).take(2)
                            hours.toIntOrNull()?.takeIf { it in 0..23 }?.let { state.hour = it }
                        }, label = { Text("Часы") }, singleLine = true, isError = !validHours,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), modifier = hourModifier)
                        OutlinedTextField(minutes, { value ->
                            minutes = value.filter(Char::isDigit).take(2)
                            minutes.toIntOrNull()?.takeIf { it in 0..59 }?.let { state.minute = it }
                        }, label = { Text("Минуты") }, singleLine = true, isError = !validMinutes,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), modifier = minuteModifier)
                    }
                    if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { fields(Modifier.fillMaxWidth(), Modifier.fillMaxWidth()) }
                    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { fields(Modifier.weight(1f), Modifier.weight(1f)) }
                    Text("24-часовой формат", style = MaterialTheme.typography.bodySmall, color = Ink.Muted, modifier = Modifier.padding(top = 8.dp))
                    com.marat.albion.ui.components.FieldError(if (!validHours) "Введите часы от 0 до 23." else if (!validMinutes) "Введите минуты от 0 до 59." else null)
                } else TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Ink.High,
                        clockDialSelectedContentColor = Ink.Bg,
                        clockDialUnselectedContentColor = Ink.Text,
                        selectorColor = Ink.Fg,
                        containerColor = Ink.Raised,
                        timeSelectorSelectedContainerColor = Ink.Fg,
                        timeSelectorUnselectedContainerColor = Ink.High,
                        timeSelectorSelectedContentColor = Ink.Bg,
                        timeSelectorUnselectedContentColor = Ink.Text,
                    ),
                )
                if (offerClock) TextButton(enabled = valid, onClick = {
                    if (clock) { hours = "%02d".format(state.hour); minutes = "%02d".format(state.minute) }
                    clock = !clock
                }) { Text(if (clock) "Ввести цифрами" else "Выбрать на циферблате") }
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    pickTime = false
                    scope.launch { c.settings.setReminderTime(state.hour, state.minute) }
                }) { Text("Готово", color = Ink.Fg) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Отмена", color = Ink.Muted) } },
        )
    }
}

/** Блок «Подстройка под вас» в разделе FSRS. */
@Composable
fun FsrsTuningBlock(s: Settings) {
    val c = LocalAppContainer.current
    val state by c.tuner.state.collectAsStateWithLifecycle()
    val custom = s.fsrsParams != null

    Hairline()
    Column(Modifier.padding(top = 16.dp)) {
        Text("Подстройка под вашу память", style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
        Spacer(Modifier.height(6.dp))
        Text(
            if (custom) {
                "Персональные параметры · обновлены " +
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(s.fsrsUpdatedAt)) +
                    " · по ${s.fsrsReviews} повторениям · снижение ошибки последнего подбора на ${"%.1f".format(s.fsrsImprovement)}%"
            } else {
                "Сейчас стандартные параметры FSRS-6. Алгоритм может изучить вашу историю ответов и подобрать " +
                    "параметры под то, как быстро вы запоминаете и забываете."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Ink.Muted,
        )

        when (val st = state) {
            is TuningState.Running -> Column(Modifier.padding(top = 16.dp)) {
                ThinProgress(st.progress)
                Spacer(Modifier.height(8.dp))
                Eyebrow("Подбираю параметры · ${(st.progress * 100).toInt()}%", color = Ink.Soft)
            }
            is TuningState.Finished -> Text(
                when (val o = st.outcome) {
                    is OptimizeOutcome.NotEnoughData ->
                        "Пока мало данных: нужно хотя бы ${o.need} повторений с интервалом от суток, сейчас ${o.have}. Позанимайтесь ещё пару недель."
                    is OptimizeOutcome.InsufficientSplit ->
                        "Пока не удалось отделить историю для проверки: ${o.trainingReviews} повторений для обучения и ${o.validationReviews} для проверки. Позанимайтесь ещё и повторите подбор."
                    OptimizeOutcome.Superseded ->
                        "Параметры изменились во время подбора. Его результат не применён — запустите подбор заново."
                    is OptimizeOutcome.Done -> if (o.improved) {
                        "Память настроена по вашей истории. Новые интервалы уже применяются; прогноз улучшился на ${"%.1f".format(o.improvementPercent)}%."
                    } else {
                        "На отложенной истории новые параметры не улучшили прогноз — текущие параметры сохранены."
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.Fg,
                modifier = Modifier.padding(top = 14.dp),
            )
            is TuningState.Failed -> Text("Не удалось: ${st.message}", style = MaterialTheme.typography.bodyMedium, color = Ink.Fg, modifier = Modifier.padding(top = 14.dp))
            TuningState.Idle -> {}
        }

        Spacer(Modifier.height(6.dp))
        if (state !is TuningState.Running) {
            ActionRow(if (custom) "Подобрать заново" else "Подобрать параметры по моим ответам") { c.tuner.run() }
            if (custom) ActionRow("Вернуть стандартные параметры") { c.tuner.reset() }
        }
    }
}

private fun plural(n: Int): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> "ответ"
        m10 in 2..4 && m100 !in 12..14 -> "ответа"
        else -> "ответов"
    }
}
