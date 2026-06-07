package com.ace.hub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    username: String,
    onUsernameChanged: (String) -> Unit,
    useSystemTheme: Boolean,
    onUseSystemThemeChanged: (Boolean) -> Unit,
    isUsageAnalyticsEnabled: Boolean,
    onUsageAnalyticsEnabledChanged: (Boolean) -> Unit,
    hasUsagePermission: Boolean,
    isOverlayEnabled: Boolean,
    onOverlayEnabledChanged: (Boolean) -> Unit,
    isAutoBoostEnabled: Boolean,
    onAutoBoostEnabledChanged: (Boolean) -> Unit
) {
    var showUsernameDialog by remember { mutableStateOf(false) }
    var tempUsername by remember { mutableStateOf(username) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Profile",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCard(onClick = { showUsernameDialog = true }) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Username",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (username.isBlank()) "User" else username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingsToggle(
            title = "Follow System Theme",
            description = "Sync app appearance with Android settings",
            checked = useSystemTheme,
            onCheckedChange = onUseSystemThemeChanged
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Features",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        SettingsToggle(
            title = "Usage Analytics",
            description = if (hasUsagePermission) "Track game play time and performance" else "Permission Required",
            checked = isUsageAnalyticsEnabled,
            onCheckedChange = onUsageAnalyticsEnabledChanged,
            error = !hasUsagePermission
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            title = "Floating Overlay",
            description = "Show FPS and stats while gaming",
            checked = isOverlayEnabled,
            onCheckedChange = onOverlayEnabledChanged
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            title = "Auto Boost",
            description = "Optimize device performance on launch",
            checked = isAutoBoostEnabled,
            onCheckedChange = onAutoBoostEnabledChanged
        )

        Spacer(modifier = Modifier.height(12.dp))

        var mockBatteryStats by remember { mutableStateOf(true) }
        SettingsToggle(
            title = "Show Battery Stats",
            description = "Monitor battery temperature and level",
            checked = mockBatteryStats,
            onCheckedChange = { mockBatteryStats = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        var mockVibration by remember { mutableStateOf(false) }
        SettingsToggle(
            title = "Vibration on Launch",
            description = "Haptic feedback when starting a game",
            checked = mockVibration,
            onCheckedChange = { mockVibration = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Game Mode Settings",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        var mockDND by remember { mutableStateOf(false) }
        SettingsToggle(
            title = "Auto DND",
            description = "Silence notifications while gaming",
            checked = mockDND,
            onCheckedChange = { mockDND = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        var mockBrightness by remember { mutableStateOf(false) }
        SettingsToggle(
            title = "Brightness Lock",
            description = "Prevent auto-brightness changes in game",
            checked = mockBrightness,
            onCheckedChange = { mockBrightness = it }
        )
        
        if (showUsernameDialog) {
            AlertDialog(
                onDismissRequest = { showUsernameDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        onUsernameChanged(tempUsername)
                        showUsernameDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUsernameDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Edit Username") },
                text = {
                    Column {
                        Text(
                            "Enter a new display name for your profile.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = tempUsername,
                            onValueChange = { tempUsername = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        content = content
    )
}

@Composable
fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    error: Boolean = false
) {
    SettingsCard {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                },
                colors = if (error) SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.error,
                    checkedTrackColor = MaterialTheme.colorScheme.errorContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.error,
                    uncheckedBorderColor = MaterialTheme.colorScheme.error
                ) else SwitchDefaults.colors()
            )
        }
    }
}
