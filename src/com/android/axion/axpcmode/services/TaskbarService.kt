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
import android.os.Binder
import android.os.IBinder
import android.view.Display
import com.android.axion.axpcmode.activities.PcModeAnimationActivity
import com.android.axion.axpcmode.ui.PcModeLauncherViewModel
import com.android.axion.axpcmode.ui.QuickSettingsViewModel
import com.android.axion.axpcmode.utils.AppInfo
import com.android.axion.axpcmode.utils.TaskbarConstants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(BaseTaskbarService::class)
class TaskbarService : Hilt_TaskbarService() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, TaskbarService::class.java).apply {
                action = TaskbarConstants.ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TaskbarService::class.java).apply {
                action = TaskbarConstants.ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override val tag = "TaskbarService"
    override val notificationId = TaskbarConstants.NOTIFICATION_ID
    override val startAction = TaskbarConstants.ACTION_START
    override val stopAction = TaskbarConstants.ACTION_STOP

    @Inject override lateinit var vm: PcModeLauncherViewModel
    @Inject override lateinit var qsViewModel: QuickSettingsViewModel
    @Inject override lateinit var mediaRepository: MediaRepository
    @Inject override lateinit var notificationHelper: TaskbarNotificationHelper
    @Inject override lateinit var appRepository: TaskbarAppRepository

    override fun resolveTargetDisplayId() = Display.DEFAULT_DISPLAY

    override fun onCreate() {
        super.onCreate()
        vm.onExitPcMode = { dismissAndExit() }
    }

    override fun onTaskbarShown() {}

    private val binder = TaskbarBinder()

    inner class TaskbarBinder : Binder() {
        fun getService(): TaskbarService = this@TaskbarService
        fun updatePinnedApps(apps: List<AppInfo>) { vm.updatePinnedApps(apps) }
        fun showTaskbar() { revealTaskbar() }
        fun hideTaskbar() { hideTaskbarImmediately() }
        fun resetAutoHideTimer() { this@TaskbarService.resetAutoHideTimer() }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun dismissAndExit() {
        vm.dismissAllPanels()
        autoHideJob?.cancel()
        _isTaskbarVisible.value = false
        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        val animIntent = Intent(this, PcModeAnimationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(PcModeAnimationActivity.EXTRA_EXIT_PC_MODE, true)
        }
        startActivity(animIntent)
    }
}
