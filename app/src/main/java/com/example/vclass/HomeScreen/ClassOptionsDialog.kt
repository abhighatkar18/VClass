

package com.example.vclass.HomeScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await



@Composable
fun ClassOptionsDialog(
    classCode: String,
    className: String,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit,
    onDeleteClass: () -> Unit,
    onManageMembers: () -> Unit,
    onPostAnnouncement: () -> Unit,
    onUploadResource: () -> Unit,
    onAddAssignment: () -> Unit,
    onLeaveClass: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var role by remember { mutableStateOf("loading") }

    // Determine the role
    LaunchedEffect(classCode, currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        try {
            val classDoc = db.collection("classes").document(classCode).get().await()
            val creatorId = classDoc.getString("creatorId") ?: ""
            val teachers = classDoc.memberIds("teachers")

            role = when {
                classDoc.getTimestamp("archivedAt") != null -> "unavailable"
                uid == creatorId -> "creator"
                uid in teachers -> "teacher"
                uid in classDoc.memberIds("members") -> "member"
                else -> "unavailable"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            role = "unavailable"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Class Options: $className") },
        text = {
            Column {
                Button(enabled = role in listOf("creator", "teacher", "member"), onClick = onViewDetails, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                    Text("View Class Details")
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (role) {
                    "creator", "teacher" -> {
                        Button(onClick = onPostAnnouncement, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("Post Announcement")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = onUploadResource, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("Upload Resources")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = onAddAssignment, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("Add Assignment")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = onManageMembers, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {

                            Text("Manage Members")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (role == "creator") {
                            Button(onClick = onDeleteClass, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                                Text("Archive Class")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    "member" -> {
                        Button(onClick = onManageMembers, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("View Members")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = onLeaveClass, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                            Text("Leave Class")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    "unavailable" -> Text("Class options are unavailable. Reopen the dialog to retry.")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                Text("Close")
            }
        }
    )
}




