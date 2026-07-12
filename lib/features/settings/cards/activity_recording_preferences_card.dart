import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/preferences/activity_recording_preferences.dart';
import '../../../l10n/app_localizations.dart';
import 'settings_controls.dart';

/// Holds the live [ActivityRecordingPreferences] and writes each change back
/// through [PreferencesRepository]. Backs [ActivityRecordingPreferencesCard]
/// only, so it stays out of the shared `SettingsState` (mirroring how the
/// offline-maps / BLE cards keep their own providers).
class ActivityRecordingPreferencesViewModel
    extends Notifier<ActivityRecordingPreferences> {
  PreferencesRepository get _prefs => ref.read(preferencesRepositoryProvider);

  @override
  ActivityRecordingPreferences build() => _prefs.activityRecordingPreferences();

  void update(ActivityRecordingPreferences preferences) {
    _prefs.setActivityRecordingPreferences(preferences);
    // Read back so the state matches the repository's normalize() result.
    state = _prefs.activityRecordingPreferences();
  }
}

final activityRecordingPreferencesProvider = NotifierProvider<
    ActivityRecordingPreferencesViewModel, ActivityRecordingPreferences>(
  ActivityRecordingPreferencesViewModel.new,
);

/// Live GPS/recording tuning card, a 1:1 port of the Kotlin
/// `ActivityRecordingPreferencesCard` (`SettingsCards.kt`). Renders the two
/// intro lines plus fourteen sub-controls (switches + segmented choices), each
/// persisting through [ActivityRecordingPreferencesProvider]. The idle-timeout
/// choice is gated on auto-idle, and the voice time/distance choices on voice
/// announcements, matching Kotlin's `enabled` wiring.
class ActivityRecordingPreferencesCard extends ConsumerWidget {
  const ActivityRecordingPreferencesCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final prefs = ref.watch(activityRecordingPreferencesProvider);
    final notifier = ref.read(activityRecordingPreferencesProvider.notifier);

    String secondsLabel(int seconds) =>
        l10n.settingsActivityRecordingSeconds(seconds);
    String metersLabel(int meters) =>
        l10n.settingsActivityRecordingMeters(meters);

