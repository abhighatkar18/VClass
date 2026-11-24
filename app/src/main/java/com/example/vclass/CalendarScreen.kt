package com.example.vclass
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
import com.example.vclass.calendarscreen.CalendarView
import com.example.vclass.calendarscreen.Event
import com.example.vclass.calendarscreen.calendarDay
import com.example.vclass.calendarscreen.saveCalendarEvent
import com.example.vclass.calendarscreen.showDatePicker
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var selectedDate by remember { mutableStateOf(calendarDay(Date(), hour = 9)) }
    var newEventText by remember { mutableStateOf("") }
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val eventsRef = firestore.collection("users").document(userId).collection("events")
    val scope = rememberCoroutineScope()
    val requestPermission = rememberReminderPermissionRequest()
    var readError by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var operationError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    val today = calendarDay(Date())
    val (upcoming, past) = events.partition { it.date >= today }

    DisposableEffect(userId, retry) {
        val registration = eventsRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            loading = false
            readError = error != null
            snapshot?.let {
                events = it.documents.mapNotNull { doc ->
                    val date = when (val value = doc.get("date")) {
                        is Timestamp -> value.toDate()
                        is Date -> value
                        else -> null
                    }
                    date?.let {
                        if (!doc.metadata.hasPendingWrites()) scheduleNotification(context, userId, doc.id, date)
                        Event(doc.id, (doc.get("title") as? String).orEmpty(), date)
                    }
                }.sortedBy { event -> event.date }
            }
        }
        onDispose { registration.remove() }
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
                        value = newEventText,
                        onValueChange = { newEventText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter event name", color = Color.Gray) }
                    )
                }

                item {
                    // Add Event Button
                    Button(
                        enabled = !saving,
                        onClick = {
                            if (saving) return@Button
                            val title = newEventText.trim()
                            val date = selectedDate
                            if (title.length !in 1..120) {
                                operationError = "Enter an event name between 1 and 120 characters."
                                return@Button
                            }
                            saving = true
                            operationError = null
                            scope.launch {
                                try {
                                    saveCalendarEvent(context, firestore, userId, title, date)
                                    if (date.after(Date())) requestPermission()
                                    newEventText = ""
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    operationError = "Could not save the event. Please try again."
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (saving) "Saving..." else "Add Event", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Future events are scheduled for 9:00 AM.", style = MaterialTheme.typography.bodySmall)
                    operationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (loading) CircularProgressIndicator()
                    if (readError) {
                        Text("Could not load events.", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { loading = true; readError = false; retry++ }) { Text("Retry") }
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
                            IconButton(onClick = {
                                operationError = null
                                scope.launch {
                                    try {
                                        eventsRef.document(event.id).delete().await()
                                        cancelNotification(context, userId, event.id)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        operationError = "Could not delete the event."
                                    }
                                }
                            }) {
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
