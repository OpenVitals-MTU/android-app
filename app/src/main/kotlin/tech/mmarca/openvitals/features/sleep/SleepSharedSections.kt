package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.SleepData
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

internal fun dailySleepTimeRangeText(
    sessions: List<SleepData>,
    selectedDate: LocalDate,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String {
    val zone = ZoneId.systemDefault()
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val ranges = sessions
        .sortedWith(compareBy<SleepData> { it.startTime }.thenBy { it.endTime })
        .joinToString(" | ") { session ->
            val start = session.startTime.atZone(zone)
            val end = session.endTime.atZone(zone)
            "${timeFormatter.format(start)} - ${timeFormatter.format(end)}"
        }

    return "${dateFormatter.format(selectedDate)}  ·  $ranges"
}

internal fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

internal fun sleepHoursDisplay(hours: Double, unitFormatter: UnitFormatter): DisplayValue =
    DisplayValue(unitFormatter.duration((hours * 3_600_000).roundToLong()), "")
