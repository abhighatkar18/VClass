package com.example.vclass.core.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal sealed interface ContentState<out T> {
    data object Loading : ContentState<Nothing>
    data object Failed : ContentState<Nothing>
    data class Ready<T>(val value: T) : ContentState<T>
}

internal fun <T> Flow<T>.asContentState(): Flow<ContentState<T>> =
    map<T, ContentState<T>> { ContentState.Ready(it) }
        .onStart { emit(ContentState.Loading) }
        .catch { failure ->
            if (failure is CancellationException) throw failure
            emit(ContentState.Failed)
        }

internal fun <T> ContentState<T>.valueOrNull(): T? = (this as? ContentState.Ready)?.value
