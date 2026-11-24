package com.example.vclass.feature.classroom

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.vclass.data.firebase.ClassroomRepository.ResourceDraft
import com.example.vclass.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun UploadResourceDialog(classCode: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = AppContainer.classrooms
    val uploader = remember(context) { AppContainer.uploader(context) }
    var uploading by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<ResourceDraft?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun save(uri: Uri? = null) {
        if (AppContainer.auth.currentUserId == null) return
        if (uploading || (uri == null && pending == null)) return
        uploading = true
        errorMessage = null
        scope.launch {
            try {
                val draft = pending ?: run {
                    val file = uploader.upload(requireNotNull(uri))
                    if (file == null) {
                        errorMessage = "Could not upload the file."
                        return@launch
                    }
                    repository.prepareResource(classCode, file.name, file.link).also { pending = it }
                }
                repository.saveResource(classCode, draft)
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
        text = {
            if (errorMessage != null) {
                Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            } else {
                Text(if (uploading) "Sharing file…" else "Pick a file to share with this class.")
            }
        },
        confirmButton = {
            Button(enabled = !uploading, onClick = {
                if (pending == null) launcher.launch(arrayOf("*/*")) else save()
            }) {
                Text(if (pending == null) "Select File" else "Retry Save")
            }
        },
        dismissButton = {
            TextButton(enabled = !uploading, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
