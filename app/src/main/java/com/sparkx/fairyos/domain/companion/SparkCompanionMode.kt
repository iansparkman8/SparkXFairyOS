package com.sparkx.fairyos.domain.companion

enum class SparkCompanionMode {
    SAFE_LOCAL,           // Pure local mode - no cloud
    LOCAL_PLUS_CLOUD_AI,  // User-enabled cloud providers
    OWNER_ASSISTED,       // Visible advanced controls with confirmation
    MARS_COMPANION        // Mars-themed local mission mode
}