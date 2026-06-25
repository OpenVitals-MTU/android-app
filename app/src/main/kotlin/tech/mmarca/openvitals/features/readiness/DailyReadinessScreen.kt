package tech.mmarca.openvitals.features.readiness

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.DailyReadinessFactor
import tech.mmarca.openvitals.domain.insights.DailyReadinessInsight
import tech.mmarca.openvitals.domain.insights.ReadinessConfidence
import tech.mmarca.openvitals.domain.insights.ReadinessFactorImpact
import tech.mmarca.openvitals.domain.insights.ReadinessState
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
fun DailyReadinessScreen(
    viewModel: DailyReadinessViewModel,
    onGrantPermissions: () -> Unit,
    onOpenBodyEnergyDetails: (LocalDate) -> Unit,
    onOpenTrainingReadinessDetails: (LocalDate) -> Unit,
    onOpenStressDetails: (LocalDate) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentDay()
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
            state.insight != null -> DailyReadinessContent(
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
                onOpenBodyEnergyDetails = { onOpenBodyEnergyDetails(state.selectedDate) },
                onOpenTrainingReadinessDetails = { onOpenTrainingReadinessDetails(state.selectedDate) },
                onOpenStressDetails = { onOpenStressDetails(state.selectedDate) },
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
private fun DailyReadinessContent(
    state: DailyReadinessUiState,
    canGoForward: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onGrantPermissions: () -> Unit,
    onDismissPermissionsCallout: () -> Unit,
    onOpenBodyEnergyDetails: () -> Unit,
    onOpenTrainingReadinessDetails: () -> Unit,
    onOpenStressDetails: () -> Unit,
) {
    val insight = state.insight ?: return
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            item {
                DailyReadinessPanel(
                    insight = insight,
                    onOpenBodyEnergyDetails = onOpenBodyEnergyDetails,
                    onOpenTrainingReadinessDetails = onOpenTrainingReadinessDetails,
                    onOpenStressDetails = onOpenStressDetails,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun DailyReadinessPanel(
    insight: DailyReadinessInsight,
    onOpenBodyEnergyDetails: () -> Unit,
    onOpenTrainingReadinessDetails: () -> Unit,
    onOpenStressDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = readinessAccentColor(insight.state)
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SelfImprovement,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dashboard_readiness_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = confidenceText(insight.confidence, insight.confidenceReason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DailyReadinessScore(score = insight.score, accentColor = accentColor)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = insight.statusTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
                Text(
                    text = insight.recommendation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = insight.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DailyReadinessMetric(
                    label = stringResource(R.string.dashboard_readiness_body_energy),
                    value = "${insight.bodyEnergyScore}/100",
                    icon = Icons.Outlined.Favorite,
                    color = HeartColor,
                    onClick = onOpenBodyEnergyDetails,
                    modifier = Modifier.weight(1f),
                )
                DailyReadinessMetric(
                    label = stringResource(R.string.dashboard_readiness_training),
                    value = "${insight.trainingReadinessScore}/100",
                    icon = Icons.Outlined.FitnessCenter,
                    color = WorkoutColor,
                    onClick = onOpenTrainingReadinessDetails,
                    modifier = Modifier.weight(1f),
                )
            }

            DailyReadinessInlineInfo(
                label = stringResource(R.string.dashboard_readiness_hrv_status),
                value = "${insight.hrvStatus.label} · ${insight.hrvStatus.detail}",
            )
            DailyReadinessInlineInfo(
                label = stringResource(R.string.dashboard_readiness_intensity_minutes),
                value = "${insight.intensityMinutes.label} · ${insight.intensityMinutes.detail}",
            )
            DailyReadinessTappableInfo(
                label = stringResource(R.string.dashboard_readiness_stress_level),
                value = stressValue(insight),
                onClick = onOpenStressDetails,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

            DailyReadinessGuidanceRow(
                label = stringResource(R.string.dashboard_readiness_recommended),
                value = insight.suggestedWorkout,
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                color = accentColor,
            )
            DailyReadinessGuidanceRow(
                label = stringResource(R.string.dashboard_readiness_avoid),
                value = insight.avoid,
                icon = Icons.Outlined.Close,
                color = MaterialTheme.colorScheme.error,
            )
            DailyReadinessGuidanceRow(
                label = stringResource(R.string.dashboard_readiness_alternative),
                value = insight.alternative,
                icon = Icons.Outlined.SelfImprovement,
                color = MindfulnessColor,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DailyReadinessInlineInfo(
                    label = stringResource(R.string.dashboard_readiness_strain),
                    value = listOfNotNull(insight.strainTarget, insight.currentStrain)
                        .joinToString(separator = " · "),
                )
                DailyReadinessInlineInfo(
                    label = stringResource(R.string.dashboard_readiness_goal),
                    value = insight.adaptiveGoal,
                )
                if (insight.recoveryModeSuggested) {
                    DailyReadinessInlineInfo(
                        label = stringResource(R.string.dashboard_readiness_recovery_mode),
                        value = stringResource(R.string.dashboard_readiness_recovery_mode_body),
                    )
                }
            }

            DailyReadinessFactors(factors = insight.factors.take(5))
        }
    }
}

@Composable
private fun DailyReadinessScore(
    score: Int,
    accentColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.dashboard_readiness_score),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$score/100",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor,
        )
    }
}

@Composable
private fun DailyReadinessMetric(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.10f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DailyReadinessGuidanceRow(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyReadinessInlineInfo(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DailyReadinessTappableInfo(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun DailyReadinessFactors(
    factors: List<DailyReadinessFactor>,
) {
    if (factors.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.dashboard_readiness_why),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        factors.forEach { factor ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .background(factorImpactColor(factor.impact), CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = factor.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = factor.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun stressValue(insight: DailyReadinessInsight): String {
    val stress = insight.physiologicalStress
    val score = stress.score?.let { " · $it/100" }.orEmpty()
    return "${stress.label}$score · ${stress.summary}"
}

@Composable
private fun readinessAccentColor(state: ReadinessState): Color =
    when (state) {
        ReadinessState.READY -> WorkoutColor
        ReadinessState.MODERATE -> HeartColor
        ReadinessState.RECOVER -> SleepColor
        ReadinessState.REST -> VitalsColor
        ReadinessState.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun factorImpactColor(impact: ReadinessFactorImpact): Color =
    when (impact) {
        ReadinessFactorImpact.POSITIVE -> WorkoutColor
        ReadinessFactorImpact.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        ReadinessFactorImpact.NEGATIVE -> SleepColor
        ReadinessFactorImpact.WARNING -> MaterialTheme.colorScheme.error
    }

private fun confidenceText(
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
