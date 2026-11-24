package com.example.vclass.data.firebase

import android.content.Context
import com.example.vclass.data.notification.cancelNotification
import com.example.vclass.domain.Event
import com.example.vclass.data.notification.scheduleNotification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

internal class CalendarRepository(private val db: FirebaseFirestore, private val applicationContext: Context) {
    private fun events(uid: String) = db.collection("users").document(uid).collection("events")

    fun observe(uid: String) = events(uid).observeSnapshots(MetadataChanges.INCLUDE).map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
            val date = when (val value = doc.get("date")) {
                is Timestamp -> value.toDate()
                is Date -> value
                else -> return@mapNotNull null
            }
            if (!doc.metadata.hasPendingWrites()) {
                scheduleNotification(applicationContext, uid, doc.id, date)
            }
            Event(doc.id, (doc.get("title") as? String).orEmpty(), date.time)
        }.sortedBy { it.timeMillis }
    }

    suspend fun save(uid: String, title: String, date: Date) {
        val name = title.trim()
        require(name.length in 1..120)
        val ref = events(uid).document()
        ref.set(mapOf("title" to name, "date" to date)).await()
        scheduleNotification(applicationContext, uid, ref.id, date)
    }

    suspend fun delete(uid: String, id: String) {
        events(uid).document(id).delete().await()
        cancelNotification(applicationContext, uid, id)
    }

    suspend fun confirmedEventTitle(uid: String, id: String): String? {
        val doc = events(uid).document(id).get(Source.SERVER).await()
        return if (doc.exists()) doc.get("title") as? String ?: "Event Reminder" else null
    }
}
