package com.android.axion.axpcmode.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun SignalIcon(
    level: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Canvas(modifier = modifier) {
        val barCount = 4
        val spacing = size.width * 0.1f
        val barWidth = (size.width - (spacing * (barCount - 1))) / barCount

        val heights = listOf(0.25f, 0.5f, 0.75f, 1.0f)

        for (i in 0 until barCount) {
            val barHeight = size.height * heights[i]
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight

            val isActive = i < level
            val barColor = if (isActive) color else color.copy(alpha = 0.3f)

            drawRect(color = barColor, topLeft = Offset(x, y), size = Size(barWidth, barHeight))
        }
    }
}
