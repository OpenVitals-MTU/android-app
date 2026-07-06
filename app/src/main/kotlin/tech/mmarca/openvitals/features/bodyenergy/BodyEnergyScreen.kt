package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.insights.BodyEnergyCalibrationMode
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen

@Composable
fun BodyEnergyDetailsScreen(
    viewModel: BodyEnergyViewModel,
    selectedDate: LocalDate,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(selectedDate) {
        if (state.selectedDate != selectedDate) {
            viewModel.selectDate(selectedDate)
        }
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.BODY_ENERGY,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { uxState ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            screenError = state.error,
            onRefresh = viewModel::refresh,
            onSelectRange = viewModel::selectRange,
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            showTimeRangeSelector = false,
            syncPaused = uxState.syncPaused,
        ) {
            if (!state.calibration.setupCompleted) {
                item {
                    BodyEnergyCalibrationCard(
                        calibration = state.calibration,
                        onSave = viewModel::completeSetup,
                        onUseAutomatic = viewModel::useAutomatic,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                return@MetricDetailScaffold
            }

            val result = state.result
            if (result == null && state.error != null) {
                item {
                    ErrorMessage(
                        message = state.error.resolve() ?: stringResource(R.string.unknown_error),
                    )
                }
                return@MetricDetailScaffold
            }
            if (result == null) return@MetricDetailScaffold
            bodyEnergyContent(
                display = state.display,
            )
        }
    }
}

private fun LazyListScope.bodyEnergyContent(
    display: BodyEnergyDisplayState,
) {
    item {
        BodyEnergySummaryCard(
            timeline = display.timeline,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        BodyEnergyDayTimelineCard(
            display = display,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        BodyEnergyReasonsCard(
            reasons = display.topReasons,
            hasTimeline = !display.isEmpty,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        BodyEnergyInputsCard(
            display = display,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        BodyEnergyCalculationCard(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    if (display.timeline?.confidence == BodyEnergyConfidence.LOW) {
        item {
            OpenVitalsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.body_energy_timeline_low_confidence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun BodyEnergySummaryCard(
    timeline: BodyEnergyTimeline?,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.BatteryChargingFull,
                    contentDescription = null,
                    tint = bodyEnergyColor(timeline?.currentScore),
                )
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.screen_body_energy),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.body_energy_timeline_estimated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = timeline?.currentScore?.toString() ?: "--",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = bodyEnergyColor(timeline?.currentScore),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BodyEnergyStat(
                    label = stringResource(R.string.body_energy_timeline_start),
                    value = timeline?.startScore?.toString() ?: "--",
                    modifier = Modifier.weight(1f),
                )
                BodyEnergyStat(
                    label = stringResource(R.string.body_energy_timeline_charged),
                    value = "+${timeline?.charged ?: 0}",
                    modifier = Modifier.weight(1f),
                )
                BodyEnergyStat(
                    label = stringResource(R.string.body_energy_timeline_drained),
                    value = "-${timeline?.drained ?: 0}",
                    modifier = Modifier.weight(1f),
                )
            }
            BodyEnergyStat(
                label = stringResource(R.string.body_energy_timeline_confidence),
                value = confidenceText(timeline?.confidence ?: BodyEnergyConfidence.NO_DATA),
                body = timeline?.confidenceReason.orEmpty(),
            )
        }
    }
}

@Composable
private fun BodyEnergyDayTimelineCard(
    display: BodyEnergyDisplayState,
    modifier: Modifier = Modifier,
) {
    BodyEnergyChartCard(
        title = stringResource(R.string.body_energy_timeline_day_title),
        display = display,
        modifier = modifier,
    )
}

@Composable
private fun BodyEnergyChartCard(
    title: String,
    display: BodyEnergyDisplayState,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (display.isEmpty) {
                Text(
                    text = stringResource(R.string.body_energy_timeline_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                BodyEnergyTimelineChart(
                    points = display.chartPoints,
                    influenceBars = display.influenceBars,
                    modifier = Modifier.fillMaxWidth(),
                )
                BodyEnergyInfluenceLegend(
                    influences = display.legendInfluences,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BodyEnergyInfluenceLegend(
    influences: List<BodyEnergyPrimaryInfluence>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        influences.forEach { influence ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(bodyEnergyInfluenceColor(influence)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = influenceLabel(influence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BodyEnergyReasonsCard(
    reasons: List<BodyEnergyReason>,
    hasTimeline: Boolean,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.body_energy_why_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (!hasTimeline || reasons.isEmpty()) {
                Text(
                    text = stringResource(R.string.body_energy_why_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                reasons.forEach { reason ->
                    BodyEnergyReasonRow(reason = reason)
                }
            }
        }
    }
}

@Composable
private fun BodyEnergyReasonRow(reason: BodyEnergyReason) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .background(bodyEnergyInfluenceColor(reason.influence)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = influenceLabel(reason.influence),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = reasonDetail(reason.influence),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (reason.direction == BodyEnergyReasonDirection.CHARGE) {
                "+${reason.roundedAmount}"
            } else {
                "-${reason.roundedAmount}"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = bodyEnergyInfluenceColor(reason.influence),
        )
    }
}

@Composable
private fun BodyEnergyInputsCard(
    display: BodyEnergyDisplayState,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
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
                    text = stringResource(R.string.body_energy_inputs_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            display.inputSummary?.let { summary ->
                Text(
                    text = stringResource(
                        R.string.body_energy_inputs_summary,
                        summary.algorithmVersion,
                        summary.bucketMinutes,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            display.inputRows.forEach { row ->
                BodyEnergyInputRowContent(row = row)
            }
        }
    }
}

@Composable
private fun BodyEnergyInputRowContent(row: BodyEnergyInputRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(inputStatusColor(row.status)),
        )
        Text(
            text = inputLabel(row.kind),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = inputStatusText(row),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BodyEnergyCalculationCard(
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.body_energy_calculation_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.body_energy_calculation_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.body_energy_calculation_inputs_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.body_energy_calculation_limits_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BodyEnergyStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    body: String = "",
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (body.isNotBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun bodyEnergyColor(score: Int?): Color =
    when {
        score == null -> MaterialTheme.colorScheme.onSurfaceVariant
        score >= 80 -> MaterialTheme.colorScheme.primary
        score >= 60 -> MaterialTheme.colorScheme.tertiary
        score >= 40 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

@Composable
private fun inputStatusColor(status: BodyEnergyInputStatus): Color =
    when (status) {
        BodyEnergyInputStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
        BodyEnergyInputStatus.MISSING -> MaterialTheme.colorScheme.error
        BodyEnergyInputStatus.OPTIONAL -> MaterialTheme.colorScheme.outline
    }

@Composable
private fun influenceLabel(influence: BodyEnergyPrimaryInfluence): String =
    stringResource(
        when (influence) {
            BodyEnergyPrimaryInfluence.SLEEP_RECOVERY -> R.string.body_energy_influence_sleep_recovery
            BodyEnergyPrimaryInfluence.QUIET_REST -> R.string.body_energy_influence_quiet_rest
            BodyEnergyPrimaryInfluence.EXERTION -> R.string.body_energy_influence_exertion
            BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE -> R.string.body_energy_influence_elevated_hr
            BodyEnergyPrimaryInfluence.RECOVERY_DEBT -> R.string.body_energy_influence_recovery_debt
            BodyEnergyPrimaryInfluence.NO_DATA -> R.string.body_energy_influence_no_data
            BodyEnergyPrimaryInfluence.STEADY -> R.string.body_energy_influence_steady
        }
    )

@Composable
private fun reasonDetail(influence: BodyEnergyPrimaryInfluence): String =
    stringResource(
        when (influence) {
            BodyEnergyPrimaryInfluence.SLEEP_RECOVERY -> R.string.body_energy_reason_sleep_recovery_detail
            BodyEnergyPrimaryInfluence.QUIET_REST -> R.string.body_energy_reason_quiet_rest_detail
            BodyEnergyPrimaryInfluence.EXERTION -> R.string.body_energy_reason_exertion_detail
            BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE -> R.string.body_energy_reason_elevated_hr_detail
            BodyEnergyPrimaryInfluence.RECOVERY_DEBT -> R.string.body_energy_reason_recovery_debt_detail
            BodyEnergyPrimaryInfluence.NO_DATA -> R.string.body_energy_reason_no_data_detail
            BodyEnergyPrimaryInfluence.STEADY -> R.string.body_energy_reason_steady_detail
        }
    )

@Composable
private fun inputLabel(kind: BodyEnergyInputKind): String =
    stringResource(
        when (kind) {
            BodyEnergyInputKind.HEART_RATE -> R.string.body_energy_input_heart_rate
            BodyEnergyInputKind.SLEEP -> R.string.body_energy_input_sleep
            BodyEnergyInputKind.WORKOUTS -> R.string.body_energy_input_workouts
            BodyEnergyInputKind.RESTING_HEART_RATE -> R.string.body_energy_input_resting_hr
            BodyEnergyInputKind.HEART_RATE_BASELINE -> R.string.body_energy_input_hr_baseline
            BodyEnergyInputKind.HRV -> R.string.body_energy_input_hrv
            BodyEnergyInputKind.RESPIRATORY_RATE -> R.string.body_energy_input_respiratory
            BodyEnergyInputKind.PREVIOUS_SCORE -> R.string.body_energy_input_previous_score
            BodyEnergyInputKind.CALIBRATION -> R.string.body_energy_input_calibration
        }
    )

@Composable
private fun inputStatusText(row: BodyEnergyInputRow): String =
    when (row.kind) {
        BodyEnergyInputKind.HEART_RATE,
        BodyEnergyInputKind.HRV,
        BodyEnergyInputKind.RESPIRATORY_RATE ->
            row.count?.let { stringResource(R.string.body_energy_input_records, it) }
                ?: inputStatusText(row.status)
        BodyEnergyInputKind.SLEEP ->
            row.count?.let { stringResource(R.string.body_energy_input_sessions, it) }
                ?: inputStatusText(row.status)
        BodyEnergyInputKind.WORKOUTS ->
            row.count?.let { stringResource(R.string.body_energy_input_workouts_value, it) }
                ?: inputStatusText(row.status)
        BodyEnergyInputKind.PREVIOUS_SCORE ->
            row.value?.let { stringResource(R.string.body_energy_input_previous_score_value, it) }
                ?: inputStatusText(row.status)
        BodyEnergyInputKind.CALIBRATION -> calibrationModeLabel(row.value)
        BodyEnergyInputKind.RESTING_HEART_RATE,
        BodyEnergyInputKind.HEART_RATE_BASELINE -> inputStatusText(row.status)
    }

@Composable
private fun inputStatusText(status: BodyEnergyInputStatus): String =
    stringResource(
        when (status) {
            BodyEnergyInputStatus.AVAILABLE -> R.string.body_energy_input_available
            BodyEnergyInputStatus.MISSING -> R.string.body_energy_input_missing
            BodyEnergyInputStatus.OPTIONAL -> R.string.body_energy_input_optional
        }
    )

@Composable
private fun calibrationModeLabel(value: String?): String {
    val mode = value
        ?.let { runCatching { BodyEnergyCalibrationMode.valueOf(it) }.getOrNull() }
        ?: BodyEnergyCalibrationMode.AUTOMATIC
    return stringResource(
        when (mode) {
            BodyEnergyCalibrationMode.AUTOMATIC -> R.string.body_energy_calibration_mode_auto
            BodyEnergyCalibrationMode.MANUAL_VALUES -> R.string.body_energy_calibration_mode_manual_values
            BodyEnergyCalibrationMode.MANUAL_ZONES -> R.string.body_energy_calibration_mode_manual_zones
        }
    )
}

private fun confidenceText(confidence: BodyEnergyConfidence): String =
    when (confidence) {
        BodyEnergyConfidence.HIGH -> "High"
        BodyEnergyConfidence.MEDIUM -> "Medium"
        BodyEnergyConfidence.LOW -> "Low"
        BodyEnergyConfidence.NO_DATA -> "No data"
    }
