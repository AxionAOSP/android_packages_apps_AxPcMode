package com.android.axion.axpcmode.ui.components.qs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray

@Composable
fun DetailView(
    title: String,
    icon: ImageVector,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Box(modifier = Modifier.padding(top = 16.dp)) { content() }
    }
}

@Composable
fun BluetoothDetailContent(
    isEnabled: Boolean,
    devicesJson: String,
    onToggle: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    data class BtDevice(
        val name: String,
        val address: String,
        val isConnected: Boolean,
        val batteryLevel: Int,
    )
    val devices =
        remember(devicesJson) {
            val list = mutableListOf<BtDevice>()
            try {
                if (devicesJson.isNotEmpty()) {
                    val jsonArray = JSONArray(devicesJson)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            BtDevice(
                                name = obj.optString("name", "Unknown Device"),
                                address = obj.optString("address", ""),
                                isConnected = obj.optBoolean("isConnected", false),
                                batteryLevel = obj.optInt("batteryLevel", -1),
                            )
                        )
                    }
                }
            } catch (e: Exception) {}
            list
        }

    val connectedDevices = devices.filter { it.isConnected }
    val otherDevices = devices.filter { !it.isConnected }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("On", modifier = Modifier.weight(1f))
            Switch(checked = isEnabled, onCheckedChange = { onToggle() })
        }

        if (isEnabled) {
            if (connectedDevices.isNotEmpty()) {
                Text(
                    "Connected",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                connectedDevices.forEach { device ->
                    ListItem(
                        headlineContent = { Text(device.name) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Headphones,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        supportingContent =
                            if (device.batteryLevel > -1) {
                                { Text("${device.batteryLevel}% battery") }
                            } else null,
                        trailingContent = {
                            Button(
                                onClick = { onDeviceClick(device.address) },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text("Disconnect")
                            }
                        },
                        colors =
                            ListItemDefaults.colors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            if (otherDevices.isNotEmpty()) {
                Text(
                    "Saved Devices",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                otherDevices.forEach { device ->
                    ListItem(
                        headlineContent = { Text(device.name) },
                        leadingContent = { Icon(Icons.Rounded.Bluetooth, null) },
                        modifier =
                            Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                onDeviceClick(device.address)
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            } else {
                if (connectedDevices.isEmpty()) {
                    Text(
                        "No devices found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onSettingsClick,
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            Text("Bluetooth Settings")
        }
    }
}

@Composable
fun InternetDetailContent(
    isWifiEnabled: Boolean,
    wifiSsid: String,
    wifiScanListJson: String,
    onWifiToggle: () -> Unit,
    onWifiConnect: (String) -> Unit,
    onWifiSettingsClick: () -> Unit,
    isMobileEnabled: Boolean,
    mobileLabel: String,
    onMobileToggle: () -> Unit,
    onMobileSettingsClick: () -> Unit,
    isHotspotEnabled: Boolean,
    hotspotInfo: String,
    onHotspotToggle: () -> Unit,
    onHotspotSettingsClick: () -> Unit,
) {
    data class WifiAp(
        val title: String,
        val key: String,
        val isConnected: Boolean,
        val level: Int,
        val isSaved: Boolean,
        val security: Int,
    )
    val wifiAps =
        remember(wifiScanListJson) {
            val list = mutableListOf<WifiAp>()
            try {
                if (wifiScanListJson.isNotEmpty()) {
                    val jsonArray = JSONArray(wifiScanListJson)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            WifiAp(
                                title = obj.optString("title", "Unknown"),
                                key = obj.optString("key", ""),
                                isConnected = obj.optBoolean("isConnected", false),
                                level = obj.optInt("level", 0),
                                isSaved = obj.optBoolean("isSaved", false),
                                security = obj.optInt("security", 0),
                            )
                        )
                    }
                }
            } catch (e: Exception) {}
            list
        }

    val connectedWifi = wifiAps.find { it.isConnected }
    val availableWifi = wifiAps.filter { !it.isConnected }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.SignalCellularAlt,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                val label =
                    if (isMobileEnabled && mobileLabel.isNotEmpty()) mobileLabel else "Mobile Data"
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = isMobileEnabled, onCheckedChange = { onMobileToggle() })
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.WifiTethering, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                val label =
                    if (isHotspotEnabled && hotspotInfo.isNotEmpty()) hotspotInfo else "Hotspot"
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = isHotspotEnabled, onCheckedChange = { onHotspotToggle() })
            }

            Button(
                onClick = onHotspotSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            ) {
                Text("Hotspot Settings")
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.NetworkWifi, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Wi-Fi",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = isWifiEnabled, onCheckedChange = { onWifiToggle() })
            }

            if (isWifiEnabled) {
                if (connectedWifi != null) {
                    ListItem(
                        headlineContent = { Text(connectedWifi.title) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        colors =
                            ListItemDefaults.colors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    )
                } else if (wifiSsid.isNotEmpty()) {
                    ListItem(
                        headlineContent = { Text(wifiSsid) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        colors =
                            ListItemDefaults.colors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    )
                }

                if (availableWifi.isNotEmpty()) {
                    Text(
                        "Available Networks",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    availableWifi.forEach { ap ->
                        ListItem(
                            headlineContent = { Text(ap.title) },
                            trailingContent = {
                                if (ap.security != 0)
                                    Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(16.dp))
                            },
                            modifier =
                                Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                    onWifiConnect(if (ap.key.isNotEmpty()) ap.key else ap.title)
                                },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }

                Button(
                    onClick = onWifiSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                ) {
                    Text("All Networks")
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Button(
            onClick = onMobileSettingsClick,
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            Text("Mobile Settings")
        }
    }
}
