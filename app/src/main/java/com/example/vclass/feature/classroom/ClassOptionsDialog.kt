

package com.example.vclass.feature.classroom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vclass.di.AppContainer
import com.example.vclass.domain.ClassRole
import kotlinx.coroutines.CancellationException



@Composable
fun ClassOptionsDialog(
    classCode: String,
    className: String,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit,
    onArchiveClass: () -> Unit,
    onManageMembers: () -> Unit,
    onPostAnnouncement: () -> Unit,
    onUploadResource: () -> Unit,
    onAddAssignment: () -> Unit,
    onLeaveClass: () -> Unit
) {
    val uid = AppContainer.auth.currentUserId
    var role by remember(classCode, uid) { mutableStateOf<ClassRole?>(null) }
    var unavailable by remember(classCode, uid) { mutableStateOf(false) }

    LaunchedEffect(classCode, uid) {
        if (uid == null) return@LaunchedEffect
        try {
            role = AppContainer.classrooms.role(classCode, uid)
            unavailable = role == null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            unavailable = true
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Class Options: $className") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Button(enabled = role != null, onClick = onViewDetails) {
                    Text("View Class Details")
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (role) {
                    ClassRole.CREATOR, ClassRole.TEACHER -> {
                        Button(onClick = onPostAnnouncement) {
                            Text("Post Announcement")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = onUploadResource) {
                            Text("Upload Resources")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = onAddAssignment) {
                            Text("Add Assignment")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = onManageMembers) {

                            Text("Manage Members")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (role == ClassRole.CREATOR) {
                            Button(onClick = onArchiveClass) {
                                Text("Archive Class")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ClassRole.STUDENT -> {
                        Button(onClick = onManageMembers) {
                            Text("View Members")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = onLeaveClass, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) {
                            Text("Leave Class")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    null -> if (unavailable) Text("Class options are unavailable. Reopen the dialog to retry.")
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
