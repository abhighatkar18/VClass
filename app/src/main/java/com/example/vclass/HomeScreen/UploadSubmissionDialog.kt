package com.example.vclass.HomeScreen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vclass.calendarscreen.showDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PendingSubmission(val id: String, val data: Map<String, Any>)

@Composable
fun UploadSubmissionDialog(
    classCode: String,
    assignmentId: String,
    assignmentTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var uploading by remember(classCode, assignmentId) { mutableStateOf(false) }
    var pending by remember(classCode, assignmentId) { mutableStateOf<PendingSubmission?>(null) }
    var errorMessage by remember(classCode, assignmentId) { mutableStateOf<String?>(null) }

    fun submit(uri: android.net.Uri? = null) {
        if (uploading || user == null || (uri == null && pending == null)) return
        uploading = true
        errorMessage = null
        scope.launch {
            try {
                val assignment = db.assignmentDocument(classCode, assignmentId)
                if (!assignment.get(Source.SERVER).await().exists()) {
                    errorMessage = "This assignment is no longer available."
                    return@launch
                }
                val submissions = assignment.collection("submissions")
                val draft = pending ?: run {
                    val cloudUrl = uploadFileToCloudinary(context, requireNotNull(uri))
                    if (cloudUrl == null) {
                        errorMessage = "Could not upload the file. Please try again."
                        return@launch
                    }
                    PendingSubmission(
                        id = submissions.document().id,
                        data = mapOf(
                            "fileUrl" to cloudUrl,
                            "submittedAt" to System.currentTimeMillis(),
                            "studentId" to user.uid,
                            "studentName" to (user.displayName ?: "Class member")
                        )
                    ).also { pending = it }
                }
                val submission = submissions.document(draft.id)
                db.runTransaction { transaction ->
                    check(transaction.get(assignment).exists())
                    val previous = transaction.get(submission)
                    if (previous.exists()) {
                        check(previous.data == draft.data)
                    } else {
                        transaction.set(submission, draft.data)
                    }
                }.await()
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

@Composable
fun AddAssignmentDialog(
    classCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Assignment") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null // clear previous error
                    },
                    label = { Text("Assignment Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        showDatePicker(context) { selectedDate ->
                            dueDate = selectedDate.time
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Pick Due Date")
                }

                dueDate?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Selected Due Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))}")
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isUploading || user == null) return@Button
                    if (title.trim().length !in 1..120) {
                        errorMessage = "Enter a title between 1 and 120 characters."
                        return@Button
                    }

                    isUploading = true
                    errorMessage = null
                    val assignmentData = hashMapOf(
                        "title" to title.trim(),
                        "dueDate" to dueDate,
                        "createdBy" to user.uid,
                        "createdAt" to System.currentTimeMillis()
                    )
                    coroutineScope.launch {
                        try {
                            db.collection("classes")
                                .document(classCode)
                                .collection("assignments")
                                .add(assignmentData)
                                .await()
                            Toast.makeText(context, "Assignment added!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            errorMessage = "Could not add the assignment. Please try again."
                        } finally {
                            isUploading = false
                        }
                    }
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text(if (isUploading) "Adding..." else "Add Assignment")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Cancel")
            }
        }
    )
}
