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

package com.android.axion.axpcmode.ui

import android.content.Intent
import android.view.Display
import android.view.Gravity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.axpcmode.R
import com.android.axion.axpcmode.services.SecondaryTaskbarService
import com.android.axion.axpcmode.services.TaskbarService
import com.android.axion.axpcmode.ui.components.*
import com.android.axion.axpcmode.ui.windows.LocalTaskbarIconRegistry
import com.android.axion.axpcmode.ui.windows.TaskbarIconRegistry
import com.android.axion.axpcmode.utils.AppUtils
import com.android.axion.axpcmode.utils.TaskbarConstants

@Composable
fun PcModeLauncherScreen(viewModel: PcModeLauncherViewModel) {
    val context = LocalContext.current

    val showStartMenu by viewModel.showStartMenu.collectAsState()
    val showNotificationPanel by viewModel.showNotificationPanel.collectAsState()
    val showQuickSettingsPanel by viewModel.showQuickSettingsPanel.collectAsState()
    val showMediaPlayer by viewModel.showMediaPlayer.collectAsState()
    val isFocused by viewModel.isAxionPcModeFocused.collectAsState()

    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val desktopApps by viewModel.desktopApps.collectAsState()
    val runningApps by viewModel.runningApps.collectAsState()

    val showWallpaperSettings by viewModel.showWallpaperSettings.collectAsState()
    val wallpaperBitmap by viewModel.wallpaperBitmap.collectAsState()
    val wallpaperScaleMode by viewModel.wallpaperScaleMode.collectAsState()

    val contextMenuState = remember { ContextMenuState() }
    val taskbarRegistry = remember { TaskbarIconRegistry() }
    val overlayManager = remember { PanelOverlayManager(context) }

    CompositionLocalProvider(
        LocalContextMenuState provides contextMenuState,
        LocalTaskbarIconRegistry provides taskbarRegistry,
    ) {
        DisposableEffect(Unit) {
            val isSecondary = context.displayId != Display.DEFAULT_DISPLAY
            val serviceClass = if (isSecondary)
                SecondaryTaskbarService::class.java else TaskbarService::class.java
            val serviceIntent = Intent(context, serviceClass).apply {
                action = if (isSecondary)
                    SecondaryTaskbarService.ACTION_START else TaskbarConstants.ACTION_START
            }
            context.startForegroundService(serviceIntent)

            onDispose { overlayManager.hide("ContextMenu") }
        }

        LaunchedEffect(isFocused) {
            if (!isFocused) {
                viewModel.dismissAllPanels()
            }
        }

        val contextMenuParams = contextMenuState.params
        LaunchedEffect(contextMenuParams, isFocused) {
            if (contextMenuParams != null && isFocused) {
                overlayManager.show(
                    id = "ContextMenu",
                    content = { OverlayContextMenu(contextMenuState) },
                    gravity = Gravity.FILL,
                    onOutsideClick = { contextMenuState.dismiss() },
                    noAnimation = true,
                )
            } else {
                overlayManager.hide("ContextMenu")
            }
        }

        val wallpaperContentScale = wallpaperScaleMode.toContentScale()
        val wallpaperSettingsLabel = stringResource(R.string.action_wallpaper_settings)
        if (showWallpaperSettings) {
            WallpaperSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = viewModel::closeWallpaperSettings,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(wallpaperSettingsLabel) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                if (contextMenuState.params == null) {
                                    val isBottomHalf = offset.y > size.height / 2f
                                    contextMenuState.show(
                                        ContextMenuParams(
                                            anchorPosition = Offset(offset.x, offset.y),
                                            direction = if (isBottomHalf) {
                                                ArrowDirection.DOWN
                                            } else {
                                                ArrowDirection.UP
                                            },
                                            actions = listOf(
                                                ContextMenuAction(
                                                    label = wallpaperSettingsLabel,
                                                    icon = Icons.Filled.Image,
                                                ) {
                                                    viewModel.showWallpaperSettings()
                                                },
                                            ),
                                        )
                                    )
                                }
                            },
                        )
                    },
            ) {
                DesktopWallpaper(
                    wallpaperBitmap = wallpaperBitmap,
                    contentScale = wallpaperContentScale,
                )

                DesktopGrid(
                    apps = desktopApps,
                    pinnedApps = pinnedApps,
                    onAppClick = { app ->
                        AppUtils.launchApp(
                            context, app.packageName, app.className, context.displayId
                        )
                    },
                    onRemoveApp = { app ->
                        AppUtils.removeAppFromDesktop(context, app.packageName)
                        viewModel.updateDesktopApps(AppUtils.getDesktopApps(context))
                    },
                    onAddToTaskbar = { app ->
                        val isPinned = pinnedApps.any { it.packageName == app.packageName }
                        if (isPinned) {
                            AppUtils.unpinApp(context, app.packageName)
                        } else {
                            AppUtils.pinApp(context, app.packageName)
                        }
                        viewModel.updatePinnedApps(AppUtils.getPinnedApps(context))
                    },
                    modifier = Modifier.padding(bottom = 56.dp),
                )
            }
        }
    }
}
