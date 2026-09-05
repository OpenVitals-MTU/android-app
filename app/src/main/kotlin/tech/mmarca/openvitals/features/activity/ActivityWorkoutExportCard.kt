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
import androidx.compose.material3.HorizontalDivider
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
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * Every way a workout leaves the app. Route formats only with a route;
 * metric formats on every workout. The description says which carry the route.
 */
@Composable
internal fun WorkoutExportCard(
    onSaveAsTcx: () -> Unit,
    onSaveAsCsv: () -> Unit,
    onSaveAsFit: () -> Unit,
    onShareAsTcx: () -> Unit,
    onShareAsCsv: () -> Unit,
    onShareAsFit: () -> Unit,
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
            WorkoutFormatRow(
                saveLabel = stringResource(R.string.activity_route_export_gpx),
                shareLabel = stringResource(R.string.activity_route_share_gpx),
                onSave = checkNotNull(onSaveRouteAsGpx),
                onShare = checkNotNull(onShareRouteAsGpx),
            )
            ExportRowDivider()
            WorkoutFormatRow(
                saveLabel = stringResource(R.string.activity_route_export_kmz),
                shareLabel = stringResource(R.string.activity_route_share_kmz),
                onSave = checkNotNull(onSaveRouteAsKmz),
                onShare = checkNotNull(onShareRouteAsKmz),
            )
            ExportRowDivider()
        }
        WorkoutFormatRow(
            saveLabel = stringResource(R.string.activity_workout_export_tcx),
            shareLabel = stringResource(R.string.activity_workout_share_tcx),
            onSave = onSaveAsTcx,
            onShare = onShareAsTcx,
        )
        ExportRowDivider()
        WorkoutFormatRow(
            saveLabel = stringResource(R.string.activity_workout_export_csv),
            shareLabel = stringResource(R.string.activity_workout_share_csv),
            onSave = onSaveAsCsv,
            onShare = onShareAsCsv,
        )
        ExportRowDivider()
        WorkoutFormatRow(
            saveLabel = stringResource(R.string.activity_workout_export_fit),
            shareLabel = stringResource(R.string.activity_workout_share_fit),
            onSave = onSaveAsFit,
            onShare = onShareAsFit,
        )
    }
}

/** A quiet rule between format rows — the subtle between-item tint, tight padding. */
@Composable
private fun ExportRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = Spacing.xs),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Emphasis.fill),
    )
}

/** One format per row, save on the left and share on the right. */
@Composable
private fun WorkoutFormatRow(
    saveLabel: String,
    shareLabel: String,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        WorkoutExportButton(
            label = saveLabel,
            icon = Icons.Outlined.FileDownload,
            onClick = onSave,
            modifier = Modifier.weight(1f),
        )
        WorkoutExportButton(
            label = shareLabel,
            icon = Icons.Outlined.Share,
            onClick = onShare,
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
