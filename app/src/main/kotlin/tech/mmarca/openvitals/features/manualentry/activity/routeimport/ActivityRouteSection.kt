package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Duration
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.features.activity.maps.OfflineRouteMapOrPreview
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface

@Composable
internal fun ImportedActivityRouteSection(
    state: ActivityEntryUiState,
    unitFormatter: UnitFormatter,
    onClearRoute: () -> Unit,
) {
    val route = state.importedRoute ?: return
    val averageMetrics = routeAverageMetrics(
        route = route,
        pauseIntervals = state.recordedPauseIntervals,
        unitFormatter = unitFormatter,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.activity_entry_imported_route),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            OpenVitalsOutlinedButton(
                onClick = onClearRoute,
                enabled = !state.isSavingEntry,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        OpenVitalsSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OfflineRouteMapOrPreview(
                    points = route.points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
                Text(
                    text = stringResource(
                        R.string.activity_entry_route_summary,
                        route.name
                            ?: route.fileName
                            ?: stringResource(R.string.activity_entry_imported_route),
                        unitFormatter.distance(route.distanceMeters).text,
                        unitFormatter.elevation(route.elevationGainedMeters).text,
                        route.points.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                averageMetrics?.let { metrics ->
                    Text(
                        text = stringResource(
                            R.string.activity_entry_route_average_metrics,
                            metrics.averagePace,
                            metrics.averageSpeed,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal data class RouteAverageMetrics(
    val averagePace: String,
    val averageSpeed: String,
)

internal fun routeAverageMetrics(
    route: RouteFileImport,
    pauseIntervals: List<ActivityPauseInterval>,
    unitFormatter: UnitFormatter,
): RouteAverageMetrics? {
    val movingDurationMs = routeMovingDurationMs(route, pauseIntervals).takeIf { it > 0L } ?: return null
    val averagePace = unitFormatter.averagePace(route.distanceMeters, movingDurationMs)?.text ?: return null
    val averageSpeed = unitFormatter.averageSpeed(route.distanceMeters, movingDurationMs).text
    return RouteAverageMetrics(
        averagePace = averagePace,
        averageSpeed = averageSpeed,
    )
}

internal fun routeMovingDurationMs(
    route: RouteFileImport,
    pauseIntervals: List<ActivityPauseInterval>,
): Long {
    val durationMs = Duration.between(route.startTime, route.endTime).toMillis().coerceAtLeast(0L)
    val pausedMs = pauseIntervals
        .sumOf { interval -> Duration.between(interval.startTime, interval.endTime).toMillis().coerceAtLeast(0L) }
        .coerceAtMost(durationMs)
    return (durationMs - pausedMs).coerceAtLeast(0L)
}
