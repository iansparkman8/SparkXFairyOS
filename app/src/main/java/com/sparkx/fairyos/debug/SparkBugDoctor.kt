package com.sparkx.fairyos.debug

object SparkBugDoctor {

    data class BugFix(
        val title: String,
        val cause: String,
        val fix: String,
        val snippet: String
    )

    fun diagnose(errorText: String): BugFix {
        val e = errorText.lowercase()

        return when {
            "'val' cannot be reassigned" in e -> BugFix(
                title = "Read-only value reassigned",
                cause = "A Kotlin val or function parameter is being assigned a new value.",
                fix = "Move the state change to the parent composable, or change local remembered state from val to var.",
                snippet = """
                    // Bad:
                    currentMood = SparkMood.HAPPY
                    
                    // Good parent-owned state:
                    var currentMood by remember { mutableStateOf(SparkMood.IDLE) }
                    
                    SparkXHomeScreen(
                        currentMood = currentMood,
                        onAvatarTap = {
                            currentMood = SparkMood.HAPPY
                            avatarPulseKey++
                        }
                    )
                """.trimIndent()
            )

            "unresolved reference" in e -> BugFix(
                title = "Missing symbol or import",
                cause = "The file references a function, class, enum, or extension that does not exist in scope.",
                fix = "Add the missing file, fix the package name, or import the symbol.",
                snippet = """
                    // Example extension import:
                    import com.sparkx.fairyos.palette
                    
                    // Example normal import:
                    import com.sparkx.fairyos.FairyForm
                """.trimIndent()
            )

            "no parameter with name" in e -> BugFix(
                title = "Function signature mismatch",
                cause = "A composable is being called with a named parameter that its function signature does not accept.",
                fix = "Update the function signature to match the call, or remove the unsupported argument.",
                snippet = """
                    @Composable
                    fun SettingsScreen(
                        isOwnerMode: Boolean = false,
                        onToggleOwnerMode: () -> Unit = {},
                        overlayVisible: Boolean = false,
                        onToggleOverlay: () -> Unit = {},
                        onRequestOverlay: () -> Unit = {}
                    ) {
                        // UI here
                    }
                """.trimIndent()
            )

            "type mismatch" in e -> BugFix(
                title = "Type mismatch",
                cause = "A value of one type is being passed where another type is expected.",
                fix = "Make the function parameter type match the call site, or convert the value before passing it.",
                snippet = """
                    // Bad:
                    color = pulseAnimation
                    
                    // Good:
                    color = fairyPalette.core.copy(alpha = pulseAnimation.value)
                """.trimIndent()
            )

            "gradlew" in e && "syntax error" in e -> BugFix(
                title = "Broken Gradle wrapper",
                cause = "The gradlew shell script is corrupted or overwritten.",
                fix = "Use global Gradle temporarily, then regenerate the Gradle wrapper.",
                snippet = """
                    gradle clean assembleDebug --stacktrace
                    
                    gradle wrapper --gradle-version 8.10.2 --distribution-type bin
                    chmod +x gradlew
                """.trimIndent()
            )

            "manifest merger failed" in e -> BugFix(
                title = "AndroidManifest merge failure",
                cause = "The manifest has conflicting declarations, missing exported flags, or invalid permissions.",
                fix = "Check activity/service declarations and Android 12+ exported requirements.",
                snippet = """
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                """.trimIndent()
            )

            else -> BugFix(
                title = "Unknown bug",
                cause = "Spark Baby does not recognize this error pattern yet.",
                fix = "Paste the first red compiler block into Teach & Grow so Spark Baby can learn this bug pattern.",
                snippet = """
                    // Save this error into Teach & Grow:
                    // Category: Build Error
                    // Error:
                    // Fix:
                    // Verified: false
                """.trimIndent()
            )
        }
    }
}