package tech.mmarca.openvitals.features.settings

import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportCategory

internal data class SettingsScreenActions(
    val onOpenSection: (SettingsSection) -> Unit,
    val onOpenPrivacyPolicy: () -> Unit,
    val onOpenIssues: () -> Unit,
    val onOpenDiscussion: () -> Unit,
    val onOpenSupport: () -> Unit,
    val onImportOfflineMap: () -> Unit,
    val onImportElevationTile: () -> Unit,
    val onSaveDebugLogs: () -> Unit,
    val onShareDebugLogs: () -> Unit,
    val onOpenManualPermissionSettings: () -> Unit,
    val onGrantPermissions: (Set<String>) -> Unit,
    val onSaveBodyEnergyCalibration: (BodyEnergyCalibration, Int?) -> Unit,
    val onResetBodyEnergyPersonalTuning: () -> Unit,
    val onResetDerivedMetrics: () -> Unit,
)
