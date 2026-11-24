package com.example.vclass.HomeScreen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.vclass.displayName
import com.example.vclass.ensureUserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

@Composable
fun CreateJoinClassDialog(
    onDismiss: () -> Unit,
    db: FirebaseFirestore,
    user: FirebaseUser?,
    context: android.content.Context,
    onClassCreated: () -> Unit
) {
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
            Column {
                Row {
                    Button(onClick = { isCreateMode = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                        Text("Create", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { isCreateMode = false }, modifier = Modifier.weight(1f),colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                        Text("Join",color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (isCreateMode) {
                    TextField(value = className, onValueChange = { className = it }, label = { Text("Class Name") })
                } else {
                    TextField(value = classCode, onValueChange = { classCode = it }, label = { Text("Class Code") })
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !isSaving, colors = ButtonDefaults.buttonColors(containerColor = Color.Black), onClick = {
                if (isSaving || user == null) return@Button
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
                        ensureUserProfile(db, user)
                        val userRef = db.collection("users").document(user.uid)
                        val classRef = db.collection("classes").document(code)
                        val rosterName = userRef.get().await().displayName()?.take(120)
                        val rosterRef = classRef.collection("roster").document(user.uid)
                        if (creating) {
                            db.runTransaction { transaction ->
                                check(!transaction.get(classRef).exists())
                                transaction.set(classRef, mapOf("name" to name, "members" to listOf(user.uid), "creatorId" to user.uid))
                                transaction.update(userRef, "classes", FieldValue.arrayUnion(code))
                                rosterName?.let { transaction.set(rosterRef, mapOf("displayName" to it)) }
                            }.await()
                        } else {
                            db.runBatch { batch ->
                                batch.update(classRef, "members", FieldValue.arrayUnion(user.uid))
                                batch.update(userRef, "classes", FieldValue.arrayUnion(code))
                                rosterName?.let { batch.set(rosterRef, mapOf("displayName" to it)) }
                            }.await()
                        }
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
                Text(if (isCreateMode) "Create" else "Join", color = Color.White)
            }
        },
        dismissButton = {
            Button(enabled = !isSaving, onClick = onDismiss,colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                Text("Cancel")
            }
        }
    )
}
