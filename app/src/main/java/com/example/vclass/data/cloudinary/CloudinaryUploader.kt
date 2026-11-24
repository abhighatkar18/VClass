package com.example.vclass.data.cloudinary

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.example.vclass.core.config.CloudinaryConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume

internal class CloudinaryUploader(private val resolver: ContentResolver, private val client: OkHttpClient) {
    data class UploadedFile(val name: String, val link: String)

    private data class PickedFile(val name: String, val size: Long)

    private suspend fun fileMetadata(uri: Uri): PickedFile = withContext(Dispatchers.IO) {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
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

    suspend fun upload(uri: Uri): UploadedFile? {
        val file = fileMetadata(uri)
        val link = uploadFile(uri, file) ?: return null
        return UploadedFile(file.name, link)
    }
    private suspend fun uploadFile(uri: Uri, file: PickedFile): String? {
        val fileBody = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
            override fun contentLength() = file.size.coerceAtLeast(-1)
            override fun isOneShot() = true

            override fun writeTo(sink: BufferedSink) {
                val input = resolver.openInputStream(uri) ?: throw IOException("File unavailable")
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
            val call = client.newCall(request)
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
}
