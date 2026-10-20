package com.marat.albion.ui.grammar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Пересчитываем очередь при возвращении на экран и раз в минуту, даже без изменений в БД. */
@Composable
internal fun grammarReviewTime(): Long {
    val clock = remember {
        flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(60_000)
            }
        }
    }
    val now by clock.collectAsStateWithLifecycle(System.currentTimeMillis())
    return now
}

internal fun grammarReviewDate(dueAt: Long, now: Long): String {
    val zone = ZoneId.systemDefault()
    val due = Instant.ofEpochMilli(dueAt).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val date = due.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru")))
    val time = due.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when {
        dueAt <= now -> if (due.toLocalDate() == today) "пора повторить" else "просрочено с $date"
        due.toLocalDate() == today -> "сегодня в $time"
        due.toLocalDate() == today.plusDays(1) -> "завтра в $time"
        else -> "$date в $time"
    }
}
