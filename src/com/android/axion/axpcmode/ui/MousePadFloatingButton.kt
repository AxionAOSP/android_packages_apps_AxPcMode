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

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import com.android.axion.axpcmode.R
import com.android.axion.axpcmode.activities.MousePadActivity

class MousePadFloatingButton(private val context: Context) {

    private var floatingView: ImageView? = null
    private var windowManager: WindowManager? = null
    private var added = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var snapAnimator: ValueAnimator? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var sizePx = 0
    private var statusBarHeight = 0
    private var navBarHeight = 0

    fun show() {
        if (added) return
        val dm = context.getSystemService(DisplayManager::class.java)
        val defaultDisplay = dm?.getDisplay(Display.DEFAULT_DISPLAY)
        val displayContext = if (defaultDisplay != null) {
            context.createWindowContext(
                defaultDisplay,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                null)
        } else context
        windowManager = displayContext.getSystemService(WindowManager::class.java)

        val metrics = displayContext.resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        sizePx = (FAB_SIZE_DP * metrics.density).toInt()

        val res = context.resources
        val statusBarId = res.getIdentifier("status_bar_height", "dimen", "android")
        statusBarHeight = if (statusBarId > 0) res.getDimensionPixelSize(statusBarId) else 0
        val navBarId = res.getIdentifier("navigation_bar_height", "dimen", "android")
        navBarHeight = if (navBarId > 0) res.getDimensionPixelSize(navBarId) else 0

        floatingView = ImageView(context).apply {
            setImageResource(R.drawable.ic_cursor)
            setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            alpha = 0.85f
            val pad = sizePx / 5
            setPadding(pad, pad, pad, pad)
        }

        val lp = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = clampY(screenHeight / 3)
            title = "AxMousePadFab"
        }

        floatingView?.setOnTouchListener(::onTouch)

        try {
            windowManager?.addView(floatingView, lp)
            added = true
        } catch (_: Exception) {}
    }

    fun hide() {
        if (!added) return
        snapAnimator?.cancel()
        snapAnimator = null
        try {
            floatingView?.let {
                if (it.isAttachedToWindow) windowManager?.removeViewImmediate(it)
            }
        } catch (_: Exception) {}
        added = false
        floatingView = null
        windowManager = null
    }

    private fun onTouch(v: View, event: MotionEvent): Boolean {
        val lp = v.layoutParams as? WindowManager.LayoutParams ?: return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                snapAnimator?.cancel()
                initialX = lp.x
                initialY = lp.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (!isDragging && (dx * dx + dy * dy) > DRAG_THRESHOLD_SQ) {
                    isDragging = true
                }
                if (isDragging) {
                    lp.x = initialX + dx.toInt()
                    lp.y = clampY(initialY + dy.toInt())
                    try {
                        windowManager?.updateViewLayout(v, lp)
                    } catch (_: Exception) {}
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    launchMousePad()
                } else {
                    snapToEdge(v, lp)
                }
                return true
            }
        }
        return false
    }

    private fun clampY(y: Int): Int {
        val minY = statusBarHeight
        val maxY = screenHeight - navBarHeight - sizePx
        return y.coerceIn(minY, maxY)
    }

    private fun snapToEdge(v: View, lp: WindowManager.LayoutParams) {
        val centerX = lp.x + sizePx / 2
        val targetX = if (centerX < screenWidth / 2) 0 else screenWidth - sizePx
        val startX = lp.x

        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = SNAP_DURATION_MS
            interpolator = OvershootInterpolator(0.8f)
            addUpdateListener { anim ->
                lp.x = anim.animatedValue as Int
                try {
                    if (added) windowManager?.updateViewLayout(v, lp)
                } catch (_: Exception) {}
            }
            start()
        }
    }

    private fun launchMousePad() {
        try {
            val intent = Intent(context, MousePadActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private companion object {
        const val FAB_SIZE_DP = 48
        const val DRAG_THRESHOLD_SQ = 400f
        const val SNAP_DURATION_MS = 250L
    }
}
