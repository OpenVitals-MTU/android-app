package tech.mmarca.openvitals.features.heart

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.ZoneId
import kotlin.math.roundToInt

enum class HeartMetric {
    AVERAGE_HEART_RATE,
    RESTING_HEART_RATE,
    HRV,
    BLOOD_PRESSURE,
    SPO2,
    VO2_MAX,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
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
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.BLOOD_PRESSURE,
    )
}

@Composable
fun SpO2Screen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.SPO2,
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
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.RESPIRATORY_RATE,
    )
}

@Composable
fun BodyTemperatureScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.BODY_TEMPERATURE,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HeartMetricScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: HeartMetric,
) {
    val state by viewModel.uiState.collectAsState()
    val requestVitalsPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onVitalsPermissionsResult(granted)
    }

    MetricDetailScaffold(
        isLoading = state.isLoading,
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        error = state.error,
        onRefresh = viewModel::load,
        onSelectRange = viewModel::selectRange,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onSelectDate = viewModel::selectDate,
    ) { period ->
        when (metric) {
            HeartMetric.AVERAGE_HEART_RATE -> averageHeartRateContent(state, period, unitFormatter, dateTimeFormatterProvider)
            HeartMetric.RESTING_HEART_RATE -> restingHeartRateContent(state, period, unitFormatter, dateTimeFormatterProvider)
            HeartMetric.HRV -> hrvContent(state, period, unitFormatter, dateTimeFormatterProvider)
            HeartMetric.BLOOD_PRESSURE -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                bloodPressureContent(state, period, unitFormatter)
            }
            HeartMetric.SPO2 -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                spO2Content(state, period, unitFormatter, dateTimeFormatterProvider)
            }
            HeartMetric.VO2_MAX -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                vo2MaxContent(state, unitFormatter, dateTimeFormatterProvider)
            }
            HeartMetric.RESPIRATORY_RATE -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                respiratoryRateContent(state, period, unitFormatter, dateTimeFormatterProvider)
            }
            HeartMetric.BODY_TEMPERATURE -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                bodyTemperatureContent(state, unitFormatter, dateTimeFormatterProvider)
            }
        }
    }
}

