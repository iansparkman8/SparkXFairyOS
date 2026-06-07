package com.sparkx.fairyos.domain.memory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.teachGrowDataStore: DataStore<Preferences> by preferencesDataStore(name = "teach_grow")

class TeachGrowRepository(private val context: Context) {

    private val ENTRIES_KEY = stringPreferencesKey("entries_json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    val entriesFlow: Flow<List<TeachGrowEntry>> = context.teachGrowDataStore.data
        .map { prefs ->
            decodeEntries(prefs[ENTRIES_KEY] ?: "[]")
                .sortedWith(
                    compareByDescending<TeachGrowEntry> { it.isPinned }
                        .thenBy { it.isArchived }
                        .thenByDescending { it.updatedAt }
                )
        }

    val activeEntriesFlow: Flow<List<TeachGrowEntry>> = entriesFlow
        .map { entries -> entries.filter { !it.isArchived } }

    val archivedEntriesFlow: Flow<List<TeachGrowEntry>> = entriesFlow
        .map { entries -> entries.filter { it.isArchived } }

    suspend fun addEntry(entry: TeachGrowEntry) {
        mutate { current ->
            current + entry.copy(
                updatedAt = System.currentTimeMillis(),
                summary = entry.summary.ifBlank { entry.content.take(180) }
            )
        }
    }

    suspend fun updateEntry(entry: TeachGrowEntry) {
        mutate { current ->
            current.map {
                if (it.id == entry.id) entry.copy(updatedAt = System.currentTimeMillis()) else it
            }
        }
    }

    suspend fun deleteEntry(id: String) {
        mutate { current -> current.filter { it.id != id } }
    }

    suspend fun archiveEntry(id: String, archived: Boolean = true) {
        mutate { current ->
            current.map {
                if (it.id == id) it.copy(isArchived = archived, updatedAt = System.currentTimeMillis()) else it
            }
        }
    }

    suspend fun pinEntry(id: String, pinned: Boolean) {
        mutate { current ->
            current.map {
                if (it.id == id) it.copy(isPinned = pinned, updatedAt = System.currentTimeMillis()) else it
            }
        }
    }

    suspend fun markReviewed(id: String) {
        val now = System.currentTimeMillis()
        mutate { current ->
            current.map { entry ->
                if (entry.id != id) {
                    entry
                } else {
                    val newMastery = (entry.mastery + 1).coerceAtMost(3)
                    val delayDays = when (newMastery) {
                        0 -> 1
                        1 -> 2
                        2 -> 5
                        else -> 14
                    }
                    entry.copy(
                        mastery = newMastery,
                        reviewCount = entry.reviewCount + 1,
                        lastReviewedAt = now,
                        nextReviewAt = now + delayDays * 24L * 60L * 60L * 1000L,
                        updatedAt = now
                    )
                }
            }
        }
    }

    suspend fun search(
        query: String,
        type: String = "all",
        includeArchived: Boolean = false
    ): List<TeachGrowEntry> {
        val q = query.trim().lowercase()
        return entriesFlow.first()
            .asSequence()
            .filter { includeArchived || !it.isArchived }
            .filter { type == "all" || it.type.equals(type, ignoreCase = true) }
            .filter { q.isBlank() || it.searchableText.contains(q) }
            .sortedWith(
                compareByDescending<TeachGrowEntry> { it.isPinned }
                    .thenByDescending { it.isDueForReview }
                    .thenByDescending { it.updatedAt }
            )
            .toList()
    }

    suspend fun exportJson(includeArchived: Boolean = true): String {
        val entries = entriesFlow.first()
            .filter { includeArchived || !it.isArchived }
        return json.encodeToString(entries)
    }

    suspend fun importJson(jsonText: String): Int {
        val imported = decodeEntries(jsonText)
        if (imported.isEmpty()) return 0

        mutate { current ->
            val currentIds = current.map { it.id }.toSet()
            current + imported.filter { it.id !in currentIds }
        }

        return imported.size
    }

    private suspend fun mutate(transform: (List<TeachGrowEntry>) -> List<TeachGrowEntry>) {
        context.teachGrowDataStore.edit { prefs ->
            val current = decodeEntries(prefs[ENTRIES_KEY] ?: "[]")
            prefs[ENTRIES_KEY] = json.encodeToString(transform(current))
        }
    }

    private fun decodeEntries(raw: String): List<TeachGrowEntry> {
        return try {
            json.decodeFromString<List<TeachGrowEntry>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}