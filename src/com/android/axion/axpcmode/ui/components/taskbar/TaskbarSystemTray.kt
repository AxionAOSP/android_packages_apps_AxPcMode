package com.android.axion.axpcmode.ui.components.taskbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.axion.axpcmode.ui.components.SignalIcon

private val systemTraySpacing = 6.dp

@Composable
fun TaskbarSystemTray(
    state: TaskbarSystemState,
    onMediaClick: () -> Unit,
    onQuickSettingsClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(systemTraySpacing),
    ) {
        Box(
            modifier =
                Modifier.size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onMediaClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = "Media",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        TaskbarDivider()

        Box(
            modifier =
                Modifier.height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onQuickSettingsClick)
                    .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(systemTraySpacing),
            ) {
                if (state.isBtEnabled) {
                    val btColor = MaterialTheme.colorScheme.onSurface
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = "Bluetooth",
                            modifier = Modifier.size(16.dp),
                            tint = btColor,
                        )
                        if (state.bluetoothBatteryLevel > -1) {
                            VerticalBatteryBar(
                                level = state.bluetoothBatteryLevel,
                                modifier = Modifier.size(6.dp, 14.dp),
                            )
                        }
                    }
                }

                if (state.isWifiEnabled) {
                    Icon(
                        imageVector =
                            if (state.isWifiConnected) Icons.Rounded.Wifi
                            else Icons.Rounded.WifiOff,
                        contentDescription = "WiFi",
                        modifier = Modifier.size(16.dp),
                        tint =
                            if (state.isWifiConnected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }

                if (state.isSimPresent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (
                            state.isMobileDataEnabled &&
                                !state.isWifiEnabled &&
                                state.mobileDataType.isNotEmpty()
                        ) {
                            val displayText = if (
                                state.mobileDataType.equals("No Internet", ignoreCase = true) ||
                                    state.mobileDataType.equals("No Service", ignoreCase = true)
                            ) "X" else state.mobileDataType
                            Text(
                                text = displayText,
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        SignalIcon(
                            level = state.mobileDataLevel,
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                TaskbarBatteryIcon(
                    level = state.batteryLevel,
                    isCharging = state.isCharging,
                    modifier = Modifier.size(34.dp, 16.dp),
                )
            }
        }

        TaskbarDivider()

        Box(
            modifier =
                Modifier.height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onNotificationClick)
                    .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = state.currentTime,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = state.currentDate,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
fun TaskbarBatteryIcon(
    level: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val strokeWidth = 1.dp.toPx()
            val cornerRadius = 3.dp.toPx()

            drawRoundRect(
                color = contentColor,
                size = Size(width - 3.5.dp.toPx(), height),
                cornerRadius = CornerRadius(cornerRadius),
                style = Stroke(strokeWidth),
            )

            drawRoundRect(
                color = contentColor,
                topLeft = Offset(width - 3.dp.toPx(), height / 4),
                size = Size(3.dp.toPx(), height / 2),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )

            val fillPadding = strokeWidth + 1.dp.toPx()
            val maxFillWidth = width - 3.5.dp.toPx() - (fillPadding * 2)
            val fillWidth = maxFillWidth * (level.coerceIn(0, 100) / 100f)

            if (fillWidth > 0) {
                drawRoundRect(
                    color =
                        if (isCharging) Color(0xFF4CAF50)
                        else if (level <= 15) Color(0xFFF44336) else contentColor,
                    topLeft = Offset(fillPadding, fillPadding),
                    size = Size(fillWidth, height - (fillPadding * 2)),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }
        }

        if (showPercentage) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(end = 3.5.dp),
            ) {
                Text(
                    text = "$level",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 9.sp,
                            letterSpacing = (-0.5).sp,
                        ),
                    color = if (level > 55 || isCharging) Color.Black else contentColor,
                )
                if (isCharging) {
                    Spacer(modifier = Modifier.width(1.dp))
                    Canvas(modifier = Modifier.size(7.dp, 11.dp)) {
                        val w = size.width
                        val h = size.height
                        val path =
                            Path().apply {
                                moveTo(w * 0.75f, h * 0.05f)
                                lineTo(w * 0.1f, h * 0.55f)
                                lineTo(w * 0.55f, h * 0.55f)
                                lineTo(w * 0.25f, h * 0.95f)
                                lineTo(w * 0.9f, h * 0.45f)
                                lineTo(w * 0.45f, h * 0.45f)
                                close()
                            }
                        drawPath(path, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalBatteryBar(
    level: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.dp.toPx()
        val width = size.width
        val height = size.height

        drawRect(
            color = color.copy(alpha = 0.3f),
            style = Stroke(width = strokeWidth),
            size = Size(width, height),
            topLeft = Offset.Zero,
        )

        val fillHeight = (height - (2 * strokeWidth)) * (level / 100f)
        val fillTop = height - strokeWidth - fillHeight

        if (level > 0) {
            drawRect(
                color = color,
                topLeft = Offset(strokeWidth, fillTop),
                size = Size(width - (2 * strokeWidth), fillHeight),
            )
        }
    }
}
