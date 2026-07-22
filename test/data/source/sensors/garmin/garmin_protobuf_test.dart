import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/source/sensors/garmin/garmin_gfdi_frame.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_messages.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_protobuf.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_protobuf_transport.dart';

Uint8List _b(List<int> xs) => Uint8List.fromList(xs);

/// Wraps [payload] the way the watch does, so the transport is exercised
/// against the real envelope rather than a mock of itself.
GarminGfdiFrame _reply(int requestId, Uint8List payload, {int? offset, int? total}) {
  final b = BytesBuilder()
    ..add([requestId & 0xFF, (requestId >> 8) & 0xFF])
    ..add(_u32(offset ?? 0))
    ..add(_u32(total ?? payload.length))
    ..add(_u32(payload.length))
    ..add(payload);
  return GarminGfdiFrame.parse(
    GarminGfdiFrame.build(GarminMessageId.protobufResponse, b.takeBytes()),
  );
}

List<int> _u32(int v) =>
    [v & 0xFF, (v >> 8) & 0xFF, (v >> 16) & 0xFF, (v >> 24) & 0xFF];

void main() {
  _chunkingTests();

  group('protobuf encoding', () {
    test('a varint field encodes key then value', () {
      expect((ProtobufWriter()..varint(1, 60)).toBytes(), _b([0x08, 0x3C]));
    });

    test('a multi-byte varint is little-endian base-128', () {
      expect((ProtobufWriter()..varint(1, 300)).toBytes(), _b([0x08, 0xAC, 0x02]));
    });

    test('an empty nested message is not the same as an absent one', () {
      // Garmin uses the empty message as the whole request for actions taking
      // no arguments — cancelling a find is exactly that.
      expect((ProtobufWriter()..emptyMessage(3)).toBytes(), _b([0x1A, 0x00]));
      expect((ProtobufWriter()).toBytes(), isEmpty);
    });

    test('round-trips through the reader', () {
      final inner = (ProtobufWriter()..varint(1, 60)).toBytes();
      final outer = (ProtobufWriter()..nested(12, inner)).toBytes();
      final fields = readProtobuf(outer);
      expect(fields.single.field, 12);
      expect(readProtobuf(fields.single.bytes!).single.varint, 60);
    });

    test('a truncated message yields what was readable, not a crash', () {
      expect(readProtobuf(_b([0x0A, 0x05, 0x01])), isEmpty);
    });
  });

  group('find my watch', () {
    test('start carries a 60-second timeout under the find service', () {
      // Smart.find_my_watch_service = 12, FindMyWatchRequest.timeout = 1.
      expect(GarminFindMyWatch.start(), _b([0x62, 0x04, 0x0A, 0x02, 0x08, 0x3C]));
    });

    test('cancel is an empty message, not a missing one', () {
      expect(GarminFindMyWatch.cancel(), _b([0x62, 0x02, 0x1A, 0x00]));
    });

    test('OK is 100 — a zero status is NOT success', () {
      Uint8List reply(int status) {
        final response = (ProtobufWriter()..varint(1, status)).toBytes();
        final service = (ProtobufWriter()..nested(2, response)).toBytes();
        return (ProtobufWriter()..nested(12, service)).toBytes();
      }

      expect(GarminFindMyWatch.outcome(reply(100)), GarminFindOutcome.ok);
      expect(GarminFindMyWatch.outcome(reply(200)), GarminFindOutcome.error);
      expect(GarminFindMyWatch.outcome(reply(0)), GarminFindOutcome.unknown);
    });

    test('an EMPTY response is acceptance — the real watch sends no status', () {
      // Captured from a vívoactive 5: `62 02 12 00` is find_response present
      // with no status field, and the watch was ringing when it sent it.
      expect(
        GarminFindMyWatch.outcome(_b([0x62, 0x02, 0x12, 0x00])),
        GarminFindOutcome.ok,
      );
      // And the cancel it answers with, field 4.
      expect(
        GarminFindMyWatch.outcome(_b([0x62, 0x02, 0x22, 0x00])),
        GarminFindOutcome.ok,
      );
    });

    test('an unreadable reply is UNKNOWN, never a refusal', () {
      // The watch was seen ringing while its reply was being read as a
      // refusal. Only an explicit ERROR means it declined; everything else has
      // to leave the alert stoppable rather than abandoning it.
      expect(GarminFindMyWatch.outcome(null), GarminFindOutcome.unknown);
      expect(GarminFindMyWatch.outcome(Uint8List(0)), GarminFindOutcome.unknown);
      // Service present, but no response field inside it.
      expect(GarminFindMyWatch.outcome(_b([0x62, 0x00])), GarminFindOutcome.unknown);
      expect(GarminFindMyWatch.outcome(_b([0xFF, 0xFF])), GarminFindOutcome.unknown);
      for (final o in [GarminFindOutcome.ok, GarminFindOutcome.unknown]) {
        expect(o.declined, isFalse);
      }
      expect(GarminFindOutcome.error.declined, isTrue);
    });
  });

  group('protobuf transport', () {
    test('matches a reply to its request by id', () async {
      final sent = <GarminGfdiFrame>[];
      late final GarminProtobufTransport transport;
      transport = GarminProtobufTransport(send: (frame) async {
        final parsed = GarminGfdiFrame.parse(frame);
        // Answer the REQUEST only. The transport now sends its own
        // acknowledgements through this hook, and replying to those recurses.
        if (parsed.messageType != GarminMessageId.protobufRequest) return;
        sent.add(parsed);
        final requestId = parsed.payload[0] | (parsed.payload[1] << 8);
        transport.handleInbound(_reply(requestId, _b([0x62, 0x00])));
      });

      final reply = await transport.request(GarminFindMyWatch.start());
      expect(reply, _b([0x62, 0x00]));
      expect(sent.single.messageType, GarminMessageId.protobufRequest);
    });

    test('request ids advance, so two requests cannot be confused', () async {
      final ids = <int>[];
      late final GarminProtobufTransport transport;
      transport = GarminProtobufTransport(send: (frame) async {
        final parsed = GarminGfdiFrame.parse(frame);
        if (parsed.messageType != GarminMessageId.protobufRequest) return;
        final id = parsed.payload[0] | (parsed.payload[1] << 8);
        ids.add(id);
        transport.handleInbound(_reply(id, _b([0x00])));
      });

      await transport.request(GarminFindMyWatch.start());
      await transport.request(GarminFindMyWatch.cancel());
      expect(ids, [1, 2]);
    });

    test('a COMPLETE message is acknowledged by request id, not generically',
        () async {
      // A generic ack says the frame arrived. The watch also wants to hear that
      // the protobuf message itself was kept, and without that it retransmitted
      // every message it had ever sent, every five seconds, for as long as the
      // link stayed open — which is how a stale reply came to be in flight while
      // a different request was pending.
      final acks = <GarminGfdiFrame>[];
      final transport = GarminProtobufTransport(send: (frame) async {
        final parsed = GarminGfdiFrame.parse(frame);
        if (parsed.messageType == GarminMessageId.response) acks.add(parsed);
      });

      transport.handleInbound(_reply(4242, _b([0x62, 0x00])));

      final ack = acks.single.payload;
      // [u16 acked type][u8 ACK][u16 requestId][u32 offset][kept][no error]
      expect(ack.length, 11);
      expect(ack[3] | (ack[4] << 8), 4242);
      expect(ack.sublist(5, 9), [0, 0, 0, 0]);
      expect(ack.sublist(9), [0, 0]);
    });

    test('a reply for an unknown id is consumed, not mistaken for ours', () {
      final transport = GarminProtobufTransport(send: (_) async {});
      // The watch starts conversations of its own; an unmatched id is one of
      // those, and must not crash or resolve somebody else's request.
      expect(transport.handleInbound(_reply(999, _b([0x01]))), isTrue);
    });

    test('reassembles a chunked reply', () async {
      late final GarminProtobufTransport transport;
      transport = GarminProtobufTransport(send: (frame) async {
        final parsed = GarminGfdiFrame.parse(frame);
        // Answer the REQUEST only. Chunk acknowledgements go out through this
        // same hook, and replying to those too would recurse forever.
        if (parsed.messageType != GarminMessageId.protobufRequest) return;
        final id = parsed.payload[0];
        transport.handleInbound(_reply(id, _b([1, 2, 3]), offset: 0, total: 6));
        transport.handleInbound(_reply(id, _b([4, 5, 6]), offset: 3, total: 6));
      });

      expect(await transport.request(GarminFindMyWatch.start()),
          _b([1, 2, 3, 4, 5, 6]));
    });

    test('a dropped link fails the request instead of hanging on it', () async {
      late final GarminProtobufTransport transport;
      transport = GarminProtobufTransport(send: (_) async {});
      final pending = transport.request(GarminFindMyWatch.start());
      transport.abort();
      await expectLater(pending, throwsA(isA<StateError>()));
    });

    test('an oversized payload is refused rather than truncated', () {
      final transport = GarminProtobufTransport(send: (_) async {});
      expect(
        () => transport.request(Uint8List(400)),
        throwsA(isA<ArgumentError>()),
      );
    });
  });
}

