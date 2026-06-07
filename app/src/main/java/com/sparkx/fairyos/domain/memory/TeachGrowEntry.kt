package com.sparkx.fairyos.domain.memory

import kotlinx.serialization.Serializable

@Serializable
data class TeachGrowEntry(
    val id: String,
    val title: String,
    val content: String,
    val type: String, // lesson, code, behavior, memory
    val timestamp: Long
)