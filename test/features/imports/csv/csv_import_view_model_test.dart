import 'dart:io';

import 'package:cross_file/cross_file.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/command_state.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/import_write_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/features/imports/csv/application/csv_import_view_model.dart';
import 'package:openvitals/features/imports/csv/csv_column_mapping.dart';
import 'package:openvitals/features/imports/csv/csv_datetime_format.dart';
import 'package:openvitals/features/imports/csv/csv_import_metric.dart';
import 'package:openvitals/features/imports/csv/csv_import_models.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _writeWeight = 'android.permission.health.WRITE_WEIGHT';

class _FakeHealthDataSource extends HealthDataSource {
  /// [unsupported] stands in for what `resolveSupportedPermissions` would have
  /// found: permissions the installed provider does not define. It feeds the
  /// base class's setter, which is what `permissionService` filters through.
  _FakeHealthDataSource({
    this.granted = const <String>{},
    Set<String> unsupported = const <String>{},
  }) {
    unsupportedPermissions = unsupported;
  }

  Set<String> granted;

  @override
  Future<HealthConnectAvailability> availability() async =>
      HealthConnectAvailability.available;

  @override
  Future<Set<String>> grantedPermissions() async => granted;
}

class _FakeImportWriteRepository implements ImportWriteRepository {
  final List<ImportRecord> inserted = [];

  @override
  bool isMindfulnessAvailable() => true;

  @override
  Future<Result<void>> insertImportedRecords(List<ImportRecord> records) async {
    inserted.addAll(records);
    return const Ok(null);
  }

  @override
  Future<Result<Set<String>>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  ) async =>
      const Ok(<String>{});
}

