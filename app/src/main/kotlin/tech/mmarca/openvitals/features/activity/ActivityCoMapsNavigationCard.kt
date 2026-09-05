package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.coMapsReadableDirection
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

/** The CoMaps guidance saved while this activity was recorded. App-local. Nothing when none was banked. */
@Composable
internal fun ActivityCoMapsNavigationCard(
    samples: List<CoMapsNavigationSnapshot>,
    modifier: Modifier = Modifier,
) {
    if (samples.isEmpty()) return
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.activity_detail_comaps_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            // Oldest first.
            samples.sortedBy { it.sampledAt }.forEachIndexed { index, sample ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                val direction = coMapsReadableDirection(
                    sample.carDirection.ifEmpty { sample.pedestrianDirection },
                )
                val title = sample.nextStreet
                    .ifEmpty { sample.currentStreet }
                    .ifEmpty { sample.sessionState }
                val detail = listOf(
                    sample.distanceToTurn.takeIf { it.isNotEmpty() }
                        ?.let { stringResource(R.string.activity_detail_comaps_to_turn, it) }
                        .orEmpty(),
                    sample.distanceToTarget.takeIf { it.isNotEmpty() }
                        ?.let { stringResource(R.string.activity_detail_comaps_to_destination, it) }
                        .orEmpty(),
                    sample.distanceToNextStop.takeIf { it.isNotEmpty() }
                        ?.let { stringResource(R.string.activity_detail_comaps_to_next_stop, it) }
                        .orEmpty(),
                    direction,
                    sample.exitNumber.takeIf { it.isNotEmpty() }
                        ?.let { stringResource(R.string.recording_comaps_exit, it) }
                        .orEmpty(),
                ).filter { it.isNotEmpty() }.joinToString(" - ").ifEmpty { sample.currentStreet }
                val meta = listOf(
                    timeFormat.format(sample.sampledAt.atZone(zone)),
                    sample.sessionState,
                    sample.completionPercent?.let { percent ->
                        stringResource(R.string.recording_comaps_completion, percent.toInt())
                    }.orEmpty(),
                ).filter { it.isNotEmpty() }.joinToString(" - ")

                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    if (detail.isNotEmpty()) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
