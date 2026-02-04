package com.android.axion.axpcmode.services

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class AxPlatformSettingsRepository(private val context: Context) {

    fun observeSecureSetting(key: String): Flow<String?> = callbackFlow {
        val uri = Settings.Secure.getUriFor(key)
        val contentResolver = context.contentResolver

        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    try {
                        val value = Settings.Secure.getString(contentResolver, key)
                        trySend(value)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading secure setting: $key", e)
                    }
                }
            }

        try {
            contentResolver.registerContentObserver(uri, false, observer)

            val initialValue = Settings.Secure.getString(contentResolver, key)
            trySend(initialValue)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering observer for: $key", e)
        }

        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }

    val mobileDataInfo: Flow<JSONObject?> =
        observeSecureSetting("ax_mobile_data_info").map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) return@map null
            try {
                JSONObject(jsonStr)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing ax_mobile_data_info", e)
                null
            }
        }

    val mobileDataEnabled: Flow<Boolean> =
        observeSecureSetting("ax_mobile_data_enabled").map { it == "1" }

    val batteryInfo: Flow<JSONObject?> =
        observeSecureSetting("ax_battery_info").map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) return@map null
            try {
                JSONObject(jsonStr)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing ax_battery_info", e)
                null
            }
        }

    val bluetoothInfo: Flow<JSONArray?> =
        observeSecureSetting("ax_bluetooth_info").map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) return@map null
            try {
                JSONArray(jsonStr)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing ax_bluetooth_info", e)
                null
            }
        }

    companion object {
        private const val TAG = "AxPlatformSettingsRepo"
    }
}
