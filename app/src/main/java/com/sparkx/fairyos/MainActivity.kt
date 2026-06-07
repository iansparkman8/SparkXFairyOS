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
        isOwnerMode = prefs.getBoolean("owner_mode", false)

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
            SparkOverlayController.updateMood(this, currentMood)
            SparkOverlayController.updateSpeaking(this, isSpeaking)
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

        if (overlayVisible) {
            SparkOverlayController.updateMood(this, result.newMood)
        }

        if (result.requiresConfirmation && result.confirmationAction != null && isOwnerMode) {
            result.confirmationAction.invoke()
            voiceController.speak("Action confirmed and executed.")
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
                    onRequestOverlay = onRequestOverlayPermission
                )
            }
            composable("permissions") { PermissionWizardScreen(onFinish = { navController.popBackStack() }) }
            composable("terms") { TermsScreen(onAgree = { navController.popBackStack() }, onSafeLocal = { navController.popBackStack() }, onExit = { /* handled in MainActivity */ }) }
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
        isListening -> "🎙️ Listening…"
        isSpeaking  -> "💬 Speaking…"
        else        -> "Mood: ${currentMood.name.lowercase().replaceFirstChar { it.uppercase() }}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
    ) {
        // Radial glow behind avatar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 100.dp)
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            moodAccent.copy(alpha = 0.15f),
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Top status row ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SparkX FairyOS",
                    fontSize = 12.sp,
                    color = textSoft.copy(alpha = 0.45f),
                    letterSpacing = 0.08.em
                )
                PremiumStatusPill(
                    label = if (overlayVisible) "Overlay" else "App",
                    value = if (overlayVisible) "ON" else "OFF",
                    accent = if (overlayVisible) cyan else textSoft.copy(alpha = 0.35f)
                )
                if (isOwnerMode) {
                    PremiumStatusPill(
                        label = "Owner",
                        value = "✓",
                        accent = gold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Hero glass card ───────────────────────────────────────────────
            PremiumGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = "Spark Baby",
                        fontSize = 11.sp,
                        color = moodAccent.copy(alpha = 0.65f),
                        letterSpacing = 0.18.em
                    )

                    Spacer(Modifier.height(14.dp))

                    // Avatar — preserves existing tap flow
                    Box(
                        modifier = Modifier
                            .size(260.dp)
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

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Spark Baby",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.02).em
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = textSoft.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Quick action row ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PremiumActionButton(
                    modifier = Modifier.weight(1f),
                    label    = "Speak",
                    emoji    = "🎙️",
                    accent   = cyan,
                    onClick  = onMicClick
                )
                PremiumActionButton(
                    modifier = Modifier.weight(1f),
                    label    = if (overlayVisible) "Overlay ON" else "Overlay",
                    emoji    = if (overlayVisible) "✦" else "◎",
                    accent   = if (overlayVisible) violet else textSoft.copy(alpha = 0.45f),
                    onClick  = onToggleOverlay
                )
                PremiumActionButton(
                    modifier = Modifier.weight(1f),
                    label    = "Teach",
                    emoji    = "📖",
                    accent   = gold,
                    onClick  = onNavigateToTeach
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Growth placeholder card ───────────────────────────────────────
            PremiumGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Growth",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Bond: syncing soon",
                            fontSize = 11.sp,
                            color = textSoft.copy(alpha = 0.45f)
                        )
                        Text(
                            text = "Growth: local mode",
                            fontSize = 11.sp,
                            color = textSoft.copy(alpha = 0.35f)
                        )
                    }
                    Text(
                        text = "✦",
                        fontSize = 28.sp,
                        color = violet.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Command input card ────────────────────────────────────────────
            PremiumGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = commandInput,
                            onValueChange = onCommandInputChange,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    violet.copy(alpha = 0.22f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            textStyle = TextStyle(
                                color    = Color.White,
                                fontSize = 13.sp
                            ),
                            singleLine = true,
                            keyboardActions = KeyboardActions(
                                onDone = { onSendCommand(commandInput) }
                            ),
                            decorationBox = { inner ->
                                if (commandInput.isEmpty()) {
                                    Text(
                                        "Talk to Spark Baby…",
                                        fontSize = 13.sp,
                                        color = textSoft.copy(alpha = 0.30f)
                                    )
                                }
                                inner()
                            }
                        )
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    violet.copy(alpha = 0.22f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    violet.copy(alpha = 0.45f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSendCommand(commandInput) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "↑",
                                fontSize = 17.sp,
                                color = violet,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
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
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(99.dp))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, fontSize = 10.sp, color = accent.copy(alpha = 0.55f))
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
            .background(accent.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text       = label,
            fontSize   = 11.sp,
            color      = accent,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
    }
}