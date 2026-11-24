
package com.example.vclass.models

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vclass.BuildConfig
import com.example.vclass.MessageModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    val messageList = mutableStateListOf<MessageModel>()

    private val conversationHistory = StringBuilder()
    private var isSending = false

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun sendMessage(question: String) {
        val text = question.trim()
        if (text.isEmpty() || isSending) return

        isSending = true
        messageList.add(MessageModel(text, "user"))

        viewModelScope.launch {
            try {
                val prompt = preprocessForEducationalContext(text)
                val reply = generateReply(prompt)

                conversationHistory.append("Student: $text\n")
                conversationHistory.append("Tutor: $reply\n")

                messageList.add(MessageModel(reply, "model"))

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Log.e("VCLASS_AI", "Gemini API failed", e)

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

                messageList.add(MessageModel(errorMessage, "model"))

            } finally {
                isSending = false
            }
        }
    }

    private suspend fun generateReply(prompt: String): String {
        for (attempt in 1..3) {
            try {
                Log.d("VCLASS_AI", "Request attempt $attempt")

                val response = generativeModel.generateContent(prompt)
                val answer = response.text?.trim().orEmpty()

                return answer.ifEmpty {
                    "Gemini returned an empty response."
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                val error = e.message.orEmpty()

                val temporaryError =
                    error.contains("503") ||
                            error.contains("high demand", true) ||
                            error.contains("UNAVAILABLE", true)

                if (!temporaryError || attempt == 3) {
                    throw e
                }

                Log.w("VCLASS_AI", "Temporary API error. Retrying.")
                delay(attempt * 2000L)
            }
        }

        return "Unable to generate a response."
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
