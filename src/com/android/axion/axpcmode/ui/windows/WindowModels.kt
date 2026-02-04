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

package com.android.axion.axpcmode.ui.windows

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

enum class WindowMode {
    NORMAL,
    MINIMIZED,
    MAXIMIZED,
}

@Immutable
data class WindowState(
    val id: String,
    val packageName: String,
    val activityName: String? = null,
    val title: String = "",
    val icon: Bitmap? = null,
    val x: Float = 100f,
    val y: Float = 100f,
    val width: Int = 800,
    val height: Int = 600,
    val mode: WindowMode = WindowMode.NORMAL,
    val isFocused: Boolean = false,
    val displayId: Int = -1,
    val taskId: Int = -1,
    val zIndex: Float = 0f,
    val snapshot: Bitmap? = null,
)
