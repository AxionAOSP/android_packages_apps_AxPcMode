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

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.android.axion.compose.lifecycle.repeatWhenAttached
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.graphics.Color
import android.view.View

class PanelOverlayManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val views = mutableMapOf<String, ComposeView>()
    private val visibilityStates = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val gravities = mutableMapOf<String, Int>()
    private val mainScope = MainScope()

    private val hideJobs = mutableMapOf<String, Job>()

    private val autoHideTimers = mutableMapOf<String, Job>()
    private val autoHideConfigs = mutableMapOf<String, AutoHideConfig>()
    private var edgeDetectorView: View? = null

    data class AutoHideConfig(
        val delayMs: Long = 3000L,
        val edgeHeight: Int = 20,
        val onShow: () -> Unit = {},
        val onHide: () -> Unit = {},
    )

    fun show(
        id: String,
        content: @Composable () -> Unit,
        gravity: Int = Gravity.FILL,
        width: Int = WindowManager.LayoutParams.MATCH_PARENT,
        height: Int = WindowManager.LayoutParams.MATCH_PARENT,
        x: Int = 0,
        y: Int = 0,
        focusable: Boolean = false,
        onOutsideClick: (() -> Unit)? = null,
        noAnimation: Boolean = false,
    ) {

        hideJobs[id]?.cancel()
        hideJobs.remove(id)

        if (views.containsKey(id)) {
            val view = views[id]!!

            view.tag = onOutsideClick
            gravities[id] = gravity

            val lp = view.layoutParams as WindowManager.LayoutParams
            if (
                lp.width != width ||
                    lp.height != height ||
                    lp.gravity != gravity ||
                    lp.x != x ||
                    lp.y != y
            ) {
                lp.width = width
                lp.height = height
                lp.gravity = gravity
                lp.x = x
                lp.y = y

                if (focusable) {
                    lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                    lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    lp.softInputMode =
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                } else {
                    lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
                    lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                }

                try {
                    windowManager.updateViewLayout(view, lp)
                } catch (e: Exception) {
                    Log.e("PanelOverlayManager", "Failed to update view: ${e.message}")
                }
            }

            visibilityStates[id]?.value = true
            return
        }

        val lp =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                this.width = width
                this.height = height
                format = PixelFormat.TRANSLUCENT
                this.gravity = gravity
                this.x = x
                this.y = y
                flags =
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

                softInputMode =
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN

                if (!focusable) {
                    flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                }
                title = "AxPanel_$id"
                windowAnimations = 0
            }

        val visibility = MutableStateFlow(false)
        visibilityStates[id] = visibility
        gravities[id] = gravity

        val composeView = createComposeView(content, visibility, gravity, noAnimation)
        composeView.tag = onOutsideClick

        try {
            views[id] = composeView
            windowManager.addView(composeView, lp)

            mainScope.launch {
                kotlinx.coroutines.delay(16)
                visibility.value = true
            }
        } catch (e: Exception) {
            Log.e("PanelOverlayManager", "Failed to add view: ${e.message}")
        }
    }

    fun hide(id: String) {
        visibilityStates[id]?.let { visibility ->
            hideJobs[id]?.cancel()

            hideJobs[id] =
                mainScope.launch {
                    visibility.value = false

                    kotlinx.coroutines.delay(300)
                    removeViewImmediate(id)
                    hideJobs.remove(id)
                }
        } ?: removeViewImmediate(id)
    }

    private fun removeViewImmediate(id: String) {
        visibilityStates.remove(id)
        gravities.remove(id)
        views.remove(id)?.let { view ->
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeViewImmediate(view)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createComposeView(
        content: @Composable () -> Unit,
        visibilityState: MutableStateFlow<Boolean>,
        gravity: Int,
        noAnimation: Boolean = false,
    ): ComposeView {
        return ComposeView(context).apply {
            repeatWhenAttached {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent {
                        val isVisible by visibilityState.collectAsState()

                        if (noAnimation) {

                            if (isVisible) {
                                content()
                            }
                        } else {

                            val (enter, exit) =
                                Pair(
                                    slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                )

                            AnimatedVisibility(
                                visible = isVisible,
                                enter = enter,
                                exit = exit,
                                label = "PanelAnimation",
                            ) {
                                content()
                            }
                        }
                    }
                }
            }

            setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    (v.tag as? (() -> Unit))?.invoke()
                }
                false
            }
        }
    }

    fun showWithAutoHide(
        id: String,
        content: @Composable () -> Unit,
        gravity: Int = Gravity.BOTTOM or Gravity.FILL_HORIZONTAL,
        width: Int = WindowManager.LayoutParams.MATCH_PARENT,
        height: Int = WindowManager.LayoutParams.WRAP_CONTENT,
        autoHideDelayMs: Long = 3000L,
        edgeHeightDp: Int = 24,
    ) {
        val config = AutoHideConfig(delayMs = autoHideDelayMs, edgeHeight = edgeHeightDp)
        autoHideConfigs[id] = config

        val wrappedContent: @Composable () -> Unit = {
            Box(
                modifier =
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (
                                    event.type == PointerEventType.Press ||
                                        event.type == PointerEventType.Move
                                ) {
                                    resetAutoHideTimer(id)
                                }
                            }
                        }
                    }
            ) {
                content()
            }
        }

        show(id = id, content = wrappedContent, gravity = gravity, width = width, height = height)

        setupEdgeDetector(id, edgeHeightDp)

        resetAutoHideTimer(id)
    }

    fun revealAutoHide(id: String) {
        val config = autoHideConfigs[id] ?: return
        visibilityStates[id]?.value = true
        resetAutoHideTimer(id)
    }

    fun resetAutoHideTimer(id: String) {
        autoHideTimers[id]?.cancel()
        val config = autoHideConfigs[id] ?: return

        autoHideTimers[id] =
            mainScope.launch {
                kotlinx.coroutines.delay(config.delayMs)

                visibilityStates[id]?.value = false
            }
    }

    fun cancelAutoHide(id: String) {
        autoHideTimers[id]?.cancel()
        autoHideTimers.remove(id)
    }

    private fun setupEdgeDetector(overlayId: String, edgeHeightDp: Int) {
        if (edgeDetectorView != null) return

        val density = context.resources.displayMetrics.density
        val edgeHeightPx = (edgeHeightDp * density).toInt()

        val edgeView =
            View(context).apply {
                setBackgroundColor(Color.TRANSPARENT)

                var startY = 0f
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startY = event.rawY
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaY = startY - event.rawY

                            if (deltaY > edgeHeightPx / 2) {
                                revealAutoHide(overlayId)
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {

                            val deltaY = startY - event.rawY
                            if (kotlin.math.abs(deltaY) < 10) {
                                revealAutoHide(overlayId)
                            }
                            true
                        }
                        else -> false
                    }
                }
            }

        val lp =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                this.width = WindowManager.LayoutParams.MATCH_PARENT
                this.height = edgeHeightPx
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.BOTTOM or Gravity.FILL_HORIZONTAL
                flags =
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                title = "EdgeDetector_$overlayId"
            }

        try {
            windowManager.addView(edgeView, lp)
            edgeDetectorView = edgeView
        } catch (e: Exception) {
            Log.e("PanelOverlayManager", "Failed to add edge detector: ${e.message}")
        }
    }

    fun removeEdgeDetector() {
        edgeDetectorView?.let { view ->
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeViewImmediate(view)
                }
            } catch (e: Exception) {
                Log.e("PanelOverlayManager", "Failed to remove edge detector: ${e.message}")
            }
        }
        edgeDetectorView = null
    }

    fun cleanup() {
        autoHideTimers.values.forEach { it.cancel() }
        autoHideTimers.clear()
        autoHideConfigs.clear()
        removeEdgeDetector()
        views.keys.toList().forEach { hide(it) }
    }
}
