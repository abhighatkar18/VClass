package com.example.vclass.HomeScreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.vclass.CloudinaryConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

fun uploadToCloudinary(context: Context, uri: Uri, classCode: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.d("CloudinaryUpload", "Starting upload for URI: $uri")

            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w("CloudinaryUpload", "Could not persist URI permission: ${e.message}")
            }

            val url = uploadFileToCloudinary(context, uri)

            if (url != null) {
                saveCloudinaryResourceToFirestore(
                    classCode,
                    getFileNameFromUri(context, uri),
                    url,
                    FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Uploaded successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Upload failed: Cloudinary rejected the file", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("CloudinaryUpload", "Error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    var name = "UnknownFile"
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) {
            name = it.getString(nameIndex)
        }
    }
    return name
}
suspend fun uploadFileToCloudinary(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val cloudName = CloudinaryConfig.CLOUD_NAME
        val uploadPreset = CloudinaryConfig.UPLOAD_PRESET

        val fileBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }

        if (fileBytes == null) {
            Log.e("CloudinaryUpload", "Failed to read bytes from URI: $uri")
            return@withContext null
        }

        Log.d("CloudinaryUpload", "Uploading file size: ${fileBytes.size} bytes")

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                getFileNameFromUri(context, uri),
                fileBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/auto/upload")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        Log.d("CloudinaryUpload", "Cloudinary response: $responseBody")

        return@withContext if (response.isSuccessful) {
            val url = Regex("\"secure_url\":\"(.*?)\"").find(responseBody ?: "")?.groupValues?.get(1)
            url?.replace("\\/", "/")
        } else {
            Log.e("CloudinaryUpload", "Upload failed. Code: ${response.code}, Body: $responseBody")
            null
        }
    } catch (e: Exception) {
        Log.e("CloudinaryUpload", "Exception during upload: ${e.message}", e)
        null
    }
}

suspend fun saveCloudinaryResourceToFirestore(
    classCode: String,
    fileName: String,
    link: String,
    uploadedBy: String
) {
    val db = FirebaseFirestore.getInstance()
    val data = mapOf(
        "name" to fileName,
        "cloudinaryLink" to link,
        "uploadedBy" to uploadedBy,
        "uploadedAt" to System.currentTimeMillis()
    )

    db.collection("classes")
        .document(classCode)
        .collection("resources")
        .add(data)
}

@Composable
fun UploadResourceDialog(
    classCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        Log.d("FilePicker", "URI selected: $uri")
        if (uri != null) {
            uploadToCloudinary(context, uri, classCode)
        }
        onDismiss() // ✅ Now only dismissing after selection
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Resource") },
        text = { Text("Pick a file to upload to Cloudinary and share with this class.") },
        confirmButton = {
            Button(onClick = {
                launcher.launch(arrayOf("*/*")) // ✅ Don't dismiss here
            }) {
                Text("Select File")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
