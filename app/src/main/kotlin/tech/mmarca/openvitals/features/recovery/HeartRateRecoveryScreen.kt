package tech.mmarca.openvitals.features.recovery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneId
import kotlin.math.roundToLong
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PeriodBarAggregation
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.theme.HeartColor

/**
 * Heart-rate recovery over time. The empty case is the usual one: a watch
 * stops recording when a workout ends, so unmeasured workouts are counted
 * and shown, not dropped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateRecoveryScreen(
    viewModel: HeartRateRecoveryViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.HEART,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            screenError = state.error,
            onRefresh = viewModel::load,
            onSelectRange = viewModel::selectRange,
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            onSelectDay = viewModel::selectDay,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
        ) { period ->
            val comparable = state.comparable

            // Not dropped: a screen showing only the measurable ones would look like no training.
            val unmeasured = state.readings.size - comparable.size

            if (comparable.isEmpty()) {
                if (!state.isLoading) {
                    item { HeartRateRecoveryEmptyCard(modifier = noteCardModifier()) }
                }
            } else {
                item {
                    val zone = ZoneId.systemDefault()
                    MetricBarChart(
                        title = stringResource(R.string.heart_rate_recovery_trend_title),
                        values = comparable.map { entry ->
                            PeriodChartValue(
                                date = entry.startTime.atZone(zone).toLocalDate(),
                                value = entry.reading.headlineDropBpm!!.toDouble(),
                            )
                        },
                        selectedRange = state.selectedRange,
                        period = period,
                        accentColor = HeartColor,
                        // The average of the measured falls only.
                        summaryValue = unitFormatter
                            .heartRate(averageDropBpm(comparable).roundToLong())
                            .text,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = noteCardModifier(),
                        yearAggregation = PeriodBarAggregation.AVERAGE_NON_ZERO,
                        valueFormatter = { value ->
                            unitFormatter.heartRate(value.roundToLong()).text
                        },
                    )
                }
            }
            if (unmeasured > 0) {
                item {
                    HeartRateRecoveryNoteCard(
                        text = stringResource(R.string.heart_rate_recovery_unmeasured, unmeasured),
                        modifier = noteCardModifier(),
                    )
                }
            }
            if (state.truncated) {
                item {
                    HeartRateRecoveryNoteCard(
                        text = stringResource(
                            R.string.heart_rate_recovery_truncated,
                            maxHeartRateRecoverySessions,
                        ),
                        modifier = noteCardModifier(),
                    )
                }
            }
        }
    }
}

private fun averageDropBpm(readings: List<HeartRateRecoverySessionReading>): Double {
    if (readings.isEmpty()) return 0.0
    return readings
        .sumOf { entry -> entry.reading.headlineDropBpm!!.toDouble() }
        .div(readings.size)
}

@Composable
internal fun HeartRateRecoveryEmptyCard(modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.heart_rate_recovery_empty),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            // Why it is empty, and what would fill it.
            Text(
                text = stringResource(R.string.heart_rate_recovery_empty_watch),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeartRateRecoveryNoteCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun noteCardModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
