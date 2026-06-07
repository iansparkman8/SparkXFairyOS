package com.sparkx.fairyos.domain.memory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.teachGrowDataStore: DataStore<Preferences> by preferencesDataStore(name = "teach_grow")

class TeachGrowRepository(private val context: Context) {

    private val ENTRIES_KEY = stringPreferencesKey("entries_json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    val entriesFlow: Flow<List<TeachGrowEntry>> = context.teachGrowDataStore.data
        .map { prefs ->
            val jsonStr = prefs[ENTRIES_KEY] ?: "[]"
            try {
                json.decodeFromString<List<TeachGrowEntry>>(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun addEntry(entry: TeachGrowEntry) {
        context.teachGrowDataStore.edit { prefs ->
            val current = prefs[ENTRIES_KEY]?.let {
                try { json.decodeFromString<List<TeachGrowEntry>>(it) } catch (e: Exception) { emptyList() }
            } ?: emptyList()
            val updated = current + entry
            prefs[ENTRIES_KEY] = json.encodeToString(updated)
        }
    }

    suspend fun deleteEntry(id: String) {
        context.teachGrowDataStore.edit { prefs ->
            val current = prefs[ENTRIES_KEY]?.let {
                try { json.decodeFromString<List<TeachGrowEntry>>(it) } catch (e: Exception) { emptyList() }
            } ?: emptyList()
            val updated = current.filter { it.id != id }
            prefs[ENTRIES_KEY] = json.encodeToString(updated)
        }
    }

    suspend fun search(query: String): List<TeachGrowEntry> {
        // Simple in-memory filter; for production could be more advanced
        return entriesFlow.map { list ->
            if (query.isBlank()) list else list.filter {
                it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
            }
        }.let { flow ->
            // Collect once for simplicity in v7; in real use collect in UI
            kotlinx.coroutines.runBlocking { flow.firstOrNull() ?: emptyList() }
        }
    }
}