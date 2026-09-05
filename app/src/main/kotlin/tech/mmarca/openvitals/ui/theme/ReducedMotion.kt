package tech.mmarca.openvitals.ui.theme

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the user asked the system to stop animating. Android's
 * equivalent of `prefers-reduced-motion` is the animator duration scale,
 * which [ValueAnimator.areAnimatorsEnabled] reports. Resolved once per
 * theme. Defaults to false outside [OpenVitalsTheme].
 */
val LocalReducedMotion = compositionLocalOf { false }

/** Reads the system animator scale; false when animations are switched off. */
internal fun animatorsEnabled(): Boolean =
    runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)

/**
 * [durationMillis], or zero under reduced motion. Functional feedback
 * stays at or under [Motion.pressMillis]. Zero jumps straight to the target.
 */
@Composable
fun animationDuration(durationMillis: Int): Int =
    if (LocalReducedMotion.current && durationMillis > Motion.pressMillis) 0 else durationMillis

/** Whether a looping animation may run at all. Callers pin to a still frame when false. */
@Composable
fun loopingMotionAllowed(): Boolean = !LocalReducedMotion.current
