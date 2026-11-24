package com.example.vclass.HomeScreen

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@Composable
fun DeleteClass(classCode: String, onDismiss: () -> Unit, onClassDeleted: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
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
                if (saving || user == null) return@Button
                saving = true
                scope.launch {
                    try {
                        val classRef = db.collection("classes").document(classCode)
                        db.runTransaction { transaction ->
                            val classroom = transaction.get(classRef)
                            check(classroom.getString("creatorId") == user.uid)
                            if (classroom.getTimestamp("archivedAt") == null) {
                                transaction.update(classRef, "archivedAt", FieldValue.serverTimestamp())
                            }
                        }.await()
                        Toast.makeText(context, "Class archived", Toast.LENGTH_SHORT).show()
                        onClassDeleted()
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
