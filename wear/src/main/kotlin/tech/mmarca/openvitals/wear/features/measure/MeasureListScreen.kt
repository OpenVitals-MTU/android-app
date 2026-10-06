package tech.mmarca.openvitals.wear.features.measure

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.health.WearCapabilities
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme

/**
 * The spot measurements this watch supports. A measurement without a sensor
 * is not listed at all, so nothing here can be tapped only to fail.
 */
@Composable
fun MeasureListScreen(
    capabilities: WearCapabilities,
    onMeasure: (Measurement) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val supported = Measurement.entries.filter { it in capabilities.measurements }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(stringResource(R.string.measure_title))
                }
            }
            when {
                !capabilities.probed -> item { CircularProgressIndicator() }
                supported.isEmpty() -> item {
                    Text(
                        text = stringResource(R.string.measure_none_supported),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    )
                }
                else -> items(supported) { measurement ->
                    Button(
                        onClick = { onMeasure(measurement) },
                        label = { Text(stringResource(measurement.label)) },
                        secondaryLabel = { Text(stringResource(measurement.description)) },
                        icon = {
                            Icon(measurement.icon, contentDescription = null, tint = measurement.accentColor)
                        },
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
}

@WearPreviews
@Composable
private fun MeasureListScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            MeasureListScreen(SampleData.capabilities, onMeasure = {})
        }
    }
}

@WearPreviews
@Composable
private fun MeasureListScreenNoneSupportedPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            MeasureListScreen(WearCapabilities(probed = true), onMeasure = {})
        }
    }
}
