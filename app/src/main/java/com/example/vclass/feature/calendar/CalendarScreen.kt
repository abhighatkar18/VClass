package com.example.vclass.feature.calendar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vclass.core.ui.ContentState
import com.example.vclass.core.ui.valueOrNull
import com.example.vclass.di.AppContainer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var selectedDate by remember { mutableStateOf(calendarDay(Date(), hour = 9)) }
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }
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
    val (upcoming, past) = events.partition { it.date >= calendarDay(Date()) }

    LaunchedEffect(form.requestPermission) {
        if (form.requestPermission) {
            model.consumePermissionRequest()
            requestPermission()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar Events", color = Color.White, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
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
                    // Date Picker Button
                    Button(
                        onClick = { showDatePicker(context, selectedDate) { date -> selectedDate = calendarDay(date, hour = 9) } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar Icon", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dateFormat.format(selectedDate), color = Color.White, fontSize = 16.sp)
                    }
                }

                item {
                    // Event Input
                    TextField(
                        value = form.title,
                        onValueChange = model::changeTitle,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter event name", color = Color.Gray) }
                    )
                }

                item {
                    // Add Event Button
                    Button(
                        enabled = !form.saving,
                        onClick = { model.add(selectedDate) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (form.saving) "Saving..." else "Add Event", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    CalendarView(events, currentCalendar, onMonthChange = {
                        currentCalendar = it
                    }, onDateSelected = { selectedDate = calendarDay(it, hour = 9) })
                }

                item {
                    // Event List Header
                    Text(
                        text = "Upcoming Events",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                if (upcoming.isEmpty() && !loading && !readError) item { Text("No upcoming events.") }
                items(upcoming + past, key = { it.id }) { event ->
                    if (past.firstOrNull()?.id == event.id) {
                        Text("Past Events", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = event.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "📅 ${dateFormat.format(event.date)}",
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                            }

                            // Delete Button
                            IconButton(onClick = { model.delete(event.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete Event",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
