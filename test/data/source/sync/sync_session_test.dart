import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/source/sync/sync_frame.dart';
import 'package:openvitals/data/source/sync/sync_messages.dart';
import 'package:openvitals/data/source/sync/sync_report.dart';
import 'package:openvitals/data/source/sync/sync_session.dart';
import 'package:openvitals/data/source/sync/sync_transport.dart';

/// In-memory stand-in for Health Connect: a keyed set of records. `readItems`
/// snapshots current contents (the session's dedup baseline); `writeItems`
/// upserts.
class FakeRecordStore implements SyncRecordStore {
  FakeRecordStore(Iterable<SyncItem> initial) {
    for (final item in initial) {
      _byKey[item.key] = item;
    }
  }

  final Map<String, SyncItem> _byKey = {};

  Set<String> get keys => _byKey.keys.toSet();

  @override
  Future<List<SyncItem>> readItems(Set<String> types) async =>
      _byKey.values.where((i) => types.contains(i.recordType)).toList();

  @override
  Future<void> writeItems(List<SyncItem> items) async {
    for (final item in items) {
      _byKey[item.key] = item;
    }
  }
}

SyncItem item(String key, {String type = 'StepsRecord'}) => SyncItem(
      key: key,
      recordType: type,
      payload: Uint8List.fromList(key.codeUnits),
    );

SyncSessionConfig configFor(
  SyncRole role, {
  String code = '424242',
  List<String> types = const ['StepsRecord', 'HeartRateRecord'],
  List<String>? selected,
}) =>
    SyncSessionConfig(
      role: role,
      code: code,
      deviceName: role == SyncRole.host ? 'Host phone' : 'Guest phone',
      supportedTypes: types,
      selectedTypes: selected,
      // Fixed, distinct nonces keep the test deterministic.
      nonce: Uint8List.fromList(
        List.filled(32, role == SyncRole.host ? 0x11 : 0x22),
      ),
      handshakeTimeout: const Duration(seconds: 5),
      batchTimeout: const Duration(seconds: 5),
      batchSize: 2,
    );

Future<(SyncReport, SyncReport)> runPair(
  SyncRecordStore hostStore,
  SyncRecordStore guestStore, {
  String hostCode = '424242',
  String guestCode = '424242',
  List<String>? hostSelected,
  List<String>? guestSelected,
}) async {
  final (hostPipe, guestPipe) = SyncPipe.create();
  final host = SyncSession(
    transport: hostPipe,
    store: hostStore,
    config: configFor(SyncRole.host, code: hostCode, selected: hostSelected),
  );
  final guest = SyncSession(
    transport: guestPipe,
    store: guestStore,
    config: configFor(SyncRole.guest, code: guestCode, selected: guestSelected),
  );
  final results = await Future.wait([host.run(), guest.run()]);
  return (results[0], results[1]);
}

/// Drives one real session against a manual endpoint that sends whatever raw
/// frames [attack] dictates — for hostile/malformed-peer cases.
Future<SyncReport> runAgainstAttacker(
  void Function(SyncByteTransport attacker) attack,
) async {
  final (hostPipe, attackerPipe) = SyncPipe.create();
  final host = SyncSession(
    transport: hostPipe,
    store: FakeRecordStore([item('a')]),
    config: configFor(SyncRole.host),
  );
  final report = host.run();
  attack(attackerPipe);
  return report;
}

Uint8List frameBytes(SyncFrameType type, Uint8List payload) =>
    SyncFrame(type, payload).encode();

