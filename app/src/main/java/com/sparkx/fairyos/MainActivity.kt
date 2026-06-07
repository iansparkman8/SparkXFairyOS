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
import com.sparkx.fairyos.domain.voice.SparkVoiceController
import com.sparkx.fairyos.overlay.SparkOverlayController
import com.sparkx.fairyos.ui.components.HoloBackground
import com.sparkx.fairyos.ui.components.SparkBabyAvatar
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) { /* mic granted */ }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as SparkXApplication
        val repo = app.teachGrowRepository

        val prefs = getSharedPreferences("sparkx_prefs", MODE_PRIVATE)
        isOwnerMode = prefs.getBoolean("owner_mode", false)

        voiceController = SparkVoiceController(
            context = this,
            onMoodChange = { mood -> currentMood = mood },
            onSpeakingChange = { speaking -> isSpeaking = speaking },
            onCommandRecognized = { text -> handleVoiceCommand(text) },
            onError = { msg -> }
        )
        voiceController.initialize()

        commandRouter = SparkCommandRouter(
            context = this,
            teachGrowRepo = repo,
            onMoodChange = { mood -> currentMood = mood },
            onSpeak = { text -> voiceController.speak(text) },
            onShowOverlay = { showOverlay() },
            onHideOverlay = { hideOverlay() }
        )

        lifecycleScope.launch {
            repo.entriesFlow.collect { teachEntries.clear(); teachEntries.addAll(it) }
        }

        // Create stable callbacks in Activity scope (where lifecycleScope, repo, voiceController exist)
        val onAddTeachEntry: (String, String, String) -> Unit = { title, content, type ->
            lifecycleScope.launch {
                val entry = TeachGrowEntry(title = title, content = content, type = type)
                repo.addEntry(entry)
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
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
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
    isListening: Boolean = false
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0B1A))) {
        HoloBackground()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SparkX FairyOS", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF00E5FF))
                if (isOwnerMode) {
                    Surface(color = Color(0xFFFF6EC7), shape = RoundedCornerShape(8.dp)) {
                        Text("OWNER MODE", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            SparkBabyAvatar(
                mood = currentMood,
                isSpeaking = isSpeaking,
                size = 260.dp
            )

            Spacer(Modifier.height(16.dp))

            Text(
                when (currentMood) {
                    SparkMood.SPEAKING -> "Speaking..."
                    SparkMood.LISTENING -> "Listening..."
                    else -> "Spark Baby is ${currentMood.name.lowercase()}"
                },
                color = Color(0xFF9C7BFF)
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = onCommandInputChange,
                    placeholder = { Text("Ask Spark Baby... (open youtube, happy, remember this)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF6B4C9A)
                    )
                )
                IconButton(onClick = { onSendCommand(commandInput) }) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF00E5FF))
                }
                IconButton(onClick = onMicClick) {
                    Icon(
                        if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = Color(0xFFFF6EC7)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onNavigateToApps, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4C9A))) {
                    Icon(Icons.Default.Apps, null); Spacer(Modifier.width(6.dp)); Text("Apps")
                }
                Button(onClick = onNavigateToTeach, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))) {
                    Text("Teach & Grow")
                }
                Button(onClick = onToggleOverlay, colors = ButtonDefaults.buttonColors(containerColor = if (overlayVisible) Color.Gray else Color(0xFF9C7BFF))) {
                    Text(if (overlayVisible) "Hide Bubble" else "Show Bubble")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isOwnerMode) {
                Text("Owner Mode Active — Powerful actions will ask for confirmation", color = Color(0xFFFF6EC7), fontSize = 13.sp)
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1530)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DockButton("Phone", "com.android.dialer", context)
                DockButton("Messages", "com.google.android.apps.messaging", context)
                DockButton("Camera", "com.android.camera2", context)
                DockButton("Browser", "com.android.chrome", context)
                DockButton("Spark", "com.sparkx.fairyos", context)
            }
        }
    }
}

