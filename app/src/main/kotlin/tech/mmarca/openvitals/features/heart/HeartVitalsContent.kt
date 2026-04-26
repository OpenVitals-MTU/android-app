package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.periodTitle
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.ZoneId

fun LazyListScope.HeartVitalsContent(
    state: HeartUiState,
    phase3Permissions: Set<String>,
    onGrantPermissions: (Set<String>) -> Unit,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.missingVitalsPermissions.isNotEmpty()) {
        item {
            PermissionCallout(
                title = "Vitals permissions needed",
                body = "Grant blood pressure, oxygen saturation, respiratory rate, temperature, and VO2 max permissions to fill this screen.",
                onGrant = {
                    onGrantPermissions(phase3Permissions)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (!state.hasVitalsData && !state.isLoading) {
        item {
            MetricCardPlaceholder(
                title = "Vitals",
                icon = Icons.Outlined.Favorite,
                accentColor = VitalsColor,
                message = "No vitals were recorded for this period.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (state.bloodPressure.isNotEmpty() || state.spO2.isNotEmpty() || state.vo2Max.isNotEmpty()) {
        item { SectionHeader("Cardiovascular") }
        item {
            VitalsSummaryRow(
                first = state.latestBloodPressure?.let {
                    val value = unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg)
                    SummaryMetric("Blood pressure", value.value, value.unit, Icons.Outlined.Favorite, VitalsColor, it.source)
                },
                second = state.latestSpO2?.let {
                    val value = unitFormatter.percent(it.percent)
                    SummaryMetric("SpO2", value.value, value.unit, Icons.Outlined.Favorite, oxygenColor, it.source)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (state.bloodPressure.isNotEmpty()) {
            item {
                BloodPressureChart(
                    entries = state.bloodPressure,
                    selectedRange = selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        if (state.spO2.isNotEmpty()) {
            val sortedSpO2 = state.spO2.sortedBy { it.time }
            item {
                VitalsLineChart(
                    title = "Oxygen saturation",
                    values = sortedSpO2.map { it.percent },
                    dates = sortedSpO2.map { it.time.atZone(ZoneId.systemDefault()).toLocalDate() },
                    selectedRange = selectedRange,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    accentColor = oxygenColor,
                    summary = "${periodTitle(selectedRange, period)} · ${unitFormatter.percent(state.spO2.map { it.percent }.average()).text} avg",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        state.latestVo2Max?.let { latest ->
            val vo2Max = unitFormatter.vo2Max(latest.vo2MaxMlPerKgPerMin)
            item {
                MetricCard(
                    title = "VO2 max",
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
        item { SectionHeader("Respiratory") }
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
                    SummaryMetric("Body temp", value.value, value.unit, Icons.Outlined.DeviceThermostat, temperatureColor, it.source)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (state.respiratoryRate.isNotEmpty()) {
            item {
                if (selectedRange == TimeRange.DAY) {
                    SimpleVitalsList(
                        title = "Respiratory rate readings",
                        entries = state.respiratoryRate,
                        value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    RespiratoryRateChart(
                        entries = state.respiratoryRate,
                        selectedRange = selectedRange,
                        period = period,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            if (selectedRange != TimeRange.DAY) {
                item { SectionHeader("Respiratory rate daily breakdown") }
                items(respiratoryRateDaySummaries(state.respiratoryRate).sortedByDescending { it.date }) { summary ->
                    RespiratoryRateDayRow(
                        summary = summary,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
        if (state.bodyTemperature.isNotEmpty()) {
            item {
                SimpleVitalsList(
                    title = "Body temperature readings",
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
        item { SectionHeader("VO2 max history") }
        items(state.vo2Max.sortedByDescending { it.time }) { entry ->
            VitalsReadingRow(
                label = unitFormatter.vo2Max(entry.vo2MaxMlPerKgPerMin).text,
                source = entry.source,
                time = entry.time.atZone(ZoneId.systemDefault()),
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}
