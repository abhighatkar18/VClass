package com.example.vclass.HomeScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Member(
    val id: String,
    val data: Map<String, Any>
)

internal fun canManageClassMembers(viewerId: String?, creatorId: String?, teacherIds: List<String>): Boolean =
    !viewerId.isNullOrBlank() && (viewerId == creatorId || viewerId in teacherIds)

@Composable
fun ManageMembersDialog(
    classCode: String,
    onDismiss: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val viewerId = auth.currentUser?.uid
    val classRef = db.collection("classes").document(classCode)
    val coroutineScope = rememberCoroutineScope()

    var canManageMembers by remember(classCode, viewerId) { mutableStateOf(false) }
    var creator by remember(classCode, viewerId) { mutableStateOf<Member?>(null) }
    var teachers by remember(classCode, viewerId) { mutableStateOf<List<Member>>(emptyList()) }
    var students by remember(classCode, viewerId) { mutableStateOf<List<Member>>(emptyList()) }
    var selectedMember by remember(classCode, viewerId) { mutableStateOf<Member?>(null) }
    var errorMessage by remember(classCode, viewerId) { mutableStateOf<String?>(null) }

    suspend fun refreshData() {
        canManageMembers = false
        selectedMember = null
        errorMessage = null
        try {
            val classDoc = classRef.get().await()
            val creatorId = classDoc.getString("creatorId")
            val teacherIds = (classDoc.get("teachers") as? List<*>).orEmpty().filterIsInstance<String>()
            val memberIds = (classDoc.get("members") as? List<*>).orEmpty().filterIsInstance<String>()
            val isManager = !classDoc.metadata.isFromCache &&
                canManageClassMembers(viewerId, creatorId, teacherIds)
            if (viewerId == null || (viewerId != creatorId && viewerId !in teacherIds && viewerId !in memberIds)) {
                errorMessage = "This roster is available to class members only."
                return
            }

            val visibleFields = setOf("fullName", "name", "class", "rollNo")
            val allowedFields = if (isManager) visibleFields + setOf("email", "phoneNumber", "address") else visibleFields

            suspend fun readMember(id: String): Member {
                val profile = db.collection("users").document(id).get().await()
                return Member(id, profile.data.orEmpty().filterKeys { it in allowedFields })
            }

            val loadedCreator = creatorId?.takeIf { it.isNotBlank() }?.let { readMember(it) }
            val loadedTeachers = teacherIds.map { readMember(it) }
            val loadedStudents = memberIds
                .filter { it !in teacherIds && it != creatorId }
                .map { readMember(it) }

            creator = loadedCreator
            teachers = loadedTeachers
            students = loadedStudents
            canManageMembers = isManager
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            errorMessage = "Could not load the class roster."
        }
    }

    fun updateMember(update: suspend () -> Unit) {
        if (!canManageMembers) return
        coroutineScope.launch {
            try {
                val classDoc = classRef.get(Source.SERVER).await()
                val teacherIds = (classDoc.get("teachers") as? List<*>).orEmpty().filterIsInstance<String>()
                if (auth.currentUser?.uid != viewerId ||
                    !canManageClassMembers(viewerId, classDoc.getString("creatorId"), teacherIds)
                ) {
                    canManageMembers = false
                    selectedMember = null
                    errorMessage = "Your class permissions have changed."
                    return@launch
                }
                update()
                refreshData()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorMessage = "Could not update the class member. Please try again."
            }
        }
    }

    LaunchedEffect(classCode, viewerId) {
        refreshData()
    }

    if (selectedMember != null) {
        MemberDetailsDialog(
            member = selectedMember!!,
            showContactDetails = canManageMembers,
            onDismiss = { selectedMember = null }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (canManageMembers) "Manage Members" else "View Members", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Creator", style = MaterialTheme.typography.titleMedium)
                creator?.let {
                    MemberItem(
                        member = it,
                        onDetails = { selectedMember = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Teachers", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                    items(teachers) { teacher ->
                        MemberItem(
                            member = teacher,
                            onDetails = { selectedMember = it },
                            showManagementActions = canManageMembers,
                            onRemove = {
                                updateMember {
                                    classRef.update("teachers", FieldValue.arrayRemove(teacher.id)).await()
                                }
                            },
                            onDemote = {
                                updateMember {
                                    classRef.update("teachers", FieldValue.arrayRemove(teacher.id)).await()
                                    classRef.update("members", FieldValue.arrayUnion(teacher.id)).await()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Students", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(students) { student ->
                        MemberItem(
                            member = student,
                            onDetails = { selectedMember = it },
                            showManagementActions = canManageMembers,
                            onRemove = {
                                updateMember {
                                    classRef.update("members", FieldValue.arrayRemove(student.id)).await()
                                }
                            },
                            onPromote = {
                                updateMember {
                                    classRef.update("teachers", FieldValue.arrayUnion(student.id)).await()
                                    classRef.update("members", FieldValue.arrayRemove(student.id)).await()
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Close")
            }
        }
    )
}

@Composable
fun MemberItem(
    member: Member,
    onDetails: (Member) -> Unit,
    showManagementActions: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onPromote: (() -> Unit)? = null,
    onDemote: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onDetails(member) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(member.data["fullName"] as? String ?: "N/A", style = MaterialTheme.typography.bodyLarge)
                Text("Class: ${member.data["class"] as? String ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                Text("Roll No: ${member.data["rollNo"] as? String ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
            }
            if (showManagementActions) Row {
                onPromote?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Promote to Teacher")
                    }
                }
                onDemote?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.PersonRemove, contentDescription = "Demote to Student")
                    }
                }
                onRemove?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Close, contentDescription = "Remove from Class")
                    }
                }
            }
        }
    }
}

@Composable
fun MemberDetailsDialog(member: Member, showContactDetails: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Member Details") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val data = member.data
                Text("Full Name: ${data["fullName"] ?: "N/A"}")
                Text("Name: ${data["name"] ?: "N/A"}")
                Text("Class: ${data["class"] ?: "N/A"}")
                Text("Roll No: ${data["rollNo"] ?: "N/A"}")
                if (showContactDetails) {
                    Text("Email: ${data["email"] ?: "N/A"}")
                    Text("Phone: ${data["phoneNumber"] ?: "N/A"}")
                    Text("Address: ${data["address"] ?: "N/A"}")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
