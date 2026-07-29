import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/garmin/garmin_crc.dart';
import 'package:openvitals/devices/garmin/garmin_gfdi_frame.dart';
import 'package:openvitals/devices/garmin/garmin_messages.dart';
import 'package:openvitals/devices/garmin/garmin_notification_messages.dart';
import 'package:openvitals/devices/garmin/garmin_notifications_handler.dart';

/// Collects what the handler put on the wire, decoded back into frames — so the
/// assertions are about bytes a watch would receive, not about calls made.
class _Wire {
  final List<GarminGfdiFrame> frames = [];

  Future<void> send(Uint8List frame) async {
    frames.add(GarminGfdiFrame.parse(frame));
    return;
  }

  Iterable<GarminGfdiFrame> ofType(int messageType) =>
      frames.where((f) => f.messageType == messageType);

  GarminGfdiFrame get last => frames.last;

  void clear() => frames.clear();
}

GarminNotification _notification({
  required int id,
  String body = 'hello',
  GarminNotificationCategory category = GarminNotificationCategory.social,
}) =>
    GarminNotification(
      id: id,
      packageName: 'com.example.chat',
      title: 'Ada',
      body: body,
      category: category,
      postedAt: DateTime(2026, 7, 28, 9, 5, 3),
    );

/// The watch asking for a notification's text.
GarminNotificationControl _attributeRequest(
  int id, {
  Map<GarminNotificationAttribute, int> attributes = const {
    GarminNotificationAttribute.title: 0,
    GarminNotificationAttribute.message: 0,
  },
}) =>
    GarminNotificationControl(
      command: GarminNotificationCommand.getNotificationAttributes,
      notificationId: id,
      attributes: attributes,
    );

GarminNotificationDataStatus _transferStatus(
  GarminNotificationTransferStatus status,
) =>
    GarminNotificationDataStatus(
      status: GarminStatus.ack,
      transferStatus: status,
    );

final _ok = _transferStatus(GarminNotificationTransferStatus.ok);

/// Reassembles a NOTIFICATION_DATA frame into its parts.
({int totalSize, int crc, int offset, Uint8List chunk}) _chunkOf(
  GarminGfdiFrame frame,
) {
  final p = frame.payload;
  return (
    totalSize: p[0] | (p[1] << 8),
    crc: p[2] | (p[3] << 8),
    offset: p[4] | (p[5] << 8),
    chunk: Uint8List.sublistView(p, 6),
  );
}

/// Builds a handler the watch has already subscribed to.
(GarminNotificationsHandler, _Wire) _enabledHandler({int maxQueued = 10}) {
  final wire = _Wire();
  final handler = GarminNotificationsHandler(
    send: wire.send,
    maxQueued: maxQueued,
  );
  handler.setEnabled(enabled: true);
  return (handler, wire);
}

