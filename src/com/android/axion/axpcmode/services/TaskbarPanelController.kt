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

package com.android.axion.axpcmode.services

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.snapshotFlow
import com.android.axion.axpcmode.ui.OverlayContextMenu
import com.android.axion.axpcmode.ui.OverlayMediaPlayer
import com.android.axion.axpcmode.ui.OverlayNotificationPanel
import com.android.axion.axpcmode.ui.OverlayQuickSettings
import com.android.axion.axpcmode.ui.OverlayQuickSettingsEditor
import com.android.axion.axpcmode.ui.OverlayStartMenu
import com.android.axion.axpcmode.ui.PanelOverlayManager
import com.android.axion.axpcmode.ui.PcModeLauncherViewModel
import com.android.axion.axpcmode.ui.QuickSettingsViewModel
import com.android.axion.axpcmode.ui.components.ContextMenuState
import com.android.axion.axpcmode.ui.components.taskbar.TaskPeekCard
import com.android.axion.axpcmode.ui.components.taskbar.TaskPeekState
import com.android.axion.axpcmode.ui.theme.AxPcModeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TaskbarPanelController(
    private val context: Context,
    private val vm: PcModeLauncherViewModel,
    private val scope: CoroutineScope,
    private val panelOverlayManager: PanelOverlayManager,
    private val qsViewModel: QuickSettingsViewModel,
    private val mediaRepository: MediaRepository,
    private val contextMenuState: ContextMenuState,
    private val peekState: TaskPeekState,
    private val displayDensityDpi: StateFlow<Int>,
    private val targetDisplayId: Int = Display.DEFAULT_DISPLAY,
) {

    private fun resolveDisplayMetrics(): DisplayMetrics {
        val dm = context.getSystemService(DisplayManager::class.java)
        val display = dm?.getDisplay(targetDisplayId)
        val metrics = DisplayMetrics()
        if (display != null) {
            display.getRealMetrics(metrics)
            val dpi = displayDensityDpi.value
            if (dpi > 0) metrics.density = dpi / 160f
        } else {
            context.resources.displayMetrics.let {
                metrics.setTo(it)
            }
        }
        return metrics
    }

    fun init() {
        val metrics = resolveDisplayMetrics()
        val density = metrics.density
        val screenWidthDp = metrics.widthPixels / density
        val screenHeightPx = metrics.heightPixels

        val taskbarYOffsetPx = (56 * density).toInt()
        val panelPaddingPx = (12 * density).toInt()
        val maxPanelHeightPx = screenHeightPx - taskbarYOffsetPx - panelPaddingPx

        val startMenuMaxWidthDp = (screenWidthDp * 0.55f).coerceIn(360f, 720f)
        val startMenuMaxWidthPx = (startMenuMaxWidthDp * density).toInt()

        val qsEditorWidthPx = ((screenWidthDp * 0.55f).coerceIn(400f, 800f) * density).toInt()

        scope.launch {
            vm.showStartMenu.collect { show ->
                if (show) {
                    panelOverlayManager.show(
                        id = "start_menu",
                        content = { OverlayStartMenu(vm, contextMenuState) },
                        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                        width = startMenuMaxWidthPx,
                        height = WindowManager.LayoutParams.WRAP_CONTENT,
                        y = taskbarYOffsetPx,
                        focusable = true,
                        onOutsideClick = { vm.dismissAllPanels(fromOutside = true) },
                    )
                } else {
                    panelOverlayManager.hide("start_menu")
                }
            }
        }

        scope.launch {
            combine(vm.showQuickSettingsPanel, qsViewModel.isEditMode) { show, edit ->
                    show to edit
                }
                .collect { (show, edit) ->
                    if (show) {
                        if (edit) {
                            panelOverlayManager.hide("quick_settings")
                            panelOverlayManager.show(
                                id = "quick_settings_editor",
                                content = { OverlayQuickSettingsEditor(qsViewModel) },
                                gravity = Gravity.BOTTOM or Gravity.END,
                                width = qsEditorWidthPx,
                                height = WindowManager.LayoutParams.WRAP_CONTENT,
                                y = taskbarYOffsetPx,
                                focusable = true,
                                onOutsideClick = {
                                    qsViewModel.toggleEditMode()
                                    vm.dismissAllPanels(fromOutside = true)
                                },
                            )
                        } else {
                            panelOverlayManager.hide("quick_settings_editor")
                            panelOverlayManager.show(
                                id = "quick_settings",
                                content = { OverlayQuickSettings(vm, qsViewModel) },
                                gravity = Gravity.BOTTOM or Gravity.END,
                                width = WindowManager.LayoutParams.WRAP_CONTENT,
                                height = WindowManager.LayoutParams.WRAP_CONTENT,
                                y = taskbarYOffsetPx,
                                onOutsideClick = { vm.dismissAllPanels(fromOutside = true) },
                            )
                        }
                    } else {

                        panelOverlayManager.hide("quick_settings")
                        panelOverlayManager.hide("quick_settings_editor")
                    }
                }
        }

        scope.launch {
            vm.showNotificationPanel.collect { show ->
                if (show) {
                    panelOverlayManager.show(
                        id = "notifications",
                        content = { OverlayNotificationPanel(vm) },
                        gravity = Gravity.BOTTOM or Gravity.END,
                        width = WindowManager.LayoutParams.WRAP_CONTENT,
                        height = WindowManager.LayoutParams.WRAP_CONTENT,
                        y = taskbarYOffsetPx,
                        onOutsideClick = { vm.dismissAllPanels(fromOutside = true) },
                    )
                } else {
                    panelOverlayManager.hide("notifications")
                }
            }
        }

        scope.launch {
            vm.showMediaPlayer.collect { show ->
                if (show) {
                    panelOverlayManager.show(
                        id = "media_player",
                        content = { OverlayMediaPlayer(vm, mediaRepository) },
                        gravity = Gravity.BOTTOM or Gravity.END,
                        width = WindowManager.LayoutParams.WRAP_CONTENT,
                        height = WindowManager.LayoutParams.WRAP_CONTENT,
                        y = taskbarYOffsetPx,
                        onOutsideClick = { vm.dismissAllPanels(fromOutside = true) },
                    )
                } else {
                    panelOverlayManager.hide("media_player")
                }
            }
        }

        scope.launch {
            snapshotFlow { contextMenuState.params }
                .collect { params ->
                    if (params != null) {
                        panelOverlayManager.show(
                            id = "context_menu",
                            content = { OverlayContextMenu(contextMenuState) },
                            gravity = Gravity.FILL,
                            onOutsideClick = { contextMenuState.dismiss() },
                            noAnimation = true,
                        )
                    } else {
                        panelOverlayManager.hide("context_menu")
                    }
                }
        }

        val peekWidthPx = (240 * density).toInt()
        val peekYOffsetPx = taskbarYOffsetPx + (8 * density).toInt()

        scope.launch {
            snapshotFlow { peekState.peekedApp to peekState.anchorXPx }
                .collect { (app, anchorX) ->
                    if (app != null) {
                        val xPos = (anchorX - peekWidthPx / 2f)
                            .coerceIn(0f, (metrics.widthPixels - peekWidthPx).toFloat())
                            .toInt()

                        panelOverlayManager.show(
                            id = "task_peek",
                            content = {
                                AxPcModeTheme {
                                    TaskPeekCard(
                                        app = app,
                                        thumbnail = peekState.thumbnail,
                                    )
                                }
                            },
                            gravity = Gravity.BOTTOM or Gravity.START,
                            width = peekWidthPx,
                            height = WindowManager.LayoutParams.WRAP_CONTENT,
                            x = xPos,
                            y = peekYOffsetPx,
                        )
                    } else {
                        panelOverlayManager.hide("task_peek")
                    }
                }
        }
    }
}
