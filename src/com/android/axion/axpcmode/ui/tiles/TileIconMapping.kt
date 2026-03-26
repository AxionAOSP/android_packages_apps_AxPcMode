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

package com.android.axion.axpcmode.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

object TileIconMapping {

    private val iconMap =
        mapOf(
            "wifi" to Icons.Rounded.Wifi,
            "internet" to Icons.Rounded.Public,
            "bt" to Icons.Rounded.Bluetooth,
            "bluetooth" to Icons.Rounded.Bluetooth,
            "cell" to Icons.Rounded.SignalCellular4Bar,
            "mobiledata" to Icons.Rounded.SignalCellular4Bar,
            "mobile_data" to Icons.Rounded.SignalCellular4Bar,
            "hotspot" to Icons.Rounded.WifiTethering,
            "airplane" to Icons.Rounded.AirplanemodeActive,
            "airplane_mode" to Icons.Rounded.AirplanemodeActive,
            "nfc" to Icons.Rounded.Nfc,
            "flashlight" to Icons.Rounded.FlashlightOn,
            "dnd" to Icons.Rounded.DoNotDisturb,
            "zen" to Icons.Rounded.DoNotDisturb,
            "rotation" to Icons.Rounded.ScreenRotation,
            "battery" to Icons.Rounded.BatterySaver,
            "saver" to Icons.Rounded.BatterySaver,
            "battery_saver" to Icons.Rounded.BatterySaver,
            "dark" to Icons.Rounded.DarkMode,
            "dark_mode" to Icons.Rounded.DarkMode,
            "night" to Icons.Rounded.NightsStay,
            "night_light" to Icons.Rounded.NightsStay,
            "inversion" to Icons.Rounded.InvertColors,
            "color_inversion" to Icons.Rounded.InvertColors,
            "color_correction" to Icons.Rounded.Palette,
            "location" to Icons.Rounded.LocationOn,
            "screenrecord" to Icons.Rounded.Videocam,
            "cast" to Icons.Rounded.Cast,
            "cameratoggle" to Icons.Rounded.CameraAlt,
            "camera_privacy" to Icons.Rounded.CameraAlt,
            "mictoggle" to Icons.Rounded.Mic,
            "mic_privacy" to Icons.Rounded.Mic,
            "reduce_brightness" to Icons.Rounded.Brightness4,
            "onehanded" to Icons.Rounded.PhoneAndroid,
            "one_handed_mode" to Icons.Rounded.PhoneAndroid,
            "dream" to Icons.Rounded.Bedtime,
            "font_scaling" to Icons.Rounded.TextFields,
            "qr_code_scanner" to Icons.Rounded.QrCodeScanner,
            "work" to Icons.Rounded.Work,
            "work_profile" to Icons.Rounded.Work,
            "alarm" to Icons.Rounded.Alarm,
            "wallet" to Icons.Rounded.Wallet,
            "controls" to Icons.Rounded.Tune,
            "aod" to Icons.Rounded.Brightness2,
            "data_saver" to Icons.Rounded.DataSaverOn,
            "heads_up" to Icons.Rounded.NotificationsActive,
            "auto_sync" to Icons.Rounded.Sync,
            "usb_tether" to Icons.Rounded.Usb,
            "reading_mode" to Icons.Rounded.MenuBook,
            "power_share" to Icons.Rounded.BatteryChargingFull,
            "caffeine" to Icons.Rounded.LocalCafe,
            "vpn" to Icons.Rounded.VpnKey,
            "profiles" to Icons.Rounded.ManageAccounts,
            "smart_pixels" to Icons.Rounded.GridOn,
            "screenshot" to Icons.Rounded.Crop,
        )

    fun getIcon(spec: String): ImageVector {
        val s = spec.lowercase()
        if (s.startsWith("custom")) return Icons.Rounded.Extension
        return iconMap[s] ?: Icons.Rounded.Settings
    }
}
