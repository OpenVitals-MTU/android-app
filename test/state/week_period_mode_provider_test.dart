import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/preferences/activity_week_mode.dart';
import 'package:openvitals/state/app_providers.dart';

void main() {
  test('weekPeriodModeProvider follows the preference without a restart',
      () async {
    SharedPreferences.setMockInitialValues(<String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    final container = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);

    expect(
      container.read(weekPeriodModeProvider),
      WeekPeriodMode.mondayToSunday,
    );

    // Toggling the setting must reach every watcher immediately. Reading the
    // repository getter without the listenable bridge froze the provider at
    // its first value until an app restart, so screens derived their period
    // under one mode while loads used the other.
    container.read(preferencesRepositoryProvider).activityWeekMode =
        ActivityWeekMode.last7Days;

    expect(container.read(weekPeriodModeProvider), WeekPeriodMode.last7Days);
  });
}
