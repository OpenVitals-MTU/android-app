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
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.insights.DailyReadinessFactor
import tech.mmarca.openvitals.domain.insights.DailyReadinessInsight
import tech.mmarca.openvitals.domain.insights.ReadinessConfidence
import tech.mmarca.openvitals.domain.insights.ReadinessFactorKind
import tech.mmarca.openvitals.domain.insights.ReadinessState
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.DayNavigator
import tech.mmarca.openvitals.ui.components.DataSourceEducationItem
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor

enum class ReadinessScoreDetailKind {
    BODY_ENERGY,
    TRAINING_READINESS,
}

@Composable
fun BodyEnergyDetailsScreen(
    viewModel: DailyReadinessViewModel,
    selectedDate: LocalDate,
) {
    ReadinessScoreDetailsScreen(
        viewModel = viewModel,
        selectedDate = selectedDate,
        kind = ReadinessScoreDetailKind.BODY_ENERGY,
    )
}

@Composable
fun TrainingReadinessDetailsScreen(
    viewModel: DailyReadinessViewModel,
    selectedDate: LocalDate,
) {
    ReadinessScoreDetailsScreen(
        viewModel = viewModel,
        selectedDate = selectedDate,
        kind = ReadinessScoreDetailKind.TRAINING_READINESS,
    )
}

