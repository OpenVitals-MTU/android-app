import 'package:flutter/material.dart';

import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';

/// The top-level settings sections, ported from the Kotlin `SettingsSection`
/// enum (each maps to a card on the settings root + a pushed sub-screen).
enum SettingsSection {
  display(
    route: AppRoutes.settingsDisplay,
    title: 'Display',
    summary: 'Language, units, theme and colours',
    icon: Icons.settings_outlined,
  ),
  activities(
    route: AppRoutes.settingsActivities,
    title: 'Activities',
    summary: 'Week layout and activity preferences',
    icon: Icons.directions_run_outlined,
  ),
  sensors(
    route: AppRoutes.settingsSensors,
    title: 'Sensors',
    summary: 'Connect Bluetooth heart-rate sensors',
    icon: Icons.bluetooth,
  ),
  nutrition(
    route: AppRoutes.settingsNutrition,
    title: 'Nutrition',
    summary: 'Calorie source and hydration goal',
    icon: Icons.restaurant_outlined,
  ),
  recovery(
    route: AppRoutes.settingsRecovery,
    title: 'Recovery',
    summary: 'Sleep window and heart-rate thresholds',
    icon: Icons.favorite_border,
  ),
  dataImport(
    route: AppRoutes.settingsDataImport,
    title: 'Data import',
    summary: 'Import from Apple Health and .fit files',
    icon: Icons.folder_open_outlined,
  ),
  deviceSync(
    route: AppRoutes.settingsDeviceSync,
    title: 'Sync with another phone',
    summary: 'Copy Health Connect records to a nearby phone over Bluetooth',
    icon: Icons.devices_other_outlined,
  ),
  healthConnect(
    route: AppRoutes.settingsHealthConnect,
    title: 'Health Connect',
    summary: 'Sync, permissions and app lock',
    icon: Icons.health_and_safety_outlined,
  ),
  // Diagnostics-only section (Kotlin gates on BuildConfig.OPENVITALS_DIAGNOSTICS
  // — debug OR ci OR nightly; the Flutter analogue is kDiagnosticsEnabled). The
  // hub + router only surface this in diagnostics-enabled builds — see
  // SettingsScreen and app_router's _settingsSectionRoutes.
  debugDiagnostics(
    route: AppRoutes.settingsDebugDiagnostics,
    title: 'Debug diagnostics',
    summary: 'Share or save sanitized diagnostics logs for troubleshooting',
    icon: Icons.bug_report_outlined,
  );

  const SettingsSection({
    required this.route,
    required this.title,
    required this.summary,
    required this.icon,
  });

  final String route;
  final String title;
  final String summary;
  final IconData icon;

  static SettingsSection? fromRoute(String route) {
    for (final section in values) {
      if (section.route == route) return section;
    }
    return null;
  }
}

/// Localized card title/summary for each settings section, sourced from the ARB
/// catalog (the Kotlin `settings_*_group_title` / `_group_body` strings). The
/// enum's const [SettingsSection.title]/[SettingsSection.summary] remain as
/// English fallbacks for non-UI callers.
extension SettingsSectionL10n on SettingsSection {
  String localizedTitle(AppLocalizations l10n) {
    switch (this) {
      case SettingsSection.display:
        return l10n.settingsDisplayGroupTitle;
      case SettingsSection.activities:
        return l10n.settingsActivitiesGroupTitle;
      case SettingsSection.sensors:
        return l10n.settingsSensorsGroupTitle;
      case SettingsSection.nutrition:
        return l10n.settingsNutritionGroupTitle;
      case SettingsSection.recovery:
        return l10n.settingsRecoveryGroupTitle;
      case SettingsSection.dataImport:
        return l10n.settingsDataImportGroupTitle;
      case SettingsSection.deviceSync:
        return l10n.settingsDeviceSyncGroupTitle;
      case SettingsSection.healthConnect:
        return l10n.settingsHealthConnectGroupTitle;
      case SettingsSection.debugDiagnostics:
        return l10n.settingsDebugDiagnosticsGroupTitle;
    }
  }

  String localizedSummary(AppLocalizations l10n) {
    switch (this) {
      case SettingsSection.display:
        return l10n.settingsDisplayGroupBody;
      case SettingsSection.activities:
        return l10n.settingsActivitiesGroupBody;
      case SettingsSection.sensors:
        return l10n.settingsSensorsGroupBody;
      case SettingsSection.nutrition:
        return l10n.settingsNutritionGroupBody;
      case SettingsSection.recovery:
        return l10n.settingsRecoveryGroupBody;
      case SettingsSection.dataImport:
        return l10n.settingsDataImportGroupBody;
      case SettingsSection.deviceSync:
        return l10n.settingsDeviceSyncGroupBody;
      case SettingsSection.healthConnect:
        return l10n.settingsHealthConnectGroupBody;
      case SettingsSection.debugDiagnostics:
        return l10n.settingsDebugDiagnosticsGroupBody;
    }
  }
}
