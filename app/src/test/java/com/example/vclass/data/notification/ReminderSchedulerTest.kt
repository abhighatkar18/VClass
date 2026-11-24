package com.example.vclass.data.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderSchedulerTest {
    @Test
    fun futureEventUsesTheRemainingTime() {
        assertEquals(60_000L, eventReminderDelayMillis(eventMillis = 70_000, nowMillis = 10_000))
    }

    @Test
    fun elapsedAndCurrentEventsDoNotEnqueueAWorkRequest() {
        assertNull(eventReminderDelayMillis(eventMillis = 9_999, nowMillis = 10_000))
        assertNull(eventReminderDelayMillis(eventMillis = 10_000, nowMillis = 10_000))
    }
}
