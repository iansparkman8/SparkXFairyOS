package com.sparkx.fairyos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.sparkx.fairyos.domain.command.SparkCommandRouter
import com.sparkx.fairyos.domain.memory.TeachGrowEntry
import com.sparkx.fairyos.domain.mood.SparkMood
import com.sparkx.fairyos.domain.personality.SparkGrowthState
import com.sparkx.fairyos.domain.voice.SparkVoiceController
import com.sparkx.fairyos.overlay.SparkOverlayController
import com.sparkx.fairyos.ui.components.HoloBackground
import com.sparkx.fairyos.ui.components.SparkBabyAvatar
import com.sparkx.fairyos.ui.screens.PermissionWizardScreen
import com.sparkx.fairyos.ui.screens.TermsScreen
import com.sparkx.fairyos.ui.theme.SparkXFairyOSTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var voiceController: SparkVoiceController
    private lateinit var commandRouter: SparkCommandRouter

    private var currentMood by mutableStateOf(SparkMood.IDLE)
    private var isSpeaking by mutableStateOf(false)
    private var isListening by mutableStateOf(false)
    private var isOwnerMode by mutableStateOf(false)
    private var overlayVisible by mutableStateOf(false)
    private var commandInput by mutableStateOf("")
    private var teachEntries = mutableStateListOf<TeachGrowEntry>()
    private var growthState by mutableStateOf(SparkGrowthState())
    private var hasAcceptedTerms by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) { /* mic granted */ }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("sparkx_prefs", MODE_PRIVATE)
        hasAcceptedTerms = prefs.getBoolean("spark_terms_accepted", false)
        isOwnerMode = prefs.getBoolean("owner_mode", false)

        val app = application as SparkXApplication
        val repo = app.teachGrowRepository
        val personalityRepo = app.sparkPersonalityRepository

        voiceController = SparkVoiceController(
            context = this,
            onMoodChange = { mood -> currentMood = mood },
            onSpeakingChange = { speaking -> isSpeaking = speaking },
            onCommandRecognized = { text -> handleVoiceCommand(text) },
            onError = { msg -> }
        )
        voiceController.initialize()

        lifecycleScope.launch {
            personalityRepo.growthFlow.collect { growthState = it }
        }

        commandRouter = SparkCommandRouter(
            context = this,
            teachGrowRepo = repo,
            onMoodChange = { mood -> currentMood = mood },
            onSpeak = { text -> voiceController.speak(text) },
            onShowOverlay = { showOverlay() },
            onHideOverlay = { hideOverlay() },
            onGrowthEntryLogged = { type ->
                lifecycleScope.launch {
                    val (didUnlock, unlockedForm) = personalityRepo.logEntryAndAddXp(type)
                    if (didUnlock && unlockedForm != null) {
                        voiceController.speak("I unlocked a new form: ${unlockedForm.displayName}.")
                    }
                }
            },
            getGrowthSummary = { growthState.describe() }
        )

        lifecycleScope.launch {
            repo.entriesFlow.collect { teachEntries.clear(); teachEntries.addAll(it) }
        }

        val onAddTeachEntry: (String, String, String) -> Unit = { title, content, type ->
            lifecycleScope.launch {
                val entry = TeachGrowEntry(title = title, content = content, type = type)
                repo.addEntry(entry)

                val (didUnlock, unlockedForm) = personalityRepo.logEntryAndAddXp(type)
                if (didUnlock && unlockedForm != null) {
                    voiceController.speak("I unlocked a new form: ${unlockedForm.displayName}. You can use it soon in my Wardrobe.")
                }
            }
        }

        val onUpdateTeachEntry: (TeachGrowEntry) -> Unit = { entry ->
            lifecycleScope.launch { repo.updateEntry(entry) }
        }

        val onDeleteTeachEntry: (String) -> Unit = { id ->
            lifecycleScope.launch { repo.deleteEntry(id) }
        }

        val onArchiveTeachEntry: (String, Boolean) -> Unit = { id, archived ->
            lifecycleScope.launch { repo.archiveEntry(id, archived) }
        }

        val onPinTeachEntry: (String, Boolean) -> Unit = { id, pinned ->
            lifecycleScope.launch { repo.pinEntry(id, pinned) }
        }

        val onReviewTeachEntry: (String) -> Unit = { id ->
            lifecycleScope.launch { repo.markReviewed(id) }
        }

        val onExportTeachJson: () -> Unit = {
            lifecycleScope.launch {
                val exported = repo.exportJson(includeArchived = true)
                commandInput = exported.take(4000)
                voiceController.speak("Teach and Grow export prepared in the command box.")
            }
        }

        setContent {
            SparkXFairyOSTheme {
                if (!hasAcceptedTerms) {
                    TermsScreen(
                        onAgree = {
                            prefs.edit().putBoolean("spark_terms_accepted", true).apply()
                            hasAcceptedTerms = true
                        },
                        onSafeLocal = {
                            prefs.edit().putBoolean("spark_terms_accepted", true).apply()
                            hasAcceptedTerms = true
                        },
                        onExit = { finish() }
                    )
                } else {
                    SparkXApp(
                        currentMood = currentMood,
                        isSpeaking = isSpeaking,
                        isListening = isListening,
                        isOwnerMode = isOwnerMode,
                        overlayVisible = overlayVisible,
                        commandInput = commandInput,
                        onCommandInputChange = { commandInput = it },
                        onSendCommand = { processTextCommand(it) },
                        onMicClick = { toggleListening() },
                        onToggleOwnerMode = { toggleOwnerMode() },
                        onToggleOverlay = { if (overlayVisible) hideOverlay() else showOverlay() },
                        teachEntries = teachEntries,
                        onAddTeachEntry = onAddTeachEntry,
                        onUpdateTeachEntry = onUpdateTeachEntry,
                        onDeleteTeachEntry = onDeleteTeachEntry,
                        onArchiveTeachEntry = onArchiveTeachEntry,
                        onPinTeachEntry = onPinTeachEntry,
                        onReviewTeachEntry = onReviewTeachEntry,
                        onExportTeachJson = onExportTeachJson,
                        onLaunchApp = { pkg -> launchApp(pkg) },
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        navController = rememberNavController()
                    )
                }
            }
        }
    }

    private fun toggleOwnerMode() {
        isOwnerMode = !isOwnerMode
        getSharedPreferences("sparkx_prefs", MODE_PRIVATE).edit()
            .putBoolean("owner_mode", isOwnerMode).apply()
        voiceController.speak(if (isOwnerMode) "Owner Mode activated. Advanced controls enabled with your confirmation." else "Owner Mode disabled. Back to safe companion mode.")
    }

    private fun showOverlay() {
        if (Settings.canDrawOverlays(this)) {
            SparkOverlayController.startOverlay(this)
            overlayVisible = true
            currentMood = SparkMood.HAPPY
        } else {
            requestOverlayPermission()
        }
    }

    private fun hideOverlay() {
        SparkOverlayController.stopOverlay(this)
        overlayVisible = false
    }

    private fun toggleListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (isListening) {
            voiceController.stopListening()
            isListening = false
        } else {
            voiceController.startListening()
            isListening = true
        }
    }

    private fun processTextCommand(text: String) {
        if (text.isBlank()) return
        val result = commandRouter.processCommand(text, isOwnerMode)
        voiceController.speak(result.spokenReply)
        currentMood = result.newMood

        if (result.requiresConfirmation && result.confirmationAction != null && isOwnerMode) {
            result.confirmationAction.invoke()
            voiceController.speak("Action confirmed and executed.")
        }

        commandInput = ""
    }

    private fun handleVoiceCommand(text: String) {
        isListening = false
        processTextCommand(text)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        startActivity(intent)
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (e: Exception) { }
    }

    override fun onPause() {
        super.onPause()
        if (isListening) voiceController.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceController.shutdown()
    }
}

