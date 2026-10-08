package com.sparkx.fairyos.domain.companion

import java.security.MessageDigest

/**
 * Drop-in for SparkXFairyOS. Wire from Settings before any owner action.
 * Same seal as the PWA. Hash only. Do not commit the seal.
 * Path: app/src/main/java/com/sparkx/fairyos/domain/companion/CoreSeal.kt
 */
object CoreSeal {
    const val CORE_SHA256 = "56ea9e77948820fce35e33f86bf1b6010428de339926d89a39830acb5897769b"

    fun matches(seal: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(seal.trim().toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { byte -> "%02x".format(byte) }
        return hex == CORE_SHA256
    }
}
