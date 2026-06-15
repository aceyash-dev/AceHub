package com.ace.hub.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ace.hub.ui.components.FpsXyGraph

@Composable
fun BootScreen(viewModel: MainViewModel) {
    val tasks by viewModel.bootTasks.collectAsState()
    val monitorData by viewModel.monitorData.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    
    val completedCount = tasks.count { it.completed || it.failed }
    val progress by animateFloatAsState(
        targetValue = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size,
        animationSpec = tween(500, easing = LinearOutSlowInEasing),
        label = "progress"
    )
    val percentLoaded = (progress * 100).toInt()

    val purpleColor = Color(0xFFD0BCFF)
    val darkBg = Color(0xFF0F0F1A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                "Booting the Hub...",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = purpleColor,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "INITIALIZING_CORE...",
                    style = MaterialTheme.typography.labelSmall,
                    color = purpleColor.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "[$percentLoaded%]",
                    style = MaterialTheme.typography.labelSmall,
                    color = purpleColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = purpleColor,
                trackColor = purpleColor.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Performance Overlay
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = purpleColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "SYSTEM_DIAGNOSTICS",
                                style = MaterialTheme.typography.labelLarge,
                                color = purpleColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!isExpanded) {
                            Text(
                                "${monitorData.fps.toInt()} FPS",
                                style = MaterialTheme.typography.bodySmall,
                                color = purpleColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(20.dp))
                        FpsXyGraph(history = monitorData.fpsHistoryList, color = purpleColor)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(tasks) { task ->
                    BootTaskRow(task, purpleColor)
                }
            }
        }
    }
}

@Composable
fun BootTaskRow(task: BootTask, baseColor: Color) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val stateColor = when {
                task.completed -> Color(0xFF4CAF50)
                task.failed -> Color(0xFFF44336)
                else -> baseColor.copy(alpha = 0.6f)
            }
            
            Text(
                if (task.completed) "[OK]" else if (task.failed) "[ER]" else "[..]",
                style = MaterialTheme.typography.bodyMedium,
                color = stateColor,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(48.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    task.name.uppercase().replace(" ", "_"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (task.completed || task.failed) stateColor else baseColor,
                    fontFamily = FontFamily.Monospace
                )
                if (task.message.isNotEmpty()) {
                    Text(
                        "> ${task.message}",
                        style = MaterialTheme.typography.labelSmall,
                        color = stateColor.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
