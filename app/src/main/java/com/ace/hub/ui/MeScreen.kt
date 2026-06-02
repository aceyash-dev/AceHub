package com.ace.hub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MeScreen() {
    var darkMode by remember { mutableStateOf(true) }
    var showStats by remember { mutableStateOf(true) }
    var autoBoost by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Personalization", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Toggles
        SwitchRow("Dark Mode", darkMode) { darkMode = it }
        SwitchRow("Show Overlay Stats", showStats) { showStats = it }
        SwitchRow("Auto-Boost on Launch", autoBoost) { autoBoost = it }

        Spacer(modifier = Modifier.height(24.dp))
        Text("About AceHub", style = MaterialTheme.typography.titleLarge)
        Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