void _chunkingTests() {
  Uint8List b(List<int> xs) => Uint8List.fromList(xs);
  List<int> u32(int v) =>
      [v & 0xFF, (v >> 8) & 0xFF, (v >> 16) & 0xFF, (v >> 24) & 0xFF];
  GarminGfdiFrame chunk(int id, List<int> payload, int offset, int total) {
    final bb = BytesBuilder()
      ..add([id & 0xFF, (id >> 8) & 0xFF])
      ..add(u32(offset))
      ..add(u32(total))
      ..add(u32(payload.length))
      ..add(payload);
    return GarminGfdiFrame.parse(
      GarminGfdiFrame.build(GarminMessageId.protobufRequest, bb.takeBytes()),
    );
  }

  group('unsolicited chunking', () {
    test('reassembles a message the watch sent under its OWN id', () async {
      // The watch answers a settings request with an id of its own rather than
      // echoing ours, so accumulation cannot be keyed on "am I waiting for
      // this" — doing that lost every screen after the first chunk.
      final delivered = <Uint8List>[];
      final acks = <GarminGfdiFrame>[];
      final transport = GarminProtobufTransport(
        send: (frame) async => acks.add(GarminGfdiFrame.parse(frame)),
        onUnsolicited: delivered.add,
      );

      transport.handleInbound(chunk(324, [1, 2, 3], 0, 6));
      expect(delivered, isEmpty, reason: 'incomplete must not be delivered');
      transport.handleInbound(chunk(324, [4, 5, 6], 3, 6));

      expect(delivered.single, b([1, 2, 3, 4, 5, 6]));
    });

    test('acknowledges a chunk with the offset IT declared', () async {
      // Not the next offset. Echoing `dataOffset + chunkLength` meant the watch
      // never saw an acknowledgement for the chunk it had sent, so it resent
      // chunk zero forever and the screen never completed.
      final acks = <GarminGfdiFrame>[];
      final transport = GarminProtobufTransport(
        send: (frame) async => acks.add(GarminGfdiFrame.parse(frame)),
        onUnsolicited: (_) {},
      );

      transport.handleInbound(chunk(324, [4, 5, 6], 3, 6));
      await Future<void>.delayed(Duration.zero);

      final payload = acks.single.payload;
      expect(acks.single.messageType, GarminMessageId.response);
      // [u16 originalType][u8 status][u16 requestId][u32 dataOffset][u8][u8]
      final requestId = payload[3] | (payload[4] << 8);
      final offset = payload[5] |
          (payload[6] << 8) |
          (payload[7] << 16) |
          (payload[8] << 24);
      expect(requestId, 324);
      expect(offset, 3, reason: 'the offset received, not the next expected');
    });
  });
}
