package com.marat.albion.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.marat.albion.AlbionApp
import com.marat.albion.MainActivity
import com.marat.albion.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Ежедневное напоминание. Будильник ставится с окном в 10 минут —
 * разрешение на точные будильники не нужно, батарея не страдает.
 */
object Reminders {
    const val CHANNEL = "daily_reminder"
    private const val ACTION = "com.marat.albion.REMIND"
    private const val REQUEST = 4201
    private const val NOTIFICATION_ID = 4202

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(CHANNEL, "Ежедневное напоминание", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Напоминание о дневной цели и карточках к повторению"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun pendingIntent(context: Context, flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent? =
        PendingIntent.getBroadcast(
            context, REQUEST,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Ставит следующее срабатывание на ближайшие [hour]:[minute]. */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 30_000) add(Calendar.DAY_OF_YEAR, 1)
        }
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.setWindow(AlarmManager.RTC_WAKEUP, next.timeInMillis, 10 * 60_000L, pendingIntent(context)!!)
    }

    fun cancel(context: Context) {
        pendingIntent(context, PendingIntent.FLAG_NO_CREATE)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Текст напоминания по реальному состоянию; null — напоминать не о чем. */
    fun message(answered: Int, goal: Int, due: Int, onlyIfNeeded: Boolean): Pair<String, String>? {
        val left = (goal - answered).coerceAtLeast(0)
        return when {
            due > 0 && left > 0 -> "Время повторить" to "${cards(due)} ждут повторения · до цели ${answers(left)}"
            left > 0 -> "Цель дня ещё не выполнена" to "Осталось ${answers(left)} — займёт пару минут"
            due > 0 -> "Карточки ждут" to "${cards(due)} к повторению — FSRS говорит, что пора"
            onlyIfNeeded -> null
            else -> "Цель выполнена" to "Отличный день. Загляните в учебник или сыграйте в мини-игру"
        }
    }

    // Разрешение проверяет canNotify() на первой строке; lint не видит проверку через вспомогательную функцию.
    @SuppressLint("MissingPermission")
    fun show(context: Context, title: String, text: String) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private fun plural(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1 && m100 != 11 -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }

    private fun cards(n: Int) = "$n ${plural(n, "карточка", "карточки", "карточек")}"
    private fun answers(n: Int) = "$n ${plural(n, "ответ", "ответа", "ответов")}"
}

/** Срабатывание будильника и восстановление после перезагрузки, обновления, смены пояса или часов. */
class ReminderReceiver : BroadcastReceiver() {
    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED, // "android.intent.action.TIME_SET"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AlbionApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val c = app.container
                val s = c.settings.current()
                if (!s.reminderEnabled) return@launch
                if (intent.action in RESCHEDULE_ACTIONS) {
                    Reminders.schedule(context, s.reminderHour, s.reminderMinute)
                    return@launch
                }
                val (answered, due) = c.study.todayStatus()
                Reminders.message(answered, s.dailyGoal, due, s.reminderOnlyIfNeeded)?.let { (title, text) ->
                    Reminders.show(context, title, text)
                }
                Reminders.schedule(context, s.reminderHour, s.reminderMinute)
            } finally {
                pending.finish()
            }
        }
    }
}
