import 'package:drift/native.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/data/source/health/native/health_connect_native_data_source.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/homewidgets/home_widget_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../features/homewidgets/fake_home_widget_client.dart';
import 'health_connect/fake_health_connect.dart';
import 'health_connect/hc_fixture.dart';

/// A live Riverpod graph with ONLY the Health Connect boundary faked.
///
/// Everything above it is the real thing: the ~2,000-line data source and its
/// mappers, gap-fills and chunking; the 14 repositories; the domain (dedup, sleep
/// merging, the backfills, splits, the sleep score); the 57 use cases; the 33
/// notifiers. A test drives a notifier and asserts on its STATE — no widgets, no
/// device, sub-second.
///
/// ## What is deliberately NOT overridden
///
/// `healthConnectAvailabilityProvider` and `grantedHealthPermissionsProvider`.
/// They resolve through the REAL health repository against the fake host API, which
/// puts `refreshAvailability()`, `resolveSupportedPermissions()`,
/// `resolveFeatureFlags()` and the whole permission taxonomy under test. A test that
/// wants a degraded device says so in the [FakeHealthConnect] — `granted: {...}` or
/// `sdkStatus: 2` — rather than reaching past the code it is meant to be exercising.
///
/// And no repository, and no use case. An override of either short-circuits every
/// layer this harness exists to run — `dashboard_resume_test.dart` once overrode
/// `loadDashboardDayUseCaseProvider` and proved less than it looked like it did;
/// it now runs here instead.
class HealthHarness {
  HealthHarness(this.container, this.hc, this.fixture);

  final ProviderContainer container;
  final FakeHealthConnect hc;
  final HcFixture fixture;

  /// Keeps a notifier alive.
  ///
  /// A `Notifier` auto-disposes the moment nothing listens, so a test that only
  /// READS one would watch it get torn down between the read and the assert.
  void keepAlive(Object provider) =>
      container.listen(provider as dynamic, (_, _) {});
}

/// Boots the graph. `granted: null` means every permission is held.
Future<HealthHarness> bootContainer({
  Set<String>? granted,
  int sdkStatus = 3,
  Map<String, Object> prefs = const {},
  bool allowUnimplemented = false,
}) async {
  SharedPreferences.setMockInitialValues(prefs);
  final sharedPreferences = await SharedPreferences.getInstance();

  final hc = FakeHealthConnect(granted: granted, sdkStatus: sdkStatus);
  final database = OpenVitalsDatabase(NativeDatabase.memory());

  final dataSource = HealthConnectNativeDataSource(
    hostApi: hc,
    appPackageName: openVitalsPackageName,
  );

  // MUST come before anything reads a permission.
  //
  // The data source CACHES availability, and every permission check is gated on that
  // cache: `grantedIfAvailable()` returns an EMPTY set while availability is still
  // unknown. So without this line every repository reads as "no permission", every
  // screen degrades to empty, and nothing fails -- the reads succeed, they just
  // return nothing.
  //
  // This has already caused four bugs in this app (home-screen widgets, one-tap
  // logging, and both reminder alarms), every one presenting as "the feature does
  // nothing". It caught this harness too: the first run of these tests returned an
  // empty heart rate and looked exactly like a regression in the code under test.
  await dataSource.availability();

  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(sharedPreferences),
      // Drift would otherwise open a real file under the app documents directory,
      // which does not exist under `flutter test`.
      openVitalsDatabaseProvider.overrideWithValue(database),
      // THE seam. The real data source, on a fake host API.
      healthDataSourceProvider.overrideWithValue(dataSource),
      // The other platform boundary a notifier can stray onto: the dashboard
      // pushes home-widget snapshots after every load, and the real client is
      // a method channel with no host behind it under `flutter test`.
      homeWidgetServiceProvider.overrideWithValue(
        HomeWidgetService(client: FakeHomeWidgetClient()),
      ),
    ],
  );

  addTearDown(() {
    // The refusal is only loud if something LISTENS for it.
    //
    // `HealthConnectNativeDataSource._catch` degrades any read failure to the
    // documented empty result, so an unimplemented host method is caught, logged,
    // and turned into an empty list -- and the test passes against no data, having
    // proved nothing. That is the exact failure mode this suite exists to end, and
    // it defeated the first version of this harness.
    if (!allowUnimplemented && hc.refused.isNotEmpty) {
      fail(
        'The test reached for host methods the fake does not answer, and the data '
        'source silently degraded each one to an empty result -- so whatever this '
        'test asserted, it asserted against NOTHING:\n'
        '  ${hc.refused.join('\n  ')}\n'
        'Implement them in FakeHealthConnect from the fixture, or pass '
        'allowUnimplemented: true if the test genuinely does not care.',
      );
    }
    container.dispose();
    database.close();
  });

  return HealthHarness(container, hc, HcFixture.load());
}
