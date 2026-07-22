import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../domain/preferences/activity_week_mode.dart';
import '../../../domain/preferences/app_theme_mode.dart';
import '../../../domain/preferences/chart_aggregation_mode.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/app_language_dropdown.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/placeholder_screen.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import 'cards/activity_recording_preferences_card.dart';
import 'cards/activity_split_distance_card.dart';
import 'cards/apple_health_import_card.dart';
import 'cards/body_energy_calibration_card.dart';
import 'cards/body_profile_card.dart';
import 'cards/caffeine_preferences_card.dart';
import 'cards/debug_diagnostics_card.dart';
import 'cards/favorite_activity_card.dart';
import 'cards/fit_import_card.dart';
import 'cards/permission_categories_card.dart';
import 'cards/route_import_card.dart';
import 'offline_maps_card.dart';
import '../application/settings_view_model.dart';
import 'settings_section.dart';

/// A settings sub-section pushed over the shell, ported from the Kotlin
/// `settingsScreenContent(section = ...)`. Renders the cards for [section] and
/// wires each control to [settingsProvider] (which persists through
/// `PreferencesRepository`).
///
/// The Data Import section is a Phase-6 subsystem and routes to a "coming soon"
/// placeholder; the Sensors section routes directly to [BleDevicesScreen] (wired
/// in the router), so its case below is unreachable but kept for exhaustiveness.
class SettingsSectionScreen extends ConsumerWidget {
  const SettingsSectionScreen({super.key, required this.section});

  final SettingsSection section;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(section.localizedTitle(l10n))),
      body: switch (section) {
        SettingsSection.sensors => const _ComingSoonBody(
            // TODO(phase6): BLE heart-rate sensor pairing is a Phase-6 subsystem.
            message: 'Bluetooth sensor pairing is coming in a later update.',
          ),
        _ => Align(
            alignment: Alignment.topCenter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 920),
              child: ListView(
                padding: screenScrollPadding(context),
                children: _cards(context, ref, section),
              ),
            ),
          ),
      },
    );
  }
}

