import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_models.dart';

void main() {
  group('AppleHealthImportProgress.percent', () {
    test('is unavailable until selected record total is known', () {
      const progress = AppleHealthImportProgress(
        phase: AppleHealthImportPhase.parsing,
        convertedRecords: 4,
      );

      expect(progress.percent, isNull);
    });

    test('uses selected record total as denominator', () {
      const progress = AppleHealthImportProgress(
        phase: AppleHealthImportPhase.parsing,
        convertedRecords: 6,
        notSelectedRecords: 2,
        expectedSelectedRecords: 8,
      );

      expect(progress.percent, 44);
    });

    test('does not count unselected records or generic skips as progress', () {
      const progress = AppleHealthImportProgress(
        phase: AppleHealthImportPhase.parsing,
        convertedRecords: 10,
        notSelectedRecords: 6,
        skippedRecords: 3,
        expectedSelectedRecords: 8,
      );

      expect(progress.percent, 44);
    });

    test('uses raw scan progress when the analyzed element total is known', () {
      // Kotlin `percent uses raw scan progress when analyzed element total is
      // known`: the scan percent WINS over the selected one. Selected-only would
      // read 8/8 → 88 here; the export is only 2/8 scanned, so 22 is the truth.
      const progress = AppleHealthImportProgress(
        phase: AppleHealthImportPhase.parsing,
        parsedRecords: 2,
        convertedRecords: 8,
        expectedSelectedRecords: 8,
        expectedParsedElements: 8,
      );

      expect(progress.percent, 22);
    });

    test('raw scan progress advances across unselected record sections', () {
      // THE regression case. `selectedPreparedRecords` is 1 in both snapshots —
      // the old selected-only formula returns the same number twice while the
      // importer streams past five unselected records.
      const before = AppleHealthImportProgress(
        phase: AppleHealthImportPhase.parsing,
        parsedRecords: 2,
        convertedRecords: 1,
        expectedSelectedRecords: 4,
        expectedParsedElements: 10,
      );
      final afterUnselectedRecords = before.copyWith(
        parsedRecords: 7,
        convertedRecords: 6,
        notSelectedRecords: 5,
      );

      expect(before.selectedPreparedRecords, 1);
      expect(afterUnselectedRecords.selectedPreparedRecords, 1);
      expect(before.percent, 18);
      expect(afterUnselectedRecords.percent, 62);
    });

    test('a complete phase is 100 even with no totals at all', () {
      // The COMPLETE check now runs BEFORE the denominator check (Kotlin moved
      // it), so a finished import no longer reports "unknown".
      const progress = AppleHealthImportProgress(
        phase: AppleHealthImportPhase.complete,
      );

      expect(progress.percent, 100);
    });

    test('reserves final steps for duplicate checks, writing and report', () {
      const duplicateCheck = AppleHealthImportProgress(
        phase: AppleHealthImportPhase.checkingDuplicates,
        convertedRecords: 8,
        expectedSelectedRecords: 8,
      );

      expect(duplicateCheck.percent, 88);
      expect(
        duplicateCheck.copyWith(phase: AppleHealthImportPhase.writing).percent,
        92,
      );
      expect(
        duplicateCheck
            .copyWith(phase: AppleHealthImportPhase.buildingReport)
            .percent,
        98,
      );
      expect(
        duplicateCheck.copyWith(phase: AppleHealthImportPhase.complete).percent,
        100,
      );
    });
  });
}
