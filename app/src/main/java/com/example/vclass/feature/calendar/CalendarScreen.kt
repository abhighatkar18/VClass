package com.example.vclass.feature.calendar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vclass.core.ui.AppTopBar
import com.example.vclass.core.ui.EmptyContent
import com.example.vclass.core.ui.rememberDateFormatter
import com.example.vclass.core.ui.ContentState
import com.example.vclass.core.ui.valueOrNull
import com.example.vclass.di.AppContainer
import java.util.*

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val dateFormat = rememberDateFormatter("dd MMM yyyy")
    var selectedDate by rememberSaveable { mutableStateOf(calendarDay(Date(), hour = 9)) }
    var currentCalendar by rememberSaveable { mutableStateOf(Calendar.getInstance()) }
    val uid = AppContainer.auth.currentUserId ?: return
    val model: CalendarViewModel = viewModel(key = "calendar:$uid", factory = viewModelFactory {
        initializer { CalendarViewModel(uid, AppContainer.calendar(context)) }
    })
    val eventState by model.events.collectAsStateWithLifecycle()
    val form by model.form.collectAsStateWithLifecycle()
    val events = eventState.valueOrNull().orEmpty()
    val loading = eventState == ContentState.Loading
    val readError = eventState == ContentState.Failed
    val requestPermission = rememberReminderPermissionRequest()
    val today = calendarDay(Date())
    val (upcoming, past) = remember(events, today) { events.partition { it.date >= today } }
    val chronologicalEvents = remember(upcoming, past) { upcoming + past }

    LaunchedEffect(form.requestPermission) {
        if (form.requestPermission) {
            model.consumePermissionRequest()
            requestPermission()
        }
    }
    Scaffold(
        topBar = { AppTopBar("Calendar Events") },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Button(
                        onClick = {
                            showDatePicker(context, selectedDate) { date ->
                                selectedDate = calendarDay(date, hour = 9)
                                currentCalendar = Calendar.getInstance().apply { time = date }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Event date: ${dateFormat.format(selectedDate)}")
                    }
                }

                item {
                    OutlinedTextField(
                        value = form.title,
                        onValueChange = model::changeTitle,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Event name") },
                        singleLine = true
                    )
                }

                item {
                    Button(
                        enabled = !form.saving,
                        onClick = { model.add(selectedDate) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (form.saving) "Saving…" else "Add Event")
                    }
                    Text("Future events are scheduled for 9:00 AM.", style = MaterialTheme.typography.bodySmall)
                    form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (loading) CircularProgressIndicator()
                    if (readError) {
                        Text("Could not load events.", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = model::reload) { Text("Retry") }
                    }
                }

                item {
                    CalendarView(events, currentCalendar, selectedDate, onMonthChange = {
                        currentCalendar = it
                    }, onDateSelected = { selectedDate = calendarDay(it, hour = 9) })
                }

                item {
                    Text("Upcoming Events", style = MaterialTheme.typography.titleLarge)
                }

                if (upcoming.isEmpty() && !loading && !readError) item { EmptyContent("No upcoming events.") }
                items(chronologicalEvents, key = { it.id }) { event ->
                    if (past.firstOrNull()?.id == event.id) {
                        Text("Past Events", style = MaterialTheme.typography.titleLarge)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(event.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    dateFormat.format(event.date),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { model.delete(event.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete ${event.title}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
