package tech.mmarca.openvitals.features.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.ui.components.FullScreenLoading

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
                        body = state.error?.resolve(),
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
