package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R

private const val EntryPageSize = 10

/**
 * A list that starts at one page and grows as the reader asks for more.
 *
 * **How far it has been expanded survives the list changing underneath it.**
 * The expansion is the reader's answer to "how much of this do I want to see",
 * not a property of the current contents, and it used to be keyed on the
 * entries themselves: deleting one row from a list expanded to fifty snapped it
 * back to ten, which threw everything below up the screen and left the reader
 * looking at a part of the page they had not scrolled to. Deleting a row is the
 * moment they are most obviously still reading the list.
 *
 * The count is only ever clamped to what exists ([boundedVisibleCount]), never
 * reset, so it also carries across a change of period. That is deliberate:
 * having asked for a longer list once, being given a longer list again is the
 * lesser surprise.
 */
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
    var visibleCount by remember(effectivePageSize) {
        mutableIntStateOf(effectivePageSize)
    }
    val boundedVisibleCount = visibleCount.coerceAtMost(entries.size)
    val visibleEntries = remember(entries, boundedVisibleCount) {
        entries.take(boundedVisibleCount)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title)
        visibleEntries.forEach { entry ->
            key(entry) {
                rowContent(
                    entry,
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        if (boundedVisibleCount < entries.size) {
            OpenVitalsOutlinedButton(
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
