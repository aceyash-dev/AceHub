package com.ace.hub.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ace.hub.data.MonitorData
import com.ace.hub.theme.*
import com.ace.hub.ui.components.*

@Composable
fun DashboardScreen(
    monitorData: MonitorData,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Performance",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Real-time Graph (Example integration)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("CPU Load History", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            RealTimeGraph(dataPoints = monitorData.cpuHistoryList) // Ensure this list exists in MonitorData
        }

        // CPU & RAM Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val cpuColor by animateColorAsState(
                targetValue = if (monitorData.cpuUsage > 80f) StatusCritical else MaterialTheme.colorScheme.primary,
                animationSpec = tween(500), label = "cpuColor"
            )

            ArcGauge(value = monitorData.cpuUsage / 100f, label = "CPU", valueText = "${monitorData.cpuUsage.toInt()}%", color = cpuColor, modifier = Modifier.weight(1f))
            ArcGauge(value = monitorData.ramUsedMB.toFloat() / monitorData.ramTotalMB.coerceAtLeast(1L), label = "RAM", valueText = "${(monitorData.ramUsedMB / 1024).toInt()}GB", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
        }

        // Battery Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = BatteryColor)
                Text("  Battery", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { monitorData.batteryLevel / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (monitorData.batteryLevel < 20) StatusCritical else BatteryColor,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Level: ${monitorData.batteryLevel}% | Temp: ${monitorData.batteryTempCelsius}°C", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
