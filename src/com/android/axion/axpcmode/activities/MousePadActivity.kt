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

package com.android.axion.axpcmode.activities

import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.axion.axpcmode.R
import com.android.axion.axpcmode.ui.theme.AxPcModeTheme

class MousePadActivity : ComponentActivity() {

    private var inputManager: InputManager? = null
    private var displayManager: DisplayManager? = null
    private var targetDisplayId: Int = Display.INVALID_DISPLAY
    private var targetDisplayWidth: Int = 1920
    private var targetDisplayHeight: Int = 1080

    private var cursorX: Float = 0f
    private var cursorY: Float = 0f
    private var sensitivity: Float = 1.5f

    private var cursorView: ImageView? = null
    private var cursorWindowManager: WindowManager? = null
    private var cursorAdded = false
    private var cursorEnabled = true

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            if (targetDisplayId == Display.INVALID_DISPLAY) {
                findSecondaryDisplay()
                if (cursorEnabled) setupCursorOverlay()
            }
        }
        override fun onDisplayRemoved(displayId: Int) {
            if (displayId == targetDisplayId) {
                removeCursorOverlay()
                targetDisplayId = Display.INVALID_DISPLAY
                finish()
            }
        }
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == targetDisplayId) {
                val display = displayManager?.getDisplay(displayId) ?: return
                updateDisplayBounds(display)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        inputManager = getSystemService(InputManager::class.java)
        displayManager = getSystemService(DisplayManager::class.java)
        findSecondaryDisplay()
        displayManager?.registerDisplayListener(displayListener, null)

        cursorX = targetDisplayWidth / 2f
        cursorY = targetDisplayHeight / 2f

        setupCursorOverlay()

        setContent {
            AxPcModeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MousePadContent()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActive = true
    }

    override fun onPause() {
        super.onPause()
        isActive = false
    }

    override fun onDestroy() {
        removeCursorOverlay()
        displayManager?.unregisterDisplayListener(displayListener)
        super.onDestroy()
    }

    private fun findSecondaryDisplay() {
        val displays = displayManager?.displays ?: return
        for (display in displays) {
            if (display.displayId == Display.DEFAULT_DISPLAY) continue;
            val name = display.name
            if (name != null && name.startsWith("freeform_")) continue;
            targetDisplayId = display.displayId
            updateDisplayBounds(display)
            break
        }
    }

    private fun updateDisplayBounds(display: Display) {
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        targetDisplayWidth = metrics.widthPixels
        targetDisplayHeight = metrics.heightPixels
        cursorX = (targetDisplayWidth / 2f).coerceIn(0f, targetDisplayWidth.toFloat())
        cursorY = (targetDisplayHeight / 2f).coerceIn(0f, targetDisplayHeight.toFloat())
    }

    private fun setupCursorOverlay() {
        if (cursorAdded || targetDisplayId == Display.INVALID_DISPLAY || !cursorEnabled) return

        val dm = displayManager ?: return
        val display = dm.getDisplay(targetDisplayId) ?: return
        val windowContext = createWindowContext(
            display,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            null,
        )
        cursorWindowManager = windowContext.getSystemService(WindowManager::class.java)

        cursorView = ImageView(windowContext).apply {
            setImageResource(R.drawable.ic_cursor)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val lp = WindowManager.LayoutParams(
            32, 32,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = cursorX.toInt()
            y = cursorY.toInt()
            title = "AxPcModeCursor"
        }

        try {
            cursorWindowManager?.addView(cursorView, lp)
            cursorAdded = true
            cursorView?.let { cursorWindowManager?.bringViewToFront(it) }
            cursorBringToFrontCallback = { bringCursorToFront() }
        } catch (_: Exception) {}
    }

    private fun bringCursorToFront() {
        val v = cursorView ?: return
        if (!cursorAdded) return
        try {
            cursorWindowManager?.bringViewToFront(v)
        } catch (_: Exception) {}
    }

    private fun removeCursorOverlay() {
        if (!cursorAdded) return
        cursorBringToFrontCallback = null
        try {
            cursorView?.let { v ->
                if (v.isAttachedToWindow) {
                    cursorWindowManager?.removeViewImmediate(v)
                }
            }
        } catch (_: Exception) {}
        cursorAdded = false
        cursorView = null
        cursorWindowManager = null
    }

    private fun toggleCursor() {
        cursorEnabled = !cursorEnabled
        if (cursorEnabled) {
            setupCursorOverlay()
        } else {
            removeCursorOverlay()
        }
    }

    private fun updateCursorPosition() {
        val v = cursorView ?: return
        if (!cursorAdded) return
        val lp = v.layoutParams as? WindowManager.LayoutParams ?: return
        lp.x = cursorX.toInt()
        lp.y = cursorY.toInt()
        try {
            cursorWindowManager?.updateViewLayout(v, lp)
        } catch (_: Exception) {}
    }

    private fun injectText(text: String) {
        if (targetDisplayId == Display.INVALID_DISPLAY || text.isEmpty()) return
        val kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        val events = kcm.getEvents(text.toCharArray()) ?: return
        for (event in events) {
            val injected = KeyEvent(
                event.downTime, event.eventTime, event.action,
                event.keyCode, event.repeatCount, event.metaState,
                event.deviceId, event.scanCode,
                KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD,
            )
            injected.displayId = targetDisplayId
            inputManager?.injectInputEvent(
                injected, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
        }
    }

    companion object {
        @Volatile
        var cursorBringToFrontCallback: (() -> Unit)? = null

        @Volatile
        var isActive = false

        private const val TAP_TIMEOUT_MS = 200L
        private const val TAP_SLOP = 20f
        private const val SCROLL_FACTOR = 3f
    }

    @Composable
    private fun MousePadContent() {
        var lastX by remember { mutableFloatStateOf(0f) }
        var lastY by remember { mutableFloatStateOf(0f) }
        var isCursorOn by remember { mutableStateOf(cursorEnabled) }
        var showKeyboard by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconToggleButton(
                    checked = isCursorOn,
                    onCheckedChange = {
                        toggleCursor()
                        isCursorOn = cursorEnabled
                    },
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mouse,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalIconToggleButton(
                    checked = showKeyboard,
                    onCheckedChange = { showKeyboard = it },
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = showKeyboard,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                AndroidView(
                    factory = { ctx ->
                        EditText(ctx).apply {
                            hint = "Type here..."
                            isSingleLine = true
                            inputType = android.text.InputType.TYPE_CLASS_TEXT
                            background = null
                            setPadding(32, 24, 32, 24)
                            setTextColor(android.graphics.Color.WHITE)
                            setHintTextColor(android.graphics.Color.GRAY)

                            var previousText = ""
                            var injecting = false

                            addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                override fun afterTextChanged(s: android.text.Editable?) {
                                    if (injecting) return
                                    val current = s?.toString() ?: ""
                                    injecting = true
                                    if (current.length > previousText.length) {
                                        val added = current.substring(previousText.length)
                                        injectText(added)
                                    } else if (current.length < previousText.length) {
                                        repeat(previousText.length - current.length) {
                                            injectKeyEvent(KeyEvent.KEYCODE_DEL, KeyEvent.ACTION_DOWN)
                                            injectKeyEvent(KeyEvent.KEYCODE_DEL, KeyEvent.ACTION_UP)
                                        }
                                    }
                                    previousText = current
                                    injecting = false
                                }
                            })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 1.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val pointerCount = currentEvent.changes.size
                                lastX = down.position.x
                                lastY = down.position.y
                                val downTime = down.uptimeMillis
                                var totalDx = 0f
                                var totalDy = 0f
                                var wasTwoFinger = pointerCount >= 2

                                injectMouseEvent(MotionEvent.ACTION_HOVER_MOVE, cursorX, cursorY)

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val fingers = event.changes.size

                                    if (fingers >= 2 && !wasTwoFinger) {
                                        wasTwoFinger = true
                                    }

                                    if (event.type == PointerEventType.Move) {
                                        val change = event.changes.firstOrNull() ?: continue

                                        if (wasTwoFinger && fingers >= 2) {
                                            val dx = (change.position.x - lastX) * SCROLL_FACTOR
                                            val dy = (change.position.y - lastY) * SCROLL_FACTOR
                                            lastX = change.position.x
                                            lastY = change.position.y
                                            injectScrollEvent(cursorX, cursorY, dy, -dx)
                                        } else if (!wasTwoFinger) {
                                            val dx = (change.position.x - lastX) * sensitivity
                                            val dy = (change.position.y - lastY) * sensitivity
                                            lastX = change.position.x
                                            lastY = change.position.y
                                            totalDx += kotlin.math.abs(dx)
                                            totalDy += kotlin.math.abs(dy)

                                            cursorX = (cursorX + dx).coerceIn(0f, targetDisplayWidth.toFloat())
                                            cursorY = (cursorY + dy).coerceIn(0f, targetDisplayHeight.toFloat())

                                            injectMouseEvent(MotionEvent.ACTION_HOVER_MOVE, cursorX, cursorY)
                                            updateCursorPosition()
                                        }
                                    } else if (event.type == PointerEventType.Release) {
                                        if (!wasTwoFinger) {
                                            val elapsed = SystemClock.uptimeMillis() - downTime
                                            val moved = totalDx + totalDy
                                            if (elapsed < TAP_TIMEOUT_MS && moved < TAP_SLOP) {
                                                injectMouseEvent(
                                                    MotionEvent.ACTION_DOWN, cursorX, cursorY,
                                                    MotionEvent.BUTTON_PRIMARY,
                                                )
                                                injectMouseEvent(
                                                    MotionEvent.ACTION_UP, cursorX, cursorY,
                                                    MotionEvent.BUTTON_PRIMARY,
                                                )
                                            }
                                        }
                                        break
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ClickButton(
                    label = "Left Click",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    onPress = { injectMouseEvent(MotionEvent.ACTION_DOWN, cursorX, cursorY, MotionEvent.BUTTON_PRIMARY) },
                    onRelease = { injectMouseEvent(MotionEvent.ACTION_UP, cursorX, cursorY, MotionEvent.BUTTON_PRIMARY) },
                )

                ClickButton(
                    label = "Right Click",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                    onPress = { injectMouseEvent(MotionEvent.ACTION_DOWN, cursorX, cursorY, MotionEvent.BUTTON_SECONDARY) },
                    onRelease = { injectMouseEvent(MotionEvent.ACTION_UP, cursorX, cursorY, MotionEvent.BUTTON_SECONDARY) },
                )
            }
        }
    }

    @Composable
    private fun ClickButton(
        label: String,
        containerColor: Color,
        contentColor: Color,
        modifier: Modifier = Modifier,
        onPress: () -> Unit,
        onRelease: () -> Unit,
    ) {
        var pressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.95f else 1f,
            animationSpec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "ClickScale",
        )

        Surface(
            modifier = modifier
                .height(80.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            shape = MaterialTheme.shapes.extraLarge,
            color = containerColor,
            tonalElevation = if (pressed) 0.dp else 2.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pressed = true
                            onPress()
                            do {
                                val ev = awaitPointerEvent()
                            } while (ev.type != PointerEventType.Release)
                            pressed = false
                            onRelease()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                )
            }
        }
    }

    private fun injectMouseEvent(action: Int, x: Float, y: Float, buttonState: Int = 0) {
        if (targetDisplayId == Display.INVALID_DISPLAY) return

        val now = SystemClock.uptimeMillis()
        val properties = arrayOf(MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        })
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = if (action == MotionEvent.ACTION_DOWN) 1f else 0f
            size = 1f
        })

        val event = MotionEvent.obtain(
            now, now, action, 1, properties, coords,
            0, buttonState, 1f, 1f, 0, 0,
            InputDevice.SOURCE_MOUSE, 0,
        )
        event.displayId = targetDisplayId

        try {
            inputManager?.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
        } finally {
            event.recycle()
        }
    }

    private fun injectScrollEvent(x: Float, y: Float, scrollY: Float, scrollX: Float = 0f) {
        if (targetDisplayId == Display.INVALID_DISPLAY) return

        val now = SystemClock.uptimeMillis()
        val properties = arrayOf(MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        })
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            setAxisValue(MotionEvent.AXIS_VSCROLL, scrollY / 100f)
            setAxisValue(MotionEvent.AXIS_HSCROLL, scrollX / 100f)
        })

        val event = MotionEvent.obtain(
            now, now, MotionEvent.ACTION_SCROLL, 1, properties, coords,
            0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_MOUSE, 0,
        )
        event.displayId = targetDisplayId

        try {
            inputManager?.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
        } finally {
            event.recycle()
        }
    }

    private fun injectKeyEvent(keyCode: Int, action: Int, metaState: Int = 0) {
        if (targetDisplayId == Display.INVALID_DISPLAY) return

        val now = SystemClock.uptimeMillis()
        val event = KeyEvent(
            now, now, action, keyCode, 0, metaState,
            -1, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD,
        )
        event.displayId = targetDisplayId

        try {
            inputManager?.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
        } finally {}
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
