package com.example.vclass.calendarscreen

import android.content.Context
import com.example.vclass.scheduleNotification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

internal suspend fun saveCalendarEvent(context: Context, db: FirebaseFirestore, uid: String, title: String, date: Date) {
    val name = title.trim()
    require(name.length in 1..120)
    val event = db.collection("users").document(uid).collection("events").document()
    event.set(mapOf("title" to name, "date" to date)).await()
    scheduleNotification(context, uid, event.id, date)
}

internal fun calendarDay(date: Date, hour: Int = 0, zone: TimeZone = TimeZone.getDefault()): Date =
    Calendar.getInstance(zone).apply {
        time = date
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
