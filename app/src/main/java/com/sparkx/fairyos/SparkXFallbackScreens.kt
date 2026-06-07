package com.sparkx.fairyos

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class AppDrawerScreen(
    val label: String,
    val emoji: String
) {
    Home("Home", "✨"),
    TeachGrow("Teach & Grow", "🌱"),
    AIProvider("AI Providers", "🧠"),
    Settings("Settings", "⚙️")
}

@Composable
fun TeachGrowScreen(
    entries: List<Any> = emptyList(),
    onAddEntry: (Any) -> Unit = {},
    onUpdateEntry: (Any) -> Unit = {},
    onDeleteEntry: (String) -> Unit = {},
    onArchiveEntry: (String) -> Unit = {},
    onPinEntry: (String) -> Unit = {},
    onReviewEntry: (String) -> Unit = {},
    onExportJson: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        modifier = modifier,
        emoji = "🌱",
        title = "Teach & Grow",
        body = "Safe learning lab ready. Entries loaded: ${entries.size}"
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
        body = "Provider connection screen placeholder. Keep API keys private and user-controlled."
    )
}

@Composable
fun SettingsScreen(
    isOwnerMode: Boolean = false,
    onToggleOwnerMode: () -> Unit = {},
    overlayVisible: Boolean = false,
    onToggleOverlay: () -> Unit = {},
    onRequestOverlay: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "⚙️", color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Settings", color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Owner mode: $isOwnerMode · Overlay visible: $overlayVisible",
            color = Color(0xFFB8AEE8)
        )

        Spacer(modifier = Modifier.height(14.dp))

        QuickActionButton(
            title = "Toggle Owner Mode",
            subtitle = "Switch secure owner controls",
            emoji = "🔐",
            onClick = onToggleOwnerMode
        )

        Spacer(modifier = Modifier.height(8.dp))

        QuickActionButton(
            title = "Toggle Overlay",
            subtitle = "Show or hide Spark Baby",
            emoji = "✨",
            onClick = onToggleOverlay
        )

        Spacer(modifier = Modifier.height(8.dp))

        QuickActionButton(
            title = "Request Overlay Permission",
            subtitle = "Open Android overlay permission screen",
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
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF19142A)
        ),
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
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF19142A)
        ),
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
    DockButtonContent(
        label = label,
        emoji = emoji,
        selected = selected,
        modifier = modifier,
        onClick = onClick
    )
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
    DockButtonContent(
        label = label,
        emoji = emoji,
        selected = selected,
        modifier = modifier,
        onClick = onClick
    )
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = body, color = Color(0xFFB8AEE8))
    }
}