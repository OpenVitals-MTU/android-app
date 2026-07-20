/// The byte-transport seam between the sync protocol (pure Dart) and the actual
/// carrier (Bluetooth RFCOMM via `bluetooth_sync_native`).
///
/// The protocol layer ([SyncSession]) depends ONLY on [SyncByteTransport], so it
/// can be driven over an in-memory pipe in tests with no Bluetooth at all. The
/// production implementation lives in the data layer and wraps the plugin's
/// `sendBytes` / `onBytesReceived`.
library;

import 'dart:async';
import 'dart:typed_data';

/// A bidirectional, ordered, reliable byte channel to one peer. Bytes written
/// with [send] arrive at the peer's [inbound] stream; the carrier may re-chunk
/// them arbitrarily (framing handles that). [inbound] closes when the link ends.
abstract interface class SyncByteTransport {
  /// Sends [bytes] to the peer. Completes when the bytes have been handed to the
  /// carrier (which applies backpressure over a slow link).
  Future<void> send(Uint8List bytes);

  /// Raw inbound byte chunks from the peer, in order. Closes on disconnect.
  Stream<Uint8List> get inbound;

  /// Closes the channel. Idempotent.
  Future<void> close();
}

/// An in-memory [SyncByteTransport] pair for tests: two endpoints wired together
/// so what one [send]s appears on the other's [inbound]. Models the real carrier
/// closely enough to exercise the whole protocol without Bluetooth.
///
/// Use [SyncPipe.create] to get the connected `(a, b)` pair.
class SyncPipe implements SyncByteTransport {
  SyncPipe._(this._name);

  /// Creates two endpoints wired to each other.
  static (SyncPipe, SyncPipe) create() {
    final a = SyncPipe._('A');
    final b = SyncPipe._('B');
    a._peer = b;
    b._peer = a;
    return (a, b);
  }

  final String _name;
  late final SyncPipe _peer;
  final StreamController<Uint8List> _inbound =
      StreamController<Uint8List>.broadcast();
  bool _closed = false;

  @override
  Stream<Uint8List> get inbound => _inbound.stream;

  @override
  Future<void> send(Uint8List bytes) async {
    if (_closed) throw StateError('pipe $_name is closed');
    // Deliver on a microtask so send/receive interleave like a real async link
    // rather than re-entering the peer synchronously.
    final copy = Uint8List.fromList(bytes);
    scheduleMicrotask(() {
      if (!_peer._closed && !_peer._inbound.isClosed) {
        _peer._inbound.add(copy);
      }
    });
  }

  @override
  Future<void> close() async {
    if (_closed) return;
    _closed = true;
    if (!_inbound.isClosed) await _inbound.close();
    // Closing one end ends the peer's inbound too.
    if (!_peer._closed && !_peer._inbound.isClosed) {
      await _peer._inbound.close();
      _peer._closed = true;
    }
  }
}
