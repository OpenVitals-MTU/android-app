package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import java.time.LocalDate

@Composable
fun entryListTitle(
    date: LocalDate?,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String =
    date?.let { "${stringResource(R.string.section_entries)} · ${dateTimeFormatterProvider.mediumDate().format(it)}" }
        ?: stringResource(R.string.section_entries)
