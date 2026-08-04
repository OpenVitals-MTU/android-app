package tech.mmarca.openvitals.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.ui.theme.Motion

/**
 * The shape of what is coming, while it comes.
 *
 * A spinner in the middle of a card says "something is happening". A skeleton says "a
 * chart is happening, and it will be about this big" — so the page does not jump when
 * the data lands, and the eye has already found the place to look. It is also the
 * difference between a screen that feels like it is loading and one that feels like it
 * is broken.
 */
enum class ChartSkeletonShape { LINE, BARS }

@Composable
fun ChartSkeleton(
    modifier: Modifier = Modifier,
    shape: ChartSkeletonShape = ChartSkeletonShape.LINE,
    height: Dp = ChartTokens.heightDay,
    barCount: Int = 7,
) {
    // A REPEATING animation is the one kind that never settles. When the user has asked
    // their phone to stop moving things (animator scale off), it is pinned to a still
    // frame instead of pulsing.
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val pulse = if (animationsEnabled) {
        rememberInfiniteTransition(label = "ChartSkeletonPulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(Motion.skeletonPulseMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "ChartSkeletonAlpha",
        ).value
    } else {
        0f
    }

    // Breathes between two alphas rather than sweeping a gradient across the card: a
    // shimmer that travels is a thing to watch, and this is a thing to stop noticing.
    val color = ChartTokens.track.copy(alpha = 0.35f + 0.25f * pulse)

    when (shape) {
        ChartSkeletonShape.BARS -> Row(
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            repeat(barCount) { index ->
                Box(
                    modifier = Modifier
                        .width(SkeletonBarWidth)
                        // Uneven, because a row of identical bars reads as data — a very
                        // boring week — rather than as an absence of it.
                        .fillMaxHeight(SkeletonBarFractions[index % SkeletonBarFractions.size])
                        .background(
                            color = color,
                            shape = RoundedCornerShape(
                                topStart = ChartTokens.barRadius(SkeletonBarWidth),
                                topEnd = ChartTokens.barRadius(SkeletonBarWidth),
                            ),
                        ),
                )
            }
        }

        ChartSkeletonShape.LINE -> Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(3.dp)
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
}

private val SkeletonBarWidth = 14.dp
private val SkeletonBarFractions =
    listOf(0.45f, 0.7f, 0.35f, 0.85f, 0.55f, 0.75f, 0.4f)
