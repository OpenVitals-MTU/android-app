package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.period.TimeRange

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    source: String? = null,
    contentAtBottom: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    OpenVitalsCard(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = if (contentAtBottom) {
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            } else {
                Modifier.padding(16.dp)
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (contentAtBottom) {
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            MetricValueRow(value = value, unit = unit)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (source != null) {
                Spacer(Modifier.height(8.dp))
                SourceChip(source = source)
            }
        }
    }
}

@Composable
fun MetricCardPlaceholder(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    message: String,
    modifier: Modifier = Modifier,
    contentAtBottom: Boolean = false,
    showHeader: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    OpenVitalsCard(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = if (contentAtBottom) {
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            } else {
                Modifier.padding(16.dp)
            }
        ) {
            if (showHeader) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (contentAtBottom) {
                Spacer(Modifier.weight(1f))
            }
            if (showHeader) {
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
fun SourceChip(source: String, modifier: Modifier = Modifier) {
    val displayName = sourceDisplayName(source)
    androidx.compose.material3.AssistChip(
        onClick = {},
        label = {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        modifier = modifier,
    )
}

private fun sourceDisplayName(packageName: String): String = when {
    packageName.contains("samsung") -> "Samsung Health"
    packageName.contains("fitbit") -> "Fitbit"
    packageName.contains("opentracks") -> "OpenTracks"
    packageName.contains("strava") -> "Strava"
    packageName.contains("polar") -> "Polar"
    packageName.contains("google.android.apps.fitness") -> "Google Fit"
    else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun TimeRangeSelector(
    selected: TimeRange,
    onSelect: (TimeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TimeRange.entries.forEach { range ->
            val isSelected = range == selected
            OpenVitalsSurface(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClick = { onSelect(range) }),
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Text(
                    text = timeRangeLabel(range),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
