package com.android.axion.axpcmode.ui.components.qs

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.axion.axpcmode.services.QSTileData
import com.android.axion.axpcmode.ui.tiles.TileIconMapping

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QsTile(tileData: QSTileData, modifier: Modifier = Modifier, onClick: () -> Unit) {

    val isActive = tileData.state == 2
    val isUnavailable = tileData.state == 0

    val backgroundColor =
        when {
            isUnavailable -> MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5f)
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceBright
        }
    val contentColor =
        when {
            isUnavailable -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            isActive -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        }

    val icon = TileIconMapping.getIcon(tileData.spec)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by
        animateFloatAsState(
            targetValue = if (isPressed) 0.9f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "press_scale",
        )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            modifier.fillMaxWidth().height(100.dp).graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
    ) {
        Box(
            modifier =
                Modifier.size(56.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = !isUnavailable,
                        onClick = onClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (tileData.isTransient) {

                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    icon,
                    contentDescription = tileData.label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Text(
            text = tileData.label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(80.dp).basicMarquee(),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )

        Text(
            text = tileData.secondaryLabel.ifEmpty { " " },
            color =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (tileData.secondaryLabel.isNotEmpty()) 0.7f else 0f
                ),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(80.dp).basicMarquee(),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
