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

/**
 * The ease-out cubic the charts arrive with (the same curve as Flutter's
 * `Curves.easeOutCubic`).
 */
val ChartRevealEasing: Easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

/**
 * Draws a chart in, once, when it first appears.
 *
 * A line draws itself from left to right; bars grow up out of the axis; the ring sweeps
 * round. It is not decoration — it is the chart telling you which way to read it, and it
 * is the difference between a picture that was already there when you arrived and one
 * that was drawn for you.
 *
 * Runs exactly once per composition of this node (plain [remember], deliberately not
 * saved state): new data flowing into an already-mounted chart must NOT replay the
 * entrance.
 *
 * ## It honours reduce-motion, and that is not a nicety
 *
 * The system animator switch is the accessibility contract: a user who has asked their
 * phone to stop moving things has asked THIS to stop moving too, and vestibular
 * disorders are the reason that switch exists. With animations disabled the content gets
 * `progress = 1f` on the first frame.
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
