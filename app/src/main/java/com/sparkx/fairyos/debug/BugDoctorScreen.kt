package com.sparkx.fairyos.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BugDoctorScreen(
    modifier: Modifier = Modifier
) {
    var errorText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SparkBugDoctor.BugFix?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Spark Bug Doctor",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = errorText,
            onValueChange = { errorText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            label = { Text("Paste first red error block") },
            placeholder = { Text("e.g. 'val' cannot be reassigned...") }
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                result = SparkBugDoctor.diagnose(errorText)
            }
        ) {
            Text("Diagnose")
        }

        Spacer(Modifier.height(16.dp))

        result?.let { fix ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF151A2E)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Problem: ${fix.title}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Cause: ${fix.cause}", color = Color(0xFFB8AEE8))
                    Spacer(Modifier.height(8.dp))
                    Text("Fix: ${fix.fix}", color = Color(0xFFB8AEE8))
                    Spacer(Modifier.height(12.dp))
                    Text("Example:", color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(fix.snippet, color = Color(0xFF7CF7D4), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}