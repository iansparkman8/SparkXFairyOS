package com.sparkx.fairyos.domain.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SparkAIClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
data class ChatMessage(val role: String, val content: String)

    @Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val max_tokens: Int = 300,
    val temperature: Double = 0.7
)

    @Serializable
data class ChatChoice(val message: ChatMessage)

    @Serializable
data class ChatResponse(val choices: List<ChatChoice>? = null, val error: Map<String, String>? = null)

    suspend fun chat(
        provider: AIProvider,
        apiKey: String,
        userMessage: String,
        systemPrompt: String = "You are Spark Baby, a kind, playful, privacy-first holographic fairy companion living on the user's Android phone. Keep replies short, helpful, magical, and in character. Never ask for passwords or sensitive data."
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "No API key set for ${provider.displayName}. Please add it in AI Providers screen."
        if (provider == AIProvider.LOCAL) return@withContext "Local offline model not yet available in v7. Coming soon!"

        val endpoint = provider.endpoint
        if (endpoint.isBlank()) return@withContext "This provider endpoint not configured in v7. Use OpenAI or Grok for now."

        try {
            val messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userMessage)
            )
            val requestBody = ChatRequest(
                model = provider.model,
                messages = messages
            )

            val bodyJson = json.encodeToString(ChatRequest.serializer(), requestBody)
            val requestBodyObj = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(endpoint)
                .post(requestBodyObj)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                // For Claude may need different header, but v7 focuses on OpenAI-compatible
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "API error (${response.code}): ${responseBody.take(200)}"
            }

            val chatResp = json.decodeFromString(ChatResponse.serializer(), responseBody)
            return@withContext chatResp.choices?.firstOrNull()?.message?.content 
                ?: chatResp.error?.get("message") 
                ?: "Spark Baby is thinking... but got no clear reply."
        } catch (e: Exception) {
            return@withContext "Connection issue: ${e.message?.take(100) ?: "unknown error"}. Check your key and internet."
        }
    }
}