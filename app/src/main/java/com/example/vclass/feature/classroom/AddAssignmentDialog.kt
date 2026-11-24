package com.example.vclass.feature.classroom

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vclass.di.AppContainer
import com.example.vclass.feature.calendar.showDatePicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddAssignmentDialog(
    classCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uid = AppContainer.auth.currentUserId
    val repository = AppContainer.assignments

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
                    if (isUploading || uid == null) return@Button
                    if (title.trim().length !in 1..120) {
                        errorMessage = "Enter a title between 1 and 120 characters."
                        return@Button
                    }

                    isUploading = true
                    errorMessage = null
                    val assignmentTitle = title.trim()
                    val assignmentDueDate = dueDate
                    coroutineScope.launch {
                        try {
                            repository.addAssignment(classCode, assignmentTitle, assignmentDueDate)
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
