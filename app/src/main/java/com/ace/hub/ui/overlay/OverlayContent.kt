package com.ace.hub.ui.overlay

import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ace.hub.data.MonitorData
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun OverlayContent(
    monitorData: MonitorData,
    appIcon: Drawable? = null,
    remainingSeconds: Int = 0,
    onCloseOverlay: () -> Unit = {},
    onGoToApp: () -> Unit = {}
) {
    val purplePrimary = Color(0xFFD0BCFF)
    val purpleContainer = Color(0xFF1D1B20)
    
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .wrapContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = purpleContainer.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, purplePrimary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // App Icon or Default
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(purplePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        androidx.compose.foundation.Image(
                            painter = rememberDrawablePainter(appIcon),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.SportsEsports,
                            contentDescription = null,
                            tint = purplePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // FPS Display
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${monitorData.fps.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "FPS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = purplePrimary,
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                if (remainingSeconds > 0) {
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(color = purplePrimary.copy(alpha = 0.2f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Native XY Graph (Mini)
                    MiniFpsGraph(monitorData.fpsHistoryList, purplePrimary)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Stats Row
                    val ramUsagePercent = if (monitorData.ramTotalMB > 0) 
                        (monitorData.ramUsedMB * 100 / monitorData.ramTotalMB).toInt() 
                    else 0
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Temperature", "${monitorData.batteryTempCelsius.toInt()}°C", purplePrimary)
                        StatItem("RAM", "$ramUsagePercent%", purplePrimary)
                        StatItem("CPU", "${monitorData.cpuUsage.toInt()}%", purplePrimary)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onGoToApp,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = purplePrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, purplePrimary.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Go Back to Hub", fontSize = 10.sp)
                        }
                        
                        Button(
                            onClick = onCloseOverlay,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Close", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Achievements(Beta)",
                        style = MaterialTheme.typography.labelSmall,
                        color = purplePrimary.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun MiniFpsGraph(history: List<Float>, color: Color) {
    val maxFps = 120f
    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = width / (if (history.size > 1) history.size - 1 else 1)

            // Draw 60fps line
            val y60 = height - (60f / maxFps) * height
            drawLine(color.copy(alpha = 0.2f), Offset(0f, y60), Offset(width, y60), 1.dp.toPx())

            if (history.isNotEmpty()) {
                val path = Path()
                history.takeLast(30).forEachIndexed { i, fps ->
                    val x = i * (width / 29f)
                    val y = height - (fps / maxFps) * height
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(path = path, color = color, style = Stroke(width = 1.5.dp.toPx()))
                
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.2f), Color.Transparent))
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Text(value, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
