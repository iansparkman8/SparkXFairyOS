package com.sparkx.fairyos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sparkx.fairyos.domain.memory.TeachGrowRepository
import com.sparkx.fairyos.domain.mode.SparkModeManager
import com.sparkx.fairyos.domain.voice.SparkVoiceController
import com.sparkx.fairyos.overlay.SparkOverlayController
import com.sparkx.fairyos.ui.components.ModeBanner
import com.sparkx.fairyos.ui.screens.*
import com.sparkx.fairyos.ui.theme.SparkXFairyOSTheme

class MainActivity : ComponentActivity() {

    private lateinit var modeManager: SparkModeManager
    private lateinit var voiceController: SparkVoiceController
    private lateinit var overlayController: SparkOverlayController
    private lateinit var teachGrowRepo: TeachGrowRepository

    // Simple screen state
    private var currentScreen by mutableStateOf(Screen.HOME)

    enum class Screen { HOME, TEACH_GROW, PERMISSIONS, AI_PROVIDERS, SETTINGS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        modeManager = SparkModeManager(this)
        voiceController = SparkVoiceController(this, modeManager)
        overlayController = SparkOverlayController(this, modeManager)
        teachGrowRepo = TeachGrowRepository(this)

        // Handle shared text from other apps
        handleIncomingShare(intent)

        setContent {
            SparkXFairyOSTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A12)) {
                    Column {
                        // Top bar with mode
                        TopAppBar(
                            title = { Text("Spark Baby v7", color = Color.White) },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A2E))
                        )
                        ModeBanner(modeManager = modeManager)

                        // Simple nav
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Screen.values().forEach { screen ->
                                FilterChip(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    label = { Text(screen.name.replace("_", " ")) }
                                )
                            }
                        }

                        // Main content
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentScreen) {
                                Screen.HOME -> HomeScreen(
                                    voiceController = voiceController,
                                    overlayController = overlayController,
                                    modeManager = modeManager,
                                    onNavigate = { currentScreen = it }
                                )
                                Screen.TEACH_GROW -> TeachGrowScreen(repo = teachGrowRepo)
                                Screen.PERMISSIONS -> PermissionsScreen(
                                    activity = this@MainActivity,
                                    overlayController = overlayController
                                )
                                Screen.AI_PROVIDERS -> AIProviderScreen()
                                Screen.SETTINGS -> SettingsScreen(
                                    modeManager = modeManager,
                                    onModeChanged = { /* refresh */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShare(intent)
    }

    private fun handleIncomingShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                // Pre-fill in Teach & Grow or auto save as memory
                Toast.makeText(this, "Shared text received! Opening Teach & Grow...", Toast.LENGTH_SHORT).show()
                currentScreen = Screen.TEACH_GROW
                // In real: pass to repo or prefill form via state
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceController.shutdown()
        overlayController.hide()
    }
}