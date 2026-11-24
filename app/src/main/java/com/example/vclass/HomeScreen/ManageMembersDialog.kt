package com.example.vclass.HomeScreen

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
    val displayName: String
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
    var errorMessage by remember(classCode, viewerId) { mutableStateOf<String?>(null) }
    var updating by remember(classCode, viewerId) { mutableStateOf(false) }

    suspend fun refreshData() {
        canManageMembers = false
        errorMessage = null
        try {
            val classDoc = classRef.get().await()
            val creatorId = classDoc.getString("creatorId")
            val teacherIds = classDoc.memberIds("teachers")
            val memberIds = classDoc.memberIds("members")
            val isManager = !classDoc.metadata.isFromCache && !classDoc.metadata.hasPendingWrites() &&
                canManageClassMembers(viewerId, creatorId, teacherIds)
            if (viewerId == null || !classDoc.includesMember(viewerId) || classDoc.getTimestamp("archivedAt") != null) {
                errorMessage = "This roster is available to class members only."
                return
            }

            val names = classRef.rosterNames()
            fun member(id: String) = Member(id, names[id] ?: "Class member")

            val loadedCreator = creatorId?.takeIf { it.isNotBlank() }?.let { member(it) }
            val loadedTeachers = teacherIds.map { member(it) }
            val loadedStudents = memberIds
                .filter { it !in teacherIds && it != creatorId }
                .map { member(it) }

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
        if (!canManageMembers || updating) return
        updating = true
        coroutineScope.launch {
            try {
                val classDoc = classRef.get(Source.SERVER).await()
                if (auth.currentUser?.uid != viewerId ||
                    classDoc.getTimestamp("archivedAt") != null || classDoc.metadata.hasPendingWrites() ||
                    !canManageClassMembers(viewerId, classDoc.getString("creatorId"), classDoc.memberIds("teachers"))
                ) {
                    canManageMembers = false
                    errorMessage = "Your class permissions have changed."
                    return@launch
                }
                update()
                refreshData()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorMessage = "Could not update the class member. Please try again."
            } finally {
                updating = false
            }
        }
    }

    LaunchedEffect(classCode, viewerId) {
        refreshData()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (canManageMembers) "Manage Members" else "View Members", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Creator", style = MaterialTheme.typography.titleMedium)
                creator?.let {
                    MemberItem(member = it)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Teachers", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                    items(teachers) { teacher ->
                        MemberItem(
                            member = teacher,
                            showManagementActions = canManageMembers && !updating,
                            onRemove = {
                                updateMember {
                                    db.runBatch { batch ->
                                        batch.update(classRef, "teachers", FieldValue.arrayRemove(teacher.id))
                                        batch.delete(classRef.collection("roster").document(teacher.id))
                                    }.await()
                                }
                            },
                            onDemote = {
                                updateMember {
                                    classRef.update(
                                        "teachers", FieldValue.arrayRemove(teacher.id),
                                        "members", FieldValue.arrayUnion(teacher.id)
                                    ).await()
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
                            showManagementActions = canManageMembers && !updating,
                            onRemove = {
                                updateMember {
                                    db.runBatch { batch ->
                                        batch.update(classRef, "members", FieldValue.arrayRemove(student.id))
                                        batch.delete(classRef.collection("roster").document(student.id))
                                    }.await()
                                }
                            },
                            onPromote = {
                                updateMember {
                                    classRef.update(
                                        "teachers", FieldValue.arrayUnion(student.id),
                                        "members", FieldValue.arrayRemove(student.id)
                                    ).await()
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
private fun MemberItem(
    member: Member,
    showManagementActions: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onPromote: (() -> Unit)? = null,
    onDemote: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(member.displayName, style = MaterialTheme.typography.bodyLarge)
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
