package tech.mmarca.openvitals.ui.theme

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the user has asked the system to stop animating.
 *
 * An accessibility floor rather than a preference to weigh: vestibular
 * disorders are the reason that switch exists. Android has no
 * `prefers-reduced-motion` media query — the equivalent is the animator
 * duration scale, which "Remove animations" in Accessibility settings and the
 * developer options both drive to zero, and which
 * [ValueAnimator.areAnimatorsEnabled] reports.
 *
 * `ChartReveal` and `ChartSkeleton` each read that API directly and have done
 * so correctly for a while. This local exists so the rest of the app does not
 * have to remember to: everything that moves reads one answer, resolved once
 * per theme.
 *
 * Defaults to false so a composable rendered outside [OpenVitalsTheme] — a
 * preview, a test harness — animates as before rather than silently freezing.
 */
val LocalReducedMotion = compositionLocalOf { false }

/** Reads the system animator scale; false when animations are switched off. */
internal fun animatorsEnabled(): Boolean =
    runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)

/**
 * [durationMillis], or zero when the user has asked for no animation.
 *
 * The policy: reveals and ambient movement drop to nothing, while functional
 * feedback stays at or under [Motion.pressMillis] — a control still
 * acknowledges a tap, it just does not travel to do it.
 *
 * Zero rather than "very fast", because Compose runs a zero-length animation by
 * jumping straight to the target value, which is what "remove animations" asks
 * for.
 */
@Composable
fun animationDuration(durationMillis: Int): Int =
    if (LocalReducedMotion.current && durationMillis > Motion.pressMillis) 0 else durationMillis

/**
 * Whether a looping, decorative-adjacent animation may run at all.
 *
 * A repeating animation is the one kind that never settles, so it is the one
 * reduced motion most needs to stop. Callers pin themselves to a still frame
 * when this is false rather than looping faster.
 */
@Composable
fun loopingMotionAllowed(): Boolean = !LocalReducedMotion.current
