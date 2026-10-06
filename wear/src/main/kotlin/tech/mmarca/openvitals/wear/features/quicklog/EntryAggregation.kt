package tech.mmarca.openvitals.wear.features.quicklog

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import tech.mmarca.openvitals.wear.MetricUiState
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.WearUiState

private const val DaysInWeek = 7

/**
 * Folds the watch's own entries into the metrics they feed: water into
 * hydration, weigh-ins into weight, spot measurements into heart rate and
 * HRV where nothing else has a reading. A metric without entries keeps
 * whatever it had.
 */
internal fun WearUiState.withEntries(
    entries: List<LoggedEntry>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): WearUiState {
    val byType = entries.groupBy { it.type }
    val updated = metrics.toMutableMap()

    byType[EntryType.WATER]?.let { water ->
        updated[WearMetric.HYDRATION] = MetricUiState(
            current = water.onDay(today, zone).sumOf { it.value },
            goal = preferences.waterGoalMl.toDouble(),
            today = water.onDay(today, zone).hourlyTotals(zone),
            week = lastDays(today).map { day -> water.onDay(day, zone).sumOf { it.value } },
        )
    }
    byType[EntryType.WEIGHT]?.let { weights ->
        updated[WearMetric.WEIGHT] = MetricUiState(
            current = weights.last().value,
            today = weights.onDay(today, zone).map { it.value },
            week = lastDays(today).mapNotNull { day -> weights.onDay(day, zone).lastOrNull()?.value },
        )
    }
    byType[EntryType.HEART_RATE]?.let { updated.fillCurrent(WearMetric.HEART_RATE, it.last().value) }
    byType[EntryType.HRV]?.let { updated.fillCurrent(WearMetric.HRV, it.last().value) }

    return copy(metrics = updated, entries = entries)
}

private fun MutableMap<WearMetric, MetricUiState>.fillCurrent(metric: WearMetric, value: Double) {
    val existing = this[metric] ?: MetricUiState()
    if (existing.current == null) this[metric] = existing.copy(current = value)
}

private fun List<LoggedEntry>.onDay(day: LocalDate, zone: ZoneId): List<LoggedEntry> =
    filter { Instant.ofEpochMilli(it.timeMillis).atZone(zone).toLocalDate() == day }

/** Totals per hour from midnight up to the last hour with an entry. */
private fun List<LoggedEntry>.hourlyTotals(zone: ZoneId): List<Double> {
    if (isEmpty()) return emptyList()
    val hours = map { Instant.ofEpochMilli(it.timeMillis).atZone(zone).hour }
    val totals = DoubleArray(hours.max() + 1)
    forEachIndexed { index, entry -> totals[hours[index]] += entry.value }
    return totals.toList()
}

/** The last seven days, oldest first, today last. */
private fun lastDays(today: LocalDate): List<LocalDate> =
    (DaysInWeek - 1 downTo 0).map { today.minusDays(it.toLong()) }