@Composable
private fun ReadinessScoreDetailsScreen(
    viewModel: DailyReadinessViewModel,
    selectedDate: LocalDate,
    kind: ReadinessScoreDetailKind,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) {
        if (state.selectedDate != selectedDate) {
            viewModel.selectDate(selectedDate)
        }
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.READINESS,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { _ ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.insight != null,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.isLoading && state.insight == null -> FullScreenLoading()
                state.error != null && state.insight == null ->
                    ErrorMessage(state.error?.resolve() ?: stringResource(R.string.unknown_error))
                state.insight != null -> ReadinessScoreDetailsContent(
                    state = state,
                    kind = kind,
                    canGoForward = state.selectedDate.isBefore(LocalDate.now()),
                    onPreviousDay = viewModel::previousDay,
                    onNextDay = viewModel::nextDay,
                    onOpenCalendar = { showDatePicker = true },
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
}

@Composable
private fun ReadinessScoreDetailsContent(
    state: DailyReadinessUiState,
    kind: ReadinessScoreDetailKind,
    canGoForward: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val insight = state.insight ?: return
    val spec = readinessDetailSpec(kind, insight)
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

            item {
                ReadinessScoreCard(
                    spec = spec,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                ReadinessExplanationCard(
                    body = stringResource(spec.explanationRes),
                    scale = stringResource(spec.scaleRes),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                ReadinessListCard(
                    title = stringResource(R.string.readiness_details_signals_used),
                    items = spec.factors.map { "${it.label}: ${it.detail}" }.ifEmpty {
                        listOf(stringResource(spec.noSignalsRes))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                ReadinessListCard(
                    title = stringResource(R.string.readiness_details_guidance),
                    items = spec.guidance,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                ReadinessListCard(
                    title = stringResource(R.string.readiness_details_caveats),
                    items = listOf(
                        stringResource(R.string.readiness_details_caveat_local),
                        stringResource(R.string.readiness_details_caveat_not_medical),
                        stringResource(R.string.readiness_details_caveat_missing_data),
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                DataSourceEducationItem()
            }
        }
    }
}

@Composable
private fun ReadinessScoreCard(
    spec: ReadinessDetailSpec,
    modifier: Modifier = Modifier,
) {
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
                        .background(spec.color.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = spec.color,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(spec.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = readinessConfidenceText(spec.confidence, spec.confidenceReason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${spec.score}/100",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = spec.color,
                )
            }
            Text(
                text = scoreBandLabel(spec.score, spec.isUnknown),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = spec.color,
            )
            Text(
                text = stringResource(spec.summaryRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReadinessExplanationCard(
    body: String,
    scale: String,
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
                    text = stringResource(R.string.readiness_details_how_calculated),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = scale,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReadinessListCard(
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
private fun scoreBandLabel(score: Int, isUnknown: Boolean): String =
    stringResource(
        when {
            isUnknown -> R.string.readiness_details_score_needs_more_data
            score >= 80 -> R.string.readiness_details_score_strong
            score >= 60 -> R.string.readiness_details_score_steady
            score >= 40 -> R.string.readiness_details_score_limited
            else -> R.string.readiness_details_score_low
        }
    )

@Composable
private fun readinessDetailSpec(
    kind: ReadinessScoreDetailKind,
    insight: DailyReadinessInsight,
): ReadinessDetailSpec =
    when (kind) {
        ReadinessScoreDetailKind.BODY_ENERGY -> ReadinessDetailSpec(
            titleRes = R.string.screen_body_energy,
            explanationRes = R.string.body_energy_details_how_calculated_body,
            scaleRes = R.string.body_energy_details_scale,
            summaryRes = R.string.body_energy_details_summary,
            noSignalsRes = R.string.body_energy_details_no_signals,
            score = insight.bodyEnergyScore,
            isUnknown = insight.state == ReadinessState.UNKNOWN,
            color = HeartColor,
            icon = Icons.Outlined.Favorite,
            confidence = insight.confidence,
            confidenceReason = insight.confidenceReason,
            factors = insight.factors.filter { it.kind in BodyEnergyFactorKinds },
            guidance = listOf(
                "${stringResource(R.string.dashboard_readiness_recommended)}: ${insight.recommendation}",
                "${stringResource(R.string.dashboard_readiness_goal)}: ${insight.adaptiveGoal}",
                "${stringResource(R.string.dashboard_readiness_alternative)}: ${insight.alternative}",
            ),
        )
        ReadinessScoreDetailKind.TRAINING_READINESS -> ReadinessDetailSpec(
            titleRes = R.string.screen_training_readiness,
            explanationRes = R.string.training_readiness_details_how_calculated_body,
            scaleRes = R.string.training_readiness_details_scale,
            summaryRes = R.string.training_readiness_details_summary,
            noSignalsRes = R.string.training_readiness_details_no_signals,
            score = insight.trainingReadinessScore,
            isUnknown = insight.state == ReadinessState.UNKNOWN,
            color = WorkoutColor,
            icon = Icons.Outlined.FitnessCenter,
            confidence = insight.confidence,
            confidenceReason = insight.confidenceReason,
            factors = insight.factors.filter { it.kind in TrainingReadinessFactorKinds },
            guidance = listOfNotNull(
                "${stringResource(R.string.dashboard_readiness_recommended)}: ${insight.suggestedWorkout}",
                "${stringResource(R.string.dashboard_readiness_avoid)}: ${insight.avoid}",
                listOfNotNull(insight.strainTarget, insight.currentStrain)
                    .joinToString(separator = " · ")
                    .takeIf { it.isNotBlank() }
                    ?.let { "${stringResource(R.string.dashboard_readiness_strain)}: $it" },
            ),
        )
    }

private data class ReadinessDetailSpec(
    val titleRes: Int,
    val explanationRes: Int,
    val scaleRes: Int,
    val summaryRes: Int,
    val noSignalsRes: Int,
    val score: Int,
    val isUnknown: Boolean,
    val color: Color,
    val icon: ImageVector,
    val confidence: ReadinessConfidence,
    val confidenceReason: String,
    val factors: List<DailyReadinessFactor>,
    val guidance: List<String>,
)

private val BodyEnergyFactorKinds = setOf(
    ReadinessFactorKind.SLEEP_BELOW_BASELINE,
    ReadinessFactorKind.SLEEP_ABOVE_BASELINE,
    ReadinessFactorKind.RESTING_HR_ELEVATED,
    ReadinessFactorKind.RESTING_HR_NORMAL,
    ReadinessFactorKind.HRV_BELOW_BASELINE,
    ReadinessFactorKind.HRV_ABOVE_BASELINE,
    ReadinessFactorKind.HRV_NORMAL,
    ReadinessFactorKind.PHYSIOLOGICAL_STRESS_HIGH,
    ReadinessFactorKind.PHYSIOLOGICAL_STRESS_LOW,
    ReadinessFactorKind.STRESS_HIGH,
    ReadinessFactorKind.STRESS_LOW,
    ReadinessFactorKind.TEMPERATURE_ELEVATED,
    ReadinessFactorKind.HYDRATION_LOW,
    ReadinessFactorKind.NUTRITION_LOGGED,
    ReadinessFactorKind.MISSING_SLEEP_DATA,
    ReadinessFactorKind.MISSING_HRV_DATA,
    ReadinessFactorKind.MISSING_STRESS_DATA,
    ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE,
)

private val TrainingReadinessFactorKinds = setOf(
    ReadinessFactorKind.SLEEP_BELOW_BASELINE,
    ReadinessFactorKind.SLEEP_ABOVE_BASELINE,
    ReadinessFactorKind.RESTING_HR_ELEVATED,
    ReadinessFactorKind.RESTING_HR_NORMAL,
    ReadinessFactorKind.HRV_BELOW_BASELINE,
    ReadinessFactorKind.HRV_ABOVE_BASELINE,
    ReadinessFactorKind.HRV_NORMAL,
    ReadinessFactorKind.TRAINING_LOAD_HIGH,
    ReadinessFactorKind.TRAINING_LOAD_NORMAL,
    ReadinessFactorKind.INTENSITY_MINUTES_ON_TARGET,
    ReadinessFactorKind.INTENSITY_MINUTES_BEHIND,
    ReadinessFactorKind.PHYSIOLOGICAL_STRESS_HIGH,
    ReadinessFactorKind.PHYSIOLOGICAL_STRESS_LOW,
    ReadinessFactorKind.STRESS_HIGH,
    ReadinessFactorKind.TEMPERATURE_ELEVATED,
    ReadinessFactorKind.MISSING_SLEEP_DATA,
    ReadinessFactorKind.MISSING_HRV_DATA,
    ReadinessFactorKind.MISSING_INTENSITY_MINUTES,
    ReadinessFactorKind.MISSING_STRESS_DATA,
    ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE,
)

@Composable
private fun readinessConfidenceText(
    confidence: ReadinessConfidence,
    reason: String,
): String {
    val label = when (confidence) {
        ReadinessConfidence.HIGH -> "High confidence"
        ReadinessConfidence.MEDIUM -> "Medium confidence"
        ReadinessConfidence.LOW -> "Low confidence"
    }
    val reasonLabel = when (reason) {
        "complete_data" -> "complete local data"
        "missing_sleep_data" -> "sleep data missing"
        "missing_hrv_data" -> "HRV data missing"
        "new_user_not_enough_baseline" -> "baseline still building"
        else -> "partial local data"
    }
    return "$label · $reasonLabel"
}