void main() {
  group('before the watch has subscribed', () {
    test('nothing is announced, so a sync session sends no notification '
        'traffic', () async {
      final wire = _Wire();
      final handler = GarminNotificationsHandler(send: wire.send);

      await handler.post(_notification(id: 1));
      await handler.remove(1);
      await handler.handleControl(_attributeRequest(1));

      expect(wire.frames, isEmpty);
    });

    test('a notification that arrives before the subscription is announced as '
        'soon as it lands', () async {
      // The race that cost the very notification the link was opened for: this
      // app opens the link to say something, so it is always ready a couple of
      // hundred milliseconds before the watch subscribes.
      final wire = _Wire();
      final handler = GarminNotificationsHandler(send: wire.send);
      await handler.post(_notification(id: 7));
      expect(wire.frames, isEmpty);

      handler.setEnabled(enabled: true);
      await handler.flushHeld();

      final announcement =
          wire.ofType(GarminMessageId.notificationUpdate).single;
      expect(announcement.payload[0], GarminNotificationUpdateType.add.index);
    });

    test('several held notifications are all announced, oldest first', () async {
      final wire = _Wire();
      final handler = GarminNotificationsHandler(send: wire.send);
      await handler.post(_notification(id: 1));
      await handler.post(_notification(id: 2));

      handler.setEnabled(enabled: true);
      await handler.flushHeld();

      final ids = wire
          .ofType(GarminMessageId.notificationUpdate)
          .map((f) => f.payload[4] | (f.payload[5] << 8))
          .toList();
      expect(ids, [1, 2]);
    });

    test('one held notification edited twice is announced once', () async {
      final wire = _Wire();
      final handler = GarminNotificationsHandler(send: wire.send);
      await handler.post(_notification(id: 7, body: 'first'));
      await handler.post(_notification(id: 7, body: 'edited'));

      handler.setEnabled(enabled: true);
      await handler.flushHeld();

      expect(wire.ofType(GarminMessageId.notificationUpdate), hasLength(1));
    });

    test('a held notification that aged out of the queue is not announced',
        () async {
      // Announcing it would invite an attribute request there is nothing to
      // answer with, and the watch would render a blank card.
      final wire = _Wire();
      final handler = GarminNotificationsHandler(send: wire.send, maxQueued: 1);
      await handler.post(_notification(id: 1));
      await handler.post(_notification(id: 2)); // evicts 1

      handler.setEnabled(enabled: true);
      await handler.flushHeld();

      final ids = wire
          .ofType(GarminMessageId.notificationUpdate)
          .map((f) => f.payload[4] | (f.payload[5] << 8))
          .toList();
      expect(ids, [2]);
    });
  });

  group('announcing', () {
    test('a new notification is announced as ADD', () async {
      final (handler, wire) = _enabledHandler();
      await handler.post(_notification(id: 7));

      final frame = wire.ofType(GarminMessageId.notificationUpdate).single;
      expect(frame.payload[0], GarminNotificationUpdateType.add.index);
    });

    test('a second notification with the same id is announced as MODIFY, so '
        'the watch updates instead of buzzing again', () async {
      final (handler, wire) = _enabledHandler();
      await handler.post(_notification(id: 7, body: 'first'));
      wire.clear();
      await handler.post(_notification(id: 7, body: 'edited'));

      expect(wire.last.payload[0], GarminNotificationUpdateType.modify.index);
      expect(handler.queued.length, 1);
    });

    test('the count names how many of that category are outstanding', () async {
      final (handler, wire) = _enabledHandler();
      await handler.post(
          _notification(id: 1, category: GarminNotificationCategory.social));
      await handler.post(
          _notification(id: 2, category: GarminNotificationCategory.social));
      await handler.post(
          _notification(id: 3, category: GarminNotificationCategory.email));

      expect(wire.frames[1].payload[3], 2); // second social
      expect(wire.last.payload[3], 1); // first email
    });

    test('dismissing a notification announces REMOVE and drops it', () async {
      final (handler, wire) = _enabledHandler();
      await handler.post(_notification(id: 7));
      wire.clear();
      await handler.remove(7);

      expect(wire.last.payload[0], GarminNotificationUpdateType.remove.index);
      expect(handler.queued, isEmpty);
    });

    test('dismissing an id the queue no longer holds sends nothing', () async {
      final (handler, wire) = _enabledHandler();
      await handler.remove(999);
      expect(wire.frames, isEmpty);
    });

    test('the eleventh notification evicts the oldest', () async {
      final (handler, _) = _enabledHandler();
      for (var id = 1; id <= 11; id++) {
        await handler.post(_notification(id: id));
      }
      expect(handler.queued.map((n) => n.id), [2, 3, 4, 5, 6, 7, 8, 9, 10, 11]);
    });
  });

  group('answering an attribute request', () {
    test('an id that has aged out of the queue sends nothing at all', () async {
      final (handler, wire) = _enabledHandler(maxQueued: 1);
      await handler.post(_notification(id: 1));
      await handler.post(_notification(id: 2)); // evicts 1
      wire.clear();

      await handler.handleControl(_attributeRequest(1));

      expect(wire.frames, isEmpty);
    });

    test('a short body goes out as one chunk', () async {
      final (handler, wire) = _enabledHandler();
      await handler.post(_notification(id: 7, body: 'hey'));
      wire.clear();

      await handler.handleControl(_attributeRequest(7));

      final frames = wire.ofType(GarminMessageId.notificationData).toList();
      expect(frames, hasLength(1));
      final chunk = _chunkOf(frames.single);
      expect(chunk.offset, 0);
      expect(chunk.chunk.length, chunk.totalSize);
    });

    test('a body too long for one chunk is split at 300 bytes with a '
        'cumulative CRC', () async {
      final (handler, wire) = _enabledHandler();
      final body = 'x' * 700;
      await handler.post(_notification(id: 7, body: body));
      wire.clear();

      await handler.handleControl(_attributeRequest(7));
      // The watch takes each chunk in turn.
      await handler.handleDataStatus(_ok);
      await handler.handleDataStatus(_ok);

      final chunks = wire
          .ofType(GarminMessageId.notificationData)
          .map(_chunkOf)
          .toList();
      expect(chunks, hasLength(3));
      expect(chunks.map((c) => c.chunk.length), [300, 300, lessThan(300)]);
      expect(chunks.map((c) => c.offset), [0, 300, 600]);

      // Every chunk's CRC covers everything sent so far, not just itself.
      var running = 0;
      for (final chunk in chunks) {
        running = GarminCrc.compute(chunk.chunk, initialCrc: running);
        expect(chunk.crc, running);
      }
      // And the parts reassemble into exactly what was declared.
      final assembled = [for (final c in chunks) ...c.chunk];
      expect(assembled, hasLength(chunks.first.totalSize));
    });

    test('the final acknowledgement is sent once the blob has drained',
        () async {
      final (handler, wire) = _enabledHandler();
      await handler.post(_notification(id: 7, body: 'hey'));
      wire.clear();

      await handler.handleControl(_attributeRequest(7));
      await handler.handleDataStatus(_ok);

      // A RESPONSE naming NOTIFICATION_DATA with ACK/OK.
      final ack = wire.ofType(GarminMessageId.response).single;
      expect(ack.payload, Uint8List.fromList([0xAB, 0x13, 0x00, 0x00]));
    });

    test('a second request for the same notification restarts the transfer '
        'from offset zero', () async {
      // The watch asks again — typically with a larger limit — when the wearer
      // scrolls into the body.
      final (handler, wire) = _enabledHandler();
      await handler.post(_notification(id: 7, body: 'hey'));
      await handler.handleControl(_attributeRequest(7));
      wire.clear();

      await handler.handleControl(_attributeRequest(7));

      expect(
        _chunkOf(wire.ofType(GarminMessageId.notificationData).single).offset,
        0,
      );
    });

    test('an action request is not answered, because none were announced',
        () async {
      final (handler, wire) = _enabledHandler();
      await handler.post(_notification(id: 7));
      wire.clear();

      await handler.handleControl(const GarminNotificationControl(
        command: GarminNotificationCommand.performNotificationAction,
        notificationId: 7,
        actionCode: 98,
      ));

      expect(wire.frames, isEmpty);
    });
  });

  group('transfer flow control', () {
    Future<(GarminNotificationsHandler, _Wire)> midTransfer() async {
      final (handler, wire) = _enabledHandler();
      await handler.post(_notification(id: 7, body: 'x' * 700));
      await handler.handleControl(_attributeRequest(7));
      wire.clear();
      return (handler, wire);
    }

    test('a chunk answered with RESEND is sent again at the same offset with '
        'the same CRC', () async {
      final (handler, wire) = await midTransfer();

      await handler.handleDataStatus(_ok); // chunk at offset 300
      final first = _chunkOf(wire.last);
      await handler
          .handleDataStatus(_transferStatus(GarminNotificationTransferStatus.resend));
      final repeat = _chunkOf(wire.last);

      expect(repeat.offset, first.offset);
      expect(repeat.crc, first.crc);
      expect(repeat.chunk, first.chunk);
    });

    test('the transfer continues normally after a honoured RESEND', () async {
      final (handler, wire) = await midTransfer();

      await handler
          .handleDataStatus(_transferStatus(GarminNotificationTransferStatus.resend));
      await handler.handleDataStatus(_ok);

      expect(_chunkOf(wire.last).offset, 300);
    });

    test('a second RESEND for the same chunk abandons the transfer', () async {
      final (handler, wire) = await midTransfer();

      await handler
          .handleDataStatus(_transferStatus(GarminNotificationTransferStatus.resend));
      wire.clear();
      await handler
          .handleDataStatus(_transferStatus(GarminNotificationTransferStatus.resend));

      expect(wire.frames, isEmpty);
      // And nothing is left in flight to answer a later status with.
      await handler.handleDataStatus(_ok);
      expect(wire.frames, isEmpty);
    });

    test('ABORT stops the transfer without sending anything further', () async {
      final (handler, wire) = await midTransfer();
      await handler
          .handleDataStatus(_transferStatus(GarminNotificationTransferStatus.abort));
      expect(wire.frames, isEmpty);
    });

    test('a CRC mismatch abandons rather than retrying, because retrying '
        'would send the same bytes', () async {
      final (handler, wire) = await midTransfer();
      await handler.handleDataStatus(
          _transferStatus(GarminNotificationTransferStatus.crcMismatch));
      expect(wire.frames, isEmpty);
    });

    test('an OFFSET_MISMATCH abandons, because the status names no offset to '
        'recover to', () async {
      final (handler, wire) = await midTransfer();
      await handler.handleDataStatus(
          _transferStatus(GarminNotificationTransferStatus.offsetMismatch));
      expect(wire.frames, isEmpty);
    });

    test('a transfer status arriving with nothing in flight is ignored',
        () async {
      final (handler, wire) = _enabledHandler();
      await handler.handleDataStatus(_ok);
      expect(wire.frames, isEmpty);
    });

    test('unsubscribing mid-transfer drops it', () async {
      final (handler, wire) = await midTransfer();
      handler.setEnabled(enabled: false);
      handler.setEnabled(enabled: true);

      await handler.handleDataStatus(_ok);

      expect(wire.frames, isEmpty);
    });
  });

  actionTests();
}

