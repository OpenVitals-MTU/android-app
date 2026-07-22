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
        final id = parsed.payload[0] | (parsed.payload[1] << 8);
        ids.add(id);
        transport.handleInbound(_reply(id, _b([0x00])));
      });

      await transport.request(GarminFindMyWatch.start());
      await transport.request(GarminFindMyWatch.cancel());
      expect(ids, [1, 2]);
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
        final id = GarminGfdiFrame.parse(frame).payload[0];
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
