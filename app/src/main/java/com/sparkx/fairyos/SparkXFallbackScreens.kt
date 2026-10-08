package com.sparkx.fairyos

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sparkx.fairyos.domain.memory.TeachGrowEntry

@Composable
fun AppDrawerScreen(
    onLaunchApp: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        modifier = modifier,
        emoji = "📱",
        title = "App Drawer",
        body = "Quick app launcher shell is active."
    )
}

@Composable
fun TeachGrowScreen(
    entries: List<TeachGrowEntry> = emptyList(),
    onAddEntry: (String, String, String) -> Unit = { _, _, _ -> },
    onUpdateEntry: (TeachGrowEntry) -> Unit = {},
    onDeleteEntry: (String) -> Unit = {},
    onArchiveEntry: (String, Boolean) -> Unit = { _, _ -> },
    onPinEntry: (String, Boolean) -> Unit = { _, _ -> },
    onReviewEntry: (String) -> Unit = {},
    onExportJson: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        modifier = modifier,
        emoji = "🌱",
        title = "Teach & Grow",
        body = "Learning lab shell active. Entries loaded: ${entries.size}"
    )
}

@Composable
fun AIProviderScreen(
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        modifier = modifier,
        emoji = "🧠",
        title = "AI Providers",
        body = "Provider connection shell active. Private keys stay user-controlled."
    )
}

@Composable
fun SettingsScreen(
    isOwnerMode: Boolean = false,
    onToggleOwnerMode: () -> Unit = {},
    overlayVisible: Boolean = false,
    onToggleOverlay: () -> Unit = {},
    onRequestOverlay: () -> Unit = {},
    coreOpen: Boolean = false,
    onSubmitSeal: (String) -> Unit = {},
    onCloseCore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var seal by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (coreOpen || isOwnerMode) {
                "Owner Mode Active — confirm before an action."
            } else {
                "Safe Companion · Overlay visible: $overlayVisible"
            },
            color = Color(0xFFF3E2B0)
        )

        Spacer(Modifier.height(14.dp))

        if (coreOpen) {
            Button(onClick = onCloseCore) { Text("Close core") }
        } else {
            OutlinedTextField(
                value = seal,
                onValueChange = { seal = it },
                label = { Text("Core seal") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val entered = seal
                seal = ""
                onSubmitSeal(entered)
            }) { Text("Open core") }
        }

        Spacer(Modifier.height(8.dp))

        QuickActionButton(
            title = "Overlay",
            subtitle = "Show or hide Spark Baby",
            emoji = "✨",
            onClick = onToggleOverlay
        )

        Spacer(Modifier.height(8.dp))

        QuickActionButton(
            title = "Overlay Permission",
            subtitle = "Open Android permission screen",
            emoji = "📲",
            onClick = onRequestOverlay
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String = "",
    emoji: String = "✨",
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF19142A)),
        border = BorderStroke(1.dp, Color(0x5539D7FF))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = title, color = Color.White)
                if (subtitle.isNotBlank()) {
                    Text(text = subtitle, color = Color(0xFFB8AEE8))
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF19142A)),
        border = BorderStroke(1.dp, Color(0x5539D7FF))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF39D7FF)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = title, color = Color.White)
                if (subtitle.isNotBlank()) {
                    Text(text = subtitle, color = Color(0xFFB8AEE8))
                }
            }
        }
    }
}

@Composable
fun DockButton(
    label: String,
    emoji: String = "✨",
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    DockButtonContent(label, emoji, selected, modifier, onClick)
}

@Composable
fun DockButton(
    label: String,
    emoji: String = "✨",
    context: Context,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    DockButtonContent(label, emoji, selected, modifier, onClick)
}

@Composable
private fun DockButtonContent(
    label: String,
    emoji: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val bg = if (selected) Color(0xFF2C2350) else Color(0xFF151022)
    val border = if (selected) Color(0xFF39D7FF) else Color(0x4439D7FF)

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji)
            Text(text = label, color = Color.White)
        }
    }
}

@Composable
private fun PlaceholderScreen(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    body: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text(text = title, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(text = body, color = Color(0xFFB8AEE8))
    }
}