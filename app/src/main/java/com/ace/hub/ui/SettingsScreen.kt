package com.ace.hub.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ace.hub.R

@Composable
fun SettingsScreen(
    useSystemTheme: Boolean,
    onUseSystemThemeChanged: (Boolean) -> Unit,
    isOverlayEnabled: Boolean,
    onOverlayEnabledChanged: (Boolean) -> Unit,
    isAutoBoostEnabled: Boolean,
    onAutoBoostEnabledChanged: (Boolean) -> Unit,
    autoDnd: Boolean,
    onAutoDndChanged: (Boolean) -> Unit,
    brightnessLock: Boolean,
    onBrightnessLockChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
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
            text = "Performance & Stats",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        SettingsToggle(
            title = "Floating Overlay",
            description = "Show FPS and stats while gaming",
            checked = isOverlayEnabled,
            onCheckedChange = onOverlayEnabledChanged
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            title = "Auto Boost",
            description = "Kill background processes on game launch",
            checked = isAutoBoostEnabled,
            onCheckedChange = onAutoBoostEnabledChanged
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Game Mode Settings",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsToggle(
            title = "Auto DND",
            description = "Silence notifications while gaming",
            checked = autoDnd,
            onCheckedChange = onAutoDndChanged
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsToggle(
            title = "Brightness Lock",
            description = "Prevent auto-brightness changes in game",
            checked = brightnessLock,
            onCheckedChange = onBrightnessLockChanged
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SettingsCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
    onCheckedChange: (Boolean) -> Unit
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
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                }
            )
        }
    }
}
