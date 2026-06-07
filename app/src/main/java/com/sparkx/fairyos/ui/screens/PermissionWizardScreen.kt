package com.sparkx.fairyos.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionWizardScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B1A))
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            "Permission Wizard",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Bold
        )
        Text(
            "Spark Baby needs these permissions to be a visible companion. Each one is explained below.",
            color = Color(0xFFCCCCCC),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        // Overlay Permission
        PermissionCard(
            title = "Overlay Permission (SYSTEM_ALERT_WINDOW)",
            status = "Needed for floating bubble",
            purpose = "Lets Spark Baby float above other apps as a visible companion bubble.",
            safety = "Spark Baby cannot see your screen through this permission. It only lets her draw her own bubble on top.",
            onAction = {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                context.startActivity(intent)
            }
        )

        // Microphone
        PermissionCard(
            title = "Microphone Permission (RECORD_AUDIO)",
            status = "Needed for voice commands",
            purpose = "Lets Spark Baby listen when you tap the mic or use the voice button.",
            safety = "Spark Baby only listens when you start voice mode. No background recording.",
            onAction = { /* Runtime permission handled in MainActivity */ }
        )

        // Notifications
        PermissionCard(
            title = "Notifications Permission (POST_NOTIFICATIONS)",
            status = "Needed for overlay controls",
            purpose = "Keeps the overlay service visible and controllable (Android 13+).",
            safety = "Used for overlay controls like Open, Hide, and Stop.",
            onAction = { /* Runtime permission */ }
        )

        // Default Home
        PermissionCard(
            title = "Default Home App",
            status = "Recommended for full companion feel",
            purpose = "Makes SparkX FairyOS your launcher so Spark Baby lives on your home screen.",
            safety = "You can switch back to your original launcher anytime in Android settings.",
            onAction = {
                try {
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback handled in UI
                }
            }
        )

        // Battery Optimization
        PermissionCard(
            title = "Battery Optimization",
            status = "Recommended",
            purpose = "Helps Spark Baby stay alive as an overlay companion.",
            safety = "This only helps Android avoid stopping the visible companion service. It does not give Spark Baby hidden control.",
            onAction = {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            }
        )

        Spacer(Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1530)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Spark Baby’s Privacy Promise", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "• I stay visible.\n• I ask before powerful actions.\n• I do not secretly read your screen.\n• I do not take screenshots.\n• I do not record in the background.\n• I do not execute saved code.\n• I only use cloud AI if you enable it.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
        ) {
            Text("Finish Setup", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun PermissionCard(
    title: String,
    status: String,
    purpose: String,
    safety: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1530)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Surface(
                    color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                ) {
                    Text(status, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), color = Color(0xFF00E5FF), fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Purpose: $purpose", color = Color(0xFFCCCCCC), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("Safety: $safety", color = Color(0xFF9C7BFF), fontSize = 12.sp)

            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text("Open Settings")
            }
        }
    }
}