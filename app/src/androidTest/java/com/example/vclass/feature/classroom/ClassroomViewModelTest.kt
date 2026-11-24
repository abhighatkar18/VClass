package com.example.vclass.feature.classroom

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vclass.core.ui.ContentState
import com.example.vclass.domain.Classroom
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassroomViewModelTest {
    @Test
    fun retriesAClassroomLoadAfterFailure() = runBlocking {
        val expected = listOf(Classroom("ab1234", "Math"))
        var attempts = 0
        val model = ClassroomViewModel {
            if (attempts++ == 0) throw IllegalStateException("Synthetic load failure")
            expected
        }
        val states = Channel<ContentState<List<Classroom>>>(Channel.UNLIMITED)
        val collect = launch { model.state.collect { states.send(it) } }
        try {
            withTimeout(5_000) {
                assertEquals(ContentState.Loading, states.receive())
                assertEquals(ContentState.Failed, states.receive())
                model.reload()
                var state = states.receive()
                if (state == ContentState.Loading) state = states.receive()
                assertEquals(ContentState.Ready(expected), state)
            }
        } finally {
            collect.cancel()
        }
    }
}
