package com.example.vclass.feature.classroom

import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vclass.core.ui.AppTopBar
import com.example.vclass.core.ui.ContentState
import com.example.vclass.core.ui.EmptyContent
import com.example.vclass.core.ui.rememberDateFormatter
import com.example.vclass.core.ui.valueOrNull
import com.example.vclass.di.AppContainer
import com.example.vclass.domain.ClassroomAssignment
import kotlinx.coroutines.launch
import java.util.*

@Composable
internal fun ClassDetailScreen(
    classCode: String,
    className: String,
    onBack: () -> Unit,
    onShowOptions: () -> Unit,
    showClassOptions: Boolean,
    onDismissOptions: () -> Unit,
    onViewDetails: () -> Unit,
    onArchiveClass: () -> Unit,
    onManageMembers: () -> Unit,
    onPostAnnouncement: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = rememberDateFormatter("dd MMM yyyy")
    val currentUserId = AppContainer.auth.currentUserId
    val repository = AppContainer.classrooms
    val owner = remember(classCode, currentUserId) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    val model: ClassDetailViewModel = viewModel(viewModelStoreOwner = owner, factory = viewModelFactory {
        initializer { ClassDetailViewModel(classCode, currentUserId, repository, AppContainer.assignments) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showUploadDialog by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var assignmentToSubmit by remember(classCode) { mutableStateOf<ClassroomAssignment?>(null) }
    var leaving by remember(classCode) { mutableStateOf(false) }
    // Define the action handlers here
    val onUploadResource = { showUploadDialog = true }
    val onAddAssignment = { showAddAssignmentDialog = true }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = className,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to classes")
                }
            },
            actions = {
                IconButton(onClick = onShowOptions) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Class options")
                }
            }
        )

        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
            if (state.loading) item { CircularProgressIndicator() }
            if (state.failed) item {
                Text("Some classroom content could not be loaded.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = model::reload) { Text("Retry") }
            }
            item { Text("Announcements", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp)) }
            if (state.announcements is ContentState.Ready && state.announcements.valueOrNull().orEmpty().isEmpty()) {
                item { EmptyContent("No announcements yet.") }
            }
            items(state.announcements.valueOrNull().orEmpty(), key = { "announcement:${it.id}" }) { announcement -> AnnouncementCard(announcement = announcement) }

            item { Text("Resources", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp)) }
            if (state.resources is ContentState.Ready && state.resources.valueOrNull().orEmpty().isEmpty()) {
                item { EmptyContent("No resources yet.") }
            }
            items(state.resources.valueOrNull().orEmpty(), key = { "resource:${it.id}" }) { res ->
                val name = res.name
                val uploadedBy = res.uploadedBy
                val uploadedAt = res.uploadedAt
                val cloudinaryLink = res.link
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Name: $name")
                        Text("Uploaded At: ${dateFormat.format(Date(uploadedAt))}")
                        Text("Uploaded By: $uploadedBy")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            openClassFile(context, cloudinaryLink)
                        }) {
                            Text("View File")
                        }
                    }
                }
            }

            item { Text("Assignments", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp)) }
            if (state.assignments is ContentState.Ready && state.assignments.valueOrNull().orEmpty().isEmpty()) {
                item { EmptyContent("No assignments yet.") }
            }
            items(state.assignments.valueOrNull().orEmpty(), key = { "assignment:${it.id}" }) { assignment ->
                val dateFormatted = assignment.dueDate?.let {
                    dateFormat.format(Date(it))
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
                            }) {
                                Text("View Submissions")
                            }
                        } else if (currentUserId != null) {
                            Button(onClick = {
                                assignmentToSubmit = assignment
                            }) {
                                Text("Submit Assignment")
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
                onArchiveClass = onArchiveClass,
                onManageMembers = onManageMembers,
                onPostAnnouncement = onPostAnnouncement,
                onUploadResource = onUploadResource,
                onAddAssignment = onAddAssignment,
                onLeaveClass = leave@{
                    val uid = currentUserId ?: return@leave
                    if (leaving) return@leave
                    leaving = true
                    scope.launch {
                        try {
                            repository.leave(classCode, uid)
                            onDismissOptions()
                            onBack()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not leave the class.", Toast.LENGTH_SHORT).show()
                        } finally {
                            leaving = false
                        }
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

internal fun openClassFile(context: Context, link: String) {
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