private fun LazyListScope.averageHeartRateContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    when {
        state.selectedRange == TimeRange.DAY && state.daySamples.isNotEmpty() -> {
            item {
                HeartRateTimelineCard(
                    date = state.selectedDate,
                    samples = state.daySamples,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
            heartRateSampleStatistics(state.daySamples, unitFormatter)
        }
        state.selectedRange == TimeRange.DAY && !state.isLoading -> {
            item { HeartRateEmptyDayCard(modifier = metricModifier()) }
        }
        state.dailySummaries.isNotEmpty() -> {
            item {
                HeartRateChart(
                    summaries = state.dailySummaries,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
            heartRateSummaryStatistics(state.dailySummaries, unitFormatter)
            item { SectionHeader(stringResource(R.string.section_daily_breakdown)) }
            items(state.dailySummaries.sortedByDescending { it.date }) { summary ->
                HeartRateDayRow(
                    summary = summary,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        !state.isLoading -> noHeartMetricData(
            titleRes = R.string.metric_average_heart_rate,
            messageRes = R.string.message_no_heart_period,
            icon = Icons.Outlined.Favorite,
            accentColor = HeartColor,
        )
    }
}

private fun LazyListScope.restingHeartRateContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    when {
        state.selectedRange == TimeRange.DAY && state.dayRestingBpm != null -> {
            item {
                RestingHRDayCard(
                    bpm = state.dayRestingBpm,
                    unitFormatter = unitFormatter,
                    modifier = metricModifier(),
                )
            }
            heartNumericStatistics(
                unitFormatter = unitFormatter,
                average = unitFormatter.heartRate(state.dayRestingBpm),
                low = unitFormatter.heartRate(state.dayRestingBpm),
                high = unitFormatter.heartRate(state.dayRestingBpm),
                readings = 1,
                icon = Icons.Outlined.FavoriteBorder,
                accentColor = HeartColor,
            )
        }
        state.selectedRange != TimeRange.DAY && state.dailyRestingHR.isNotEmpty() -> {
            item {
                RestingHRChart(
                    entries = state.dailyRestingHR,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
            restingHeartRateStatistics(state.dailyRestingHR, unitFormatter)
        }
        !state.isLoading -> noHeartMetricData(
            titleRes = R.string.metric_resting_heart_rate,
            messageRes = R.string.message_no_readings_period,
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = HeartColor,
        )
    }
}

private fun LazyListScope.hrvContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    when {
        state.selectedRange == TimeRange.DAY && state.dayHrvMs != null -> {
            item {
                HRVDayCard(
                    rmssdMs = state.dayHrvMs,
                    unitFormatter = unitFormatter,
                    modifier = metricModifier(),
                )
            }
            heartNumericStatistics(
                unitFormatter = unitFormatter,
                average = unitFormatter.hrv(state.dayHrvMs),
                low = unitFormatter.hrv(state.dayHrvMs),
                high = unitFormatter.hrv(state.dayHrvMs),
                readings = 1,
                icon = Icons.Outlined.FavoriteBorder,
                accentColor = HeartColor,
            )
        }
        state.selectedRange != TimeRange.DAY && state.dailyHrv.isNotEmpty() -> {
            item {
                HRVChart(
                    entries = state.dailyHrv,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
            hrvStatistics(state.dailyHrv, unitFormatter)
        }
        !state.isLoading -> noHeartMetricData(
            titleRes = R.string.metric_hrv,
            messageRes = R.string.message_no_readings_period,
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = HeartColor,
        )
    }
}

private fun LazyListScope.vitalsMetricContent(
    state: HeartUiState,
    phase3Permissions: Set<String>,
    onGrantPermissions: (Set<String>) -> Unit,
    content: LazyListScope.() -> Unit,
) {
    if (state.missingVitalsPermissions.isNotEmpty()) {
        item {
            PermissionCallout(
                title = stringResource(R.string.vitals_permissions_needed_title),
                body = stringResource(R.string.vitals_permissions_needed_body),
                onGrant = { onGrantPermissions(phase3Permissions) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    content()
}

private fun LazyListScope.bloodPressureContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
) {
    if (state.bloodPressure.isNotEmpty()) {
        item {
            BloodPressureChart(
                entries = state.bloodPressure,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                modifier = metricModifier(),
            )
        }
        bloodPressureStatistics(state.bloodPressure, unitFormatter)
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_blood_pressure,
            messageRes = R.string.message_no_blood_pressure,
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
        )
    }
}

private fun LazyListScope.spO2Content(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.spO2.isNotEmpty()) {
        val sorted = state.spO2.sortedBy { it.time }
        item {
            VitalsLineChart(
                title = stringResource(R.string.metric_oxygen_saturation),
                values = sorted.map { it.percent },
                dates = sorted.map { it.time.atZone(ZoneId.systemDefault()).toLocalDate() },
                selectedRange = state.selectedRange,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = oxygenColor,
                summary = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                    stringResource(R.string.summary_value_avg, unitFormatter.percent(state.spO2.map { it.percent }.average()).text)
                }",
                valueFormatter = { unitFormatter.percent(it).text },
                modifier = metricModifier(),
            )
        }
        spO2Statistics(state.spO2, unitFormatter)
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_spo2,
            messageRes = R.string.message_no_oxygen,
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = oxygenColor,
        )
    }
}

private fun LazyListScope.vo2MaxContent(
    state: HeartUiState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val latest = state.latestVo2Max
    if (latest != null) {
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
        vo2MaxStatistics(state.vo2Max, unitFormatter)
        if (state.vo2Max.size > 1) {
            item { SectionHeader(stringResource(R.string.section_vo2_max_history)) }
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
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_vo2_max,
            messageRes = R.string.message_no_vo2_max,
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = vo2Color,
        )
    }
}

private fun LazyListScope.respiratoryRateContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.respiratoryRate.isNotEmpty()) {
        item {
            if (state.selectedRange == TimeRange.DAY) {
                SimpleVitalsList(
                    title = stringResource(R.string.vitals_respiratory_rate_readings),
                    entries = state.respiratoryRate,
                    value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
                    source = { it.source },
                    time = { it.time },
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            } else {
                RespiratoryRateChart(
                    entries = state.respiratoryRate,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
        }
        respiratoryRateStatistics(state.respiratoryRate, unitFormatter)
        if (state.selectedRange != TimeRange.DAY) {
            item { SectionHeader(stringResource(R.string.section_respiratory_rate_daily_breakdown)) }
            items(respiratoryRateDaySummaries(state.respiratoryRate).sortedByDescending { it.date }) { summary ->
                RespiratoryRateDayRow(
                    summary = summary,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_respiratory_rate,
            messageRes = R.string.message_no_readings_period,
            icon = Icons.Outlined.Favorite,
            accentColor = respiratoryColor,
        )
    }
}

private fun LazyListScope.bodyTemperatureContent(
    state: HeartUiState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.bodyTemperature.isNotEmpty()) {
        item {
            SimpleVitalsList(
                title = stringResource(R.string.vitals_body_temperature_readings),
                entries = state.bodyTemperature,
                value = { unitFormatter.temperature(it.temperatureCelsius).text },
                source = { it.source },
                time = { it.time },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
            )
        }
        bodyTemperatureStatistics(state.bodyTemperature, unitFormatter)
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_body_temp,
            messageRes = R.string.message_no_readings_period,
            icon = Icons.Outlined.DeviceThermostat,
            accentColor = temperatureColor,
        )
    }
}

private fun LazyListScope.heartRateSampleStatistics(
    samples: List<HeartRateSample>,
    unitFormatter: UnitFormatter,
) {
    val values = samples.map { it.beatsPerMinute }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(values.average().roundToInt().toLong()),
        low = unitFormatter.heartRate(values.minOrNull() ?: 0L),
        high = unitFormatter.heartRate(values.maxOrNull() ?: 0L),
        readings = samples.size,
        icon = Icons.Outlined.Favorite,
        accentColor = HeartColor,
    )
}

private fun LazyListScope.heartRateSummaryStatistics(
    summaries: List<HeartRateSummary>,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(summaries.map { it.avgBpm }.average().roundToInt().toLong()),
        low = unitFormatter.heartRate(summaries.minOfOrNull { it.minBpm } ?: 0L),
        high = unitFormatter.heartRate(summaries.maxOfOrNull { it.maxBpm } ?: 0L),
        readings = summaries.size,
        icon = Icons.Outlined.Favorite,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
    )
}

private fun LazyListScope.restingHeartRateStatistics(
    entries: List<DailyRestingHR>,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(entries.map { it.bpm }.average().roundToInt().toLong()),
        low = unitFormatter.heartRate(entries.minOfOrNull { it.bpm } ?: 0L),
        high = unitFormatter.heartRate(entries.maxOfOrNull { it.bpm } ?: 0L),
        readings = entries.size,
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
    )
}

private fun LazyListScope.hrvStatistics(
    entries: List<DailyHrv>,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.hrv(entries.map { it.rmssdMs }.average()),
        low = unitFormatter.hrv(entries.minOfOrNull { it.rmssdMs } ?: 0.0),
        high = unitFormatter.hrv(entries.maxOfOrNull { it.rmssdMs } ?: 0.0),
        readings = entries.size,
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
    )
}

private fun LazyListScope.bloodPressureStatistics(
    entries: List<BloodPressureEntry>,
    unitFormatter: UnitFormatter,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        val latest = entries.maxByOrNull { it.time }
        val average = unitFormatter.bloodPressure(
            entries.map { it.systolicMmHg }.average().roundToInt(),
            entries.map { it.diastolicMmHg }.average().roundToInt(),
        )
        val highest = entries
            .maxWithOrNull(compareBy<BloodPressureEntry> { it.systolicMmHg }.thenBy { it.diastolicMmHg })
            ?.let { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg) }
            ?: unitFormatter.bloodPressure(0, 0)

        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.metric_latest),
                    value = latest?.let { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg).value }.orEmpty(),
                    unit = latest?.let { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg).unit }.orEmpty(),
                    icon = Icons.Outlined.Favorite,
                    accentColor = VitalsColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_average),
                    value = average.value,
                    unit = average.unit,
                    icon = Icons.Outlined.Star,
                    accentColor = VitalsColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_highest),
                    value = highest.value,
                    unit = highest.unit,
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = VitalsColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_readings),
                    value = unitFormatter.count(entries.size),
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = VitalsColor,
                ),
            ),
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.spO2Statistics(
    entries: List<SpO2Entry>,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.percent(entries.map { it.percent }.average()),
        low = unitFormatter.percent(entries.minOfOrNull { it.percent } ?: 0.0),
        high = unitFormatter.percent(entries.maxOfOrNull { it.percent } ?: 0.0),
        readings = entries.size,
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = oxygenColor,
    )
}

