package com.example.vclass.calendarscreen

import com.example.vclass.eventReminderKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CalendarEventsTest {
    @Test
    fun usesTheSelectedDayAndNormalizesReminderTimeInTheRequestedZone() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val picked = Calendar.getInstance(zone).apply {
            clear()
            set(2026, Calendar.MARCH, 8, 17, 39, 55)
        }
        val reminder = Calendar.getInstance(zone).apply { time = calendarDay(picked.time, 9, zone) }
        assertEquals(8, reminder.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, reminder.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, reminder.get(Calendar.MINUTE))
        assertEquals(0, reminder.get(Calendar.SECOND))
    }

    @Test
    fun reminderKeysAreStablePerAccountAndEvent() {
        val first = eventReminderKey("student", "event-a")
        assertEquals(first, eventReminderKey("student", "event-a"))
        assertNotEquals(first, eventReminderKey("student", "event-b"))
        assertNotEquals(first, eventReminderKey("teacher", "event-a"))
    }
}
