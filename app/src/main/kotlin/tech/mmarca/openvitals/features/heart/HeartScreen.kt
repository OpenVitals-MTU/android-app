package tech.mmarca.openvitals.features.heart

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection

enum class HeartMetric {
    AVERAGE_HEART_RATE,
    RESTING_HEART_RATE,
    HRV,
    BLOOD_PRESSURE,
    SPO2,
    VO2_MAX,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    BLOOD_GLUCOSE,
    SKIN_TEMPERATURE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AverageHeartRateScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.AVERAGE_HEART_RATE,
    )
}

@Composable
fun RestingHeartRateScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.RESTING_HEART_RATE,
    )
}

@Composable
fun HrvScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.HRV,
    )
}

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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HeartMetricScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: HeartMetric,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate, metric)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.HEART,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            screenError = state.error,
            onRefresh = viewModel::load,
            onSelectRange = viewModel::selectRange,
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
        ) { period ->
        when (metric) {
            HeartMetric.AVERAGE_HEART_RATE -> averageHeartRateContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                onDecreaseHighHeartRateThreshold = viewModel::decreaseHighHeartRateThreshold,
                onIncreaseHighHeartRateThreshold = viewModel::increaseHighHeartRateThreshold,
                onDecreaseLowHeartRateThreshold = viewModel::decreaseLowHeartRateThreshold,
                onIncreaseLowHeartRateThreshold = viewModel::increaseLowHeartRateThreshold,
            )
            HeartMetric.RESTING_HEART_RATE -> restingHeartRateContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
            )
            HeartMetric.HRV -> hrvContent(state, period, unitFormatter, dateTimeFormatterProvider, chartDaySelection)
            HeartMetric.BLOOD_PRESSURE -> bloodPressureContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                onEditVitalsMeasurement,
                viewModel::deleteVitalsMeasurementEntry,
            )
            HeartMetric.SPO2 -> spO2Content(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                onEditVitalsMeasurement,
                viewModel::deleteVitalsMeasurementEntry,
            )
            HeartMetric.VO2_MAX -> vo2MaxContent(state, period, unitFormatter, dateTimeFormatterProvider)
            HeartMetric.RESPIRATORY_RATE -> respiratoryRateContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                onEditVitalsMeasurement,
                viewModel::deleteVitalsMeasurementEntry,
            )
            HeartMetric.BODY_TEMPERATURE -> bodyTemperatureContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                onEditVitalsMeasurement,
                viewModel::deleteVitalsMeasurementEntry,
            )
            HeartMetric.BLOOD_GLUCOSE -> bloodGlucoseContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
            )
            HeartMetric.SKIN_TEMPERATURE -> skinTemperatureContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
            )
        }
    }
    }
}
