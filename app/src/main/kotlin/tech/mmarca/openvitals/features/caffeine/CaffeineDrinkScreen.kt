package tech.mmarca.openvitals.features.caffeine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.CaffeineDrinkProfile
import tech.mmarca.openvitals.domain.insights.CaffeineProfileHorizon
import tech.mmarca.openvitals.domain.insights.caffeineDrinkProfile
import tech.mmarca.openvitals.ui.components.ChartTokens
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.ChartZoom
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.timeAxisInstantsFor

/**
 * One drink and what it is doing to you: the day's model asked about one
 * drink. No second load: the entries are already in [CaffeineViewModel].
 */
@Composable
fun CaffeineDrinkScreen(
    viewModel: CaffeineViewModel,
    entryId: String,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onTitleChanged: (String?) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entry = state.entries.firstOrNull { it.id == entryId }

    LaunchedEffect(entry?.name) {
        onTitleChanged(entry?.name?.takeIf { it.isNotBlank() })
    }

    if (entry == null) {
        // Deleted while open, or followed from something stale.
        if (!state.isLoading) {
            ErrorMessage(message = stringResource(R.string.no_data))
        }
        return
    }

    val profile = remember(entry, state.preferences, state.bodyProfile) {
        caffeineDrinkProfile(
            entry = entry,
            now = Instant.now(),
            preferences = state.preferences,
            bodyProfile = state.bodyProfile,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            CaffeineDrinkHeadlineCard(
                profile = profile,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = cardModifier(),
            )
        }
        item {
            CaffeineDrinkStatsCard(
                profile = profile,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = cardModifier(),
            )
        }
        item {
            CaffeineDrinkCurveCard(
                profile = profile,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = cardModifier(),
            )
        }
    }
}

@Composable
private fun CaffeineDrinkHeadlineCard(
    profile: CaffeineDrinkProfile,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val entry = profile.entry
    val time = timeFormatter.format(entry.startTime.atZone(zone))
    val drankOverMinutes = Duration.between(entry.startTime, entry.endTime).toMinutes()

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = drinkMg(entry.caffeineMg, unitFormatter),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (drankOverMinutes > 0L) "$time · $drankOverMinutes min" else time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            // Without this the screen looks broken: you drank 95mg and the peak says 62.
            Text(
                text = stringResource(R.string.caffeine_drink_peak_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The four numbers: the peak, what is left, and the two moments that decide bedtime. */
@Composable
private fun CaffeineDrinkStatsCard(
    profile: CaffeineDrinkProfile,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val stillGoing = stringResource(
        R.string.caffeine_drink_still_going,
        CaffeineProfileHorizon.toHours().toString(),
    )

    fun moment(time: Instant?): String =
        time?.let { timeFormatter.format(it.atZone(zone)) } ?: stillGoing

    val rows = listOf(
        stringResource(R.string.caffeine_drink_peak) to
            "${drinkMg(profile.peakMg, unitFormatter)} · " +
            timeFormatter.format(profile.peakTime.atZone(zone)),
        stringResource(R.string.caffeine_drink_now) to drinkMg(profile.currentMg, unitFormatter),
        stringResource(R.string.caffeine_drink_half_gone) to moment(profile.halfGoneTime),
        stringResource(R.string.caffeine_drink_gone) to moment(profile.goneTime),
    )

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            rows.forEach { (label, value) ->
                Row(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CaffeineDrinkCurveCard(
    profile: CaffeineDrinkProfile,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val curve = profile.curve
    if (curve.size < 2) return

    val zone = ZoneId.systemDefault()
    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val start = curve.first().time
    val end = curve.last().time
    val spanMillis = (end.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(1L)
    val accentColor = MaterialTheme.colorScheme.primary

    // Built once, identity-stable, so the geometry cache holds.
    val chartPoints = remember(curve) {
        curve.map { point ->
            MetricLinePlotPoint(
                xFraction = (point.time.toEpochMilli() - start.toEpochMilli()).toFloat() / spanMillis,
                value = point.valueMg,
            )
        }
    }

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.caffeine_drink_curve_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(12.dp))
            // The curve runs 36 hours; zoom makes one chart show the fade and the peak.
            ChartZoom(curve) { zoom ->
                Column {
                    MetricLinePlot(
                        points = chartPoints,
                        minValue = 0.0,
                        // This drink's own scale: the chart is about its shape.
                        maxValue = if (profile.peakMg <= 0.0) 1.0 else profile.peakMg,
                        accentColor = accentColor,
                        chartHeight = ChartTokens.heightDay,
                        valueFormatter = { drinkMg(it, unitFormatter) },
                        lineStrokeWidth = 3.dp,
                        drawPoints = false,
                        viewport = zoom.viewport,
                        multiTouch = zoom.multiTouch,
                        scrubLabel = { point ->
                            val at = start.plusMillis(
                                (point.xFraction.coerceIn(0f, 1f) * spanMillis).roundToLong(),
                            )
                            drinkMg(point.value, unitFormatter) to
                                timeFormatter.format(at.atZone(zone))
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    // The x axis: a fade with no hours under it says nothing.
                    ChartXAxisWithYAxis {
                        val edges = timeAxisInstantsFor(start, end, zoom.viewport)
                        Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            edges.forEach { at ->
                                Text(
                                    text = timeFormatter.format(at.atZone(zone)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun drinkMg(value: Double, unitFormatter: UnitFormatter): String =
    "${unitFormatter.count(value.roundToInt())} mg"

private fun cardModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
