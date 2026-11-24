package com.example.vclass.domain

import java.util.Date

internal data class Event(val id: String, val title: String, val timeMillis: Long) {
    val date: Date get() = Date(timeMillis)
}
