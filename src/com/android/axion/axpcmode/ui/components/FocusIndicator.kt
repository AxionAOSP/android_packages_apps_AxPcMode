package com.android.axion.axpcmode.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberFocusIndicatorState(): FocusIndicatorState {
    return remember { FocusIndicatorState() }
}

class FocusIndicatorState {
    var isFocused by mutableStateOf(false)
        internal set
}

fun Modifier.focusIndicator(
    state: FocusIndicatorState,
    focusRequester: FocusRequester,
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: Dp = 2.dp,
    onSelect: (() -> Unit)? = null,
    onLongSelect: (() -> Unit)? = null,
): Modifier = composed {
    val borderWidthAnimated by animateDpAsState(
        targetValue = if (state.isFocused) borderWidth else 0.dp,
        animationSpec = tween(150),
        label = "focus_border"
    )
    val focusColor = MaterialTheme.colorScheme.primary

    this
        .focusRequester(focusRequester)
        .onFocusChanged { focusState ->
            state.isFocused = focusState.isFocused
        }
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.Enter, Key.DirectionCenter, Key.ButtonA -> {
                        onSelect?.invoke()
                        true
                    }
                    Key.Menu, Key.ButtonX -> {
                        onLongSelect?.invoke()
                        true
                    }
                    else -> false
                }
            } else false
        }
        .focusable()
        .then(
            if (state.isFocused) {
                Modifier
                    .border(borderWidthAnimated, focusColor, shape)
                    .padding(borderWidthAnimated)
            } else {
                Modifier.padding(borderWidth)
            }
        )
}

fun Modifier.focusIndicatorSimple(
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: Dp = 2.dp,
    focusedColor: Color? = null,
    onSelect: (() -> Unit)? = null,
    onLongSelect: (() -> Unit)? = null,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val borderWidthAnimated by animateDpAsState(
        targetValue = if (isFocused) borderWidth else 0.dp,
        animationSpec = tween(150),
        label = "focus_border"
    )
    val focusColor = focusedColor ?: MaterialTheme.colorScheme.primary

    this
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
        }
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.Enter, Key.DirectionCenter, Key.ButtonA -> {
                        onSelect?.invoke()
                        true
                    }
                    Key.Menu, Key.ButtonX -> {
                        onLongSelect?.invoke()
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
                    .border(borderWidthAnimated, focusColor, shape)
                    .padding(borderWidthAnimated)
            } else {
                Modifier.padding(borderWidth)
            }
        )
}

fun Modifier.focusableBorder(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: Dp = 2.dp,
    focusedColor: Color? = null,
): Modifier = composed {
    val borderWidthAnimated by animateDpAsState(
        targetValue = if (isFocused) borderWidth else 0.dp,
        animationSpec = tween(150),
        label = "focus_border"
    )
    val focusColor = focusedColor ?: MaterialTheme.colorScheme.primary

    if (isFocused) {
        Modifier
            .border(borderWidthAnimated, focusColor, shape)
            .padding(borderWidthAnimated)
    } else {
        Modifier.padding(borderWidth)
    }
}
