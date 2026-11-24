package com.example.vclass.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

internal class AuthRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {
    val currentUserId: String? get() = auth.currentUser?.uid

    fun observeSession(onSessionChanged: (String?) -> Unit): AutoCloseable {
        val listener = FirebaseAuth.AuthStateListener { onSessionChanged(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        return AutoCloseable { auth.removeAuthStateListener(listener) }
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun register(name: String, email: String, password: String): Boolean {
        val user = checkNotNull(auth.createUserWithEmailAndPassword(email, password).await().user)
        try {
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The Firestore profile can still be provisioned with the requested name.
        }
        return try {
            ensureUserProfile(db, user, name)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    fun signOut() = auth.signOut()
}
