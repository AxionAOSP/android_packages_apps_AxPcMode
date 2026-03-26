package com.android.axion.axpcmode.ui.components

import android.view.Display
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector

val LocalWindowScreenOffset = staticCompositionLocalOf { Offset.Zero }
val LocalTargetDisplayId = staticCompositionLocalOf { Display.DEFAULT_DISPLAY }

data class ContextMenuAction(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

data class ContextMenuParams(
    val anchorPosition: Offset,
    val actions: List<ContextMenuAction>,
    val direction: ArrowDirection = ArrowDirection.UP,
)

class ContextMenuState {
    var params by mutableStateOf<ContextMenuParams?>(null)
        private set

    fun show(newParams: ContextMenuParams) {
        params = newParams
    }

    fun dismiss() {
        params = null
    }
}

val LocalContextMenuState =
    staticCompositionLocalOf<ContextMenuState> { error("No ContextMenuState provided") }
