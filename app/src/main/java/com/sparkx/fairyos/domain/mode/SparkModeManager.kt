package com.sparkx.fairyos.domain.mode

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.sparkx.fairyos.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SparkModeManager(private val context: Context) {
    private val OWNER_MODE_KEY = booleanPreferencesKey("owner_mode_enabled")

    val isOwnerMode: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[OWNER_MODE_KEY] ?: false }

    suspend fun setOwnerMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[OWNER_MODE_KEY] = enabled
        }
    }

    // Simple consent flag for wizard
    suspend fun enableOwnerModeWithConsent() {
        setOwnerMode(true)
    }
}