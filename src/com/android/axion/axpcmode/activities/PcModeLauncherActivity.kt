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

import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.axion.axpcmode.services.NotificationManagerHelper
import com.android.axion.axpcmode.ui.PcModeLauncherScreen
import com.android.axion.axpcmode.ui.PcModeLauncherViewModel
import com.android.axion.axpcmode.ui.theme.AxPcModeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay

@AndroidEntryPoint(ComponentActivity::class)
class PcModeLauncherActivity : Hilt_PcModeLauncherActivity() {

    @Inject lateinit var viewModel: PcModeLauncherViewModel

    private var notificationHelper: NotificationManagerHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {}
            },
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        notificationHelper = NotificationManagerHelper(this)
        notificationHelper?.register()

        setContent {
            AxPcModeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    PcModeIntroAnimation(viewModel)
                }
            }
        }
    }

    @Composable
    private fun PcModeIntroAnimation(viewModel: PcModeLauncherViewModel) {

        val progress = remember { Animatable(0f) }
        var showContent by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            )
            delay(300)
            showContent = true
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))

            if (!showContent) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AxioLogoAnim(progress = progress.value, modifier = Modifier.size(200.dp))
                }
            }

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)),
                modifier = Modifier.fillMaxSize(),
            ) {
                PcModeLauncherScreen(viewModel)
            }
        }
    }

    @Composable
    private fun AxioLogoAnim(
        progress: Float,
        modifier: Modifier = Modifier,
        strokeWidthDp: Dp = 3.dp,
        alpha: Float = 1f,
    ) {
        val strokeWidth = with(LocalDensity.current) { strokeWidthDp.toPx() }
        val backgroundColor = MaterialTheme.colorScheme.onSurface
        val letterColor = MaterialTheme.colorScheme.surfaceContainer

        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height

            val hexPath =
                ComposePath().apply {
                    val cx = w / 2f

                    moveTo(cx, 0f)
                    lineTo(w, h * 0.25f)
                    lineTo(w, h * 0.75f)
                    lineTo(cx, h)
                    lineTo(0f, h * 0.75f)
                    lineTo(0f, h * 0.25f)
                    close()
                }

            val aPath =
                ComposePath().apply {
                    val cx = w / 2f
                    val topY = h * 0.25f
                    val botY = h * 0.75f
                    val widthAtBot = w * 0.4f

                    moveTo(cx, topY)
                    lineTo(cx + widthAtBot, botY)
                    lineTo(cx + widthAtBot * 0.7f, botY)
                    lineTo(cx + widthAtBot * 0.2f, h * 0.55f)
                    lineTo(cx - widthAtBot * 0.2f, h * 0.55f)
                    lineTo(cx - widthAtBot * 0.7f, botY)
                    lineTo(cx - widthAtBot, botY)
                    close()
                }

            fun drawPartialPath(path: ComposePath, pathProgress: Float, color: Color) {
                if (pathProgress <= 0f) return

                val measure = PathMeasure(path.asAndroidPath(), false)
                val length = measure.length
                val partialPath = AndroidPath()
                measure.getSegment(0f, length * pathProgress, partialPath, true)

                drawPath(
                    path = ComposePath().apply { addPath(partialPath.asComposePath()) },
                    color = color,
                    style =
                        Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    alpha = alpha,
                )
            }

            val traceProgress = (progress / 0.7f).coerceIn(0f, 1f)
            val fillAlpha = ((progress - 0.7f) / 0.3f).coerceIn(0f, 1f)

            drawPartialPath(hexPath, traceProgress, Color.White)

            drawPartialPath(aPath, traceProgress, Color.White)

            if (fillAlpha > 0f) {
                drawPath(path = hexPath, color = backgroundColor.copy(alpha = fillAlpha * alpha))
                drawPath(path = aPath, color = letterColor.copy(alpha = fillAlpha * alpha))
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        notificationHelper?.unregister()
    }
}
