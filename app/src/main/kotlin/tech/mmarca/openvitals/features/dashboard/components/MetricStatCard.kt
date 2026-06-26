package tech.mmarca.openvitals.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.features.dashboard.dashboardDisplayValue
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.accentSurfaceContainerColor

private val MetricStatIconSize = 28.dp
private val MetricStatIconGlyphSize = 16.dp
private val MetricStatProgressHeight = 3.dp

@Composable
internal fun MetricStatCard(
    title: String,
    value: DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    message: String? = null,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showTitle: Boolean = true,
    progressFraction: Float? = null,
    onClick: (() -> Unit)? = null,
) {
    val iconContainerColor = accentSurfaceContainerColor(
        accentColor = accentColor,
        amoledAlpha = 0.16f,
        fallback = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    val progressFillColor = accentColor.copy(alpha = 0.55f)

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(MetricStatIconSize)
                        .background(color = iconContainerColor, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(MetricStatIconGlyphSize),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (showTitle) {
                        AutoResizeText(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    AutoResizeText(
                        text = message ?: dashboardDisplayValue(value),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (message == null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                    )
                    if (message == null && subtitle != null) {
                        AutoResizeText(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = subtitleColor,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (progressFraction != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .height(MetricStatProgressHeight)
                        .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(progressFillColor),
                )
            }
        }
    }
}
