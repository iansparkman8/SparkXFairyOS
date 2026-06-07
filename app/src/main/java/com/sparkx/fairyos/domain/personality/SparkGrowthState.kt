package com.sparkx.fairyos.domain.personality

import kotlinx.serialization.Serializable

@Serializable
data class SparkGrowthState(
    val learningXp: Int = 0,
    val teachGrowXp: Int = 0,
    val selfUpgradeXp: Int = 0,
    val voiceXp: Int = 0,
    val bugReportCount: Int = 0,
    val totalEntriesCount: Int = 0,
    val bondLevel: Int = 1,
    val dominantTraits: Set<SparkTrait> = setOf(SparkTrait.CURIOUS, SparkTrait.PLAYFUL),
    val unlockedForms: Set<SparkForm> = setOf(SparkForm.DEFAULT_FAIRY),
    val currentForm: SparkForm = SparkForm.DEFAULT_FAIRY,
    val lastFormChangeAt: Long = 0L,
    val allowSparkToSuggestForms: Boolean = true
) {
    fun describe(): String {
        val totalXp = learningXp + teachGrowXp + selfUpgradeXp + voiceXp
        val formNames = unlockedForms.joinToString(", ") { it.displayName }
        val traitNames = dominantTraits.joinToString(", ") { it.name.lowercase() }

        return buildString {
            append("I am currently a ${currentForm.displayName}. ")
            append("Bond level $bondLevel. ")
            append("Total experience: $totalXp. ")
            append("I have learned $totalEntriesCount things. ")
            if (bugReportCount > 0) append("Reported $bugReportCount bugs. ")
            append("Unlocked forms: $formNames. ")
            append("My dominant traits are $traitNames.")
        }
    }
}