@Composable
fun DockButton(label: String, pkg: String, context: android.content.Context) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
        try {
            val i = context.packageManager.getLaunchIntentForPackage(pkg)
            if (i != null) context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }) {
        Icon(Icons.Default.Star, contentDescription = label, tint = Color(0xFF00E5FF))
        Text(label, fontSize = 11.sp, color = Color.White)
    }
}

@Composable
fun AppDrawerScreen(onLaunchApp: (String) -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val apps = remember {
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0B1A)).padding(16.dp)) {
        Text("App Drawer", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(apps) { (pkg, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onLaunchApp(pkg) }) {
                    Icon(Icons.Default.Android, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(48.dp))
                    Text(label, fontSize = 12.sp, color = Color.White, maxLines = 1)
                }
            }
        }
    }
}

// (TeachGrowScreen and all its helper composables remain exactly as in the previous successful replace)
// They are self-contained and do not need changes for this scope fix.

@Composable
fun TeachGrowScreen(
    entries: List<TeachGrowEntry>,
    onAddEntry: (String, String, String) -> Unit,
    onUpdateEntry: (TeachGrowEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onArchiveEntry: (String, Boolean) -> Unit,
    onPinEntry: (String, Boolean) -> Unit,
    onReviewEntry: (String) -> Unit,
    onExportJson: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TeachGrowEntry?>(null) }
    var selectedType by remember { mutableStateOf("all") }
    var query by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }

    val types = listOf("all", "lesson", "code", "behavior", "memory", "prompt", "idea", "system")

    val filtered = remember(entries, query, selectedType, showArchived) {
        val q = query.trim().lowercase()
        entries
            .asSequence()
            .filter { showArchived || !it.isArchived }
            .filter { selectedType == "all" || it.type.equals(selectedType, ignoreCase = true) }
            .filter { q.isBlank() || it.searchableText.contains(q) }
            .sortedWith(
                compareByDescending<TeachGrowEntry> { it.isPinned }
                    .thenByDescending { it.isDueForReview }
                    .thenByDescending { it.updatedAt }
            )
            .toList()
    }

    val activeCount = entries.count { !it.isArchived }
    val dueCount = entries.count { it.isDueForReview }
    val masteredCount = entries.count { it.mastery >= 3 && !it.isArchived }
    val codeCount = entries.count { it.type.equals("code", ignoreCase = true) && !it.isArchived }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B1A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Teach & Grow Lab",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Local lessons, memories, behaviors, prompts, and safe code notes.",
                    color = Color(0xFF9C7BFF),
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onExportJson) {
                    Icon(Icons.Default.Share, contentDescription = "Export", tint = Color(0xFF00E5FF))
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Teach")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TeachStatCard("Active", activeCount.toString(), Modifier.weight(1f))
            TeachStatCard("Due", dueCount.toString(), Modifier.weight(1f))
            TeachStatCard("Code", codeCount.toString(), Modifier.weight(1f))
            TeachStatCard("Mastered", masteredCount.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Spark Baby's local brain") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF6B4C9A),
                focusedLabelColor = Color(0xFF00E5FF),
                cursorColor = Color(0xFF00E5FF)
            )
        )

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all", "lesson", "code", "behavior", "memory").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.uppercase(), fontSize = 11.sp) }
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("prompt", "idea", "system").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.uppercase(), fontSize = 11.sp) }
                        )
                    }
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = !showArchived },
                        label = { Text("ARCHIVED", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            SparkEmptyTeachState(
                onAdd = { showAddDialog = true },
                hasEntries = entries.isNotEmpty()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { entry ->
                    TeachEntryCard(
                        entry = entry,
                        onOpen = { editingEntry = entry },
                        onPin = { onPinEntry(entry.id, !entry.isPinned) },
                        onArchive = { onArchiveEntry(entry.id, !entry.isArchived) },
                        onReview = { onReviewEntry(entry.id) },
                        onDelete = { onDeleteEntry(entry.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        TeachEntryEditorDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onSaveNew = { title, content, type ->
                onAddEntry(title, content, type)
                showAddDialog = false
            },
            onSaveExisting = { }
        )
    }

    editingEntry?.let { entry ->
        TeachEntryEditorDialog(
            initial = entry,
            onDismiss = { editingEntry = null },
            onSaveNew = { _, _, _ -> },
            onSaveExisting = { updated ->
                onUpdateEntry(updated)
                editingEntry = null
            }
        )
    }
}

@Composable
fun TeachStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1530)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = Color(0xFFCCCCCC), fontSize = 11.sp)
        }
    }
}

