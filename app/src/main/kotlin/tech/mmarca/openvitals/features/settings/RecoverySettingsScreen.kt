package tech.mmarca.openvitals.features.settings

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.HeartRateThresholds
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.features.bodyenergy.BodyEnergyCalibrationCard
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

/** Recovery: the night window, the heart rate alerts, the Body Energy zones and the derived-data reset. */
@Composable
fun RecoverySettingsScreen(viewModel: BodySettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val calibrationSaved = stringResource(R.string.body_energy_calibration_saved)
    val calibrationReset = stringResource(R.string.body_energy_calibration_reset)
    val resetDone = stringResource(R.string.settings_derived_metrics_reset_done)
    val resetFailed = stringResource(R.string.settings_derived_metrics_reset_failed)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    SettingsSectionList {
        recoverySettingsCards(
            state = state,
            viewModel = viewModel,
            onSaveBodyEnergyCalibration = { calibration, birthYear ->
                viewModel.updateBodyEnergyCalibration(calibration, birthYear)
                Toast.makeText(context, calibrationSaved, Toast.LENGTH_SHORT).show()
            },
            onResetBodyEnergyPersonalTuning = {
                viewModel.resetBodyEnergyPersonalTuning()
                Toast.makeText(context, calibrationReset, Toast.LENGTH_SHORT).show()
            },
            onResetDerivedMetrics = {
                viewModel.resetDerivedMetrics { succeeded ->
                    val message = if (succeeded) resetDone else resetFailed
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

private fun LazyListScope.recoverySettingsCards(
    state: BodySettingsUiState,
    viewModel: BodySettingsViewModel,
    onSaveBodyEnergyCalibration: (BodyEnergyCalibration, Int?) -> Unit,
    onResetBodyEnergyPersonalTuning: () -> Unit,
    onResetDerivedMetrics: () -> Unit,
) {
    item { SectionHeader(stringResource(SettingsSection.RECOVERY.titleRes)) }
    item {
        SleepHourStepperCard(
            title = stringResource(R.string.settings_sleep_night_start_title),
            body = stringResource(R.string.settings_sleep_night_start_body),
            hour = state.nightStartHour,
            onHourChange = viewModel::setNightStartHour,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        SleepHourStepperCard(
            title = stringResource(R.string.settings_sleep_night_end_title),
            body = stringResource(R.string.settings_sleep_night_end_body),
            hour = state.nightEndHour,
            onHourChange = viewModel::setNightEndHour,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        HeartRateThresholdCard(
            titleRes = R.string.settings_high_heart_rate_alert_title,
            bodyRes = R.string.settings_high_heart_rate_alert_body,
            thresholdBpm = state.highHeartRateThresholdBpm,
            onThresholdChange = viewModel::setHighHeartRateThresholdBpm,
        )
    }
    item { SettingsCardSpacer() }
    item {
        HeartRateThresholdCard(
            titleRes = R.string.settings_low_heart_rate_alert_title,
            bodyRes = R.string.settings_low_heart_rate_alert_body,
            thresholdBpm = state.lowHeartRateThresholdBpm,
            onThresholdChange = viewModel::setLowHeartRateThresholdBpm,
        )
    }
    item { SettingsCardSpacer() }
    item {
        BodyEnergyCalibrationCard(
            calibration = state.bodyEnergyCalibration,
            bodyProfile = state.bodyProfile,
            // The Body profile section owns the birth year.
            showBirthYear = false,
            onSave = onSaveBodyEnergyCalibration,
            onResetPersonalTuning = onResetBodyEnergyPersonalTuning,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        DerivedMetricsResetCard(
            isResetting = state.isResettingDerivedMetrics,
            onReset = onResetDerivedMetrics,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
}

/** One alert threshold, stepped by [HeartRateThresholds.STEP_BPM]; the ViewModel keeps the pair apart. */
@Composable
private fun HeartRateThresholdCard(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    thresholdBpm: Int,
    onThresholdChange: (Int) -> Unit,
) {
    SettingsStepperCard(
        title = stringResource(titleRes),
        body = stringResource(bodyRes),
        valueLabel = stringResource(R.string.settings_heart_rate_bpm_value, thresholdBpm),
        onIncrease = { onThresholdChange(thresholdBpm + HeartRateThresholds.STEP_BPM) },
        onDecrease = { onThresholdChange(thresholdBpm - HeartRateThresholds.STEP_BPM) },
        modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
    )
}
