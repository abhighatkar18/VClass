package com.example.vclass.data.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.vclass.NotificationWorker
import java.util.Date
import java.util.concurrent.TimeUnit

internal fun eventReminderKey(uid: String, eventId: String): String = "calendar/$uid/$eventId"

internal fun eventReminderDelayMillis(eventMillis: Long, nowMillis: Long): Long? =
    (eventMillis - nowMillis).takeIf { it > 0 }

internal fun canPostReminders(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

internal fun scheduleNotification(context: Context, uid: String, eventId: String, eventDate: Date) {
    val delay = eventReminderDelayMillis(eventDate.time, System.currentTimeMillis()) ?: return
    val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setInputData(
            workDataOf("uid" to uid, "eventId" to eventId)
        )
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(eventReminderKey(uid, eventId), ExistingWorkPolicy.KEEP, workRequest)
}

internal fun cancelNotification(context: Context, uid: String, eventId: String) {
    val key = eventReminderKey(uid, eventId)
    WorkManager.getInstance(context).cancelUniqueWork(key)
    context.getSystemService(NotificationManager::class.java).cancel(key, 1)
}
