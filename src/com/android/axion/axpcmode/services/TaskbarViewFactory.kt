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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.android.axion.axpcmode.ui.components.ContextMenuState
import com.android.axion.axpcmode.ui.components.GestureHintBar
import com.android.axion.axpcmode.ui.components.LocalContextMenuState
import com.android.axion.axpcmode.ui.components.LocalWindowScreenOffset
import com.android.axion.axpcmode.ui.components.Taskbar
import com.android.axion.axpcmode.ui.theme.AxPcModeTheme
import com.android.axion.axpcmode.ui.windows.LocalTaskbarIconRegistry
import com.android.axion.axpcmode.ui.windows.TaskbarIconRegistry
import com.android.axion.axpcmode.utils.AppInfo
import com.android.axion.compose.lifecycle.repeatWhenAttached
import kotlinx.coroutines.flow.StateFlow

private val pillWidth = 224.dp

class TaskbarViewFactory(
    private val context: Context,
    private val windowHelper: TaskbarWindowManager,
    private val contextMenuState: ContextMenuState,
    private val taskbarIconRegistry: TaskbarIconRegistry,
    private val interactor: TaskbarInteractor,
) {

    fun createTaskbar(
        isVisibleFlow: StateFlow<Boolean>,
        pinnedAppsFlow: StateFlow<List<AppInfo>>,
        runningAppsFlow: StateFlow<List<AppInfo>>,
    ): ComposeView {
        return createComposeView {
            val isVisible by isVisibleFlow.collectAsState()
            val pinnedApps by pinnedAppsFlow.collectAsState()
            val runningApps by runningAppsFlow.collectAsState()

            val alpha by
                animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = tween(300),
                    label = "TaskbarAlpha",
                )

            val density = LocalDensity.current
            val config = LocalConfiguration.current
            val screenHeight = config.screenHeightDp.dp
            val yOffsetPx = with(density) { (screenHeight - 56.dp).toPx() }

            CompositionLocalProvider(
                LocalContextMenuState provides contextMenuState,
                LocalTaskbarIconRegistry provides taskbarIconRegistry,
                LocalWindowScreenOffset provides Offset(0f, yOffsetPx),
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer { this.alpha = alpha }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (
                                            event.type == PointerEventType.Press ||
                                                event.type == PointerEventType.Move
                                        ) {
                                            interactor.onUserInteraction()
                                        }
                                    }
                                }
                            },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Taskbar(
                        pinnedApps = pinnedApps,
                        runningApps = runningApps,
                        onStartClick = { interactor.onStartClick() },
                        onAppClick = { app -> interactor.onAppClick(app) },
                        onBackClick = { interactor.onBackClick() },
                        onHomeClick = { interactor.onHomeClick() },
                        onRecentsClick = { interactor.onRecentsClick() },
                        onImeClick = { interactor.onImeClick() },
                        onMediaClick = { interactor.onMediaClick() },
                        onQuickSettingsClick = { interactor.onQuickSettingsClick() },
                        onNotificationClick = { interactor.onNotificationClick() },
                        modifier = Modifier.height(56.dp),
                    )
                }
            }
        }
    }

    fun createHint(isVisibleFlow: StateFlow<Boolean>): ComposeView {
        return createComposeView {
            val isVisible by isVisibleFlow.collectAsState()
            val density = LocalDensity.current

            val alpha by
                animateFloatAsState(
                    targetValue = if (!isVisible) 1f else 0f,
                    animationSpec = tween(300),
                    label = "HintAlpha",
                )

            Box(
                modifier =
                    Modifier.width(pillWidth)
                        .graphicsLayer { this.alpha = alpha }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Enter) {
                                        interactor.onRevealTaskbar()
                                    }
                                }
                            }
                        },
                contentAlignment = Alignment.BottomCenter,
            ) {
                GestureHintBar(
                    onReveal = { interactor.onRevealTaskbar() },
                    modifier = Modifier.width(pillWidth),
                )
            }
        }
    }

    private fun createComposeView(content: @Composable () -> Unit): ComposeView {
        return ComposeView(context).apply {
            repeatWhenAttached {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent { AxPcModeTheme { content() } }
                }
            }
        }
    }
}
