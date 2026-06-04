package com.ace.hub.ui.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(12.dp).clickable { onToggleMode() }) {
            // Minimized Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${monitorData.fps.toInt()} FPS", style = MaterialTheme.typography.labelMedium)
            }
            
            // Sliding Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Performance", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text("CPU: ${monitorData.cpuUsage.toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Text("GPU: ${monitorData.gpuUsage.toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Text("FPS: ${monitorData.fps.toInt()}", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(8.dp))
                    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                        val path = Path()
                        val width = size.width
                        val height = size.height
                        val points = monitorData.fpsHistoryList
                        if (points.isNotEmpty()) {
                            val step = width / (points.size - 1)
                            points.forEachIndexed { index, value ->
                                val x = index * step
                                val y = height - (value / 120f * height)
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, color = Color.Cyan, style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                }
            }
        }
    }
}
