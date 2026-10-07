package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.ui.theme.Spacing

/** The pieces every paired-device screen shares: the round glyph, the value row, the time. */

/** The round glyph for a device. [icon] is the device's kind; a watch by default. */
@Composable
fun DeviceAvatar(
    size: Int = 40,
    icon: ImageVector? = null,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon ?: Icons.Outlined.Watch,
            contentDescription = null,
            modifier = Modifier.size((size * 0.55f).dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** A label/value row for a stored device value, with optional supporting text. */
@Composable
fun DeviceValueRow(
    label: String,
    value: String,
    supporting: String? = null,
) {
    OpenVitalsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.lg))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

/** A device timestamp: the time for today, the date once older. */
fun formatDeviceTime(
    at: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(),
): String {
    val local = at.atZone(zone)
    return if (local.toLocalDate() == today) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(local)
    } else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).format(local)
    }
}
