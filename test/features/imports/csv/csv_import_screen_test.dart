import 'dart:io';

import 'package:cross_file/cross_file.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/import_write_repository.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/imports/csv/application/csv_import_view_model.dart';
import 'package:openvitals/features/imports/csv/csv_column_mapping.dart';
import 'package:openvitals/features/imports/csv/csv_datetime_format.dart';
import 'package:openvitals/features/imports/csv/csv_import_metric.dart';
import 'package:openvitals/core/presentation/command_state.dart';
import 'package:openvitals/features/imports/csv/presentation/csv_import_result_view.dart';
import 'package:openvitals/features/imports/csv/presentation/csv_import_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/state/app_providers.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _allBodyWrites = {
  'android.permission.health.WRITE_WEIGHT',
  'android.permission.health.WRITE_BODY_FAT',
  'android.permission.health.WRITE_BONE_MASS',
};

class _FakeHealthDataSource extends HealthDataSource {
  @override
  Future<HealthConnectAvailability> availability() async =>
      HealthConnectAvailability.available;

  @override
  Future<Set<String>> grantedPermissions() async => _allBodyWrites;
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

/// The importer reads a REAL file off disk, which only completes on the real
/// event loop — hence [WidgetTester.runAsync] around every step that does I/O.
/// `pumpAndSettle` is also unusable once the import starts: the progress bar is
/// an indeterminate `LinearProgressIndicator`, so the tree never goes quiet.
/// Bounded `pump`s throughout, per the AGENTS.md test checklist.
void main() {
  late Directory dir;

  setUp(() => dir = Directory.systemTemp.createTempSync('csv_screen_test'));
  tearDown(() => dir.deleteSync(recursive: true));

  File writeCsv(String name, String content) =>
      File('${dir.path}/$name')..writeAsStringSync(content);

  File withingsFile() => writeCsv(
        'withings.csv',
        'Date,"Weight (kg)","Fat mass (kg)"\n'
            '2026-07-01 08:12:00,78.4,15.2\n'
            '2026-07-02 08:14:00,78.6,15.3\n',
      );

  Future<ProviderContainer> pump(WidgetTester tester) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    final container = ProviderContainer(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        healthDataSourceProvider.overrideWithValue(_FakeHealthDataSource()),
        importWriteRepositoryProvider
            .overrideWithValue(_FakeImportWriteRepository()),
        // The default unit system derives from the host locale, so a screen
        // asserting unit-bearing text must pin it (AGENTS.md).
        unitSystemProvider.overrideWithValue(UnitSystem.metric),
      ],
    );
    addTearDown(container.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: CsvImportScreen(),
        ),
      ),
    );
    await tester.pump();
    return container;
  }

  /// Picks [file] through the view-model on the real event loop, then rebuilds.
  Future<void> pick(
    WidgetTester tester,
    ProviderContainer container,
    File file,
  ) async {
    await tester.runAsync(
      () => container
          .read(csvImportProvider.notifier)
          .pickFile(picker: () async => XFile(file.path)),
    );
    await tester.pump();
  }

  /// Maps the weight column and pins the timestamp reading.
  void mapWeight(ProviderContainer container) {
    final notifier = container.read(csvImportProvider.notifier);
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
  }

  testWidgets('the importer opens on the pick step with its explainer',
      (tester) async {
    await pump(tester);

    expect(find.text('Choose a CSV file'), findsOneWidget);
    expect(find.text('Choose CSV file'), findsOneWidget);
  });

  testWidgets('picking a file lists one row per column with its header',
      (tester) async {
    final container = await pump(tester);

    await pick(tester, container, withingsFile());

    expect(find.text('Date'), findsWidgets);
    expect(find.text('Weight (kg)'), findsWidgets);
    expect(find.text('Fat mass (kg)'), findsWidgets);
  });

  testWidgets('a file with only a header row shows the empty-file message',
      (tester) async {
    final container = await pump(tester);

    await pick(tester, container, writeCsv('empty.csv', 'Date,Weight\n'));

    expect(find.text('This file has no data rows.'), findsOneWidget);
  });

  testWidgets(
    'a freshly picked file cannot continue until a metric column is chosen',
    (tester) async {
      final container = await pump(tester);

      await pick(tester, container, withingsFile());

      final button = tester.widget<FilledButton>(
        find.widgetWithText(FilledButton, 'Continue'),
      );
      expect(button.onPressed, isNull);
    },
  );

  testWidgets(
    'mapping body fat as a mass with no weight column shows the needs-weight '
    'error and keeps Continue disabled',
    (tester) async {
      final container = await pump(tester);
      await pick(tester, container, withingsFile());

      container.read(csvImportProvider.notifier).setColumnRole(
            2,
            role: CsvColumnRole.metric,
            metric: CsvImportMetric.bodyFat,
          );
      await tester.pump();

      // The issues render at the end of a lazy ListView, so scroll them into
      // existence before asserting they are on screen.
      await tester.scrollUntilVisible(
        find.textContaining('needs a weight column'),
        200,
        scrollable: find.byType(Scrollable).first,
      );

      expect(find.textContaining('needs a weight column'), findsOneWidget);
      final button = tester.widget<FilledButton>(
        find.widgetWithText(FilledButton, 'Continue'),
      );
      expect(button.onPressed, isNull);
    },
  );

  testWidgets('the confirm step shows the observed range for each metric',
      (tester) async {
    final container = await pump(tester);
    await pick(tester, container, withingsFile());

    mapWeight(container);
    await tester.pump();
    await tester.tap(find.text('Continue'));
    await tester.pump();

    // The guard against a bad derivation: the range that will be written.
    expect(find.text('Weight: 78.4 to 78.6'), findsOneWidget);
  });

  testWidgets('the confirm step shows the date span the import will write',
      (tester) async {
    final container = await pump(tester);
    await pick(tester, container, withingsFile());

    mapWeight(container);
    await tester.pump();
    await tester.tap(find.text('Continue'));
    await tester.pump();

    expect(find.text('Dates run from July 1, 2026 to July 2, 2026'), findsOneWidget);
  });

  testWidgets(
    'reading an ambiguous file month-first instead of day-first is visible in '
    'the date span',
    (tester) async {
      // The guard the single-row echo cannot provide: `01/07` is plausible
      // either way on its own, but a span of Jan-Mar rather than three days in
      // July is not.
      final container = await pump(tester);
      await pick(
        tester,
        container,
        writeCsv(
          'ambiguous.csv',
          'Date,Weight\n01/07/2026,78.4\n02/07/2026,78.6\n03/07/2026,78.2\n',
        ),
      );

      final notifier = container.read(csvImportProvider.notifier);
      notifier.setColumnRole(0, role: CsvColumnRole.timestamp);
      notifier.setColumnRole(
        1,
        role: CsvColumnRole.metric,
        metric: CsvImportMetric.weight,
      );
      notifier.setDateTimeSettings(
        const CsvDateTimeSettings(
          format: CsvDateTimeFormat.monthFirst,
          zone: CsvTimeZoneMode.utc,
        ),
      );
      await tester.pump();
      await tester.tap(find.text('Continue'));
      await tester.pump();

      expect(
        find.text('Dates run from January 7, 2026 to March 7, 2026'),
        findsOneWidget,
      );
    },
  );

  testWidgets('a finished import reports what was written', (tester) async {
    final container = await pump(tester);
    await pick(tester, container, withingsFile());

    mapWeight(container);
    await tester.pump();
    await tester.tap(find.text('Continue'));
    await tester.pump();

    await tester.runAsync(
      () => container.read(csvImportProvider.notifier).startImport(),
    );
    await tester.pump();

    expect(
      find.text('Written 2. Already present 0. Rejected 0.'),
      findsOneWidget,
    );
  });

  testWidgets('an import that rejects every row says nothing was imported',
      (tester) async {
    final container = await pump(tester);
    await pick(
      tester,
      container,
      writeCsv('bad.csv', 'Date,Weight\nnot a date,78.4\nalso not,78.6\n'),
    );

    final notifier = container.read(csvImportProvider.notifier);
    notifier.setColumnRole(0, role: CsvColumnRole.timestamp);
    mapWeight(container);
    await tester.pump();
    await tester.tap(find.text('Continue'));
    await tester.pump();

    await tester.runAsync(notifier.startImport);
    await tester.pump();

    expect(find.text('Nothing was imported from this file.'), findsOneWidget);
    expect(find.text('Rejected rows'), findsOneWidget);
    expect(find.text('Date not understood: 2'), findsOneWidget);
  });

  testWidgets('the finished import offers to save a report', (tester) async {
    final container = await pump(tester);
    await pick(tester, container, withingsFile());
    mapWeight(container);
    await tester.pump();
    await tester.tap(find.text('Continue'));
    await tester.pump();
    await tester.runAsync(
      () => container.read(csvImportProvider.notifier).startImport(),
    );
    await tester.pump();

    expect(find.text('Save report'), findsOneWidget);
  });

  testWidgets('saving the report writes it and confirms once', (tester) async {
    // The screen is rebuilt with a saver seam so no real save picker opens.
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    final container = ProviderContainer(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        healthDataSourceProvider.overrideWithValue(_FakeHealthDataSource()),
        importWriteRepositoryProvider
            .overrideWithValue(_FakeImportWriteRepository()),
        unitSystemProvider.overrideWithValue(UnitSystem.metric),
      ],
    );
    addTearDown(container.dispose);

    String? savedContent;
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: Scaffold(
            body: CsvImportResultView(
              saveReportFile: (content, name) async {
                savedContent = content;
                return true;
              },
            ),
          ),
        ),
      ),
    );

    // Drive a real import so there is a result for the view to render.
    final notifier = container.read(csvImportProvider.notifier);
    await tester.runAsync(
      () => notifier.pickFile(picker: () async => XFile(withingsFile().path)),
    );
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
    await tester.runAsync(notifier.startImport);
    await tester.pump();

    await tester.tap(find.text('Save report'));
    await tester.pump();
    await tester.pump();

    expect(savedContent, contains('OpenVitals CSV import report'));
    expect(savedContent, contains('Records written: 2'));
    expect(find.text('Report saved.'), findsOneWidget);
    // Consumed, so re-rendering cannot replay the snackbar.
    expect(
      container.read(csvImportProvider).saveReport,
      const CommandState<bool>.idle(),
    );
  });

  testWidgets('tapping Choose CSV file invokes the picker', (tester) async {
    // The one test that goes through the real button, so the wiring from tap to
    // view-model is covered rather than assumed.
    //
    // The picker returns null (a cancelled pick) on purpose: a real pick would
    // do file I/O on a future created inside the fake-async zone, which
    // `runAsync` cannot drive to completion. Every step after the pick is
    // covered by the tests above, which call the view-model directly.
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    var pickerCalls = 0;
    final container = ProviderContainer(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        healthDataSourceProvider.overrideWithValue(_FakeHealthDataSource()),
        importWriteRepositoryProvider
            .overrideWithValue(_FakeImportWriteRepository()),
        unitSystemProvider.overrideWithValue(UnitSystem.metric),
      ],
    );
    addTearDown(container.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: CsvImportScreen(
            pickCsvFile: () async {
              pickerCalls++;
              return null;
            },
          ),
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.text('Choose CSV file'));
    await tester.pump();

    expect(pickerCalls, 1);
    // A cancelled pick is not an error and must leave the step alone.
    expect(container.read(csvImportProvider).step, CsvImportStep.pick);
    expect(container.read(csvImportProvider).error, isNull);
  });
}
