package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.AccentIconChip
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

private val ChoiceChipSize: Dp = 40.dp
private val ChoiceIconSize: Dp = 22.dp

/** One card per thing to log. A card opens the day log on that section alone. */
@Composable
fun CycleEntryChooserScreen(
    onChoose: (CycleEntrySection) -> Unit,
    onOpenFullDayLog: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = Spacing.sm)) {
        item(key = "prompt") {
            Text(
                text = stringResource(R.string.cycle_choose_prompt),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.sm),
            )
        }
        items(CycleEntrySection.entries, key = { it.routeValue }) { section ->
            CycleEntryChoiceCard(
                section = section,
                onClick = { onChoose(section) },
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xs),
            )
        }
        item(key = "full-day-log") {
            OpenVitalsTextButton(
                onClick = onOpenFullDayLog,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.sm),
            ) {
                Text(text = stringResource(R.string.cycle_choice_full_day_log))
            }
        }
    }
}

@Composable
private fun CycleEntryChoiceCard(
    section: CycleEntrySection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(LayoutMetrics.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIconChip(icon = section.icon, color = CycleColor, size = ChoiceChipSize, iconSize = ChoiceIconSize)
            Column(
                modifier = Modifier
                    .padding(horizontal = Spacing.lg)
                    .weight(1f),
            ) {
                Text(
                    text = stringResource(section.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(section.summaryRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
