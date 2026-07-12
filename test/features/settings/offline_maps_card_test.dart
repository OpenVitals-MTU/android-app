import 'dart:async';
import 'dart:io';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart' as p;

import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/features/activity/maps/offline_map_import_controller.dart';
import 'package:openvitals/features/activity/maps/offline_map_metadata_store.dart';
import 'package:openvitals/features/activity/maps/offline_map_models.dart';
import 'package:openvitals/features/settings/presentation/offline_maps_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/state/app_providers.dart';

/// An import controller that blocks inside [importMap] until [gate] completes,
/// so tests can observe the in-flight import UI. It never performs real file
/// I/O: chunked `dart:io` streams do not complete inside the widget-test
/// FakeAsync zone, so the completed import is simulated by seeding the state.
class _GatedImportController extends OfflineMapImportController {
  _GatedImportController({
    required super.metadataStore,
    required super.mapsDirectoryPath,
  });

  final Completer<void> gate = Completer<void>();

  @override
  Future<Result<OfflineMapPack>> importMap(
    File source, {
    String? originalFileName,
    void Function(OfflineMapImportProgress progress)? onProgress,
  }) async {
    onProgress?.call(
      const OfflineMapImportProgress(
        phase: OfflineMapImportPhase.copying,
        bytesCopied: 50,
        totalBytes: 100,
      ),
    );
    await gate.future;
    final pack = OfflineMapPack(
      id: 'estonia-00000000',
      displayName: 'estonia',
      originalFileName: 'estonia.pmtiles',
      sizeBytes: 2048,
      importedAtMillis: DateTime.utc(2026, 7, 1).millisecondsSinceEpoch,
      path: source.path,
      format: OfflineMapPackFormat.pmtiles,
    );
    final updated = OfflineMapLibraryState(
      mapPacks: [pack],
      activeFormat: OfflineMapPackFormat.pmtiles,
    );
    state.value = updated;
    onProgress?.call(
      const OfflineMapImportProgress(
        phase: OfflineMapImportPhase.complete,
        bytesCopied: 2048,
        totalBytes: 2048,
      ),
    );
    return Ok(pack);
  }
}

