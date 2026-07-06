package tech.mmarca.openvitals.features.caffeine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.MealType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.model.CaffeineCatalogMatchConfidence
import tech.mmarca.openvitals.domain.model.CaffeineDailyStat
import tech.mmarca.openvitals.domain.model.CaffeineDistributionSlice
import tech.mmarca.openvitals.domain.model.CaffeineEntryInsight
import tech.mmarca.openvitals.domain.model.CaffeineInsights
import tech.mmarca.openvitals.domain.model.CaffeinePoint
import tech.mmarca.openvitals.domain.model.CaffeineTimeBucket
import tech.mmarca.openvitals.domain.model.CaffeineTimeOfDayBucket
import tech.mmarca.openvitals.domain.model.displayLabel
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.theme.HydrationColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaffeineScreen(
    viewModel: CaffeineViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedInsight = (state.homeDisplay.entryInsights + state.analyticsDisplay.entryInsights)
        .firstOrNull { it.entry.id == state.selectedEntryId }
    val errorMessage = state.error.resolve()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.CAFFEINE,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            enabled = !hcUx.syncPaused,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                caffeineHomeAndAnalyticsContent(
                    state = state,
                    errorMessage = errorMessage,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onCompleteSetup = viewModel::completeSetup,
                    onSkipSetup = viewModel::skipSetup,
                    onSelectAnalyticsRange = viewModel::selectAnalyticsRange,
                    onSelectEntry = viewModel::selectEntry,
                )
            }
        }
    }

    if (selectedInsight != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectEntry(null) },
        ) {
            CaffeineContributionSheet(
                insight = selectedInsight,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun LazyListScope.caffeineHomeAndAnalyticsContent(
    state: CaffeineUiState,
    errorMessage: String?,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onCompleteSetup: (CaffeinePreferences) -> Unit,
    onSkipSetup: () -> Unit,
    onSelectAnalyticsRange: (CaffeineAnalyticsRange) -> Unit,
    onSelectEntry: (String) -> Unit,
) {
    if (errorMessage != null) {
        item {
            ErrorMessage(message = errorMessage)
        }
    }

    if (state.showSetup) {
        item {
            CaffeineSetupCard(
                preferences = state.preferences,
                bodyProfile = state.bodyProfile,
                onSave = onCompleteSetup,
                onSkip = onSkipSetup,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    item { SectionHeader(stringResource(R.string.caffeine_section_dashboard)) }
    item {
        CaffeineHomeOverviewCard(
            insights = state.homeDisplay,
            unitFormatter = unitFormatter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        CaffeineCurveCard(
            insights = state.homeDisplay,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onSelectEntry = onSelectEntry,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    item { SectionHeader(stringResource(R.string.caffeine_section_sleep)) }
    item {
        CaffeineSleepImpactCard(
            insights = state.homeDisplay,
            unitFormatter = unitFormatter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    item { SectionHeader(stringResource(R.string.caffeine_section_entries)) }
    if (state.homeDisplay.entryInsights.isEmpty()) {
        item {
            EmptyCaffeineCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    } else {
        items(state.homeDisplay.entryInsights, key = { it.entry.id }) { insight ->
            CaffeineEntryRow(
                insight = insight,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onClick = { onSelectEntry(insight.entry.id) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }

    item { SectionHeader(stringResource(R.string.caffeine_section_analytics)) }
    item {
        CaffeineAnalyticsRangePicker(
            selectedRange = state.analyticsRange,
            onSelectRange = onSelectAnalyticsRange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        CaffeineAnalyticsSummaryCard(
            insights = state.analyticsDisplay,
            range = state.analyticsRange,
            unitFormatter = unitFormatter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        CaffeineDailyImpactCard(
            stats = state.analyticsDisplay.dailyStats,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            rangeLabel = state.analyticsRange.displayLabel(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    item {
        CaffeineDistributionCard(
            title = stringResource(R.string.caffeine_sources),
            slices = state.analyticsDisplay.sourceTotals,
            unitFormatter = unitFormatter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        CaffeineDistributionCard(
            title = stringResource(R.string.caffeine_items),
            slices = state.analyticsDisplay.itemTotals,
            unitFormatter = unitFormatter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        CaffeineDistributionCard(
            title = stringResource(R.string.caffeine_inferred_categories),
            slices = state.analyticsDisplay.categoryTotals,
            unitFormatter = unitFormatter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        CaffeineTimeBucketsCard(
            buckets = state.analyticsDisplay.timeBuckets,
            unitFormatter = unitFormatter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    item { SectionHeader(stringResource(R.string.caffeine_section_science)) }
    item {
        CaffeineScienceCard(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        CaffeineReferencesCard(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun CaffeineSetupCard(
    preferences: CaffeinePreferences,
    bodyProfile: BodyProfile,
    onSave: (CaffeinePreferences) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(preferences) { mutableStateOf(preferences) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.caffeine_setup_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = stringResource(R.string.caffeine_setup_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            CaffeinePreferencesEditor(
                preferences = draft,
                bodyProfile = bodyProfile,
                onChange = { draft = it },
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                OutlinedButton(onClick = onSkip) {
                    Text(stringResource(R.string.action_not_now))
                }
                Button(onClick = { onSave(draft) }) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun CaffeineHomeOverviewCard(
    insights: CaffeineInsights,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.caffeine_current_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = formatMg(insights.currentMg, unitFormatter),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            CaffeineCurrentSleepStatus(
                insights = insights,
                unitFormatter = unitFormatter,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                CaffeineMiniStat(
                    title = stringResource(R.string.caffeine_today_total),
                    value = formatMg(insights.todayTotalMg, unitFormatter),
                    icon = Icons.Outlined.LocalDrink,
                    modifier = Modifier.weight(1f),
                )
                CaffeineMiniStat(
                    title = stringResource(R.string.caffeine_time_to_safe),
                    value = insights.timeToThresholdMinutes?.let(::formatDurationMinutes)
                        ?: stringResource(R.string.not_available),
                    icon = Icons.Outlined.QueryStats,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CaffeineCurrentSleepStatus(
    insights: CaffeineInsights,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val status = caffeineSleepImpactStatus(insights)
    val color = when (status) {
        CaffeineSleepImpactStatus.UNLIKELY -> MaterialTheme.colorScheme.primary
        CaffeineSleepImpactStatus.ELEVATED_NOW -> MaterialTheme.colorScheme.tertiary
        CaffeineSleepImpactStatus.MAY_AFFECT_SLEEP -> MaterialTheme.colorScheme.error
    }
    val icon = when (status) {
        CaffeineSleepImpactStatus.UNLIKELY -> Icons.Outlined.CheckCircle
        CaffeineSleepImpactStatus.ELEVATED_NOW -> Icons.Outlined.QueryStats
        CaffeineSleepImpactStatus.MAY_AFFECT_SLEEP -> Icons.Outlined.WarningAmber
    }
    val title = stringResource(
        when (status) {
            CaffeineSleepImpactStatus.UNLIKELY -> R.string.caffeine_sleep_status_unlikely
            CaffeineSleepImpactStatus.ELEVATED_NOW -> R.string.caffeine_sleep_status_elevated_now
            CaffeineSleepImpactStatus.MAY_AFFECT_SLEEP -> R.string.caffeine_sleep_status_may_affect
        }
    )
    val body = when (status) {
        CaffeineSleepImpactStatus.UNLIKELY -> stringResource(
            R.string.caffeine_sleep_status_unlikely_body,
            formatMg(insights.currentMg, unitFormatter),
            formatMg(insights.sleepThresholdMg.toDouble(), unitFormatter),
        )
        CaffeineSleepImpactStatus.ELEVATED_NOW -> {
            val timeToSafe = insights.timeToThresholdMinutes
                ?.takeIf { it > 0L }
                ?.let(::formatDurationMinutes)
                ?: stringResource(R.string.not_available)
            stringResource(
                R.string.caffeine_sleep_status_elevated_now_body,
                formatMg(insights.currentMg, unitFormatter),
                timeToSafe,
                formatMg(insights.bedtimeMg, unitFormatter),
                insights.bedtime.toString(),
            )
        }
        CaffeineSleepImpactStatus.MAY_AFFECT_SLEEP -> stringResource(
            R.string.caffeine_sleep_status_may_affect_body,
            formatMg(insights.bedtimeMg, unitFormatter),
            insights.bedtime.toString(),
            formatMg(insights.sleepThresholdMg.toDouble(), unitFormatter),
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = if (color.luminance() > 0.5f) 0.12f else 0.18f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = color,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private enum class CaffeineSleepImpactStatus {
    UNLIKELY,
    ELEVATED_NOW,
    MAY_AFFECT_SLEEP,
}

private fun caffeineSleepImpactStatus(insights: CaffeineInsights): CaffeineSleepImpactStatus {
    val threshold = insights.sleepThresholdMg.toDouble()
    return when {
        insights.bedtimeMg > threshold -> CaffeineSleepImpactStatus.MAY_AFFECT_SLEEP
        insights.currentMg > threshold -> CaffeineSleepImpactStatus.ELEVATED_NOW
        else -> CaffeineSleepImpactStatus.UNLIKELY
    }
}

@Composable
private fun CaffeineAnalyticsRangePicker(
    selectedRange: CaffeineAnalyticsRange,
    onSelectRange: (CaffeineAnalyticsRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf(CaffeineAnalyticsRange.TODAY, CaffeineAnalyticsRange.YESTERDAY),
        listOf(CaffeineAnalyticsRange.LAST_30_DAYS, CaffeineAnalyticsRange.LAST_90_DAYS),
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        rows.forEach { ranges ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ranges.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { onSelectRange(range) },
                        label = { Text(range.displayLabel()) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CaffeineAnalyticsSummaryCard(
    insights: CaffeineInsights,
    range: CaffeineAnalyticsRange,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = range.displayLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatMg(insights.periodTotalMg, unitFormatter),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.caffeine_period_total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                CaffeineMiniStat(
                    title = stringResource(R.string.caffeine_daily_average),
                    value = formatMg(insights.periodAverageMg, unitFormatter),
                    icon = Icons.Outlined.QueryStats,
                    modifier = Modifier.weight(1f),
                )
                CaffeineMiniStat(
                    title = stringResource(R.string.caffeine_safe_nights),
                    value = "${insights.safeNights}/${insights.totalNights}",
                    icon = Icons.Outlined.NightsStay,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                CaffeineMiniStat(
                    title = stringResource(R.string.caffeine_top_source),
                    value = insights.sourceTotals.firstOrNull()?.label
                        ?: stringResource(R.string.not_available),
                    icon = Icons.Outlined.LocalDrink,
                    modifier = Modifier.weight(1f),
                )
                CaffeineMiniStat(
                    title = stringResource(R.string.caffeine_sleep_threshold),
                    value = formatMg(insights.sleepThresholdMg.toDouble(), unitFormatter),
                    icon = Icons.Outlined.Bedtime,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CaffeineMiniStat(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .background(
                color = color.copy(alpha = if (color.luminance() > 0.5f) 0.12f else 0.18f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CaffeineCurveCard(
    insights: CaffeineInsights,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onSelectEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val points = insights.curvePoints
    val entries = insights.entryInsights
    val zone = ZoneId.systemDefault()
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.caffeine_curve_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(12.dp))
            CaffeineLineChart(
                points = points,
                thresholdMg = insights.sleepThresholdMg.toDouble(),
                entryInsights = entries,
                onSelectEntry = onSelectEntry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                points.firstOrNull()?.let {
                    Text(
                        text = it.time.atZone(zone).format(dateTimeFormatterProvider.shortTime()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.caffeine_threshold_line,
                        formatMg(insights.sleepThresholdMg.toDouble(), unitFormatter),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                points.lastOrNull()?.let {
                    Text(
                        text = it.time.atZone(zone).format(dateTimeFormatterProvider.shortTime()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CaffeineLineChart(
    points: List<CaffeinePoint>,
    thresholdMg: Double,
    entryInsights: List<CaffeineEntryInsight>,
    onSelectEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val thresholdColor = MaterialTheme.colorScheme.error
    val markerColor = HydrationColor
    Canvas(
        modifier = modifier.pointerInput(points, entryInsights) {
            detectTapGestures { offset ->
                if (entryInsights.isEmpty() || points.size < 2) return@detectTapGestures
                val start = points.first().time.toEpochMilli()
                val end = points.last().time.toEpochMilli()
                val tappedTime = start + ((offset.x / size.width) * (end - start)).toLong()
                entryInsights
                    .minByOrNull { kotlin.math.abs(it.entry.startTime.toEpochMilli() - tappedTime) }
                    ?.takeIf { kotlin.math.abs(it.entry.startTime.toEpochMilli() - tappedTime) < Duration.ofHours(2).toMillis() }
                    ?.let { onSelectEntry(it.entry.id) }
            }
        },
    ) {
        if (points.size < 2) return@Canvas
        val startMillis = points.first().time.toEpochMilli()
        val endMillis = points.last().time.toEpochMilli().coerceAtLeast(startMillis + 1)
        val maxValue = maxOf(points.maxOf { it.valueMg }, thresholdMg, 1.0)
        fun xFor(time: Instant): Float =
            ((time.toEpochMilli() - startMillis).toFloat() / (endMillis - startMillis)) * size.width
        fun yFor(value: Double): Float =
            size.height - ((value / maxValue).toFloat().coerceIn(0f, 1f) * size.height)

        val thresholdY = yFor(thresholdMg)
        drawLine(
            color = thresholdColor.copy(alpha = 0.45f),
            start = Offset(0f, thresholdY),
            end = Offset(size.width, thresholdY),
            strokeWidth = 2.dp.toPx(),
        )
        val path = Path()
        points.forEachIndexed { index, point ->
            val offset = Offset(xFor(point.time), yFor(point.valueMg))
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
        entryInsights.forEach { insight ->
            val x = xFor(insight.entry.startTime)
            if (x in 0f..size.width) {
                drawCircle(
                    color = markerColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, size.height - 4.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun CaffeineSleepImpactCard(
    insights: CaffeineInsights,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.caffeine_bedtime_forecast),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = formatMg(insights.bedtimeMg, unitFormatter),
                style = MaterialTheme.typography.headlineMedium,
                color = if (insights.bedtimeMg <= insights.sleepThresholdMg) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.caffeine_bedtime_summary,
                    insights.bedtime.toString(),
                    formatMg(insights.sleepThresholdMg.toDouble(), unitFormatter),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                CaffeineMiniStat(
                    title = stringResource(R.string.caffeine_safe_nights),
                    value = "${insights.safeNights}/${insights.totalNights}",
                    icon = Icons.Outlined.NightsStay,
                    modifier = Modifier.weight(1f),
                )
                CaffeineMiniStat(
                    title = stringResource(R.string.caffeine_safe_streak),
                    value = unitFormatter.count(insights.safeSleepStreak),
                    icon = Icons.Outlined.Bedtime,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CaffeineDailyImpactCard(
    stats: List<CaffeineDailyStat>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    rangeLabel: String,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.caffeine_daily_impact),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(12.dp))
            CaffeineBarChart(
                stats = stats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.caffeine_safe_calendar),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            SafeNightCalendar(
                stats = stats,
                modifier = Modifier.padding(top = 8.dp),
            )
            stats.takeLast(7).forEach { stat ->
                DistributionRow(
                    label = stat.date.format(dateTimeFormatterProvider.chartDay()),
                    value = formatMg(stat.bedtimeMg, unitFormatter),
                    fraction = if (stats.maxOfOrNull { it.bedtimeMg } == 0.0) {
                        0.0
                    } else {
                        stat.bedtimeMg / stats.maxOf { it.bedtimeMg.coerceAtLeast(1.0) }
                    },
                    color = if (stat.safeForSleep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SafeNightCalendar(
    stats: List<CaffeineDailyStat>,
    modifier: Modifier = Modifier,
) {
    val safeColor = MaterialTheme.colorScheme.primary
    val unsafeColor = MaterialTheme.colorScheme.error
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        stats.takeLast(42).chunked(7).forEach { week ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                week.forEach { stat ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .background(
                                color = when {
                                    stat.totalMg <= 0.0 && stat.bedtimeMg <= 0.0 -> emptyColor
                                    stat.safeForSleep -> safeColor
                                    else -> unsafeColor
                                },
                                shape = RoundedCornerShape(6.dp),
                            ),
                    )
                }
                repeat(7 - week.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CaffeineBarChart(
    stats: List<CaffeineDailyStat>,
    modifier: Modifier = Modifier,
) {
    val totalColor = MaterialTheme.colorScheme.primary
    val bedtimeColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        if (stats.isEmpty()) return@Canvas
        val maxValue = stats.maxOf { maxOf(it.totalMg, it.bedtimeMg) }.coerceAtLeast(1.0)
        val slotWidth = size.width / stats.size
        stats.forEachIndexed { index, stat ->
            val center = index * slotWidth + slotWidth / 2f
            val barWidth = (slotWidth * 0.28f).coerceAtLeast(2.dp.toPx())
            val totalHeight = (stat.totalMg / maxValue).toFloat() * size.height
            val bedtimeHeight = (stat.bedtimeMg / maxValue).toFloat() * size.height
            drawRoundRect(
                color = totalColor,
                topLeft = Offset(center - barWidth - 1.dp.toPx(), size.height - totalHeight),
                size = Size(barWidth, totalHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
            drawRoundRect(
                color = bedtimeColor,
                topLeft = Offset(center + 1.dp.toPx(), size.height - bedtimeHeight),
                size = Size(barWidth, bedtimeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
        }
    }
}

@Composable
private fun CaffeineDistributionCard(
    title: String,
    slices: List<CaffeineDistributionSlice>,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (slices.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                val max = slices.maxOf { it.valueMg }.coerceAtLeast(1.0)
                slices.take(6).forEach { slice ->
                    DistributionRow(
                        label = slice.label,
                        value = formatMg(slice.valueMg, unitFormatter),
                        fraction = slice.valueMg / max,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CaffeineTimeBucketsCard(
    buckets: List<CaffeineTimeBucket>,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.caffeine_time_of_day),
                style = MaterialTheme.typography.titleSmall,
            )
            val max = buckets.maxOfOrNull { it.valueMg }?.coerceAtLeast(1.0) ?: 1.0
            buckets.forEach { bucket ->
                DistributionRow(
                    label = bucket.bucket.displayLabel(),
                    value = formatMg(bucket.valueMg, unitFormatter),
                    fraction = bucket.valueMg / max,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CaffeineScienceCard(modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.caffeine_science_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.caffeine_science_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.caffeine_science_measurements),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = stringResource(R.string.caffeine_science_measurements_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = stringResource(R.string.caffeine_science_limits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun CaffeineReferencesCard(modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.caffeine_references_title),
                style = MaterialTheme.typography.titleSmall,
            )
            ReferenceLinkButton(
                label = stringResource(R.string.caffeine_reference_drake),
                url = DrakeSleepStudyUrl,
                modifier = Modifier.padding(top = 10.dp),
            )
            ReferenceLinkButton(
                label = stringResource(R.string.caffeine_reference_nehlig),
                url = NehligMetabolismReviewUrl,
                modifier = Modifier.padding(top = 8.dp),
            )
            ReferenceLinkButton(
                label = stringResource(R.string.caffeine_reference_efsa),
                url = EfsaCaffeineTopicUrl,
                modifier = Modifier.padding(top = 8.dp),
            )
            ReferenceLinkButton(
                label = stringResource(R.string.caffeine_reference_health_connect),
                url = HealthConnectNutritionUrl,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ReferenceLinkButton(
    label: String,
    url: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    OutlinedButton(
        onClick = { uriHandler.openUri(url) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun DistributionRow(
    label: String,
    value: String,
    fraction: Double,
    color: Color,
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.toFloat().coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun CaffeineEntryRow(
    insight: CaffeineEntryInsight,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    OpenVitalsCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalDrink,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = insight.entry.name?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.caffeine_entry),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = insight.entry.startTime.atZone(zone).format(
                        dateTimeFormatterProvider.mediumDateTime()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.caffeine_inferred_category, insight.inferredCategory.displayLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                insight.catalogMatch?.let { match ->
                    Text(
                        text = stringResource(R.string.caffeine_catalog_match, match.item.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMg(insight.entry.caffeineMg, unitFormatter),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(
                        R.string.caffeine_current_contribution,
                        formatMg(insight.currentContributionMg, unitFormatter),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CaffeineContributionSheet(
    insight: CaffeineEntryInsight,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val unknownSource = stringResource(R.string.unknown_source)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = insight.entry.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.caffeine_entry),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = insight.entry.startTime.atZone(zone).format(dateTimeFormatterProvider.mediumDateTime()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            SheetMetadataRow(
                label = stringResource(R.string.caffeine_category),
                value = insight.inferredCategory.displayLabel,
            )
            insight.catalogMatch?.let { match ->
                SheetMetadataRow(
                    label = stringResource(R.string.caffeine_catalog),
                    value = stringResource(
                        R.string.caffeine_catalog_match_detail,
                        match.item.name,
                        formatMg(match.item.typicalCaffeineMg, unitFormatter),
                        match.confidence.displayLabel(),
                    ),
                )
            }
            SheetMetadataRow(
                label = stringResource(R.string.caffeine_health_connect_source_label),
                value = insight.entry.source.ifBlank { unknownSource },
            )
            SheetMetadataRow(
                label = stringResource(R.string.caffeine_health_connect_meal_label),
                value = mealTypeLabel(insight.entry.mealType),
            )
            SheetMetadataRow(
                label = stringResource(R.string.caffeine_health_connect_duration_label),
                value = formatDurationMinutes(
                    Duration.between(insight.entry.startTime, insight.entry.endTime)
                        .toMinutes()
                        .takeIf { it > 0L }
                        ?: CaffeinePreferences.DefaultConsumptionDurationMinutes.toLong(),
                ),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            CaffeineMiniStat(
                title = stringResource(R.string.caffeine_dose),
                value = formatMg(insight.entry.caffeineMg, unitFormatter),
                icon = Icons.Outlined.LocalDrink,
                modifier = Modifier.weight(1f),
            )
            CaffeineMiniStat(
                title = stringResource(R.string.caffeine_peak),
                value = formatMg(insight.peakMg, unitFormatter),
                icon = Icons.Outlined.QueryStats,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            CaffeineMiniStat(
                title = stringResource(R.string.caffeine_current_contribution_label),
                value = formatMg(insight.currentContributionMg, unitFormatter),
                icon = Icons.Outlined.LocalDrink,
                modifier = Modifier.weight(1f),
            )
            CaffeineMiniStat(
                title = stringResource(R.string.caffeine_peak_time),
                value = insight.peakTime.atZone(zone).format(dateTimeFormatterProvider.shortTime()),
                icon = Icons.Outlined.QueryStats,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.caffeine_contribution_curve),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        CaffeineLineChart(
            points = insight.contributionPoints,
            thresholdMg = 0.0,
            entryInsights = emptyList(),
            onSelectEntry = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun SheetMetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.36f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.64f),
        )
    }
}

@Composable
private fun EmptyCaffeineCard(modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.caffeine_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun formatMg(value: Double, unitFormatter: UnitFormatter): String =
    "${unitFormatter.count(value.roundToInt())} mg"

private fun formatDurationMinutes(minutes: Long): String =
    if (minutes < 60L) {
        "${minutes.coerceAtLeast(0)} min"
    } else {
        val hours = minutes / 60L
        val remaining = minutes % 60L
        if (remaining == 0L) "${hours}h" else "${hours}h ${remaining}m"
    }

private fun CaffeineAnalyticsRange.displayLabel(): String = when (this) {
    CaffeineAnalyticsRange.TODAY -> "Today"
    CaffeineAnalyticsRange.YESTERDAY -> "Yesterday"
    CaffeineAnalyticsRange.LAST_30_DAYS -> "Last 30 days"
    CaffeineAnalyticsRange.LAST_90_DAYS -> "Last 90 days"
}

private fun CaffeineTimeOfDayBucket.displayLabel(): String = when (this) {
    CaffeineTimeOfDayBucket.MORNING -> "Morning"
    CaffeineTimeOfDayBucket.AFTERNOON -> "Afternoon"
    CaffeineTimeOfDayBucket.EVENING -> "Evening"
    CaffeineTimeOfDayBucket.NIGHT -> "Night"
}

@Composable
private fun mealTypeLabel(mealType: Int): String = stringResource(
    when (mealType) {
        MealType.MEAL_TYPE_BREAKFAST -> R.string.meal_breakfast
        MealType.MEAL_TYPE_LUNCH -> R.string.meal_lunch
        MealType.MEAL_TYPE_DINNER -> R.string.meal_dinner
        MealType.MEAL_TYPE_SNACK -> R.string.meal_snack
        else -> R.string.meal_generic
    }
)

private fun CaffeineCatalogMatchConfidence.displayLabel(): String = when (this) {
    CaffeineCatalogMatchConfidence.EXACT -> "exact"
    CaffeineCatalogMatchConfidence.ALIAS -> "alias"
    CaffeineCatalogMatchConfidence.CONTAINS -> "contains"
}

private const val DrakeSleepStudyUrl = "https://jcsm.aasm.org/doi/10.5664/jcsm.3170"
private const val NehligMetabolismReviewUrl = "https://pubmed.ncbi.nlm.nih.gov/29514871/"
private const val EfsaCaffeineTopicUrl = "https://www.efsa.europa.eu/en/topics/topic/caffeine"
private const val HealthConnectNutritionUrl =
    "https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/NutritionRecord"
