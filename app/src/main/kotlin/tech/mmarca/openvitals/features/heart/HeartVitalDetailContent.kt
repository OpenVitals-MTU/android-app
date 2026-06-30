package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricLineChart
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import java.time.ZoneId
import kotlin.math.roundToInt

internal fun LazyListScope.spO2Content(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    val display = state.display.metric
    if (display.hasVitalsEntries) {
        val sorted = state.spO2.sortedBy { it.time }
        renderChartMetricSections(
            sectionContext = sectionContext,
            selectedRange = state.selectedRange,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            periodChart = {
                MetricLineChart(
                    title = stringResource(R.string.metric_oxygen_saturation),
                    entries = sorted,
                    selectedRange = state.selectedRange,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    accentColor = oxygenColor,
                    summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                        stringResource(R.string.summary_value_avg, unitFormatter.percent(state.spO2.map { it.percent }.average()).text)
                    }",
                    time = { it.time },
                    value = { it.percent },
                    valueFormatter = { unitFormatter.percent(it).text },
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                )
            },
            selectedDayEntries = chartDaySelection.selectedDate?.let { selectedDate ->
                {
                    HeartEntryListContent(
                        entries = state.spO2.filter {
                            it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                        },
                        value = { unitFormatter.percent(it.percent).text },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        titleDate = selectedDate,
                        editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                        onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.SPO2, it.id) },
                        onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.SPO2, it.id) },
                    )
                }
            },
            dataConfidence = {
                HeartRawDataConfidenceContent(
                    period = period,
                    entries = state.spO2,
                    source = { it.source },
                    time = { it.time },
                    accentColor = oxygenColor,
                )
            },
            contextInsight = {
                OxygenSaturationContextCardContent(state.spO2.maxByOrNull { it.time })
            },
            statistics = {
                SpO2StatisticsContent(
                    entries = state.spO2,
                    previousEntries = state.previousSpO2,
                    baselineEntries = state.baselineSpO2,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                )
            },
            entries = {
                HeartEntryListContent(
                    entries = state.spO2,
                    value = { unitFormatter.percent(it.percent).text },
                    source = { it.source },
                    time = { it.time },
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                    onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.SPO2, it.id) },
                    onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.SPO2, it.id) },
                )
            },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_spo2,
            messageRes = R.string.message_no_oxygen,
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = oxygenColor,
        )
    }
}

