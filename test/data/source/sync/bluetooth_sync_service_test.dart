import 'dart:typed_data';

import 'package:bluetooth_sync_native/bluetooth_sync_native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/source/sync/bluetooth_sync_service.dart';

/// A host API whose every call is a no-op future, so the service never reaches a
/// real platform channel (disconnect(), sendBytes(), …).
class _FakeApi implements BluetoothSyncHostApi {
  @override
  dynamic noSuchMethod(Invocation invocation) => Future<void>.value();
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('a disconnect event closes the transport inbound stream', () async {
    final service = BluetoothSyncService(api: _FakeApi());
    addTearDown(service.dispose);

    var done = false;
    service.transport.inbound.listen((_) {}, onDone: () => done = true);

    // A byte arrives, then the link drops mid-transfer.
    service.onBytesReceived(Uint8List.fromList([1, 2, 3]));
    service.onConnectionStateChanged(SyncConnectionStateMsg.disconnected);
    await pumpEventQueue();

    // onDone fired: a SyncSession parked in its receiver loop now unblocks (via
    // inbound.onDone -> abort) instead of hanging forever on a wake-locked FGS.
    expect(done, isTrue);
  });

  test('a connectFailed event also closes the inbound stream', () async {
    final service = BluetoothSyncService(api: _FakeApi());
    addTearDown(service.dispose);

    var done = false;
    service.transport.inbound.listen((_) {}, onDone: () => done = true);

    service.onConnectionStateChanged(SyncConnectionStateMsg.connectFailed);
    await pumpEventQueue();

    expect(done, isTrue);
  });
}
