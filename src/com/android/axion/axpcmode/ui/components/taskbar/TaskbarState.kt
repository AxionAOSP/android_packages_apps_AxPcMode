package com.android.axion.axpcmode.ui.components.taskbar

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.android.axion.axpcmode.services.AxPlatformSettingsRepository
import java.text.SimpleDateFormat
import java.util.*
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
    val context = LocalContext.current.applicationContext

    val wifiManager = remember { context.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val bluetoothManager = remember {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    var wifiLevel by remember { mutableIntStateOf(0) }
    var isWifiConnected by remember { mutableStateOf(false) }
    var isWifiEnabled by remember { mutableStateOf(wifiManager.isWifiEnabled) }
    var isBtEnabled by remember { mutableStateOf(bluetoothManager?.adapter?.isEnabled ?: false) }

    var batteryLevel by remember { mutableIntStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var mobileDataLevel by remember { mutableIntStateOf(4) }
    var isMobileDataEnabled by remember { mutableStateOf(false) }
    var isSimPresent by remember { mutableStateOf(false) }
    var mobileDataType by remember { mutableStateOf("") }
    var bluetoothBatteryLevel by remember { mutableIntStateOf(-1) }

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
            currentDate = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now.time)
            delay(60000)
        }
    }

    val repository = remember { AxPlatformSettingsRepository(context) }
    val mobileDataJson by repository.mobileDataInfo.collectAsState(initial = null)
    val mobileDataEnabledSetting by repository.mobileDataEnabled.collectAsState(initial = false)
    val batteryJson by repository.batteryInfo.collectAsState(initial = null)
    val bluetoothJson by repository.bluetoothInfo.collectAsState(initial = null)

    LaunchedEffect(mobileDataJson) {
        if (mobileDataJson != null) {
            mobileDataJson?.let {
                mobileDataLevel = it.optInt("level", 4)
                val type = it.optString("type")
                mobileDataType = type

                isSimPresent = it.has("level")
            }
        } else {

            isSimPresent = false
            mobileDataLevel = 0
            mobileDataType = ""
        }
    }

    LaunchedEffect(mobileDataEnabledSetting) { isMobileDataEnabled = mobileDataEnabledSetting }

    LaunchedEffect(batteryJson) {
        batteryJson?.let {
            batteryLevel = it.optInt("level", 0)
            val charging = it.optBoolean("charging", false)
            val plugged = it.optBoolean("pluggedIn", false)
            isCharging = charging || plugged
        }
    }

    LaunchedEffect(bluetoothJson) {
        bluetoothJson?.let { devices ->
            var bestLevel = -1
            for (i in 0 until devices.length()) {
                val dev = devices.optJSONObject(i)
                if (dev?.optBoolean("isConnected", false) == true) {
                    val level = dev.optInt("batteryLevel", -1)
                    if (level > -1) {
                        bestLevel = level
                        break
                    }
                }
            }
            bluetoothBatteryLevel = bestLevel
        }
    }

    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        WifiManager.RSSI_CHANGED_ACTION,
                        WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                            @Suppress("DEPRECATION") val info = wifiManager.connectionInfo
                            isWifiConnected = info.networkId != -1
                            wifiLevel = wifiManager.calculateSignalLevel(info.rssi)
                            isWifiEnabled = wifiManager.isWifiEnabled
                        }
                        BluetoothAdapter.ACTION_STATE_CHANGED -> {
                            isBtEnabled = bluetoothManager?.adapter?.isEnabled ?: false
                        }
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(WifiManager.RSSI_CHANGED_ACTION)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
        context.registerReceiver(receiver, filter)

        val info = wifiManager.connectionInfo
        isWifiConnected = info.networkId != -1
        wifiLevel = wifiManager.calculateSignalLevel(info.rssi)
        isWifiEnabled = wifiManager.isWifiEnabled

        onDispose { context.unregisterReceiver(receiver) }
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
