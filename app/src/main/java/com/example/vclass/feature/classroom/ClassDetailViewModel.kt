package com.example.vclass.feature.classroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vclass.core.ui.ContentState
import com.example.vclass.core.ui.asContentState
import com.example.vclass.data.firebase.AssignmentRepository
import com.example.vclass.data.firebase.ClassroomRepository
import com.example.vclass.domain.ClassAnnouncement
import com.example.vclass.domain.ClassResource
import com.example.vclass.domain.ClassroomAssignment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ClassFeedState(
    val announcements: ContentState<List<ClassAnnouncement>> = ContentState.Loading,
    val resources: ContentState<List<ClassResource>> = ContentState.Loading,
    val assignments: ContentState<List<ClassroomAssignment>> = ContentState.Loading
) {
    val loading: Boolean get() = announcements == ContentState.Loading ||
        resources == ContentState.Loading || assignments == ContentState.Loading
    val failed: Boolean get() = announcements == ContentState.Failed ||
        resources == ContentState.Failed || assignments == ContentState.Failed
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class ClassDetailViewModel(
    code: String,
    uid: String?,
    classrooms: ClassroomRepository,
    assignments: AssignmentRepository
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    val state = refresh.flatMapLatest {
        combine(
            classrooms.announcements(code).asContentState(),
            classrooms.resources(code).asContentState(),
            assignments.assignments(code).asContentState(),
            ::ClassFeedState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ClassFeedState())

    init {
        if (uid != null) viewModelScope.launch {
            try {
                classrooms.publishRosterName(code, uid)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Class content stays usable when the optional roster name cannot be published.
            }
        }
    }

    fun reload() = refresh.update { it + 1 }
}
