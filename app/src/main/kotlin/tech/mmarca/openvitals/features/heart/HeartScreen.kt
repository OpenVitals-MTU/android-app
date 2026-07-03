package tech.mmarca.openvitals.features.heart

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.map
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.rememberMetricDetailSectionOrdering
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.features.vitals.bloodGlucoseContent
import tech.mmarca.openvitals.features.vitals.bloodPressureContent
import tech.mmarca.openvitals.features.vitals.bodyTemperatureContent
import tech.mmarca.openvitals.features.vitals.respiratoryRateContent
import tech.mmarca.openvitals.features.vitals.skinTemperatureContent
import tech.mmarca.openvitals.features.vitals.spO2Content
import tech.mmarca.openvitals.features.vitals.vo2MaxContent
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
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.AVERAGE_HEART_RATE,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun RestingHeartRateScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.RESTING_HEART_RATE,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
fun HrvScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.HRV,
        onSectionEditStateChanged = onSectionEditStateChanged,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun HeartMetricScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: HeartMetric,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit,
) {
    val uiState = viewModel.uiState
    val isLoading by remember(viewModel) { uiState.map { it.isLoading } }
        .collectAsStateWithLifecycle(initialValue = true)
    val state by uiState.collectAsStateWithLifecycle()
    val sectionContext = rememberMetricDetailSectionOrdering(onSectionEditStateChanged)
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate, metric)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.HEART,
        isLoading = isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        MetricDetailScaffold(
            isLoading = isLoading,
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
            sectionListState = sectionContext.listState,
        ) { period ->
            when (metric) {
                HeartMetric.AVERAGE_HEART_RATE -> averageHeartRateContent(
                    state = state,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    chartDaySelection = chartDaySelection,
                    sectionContext = sectionContext,
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
                    sectionContext,
                )
                HeartMetric.HRV -> hrvContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    sectionContext,
                )
                HeartMetric.BLOOD_PRESSURE -> bloodPressureContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    sectionContext,
                    onEditVitalsMeasurement,
                    viewModel::deleteVitalsMeasurementEntry,
                )
                HeartMetric.SPO2 -> spO2Content(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    sectionContext,
                    onEditVitalsMeasurement,
                    viewModel::deleteVitalsMeasurementEntry,
                )
                HeartMetric.VO2_MAX -> vo2MaxContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    sectionContext,
                )
                HeartMetric.RESPIRATORY_RATE -> respiratoryRateContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    sectionContext,
                    onEditVitalsMeasurement,
                    viewModel::deleteVitalsMeasurementEntry,
                )
                HeartMetric.BODY_TEMPERATURE -> bodyTemperatureContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    sectionContext,
                    onEditVitalsMeasurement,
                    viewModel::deleteVitalsMeasurementEntry,
                )
                HeartMetric.BLOOD_GLUCOSE -> bloodGlucoseContent(
                    state = state,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    chartDaySelection = chartDaySelection,
                    sectionContext = sectionContext,
                )
                HeartMetric.SKIN_TEMPERATURE -> skinTemperatureContent(
                    state = state,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    chartDaySelection = chartDaySelection,
                    sectionContext = sectionContext,
                )
            }
        }
    }
}
