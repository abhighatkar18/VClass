package com.example.vclass.feature.chatbot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatViewModelTest {
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
            model.messageList.value
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
        assertEquals("Too many requests. Please try again later.", model.messageList.value[1].message)
        assertFalse(nextPrompt.contains("Failed question"))
        assertEquals("Recovered", model.messageList.value.last().message)
    }
}
