package com.example.vclass.feature.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vclass.core.ui.theme.VclassTheme
import com.example.vclass.domain.Event
import java.util.Calendar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dateInLastColumnIsAccessibleOnNarrowLayoutAndShowsItsEvents() {
        val month = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 1, 9, 0) }
        val eventDate = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 7) }.time
        val selectedDate = mutableStateOf(month.time)
        compose.setContent {
            VclassTheme(darkTheme = true) {
                Box(Modifier.width(288.dp)) {
                    CalendarView(
                        events = listOf(Event("event-1", "Review session", eventDate.time)),
                        calendar = month,
                        selectedDate = selectedDate.value,
                        onMonthChange = {},
                        onDateSelected = { selectedDate.value = it }
                    )
                }
            }
        }
        val cell = compose.onNodeWithContentDescription("1 event", substring = true)
        cell.assertIsDisplayed().performClick()
        compose.onNodeWithText("Review session").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        cell.assertIsSelected()
    }
}
