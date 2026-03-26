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

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.android.axion.platform.AxPlatformClient
import com.android.axion.platform.IAxPlatformCallback
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class AxPlatformRepository(context: Context) {

    private val client = AxPlatformClient.getInstance().also { it.init(context) }
    private val handler = Handler(Looper.getMainLooper())
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val stateFlows = mutableMapOf<String, MutableSharedFlow<Bundle>>()
    private var registered = false

    private val callback = object : IAxPlatformCallback.Stub() {
        override fun onStateChanged(key: String, state: Bundle) {
            handler.post { getOrCreateFlow(key).tryEmit(state) }
        }
    }

    fun connect() {
        if (registered) return
        client.registerCallback(callback)
        registered = true
    }

    fun disconnect() {
        if (!registered) return
        client.unregisterCallback(callback)
        registered = false
    }

    fun stateFlow(key: String): Flow<Bundle> {
        connect()
        return getOrCreateFlow(key).onStart {
            val cached = client.getState(key)
            if (!cached.isEmpty) emit(cached)
        }
    }

    fun featureActive(feature: String): Flow<Boolean> =
        stateFlow(feature).map { it.getBoolean("active", false) }.distinctUntilChanged()

    fun toggle(feature: String) = client.toggle(feature)

    fun setEnabled(feature: String, enabled: Boolean) = client.setEnabled(feature, enabled)

    fun setValue(feature: String, value: Int) = client.setValue(feature, value)

    fun performAction(feature: String, param: String) = client.performAction(feature, param)

    fun getSupportedFeatures(): Array<String> = client.supportedFeatures

    fun getState(feature: String): Bundle = client.getState(feature)

    fun connectWifi(networkKey: String) = client.connectWifi(networkKey)

    fun connectBluetooth(address: String) = client.connectBluetoothDevice(address)

    fun getSavedTileSpecs(): List<String> {
        val saved = prefs.getString(KEY_TILE_SPECS, null) ?: return DEFAULT_TILES
        return saved.split(",").filter { it.isNotEmpty() }
    }

    fun saveTileSpecs(specs: List<String>) {
        prefs.edit().putString(KEY_TILE_SPECS, specs.joinToString(",")).apply()
    }

    private fun getOrCreateFlow(key: String): MutableSharedFlow<Bundle> =
        stateFlows.getOrPut(key) {
            MutableSharedFlow(
                replay = 1,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }

    companion object {
        private const val PREFS_NAME = "ax_platform_tiles"
        private const val KEY_TILE_SPECS = "tile_specs"

        val DEFAULT_TILES = listOf(
            AxPlatformClient.FEATURE_WIFI,
            AxPlatformClient.FEATURE_BLUETOOTH,
            AxPlatformClient.FEATURE_MOBILE_DATA,
            AxPlatformClient.FEATURE_FLASHLIGHT,
            AxPlatformClient.FEATURE_ZEN,
            AxPlatformClient.FEATURE_ROTATION,
            AxPlatformClient.FEATURE_DARK_MODE,
            AxPlatformClient.FEATURE_AIRPLANE_MODE,
        )
    }
}
