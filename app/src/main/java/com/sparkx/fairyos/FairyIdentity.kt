package com.sparkx.fairyos

import androidx.compose.ui.graphics.Color

enum class FairyForm(
    val label: String,
    val pronouns: String,
    val description: String
) {
    Androgynous(
        label = "Androgynous",
        pronouns = "they/them",
        description = "Balanced, luminous, calm, and universal."
    ),
    Feminine(
        label = "Feminine",
        pronouns = "she/her",
        description = "Graceful, warm, expressive, and elegant."
    ),
    Masculine(
        label = "Masculine",
        pronouns = "he/him",
        description = "Focused, bold, protective, and steady."
    ),
    Fluid(
        label = "Fluid",
        pronouns = "any",
        description = "Shifts presentation as Spark Baby grows and explores."
    ),
    Mythic(
        label = "Mythic",
        pronouns = "they/them",
        description = "More magical creature than human — ancient, strange, and powerful."
    )
}

data class FairyPalette(
    val core: Color,
    val aura: Color,
    val wingA: Color,
    val wingB: Color,
    val accent: Color,
    val shadow: Color
)

fun FairyForm.palette(): FairyPalette {
    return when (this) {
        FairyForm.Androgynous -> FairyPalette(
            core = Color(0xFF7CF7D4),
            aura = Color(0xFF39D7FF),
            wingA = Color(0xFF9CEBFF),
            wingB = Color(0xFFB8A7FF),
            accent = Color(0xFFFFD36E),
            shadow = Color(0xFF12182A)
        )
        FairyForm.Feminine -> FairyPalette(
            core = Color(0xFFEAC7FF),
            aura = Color(0xFFFFB7E8),
            wingA = Color(0xFFFFD6F7),
            wingB = Color(0xFFCDB7FF),
            accent = Color(0xFFFFD36E),
            shadow = Color(0xFF1A1024)
        )
        FairyForm.Masculine -> FairyPalette(
            core = Color(0xFF6FD6FF),
            aura = Color(0xFF3EA2FF),
            wingA = Color(0xFFBDEEFF),
            wingB = Color(0xFF7EA7FF),
            accent = Color(0xFFFFD36E),
            shadow = Color(0xFF0B1526)
        )
        FairyForm.Fluid -> FairyPalette(
            core = Color(0xFFB9A7FF),
            aura = Color(0xFF63F5FF),
            wingA = Color(0xFFFFC4F2),
            wingB = Color(0xFF8CFFDC),
            accent = Color(0xFFFFE38A),
            shadow = Color(0xFF111126)
        )
        FairyForm.Mythic -> FairyPalette(
            core = Color(0xFFFFD36E),
            aura = Color(0xFFB06DFF),
            wingA = Color(0xFFE7D6FF),
            wingB = Color(0xFF80FFE0),
            accent = Color(0xFFFFF1A8),
            shadow = Color(0xFF130B24)
        )
    }
}

object FairyFormChooser {
    fun chooseFromGrowth(
        growthPoints: Int,
        lessonsLearned: Int,
        voiceInteractions: Int,
        current: FairyForm
    ): FairyForm {
        if (growthPoints < 6) return current

        return when {
            lessonsLearned >= 20 && voiceInteractions >= 20 -> FairyForm.Mythic
            voiceInteractions >= lessonsLearned + 8 -> FairyForm.Fluid
            lessonsLearned >= voiceInteractions + 8 -> FairyForm.Androgynous
            growthPoints % 5 == 0 -> FairyForm.Fluid
            growthPoints % 3 == 0 -> FairyForm.Mythic
            else -> current
        }
    }
}