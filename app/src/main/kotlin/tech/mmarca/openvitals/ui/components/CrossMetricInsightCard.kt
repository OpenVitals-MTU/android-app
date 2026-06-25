package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.CrossMetricDirection
import tech.mmarca.openvitals.domain.insights.CrossMetricInsight
import tech.mmarca.openvitals.domain.insights.CrossMetricStrength
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CrossMetricInsightCard(
    insight: CrossMetricInsight,
    title: String,
    positiveMessage: String,
    negativeMessage: String,
    neutralMessage: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val message = when {
        insight.strength == CrossMetricStrength.WEAK -> neutralMessage
        insight.direction == CrossMetricDirection.POSITIVE -> positiveMessage
        insight.direction == CrossMetricDirection.NEGATIVE -> negativeMessage
        else -> neutralMessage
    }
    val relationship = when {
        insight.strength == CrossMetricStrength.WEAK -> stringResource(R.string.cross_metric_weak_link)
        insight.direction == CrossMetricDirection.POSITIVE -> stringResource(R.string.cross_metric_positive_link)
        insight.direction == CrossMetricDirection.NEGATIVE -> stringResource(R.string.cross_metric_negative_link)
        else -> stringResource(R.string.cross_metric_weak_link)
    }

    OpenVitalsCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                shape = shape,
            ),
        shape = shape,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = when {
                            insight.strength == CrossMetricStrength.WEAK -> Icons.AutoMirrored.Outlined.TrendingFlat
                            insight.direction == CrossMetricDirection.POSITIVE -> Icons.AutoMirrored.Outlined.TrendingUp
                            insight.direction == CrossMetricDirection.NEGATIVE -> Icons.AutoMirrored.Outlined.TrendingDown
                            else -> Icons.AutoMirrored.Outlined.TrendingFlat
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = relationship,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val correlationPercent = (insight.correlation * 100.0).roundToInt()
                Text(
                    text = stringResource(
                        R.string.cross_metric_correlation,
                        signedPercent(correlationPercent),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.cross_metric_paired_days, insight.pairedDays),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun signedPercent(value: Int): String =
    when {
        value > 0 -> "+${abs(value)}%"
        value < 0 -> "-${abs(value)}%"
        else -> "0%"
    }
