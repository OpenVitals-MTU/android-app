package tech.mmarca.openvitals.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import tech.mmarca.openvitals.ui.theme.Motion

/** The ease-out cubic the charts arrive with, matching Flutter's `easeOutCubic`. */
val ChartRevealEasing: Easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

/**
 * Draws a chart in once, when it first appears. Runs once per composition
 * (plain [remember]): new data must not replay the entrance. Under reduced
 * motion the content gets `progress = 1f` on the first frame.
 */
@Composable
fun ChartReveal(
    durationMillis: Int = Motion.chartEntryMillis,
    easing: Easing = ChartRevealEasing,
    content: @Composable (progress: Float) -> Unit,
) {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis, easing = easing),
            )
        }
    }
    content(progress.value)
}
