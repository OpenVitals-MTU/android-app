package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.ExerciseSessionRecord
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
internal fun ActivityTypeAggregateStatsCard(
    aggregates: List<ActivityTypeAggregate>,
    unitFormatter: UnitFormatter,
) {
    if (aggregates.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = stringResource(R.string.section_activity_type_stats))
        OpenVitalsCard(
            modifier = activityTypeAggregateModifier(),
        ) {
            aggregates.forEachIndexed { index, aggregate ->
                ActivityTypeAggregateRow(
                    aggregate = aggregate,
                    unitFormatter = unitFormatter,
                )
                if (index < aggregates.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

private fun activityTypeAggregateModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

@Composable
private fun ActivityTypeAggregateRow(
    aggregate: ActivityTypeAggregate,
    unitFormatter: UnitFormatter,
) {
    val usePace = aggregate.prefersPace()
    val noData = stringResource(R.string.no_data)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(WorkoutColor.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = exerciseTypeIcon(aggregate.exerciseType),
                    contentDescription = null,
                    tint = WorkoutColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AutoResizeText(
                    text = exerciseTypeLabel(aggregate.exerciseType),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.activity_type_stats_activity_count,
                        aggregate.count,
                        aggregate.count,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AggregateMetric(
                label = stringResource(R.string.metric_distance),
                value = aggregate.totalDistanceMeters
                    .takeIf { it > 0.0 }
                    ?.let(unitFormatter::distance)
                    ?: DisplayValue(noData, ""),
                modifier = Modifier.weight(1f),
            )
            AggregateMetric(
                label = stringResource(R.string.stat_time),
                value = DisplayValue(unitFormatter.duration(aggregate.totalDurationMs), ""),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AggregateMetric(
                label = stringResource(
                    if (usePace) {
                        R.string.stat_average_moving_pace
                    } else {
                        R.string.activity_entry_recording_average_moving_speed
                    },
                ),
                value = if (usePace) {
                    aggregate.averageMovingPace(unitFormatter) ?: DisplayValue(noData, "")
                } else {
                    aggregate.averageMovingSpeedMetersPerSecond
                        ?.let(unitFormatter::speed)
                        ?: DisplayValue(noData, "")
                },
                modifier = Modifier.weight(1f),
            )
            AggregateMetric(
                label = stringResource(
                    if (usePace) {
                        R.string.stat_fastest_pace
                    } else {
                        R.string.stat_best_speed
                    },
                ),
                value = if (usePace) {
                    aggregate.bestPace(unitFormatter) ?: DisplayValue(noData, "")
                } else {
                    aggregate.bestSpeedMetersPerSecond
                        ?.let(unitFormatter::speed)
                        ?: DisplayValue(noData, "")
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AggregateMetric(
    label: String,
    value: DisplayValue,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AutoResizeText(
            text = value.value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        if (value.unit.isNotBlank()) {
            Text(
                text = value.unit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun ActivityTypeAggregate.prefersPace(): Boolean =
    exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING ||
        exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL ||
        exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_WALKING ||
        exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_HIKING ||
        exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING

private fun ActivityTypeAggregate.averageMovingPace(unitFormatter: UnitFormatter): DisplayValue? =
    unitFormatter.averagePace(totalDistanceMeters, totalMovingDurationMs)

private fun ActivityTypeAggregate.bestPace(unitFormatter: UnitFormatter): DisplayValue? {
    val speed = bestSpeedMetersPerSecond?.takeIf { it > 0.0 && it.isFinite() } ?: return null
    return unitFormatter.averagePace(distanceMeters = 1.0, durationMs = (1_000.0 / speed).toLong())
}
