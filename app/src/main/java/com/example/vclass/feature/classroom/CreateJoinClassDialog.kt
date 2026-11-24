package com.example.vclass.feature.classroom

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.vclass.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@Composable
fun CreateJoinClassDialog(
    onDismiss: () -> Unit,
    onClassCreated: () -> Unit
) {
    val context = LocalContext.current
    var className by remember { mutableStateOf("") }
    var classCode by remember { mutableStateOf("") }
    var isCreateMode by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Create or Join Class") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row {
                    Button(onClick = { isCreateMode = true }, modifier = Modifier.weight(1f)) {
                        Text("Create")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { isCreateMode = false }, modifier = Modifier.weight(1f)) {
                        Text("Join")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (isCreateMode) {
                    OutlinedTextField(singleLine = true, modifier = Modifier.fillMaxWidth(), value = className, onValueChange = { className = it }, label = { Text("Class Name") })
                } else {
                    OutlinedTextField(singleLine = true, modifier = Modifier.fillMaxWidth(), value = classCode, onValueChange = { classCode = it }, label = { Text("Class Code") })
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !isSaving, onClick = {
                if (isSaving || AppContainer.auth.currentUserId == null) return@Button
                val creating = isCreateMode
                val name = className.trim()
                val code = if (creating) UUID.randomUUID().toString().substring(0, 6)
                    else classCode.trim().lowercase(Locale.ROOT)
                if (creating && name.length !in 1..120) {
                    errorMessage = "Enter a class name between 1 and 120 characters."
                    return@Button
                }
                if (!creating && !code.matches(Regex("[0-9a-f]{6}"))) {
                    errorMessage = "Enter a valid six-character class code."
                    return@Button
                }
                isSaving = true
                errorMessage = null
                scope.launch {
                    try {
                        AppContainer.classrooms.createOrJoin(creating, name, code)
                        Toast.makeText(context, if (creating) "Class Created! Code: $code" else "Joined Class", Toast.LENGTH_LONG).show()
                        onClassCreated()
                        onDismiss()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        errorMessage = if (creating) "Could not create the class. Please try again."
                            else "Could not join the class. Check the code and your connection."
                    } finally {
                        isSaving = false
                    }
                }
            }) {
                Text(if (isSaving) "Saving…" else if (isCreateMode) "Create" else "Join")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
