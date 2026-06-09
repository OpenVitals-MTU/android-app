package tech.mmarca.openvitals.features.heart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.HeartColor
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
internal fun HeartRateChart(
    summaries: List<HeartRateSummary>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val sorted = summaries.sortedBy { it.date }
    val summaryText = if (sorted.isNotEmpty()) {
        val avgAll = sorted.map { it.avgBpm }.average().roundToInt()
        val overallMin = sorted.minOf { it.minBpm }
        val overallMax = sorted.maxOf { it.maxBpm }
        "${localizedPeriodTitle(selectedRange, period)} · ${
            stringResource(
                R.string.summary_avg_value_range,
                unitFormatter.heartRate(avgAll.toLong()).text,
                unitFormatter.heartRate(overallMin).text,
                unitFormatter.heartRate(overallMax).text,
            )
        }"
    } else {
        localizedPeriodTitle(selectedRange, period)
    }

    val avgPoints = sorted.map { VitalsLinePoint(date = it.date, value = it.avgBpm.toDouble()) }
    val minPoints = sorted.map { VitalsLinePoint(date = it.date, value = it.minBpm.toDouble()) }
    val maxPoints = sorted.map { VitalsLinePoint(date = it.date, value = it.maxBpm.toDouble()) }
    val hasRange = sorted.any { it.minBpm != it.maxBpm }

    VitalsTrendLineChart(
        title = stringResource(R.string.metric_average_heart_rate),
        series = buildList {
            add(VitalsLineSeries(avgPoints, HeartColor, stringResource(R.string.summary_average)))
            if (hasRange) {
                add(VitalsLineSeries(minPoints, HeartColor.copy(alpha = 0.55f), stringResource(R.string.stat_lowest)))
                add(VitalsLineSeries(maxPoints, HeartColor.copy(alpha = 0.9f), stringResource(R.string.stat_highest)))
            }
        },
        selectedRange = selectedRange,
        period = period,
        accentColor = HeartColor,
        summaryText = summaryText,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.heartRate(it.roundToLong()).text },
    )
}

@Composable
internal fun RestingHRChart(
    entries: List<DailyRestingHR>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val sorted = entries.sortedBy { it.date }
    val maxBpm = sorted.maxOfOrNull { it.bpm } ?: 80L
    val minBpm = sorted.minOfOrNull { it.bpm } ?: 40L
    val avg = sorted.map { it.bpm }.average().takeUnless { it.isNaN() }?.roundToInt() ?: 0

    VitalsTrendLineChart(
        title = stringResource(R.string.metric_resting_heart_rate),
        series = listOf(
            VitalsLineSeries(
                points = sorted.map { VitalsLinePoint(date = it.date, value = it.bpm.toDouble()) },
                color = HeartColor,
                label = stringResource(R.string.metric_resting_heart_rate),
            )
        ),
        selectedRange = selectedRange,
        period = period,
        accentColor = HeartColor,
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
            stringResource(
                R.string.summary_avg_value_range,
                unitFormatter.heartRate(avg.toLong()).text,
                unitFormatter.heartRate(minBpm).text,
                unitFormatter.heartRate(maxBpm).text,
            )
        }",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.heartRate(it.roundToLong()).text },
    )
}

@Composable
internal fun HRVChart(
    entries: List<DailyHrv>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val sorted = entries.sortedBy { it.date }
    val maxMs = sorted.maxOfOrNull { it.rmssdMs } ?: 100.0
    val minMs = sorted.minOfOrNull { it.rmssdMs } ?: 0.0
    val avg = sorted.map { it.rmssdMs }.average().takeUnless { it.isNaN() } ?: 0.0

    VitalsTrendLineChart(
        title = stringResource(R.string.metric_hrv),
        series = listOf(
            VitalsLineSeries(
                points = sorted.map { VitalsLinePoint(date = it.date, value = it.rmssdMs) },
                color = HeartColor.copy(alpha = 0.85f),
                label = stringResource(R.string.metric_hrv),
            )
        ),
        selectedRange = selectedRange,
        period = period,
        accentColor = HeartColor,
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
            stringResource(
                R.string.summary_avg_value_range,
                unitFormatter.hrv(avg).text,
                unitFormatter.hrv(minMs).text,
                unitFormatter.hrv(maxMs).text,
            )
        }",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.hrv(it).text },
    )
}
