
package com.example.vclass.feature.chatbot

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class ChatViewModel(private val generateReply: suspend (String) -> String) : ViewModel() {

    private val messages = MutableStateFlow(emptyList<MessageModel>())
    val messageList = messages.asStateFlow()

    private val conversationHistory = StringBuilder()
    private var isSending = false

    fun sendMessage(question: String) {
        val text = question.trim()
        if (text.isEmpty() || isSending) return

        isSending = true
        messages.update { it + MessageModel(text, "user") }

        viewModelScope.launch {
            try {
                val prompt = preprocessForEducationalContext(text)
                val reply = generateReply(prompt)

                conversationHistory.append("Student: $text\n")
                conversationHistory.append("Tutor: $reply\n")

                messages.update { it + MessageModel(reply, "model") }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Log.e("VCLASS_AI", "Gemini API failed")

                val error = e.message.orEmpty()

                val errorMessage = when {
                    error.contains("404") ->
                        "AI model unavailable. Please check the model configuration."

                    error.contains("503") ||
                            error.contains("high demand", true) ->
                        "Gemini is currently busy. Please try again later."

                    error.contains("429") ->
                        "Too many requests. Please try again later."

                    else ->
                        "Unable to get a response. Please try again."
                }

                messages.update { it + MessageModel(errorMessage, "model") }

            } finally {
                isSending = false
            }
        }
    }

    private fun preprocessForEducationalContext(
        question: String
    ): String {
        return """
            You are VClass AI Tutor, a helpful educational assistant.

            Help students with:
            1. Academic doubts and concept explanations.
            2. Assignments and study guidance.
            3. Class-related questions and FAQs.
            4. Study recommendations and learning resources.
            5. Group projects and collaboration.
            6. Technical support for educational tools.

            Instructions:
            - Provide accurate and understandable answers.
            - Explain difficult concepts step by step.
            - Use examples wherever useful.
            - Maintain a friendly and professional tone.
            - Consider the previous conversation.

            Previous conversation:
            $conversationHistory

            Student question:
            $question
        """.trimIndent()
    }
}
