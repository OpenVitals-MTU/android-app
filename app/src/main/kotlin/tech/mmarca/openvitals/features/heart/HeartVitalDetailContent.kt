package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import java.time.ZoneId

internal fun LazyListScope.spO2Content(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    if (state.spO2.isNotEmpty()) {
        val sorted = state.spO2.sortedBy { it.time }
        item {
            VitalsLineChart(
                title = stringResource(R.string.metric_oxygen_saturation),
                points = rawVitalsPoints(
                    entries = sorted,
                    time = { it.time },
                    value = { it.percent },
                ),
                selectedRange = state.selectedRange,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = oxygenColor,
                summary = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                    stringResource(R.string.summary_value_avg, unitFormatter.percent(state.spO2.map { it.percent }.average()).text)
                }",
                valueFormatter = { unitFormatter.percent(it).text },
                modifier = metricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            heartEntryRows(
                entries = state.spO2.filter { it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate },
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
        heartRawDataConfidence(
            period = period,
            entries = state.spO2,
            source = { it.source },
            time = { it.time },
            accentColor = oxygenColor,
        )
        oxygenSaturationContextCard(state.spO2.maxByOrNull { it.time })
        spO2Statistics(
            entries = state.spO2,
            previousEntries = state.previousSpO2,
            baselineEntries = state.baselineSpO2,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.spO2,
            value = { unitFormatter.percent(it.percent).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
            onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.SPO2, it.id) },
            onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.SPO2, it.id) },
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
) {
    val latest = state.latestVo2Max
    if (latest != null) {
        if (state.vo2Max.size > 1) {
            item {
                Vo2MaxChart(
                    entries = state.vo2Max,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
        }
        item {
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
        }
        heartRawDataConfidence(
            period = period,
            entries = state.vo2Max,
            source = { it.source },
            time = { it.time },
            accentColor = vo2Color,
        )
        vo2MaxStatistics(
            entries = state.vo2Max,
            previousEntries = state.previousVo2Max,
            baselineEntries = state.baselineVo2Max,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.vo2Max,
            value = { unitFormatter.vo2Max(it.vo2MaxMlPerKgPerMin).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
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
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    if (state.respiratoryRate.isNotEmpty()) {
        item {
            RespiratoryRateChart(
                entries = state.respiratoryRate,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }
        if (state.selectedRange == TimeRange.DAY) {
            item {
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
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            heartEntryRows(
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
        heartRawDataConfidence(
            period = period,
            entries = state.respiratoryRate,
            source = { it.source },
            time = { it.time },
            accentColor = respiratoryColor,
        )
        respiratoryRateContextCard(state.respiratoryRate.map { it.breathsPerMinute }.average())
        respiratoryRateStatistics(
            entries = state.respiratoryRate,
            previousEntries = state.previousRespiratoryRate,
            baselineEntries = state.baselineRespiratoryRate,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        if (state.selectedRange != TimeRange.DAY) {
            item {
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
        }
        heartEntryRows(
            entries = state.respiratoryRate,
            value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
            onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
            onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
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
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    if (state.bodyTemperature.isNotEmpty()) {
        item {
            BodyTemperatureChart(
                entries = state.bodyTemperature,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
            )
        }
        item {
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
        }
        heartRawDataConfidence(
            period = period,
            entries = state.bodyTemperature,
            source = { it.source },
            time = { it.time },
            accentColor = temperatureColor,
        )
        bodyTemperatureContextCard(state.bodyTemperature.maxByOrNull { it.time })
        bodyTemperatureStatistics(
            entries = state.bodyTemperature,
            previousEntries = state.previousBodyTemperature,
            baselineEntries = state.baselineBodyTemperature,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.bodyTemperature,
            value = { unitFormatter.temperature(it.temperatureCelsius).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
            onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.BODY_TEMPERATURE, it.id) },
            onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.BODY_TEMPERATURE, it.id) },
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
) {
    if (state.bloodGlucose.isNotEmpty()) {
        item {
            BloodGlucoseChart(
                entries = state.bloodGlucose,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            heartEntryRows(
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
        heartRawDataConfidence(
            period = period,
            entries = state.bloodGlucose,
            source = { it.source },
            time = { it.time },
            accentColor = glucoseColor,
        )
        bloodGlucoseStatistics(
            entries = state.bloodGlucose,
            previousEntries = state.previousBloodGlucose,
            baselineEntries = state.baselineBloodGlucose,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.bloodGlucose,
            value = { unitFormatter.bloodGlucose(it.millimolesPerLiter).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
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
) {
    if (state.skinTemperature.isNotEmpty()) {
        item {
            SkinTemperatureChart(
                entries = state.skinTemperature,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            heartEntryRows(
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
        heartRawDataConfidence(
            period = period,
            entries = state.skinTemperature,
            source = { it.source },
            time = { it.time },
            accentColor = temperatureColor,
        )
        skinTemperatureStatistics(
            entries = state.skinTemperature,
            previousEntries = state.previousSkinTemperature,
            baselineEntries = state.baselineSkinTemperature,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.skinTemperature,
            value = { it.skinTemperatureValue(unitFormatter) },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
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
