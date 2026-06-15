package com.ace.hub.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FpsXyGraph(history: List<Float>, color: Color, maxVal: Float = 120f) {
    val milestones = if (maxVal > 100) listOf(30f, 60f, 90f) else listOf(maxVal * 0.25f, maxVal * 0.5f, maxVal * 0.75f)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(start = 30.dp, bottom = 20.dp, end = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = width / (if (history.size > 1) history.size - 1 else 1)

            // Draw Milestones
            milestones.forEach { valY ->
                val y = height - (valY / maxVal) * height
                drawLine(
                    color = color.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Draw text labels
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.argb((0.4f * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                        this.textSize = 10.sp.toPx()
                        this.typeface = android.graphics.Typeface.MONOSPACE
                    }
                    drawText(valY.toInt().toString(), -25.dp.toPx(), y + 4.dp.toPx(), paint)
                }
            }

            // Draw Axis
            drawLine(color.copy(alpha = 0.3f), Offset(0f, 0f), Offset(0f, height), 1.dp.toPx())
            drawLine(color.copy(alpha = 0.3f), Offset(0f, height), Offset(width, height), 1.dp.toPx())

            if (history.isNotEmpty()) {
                val path = Path()
                history.forEachIndexed { i, value ->
                    val x = i * spacing
                    val y = height - (value.coerceAtMost(maxVal) / maxVal) * height
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                // Draw Path
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Draw Area Gradient
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
            }
        }
    }
}
