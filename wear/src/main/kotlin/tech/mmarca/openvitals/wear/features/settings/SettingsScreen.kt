package tech.mmarca.openvitals.wear.features.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.SettingsUiState
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onPhoneClick: () -> Unit,
    onSensorAccessClick: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(stringResource(R.string.settings_title))
                }
            }
            item {
                Button(
                    onClick = onPhoneClick,
                    label = { Text(stringResource(R.string.settings_phone)) },
                    secondaryLabel = {
                        Text(
                            stringResource(
                                if (state.phoneConnected) {
                                    R.string.settings_phone_connected
                                } else {
                                    R.string.settings_phone_not_connected
                                },
                            ),
                        )
                    },
                    icon = { Icon(Icons.Outlined.Smartphone, contentDescription = null) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                Button(
                    onClick = onSensorAccessClick,
                    label = { Text(stringResource(R.string.settings_sensor_access)) },
                    secondaryLabel = {
                        Text(
                            stringResource(
                                if (state.sensorAccessGranted) {
                                    R.string.settings_sensor_access_granted
                                } else {
                                    R.string.settings_sensor_access_missing
                                },
                            ),
                        )
                    },
                    icon = { Icon(Icons.Outlined.Sensors, contentDescription = null) },
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
private fun SettingsScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            SettingsScreen(SampleData.settings, onPhoneClick = {}, onSensorAccessClick = {})
        }
    }
}
