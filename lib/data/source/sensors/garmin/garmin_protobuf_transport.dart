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
  GarminProtobufTransport({required this.send, this.onUnsolicited});

  /// Hands a built GFDI frame to the layer below.
  final Future<void> Function(Uint8List frame) send;

  /// Called with a message the watch sent on its own account — one that answers
  /// no outstanding request. The watch narrates state changes this way, so a
  /// caller waiting on something can learn it has already happened.
  void Function(Uint8List payload)? onUnsolicited;

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
  /// Chunks in flight: request id → offset → bytes.
  ///
  /// Keyed by OFFSET rather than appended, because the watch retransmits chunks
  /// it thinks were not acknowledged. Appending them grew a 1017-byte message to
  /// 1461 and only parsed by luck, protobuf reading the declared length and
  /// ignoring the tail.
  final Map<int, Map<int, Uint8List>> _incoming = {};

  /// Sends [payload] as a `Smart` message and waits for the watch's reply.
  ///
  /// Returns the reply's protobuf bytes, or null when it does not arrive in
  /// time — a caller that only wants fire-and-forget can ignore the result.
  Future<Uint8List?> request(
    Uint8List payload, {
    String? label,
    Duration? timeout,
  }) async {
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
      return await completer.future.timeout(timeout ?? replyTimeout);
    } on TimeoutException {
      debugPrint('[GARMIN-PB] ✗ no reply to #$requestId within '
          '${(timeout ?? replyTimeout).inSeconds}s');
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
    if (payload.length < 14) {
      // Malformed, but still ours — acknowledge it or the watch repeats it.
      unawaited(send(buildGenericAck(frame.messageType)));
      return true;
    }
    final data = ByteData.sublistView(payload);
    final requestId = data.getUint16(0, Endian.little);
    final dataOffset = data.getUint32(2, Endian.little);
    final totalLength = data.getUint32(6, Endian.little);
    final chunkLength = data.getUint32(10, Endian.little);
    if (payload.length < 14 + chunkLength) {
      unawaited(send(buildGenericAck(frame.messageType)));
      return true;
    }
    final bytes = Uint8List.sublistView(payload, 14, 14 + chunkLength);

    // Chunked, whoever it belongs to. Accumulation is keyed on the id alone,
    // NOT on whether we are waiting for that id: the watch answers a settings
    // request under an id OF ITS OWN rather than echoing ours, so treating an
    // unmatched id as unchunked lost every screen after the first 487 bytes.
    if (totalLength != chunkLength || dataOffset != 0) {
      final chunks = _incoming.putIfAbsent(requestId, () => <int, Uint8List>{});
      chunks[dataOffset] = bytes;
      // A chunk needs an acknowledgement that names it, or the watch never
      // sends the next one.
      // The offset AS RECEIVED, not the next one. Gadgetbridge echoes what the
      // chunk declared; acknowledging `dataOffset + chunkLength` instead left
      // the watch resending chunk zero forever, because it never saw an
      // acknowledgement for the chunk it had actually sent.
      unawaited(send(buildProtobufChunkAck(
        originalMessageType: frame.messageType,
        requestId: requestId,
        dataOffset: dataOffset,
      )));
      final held = chunks.values.fold<int>(0, (sum, c) => sum + c.length);
      if (held < totalLength) {
        debugPrint('[GARMIN-PB] ← #$requestId chunk $held/$totalLength B');
        return true;
      }
      // Assembled in offset order, so a retransmission that arrived late lands
      // where it belongs rather than at the end.
      final assembled = BytesBuilder();
      for (final offset in chunks.keys.toList()..sort()) {
        assembled.add(chunks[offset]!);
      }
      _incoming.remove(requestId);
      _deliver(requestId, assembled.takeBytes());
      return true;
    }

    // Complete in one message: the plain acknowledgement, which the session no
    // longer sends on our behalf.
    unawaited(send(buildGenericAck(frame.messageType)));
    _deliver(requestId, bytes);
    return true;
  }

  /// Hands a COMPLETE message to whoever is waiting for it, or to the
  /// unsolicited hook when nobody is.
  void _deliver(int requestId, Uint8List bytes) {
    final completer = _pending[requestId];
    if (completer != null) {
      debugPrint('[GARMIN-PB] ← #$requestId (${bytes.length}B) ${_hex(bytes)}');
      if (!completer.isCompleted) completer.complete(bytes);
      return;
    }
    // Not an answer to anything outstanding — either the watch started this
    // conversation, or it answered one of ours under its own id.
    debugPrint('[GARMIN-PB] ← unsolicited #$requestId '
        '(${bytes.length}B) ${_hex(bytes)}');
    onUnsolicited?.call(bytes);
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

  static String _hex(Uint8List bytes) =>
      bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join(' ');

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

/// What the watch said about a find request.
enum GarminFindOutcome {
  /// It answered OK.
  ok,

  /// It answered ERROR — the only reading that means the watch declined.
  error,

  /// It answered something this app does not recognise, or did not answer.
  /// Treated as "probably ringing", because it demonstrably can be.
  unknown;

  bool get declined => this == GarminFindOutcome.error;
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
  static const int _cancelResponse = 4;
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

  /// Whether [payload] is the watch reporting on a find, rather than one of the
  /// many other things it narrates unprompted.
  static bool isFindMessage(Uint8List payload) =>
      protobufField(readProtobuf(payload), GarminSmartService.findMyWatch) !=
          null;

  /// What a reply says about the request — including "it did not say".
  ///
  /// Three outcomes, not two. The watch was observed to ring while this code
  /// read its reply as a refusal, and treating "I could not parse that" as
  /// failure is what left it ringing with the phone convinced nothing had
  /// happened. Only an explicit ERROR means the watch declined.
  static GarminFindOutcome outcome(Uint8List? reply) {
    if (reply == null || reply.isEmpty) return GarminFindOutcome.unknown;
    final service = protobufField(
      readProtobuf(reply),
      GarminSmartService.findMyWatch,
    );
    final bytes = service?.bytes;
    if (bytes == null) return GarminFindOutcome.unknown;
    final fields = readProtobuf(bytes);
    // A find is answered in field 2, a cancel in field 4; either is an answer.
    for (final field in [_findResponse, _cancelResponse]) {
      final response = protobufField(fields, field)?.bytes;
      if (response == null) continue;
      final status = protobufField(readProtobuf(response), _status)?.varint;
      // A real vívoactive 5 answers `62 02 12 00` — the response message with
      // NO status field at all, which the schema allows since status is
      // optional. So the presence of the response IS the acknowledgement, and
      // only an explicit ERROR is a refusal. OK is 100 and ERROR is 200, not 0
      // and 1, so a missing status must never be read as zero.
      if (status == null || status == 100) return GarminFindOutcome.ok;
      if (status == 200) return GarminFindOutcome.error;
      return GarminFindOutcome.unknown;
    }
    return GarminFindOutcome.unknown;
  }
}
