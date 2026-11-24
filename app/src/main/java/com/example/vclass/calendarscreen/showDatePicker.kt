package com.example.vclass.calendarscreen

import android.app.DatePickerDialog
import android.content.Context
import java.util.Calendar
import java.util.Date

fun showDatePicker(context: Context, initialDate: Date = Date(), onDateSelected: (Date) -> Unit) {
    val calendar = Calendar.getInstance().apply { time = initialDate }
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            calendar.clear()
            calendar.set(year, month, day)
            onDateSelected(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePicker.show()
}
