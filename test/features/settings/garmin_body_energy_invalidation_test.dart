import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/devices/garmin/garmin_directory.dart';
import 'package:openvitals/devices/garmin/garmin_file_types.dart';
import 'package:openvitals/devices/garmin/garmin_session.dart';
import 'package:openvitals/features/settings/application/garmin_device_sync_port.dart';

void main() {
  GarminDownloadedFile file(DateTime? fileDate, {int index = 1}) =>
      GarminDownloadedFile(
        entry: GarminDirectoryEntry(
          fileIndex: index,
          type: GarminFileType.monitor,
          fileNumber: index,
          specificFlags: 0,
          fileFlags: 0,
          fileSize: 64,
          fileDate: fileDate,
        ),
        bytes: Uint8List(0),
      );

  group('garminEarliestAffectedDay', () {
    test('is the oldest dated file, whatever order they arrive in', () {
      final earliest = garminEarliestAffectedDay([
        file(DateTime(2026, 7, 24, 9), index: 1),
        file(DateTime(2026, 7, 21, 23), index: 2),
        file(DateTime(2026, 7, 26, 1), index: 3),
      ]);

      // A sync that hands over the 21st invalidates from the 21st: Body Energy
      // chains, so every later day was seeded from a score computed without it.
      expect(earliest, const LocalDate(2026, 7, 21));
    });

    test('ignores undated files rather than guessing at them', () {
      final earliest = garminEarliestAffectedDay([
        file(null, index: 1),
        file(DateTime(2026, 7, 23, 6), index: 2),
        file(null, index: 3),
      ]);

      expect(earliest, const LocalDate(2026, 7, 23));
    });

    test('is null when the watch dated nothing', () {
      // The vívoactive 5 really does send the "no date" sentinel for some
      // files. Invalidating from an invented date would be worse than leaving
      // the settling window to cover it.
      expect(garminEarliestAffectedDay([file(null), file(null)]), isNull);
    });

    test('is null for an empty sync', () {
      expect(garminEarliestAffectedDay(const []), isNull);
    });

    test('resolves the local day, not the UTC one', () {
      // A file recorded just before local midnight belongs to that local day;
      // taking the UTC date would invalidate from the wrong side of it.
      final localLateEvening = DateTime(2026, 7, 22, 23, 30);
      expect(
        garminEarliestAffectedDay([file(localLateEvening)]),
        const LocalDate(2026, 7, 22),
      );
    });
  });
}
