import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_checkpoint_store.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_models.dart';

void main() {
  late Directory importDirectory;

  setUp(() {
    importDirectory =
        Directory.systemTemp.createTempSync('apple_health_checkpoint_test');
  });

  tearDown(() {
    if (importDirectory.existsSync()) {
      importDirectory.deleteSync(recursive: true);
    }
  });

  AppleHealthImportCheckpointStore store() => AppleHealthImportCheckpointStore(
        directory: () async => importDirectory,
      );

  const sourceKey = 'content://downloads/1|export.zip|4096';
  const categories = {
    AppleHealthImportCategory.heart,
    AppleHealthImportCategory.body,
  };

  const checkpoint = AppleHealthImportCheckpoint(
    sourceKey: sourceKey,
    selectedCategories: categories,
    committedSelectedRecords: 300,
    importedRecords: 280,
    duplicateSkippedRecords: 15,
    failedRecords: 5,
    typeStats: {
      'HKQuantityTypeIdentifierHeartRate': AppleHealthImportCheckpointTypeStats(
        imported: 280,
        duplicateSkipped: 15,
        failed: 5,
      ),
    },
  );

  group('AppleHealthImportCheckpointStore', () {
    test('round-trips a checkpoint for the same source and categories',
        () async {
      await store().save(checkpoint);

      final loaded = await store().load(sourceKey, categories);

      expect(loaded, isNotNull);
      expect(loaded!.committedSelectedRecords, 300);
      expect(loaded.importedRecords, 280);
      expect(loaded.duplicateSkippedRecords, 15);
      expect(loaded.failedRecords, 5);
      expect(
        loaded.typeStats['HKQuantityTypeIdentifierHeartRate'],
        const AppleHealthImportCheckpointTypeStats(
          imported: 280,
          duplicateSkipped: 15,
          failed: 5,
        ),
      );
      expect(loaded.selectedCategories, categories);
    });

    test('is not reused when the source key differs', () async {
      await store().save(checkpoint);

      final loaded = await store().load(
        'content://downloads/2|other.zip|4096',
        categories,
      );

      expect(loaded, isNull);
    });

    test('is not reused when the selected categories differ', () async {
      await store().save(checkpoint);

      expect(
        await store().load(sourceKey, {AppleHealthImportCategory.heart}),
        isNull,
      );
      expect(
        await store().load(sourceKey, {
          ...categories,
          AppleHealthImportCategory.sleep,
        }),
        isNull,
      );
    });

    test('load returns null when nothing was ever written', () async {
      expect(await store().load(sourceKey, categories), isNull);
    });

    test('clear removes the checkpoint', () async {
      await store().save(checkpoint);
      await store().clear();

      expect(await store().load(sourceKey, categories), isNull);
      expect(File('${importDirectory.path}/checkpoint').existsSync(), isFalse);
    });
  });
}
