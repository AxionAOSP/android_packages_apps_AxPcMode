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

import android.content.Context
import android.graphics.Bitmap
import android.os.Process
import android.os.UserManager
import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopAccessDisabled
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.axpcmode.utils.AppInfo
import com.android.axion.axpcmode.utils.AppUtils
import com.android.axion.axpcmode.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartMenu(
    allApps: List<AppInfo>,
    pinnedApps: List<AppInfo>,
    desktopApps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAddToDesktop: (AppInfo) -> Unit,
    onRemoveFromDesktop: (AppInfo) -> Unit,
    onAddToTaskbar: (AppInfo) -> Unit,
    onExitPcMode: () -> Unit,
    maxGridHeight: Dp = 400.dp,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps =
        remember(searchQuery, allApps) {
            if (searchQuery.isEmpty()) allApps
            else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }

    val context = LocalContext.current
    var userName by remember { mutableStateOf("User") }
    var userIcon by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        try {
            userName = userManager.userName ?: "User"
            val userId = Process.myUserHandle().identifier
            userIcon = userManager.getUserIcon(userId)
        } catch (e: Exception) {}
    }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .heightIn(max = maxGridHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val chunkSize = 5
                val chunkedApps = filteredApps.chunked(chunkSize)
                
                chunkedApps.forEach { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowApps.forEach { app ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                val isPinned = pinnedApps.any { it.packageName == app.packageName }
                                val isOnDesktop = desktopApps.any { it.packageName == app.packageName }
                                StartMenuAppItem(
                                    app = app,
                                    isPinned = isPinned,
                                    isOnDesktop = isOnDesktop,
                                    onClick = { onAppClick(app) },
                                    onAddToDesktop = { onAddToDesktop(app) },
                                    onRemoveFromDesktop = { onRemoveFromDesktop(app) },
                                    onAddToTaskbar = { onAddToTaskbar(app) },
                                )
                            }
                        }
                        repeat(chunkSize - rowApps.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_apps_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        Box(
                            modifier =
                                Modifier.size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (userIcon != null) {
                                Image(
                                    bitmap = userIcon!!.asImageBitmap(),
                                    contentDescription = userName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text(
                                    text = userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors =
                        TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                            cursorColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )

                val powerInteractionSource = remember { MutableInteractionSource() }
                val powerPressed by powerInteractionSource.collectIsPressedAsState()
                val powerScale by
                    animateFloatAsState(if (powerPressed) 0.9f else 1f, label = "power_scale")

                FilledIconButton(
                    onClick = onExitPcMode,
                    modifier =
                        Modifier.size(40.dp).graphicsLayer {
                            scaleX = powerScale
                            scaleY = powerScale
                        },
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                        ),
                ) {
                    Icon(
                        Icons.Filled.DesktopAccessDisabled,
                        "Exit PC Mode",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartMenuAppItem(
    app: AppInfo,
    isPinned: Boolean,
    isOnDesktop: Boolean,
    onClick: () -> Unit,
    onAddToDesktop: () -> Unit,
    onRemoveFromDesktop: () -> Unit,
    onAddToTaskbar: () -> Unit,
) {
    val appIconBitmap = remember(app) { app.icon.toBitmap().asImageBitmap() }
    val context = LocalContext.current

    val contextMenuState = LocalContextMenuState.current
    val windowOffset = LocalWindowScreenOffset.current
    val targetDisplayId = LocalTargetDisplayId.current
    var iconCenterLocal by remember { mutableStateOf(Offset.Zero) }
    var isFocused by remember { mutableStateOf(false) }
    val focusColor = MaterialTheme.colorScheme.primary
    val focusBorderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        animationSpec = tween(150),
        label = "focus_border"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale",
    )

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    var isBottomHalf by remember { mutableStateOf(false) }

    val strFloatingWindow = stringResource(R.string.action_floating_window)
    val strRemoveFromDesktop = stringResource(R.string.action_remove_from_desktop)
    val strAddToDesktop = stringResource(R.string.action_add_to_desktop)
    val strUnpin = stringResource(R.string.action_unpin_from_taskbar)
    val strPin = stringResource(R.string.action_pin_to_taskbar)

    val showContextMenu: () -> Unit = {
        val screenPosition = Offset(
            iconCenterLocal.x + windowOffset.x,
            iconCenterLocal.y + windowOffset.y,
        )
        contextMenuState.show(
            ContextMenuParams(
                anchorPosition = screenPosition,
                direction = if (isBottomHalf) ArrowDirection.DOWN else ArrowDirection.UP,
                actions = listOf(
                    ContextMenuAction(
                        label = strFloatingWindow,
                        icon = Icons.Filled.Fullscreen,
                    ) {
                        AppUtils.launchAppInFreeform(context, app.packageName, app.className, targetDisplayId)
                    },
                    ContextMenuAction(
                        label = if (isOnDesktop) strRemoveFromDesktop else strAddToDesktop,
                        icon = if (isOnDesktop) Icons.Filled.Delete else Icons.Filled.Add,
                    ) {
                        if (isOnDesktop) onRemoveFromDesktop() else onAddToDesktop()
                    },
                    ContextMenuAction(
                        label = if (isPinned) strUnpin else strPin,
                        icon = Icons.Filled.VerticalAlignBottom,
                    ) {
                        onAddToTaskbar()
                    },
                ),
            )
        )
        Log.d("AxPcMode_UI", "StartMenu: ${app.label} onLongClick triggered")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = showContextMenu,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(4.dp)
            .width(72.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val anchorX = pos.x + coords.size.width / 2f
                val anchorY = if (isBottomHalf) pos.y else pos.y + coords.size.height
                iconCenterLocal = Offset(anchorX, anchorY)
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
