package tech.mmarca.openvitals.features.watches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Duration
import kotlin.math.roundToLong
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.GarminWellnessMetric

/**
 * Everything the watch measures that Health Connect has no type for. Port of
 * the Flutter build's `watch_data_screen.dart`.
 *
 * Grouped by WHEN the measurement happened, not by which file carried it —
 * the sleep score and Sleep Coach arrive in the metrics file rather than the
 * sleep one, and nobody using the app should ever have to know that.
 *
 * A metric the watch has never sent is absent rather than blank. Permanent
 * em-dash rows teach people to stop reading a screen, so what is missing is
 * named once at the foot instead.
 */
@Composable
fun WatchDataScreen(viewModel: WatchDataViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        state.isLoading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        state.metrics.isEmpty -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_watch_data_empty),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        else -> WatchDataContent(metrics = state.metrics)
    }
}

/**
 * What this screen would show if the watch sent everything — the list the
 * footer diffs against to say what is missing.
 */
private val ExpectedMetrics = listOf(
    GarminWellnessMetric.STRESS,
    GarminWellnessMetric.BODY_ENERGY,
    GarminWellnessMetric.MODERATE_MINUTES,
    GarminWellnessMetric.SLEEP_SCORE,
    GarminWellnessMetric.SLEEP_AWAKE_SECONDS,
    GarminWellnessMetric.SLEEP_AWAKENINGS,
    GarminWellnessMetric.SLEEP_NEED_MINUTES,
    GarminWellnessMetric.RECOVERY_TIME,
    GarminWellnessMetric.TRAINING_READINESS,
    GarminWellnessMetric.TRAINING_LOAD_ACUTE,
)

