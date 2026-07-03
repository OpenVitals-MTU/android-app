package tech.mmarca.openvitals.features.vitals

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.features.heart.BloodPressureContextCardContent
import tech.mmarca.openvitals.features.heart.BloodPressureStatisticsContent
import tech.mmarca.openvitals.features.heart.HeartEntryListContent
import tech.mmarca.openvitals.features.heart.HeartRawDataConfidenceContent
import tech.mmarca.openvitals.features.heart.HeartUiState
import tech.mmarca.openvitals.features.heart.metricModifier
import tech.mmarca.openvitals.features.heart.noHeartMetricData
import tech.mmarca.openvitals.features.heart.renderChartMetricSections
import tech.mmarca.openvitals.ui.components.MetricLineChart
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.VitalsColor
import kotlin.math.roundToInt

internal fun LazyListScope.bloodPressureContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    sectionContext: MetricDetailSectionContext,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    val display = state.display.metric
    if (display.hasVitalsEntries) {
        val sortedBloodPressure = state.bloodPressure.sortedBy { it.time }
        renderChartMetricSections(
            sectionContext = sectionContext,
            selectedRange = state.selectedRange,
            period = period,
            selectedDate = null,
            periodChart = {
                MetricLineChart(
                    title = stringResource(R.string.metric_blood_pressure),
                    series = bloodPressureSeries(
                        entries = sortedBloodPressure,
                        selectedRange = state.selectedRange,
                        systolicLabel = stringResource(R.string.vitals_entry_systolic_label),
                        diastolicLabel = stringResource(R.string.vitals_entry_diastolic_label),
                    ),
                    selectedRange = state.selectedRange,
                    period = period,
                    accentColor = VitalsColor,
                    summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                        stringResource(R.string.summary_readings, unitFormatter.count(display.vitalsSampleCount))
                    }",
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    valueFormatter = { "${it.roundToInt()} mmHg" },
                )
            },
            dataConfidence = {
                HeartRawDataConfidenceContent(
                    period = period,
                    entries = state.bloodPressure,
                    source = { it.source },
                    time = { it.time },
                    accentColor = VitalsColor,
                )
            },
            contextInsight = {
                BloodPressureContextCardContent(state.bloodPressure.maxByOrNull { it.time })
            },
            statistics = {
                BloodPressureStatisticsContent(
                    entries = state.bloodPressure,
                    previousEntries = state.previousBloodPressure,
                    baselineEntries = state.baselineBloodPressure,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                )
            },
            entries = {
                HeartEntryListContent(
                    entries = state.bloodPressure,
                    value = { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg).text },
                    source = { it.source },
                    time = { it.time },
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                    onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.BLOOD_PRESSURE, it.id) },
                    onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.BLOOD_PRESSURE, it.id) },
                )
            },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_blood_pressure,
            messageRes = R.string.message_no_blood_pressure,
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
        )
    }
}
