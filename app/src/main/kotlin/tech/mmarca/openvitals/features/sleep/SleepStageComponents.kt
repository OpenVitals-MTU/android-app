package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.DetailSectionCard
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
    val laneHeight = 72.dp
    val labelHeight = 28.dp
    val trackCenterOffset = 18.dp
    val trackHeight = 26.dp

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(laneHeight * lanes.size.toFloat()),
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

                val sleepPath = Path()
                visibleStages.forEachIndexed { index, stage ->
                    val left = timeX(stage.start)
                    val right = timeX(stage.end)
                    val width = right - left
                    if (width > 0f) {
                        val centerY = laneCenterY(stage.laneIndex)
                        val previous = visibleStages.getOrNull(index - 1)
                        if (previous != null && previous.end == stage.start) {
                            sleepPath.lineTo(left, centerY)
                        } else {
                            sleepPath.moveTo(left, centerY)
                        }
                        val radius = minOf(trackRadius, width / 2f)
                        sleepPath.addRoundRect(
                            RoundRect(
                                rect = Rect(
                                    offset = Offset(left, centerY - trackHeightPx / 2f),
                                    size = Size(width, trackHeightPx),
                                ),
                                cornerRadius = CornerRadius(radius, radius),
                            ),
                        )
                        sleepPath.moveTo(right, centerY)
                    }
                }

                drawPath(path = sleepPath, brush = stageBrush)
                drawPath(
                    path = sleepPath,
                    brush = stageBrush,
                    style = Stroke(
                        width = transitionStrokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.cornerPathEffect(transitionStrokePx),
                    ),
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                lanes.forEach { lane ->
                    val label = sleepStageLabel(lane.labelStageType)
                    val text = if (showInlineLabels) {
                        "$label - ${unitFormatter.duration(laneDurationMs(orderedStages, lane))}"
                    } else {
                        label
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(labelHeight),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(laneHeight - labelHeight),
                    )
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

/** Grouped per-stage durations (Awake / REM / Light / Deep) for the stage breakdown. */
internal data class SleepStageDurations(
    val awakeMs: Long,
    val remMs: Long,
    val lightMs: Long,
    val deepMs: Long,
) {
    val totalMs: Long get() = awakeMs + remMs + lightMs + deepMs
}

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
    val rows = listOf(
        SleepStage.STAGE_AWAKE to durations.awakeMs,
        SleepStage.STAGE_REM to durations.remMs,
        SleepStage.STAGE_LIGHT to durations.lightMs,
        SleepStage.STAGE_DEEP to durations.deepMs,
    ).filter { it.second > 0L }
    val totalMs = durations.totalMs.takeIf { it > 0L } ?: return
    if (rows.isEmpty()) return

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { (stageType, durationMs) ->
            val fraction = (durationMs.toFloat() / totalMs).coerceIn(0f, 1f)
            val percent = (fraction * 100f).roundToInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = sleepStageLabel(stageType),
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
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(stageColor(stageType)),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${unitFormatter.duration(durationMs)} (${percent}%)",
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

internal fun stageColor(stageType: Int): Color = when (stageType) {
    SleepStage.STAGE_AWAKE -> Color(0xFFF48FB1)
    SleepStage.STAGE_LIGHT -> Color(0xFF8AB4F8)
    SleepStage.STAGE_DEEP -> Color(0xFF8E63CE)
    SleepStage.STAGE_REM -> Color(0xFFB3E5FC)
    SleepStage.STAGE_AWAKE_IN_BED -> Color(0xFFF8A6C6)
    SleepStage.STAGE_SLEEPING -> Color(0xFF7EA7F5)
    SleepStage.STAGE_OUT_OF_BED -> Color(0xFFEF9A9A)
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
