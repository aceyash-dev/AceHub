package com.ace.hub.ui.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ace.hub.data.MonitorData

@Composable
fun OverlayContent(
    monitorData: MonitorData,
    onToggleMode: () -> Unit,
    isExpanded: Boolean,
    isAutoBoostEnabled: Boolean,
    onAutoBoostToggle: (Boolean) -> Unit
) {
    val fpsColor = remember(monitorData.fps) {
        when {
            monitorData.fps >= 90 -> Color(0xFF4CAF50)
            monitorData.fps >= 60 -> Color(0xFF8BC34A)
            monitorData.fps >= 30 -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }
    }

    Surface(
        modifier = Modifier
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
            .padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp).copy(alpha = 0.92f),
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .clickable { onToggleMode() }
        ) {
            // Minimized Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = fpsColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, fpsColor.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = fpsColor
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${monitorData.fps.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = fpsColor,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = " FPS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = fpsColor.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Mini Sparkline Graph
                    FPSGraph(
                        points = monitorData.fpsHistoryList,
                        color = fpsColor,
                        modifier = Modifier.width(60.dp).height(12.dp).padding(top = 2.dp)
                    )
                }
            }
            
            // Sliding Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Metric Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricChip("CPU", "${monitorData.cpuUsage.toInt()}%", Modifier.weight(1f))
                        MetricChip("GPU", "${monitorData.gpuUsage.toInt()}%", Modifier.weight(1f))
                        MetricChip("RAM", "${monitorData.ramUsedMB}MB", Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto Boost", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("System priority mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isAutoBoostEnabled,
                            onCheckedChange = onAutoBoostToggle,
                            modifier = Modifier.scale(0.8f),
                            thumbContent = if (isAutoBoostEnabled) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else {
                                { Icon(Icons.Default.Close, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "metric_val"
            ) { targetValue ->
                Text(targetValue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun FPSGraph(points: List<Float>, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val maxFps = 120f
        val minFps = 0f
        val range = maxFps - minFps
        
        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, fps ->
            val x = index * (width / (points.size - 1))
            val y = height - ((fps.coerceIn(minFps, maxFps) - minFps) / range * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}
