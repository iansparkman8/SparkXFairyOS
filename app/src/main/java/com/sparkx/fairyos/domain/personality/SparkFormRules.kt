package com.sparkx.fairyos.domain.personality

object SparkFormRules {

    fun determineBondLevel(totalXp: Int): Int {
        return when {
            totalXp >= 120 -> 7
            totalXp >= 90 -> 6
            totalXp >= 65 -> 5
            totalXp >= 45 -> 4
            totalXp >= 28 -> 3
            totalXp >= 15 -> 2
            else -> 1
        }
    }

    fun calculateUnlocks(state: SparkGrowthState): Set<SparkForm> {
        val unlocked = state.unlockedForms.toMutableSet()

        val totalXp = state.learningXp + state.teachGrowXp + state.selfUpgradeXp + state.voiceXp

        // Base forms
        if (totalXp >= 8) unlocked += SparkForm.CYBER_FAIRY
        if (state.teachGrowXp >= 10) unlocked += SparkForm.STAR_FAIRY
        if (state.learningXp >= 12) unlocked += SparkForm.DREAM_FAIRY

        // Higher forms
        if (state.bugReportCount >= 3 && state.selfUpgradeXp >= 6) {
            unlocked += SparkForm.SHADOW_FAIRY
        }
        if (state.bondLevel >= 5 && totalXp >= 70) {
            unlocked += SparkForm.ROYAL_FAIRY
        }

        return unlocked
    }
}