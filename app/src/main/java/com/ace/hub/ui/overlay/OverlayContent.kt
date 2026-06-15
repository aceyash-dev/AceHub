package com.ace.hub.ui.overlay

import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
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

    val cardScale by animateFloatAsState(
        targetValue = if (expanded) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "CardScale"
    )

    Card(
        modifier = Modifier
            .wrapContentSize()
            .padding(2.dp)
            .graphicsLayer(scaleX = cardScale, scaleY = cardScale)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = purpleContainer.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, purplePrimary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(purplePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        androidx.compose.foundation.Image(
                            painter = rememberDrawablePainter(appIcon),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.SportsEsports,
                            contentDescription = null,
                            tint = purplePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(
                        targetState = monitorData.fps.toInt(),
                        label = "fps"
                    ) { fps ->
                        Text(
                            text = "$fps",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = " FPS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    )
                }

                if (remainingSeconds > 0) {
                    Text(
                        text = String.format(
                            "%02d:%02d",
                            remainingSeconds / 60,
                            remainingSeconds % 60
                        ),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (expanded) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onGoToApp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Launch, null, tint = purplePrimary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onCloseOverlay,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 8.dp)
                        .width(220.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(color = purplePrimary.copy(alpha = 0.2f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Boot Screen Style Graph
                    Box(modifier = Modifier.height(80.dp).fillMaxWidth().padding(horizontal = 8.dp)) {
                        OverlayFpsGraph(monitorData.fpsHistoryList, purplePrimary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats Row
                    val ramUsagePercent = if (monitorData.ramTotalMB > 0)
                        (monitorData.ramUsedMB * 100 / monitorData.ramTotalMB).toInt()
                    else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("TEMP", "${monitorData.batteryTempCelsius.toInt()}°C", purplePrimary)
                        StatItem("RAM", "$ramUsagePercent%", purplePrimary)
                        StatItem("CPU", "${monitorData.cpuUsage.toInt()}%", purplePrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayFpsGraph(history: List<Float>, color: Color) {
    val maxFps = 120f
    val milestones = listOf(30f, 60f, 90f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val spacing = width / 29f // showing last 30 frames

        // Draw Milestones
        milestones.forEach { fps ->
            val y = height - (fps / maxFps) * height
            drawLine(
                color = color.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 0.5.dp.toPx()
            )

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    this.color = android.graphics.Color.argb((0.4f * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                    this.textSize = 8.sp.toPx()
                    this.typeface = android.graphics.Typeface.MONOSPACE
                }
                drawText(fps.toInt().toString(), 0f, y - 2.dp.toPx(), paint)
            }
        }

        if (history.isNotEmpty()) {
            val path = Path()
            val lastHistory = history.takeLast(30)
            lastHistory.forEachIndexed { i, fps ->
                val x = i * spacing
                val y = height - (fps.coerceAtMost(maxFps) / maxFps) * height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 1.5.dp.toPx())
            )

            val fillPath = Path().apply {
                addPath(path)
                lineTo(spacing * (lastHistory.size - 1), height)
                lineTo(0f, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.2f), Color.Transparent)
                )
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}