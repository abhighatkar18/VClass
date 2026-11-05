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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UploadSubmissionDialog(
    classCode: String,
    assignmentTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var uploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && user != null) {
            uploading = true
            scope.launch {
                try {
                    val cloudUrl = uploadFileToCloudinary(context, uri)
                    uploading = false
                    if (cloudUrl != null) {
                        val submission = mapOf(
                            "fileUrl" to cloudUrl,
                            "submittedAt" to System.currentTimeMillis(),
                            "studentId" to user.uid,
                            "studentName" to (user.displayName ?: "Unknown")
                        )
                        db.collection("classes").document(classCode)
                            .collection("assignments").document(assignmentTitle)
                            .collection("submissions").add(submission)

                        Toast.makeText(context, "Assignment submitted!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Upload failed!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    uploading = false
                    e.printStackTrace() // Log full stack trace to Logcat
                    Toast.makeText(context, "Upload failed: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }

            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Assignment Submission") },
        text = {
            Column {
                if (uploading) {
                    CircularProgressIndicator()
                } else {
                    Text("Select a file to upload for \"$assignmentTitle\".")
                }
            }
        },
        confirmButton = {
            if (!uploading) {
                Button(onClick = { launcher.launch("*/*") }) {
                    Text("Choose File")
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
                    if (title.isBlank()) {
                        errorMessage = "Title cannot be empty"
                        return@Button
                    }

                    coroutineScope.launch {
                        try {
                            isUploading = true

                            val sanitizedTitle = title.trim().replace("\\s+".toRegex(), "_")

                            val assignmentData = hashMapOf(
                                "title" to title.trim(),
                                "dueDate" to dueDate,
                                "createdBy" to user?.uid,
                                "createdAt" to System.currentTimeMillis()
                            )

                            db.collection("classes")
                                .document(classCode)
                                .collection("assignments")
                                .document(sanitizedTitle) // use sanitized title as ID
                                .set(assignmentData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Assignment added!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .addOnFailureListener { e ->
                                    errorMessage = "Failed to add assignment: ${e.message}"
                                }

                        } catch (e: Exception) {
                            errorMessage = "Unexpected error: ${e.message}"
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
