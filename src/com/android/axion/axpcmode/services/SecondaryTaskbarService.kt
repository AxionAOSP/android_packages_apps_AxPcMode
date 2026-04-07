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
import android.util.Log
import android.os.IBinder
import android.os.UserHandle
import android.provider.Settings
import android.view.Display
import com.android.axion.axpcmode.activities.MousePadActivity
import com.android.axion.axpcmode.ui.MousePadFloatingButton
import com.android.axion.axpcmode.ui.PcModeLauncherViewModel
import com.android.axion.axpcmode.ui.QuickSettingsViewModel
import com.android.axion.axpcmode.utils.AppInfo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint(BaseTaskbarService::class)
class SecondaryTaskbarService : Hilt_SecondaryTaskbarService() {

    companion object {
        const val ACTION_START = "com.android.axion.axpcmode.START_SECONDARY_TASKBAR"
        const val ACTION_STOP = "com.android.axion.axpcmode.STOP_SECONDARY_TASKBAR"
        private const val NOTIFICATION_ID = 1002

        fun start(context: Context) {
            val intent = Intent(context, SecondaryTaskbarService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SecondaryTaskbarService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override val tag = "SecondaryTaskbarService"
    override val notificationId = NOTIFICATION_ID
    override val startAction = ACTION_START
    override val stopAction = ACTION_STOP

    @Inject override lateinit var vm: PcModeLauncherViewModel
    @Inject override lateinit var qsViewModel: QuickSettingsViewModel
    @Inject override lateinit var mediaRepository: MediaRepository
    @Inject override lateinit var calendarWeatherRepository: CalendarWeatherRepository
    @Inject override lateinit var notificationHelper: TaskbarNotificationHelper
    @Inject override lateinit var appRepository: TaskbarAppRepository

    private var mousePadFab: MousePadFloatingButton? = null

    override fun resolveTargetDisplayId(): Int {
        val id = Settings.Secure.getIntForUser(
            contentResolver, "ax_pc_mode_target_display_id",
            Display.INVALID_DISPLAY, UserHandle.USER_CURRENT
        )
        return if (id != Display.INVALID_DISPLAY && id != Display.DEFAULT_DISPLAY) id
        else Display.INVALID_DISPLAY
    }

    override fun onCreate() {
        if (resolveTargetDisplayId() == Display.INVALID_DISPLAY) {
            Log.w(tag, "No valid secondary display, stopping")
            stopSelf()
            return
        }
        super.onCreate()
        panelOverlayManager.setOnPanelShownListener {
            MousePadActivity.cursorBringToFrontCallback?.invoke()
        }
    }

    override fun onTaskbarShown() {
        mousePadFab = MousePadFloatingButton(this)
        serviceScope.launch {
            appRepository.topTaskPackage.collect {
                if (MousePadActivity.isActive) mousePadFab?.hide() else mousePadFab?.show()
            }
        }
        if (!MousePadActivity.isActive) mousePadFab?.show()
    }

    private val binder = SecondaryBinder()

    inner class SecondaryBinder : Binder() {
        fun getService(): SecondaryTaskbarService = this@SecondaryTaskbarService
        fun updatePinnedApps(apps: List<AppInfo>) { vm.updatePinnedApps(apps) }
        fun showTaskbar() { revealTaskbar() }
        fun hideTaskbar() { hideTaskbarImmediately() }
        fun resetAutoHideTimer() { this@SecondaryTaskbarService.resetAutoHideTimer() }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun cleanup() {
        mousePadFab?.hide()
        mousePadFab = null
        super.cleanup()
    }
}
