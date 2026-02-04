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

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.axpcmode.R
import com.android.axion.axpcmode.activities.PcModeLauncherActivity
import com.android.axion.axpcmode.activities.TasksOverviewActivity
import com.android.axion.axpcmode.services.MediaRepository
import com.android.axion.axpcmode.ui.components.*
import com.android.axion.axpcmode.ui.theme.AxPcModeTheme
import com.android.axion.axpcmode.ui.windows.LocalTaskbarIconRegistry
import com.android.axion.axpcmode.ui.windows.TaskbarIconRegistry
import com.android.axion.axpcmode.utils.AppUtils

@Composable
fun OverlayTaskbar(
    viewModel: PcModeLauncherViewModel,
    contextMenuState: ContextMenuState,
    taskbarRegistry: TaskbarIconRegistry,
) {
    val context = LocalContext.current
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val runningApps by viewModel.runningApps.collectAsState()

    var windowOffset by remember { mutableStateOf(Offset.Zero) }

    AxPcModeTheme {
        CompositionLocalProvider(
            LocalContextMenuState provides contextMenuState,
            LocalTaskbarIconRegistry provides taskbarRegistry,
            LocalWindowScreenOffset provides windowOffset,
        ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth().height(56.dp).onGloballyPositioned { coords ->
                        windowOffset = coords.positionOnScreen()
                    }
            ) {
                Taskbar(
                    pinnedApps = pinnedApps,
                    runningApps = runningApps,
                    onStartClick = { viewModel.toggleStartMenu() },
                    onBackClick = {
                        try {
                            Runtime.getRuntime().exec("input keyevent 4")
                        } catch (e: Exception) {
                            Log.e("OverlayTaskbar", "Failed to inject back key", e)
                        }
                    },
                    onHomeClick = {
                        val intent = Intent(context, PcModeLauncherActivity::class.java)
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                        context.startActivity(intent)
                    },
                    onRecentsClick = {
                        val intent = Intent(context, TasksOverviewActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    onImeClick = {
                        val imm =
                            context.getSystemService(Context.INPUT_METHOD_SERVICE)
                                as InputMethodManager
                        imm.showInputMethodPicker()
                    },
                    onAppClick = { app ->
                        AppUtils.launchApp(context, app.packageName, app.className)
                    },
                    onNotificationClick = { viewModel.toggleNotificationPanel() },
                    onQuickSettingsClick = { viewModel.toggleQuickSettingsPanel() },
                    onMediaClick = { viewModel.toggleMediaPlayer() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
fun OverlayStartMenu(viewModel: PcModeLauncherViewModel, contextMenuState: ContextMenuState) {
    val context = LocalContext.current
    val allApps by viewModel.allApps.collectAsState()
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val desktopApps by viewModel.desktopApps.collectAsState()

    var windowOffset by remember { mutableStateOf(Offset.Zero) }

    AxPcModeTheme {
        CompositionLocalProvider(
            LocalContextMenuState provides contextMenuState,
            LocalWindowScreenOffset provides windowOffset,
        ) {
            Box(
                contentAlignment = Alignment.BottomStart,
                modifier =
                    Modifier.onGloballyPositioned { coords ->
                        windowOffset = coords.positionOnScreen()
                    },
            ) {
                StartMenu(
                    allApps = allApps,
                    pinnedApps = pinnedApps,
                    desktopApps = desktopApps,
                    onAppClick = { app ->
                        AppUtils.launchApp(context, app.packageName, app.className)
                        viewModel.dismissAllPanels()
                    },
                    onAddToDesktop = { app ->
                        AppUtils.addAppToDesktop(context, app.packageName)
                        viewModel.onRefreshDesktopApps?.invoke()
                        viewModel.dismissAllPanels()
                    },
                    onRemoveFromDesktop = { app ->
                        AppUtils.removeAppFromDesktop(context, app.packageName)
                        viewModel.onRefreshDesktopApps?.invoke()
                        viewModel.dismissAllPanels()
                    },
                    onAddToTaskbar = { app ->
                        val isPinned = pinnedApps.any { p -> p.packageName == app.packageName }
                        if (isPinned) {
                            AppUtils.unpinApp(context, app.packageName)
                        } else {
                            AppUtils.pinApp(context, app.packageName)
                        }
                        viewModel.onRefreshPinnedApps?.invoke()
                        viewModel.dismissAllPanels()
                    },
                    modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
fun OverlayQuickSettings(viewModel: PcModeLauncherViewModel, qsViewModel: QuickSettingsViewModel) {
    AxPcModeTheme {
        Box(contentAlignment = Alignment.BottomEnd) {
            QuickSettingsPanel(
                viewModel = qsViewModel,
                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp, end = 12.dp),
            )
        }
    }
}

@Composable
fun OverlayQuickSettingsEditor(viewModel: QuickSettingsViewModel) {
    val qsTiles by viewModel.qsTiles.collectAsState()
    val availableTiles by viewModel.availableTiles.collectAsState()
    val configuration = LocalConfiguration.current
    val safeMaxHeight = (configuration.screenHeightDp - 80).dp

    AxPcModeTheme {
        Surface(
            modifier = Modifier.width(800.dp).heightIn(max = safeMaxHeight).padding(12.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(id = R.string.edit_tiles),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Button(onClick = { viewModel.toggleEditMode() }, shape = CircleShape) {
                        Icon(
                            Icons.Rounded.Done,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                QsTileEditor(
                    viewModel = viewModel,
                    qsTiles = qsTiles,
                    availableTiles = availableTiles,
                )
            }
        }
    }
}

@Composable
fun OverlayNotificationPanel(viewModel: PcModeLauncherViewModel) {
    AxPcModeTheme {
        Box(contentAlignment = Alignment.BottomEnd) {
            NotificationPanel(modifier = Modifier.padding(top = 6.dp, bottom = 6.dp, end = 12.dp))
        }
    }
}

@Composable
fun OverlayMediaPlayer(viewModel: PcModeLauncherViewModel, mediaRepository: MediaRepository) {
    AxPcModeTheme {
        Box(contentAlignment = Alignment.BottomEnd) {
            MediaPlayerCard(
                mediaRepository = mediaRepository,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 12.dp),
            )
        }
    }
}

@Composable
fun OverlayContextMenu(contextMenuState: ContextMenuState) {
    AxPcModeTheme {
        CompositionLocalProvider(LocalContextMenuState provides contextMenuState) {
            ContextMenuOverlay()
        }
    }
}
