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

import androidx.lifecycle.ViewModel
import com.android.axion.axpcmode.utils.AppInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class PcModeLauncherViewModel @Inject constructor() : ViewModel() {

    var onRefreshPinnedApps: (() -> Unit)? = null
    var onRefreshDesktopApps: (() -> Unit)? = null

    private val _showStartMenu = MutableStateFlow(false)
    val showStartMenu: StateFlow<Boolean> = _showStartMenu.asStateFlow()

    private val _showNotificationPanel = MutableStateFlow(false)
    val showNotificationPanel: StateFlow<Boolean> = _showNotificationPanel.asStateFlow()

    private val _showQuickSettingsPanel = MutableStateFlow(false)
    val showQuickSettingsPanel: StateFlow<Boolean> = _showQuickSettingsPanel.asStateFlow()

    private val _showMediaPlayer = MutableStateFlow(false)
    val showMediaPlayer: StateFlow<Boolean> = _showMediaPlayer.asStateFlow()

    private val _showTasksOverview = MutableStateFlow(false)
    val showTasksOverview: StateFlow<Boolean> = _showTasksOverview.asStateFlow()

    private val _isAxionPcModeFocused = MutableStateFlow(true)
    val isAxionPcModeFocused: StateFlow<Boolean> = _isAxionPcModeFocused.asStateFlow()

    private var lastDismissTime = 0L

    fun updateFocus(isFocused: Boolean) {
        _isAxionPcModeFocused.value = isFocused
        if (!isFocused) {
            dismissAllPanels()
        }
    }

    fun toggleStartMenu() {
        val now = System.currentTimeMillis()
        if (now - lastDismissTime < 200) return

        val newState = !_showStartMenu.value
        dismissAllPanels()
        _showStartMenu.value = newState
    }

    fun toggleNotificationPanel() {
        val now = System.currentTimeMillis()
        if (now - lastDismissTime < 200) return

        val newState = !_showNotificationPanel.value
        dismissAllPanels()
        _showNotificationPanel.value = newState
    }

    fun toggleQuickSettingsPanel() {
        val now = System.currentTimeMillis()
        if (now - lastDismissTime < 200) return

        val newState = !_showQuickSettingsPanel.value
        dismissAllPanels()
        _showQuickSettingsPanel.value = newState
    }

    fun toggleMediaPlayer() {
        val now = System.currentTimeMillis()
        if (now - lastDismissTime < 200) return

        val newState = !_showMediaPlayer.value
        dismissAllPanels()
        _showMediaPlayer.value = newState
    }

    fun toggleTasksOverview() {
        val now = System.currentTimeMillis()
        if (now - lastDismissTime < 200) return

        val newState = !_showTasksOverview.value
        dismissAllPanels()
        _showTasksOverview.value = newState
    }

    private val _pinnedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val pinnedApps: StateFlow<List<AppInfo>> = _pinnedApps.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _desktopApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val desktopApps: StateFlow<List<AppInfo>> = _desktopApps.asStateFlow()

    private val _runningApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val runningApps: StateFlow<List<AppInfo>> = _runningApps.asStateFlow()

    fun updatePinnedApps(apps: List<AppInfo>) {
        _pinnedApps.value = apps
    }

    fun updateAllApps(apps: List<AppInfo>) {
        _allApps.value = apps
    }

    fun updateDesktopApps(apps: List<AppInfo>) {
        _desktopApps.value = apps
    }

    fun updateRunningApps(apps: List<AppInfo>) {
        _runningApps.value = apps
    }

    fun dismissAllPanels(fromOutside: Boolean = false) {
        if (fromOutside) {
            lastDismissTime = System.currentTimeMillis()
        }
        _showStartMenu.value = false
        _showNotificationPanel.value = false
        _showQuickSettingsPanel.value = false
        _showMediaPlayer.value = false
        _showTasksOverview.value = false
    }

    fun isAnyPanelShowing(): Boolean {
        return _showStartMenu.value ||
            _showNotificationPanel.value ||
            _showQuickSettingsPanel.value ||
            _showMediaPlayer.value ||
            _showTasksOverview.value
    }
}
