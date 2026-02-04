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

object TaskbarConstants {
    const val NOTIFICATION_ID = 1001
    const val CHANNEL_ID = "taskbar_service_channel"
    const val AUTO_HIDE_DELAY_MS = 3000L
    const val EDGE_HEIGHT_DP = 24

    const val ACTION_START = "com.android.axion.axpcmode.START_TASKBAR"
    const val ACTION_STOP = "com.android.axion.axpcmode.STOP_TASKBAR"
    const val ACTION_SHOW = "com.android.axion.axpcmode.SHOW_TASKBAR"
    const val ACTION_HIDE = "com.android.axion.axpcmode.HIDE_TASKBAR"
    const val ACTION_TOGGLE_START_MENU = "com.android.axion.axpcmode.TOGGLE_START_MENU"
    const val ACTION_TOGGLE_QUICK_SETTINGS = "com.android.axion.axpcmode.TOGGLE_QUICK_SETTINGS"
    const val ACTION_TOGGLE_NOTIFICATIONS = "com.android.axion.axpcmode.TOGGLE_NOTIFICATIONS"
    const val ACTION_REFRESH_PINNED_APPS = "com.android.axion.axpcmode.REFRESH_PINNED_APPS"
    const val ACTION_REFRESH_DESKTOP_APPS = "com.android.axion.axpcmode.REFRESH_DESKTOP_APPS"
}
