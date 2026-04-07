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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.provider.CalendarContract
import android.util.Log
import com.android.axion.quicklook.IAxQuickLookService
import com.android.axion.quicklook.IQuickLookCallback
import com.android.axion.quicklook.QuickLookTarget
import com.android.axion.quicklook.WeatherData
import com.android.axion.quicklook.calendarData
import com.android.axion.quicklook.weatherData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class CalendarEvent(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val allDay: Boolean,
    val color: Int,
)

class CalendarWeatherRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())

    private val _weather = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather.asStateFlow()

    private val _weatherIcon = MutableStateFlow<Bitmap?>(null)
    val weatherIcon: StateFlow<Bitmap?> = _weatherIcon.asStateFlow()

    private val _todayEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val todayEvents: StateFlow<List<CalendarEvent>> = _todayEvents.asStateFlow()

    private var service: IAxQuickLookService? = null
    private var isBound = false
    private var rebindAttempts = 0
    private var calendarObserver: ContentObserver? = null

    private val quickLookCallback = object : IQuickLookCallback.Stub() {
        override fun onTargetsUpdated(targets: MutableList<QuickLookTarget>) {
            handler.post { processTargets(targets) }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IAxQuickLookService.Stub.asInterface(binder)
            rebindAttempts = 0
            try {
                service?.registerCallback(quickLookCallback)
                service?.getCurrentTargets()?.let { handler.post { processTargets(it) } }
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to register callback", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            scheduleRebind()
        }
    }

    fun start() {
        bindToAxQuickLook()
        scope.launch { refreshTodayEvents() }

        calendarObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                scope.launch { refreshTodayEvents() }
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI, true, calendarObserver!!
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register calendar observer", e)
        }
    }

    fun stop() {
        if (isBound) {
            try { service?.unregisterCallback(quickLookCallback) } catch (_: RemoteException) {}
            try { context.unbindService(serviceConnection) } catch (_: Exception) {}
            service = null
            isBound = false
        }
        calendarObserver?.let {
            try { context.contentResolver.unregisterContentObserver(it) } catch (_: Exception) {}
            calendarObserver = null
        }
        handler.removeCallbacksAndMessages(null)
    }

    private fun bindToAxQuickLook() {
        if (isBound) return
        val intent = Intent(SERVICE_ACTION).setPackage(SERVICE_PACKAGE)
        try {
            isBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to bind to AxQuickLook", e)
        }
    }

    private fun scheduleRebind() {
        if (!isBound) return
        rebindAttempts++
        val delay = (REBIND_DELAY_MS * rebindAttempts).coerceAtMost(MAX_REBIND_DELAY_MS)
        handler.postDelayed(::bindToAxQuickLook, delay)
    }

    private fun processTargets(targets: List<QuickLookTarget>) {
        var weather: WeatherData? = null
        for (target in targets) {
            if (target.targetType == QuickLookTarget.TYPE_WEATHER) {
                weather = target.weatherData
                break
            }
        }
        _weather.value = weather
        _weatherIcon.value = weather?.iconBytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }

    private fun refreshTodayEvents() {
        try {
            val now = Calendar.getInstance()
            val startOfDay = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endOfDay = startOfDay + DAY_MS - 1

            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(startOfDay.toString())
                .appendPath(endOfDay.toString())
                .build()

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_COLOR,
            )

            val events = mutableListOf<CalendarEvent>()
            context.contentResolver.query(
                uri, projection, null, null, CalendarContract.Instances.BEGIN
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            title = cursor.getString(0) ?: "",
                            startMs = cursor.getLong(1),
                            endMs = cursor.getLong(2),
                            allDay = cursor.getInt(3) != 0,
                            color = cursor.getInt(4),
                        )
                    )
                }
            }
            _todayEvents.value = events
        } catch (e: Exception) {
            Log.w(TAG, "Calendar query failed", e)
            _todayEvents.value = emptyList()
        }
    }

    companion object {
        private const val TAG = "CalendarWeatherRepo"
        private const val SERVICE_ACTION = "com.android.axion.quicklook.SERVICE"
        private const val SERVICE_PACKAGE = "com.android.axion.quicklook"
        private const val REBIND_DELAY_MS = 5000L
        private const val MAX_REBIND_DELAY_MS = 30000L
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
