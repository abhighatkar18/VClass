package com.example.vclass.feature.classroom

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.vclass.data.firebase.AssignmentRepository.SubmissionDraft
import com.example.vclass.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


@Composable
fun UploadSubmissionDialog(
    classCode: String,
    assignmentId: String,
    assignmentTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uid = AppContainer.auth.currentUserId
    val repository = AppContainer.assignments
    val uploader = remember(context) { AppContainer.uploader(context) }
    val scope = rememberCoroutineScope()

    var uploading by remember(classCode, assignmentId) { mutableStateOf(false) }
    var pending by remember(classCode, assignmentId) { mutableStateOf<SubmissionDraft?>(null) }
    var errorMessage by remember(classCode, assignmentId) { mutableStateOf<String?>(null) }

    fun submit(uri: android.net.Uri? = null) {
        if (uploading || uid == null || (uri == null && pending == null)) return
        uploading = true
        errorMessage = null
        scope.launch {
            try {
                if (!repository.existsOnServer(classCode, assignmentId)) {
                    errorMessage = "This assignment is no longer available."
                    return@launch
                }
                val draft = pending ?: run {
                    val uploaded = uploader.upload(requireNotNull(uri))
                    if (uploaded == null) {
                        errorMessage = "Could not upload the file. Please try again."
                        return@launch
                    }
                    repository.prepareSubmission(classCode, assignmentId, uploaded.link).also { pending = it }
                }
                repository.saveSubmission(classCode, assignmentId, draft)
                Toast.makeText(context, "Assignment submitted!", Toast.LENGTH_SHORT).show()
                onDismiss()
            } catch (e: CancellationException) {
                throw e
            } catch (_: IllegalArgumentException) {
                errorMessage = "This assignment reference is invalid."
            } catch (_: Exception) {
                errorMessage = "Could not save the submission. Please try again."
            } finally {
                uploading = false
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) submit(uri)
    }

    AlertDialog(
        onDismissRequest = { if (!uploading) onDismiss() },
        title = { Text("Upload Assignment Submission") },
        text = {
            Column {
                if (uploading) {
                    CircularProgressIndicator()
                } else {
                    Text("Select a file to upload for \"$assignmentTitle\".")
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            if (!uploading) {
                Button(onClick = { if (pending == null) launcher.launch("*/*") else submit() }) {
                    Text(if (pending == null) "Choose File" else "Retry Save")
                }
            }
        },
        dismissButton = {
            if (!uploading) {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