@Composable
fun SparkEmptyTeachState(onAdd: () -> Unit, hasEntries: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1530)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFF6EC7),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (hasEntries) "No matching lessons found." else "Spark Baby is ready to learn.",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (hasEntries) "Try changing search or filters." else "Add lessons, code snippets, memories, behaviors, prompts, and ideas.",
                color = Color(0xFFCCCCCC),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAdd) {
                Text("Teach Spark Baby")
            }
        }
    }
}

@Composable
fun TeachEntryCard(
    entry: TeachGrowEntry,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onReview: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = when (entry.type.lowercase()) {
        "lesson" -> Color(0xFF00E5FF)
        "code" -> Color(0xFF00FF9C)
        "behavior" -> Color(0xFFFF6EC7)
        "memory" -> Color(0xFF9C7BFF)
        "prompt" -> Color(0xFFFFD166)
        "idea" -> Color(0xFFFF9F1C)
        "system" -> Color(0xFFFF5555)
        else -> Color(0xFFCCCCCC)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isArchived) Color(0xFF171320) else Color(0xFF1A1530)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.isPinned) {
                    Icon(Icons.Default.Star, contentDescription = "Pinned", tint = Color(0xFFFFD166), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }

                Text(
                    entry.title,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Surface(
                    color = typeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        entry.type.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = typeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (entry.summary.isNotBlank()) {
                Text(entry.summary, color = Color(0xFFE8E0FF), fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
            }

            Text(
                entry.content.take(180) + if (entry.content.length > 180) "..." else "",
                color = if (entry.isCode) Color(0xFFB9FFD9) else Color(0xFFCCCCCC),
                fontSize = 13.sp
            )

            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.take(4).forEach { tag ->
                        Surface(
                            color = Color(0xFF2A1F4A),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                "#$tag",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = Color(0xFF9C7BFF),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Mastery ${entry.mastery}/3 • Reviews ${entry.reviewCount} • ${formatTeachDate(entry.updatedAt)}",
                    modifier = Modifier.weight(1f),
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                IconButton(onClick = onReview) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Review", tint = Color(0xFF00FF9C))
                }
                IconButton(onClick = onPin) {
                    Icon(Icons.Default.Star, contentDescription = "Pin", tint = if (entry.isPinned) Color(0xFFFFD166) else Color.Gray)
                }
                IconButton(onClick = onArchive) {
                    Icon(Icons.Default.Archive, contentDescription = "Archive", tint = Color(0xFF9C7BFF))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5555))
                }
            }
        }
    }
}

