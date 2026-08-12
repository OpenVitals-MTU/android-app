package tech.mmarca.openvitals.features.watches

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.contract.GarminWellnessRepository
import tech.mmarca.openvitals.domain.model.GarminWellnessMetric

/** One stored watch metric, resolved to a value and the instant it was measured. */
@Immutable
data class WatchMetricReading(
    val value: Long,
    val time: Instant,
)

/**
 * Everything the app holds that Health Connect cannot: the latest value of
 * each watch-only metric, plus the day series for the two that have one.
 * Port of the Flutter build's `watch_metrics_view_model.dart`.
 *
 * Read-only and derived — nothing here decides what to store, only what the
 * watch-data screen can show. A metric the watch has never sent is simply
 * absent, which is what lets the screen omit it rather than render a
 * permanent blank row.
 */
@Immutable
data class WatchMetrics(
    val latest: Map<GarminWellnessMetric, WatchMetricReading> = emptyMap(),
    /** Today's series, oldest first, for the metrics dense enough to draw. */
    val stressToday: List<WatchMetricReading> = emptyList(),
    val bodyEnergyToday: List<WatchMetricReading> = emptyList(),
    /**
     * Intensity minutes accumulated across the current week
     * (Monday-anchored), vigorous counted double as Garmin counts them
     * towards the weekly goal. Null when the watch has sent no intensity
     * minutes at all. This is a week-long sum of daily finals, not the single
     * latest reading: the watch stores a *running daily total* that resets
     * each midnight, so the latest value is only today's — using it for the
     * weekly goal understates the week.
     */
    val intensityMinutesWeek: Long? = null,
) {
    val isEmpty: Boolean get() = latest.isEmpty()

    operator fun get(metric: GarminWellnessMetric): WatchMetricReading? = latest[metric]

    fun valueOf(metric: GarminWellnessMetric): Long? = latest[metric]?.value

    /**
     * The metrics this watch has never sent, in declaration order — what the
     * UI names once at the foot of the screen instead of showing empty rows.
     */
    fun missingFrom(expected: List<GarminWellnessMetric>): List<GarminWellnessMetric> =
        expected.filterNot { it in latest }
}

/**
 * Loads [WatchMetrics] from [repository], the aggregation windows exactly as
 * the Flutter provider computed them: today's series over the local calendar
 * day, and the weekly total over the Monday-anchored local week, summing the
 * FINAL reading of each day (the watch's running total resets nightly).
 *
 * Top-level and clock-driven so the windowing is testable without a
 * view-model.
 */
internal suspend fun loadWatchMetrics(
    repository: GarminWellnessRepository,
    clock: Clock = Clock.systemDefaultZone(),
): WatchMetrics {
    val zone: ZoneId = clock.zone

    val latest = buildMap {
        for (metric in GarminWellnessMetric.entries) {
            val sample = repository.latest(metric) ?: continue
            put(metric, WatchMetricReading(value = sample.value, time = sample.time))
        }
    }

    val today = LocalDate.now(clock)
    val dayStart = today.atStartOfDay(zone).toInstant()
    val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant()

    suspend fun series(metric: GarminWellnessMetric): List<WatchMetricReading> =
        repository.samplesBetween(metric, dayStart, dayEnd)
            .map { WatchMetricReading(value = it.value, time = it.time) }

    // The weekly intensity-minutes total. Rows arrive oldest-first, so the
    // last value seen for a day is its final running total. Key by local
    // calendar day so a UTC-stored instant lands on the day it was actually
    // measured.
    val weekStart = today.with(DayOfWeek.MONDAY)
    suspend fun dailyFinalsSum(metric: GarminWellnessMetric): Long {
        val rows = repository.samplesBetween(
            metric,
            weekStart.atStartOfDay(zone).toInstant(),
            weekStart.plusDays(7).atStartOfDay(zone).toInstant(),
        )
        val finalByDay = LinkedHashMap<LocalDate, Long>()
        for (row in rows) {
            finalByDay[row.time.atZone(zone).toLocalDate()] = row.value
        }
        return finalByDay.values.sum()
    }

    val intensityMinutesWeek = if (
        GarminWellnessMetric.MODERATE_MINUTES in latest ||
        GarminWellnessMetric.VIGOROUS_MINUTES in latest
    ) {
        // Garmin's own convention: vigorous minutes count double towards the
        // weekly goal, which is why a bare sum would understate the week.
        dailyFinalsSum(GarminWellnessMetric.MODERATE_MINUTES) +
            2 * dailyFinalsSum(GarminWellnessMetric.VIGOROUS_MINUTES)
    } else {
        null
    }

    return WatchMetrics(
        latest = latest,
        stressToday = series(GarminWellnessMetric.STRESS),
        bodyEnergyToday = series(GarminWellnessMetric.BODY_ENERGY),
        intensityMinutesWeek = intensityMinutesWeek,
    )
}

data class WatchDataUiState(
    val isLoading: Boolean = true,
    val metrics: WatchMetrics = WatchMetrics(),
)

/** Loads the watch-only metrics for the watch-data screen. */
@HiltViewModel
class WatchDataViewModel @Inject constructor(
    private val wellnessRepository: GarminWellnessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchDataUiState())
    val uiState: StateFlow<WatchDataUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val metrics = try {
                loadWatchMetrics(wellnessRepository)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // The screen's empty state covers a failed read: "nothing
                // yet" is also what an unreadable table amounts to.
                WatchMetrics()
            }
            _uiState.value = WatchDataUiState(isLoading = false, metrics = metrics)
        }
    }
}
