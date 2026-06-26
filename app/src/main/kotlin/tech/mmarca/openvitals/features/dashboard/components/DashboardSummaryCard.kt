package tech.mmarca.openvitals.features.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetProgress
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.accentSurfaceContainerColor

private val SummaryCardPadding = 6.dp
private val SummaryArcStartAngle = 130f
private val SummaryArcSweepAngle = 280f

@Composable
internal fun DashboardSummaryCard(
    title: String,
    value: DisplayValue,
    @Suppress("UNUSED_PARAMETER") icon: ImageVector,
    accentColor: Color,
    progress: DashboardWidgetProgress,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val progressTrackColor = accentSurfaceContainerColor(
        accentColor = accentColor,
        amoledAlpha = 0.2f,
        fallback = MaterialTheme.colorScheme.outlineVariant,
    )
    val progressFillColor = accentColor.copy(alpha = 0.65f)
    val subtitle = summarySubtitle(value = value, progress = progress)

    OpenVitalsCard(
        modifier = modifier.fillMaxSize(),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(SummaryCardPadding),
            contentAlignment = Alignment.Center,
        ) {
            val ringSize = minOf(maxWidth, maxHeight)
            val strokeWidth = summaryRingStroke(ringSize)

            Box(
                modifier = Modifier.size(ringSize),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = strokeWidth.toPx()
                    val diameter = size.minDimension - strokePx
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = (size.height - diameter) / 2f,
                    )
                    val arcSize = Size(diameter, diameter)
                    drawArc(
                        color = progressTrackColor,
                        startAngle = SummaryArcStartAngle,
                        sweepAngle = SummaryArcSweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = progressFillColor,
                        startAngle = SummaryArcStartAngle,
                        sweepAngle = SummaryArcSweepAngle * progress.fraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(strokeWidth + 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AutoResizeText(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    AutoResizeText(
                        text = value.value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (subtitle.isNotBlank()) {
                        AutoResizeText(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

private fun summarySubtitle(
    value: DisplayValue,
    progress: DashboardWidgetProgress,
): String = when {
    value.unit.isNotBlank() && progress.label.isNotBlank() -> "${value.unit} ${progress.label}"
    value.unit.isNotBlank() -> value.unit
    else -> progress.label
}

private fun summaryRingStroke(ringSize: Dp): Dp =
    (ringSize * 0.09f).coerceIn(5.dp, 10.dp)