void main() {
  late Directory dir;
  late _FakeImportWriteRepository repository;

  setUp(() {
    dir = Directory.systemTemp.createTempSync('csv_vm_test');
    repository = _FakeImportWriteRepository();
  });
  tearDown(() => dir.deleteSync(recursive: true));

  File writeCsv(String name, String content) =>
      File('${dir.path}/$name')..writeAsStringSync(content);

  /// The Withings shape, three rows.
  File withingsFile() => writeCsv(
        'withings.csv',
        'Date,"Weight (kg)","Fat mass (kg)","Bone mass (kg)"\n'
            '2026-07-01 08:12:00,78.4,15.2,3.1\n'
            '2026-07-02 08:14:00,78.6,15.3,3.1\n'
            '2026-07-03 08:11:00,78.2,15.1,3.1\n',
      );

  Future<ProviderContainer> boot({
    Set<String> granted = const {},
    Set<String> unsupported = const {},
  }) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    final container = ProviderContainer(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        healthDataSourceProvider.overrideWithValue(
          _FakeHealthDataSource(granted: granted, unsupported: unsupported),
        ),
        importWriteRepositoryProvider.overrideWithValue(repository),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  Future<XFile?> Function() picker(File file) => () async => XFile(file.path);

  test('picking a file advances to the mapping step and exposes its columns',
      () async {
    final container = await boot();
    final notifier = container.read(csvImportProvider.notifier);

    await notifier.pickFile(picker: picker(withingsFile()));

    final state = container.read(csvImportProvider);
    expect(state.step, CsvImportStep.mapping);
    expect(state.sample?.headerRow, [
      'Date',
      'Weight (kg)',
      'Fat mass (kg)',
      'Bone mass (kg)',
    ]);
  });

  test(
    'the app-open permission refresh does not discard a file already picked',
    () async {
      // The "you have to pick the file twice" bug. Returning from the system
      // file picker is an app open, and data_refresh_bootstrap invalidates
      // grantedHealthPermissionsProvider on every one. While build() WATCHED
      // that provider, the invalidation re-ran build() and reset the state,
      // silently dropping the picked file. It only bit on the first attempt
      // because the refresh is guarded to once per 30s.
      final container = await boot();
      final notifier = container.read(csvImportProvider.notifier);
      await notifier.pickFile(picker: picker(withingsFile()));
      expect(container.read(csvImportProvider).step, CsvImportStep.mapping);

      container.invalidate(grantedHealthPermissionsProvider);
      await container.read(grantedHealthPermissionsProvider.future);

      final state = container.read(csvImportProvider);
      expect(state.step, CsvImportStep.mapping);
      expect(state.fileName, 'withings.csv');
      expect(state.sample, isNotNull);
      expect(state.mapping, isNotNull);
    },
  );

  test(
    'a permission granted after the file was picked still reaches the state',
    () async {
      // The other half: listening must actually keep the granted set current,
      // not merely avoid resetting.
      final container = await boot();
      final notifier = container.read(csvImportProvider.notifier);
      await notifier.pickFile(picker: picker(withingsFile()));
      notifier.setColumnRole(
        1,
        role: CsvColumnRole.metric,
        metric: CsvImportMetric.weight,
      );
      expect(container.read(csvImportProvider).missingPermissions,
          {_writeWeight});

      // The user grants it in the Health Connect dialog; the app-open refresh
      // re-resolves the granted set.
      (container.read(healthDataSourceProvider) as _FakeHealthDataSource)
          .granted = {_writeWeight};
      container.invalidate(grantedHealthPermissionsProvider);
      await container.read(grantedHealthPermissionsProvider.future);

      expect(container.read(csvImportProvider).missingPermissions, isEmpty);
      // ...and the mapping is still there.
      expect(container.read(csvImportProvider).mapping, isNotNull);
    },
  );

  test('cancelling the file picker leaves the importer on the pick step',
      () async {
    final container = await boot();

    await container
        .read(csvImportProvider.notifier)
        .pickFile(picker: () async => null);

    expect(container.read(csvImportProvider).step, CsvImportStep.pick);
    expect(container.read(csvImportProvider).error, isNull);
  });

  test('the date column is pre-selected but no metric is guessed', () async {
    final container = await boot();

    await container
        .read(csvImportProvider.notifier)
        .pickFile(picker: picker(withingsFile()));

    final mapping = container.read(csvImportProvider).mapping!;
    expect(mapping.timestampColumn?.columnIndex, 0);
    expect(mapping.metricColumns, isEmpty);
  });

  test('a freshly picked file cannot continue until a metric is mapped',
      () async {
    final container = await boot();
    final notifier = container.read(csvImportProvider.notifier);

    await notifier.pickFile(picker: picker(withingsFile()));
    expect(container.read(csvImportProvider).canContinue, isFalse);

    notifier.setColumnRole(
      1,
      role: CsvColumnRole.metric,
      metric: CsvImportMetric.weight,
    );

    expect(container.read(csvImportProvider).canContinue, isTrue);
  });

  test("mapping a column defaults its unit from the column's own header", () async {
    final container = await boot();
    final notifier = container.read(csvImportProvider.notifier);

    await notifier.pickFile(picker: picker(withingsFile()));
    notifier.setColumnRole(
      1,
      role: CsvColumnRole.metric,
      metric: CsvImportMetric.weight,
    );

    final column = container
        .read(csvImportProvider)
        .mapping!
        .columns
        .firstWhere((it) => it.columnIndex == 1);
    expect(column.effectiveInterpretation, const CsvDirectValue(CsvUnit.kilograms));
  });

  test(
    'mapping fat mass in kg to body fat without a weight column blocks continuing',
    () async {
      final container = await boot();
      final notifier = container.read(csvImportProvider.notifier);

      await notifier.pickFile(picker: picker(withingsFile()));
      notifier.setColumnRole(
        2,
        role: CsvColumnRole.metric,
        metric: CsvImportMetric.bodyFat,
      );

      final state = container.read(csvImportProvider);
      expect(
        state.issues,
        contains(CsvMappingIssue.massShareNeedsWeightColumn),
      );
      expect(state.canContinue, isFalse);
    },
  );

  test('mapping the weight column too clears the derivation issue', () async {
    final container = await boot();
    final notifier = container.read(csvImportProvider.notifier);

    await notifier.pickFile(picker: picker(withingsFile()));
    notifier.setColumnRole(
      2,
      role: CsvColumnRole.metric,
      metric: CsvImportMetric.bodyFat,
    );
    notifier.setColumnRole(
      1,
      role: CsvColumnRole.metric,
      metric: CsvImportMetric.weight,
    );

    expect(container.read(csvImportProvider).canContinue, isTrue);
  });

  test('only the mapped metrics permissions are reported missing', () async {
    final container = await boot();
    final notifier = container.read(csvImportProvider.notifier);

    await notifier.pickFile(picker: picker(withingsFile()));
    notifier.setColumnRole(
      1,
      role: CsvColumnRole.metric,
      metric: CsvImportMetric.weight,
    );

    expect(container.read(csvImportProvider).missingPermissions, {_writeWeight});
  });

  test(
    'a permission the installed provider does not define is never requested',
    () async {
      // AGENTS.md invariant #5: the app pins a connect-client ahead of what most
      // providers implement, and REQUESTING an unsupported permission throws
      // rather than being refused. Before this was wired up, missingPermissions
      // returned the raw catalog set and would have asked for it.
      final container = await boot(
        unsupported: {'android.permission.health.WRITE_BODY_WATER_MASS'},
      );
      final notifier = container.read(csvImportProvider.notifier);

      await notifier.pickFile(picker: picker(withingsFile()));
      notifier.setColumnRole(
        3,
        role: CsvColumnRole.metric,
        metric: CsvImportMetric.bodyWaterMass,
      );

      expect(container.read(csvImportProvider).missingPermissions, isEmpty);
    },
  );

  test(
    'a supported permission is still requested when another is unsupported',
    () async {
      final container = await boot(
        unsupported: {'android.permission.health.WRITE_BODY_WATER_MASS'},
      );
      final notifier = container.read(csvImportProvider.notifier);

      await notifier.pickFile(picker: picker(withingsFile()));
      notifier.setColumnRole(
        1,
        role: CsvColumnRole.metric,
        metric: CsvImportMetric.weight,
      );
      notifier.setColumnRole(
        3,
        role: CsvColumnRole.metric,
        metric: CsvImportMetric.bodyWaterMass,
      );

      expect(
        container.read(csvImportProvider).missingPermissions,
        {_writeWeight},
      );
    },
  );

  test('an already-granted permission is not reported missing', () async {
    final container = await boot(granted: {_writeWeight});
    final notifier = container.read(csvImportProvider.notifier);
    // Resolve the granted-permissions provider before the view-model reads it.
    await container.read(grantedHealthPermissionsProvider.future);

    await notifier.pickFile(picker: picker(withingsFile()));
    notifier.setColumnRole(
      1,
      role: CsvColumnRole.metric,
      metric: CsvImportMetric.weight,
    );

    expect(container.read(csvImportProvider).missingPermissions, isEmpty);
  });

  test('a completed import writes one record per mapped metric per row',
      () async {
    final container = await boot();
    final notifier = container.read(csvImportProvider.notifier);

    await notifier.pickFile(picker: picker(withingsFile()));
    notifier.setColumnRole(
      1,
      role: CsvColumnRole.metric,
      metric: CsvImportMetric.weight,
    );
    notifier.setDateTimeSettings(
      const CsvDateTimeSettings(
        format: CsvDateTimeFormat.yearFirst,
        zone: CsvTimeZoneMode.utc,
      ),
    );
    await notifier.startImport();

    final state = container.read(csvImportProvider);
    expect(state.step, CsvImportStep.done);
    expect(state.result?.outcome, CsvImportOutcome.completed);
    expect(state.result?.progress.written, 3);
    expect(repository.inserted, hasLength(3));
  });

  test('the Withings fat-mass derivation writes body-fat percentages', () async {
    final container = await boot();
    final notifier = container.read(csvImportProvider.notifier);

    await notifier.pickFile(picker: picker(withingsFile()));
    notifier.setColumnRole(
      1,
      role: CsvColumnRole.metric,
      metric: CsvImportMetric.weight,
    );
    notifier.setColumnRole(
      2,
      role: CsvColumnRole.metric,
      metric: CsvImportMetric.bodyFat,
    );
    notifier.setDateTimeSettings(
      const CsvDateTimeSettings(
        format: CsvDateTimeFormat.yearFirst,
        zone: CsvTimeZoneMode.utc,
      ),
    );
    await notifier.startImport();

    final bodyFat =
        repository.inserted.whereType<BodyFatImportRecord>().toList();
    expect(bodyFat, hasLength(3));
    expect(bodyFat.first.percent, closeTo(19.39, 0.01));
  });

  group('saveReport', () {
    /// Runs a small import so there is a finished result to report on.
    Future<ProviderContainer> importedContainer() async {
      final container = await boot();
      final notifier = container.read(csvImportProvider.notifier);
      await notifier.pickFile(picker: picker(withingsFile()));
      notifier.setColumnRole(
        1,
        role: CsvColumnRole.metric,
        metric: CsvImportMetric.weight,
      );
      notifier.setDateTimeSettings(
        const CsvDateTimeSettings(
          format: CsvDateTimeFormat.yearFirst,
          zone: CsvTimeZoneMode.utc,
        ),
      );
      await notifier.startImport();
      return container;
    }

    test('the report describes the run that just finished', () async {
      final container = await importedContainer();

      final report = container.read(csvImportProvider.notifier).reportText;

      expect(report, contains('File: withings.csv'));
      expect(report, contains('Records written: 3'));
      expect(report, contains('[1] Weight (kg) -> weight (kilograms)'));
    });

    test('there is nothing to report before an import has run', () async {
      final container = await boot();

      expect(container.read(csvImportProvider.notifier).reportText, isNull);
    });

    test('a saved report ends the command in success carrying true', () async {
      final container = await importedContainer();
      String? savedName;

      await container.read(csvImportProvider.notifier).saveReport(
            saver: (content, name) async {
              savedName = name;
              return true;
            },
          );

      expect(
        container.read(csvImportProvider).saveReport,
        const CommandState<bool>.success(true),
      );
      expect(savedName, 'openvitals-csv-import-report.txt');
    });

    test('a cancelled save succeeds carrying false, not a failure', () async {
      // Cancelling a save picker is a choice, not an error, and must not be
      // rendered as one.
      final container = await importedContainer();

      await container
          .read(csvImportProvider.notifier)
          .saveReport(saver: (_, _) async => false);

      expect(
        container.read(csvImportProvider).saveReport,
        const CommandState<bool>.success(false),
      );
    });

    test('a throwing save lands as a command failure rather than escaping',
        () async {
      final container = await importedContainer();

      await container.read(csvImportProvider.notifier).saveReport(
            saver: (_, _) async => throw const FileSystemException('nope'),
          );

      expect(
        container.read(csvImportProvider).saveReport,
        isA<CommandFailure<bool>>(),
      );
    });

    test('clearing returns the command to idle so it cannot replay', () async {
      final container = await importedContainer();
      await container
          .read(csvImportProvider.notifier)
          .saveReport(saver: (_, _) async => true);

      container.read(csvImportProvider.notifier).clearSaveReport();

      expect(
        container.read(csvImportProvider).saveReport,
        const CommandState<bool>.idle(),
      );
    });

    test('saving before an import has run does nothing', () async {
      final container = await boot();
      var called = false;

      await container.read(csvImportProvider.notifier).saveReport(
            saver: (_, _) async {
              called = true;
              return true;
            },
          );

      expect(called, isFalse);
      expect(
        container.read(csvImportProvider).saveReport,
        const CommandState<bool>.idle(),
      );
    });
  });

  test('an empty file lands on the mapping step with no mapping to edit',
      () async {
    final container = await boot();

    await container.read(csvImportProvider.notifier).pickFile(
          picker: picker(writeCsv('empty.csv', 'Date,Weight\n')),
        );

    final state = container.read(csvImportProvider);
    expect(state.step, CsvImportStep.mapping);
    expect(state.sample?.isEmpty, isTrue);
    expect(state.mapping, isNull);
  });

  test('resetting returns to the pick step and drops the previous run',
      () async {
    final container = await boot();
    final notifier = container.read(csvImportProvider.notifier);

    await notifier.pickFile(picker: picker(withingsFile()));
    notifier.reset();

    final state = container.read(csvImportProvider);
    expect(state.step, CsvImportStep.pick);
    expect(state.sample, isNull);
    expect(state.result, isNull);
  });

  test('changing the separator re-reads the file under the new dialect',
      () async {
    final container = await boot();
    final notifier = container.read(csvImportProvider.notifier);
    final file = writeCsv(
      'euro.csv',
      'Datum;Gewicht\n2026-07-01 08:12:00;78,4\n',
    );

    await notifier.pickFile(picker: picker(file));
    // Sniffed correctly to begin with: two columns, semicolon-delimited.
    expect(container.read(csvImportProvider).sample?.dialect.fieldDelimiter, ';');
    expect(container.read(csvImportProvider).sample?.headerRow, [
      'Datum',
      'Gewicht',
    ]);

    final sniffed = container.read(csvImportProvider).sample!.dialect;
    await notifier.setDialect(sniffed.copyWith(fieldDelimiter: ','));

    // Forced onto a comma, the header is one cell that still holds its
    // semicolon — the file was re-read, not left as it was.
    expect(container.read(csvImportProvider).sample?.headerRow.first,
        'Datum;Gewicht');
  });
}
