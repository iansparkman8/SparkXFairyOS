package com.sparkx.fairyos.domain.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Optional cloud AI client. Only used when user explicitly enables a provider and provides their own API key.
 * Keys stored securely with EncryptedSharedPreferences + Android Keystore.
 * No calls without user consent.
 */
class SparkAIClient(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "sparkx_ai_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    enum class Provider { OPENAI, GROK, GEMINI, CLAUDE, LOCAL }

    fun saveApiKey(provider: Provider, key: String) {
        prefs.edit().putString(provider.name, key).apply()
    }

    fun getApiKey(provider: Provider): String? = prefs.getString(provider.name, null)

    fun isProviderEnabled(provider: Provider): Boolean = getApiKey(provider) != null

    suspend fun chat(provider: Provider, prompt: String, systemPrompt: String = "You are Spark Baby, a helpful, whimsical holographic fairy companion living on the user's Android phone. Keep replies short, friendly, and magical."): String {
        val key = getApiKey(provider) ?: return "Please add your API key for ${provider.name} in the AI Console first."

        return when (provider) {
            Provider.OPENAI -> callOpenAI(key, prompt, systemPrompt)
            Provider.GROK -> callGrok(key, prompt, systemPrompt) // xAI endpoint similar
            else -> "${provider.name} integration coming in v8. For now, try OpenAI or Grok."
        }
    }

    private fun callOpenAI(key: String, prompt: String, system: String): String {
        val body = """
        {
          "model": "gpt-4o-mini",
          "messages": [
            {"role": "system", "content": "$system"},
            {"role": "user", "content": "$prompt"}
          ],
          "max_tokens": 150
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "OpenAI error: ${response.code}"
                val respBody = response.body?.string() ?: return "Empty response"
                // Simple parse for content (in production use proper json)
                val content = Regex("\"content\":\"(.*?)\"").find(respBody)?.groupValues?.get(1) ?: "Spark Baby couldn't parse the reply."
                content.replace("\\n", "\n").take(300)
            }
        } catch (e: Exception) {
            "Network error talking to OpenAI: ${e.message}"
        }
    }

    private fun callGrok(key: String, prompt: String, system: String): String {
        // xAI Grok API (similar OpenAI compatible)
        val body = """
        {
          "model": "grok-2-latest",
          "messages": [
            {"role": "system", "content": "$system"},
            {"role": "user", "content": "$prompt"}
          ],
          "max_tokens": 150
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://api.x.ai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Grok error: ${response.code}"
                val respBody = response.body?.string() ?: return "Empty response"
                val content = Regex("\"content\":\"(.*?)\"").find(respBody)?.groupValues?.get(1) ?: "Spark Baby is thinking..."
                content.replace("\\n", "\n").take(300)
            }
        } catch (e: Exception) {
            "Network error talking to Grok: ${e.message}"
        }
    }

    // Gemini and Claude can be added similarly in v8
}