package com.example.vclass

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vclass.data.notification.canPostReminders
import com.example.vclass.data.notification.eventReminderKey
import com.example.vclass.di.AppContainer
import kotlinx.coroutines.CancellationException

// WorkManager stores this fully qualified name with scheduled reminders.
class NotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.success()
        val eventId = inputData.getString("eventId") ?: return Result.success()
        val auth = AppContainer.auth
        if (auth.currentUserId != uid || !canPostReminders(applicationContext)) return Result.success()

        val eventTitle = try {
            AppContainer.calendar(applicationContext).confirmedEventTitle(uid, eventId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
        if (eventTitle == null || auth.currentUserId != uid) return Result.success()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "event_channel",
                "Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, "event_channel")
            .setContentTitle("Upcoming Event")
            .setContentText(eventTitle)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        if (canPostReminders(applicationContext)) {
            try {
                notificationManager.notify(eventReminderKey(uid, eventId), 1, notification)
            } catch (_: SecurityException) {
                // Notification permissions may change between the check and the call.
            }
        }
        return Result.success()
    }
}
