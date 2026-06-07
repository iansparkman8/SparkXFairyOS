package com.sparkx.fairyos.domain.personality

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

private val Context.personalityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "spark_personality"
)

class SparkPersonalityRepository(private val context: Context) {

    private val stateKey = stringPreferencesKey("growth_state")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    val growthFlow: Flow<SparkGrowthState> = context.personalityDataStore.data.map { prefs ->
        decodeState(prefs[stateKey])
    }

    suspend fun getState(): SparkGrowthState {
        return growthFlow.first()
    }

    suspend fun logEntryAndAddXp(type: String, amount: Int = xpForType(type)): Pair<Boolean, SparkForm?> {
        var unlocked: SparkForm? = null
        var didUnlock = false

        context.personalityDataStore.edit { prefs ->
            val current = decodeState(prefs[stateKey])

            var learningXp = current.learningXp
            var teachGrowXp = current.teachGrowXp
            var selfUpgradeXp = current.selfUpgradeXp
            var bugReportCount = current.bugReportCount
            val totalEntriesCount = current.totalEntriesCount + 1

            when (type.lowercase()) {
                "lesson" -> {
                    learningXp += amount
                    teachGrowXp += 1
                }
                "code" -> {
                    learningXp += amount
                    teachGrowXp += 2
                }
                "memory" -> {
                    teachGrowXp += amount
                }
                "behavior" -> {
                    teachGrowXp += amount
                }
                "self-upgrade" -> {
                    selfUpgradeXp += amount
                }
                "bug" -> {
                    selfUpgradeXp += amount
                    bugReportCount += 1
                }
                "feature" -> {
                    teachGrowXp += amount
                    selfUpgradeXp += 1
                }
                "system" -> {
                    selfUpgradeXp += amount
                }
                else -> {
                    teachGrowXp += amount.coerceAtLeast(1)
                }
            }

            val totalXp = learningXp + teachGrowXp + selfUpgradeXp + current.voiceXp
            val bondLevel = SparkFormRules.determineBondLevel(totalXp)

            val traits = buildTraits(
                learningXp = learningXp,
                teachGrowXp = teachGrowXp,
                selfUpgradeXp = selfUpgradeXp,
                bugReportCount = bugReportCount,
                fallback = current.dominantTraits
            )

            val tentative = current.copy(
                learningXp = learningXp,
                teachGrowXp = teachGrowXp,
                selfUpgradeXp = selfUpgradeXp,
                bugReportCount = bugReportCount,
                totalEntriesCount = totalEntriesCount,
                bondLevel = bondLevel,
                dominantTraits = traits
            )

            val nextUnlocks = SparkFormRules.calculateUnlocks(tentative)
            val freshUnlocks = nextUnlocks - current.unlockedForms

            if (freshUnlocks.isNotEmpty()) {
                unlocked = freshUnlocks.first()
                didUnlock = true
            }

            val updated = tentative.copy(unlockedForms = nextUnlocks)
            prefs[stateKey] = json.encodeToString(updated)
        }

        return didUnlock to unlocked
    }

    suspend fun setForm(form: SparkForm): Boolean {
        var success = false

        context.personalityDataStore.edit { prefs ->
            val current = decodeState(prefs[stateKey])

            if (form in current.unlockedForms) {
                val updated = current.copy(
                    currentForm = form,
                    lastFormChangeAt = System.currentTimeMillis()
                )
                prefs[stateKey] = json.encodeToString(updated)
                success = true
            }
        }

        return success
    }

    suspend fun unlockForm(form: SparkForm) {
        context.personalityDataStore.edit { prefs ->
            val current = decodeState(prefs[stateKey])
            val updated = current.copy(
                unlockedForms = current.unlockedForms + form
            )
            prefs[stateKey] = json.encodeToString(updated)
        }
    }

    suspend fun setTraits(traits: Set<SparkTrait>) {
        context.personalityDataStore.edit { prefs ->
            val current = decodeState(prefs[stateKey])
            val updated = current.copy(
                dominantTraits = traits.ifEmpty { current.dominantTraits }
            )
            prefs[stateKey] = json.encodeToString(updated)
        }
    }

    suspend fun setAllowSparkToSuggestForms(enabled: Boolean) {
        context.personalityDataStore.edit { prefs ->
            val current = decodeState(prefs[stateKey])
            val updated = current.copy(allowSparkToSuggestForms = enabled)
            prefs[stateKey] = json.encodeToString(updated)
        }
    }

    suspend fun suggestBestForm(): SparkForm {
        val current = getState()

        return when {
            SparkTrait.TECHNICAL in current.dominantTraits &&
                SparkForm.CYBER_FAIRY in current.unlockedForms -> SparkForm.CYBER_FAIRY

            SparkTrait.CREATIVE in current.dominantTraits &&
                SparkForm.STAR_FAIRY in current.unlockedForms -> SparkForm.STAR_FAIRY

            SparkTrait.CALM in current.dominantTraits &&
                SparkForm.DREAM_FAIRY in current.unlockedForms -> SparkForm.DREAM_FAIRY

            current.bugReportCount >= 3 &&
                SparkForm.SHADOW_FAIRY in current.unlockedForms -> SparkForm.SHADOW_FAIRY

            current.bondLevel >= 5 &&
                SparkForm.ROYAL_FAIRY in current.unlockedForms -> SparkForm.ROYAL_FAIRY

            else -> current.currentForm
        }
    }

    private fun decodeState(raw: String?): SparkGrowthState {
        return try {
            if (raw.isNullOrBlank()) SparkGrowthState()
            else json.decodeFromString<SparkGrowthState>(raw)
        } catch (_: Exception) {
            SparkGrowthState()
        }
    }

    private fun xpForType(type: String): Int {
        return when (type.lowercase()) {
            "lesson" -> 2
            "code" -> 3
            "memory" -> 2
            "behavior" -> 2
            "self-upgrade" -> 4
            "bug" -> 3
            "feature" -> 3
            "system" -> 3
            else -> 1
        }
    }

    private fun buildTraits(
        learningXp: Int,
        teachGrowXp: Int,
        selfUpgradeXp: Int,
        bugReportCount: Int,
        fallback: Set<SparkTrait>
    ): Set<SparkTrait> {
        val traits = mutableSetOf<SparkTrait>()

        if (learningXp >= 6) traits += SparkTrait.CURIOUS
        if (learningXp >= 12) traits += SparkTrait.TECHNICAL
        if (teachGrowXp >= 8) traits += SparkTrait.CREATIVE
        if (teachGrowXp >= 14) traits += SparkTrait.PLAYFUL
        if (selfUpgradeXp >= 8) traits += SparkTrait.FOCUSED
        if (bugReportCount >= 2) traits += SparkTrait.PROTECTIVE
        if (teachGrowXp >= 10 && learningXp >= 10) traits += SparkTrait.CALM

        return traits.ifEmpty { fallback.ifEmpty { setOf(SparkTrait.CURIOUS, SparkTrait.PLAYFUL) } }
    }
}