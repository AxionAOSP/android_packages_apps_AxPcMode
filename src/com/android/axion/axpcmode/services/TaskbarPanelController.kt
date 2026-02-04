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

import android.app.FreeformLauncher
import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
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
) {

    fun init() {
        scope.launch {
            combine(
                    vm.showStartMenu,
                    vm.showQuickSettingsPanel,
                    vm.showNotificationPanel,
                    vm.showMediaPlayer,
                ) { states ->
                    states.any { it }
                }
                .collect { isAnyShowing ->
                    if (isAnyShowing) {
                        FreeformLauncher.bringAllWindowsToBack()
                    }
                }
        }

        scope.launch {
            vm.showStartMenu.collect { show ->
                if (show) {
                    val resources = context.resources
                    val density = resources.displayMetrics.density
                    val screenHeight = resources.displayMetrics.heightPixels

                    val widthPx = (550 * density).toInt()
                    val taskbarHeightPx = (48 * density).toInt()
                    val yOffsetPx = (56 * density).toInt()
                    val paddingPx = (12 * density).toInt()

                    val maxHeightPx = screenHeight - taskbarHeightPx - yOffsetPx - paddingPx

                    panelOverlayManager.show(
                        id = "start_menu",
                        content = { OverlayStartMenu(vm, contextMenuState) },
                        gravity = Gravity.BOTTOM or Gravity.START,
                        width = widthPx,
                        height = maxHeightPx,
                        y = yOffsetPx,
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
                    val density = context.resources.displayMetrics.density

                    if (show) {
                        if (edit) {
                            panelOverlayManager.hide("quick_settings")
                            panelOverlayManager.show(
                                id = "quick_settings_editor",
                                content = { OverlayQuickSettingsEditor(qsViewModel) },
                                gravity = Gravity.BOTTOM or Gravity.END,
                                width = (800 * density).toInt(),
                                height = WindowManager.LayoutParams.WRAP_CONTENT,
                                x = (12 * density).toInt(),
                                y = (56 * density).toInt(),
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
                                y = (56 * density).toInt(),
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
                    val density = context.resources.displayMetrics.density
                    panelOverlayManager.show(
                        id = "notifications",
                        content = { OverlayNotificationPanel(vm) },
                        gravity = Gravity.BOTTOM or Gravity.END,
                        width = WindowManager.LayoutParams.WRAP_CONTENT,
                        height = WindowManager.LayoutParams.WRAP_CONTENT,
                        y = (56 * density).toInt(),
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
                    val density = context.resources.displayMetrics.density
                    panelOverlayManager.show(
                        id = "media_player",
                        content = { OverlayMediaPlayer(vm, mediaRepository) },
                        gravity = Gravity.BOTTOM or Gravity.END,
                        width = WindowManager.LayoutParams.WRAP_CONTENT,
                        height = WindowManager.LayoutParams.WRAP_CONTENT,
                        y = (56 * density).toInt(),
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
    }
}
