import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/bootstrap/background_health_access.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';

import '../support/health_connect/fake_health_connect.dart';

/// The refresh-before-handoff contract. `cachedAvailability` starts at
/// `notSupported` and every repository gates on it, so a data source handed to
/// an isolate un-refreshed reads empty with no error — the bug class that hit
/// four separate features (home widgets, one-tap logging, both reminder
/// alarms). These pin that [openBackgroundHealthAccess] can never hand one out.
void main() {
  setUp(() => SharedPreferences.setMockInitialValues(const {}));

  test('hands back a data source whose availability is already resolved',
      () async {
    final result = await openBackgroundHealthAccess(
      hostApi: FakeHealthConnect(),
    );

    final source = result.orThrow();
    expect(source.cachedAvailability, HealthConnectAvailability.available,
        reason: 'still at the constructor default would mean the refresh '
            'never ran — the silent-empty-reads bug');
  });

  test('an unavailable provider is an answer, not an error', () async {
    final result = await openBackgroundHealthAccess(
      hostApi: FakeHealthConnect(sdkStatus: 1),
    );

    // The isolate gets a working source that KNOWS Health Connect is absent —
    // its reads degrade to documented empties instead of the caller crashing.
    final source = result.orThrow();
    expect(source.cachedAvailability, HealthConnectAvailability.notSupported);
  });
}
