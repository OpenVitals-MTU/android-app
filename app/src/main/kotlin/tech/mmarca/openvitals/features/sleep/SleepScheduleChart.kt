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
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PeriodChartXAxis
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val ScheduleChartHeight = 232.dp
private val ScheduleAxisLabelWidth = 46.dp

/**
 * Time-aligned, stage-coloured sleep chart for the period views: one bar
 * per night on a clock axis anchored at the window's start hour. Gate on
 * [SleepScheduleAxis.range] being non-null.
 */
@Composable
internal fun SleepScheduleStageChart(
    title: String,
    summaryText: String,
    days: List<SleepScheduleDay>,
    sleepWindow: SleepWindow,
    selectedRange: TimeRange,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    averageSchedule: SleepOverviewSchedule? = null,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val zone = remember { ZoneId.systemDefault() }
    val anchorMinute = sleepWindow.startHour * 60
    val axis = remember(days, anchorMinute) {
        SleepScheduleAxis.range(days, zone, anchorMinute)
    } ?: return

    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val averageMarkers = remember(averageSchedule, anchorMinute, timeFormatter) {
        averageSchedule?.let { schedule ->
            listOf(
                SleepScheduleAxis.anchoredClockMinute(schedule.startMinute, anchorMinute) to
                    timeFormatter.format(LocalTime.of(schedule.startMinute / 60, schedule.startMinute % 60)),
                SleepScheduleAxis.anchoredClockMinute(schedule.endMinute, anchorMinute) to
                    timeFormatter.format(LocalTime.of(schedule.endMinute / 60, schedule.endMinute % 60)),
            )
        }.orEmpty()
    }
    val axisLabels = remember(axis, anchorMinute) {
        axis.tickMinutes().map { minute ->
            minute to timeFormatter.format(SleepScheduleAxis.clockTime(minute, anchorMinute))
        }
    }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val selectionColor = SleepColor.copy(alpha = 0.16f)
    val baseBarColor = SleepInBedBaseColor
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
                    val startMinute = SleepScheduleAxis.anchoredMinutes(start, zone, anchorMinute)
                    val endMinute = SleepScheduleAxis.normalizedEndMinutes(start, end, zone, anchorMinute)
                    if (endMinute <= startMinute) return@forEachIndexed

                    val left = slotLeft + (slotWidth - barWidth) / 2f
                    val top = yFor(startMinute)
                    val bottom = yFor(endMinute)
                    val barRect = Rect(left, top, left + barWidth, bottom)
                    val roundRect = RoundRect(barRect, CornerRadius(cornerRadius, cornerRadius))
                    val barPath = Path().apply { addRoundRect(roundRect) }

                    // Base block for the full in-bed span, with square stage rects clipped on top.
                    drawPath(path = barPath, color = baseBarColor)

                    val segments = day.stages.mapNotNull { stage ->
                        val segStart = stage.startTime.coerceIn(start, end)
                        val segEnd = stage.endTime.coerceIn(start, end)
                        if (!segStart.isBefore(segEnd)) {
                            null
                        } else {
                            // Normalized from this night's own start so segments stay ordered.
                            val sMinute = SleepScheduleAxis.normalizedEndMinutes(start, segStart, zone, anchorMinute)
                                .coerceIn(startMinute, endMinute)
                            val eMinute = SleepScheduleAxis.normalizedEndMinutes(start, segEnd, zone, anchorMinute)
                                .coerceIn(startMinute, endMinute)
                            Triple(stage.stageType, sMinute, eMinute)
                        }
                    }

                    if (segments.isNotEmpty()) {
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
