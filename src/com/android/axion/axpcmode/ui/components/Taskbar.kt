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

package com.android.axion.axpcmode.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.axion.axpcmode.ui.components.taskbar.*
import com.android.axion.axpcmode.utils.AppInfo

private const val MAX_VISIBLE_TASKS = 4
private val MAX_TASKS_WIDTH = (MAX_VISIBLE_TASKS * 48 + (MAX_VISIBLE_TASKS - 1) * 4).dp

@Composable
fun Taskbar(
    pinnedApps: List<AppInfo>,
    runningApps: List<AppInfo>,
    onStartClick: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onImeClick: () -> Unit,
    onMediaClick: () -> Unit,
    onQuickSettingsClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemState = rememberTaskbarSystemState()

    val pinnedPackageNames = remember(pinnedApps) { pinnedApps.map { it.packageName }.toSet() }
    val unpinnedRunningApps =
        remember(runningApps, pinnedPackageNames) {
            runningApps.filter { !pinnedPackageNames.contains(it.packageName) }
        }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().height(56.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TaskbarNavigation(
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
                onRecentsClick = onRecentsClick,
            )

            TaskbarDivider()

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                TaskbarIconButton(onClick = onStartClick, hasBackground = true) {
                    AxionLogo(modifier = Modifier.size(22.dp))
                }

                TaskbarDivider()

                Row(
                    modifier =
                        Modifier.widthIn(max = MAX_TASKS_WIDTH)
                            .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    pinnedApps.forEach { app ->
                        val runningTask = runningApps.find { it.packageName == app.packageName }
                        val appToUse = runningTask ?: app
                        val isRunning = runningTask != null

                        TaskbarIcon(
                            app = appToUse,
                            isRunning = isRunning,
                            isPinned = true,
                            pinnedCount = pinnedApps.size,
                            onClick = { onAppClick(appToUse) },
                        )
                    }

                    if (unpinnedRunningApps.isNotEmpty()) {
                        unpinnedRunningApps.forEach { app ->
                            TaskbarIcon(
                                app = app,
                                isRunning = true,
                                isPinned = false,
                                pinnedCount = pinnedApps.size,
                                onClick = { onAppClick(app) },
                            )
                        }
                    }
                }
            }

            TaskbarDivider()

            TaskbarSystemTray(
                state = systemState,
                onMediaClick = onMediaClick,
                onQuickSettingsClick = onQuickSettingsClick,
                onNotificationClick = onNotificationClick,
            )
        }
    }
}
