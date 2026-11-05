

package com.example.vclass.HomeScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    LaunchedEffect(classCode) {
        currentUser?.uid?.let { uid ->
            val classDoc = db.collection("classes").document(classCode).get().await()
            val creatorId = classDoc.getString("creatorId") ?: ""
            val teachers = classDoc.get("teachers") as? List<String> ?: emptyList()

            role = when {
                uid == creatorId -> "creator"
                uid in teachers -> "teacher"
                else -> "member"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Class Options: $className") },
        text = {
            Column {
                Button(onClick = onViewDetails, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
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

                        Button(onClick = onDeleteClass, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("Delete Class")
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




