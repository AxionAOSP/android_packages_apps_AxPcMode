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

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.hardware.display.BrightnessInfo
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject

data class QSTileData(
    val spec: String,
    val label: String,
    val secondaryLabel: String,
    val state: Int,
    val isTransient: Boolean,
)

data class AvailableTileData(
    val spec: String,
    val label: String,
    val isSystem: Boolean,
    val packageName: String? = null,
)

class QuickSettingsRepository(val context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val contentResolver = context.contentResolver
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isWifiEnabled = MutableStateFlow(false)
    val isWifiEnabled = _isWifiEnabled.asStateFlow()

    private val _wifiSsid = MutableStateFlow("")
    val wifiSsid = _wifiSsid.asStateFlow()

    private val _wifiScanListJson = MutableStateFlow("")
    val wifiScanListJson = _wifiScanListJson.asStateFlow()

    private val _isBtEnabled = MutableStateFlow(false)
    val isBtEnabled = _isBtEnabled.asStateFlow()

    private val _btDevicesJson = MutableStateFlow("")
    val btDevicesJson = _btDevicesJson.asStateFlow()

    private val _isDndEnabled = MutableStateFlow(false)
    val isDndEnabled = _isDndEnabled.asStateFlow()

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn = _isFlashlightOn.asStateFlow()

    private val _isMobileDataEnabled = MutableStateFlow(false)
    val isMobileDataEnabled = _isMobileDataEnabled.asStateFlow()

    private val _mobileDataInfo = MutableStateFlow("")
    val mobileDataInfo = _mobileDataInfo.asStateFlow()

    private val _isHotspotEnabled = MutableStateFlow(false)
    val isHotspotEnabled = _isHotspotEnabled.asStateFlow()

    private val _hotspotInfo = MutableStateFlow("")
    val hotspotInfo = _hotspotInfo.asStateFlow()

    private val _isAutoRotate = MutableStateFlow(true)
    val isAutoRotate = _isAutoRotate.asStateFlow()

    private val _isAirplaneMode = MutableStateFlow(false)
    val isAirplaneMode = _isAirplaneMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _brightnessInfo = MutableStateFlow<BrightnessInfo?>(null)
    val brightnessInfo = _brightnessInfo.asStateFlow()

    private val _isAutoBrightness = MutableStateFlow(false)
    val isAutoBrightness = _isAutoBrightness.asStateFlow()

    private val _volume = MutableStateFlow(0f)
    val volume = _volume.asStateFlow()

    private val _maxVolume = MutableStateFlow(1f)
    val maxVolume = _maxVolume.asStateFlow()

    private val _qsTiles = MutableStateFlow<List<QSTileData>>(emptyList())
    val qsTiles = _qsTiles.asStateFlow()

    private val _availableTiles = MutableStateFlow<List<AvailableTileData>>(emptyList())
    val availableTiles = _availableTiles.asStateFlow()

    private val observer =
        object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                uri?.lastPathSegment?.let { key ->
                    if (key.startsWith("ax_")) refreshStates()
                    else if (
                        key == Settings.System.SCREEN_BRIGHTNESS ||
                            key == Settings.System.SCREEN_BRIGHTNESS_MODE
                    )
                        refreshBrightness()
                }
            }
        }

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "VOLUME_CHANGED_ACTION" -> {
                        _volume.value =
                            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    }
                    Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                        _isAirplaneMode.value =
                            Settings.Global.getInt(
                                contentResolver,
                                Settings.Global.AIRPLANE_MODE_ON,
                                0,
                            ) == 1
                    }
                }
            }
        }

    init {
        _maxVolume.value = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        _volume.value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()

        val secureUris =
            listOf(
                "ax_wifi_enabled",
                "ax_wifi_info",
                "ax_wifi_scan_list",
                "ax_bluetooth_enabled",
                "ax_bluetooth_info",
                "ax_zen_mode",
                "ax_flashlight_enabled",
                "ax_mobile_data_enabled",
                "ax_mobile_data_info",
                "ax_hotspot_enabled",
                "ax_hotspot_info",
                "ax_rotation_locked",
                "ax_qs_tiles_json",
                "ax_qs_tiles_available_json",
            )
        secureUris.forEach {
            contentResolver.registerContentObserver(Settings.Secure.getUriFor(it), false, observer)
        }
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            observer,
        )
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
            false,
            observer,
        )

        val filter =
            IntentFilter().apply {
                addAction("VOLUME_CHANGED_ACTION")
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

        refreshStates()
        refreshBrightness()
    }

    fun refreshStates() {
        _isWifiEnabled.value = Settings.Secure.getInt(contentResolver, "ax_wifi_enabled", 0) == 1
        _isBtEnabled.value = Settings.Secure.getInt(contentResolver, "ax_bluetooth_enabled", 0) == 1
        _isDndEnabled.value = Settings.Secure.getInt(contentResolver, "ax_zen_mode", 0) != 0
        _isFlashlightOn.value =
            Settings.Secure.getInt(contentResolver, "ax_flashlight_enabled", 0) == 1
        _isMobileDataEnabled.value =
            Settings.Secure.getInt(contentResolver, "ax_mobile_data_enabled", 0) == 1
        _isHotspotEnabled.value =
            Settings.Secure.getInt(contentResolver, "ax_hotspot_enabled", 0) == 1
        _isAutoRotate.value = Settings.Secure.getInt(contentResolver, "ax_rotation_locked", 1) == 0
        _isAirplaneMode.value =
            Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        _isDarkMode.value =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val wifiInfoJson = Settings.Secure.getString(contentResolver, "ax_wifi_info")
        _wifiSsid.value =
            try {
                if (!wifiInfoJson.isNullOrEmpty()) {
                    val json = JSONObject(wifiInfoJson)
                    if (json.optBoolean("connected", false))
                        json.optString("ssid", "").removeSurrounding("\"")
                    else ""
                } else ""
            } catch (e: Exception) {
                ""
            }

        _wifiScanListJson.value =
            Settings.Secure.getString(contentResolver, "ax_wifi_scan_list") ?: ""
        _btDevicesJson.value = Settings.Secure.getString(contentResolver, "ax_bluetooth_info") ?: ""

        val mobileInfoJson = Settings.Secure.getString(contentResolver, "ax_mobile_data_info")
        _mobileDataInfo.value =
            try {
                if (!mobileInfoJson.isNullOrEmpty()) {
                    val json = JSONObject(mobileInfoJson)
                    val type = json.optString("type", "")
                    val desc = json.optString("description", "")
                    if (type.isNotEmpty() && desc.isNotEmpty()) "$type - $desc" else type + desc
                } else ""
            } catch (e: Exception) {
                ""
            }

        val hotspotInfoJson = Settings.Secure.getString(contentResolver, "ax_hotspot_info")
        _hotspotInfo.value =
            try {
                if (!hotspotInfoJson.isNullOrEmpty()) {
                    val json = JSONObject(hotspotInfoJson)
                    if (json.optBoolean("enabled", false)) {
                        val devices = json.optInt("numDevices", 0)
                        if (devices > 0) "$devices connected" else "Active"
                    } else ""
                } else ""
            } catch (e: Exception) {
                ""
            }

        refreshQSTiles()
        refreshAvailableTiles()
    }

    private fun refreshAvailableTiles() {
        val availableJson = Settings.Secure.getString(contentResolver, "ax_qs_tiles_available_json")
        _availableTiles.value =
            try {
                if (!availableJson.isNullOrEmpty()) {
                    val jsonArray = JSONArray(availableJson)
                    (0 until jsonArray.length()).map { i ->
                        val obj = jsonArray.getJSONObject(i)
                        AvailableTileData(
                            spec = obj.optString("spec", ""),
                            label = obj.optString("label", ""),
                            isSystem = obj.optBoolean("isSystem", true),
                            packageName = obj.optString("packageName"),
                        )
                    }
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
    }

    private fun refreshQSTiles() {
        val tilesJson = Settings.Secure.getString(contentResolver, "ax_qs_tiles_json")
        _qsTiles.value =
            try {
                if (!tilesJson.isNullOrEmpty()) {
                    val jsonArray = JSONArray(tilesJson)
                    (0 until jsonArray.length()).map { i ->
                        val obj = jsonArray.getJSONObject(i)
                        QSTileData(
                            spec = obj.optString("spec", ""),
                            label = obj.optString("label", ""),
                            secondaryLabel = obj.optString("secondaryLabel", ""),
                            state = obj.optInt("state", 1),
                            isTransient = obj.optBoolean("isTransient", false),
                        )
                    }
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
    }

    fun clickQSTile(spec: String) {
        try {
            Settings.Secure.putString(contentResolver, "ax_qs_tile_click", spec)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun queryAvailableTiles() {
        try {
            Settings.Secure.putInt(contentResolver, "ax_qs_tiles_available_query", 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateTiles(specs: List<String>) {
        try {
            val jsonArray = JSONArray(specs)
            Settings.Secure.putString(contentResolver, "ax_qs_tiles_update", jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshBrightness() {
        _isAutoBrightness.value =
            Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        _brightnessInfo.value = context.display?.brightnessInfo
    }

    fun toggleWifi() {
        putSecureInt("ax_wifi_enabled", if (_isWifiEnabled.value) 0 else 1)
    }

    fun toggleBluetooth() {
        putSecureInt("ax_bluetooth_enabled", if (_isBtEnabled.value) 0 else 1)
    }

    fun toggleDnd() {
        putSecureInt("ax_zen_mode", if (_isDndEnabled.value) 0 else 1)
    }

    fun toggleFlashlight() {
        putSecureInt("ax_flashlight_enabled", if (_isFlashlightOn.value) 0 else 1)
    }

    fun toggleMobileData() {
        val newState = !_isMobileDataEnabled.value
        _isMobileDataEnabled.value = newState
        putSecureInt("ax_mobile_data_enabled", if (newState) 1 else 0)
    }

    fun toggleHotspot() {
        putSecureInt("ax_hotspot_enabled", if (_isHotspotEnabled.value) 0 else 1)
    }

    fun toggleAutoRotate() {
        putSecureInt("ax_rotation_locked", if (_isAutoRotate.value) 1 else 0)
    }

    fun toggleDarkMode() {
        uiModeManager.setNightMode(
            if (_isDarkMode.value) UiModeManager.MODE_NIGHT_NO else UiModeManager.MODE_NIGHT_YES
        )
    }

    fun toggleAirplaneMode() {
        val newVal = if (_isAirplaneMode.value) 0 else 1
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, newVal)
        context.sendBroadcast(
            Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply { putExtra("state", newVal == 1) }
        )
    }

    fun setBrightness(linear: Float) {
        displayManager.setBrightness(context.display?.displayId ?: 0, linear)
    }

    fun setAutoBrightness(enabled: Boolean) {
        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
    }

    fun setVolume(volume: Float) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume.toInt(), 0)
        _volume.value = volume
    }

    private fun putSecureInt(key: String, value: Int) {
        try {
            Settings.Secure.putInt(contentResolver, key, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onDestroy() {
        contentResolver.unregisterContentObserver(observer)
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
        }
    }
}