private fun LazyListScope.vo2MaxStatistics(
    entries: List<Vo2MaxEntry>,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.vo2Max(entries.map { it.vo2MaxMlPerKgPerMin }.average()),
        low = unitFormatter.vo2Max(entries.minOfOrNull { it.vo2MaxMlPerKgPerMin } ?: 0.0),
        high = unitFormatter.vo2Max(entries.maxOfOrNull { it.vo2MaxMlPerKgPerMin } ?: 0.0),
        readings = entries.size,
        icon = Icons.Outlined.Speed,
        accentColor = vo2Color,
    )
}

private fun LazyListScope.respiratoryRateStatistics(
    entries: List<RespiratoryRateEntry>,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.breathsPerMinute }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.respiratoryRate(values.average()),
        low = unitFormatter.respiratoryRate(values.minOrNull() ?: 0.0),
        high = unitFormatter.respiratoryRate(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        icon = Icons.Outlined.Favorite,
        accentColor = respiratoryColor,
    )
}

private fun LazyListScope.bodyTemperatureStatistics(
    entries: List<BodyTempEntry>,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.temperatureCelsius }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.temperature(values.average()),
        low = unitFormatter.temperature(values.minOrNull() ?: 0.0),
        high = unitFormatter.temperature(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        icon = Icons.Outlined.DeviceThermostat,
        accentColor = temperatureColor,
    )
}

private fun LazyListScope.heartNumericStatistics(
    unitFormatter: UnitFormatter,
    average: DisplayValue,
    low: DisplayValue,
    high: DisplayValue,
    readings: Int,
    icon: ImageVector,
    accentColor: Color,
    countTitleRes: Int = R.string.stat_readings,
    countUnitRes: Int? = null,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.stat_average),
                    value = average.value,
                    unit = average.unit,
                    icon = icon,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_lowest),
                    value = low.value,
                    unit = low.unit,
                    icon = Icons.Outlined.Star,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_highest),
                    value = high.value,
                    unit = high.unit,
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(countTitleRes),
                    value = unitFormatter.count(readings),
                    unit = countUnitRes?.let { stringResource(it) }.orEmpty(),
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = accentColor,
                ),
            ),
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.noHeartMetricData(
    titleRes: Int,
    messageRes: Int,
    icon: ImageVector,
    accentColor: Color,
) {
    item {
        MetricCardPlaceholder(
            title = stringResource(titleRes),
            icon = icon,
            accentColor = accentColor,
            message = stringResource(messageRes),
            modifier = metricModifier(),
        )
    }
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
