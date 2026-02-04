package com.android.axion.axpcmode.ui.components.taskbar

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

@Composable
fun TaskbarDivider() {
    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Transparent))
}

@Composable
fun TaskbarIconButton(
    onClick: () -> Unit,
    hasBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val focusColor = MaterialTheme.colorScheme.primary
    val focusBorderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        animationSpec = tween(150),
        label = "focus_border"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "taskbar_icon_scale",
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Enter, Key.DirectionCenter, Key.ButtonA -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .then(
                if (isFocused) {
                    Modifier
                        .border(focusBorderWidth, focusColor, CircleShape)
                        .padding(2.dp)
                } else {
                    Modifier.padding(2.dp)
                }
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .then(
                if (hasBackground)
                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
