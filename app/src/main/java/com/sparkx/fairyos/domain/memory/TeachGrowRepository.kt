package com.sparkx.fairyos.domain.memory

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class TeachGrowRepository(private val context: Context) {
    private val file = File(context.filesDir, "teachgrow_entries.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _entries = MutableStateFlow<List<TeachGrowEntry>>(emptyList())
    val entries: StateFlow<List<TeachGrowEntry>> = _entries.asStateFlow()

    init {
        load()
    }

    private fun load() {
        if (file.exists()) {
            try {
                val text = file.readText()
                if (text.isNotBlank()) {
                    _entries.value = json.decodeFromString(text)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun save() = withContext(Dispatchers.IO) {
        try {
            file.writeText(json.encodeToString(_entries.value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addEntry(title: String, content: String, type: String) {
        val entry = TeachGrowEntry(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Untitled" },
            content = content,
            type = type,
            timestamp = System.currentTimeMillis()
        )
        _entries.value = _entries.value + entry
        save()
    }

    suspend fun deleteEntry(id: String) {
        _entries.value = _entries.value.filter { it.id != id }
        save()
    }

    fun search(query: String): List<TeachGrowEntry> {
        if (query.isBlank()) return _entries.value
        val q = query.lowercase()
        return _entries.value.filter {
            it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
        }
    }
}