// The onboarding rows are a pure derivation of the permission catalog against
// the granted set: what each row still needs, and whether the runtime dialog can
// even ask for it. The screen used to work all of this out in `build`.

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/model/onboarding_permission_category.dart';
import 'package:openvitals/features/onboarding/application/onboarding_display.dart';

const _catalog = OnboardingPermissionCatalog(
  categories: [
    OnboardingPermissionCategory(
      id: 'activity_sleep',
      permissions: {'steps', 'sleep'},
      isRequired: true,
    ),
    OnboardingPermissionCategory(
      id: 'additional_data_access',
      permissions: {'routes'},
      manualPermissions: {'routes'},
    ),
    OnboardingPermissionCategory(
      id: 'mindfulness',
      permissions: {'mindfulness'},
      available: false,
    ),
  ],
  minimumPermissions: {'steps', 'sleep'},
  allPermissions: {'steps', 'sleep', 'routes', 'mindfulness'},
);

OnboardingCategoryRow _row(OnboardingDisplay display, String id) =>
    display.rows.firstWhere((r) => r.category.id == id);

void main() {
  test('a fresh install: nothing granted, everything outstanding', () {
    final display = buildOnboardingDisplay(_catalog, const <String>{});

    expect(display.minimumGranted, isFalse);
    expect(display.missingMinimum, {'steps', 'sleep'});
    // The optional offer is everything else — the minimum is not counted twice.
    expect(display.missingOptional, {'routes', 'mindfulness'});

    final activity = _row(display, 'activity_sleep');
    expect(activity.total, 2);
    expect(activity.grantedCount, 0);
    expect(activity.fullyGranted, isFalse);
    expect(activity.partial, isFalse);
    expect(activity.missingRequestable, {'steps', 'sleep'});
    expect(activity.isManualGrant, isFalse);
  });

  test('a partially granted category reports the count, not just the flag', () {
    final display = buildOnboardingDisplay(_catalog, const {'steps'});

    final activity = _row(display, 'activity_sleep');
    expect(activity.grantedCount, 1);
    expect(activity.partial, isTrue);
    expect(activity.fullyGranted, isFalse);
    expect(activity.missingRequestable, {'sleep'});
    expect(display.missingMinimum, {'sleep'});
    expect(display.minimumGranted, isFalse);
  });

  test('the minimum being granted is what turns the button into Continue', () {
    final display = buildOnboardingDisplay(_catalog, const {'steps', 'sleep'});

    expect(display.minimumGranted, isTrue);
    expect(display.missingMinimum, isEmpty);
    expect(display.missingOptional, {'routes', 'mindfulness'});
    expect(_row(display, 'activity_sleep').fullyGranted, isTrue);
  });

  test('a manual-only category cannot be granted by the runtime dialog', () {
    final display = buildOnboardingDisplay(_catalog, const <String>{});

    final additional = _row(display, 'additional_data_access');
    // Its only missing permission is manual-only: no dialog, "Open settings".
    expect(additional.missingRequestable, isEmpty);
    expect(additional.missingManual, {'routes'});
    expect(additional.isManualGrant, isTrue);

    // Once granted it is neither manual nor outstanding.
    final granted = buildOnboardingDisplay(_catalog, const {'routes'});
    expect(_row(granted, 'additional_data_access').isManualGrant, isFalse);
    expect(_row(granted, 'additional_data_access').fullyGranted, isTrue);
  });

  test('an unsupported category is never "granted", whatever is in the set', () {
    // The device does not do mindfulness: the row is locked, not complete.
    final display = buildOnboardingDisplay(_catalog, const {'mindfulness'});

    final mindfulness = _row(display, 'mindfulness');
    expect(mindfulness.grantedCount, 1);
    expect(mindfulness.fullyGranted, isFalse);
    expect(mindfulness.partial, isFalse);
    expect(mindfulness.isManualGrant, isFalse);
  });

  test('an empty catalog derives an empty display', () {
    final display = buildOnboardingDisplay(
      const OnboardingPermissionCatalog(
        categories: [],
        minimumPermissions: {},
        allPermissions: {},
      ),
      const <String>{},
    );

    expect(display.rows, isEmpty);
    expect(display.missingMinimum, isEmpty);
    expect(display.missingOptional, isEmpty);
    // Nothing required is missing, so onboarding can be finished.
    expect(display.minimumGranted, isTrue);
  });
}
