package com.example.vclass.data.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vclass.domain.Classroom
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassroomIndexTest {
    @Test
    fun readsFourAtATimeAndPreservesOrderWithoutDuplicates() = runBlocking {
        withTimeout(5_000L) {
            val started = Channel<String>(Channel.UNLIMITED)
            val releaseFirst = CompletableDeferred<Unit>()
            val result = async {
                loadClassroomIndex(listOf("a", "b", "a", "c", "stale", "d")) { code ->
                    started.send(code)
                    if (code == "a") releaseFirst.await()
                    if (code == "stale") {
                        throw FirebaseFirestoreException("Denied", FirebaseFirestoreException.Code.PERMISSION_DENIED)
                    }
                    Classroom(code, code)
                }
            }

            val firstBatch = List(4) { started.receive() }
            assertEquals(setOf("a", "b", "c", "stale"), firstBatch.toSet())
            assertFalse(started.tryReceive().isSuccess)
            releaseFirst.complete(Unit)
            assertEquals("d", started.receive())
            assertEquals(listOf("a", "b", "c", "d"), result.await().map { it.code })
        }
    }

    @Test
    fun propagatesNetworkFailureToTheCaller() = runBlocking {
        val failure = runCatching {
            loadClassroomIndex(listOf("class")) {
                throw FirebaseFirestoreException("Offline", FirebaseFirestoreException.Code.UNAVAILABLE)
            }
        }.exceptionOrNull() as FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.UNAVAILABLE, failure.code)
    }
}
