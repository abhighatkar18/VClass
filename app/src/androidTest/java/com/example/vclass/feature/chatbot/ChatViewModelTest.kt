package com.example.vclass.feature.chatbot

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatViewModelTest {
    @Test
    fun clearingTheViewModelCancelsAnActiveReplyWithoutAddingAnErrorMessage() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = ViewModelStore()
        var cancelled = false
        val factory = viewModelFactory {
            initializer {
                ChatViewModel {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled = true
                    }
                }
            }
        }
        lateinit var model: ChatViewModel
        instrumentation.runOnMainSync {
            model = ViewModelProvider(store, factory)[ChatViewModel::class.java]
            assertTrue(model.sendMessage("Question"))
            assertTrue(model.state.value.sending)
            store.clear()
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertTrue(cancelled)
            assertFalse(model.state.value.sending)
            assertEquals(listOf(MessageModel("Question", "user")), model.state.value.messages)
        }
    }

    @Test
    fun followUpIncludesThePreviousExchangeAndRetainsTheTranscript() {
        val prompts = mutableListOf<String>()
        val model = ChatViewModel { prompt ->
            prompts.add(prompt)
            "Reply ${prompts.size}"
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            model.sendMessage("  First question  ")
            model.sendMessage("Follow-up")
        }
        assertFalse(prompts[0].contains("Student: First question"))
        assertTrue(prompts[1].contains("Student: First question\nTutor: Reply 1"))
        assertTrue(prompts[1].endsWith("Follow-up"))
        assertEquals(
            listOf(
                MessageModel("First question", "user"),
                MessageModel("Reply 1", "model"),
                MessageModel("Follow-up", "user"),
                MessageModel("Reply 2", "model")
            ),
            model.state.value.messages
        )
    }

    @Test
    fun failedRequestShowsTheExistingErrorAndIsExcludedFromTheNextPrompt() {
        var attempts = 0
        var nextPrompt = ""
        val model = ChatViewModel { prompt ->
            if (attempts++ == 0) throw IllegalStateException("429")
            nextPrompt = prompt
            "Recovered"
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            model.sendMessage("Failed question")
            model.sendMessage("Retry question")
        }
        assertEquals("Too many requests. Please try again later.", model.state.value.messages[1].message)
        assertFalse(nextPrompt.contains("Failed question"))
        assertEquals("Recovered", model.state.value.messages.last().message)
    }

    @Test
    fun blankInputSkipsTheServiceAndResponseFailureIsSafeToShow() {
        var attempts = 0
        val model = ChatViewModel {
            attempts++
            throw java.net.SocketTimeoutException("internal test detail")
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertFalse(model.sendMessage("   "))
            assertTrue(model.state.value.messages.isEmpty())
            assertEquals(0, attempts)

            assertTrue(model.sendMessage("Study question"))
            assertEquals(1, attempts)
            assertEquals("Unable to get a response. Please try again.", model.state.value.messages.last().message)
            assertFalse(model.state.value.sending)
        }
    }

    @Test
    fun pendingReplyRejectsDuplicatesAndClearsSendingStateWhenComplete() {
        val reply = CompletableDeferred<String>()
        val model = ChatViewModel { reply.await() }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            assertTrue(model.sendMessage("First question"))
            assertTrue(model.state.value.sending)
            assertFalse(model.sendMessage("Second question"))
            assertEquals(listOf(MessageModel("First question", "user")), model.state.value.messages)
            reply.complete("First answer")
        }
        instrumentation.waitForIdleSync()
        assertFalse(model.state.value.sending)
        assertEquals(MessageModel("First answer", "model"), model.state.value.messages.last())
    }
}
