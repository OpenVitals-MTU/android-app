import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/permission_grant_mode.dart';
import 'package:openvitals/domain/health/health_permissions.dart';

void main() {
  const service = HealthPermissionService();

  group('phased permission sets', () {
    test('PERMISSION_SET_VERSION is 3', () {
      expect(HealthPermissionService.PERMISSION_SET_VERSION, 3);
    });

    test('phase1 == core == steps/distance/exercise/sleep reads', () {
      expect(service.phase1Permissions, service.corePermissions);
      expect(
        service.corePermissions,
        {
          HcPermissions.readSteps,
          HcPermissions.readDistance,
          HcPermissions.readExercise,
          HcPermissions.readSleep,
        },
      );
    });

    test('phase2 covers heart, body, activity-extras and nutrition/hydration', () {
      expect(service.phase2Permissions, containsAll(service.heartPermissions));
      expect(service.phase2Permissions, containsAll(service.bodyPermissions));
      expect(
        service.phase2Permissions,
        containsAll(service.nutritionHydrationPermissions),
      );
    });

    test('phase3 == vitals reads; phase4 == cycle reads', () {
      expect(service.phase3Permissions, service.vitalsPermissions);
      expect(service.phase4Permissions, service.cyclePermissions);
    });

    test('manual-only == route permissions and drives grant mode', () {
      expect(service.manualOnlyPermissions, service.routePermissions);
      expect(
        service.grantModeFor(HealthPermissionService.readExerciseRoutesPermission),
        PermissionGrantMode.manual,
      );
      expect(
        service.grantModeFor(HcPermissions.readSteps),
        PermissionGrantMode.requestable,
      );
    });

    test('managed permissions include reads, writes and route', () {
      final managed = service.managedPermissions;
      expect(managed, containsAll(service.corePermissions));
      expect(managed, containsAll(service.activityWritePermissions));
      expect(managed, containsAll(service.dataImportWritePermissions));
      expect(managed, contains(HealthPermissionService.readExerciseRoutesPermission));
    });
  });

  group('feature gating', () {
    test('mindfulness excluded from phase2 / requestable writes when unavailable',
        () {
      expect(
        service.phase2Permissions.contains(HcPermissions.readMindfulness),
        isFalse,
      );
      expect(
        service.requestableWritePermissions
            .contains(HcPermissions.writeMindfulness),
        isFalse,
      );
    });

    test('mindfulness included when the feature flag is set', () {
      const withMindfulness = HealthPermissionService(
        HealthConnectFeatureFlags(mindfulnessAvailable: true),
      );
      expect(
        withMindfulness.phase2Permissions.contains(HcPermissions.readMindfulness),
        isTrue,
      );
    });

    // Kotlin 1.9.0 (1f2b435) moved the availability check into the getters
    // themselves, because the per-call-site guards had been forgotten in
    // allPermissions and managedPermissions — so an unsupported device still
    // asked for a mindfulness permission its provider does not define, and the
    // request could never be granted.
    test('mindfulness permissions are empty when the provider lacks it', () {
      expect(service.mindfulnessPermissions, isEmpty);
      expect(service.mindfulnessWritePermissions, isEmpty);
    });

    test('an unavailable mindfulness leaks into NO permission set', () {
      for (final set in <Set<String>>{
        service.allPermissions,
        service.managedPermissions,
        service.dataImportWritePermissions,
        service.phase2Permissions,
        service.requestableWritePermissions,
        service.requiredOnboardingPermissions,
      }) {
        expect(set.contains(HcPermissions.readMindfulness), isFalse);
        expect(set.contains(HcPermissions.writeMindfulness), isFalse);
      }
    });

    // The opt-in exists because some providers crash their own permission screen
    // on mindfulness, taking every other permission down with them. Onboarding
    // asks for everything in ONE request, so if mindfulness could get into it an
    // opted-in user on such a device could not grant anything at all.
    test('an AVAILABLE mindfulness still stays out of the required set', () {
      const withMindfulness = HealthPermissionService(
        HealthConnectFeatureFlags(mindfulnessAvailable: true),
      );
      expect(withMindfulness.mindfulnessPermissions, isNotEmpty);
      expect(
        withMindfulness.requiredOnboardingPermissions
            .where((p) => p.contains('MINDFULNESS')),
        isEmpty,
      );
    });

    test('the device answer and the opt-in are separate flags', () {
      // Device says yes, user has not opted in: offer the switch, ask for
      // nothing.
      const deviceOnly = HealthPermissionService(
        HealthConnectFeatureFlags(mindfulnessSupportedByDevice: true),
      );
      expect(deviceOnly.isMindfulnessSupportedByDevice(), isTrue);
      expect(deviceOnly.isMindfulnessAvailable(), isFalse);
      expect(deviceOnly.mindfulnessPermissions, isEmpty);

      // Omitting the device flag defaults it to the resolved answer, so the
      // existing single-flag construction keeps meaning what it meant.
      const both = HealthPermissionService(
        HealthConnectFeatureFlags(mindfulnessAvailable: true),
      );
      expect(both.isMindfulnessSupportedByDevice(), isTrue);
    });

    test('cycle tracking stays out of the required set, both directions', () {
      expect(
        service.requiredOnboardingPermissions
            .intersection(service.cyclePermissions),
        isEmpty,
      );
      expect(
        service.requiredOnboardingPermissions
            .intersection(service.cycleWritePermissions),
        isEmpty,
      );
      // But the import set still carries the cycle writes — the split is for
      // onboarding's benefit, not the CSV importer's.
      expect(
        service.dataImportWritePermissions,
        containsAll(service.cycleWritePermissions),
      );
    });

    test('skin temperature gated on the feature flag', () {
      expect(service.vitalsPermissions.contains(HcPermissions.readSkinTemperature),
          isFalse);
      const withSkin = HealthPermissionService(
        HealthConnectFeatureFlags(skinTemperatureAvailable: true),
      );
      expect(
        withSkin.vitalsPermissions.contains(HcPermissions.readSkinTemperature),
        isTrue,
      );
    });
  });
}
