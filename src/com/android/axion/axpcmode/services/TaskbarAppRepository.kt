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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.android.axion.axpcmode.di.MainScope
import com.android.axion.axpcmode.ui.PcModeLauncherViewModel
import com.android.axion.axpcmode.utils.AppUtils
import com.android.axion.axpcmode.utils.TaskbarConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Singleton
class TaskbarAppRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val vm: PcModeLauncherViewModel,
    private val taskMonitor: TaskMonitor,
    @MainScope private val scope: CoroutineScope,
) {

    private val refreshReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    TaskbarConstants.ACTION_REFRESH_PINNED_APPS -> {
                        vm.onRefreshPinnedApps?.invoke()
                    }
                    TaskbarConstants.ACTION_REFRESH_DESKTOP_APPS -> {
                        vm.onRefreshDesktopApps?.invoke()
                    }
                }
            }
        }

    private val packageReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                vm.onRefreshPinnedApps?.invoke()
                vm.onRefreshDesktopApps?.invoke()

                scope.launch {
                    val allApps = AppUtils.getInstalledApps(context!!)
                    vm.updateAllApps(allApps)
                }
            }
        }

    private val _topTaskPackage = MutableStateFlow<String?>(null)
    val topTaskPackage: StateFlow<String?> = _topTaskPackage

    fun init() {

        scope.launch {
            val allApps = AppUtils.getInstalledApps(context)
            vm.updateAllApps(allApps)
            vm.updatePinnedApps(AppUtils.getPinnedApps(context))
            vm.updateDesktopApps(AppUtils.getDesktopApps(context))
        }

        scope.launch { taskMonitor.runningTasks.collect { apps -> vm.updateRunningApps(apps) } }

        scope.launch { taskMonitor.topTaskPackage.collect { pkg -> _topTaskPackage.value = pkg } }

        val refreshFilter =
            IntentFilter().apply {
                addAction(TaskbarConstants.ACTION_REFRESH_PINNED_APPS)
                addAction(TaskbarConstants.ACTION_REFRESH_DESKTOP_APPS)
            }
        context.registerReceiver(refreshReceiver, refreshFilter, Context.RECEIVER_NOT_EXPORTED)

        val packageFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
        context.registerReceiver(packageReceiver, packageFilter)

        vm.onRefreshPinnedApps = {
            scope.launch {
                val pinnedApps = AppUtils.getPinnedApps(context)
                vm.updatePinnedApps(pinnedApps)
            }
        }
        vm.onRefreshDesktopApps = {
            scope.launch {
                val desktopApps = AppUtils.getDesktopApps(context)
                vm.updateDesktopApps(desktopApps)
            }
        }
    }

    fun cleanup() {

        try {
            context.unregisterReceiver(refreshReceiver)
            context.unregisterReceiver(packageReceiver)
        } catch (e: Exception) {}
    }
}
