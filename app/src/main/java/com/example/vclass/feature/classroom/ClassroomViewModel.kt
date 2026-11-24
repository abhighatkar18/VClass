package com.example.vclass.feature.classroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vclass.core.ui.ContentState
import com.example.vclass.core.ui.asContentState
import com.example.vclass.domain.Classroom
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
internal class ClassroomViewModel(private val listClasses: suspend () -> List<Classroom>) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    val state = refresh.flatMapLatest { flow { emit(listClasses()) }.asContentState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ContentState.Loading)

    fun reload() = refresh.update { it + 1 }
}
