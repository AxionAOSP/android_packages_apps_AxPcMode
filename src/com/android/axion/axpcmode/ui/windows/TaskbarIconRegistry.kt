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

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

class TaskbarIconRegistry {
    private val iconPositions = mutableStateMapOf<String, Offset>()

    fun updatePosition(packageName: String, position: Offset) {
        iconPositions[packageName] = position
    }

    fun getPosition(packageName: String): Offset? {
        return iconPositions[packageName]
    }
}

val LocalTaskbarIconRegistry = staticCompositionLocalOf { TaskbarIconRegistry() }
