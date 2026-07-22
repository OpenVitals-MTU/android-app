import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/foundation.dart';

import 'garmin_byte_writer.dart';
import 'garmin_gfdi_frame.dart';
import 'garmin_messages.dart';
import 'garmin_protobuf.dart';

/// One protobuf exchange with the watch, over GFDI messages 5043/5044.
///
/// The envelope is `[u16 requestId][u32 dataOffset][u32 totalLength]
/// [u32 chunkLength][bytes]`, and a reply is matched to its request by the id —
/// the watch answers out of band, whenever it feels like it, so there is nothing
/// else to correlate on.
///
/// Chunking is implemented for RECEIVING only. Every request this app sends is a
/// few dozen bytes, far under the 375-byte chunk the watch accepts, so the
/// outbound half would be untestable code written against a case that cannot
/// currently arise. Sending something larger throws rather than silently
/// truncating.
class GarminProtobufTransport {
  GarminProtobufTransport({required this.send});

  /// Hands a built GFDI frame to the layer below.
  final Future<void> Function(Uint8List frame) send;

  /// The largest payload the watch accepts in one message, from Gadgetbridge's
  /// `ProtocolBufferHandler` (measured on a Vívomove Style).
  static const int maxChunkSize = 375;

  /// How long to wait for a reply before giving up on it.
  ///
  /// A request the watch never answers must not leak its completer: the caller
  /// is usually a button, and a button that never resolves leaves a spinner on
  /// screen forever.
  static const Duration replyTimeout = Duration(seconds: 10);

  int _lastRequestId = 0;
  final Map<int, Completer<Uint8List>> _pending = {};
  final Map<int, BytesBuilder> _incoming = {};

  /// Sends [payload] as a `Smart` message and waits for the watch's reply.
  ///
  /// Returns the reply's protobuf bytes, or null when it does not arrive in
  /// time — a caller that only wants fire-and-forget can ignore the result.
  Future<Uint8List?> request(Uint8List payload, {String? label}) async {
    if (payload.length > maxChunkSize) {
      throw ArgumentError(
        'Protobuf request is ${payload.length}B, over the $maxChunkSize B '
        'the watch accepts in one message, and outbound chunking is not '
        'implemented.',
      );
    }
    final requestId = _nextRequestId();
    final completer = Completer<Uint8List>();
    _pending[requestId] = completer;

    debugPrint('[GARMIN-PB] → ${label ?? "request"} #$requestId '
        '(${payload.length}B)');
    await send(_frame(GarminMessageId.protobufRequest, requestId, payload));

    try {
      return await completer.future.timeout(replyTimeout);
    } on TimeoutException {
      debugPrint('[GARMIN-PB] ✗ no reply to #$requestId within '
          '${replyTimeout.inSeconds}s');
      return null;
    } finally {
      _pending.remove(requestId);
      _incoming.remove(requestId);
    }
  }

  /// Feeds an inbound protobuf message in. Returns true when it was consumed.
  ///
  /// Both 5043 and 5044 land here: the watch sends REQUESTS of its own as well
  /// as responses, and an unmatched request id simply means it started the
  /// conversation rather than answering ours.
  bool handleInbound(GarminGfdiFrame frame) {
    if (frame.messageType != GarminMessageId.protobufRequest &&
        frame.messageType != GarminMessageId.protobufResponse) {
      return false;
    }
    final payload = frame.payload;
    if (payload.length < 14) return false;
    final data = ByteData.sublistView(payload);
    final requestId = data.getUint16(0, Endian.little);
    final dataOffset = data.getUint32(2, Endian.little);
    final totalLength = data.getUint32(6, Endian.little);
    final chunkLength = data.getUint32(10, Endian.little);
    if (payload.length < 14 + chunkLength) return false;
    final bytes = Uint8List.sublistView(payload, 14, 14 + chunkLength);

    final completer = _pending[requestId];
    if (completer == null) {
      // The watch's own request, not a reply to ours. Logged and dropped: this
      // app answers none of them, and the generic ACK the session already sends
      // is enough to stop it retransmitting.
      debugPrint('[GARMIN-PB] ← unsolicited #$requestId (${bytes.length}B)');
      return true;
    }

    if (totalLength == chunkLength && dataOffset == 0) {
      if (!completer.isCompleted) completer.complete(bytes);
      return true;
    }

    // Chunked: accumulate until the offsets add up to the declared total.
    final buffer = _incoming.putIfAbsent(requestId, BytesBuilder.new);
    buffer.add(bytes);
    if (buffer.length >= totalLength) {
      if (!completer.isCompleted) completer.complete(buffer.takeBytes());
      _incoming.remove(requestId);
    }
    return true;
  }

  /// Fails every outstanding request, so a dropped link does not leave a caller
  /// waiting out the full timeout for a reply that can never come.
  void abort() {
    for (final completer in _pending.values) {
      if (!completer.isCompleted) {
        completer.completeError(StateError('link closed'));
      }
    }
    _pending.clear();
    _incoming.clear();
  }

  int _nextRequestId() => _lastRequestId = (_lastRequestId + 1) % 65536;

  Uint8List _frame(int messageType, int requestId, Uint8List payload) {
    final writer = GarminByteWriter()
      ..writeShort(requestId)
      ..writeInt(0) // dataOffset — single chunk
      ..writeInt(payload.length) // total
      ..writeInt(payload.length) // this chunk
      ..writeBytes(payload);
    return GarminGfdiFrame.build(messageType, writer.toBytes());
  }
}

/// Builds the `Smart` message that starts a find, and the one that stops it.
///
/// Find is a TOGGLE, not a one-shot: the request carries a timeout in seconds
/// and there is a matching cancel, so the watch alerts for that long unless
/// stopped. Field numbers from `gdi_find_my_watch.proto`.
class GarminFindMyWatch {
  const GarminFindMyWatch._();

  static const int _findRequest = 1;
  static const int _findResponse = 2;
  static const int _cancelRequest = 3;
  static const int _timeout = 1;
  static const int _status = 1;

  /// Gadgetbridge's value, and a sensible one: long enough to find a watch down
  /// the back of a sofa, short enough that a forgotten alert stops itself.
  static const Duration defaultTimeout = Duration(seconds: 60);

  static Uint8List start({Duration timeout = defaultTimeout}) {
    final request = (ProtobufWriter()..varint(_timeout, timeout.inSeconds))
        .toBytes();
    final service = (ProtobufWriter()..nested(_findRequest, request)).toBytes();
    return (ProtobufWriter()
          ..nested(GarminSmartService.findMyWatch, service))
        .toBytes();
  }

  static Uint8List cancel() {
    final service =
        (ProtobufWriter()..emptyMessage(_cancelRequest)).toBytes();
    return (ProtobufWriter()
          ..nested(GarminSmartService.findMyWatch, service))
        .toBytes();
  }

  /// Whether a reply says the watch accepted the request.
  ///
  /// `OK` is 100 and `ERROR` is 200 — not 0 and 1 — so a missing status must
  /// not be read as success by defaulting to zero.
  static bool accepted(Uint8List? reply) {
    if (reply == null) return false;
    final service = protobufField(
      readProtobuf(reply),
      GarminSmartService.findMyWatch,
    );
    final bytes = service?.bytes;
    if (bytes == null) return false;
    final fields = readProtobuf(bytes);
    // A cancel is answered in field 4, a find in field 2; either is an answer.
    for (final field in [_findResponse, 4]) {
      final response = protobufField(fields, field)?.bytes;
      if (response == null) continue;
      final status = protobufField(readProtobuf(response), _status)?.varint;
      return status == 100;
    }
    return false;
  }
}
