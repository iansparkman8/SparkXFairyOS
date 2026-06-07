package com.sparkx.fairyos.domain.companion

object SparkCompanionPolicy {
    const val PRIVACY_PROMISE = """Spark Baby’s Privacy Promise:
• I stay visible.
• I ask before powerful actions.
• I do not secretly read your screen.
• I do not take screenshots.
• I do not record in the background.
• I do not execute saved code.
• I only use cloud AI if you enable it."""

    const val NO_HIDDEN_READING = "SparkX FairyOS does not use hidden screen capture, screenshot APIs, or Accessibility Service screen scraping in safe companion mode."

    const val NO_AUTO_EXECUTION = "Code snippets saved in Teach & Grow are stored as text only. Spark Baby never automatically executes saved code."

    const val CLOUD_AI_OPTIONAL = "Cloud AI features are optional. No hardcoded keys. Messages only leave your device when you enable a provider and send a message."
}