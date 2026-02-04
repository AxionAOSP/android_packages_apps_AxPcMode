package com.android.axion.axpcmode.ui.components.qs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BrightnessAndVolumeSliders(
    sliderPosition: Float,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    isAutoBrightness: Boolean,
    onAutoBrightnessToggle: () -> Unit,
    currentVolume: Float,
    maxVolume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.WbSunny,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Slider(
                value = sliderPosition,
                onValueChange = onSliderChange,
                onValueChangeFinished = onSliderChangeFinished,
                modifier = Modifier.weight(1f),
                colors =
                    SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onAutoBrightnessToggle) {
                Icon(
                    imageVector =
                        if (isAutoBrightness) Icons.Rounded.BrightnessAuto
                        else Icons.Rounded.Brightness7,
                    contentDescription = "Auto Brightness",
                    tint =
                        if (isAutoBrightness) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Slider(
                value = currentVolume / maxVolume,
                onValueChange = { onVolumeChange(it * maxVolume) },
                modifier = Modifier.weight(1f),
                colors =
                    SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(bottom = 12.dp, top = 4.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    }
}
