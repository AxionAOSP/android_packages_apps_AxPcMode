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

package com.android.axion.axpcmode.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.axion.axpcmode.R
import com.android.axion.axpcmode.services.DesktopWallpaperRepository
import com.android.axion.axpcmode.ui.theme.AxPcModeTheme
import com.android.axion.compose.preferences.ClickablePreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.scaffold.AxionScaffold

@Composable
fun WallpaperSettingsScreen(
    viewModel: PcModeLauncherViewModel,
    onNavigateBack: () -> Unit,
) {
    AxPcModeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            WallpaperSettingsContent(
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WallpaperSettingsContent(
    viewModel: PcModeLauncherViewModel,
    onNavigateBack: () -> Unit,
) {
    BackHandler(onBack = onNavigateBack)

    val wallpaperBitmap by viewModel.wallpaperBitmap.collectAsState()
    val scaleMode by viewModel.wallpaperScaleMode.collectAsState()
    val hasCustomWallpaper by viewModel.hasCustomWallpaper.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.selectWallpaper(uri)
        }
    }

    val selectWallpaper = stringResource(R.string.select_wallpaper)
    val removeCustomWallpaper = stringResource(R.string.remove_custom_wallpaper)

    AxionScaffold(
        title = stringResource(R.string.wallpaper_settings_title),
        onBackClick = onNavigateBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            WallpaperPreviewCard(
                bitmap = wallpaperBitmap,
                scaleMode = scaleMode,
            )

            PreferenceGroup(title = stringResource(R.string.wallpaper_actions_title)) {
                item {
                    ClickablePreference(
                        title = selectWallpaper,
                        summary = stringResource(R.string.wallpaper_preview_summary),
                        icon = Icons.Filled.Image,
                        onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                    )
                }
                if (hasCustomWallpaper) {
                    item {
                        ClickablePreference(
                            title = removeCustomWallpaper,
                            icon = Icons.Filled.Delete,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { viewModel.removeCustomWallpaper() },
                        )
                    }
                }
            }

            PreferenceGroup(title = stringResource(R.string.wallpaper_scaling_title)) {
                item {
                    WallpaperScalePreference(
                        selectedMode = scaleMode,
                        onModeSelected = viewModel::setWallpaperScaleMode,
                    )
                }
            }

            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
private fun WallpaperPreviewCard(
    bitmap: Bitmap?,
    scaleMode: DesktopWallpaperRepository.ScaleMode,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.wallpaper_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.wallpaper_preview_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.wallpaper_preview_desc),
                        contentScale = scaleMode.toContentScale(),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WallpaperScalePreference(
    selectedMode: DesktopWallpaperRepository.ScaleMode,
    onModeSelected: (DesktopWallpaperRepository.ScaleMode) -> Unit,
) {
    val options = listOf(
        ScaleOption(
            mode = DesktopWallpaperRepository.ScaleMode.FILL,
            label = stringResource(R.string.wallpaper_scale_fill),
            summary = stringResource(R.string.wallpaper_scale_fill_summary),
        ),
        ScaleOption(
            mode = DesktopWallpaperRepository.ScaleMode.FIT,
            label = stringResource(R.string.wallpaper_scale_fit),
            summary = stringResource(R.string.wallpaper_scale_fit_summary),
        ),
        ScaleOption(
            mode = DesktopWallpaperRepository.ScaleMode.STRETCH,
            label = stringResource(R.string.wallpaper_scale_stretch),
            summary = stringResource(R.string.wallpaper_scale_stretch_summary),
        ),
        ScaleOption(
            mode = DesktopWallpaperRepository.ScaleMode.CENTER,
            label = stringResource(R.string.wallpaper_scale_center),
            summary = stringResource(R.string.wallpaper_scale_center_summary),
        ),
    )
    val selectedSummary = options.first { it.mode == selectedMode }.summary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.AspectRatio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.wallpaper_scaling_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = selectedSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ScaleOptionRow(
                    options = options.take(2),
                    selectedMode = selectedMode,
                    onModeSelected = onModeSelected,
                )
                ScaleOptionRow(
                    options = options.drop(2),
                    selectedMode = selectedMode,
                    onModeSelected = onModeSelected,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScaleOptionRow(
    options: List<ScaleOption>,
    selectedMode: DesktopWallpaperRepository.ScaleMode,
    onModeSelected: (DesktopWallpaperRepository.ScaleMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = selectedMode == option.mode,
                onCheckedChange = { onModeSelected(option.mode) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shapes = if (index == 0) {
                    ButtonGroupDefaults.connectedLeadingButtonShapes()
                } else {
                    ButtonGroupDefaults.connectedTrailingButtonShapes()
                },
            ) {
                Text(
                    text = option.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class ScaleOption(
    val mode: DesktopWallpaperRepository.ScaleMode,
    val label: String,
    val summary: String,
)

internal fun DesktopWallpaperRepository.ScaleMode.toContentScale(): ContentScale = when (this) {
    DesktopWallpaperRepository.ScaleMode.FILL -> ContentScale.Crop
    DesktopWallpaperRepository.ScaleMode.FIT -> ContentScale.Fit
    DesktopWallpaperRepository.ScaleMode.STRETCH -> ContentScale.FillBounds
    DesktopWallpaperRepository.ScaleMode.CENTER -> ContentScale.None
}
