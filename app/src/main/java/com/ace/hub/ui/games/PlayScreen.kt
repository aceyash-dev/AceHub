package com.ace.hub.ui.games

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ace.hub.data.GameApp
import com.ace.hub.ui.MainViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun PlayScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    username: String
) {
    val context = LocalContext.current
    val monitorData by viewModel.monitorData.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    var pinnedGames by remember { mutableStateOf<List<GameApp>>(emptyList()) }
    var showAppPicker by remember { mutableStateOf(false) }

    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("$greeting, $username!", style = MaterialTheme.typography.headlineLarge)
            
            // Recently Played / Pinned (LazyRow for scrolling)
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(pinnedGames) { game ->
                    Card(
                        modifier = Modifier.width(280.dp).animateContentSize(animationSpec = spring()),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = rememberDrawablePainter(drawable = game.icon),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(game.appName, style = MaterialTheme.typography.titleMedium)
                                Row {
                                    IconButton(onClick = {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        intent.data = Uri.parse("package:${game.packageName}")
                                        context.startActivity(intent)
                                    }) { Icon(Icons.Default.Info, "App Info") }
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${game.packageName}"))
                                        context.startActivity(intent)
                                    }) { Icon(Icons.Default.Store, "Store") }
                                    IconButton(onClick = { viewModel.launchGameWithOverlay(game.packageName) }) {
                                        Icon(Icons.Default.Launch, "Launch")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Performance Card
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Performance", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("CPU: ${monitorData.cpuUsage.toInt()}%")
                    Text("GPU: ${monitorData.gpuRenderer}")
                    
                    if (!android.app.AppOpsManager.MODE_ALLOWED.equals(
                        (context.getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager)
                            .checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
                    )) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }) {
                            Icon(Icons.Default.Settings, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Grant Usage Access")
                        }
                    }
                }
            }
            
            FilledIconButton(
                onClick = { showAppPicker = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Add, "Add game")
            }
        }
        
        if (showAppPicker) {
            AppPickerSheet(
                apps = allApps,
                onAppSelected = {
                    pinnedGames = pinnedGames + it
                    showAppPicker = false
                },
                onDismiss = { showAppPicker = false }
            )
        }
    }
}
