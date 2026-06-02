package com.ace.hub.ui.overlay

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ace.hub.data.MonitorData

@Composable
fun OverlayContent(
    monitorData: MonitorData,
    onToggleMode: () -> Unit,
    isExpanded: Boolean
) {
    Surface(
        modifier = Modifier
            .animateContentSize()
            .padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(12.dp).clickable { onToggleMode() }) {
            if (isExpanded) {
                // Expanded View
                Text("Performance", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Simplified Graph representation
                LinearProgressIndicator(progress = { monitorData.cpuUsage / 100f }, modifier = Modifier.fillMaxWidth())
                
                Text("CPU: ${monitorData.cpuUsage.toInt()}%")
                Text("GPU: ${monitorData.gpuRenderer}")
                Text("Temp: ${monitorData.batteryTempCelsius.toInt()}°C")
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { /* Capture */ }) { Icon(Icons.Default.CameraAlt, "Capture") }
                    IconButton(onClick = { /* Record */ }) { Icon(Icons.Default.Videocam, "Record") }
                }
            } else {
                // Collapsed View
                Text("FPS: 60", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
