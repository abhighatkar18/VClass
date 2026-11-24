package com.example.vclass.feature.classroom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vclass.core.ui.EmptyContent
import com.example.vclass.di.AppContainer
import com.example.vclass.domain.Submission
import kotlinx.coroutines.CancellationException

@Composable
internal fun ViewSubmissionsDialog(
    classCode: String,
    assignmentId: String,
    assignmentTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repository = AppContainer.assignments
    var submissions by remember(classCode, assignmentId) { mutableStateOf<List<Submission>>(emptyList()) }
    var loading by remember(classCode, assignmentId) { mutableStateOf(true) }
    var loadError by remember(classCode, assignmentId) { mutableStateOf<String?>(null) }

    var retry by remember(classCode, assignmentId) { mutableIntStateOf(0) }
    LaunchedEffect(classCode, assignmentId, retry) {
        loading = true
        loadError = null
        try {
            val result = repository.submissions(classCode, assignmentId)
            if (result == null) {
                loadError = "This assignment is no longer available."
            } else {
                submissions = result
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IllegalArgumentException) {
            loadError = "This assignment reference is invalid."
        } catch (_: Exception) {
            loadError = "Could not load submissions."
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
                Column {
                    Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) { Text("Retry") }
                }
            } else if (submissions.isEmpty()) {
                EmptyContent("No submissions yet.")
            } else {
                LazyColumn {
                    items(submissions, key = { it.id }) { submission ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text("By: ${submission.submittedByName}")
                            Text("File: ${submission.fileName}")
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    openClassFile(context, submission.fileUrl)
                                }
                            ) {
                                Text("View File")
                            }
                        }
                    }
                }
            }
        }
    )
}
