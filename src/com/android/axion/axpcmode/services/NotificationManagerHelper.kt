package com.android.axion.axpcmode.services

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import android.util.Log

class NotificationManagerHelper(private val context: Context) {
    private val listener = AxNotificationListener()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var previousFilter = NotificationManager.INTERRUPTION_FILTER_ALL

    fun register() {
        try {
            val componentName = ComponentName(context, AxNotificationListener::class.java)
            listener.registerAsSystemService(context, componentName, UserHandle.USER_CURRENT)
            Log.d("AxPcMode", "Notification Listener Registered")
        } catch (e: Exception) {
            Log.e("AxPcMode", "Failed to register notification listener", e)
        }
    }

    fun unregister() {
        try {
            listener.unregisterAsSystemService()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
