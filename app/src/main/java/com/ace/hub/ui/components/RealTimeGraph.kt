package com.ace.hub.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun RealTimeGraph(dataPoints: List<Float>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
        if (dataPoints.size < 2) return@Canvas
        
        val path = Path()
        val step = size.width / (dataPoints.size - 1)
        
        dataPoints.forEachIndexed { index, value ->
            val x = index * step
            // Normalize value to 0..1 (assuming incoming value is 0..100)
            val normalizedValue = (value / 100f).coerceIn(0f, 1f)
            val y = size.height - (normalizedValue * size.height)
            
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
    }
}
