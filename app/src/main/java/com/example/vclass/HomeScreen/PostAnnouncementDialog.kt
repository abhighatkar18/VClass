package com.example.vclass.HomeScreen

import com.example.vclass.calendarscreen.showDatePicker
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PostAnnouncementDialog(
    classCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val announcementsRef = firestore.collection("classes").document(classCode).collection("announcements")

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var saveAsEvent by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Date()) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Announcement") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))



            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val data = hashMapOf(
                        "title" to title,
                        "description" to description,
                        "posterId" to userId,
                        "timestamp" to Date()
                    )
                    announcementsRef.add(data).addOnSuccessListener { docRef ->
                        if (saveAsEvent) {
                            val event = hashMapOf(
                                "title" to title,
                                "date" to selectedDate
                            )
                            firestore.collection("users").document(userId)
                                .collection("events")
                                .add(event)
                        }
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AnnouncementCard(announcement: Map<String, Any>) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return

    var showDialog by remember { mutableStateOf(false) }
    var eventTitle by remember { mutableStateOf(announcement["title"].toString()) }
    var selectedDate by remember { mutableStateOf(Date()) }

    val timestamp = (announcement["timestamp"] as? com.google.firebase.Timestamp)?.toDate()
    val formattedDate = timestamp?.let {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
    } ?: "Unknown date"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Title: ${announcement["title"]}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Description: ${announcement["description"]}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Posted on: $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.Start),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Save as Event")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Save Announcement as Event") },
            text = {
                Column {
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Event Title") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        showDatePicker(context) { selectedDate = it }
                    }) {
                        Text("Select Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate)}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val event = hashMapOf(
                        "title" to eventTitle,
                        "date" to selectedDate
                    )
                    firestore.collection("users").document(userId).collection("events").add(event)
                    showDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
