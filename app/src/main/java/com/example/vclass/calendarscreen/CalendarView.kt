package com.example.vclass.calendarscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarView(events: List<Event>, calendar: Calendar, onMonthChange: (Calendar) -> Unit, onDateSelected: (Date) -> Unit) {
    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startOfMonth = calendar.clone() as Calendar
    startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
    val dayOfWeekStart = startOfMonth.get(Calendar.DAY_OF_WEEK) - 1

    val eventsByDay = remember(events, calendar.timeZone) {
        events.groupBy { calendarDay(it.date, zone = calendar.timeZone) }
    }
    var selectedEvents by remember { mutableStateOf<List<Event>>(emptyList()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newCalendar = calendar.clone() as Calendar
                newCalendar.add(Calendar.MONTH, -1)
                onMonthChange(newCalendar)
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Previous Month")
            }
            Text(text = dateFormat.format(calendar.time), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                val newCalendar = calendar.clone() as Calendar
                newCalendar.add(Calendar.MONTH, 1)
                onMonthChange(newCalendar)
            }) {
                Icon(Icons.Filled.ArrowForward, contentDescription = "Next Month")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Days of the week
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(text = day, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        Column {
            repeat(6) { rowIndex ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    repeat(7) { columnIndex ->
                        val day = rowIndex * 7 + columnIndex - dayOfWeekStart + 1
                        if (day in 1..daysInMonth) {
                            val dayDate = calendar.clone() as Calendar
                            dayDate.set(Calendar.DAY_OF_MONTH, day)
                            val dayEvents = eventsByDay[calendarDay(dayDate.time, zone = calendar.timeZone)].orEmpty()

                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .background(if (dayEvents.isNotEmpty()) Color.Cyan else Color.Transparent, CircleShape)
                                    .clickable {
                                        onDateSelected(dayDate.time)
                                        selectedEvents = dayEvents
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = day.toString(), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.size(45.dp))
                        }
                    }
                }
            }
        }
    }

    if (selectedEvents.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { selectedEvents = emptyList() },
            title = { Text("Event Details") },
            text = { Text(selectedEvents.joinToString("\n") { it.title }) },
            confirmButton = {
                TextButton(onClick = { selectedEvents = emptyList() }) {
                    Text("OK")
                }
            }
        )
    }
}



//
//
//@Composable
//fun CalendarView(events: List<Event>, calendar: Calendar, onMonthChange: (Calendar) -> Unit, onDateSelected: (Date) -> Unit) {
//    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
//    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
//    val startOfMonth = calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1
//
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//            IconButton(onClick = {
//                val newCalendar = calendar.clone() as Calendar
//                newCalendar.add(Calendar.MONTH, -1)
//                onMonthChange(newCalendar)
//            }) {
//                Icon(Icons.Filled.ArrowBack, contentDescription = "Previous Month")
//            }
//            Text(text = dateFormat.format(calendar.time), fontSize = 18.sp, fontWeight = FontWeight.Bold)
//            IconButton(onClick = {
//                val newCalendar = calendar.clone() as Calendar
//                newCalendar.add(Calendar.MONTH, 1)
//                onMonthChange(newCalendar)
//            }) {
//                Icon(Icons.Filled.ArrowForward, contentDescription = "Next Month")
//            }
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//        Column {
//            repeat(6) { rowIndex ->
//                Row {
//                    repeat(7) { columnIndex ->
//                        val day = rowIndex * 7 + columnIndex - startOfMonth + 1
//                        if (day > 0 && day <= daysInMonth) {
//                            val dayDate = calendar.clone() as Calendar
//                            dayDate.set(Calendar.DAY_OF_MONTH, day)
//                            val eventExists = events.any { event ->
//                                val eventCal = Calendar.getInstance().apply { time = event.date }
//                                eventCal.get(Calendar.YEAR) == dayDate.get(Calendar.YEAR) &&
//                                        eventCal.get(Calendar.MONTH) == dayDate.get(Calendar.MONTH) &&
//                                        eventCal.get(Calendar.DAY_OF_MONTH) == dayDate.get(Calendar.DAY_OF_MONTH)
//                            }
//                            Box(
//                                modifier = Modifier
//                                    .size(40.dp)
//                                    .background(if (eventExists) Color.Cyan else Color.Transparent)
//                                    .clickable { onDateSelected(dayDate.time) },
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Text(text = day.toString())
//                            }
//                        } else {
//                            Spacer(modifier = Modifier.size(40.dp))
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
