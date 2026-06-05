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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    useSystemTheme: Boolean,
    onUseSystemThemeChanged: (Boolean) -> Unit,
    customSeedColor: Int,
    onCustomSeedColorChanged: (Int) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

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
        
        // Material You Style Toggle
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic Color",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Use system wallpaper colors",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useSystemTheme,
                    onCheckedChange = onUseSystemThemeChanged,
                    thumbContent = if (useSystemTheme) {
                        {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.ColorLens,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Custom Theme Color Picker
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showColorPicker = true },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(customSeedColor))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Theme Color",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Customize the primary accent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (showColorPicker) {
            ColorPickerDial(
                initialColor = Color(customSeedColor),
                onColorSelected = { 
                    onCustomSeedColorChanged(it.toArgb())
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }
    }
}

@Composable
fun ColorPickerDial(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hexText by remember { mutableStateOf(String.format("#%06X", (0xFFFFFF and initialColor.toArgb()))) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pick a color",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                // Color Wheel Placeholder / Simple Grid
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onColorSelected(color) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { 
                        hexText = it
                        if (it.startsWith("#") && (it.length == 7 || it.length == 9)) {
                            try {
                                onColorSelected(Color(android.graphics.Color.parseColor(it)))
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text("Hex Color") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
