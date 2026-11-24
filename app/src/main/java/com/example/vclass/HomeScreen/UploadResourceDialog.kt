package com.example.vclass.HomeScreen

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.vclass.CloudinaryConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume

private val uploadClient = OkHttpClient()
private data class PickedFile(val name: String, val size: Long)
private data class PendingResource(val reference: DocumentReference, val data: Map<String, Any>)

private suspend fun fileMetadata(context: Context, uri: Uri): PickedFile = withContext(Dispatchers.IO) {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            PickedFile(
                name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "Class file" else "Class file",
                size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1
            )
        } ?: PickedFile("Class file", -1)
}

internal suspend fun uploadFileToCloudinary(context: Context, uri: Uri): String? =
    uploadFileToCloudinary(context, uri, fileMetadata(context, uri))

private suspend fun uploadFileToCloudinary(context: Context, uri: Uri, file: PickedFile): String? {
    val fileBody = object : RequestBody() {
        override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
        override fun contentLength() = file.size.coerceAtLeast(-1)
        override fun isOneShot() = true

        override fun writeTo(sink: BufferedSink) {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("File unavailable")
            input.source().use { sink.writeAll(it) }
        }
    }
    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("file", file.name, fileBody)
        .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
        .build()
    val request = Request.Builder()
        .url("https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/auto/upload")
        .post(body)
        .build()

    return suspendCancellableCoroutine { continuation ->
        val call = uploadClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val link = response.use {
                    runCatching {
                        if (!it.isSuccessful) return@runCatching null
                        val url = JSONObject(it.body?.string().orEmpty()).optString("secure_url")
                        url.takeIf { link ->
                            val parsed = Uri.parse(link)
                            parsed.scheme == "https" && !parsed.host.isNullOrBlank()
                        }
                    }.getOrNull()
                }
                continuation.resume(link)
            }
        })
    }
}

@Composable
fun UploadResourceDialog(classCode: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingResource?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun save(uri: Uri? = null) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (uploading || (uri == null && pending == null)) return
        uploading = true
        errorMessage = null
        scope.launch {
            try {
                val draft = pending ?: run {
                    val fileUri = requireNotNull(uri)
                    val file = fileMetadata(context, fileUri)
                    val link = uploadFileToCloudinary(context, fileUri, file)
                    if (link == null) {
                        errorMessage = "Could not upload the file."
                        return@launch
                    }
                    val reference = FirebaseFirestore.getInstance().collection("classes").document(classCode)
                        .collection("resources").document()
                    PendingResource(
                        reference,
                        mapOf(
                            "name" to file.name,
                            "cloudinaryLink" to link,
                            "uploadedBy" to (user.displayName ?: "Class member"),
                            "uploadedAt" to System.currentTimeMillis()
                        )
                    ).also { pending = it }
                }
                draft.reference.set(draft.data).await()
                Toast.makeText(context, "Uploaded successfully!", Toast.LENGTH_SHORT).show()
                onDismiss()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorMessage = "Could not share this resource. Please try again."
            } finally {
                uploading = false
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) save(uri)
    }

    AlertDialog(
        onDismissRequest = { if (!uploading) onDismiss() },
        title = { Text("Upload Resource") },
        text = { Text(errorMessage ?: if (uploading) "Sharing file..." else "Pick a file to share with this class.") },
        confirmButton = {
            Button(enabled = !uploading, onClick = {
                if (pending == null) launcher.launch(arrayOf("*/*")) else save()
            }) {
                Text(if (pending == null) "Select File" else "Retry Save")
            }
        },
        dismissButton = {
            Button(enabled = !uploading, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
