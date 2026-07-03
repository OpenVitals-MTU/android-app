package tech.mmarca.openvitals.features.vitals

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.features.heart.HeartUiState
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricLineChart
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.ZoneId
import kotlin.math.roundToInt

fun LazyListScope.HeartVitalsContent(
    state: HeartUiState,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (!state.hasVitalsData && !state.isLoading) {
        item {
            MetricCardPlaceholder(
                title = stringResource(R.string.section_vitals),
                icon = Icons.Outlined.Favorite,
                accentColor = VitalsColor,
                message = stringResource(R.string.message_no_vitals_period),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (state.bloodPressure.isNotEmpty() || state.spO2.isNotEmpty() || state.vo2Max.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.section_cardiovascular)) }
        item {
            VitalsSummaryRow(
                first = state.latestBloodPressure?.let {
                    val value = unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg)
                    SummaryMetric(stringResource(R.string.metric_blood_pressure), value.value, value.unit, Icons.Outlined.Favorite, VitalsColor, it.source)
                },
                second = state.latestSpO2?.let {
                    val value = unitFormatter.percent(it.percent)
                    SummaryMetric(stringResource(R.string.metric_spo2), value.value, value.unit, Icons.Outlined.Favorite, oxygenColor, it.source)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (state.bloodPressure.isNotEmpty()) {
            val sortedBloodPressure = state.bloodPressure.sortedBy { it.time }
            item {
                MetricLineChart(
                    title = stringResource(R.string.metric_blood_pressure),
                    series = bloodPressureSeries(
                        entries = sortedBloodPressure,
                        selectedRange = selectedRange,
                        systolicLabel = stringResource(R.string.vitals_entry_systolic_label),
                        diastolicLabel = stringResource(R.string.vitals_entry_diastolic_label),
                    ),
                    selectedRange = selectedRange,
                    period = period,
                    accentColor = VitalsColor,
                    summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
                        stringResource(R.string.summary_readings, unitFormatter.count(sortedBloodPressure.size))
                    }",
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    valueFormatter = { "${it.roundToInt()} mmHg" },
                )
            }
        }
        if (state.spO2.isNotEmpty()) {
            val sortedSpO2 = state.spO2.sortedBy { it.time }
            item {
                MetricLineChart(
                    title = stringResource(R.string.metric_oxygen_saturation),
                    entries = sortedSpO2,
                    selectedRange = selectedRange,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    accentColor = oxygenColor,
                    summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
                        stringResource(R.string.summary_value_avg, unitFormatter.percent(state.spO2.map { it.percent }.average()).text)
                    }",
                    time = { it.time },
                    value = { it.percent },
                    valueFormatter = { unitFormatter.percent(it).text },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        state.latestVo2Max?.let { latest ->
            val vo2Max = unitFormatter.vo2Max(latest.vo2MaxMlPerKgPerMin)
            if (state.vo2Max.size > 1) {
                val sortedVo2Max = state.vo2Max.sortedBy { it.time }
                item {
                    MetricLineChart(
                        title = stringResource(R.string.metric_vo2_max),
                        entries = sortedVo2Max,
                        selectedRange = selectedRange,
                        period = period,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = vo2Color,
                        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
                            stringResource(R.string.summary_readings, unitFormatter.count(sortedVo2Max.size))
                        }",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        time = { it.time },
                        value = { it.vo2MaxMlPerKgPerMin },
                        valueFormatter = { unitFormatter.vo2Max(it).text },
                    )
                }
            }
            item {
                MetricCard(
                    title = stringResource(R.string.metric_vo2_max),
                    value = vo2Max.value,
                    unit = vo2Max.unit,
                    icon = Icons.Outlined.Speed,
                    accentColor = vo2Color,
                    source = latest.source,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (state.respiratoryRate.isNotEmpty() || state.bodyTemperature.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.section_respiratory)) }
        item {
            VitalsSummaryRow(
                first = respiratoryRateSummaryMetric(
                    entries = state.respiratoryRate,
                    selectedRange = selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                ),
                second = state.latestBodyTemperature?.let {
                    val value = unitFormatter.temperature(it.temperatureCelsius)
                    SummaryMetric(stringResource(R.string.metric_body_temp), value.value, value.unit, Icons.Outlined.DeviceThermostat, temperatureColor, it.source)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (state.respiratoryRate.isNotEmpty()) {
            item {
                MetricLineChart(
                    title = stringResource(R.string.metric_respiratory_rate),
                    series = respiratoryRateSeries(
                        entries = state.respiratoryRate,
                        selectedRange = selectedRange,
                        metricLabel = stringResource(R.string.metric_respiratory_rate),
                        averageLabel = stringResource(R.string.summary_average),
                        lowestLabel = stringResource(R.string.stat_lowest),
                        highestLabel = stringResource(R.string.stat_highest),
                    ),
                    selectedRange = selectedRange,
                    period = period,
                    accentColor = respiratoryColor,
                    summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
                        stringResource(
                            R.string.summary_value_avg,
                            unitFormatter.respiratoryRate(
                                respiratoryRateAverage(respiratoryRateBuckets(state.respiratoryRate, selectedRange, period)),
                            ).text,
                        )
                    }",
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    valueFormatter = { unitFormatter.respiratoryRate(it).text },
                )
            }
            if (selectedRange == TimeRange.DAY) {
                item {
                    SimpleVitalsList(
                        title = stringResource(R.string.vitals_respiratory_rate_readings),
                        entries = state.respiratoryRate,
                        value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            if (selectedRange != TimeRange.DAY) {
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
        }
        if (state.bodyTemperature.isNotEmpty()) {
            val sortedBodyTemperature = state.bodyTemperature.sortedBy { it.time }
            item {
                MetricLineChart(
                    title = stringResource(R.string.metric_body_temp),
                    entries = sortedBodyTemperature,
                    selectedRange = selectedRange,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    accentColor = temperatureColor,
                    summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
                        stringResource(R.string.summary_readings, unitFormatter.count(sortedBodyTemperature.size))
                    }",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    time = { it.time },
                    value = { it.temperatureCelsius },
                    valueFormatter = { unitFormatter.temperature(it).text },
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (state.vo2Max.size > 1) {
        item {
            PaginatedEntryList(
                title = stringResource(R.string.section_vo2_max_history),
                entries = state.vo2Max.sortedByDescending { it.time },
            ) { entry, rowModifier ->
                VitalsReadingRow(
                    label = unitFormatter.vo2Max(entry.vo2MaxMlPerKgPerMin).text,
                    source = entry.source,
                    time = entry.time.atZone(ZoneId.systemDefault()),
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = rowModifier,
                )
            }
        }
    }
}
