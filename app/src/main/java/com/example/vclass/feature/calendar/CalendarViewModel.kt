package com.example.vclass.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vclass.core.ui.ContentState
import com.example.vclass.core.ui.asContentState
import com.example.vclass.data.firebase.CalendarRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

internal data class EventForm(
    val title: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val requestPermission: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class CalendarViewModel(private val uid: String, private val repository: CalendarRepository) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    val events = refresh.flatMapLatest { repository.observe(uid).asContentState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ContentState.Loading)

    private val mutableForm = MutableStateFlow(EventForm())
    val form = mutableForm.asStateFlow()

    fun reload() = refresh.update { it + 1 }
    fun changeTitle(title: String) = mutableForm.update { it.copy(title = title) }

    fun add(date: Date) {
        val draft = mutableForm.value
        if (draft.saving) return
        val title = draft.title.trim()
        if (title.length !in 1..120) {
            mutableForm.update { it.copy(error = "Enter an event name between 1 and 120 characters.") }
            return
        }
        mutableForm.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.save(uid, title, date)
                mutableForm.update {
                    it.copy(title = if (it.title == draft.title) "" else it.title, requestPermission = date.after(Date()))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableForm.update { it.copy(error = "Could not save the event. Please try again.") }
            } finally {
                mutableForm.update { it.copy(saving = false) }
            }
        }
    }

    fun delete(id: String) {
        mutableForm.update { it.copy(error = null) }
        viewModelScope.launch {
            try {
                repository.delete(uid, id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableForm.update { it.copy(error = "Could not delete the event.") }
            }
        }
    }

    fun consumePermissionRequest() = mutableForm.update { it.copy(requestPermission = false) }
}
