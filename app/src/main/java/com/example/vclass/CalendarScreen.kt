package com.example.vclass
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.vclass.calendarscreen.CalendarView
import com.example.vclass.calendarscreen.Event
import com.example.vclass.calendarscreen.showDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val window: Window? = activity?.window
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    SideEffect {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            val controller = WindowInsetsControllerCompat(it, it.decorView)
            controller.isAppearanceLightStatusBars = false  // Makes icons white
            it.statusBarColor = android.graphics.Color.BLACK
        }
    }

    var selectedDate by remember { mutableStateOf(Date()) }
    var newEventText by remember { mutableStateOf("") }
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "test_user"
    val eventsRef = firestore.collection("users").document(userId).collection("events")

    // Fetch Events in Real-time
    LaunchedEffect(Unit) {
        eventsRef.addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                events = it.documents.map { doc ->
                    Event(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        date = doc.getDate("date") ?: Date()
                    )
                }
            }
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
                        onClick = { showDatePicker(context) { date -> selectedDate = date } },
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
                        onClick = {
                            if (newEventText.isNotBlank()) {
                                val newEvent = hashMapOf(
                                    "title" to newEventText,
                                    "date" to selectedDate
                                )
                                eventsRef.add(newEvent)
                                scheduleNotification(context, newEventText, selectedDate)
                                newEventText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Event", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    CalendarView(events, currentCalendar, onMonthChange = {
                        currentCalendar = it
                    }, onDateSelected = { selectedDate = it })
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

                items(events.size) { index ->
                    val event = events[index]
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
                            IconButton(onClick = { deleteEvent(event.id, eventsRef) }) {
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
fun deleteEvent(eventId: String, eventsRef: com.google.firebase.firestore.CollectionReference) {
    eventsRef.document(eventId).delete()
}