List<Widget> _cards(BuildContext context, WidgetRef ref, SettingsSection section) {
  final notifier = ref.read(settingsProvider.notifier);
  final state = ref.watch(settingsProvider);
  final l10n = AppLocalizations.of(context);
  switch (section) {
    case SettingsSection.display:
      return [
        _SettingsCard(
          title: l10n.settingsLanguageTitle,
          body: l10n.settingsLanguageBody,
          child: AppLanguageDropdown(
            selected: state.appLanguage,
            onSelect: notifier.selectAppLanguage,
          ),
        ),
        _SettingsCard(
          title: l10n.settingsUnitsTitle,
          body: l10n.settingsUnitsBody,
          child: _ChoiceRow<UnitSystem>(
            options: UnitSystem.values,
            selected: state.unitSystem,
            labelFor: (value) => _unitLabel(l10n, value),
            onSelect: notifier.selectUnitSystem,
          ),
        ),
        _SettingsCard(
          title: l10n.settingsThemeTitle,
          body: l10n.settingsThemeBody,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _ChoiceRow<AppThemeMode>(
                options: AppThemeMode.values,
                selected: state.appThemeMode,
                labelFor: (value) => _themeLabel(l10n, value),
                onSelect: notifier.selectAppThemeMode,
              ),
              const SizedBox(height: 8),
              _InlineSwitchRow(
                title: l10n.settingsDynamicColorTitle,
                body: l10n.settingsDynamicColorBody,
                value: state.dynamicColor,
                onChanged: notifier.setDynamicColor,
              ),
            ],
          ),
        ),
        _SettingsCard(
          title: l10n.settingsChartAggregationTitle,
          body: l10n.settingsChartAggregationBody,
          child: _ChoiceRow<ChartAggregationMode>(
            options: ChartAggregationMode.values,
            selected: state.chartAggregationMode,
            labelFor: (value) => _chartAggregationLabel(l10n, value),
            onSelect: notifier.selectChartAggregationMode,
          ),
        ),
      ];
    case SettingsSection.activities:
      return [
        _SettingsCard(
          title: l10n.settingsActivityWeekTitle,
          body: l10n.settingsActivityWeekBody,
          child: _ChoiceRow<ActivityWeekMode>(
            options: ActivityWeekMode.values,
            selected: state.activityWeekMode,
            labelFor: (value) => _weekLabel(l10n, value),
            onSelect: notifier.selectActivityWeekMode,
          ),
        ),
        // Kotlin ACTIVITIES order: week mode, favorite activity, recording
        // preferences, offline maps (SettingsScreenContent.kt:89-129). The
        // split-distance card is new (no Kotlin counterpart) and sits with the
        // other activity-display settings, before the recording tuning block.
        const FavoriteActivityCard(),
        const ActivitySplitDistanceCard(),
        const ActivityRecordingPreferencesCard(),
        const OfflineMapsCard(),
      ];
    case SettingsSection.nutrition:
      final formatter = ref.watch(unitFormatterProvider);
      return [
        _SwitchCard(
          title: l10n.settingsCalorieDataTitle,
          body: l10n.settingsCalorieDataBody,
          value: state.showOpenVitalsCalculatedCalories,
          onChanged: notifier.setShowOpenVitalsCalculatedCalories,
        ),
        _StepperCard(
          title: 'Hydration goal',
          body: 'Your daily hydration target.',
          valueLabel: formatter.hydration(state.hydrationDailyGoalLiters).text,
          onDecrement: () => notifier.setHydrationDailyGoalLiters(
            state.hydrationDailyGoalLiters - 0.25,
          ),
          onIncrement: () => notifier.setHydrationDailyGoalLiters(
            state.hydrationDailyGoalLiters + 0.25,
          ),
        ),
        const CaffeinePreferencesCard(),
      ];
    case SettingsSection.recovery:
      return [
        _StepperCard(
          title: l10n.settingsSleepNightStartTitle,
          body: l10n.settingsSleepNightStartBody,
          valueLabel: _hourLabel(state.nightStartHour),
          onDecrement: () =>
              notifier.setNightStartHour(state.nightStartHour - 1),
          onIncrement: () =>
              notifier.setNightStartHour(state.nightStartHour + 1),
        ),
        _StepperCard(
          title: l10n.settingsSleepNightEndTitle,
          body: l10n.settingsSleepNightEndBody,
          valueLabel: _hourLabel(state.nightEndHour),
          onDecrement: () => notifier.setNightEndHour(state.nightEndHour - 1),
          onIncrement: () => notifier.setNightEndHour(state.nightEndHour + 1),
        ),
        _StepperCard(
          title: 'High heart-rate alert',
          body: 'Threshold for a high resting heart-rate flag.',
          valueLabel: '${state.highHeartRateThresholdBpm} bpm',
          onDecrement: () => notifier.setHighHeartRateThresholdBpm(
            state.highHeartRateThresholdBpm - 5,
          ),
          onIncrement: () => notifier.setHighHeartRateThresholdBpm(
            state.highHeartRateThresholdBpm + 5,
          ),
        ),
        _StepperCard(
          title: 'Low heart-rate alert',
          body: 'Threshold for a low resting heart-rate flag.',
          valueLabel: '${state.lowHeartRateThresholdBpm} bpm',
          onDecrement: () => notifier.setLowHeartRateThresholdBpm(
            state.lowHeartRateThresholdBpm - 5,
          ),
          onIncrement: () => notifier.setLowHeartRateThresholdBpm(
            state.lowHeartRateThresholdBpm + 5,
          ),
        ),
        // Kotlin RECOVERY order: sleep range, body profile, body energy
        // calibration (SettingsScreenContent.kt:154-181). The high/low HR
        // steppers above are a deliberate Flutter-side extra.
        const BodyProfileCard(),
        const BodyEnergyCalibrationCard(),
      ];
    case SettingsSection.healthConnect:
      return [
        _SwitchCard(
          title: l10n.settingsHealthConnectSyncTitle,
          body: l10n.settingsHealthConnectSyncBody,
          value: state.healthConnectSyncEnabled,
          onChanged: notifier.setHealthConnectSyncEnabled,
        ),
        _SwitchCard(
          title: l10n.settingsHealthConnectMindfulnessTitle,
          body: l10n.settingsHealthConnectMindfulnessBody,
          value: state.healthConnectMindfulnessEnabled,
          onChanged: notifier.setHealthConnectMindfulnessEnabled,
        ),
        _SwitchCard(
          title: l10n.settingsAppLockTitle,
          body: l10n.settingsAppLockBody,
          value: state.appLockEnabled,
          onChanged: notifier.setAppLockEnabled,
        ),
        const PermissionCategoriesCard(),
      ];
    case SettingsSection.dataImport:
      // Kotlin DATA_IMPORT order: Apple Health, route (single + bulk), FIT
      // (SettingsScreenContent.kt:182-231).
      return const [
        AppleHealthImportCard(),
        RouteImportCard(),
        FitImportCard(),
      ];
    case SettingsSection.debugDiagnostics:
      // Kotlin DEBUG_DIAGNOSTICS: a single "Save logs" card (SettingsScreenContent
      // .kt:302-312). The route is only reachable in debug builds.
      return const [DebugDiagnosticsCard()];
    case SettingsSection.sensors:
    case SettingsSection.watches:
    case SettingsSection.deviceSync:
      // Both route to bespoke screens (BleDevicesScreen / DeviceSyncScreen), so
      // SettingsSectionScreen never renders cards for them.
      return const [];
  }
}

