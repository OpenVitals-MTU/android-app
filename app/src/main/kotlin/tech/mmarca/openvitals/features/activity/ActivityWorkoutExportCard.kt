package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.DetailSectionCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * Every way a workout leaves the app, in one place. The route formats (GPX,
 * KMZ) appear only when a route exists; the metric formats (TCX, CSV) are on
 * every workout, because a treadmill session has data worth getting out too,
 * and a session with a route may be shared with someone who should not get
 * the location half of it. The description names which formats carry the
 * route, so the privacy choice is made with open eyes.
 */
@Composable
internal fun WorkoutExportCard(
    onSaveAsTcx: () -> Unit,
    onSaveAsCsv: () -> Unit,
    onShareAsTcx: () -> Unit,
    onShareAsCsv: () -> Unit,
    onSaveRouteAsGpx: (() -> Unit)? = null,
    onSaveRouteAsKmz: (() -> Unit)? = null,
    onShareRouteAsGpx: (() -> Unit)? = null,
    onShareRouteAsKmz: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val hasRouteFormats = onSaveRouteAsGpx != null && onSaveRouteAsKmz != null &&
        onShareRouteAsGpx != null && onShareRouteAsKmz != null
    DetailSectionCard(title = stringResource(R.string.activity_workout_export_title), modifier = modifier) {
        Text(
            text = stringResource(
                if (hasRouteFormats) {
                    R.string.activity_workout_export_description_with_route
                } else {
                    R.string.activity_workout_export_description
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        if (hasRouteFormats) {
            WorkoutExportButtonRow(
                firstLabel = stringResource(R.string.activity_route_export_gpx),
                secondLabel = stringResource(R.string.activity_route_export_kmz),
                icon = Icons.Outlined.FileDownload,
                onFirst = checkNotNull(onSaveRouteAsGpx),
                onSecond = checkNotNull(onSaveRouteAsKmz),
            )
            WorkoutExportButtonRow(
                firstLabel = stringResource(R.string.activity_route_share_gpx),
                secondLabel = stringResource(R.string.activity_route_share_kmz),
                icon = Icons.Outlined.Share,
                onFirst = checkNotNull(onShareRouteAsGpx),
                onSecond = checkNotNull(onShareRouteAsKmz),
            )
        }
        WorkoutExportButtonRow(
            firstLabel = stringResource(R.string.activity_workout_export_tcx),
            secondLabel = stringResource(R.string.activity_workout_export_csv),
            icon = Icons.Outlined.FileDownload,
            onFirst = onSaveAsTcx,
            onSecond = onSaveAsCsv,
        )
        WorkoutExportButtonRow(
            firstLabel = stringResource(R.string.activity_workout_share_tcx),
            secondLabel = stringResource(R.string.activity_workout_share_csv),
            icon = Icons.Outlined.Share,
            onFirst = onShareAsTcx,
            onSecond = onShareAsCsv,
        )
    }
}

@Composable
private fun WorkoutExportButtonRow(
    firstLabel: String,
    secondLabel: String,
    icon: ImageVector,
    onFirst: () -> Unit,
    onSecond: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        WorkoutExportButton(
            label = firstLabel,
            icon = icon,
            onClick = onFirst,
            modifier = Modifier.weight(1f),
        )
        WorkoutExportButton(
            label = secondLabel,
            icon = icon,
            onClick = onSecond,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WorkoutExportButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsOutlinedButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ExportButtonIconSize),
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = ExportButtonLabelGap),
        )
    }
}

// Icon geometry, not spacing: matches the icons on the route card's buttons.
private val ExportButtonIconSize: Dp = 18.dp
private val ExportButtonLabelGap: Dp = 6.dp
