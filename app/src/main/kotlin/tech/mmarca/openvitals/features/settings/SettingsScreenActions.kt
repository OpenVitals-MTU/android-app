package tech.mmarca.openvitals.features.settings

import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportCategory

internal data class SettingsScreenActions(
    val onOpenSection: (SettingsSection) -> Unit,
    val onOpenPrivacyPolicy: () -> Unit,
    val onOpenIssues: () -> Unit,
    val onOpenDiscussion: () -> Unit,
    val onOpenSupport: () -> Unit,
    val onGrantDataImportPermissions: () -> Unit,
    val onImportAppleHealth: () -> Unit,
    val onToggleAppleHealthImportCategory: (AppleHealthImportCategory, Boolean) -> Unit,
    val onImportSelectedAppleHealth: () -> Unit,
    val onImportRouteFile: () -> Unit,
    val onImportFitFile: () -> Unit,
    val onImportOfflineMap: () -> Unit,
    val onCopyAppleHealthReport: (String) -> Unit,
    val onCopyAppleHealthError: (String) -> Unit,
    val onSaveAppleHealthReport: () -> Unit,
    val onSaveDebugLogs: () -> Unit,
    val onOpenManualPermissionSettings: () -> Unit,
    val onGrantPermissions: (Set<String>) -> Unit,
    val onSaveBodyEnergyCalibration: (BodyEnergyCalibration) -> Unit,
    val onResetBodyEnergyCalibration: () -> Unit,
)
