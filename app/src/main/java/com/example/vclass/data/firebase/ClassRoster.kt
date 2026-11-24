package com.example.vclass.data.firebase

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

internal fun DocumentSnapshot.memberIds(field: String): List<String> =
    (get(field) as? List<*>).orEmpty().filterIsInstance<String>()

internal fun DocumentSnapshot.includesMember(uid: String): Boolean =
    getString("creatorId") == uid || uid in memberIds("teachers") || uid in memberIds("members")

internal suspend fun DocumentReference.rosterNames(): Map<String, String> =
    collection("roster").get().await().documents.mapNotNull { doc ->
        doc.getString("displayName")?.let { doc.id to it }
    }.toMap()

internal suspend fun DocumentReference.publishRosterName(uid: String, profile: DocumentSnapshot) {
    val name = profile.displayName()?.take(120) ?: return
    val entry = collection("roster").document(uid)
    if (entry.get().await().getString("displayName") != name) {
        entry.set(mapOf("displayName" to name)).await()
    }
}
