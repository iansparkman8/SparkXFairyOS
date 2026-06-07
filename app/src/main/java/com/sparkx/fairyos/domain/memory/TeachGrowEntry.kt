package com.sparkx.fairyos.domain.memory

import kotlinx.serialization.Serializable

@Serializable
data class TeachGrowEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val type: String, // "lesson", "code", "behavior", "memory"
    val timestamp: Long = System.currentTimeMillis()
)