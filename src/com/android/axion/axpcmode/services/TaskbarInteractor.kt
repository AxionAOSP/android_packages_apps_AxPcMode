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

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.input.InputManager
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import com.android.axion.axpcmode.activities.PcModeLauncherActivity
import com.android.axion.axpcmode.activities.SecondaryPcModeLauncherActivity
import com.android.axion.axpcmode.activities.TasksOverviewActivity
import com.android.axion.axpcmode.ui.PcModeLauncherViewModel
import com.android.axion.axpcmode.utils.AppInfo
import com.android.axion.axpcmode.utils.AppUtils

interface TaskbarHost {
    fun hideTaskbarImmediately()
    fun revealTaskbar()
    fun resetAutoHideTimer()
}

class TaskbarInteractor(
    private val context: Context,
    private val viewModel: PcModeLauncherViewModel,
    private val host: TaskbarHost,
    private val inputMethodManager: InputMethodManager,
    val targetDisplayId: Int = Display.DEFAULT_DISPLAY,
) {
    companion object {
        private const val TAG = "TaskbarInteractor"
    }

    private val inputManager: InputManager? =
        context.getSystemService(InputManager::class.java)

    fun onStartClick() {
        viewModel.toggleStartMenu()
    }

    fun onAppClick(app: AppInfo) {
        AppUtils.launchApp(context, app.packageName, app.className, targetDisplayId)
    }

    fun onRecentsClick() {
        host.hideTaskbarImmediately()
        try {
            val intent = Intent(context, TasksOverviewActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent, makeDisplayOptions())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch recents: ${e.message}")
        }
    }

    fun onHomeClick() {
        if (viewModel.closeWallpaperSettingsIfOpen()) return

        try {
            val activityClass = if (targetDisplayId != Display.DEFAULT_DISPLAY)
                SecondaryPcModeLauncherActivity::class.java
            else PcModeLauncherActivity::class.java
            val intent = Intent(context, activityClass)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            context.startActivity(intent, makeDisplayOptions())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show desktop: ${e.message}")
        }
    }

    fun onBackClick() {
        if (viewModel.closeWallpaperSettingsIfOpen()) return

        try {
            val now = SystemClock.uptimeMillis()
            val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0, 0,
                -1, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD)
            val up = KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0, 0,
                -1, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD)
            down.displayId = targetDisplayId
            up.displayId = targetDisplayId
            inputManager?.injectInputEvent(down, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
            inputManager?.injectInputEvent(up, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject back key: ${e.message}")
        }
    }

    fun onImeClick() {
        inputMethodManager.showInputMethodPicker()
    }

    fun onMediaClick() {
        viewModel.toggleMediaPlayer()
    }

    fun onQuickSettingsClick() {
        viewModel.toggleQuickSettingsPanel()
    }

    fun onNotificationClick() {
        viewModel.toggleNotificationPanel()
    }

    fun onRevealTaskbar() {
        host.revealTaskbar()
    }

    fun onUserInteraction() {
        host.resetAutoHideTimer()
    }

    private fun makeDisplayOptions(): android.os.Bundle? {
        if (targetDisplayId == Display.DEFAULT_DISPLAY) return null
        return ActivityOptions.makeBasic().apply {
            launchDisplayId = targetDisplayId
        }.toBundle()
    }
}
