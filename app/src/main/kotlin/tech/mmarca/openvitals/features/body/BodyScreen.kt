package tech.mmarca.openvitals.features.body

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection

enum class BodyMetric {
    WEIGHT,
    HEIGHT,
    BMI,
    BODY_FAT,
    LEAN_MASS,
    BMR,
    BONE_MASS,
    BODY_WATER_MASS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        key = "body",
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.BODY,
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
            bodyContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                onEditBodyMeasurement = onEditBodyMeasurement,
                onDeleteBodyMeasurement = viewModel::deleteBodyMeasurementEntry,
            )
        }
    }
}

@Composable
fun WeightScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.WEIGHT,
        onEditBodyMeasurement = onEditBodyMeasurement,
    )
}

@Composable
fun HeightScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.HEIGHT,
        onEditBodyMeasurement = onEditBodyMeasurement,
    )
}

@Composable
fun BmiScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.BMI,
        onEditBodyMeasurement = onEditBodyMeasurement,
    )
}

@Composable
fun BodyFatScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.BODY_FAT,
        onEditBodyMeasurement = onEditBodyMeasurement,
    )
}

@Composable
fun LeanMassScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.LEAN_MASS,
    )
}

@Composable
fun BmrScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.BMR,
    )
}

@Composable
fun BoneMassScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.BONE_MASS,
    )
}

@Composable
fun BodyWaterMassScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.BODY_WATER_MASS,
    )
}
