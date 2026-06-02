package com.ace.hub.ui.device

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ace.hub.data.DeviceInfo
import com.ace.hub.ui.components.GlassCard
import com.ace.hub.ui.components.MetricRow

@Composable
fun DeviceInfoScreen(
    deviceInfo: DeviceInfo,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Device Info",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Device Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = "Device",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "  Device",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow(label = "Device Name", value = deviceInfo.deviceName)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            MetricRow(label = "Manufacturer", value = deviceInfo.manufacturer)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            MetricRow(label = "Model", value = deviceInfo.model)
        }

        // Software Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Android,
                    contentDescription = "Software",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "  Software",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow(label = "Android Version", value = deviceInfo.androidVersion)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            MetricRow(label = "API Level", value = deviceInfo.apiLevel.toString())
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            MetricRow(label = "Security Patch", value = Build.VERSION.SECURITY_PATCH)
        }

        // Processor Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeveloperBoard,
                    contentDescription = "Processor",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "  Processor",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow(label = "Chipset", value = deviceInfo.processor)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            MetricRow(label = "CPU Cores", value = "${deviceInfo.coreCount}")
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            MetricRow(
                label = "Total RAM",
                value = "${String.format("%.1f", deviceInfo.totalRamMB / 1024f)} GB"
            )
        }

        // GPU Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = "GPU",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "  GPU",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow(label = "Renderer", value = deviceInfo.gpuRenderer)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            MetricRow(label = "Vendor", value = deviceInfo.gpuVendor)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            MetricRow(label = "GL Version", value = deviceInfo.gpuVersion)
        }

        // Display Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.ScreenRotation,
                    contentDescription = "Display",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "  Display",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow(label = "Resolution", value = deviceInfo.screenResolution)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            MetricRow(label = "Density", value = "${deviceInfo.screenDensity} dpi")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
