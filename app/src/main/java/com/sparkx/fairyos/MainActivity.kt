package com.sparkx.fairyos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.sparkx.fairyos.domain.command.SparkCommandRouter
import com.sparkx.fairyos.domain.companion.CoreSeal
import com.sparkx.fairyos.domain.memory.TeachGrowEntry
import com.sparkx.fairyos.domain.mood.SparkMood
import com.sparkx.fairyos.domain.personality.SparkGrowthState
import com.sparkx.fairyos.domain.voice.SparkVoiceController
import com.sparkx.fairyos.overlay.SparkOverlayController
import com.sparkx.fairyos.ui.components.HoloBackground
import com.sparkx.fairyos.ui.components.SparkBabyAvatar
import com.sparkx.fairyos.ui.screens.PermissionWizardScreen
import com.sparkx.fairyos.ui.screens.TermsScreen
import com.sparkx.fairyos.ui.theme.SparkGlass
import com.sparkx.fairyos.ui.theme.SparkXFairyOSTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private lateinit var voiceController: SparkVoiceController
    private lateinit var commandRouter: SparkCommandRouter

    private var currentMood by mutableStateOf(SparkMood.IDLE)
    private var isSpeaking by mutableStateOf(false)
    private var isListening by mutableStateOf(false)
    private var isOwnerMode by mutableStateOf(false)
    private var coreOpen by mutableStateOf(false)
    private var pendingLabel by mutableStateOf("")
    private var pendingAction by mutableStateOf<(() -> Unit)?>(null)
    private var ownerError by mutableStateOf<String?>(null)
    private var overlayVisible by mutableStateOf(false)
    private var commandInput by mutableStateOf("")
    private var teachEntries = mutableStateListOf<TeachGrowEntry>()
    private var growthState by mutableStateOf(SparkGrowthState())
    private var hasAcceptedTerms by mutableStateOf(false)
    private var avatarPulseKey by mutableIntStateOf(0)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) { /* mic granted */ }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("sparkx_prefs", MODE_PRIVATE)
        hasAcceptedTerms = prefs.getBoolean("spark_terms_accepted", false)
        // Core is session-only. A stored flag must not boot Owner Mode.
        isOwnerMode = false
        coreOpen = false
        prefs.edit().putBoolean("owner_mode", false).apply()

        val app = application as SparkXApplication
        val repo = app.teachGrowRepository
        val personalityRepo = app.sparkPersonalityRepository

        voiceController = SparkVoiceController(
            context = this,
            onMoodChange = { mood ->
                currentMood = mood
                if (overlayVisible) {
                    SparkOverlayController.updateMood(this, mood)
                }
            },
            onSpeakingChange = { speaking ->
                isSpeaking = speaking
                if (overlayVisible) {
                    SparkOverlayController.updateSpeaking(this, speaking)
                }
            },
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
            onMoodChange = { mood ->
                currentMood = mood
                if (overlayVisible) {
                    SparkOverlayController.updateMood(this, mood)
                }
            },
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

        // Living idle presence loop
        lifecycleScope.launch {
            while (true) {
                delay(4200L)
                if (!isSpeaking && !isListening && !isOwnerMode) {
                    val nextMood = when (Random.nextInt(100)) {
                        in 0..52 -> SparkMood.IDLE
                        in 53..68 -> SparkMood.HAPPY
                        in 69..82 -> SparkMood.THINKING
                        in 83..93 -> SparkMood.SLEEPY
                        else -> SparkMood.IDLE
                    }
                    currentMood = nextMood
                    avatarPulseKey++
                    if (overlayVisible) {
                        SparkOverlayController.updateMood(this@MainActivity, nextMood)
                    }
                }
            }
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
                        onToggleOwnerMode = { if (isOwnerMode || coreOpen) closeCore() },
                        onToggleOverlay = { if (overlayVisible) hideOverlay() else showOverlay() },
                        coreOpen = coreOpen,
                        ownerError = ownerError,
                        onOpenCore = { seal -> openCore(seal) },
                        onCloseCore = { closeCore() },
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
                        navController = rememberNavController(),
                        avatarPulseKey = avatarPulseKey,
                        onAvatarTap = {
                            currentMood = SparkMood.HAPPY
                            avatarPulseKey++
                            if (overlayVisible) {
                                SparkOverlayController.updateMood(this@MainActivity, currentMood)
                            }
                            voiceController.speak("I'm here.")
                        }
                    )
                    if (pendingAction != null && coreOpen && isOwnerMode) {
                        AlertDialog(
                            onDismissRequest = { pendingAction = null },
                            title = { Text("Confirm") },
                            text = { Text(pendingLabel.ifBlank { "Do this?" }) },
                            confirmButton = {
                                TextButton(onClick = {
                                    val action = pendingAction
                                    pendingAction = null
                                    if (!coreOpen || !isOwnerMode) return@TextButton
                                    try {
                                        action?.invoke()
                                        voiceController.speak("Done.")
                                    } catch (_: Exception) {
                                        coreOpen = false
                                        isOwnerMode = false
                                        ownerError = "Owner Mode failed. Safe Companion is still on."
                                        try {
                                            voiceController.speak("That failed. Safe Companion is still here.")
                                        } catch (_: Exception) {}
                                    }
                                }) { Text("Confirm") }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingAction = null }) { Text("Not now") }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun openCore(seal: String) {
        val ok = try {
            CoreSeal.matches(seal)
        } catch (_: Exception) {
            false
        }
        if (!ok) {
            coreOpen = false
            isOwnerMode = false
            try { voiceController.speak("That seal does not open core.") } catch (_: Exception) {}
            return
        }
        coreOpen = true
        isOwnerMode = true
        ownerError = null
        pendingAction = null
        getSharedPreferences("sparkx_prefs", MODE_PRIVATE).edit().putBoolean("owner_mode", false).apply()
        try { voiceController.speak("Core open. I'll ask before I act.") } catch (_: Exception) {}
    }

    private fun closeCore() {
        coreOpen = false
        isOwnerMode = false
        pendingAction = null
        pendingLabel = ""
        getSharedPreferences("sparkx_prefs", MODE_PRIVATE).edit().putBoolean("owner_mode", false).apply()
        try { voiceController.speak("Core closed.") } catch (_: Exception) {}
    }

    private fun showOverlay() {
        if (Settings.canDrawOverlays(this)) {
            SparkOverlayController.startOverlay(this)
            overlayVisible = true
            currentMood = SparkMood.HAPPY
            SparkOverlayController.updateMood(this, currentMood)
            SparkOverlayController.updateSpeaking(this, isSpeaking)
        } else {
            requestOverlayPermission()
        }
    }

    private fun hideOverlay() {
        SparkOverlayController.hideOverlay(this)
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
        try {
            val result = commandRouter.processCommand(text, isOwnerMode && coreOpen)
            voiceController.speak(result.spokenReply)
            currentMood = result.newMood

            if (overlayVisible) {
                SparkOverlayController.updateMood(this, result.newMood)
            }

            if (result.requiresConfirmation && result.confirmationAction != null) {
                if (!isOwnerMode || !coreOpen) {
                    voiceController.speak("Core is closed. I won't do that.")
                } else {
                    pendingLabel = result.actionDescription.ifBlank { result.spokenReply }
                    pendingAction = result.confirmationAction
                }
            }
        } catch (_: Exception) {
            coreOpen = false
            isOwnerMode = false
            pendingAction = null
            ownerError = "Owner Mode failed. Safe Companion is still on."
            try { voiceController.speak("That failed. Safe Companion is still here.") } catch (_: Exception) {}
        }

        commandInput = ""
        avatarPulseKey++
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
    navController: NavHostController,
    coreOpen: Boolean = false,
    ownerError: String? = null,
    onOpenCore: (String) -> Unit = {},
    onCloseCore: () -> Unit = {},
    avatarPulseKey: Int = 0,
    onAvatarTap: () -> Unit = {}
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
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") }
                )
                NavigationBarItem(
                    selected = navController.currentDestination?.route == "teach",
                    onClick = { navController.navigate("teach") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Teach") }
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (coreOpen) {
                Text(
                    text = "Owner Mode Active — All advanced actions require your confirmation",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3A2E12))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Color(0xFFF3E2B0),
                    fontSize = 13.sp
                )
            }
            if (!ownerError.isNullOrBlank()) {
                Text(
                    text = ownerError,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color(0xFFF3E2B0),
                    fontSize = 13.sp
                )
            }
            NavHost(navController, startDestination = "home", modifier = Modifier.weight(1f)) {
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
                    isListening = isListening,
                    avatarPulseKey = avatarPulseKey,
                    onAvatarTap = onAvatarTap
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
                    onRequestOverlay = onRequestOverlayPermission,
                    coreOpen = coreOpen,
                    onSubmitSeal = onOpenCore,
                    onCloseCore = onCloseCore
                )
            }
            composable("permissions") { PermissionWizardScreen(onFinish = { navController.popBackStack() }) }
            composable("terms") { TermsScreen(onAgree = { navController.popBackStack() }, onSafeLocal = { navController.popBackStack() }, onExit = { /* handled in MainActivity */ }) }
            }
        }
    }
}