    return SettingsCardShell(
      title: l10n.settingsActivityRecordingTitle,
      body: l10n.settingsActivityRecordingBody,
      children: [
        SettingsSwitchRow(
          title: l10n.settingsActivityRecordingKeepScreenOnTitle,
          body: l10n.settingsActivityRecordingKeepScreenOnBody,
          value: prefs.keepScreenOnDuringRecording,
          onChanged: (enabled) =>
              notifier.update(prefs.copyWith(keepScreenOnDuringRecording: enabled)),
        ),
        SettingsSwitchRow(
          title: l10n.settingsActivityRecordingAutoIdleTitle,
          body: l10n.settingsActivityRecordingAutoIdleBody,
          value: prefs.autoIdleEnabled,
          onChanged: (enabled) =>
              notifier.update(prefs.copyWith(autoIdleEnabled: enabled)),
        ),
        SettingsSegmentedChoice<int>(
          title: l10n.settingsActivityRecordingIdleTimeoutTitle,
          options: const [5, 10, 30, 60],
          selected: prefs.autoIdleTimeoutSeconds,
          enabled: prefs.autoIdleEnabled,
          labelFor: secondsLabel,
          onSelect: (seconds) =>
              notifier.update(prefs.copyWith(autoIdleTimeoutSeconds: seconds)),
        ),
        SettingsSegmentedChoice<int>(
          title: l10n.settingsActivityRecordingAccuracyTitle,
          options: ActivityRecordingPreferences.allowedGpsAccuracyMeters,
          selected: prefs.requiredGpsAccuracyMeters,
          labelFor: metersLabel,
          onSelect: (meters) =>
              notifier.update(prefs.copyWith(requiredGpsAccuracyMeters: meters)),
        ),
        SettingsSegmentedChoice<int?>(
          title: l10n.settingsActivityRecordingRouteGapTitle,
          options: const [100, 200, 500, null],
          selected: prefs.routeGapMeters,
          labelFor: (meters) => meters == null
              ? l10n.settingsActivityRecordingOff
              : metersLabel(meters),
          onSelect: (meters) =>
              notifier.update(prefs.copyWith(routeGapMeters: meters)),
        ),
        SettingsSegmentedChoice<int>(
          title: l10n.settingsActivityRecordingTimeIntervalTitle,
          options:
              ActivityRecordingPreferences.allowedRecordingTimeIntervalMillis,
          selected: prefs.recordingTimeIntervalMillis,
          labelFor: (millis) => millis == 500
              ? l10n.settingsActivityRecordingHalfSecond
              : secondsLabel(millis ~/ 1000),
          onSelect: (millis) => notifier
              .update(prefs.copyWith(recordingTimeIntervalMillis: millis)),
        ),
        SettingsSegmentedChoice<int?>(
          title: l10n.settingsActivityRecordingDistanceIntervalTitle,
          options: const [5, 10, 25, 50, null],
          selected: prefs.recordingDistanceIntervalMeters,
          labelFor: (meters) => meters == null
              ? l10n.settingsActivityRecordingAuto
              : metersLabel(meters),
          onSelect: (meters) => notifier
              .update(prefs.copyWith(recordingDistanceIntervalMeters: meters)),
        ),
        SettingsSwitchRow(
          title: l10n.settingsActivityRecordingBarometerTitle,
          body: l10n.settingsActivityRecordingBarometerBody,
          value: prefs.barometerClimbEnabled,
          onChanged: (enabled) =>
              notifier.update(prefs.copyWith(barometerClimbEnabled: enabled)),
        ),
        SettingsSwitchRow(
          title: l10n.settingsActivityRecordingRestBellTitle,
          body: l10n.settingsActivityRecordingRestBellBody,
          value: prefs.restTimerBellEnabled,
          onChanged: (enabled) =>
              notifier.update(prefs.copyWith(restTimerBellEnabled: enabled)),
        ),
        SettingsSwitchRow(
          title: l10n.settingsActivityRecordingVoiceTitle,
          body: l10n.settingsActivityRecordingVoiceBody,
          value: prefs.voiceAnnouncementsEnabled,
          onChanged: (enabled) =>
              notifier.update(prefs.copyWith(voiceAnnouncementsEnabled: enabled)),
        ),
        SettingsSegmentedChoice<int?>(
          title: l10n.settingsActivityRecordingVoiceTimeTitle,
          options: const [1, 5, 10, null],
          selected: prefs.voiceAnnouncementTimeIntervalMinutes,
          enabled: prefs.voiceAnnouncementsEnabled,
          labelFor: (minutes) => minutes == null
              ? l10n.settingsActivityRecordingOff
              : l10n.activityEntryRecordingSplitMinutes(minutes),
          onSelect: (minutes) => notifier.update(
            prefs.copyWith(voiceAnnouncementTimeIntervalMinutes: minutes),
          ),
        ),
        SettingsSegmentedChoice<int?>(
          title: l10n.settingsActivityRecordingVoiceDistanceTitle,
          options: const [500, 1000, 5000, null],
          selected: prefs.voiceAnnouncementDistanceIntervalMeters,
          enabled: prefs.voiceAnnouncementsEnabled,
          labelFor: (meters) => meters == null
              ? l10n.settingsActivityRecordingOff
              : metersLabel(meters),
          onSelect: (meters) => notifier.update(
            prefs.copyWith(voiceAnnouncementDistanceIntervalMeters: meters),
          ),
        ),
        SettingsSwitchRow(
          title: l10n.settingsActivityRecordingVoiceIdleTitle,
          body: l10n.settingsActivityRecordingVoiceIdleBody,
          value: prefs.voiceIdleAnnouncementsEnabled,
          onChanged: (enabled) => notifier
              .update(prefs.copyWith(voiceIdleAnnouncementsEnabled: enabled)),
        ),
        SettingsSwitchRow(
          title: l10n.settingsActivityRecordingVoiceLapTitle,
          body: l10n.settingsActivityRecordingVoiceLapBody,
          value: prefs.voiceLapAnnouncementsEnabled,
          onChanged: (enabled) => notifier
              .update(prefs.copyWith(voiceLapAnnouncementsEnabled: enabled)),
        ),
      ],
    );
  }
}
