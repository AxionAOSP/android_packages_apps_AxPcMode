/*
 * Copyright (C) 2025-2026 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.axion.axpcmode.ui.components

import android.app.FreeformLauncher
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.axpcmode.utils.AppInfo
import com.android.axion.axpcmode.utils.AppUtils
import com.android.axion.axpcmode.R

@Composable
fun DesktopGrid(
    apps: List<AppInfo>,
    pinnedApps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    onAddToTaskbar: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyHorizontalGrid(
        rows = GridCells.Adaptive(minSize = 84.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = 44.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(apps) { app ->
            val isPinned = pinnedApps.any { it.packageName == app.packageName }
            DesktopAppIcon(
                app = app,
                isPinned = isPinned,
                onClick = { onAppClick(app) },
                onRemove = { onRemoveApp(app) },
                onAddToTaskbar = { onAddToTaskbar(app) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopAppIcon(
    app: AppInfo,
    isPinned: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onAddToTaskbar: () -> Unit,
) {
    val launcherContext = LocalContext.current
    val appIconBitmap = remember(app) { app.icon.toBitmap().asImageBitmap() }
    var isLongPressed by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(if (isLongPressed) 0.9f else 1f, label = "scale")
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isLongPressed = true
                is PressInteraction.Release,
                is PressInteraction.Cancel -> isLongPressed = false
            }
        }
    }

    val contextMenuState = LocalContextMenuState.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    var isBottomHalf by remember { mutableStateOf(false) }
    var iconCenterScreen by remember { mutableStateOf(Offset.Zero) }
    val focusColor = MaterialTheme.colorScheme.primary
    val focusBorderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        animationSpec = tween(150),
        label = "focus_border"
    )

    val strFloatingWindow = stringResource(R.string.action_floating_window)
    val strRemoveFromDesktop = stringResource(R.string.action_remove_from_desktop)
    val strUnpin = stringResource(R.string.action_unpin_from_taskbar)
    val strPin = stringResource(R.string.action_pin_to_taskbar)
    val strAppInfo = stringResource(R.string.action_app_info)

    val showContextMenu: () -> Unit = {
        isLongPressed = false
        contextMenuState.show(
            ContextMenuParams(
                anchorPosition = iconCenterScreen,
                direction = if (isBottomHalf) ArrowDirection.DOWN else ArrowDirection.UP,
                actions = listOf(
                    ContextMenuAction(
                        label = strFloatingWindow,
                        icon = Icons.Filled.Fullscreen,
                    ) {
                        FreeformLauncher.launchDesktopApp(app.packageName, app.className)
                    },
                    ContextMenuAction(
                        label = strRemoveFromDesktop,
                        icon = Icons.Filled.Delete,
                    ) {
                        onRemove()
                    },
                    ContextMenuAction(
                        label = if (isPinned) strUnpin else strPin,
                        icon = if (isPinned) Icons.Filled.VerticalAlignBottom else Icons.Filled.PushPin,
                    ) {
                        onAddToTaskbar()
                    },
                    ContextMenuAction(
                        label = strAppInfo,
                        icon = Icons.Filled.Info,
                    ) {
                        AppUtils.launchAppInfo(launcherContext, app.packageName)
                    },
                ),
            )
        )
        Log.d("AxPcMode_UI", "DesktopGrid: ${app.label} onLongClick triggered")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Enter, Key.DirectionCenter, Key.ButtonA -> {
                            onClick()
                            true
                        }
                        Key.Menu, Key.ButtonX -> {
                            showContextMenu()
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
                        .border(focusBorderWidth, focusColor, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                } else {
                    Modifier.padding(2.dp)
                }
            )
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = showContextMenu,
            )
            .padding(4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val anchorX = pos.x + coords.size.width / 2f
                val anchorY = if (isBottomHalf) pos.y else pos.y + coords.size.height
                iconCenterScreen = Offset(anchorX, anchorY)
                val screenHeight = configuration.screenHeightDp.coerceAtLeast(1)
                isBottomHalf = with(density) { pos.y.toDp() > (screenHeight / 2).dp }
            },
        ) {
            Image(
                painter = BitmapPainter(appIconBitmap),
                contentDescription = app.label,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.labelMedium.copy(
                shadow = Shadow(color = Color.Black, blurRadius = 4f)
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
