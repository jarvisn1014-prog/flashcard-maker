package com.nish.flashcards.data.remote

import com.google.gson.Gson
import com.nish.flashcards.data.model.Flashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// PM Insight: Multi-provider support. The app can use either:
// 1. Google Gemini (generativelanguage.googleapis.com) — Gemini native API
// 2. Ollama Cloud (ollama.com/v1) — OpenAI-compatible API
// The user picks the provider in Settings and enters the corresponding API key.
// This makes the app resilient when one provider deprecates models or has outages.

object ProviderConfig {
    const val GEMINI = "gemini"
    const val OLLAMA = "ollama"

    // Gemini endpoint
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    // Try gemini-2.5-flash first (current), fall back to gemini-2.0-flash
    const val GEMINI_MODEL = "gemini-2.5-flash"

    // Ollama Cloud endpoint (OpenAI-compatible)
    const val OLLAMA_BASE_URL = "https://ollama.com/v1/chat/completions"
    const val OLLAMA_MODEL = "glm-5.2" // same model Hermes uses, good at structured generation

    fun geminiUrl(): String = "$GEMINI_BASE_URL$GEMINI_MODEL:generateContent"
}

data class OllamaMessage(
    val role: String,
    val content: String
)

data class OllamaRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 4096,
    val response_format: OllamaResponseFormat? = null,
    val reasoning_effort: String? = null  // "none" skips thinking tokens (cost optimization)
)

data class OllamaResponseFormat(
    val type: String = "json_object"
)

data class OllamaResponse(
    val choices: List<OllamaChoice>?
)

data class OllamaChoice(
    val message: OllamaResponseMessage?
)

data class OllamaResponseMessage(
    val content: String?
)

class FlashcardService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun generateFlashcards(
        apiKey: String,
        sourceText: String,
        deckId: String,
        provider: String = ProviderConfig.GEMINI
    ): Result<List<Flashcard>> = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                ProviderConfig.OLLAMA -> generateViaOllama(apiKey, sourceText, deckId)
                else -> generateViaGemini(apiKey, sourceText, deckId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateViaGemini(
        apiKey: String,
        sourceText: String,
        deckId: String
    ): Result<List<Flashcard>> {
        val prompt = FlashcardPrompt.buildPrompt(sourceText)
        val url = ProviderConfig.geminiUrl()

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
            return Result.failure(Exception("Gemini API error (${response.code}): ${responseBody?.take(200)}"))
        }

        val json = try {
            gson.fromJson(responseBody, GeminiResponse::class.java)
        } catch (e: Exception) {
            return Result.failure(Exception("Failed to parse Gemini response: ${e.message}"))
        }

        val text = json?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: return Result.failure(Exception("Gemini returned no content"))

        val cleaned = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val cards = parseFlashcards(cleaned, deckId)
        return Result.success(cards)
    }

    private fun generateViaOllama(
        apiKey: String,
        sourceText: String,
        deckId: String
    ): Result<List<Flashcard>> {
        val prompt = FlashcardPrompt.buildPrompt(sourceText)

        val ollamaRequest = OllamaRequest(
            model = ProviderConfig.OLLAMA_MODEL,
            messages = listOf(
                OllamaMessage(role = "system", content = "You are an expert educator who creates high-quality study flashcards. Return ONLY a JSON array of objects with 'front' and 'back' keys. No markdown, no explanation."),
                OllamaMessage(role = "user", content = prompt)
            ),
            temperature = 0.7,
            max_tokens = 4096,
            response_format = OllamaResponseFormat(type = "json_object"),
            reasoning_effort = "none"  // skip thinking tokens for cost optimization
        )

        val requestBody = gson.toJson(ollamaRequest)

        val request = Request.Builder()
            .url(ProviderConfig.OLLAMA_BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            return Result.failure(Exception("Ollama API error (${response.code}): ${responseBody?.take(200)}"))
        }

        val ollamaResponse = try {
            gson.fromJson(responseBody, OllamaResponse::class.java)
        } catch (e: Exception) {
            return Result.failure(Exception("Failed to parse Ollama response: ${e.message}"))
        }

        val text = ollamaResponse?.choices?.firstOrNull()?.message?.content
            ?: return Result.failure(Exception("Ollama returned no content"))

        // Ollama may wrap in json_object format — extract the array
        val cleaned = extractJsonArray(text)
        val cards = parseFlashcards(cleaned, deckId)
        return Result.success(cards)
    }

    /// Extract JSON array from text (may be wrapped in an object or markdown)
    private fun extractJsonArray(text: String): String {
        var cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        // If wrapped in {"flashcards": [...]}, extract the array
        val arrayStart = cleaned.indexOf('[')
        val arrayEnd = cleaned.lastIndexOf(']')
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return cleaned.substring(arrayStart, arrayEnd + 1)
        }
        return cleaned
    }

    private fun parseFlashcards(jsonText: String, deckId: String): List<Flashcard> {
        return try {
            val dtos = gson.fromJson(jsonText, Array<FlashcardDto>::class.java)
            dtos.map { dto ->
                Flashcard(
                    id = java.util.UUID.randomUUID().toString(),
                    deckId = deckId,
                    front = dto.front,
                    back = dto.back
                )
            }
        } catch (e: Exception) {
            // Try parsing as a single object with a "flashcards" array
            try {
                val jsonObj = com.google.gson.JsonParser.parseString(jsonText).asJsonObject
                val arr = jsonObj.getAsJsonArray("flashcards") ?: jsonObj.getAsJsonArray("cards")
                arr?.map { item ->
                    val obj = item.asJsonObject
                    Flashcard(
                        id = java.util.UUID.randomUUID().toString(),
                        deckId = deckId,
                        front = obj.get("front")?.asString ?: "",
                        back = obj.get("back")?.asString ?: ""
                    )
                } ?: emptyList()
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    suspend fun validateApiKey(apiKey: String, provider: String = ProviderConfig.GEMINI): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                ProviderConfig.OLLAMA -> validateOllamaKey(apiKey)
                else -> validateGeminiKey(apiKey)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateGeminiKey(apiKey: String): Result<Boolean> {
        val prompt = "Return a JSON array with one flashcard: [{\"front\": \"Test\", \"back\": \"Success\"}]"
        val url = ProviderConfig.geminiUrl()

        val requestBody = gson.toJson(
            GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(temperature = 0.0, maxOutputTokens = 100, responseMimeType = "application/json")
            )
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey.trim())
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return Result.success(false)

        val body = response.body?.string() ?: return Result.success(false)
        val json = try { gson.fromJson(body, GeminiResponse::class.java) } catch (e: Exception) { return Result.success(false) }
        val hasContent = json?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.isNotBlank() == true
        return Result.success(hasContent)
    }

    private fun validateOllamaKey(apiKey: String): Result<Boolean> {
        val ollamaRequest = OllamaRequest(
            model = ProviderConfig.OLLAMA_MODEL,
            messages = listOf(OllamaMessage(role = "user", content = "Say 'OK'")),
            max_tokens = 50,
            reasoning_effort = "none"
        )

        val request = Request.Builder()
            .url(ProviderConfig.OLLAMA_BASE_URL)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(ollamaRequest).toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return Result.success(false)

        val body = response.body?.string() ?: return Result.success(false)
        val ollamaResponse = try { gson.fromJson(body, OllamaResponse::class.java) } catch (e: Exception) { return Result.success(false) }
        val hasContent = ollamaResponse?.choices?.firstOrNull()?.message?.content?.isNotBlank() == true
        return Result.success(hasContent)
    }
}