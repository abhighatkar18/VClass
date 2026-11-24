package com.example.vclass.HomeScreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

private data class ClassroomAssignment(
    val id: String,
    val title: String,
    val dueDate: Long?,
    val createdBy: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassViewScreen(
    classCode: String,
    className: String,
    onBack: () -> Unit,
    onShowOptions: () -> Unit,
    showClassOptions: Boolean,
    onDismissOptions: () -> Unit,
    onViewDetails: () -> Unit,
    onDeleteClass: () -> Unit,
    onManageMembers: () -> Unit,
    onPostAnnouncement: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid

    var announcements by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var resources by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var assignments by remember(classCode) { mutableStateOf<List<ClassroomAssignment>>(emptyList()) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var assignmentToSubmit by remember(classCode) { mutableStateOf<ClassroomAssignment?>(null) }

    // Firebase Listeners
    LaunchedEffect(classCode) {
        db.collection("classes").document(classCode).collection("announcements")
            .addSnapshotListener { snapshot, _ ->
                announcements = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            }
    }

    LaunchedEffect(classCode) {
        db.collection("classes").document(classCode).collection("resources")
            .addSnapshotListener { snapshot, _ ->
                resources = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            }
    }

    LaunchedEffect(classCode) {
        db.collection("classes").document(classCode).collection("assignments")
            .addSnapshotListener { snapshot, _ ->
                assignments = snapshot?.documents.orEmpty().map { document ->
                    val data = document.data
                    ClassroomAssignment(
                        id = document.id,
                        title = data?.get("title") as? String ?: "Untitled",
                        dueDate = data?.get("dueDate") as? Long,
                        createdBy = data?.get("createdBy") as? String
                    )
                }
            }
    }

    // Define the action handlers here
    val onUploadResource = { showUploadDialog = true }
    val onAddAssignment = { showAddAssignmentDialog = true }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = className, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = onShowOptions) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                }
            },
            modifier = Modifier.statusBarsPadding(),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
            item { Text("Announcements", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp)) }
            items(announcements) { announcement -> AnnouncementCard(announcement = announcement) }

            item { Text("Resources", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp)) }
            items(resources) { res ->
                val name = res["name"] as? String ?: "Unnamed"
                val uploadedBy = res["uploadedBy"] as? String ?: "Unknown"
                val uploadedAt = res["uploadedAt"] as? Long ?: 0L
                val cloudinaryLink = res["cloudinaryLink"] as? String ?: ""

                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Name: $name")
                        Text("Uploaded At: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(uploadedAt))}")
                        Text("Uploaded By: $uploadedBy")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cloudinaryLink))
                            context.startActivity(intent)
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("View File", color = Color.White)
                        }
                    }
                }
            }

            item { Text("Assignments", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp)) }
            items(assignments, key = { it.id }) { assignment ->
                val dateFormatted = assignment.dueDate?.let {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: "No Due Date"

                val isAssignmentCreator = currentUserId != null && assignment.createdBy == currentUserId

                var showSubmissionList by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Title: ${assignment.title}")
                        Text("Due: $dateFormatted")
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isAssignmentCreator) {
                            Button(onClick = {
                                showSubmissionList = true
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                                Text("View Submissions", color = Color.White)
                            }
                        } else {
                            Button(onClick = {
                                assignmentToSubmit = assignment
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                                Text("Submit Assignment", color = Color.White)
                            }
                        }
                    }
                }

                if (showSubmissionList) {
                    ViewSubmissionsDialog(
                        classCode = classCode,
                        assignmentId = assignment.id,
                        assignmentTitle = assignment.title,
                        onDismiss = { showSubmissionList = false }
                    )
                }
            }
        }

        assignmentToSubmit?.let { assignment ->
            UploadSubmissionDialog(
                classCode = classCode,
                assignmentId = assignment.id,
                assignmentTitle = assignment.title,
                onDismiss = { assignmentToSubmit = null }
            )
        }

        if (showAddAssignmentDialog) {
            AddAssignmentDialog(
                classCode = classCode,
                onDismiss = { showAddAssignmentDialog = false }
            )
        }

        if (showClassOptions) {
            ClassOptionsDialog(
                classCode = classCode,
                className = className,
                onDismiss = onDismissOptions,
                onViewDetails = onViewDetails,
                onDeleteClass = onDeleteClass,
                onManageMembers = onManageMembers,
                onPostAnnouncement = onPostAnnouncement,
                onUploadResource = onUploadResource,
                onAddAssignment = onAddAssignment,
                onLeaveClass = {
                    currentUserId?.let { uid ->
                        leaveClass(
                            classCode = classCode,
                            userId = uid,
                            onSuccess = {
                                onDismissOptions()
                                onBack()
                            },
                            onError = { error ->
                                println("Failed to leave class: $error")
                            }
                        )
                    }
                }
            )
        }

        if (showUploadDialog) {
            UploadResourceDialog(
                classCode = classCode,
                onDismiss = { showUploadDialog = false }
            )
        }
    }
}

data class SubmissionDisplay(
    val submittedByName: String,
    val fileName: String,
    val fileUrl: String
)

@Composable
fun ViewSubmissionsDialog(
    classCode: String,
    assignmentId: String,
    assignmentTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var submissions by remember { mutableStateOf<List<SubmissionDisplay>>(emptyList()) }

    LaunchedEffect(classCode, assignmentId) {
        db.collection("classes")
            .document(classCode)
            .collection("assignments")
            .document(assignmentId)
            .collection("submissions")
            .get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.documents
                val tempList = mutableListOf<SubmissionDisplay>()

                docs.forEach { doc ->
                    val studentId = doc.getString("studentId")
                    val fileUrl = doc.getString("fileUrl") ?: ""
                    val fileName = fileUrl.substringAfterLast("/") // Use file name from URL if not stored

                    if (studentId != null) {
                        db.collection("users")
                            .document(studentId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val fullName = userDoc.getString("fullName") ?: "Unknown"
                                tempList.add(SubmissionDisplay(fullName, fileName, fileUrl))
                                submissions = tempList.toList()
                            }
                            .addOnFailureListener {
                                tempList.add(SubmissionDisplay("Unknown", fileName, fileUrl))
                                submissions = tempList.toList()
                            }
                    } else {
                        tempList.add(SubmissionDisplay("Unknown", fileName, fileUrl))
                        submissions = tempList.toList()
                    }
                }
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text("Submissions for \"$assignmentTitle\"") },
        text = {
            if (submissions.isEmpty()) {
                Text("No submissions yet.")
            } else {
                LazyColumn {
                    items(submissions) { submission ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text("By: ${submission.submittedByName}")
                            Text("File: ${submission.fileName}")
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(submission.fileUrl))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("View File", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    )
}
fun leaveClass(classCode: String, userId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("users").document(userId)
    val classRef = db.collection("classes").document(classCode)

    db.runBatch { batch ->
        batch.update(userRef, "classes", com.google.firebase.firestore.FieldValue.arrayRemove(classCode))
        batch.update(classRef, "members", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
    }.addOnSuccessListener {
        onSuccess()
    }.addOnFailureListener { e ->
        onError(e.message ?: "Unknown error")
    }
}
