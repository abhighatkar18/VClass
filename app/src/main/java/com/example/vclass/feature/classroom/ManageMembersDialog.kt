package com.example.vclass.feature.classroom

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
import com.example.vclass.di.AppContainer
import com.example.vclass.data.firebase.ClassroomRepository.MemberAction
import com.example.vclass.domain.ClassMember
import com.example.vclass.domain.ClassroomRoster
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ManageMembersDialog(
    classCode: String,
    onDismiss: () -> Unit
) {
    val repository = AppContainer.classrooms
    val viewerId = AppContainer.auth.currentUserId
    val scope = rememberCoroutineScope()
    var roster by remember(classCode, viewerId) { mutableStateOf<ClassroomRoster?>(null) }
    var errorMessage by remember(classCode, viewerId) { mutableStateOf<String?>(null) }
    var updating by remember(classCode, viewerId) { mutableStateOf(false) }

    suspend fun refreshData() {
        errorMessage = null
        try {
            roster = viewerId?.let { repository.roster(classCode, it) }
            if (roster == null) errorMessage = "This roster is available to class members only."
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            roster = null
            errorMessage = "Could not load the class roster."
        }
    }

    fun updateMember(member: ClassMember, action: MemberAction) {
        if (viewerId == null || roster?.canManage != true || updating) return
        updating = true
        scope.launch {
            try {
                if (repository.changeMember(classCode, viewerId, member.id, action)) {
                    refreshData()
                } else {
                    roster = roster?.copy(canManage = false)
                    errorMessage = "Your class permissions have changed."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorMessage = "Could not update the class member. Please try again."
            } finally {
                updating = false
            }
        }
    }

    val canManageMembers = roster?.canManage == true
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
                roster?.creator?.let {
                    MemberItem(member = it)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Teachers", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                    items(roster?.teachers.orEmpty()) { teacher ->
                        MemberItem(
                            member = teacher,
                            showManagementActions = canManageMembers && !updating,
                            onRemove = { updateMember(teacher, MemberAction.REMOVE_TEACHER) },
                            onDemote = { updateMember(teacher, MemberAction.DEMOTE) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Students", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(roster?.students.orEmpty()) { student ->
                        MemberItem(
                            member = student,
                            showManagementActions = canManageMembers && !updating,
                            onRemove = { updateMember(student, MemberAction.REMOVE_STUDENT) },
                            onPromote = { updateMember(student, MemberAction.PROMOTE) }
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
    member: ClassMember,
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
