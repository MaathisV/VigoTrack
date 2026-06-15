package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun MiniGraph(
    currentValue: Float?,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF4CAF50),
    maxPoints: Int = 100,
    rateLimitMs: Long = 100L
) {
    val buffer = remember { mutableStateListOf<Float>() }
    val lastPushTime = remember { mutableLongStateOf(0L) }

    if (currentValue != null) {
        val now = System.currentTimeMillis()
        if (now - lastPushTime.longValue >= rateLimitMs) {
            lastPushTime.longValue = now
            buffer.add(currentValue)
            if (buffer.size > maxPoints) {
                buffer.removeAt(0)
            }
        }
    }

    val strokeWidth = 2f

    Canvas(modifier = modifier) {
        if (buffer.size < 2) return@Canvas
        val count = buffer.size
        val min = buffer.min()
        val max = buffer.max()
        val range = (max - min).coerceAtLeast(1f)
        val stepX = size.width / (maxPoints - 1).coerceAtLeast(1)
        val startOffset = ((maxPoints - count).coerceAtLeast(0) * stepX)
        val path = Path()
        buffer.forEachIndexed { i, v ->
            val x = startOffset + i * stepX
            val y = size.height - ((v - min) / range) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
