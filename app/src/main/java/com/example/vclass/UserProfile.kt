package com.example.vclass

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal fun DocumentSnapshot.displayName(): String? =
    getString("fullName")?.takeIf { it.isNotBlank() }
        ?: getString("name")?.takeIf { it.isNotBlank() }

internal suspend fun ensureUserProfile(db: FirebaseFirestore, user: FirebaseUser, name: String? = user.displayName) {
    val profile = db.collection("users").document(user.uid)
    db.runTransaction { transaction ->
        if (!transaction.get(profile).exists()) {
            transaction.set(profile, mapOf("name" to name.orEmpty().trim().take(120), "email" to user.email.orEmpty()))
        }
    }.await()
}
