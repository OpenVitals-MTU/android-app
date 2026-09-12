package tech.mmarca.openvitals.features.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.geo.HgtResolution
import tech.mmarca.openvitals.core.geo.HgtTileKey
import tech.mmarca.openvitals.features.activity.elevation.ElevationTile
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

private val LeadingIconOpticalOffset = 2.dp
private val LeadingIconSize = 20.dp
private val ButtonIconSize = 18.dp
private val ButtonIconGap = 6.dp

/** The elevation-correction toggle and the imported SRTM tiles. Mirrors [OfflineMapsCard]. */
@Composable
internal fun ElevationCorrectionCard(
    enabled: Boolean,
    tiles: List<ElevationTile>,
    isImporting: Boolean,
    result: ElevationTile?,
    error: String?,
    onEnabledChange: (Boolean) -> Unit,
    onImport: () -> Unit,
    onDeleteTile: (HgtTileKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val helpUrl = stringResource(R.string.settings_elevation_help_url)
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(LayoutMetrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.Terrain,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = LeadingIconOpticalOffset)
                        .size(LeadingIconSize),
                )
                Column(
                    modifier = Modifier
                        .padding(start = Spacing.md)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.settings_elevation_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_elevation_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_elevation_toggle_title),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_elevation_toggle_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = LeadingIconOpticalOffset),
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.padding(start = Spacing.md),
                )
            }

            Text(
                text = stringResource(R.string.settings_elevation_help_prompt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OpenVitalsTextButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(helpUrl)))
                    }
                },
            ) {
                Text(stringResource(R.string.settings_elevation_help_link))
            }

            if (tiles.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_elevation_tiles_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                tiles.forEach { tile ->
                    ElevationTileRow(
                        tile = tile,
                        onDelete = { onDeleteTile(tile.key) },
                    )
                }
            }

            result?.let { imported ->
                Text(
                    text = stringResource(
                        R.string.settings_elevation_tile_import_result,
                        imported.displayName,
                        formatOfflineMapSize(imported.sizeBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.settings_elevation_tile_import_error, error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (isImporting) {
                // A tile is at most 26 MB: the copy is too short for a percentage to mean anything.
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            OpenVitalsOutlinedButton(
                onClick = onImport,
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonIconSize),
                )
                Spacer(Modifier.widthIn(min = ButtonIconGap))
                Text(
                    if (isImporting) {
                        stringResource(R.string.settings_elevation_tile_importing)
                    } else {
                        stringResource(R.string.settings_elevation_tile_import_action)
                    },
                )
            }
        }
    }
}

@Composable
private fun ElevationTileRow(
    tile: ElevationTile,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tile.displayName,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.settings_elevation_tile_detail,
                    stringResource(tile.resolution.settingsLabelRes),
                    formatOfflineMapSize(tile.sizeBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OpenVitalsTextButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.action_delete),
                modifier = Modifier.size(ButtonIconSize),
            )
        }
    }
}

private val HgtResolution.settingsLabelRes: Int
    get() = when (this) {
        HgtResolution.ONE_ARC_SECOND -> R.string.settings_elevation_tile_resolution_1
        HgtResolution.THREE_ARC_SECOND -> R.string.settings_elevation_tile_resolution_3
    }
