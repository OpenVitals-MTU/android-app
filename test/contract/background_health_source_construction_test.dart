import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// `HealthConnectNativeDataSource` may be constructed in exactly two places:
/// the DI graph (where `HealthConnectGate` mounts the availability refresh) and
/// `openBackgroundHealthAccess` (which refreshes before handing the source to
/// an isolate). Anywhere else is an un-refreshed source waiting to read empty
/// with no error — the bug class background_health_access.dart exists to end,
/// after it shipped four separate times.
///
/// A text scan, not an analyzer pass: the failure mode is someone adding a
/// constructor call, and a constructor call is textual.
const _sanctioned = {
  'lib/data/source/health/native/health_connect_native_data_source.dart',
  'lib/di/data_providers.dart',
  'lib/bootstrap/background_health_access.dart',
};

void main() {
  test(
      'no isolate can hand-build a HealthConnectNativeDataSource that skips '
      'the availability refresh', () {
    final offenders = <String>[];
    for (final entity in Directory('lib').listSync(recursive: true)) {
      if (entity is! File || !entity.path.endsWith('.dart')) continue;
      final path = entity.path.replaceAll(r'\', '/');
      if (_sanctioned.contains(path)) continue;
      if (entity
          .readAsStringSync()
          .contains('HealthConnectNativeDataSource(')) {
        offenders.add(path);
      }
    }

    expect(offenders, isEmpty,
        reason: 'These files construct HealthConnectNativeDataSource outside '
            'the sanctioned sites. Route them through '
            'openBackgroundHealthAccess() (isolates) or the DI graph '
            '(screens), or the source reads empty until someone remembers '
            'refreshAvailability() — again.');
  });
}
