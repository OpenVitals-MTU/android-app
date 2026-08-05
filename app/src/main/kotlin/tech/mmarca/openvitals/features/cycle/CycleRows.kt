package tech.mmarca.openvitals.features.cycle

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.SwipeToDeleteEntryRow
import tech.mmarca.openvitals.ui.theme.CycleColor
import java.time.ZoneId

@Composable
internal fun CycleObservationRow(
    observation: CycleObservation,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    if (onDelete != null) {
        SwipeToDeleteEntryRow(onDelete = onDelete, modifier = modifier) {
            CycleObservationRowContent(
                observation = observation,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEdit = onEdit,
            )
        }
    } else {
        CycleObservationRowContent(
            observation = observation,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEdit = onEdit,
            modifier = modifier,
        )
    }
}

@Composable
private fun CycleObservationRowContent(
    observation: CycleObservation,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val time = observation.time.atZone(zone)

    OpenVitalsCard(
        modifier = modifier,

    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = observation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = dateTimeFormatterProvider.mediumDateTime().format(time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                SourceChip(source = observation.source)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = observation.value,
                style = MaterialTheme.typography.bodyMedium,
                color = CycleColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
            if (onEdit != null) {
                Spacer(Modifier.width(4.dp))
                OpenVitalsIconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.cd_edit_entry),
                    )
                }
            }
        }
    }
}
