package com.marat.albion

import com.marat.albion.notify.Reminders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderMessageTest {

    @Test
    fun mentionsDueCardsAndGoal() {
        val (title, text) = Reminders.message(answered = 5, goal = 30, due = 12, onlyIfNeeded = true)!!
        assertEquals("Время повторить", title)
        assertEquals("12 карточек ждут повторения · до цели 25 ответов", text)
    }

    @Test
    fun russianPlurals() {
        assertEquals("Осталось 1 ответ — займёт пару минут", Reminders.message(29, 30, 0, true)!!.second)
        assertEquals("Осталось 3 ответа — займёт пару минут", Reminders.message(27, 30, 0, true)!!.second)
        assertEquals("21 карточка к повторению — FSRS говорит, что пора", Reminders.message(30, 30, 21, true)!!.second)
    }

    @Test
    fun silentWhenNothingToDo() {
        assertNull(Reminders.message(answered = 40, goal = 30, due = 0, onlyIfNeeded = true))
        assertEquals("Цель выполнена", Reminders.message(40, 30, 0, onlyIfNeeded = false)!!.first)
    }
}