/// Actions the handler resolved and handed up.
class _ActionSink {
  final List<GarminNotificationActionRequest> requests = [];

  Future<void> call(GarminNotificationActionRequest request) async {
    requests.add(request);
  }
}

const _dismissAction = GarminNotificationAction(
  kind: GarminNotificationActionKind.dismiss,
  label: 'Dismiss',
  androidIndex: -1,
);

const _replyAction = GarminNotificationAction(
  kind: GarminNotificationActionKind.reply,
  label: 'Reply',
  androidIndex: 0,
  isReply: true,
);

const _customAction = GarminNotificationAction(
  kind: GarminNotificationActionKind.custom1,
  label: 'Mark as read',
  androidIndex: 1,
);

GarminNotification _actionable({
  int id = 7,
  List<GarminNotificationAction> actions = const [
    _dismissAction,
    _replyAction,
    _customAction,
  ],
}) =>
    GarminNotification(
      id: id,
      packageName: 'com.example.chat',
      title: 'Ada',
      body: 'On my way',
      category: GarminNotificationCategory.sms,
      postedAt: DateTime(2026, 7, 28, 9, 5, 3),
      actions: actions,
    );

/// A handler the watch has subscribed to, with actions wired up.
(GarminNotificationsHandler, _Wire, _ActionSink) _actionHandler() {
  final wire = _Wire();
  final sink = _ActionSink();
  final handler = GarminNotificationsHandler(send: wire.send, onAction: sink.call);
  handler.setEnabled(enabled: true);
  return (handler, wire, sink);
}

