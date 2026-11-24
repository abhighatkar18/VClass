package com.example.vclass.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vclass.core.ui.AppSpacing
import com.example.vclass.core.ui.rememberDateFormatter
import com.example.vclass.domain.Event
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Date

@Composable
internal fun CalendarView(
    events: List<Event>,
    calendar: Calendar,
    selectedDate: Date,
    onMonthChange: (Calendar) -> Unit,
    onDateSelected: (Date) -> Unit
) {
    val monthFormat = rememberDateFormatter("MMMM yyyy", calendar.timeZone)
    val dayFormat = rememberDateFormatter("EEEE, d MMMM yyyy", calendar.timeZone)
    val zone = calendar.timeZone
    val locale = LocalConfiguration.current.locales[0]
    val weekdays = remember(locale) { DateFormatSymbols(locale).shortWeekdays.drop(1) }
    val cells = remember(calendar, dayFormat) {
        val day = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val offset = day.get(Calendar.DAY_OF_WEEK) - 1
        val days = day.getActualMaximum(Calendar.DAY_OF_MONTH)
        val weeks = (offset + days + 6) / 7
        List(weeks * 7) { position ->
            val number = position - offset + 1
            if (number in 1..days) {
                day.set(Calendar.DAY_OF_MONTH, number)
                val date = calendarDay(day.time, zone = zone)
                CalendarCell(number, date, dayFormat.format(date))
            } else null
        }.chunked(7)
    }
    val eventsByDay = remember(events, zone) { events.groupBy { calendarDay(it.date, zone = zone) } }
    val selected = remember(selectedDate, zone) { calendarDay(selectedDate, zone = zone) }
    var eventDialogDay by remember { mutableStateOf<Date?>(null) }
    val selectedEvents = eventsByDay[eventDialogDay].orEmpty()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange((calendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
            }
            Text(
                monthFormat.format(calendar.time),
                Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onMonthChange((calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { weekday ->
                Text(
                    weekday,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        val colors = MaterialTheme.colorScheme
        cells.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    if (cell == null) {
                        Spacer(Modifier.weight(1f).height(48.dp))
                    } else {
                        val dayEvents = eventsByDay[cell.date].orEmpty()
                        val isSelected = selected == cell.date
                        val label = when (dayEvents.size) {
                            0 -> cell.label
                            1 -> "${cell.label}, 1 event"
                            else -> "${cell.label}, ${dayEvents.size} events"
                        }
                        Box(
                            Modifier.weight(1f).heightIn(min = 48.dp)
                                .selectable(selected = isSelected, role = Role.Button) {
                                    onDateSelected(cell.date)
                                    eventDialogDay = cell.date.takeIf { dayEvents.isNotEmpty() }
                                }
                                .semantics { contentDescription = label },
                            contentAlignment = Alignment.Center
                        ) {
                            val color = when {
                                isSelected -> colors.primary
                                dayEvents.isNotEmpty() -> colors.secondaryContainer
                                else -> colors.surface
                            }
                            val textColor = when {
                                isSelected -> colors.onPrimary
                                dayEvents.isNotEmpty() -> colors.onSecondaryContainer
                                else -> colors.onSurface
                            }
                            Text(
                                cell.number.toString(),
                                Modifier.background(color, CircleShape).padding(AppSpacing.small),
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedEvents.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { eventDialogDay = null },
            title = { Text("Event Details") },
            text = {
                Text(selectedEvents.joinToString("\n") { it.title }, Modifier.verticalScroll(rememberScrollState()))
            },
            confirmButton = { TextButton(onClick = { eventDialogDay = null }) { Text("OK") } }
        )
    }
}

private data class CalendarCell(val number: Int, val date: Date, val label: String)
