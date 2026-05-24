package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.theme.WeightColor
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun WeightEntryRow(
    entry: WeightEntry,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    BodyReadingRow(
        value = unitFormatter.weight(entry.weightKg).text,
        source = entry.source,
        time = entry.time,
        accentColor = WeightColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
internal fun BodyReadingRow(
    value: String,
    source: String,
    time: Instant,
    accentColor: Color,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val localTime = time.atZone(ZoneId.systemDefault())
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = dateTimeFormatterProvider.mediumDateTime().format(localTime),
                    style = MaterialTheme.typography.bodyMedium,
                )
                SourceChip(source = source)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
            )
        }
    }
}