@Composable
fun SparkXApp(
    currentMood: SparkMood,
    isSpeaking: Boolean,
    isListening: Boolean,
    isOwnerMode: Boolean,
    overlayVisible: Boolean,
    commandInput: String,
    onCommandInputChange: (String) -> Unit,
    onSendCommand: (String) -> Unit,
    onMicClick: () -> Unit,
    onToggleOwnerMode: () -> Unit,
    onToggleOverlay: () -> Unit,
    teachEntries: List<TeachGrowEntry>,
    onAddTeachEntry: (String, String, String) -> Unit,
    onUpdateTeachEntry: (TeachGrowEntry) -> Unit,
    onDeleteTeachEntry: (String) -> Unit,
    onArchiveTeachEntry: (String, Boolean) -> Unit,
    onPinTeachEntry: (String, Boolean) -> Unit,
    onReviewTeachEntry: (String) -> Unit,
    onExportTeachJson: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = navController.currentDestination?.route == "home",
                    onClick = { navController.navigate("home") { popUpTo("home") } },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = navController.currentDestination?.route == "apps",
                    onClick = { navController.navigate("apps") },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
                    label = { Text("Apps") }
                )
                NavigationBarItem(
                    selected = navController.currentDestination?.route == "teach",
                    onClick = { navController.navigate("teach") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Teach & Grow") },
                    label = { Text("Teach") }
                )
                NavigationBarItem(
                    selected = navController.currentDestination?.route == "ai",
                    onClick = { navController.navigate("ai") },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI") },
                    label = { Text("AI") }
                )
                NavigationBarItem(
                    selected = navController.currentDestination?.route == "settings",
                    onClick = { navController.navigate("settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                SparkXHomeScreen(
                    currentMood = currentMood,
                    isSpeaking = isSpeaking,
                    isOwnerMode = isOwnerMode,
                    overlayVisible = overlayVisible,
                    commandInput = commandInput,
                    onCommandInputChange = onCommandInputChange,
                    onSendCommand = onSendCommand,
                    onMicClick = onMicClick,
                    onToggleOverlay = onToggleOverlay,
                    onToggleOwnerMode = onToggleOwnerMode,
                    onNavigateToApps = { navController.navigate("apps") },
                    onNavigateToTeach = { navController.navigate("teach") },
                    isListening = isListening
                )
            }
            composable("apps") { AppDrawerScreen(onLaunchApp = onLaunchApp) }
            composable("teach") {
                TeachGrowScreen(
                    entries = teachEntries,
                    onAddEntry = onAddTeachEntry,
                    onUpdateEntry = onUpdateTeachEntry,
                    onDeleteEntry = onDeleteTeachEntry,
                    onArchiveEntry = onArchiveTeachEntry,
                    onPinEntry = onPinTeachEntry,
                    onReviewEntry = onReviewTeachEntry,
                    onExportJson = onExportTeachJson
                )
            }
            composable("ai") { AIProviderScreen() }
            composable("settings") {
                SettingsScreen(
                    isOwnerMode = isOwnerMode,
                    onToggleOwnerMode = onToggleOwnerMode,
                    overlayVisible = overlayVisible,
                    onToggleOverlay = onToggleOverlay,
                    onRequestOverlay = onRequestOverlayPermission
                )
            }
            composable("permissions") { PermissionWizardScreen(onFinish = { navController.popBackStack() }) }
            composable("terms") { TermsScreen(onAgree = { navController.popBackStack() }, onSafeLocal = { navController.popBackStack() }, onExit = { /* handled in MainActivity */ }) }
        }
    }
}

// ... (rest of the file remains the same as previous version)