package com.sparkx.fairyos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawerScreen(onLaunchApp: (String) -> Unit) {
    PlaceholderScreen(
        emoji = "📱",
        title = "App Drawer",
        body = "Launch installed apps from here. This is a placeholder until the full app drawer is implemented."
    )
}

@Composable
fun TeachGrowScreen(
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        modifier = modifier,
        emoji = "🌱",
        title = "Teach & Grow",
        body = "Save lessons, code snippets, memories, behaviors, and safe upgrade notes for Spark Baby."
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
        body = "Connect local or external AI providers later. Keep keys private and user-controlled."
    )
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        modifier = modifier,
        emoji = "⚙️",
        title = "Settings",
        body = "Control overlay, voice, privacy, avatar behavior, and Spark Baby personality options."
    )
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
            Spacer(modifier = Modifier.width(12.dp))
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
        Text(text = emoji)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = body, color = Color(0xFFB8AEE8))
    }
}