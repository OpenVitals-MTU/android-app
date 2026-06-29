package tech.mmarca.openvitals.features.hydration

import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.SwipeToDeleteEntryRow
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.theme.HydrationColor
import java.time.LocalDate
import java.time.ZoneId

internal fun LazyListScope.hydrationEntries(
    entries: List<HydrationEntry>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    titleDate: LocalDate? = null,
    onEditHydrationEntry: (String) -> Unit = {},
    onDeleteHydrationEntry: (String) -> Unit = {},
) {
    val sortedEntries = entries.sortedByDescending { it.startTime }
    item {
        PaginatedEntryList(
            title = entryListTitle(titleDate, dateTimeFormatterProvider),
            entries = sortedEntries,
        ) { entry, rowModifier ->
            HydrationEntryRow(
                entry = entry,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEdit = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                    { onEditHydrationEntry(entry.id) }
                } else {
                    null
                },
                onDelete = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                    { onDeleteHydrationEntry(entry.id) }
                } else {
                    null
                },
                modifier = rowModifier,
            )
        }
    }
}

@Composable
private fun HydrationEntryRow(
    entry: HydrationEntry,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (onDelete != null) {
        SwipeToDeleteEntryRow(
            onDelete = onDelete,
            modifier = modifier,
        ) {
            HydrationEntryRowContent(
                entry = entry,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEdit = onEdit,
            )
        }
    } else {
        HydrationEntryRowContent(
            entry = entry,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEdit = onEdit,
            modifier = modifier,
        )
    }
}

@Composable
private fun HydrationEntryRowContent(
    entry: HydrationEntry,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = entry.startTime.atZone(zone)
    val end = entry.endTime.atZone(zone)
    OpenVitalsCard(
        modifier = modifier,

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateTimeFormatterProvider.mediumDate().format(start),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${dateTimeFormatterProvider.shortTime().format(start)} - ${dateTimeFormatterProvider.shortTime().format(end)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SourceChip(source = entry.source)
            }
            Text(
                text = unitFormatter.hydration(entry.liters).text,
                style = MaterialTheme.typography.titleMedium,
                color = HydrationColor,
            )
            if (onEdit != null) {
                OpenVitalsIconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.cd_edit_entry),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
