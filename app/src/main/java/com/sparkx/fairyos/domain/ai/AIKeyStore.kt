package com.sparkx.fairyos.domain.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIKeyStore(private val context: Context) {
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

    fun saveKey(provider: AIProvider, key: String) {
        prefs.edit().putString(provider.name, key.trim()).apply()
    }

    fun getKey(provider: AIProvider): String? = prefs.getString(provider.name, null)

    fun hasKey(provider: AIProvider): Boolean = !getKey(provider).isNullOrBlank()

    fun clearKey(provider: AIProvider) {
        prefs.edit().remove(provider.name).apply()
    }
}