GarminNotificationControl _invoke(
  int id,
  int actionCode, {
  String? text,
  GarminNotificationCommand command =
      GarminNotificationCommand.performNotificationAction,
}) =>
    GarminNotificationControl(
      command: command,
      notificationId: id,
      actionCode: actionCode,
      actionText: text,
    );

void actionTests() {
  group('announcing actions', () {
    test('a notification with actions sets the NEW_ACTIONS phone flag', () async {
      // Without it the watch draws no controls at all, however many the ACTIONS
      // attribute later offers — the announcement is where it decides.
      final (handler, wire, _) = _actionHandler();
      await handler.post(_actionable());

      final announcement =
          wire.ofType(GarminMessageId.notificationUpdate).single;
      expect(
        announcement.payload.last & GarminNotificationPhoneFlag.newActions.bit,
        isNot(0),
      );
    });

    test('a notification with no actions does not claim any', () async {
      final (handler, wire, _) = _actionHandler();
      await handler.post(_actionable(actions: const []));

      final announcement =
          wire.ofType(GarminMessageId.notificationUpdate).single;
      expect(announcement.payload.last, 0);
    });

    test('the ACTIONS attribute carries every offered action', () async {
      final (handler, wire, _) = _actionHandler();
      await handler.post(_actionable());
      wire.clear();

      await handler.handleControl(_attributeRequest(7, attributes: const {
        GarminNotificationAttribute.actions: 0,
      }));

      final blob = <int>[
        for (final frame in wire.ofType(GarminMessageId.notificationData))
          ...Uint8List.sublistView(frame.payload, 6),
      ];
      // command + id + {code, u16 length, value}; the value starts at 8.
      expect(blob[8], 3, reason: 'three actions offered');
    });
  });

  group('acting from the wrist', () {
    test('a custom action resolves to the Android index it came from', () async {
      final (handler, _, sink) = _actionHandler();
      await handler.post(_actionable());

      await handler.handleControl(
          _invoke(7, GarminNotificationActionKind.custom1.code));

      expect(sink.requests.single.action.androidIndex, 1);
      expect(sink.requests.single.notificationId, 7);
    });

    test('a reply carries the text the wearer dictated', () async {
      final (handler, _, sink) = _actionHandler();
      await handler.post(_actionable());

      await handler.handleControl(_invoke(
        7,
        GarminNotificationActionKind.reply.code,
        text: 'on my way',
      ));

      expect(sink.requests.single.replyText, 'on my way');
      expect(sink.requests.single.action.isReply, isTrue);
    });

    test('dismiss resolves to the synthetic action, not one of the app\'s',
        () async {
      final (handler, _, sink) = _actionHandler();
      await handler.post(_actionable());

      await handler.handleControl(
          _invoke(7, GarminNotificationActionKind.dismiss.code));

      expect(sink.requests.single.action.isSynthetic, isTrue);
    });

    test('the legacy refuse control maps onto dismiss', () async {
      // This is the button the watch draws from the ACTION_DECLINE category
      // flag, and the one a wearer presses expecting the card to go away. It
      // was dead until now.
      final (handler, _, sink) = _actionHandler();
      await handler.post(_actionable());

      await handler.handleControl(_invoke(
        7,
        1, // LegacyNotificationAction.REFUSE
        command: GarminNotificationCommand.performLegacyNotificationAction,
      ));

      expect(sink.requests.single.action.kind,
          GarminNotificationActionKind.dismiss);
    });

    test('the legacy accept control does nothing, because nothing offers it',
        () async {
      final (handler, _, sink) = _actionHandler();
      await handler.post(_actionable());

      await handler.handleControl(_invoke(
        7,
        0, // ACCEPT
        command: GarminNotificationCommand.performLegacyNotificationAction,
      ));

      expect(sink.requests, isEmpty);
    });

    test('an action code that was never offered is ignored', () async {
      final (handler, _, sink) = _actionHandler();
      await handler.post(_actionable(actions: const [_dismissAction]));

      await handler.handleControl(
          _invoke(7, GarminNotificationActionKind.custom3.code));

      expect(sink.requests, isEmpty);
    });

    test('an action on a notification that has aged out is ignored', () async {
      final (handler, _, sink) = _actionHandler();
      await handler.handleControl(
          _invoke(999, GarminNotificationActionKind.dismiss.code));

      expect(sink.requests, isEmpty);
    });

    test('actions are ignored entirely before the watch subscribes', () async {
      final wire = _Wire();
      final sink = _ActionSink();
      final handler =
          GarminNotificationsHandler(send: wire.send, onAction: sink.call);
      await handler.post(_actionable());

      await handler.handleControl(
          _invoke(7, GarminNotificationActionKind.dismiss.code));

      expect(sink.requests, isEmpty);
    });
  });
}
