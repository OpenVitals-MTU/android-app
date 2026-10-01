package tech.mmarca.openvitals.wear.features.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.VitalsUiState
import tech.mmarca.openvitals.wear.ui.components.MetricTile
import tech.mmarca.openvitals.wear.ui.components.formatCount
import tech.mmarca.openvitals.wear.ui.components.formatKilometers
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.wear.ui.theme.DistanceColor
import tech.mmarca.openvitals.wear.ui.theme.HeartColor
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme
import tech.mmarca.openvitals.wear.ui.theme.StepsColor

@Composable
fun DashboardScreen(
    state: VitalsUiState,
    onOpenHeart: () -> Unit,
    onOpenActivity: () -> Unit,
    onStartActivity: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = onStartActivity) {
                Text(stringResource(R.string.dashboard_start_activity))
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(stringResource(R.string.dashboard_today))
                }
            }
            item {
                MetricTile(
                    title = stringResource(R.string.metric_steps),
                    value = state.steps?.let(::formatCount),
                    unit = stringResource(R.string.unit_steps),
                    subtitle = stringResource(R.string.steps_goal, formatCount(state.stepGoal)),
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    accentColor = StepsColor,
                    onClick = onOpenActivity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                MetricTile(
                    title = stringResource(R.string.metric_heart_rate),
                    value = state.heartRateBpm?.toString(),
                    unit = stringResource(R.string.unit_bpm),
                    icon = Icons.Outlined.Favorite,
                    accentColor = HeartColor,
                    onClick = onOpenHeart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                MetricTile(
                    title = stringResource(R.string.metric_distance),
                    value = state.distanceMeters?.let(::formatKilometers),
                    unit = stringResource(R.string.unit_km),
                    icon = Icons.Outlined.Straighten,
                    accentColor = DistanceColor,
                    onClick = onOpenActivity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                MetricTile(
                    title = stringResource(R.string.metric_active_calories),
                    value = state.activeCalories?.let(::formatCount),
                    unit = stringResource(R.string.unit_kcal),
                    icon = Icons.Outlined.LocalFireDepartment,
                    accentColor = ActiveCaloriesColor,
                    onClick = onOpenActivity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                Button(
                    onClick = onOpenSettings,
                    label = { Text(stringResource(R.string.settings_title)) },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
        }
    }
}

@WearPreviews
@Composable
private fun DashboardScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            DashboardScreen(SampleData.vitals, {}, {}, {}, {})
        }
    }
}

@WearPreviews
@Composable
private fun DashboardScreenEmptyPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            DashboardScreen(VitalsUiState(), {}, {}, {}, {})
        }
    }
}
