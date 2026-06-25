package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.ui.theme.accentSurfaceContainerColor

val OpenVitalsCardHorizontalPadding: Dp = 16.dp
val OpenVitalsSectionSpacing: Dp = 8.dp
val OpenVitalsMetricTilePadding: Dp = 12.dp
val OpenVitalsMetricTileSpacing: Dp = 8.dp

enum class OpenVitalsCardStyle {
    Neutral,
    Metric,
    Accent,
    Error,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenVitalsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    style: OpenVitalsCardStyle = OpenVitalsCardStyle.Neutral,
    accentColor: Color = Color.Unspecified,
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    content: @Composable () -> Unit,
) {
    val resolvedContainerColor = when {
        containerColor != Color.Unspecified -> containerColor
        style == OpenVitalsCardStyle.Metric -> MaterialTheme.colorScheme.surfaceContainerHighest
        style == OpenVitalsCardStyle.Accent && accentColor != Color.Unspecified ->
            accentSurfaceContainerColor(accentColor, amoledAlpha = 0.09f)
        style == OpenVitalsCardStyle.Error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val colors = CardDefaults.cardColors(containerColor = resolvedContainerColor)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
        ) {
            content()
        }
    }
}

@Composable
fun DetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OpenVitalsCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(OpenVitalsCardHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(OpenVitalsSectionSpacing),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f),
        )
    }
}

@Composable
fun CompactHeadingText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
fun CompactBodyText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun AccentIconChip(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun SoftProgressBand(
    progress: () -> Float,
    trackColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier,
    trackAlpha: Float = 0.3f,
) {
    Box(modifier = modifier) {
        LinearProgressIndicator(
            progress = progress,
            trackColor = trackColor.copy(alpha = trackAlpha),
            color = fillColor,
        )
    }
}

@Composable
fun SharedMetricTile(
    title: String,
    value: DisplayValue,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        modifier = modifier,
        style = OpenVitalsCardStyle.Metric,
    ) {
        Column(modifier = Modifier.padding(OpenVitalsMetricTilePadding)) {
            AutoResizeText(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Spacer(Modifier.height(OpenVitalsMetricTileSpacing))
            Row(verticalAlignment = Alignment.Bottom) {
                AutoResizeText(
                    text = value.value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (value.unit.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = value.unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}