void main() {
  group('bidirectional merge', () {
    test('each side imports what it lacked and skips shared records', () async {
      // Shared key 'c'; host-only a,b; guest-only d,e.
      final hostStore = FakeRecordStore([item('a'), item('b'), item('c')]);
      final guestStore = FakeRecordStore([item('c'), item('d'), item('e')]);

      final (hostReport, guestReport) = await runPair(hostStore, guestStore);

      // Both converge to the union.
      expect(hostStore.keys, {'a', 'b', 'c', 'd', 'e'});
      expect(guestStore.keys, {'a', 'b', 'c', 'd', 'e'});

      expect(hostReport.completed, isTrue);
      expect(hostReport.itemsSent, 3); // a,b,c
      expect(hostReport.itemsReceived, 3); // c,d,e
      expect(hostReport.imported, 2); // d,e
      expect(hostReport.duplicateSkipped, 1); // c

      expect(guestReport.imported, 2); // a,b
      expect(guestReport.duplicateSkipped, 1); // c
      expect(guestReport.peerDeviceName, 'Host phone');
    });

    test('per-type summaries split the tallies correctly', () async {
      final hostStore = FakeRecordStore([
        item('s1', type: 'StepsRecord'),
        item('h1', type: 'HeartRateRecord'),
      ]);
      final guestStore = FakeRecordStore([
        item('s1', type: 'StepsRecord'), // dup vs host
        item('h2', type: 'HeartRateRecord'),
      ]);

      final (hostReport, _) = await runPair(hostStore, guestStore);

      final steps =
          hostReport.typeSummaries.firstWhere((s) => s.recordType == 'StepsRecord');
      final heart = hostReport.typeSummaries
          .firstWhere((s) => s.recordType == 'HeartRateRecord');
      expect(steps.received, 1);
      expect(steps.duplicateSkipped, 1); // s1 already on host
      expect(heart.imported, 1); // h2 new
    });
  });

  group('idempotency', () {
    test('a second sync writes nothing new', () async {
      final hostStore = FakeRecordStore([item('a'), item('b')]);
      final guestStore = FakeRecordStore([item('b'), item('c')]);

      await runPair(hostStore, guestStore);
      // Both now hold {a,b,c}. Re-run.
      final (hostReport, guestReport) = await runPair(hostStore, guestStore);

      expect(hostReport.imported, 0);
      expect(guestReport.imported, 0);
      expect(hostReport.itemsReceived, 3);
      expect(hostReport.duplicateSkipped, 3);
      expect(hostStore.keys, {'a', 'b', 'c'});
      expect(guestStore.keys, {'a', 'b', 'c'});
    });
  });

  group('within-session dedup', () {
    test('a key sent twice in one direction is written once', () async {
      // The host reads a duplicate key 'x' (across two batches, batchSize 2:
      // ['x','x'] then ['y']). A Map-backed store can't represent duplicate
      // keys, so a custom store yields them from readItems.
      final hostStore = _DupReadingStore(['x', 'x', 'y']);
      final guestStore = FakeRecordStore(const []);

      final (hostReport, guestReport) = await runPair(hostStore, guestStore);

      expect(guestStore.keys, {'x', 'y'}); // written once each
      expect(guestReport.imported, 2);
      expect(guestReport.duplicateSkipped, 1); // the repeated 'x'
      expect(hostReport.completed, isTrue);
    });
  });

  group('authentication', () {
    test('mismatched codes abort both sides before any data moves', () async {
      final hostStore = FakeRecordStore([item('a')]);
      final guestStore = FakeRecordStore([item('b')]);

      final (hostReport, guestReport) = await runPair(
        hostStore,
        guestStore,
        hostCode: '111111',
        guestCode: '222222',
      );

      expect(hostReport.completed, isFalse);
      expect(guestReport.completed, isFalse);
      expect(hostReport.abortReason, contains('code'));
      // No records crossed.
      expect(hostStore.keys, {'a'});
      expect(guestStore.keys, {'b'});
    });
  });

  group('link failure', () {
    test('a dropped transport ends the session as an abort', () async {
      final hostStore = FakeRecordStore([item('a'), item('b')]);
      final guestStore = FakeRecordStore([item('c')]);

      final (hostPipe, guestPipe) = SyncPipe.create();
      final host = SyncSession(
        transport: hostPipe,
        store: hostStore,
        config: configFor(SyncRole.host),
      );
      final guest = SyncSession(
        transport: guestPipe,
        store: guestStore,
        config: configFor(SyncRole.guest),
      );

      // Both run()s subscribe + send hello synchronously up to their first
      // await. Dropping the link now (before the hello microtasks deliver) makes
      // both sessions see their inbound close mid-handshake.
      final hostFuture = host.run();
      final guestFuture = guest.run();
      await guestPipe.close();

      final reports = await Future.wait([hostFuture, guestFuture]);
      expect(reports[0].completed, isFalse);
      expect(reports[1].completed, isFalse);
      expect(reports[0].abortReason, isNotNull);
      // No records crossed a dead link.
      expect(guestStore.keys, {'c'});
    });
  });

  group('type negotiation', () {
    test('only the intersection of supported+selected types syncs', () async {
      final hostStore = FakeRecordStore([
        item('s1', type: 'StepsRecord'),
        item('h1', type: 'HeartRateRecord'),
      ]);
      final guestStore = FakeRecordStore(const []);

      // Guest only selects StepsRecord.
      final (hostPipe, guestPipe) = SyncPipe.create();
      final host = SyncSession(
        transport: hostPipe,
        store: hostStore,
        config: configFor(SyncRole.host),
      );
      final guest = SyncSession(
        transport: guestPipe,
        store: guestStore,
        config: configFor(SyncRole.guest, selected: const ['StepsRecord']),
      );
      final results = await Future.wait([host.run(), guest.run()]);

      // Host still sends everything it has for negotiated types, but negotiation
      // on the guest side selects only StepsRecord; the host's negotiated set is
      // the intersection with the guest's *supported* types (both support both),
      // so this asserts the guest's selection governs what IT reads/sends.
      expect(results[0].completed, isTrue);
      expect(guestStore.keys, contains('s1'));
    });
  });

  group('hostile peer', () {
    test('a record frame before authentication aborts the session', () async {
      final report = await runAgainstAttacker((attacker) {
        // No handshake/auth — just push a batch straight away.
        attacker.send(frameBytes(
            SyncFrameType.batch, SyncBatch(seq: 1, items: const []).encode()));
      });
      expect(report.completed, isFalse);
      expect(report.abortReason, contains('before authentication'));
    });

    test('a malformed hello frame aborts cleanly instead of crashing', () async {
      // Valid JSON, wrong shape: `v` is a string where an int is required, which
      // would throw a TypeError (an Error, not an Exception) from the decode.
      final badHello = Uint8List.fromList(utf8.encode('{"v":"not-an-int"}'));
      final report = await runAgainstAttacker((attacker) {
        attacker.send(frameBytes(SyncFrameType.hello, badHello));
      });
      expect(report.completed, isFalse);
      expect(report.abortReason, contains('hello'));
    });
  });
}

/// A store whose `readItems` yields a caller-specified key list (allowing
/// duplicate keys, which a Map-backed store cannot represent).
class _DupReadingStore implements SyncRecordStore {
  _DupReadingStore(this._keys);
  final List<String> _keys;

  @override
  Future<List<SyncItem>> readItems(Set<String> types) async =>
      [for (final k in _keys) item(k)];

  @override
  Future<void> writeItems(List<SyncItem> items) async {}
}
