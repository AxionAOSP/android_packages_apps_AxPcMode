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

package com.android.axion.axpcmode.activities

import android.R
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.axion.axpcmode.ui.components.TasksOverviewPanel
import com.android.axion.axpcmode.ui.theme.AxPcModeTheme
import com.android.axion.axpcmode.utils.AppInfo
import com.android.axion.axpcmode.utils.AppUtils

class TasksOverviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        window.setBackgroundDrawableResource(R.color.transparent)

        setContent {
            val runningTasks = remember { loadRunningTasks() }

            val tasksWithDesktop =
                remember(runningTasks) {
                    val desktopIcon =
                        try {
                            packageManager.getApplicationIcon("com.android.axion.axpcmode")
                        } catch (e: Exception) {
                            getDrawable(R.drawable.ic_menu_view)!!
                        }

                    val desktopCard =
                        AppInfo(
                            label = "Desktop",
                            packageName = "com.android.axion.axpcmode",
                            className = "PcModeLauncherActivity",
                            icon = desktopIcon,
                            taskId = -2,
                        )
                    listOf(desktopCard) + runningTasks
                }

            AxPcModeTheme {
                TasksOverviewPanel(
                    windows = emptyList(),
                    runningTasks = tasksWithDesktop,
                    onTaskClick = {},
                    onRunningTaskClick = { appInfo ->
                        if (appInfo.taskId == -2) {

                            showDesktop()
                        } else {
                            bringTaskToFront(appInfo)
                        }
                        finish()
                    },
                    onTaskClose = { appInfo ->
                        if (appInfo.taskId > 0) {
                            closeTask(appInfo)
                        }
                    },
                    onClearAllTasks = {
                        clearAllTasks(runningTasks)
                        finish()
                    },
                    onDismiss = { finish() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun loadRunningTasks(): List<AppInfo> {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = packageManager
        val tasks = mutableListOf<AppInfo>()

        try {
            val targetDisplayId = display?.displayId ?: android.view.Display.DEFAULT_DISPLAY
            val atm = android.app.ActivityTaskManager.getService()
            val runningTasks = atm.getTasks(20, false, false, targetDisplayId)

            runningTasks.forEach { taskInfo ->
                val componentName = taskInfo.baseActivity ?: taskInfo.topActivity
                if (componentName != null) {

                    if (componentName.packageName == packageName) return@forEach
                    if (isLauncherPackage(componentName.packageName)) return@forEach

                    try {
                        val appInfo = pm.getApplicationInfo(componentName.packageName, 0)
                        val label = pm.getApplicationLabel(appInfo).toString()
                        val icon = pm.getApplicationIcon(appInfo)

                        tasks.add(
                            AppInfo(
                                label = label,
                                packageName = componentName.packageName,
                                className = componentName.className,
                                icon = icon,
                                taskId = taskInfo.taskId,
                            )
                        )
                    } catch (e: PackageManager.NameNotFoundException) {}
                }
            }
        } catch (e: Exception) {
            Log.e("TasksOverviewActivity", "Failed to load running tasks", e)
        }

        return tasks
    }

    private fun isLauncherPackage(packageName: String): Boolean {
        return packageName == "com.android.launcher3" ||
            packageName == "com.google.android.apps.nexuslauncher" ||
            packageName.contains("launcher")
    }

    private fun bringTaskToFront(appInfo: AppInfo) {
        val displayId = display?.displayId ?: android.view.Display.DEFAULT_DISPLAY
        if (appInfo.taskId != -1) {
            try {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.moveTaskToFront(appInfo.taskId, ActivityManager.MOVE_TASK_WITH_HOME)
            } catch (e: Exception) {
                AppUtils.launchApp(this, appInfo.packageName, appInfo.className, displayId)
            }
        } else {
            AppUtils.launchApp(this, appInfo.packageName, appInfo.className, displayId)
        }
    }

    private fun closeTask(appInfo: AppInfo) {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.forceStopPackage(appInfo.packageName)
        } catch (e: Exception) {
            Log.e("TasksOverviewActivity", "Failed to close task ${appInfo.packageName}", e)
        }
    }

    private fun clearAllTasks(tasks: List<AppInfo>) {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        tasks.forEach { task ->
            try {
                am.forceStopPackage(task.packageName)
            } catch (e: Exception) {
                Log.e("TasksOverviewActivity", "Failed to force stop ${task.packageName}", e)
            }
        }
    }

    private fun showDesktop() {
        try {
            val intent = Intent(this, PcModeLauncherActivity::class.java)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("TasksOverviewActivity", "Failed to show desktop", e)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
