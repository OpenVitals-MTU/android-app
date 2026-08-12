package tech.mmarca.openvitals.features.watches

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * The pieces the device view and the watch-data screen both use, so the two
 * cannot drift into looking like different features. Port of the Flutter
 * build's `watch_common.dart`.
 */

/**
 * The round glyph used wherever a watch is identified. [icon] overrides the
 * watch face for a non-watch GFDI device — a cycling glyph for an Edge bike
 * computer.
 */
@Composable
internal fun WatchAvatar(
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

/**
 * One icon action in the device view's action band.
 *
 * Actions are icons because they are verbs — things asked of the watch now.
 * Anything that changes what happens NEXT time is a row further down instead.
 */
@Composable
internal fun WatchAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, enabled = enabled) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Spacing.xl),
                    strokeWidth = WatchProgressStroke,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                // Named, not decorative: the label below is a sibling text node,
                // so without this the button carries no accessible name and a
                // screen reader announces four identical unlabelled buttons.
                Icon(imageVector = icon, contentDescription = label)
            }
        }
        Spacer(modifier = Modifier.padding(top = Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
        )
    }
}

/** A label/value row for a stored watch metric, with optional supporting text. */
@Composable
internal fun WatchValueRow(
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

/**
 * `7h 50m`, `17 min`, `45s` — the coarsest unit that still says something.
 *
 * Durations here span three orders of magnitude (time awake in minutes, sleep
 * need in hours), so a single format would either bury the hours or pad the
 * minutes with a pointless `0h`.
 */
internal fun formatWatchDuration(duration: Duration): String {
    val minutes = duration.toMinutes()
    if (minutes >= 60) {
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest == 0L) "${hours}h" else "${hours}h ${rest}m"
    }
    if (minutes > 0) return "$minutes min"
    return "${duration.seconds}s"
}

/**
 * A sync timestamp as the device view shows it: the time for today, the date
 * once it is older, because "11:35" on its own is a lie after midnight.
 */
internal fun formatWatchSyncTime(
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

/** The Sensors screen's capability label, re-declared here (it is private there). */
@Composable
internal fun capabilityLabel(
    capability: BleSensorCapability,
): String = when (capability) {
    BleSensorCapability.HEART_RATE ->
        stringResource(R.string.settings_sensors_capability_heart_rate)

    BleSensorCapability.CYCLING_CADENCE ->
        stringResource(R.string.settings_sensors_capability_cycling_cadence)

    BleSensorCapability.CYCLING_POWER ->
        stringResource(R.string.settings_sensors_capability_cycling_power)

    BleSensorCapability.CYCLING_SPEED_DISTANCE ->
        stringResource(R.string.settings_sensors_capability_cycling_speed)

    BleSensorCapability.RUNNING_SPEED_CADENCE ->
        stringResource(R.string.settings_sensors_capability_running_speed_cadence)
}

/**
 * Confirms removing a paired watch.
 *
 * It asks because removal loses what re-pairing cannot restore: the Bluetooth
 * bond, the companion association and the record of which files were already
 * copied.
 */
@Composable
internal fun ConfirmRemoveWatchDialog(
    deviceName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_device_remove_confirm_title, deviceName))
        },
        text = { Text(stringResource(R.string.settings_watch_remove_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_remove),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** The small in-row progress spinner's stroke. */
private val WatchProgressStroke = 2.dp
