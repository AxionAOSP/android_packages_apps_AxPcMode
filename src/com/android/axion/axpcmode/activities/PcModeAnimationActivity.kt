package com.android.axion.axpcmode.activities

import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.android.axion.axpcmode.ui.theme.AxPcModeTheme
import kotlinx.coroutines.delay

class PcModeAnimationActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PcModeAnimationActivity"
        const val EXTRA_EXIT_PC_MODE = "exit_pc_mode"
    }

    private var shouldExitPcMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shouldExitPcMode = intent?.getBooleanExtra(EXTRA_EXIT_PC_MODE, false) == true
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {}
            },
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent { AxPcModeTheme { PcModeExitAnimation(onExitComplete = { onAnimationComplete() }) } }
    }

    private fun onAnimationComplete() {
        if (shouldExitPcMode) {
            Settings.Secure.putInt(contentResolver, "ax_pc_mode", 0)
        }
        finishAffinity()
    }

    @Composable
    private fun PcModeExitAnimation(onExitComplete: () -> Unit) {

        val overlayAlpha = remember { Animatable(0f) }
        val traceProgress = remember { Animatable(1f) }

        LaunchedEffect(Unit) {
            overlayAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing),
            )

            delay(200)

            traceProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            )
            onExitComplete()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = overlayAlpha.value))
            )

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (overlayAlpha.value > 0f) {
                    AxioLogoAnim(
                        progress = traceProgress.value,
                        modifier = Modifier.size(200.dp),
                        alpha = overlayAlpha.value,
                    )
                }
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
}
