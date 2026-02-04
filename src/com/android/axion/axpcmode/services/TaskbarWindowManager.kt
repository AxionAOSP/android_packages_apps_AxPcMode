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
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView

class TaskbarWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var taskbarView: View? = null
    private var hintView: View? = null

    private fun createLayoutParams(isHint: Boolean) =
        WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width =
                if (isHint) WindowManager.LayoutParams.WRAP_CONTENT
                else WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            softInputMode =
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            title = if (isHint) "AxGestureHint" else "AxTaskbar"
            windowAnimations = 0
        }

    fun addTaskbarView(view: ComposeView) {
        if (taskbarView != null) return
        try {
            windowManager.addView(view, createLayoutParams(false))
            taskbarView = view
        } catch (e: Exception) {
            Log.e("TaskbarWindowManager", "Failed to add taskbar view: ${e.message}")
        }
    }

    fun addHintView(view: ComposeView) {
        if (hintView != null) return
        try {
            windowManager.addView(view, createLayoutParams(true))
            hintView = view
        } catch (e: Exception) {
            Log.e("TaskbarWindowManager", "Failed to add hint view: ${e.message}")
        }
    }

    fun removeTaskbarView() {
        taskbarView?.let { view ->
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeViewImmediate(view)
                }
            } catch (e: Exception) {
                Log.e("TaskbarWindowManager", "Failed to remove taskbar view: ${e.message}")
            }
        }
        taskbarView = null
    }

    fun removeHintView() {
        hintView?.let { view ->
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeViewImmediate(view)
                }
            } catch (e: Exception) {
                Log.e("TaskbarWindowManager", "Failed to remove hint view: ${e.message}")
            }
        }
        hintView = null
    }

    fun getTaskbarView(): View? = taskbarView

    fun getHintView(): View? = hintView
}
