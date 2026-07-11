import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/diagnostics/diagnostics_build_config.dart';

/// Compile-time proof that [kDiagnosticsEnabled] is a genuine constant: a
/// `const` variable's initializer is a const context, so this only compiles if
/// `kDebugMode || bool.fromEnvironment(...)` folds at compile time. That is what
/// lets the tree shaker drop the diagnostics UI from a plain release build.
const bool _constFolds = kDiagnosticsEnabled;

/// Same, in a const collection — a non-const value would be a compile error.
const List<bool> _constContext = <bool>[kDiagnosticsEnabled];

void main() {
  test('is enabled under the test environment (a debug build)', () {
    expect(kDebugMode, isTrue, reason: 'flutter test runs a debug build');
    expect(kDiagnosticsEnabled, isTrue);
    expect(_constFolds, isTrue);
    expect(_constContext.single, isTrue);
  });

  test('mirrors Kotlin BuildConfig.OPENVITALS_DIAGNOSTICS, not BuildConfig.DEBUG',
      () {
    // Kotlin sets OPENVITALS_DIAGNOSTICS = true for debug OR ci OR nightly. The
    // nightly channel is a plain *release* Flutter build, so the gate must have
    // a second, non-kDebugMode input that CI can switch on with
    // --dart-define=OPENVITALS_DIAGNOSTICS=true.
    const bool fromDefine = bool.fromEnvironment('OPENVITALS_DIAGNOSTICS');
    expect(
      kDiagnosticsEnabled,
      kDebugMode || fromDefine,
      reason: 'the gate is exactly kDebugMode || the OPENVITALS_DIAGNOSTICS '
          'dart-define',
    );
    // No dart-define is passed to `flutter test`, so the define half is off and
    // kDebugMode alone is carrying the gate here.
    expect(fromDefine, isFalse);
  });

  group('the gate sites consult kDiagnosticsEnabled, not kDebugMode', () {
    // A source guard: under `flutter test` kDebugMode and kDiagnosticsEnabled
    // are both true, so no runtime assertion can tell the two gates apart. The
    // regression we actually care about — a nightly release build silently
    // losing its diagnostics UI because the gate reverted to kDebugMode — is
    // only observable in the source.
    for (final path in const [
      'lib/navigation/app_router.dart',
      'lib/features/settings/settings_screen.dart',
    ]) {
      test(path, () {
        final source = File(path).readAsStringSync();
        expect(
          source,
          contains('kDiagnosticsEnabled'),
          reason: '$path must gate SettingsSection.debugDiagnostics on the '
              'diagnostics build config',
        );
        expect(
          source,
          isNot(contains('kDebugMode')),
          reason: '$path must not gate on kDebugMode: a nightly build is a '
              'release build and would lose the diagnostics UI entirely',
        );
      });
    }
  });
}
