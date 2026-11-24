package com.example.vclass.feature.chatbot

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vclass.core.ui.theme.VclassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatbotContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun draftSurvivesWaitingAndARejectedSendThenClearsAfterAcceptance() {
        val state = mutableStateOf(ChatUiState(sending = true))
        val attempted = mutableListOf<String>()
        compose.setContent {
            VclassTheme {
                ChatbotContent(state.value) { message ->
                    attempted += message
                    attempted.size > 1
                }
            }
        }

        val send = compose.onNodeWithContentDescription("Send message")
        val message = "What is photosynthesis?"
        compose.onNodeWithText("Waiting for reply…").assertIsDisplayed()
        compose.onNodeWithText("Message").performTextInput(message)
        send.assertIsNotEnabled()

        compose.runOnIdle { state.value = ChatUiState() }
        send.assertIsEnabled().performClick()
        compose.onNodeWithText(message).assertIsDisplayed()
        send.performClick().assertIsNotEnabled()
        compose.onNodeWithText(message).assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(message, message), attempted) }
    }
}
