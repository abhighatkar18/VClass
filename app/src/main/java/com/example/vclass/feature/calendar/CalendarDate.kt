package com.example.vclass.feature.calendar

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

internal fun calendarDay(date: Date, hour: Int = 0, zone: TimeZone = TimeZone.getDefault()): Date =
    Calendar.getInstance(zone).apply {
        time = date
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
