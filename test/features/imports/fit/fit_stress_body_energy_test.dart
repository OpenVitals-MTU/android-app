import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/features/manualentry/activity/routeimport/fit_route_parser.dart';

/// Garmin device epoch: seconds between the Unix and Garmin epochs.
const int _garminEpochOffset = 631065600;

int _fitTimestamp(DateTime t) =>
    t.toUtc().millisecondsSinceEpoch ~/ 1000 - _garminEpochOffset;

/// Minimal FIT writer — just enough to build a monitoring file carrying
/// `stress_level` (227) records.
class _W {
  final List<int> _b = [];
  void u8(int v) => _b.add(v & 0xFF);
  void i8(int v) => _b.add(v & 0xFF);
  void u16(int v) => _b..addAll([v & 0xFF, (v >> 8) & 0xFF]);
  void u32(int v) => _b.addAll([
        v & 0xFF,
        (v >> 8) & 0xFF,
        (v >> 16) & 0xFF,
        (v >> 24) & 0xFF,
      ]);
  void bytes(List<int> v) => _b.addAll(v);
  Uint8List toBytes() => Uint8List.fromList(_b);
}

/// `stress_level`: field 0 stress (sint8), 1 time (uint32), 3 body energy (uint8).
Uint8List _stressFile(List<(DateTime, int stress, int energy)> samples) {
  final d = _W()
    // file_id (global 0), just the type so the file classifies as monitoring.
    ..u8(0x40)
    ..u8(0)
    ..u8(0)
    ..u16(0)
    ..u8(1)
    ..bytes([0, 1, 0]) // field 0 (type), size 1, base enum
    ..u8(0)
    ..u8(32) // FILE_TYPE monitoring
    // stress_level definition
    ..u8(0x41)
    ..u8(0)
    ..u8(0)
    ..u16(227)
    ..u8(3)
    ..bytes([0, 1, 1]) // stress value, sint8
    ..bytes([1, 4, 134]) // stress time, uint32
    ..bytes([3, 1, 2]); // body energy, uint8
  for (final (at, stress, energy) in samples) {
    d.u8(0x01);
    d.i8(stress);
    d.u32(_fitTimestamp(at));
    d.u8(energy);
  }
  final data = d.toBytes();

  final f = _W()
    ..u8(14)
    ..u8(16)
    ..u16(0)
    ..u32(data.length)
    ..bytes([0x2E, 0x46, 0x49, 0x54])
    ..u16(0)
    ..bytes(data)
    ..u16(0);
  return f.toBytes();
}

void main() {
  test('extracts stress and body energy from the stress_level message', () {
    final t0 = DateTime.utc(2026, 7, 22, 10, 1);
    final m = FitRouteParser.parseWellness(_stressFile([
      (t0, 42, 72),
      (t0.add(const Duration(minutes: 1)), 51, 72),
    ])).monitoring!;

    // Both series come off ONE message — Body Battery has no message of its own.
    expect(m.stress, [(t0, 42), (t0.add(const Duration(minutes: 1)), 51)]);
    expect(m.bodyEnergy, [(t0, 72), (t0.add(const Duration(minutes: 1)), 72)]);
  });

  test('a negative stress score is dropped, not clamped', () {
    final t0 = DateTime.utc(2026, 7, 22, 10, 1);
    final m = FitRouteParser.parseWellness(_stressFile([
      (t0, -23, 72), // Garmin's "not measurable": asleep, moving, poor contact
      (t0.add(const Duration(minutes: 1)), 30, 71),
    ])).monitoring!;

    // Recording it as 0 would read as "completely relaxed", which is a lie.
    expect(m.stress, [(t0.add(const Duration(minutes: 1)), 30)]);
    // Body energy is still valid on that same record, so it survives.
    expect(m.bodyEnergy, hasLength(2));
  });

  test('the stress message alone makes a file non-empty', () {
    // Without this the monitoring summary would be dropped as empty and the
    // samples never reach the database.
    final m = FitRouteParser.parseWellness(
      _stressFile([(DateTime.utc(2026, 7, 22, 10), 40, 70)]),
    ).monitoring;
    expect(m, isNotNull);
    expect(m!.isEmpty, isFalse);
  });

  test('uses the message own time field, not the record header', () {
    // stress_level carries its own timestamp; Gadgetbridge prefers it too.
    final at = DateTime.utc(2026, 7, 22, 3, 45);
    final m = FitRouteParser.parseWellness(_stressFile([(at, 20, 90)]))
        .monitoring!;
    expect(m.stress.single.$1, at);
  });
}
