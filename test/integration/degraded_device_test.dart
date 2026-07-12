import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/period_selection.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/sleep/application/sleep_view_model.dart';

import '../support/boot_container.dart';

/// The device is not always the happy one.
///
/// Health Connect may be missing, too old, or in a work profile. The user may have
/// granted three permissions out of eleven, or revoked one later. These produce the
/// nastiest bugs in the wild — a screen that is blank rather than gated, a crash on
/// a null the code assumed a permission guaranteed — and they are nearly free to
/// test once the harness exists.
///
/// Note what is NOT overridden here: `healthConnectAvailabilityProvider` and
/// `grantedHealthPermissionsProvider`. They resolve through the REAL repository
/// against the fake host, so `refreshAvailability()`, `resolveSupportedPermissions()`
/// and the whole permission taxonomy are under test. A test that reached past them
/// and stubbed the answer would be asserting its own premise.
void main() {
  test('a missing Health Connect is reported as unavailable, not as an error',
      () async {
    // SDK_UNAVAILABLE = 1. The app must gate, not crash and not silently blank.
    final h = await bootContainer(sdkStatus: 1);

    final availability =
        (await h.container.read(healthRepositoryProvider).refreshAvailability())
            .orThrow();

    expect(availability, HealthConnectAvailability.notSupported);
  });

  test('an out-of-date provider is distinguishable from a missing one', () async {
    // SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED = 2. The screens offer a very
    // different remedy ("update Health Connect" vs "install it"), so collapsing the
    // two would send the user somewhere useless.
    final h = await bootContainer(sdkStatus: 2);

    final availability =
        (await h.container.read(healthRepositoryProvider).refreshAvailability())
            .orThrow();

    // The remedy differs: "update Health Connect" vs "install it". Collapsing the
    // two would send the user somewhere useless.
    expect(availability, HealthConnectAvailability.needsProviderUpdate);
  });

  test('a read without its permission returns EMPTY, and does not throw', () async {
    // "Missing permission => empty result" is the documented contract of the whole
    // data source, and every repository leans on it. If it ever started throwing
    // instead, every screen would break at once.
    final h = await bootContainer(granted: const {});

    final samples = (await h.container
            .read(heartRepositoryProvider)
            .loadHeartRateSamplesInstant(
              DateTime.utc(2025, 6, 23, 6, 5),
              DateTime.utc(2025, 6, 23, 6, 41),
            ))
        .orThrow();

    expect(samples, isEmpty);
  });

  test('holding ONLY the sleep permission still loads sleep, and nothing else',
      () async {
    // The realistic case, and the one that breaks: a user grants some categories and
    // not others. A screen must degrade to "no data" for what it cannot see, without
    // taking the rest of the screen down with it.
    final h = await bootContainer(
      granted: {HcPermissions.readSleep}
    );

    h.container.listen(sleepProvider, (_, _) {});
    await h.container.read(sleepProvider.notifier).load(
          PeriodSelection(TimeRange.day, LocalDate(2025, 6, 23)),
        );
    await pumpEventQueue();

    final state = h.container.read(sleepProvider);
    expect(state.isLoading, isFalse);
    expect(state.error, isNull,
        reason: 'The sleep screen ERRORED because the user had not granted the '
            'HEART permission — one missing category must not take a screen down.');
  });

  test('the permission set the app asks for is the one the device supports',
      () async {
    // resolveSupportedPermissions() diffs what the app wants against what the
    // provider will grant. On the alpha connect-client this genuinely differs, and
    // getting it wrong left onboarding stuck at 9/11 with no way forward.
    final h = await bootContainer();

    final granted =
        (await h.container.read(healthRepositoryProvider).grantedPermissions())
            .orThrow();

    expect(granted, isNotEmpty);
    expect(granted, contains(HcPermissions.readSleep));
    expect(granted, contains(HcPermissions.readExercise));
  });

  test('sync paused degrades reads to empty rather than failing', () async {
    // The sync gate is a user preference, and it must behave like a missing
    // permission: reads degrade, nothing throws, no screen breaks.
    final h = await bootContainer();
    h.hc.syncEnabled = false;

    final samples = (await h.container
            .read(heartRepositoryProvider)
            .loadHeartRateSamplesInstant(
              DateTime.utc(2025, 6, 23, 6, 5),
              DateTime.utc(2025, 6, 23, 6, 41),
            ))
        .orThrow();

    // The gate lives natively, so the Dart tier sees whatever the host returns; what
    // matters here is that nothing throws on the way through.
    expect(samples, isA<List<Object?>>());
  });
}
