package tech.mmarca.openvitals.features.readiness

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressConfidence
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressEstimate
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressLevel
import tech.mmarca.openvitals.ui.components.DayNavigator
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.MindfulnessColor
import tech.mmarca.openvitals.ui.theme.SleepColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
fun StressDetailsScreen(
    viewModel: DailyReadinessViewModel,
    selectedDate: LocalDate,
    onGrantPermissions: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) {
        if (state.selectedDate != selectedDate) {
            viewModel.selectDate(selectedDate)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.insight != null,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.insight == null -> FullScreenLoading()
            state.errorMessage != null && state.insight == null ->
                ErrorMessage(state.errorMessage ?: stringResource(R.string.unknown_error))
            state.insight != null -> StressDetailsContent(
                state = state,
                canGoForward = state.selectedDate.isBefore(LocalDate.now()),
                onPreviousDay = viewModel::previousDay,
                onNextDay = viewModel::nextDay,
                onOpenCalendar = { showDatePicker = true },
                onGrantPermissions = {
                    viewModel.acknowledgePermissionsCallout()
                    onGrantPermissions()
                },
                onDismissPermissionsCallout = viewModel::acknowledgePermissionsCallout,
            )
            else -> ErrorMessage(stringResource(R.string.message_no_dashboard_data))
        }
    }

    if (showDatePicker) {
        HealthDatePickerDialog(
            selectedDate = state.selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                viewModel.selectDate(date)
            },
        )
    }
}

@Composable
private fun StressDetailsContent(
    state: DailyReadinessUiState,
    canGoForward: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onGrantPermissions: () -> Unit,
    onDismissPermissionsCallout: () -> Unit,
) {
    val stress = state.insight?.physiologicalStress ?: return
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DayNavigator(
                    date = state.selectedDate,
                    canGoForward = canGoForward,
                    onPreviousDay = onPreviousDay,
                    onNextDay = onNextDay,
                    onOpenCalendar = onOpenCalendar,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (state.showPermissionsCallout) {
                item {
                    PermissionCallout(
                        title = stringResource(R.string.message_missing_permissions_title),
                        body = stringResource(R.string.message_missing_permissions_body),
                        onGrant = onGrantPermissions,
                        onDismiss = onDismissPermissionsCallout,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            item {
                StressScoreCard(
                    stress = stress,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                StressExplanationCard(
                    stress = stress,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                StressListCard(
                    title = stringResource(R.string.stress_details_inputs),
                    items = stress.contributingFactors.ifEmpty {
                        listOf(stringResource(R.string.stress_details_no_inputs))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                StressListCard(
                    title = stringResource(R.string.stress_details_data_coverage),
                    items = stress.dataCoverage.ifEmpty {
                        listOf(stringResource(R.string.stress_details_no_data_coverage))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                StressListCard(
                    title = stringResource(R.string.stress_details_caveats),
                    items = stress.caveats,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun StressScoreCard(
    stress: PhysiologicalStressEstimate,
    modifier: Modifier = Modifier,
) {
    val accent = stressColor(stress.level)
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accent.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = accent,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.screen_stress_tracking),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stressConfidenceText(stress.confidence, stress.confidenceReason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stress.score?.let { "$it/100" } ?: "--",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            Text(
                text = stress.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
            Text(
                text = stress.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StressExplanationCard(
    stress: PhysiologicalStressEstimate,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.stress_details_how_tracked),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.stress_details_how_tracked_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.stress_details_scale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (stress.level == PhysiologicalStressLevel.MEDIUM || stress.level == PhysiologicalStressLevel.HIGH) {
                StressGuidanceRow()
            }
        }
    }
}

@Composable
private fun StressGuidanceRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MindfulnessColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SelfImprovement,
                contentDescription = null,
                tint = MindfulnessColor,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.stress_details_relaxation_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StressListCard(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun stressColor(level: PhysiologicalStressLevel): Color =
    when (level) {
        PhysiologicalStressLevel.RESTING -> SleepColor
        PhysiologicalStressLevel.LOW -> WorkoutColor
        PhysiologicalStressLevel.MEDIUM -> HeartColor
        PhysiologicalStressLevel.HIGH -> VitalsColor
        PhysiologicalStressLevel.NEEDS_MORE_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun stressConfidenceText(
    confidence: PhysiologicalStressConfidence,
    reason: String,
): String {
    val label = when (confidence) {
        PhysiologicalStressConfidence.HIGH -> "High confidence"
        PhysiologicalStressConfidence.MEDIUM -> "Medium confidence"
        PhysiologicalStressConfidence.LOW -> "Low confidence"
        PhysiologicalStressConfidence.NO_DATA -> "No stress estimate"
    }
    val reasonLabel = when (reason) {
        "hrv_resting_hr_average_hr" -> "HRV, resting HR, and average HR"
        "partial_hrv_or_heart_rate_context" -> "partial HRV or heart-rate context"
        "activity_may_influence" -> "activity may influence"
        "single_signal" -> "single local signal"
        else -> "needs more local data"
    }
    return "$label · $reasonLabel"
}