@Composable
fun TeachEntryEditorDialog(
    initial: TeachGrowEntry?,
    onDismiss: () -> Unit,
    onSaveNew: (String, String, String) -> Unit,
    onSaveExisting: (TeachGrowEntry) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var content by remember(initial?.id) { mutableStateOf(initial?.content ?: "") }
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: "lesson") }
    var summary by remember(initial?.id) { mutableStateOf(initial?.summary ?: "") }
    var tagsText by remember(initial?.id) { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    var priority by remember(initial?.id) { mutableStateOf(initial?.priority ?: 2) }

    val typeOptions = listOf("lesson", "code", "behavior", "memory", "prompt", "idea", "system")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "Teach Spark Baby" else "Edit Lesson")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 520.dp)
            ) {
                item {
                    Text(
                        "Saved locally. Code is stored as text only and never executed.",
                        color = Color(0xFF9C7BFF),
                        fontSize = 12.sp
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = { Text("Short summary") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(if (type == "code") "Code snippet / notes" else "Lesson content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }

                item {
                    Text("Type", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        typeOptions.take(4).forEach { option ->
                            FilterChip(
                                selected = type == option,
                                onClick = { type = option },
                                label = { Text(option, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        typeOptions.drop(4).forEach { option ->
                            FilterChip(
                                selected = type == option,
                                onClick = { type = option },
                                label = { Text(option, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        label = { Text("Tags, comma separated") },
                        placeholder = { Text("kotlin, avatar, command") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Priority", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "Low", 2 to "Normal", 3 to "High").forEach { (value, label) ->
                            FilterChip(
                                selected = priority == value,
                                onClick = { priority = value },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                item {
                    TemplateButtons(
                        onTemplate = { templateTitle, templateContent, templateType ->
                            if (title.isBlank()) title = templateTitle
                            if (content.isBlank()) content = templateContent
                            type = templateType
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTags = tagsText
                        .split(",")
                        .map { it.trim().lowercase() }
                        .filter { it.isNotBlank() }
                        .distinct()

                    if (title.isNotBlank() && content.isNotBlank()) {
                        if (initial == null) {
                            onSaveNew(title.trim(), content.trim(), type)
                        } else {
                            onSaveExisting(
                                initial.copy(
                                    title = title.trim(),
                                    content = content.trim(),
                                    type = type.trim(),
                                    summary = summary.trim(),
                                    tags = cleanTags,
                                    priority = priority,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            ) {
                Text(if (initial == null) "Save Lesson" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TemplateButtons(
    onTemplate: (String, String, String) -> Unit
) {
    Column {
        Text("Quick templates", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(
                onClick = {
                    onTemplate(
                        "Kotlin Lesson",
                        "Concept:\n\nWhy it matters:\n\nExample:\n\nMistakes to avoid:\n\nSpark Baby behavior idea:",
                        "lesson"
                    )
                },
                label = { Text("Lesson") }
            )
            AssistChip(
                onClick = {
                    onTemplate(
                        "Safe Code Snippet",
                        "// Paste code here as text only.\n// Purpose:\n// Inputs:\n// Output:\n// Safety notes:",
                        "code"
                    )
                },
                label = { Text("Code") }
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(
                onClick = {
                    onTemplate(
                        "Behavior Rule",
                        "When this happens:\n\nSpark Baby should:\n\nSpark Baby should not:\n\nMood to use:",
                        "behavior"
                    )
                },
                label = { Text("Behavior") }
            )
            AssistChip(
                onClick = {
                    onTemplate(
                        "Memory",
                        "Remember:\n\nWhy it matters:\n\nWhen to bring it up:",
                        "memory"
                    )
                },
                label = { Text("Memory") }
            )
        }
    }
}

fun formatTeachDate(timestamp: Long): String {
    return try {
        java.text.SimpleDateFormat("MMM dd, h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}

@Composable
fun SettingsScreen(
    isOwnerMode: Boolean,
    onToggleOwnerMode: () -> Unit,
    overlayVisible: Boolean,
    onToggleOverlay: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0B1A)).padding(20.dp)) {
        Text("Settings & Privacy", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(20.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1530))) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Owner Mode", modifier = Modifier.weight(1f), color = Color.White, fontSize = 18.sp)
                    Switch(checked = isOwnerMode, onCheckedChange = { onToggleOwnerMode() })
                }
                Text("Gives Spark Baby approved control over apps, timers, and settings panels. Every action shows confirmation.", color = Color.Gray, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1530))) {
            Column(Modifier.padding(16.dp)) {
                Text("Floating Overlay (Spark Bubble)", color = Color.White, fontSize = 18.sp)
                Switch(checked = overlayVisible, onCheckedChange = { onToggleOverlay() })
                Text("Draggable fairy that stays above other apps. Long-press for free-roam wandering.", color = Color.Gray, fontSize = 13.sp)
                if (!overlayVisible) {
                    Button(onClick = onRequestOverlay, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Request Overlay Permission")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("SparkX FairyOS v7 — Privacy-first AI Launcher. All local by default. Cloud AI optional & user-keyed.", color = Color.Gray, fontSize = 12.sp)
    }
}
