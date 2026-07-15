package com.nish.flashcards.data.remote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.nish.flashcards.data.model.Flashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

// PM Insight: We use OkHttp directly (not Retrofit) for simplicity.
// One HTTP call, one JSON parse. The "service" is a thin wrapper
// that turns API responses into domain objects.
// This keeps the AI layer isolated and easy to swap providers.

class FlashcardService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun generateFlashcards(
        apiKey: String,
        sourceText: String,
        deckId: String
    ): Result<List<Flashcard>> = withContext(Dispatchers.IO) {
        try {
            val prompt = FlashcardPrompt.buildPrompt(sourceText)
            val url = FlashcardPrompt.buildEndpointUrl()

            val requestBody = gson.toJson(
                GeminiRequest(
                    contents = listOf(GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.7,
                        maxOutputTokens = 4096,
                        responseMimeType = "application/json"
                    )
                )
            )

            val request = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("API error ${response.code}: ${responseBody?.take(200)}")
                )
            }

            // Parse Gemini response
            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            val candidates = jsonResponse.getAsJsonArray("candidates")
            if (candidates == null || candidates.size() == 0) {
                return@withContext Result.failure(Exception("No candidates in response"))
            }

            val content = candidates[0].asJsonObject
                .getAsJsonObject("content")
            val parts = content?.getAsJsonArray("parts")
            val text = parts?.get(0)?.asJsonObject?.get("text")?.asString

            if (text.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Empty response from AI"))
            }

            // Parse the flashcard JSON from the AI response
            // The AI returns a JSON array of {front, back} objects
            val cleanJson = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val cardDtos = gson.fromJson(cleanJson, Array<FlashcardDto>::class.java).toList()

            val cards = cardDtos.mapIndexed { index, dto ->
                Flashcard(
                    id = UUID.randomUUID().toString(),
                    deckId = deckId,
                    front = dto.front.trim(),
                    back = dto.back.trim()
                )
            }

            // Filter out empty cards
            val validCards = cards.filter { it.front.isNotBlank() && it.back.isNotBlank() }

            if (validCards.isEmpty()) {
                return@withContext Result.failure(Exception("AI generated no valid flashcards"))
            }

            Result.success(validCards)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Test API key validity with a minimal request
    suspend fun validateApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val prompt = "Return a JSON array with one flashcard: [{\"front\": \"Test\", \"back\": \"Success\"}]"
            val url = FlashcardPrompt.buildEndpointUrl()

            val requestBody = gson.toJson(
                GeminiRequest(
                    contents = listOf(GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.0,
                        maxOutputTokens = 100,
                        responseMimeType = "application/json"
                    )
                )
            )

            val request = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}