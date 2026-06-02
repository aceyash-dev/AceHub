package com.ace.hub.ui.games

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ace.hub.ui.MainViewModel

@Composable
fun PlayScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val monitorData by viewModel.monitorData.collectAsState()
    
    val ramUsedPercentage = if (monitorData.ramTotalMB > 0) 
        (monitorData.ramUsedMB.toFloat() / monitorData.ramTotalMB.toFloat()) * 100 
        else 0f

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Play", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Pinned Game Section
        Text("Pinned", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            // Placeholder: Logic to retrieve and show the last played game
            Text("Last played game here", modifier = Modifier.padding(16.dp))
        }

        // Performance Card
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Performance", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("CPU Usage: ${monitorData.cpuUsage.toInt()}%")
                Text("RAM Usage: ${ramUsedPercentage.toInt()}% (${monitorData.ramUsedMB}MB / ${monitorData.ramTotalMB}MB)")
                
                Button(
                    onClick = { /* Implement real boost logic */ },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Boost")
                }
            }
        }

        // Add Button
        FilledIconButton(
            onClick = { /* Navigate to All Apps List */ },
            modifier = Modifier.align(Alignment.End).padding(top = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add game")
        }
    }
}
