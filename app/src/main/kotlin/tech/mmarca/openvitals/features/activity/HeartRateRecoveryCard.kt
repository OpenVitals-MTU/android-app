package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryIssue
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryQuality
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryReading
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

/**
 * How far the heart rate fell after one workout. A mark the watch could not
 * record comes back blank with a line saying why, never interpolated.
 */
@Composable
internal fun HeartRateRecoveryCard(
    reading: HeartRateRecoveryReading,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = accentColor(reading.quality, scheme)
    val headlineDrop = reading.headlineDropBpm
    val peakBpm = reading.peakBpm

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.TrendingDown,
                    contentDescription = null,
                    tint = accent,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.heart_rate_recovery_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (peakBpm != null) {
                    Text(
                        text = stringResource(R.string.heart_rate_recovery_peak, peakBpm),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (headlineDrop != null) {
                    stringResource(R.string.heart_rate_recovery_headline, headlineDrop)
                } else {
                    stringResource(R.string.heart_rate_recovery_headline_unavailable)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (headlineDrop != null) accent else scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            HeartRateRecoveryMarksRow(reading = reading)
            issueExplanations(reading).forEach { text ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The six marks. A mark with no reading behind it shows a dash, not a number. */
@Composable
private fun HeartRateRecoveryMarksRow(reading: HeartRateRecoveryReading) {
    val scheme = MaterialTheme.colorScheme
    val notMeasured = stringResource(R.string.heart_rate_recovery_no_sample)
    Row {
        reading.marks.forEach { mark ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = markLabel(mark.offset),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                val dropBpm = mark.dropBpm
                Text(
                    text = dropBpm?.toString() ?: "—",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dropBpm != null) {
                        scheme.onSurface
                    } else {
                        scheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = if (dropBpm == null) {
                        Modifier.semantics { contentDescription = notMeasured }
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

/** Why the reading looks the way it does, most consequential first. */
@Composable
private fun issueExplanations(reading: HeartRateRecoveryReading): List<String> {
    val issues = reading.issues
    return buildList {
        if (HeartRateRecoveryIssue.NO_RECOVERY_SAMPLES in issues) {
            add(stringResource(R.string.heart_rate_recovery_no_recovery_samples))
        }
        if (HeartRateRecoveryIssue.HEART_RATE_DID_NOT_FALL in issues) {
            add(stringResource(R.string.heart_rate_recovery_did_not_fall))
        }
        if (HeartRateRecoveryIssue.COOLDOWN_BEFORE_STOP in issues) {
            add(stringResource(R.string.heart_rate_recovery_cooldown_before_stop))
        }
        if (HeartRateRecoveryIssue.SUBMAXIMAL_EFFORT in issues) {
            add(stringResource(R.string.heart_rate_recovery_submaximal_effort))
        }
        if (HeartRateRecoveryIssue.PEAK_FROM_SINGLE_SAMPLE in issues) {
            add(stringResource(R.string.heart_rate_recovery_peak_from_single_sample))
        }
        if (HeartRateRecoveryIssue.UNKNOWN_MAX_HEART_RATE in issues) {
            add(stringResource(R.string.heart_rate_recovery_unknown_max_heart_rate))
        }
    }
}

@Composable
private fun markLabel(offset: Duration): String =
    if (offset.seconds < 60) {
        stringResource(R.string.heart_rate_recovery_mark_seconds, offset.seconds)
    } else {
        stringResource(R.string.heart_rate_recovery_mark_minutes, offset.toMinutes())
    }

private fun accentColor(quality: HeartRateRecoveryQuality, scheme: ColorScheme): Color =
    when (quality) {
        HeartRateRecoveryQuality.CLEAN,
        HeartRateRecoveryQuality.APPROXIMATE,
        -> scheme.primary
        HeartRateRecoveryQuality.NOT_COMPARABLE -> scheme.tertiary
        HeartRateRecoveryQuality.INVALID,
        HeartRateRecoveryQuality.NO_DATA,
        -> scheme.onSurfaceVariant
    }
