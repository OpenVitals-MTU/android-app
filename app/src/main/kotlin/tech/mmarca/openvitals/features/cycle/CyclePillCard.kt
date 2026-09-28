package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.model.PillPlan
import tech.mmarca.openvitals.ui.components.AccentIconChip
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing
import java.time.LocalDate

/** Today's place in the pill scheme, with one tap to mark the pill as taken. */
@Composable
internal fun CyclePillCard(
    pill: PillTodayDisplay,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onToggleTaken: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccentIconChip(icon = Icons.Outlined.Medication, color = CycleColor)
                Text(
                    text = stringResource(R.string.cycle_pill_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = Spacing.md),
                )
            }
            Text(
                text = pillStatusText(pill.plan, pill.today, dateTimeFormatterProvider),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (pill.day?.isActive == true) {
                Text(
                    text = stringResource(if (pill.taken) R.string.cycle_pill_taken_today else R.string.cycle_pill_not_taken),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pill.taken) CycleColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (pill.taken) {
                    OpenVitalsTextButton(onClick = { onToggleTaken(false) }) {
                        Text(text = stringResource(R.string.cycle_pill_undo_taken))
                    }
                } else {
                    OpenVitalsButton(onClick = { onToggleTaken(true) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
                        Text(text = stringResource(R.string.cycle_pill_mark_taken), modifier = Modifier.padding(start = Spacing.sm))
                    }
                }
            }
        }
    }
}

/** "Day 12 of 21, taking", "Pause day 3 of 7 · Next pack starts Oct 5", or when the first pack starts. */
@Composable
internal fun pillStatusText(plan: PillPlan, date: LocalDate, dateTimeFormatterProvider: DateTimeFormatterProvider): String {
    val start = plan.packStart ?: return ""
    val formatter = dateTimeFormatterProvider.mediumDate()
    val day = plan.dayAt(date) ?: return stringResource(R.string.cycle_pill_before_start, formatter.format(start))
    return if (day.isActive) {
        stringResource(R.string.cycle_pill_day_active, day.dayOfPhase, day.phaseLength)
    } else {
        stringResource(R.string.cycle_pill_day_pause, day.dayOfPhase, day.phaseLength) +
            " · " + stringResource(R.string.cycle_pill_next_pack, formatter.format(day.nextPackStart))
    }
}
