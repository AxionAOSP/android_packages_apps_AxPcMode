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

package com.android.axion.axpcmode.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class ArrowDirection {
    UP,
    DOWN,
}

class PopoverShape(
    private val direction: ArrowDirection = ArrowDirection.UP,
    private val arrowOffset: Dp? = null,
    private val arrowWidth: Dp = 16.dp,
    private val arrowHeight: Dp = 8.dp,
    private val cornerRadius: Dp = 16.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path =
            Path().apply {
                val aWidth = with(density) { arrowWidth.toPx() }
                val aHeight = with(density) { arrowHeight.toPx() }
                val radius = with(density) { cornerRadius.toPx() }

                val rectTop = if (direction == ArrowDirection.UP) aHeight else 0f
                val rectBottom =
                    if (direction == ArrowDirection.UP) size.height else size.height - aHeight

                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, rectTop, size.width, rectBottom),
                        cornerRadius = CornerRadius(radius),
                    )
                )

                val centerX =
                    if (arrowOffset != null) {
                        with(density) { arrowOffset.toPx() }
                    } else {
                        size.width / 2f
                    }

                val clampedCenterX =
                    centerX.coerceIn(radius + aWidth / 2f, size.width - radius - aWidth / 2f)

                if (direction == ArrowDirection.UP) {
                    moveTo(clampedCenterX - aWidth / 2f, aHeight)
                    lineTo(clampedCenterX, 0f)
                    lineTo(clampedCenterX + aWidth / 2f, aHeight)
                } else {
                    moveTo(clampedCenterX - aWidth / 2f, size.height - aHeight)
                    lineTo(clampedCenterX, size.height)
                    lineTo(clampedCenterX + aWidth / 2f, size.height - aHeight)
                }
                close()
            }
        return Outline.Generic(path)
    }
}