void main() {
  late Directory root;
  late Directory mapsDir;
  String? raw;

  setUp(() {
    root = Directory.systemTemp.createTempSync('offline_maps_card_test');
    mapsDir = Directory(p.join(root.path, 'offline_maps'));
    raw = null;
  });

  tearDown(() {
    if (root.existsSync()) root.deleteSync(recursive: true);
  });

  OfflineMapMetadataStore store() => OfflineMapMetadataStore(
        readRaw: () => raw,
        writeRaw: (value) => raw = value,
        mapsDirectoryPath: mapsDir.path,
      );

  OfflineMapImportController controller() => OfflineMapImportController(
        metadataStore: store(),
        mapsDirectoryPath: mapsDir.path,
      );

  File sourceFile(String name, List<int> bytes) =>
      File(p.join(root.path, name))..writeAsBytesSync(bytes);

  /// Seeds an imported pack without running the real chunked import (real
  /// `dart:io` stream copies never complete inside the widget-test FakeAsync
  /// zone). Writes the pack file + metadata exactly as an import would.
  OfflineMapPack seedPack(
    String baseName,
    OfflineMapPackFormat format, {
    int sizeBytes = 2048,
  }) {
    final id = '$baseName-0000000${format == OfflineMapPackFormat.pmtiles ? '1' : '2'}';
    final file = File(p.join(mapsDir.path, '$id${format.fileExtension}'))
      ..createSync(recursive: true)
      ..writeAsBytesSync(List.filled(sizeBytes, 7));
    final pack = OfflineMapPack(
      id: id,
      displayName: baseName,
      originalFileName: '$baseName${format.fileExtension}',
      sizeBytes: sizeBytes,
      importedAtMillis: DateTime.utc(2026, 7, 1).millisecondsSinceEpoch,
      path: file.path,
      format: format,
    );
    final current = store().read();
    store().write(OfflineMapLibraryState(
      mapPacks: [...current.mapPacks, pack],
      activeFormat: current.activeFormat ?? format,
    ));
    return pack;
  }

  Widget harness(
    OfflineMapImportController ctrl, {
    Future<XFile?> Function()? pickFile,
  }) {
    return ProviderScope(
      overrides: [
        offlineMapImportControllerProvider.overrideWith((ref) async => ctrl),
        // The default follows the host locale; pin it so the harness is
        // deterministic everywhere.
        unitSystemProvider.overrideWithValue(UnitSystem.metric),
      ],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: SingleChildScrollView(
            child: OfflineMapsCard(pickOfflineMapFile: pickFile),
          ),
        ),
      ),
    );
  }

  OutlinedButton importButton(WidgetTester tester) =>
      tester.widget<OutlinedButton>(find.byType(OutlinedButton));

  testWidgets('renders the empty state when no packs are imported',
      (tester) async {
    await tester.pumpWidget(harness(controller()));
    await tester.pumpAndSettle();

    expect(find.text('Offline maps'), findsOneWidget);
    expect(find.text('No offline maps imported yet.'), findsOneWidget);
    expect(find.text('Import offline map'), findsOneWidget);
    expect(importButton(tester).onPressed, isNotNull);
    // No packs -> no render-format selector.
    expect(find.text('Render format'), findsNothing);
  });

  testWidgets('renders the pack list and the active render format',
      (tester) async {
    seedPack('estonia', OfflineMapPackFormat.pmtiles);
    final ctrl = controller();

    await tester.pumpWidget(harness(ctrl));
    await tester.pumpAndSettle();

    expect(find.text('estonia'), findsOneWidget);
    expect(find.text('PMTiles • estonia.pmtiles • 2.0 KB'), findsOneWidget);
    expect(find.text('Render format'), findsOneWidget);

    final pmtilesChip = tester.widget<ChoiceChip>(
      find.widgetWithText(ChoiceChip, 'PMTiles (1)'),
    );
    expect(pmtilesChip.selected, isTrue);
    expect(pmtilesChip.onSelected, isNotNull);

    // No Mapsforge pack -> that chip is disabled and unselected.
    final mapsforgeChip = tester.widget<ChoiceChip>(
      find.widgetWithText(ChoiceChip, 'Mapsforge (0)'),
    );
    expect(mapsforgeChip.selected, isFalse);
    expect(mapsforgeChip.onSelected, isNull);
  });

  testWidgets('selecting an enabled format chip calls setActiveFormat',
      (tester) async {
    seedPack('estonia', OfflineMapPackFormat.pmtiles, sizeBytes: 1024);
    seedPack('tartu', OfflineMapPackFormat.mapsforge, sizeBytes: 512);
    final ctrl = controller();
    ctrl.setActiveFormat(OfflineMapPackFormat.pmtiles);

    await tester.pumpWidget(harness(ctrl));
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(ChoiceChip, 'Mapsforge (1)'));
    await tester.pumpAndSettle();

    expect(ctrl.state.value.activeFormat, OfflineMapPackFormat.mapsforge);
    expect(
      tester
          .widget<ChoiceChip>(find.widgetWithText(ChoiceChip, 'Mapsforge (1)'))
          .selected,
      isTrue,
    );
  });

  testWidgets('the delete affordance deletes the pack', (tester) async {
    final pack = seedPack('estonia', OfflineMapPackFormat.pmtiles, sizeBytes: 64);
    final ctrl = controller();

    await tester.pumpWidget(harness(ctrl));
    await tester.pumpAndSettle();

    // Kotlin deletes immediately (no confirmation dialog); mirror that.
    await tester.tap(find.byIcon(Icons.delete_outline));
    await tester.pumpAndSettle();

    expect(ctrl.state.value.mapPacks, isEmpty);
    expect(File(pack.path).existsSync(), isFalse);
    expect(find.text('No offline maps imported yet.'), findsOneWidget);
  });

  testWidgets('import shows progress, disables the button, then reports',
      (tester) async {
    final ctrl = _GatedImportController(
      metadataStore: store(),
      mapsDirectoryPath: mapsDir.path,
    );
    final source = sourceFile('estonia.pmtiles', List.filled(2048, 7));

    await tester.pumpWidget(
      harness(ctrl, pickFile: () async => XFile(source.path)),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Import offline map'));
    await tester.pump();

    // Import in flight: button disabled + relabelled, progress line visible.
    expect(importButton(tester).onPressed, isNull);
    expect(find.text('Importing...'), findsOneWidget);
    expect(find.byType(LinearProgressIndicator), findsOneWidget);
    expect(find.text('Copying map • 50%'), findsOneWidget);

    ctrl.gate.complete();
    await tester.pumpAndSettle();

    // Finished: button re-enabled, result reported, pack listed.
    expect(importButton(tester).onPressed, isNotNull);
    expect(find.text('Import offline map'), findsOneWidget);
    expect(find.text('Imported estonia (2.0 KB).'), findsOneWidget);
    expect(find.text('estonia'), findsOneWidget);
  });

  testWidgets('a rejected file surfaces the import error in error color',
      (tester) async {
    final ctrl = controller();
    final source = sourceFile('estonia.osm.pbf', const [1, 2, 3]);

    await tester.pumpWidget(
      harness(ctrl, pickFile: () async => XFile(source.path)),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Import offline map'));
    await tester.pumpAndSettle();

    final errorText = tester.widget<Text>(
      find.text(
        'Map import failed: Only .pmtiles, .map, and .maps offline map '
        'packs are supported.',
      ),
    );
    expect(
      errorText.style?.color,
      Theme.of(tester.element(find.byType(OfflineMapsCard))).colorScheme.error,
    );
    expect(importButton(tester).onPressed, isNotNull);
  });
}
