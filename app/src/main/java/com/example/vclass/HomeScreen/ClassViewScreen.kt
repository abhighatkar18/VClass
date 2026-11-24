package com.example.vclass.HomeScreen

import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
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
    var announcementError by remember(classCode) { mutableStateOf(false) }
    var resourceError by remember(classCode) { mutableStateOf(false) }
    var assignmentError by remember(classCode) { mutableStateOf(false) }
    var loadedFeedCount by remember(classCode) { mutableStateOf(emptySet<String>()) }
    var retryFeeds by remember(classCode) { mutableIntStateOf(0) }
    val classRef = db.collection("classes").document(classCode)

    LaunchedEffect(classCode, currentUserId) {
        if (currentUserId != null) {
            try {
                val profile = db.collection("users").document(currentUserId).get().await()
                classRef.publishRosterName(currentUserId, profile)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // A missing roster entry has a generic label and can be retried on class entry.
            }
        }
    }

    DisposableEffect(classCode, retryFeeds) {
        val registration = classRef.collection("announcements")
            .addSnapshotListener { snapshot, error ->
                announcementError = error != null
                loadedFeedCount = loadedFeedCount + "announcements"
                announcements = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            }
        onDispose { registration.remove() }
    }

    DisposableEffect(classCode, retryFeeds) {
        val registration = classRef.collection("resources")
            .addSnapshotListener { snapshot, error ->
                resourceError = error != null
                loadedFeedCount = loadedFeedCount + "resources"
                resources = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            }
        onDispose { registration.remove() }
    }

    DisposableEffect(classCode, retryFeeds) {
        val registration = classRef.collection("assignments")
            .addSnapshotListener { snapshot, error ->
                assignmentError = error != null
                loadedFeedCount = loadedFeedCount + "assignments"
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
        onDispose { registration.remove() }
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
            if (loadedFeedCount.size < 3) item { CircularProgressIndicator() }
            if (announcementError || resourceError || assignmentError) item {
                Text("Some classroom content could not be loaded.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = {
                    loadedFeedCount = emptySet()
                    announcementError = false
                    resourceError = false
                    assignmentError = false
                    retryFeeds++
                }) { Text("Retry") }
            }
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
                            openClassFile(context, cloudinaryLink)
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
                        } else if (currentUserId != null) {
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
                            onError = {
                                Toast.makeText(context, "Could not leave the class.", Toast.LENGTH_SHORT).show()
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
    var submissions by remember(classCode, assignmentId) { mutableStateOf<List<SubmissionDisplay>>(emptyList()) }
    var loading by remember(classCode, assignmentId) { mutableStateOf(true) }
    var loadError by remember(classCode, assignmentId) { mutableStateOf<String?>(null) }

    LaunchedEffect(classCode, assignmentId) {
        try {
            val assignment = db.assignmentDocument(classCode, assignmentId)
            if (!assignment.get().await().exists()) {
                loadError = "This assignment is no longer available."
                return@LaunchedEffect
            }
            val docs = assignment.collection("submissions").get().await().documents
            val names = try {
                db.collection("classes").document(classCode).rosterNames()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyMap()
            }
            submissions = docs.map { doc ->
                val fileUrl = doc.getString("fileUrl").orEmpty()
                SubmissionDisplay(
                    doc.getString("studentId")?.let(names::get) ?: doc.getString("studentName") ?: "Class member",
                    doc.getString("fileName") ?: Uri.parse(fileUrl).lastPathSegment.orEmpty(),
                    fileUrl
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IllegalArgumentException) {
            loadError = "This assignment reference is invalid."
        } catch (_: Exception) {
            loadError = "Could not load submissions. Reopen the assignment to retry."
        } finally {
            loading = false
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
            if (loading) {
                CircularProgressIndicator()
            } else if (loadError != null) {
                Text(loadError.orEmpty())
            } else if (submissions.isEmpty()) {
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
                                    openClassFile(context, submission.fileUrl)
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

private fun openClassFile(context: Context, link: String) {
    val uri = Uri.parse(link)
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) {
        Toast.makeText(context, "This file link is unavailable.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this file link.", Toast.LENGTH_SHORT).show()
    }
}

fun leaveClass(classCode: String, userId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("users").document(userId)
    val classRef = db.collection("classes").document(classCode)

    db.runBatch { batch ->
        batch.update(userRef, "classes", com.google.firebase.firestore.FieldValue.arrayRemove(classCode))
        batch.update(classRef, "members", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
        batch.delete(classRef.collection("roster").document(userId))
    }.addOnSuccessListener {
        onSuccess()
    }.addOnFailureListener { e ->
        onError(e.message ?: "Unknown error")
    }
}
