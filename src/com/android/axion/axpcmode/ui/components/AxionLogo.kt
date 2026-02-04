package com.android.axion.axpcmode.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill

@Composable
fun AxionLogo(modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.onSurface
    val letterColor = MaterialTheme.colorScheme.surfaceContainer

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val hexPath =
            Path().apply {
                val cx = w / 2f
                val cy = h / 2f

                moveTo(cx, 0f)
                lineTo(w, h * 0.25f)
                lineTo(w, h * 0.75f)
                lineTo(cx, h)
                lineTo(0f, h * 0.75f)
                lineTo(0f, h * 0.25f)
                close()
            }

        drawPath(path = hexPath, color = backgroundColor, style = Fill)

        val aPath =
            Path().apply {
                val cx = w / 2f
                val topY = h * 0.25f
                val botY = h * 0.75f
                val widthAtBot = w * 0.4f

                moveTo(cx, topY)
                lineTo(cx + widthAtBot, botY)
                lineTo(cx + widthAtBot * 0.7f, botY)
                lineTo(cx + widthAtBot * 0.2f, h * 0.55f)
                lineTo(cx - widthAtBot * 0.2f, h * 0.55f)
                lineTo(cx - widthAtBot * 0.7f, botY)
                lineTo(cx - widthAtBot, botY)
                close()
            }

        drawPath(path = aPath, color = letterColor, style = Fill)
    }
}