@Composable
private fun WatchDataContent(metrics: WatchMetrics) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "intro") {
            Text(
                text = stringResource(R.string.settings_watch_data_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        todayItems(metrics)
        lastNightItems(metrics)
        trainingItems(metrics)
        item(key = "missing") { MissingFooter(metrics) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.todayItems(metrics: WatchMetrics) {
    val stress = metrics[GarminWellnessMetric.STRESS]
    val energy = metrics[GarminWellnessMetric.BODY_ENERGY]
    val moderate = metrics.valueOf(GarminWellnessMetric.MODERATE_MINUTES)
    val vigorous = metrics.valueOf(GarminWellnessMetric.VIGOROUS_MINUTES)
    if (stress == null && energy == null && moderate == null && vigorous == null) return

    item(key = "today-header") {
        SectionTitle(stringResource(R.string.dashboard_summary_today))
    }
    if (stress != null) {
        item(key = "stress") {
            WatchValueRow(
                label = stringResource(R.string.settings_watch_metric_stress),
                supporting = averageOf(metrics.stressToday)?.let {
                    stringResource(R.string.settings_watch_average_prefix, it)
                },
                value = "${stress.value}",
            )
        }
    }
    if (energy != null) {
        item(key = "body-energy") {
            WatchValueRow(
                label = stringResource(R.string.settings_watch_metric_body_battery),
                supporting = metrics.bodyEnergyToday.maxOfOrNull { it.value }?.toString(),
                value = "${energy.value}",
            )
        }
    }
    if (moderate != null || vigorous != null) {
        item(key = "intensity") {
            // Garmin's own convention: vigorous minutes count double towards
            // the weekly goal, which is why a bare sum would understate it.
            val today = (moderate ?: 0) + 2 * (vigorous ?: 0)
            // The goal is weekly, so its progress must be the week's total,
            // not today's — the watch stores a running daily total that
            // resets nightly.
            val week = metrics.intensityMinutesWeek ?: today
            WatchValueRow(
                label = stringResource(R.string.settings_watch_metric_intensity_minutes),
                supporting = stringResource(
                    R.string.settings_watch_metric_intensity_goal,
                    "$week",
                    "150",
                ),
                value = "$today",
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.lastNightItems(metrics: WatchMetrics) {
    val score = metrics[GarminWellnessMetric.SLEEP_SCORE]
    val awake = metrics.valueOf(GarminWellnessMetric.SLEEP_AWAKE_SECONDS)
    val awakenings = metrics.valueOf(GarminWellnessMetric.SLEEP_AWAKENINGS)
    val needed = metrics.valueOf(GarminWellnessMetric.SLEEP_NEED_MINUTES)
    if (score == null && awake == null && awakenings == null && needed == null) return

    item(key = "night-header") {
        SectionTitle(stringResource(R.string.settings_watch_data_last_night))
    }
    if (score != null) {
        item(key = "sleep-score") {
            WatchValueRow(
                label = stringResource(R.string.settings_watch_metric_sleep_score),
                value = "${score.value}",
            )
        }
    }
    if (awake != null) {
        item(key = "awake") {
            WatchValueRow(
                label = stringResource(R.string.settings_watch_metric_awake),
                value = formatWatchDuration(Duration.ofSeconds(awake)),
            )
        }
    }
    if (awakenings != null) {
        item(key = "awakenings") {
            WatchValueRow(
                label = stringResource(R.string.settings_watch_metric_awakenings),
                value = "$awakenings",
            )
        }
    }
    if (needed != null) {
        item(key = "sleep-coach") {
            val reading = sleepCoachReading(
                neededMinutes = needed,
                usualMinutes = metrics.valueOf(GarminWellnessMetric.SLEEP_NEED_NORMAL_MINUTES),
            )
            val supporting = when (val comparison = reading.comparison) {
                SleepCoachComparison.Unknown -> null
                is SleepCoachComparison.Above -> stringResource(
                    R.string.settings_watch_metric_sleep_coach_body,
                    comparison.extraText,
                    comparison.usualText,
                )

                is SleepCoachComparison.Same -> stringResource(
                    R.string.settings_watch_metric_sleep_coach_equal,
                    comparison.usualText,
                )
            }
            WatchValueRow(
                label = stringResource(R.string.settings_watch_metric_sleep_coach),
                supporting = supporting,
                value = reading.neededText,
            )
        }
    }
}

/**
 * How tonight's sleep need compares to the usual one. A comparison, not a
 * number: "8h 40m needed" alone says nothing, but against the usual 7h 50m it
 * says what the day's strain cost.
 */
internal sealed interface SleepCoachComparison {
    /** The watch never sent a usual need, so there is nothing to compare to. */
    data object Unknown : SleepCoachComparison

    data class Above(val extraText: String, val usualText: String) : SleepCoachComparison

    data class Same(val usualText: String) : SleepCoachComparison
}

/** The Sleep Coach row's figures, already formatted the way the row reads. */
internal data class SleepCoachReading(
    val neededText: String,
    val comparison: SleepCoachComparison,
)

/**
 * Derives the Sleep Coach row from the two metrics that feed it. Extracted
 * from the composable so the comparison — the whole point of the row — is
 * testable without a screen.
 */
internal fun sleepCoachReading(neededMinutes: Long, usualMinutes: Long?): SleepCoachReading =
    SleepCoachReading(
        neededText = formatWatchDuration(Duration.ofMinutes(neededMinutes)),
        comparison = when {
            usualMinutes == null -> SleepCoachComparison.Unknown
            neededMinutes > usualMinutes -> SleepCoachComparison.Above(
                extraText = formatWatchDuration(Duration.ofMinutes(neededMinutes - usualMinutes)),
                usualText = formatWatchDuration(Duration.ofMinutes(usualMinutes)),
            )

            else -> SleepCoachComparison.Same(
                usualText = formatWatchDuration(Duration.ofMinutes(usualMinutes)),
            )
        },
    )

private fun androidx.compose.foundation.lazy.LazyListScope.trainingItems(metrics: WatchMetrics) {
    val recovery = metrics.valueOf(GarminWellnessMetric.RECOVERY_TIME)
    val readiness = metrics.valueOf(GarminWellnessMetric.TRAINING_READINESS)
    val acute = metrics.valueOf(GarminWellnessMetric.TRAINING_LOAD_ACUTE)
    if (recovery == null && readiness == null && acute == null) return

    item(key = "training-header") {
        SectionTitle(stringResource(R.string.settings_watch_data_training))
    }
    if (recovery != null) {
        item(key = "recovery") {
            WatchValueRow(
                label = stringResource(R.string.settings_watch_metric_recovery_time),
                value = formatWatchDuration(Duration.ofMinutes(recovery)),
            )
        }
    }
    if (readiness != null) {
        item(key = "readiness") {
            WatchValueRow(
                label = stringResource(R.string.settings_watch_metric_training_readiness),
                value = "$readiness",
            )
        }
    }
    if (acute != null) {
        item(key = "acute-load") {
            val chronic = metrics.valueOf(GarminWellnessMetric.TRAINING_LOAD_CHRONIC)
            WatchValueRow(
                label = stringResource(R.string.settings_watch_metric_training_load),
                supporting = chronic?.toString(),
                value = "$acute",
            )
        }
    }
}

@Composable
private fun MissingFooter(metrics: WatchMetrics) {
    val missing = metrics.missingFrom(ExpectedMetrics)
    if (missing.isEmpty()) return
    val names = missing.map { stringResource(labelFor(it)) }.distinct().joinToString(", ")
    Text(
        text = stringResource(R.string.settings_watch_data_missing, names),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private fun labelFor(metric: GarminWellnessMetric): Int = when (metric) {
    GarminWellnessMetric.STRESS -> R.string.settings_watch_metric_stress
    GarminWellnessMetric.BODY_ENERGY -> R.string.settings_watch_metric_body_battery
    GarminWellnessMetric.MODERATE_MINUTES,
    GarminWellnessMetric.VIGOROUS_MINUTES,
    -> R.string.settings_watch_metric_intensity_minutes

    GarminWellnessMetric.SLEEP_SCORE -> R.string.settings_watch_metric_sleep_score
    GarminWellnessMetric.SLEEP_AWAKE_SECONDS -> R.string.settings_watch_metric_awake
    GarminWellnessMetric.SLEEP_AWAKENINGS -> R.string.settings_watch_metric_awakenings
    GarminWellnessMetric.SLEEP_NEED_MINUTES,
    GarminWellnessMetric.SLEEP_NEED_NORMAL_MINUTES,
    -> R.string.settings_watch_metric_sleep_coach

    GarminWellnessMetric.RECOVERY_TIME -> R.string.settings_watch_metric_recovery_time
    GarminWellnessMetric.TRAINING_READINESS -> R.string.settings_watch_metric_training_readiness
    GarminWellnessMetric.TRAINING_LOAD_ACUTE,
    GarminWellnessMetric.TRAINING_LOAD_CHRONIC,
    -> R.string.settings_watch_metric_training_load

    // Deliberately unshown: its scale is undocumented — no units, no range,
    // no direction — so the number would be decoration.
    GarminWellnessMetric.SLEEP_PRESSURE -> R.string.settings_watch_data_title
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

private fun averageOf(series: List<WatchMetricReading>): String? {
    if (series.isEmpty()) return null
    return series.map { it.value }.average().roundToLong().toString()
}
