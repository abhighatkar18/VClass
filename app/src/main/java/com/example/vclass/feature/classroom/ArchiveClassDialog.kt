package com.example.vclass.feature.classroom

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.vclass.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


@Composable
fun ArchiveClassDialog(classCode: String, onDismiss: () -> Unit, onArchived: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Archive Class") },
        text = { Text(errorMessage ?: "This removes the class from active lists and keeps its existing records.") },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                if (saving || AppContainer.auth.currentUserId == null) return@Button
                saving = true
                scope.launch {
                    try {
                        AppContainer.classrooms.archive(classCode)
                        Toast.makeText(context, "Class archived", Toast.LENGTH_SHORT).show()
                        onArchived()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        errorMessage = "Could not archive this class. Only its creator can archive it."
                    } finally {
                        saving = false
                    }
                }
            }) {
                Text("Archive")
            }
        },
        dismissButton = {
            Button(enabled = !saving, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
