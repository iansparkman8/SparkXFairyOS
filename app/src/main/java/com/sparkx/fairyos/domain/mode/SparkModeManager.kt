package com.sparkx.fairyos.domain.mode

import android.content.Context

/**
 * Stub for v7. Owner Mode state is currently managed in MainActivity with SharedPreferences.
 * Full DataStore version can be restored later.
 */
class SparkModeManager(private val context: Context) {
    val isOwnerMode: Boolean = false

    suspend fun setOwnerMode(enabled: Boolean) {
        // No-op in stub
    }

    suspend fun enableOwnerModeWithConsent() {
        // No-op in stub
    }
}