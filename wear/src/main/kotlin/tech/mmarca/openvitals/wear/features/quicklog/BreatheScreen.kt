package tech.mmarca.openvitals.wear.features.quicklog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.Emphasis
import tech.mmarca.openvitals.wear.ui.theme.MindfulnessColor
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme

/** Six breaths a minute: four seconds in, six out. */
private const val InhaleMillis = 4_000
private const val ExhaleMillis = 6_000
private const val Breaths = 6
private const val SessionMillis = Breaths * (InhaleMillis + ExhaleMillis)
private const val SmallestCircle = 0.45f

/**
 * A one-minute guided breath, like Fitbit Relax or Apple's Breathe: a circle
 * grows on the in-breath and shrinks on the out-breath, with a tick of haptics
 * at each turn. Finishing logs a minute of mindfulness; swiping away does not.
 */
@Composable
fun BreatheScreen(onFinished: () -> Unit, onClose: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val circle = remember { Animatable(SmallestCircle) }
    val progress = remember { Animatable(0f) }
    var inhaling by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { progress.animateTo(1f, tween(SessionMillis, easing = LinearEasing)) }
            repeat(Breaths) {
                inhaling = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                circle.animateTo(1f, tween(InhaleMillis, easing = FastOutSlowInEasing))
                inhaling = false
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                circle.animateTo(SmallestCircle, tween(ExhaleMillis, easing = FastOutSlowInEasing))
            }
        }
        finish()
        finished = true
    }

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .keepScreenOn(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                colors = ProgressIndicatorDefaults.colors(
                    indicatorColor = MindfulnessColor,
                    trackColor = MindfulnessColor.copy(alpha = Emphasis.subtle),
                ),
            )
            Canvas(modifier = Modifier.size(120.dp)) {
                drawCircle(MindfulnessColor.copy(alpha = Emphasis.fill), radius = size.minDimension / 2 * circle.value)
            }
            Text(
                text = stringResource(if (inhaling) R.string.breathe_in else R.string.breathe_out),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    SuccessConfirmationDialog(
        visible = finished,
        onDismissRequest = onClose,
        curvedText = null,
    )
}

@WearPreviews
@Composable
private fun BreatheScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            BreatheScreen(onFinished = {}, onClose = {})
        }
    }
}
