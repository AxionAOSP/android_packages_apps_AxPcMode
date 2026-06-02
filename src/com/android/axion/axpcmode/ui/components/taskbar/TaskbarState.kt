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

package com.android.axion.axpcmode.ui.components.taskbar

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.axion.platform.AxFeatureState
import com.android.axion.platform.AxPlatformClient
import com.android.axion.platform.AxPlatformFeature
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executor
import kotlinx.coroutines.delay

class TaskbarSystemState(
    val currentTime: String,
    val currentDate: String,
    val wifiLevel: Int,
    val isWifiConnected: Boolean,
    val isWifiEnabled: Boolean,
    val isBtEnabled: Boolean,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val mobileDataLevel: Int,
    val isMobileDataEnabled: Boolean,
    val isSimPresent: Boolean,
    val mobileDataType: String,
    val bluetoothBatteryLevel: Int = -1,
)

@Composable
fun rememberTaskbarSystemState(): TaskbarSystemState {
    val client = remember { AxPlatformClient.getInstance() }

    var wifiLevel by remember { mutableIntStateOf(0) }
    var isWifiConnected by remember { mutableStateOf(false) }
    var isWifiEnabled by remember { mutableStateOf(false) }
    var isBtEnabled by remember { mutableStateOf(false) }

    var batteryLevel by remember { mutableIntStateOf(-1) }
    var isCharging by remember { mutableStateOf(false) }
    var mobileDataLevel by remember { mutableIntStateOf(4) }
    var isMobileDataEnabled by remember { mutableStateOf(false) }
    var isSimPresent by remember { mutableStateOf(false) }
    var mobileDataType by remember { mutableStateOf("") }
    var bluetoothBatteryLevel by remember { mutableIntStateOf(-1) }

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    fun updateBluetoothBattery(devices: ArrayList<Bundle>?) {
        var bestLevel = -1
        devices?.forEach { device ->
            if (device.getBoolean("isConnected", false)) {
                val level = device.getInt("batteryLevel", -1)
                if (level > -1) {
                    bestLevel = level
                    return@forEach
                }
            }
        }
        bluetoothBatteryLevel = bestLevel
    }

    fun updateState(key: String, state: AxFeatureState) {
        when (key) {
            AxPlatformClient.KEY_BATTERY -> {
                batteryLevel = state.getInt("level", 0)
                isCharging = state.getBoolean("isCharging", false) ||
                    state.getBoolean("isPluggedIn", false)
            }
            AxPlatformFeature.MOBILE_DATA -> {
                isMobileDataEnabled = state.isActive
                mobileDataLevel = state.getInt("level", 4)
                mobileDataType = state.getString("type", "")
                isSimPresent = state.isAvailable
            }
            AxPlatformFeature.BLUETOOTH -> {
                isBtEnabled = state.isActive
                updateBluetoothBattery(state.toBundle().getParcelableArrayList<Bundle>("devices"))
            }
            AxPlatformFeature.WIFI -> {
                isWifiEnabled = state.isActive
                isWifiConnected = state.getBoolean("connected", false)
            }
            AxPlatformClient.KEY_WIFI_SCAN -> {
                val networks = state.toBundle().getParcelableArrayList<Bundle>("networks")
                val connected = networks?.firstOrNull { it.getBoolean("isConnected", false) }
                wifiLevel = connected?.getInt("level", 0) ?: 0
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
            currentDate = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now.time)
            delay(60000)
        }
    }

    DisposableEffect(client) {
        val handler = Handler(Looper.getMainLooper())
        val executor = Executor { command -> handler.post(command) }
        val callback = AxPlatformClient.StateCallback { key, state -> updateState(key, state) }

        client.registerCallback(executor, callback)
        updateState(AxPlatformClient.KEY_BATTERY, client.getState(AxPlatformClient.KEY_BATTERY))
        updateState(AxPlatformFeature.MOBILE_DATA, client.getState(AxPlatformFeature.MOBILE_DATA))
        updateState(AxPlatformFeature.BLUETOOTH, client.getState(AxPlatformFeature.BLUETOOTH))
        updateState(AxPlatformFeature.WIFI, client.getState(AxPlatformFeature.WIFI))

        onDispose { client.unregisterCallback(callback) }
    }

    return TaskbarSystemState(
        currentTime = currentTime,
        currentDate = currentDate,
        wifiLevel = wifiLevel,
        isWifiConnected = isWifiConnected,
        isWifiEnabled = isWifiEnabled,
        isBtEnabled = isBtEnabled,
        batteryLevel = batteryLevel,
        isCharging = isCharging,
        mobileDataLevel = mobileDataLevel,
        isMobileDataEnabled = isMobileDataEnabled,
        isSimPresent = isSimPresent,
        mobileDataType = mobileDataType,
        bluetoothBatteryLevel = bluetoothBatteryLevel,
    )
}