String _unitLabel(AppLocalizations l10n, UnitSystem value) => switch (value) {
      UnitSystem.metric => l10n.settingsUnitMetric,
      UnitSystem.imperial => l10n.settingsUnitImperial,
    };

String _themeLabel(AppLocalizations l10n, AppThemeMode value) => switch (value) {
      AppThemeMode.system => l10n.settingsThemeSystem,
      AppThemeMode.light => l10n.settingsThemeLight,
      AppThemeMode.dark => l10n.settingsThemeDark,
      AppThemeMode.amoled => l10n.settingsThemeAmoled,
    };

/// A 24h clock hour as `HH:00` (e.g. 18 → "18:00").
String _hourLabel(int hour) => '${hour.toString().padLeft(2, '0')}:00';

String _chartAggregationLabel(AppLocalizations l10n, ChartAggregationMode value) =>
    switch (value) {
      ChartAggregationMode.off => l10n.settingsChartAggregationOff,
      ChartAggregationMode.min5 => l10n.settingsChartAggregationMin5,
      ChartAggregationMode.min10 => l10n.settingsChartAggregationMin10,
      ChartAggregationMode.min30 => l10n.settingsChartAggregationMin30,
    };

String _weekLabel(AppLocalizations l10n, ActivityWeekMode value) =>
    switch (value) {
      ActivityWeekMode.mondayToSunday => l10n.settingsActivityWeekMondayToSunday,
      ActivityWeekMode.last7Days => l10n.settingsActivityWeekLast7Days,
    };

/// A titled card wrapping a settings control. Port of the Kotlin settings card
/// chrome (title + body + control).
class _SettingsCard extends StatelessWidget {
  const _SettingsCard({
    required this.title,
    required this.body,
    required this.child,
  });

  final String title;
  final String body;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: theme.textTheme.titleSmall),
              const SizedBox(height: 4),
              Text(
                body,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 12),
              child,
            ],
          ),
        ),
      ),
    );
  }
}

/// A single-choice chip row. A trimmed port of the Kotlin
/// `SingleChoiceSegmentedButtonRow`.
class _ChoiceRow<T> extends StatelessWidget {
  const _ChoiceRow({
    required this.options,
    required this.selected,
    required this.labelFor,
    required this.onSelect,
  });

  final List<T> options;
  final T selected;
  final String Function(T) labelFor;
  final ValueChanged<T> onSelect;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final option in options)
          ChoiceChip(
            label: Text(labelFor(option)),
            selected: option == selected,
            onSelected: (_) => onSelect(option),
          ),
      ],
    );
  }
}

/// A card whose whole body is a switch row.
class _SwitchCard extends StatelessWidget {
  const _SwitchCard({
    required this.title,
    required this.body,
    required this.value,
    required this.onChanged,
  });

  final String title;
  final String body;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: _InlineSwitchRow(
            title: title,
            body: body,
            value: value,
            onChanged: onChanged,
          ),
        ),
      ),
    );
  }
}

class _InlineSwitchRow extends StatelessWidget {
  const _InlineSwitchRow({
    required this.title,
    required this.body,
    required this.value,
    required this.onChanged,
  });

  final String title;
  final String body;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: theme.textTheme.bodyLarge),
              const SizedBox(height: 2),
              Text(
                body,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
        const SizedBox(width: 12),
        Switch(value: value, onChanged: onChanged),
      ],
    );
  }
}

/// A -/+ stepper card for numeric settings (hydration goal, HR thresholds).
class _StepperCard extends StatelessWidget {
  const _StepperCard({
    required this.title,
    required this.body,
    required this.valueLabel,
    required this.onDecrement,
    required this.onIncrement,
  });

  final String title;
  final String body;
  final String valueLabel;
  final VoidCallback onDecrement;
  final VoidCallback onIncrement;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: theme.textTheme.titleSmall),
                    const SizedBox(height: 2),
                    Text(
                      body,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: onDecrement,
                icon: const Icon(Icons.remove_circle_outline),
                tooltip: 'Decrease',
              ),
              SizedBox(
                width: 88,
                child: Text(
                  valueLabel,
                  textAlign: TextAlign.center,
                  style: theme.textTheme.titleMedium
                      ?.copyWith(fontWeight: FontWeight.w600),
                ),
              ),
              IconButton(
                onPressed: onIncrement,
                icon: const Icon(Icons.add_circle_outline),
                tooltip: 'Increase',
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ComingSoonBody extends StatelessWidget {
  const _ComingSoonBody({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) =>
      PlaceholderBody(title: message);
}
