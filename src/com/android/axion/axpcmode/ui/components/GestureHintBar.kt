package com.android.axion.axpcmode.ui.components

import android.app.contextualsearch.ContextualSearchManager
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GestureHintBar(onReveal: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    fun triggerContextualSearch() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        try {
            val csm = context.getSystemService(ContextualSearchManager::class.java)
            if (csm != null) {
                csm.startContextualSearch(ContextualSearchManager.ENTRYPOINT_LONG_PRESS_NAV_HANDLE)
            } else {
                Log.w("GestureHintBar", "Contextual Search Manager not found")
            }
        } catch (e: Exception) {
            Log.e("GestureHintBar", "Failed to start Contextual Search: ${e.message}")
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release,
                is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")
    val alpha by animateFloatAsState(if (isPressed) 0.6f else 0.4f, label = "alpha")

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(20.dp)
                .pointerInput(Unit) {
                    var totalDragY = 0f
                    var isTriggered = false
                    detectVerticalDragGestures(
                        onDragStart = {
                            totalDragY = 0f
                            isTriggered = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (isTriggered) return@detectVerticalDragGestures
                            totalDragY += dragAmount
                            val threshold = with(density) { -50.dp.toPx() }
                            if (totalDragY < threshold) {
                                isTriggered = true
                                onReveal()
                                change.consume()
                            }
                        },
                    )
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                    onLongClick = { triggerContextualSearch() },
                )
                .padding(top = 10.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(4.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
        )
    }
}
