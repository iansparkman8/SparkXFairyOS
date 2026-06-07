package com.sparkx.fairyos.ui.screens

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
fun TermsScreen(
    onAgree: () -> Unit,
    onSafeLocal: () -> Unit,
    onExit: () -> Unit
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
            "SparkX FairyOS Terms & Conditions",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Bold
        )
        Text("Version 1.0", color = Color.Gray, fontSize = 13.sp)

        Spacer(Modifier.height(20.dp))

        Text(
            "Welcome to SparkX FairyOS.",
            color = Color.White,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "SparkX FairyOS is a personal Android launcher and visible AI companion experience featuring Spark Baby, a holographic fairy companion.",
            color = Color(0xFFCCCCCC),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(20.dp))

        Text("By using this app, you understand and agree to the following:", color = Color.White, fontSize = 15.sp)

        Spacer(Modifier.height(16.dp))

        val sections = listOf(
            "1. Companion Purpose" to "Spark Baby is designed to act as a visible phone companion, launcher assistant, learning notebook, voice interface, and optional AI chat interface. Spark Baby is not a hidden surveillance tool, not a medical device, not a legal advisor, and not an emergency service.",
            "2. Permissions" to "SparkX FairyOS may request permissions for overlay display, microphone input, notifications, launcher/home functionality, and optional internet access for user-enabled AI providers. Each permission is used only for the feature described in the permission screen.",
            "3. Overlay Behavior" to "If you grant overlay permission, Spark Baby may appear as a floating bubble above other apps. The overlay is visible. Overlay permission does not give Spark Baby the ability to secretly read your screen.",
            "4. Microphone Use" to "Microphone access is used for voice commands when you start listening. SparkX FairyOS does not intentionally record audio in the background without user action.",
            "5. Local Memory" to "Teach & Grow entries, lessons, self-upgrade notes, bugs, features, and memories are stored locally on your device unless you choose to export or send content to an enabled AI provider.",
            "6. AI Providers" to "Cloud AI features are optional. SparkX FairyOS does not include hardcoded API keys. If you add your own provider key, messages you send to that provider may leave your device and be processed under that provider’s terms and privacy policy.",
            "7. No Hidden Screen Reading" to "SparkX FairyOS does not use hidden screen capture, screenshot APIs, or Accessibility Service screen scraping as part of its safe companion mode.",
            "8. No Auto-Execution" to "Code snippets or commands saved in Teach & Grow are stored as text. SparkX FairyOS does not automatically execute saved code.",
            "9. Owner Mode" to "Owner Mode may expose advanced controls. Advanced actions must remain visible and confirmation-based. You are responsible for actions you explicitly approve.",
            "10. Accuracy" to "Spark Baby may make mistakes. AI-generated or locally generated responses should be reviewed by you. Do not rely on Spark Baby as your only source for medical, legal, financial, safety, or emergency decisions.",
            "11. Device Performance" to "Overlay, animation, voice, and launcher features may affect battery life, performance, or device behavior. You can disable overlay mode or switch launchers at any time.",
            "12. User Responsibility" to "You are responsible for how you use SparkX FairyOS, what information you enter, and which optional AI providers you connect.",
            "13. Changes" to "These terms may be updated as the app evolves. If terms change significantly, the app should ask you to review and accept the new version.",
            "14. Contact / Project Ownership" to "SparkX FairyOS is an evolving personal project. The app should clearly identify the project owner, repository, and version in the About screen."
        )

        sections.forEach { (title, body) ->
            Text(title, color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(body, color = Color(0xFFCCCCCC), fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(20.dp))

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAgree,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("I Understand and Agree", color = Color.Black)
            }

            OutlinedButton(
                onClick = onSafeLocal,
                modifier = Modifier.weight(1f)
            ) {
                Text("Use Safe Local Mode")
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = onExit,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Exit App", color = Color(0xFFFF6E6E))
        }

        Spacer(Modifier.height(40.dp))
    }
}