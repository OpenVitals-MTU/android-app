package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.ChartTokens
import tech.mmarca.openvitals.ui.components.DetailSectionCard
import tech.mmarca.openvitals.ui.theme.SleepColor
import tech.mmarca.openvitals.domain.model.SleepStage
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
internal fun SleepStagesBar(
    stages: List<SleepStage>,
    totalMs: Long,
    modifier: Modifier = Modifier,
    timelineStart: Instant? = null,
    timelineEnd: Instant? = null,
) {
    if (totalMs == 0L) return
    val stageTotalMs = stages.sumOf { it.durationMs.coerceAtLeast(0L) }
    val normalizedTotalMs = stageTotalMs.takeIf { it > 0L } ?: totalMs
    val timelineTotalMs = timelineStart
        ?.let { start -> timelineEnd?.let { end -> Duration.between(start, end).toMillis() } }
        ?.takeIf { it > 0L }

    Canvas(modifier = modifier) {
        if (timelineTotalMs != null) {
            val startBoundary = timelineStart ?: return@Canvas
            val endBoundary = timelineEnd ?: return@Canvas
            stages.sortedBy { it.startTime }.forEach { stage ->
                val start = stage.startTime.coerceAtLeast(startBoundary)
                val end = stage.endTime.coerceAtMost(endBoundary)
                val stageMs = Duration.between(start, end).toMillis().coerceAtLeast(0L)
                if (stageMs > 0L) {
                    val leftFraction = Duration.between(startBoundary, start).toMillis().toFloat() / timelineTotalMs
                    val widthFraction = stageMs.toFloat() / timelineTotalMs
                    drawRoundRect(
                        color = stageColor(stage.stageType),
                        topLeft = Offset(size.width * leftFraction, 0f),
                        size = Size(size.width * widthFraction, size.height),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
            }
        } else {
            var x = 0f
            stages.sortedBy { it.startTime }.forEach { stage ->
                val fraction = stage.durationMs.coerceAtLeast(0L).toFloat() / normalizedTotalMs
                val width = size.width * fraction
                drawRoundRect(
                    color = stageColor(stage.stageType),
                    topLeft = Offset(x, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                x += width
            }
        }
    }
}

@Composable
internal fun SleepStagesLaneChart(
    stages: List<SleepStage>,
    unitFormatter: UnitFormatter,
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
    timelineStart: Instant? = null,
    timelineEnd: Instant? = null,
    showInlineLabels: Boolean = true,
) {
    val orderedStages = stages
        .filter { it.durationMs > 0L }
        .sortedBy { it.startTime }
    if (orderedStages.isEmpty()) return

    val lanes = sleepStageLanes(orderedStages)
    val chartStart = timelineStart ?: orderedStages.first().startTime
    val chartEnd = timelineEnd ?: orderedStages.maxBy { it.endTime }.endTime
    val totalMs = Duration.between(chartStart, chartEnd).toMillis().takeIf { it > 0L } ?: return
    val zone = ZoneId.systemDefault()
    val midpoint = chartStart.plusMillis(totalMs / 2L)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    val labelStyle = MaterialTheme.typography.titleSmall

    // The label sits above the track in a band of its own; at a large system font the
    // label text is TALLER than its base box, so the band grows to fit it and the whole
    // lane with it. Otherwise the label overflows downward onto the track and the
    // hypnogram reads as drawn on top of its own axis. 'Ag' because an ascender and a
    // descender together are the tallest a line of this style can get.
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelHeight = remember(textMeasurer, labelStyle, density) {
        val measured = with(density) {
            textMeasurer.measure(text = "Ag", style = labelStyle).size.height.toDp()
        }
        maxOf(ChartTokens.sleepLaneLabelHeight, measured)
    }
    val laneHeight = labelHeight + SleepLaneTrackBandHeight
    val trackCenterOffset = 18.dp
    val trackHeight = ChartTokens.sleepLaneTrackHeight

    // Scrubber: continuous time under the finger, and the stage occupying that time (null in gaps).
    fun stageTypeAt(fraction: Float): Int? =
        sleepStageTypeAt(orderedStages, sleepScrubTimeAt(chartStart, totalMs, fraction))

    val haptics = LocalHapticFeedback.current
    var chartWidthPx by remember { mutableIntStateOf(0) }
    var scrubX by remember(orderedStages, chartStart, totalMs) { mutableStateOf<Float?>(null) }
    val scrubModifier = Modifier.pointerInput(orderedStages, chartStart, totalMs) {
        var lastStageType: Int? = null
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                val x = offset.x.coerceIn(0f, size.width.toFloat())
                scrubX = x
                lastStageType = stageTypeAt((x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
            },
            onDragEnd = { scrubX = null },
            onDragCancel = { scrubX = null },
        ) { change, _ ->
            change.consume()
            val x = change.position.x.coerceIn(0f, size.width.toFloat())
            scrubX = x
            val stageType = stageTypeAt((x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
            if (stageType != lastStageType) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            lastStageType = stageType
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(laneHeight * lanes.size.toFloat())
                .onSizeChanged { chartWidthPx = it.width }
                .then(scrubModifier),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val laneHeightPx = laneHeight.toPx()
                val labelHeightPx = labelHeight.toPx()
                val trackCenterOffsetPx = trackCenterOffset.toPx()
                val trackHeightPx = trackHeight.toPx()
                val transitionStrokePx = 2.dp.toPx()
                val trackRadius = trackHeightPx / 2f

                fun laneCenterY(index: Int): Float =
                    index * laneHeightPx + labelHeightPx + trackCenterOffsetPx

                fun timeX(value: Instant): Float {
                    val elapsedMs = Duration.between(chartStart, value).toMillis()
                        .coerceIn(0L, totalMs)
                    return size.width * (elapsedMs.toFloat() / totalMs)
                }

                lanes.indices.forEach { index ->
                    val centerY = laneCenterY(index)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                        size = Size(size.width, trackHeightPx),
                        cornerRadius = CornerRadius(trackRadius, trackRadius),
                    )
                }

                val visibleStages = orderedStages.mapNotNull { stage ->
                    val start = stage.startTime.coerceAtLeast(chartStart)
                    val end = stage.endTime.coerceAtMost(chartEnd)
                    if (!start.isBefore(end)) {
                        null
                    } else {
                        val laneIndex = stageLaneIndex(stage.stageType, lanes)
                        VisibleSleepStage(
                            start = start,
                            end = end,
                            laneIndex = laneIndex,
                            stageType = stage.stageType,
                        )
                    }
                }

                val gradientStartY = laneCenterY(0)
                val gradientEndY = laneCenterY(lanes.lastIndex).takeIf { it > gradientStartY }
                    ?: size.height
                val stageBrush = Brush.verticalGradient(
                    colorStops = if (lanes.size == 1) {
                        val color = stageColor(lanes.first().labelStageType)
                        arrayOf(0f to color, 1f to color)
                    } else {
                        lanes.mapIndexed { index, lane ->
                            val fraction = ((laneCenterY(index) - gradientStartY) /
                                (gradientEndY - gradientStartY)).coerceIn(0f, 1f)
                            fraction to stageColor(lane.labelStageType)
                        }.toTypedArray()
                    },
                    startY = gradientStartY,
                    endY = gradientEndY,
                )

                // Two paths, because out-of-bed is the one stretch that is NOT a sleep stage:
                // it is painted flat in its own colour while everything else takes the lane
                // gradient, which would otherwise hand it the Awake pink for sitting on the
                // Awake lane. Both share the same geometry and the same connectors, so the
                // hypnogram keeps its shape and only the gap changes colour.
                val outOfBedColor = stageColor(SleepStage.STAGE_OUT_OF_BED)
                val sleepPath = Path()
                val gapPath = Path()
                visibleStages.forEachIndexed { index, stage ->
                    val left = timeX(stage.start)
                    val right = timeX(stage.end)
                    val width = right - left
                    if (width > 0f) {
                        val centerY = laneCenterY(stage.laneIndex)
                        val path = if (stage.stageType == SleepStage.STAGE_OUT_OF_BED) {
                            gapPath
                        } else {
                            sleepPath
                        }
                        val previous = visibleStages.getOrNull(index - 1)
                        if (previous != null && previous.end == stage.start) {
                            // Each path carries its own current point now, so the transition
                            // is anchored explicitly at the previous stage's right edge rather
                            // than inherited from whatever was appended last.
                            path.moveTo(timeX(previous.end), laneCenterY(previous.laneIndex))
                            path.lineTo(left, centerY)
                        } else {
                            path.moveTo(left, centerY)
                        }
                        val radius = minOf(trackRadius, width / 2f)
                        path.addRoundRect(
                            RoundRect(
                                rect = Rect(
                                    offset = Offset(left, centerY - trackHeightPx / 2f),
                                    size = Size(width, trackHeightPx),
                                ),
                                cornerRadius = CornerRadius(radius, radius),
                            ),
                        )
                    }
                }

                val transitionStroke = Stroke(
                    width = transitionStrokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.cornerPathEffect(transitionStrokePx),
                )
                drawPath(path = gapPath, color = outOfBedColor)
                drawPath(path = gapPath, color = outOfBedColor, style = transitionStroke)
                drawPath(path = sleepPath, brush = stageBrush)
                drawPath(path = sleepPath, brush = stageBrush, style = transitionStroke)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                lanes.forEach { lane ->
                    val label = sleepStageLabel(lane.labelStageType)
                    val text = if (showInlineLabels) {
                        "$label - ${unitFormatter.duration(laneDurationMs(orderedStages, lane))}"
                    } else {
                        label
                    }
                    // A solid chip, shrink-wrapped to the words and left-aligned. The
                    // hypnogram's segments and diagonal connectors run under these
                    // labels at the left edge; without an opaque scrim the path draws
                    // straight through the text and neither is readable. Opaque on
                    // purpose — a translucent or gradient scrim still lets the path
                    // show through, which is the whole problem.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(labelHeight),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = text,
                            style = labelStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(laneHeight - labelHeight),
                    )
                }
            }

            // Scrub overlays: crosshair + tooltip drawn above the lanes, never intercepting input.
            val rawScrubX = scrubX
            if (rawScrubX != null && chartWidthPx > 0) {
                val density = LocalDensity.current
                val x = rawScrubX.coerceIn(0f, chartWidthPx.toFloat())
                val fraction = (x / chartWidthPx).coerceIn(0f, 1f)
                val scrubTime = sleepScrubTimeAt(chartStart, totalMs, fraction)
                val scrubStageType = sleepStageTypeAt(orderedStages, scrubTime)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(x.roundToInt(), 0) }
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(ChartTokens.crosshair),
                )

                val tooltipWidth = 132.dp
                val tooltipLeftPx = with(density) {
                    (x - 66.dp.toPx())
                        .coerceIn(0f, (chartWidthPx - tooltipWidth.toPx()).coerceAtLeast(0f))
                }
                Column(
                    modifier = Modifier
                        .offset { IntOffset(tooltipLeftPx.roundToInt(), 0) }
                        .width(tooltipWidth)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChartTokens.tooltipSurface)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = timeFormatter.format(scrubTime.atZone(zone)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ChartTokens.onTooltipSurface,
                    )
                    if (scrubStageType != null) {
                        Text(
                            text = sleepStageLabel(scrubStageType),
                            style = MaterialTheme.typography.labelSmall,
                            color = ChartTokens.onTooltipSurface.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = timeFormatter.format(chartStart.atZone(zone)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = timeFormatter.format(midpoint.atZone(zone)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = timeFormatter.format(chartEnd.atZone(zone)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Grouped per-stage durations (Awake / REM / Light / Deep, plus any out-of-bed gap in a
 * night slept in two goes) for the stage breakdown.
 */
internal data class SleepStageDurations(
    val awakeMs: Long,
    val remMs: Long,
    val lightMs: Long,
    val deepMs: Long,
    val outOfBedMs: Long = 0L,
) {
    val totalMs: Long get() = awakeMs + remMs + lightMs + deepMs + outOfBedMs
}

/** One stage's share of the recorded stage time in the breakdown card. */
internal data class SleepStageShare(
    val stageType: Int,
    val durationMs: Long,
    val fraction: Float,
    val percent: Int,
)

/**
 * The rows of [SleepStageBreakdown]: only the stages that recorded any time, each with its share
 * of the recorded total. Out of bed — the gap of a night slept in two goes — gets its own row
 * after Awake, so the shares account for the whole span the timeline draws. Empty when nothing
 * was recorded, in which case the card self-hides.
 */
internal fun sleepStageShares(durations: SleepStageDurations): List<SleepStageShare> {
    val totalMs = durations.totalMs.takeIf { it > 0L } ?: return emptyList()
    return listOf(
        SleepStage.STAGE_AWAKE to durations.awakeMs,
        SleepStage.STAGE_OUT_OF_BED to durations.outOfBedMs,
        SleepStage.STAGE_REM to durations.remMs,
        SleepStage.STAGE_LIGHT to durations.lightMs,
        SleepStage.STAGE_DEEP to durations.deepMs,
    )
        .filter { it.second > 0L }
        .map { (stageType, durationMs) ->
            val fraction = (durationMs.toFloat() / totalMs).coerceIn(0f, 1f)
            SleepStageShare(
                stageType = stageType,
                durationMs = durationMs,
                fraction = fraction,
                percent = (fraction * 100f).roundToInt(),
            )
        }
}

/** Continuous chart time under the finger for a horizontal scrub at [fraction] of the width. */
internal fun sleepScrubTimeAt(chartStart: Instant, totalMs: Long, fraction: Float): Instant =
    chartStart.plusMillis((fraction * totalMs).toLong())

/** The stage occupying [time], or null when the scrub sits in a gap between stages. */
internal fun sleepStageTypeAt(stages: List<SleepStage>, time: Instant): Int? =
    stages
        .firstOrNull { !time.isBefore(it.startTime) && time.isBefore(it.endTime) }
        ?.stageType

/**
 * "Share of time in bed" card wrapping the [SleepStageBreakdown]. Self-hides when there is no stage
 * data. Used across the day / week / month sleep views for a consistent breakdown card.
 */
@Composable
internal fun SleepStageShareCard(
    durations: SleepStageDurations,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    if (durations.totalMs <= 0L) return
    DetailSectionCard(title = stringResource(R.string.sleep_stages_share_title), modifier = modifier) {
        SleepStageBreakdown(durations = durations, unitFormatter = unitFormatter)
    }
}

/**
 * Vertical per-stage list (Awake / REM / Light / Deep). Each row shows the stage name, a
 * stage-colored bar that fills to the stage's share of the total, and the duration with that share
 * in parentheses.
 */
@Composable
internal fun SleepStageBreakdown(
    durations: SleepStageDurations,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val rows = sleepStageShares(durations)
    if (rows.isEmpty()) return

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { share ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = sleepStageLabel(share.stageType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(64.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(trackColor),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(share.fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(stageColor(share.stageType)),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${unitFormatter.duration(share.durationMs)} (${share.percent}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun SleepStageLegend(stages: List<SleepStage>, unitFormatter: UnitFormatter) {
    val stageTotals = stages
        .groupBy { it.stageType }
        .mapValues { (_, list) -> list.sumOf { it.durationMs } }
        .toList()
        .sortedByDescending { it.second }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stageTotals.forEach { (stageType, durationMs) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(modifier = Modifier.height(8.dp).width(8.dp)) {
                    drawCircle(color = stageColor(stageType))
                }
                Text(
                    text = sleepStageLabel(stageType),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = unitFormatter.duration(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/**
 * The in-bed span itself, under everything the night recorded. Where a stage covers it
 * the stage wins; where nothing does, this is what shows, and it means "in bed, nothing
 * recorded here" — NOT "out of bed". Keeping it translucent [SleepColor] is what makes a
 * bar read as sleep at a glance.
 */
internal val SleepInBedBaseColor: Color = SleepColor.copy(alpha = 0.5f)

internal fun stageColor(stageType: Int): Color = when (stageType) {
    SleepStage.STAGE_AWAKE -> Color(0xFFF48FB1)
    SleepStage.STAGE_LIGHT -> Color(0xFF8AB4F8)
    SleepStage.STAGE_DEEP -> Color(0xFF8E63CE)
    SleepStage.STAGE_REM -> Color(0xFFB3E5FC)
    SleepStage.STAGE_AWAKE_IN_BED -> Color(0xFFF8A6C6)
    SleepStage.STAGE_SLEEPING -> Color(0xFF7EA7F5)
    // Out of bed sits on the Awake lane and is a kind of awake, so it belongs to the
    // Awake pink family — a dusty, desaturated rose rather than the bright pink — so
    // it reads as "up" at a glance while still telling apart from awake-in-bed. It
    // stays far from every sleep stage (all cool: violet, two blues, a cyan). The
    // earlier warm brown-grey read as a different kind of thing altogether.
    SleepStage.STAGE_OUT_OF_BED -> Color(0xFFC98FA6)
    else -> Color(0xFF90A4AE)
}

@Composable
internal fun sleepStageLabel(stageType: Int): String = stringResource(
    when (stageType) {
        SleepStage.STAGE_AWAKE -> R.string.sleep_stage_awake
        SleepStage.STAGE_SLEEPING -> R.string.sleep_stage_sleeping
        SleepStage.STAGE_OUT_OF_BED -> R.string.sleep_stage_out_of_bed
        SleepStage.STAGE_LIGHT -> R.string.sleep_stage_light
        SleepStage.STAGE_DEEP -> R.string.sleep_stage_deep
        SleepStage.STAGE_REM -> R.string.sleep_stage_rem
        SleepStage.STAGE_AWAKE_IN_BED -> R.string.sleep_stage_awake_in_bed
        else -> R.string.sleep_stage_unknown
    }
)

private data class SleepStageLane(
    val stageTypes: Set<Int>,
    val labelStageType: Int,
)

private data class VisibleSleepStage(
    val start: Instant,
    val end: Instant,
    val laneIndex: Int,
    val stageType: Int,
)

private val StandardSleepStageLanes = listOf(
    SleepStageLane(
        stageTypes = setOf(
            SleepStage.STAGE_AWAKE,
            SleepStage.STAGE_AWAKE_IN_BED,
            SleepStage.STAGE_OUT_OF_BED,
        ),
        labelStageType = SleepStage.STAGE_AWAKE,
    ),
    SleepStageLane(stageTypes = setOf(SleepStage.STAGE_REM), labelStageType = SleepStage.STAGE_REM),
    SleepStageLane(
        stageTypes = setOf(SleepStage.STAGE_LIGHT, SleepStage.STAGE_SLEEPING),
        labelStageType = SleepStage.STAGE_LIGHT,
    ),
    SleepStageLane(stageTypes = setOf(SleepStage.STAGE_DEEP), labelStageType = SleepStage.STAGE_DEEP),
)

private fun sleepStageLanes(stages: List<SleepStage>): List<SleepStageLane> {
    val knownTypes = StandardSleepStageLanes.flatMap { it.stageTypes }.toSet()
    val extraLanes = stages
        .map { it.stageType }
        .distinct()
        .filterNot { it in knownTypes }
        .map { stageType ->
            SleepStageLane(stageTypes = setOf(stageType), labelStageType = stageType)
        }
    return StandardSleepStageLanes + extraLanes
}

private fun stageLaneIndex(stageType: Int, lanes: List<SleepStageLane>): Int =
    lanes.indexOfFirst { stageType in it.stageTypes }.coerceAtLeast(0)

private fun laneDurationMs(stages: List<SleepStage>, lane: SleepStageLane): Long =
    stages
        .filter { it.stageType in lane.stageTypes }
        .sumOf { it.durationMs.coerceAtLeast(0L) }

/** The fixed part of a lane: the track and its air, under the label band. */
private val SleepLaneTrackBandHeight = ChartTokens.heightSleepLane - ChartTokens.sleepLaneLabelHeight
