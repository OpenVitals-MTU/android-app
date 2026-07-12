import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/source/health/native/health_connect_native_data_source.dart';
import 'health_connect_native_data_source_test.dart';
import 'package:openvitals/domain/health/health_permissions.dart';

/// The Health Connect mindfulness opt-in.
///
/// A Health Connect module on a de-Googled ROM can DEFINE the mindfulness
/// permission and report `FEATURE_MINDFULNESS_SESSION` as available, while its
/// own permission screen has no category for it and throws
/// `IllegalArgumentException: No Category for fitness permission type
/// MINDFULNESS` the moment it is asked to draw a row for it. The system Health
/// Connect app dies, and the user can then grant this app *nothing at all*.
///
/// The permission screen renders the permissions we ASK FOR
/// (`FitnessPermissionsFragment.updateDataList`), so not asking is what keeps
/// that phone usable. There is no API that tells us the UI is broken, so the
/// device's own "yes" is not enough on its own — a user has to say yes too.
void main() {
  HealthPermissionService serviceWith({required bool mindfulnessAvailable}) =>
      HealthPermissionService(
        HealthConnectFeatureFlags(mindfulnessAvailable: mindfulnessAvailable),
        const <String>{},
      );

  test('with the integration off, mindfulness is never asked for', () {
    // This is the flag the data source folds the opt-in into: device says yes
    // AND user says yes. With the user's half missing it lands here.
    final service = serviceWith(mindfulnessAvailable: false);

    expect(service.mindfulnessPermissions, isEmpty);
    expect(service.mindfulnessWritePermissions, isEmpty);
    // And — the part that actually matters — it is absent from the sets the app
    // hands to Health Connect when it asks for permissions. A permission we do
    // not request cannot be drawn, and cannot crash the screen drawing it.
    expect(
      service.allPermissions.where((p) => p.contains('MINDFULNESS')),
      isEmpty,
    );
    expect(
      service.managedPermissions.where((p) => p.contains('MINDFULNESS')),
      isEmpty,
    );
  });

  test('with it on, and a device that supports it, we ask as before', () {
    final service = serviceWith(mindfulnessAvailable: true);

    expect(service.mindfulnessPermissions, isNotEmpty);
    expect(service.mindfulnessWritePermissions, isNotEmpty);
    expect(
      service.allPermissions.where((p) => p.contains('MINDFULNESS')),
      isNotEmpty,
    );
  });

  test('turning it off costs mindfulness and nothing else', () {
    final off = serviceWith(mindfulnessAvailable: false);
    final on = serviceWith(mindfulnessAvailable: true);

    // Every other permission the app asks for is untouched: the point of the
    // opt-in is that the user can still grant the other twenty-odd metrics on a
    // phone whose Health Connect cannot cope with this one.
    final lostByOptingOut = on.allPermissions.difference(off.allPermissions);
    expect(
      lostByOptingOut.every((permission) => permission.contains('MINDFULNESS')),
      isTrue,
      reason: 'opting out of mindfulness must not drop any other permission',
    );
    expect(lostByOptingOut, isNotEmpty);
  });

  group('the data source folds the opt-in into the device answer', () {
    test('a device that says YES is still refused while the user has not',
        () async {
      // This is the reported phone: the module reports the feature available
      // (it defines the permission and the enum) and its permission UI still
      // cannot draw it. The device's answer alone must not be enough.
      final api = FakeHostApi()
        ..availableFeatures = {'MINDFULNESS_SESSION', 'SKIN_TEMPERATURE'};
      final source = HealthConnectNativeDataSource(
        hostApi: api,
        appPackageName: 'tech.mmarca.openvitals',
        mindfulnessIntegrationEnabled: () => false,
      );

      final flags = await source.resolveFeatureFlags();

      expect(flags.mindfulnessAvailable, isFalse);
      // Only mindfulness is withheld — the rest of the device's answer stands.
      expect(flags.skinTemperatureAvailable, isTrue);
      expect(source.permissionService.mindfulnessPermissions, isEmpty);
    });

    test('both halves say yes, and the feature comes back', () async {
      final api = FakeHostApi()
        ..availableFeatures = {'MINDFULNESS_SESSION'};
      final source = HealthConnectNativeDataSource(
        hostApi: api,
        appPackageName: 'tech.mmarca.openvitals',
        mindfulnessIntegrationEnabled: () => true,
      );

      final flags = await source.resolveFeatureFlags();

      expect(flags.mindfulnessAvailable, isTrue);
      expect(source.permissionService.mindfulnessPermissions, isNotEmpty);
    });

    test('the user says yes but the device does not — still no', () async {
      final api = FakeHostApi()..availableFeatures = <String>{};
      final source = HealthConnectNativeDataSource(
        hostApi: api,
        appPackageName: 'tech.mmarca.openvitals',
        mindfulnessIntegrationEnabled: () => true,
      );

      expect((await source.resolveFeatureFlags()).mindfulnessAvailable, isFalse);
    });
  });
}