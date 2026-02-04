package com.android.axion.axpcmode.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import kotlin.math.roundToInt

@Composable
fun ContextMenuOverlay() {
    val state = LocalContextMenuState.current
    val params = state.params ?: return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val menuWidth = 210.dp
    val menuHeight = 52.dp
    val margin = 8.dp

    val anchorX = with(density) { params.anchorPosition.x.toDp() }
    val anchorY = with(density) { params.anchorPosition.y.toDp() }

    var menuLeft = anchorX - (menuWidth / 2)
    if (menuLeft < margin) menuLeft = margin
    if (menuLeft + menuWidth > screenWidth - margin) menuLeft = screenWidth - margin - menuWidth

    val arrowOffset = anchorX - menuLeft

    Box(
        modifier =
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                state.dismiss()
            }
    ) {
        val actions = params.actions
        val spacing = 1.dp
        val largeRadius = 24.dp
        val smallRadius = 4.dp

        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(2.dp),
            modifier =
                Modifier.size(16.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val x =
                            with(density) { (menuLeft + arrowOffset).toPx() }.roundToInt() -
                                (placeable.width / 2)

                        val y =
                            if (params.direction == ArrowDirection.UP) {
                                with(density) { (anchorY + 4.dp).toPx() }.roundToInt()
                            } else {
                                with(density) { (anchorY - 4.dp).toPx() }.roundToInt() -
                                    placeable.height
                            }

                        layout(placeable.width, placeable.height) { placeable.placeRelative(x, y) }
                    }
                    .graphicsLayer { rotationZ = 45f },
        ) {}

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier =
                Modifier.width(menuWidth).layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val width = placeable.width
                    val height = placeable.height

                    val x = with(density) { menuLeft.toPx() }.roundToInt()

                    val y =
                        if (params.direction == ArrowDirection.UP) {
                            with(density) { (anchorY + 8.dp).toPx() }.roundToInt()
                        } else {
                            with(density) { (anchorY - 8.dp).toPx() }.roundToInt() - height
                        }

                    layout(width, height) { placeable.placeRelative(x, y) }
                },
        ) {
            actions.forEachIndexed { index, action ->
                val isFirst = index == 0
                val isLast = index == actions.size - 1

                val topRadius = if (isFirst) 16.dp else 4.dp
                val bottomRadius = if (isLast) 16.dp else 4.dp

                Surface(
                    onClick = {
                        action.onClick()
                        state.dismiss()
                    },
                    shape =
                        RoundedCornerShape(
                            topStart = topRadius,
                            topEnd = topRadius,
                            bottomStart = bottomRadius,
                            bottomEnd = bottomRadius,
                        ),
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().height(menuHeight),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        if (action.icon != null) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
