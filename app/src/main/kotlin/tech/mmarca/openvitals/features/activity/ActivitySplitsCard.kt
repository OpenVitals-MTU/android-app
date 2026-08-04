package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.ActivitySplit
import tech.mmarca.openvitals.domain.insights.ActivitySplits
import tech.mmarca.openvitals.domain.insights.SplitSource
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.WorkoutColor

/**
 * The splits card on the activity detail screen: one row per split (or per
 * device lap), with a bar whose length tracks the split's pace so a slow
 * kilometre is visible without reading a single number.
 *
 * The header states the PROVENANCE, and the estimated case says out loud that
 * the identical pace on every row is an artefact of missing data — the whole
 * point of keeping [SplitSource] on the result.
 */
@Composable
internal fun ActivitySplitsCard(
    splits: ActivitySplits,
    splitDistanceMeters: Double,
    slowestPaceSeconds: Double?,
    fastestPaceSeconds: Double?,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val rows = splits.splits
    if (rows.isEmpty()) return

    val unitMeters = when (unitFormatter.unitSystem()) {
        UnitSystem.METRIC -> 1000.0
        UnitSystem.IMPERIAL -> 1609.344
    }

    val title: String
    val subtitle: String?
    when (splits.source) {
        SplitSource.DEVICE_LAPS -> {
            title = stringResource(R.string.activity_splits_laps_title)
            subtitle = stringResource(R.string.activity_splits_laps_body)
        }
        SplitSource.ROUTE, SplitSource.SPEED_SAMPLES -> {
            title = stringResource(
                R.string.activity_splits_derived_title,
                splitDistanceLabel(unitFormatter, splitDistanceMeters),
            )
            subtitle = null
        }
        SplitSource.ESTIMATED -> {
            title = stringResource(R.string.activity_splits_estimated_title)
            subtitle = stringResource(R.string.activity_splits_estimated_body)
        }
    }

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            rows.forEach { split ->
                SplitRow(
                    split = split,
                    unitFormatter = unitFormatter,
                    unitMeters = unitMeters,
                    slowestPaceSeconds = slowestPaceSeconds,
                    fastestPaceSeconds = fastestPaceSeconds,
                    // The estimated source gives every split the same pace: a
                    // bar chart of it would be a straight line masquerading as
                    // a measurement. Show the numbers, drop the bar.
                    showBar = splits.source != SplitSource.ESTIMATED,
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SplitRow(
    split: ActivitySplit,
    unitFormatter: UnitFormatter,
    unitMeters: Double,
    slowestPaceSeconds: Double?,
    fastestPaceSeconds: Double?,
    showBar: Boolean,
) {
    val muted = MaterialTheme.typography.bodySmall
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    val pace = unitFormatter.averagePace(split.distanceMeters, split.elapsedMs)
    val delta = split.paceDeltaSecondsPerUnit(unitMeters)

    // A partial split is short ON PURPOSE — say so, or it reads as a bad fix.
    val distance = unitFormatter.distance(split.distanceMeters).text
    val distanceText = if (split.isPartial) {
        "$distance (${stringResource(R.string.activity_splits_partial)})"
    } else {
        distance
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(24.dp)) {
                Text(
                    text = split.index.toString(),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = distanceText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = unitFormatter.duration(split.elapsedMs),
                style = muted,
                color = mutedColor,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = pace?.text ?: "--",
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(92.dp),
            )
        }
        if (showBar) {
            Spacer(Modifier.height(4.dp))
            PaceBar(
                // Per kilometre, like the scale it is measured against.
                paceSeconds = split.paceSecondsPerUnit(1000.0),
                slowestPaceSeconds = slowestPaceSeconds,
                fastestPaceSeconds = fastestPaceSeconds,
                fasterThanAverage = delta != null && delta < 0,
                modifier = Modifier.padding(start = 24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = splitDetailLine(split, delta, unitFormatter),
            style = muted,
            color = mutedColor,
            modifier = Modifier.padding(start = 24.dp),
        )
    }
}

/**
 * avg HR · elevation ± · pace delta. Each piece is dropped when its datum is
 * missing rather than rendered as a zero: a treadmill split has NO elevation,
 * which is not the same claim as "flat".
 */
@Composable
private fun splitDetailLine(
    split: ActivitySplit,
    delta: Double?,
    unitFormatter: UnitFormatter,
): String {
    val parts = mutableListOf<String>()
    split.averageHeartRateBpm?.let { bpm ->
        parts.add(unitFormatter.heartRate(bpm.roundToLong()).text)
    }
    val gain = split.elevationGainMeters
    val loss = split.elevationLossMeters
    if (gain != null && loss != null) {
        parts.add(
            "↑ ${unitFormatter.elevation(gain).text}  ↓ ${unitFormatter.elevation(loss).text}",
        )
    }
    if (delta != null && delta.roundToInt() != 0) {
        val magnitude = formatSplitDeltaSeconds(abs(delta))
        parts.add(
            if (delta < 0) {
                stringResource(R.string.activity_splits_faster, magnitude)
            } else {
                stringResource(R.string.activity_splits_slower, magnitude)
            },
        )
    }
    return parts.joinToString("  ·  ")
}

/** `0:08` / `1:12` — the delta's magnitude as minutes:seconds. */
private fun formatSplitDeltaSeconds(seconds: Double): String {
    val total = seconds.roundToInt()
    val minutes = total / 60
    val rest = total % 60
    return "$minutes:${rest.toString().padStart(2, '0')}"
}

/**
 * A pace bar: the slowest split in the activity fills the track, the fastest
 * leaves it visibly shorter. Deliberately NOT zero-based — the interesting
 * range of a run is the few percent between its fastest and slowest kilometre,
 * and a zero-based bar squashes that into invisibility. The floor is 25% of
 * the track so the fastest split still reads as a bar, not as nothing.
 */
@Composable
private fun PaceBar(
    paceSeconds: Double?,
    slowestPaceSeconds: Double?,
    fastestPaceSeconds: Double?,
    fasterThanAverage: Boolean,
    modifier: Modifier = Modifier,
) {
    if (paceSeconds == null || slowestPaceSeconds == null || fastestPaceSeconds == null) {
        Spacer(modifier = modifier.height(6.dp))
        return
    }

    val minFraction = 0.25
    val span = slowestPaceSeconds - fastestPaceSeconds
    val fraction = if (span <= 0) {
        1.0
    } else {
        minFraction + (1 - minFraction) * ((paceSeconds - fastestPaceSeconds) / span)
    }
    val color = if (fasterThanAverage) {
        WorkoutColor
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(minFraction, 1.0).toFloat())
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}
