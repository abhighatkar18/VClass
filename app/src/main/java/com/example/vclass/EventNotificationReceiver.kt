package com.example.vclass

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.success()
        val eventId = inputData.getString("eventId") ?: return Result.success()
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser?.uid != uid || !canPostReminders(applicationContext)) return Result.success()

        val event = try {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("events").document(eventId).get(Source.SERVER).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
        if (!event.exists() || auth.currentUser?.uid != uid) return Result.success()
        val eventTitle = event.get("title") as? String ?: "Event Reminder"

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



internal fun eventReminderKey(uid: String, eventId: String): String = "calendar/$uid/$eventId"

internal fun canPostReminders(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
internal fun rememberReminderPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostReminders(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

internal fun scheduleNotification(context: Context, uid: String, eventId: String, eventDate: Date) {
    val delay = eventDate.time - System.currentTimeMillis()

    if (delay > 0) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf("uid" to uid, "eventId" to eventId)
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(eventReminderKey(uid, eventId), ExistingWorkPolicy.KEEP, workRequest)
    }
}

internal fun cancelNotification(context: Context, uid: String, eventId: String) {
    val key = eventReminderKey(uid, eventId)
    WorkManager.getInstance(context).cancelUniqueWork(key)
    context.getSystemService(NotificationManager::class.java).cancel(key, 1)
}