@Composable
fun SparkXHomeScreen(
    currentMood: SparkMood,
    isSpeaking: Boolean,
    isOwnerMode: Boolean,
    overlayVisible: Boolean,
    commandInput: String,
    onCommandInputChange: (String) -> Unit,
    onSendCommand: (String) -> Unit,
    onMicClick: () -> Unit,
    onToggleOverlay: () -> Unit,
    onToggleOwnerMode: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToTeach: () -> Unit,
    isListening: Boolean = false,
    avatarPulseKey: Int = 0,
    onAvatarTap: () -> Unit = {}
) {
    // ── Colours ───────────────────────────────────────────────────────────────
    val bgTop       = Color(0xFF03050A)
    val bgBottom    = Color(0xFF0B111A)
    val glass       = Color(0xFF101722).copy(alpha = 0.84f)
    val glassBorder = Color(0xFF7DD3FC).copy(alpha = 0.22f)
    val cyan        = Color(0xFF7DD3FC)
    val violet      = Color(0xFF3B82F6)
    val gold        = Color(0xFFBFC7D5)
    val textSoft    = Color(0xFFBFC7D5)

    val moodAccent = when (currentMood) {
        SparkMood.HAPPY -> Color(0xFFE5E7EB)
        SparkMood.ALERT -> Color(0xFF60A5FA)
        SparkMood.THINKING -> cyan
        SparkMood.LISTENING -> Color(0xFF38BDF8)
        SparkMood.SLEEPY -> Color(0xFF94A3B8)
        SparkMood.SPEAKING -> Color(0xFF7DD3FC)
        else -> violet
    }

    val statusText = when {
        isListening -> "Listening"
        isSpeaking  -> "Speaking"
        else        -> currentMood.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
    ) {
        // Strong mood-reactive glow behind avatar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 80.dp)
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            moodAccent.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Top status bar ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPARKX FAIRYOS",
                    fontSize = 11.sp,
                    color = textSoft.copy(alpha = 0.4f),
                    letterSpacing = 0.12.em,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isOwnerMode) {
                        PremiumStatusPill(label = "OWNER", value = "ON", accent = gold)
                    }
                    PremiumStatusPill(
                        label = "OVERLAY",
                        value = if (overlayVisible) "LIVE" else "OFF",
                        accent = if (overlayVisible) cyan else textSoft.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── HERO SECTION ──────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Small label
                Text(
                    text = "YOUR COMPANION",
                    fontSize = 10.sp,
                    color = moodAccent.copy(alpha = 0.6f),
                    letterSpacing = 0.2.em,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(8.dp))

                // Big name
                Text(
                    text = "Spark Baby",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.02).em
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = moodAccent,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(20.dp))

                // Avatar Hero
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onAvatarTap() },
                    contentAlignment = Alignment.Center
                ) {
                    SparkBabyAvatar(
                        mood        = currentMood,
                        isSpeaking  = isSpeaking,
                        reactionKey = avatarPulseKey,
                        modifier    = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── QUICK ACTIONS ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumActionButton(
                    modifier = Modifier.weight(1f),
                    label = "Speak",
                    emoji = "🎙️",
                    accent = cyan,
                    onClick = onMicClick
                )
                PremiumActionButton(
                    modifier = Modifier.weight(1f),
                    label = if (overlayVisible) "Hide Overlay" else "Show Overlay",
                    emoji = if (overlayVisible) "◉" else "◎",
                    accent = if (overlayVisible) violet else textSoft.copy(alpha = 0.5f),
                    onClick = onToggleOverlay
                )
                PremiumActionButton(
                    modifier = Modifier.weight(1f),
                    label = "Teach & Grow",
                    emoji = "🌱",
                    accent = gold,
                    onClick = onNavigateToTeach
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── COMMAND BAR ───────────────────────────────────────────────────
            PremiumGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BasicTextField(
                        value = commandInput,
                        onValueChange = onCommandInputChange,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                            .border(1.dp, violet.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp
                        ),
                        singleLine = true,
                        keyboardActions = KeyboardActions(
                            onDone = { onSendCommand(commandInput) }
                        ),
                        decorationBox = { inner ->
                            if (commandInput.isEmpty()) {
                                Text(
                                    "Talk to Spark Baby…",
                                    fontSize = 15.sp,
                                    color = textSoft.copy(alpha = 0.35f)
                                )
                            }
                            inner()
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(violet.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .clickable { onSendCommand(commandInput) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "↑",
                            fontSize = 20.sp,
                            color = violet,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRIVATE HELPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.07f)
        ),
        border    = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun PremiumStatusPill(
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(99.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, fontSize = 9.sp, color = accent.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
        Text(value, fontSize = 10.sp, color = accent, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PremiumActionButton(
    label: String,
    emoji: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(5.dp))
        Text(
            text       = label,
            fontSize   = 12.sp,
            color      = accent,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
    }
}