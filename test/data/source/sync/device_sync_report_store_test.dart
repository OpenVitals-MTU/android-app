import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/source/sync/device_sync_report_store.dart';
import 'package:openvitals/data/source/sync/sync_report.dart';

void main() {
  group('buildSyncReportText', () {
    final at = DateTime.utc(2026, 1, 20, 14, 30);

    test('renders the summary and per-type lines for a completed sync', () {
      final report = SyncReport(
        completed: true,
        peerDeviceName: 'Galaxy S23',
        negotiatedTypes: const ['WeightRecord'],
        itemsSent: 10,
        itemsReceived: 8,
        imported: 6,
        duplicateSkipped: 2,
        typeSummaries: const [
          SyncTypeSummary(recordType: 'WeightRecord', received: 8, imported: 6, duplicateSkipped: 2),
        ],
      );
      final text = buildSyncReportText(report, generatedAt: at);
      expect(text, contains('Status: completed'));
      expect(text, contains('Peer: Galaxy S23'));
      expect(text, contains('Imported: 6'));
      expect(text, contains('Already had (skipped): 2'));
      expect(text, contains('WeightRecord: received 8, imported 6, skipped 2'));
      expect(text, contains('2026-01-20T14:30'));
    });

    test('renders the abort reason for an aborted sync', () {
      final report = SyncReport(
        completed: false,
        peerDeviceName: 'unknown',
        negotiatedTypes: const [],
        itemsSent: 0,
        itemsReceived: 0,
        imported: 0,
        duplicateSkipped: 0,
        typeSummaries: const [],
        abortReason: 'pairing code did not match',
      );
      final text = buildSyncReportText(report, generatedAt: at);
      expect(text, contains('Status: aborted'));
      expect(text, contains('Reason: pairing code did not match'));
      expect(text, contains('(none)'));
    });
  });

  group('DeviceSyncReportStore', () {
    test('round-trips a report through a file', () async {
      final dir = await Directory.systemTemp.createTemp('ovsync');
      final store = DeviceSyncReportStore(directoryResolver: () async => dir);
      expect(await store.readReport(), '');
      await store.writeReport('hello report');
      expect(await store.readReport(), 'hello report');
      await dir.delete(recursive: true);
    });
  });
}
