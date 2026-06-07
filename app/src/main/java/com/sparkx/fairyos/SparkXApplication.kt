package com.sparkx.fairyos

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sparkx_prefs")

class SparkXApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Init any global singletons if needed
    }
}