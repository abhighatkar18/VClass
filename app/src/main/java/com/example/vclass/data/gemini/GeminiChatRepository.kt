package com.example.vclass.data.gemini

import android.util.Log
import com.example.vclass.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal class GeminiChatRepository {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun generateReply(prompt: String): String {
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
}
