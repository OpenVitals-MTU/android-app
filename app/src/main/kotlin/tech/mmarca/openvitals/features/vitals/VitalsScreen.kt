package tech.mmarca.openvitals.features.vitals

import androidx.compose.runtime.Composable
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.features.heart.HeartMetric
import tech.mmarca.openvitals.features.heart.HeartMetricScreen
import tech.mmarca.openvitals.features.heart.HeartViewModel

@Composable
fun BloodPressureScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.BLOOD_PRESSURE,
        onEditVitalsMeasurement = onEditVitalsMeasurement,
    )
}

@Composable
fun SpO2Screen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.SPO2,
        onEditVitalsMeasurement = onEditVitalsMeasurement,
    )
}

@Composable
fun Vo2MaxScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.VO2_MAX,
    )
}

@Composable
fun RespiratoryRateScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.RESPIRATORY_RATE,
        onEditVitalsMeasurement = onEditVitalsMeasurement,
    )
}

@Composable
fun BodyTemperatureScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.BODY_TEMPERATURE,
        onEditVitalsMeasurement = onEditVitalsMeasurement,
    )
}

@Composable
fun BloodGlucoseScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.BLOOD_GLUCOSE,
    )
}

@Composable
fun SkinTemperatureScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.SKIN_TEMPERATURE,
    )
}
