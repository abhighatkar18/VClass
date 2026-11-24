package com.example.vclass.data.firebase

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

internal data class ProfileDetails(
    val name: String,
    val className: String,
    val rollNumber: String,
    val address: String,
    val phone: String
)

internal data class Profile(val details: ProfileDetails, val photo: Bitmap?)

internal class ProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val resolver: ContentResolver
) {
    private fun profile(uid: String) = db.collection("users").document(uid)

    suspend fun load(uid: String): Profile {
        val user = checkNotNull(auth.currentUser?.takeIf { it.uid == uid })
        val ref = profile(uid)
        var doc = ref.get().await()
        if (!doc.exists()) {
            ensureUserProfile(db, user)
            doc = ref.get().await()
        }
        val encoded = doc.getString("profileImage")
        return Profile(
            ProfileDetails(
                doc.displayName().orEmpty(), doc.getString("class").orEmpty(), doc.getString("rollNo").orEmpty(),
                doc.getString("address").orEmpty(), doc.getString("phoneNumber").orEmpty()
            ),
            decodeSavedPhoto(encoded)
        )
    }

    suspend fun update(uid: String, details: ProfileDetails) {
        profile(uid).update(
            mapOf(
                "fullName" to details.name, "class" to details.className, "rollNo" to details.rollNumber,
                "address" to details.address, "phoneNumber" to details.phone
            )
        ).await()
    }

    suspend fun savePhoto(uid: String, uri: Uri): Bitmap {
        val (bitmap, encoded) = withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            require(options.outWidth > 0 && options.outHeight > 0)
            options.inSampleSize = sampleSize(options.outWidth, options.outHeight)
            options.inJustDecodeBounds = false
            val image = requireNotNull(resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) })
            val bytes = ByteArrayOutputStream()
            check(image.compress(Bitmap.CompressFormat.JPEG, 85, bytes))
            val encoded = Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
            require(encoded.length <= 850_000)
            image to encoded
        }
        profile(uid).update("profileImage", encoded).await()
        return bitmap
    }

    private suspend fun decodeSavedPhoto(encoded: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (encoded == null) return@withContext null
        try {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null
            options.inSampleSize = sampleSize(options.outWidth, options.outHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var size = 1
        while (maxOf(width, height) / size > 1024) size *= 2
        return size
    }
}
