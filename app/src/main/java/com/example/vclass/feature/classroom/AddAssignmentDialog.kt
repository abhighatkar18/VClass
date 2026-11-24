package com.example.vclass.feature.classroom

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vclass.di.AppContainer
import com.example.vclass.feature.calendar.showDatePicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.vclass.core.ui.rememberDateFormatter
import java.util.Date

@Composable
fun AddAssignmentDialog(
    classCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = rememberDateFormatter("dd MMM yyyy")
    val coroutineScope = rememberCoroutineScope()
    val uid = AppContainer.auth.currentUserId
    val repository = AppContainer.assignments

    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Add Assignment") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null // clear previous error
                    },
                    label = { Text("Assignment Title") },
                    singleLine = true,
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    enabled = !isUploading,
                    onClick = {
                        showDatePicker(context) { selectedDate ->
                            dueDate = selectedDate.time
                        }
                    }
                ) {
                    Text("Pick Due Date")
                }

                dueDate?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Selected Due Date: ${dateFormat.format(Date(it))}")
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
                enabled = !isUploading
            ) {
                Text(if (isUploading) "Adding..." else "Add Assignment")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Cancel")
            }
        }
    )
}
