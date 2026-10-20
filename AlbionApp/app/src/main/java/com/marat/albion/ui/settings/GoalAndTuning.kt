package com.marat.albion.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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

    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) scope.launch { c.settings.setReminderEnabled(true) }
    }

    SectionHeader("Цель и напоминания")
    SliderSetting(
        "Дневная цель", "${s.dailyGoal} ${plural(s.dailyGoal)}",
        "Ответы во всех колодах за учебный день. Прогресс виден на главном экране.",
        s.dailyGoal.toFloat(), 10f..200f, 18,
    ) { scope.launch { c.settings.setDailyGoal(((it / 10).roundToInt() * 10).coerceIn(10, 200)) } }

    SwitchSetting("Ежедневное напоминание", s.reminderEnabled) { on ->
        when {
            !on -> scope.launch { c.settings.setReminderEnabled(false) }
            Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context) -> notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> scope.launch { c.settings.setReminderEnabled(true) }
        }
    }
    AnimatedVisibility(denied && !s.reminderEnabled, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Text(
            "Без разрешения на уведомления напомнить не получится. Его можно выдать в настройках телефона → Приложения → Albion.",
            style = MaterialTheme.typography.bodySmall,
            color = Ink.Muted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    AnimatedVisibility(s.reminderEnabled, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
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
        AlertDialog(
            onDismissRequest = { pickTime = false },
            containerColor = Ink.Elevated,
            title = { Eyebrow("Время напоминания", color = Ink.Soft) },
            text = {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Ink.High,
                        clockDialSelectedContentColor = Ink.Bg,
                        clockDialUnselectedContentColor = Ink.Text,
                        selectorColor = Ink.Fg,
                        containerColor = Ink.Elevated,
                        timeSelectorSelectedContainerColor = Ink.Fg,
                        timeSelectorUnselectedContainerColor = Ink.High,
                        timeSelectorSelectedContentColor = Ink.Bg,
                        timeSelectorUnselectedContentColor = Ink.Text,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
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
                        "Готово: ошибка на ${o.validationReviews} отложенных повторениях ${"%.3f".format(o.lossBefore)} → ${"%.3f".format(o.lossAfter)} " +
                            "(−${"%.1f".format(o.improvementPercent)}%). Новые параметры уже применяются к интервалам."
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
