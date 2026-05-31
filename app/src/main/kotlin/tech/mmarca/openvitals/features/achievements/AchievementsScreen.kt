package tech.mmarca.openvitals.features.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import kotlin.math.roundToLong
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.StepsColor

@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading && state.badges.isEmpty()) {
        FullScreenLoading()
        return
    }

    var selectedCategory by rememberSaveable { mutableStateOf<AchievementCategory?>(null) }
    val filteredBadges = state.badges.filter { progress ->
        selectedCategory == null || progress.definition.category == selectedCategory
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                AchievementSummaryCard(
                    state = state,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item {
                AchievementStatsRow(
                    stats = state.stats,
                    unitFormatter = unitFormatter,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                AchievementFilters(
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.error != null) {
                item {
                    MessageCard(
                        title = stringResource(R.string.achievements_error_title),
                        body = state.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else if (!state.hasActivityHistory) {
                item {
                    MessageCard(
                        title = stringResource(R.string.achievements_no_data_title),
                        body = stringResource(R.string.achievements_no_data_body),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (!state.hasFloorHistory && state.hasActivityHistory) {
                item {
                    MessageCard(
                        title = stringResource(R.string.achievements_no_floor_data_title),
                        body = stringResource(R.string.achievements_no_floor_data_body),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            items(
                items = filteredBadges,
                key = { it.definition.id },
            ) { progress ->
                AchievementBadgeCard(
                    progress = progress,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun AchievementSummaryCard(
    state: AchievementsUiState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressText = stringResource(
        R.string.achievements_progress_summary,
        state.unlockedCount,
        state.totalCount,
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBadge(
                    icon = Icons.Outlined.WorkspacePremium,
                    color = MaterialTheme.colorScheme.primary,
                    isUnlocked = true,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.achievements_legacy_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { state.completionRatio },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(
                    R.string.achievements_data_window,
                    state.stats.startDate.format(dateTimeFormatterProvider.mediumDate()),
                    state.stats.endDate.format(dateTimeFormatterProvider.mediumDate()),
                    unitFormatter.count(state.stats.trackedDays),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AchievementStatsRow(
    stats: AchievementStats,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            AchievementStatCard(
                label = stringResource(R.string.achievements_tracked_days),
                value = unitFormatter.count(stats.trackedDays),
            )
        }
        item {
            AchievementStatCard(
                label = stringResource(R.string.achievements_best_steps),
                value = unitFormatter.count(stats.maxDailySteps),
            )
        }
        item {
            AchievementStatCard(
                label = stringResource(R.string.achievements_total_distance),
                value = unitFormatter.distance(stats.totalDistanceMeters).text,
            )
        }
        item {
            AchievementStatCard(
                label = stringResource(R.string.achievements_best_floors),
                value = unitFormatter.count(stats.maxDailyFloors),
            )
        }
        item {
            AchievementStatCard(
                label = stringResource(R.string.achievements_total_floors),
                value = unitFormatter.count(stats.totalFloors),
            )
        }
    }
}

@Composable
private fun AchievementStatCard(
    label: String,
    value: String,
) {
    Card(
        modifier = Modifier.width(156.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AchievementFilters(
    selectedCategory: AchievementCategory?,
    onSelectCategory: (AchievementCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = listOf<AchievementCategory?>(null) + AchievementCategory.values()
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelectCategory(category) },
                label = {
                    Text(
                        text = category?.label() ?: stringResource(R.string.achievements_filter_all),
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun AchievementBadgeCard(
    progress: AchievementProgress,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val category = progress.definition.category
    val color = category.color()
    val targetText = progress.definition.targetDisplay(unitFormatter)
    val currentText = progress.currentDisplay(unitFormatter)
    val achievedOn = progress.achievedOn
    val containerColor = if (progress.isUnlocked) {
        color.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBadge(
                    icon = category.icon(),
                    color = color,
                    isUnlocked = progress.isUnlocked,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = progress.definition.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (progress.isUnlocked) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (progress.isUnlocked) color else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = progress.definition.requirementText(targetText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            BadgeProgressBar(
                progress = progress.progressRatio,
                color = color,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.achievements_progress_value, currentText, targetText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = progress.statusText(achievedOn, dateTimeFormatterProvider),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (progress.isUnlocked) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    color: Color,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (isUnlocked) 0.20f else 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun BadgeProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(color),
        )
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!body.isNullOrBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AchievementCategory.label(): String =
    when (this) {
        AchievementCategory.DAILY_STEPS -> stringResource(R.string.achievements_category_daily_steps)
        AchievementCategory.LIFETIME_DISTANCE -> stringResource(R.string.achievements_category_lifetime_distance)
        AchievementCategory.DAILY_FLOORS -> stringResource(R.string.achievements_category_daily_floors)
        AchievementCategory.LIFETIME_FLOORS -> stringResource(R.string.achievements_category_lifetime_floors)
    }

private fun AchievementCategory.icon(): ImageVector =
    when (this) {
        AchievementCategory.DAILY_STEPS -> Icons.AutoMirrored.Outlined.DirectionsWalk
        AchievementCategory.LIFETIME_DISTANCE -> Icons.Outlined.Straighten
        AchievementCategory.DAILY_FLOORS -> Icons.Outlined.Stairs
        AchievementCategory.LIFETIME_FLOORS -> Icons.Outlined.WorkspacePremium
    }

private fun AchievementCategory.color(): Color =
    when (this) {
        AchievementCategory.DAILY_STEPS -> StepsColor
        AchievementCategory.LIFETIME_DISTANCE -> DistanceColor
        AchievementCategory.DAILY_FLOORS -> FloorsColor
        AchievementCategory.LIFETIME_FLOORS -> ElevationColor
    }

@Composable
private fun AchievementDefinition.requirementText(targetText: String): String =
    when (metric) {
        AchievementMetric.DAILY_STEPS -> stringResource(R.string.achievements_daily_steps_requirement, targetText)
        AchievementMetric.LIFETIME_DISTANCE_METERS ->
            stringResource(R.string.achievements_lifetime_distance_requirement, targetText)
        AchievementMetric.DAILY_FLOORS -> stringResource(R.string.achievements_daily_floors_requirement, targetText)
        AchievementMetric.LIFETIME_FLOORS -> stringResource(R.string.achievements_lifetime_floors_requirement, targetText)
    }

private fun AchievementDefinition.targetDisplay(unitFormatter: UnitFormatter): String =
    when (metric) {
        AchievementMetric.DAILY_STEPS -> unitFormatter.count(target.roundToLong())
        AchievementMetric.LIFETIME_DISTANCE_METERS -> unitFormatter.distance(target).text
        AchievementMetric.DAILY_FLOORS -> unitFormatter.count(target.roundToLong())
        AchievementMetric.LIFETIME_FLOORS -> unitFormatter.count(target.roundToLong())
    }

private fun AchievementProgress.currentDisplay(unitFormatter: UnitFormatter): String =
    when (definition.metric) {
        AchievementMetric.DAILY_STEPS -> unitFormatter.count(currentValue.roundToLong())
        AchievementMetric.LIFETIME_DISTANCE_METERS -> unitFormatter.distance(currentValue).text
        AchievementMetric.DAILY_FLOORS -> unitFormatter.count(currentValue.roundToLong())
        AchievementMetric.LIFETIME_FLOORS -> unitFormatter.count(currentValue.roundToLong())
    }

@Composable
private fun AchievementProgress.statusText(
    achievedOn: LocalDate?,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String =
    when {
        timesEarned > 1 -> stringResource(R.string.achievements_earned_times, timesEarned)
        isUnlocked && achievedOn != null -> stringResource(
            R.string.achievements_achieved_on,
            achievedOn.format(dateTimeFormatterProvider.mediumDate()),
        )
        isUnlocked -> stringResource(R.string.achievements_earned_once)
        else -> stringResource(R.string.achievements_locked)
    }
