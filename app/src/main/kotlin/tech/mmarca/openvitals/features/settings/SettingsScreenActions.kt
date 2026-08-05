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
    val onGrantRouteImportPermissions: () -> Unit,
    val onImportAppleHealth: () -> Unit,
    val onToggleAppleHealthImportCategory: (AppleHealthImportCategory, Boolean) -> Unit,
    val onImportSelectedAppleHealth: () -> Unit,
    val onImportRouteFile: () -> Unit,
    val onImportRouteFiles: () -> Unit,
    val onImportFitFile: () -> Unit,
    val onOpenCsvImport: () -> Unit,
    val onOpenReportExport: () -> Unit,
    val onImportOfflineMap: () -> Unit,
    val onCopyAppleHealthReport: (String) -> Unit,
    val onCopyAppleHealthError: (String) -> Unit,
    val onSaveAppleHealthReport: () -> Unit,
    val onSaveDebugLogs: () -> Unit,
    val onShareDebugLogs: () -> Unit,
    val onOpenManualPermissionSettings: () -> Unit,
    val onGrantPermissions: (Set<String>) -> Unit,
    val onSaveBodyEnergyCalibration: (BodyEnergyCalibration, Int?) -> Unit,
    val onResetBodyEnergyPersonalTuning: () -> Unit,
)
