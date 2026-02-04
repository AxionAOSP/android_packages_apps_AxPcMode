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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.android.axion.axpcmode.R
import com.android.axion.axpcmode.activities.PcModeLauncherActivity
import com.android.axion.axpcmode.utils.TaskbarConstants

class TaskbarNotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                    TaskbarConstants.CHANNEL_ID,
                    "PC Mode Taskbar",
                    NotificationManager.IMPORTANCE_LOW,
                )
                .apply {
                    description = "Keeps the taskbar running in PC mode"
                    setShowBadge(false)
                }
        notificationManager.createNotificationChannel(channel)
    }

    fun createForegroundNotification(): Notification {
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, PcModeLauncherActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )

        return Notification.Builder(context, TaskbarConstants.CHANNEL_ID)
            .setContentTitle("PC Mode Active")
            .setContentText("Taskbar is running")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
