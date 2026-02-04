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
import android.content.Intent
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.android.axion.axpcmode.activities.PcModeLauncherActivity
import com.android.axion.axpcmode.activities.TasksOverviewActivity
import com.android.axion.axpcmode.ui.PcModeLauncherViewModel
import com.android.axion.axpcmode.utils.AppInfo
import com.android.axion.axpcmode.utils.AppUtils

class TaskbarInteractor(
    private val context: Context,
    private val viewModel: PcModeLauncherViewModel,
    private val service: TaskbarService,
    private val inputMethodManager: InputMethodManager,
) {
    companion object {
        private const val TAG = "TaskbarInteractor"
    }

    fun onStartClick() {
        viewModel.toggleStartMenu()
    }

    fun onAppClick(app: AppInfo) {
        AppUtils.launchApp(context, app.packageName, app.className)
    }

    fun onRecentsClick() {
        service.hideTaskbarImmediately()
        val intent = Intent(context, TasksOverviewActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun onHomeClick() {
        try {
            val intent = Intent(context, PcModeLauncherActivity::class.java)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show desktop: ${e.message}")
        }
    }

    fun onBackClick() {
        try {
            Runtime.getRuntime().exec("input keyevent 4")
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
        service.revealTaskbar()
    }

    fun onUserInteraction() {
        service.resetAutoHideTimer()
    }
}