internal fun LazyListScope.vo2MaxContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    sectionContext: MetricDetailSectionContext,
) {
    val display = state.display.metric
    val latest = state.latestVo2Max
    if (display.hasData && latest != null) {
        val sorted = state.vo2Max.sortedBy { it.time }
        renderChartMetricSections(
            sectionContext = sectionContext,
            selectedRange = state.selectedRange,
            period = period,
            selectedDate = null,
            periodChart = if (state.vo2Max.size > 1) {
                {
                    MetricLineChart(
                        title = stringResource(R.string.metric_vo2_max),
                        entries = sorted,
                        selectedRange = state.selectedRange,
                        period = period,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = vo2Color,
                        summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                            stringResource(R.string.summary_readings, unitFormatter.count(sorted.size))
                        }",
                        modifier = metricModifier(),
                        time = { it.time },
                        value = { it.vo2MaxMlPerKgPerMin },
                        valueFormatter = { unitFormatter.vo2Max(it).text },
                    )
                }
            } else {
                null
            },
            highlightCard = {
                val value = unitFormatter.vo2Max(latest.vo2MaxMlPerKgPerMin)
                MetricCard(
                    title = stringResource(R.string.metric_vo2_max),
                    value = value.value,
                    unit = value.unit,
                    icon = Icons.Outlined.Speed,
                    accentColor = vo2Color,
                    source = latest.source,
                    modifier = metricModifier(),
                )
            },
            dataConfidence = {
                HeartRawDataConfidenceContent(
                    period = period,
                    entries = state.vo2Max,
                    source = { it.source },
                    time = { it.time },
                    accentColor = vo2Color,
                )
            },
            statistics = {
                Vo2MaxStatisticsContent(
                    entries = state.vo2Max,
                    previousEntries = state.previousVo2Max,
                    baselineEntries = state.baselineVo2Max,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                )
            },
            entries = {
                HeartEntryListContent(
                    entries = state.vo2Max,
                    value = { unitFormatter.vo2Max(it.vo2MaxMlPerKgPerMin).text },
                    source = { it.source },
                    time = { it.time },
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_vo2_max,
            messageRes = R.string.message_no_vo2_max,
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = vo2Color,
        )
    }
}

internal fun LazyListScope.respiratoryRateContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    val display = state.display.metric
    if (display.hasVitalsEntries) {
        renderChartMetricSections(
            sectionContext = sectionContext,
            selectedRange = state.selectedRange,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            periodChart = {
                MetricLineChart(
                    title = stringResource(R.string.metric_respiratory_rate),
                    series = respiratoryRateSeries(
                        entries = state.respiratoryRate,
                        selectedRange = state.selectedRange,
                        metricLabel = stringResource(R.string.metric_respiratory_rate),
                        averageLabel = stringResource(R.string.summary_average),
                        lowestLabel = stringResource(R.string.stat_lowest),
                        highestLabel = stringResource(R.string.stat_highest),
                    ),
                    selectedRange = state.selectedRange,
                    period = period,
                    accentColor = respiratoryColor,
                    summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                        stringResource(
                            R.string.summary_value_avg,
                            unitFormatter.respiratoryRate(
                                respiratoryRateAverage(respiratoryRateBuckets(state.respiratoryRate, state.selectedRange, period)),
                            ).text,
                        )
                    }",
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                    valueFormatter = { unitFormatter.respiratoryRate(it).text },
                )
            },
            selectedDayEntries = chartDaySelection.selectedDate?.let { selectedDate ->
                {
                    HeartEntryListContent(
                        entries = state.respiratoryRate.filter {
                            it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                        },
                        value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        titleDate = selectedDate,
                        editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                        onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
                        onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
                    )
                }
            },
            dataConfidence = {
                HeartRawDataConfidenceContent(
                    period = period,
                    entries = state.respiratoryRate,
                    source = { it.source },
                    time = { it.time },
                    accentColor = respiratoryColor,
                )
            },
            contextInsight = {
                RespiratoryRateContextCardContent(state.respiratoryRate.map { it.breathsPerMinute }.average())
            },
            statistics = {
                RespiratoryRateStatisticsContent(
                    entries = state.respiratoryRate,
                    previousEntries = state.previousRespiratoryRate,
                    baselineEntries = state.baselineRespiratoryRate,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                )
            },
            entries = {
                Column {
                    if (state.selectedRange == TimeRange.DAY) {
                        SimpleVitalsList(
                            title = stringResource(R.string.vitals_respiratory_rate_readings),
                            entries = state.respiratoryRate,
                            value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
                            source = { it.source },
                            time = { it.time },
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = metricModifier(),
                            editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                            onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
                            onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
                        )
                    }
                    if (state.selectedRange != TimeRange.DAY) {
                        PaginatedEntryList(
                            title = stringResource(R.string.section_respiratory_rate_daily_breakdown),
                            entries = respiratoryRateDaySummaries(state.respiratoryRate).sortedByDescending { it.date },
                        ) { summary, rowModifier ->
                            RespiratoryRateDayRow(
                                summary = summary,
                                unitFormatter = unitFormatter,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                modifier = rowModifier,
                            )
                        }
                    }
                    HeartEntryListContent(
                        entries = state.respiratoryRate,
                        value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                        onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
                        onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
                    )
                }
            },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_respiratory_rate,
            messageRes = R.string.message_no_readings_period,
            icon = Icons.Outlined.Favorite,
            accentColor = respiratoryColor,
        )
    }
}

internal fun LazyListScope.bodyTemperatureContent(
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
        val sorted = state.bodyTemperature.sortedBy { it.time }
        renderChartMetricSections(
            sectionContext = sectionContext,
            selectedRange = state.selectedRange,
            period = period,
            selectedDate = null,
            periodChart = {
                MetricLineChart(
                    title = stringResource(R.string.metric_body_temp),
                    entries = sorted,
                    selectedRange = state.selectedRange,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    accentColor = temperatureColor,
                    summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                        stringResource(R.string.summary_readings, unitFormatter.count(sorted.size))
                    }",
                    modifier = metricModifier(),
                    time = { it.time },
                    value = { it.temperatureCelsius },
                    valueFormatter = { unitFormatter.temperature(it).text },
                )
            },
            dataConfidence = {
                HeartRawDataConfidenceContent(
                    period = period,
                    entries = state.bodyTemperature,
                    source = { it.source },
                    time = { it.time },
                    accentColor = temperatureColor,
                )
            },
            contextInsight = {
                BodyTemperatureContextCardContent(state.bodyTemperature.maxByOrNull { it.time })
            },
            statistics = {
                BodyTemperatureStatisticsContent(
                    entries = state.bodyTemperature,
                    previousEntries = state.previousBodyTemperature,
                    baselineEntries = state.baselineBodyTemperature,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                )
            },
            entries = {
                Column {
                    SimpleVitalsList(
                        title = stringResource(R.string.vitals_body_temperature_readings),
                        entries = state.bodyTemperature,
                        value = { unitFormatter.temperature(it.temperatureCelsius).text },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = metricModifier(),
                        editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                        onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.BODY_TEMPERATURE, it.id) },
                        onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.BODY_TEMPERATURE, it.id) },
                    )
                    HeartEntryListContent(
                        entries = state.bodyTemperature,
                        value = { unitFormatter.temperature(it.temperatureCelsius).text },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                        onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.BODY_TEMPERATURE, it.id) },
                        onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.BODY_TEMPERATURE, it.id) },
                    )
                }
            },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_body_temp,
            messageRes = R.string.message_no_readings_period,
            icon = Icons.Outlined.DeviceThermostat,
            accentColor = temperatureColor,
        )
    }
}

