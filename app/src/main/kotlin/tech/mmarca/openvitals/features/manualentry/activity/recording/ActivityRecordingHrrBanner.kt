package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

/**
 * What the rider must do next, and for how long. End effort is available
 * through the warmup and effort, gone during recovery: pressing it again
 * would move the instant the measurement hangs on.
 */
@Composable
internal fun ActivityHeartRateRecoveryPhaseBanner(
    state: ActivityRecordingState,
    now: Instant,
    onEndEffort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remaining = state.hrrPhaseRemaining(now)
    val canEndEffort = state.hrrPhase == ActivityRecordingHrrPhase.WARMUP ||
        state.hrrPhase == ActivityRecordingHrrPhase.EFFORT

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = hrrPhaseText(state.hrrPhase),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when (state.hrrPhase) {
                    ActivityRecordingHrrPhase.EFFORT -> MaterialTheme.colorScheme.error
                    ActivityRecordingHrrPhase.RECOVERY,
                    ActivityRecordingHrrPhase.COMPLETE,
                    -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            if (remaining != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = hrrCountdownText(remaining),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            state.currentHeartRateBpm?.let { bpm ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.activity_recording_hrr_current_bpm, bpm),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canEndEffort) {
                Spacer(Modifier.height(12.dp))
                OpenVitalsButton(
                    onClick = onEndEffort,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.activity_recording_hrr_end_effort),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun hrrPhaseText(phase: ActivityRecordingHrrPhase): String = when (phase) {
    ActivityRecordingHrrPhase.WARMUP ->
        stringResource(R.string.activity_recording_hrr_phase_warmup)
    ActivityRecordingHrrPhase.EFFORT ->
        stringResource(R.string.activity_recording_hrr_phase_effort)
    ActivityRecordingHrrPhase.RECOVERY ->
        stringResource(R.string.activity_recording_hrr_phase_recovery)
    ActivityRecordingHrrPhase.COMPLETE ->
        stringResource(R.string.activity_recording_hrr_phase_complete)
    ActivityRecordingHrrPhase.NONE -> ""
}

internal fun hrrCountdownText(remaining: Duration): String {
    val minutes = remaining.toMinutes()
    val seconds = remaining.seconds % 60
    return "%d:%02d".format(minutes, seconds)
}
