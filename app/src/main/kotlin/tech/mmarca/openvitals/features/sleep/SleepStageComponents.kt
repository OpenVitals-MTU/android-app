package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.SleepStage

@Composable
internal fun SleepStagesBar(
    stages: List<SleepStage>,
    totalMs: Long,
    modifier: Modifier = Modifier,
) {
    if (totalMs == 0L) return
    Canvas(modifier = modifier) {
        var x = 0f
        stages.sortedBy { it.startTime }.forEach { stage ->
            val fraction = stage.durationMs.toFloat() / totalMs
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
                    text = SleepStage.stageLabel(stageType),
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
    SleepStage.STAGE_AWAKE -> Color(0xFFFFB74D)
    SleepStage.STAGE_LIGHT -> Color(0xFF90CAF9)
    SleepStage.STAGE_DEEP -> Color(0xFF3949AB)
    SleepStage.STAGE_REM -> Color(0xFF7E57C2)
    SleepStage.STAGE_AWAKE_IN_BED -> Color(0xFFFFCC80)
    SleepStage.STAGE_SLEEPING -> Color(0xFF5C6BC0)
    SleepStage.STAGE_OUT_OF_BED -> Color(0xFFEF9A9A)
    else -> Color(0xFF90A4AE)
}
