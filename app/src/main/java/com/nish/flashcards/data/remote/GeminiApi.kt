package com.nish.flashcards.data.remote

import com.google.gson.annotations.SerializedName

// PM Insight: This is the AI layer — the entire "intelligence" of the app.
// The API client sends user's text + a carefully crafted prompt to the LLM,
// and parses the structured JSON response into flashcard objects.
//
// The prompt IS the product. A bad prompt = bad flashcards = bad product.
// We iterate on the prompt like a PM iterates on a feature spec.

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 4096,
    val responseMimeType: String = "application/json"
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

data class FlashcardDto(
    @SerializedName("front") val front: String,
    @SerializedName("back") val back: String
)

// PM Insight: Prompt engineering for structured output.
// We ask for JSON with specific keys, give formatting rules,
// and constrain the output to what we can actually use.
// This prompt is the core IP of the app — it determines quality.

object FlashcardPrompt {
    private const val SYSTEM_INSTRUCTION = """
You are an expert educator who creates high-quality study flashcards from text.

Rules for generating flashcards:
1. Create flashcards that test key concepts, definitions, and relationships from the input text.
2. Front (question side): Write a clear, concise question. Maximum 1-2 sentences.
3. Back (answer side): Write a clear, accurate answer. Maximum 2-3 sentences.
4. Focus on understanding, not memorization. Ask "why" and "how" questions, not just "what is X".
5. Avoid trivial details (dates, names) unless they are core concepts.
6. Generate 10-20 flashcards depending on the length and density of the input.
7. Each flashcard should test ONE concept only.
8. Do not repeat questions with slightly different wording.

Return ONLY a JSON array of objects with "front" and "back" keys. No markdown, no explanation.
"""

    fun buildPrompt(sourceText: String): String {
        return """
$SYSTEM_INSTRUCTION

Generate flashcards from the following text:

---
$sourceText
---
""".trimIndent()
    }

    // The model name — Gemini 2.0 Flash is fast and has a good free tier
    const val MODEL = "gemini-2.0-flash"
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    fun buildEndpointUrl(model: String = MODEL): String {
        return "$BASE_URL$model:generateContent"
    }
}