internal fun LazyListScope.bloodGlucoseContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
) {
    val display = state.display.metric
    if (display.hasVitalsEntries) {
        val sorted = state.bloodGlucose.sortedBy { it.time }
        renderChartMetricSections(
            sectionContext = sectionContext,
            selectedRange = state.selectedRange,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            periodChart = {
                MetricLineChart(
                    title = stringResource(R.string.metric_blood_glucose),
                    entries = sorted,
                    selectedRange = state.selectedRange,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    accentColor = glucoseColor,
                    summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                        stringResource(
                            R.string.summary_value_avg,
                            unitFormatter.bloodGlucose(sorted.map { it.millimolesPerLiter }.average()).text,
                        )
                    }",
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                    time = { it.time },
                    value = { it.millimolesPerLiter },
                    valueFormatter = { unitFormatter.bloodGlucose(it).text },
                )
            },
            selectedDayEntries = chartDaySelection.selectedDate?.let { selectedDate ->
                {
                    HeartEntryListContent(
                        entries = state.bloodGlucose.filter {
                            it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                        },
                        value = { unitFormatter.bloodGlucose(it.millimolesPerLiter).text },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        titleDate = selectedDate,
                    )
                }
            },
            dataConfidence = {
                HeartRawDataConfidenceContent(
                    period = period,
                    entries = state.bloodGlucose,
                    source = { it.source },
                    time = { it.time },
                    accentColor = glucoseColor,
                )
            },
            statistics = {
                BloodGlucoseStatisticsContent(
                    entries = state.bloodGlucose,
                    previousEntries = state.previousBloodGlucose,
                    baselineEntries = state.baselineBloodGlucose,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                )
            },
            entries = {
                HeartEntryListContent(
                    entries = state.bloodGlucose,
                    value = { unitFormatter.bloodGlucose(it.millimolesPerLiter).text },
                    source = { it.source },
                    time = { it.time },
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_blood_glucose,
            messageRes = R.string.message_no_blood_glucose,
            icon = Icons.Outlined.Favorite,
            accentColor = glucoseColor,
        )
    }
}

internal fun LazyListScope.skinTemperatureContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
) {
    val display = state.display.metric
    if (display.hasVitalsEntries) {
        val chartEntries = state.skinTemperature
            .filter { it.averageDeltaCelsius != null }
            .sortedBy { it.time }
        renderChartMetricSections(
            sectionContext = sectionContext,
            selectedRange = state.selectedRange,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            periodChart = if (chartEntries.isNotEmpty()) {
                {
                    MetricLineChart(
                        title = stringResource(R.string.metric_skin_temperature),
                        entries = chartEntries,
                        selectedRange = state.selectedRange,
                        period = period,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = temperatureColor,
                        summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                            stringResource(
                                R.string.summary_value_avg,
                                unitFormatter.temperatureDelta(chartEntries.mapNotNull { it.averageDeltaCelsius }.average()).text,
                            )
                        }",
                        modifier = metricModifier(),
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                        time = { it.time },
                        value = { it.averageDeltaCelsius ?: 0.0 },
                        valueFormatter = { unitFormatter.temperatureDelta(it).text },
                    )
                }
            } else {
                null
            },
            selectedDayEntries = chartDaySelection.selectedDate?.let { selectedDate ->
                {
                    HeartEntryListContent(
                        entries = state.skinTemperature.filter {
                            it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                        },
                        value = { it.skinTemperatureValue(unitFormatter) },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        titleDate = selectedDate,
                    )
                }
            },
            dataConfidence = {
                HeartRawDataConfidenceContent(
                    period = period,
                    entries = state.skinTemperature,
                    source = { it.source },
                    time = { it.time },
                    accentColor = temperatureColor,
                )
            },
            statistics = {
                SkinTemperatureStatisticsContent(
                    entries = state.skinTemperature,
                    previousEntries = state.previousSkinTemperature,
                    baselineEntries = state.baselineSkinTemperature,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                )
            },
            entries = {
                HeartEntryListContent(
                    entries = state.skinTemperature,
                    value = { it.skinTemperatureValue(unitFormatter) },
                    source = { it.source },
                    time = { it.time },
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_skin_temperature,
            messageRes = R.string.message_no_skin_temperature,
            icon = Icons.Outlined.DeviceThermostat,
            accentColor = temperatureColor,
        )
    }
}
