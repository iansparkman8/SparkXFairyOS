package com.sparkx.fairyos.domain.memory

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TeachGrowEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val type: String, // lesson, code, behavior, memory, prompt, idea, system
    val timestamp: Long = System.currentTimeMillis(),

    // v8 learning-lab fields (defaults preserve old JSON entries)
    val updatedAt: Long = timestamp,
    val summary: String = "",
    val tags: List<String> = emptyList(),
    val source: String = "manual",
    val priority: Int = 2, // 1 low, 2 normal, 3 high
    val mastery: Int = 0, // 0 fresh, 1 seen, 2 learned, 3 mastered
    val reviewCount: Int = 0,
    val lastReviewedAt: Long = 0L,
    val nextReviewAt: Long = 0L,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val links: List<String> = emptyList()
) {
    val searchableText: String
        get() = buildString {
            append(title).append(" ")
            append(content).append(" ")
            append(summary).append(" ")
            append(type).append(" ")
            append(tags.joinToString(" "))
            append(source)
        }.lowercase()

    val isCode: Boolean
        get() = type.equals("code", ignoreCase = true)

    val isDueForReview: Boolean
        get() = nextReviewAt > 0L && nextReviewAt <= System.currentTimeMillis() && !isArchived
}