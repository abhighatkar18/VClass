package com.example.vclass.feature.classroom

import com.example.vclass.feature.calendar.showDatePicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vclass.feature.calendar.calendarDay
import com.example.vclass.feature.calendar.rememberReminderPermissionRequest
import com.example.vclass.di.AppContainer
import com.example.vclass.domain.ClassAnnouncement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.vclass.core.ui.rememberDateFormatter
import java.util.*

@Composable
fun PostAnnouncementDialog(
    classCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userId = AppContainer.auth.currentUserId ?: return

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Post Announcement") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

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
                enabled = !saving,
                onClick = {
                    if (saving) return@Button
                    val subject = title.trim()
                    val body = description.trim()
                    if (subject.length !in 1..120 || body.length !in 1..5000) {
                        errorMessage = "Enter a title and description (up to 120 and 5,000 characters)."
                        return@Button
                    }
                    saving = true
                    errorMessage = null
                    scope.launch {
                        try {
                            AppContainer.classrooms.postAnnouncement(classCode, subject, body, userId)
                            onDismiss()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            errorMessage = "Could not post the announcement. Please try again."
                        } finally {
                            saving = false
                        }
                    }
                }
            ) {
                Text(if (saving) "Posting…" else "Post")
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun AnnouncementCard(announcement: ClassAnnouncement) {
    val context = LocalContext.current
    val userId = AppContainer.auth.currentUserId ?: return

    var showDialog by remember { mutableStateOf(false) }
    var eventTitle by remember { mutableStateOf(announcement.title) }
    var selectedDate by remember { mutableStateOf(calendarDay(Date(), hour = 9)) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val requestPermission = rememberReminderPermissionRequest()

    val dateFormat = rememberDateFormatter("dd MMM yyyy, hh:mm a")
    val eventDateFormat = rememberDateFormatter("dd MMM yyyy")
    val formattedDate = remember(announcement.postedAt, dateFormat) {
        announcement.postedAt?.let { dateFormat.format(Date(it)) } ?: "Unknown date"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Title: ${announcement.title}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Description: ${announcement.description}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Posted on: $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("Save as Event")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { if (!saving) showDialog = false },
            title = { Text("Save Announcement as Event") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Event Title") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        showDatePicker(context, selectedDate) { selectedDate = calendarDay(it, hour = 9) }
                    }) {
                        Text("Select Date: ${eventDateFormat.format(selectedDate)}")
                    }
                    Text("Future events are scheduled for 9:00 AM.", style = MaterialTheme.typography.bodySmall)
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(enabled = !saving, onClick = {
                    if (saving) return@Button
                    val title = eventTitle.trim()
                    val date = selectedDate
                    if (title.length !in 1..120) {
                        errorMessage = "Enter an event name between 1 and 120 characters."
                        return@Button
                    }
                    saving = true
                    errorMessage = null
                    scope.launch {
                        try {
                            AppContainer.calendar(context).save(userId, title, date)
                            if (date.after(Date())) requestPermission()
                            showDialog = false
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            errorMessage = "Could not save the event. Please try again."
                        } finally {
                            saving = false
                        }
                    }
                }) {
                    Text(if (saving) "Saving…" else "Save")
                }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showDialog = false; errorMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
