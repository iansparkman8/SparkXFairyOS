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

        setContent {
            SparkXFairyOSTheme {
                SparkXApp(
                    currentMood = currentMood,
                    isSpeaking = isSpeaking,
                    isOwnerMode = isOwnerMode,
                    overlayVisible = overlayVisible,
                    commandInput = commandInput,
                    onCommandInputChange = { commandInput = it },
                    onSendCommand = { processTextCommand(it) },
                    onMicClick = { toggleListening() },
                    onToggleOwnerMode = { toggleOwnerMode() },
                    onToggleOverlay = { if (overlayVisible) hideOverlay() else showOverlay() },
                    teachEntries = teachEntries,
                    onAddTeachEntry = { title, content, type ->
                        lifecycleScope.launch {
                            val entry = TeachGrowEntry(title = title, content = content, type = type)
                            repo.addEntry(entry)
                        }
                    },
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
            composable("teach") { TeachGrowScreen(entries = teachEntries, onAddEntry = onAddTeachEntry) }
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

@Composable
fun TeachGrowScreen(
    entries: List<TeachGrowEntry>,
    onAddEntry: (String, String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("memory") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0B1A)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Teach & Grow", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF00E5FF))
            Button(onClick = { showAddDialog = true }) { Text("+") }
        }
        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Text("No entries yet. Add lessons, code snippets, or memories.", color = Color.Gray)
        } else {
            LazyColumn {
                items(entries) { entry ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1530))) {
                        Column(Modifier.padding(12.dp)) {
                            Text(entry.title, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(entry.content.take(120) + if (entry.content.length > 120) "..." else "", color = Color(0xFFCCCCCC), fontSize = 13.sp)
                            Text("${entry.type} • ${java.text.SimpleDateFormat("MMM dd").format(java.util.Date(entry.timestamp))}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Entry") },
            text = {
                Column {
                    OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Title") })
                    OutlinedTextField(value = newContent, onValueChange = { newContent = it }, label = { Text("Content") }, modifier = Modifier.height(120.dp))
                    Row { listOf("lesson","code","behavior","memory").forEach { t -> FilterChip(selected = newType == t, onClick = { newType = t }, label = { Text(t) }) } }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                        onAddEntry(newTitle, newContent, newType)
                        newTitle = ""; newContent = ""; showAddDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AIProviderScreen() {
    val context = LocalContext.current
    val client = remember { com.sparkx.fairyos.domain.ai.SparkAIClient(context) }
    var selected by remember { mutableStateOf(com.sparkx.fairyos.domain.ai.SparkAIClient.Provider.OPENAI) }
    var keyInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0B1A)).padding(20.dp)) {
        Text("AI Provider Console", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF00E5FF))
        Text("Add your own keys. Stored securely. No calls without your enable.", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        com.sparkx.fairyos.domain.ai.SparkAIClient.Provider.values().forEach { provider ->
            val hasKey = client.getApiKey(provider) != null
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selected = provider },
                colors = CardDefaults.cardColors(containerColor = if (selected == provider) Color(0xFF2A1F4A) else Color(0xFF1A1530))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(provider.name, modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Medium)
                    if (hasKey) Text("Configured", color = Color(0xFF00E5FF), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Paste ${selected.name} API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            if (keyInput.isNotBlank()) {
                client.saveApiKey(selected, keyInput.trim())
                keyInput = ""
            }
        }, modifier = Modifier.align(Alignment.End)) {
            Text("Save Key Securely")
        }

        Text("Cloud features activate only when key is present. All processing respects your privacy.", fontSize = 12.sp, color = Color.Gray)
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
