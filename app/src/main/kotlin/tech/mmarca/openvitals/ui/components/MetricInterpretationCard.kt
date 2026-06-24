package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.domain.insights.InterpretationSeverity

@Composable
fun MetricInterpretationCard(
    title: String,
    status: String,
    body: String,
    source: String,
    icon: ImageVector,
    accentColor: Color,
    severity: InterpretationSeverity,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val severityColor = when (severity) {
        InterpretationSeverity.POSITIVE -> accentColor
        InterpretationSeverity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        InterpretationSeverity.CAUTION -> MaterialTheme.colorScheme.tertiary
        InterpretationSeverity.ALERT -> MaterialTheme.colorScheme.error
    }

    OpenVitalsCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = severityColor.copy(alpha = 0.45f),
                shape = shape,
            ),
        shape = shape,

        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = severityColor,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = severityColor,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
