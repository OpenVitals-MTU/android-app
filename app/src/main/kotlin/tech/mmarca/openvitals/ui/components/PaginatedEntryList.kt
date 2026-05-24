package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R

private const val EntryPageSize = 10

@Composable
fun <T> PaginatedEntryList(
    title: String,
    entries: List<T>,
    modifier: Modifier = Modifier,
    pageSize: Int = EntryPageSize,
    rowContent: @Composable (entry: T, modifier: Modifier) -> Unit,
) {
    if (entries.isEmpty()) return

    val effectivePageSize = pageSize.coerceAtLeast(1)
    var visibleCount by remember(entries, effectivePageSize) {
        mutableIntStateOf(entries.size.coerceAtMost(effectivePageSize))
    }
    val boundedVisibleCount = visibleCount.coerceAtMost(entries.size)

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title)
        entries.take(boundedVisibleCount).forEach { entry ->
            rowContent(
                entry,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (boundedVisibleCount < entries.size) {
            OutlinedButton(
                onClick = {
                    visibleCount = (boundedVisibleCount + effectivePageSize).coerceAtMost(entries.size)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.action_load_more_entries))
            }
        }
    }
}
