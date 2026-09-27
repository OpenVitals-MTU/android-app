package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.cycle.LongitudinalCycleItem
import tech.mmarca.openvitals.domain.cycle.LongitudinalCycleStats
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Radii
import tech.mmarca.openvitals.ui.theme.Spacing

private const val ReferenceMinDays = 21f
private const val ReferenceMaxDays = 35f
private const val AxisMinDays = 45
private const val MinBarFraction = 0.04f
private const val LabelInsideFraction = 0.40f
private val BarHeight: Dp = 28.dp
private val BarBorder: Dp = 1.dp
private val CurrentBarBorder: Dp = 2.dp
private val GuideStroke: Dp = 1.dp
private val DashOn = 6f
private val DashOff = 6f

/** One bar per cycle against the 21 to 35 day reference zone. Tap a bar for its detail. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CycleHistoryCard(
    stats: LongitudinalCycleStats,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onManageExclusion: (LongitudinalCycleItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<LongitudinalCycleItem?>(null) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = stringResource(R.string.cycle_variability_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (stats.items.isEmpty()) {
                Text(text = stringResource(R.string.cycle_variability_empty_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.cycle_variability_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = stringResource(R.string.cycle_variability_total, stats.totalCyclesCount), style = MaterialTheme.typography.bodyMedium)
                stats.medianDays?.let {
                    Text(
                        text = stringResource(R.string.cycle_variability_median, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                stats.meanDays?.let {
                    Text(
                        text = stringResource(R.string.cycle_variability_mean, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (stats.excludedCyclesCount > 0) {
                    Text(
                        text = stringResource(R.string.cycle_variability_excluded, stats.excludedCyclesCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            CycleBars(stats = stats, dateTimeFormatterProvider = dateTimeFormatterProvider, onSelect = { selected = it })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.cycle_variability_ref_min), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.cycle_variability_reference_band), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.cycle_variability_ref_max), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    selected?.let { item ->
        CycleDetailSheet(
            item = item,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onDismiss = { selected = null },
            onManageExclusion = {
                selected = null
                onManageExclusion(item)
            },
        )
    }
}

@Composable
private fun CycleBars(
    stats: LongitudinalCycleStats,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSelect: (LongitudinalCycleItem) -> Unit,
) {
    val maxDays = remember(stats.items) { maxOf(AxisMinDays, stats.items.maxOfOrNull { it.lengthDays } ?: AxisMinDays) }
    val bandColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Emphasis.fill)
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(DashOn, DashOff)) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x21 = size.width * (ReferenceMinDays / maxDays).coerceIn(0f, 1f)
                val x35 = size.width * (ReferenceMaxDays / maxDays).coerceIn(0f, 1f)
                drawRect(color = bandColor, topLeft = Offset(x21, 0f), size = Size((x35 - x21).coerceAtLeast(0f), size.height))
                listOf(x21, x35).forEach { x ->
                    drawLine(guideColor, Offset(x, 0f), Offset(x, size.height), GuideStroke.toPx(), pathEffect = dash)
                }
            },
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        stats.items.forEach { item ->
            CycleBarRow(item = item, maxDays = maxDays, dateTimeFormatterProvider = dateTimeFormatterProvider, onClick = { onSelect(item) })
        }
    }
}

@Composable
private fun CycleBarRow(
    item: LongitudinalCycleItem,
    maxDays: Int,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: () -> Unit,
) {
    val start = dateTimeFormatterProvider.mediumDate().format(item.startDate)
    val description = buildString {
        append(
            stringResource(
                R.string.cycle_variability_bar_a11y,
                start,
                pluralStringResource(R.plurals.cycle_variability_cycle_days, item.lengthDays, item.lengthDays),
                pluralStringResource(R.plurals.cycle_variability_bleeding_days, item.bleedingDaysCount, item.bleedingDaysCount),
            ),
        )
        if (item.isCurrent) append(" ").append(stringResource(R.string.cycle_variability_current))
        if (item.isExcluded) append(" ").append(stringResource(R.string.cycle_variability_excluded_badge))
        if (item.hasStrawSwing) append(" ").append(stringResource(R.string.cycle_variability_swing))
    }
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LayoutMetrics.minTouchTarget)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cycle_variability_bar_action), onClick = onClick)
            // The texts below repeat what the description says.
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = start, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                if (item.hasStrawSwing) CycleStatusPill(stringResource(R.string.cycle_variability_swing), scheme.tertiary)
                if (item.isExcluded) CycleStatusPill(stringResource(R.string.cycle_variability_excluded_badge), scheme.outline)
                if (item.isCurrent) CycleStatusPill(stringResource(R.string.cycle_variability_current), CycleColor)
            }
        }
        val fraction = (item.lengthDays.toFloat() / maxDays).coerceIn(MinBarFraction, 1f)
        val label = stringResource(R.string.cycle_variability_bar_label, item.lengthDays, item.bleedingDaysCount)
        val inside = fraction >= LabelInsideFraction
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(BarHeight)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        when {
                            item.isExcluded -> scheme.surfaceVariant
                            item.isCurrent -> CycleColor.copy(alpha = Emphasis.subtle)
                            else -> CycleColor.copy(alpha = Emphasis.disabled)
                        },
                    )
                    .border(
                        width = if (item.isCurrent) CurrentBarBorder else BarBorder,
                        color = when {
                            item.isExcluded -> scheme.outline
                            item.isCurrent -> CycleColor
                            else -> scheme.outlineVariant
                        },
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = Spacing.sm),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (inside) Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
            }
            if (!inside) Text(text = label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1, softWrap = false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleDetailSheet(
    item: LongitudinalCycleItem,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onDismiss: () -> Unit,
    onManageExclusion: () -> Unit,
) {
    val formatter = dateTimeFormatterProvider.mediumDate()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(text = stringResource(R.string.cycle_variability_detail_title, formatter.format(item.startDate)), style = MaterialTheme.typography.titleLarge)
            Text(
                text = pluralStringResource(R.plurals.cycle_variability_cycle_days, item.lengthDays, item.lengthDays),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = pluralStringResource(R.plurals.cycle_variability_bleeding_days, item.bleedingDaysCount, item.bleedingDaysCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.endDate?.let {
                Text(
                    text = stringResource(R.string.cycle_variability_detail_end, formatter.format(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.isExcluded) {
                Text(text = stringResource(R.string.cycle_variability_status_excluded), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                item.exclusionReason?.let {
                    Text(
                        text = stringResource(R.string.cycle_variability_exclusion_reason, stringResource(exclusionReasonLabelRes(it))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(text = stringResource(R.string.cycle_variability_status_included), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            OpenVitalsOutlinedButton(onClick = onManageExclusion, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cycle_variability_manage_exclusion))
            }
            Box(modifier = Modifier.height(Radii.lg))
        }
    }
}
