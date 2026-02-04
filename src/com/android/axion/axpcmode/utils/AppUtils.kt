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

package com.android.axion.axpcmode.utils

import android.app.ActivityManager
import android.app.FreeformLauncher
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by
    preferencesDataStore(name = "axpcmode_prefs")

object AppUtils {
    private val PINNED_APPS_KEY = stringSetPreferencesKey("pinned_apps")

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val apps = pm.queryIntentActivities(intent, 0)
        return apps
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    className = resolveInfo.activityInfo.name,
                    label = resolveInfo.loadLabel(pm).toString(),
                    icon = resolveInfo.loadIcon(pm),
                )
            }
            .sortedBy { it.label }
    }

    private val DESKTOP_APPS_KEY = stringSetPreferencesKey("desktop_apps")

    fun getPinnedApps(context: Context): List<AppInfo> {
        return runBlocking {
            val pinnedPackages =
                context.dataStore.data
                    .map { preferences -> preferences[PINNED_APPS_KEY] ?: emptySet() }
                    .first()

            val allApps = getInstalledApps(context)
            allApps.filter { it.packageName in pinnedPackages }
        }
    }

    fun getDesktopApps(context: Context): List<AppInfo> {
        return runBlocking {
            val desktopPackages =
                context.dataStore.data
                    .map { preferences -> preferences[DESKTOP_APPS_KEY] ?: emptySet() }
                    .first()

            val allApps = getInstalledApps(context)
            allApps.filter { it.packageName in desktopPackages }
        }
    }

    fun pinApp(context: Context, packageName: String) {
        runBlocking {
            context.dataStore.edit { preferences ->
                val current = preferences[PINNED_APPS_KEY] ?: emptySet()
                preferences[PINNED_APPS_KEY] = current + packageName
            }
        }
    }

    fun unpinApp(context: Context, packageName: String) {
        runBlocking {
            context.dataStore.edit { preferences ->
                val current = preferences[PINNED_APPS_KEY] ?: emptySet()
                preferences[PINNED_APPS_KEY] = current - packageName
            }
        }
    }

    fun addAppToDesktop(context: Context, packageName: String) {
        runBlocking {
            context.dataStore.edit { preferences ->
                val current = preferences[DESKTOP_APPS_KEY] ?: emptySet()
                preferences[DESKTOP_APPS_KEY] = current + packageName
            }
        }
        context.sendBroadcast(Intent("REFRESH_DESKTOP_APPS"))
    }

    fun removeAppFromDesktop(context: Context, packageName: String) {
        runBlocking {
            context.dataStore.edit { preferences ->
                val current = preferences[DESKTOP_APPS_KEY] ?: emptySet()
                preferences[DESKTOP_APPS_KEY] = current - packageName
            }
        }
        context.sendBroadcast(Intent("REFRESH_DESKTOP_APPS"))
    }

    fun launchApp(context: Context, packageName: String, className: String) {
        try {
            val intent =
                Intent().apply {
                    setClassName(packageName, className)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent != null) {
                    context.startActivity(intent)
                }
            } catch (e2: Exception) {
                Log.e("AppUtils", "Failed to launch app", e2)
            }
        }
    }

    fun launchAppInFreeform(packageName: String, className: String) {
        try {
            FreeformLauncher.launchDesktopApp(packageName, className)
        } catch (e: Exception) {
            Log.e("AppUtils", "Failed to launch app in freeform", e)
        }
    }

    fun launchAppInfo(context: Context, packageName: String) {
        try {
            val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUtils", "Failed to launch app info", e)
        }
    }

    fun getRunningTasks(context: Context): List<AppInfo> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager
        val tasks = am.getRunningTasks(50)

        return tasks.mapNotNull { task ->
            try {
                val pkg = task.baseActivity?.packageName ?: return@mapNotNull null

                if (pkg == context.packageName) return@mapNotNull null

                val appInfo = pm.getApplicationInfo(pkg, 0)
                AppInfo(
                    packageName = pkg,
                    className = task.baseActivity?.className ?: "",
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo),
                    taskId = task.id,
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun switchToTask(context: Context, taskId: Int) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
    }
}

data class AppInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Drawable,
    val taskId: Int = -1,
)
