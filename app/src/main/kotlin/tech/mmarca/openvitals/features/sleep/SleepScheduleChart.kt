package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PeriodChartXAxis
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor

/** Minute-of-day the vertical axis is anchored at (18:00) so a normal night stays contiguous. */
private const val AnchorMinuteOfDay = 18 * 60
private const val MinutesPerDay = 24 * 60

private val ScheduleChartHeight = 232.dp
private val ScheduleAxisLabelWidth = 46.dp

/**
 * Time-aligned, stage-colored sleep chart for the week/month period views. Each day is a vertical
 * bar spanning that night's in-bed window on a shared clock-time axis (earliest time at the top),
 * with stage segments colored via [stageColor]. Nights with no stage detail fall back to a solid
 * bar. Requires at least one night with an [SleepScheduleDay.inBedStart]; callers should fall back
 * to the duration bar chart otherwise.
 */
@Composable
internal fun SleepScheduleStageChart(
    title: String,
    summaryText: String,
    days: List<SleepScheduleDay>,
    selectedRange: TimeRange,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    averageSchedule: SleepOverviewSchedule? = null,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val zone = remember { ZoneId.systemDefault() }
    val axis = remember(days) { scheduleAxisRange(days, zone) } ?: return

    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val averageMarkers = remember(averageSchedule, timeFormatter) {
        averageSchedule?.let { schedule ->
            listOf(
                minuteOfDayToAnchored(schedule.startMinute) to
                    timeFormatter.format(LocalTime.of(schedule.startMinute / 60, schedule.startMinute % 60)),
                minuteOfDayToAnchored(schedule.endMinute) to
                    timeFormatter.format(LocalTime.of(schedule.endMinute / 60, schedule.endMinute % 60)),
            )
        }.orEmpty()
    }
    val axisLabels = remember(axis) {
        axis.labelMinutes().map { minute ->
            minute to timeFormatter.format(anchoredMinuteToClock(minute))
        }
    }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val selectionColor = SleepColor.copy(alpha = 0.16f)
    val emptyBarColor = SleepColor.copy(alpha = 0.5f)
    val averageLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val averageLabelStyle = MaterialTheme.typography.labelSmall

    val tapModifier = if (onDateSelected != null && days.isNotEmpty()) {
        Modifier.pointerInput(days, onDateSelected) {
            detectTapGestures { offset ->
                val barsWidth = (size.width - ScheduleAxisLabelWidth.toPx()).coerceAtLeast(1f)
                val slotWidth = barsWidth / days.size
                val index = (offset.x / slotWidth).toInt().coerceIn(0, days.lastIndex)
                onDateSelected(days[index].date)
            }
        }
    } else {
        Modifier
    }

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ScheduleChartHeight)
                    .then(tapModifier),
            ) {
                val axisLabelWidthPx = ScheduleAxisLabelWidth.toPx()
                val barsWidth = (size.width - axisLabelWidthPx).coerceAtLeast(1f)
                val slotWidth = barsWidth / days.size
                val gap = when {
                    days.size <= 7 -> 10.dp.toPx()
                    days.size <= 12 -> 6.dp.toPx()
                    else -> 3.dp.toPx()
                }.coerceAtMost(slotWidth * 0.6f)
                val barWidth = (slotWidth - gap).coerceAtLeast(1.dp.toPx())
                val cornerRadius = (barWidth / 2f).coerceAtMost(8.dp.toPx())

                fun yFor(anchoredMinute: Double): Float =
                    (size.height * ((anchoredMinute - axis.min) / axis.span)).toFloat()
                        .coerceIn(0f, size.height)

                // Horizontal clock-time gridlines + right-hand labels.
                axisLabels.forEach { (minute, label) ->
                    val y = yFor(minute.toDouble())
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(barsWidth, y),
                        strokeWidth = 1f,
                    )
                    val measured = textMeasurer.measure(label, style = labelStyle.copy(color = labelColor))
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = barsWidth + (axisLabelWidthPx - measured.size.width) / 2f,
                            y = (y - measured.size.height / 2f)
                                .coerceIn(0f, size.height - measured.size.height),
                        ),
                    )
                }

                days.forEachIndexed { index, day ->
                    val slotLeft = index * slotWidth
                    if (selectedDate == day.date && selectedRange == TimeRange.WEEK) {
                        drawRoundRect(
                            color = selectionColor,
                            topLeft = Offset(slotLeft, 0f),
                            size = Size(slotWidth, size.height),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        )
                    }
                    val start = day.inBedStart ?: return@forEachIndexed
                    val end = day.inBedEnd ?: return@forEachIndexed
                    val startMinute = start.anchoredMinutes(zone)
                    val endMinute = start.normalizedEndMinutes(end, zone)
                    if (endMinute <= startMinute) return@forEachIndexed

                    val left = slotLeft + (slotWidth - barWidth) / 2f
                    val top = yFor(startMinute)
                    val bottom = yFor(endMinute)
                    val barRect = Rect(left, top, left + barWidth, bottom)
                    val roundRect = RoundRect(barRect, CornerRadius(cornerRadius, cornerRadius))
                    val barPath = Path().apply { addRoundRect(roundRect) }

                    val segments = day.stages.mapNotNull { stage ->
                        val segStart = stage.startTime.coerceIn(start, end)
                        val segEnd = stage.endTime.coerceIn(start, end)
                        if (!segStart.isBefore(segEnd)) {
                            null
                        } else {
                            val sMinute = start.normalizedEndMinutes(segStart, zone)
                                .coerceIn(startMinute, endMinute)
                            val eMinute = start.normalizedEndMinutes(segEnd, zone)
                                .coerceIn(startMinute, endMinute)
                            Triple(stage.stageType, sMinute, eMinute)
                        }
                    }

                    if (segments.isEmpty()) {
                        drawPath(path = barPath, color = emptyBarColor)
                    } else {
                        clipPath(barPath) {
                            segments.forEach { (stageType, sMinute, eMinute) ->
                                val segTop = yFor(sMinute)
                                val segBottom = yFor(eMinute)
                                drawRect(
                                    color = stageColor(stageType),
                                    topLeft = Offset(left, segTop),
                                    size = Size(barWidth, (segBottom - segTop).coerceAtLeast(0f)),
                                )
                            }
                        }
                    }
                }

                // Average bedtime / wake-up reference lines with time chips at the left edge.
                if (averageMarkers.isNotEmpty()) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    averageMarkers.forEach { (anchoredMinute, label) ->
                        val y = yFor(anchoredMinute)
                        drawLine(
                            color = averageLineColor,
                            start = Offset(0f, y),
                            end = Offset(barsWidth, y),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = dash,
                        )
                        val measured = textMeasurer.measure(
                            label,
                            style = averageLabelStyle.copy(color = Color.White),
                        )
                        val padH = 5.dp.toPx()
                        val padV = 2.dp.toPx()
                        val chipHeight = measured.size.height + padV * 2
                        val chipTop = (y - chipHeight / 2f)
                            .coerceIn(0f, size.height - chipHeight)
                        drawRoundRect(
                            color = averageLineColor,
                            topLeft = Offset(0f, chipTop),
                            size = Size(measured.size.width + padH * 2, chipHeight),
                            cornerRadius = CornerRadius(chipHeight / 2f, chipHeight / 2f),
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(padH, chipTop + padV),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            PeriodChartXAxis(
                dates = days.map { it.date },
                selectedRange = selectedRange,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier.padding(end = ScheduleAxisLabelWidth),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Vertical axis range, in anchored minutes (minutes since 18:00), covering every night's window. */
private data class ScheduleAxis(val min: Double, val max: Double) {
    val span: Double get() = (max - min).coerceAtLeast(1.0)

    /** Whole-hour tick positions (in anchored minutes) within the range, thinned when tall. */
    fun labelMinutes(): List<Int> {
        val step = if (span > 8 * 60) 120 else 60
        val first = (ceil(min / step) * step).toInt()
        val last = (floor(max / step) * step).toInt()
        if (last < first) return listOf(min.toInt())
        return (first..last step step).toList()
    }
}

private fun scheduleAxisRange(days: List<SleepScheduleDay>, zone: ZoneId): ScheduleAxis? {
    var min = Double.MAX_VALUE
    var max = -Double.MAX_VALUE
    days.forEach { day ->
        val start = day.inBedStart ?: return@forEach
        val end = day.inBedEnd ?: return@forEach
        val startMinute = start.anchoredMinutes(zone)
        val endMinute = start.normalizedEndMinutes(end, zone)
        if (startMinute < min) min = startMinute
        if (endMinute > max) max = endMinute
    }
    if (min == Double.MAX_VALUE || max <= min) return null
    // Pad out to whole-hour boundaries so the top/bottom gridlines frame the bars.
    val paddedMin = floor(min / 60.0) * 60.0
    val paddedMax = ceil(max / 60.0) * 60.0
    return ScheduleAxis(min = paddedMin, max = paddedMax)
}

/** Minutes since the 18:00 anchor, in [0, 1440). */
private fun Instant.anchoredMinutes(zone: ZoneId): Double {
    val time = atZone(zone).toLocalTime()
    val minuteOfDay = time.hour * 60 + time.minute + time.second / 60.0
    return ((minuteOfDay - AnchorMinuteOfDay) + MinutesPerDay) % MinutesPerDay
}

/**
 * Anchored minutes for [value] measured from this night's start, so end/stage times that fall on
 * the next calendar day (or after an 18:00 wrap) stay monotonically after the start.
 */
private fun Instant.normalizedEndMinutes(value: Instant, zone: ZoneId): Double {
    val startMinute = anchoredMinutes(zone)
    val valueMinute = value.anchoredMinutes(zone)
    return if (valueMinute < startMinute) valueMinute + MinutesPerDay else valueMinute
}

/** Minutes since the 18:00 anchor for a clock minute-of-day, in [0, 1440). */
private fun minuteOfDayToAnchored(minuteOfDay: Int): Double =
    (((minuteOfDay - AnchorMinuteOfDay) + MinutesPerDay) % MinutesPerDay).toDouble()

private fun anchoredMinuteToClock(anchoredMinute: Int): LocalTime {
    val minuteOfDay = ((AnchorMinuteOfDay + anchoredMinute) % MinutesPerDay + MinutesPerDay) % MinutesPerDay
    return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
}
