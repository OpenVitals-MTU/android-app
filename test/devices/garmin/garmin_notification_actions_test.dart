import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:notification_listener_native/notification_listener_native.dart';

import 'package:openvitals/devices/garmin/garmin_notification_actions.dart';
import 'package:openvitals/devices/garmin/garmin_notification_messages.dart';

NotificationMsg _message({
  List<NotificationActionMsg> actions = const [],
  bool dismissable = true,
}) =>
    NotificationMsg(
      id: 7,
      packageName: 'com.example.chat',
      appLabel: 'Chat',
      title: 'Ada',
      body: 'On my way',
      whenEpochMillis: 0,
      categoryOrdinal: GarminNotificationCategory.sms.index,
      removed: false,
      actions: actions,
      dismissable: dismissable,
    );

NotificationActionMsg _action(
  int index,
  String title, {
  bool reply = false,
  bool fireable = true,
}) =>
    NotificationActionMsg(
      index: index,
      title: title,
      isReply: reply,
      fireableFromBackground: fireable,
    );

void main() {
  group('mapping Android actions onto the watch', () {
    test('every dismissable notification gets a dismiss the app did not '
        'provide', () {
      // Android has no "clear this" action — clearing is the listener's job —
      // so it has to be synthesised, and marked so the performer cancels
      // instead of firing an intent that does not exist.
      final actions = garminActionsFor(_message());

      expect(actions.single.kind, GarminNotificationActionKind.dismiss);
      expect(actions.single.isSynthetic, isTrue);
      expect(actions.single.androidIndex, -1);
    });

    test('an ongoing notification gets no dismiss, because clearing it would '
        'fail', () {
      expect(garminActionsFor(_message(dismissable: false)), isEmpty);
    });

    test("the app's own buttons land in the numbered custom slots, in order",
        () {
      final actions = garminActionsFor(_message(actions: [
        _action(0, 'Mark as read'),
        _action(1, 'Snooze'),
      ]));

      expect(actions.map((a) => a.kind), [
        GarminNotificationActionKind.dismiss,
        GarminNotificationActionKind.custom1,
        GarminNotificationActionKind.custom2,
      ]);
      expect(actions[1].label, 'Mark as read');
      expect(actions[1].androidIndex, 0);
    });

    test('a reply action takes the reply slot, not a custom one', () {
      final actions = garminActionsFor(_message(actions: [
        _action(0, 'Reply', reply: true),
        _action(1, 'Mark as read'),
      ]));

      final reply = actions
          .firstWhere((a) => a.kind == GarminNotificationActionKind.reply);
      expect(reply.isReply, isTrue);
      expect(reply.androidIndex, 0);
      expect(
        actions.any((a) => a.kind == GarminNotificationActionKind.custom1),
        isTrue,
      );
    });

    test('a second reply becomes a plain button rather than overwriting the '
        'first', () {
      // The watch has one reply control, so a second would silently replace it.
      final actions = garminActionsFor(_message(actions: [
        _action(0, 'Reply', reply: true),
        _action(1, 'Reply privately', reply: true),
      ]));

      expect(
        actions.where((a) => a.kind == GarminNotificationActionKind.reply),
        hasLength(1),
      );
      final second = actions.firstWhere((a) => a.androidIndex == 1);
      expect(second.kind, GarminNotificationActionKind.custom1);
      expect(second.isReply, isFalse);
    });

    test('more buttons than there are slots are dropped, not crammed in', () {
      // Reusing a slot would make two controls invoke the same thing.
      final actions = garminActionsFor(_message(actions: [
        for (var i = 0; i < 8; i++) _action(i, 'Action $i'),
      ]));

      final customs = actions
          .where((a) => GarminNotificationActionKind.customSlots
              .contains(a.kind))
          .toList();
      expect(customs, hasLength(maxGarminCustomActions));
      expect(customs.map((a) => a.androidIndex), [0, 1, 2, 3, 4]);
    });

    test('an action that only opens the app is not offered at all', () {
      // A stock SMS app publishes a "Reply" that prefills its compose screen
      // rather than sending, and Android blocks a background activity launch —
      // so it does nothing, silently. Offering it puts back the dead button
      // this feature exists to remove.
      final actions = garminActionsFor(_message(actions: [
        _action(0, 'Reply', reply: true, fireable: false),
        _action(1, 'Mark as read'),
      ]));

      expect(
        actions.any((a) => a.kind == GarminNotificationActionKind.reply),
        isFalse,
      );
      expect(actions.map((a) => a.label), contains('Mark as read'));
    });

    test('a blocked reply does not consume the reply slot, so a later usable '
        'one still gets it', () {
      final actions = garminActionsFor(_message(actions: [
        _action(0, 'Reply', reply: true, fireable: false),
        _action(1, 'Quick reply', reply: true),
      ]));

      final reply = actions
          .firstWhere((a) => a.kind == GarminNotificationActionKind.reply);
      expect(reply.androidIndex, 1);
    });

    test('an index survives the round trip, so the phone never re-derives which '
        'button was meant', () {
      final actions = garminActionsFor(_message(actions: [
        _action(3, 'Archive'),
      ]));
      expect(actions.last.androidIndex, 3);
    });
  });

  group('encoding the ACTIONS attribute', () {
    test('no actions encodes as the four-zero-byte sentinel', () {
      expect(
        encodeGarminNotificationActions(const []),
        Uint8List.fromList(const [0, 0, 0, 0]),
      );
    });

    test('each action is a code, an icon position, a length and a label', () {
      final bytes = encodeGarminNotificationActions(const [
        GarminNotificationAction(
          kind: GarminNotificationActionKind.custom1,
          label: 'Ok',
          androidIndex: 0,
        ),
      ]);

      expect(
        bytes,
        Uint8List.fromList(const [
          1, // one action
          1, // CUSTOM_ACTION_1
          0, // no fixed icon position
          2, // label length
          0x4F, 0x6B, // "Ok"
        ]),
      );
    });

    test('dismiss carries the LEFT icon position, which is where the watch '
        'draws it', () {
      final bytes = encodeGarminNotificationActions(const [
        GarminNotificationAction(
          kind: GarminNotificationActionKind.dismiss,
          label: 'X',
          androidIndex: -1,
        ),
      ]);
      expect(bytes[1], 98); // DISMISS_NOTIFICATION
      expect(bytes[2], GarminActionIconPosition.left.bit);
    });

    test('reply carries the BOTTOM icon position', () {
      final bytes = encodeGarminNotificationActions(const [
        GarminNotificationAction(
          kind: GarminNotificationActionKind.reply,
          label: 'R',
          androidIndex: 0,
          isReply: true,
        ),
      ]);
      expect(bytes[1], 95); // REPLY_MESSAGES
      expect(bytes[2], GarminActionIconPosition.bottom.bit);
    });

    test('the label length is BYTES, so a non-ASCII label still parses', () {
      final bytes = encodeGarminNotificationActions(const [
        GarminNotificationAction(
          kind: GarminNotificationActionKind.custom1,
          label: 'áé',
          androidIndex: 0,
        ),
      ]);
      expect(bytes[3], 4, reason: 'two characters, four UTF-8 bytes');
    });

    test('an absurdly long label is trimmed rather than wrapping the length '
        'byte', () {
      // A length byte cannot carry more than 255, and wrapping would make the
      // watch read the next action code as label text.
      final bytes = encodeGarminNotificationActions([
        GarminNotificationAction(
          kind: GarminNotificationActionKind.custom1,
          label: 'x' * 400,
          androidIndex: 0,
        ),
      ]);
      expect(bytes[3], 255);
      expect(bytes.length, 4 + 255);
    });

    test('several actions pack one after another', () {
      final bytes = encodeGarminNotificationActions(const [
        GarminNotificationAction(
          kind: GarminNotificationActionKind.dismiss,
          label: 'a',
          androidIndex: -1,
        ),
        GarminNotificationAction(
          kind: GarminNotificationActionKind.custom1,
          label: 'bb',
          androidIndex: 0,
        ),
      ]);
      expect(bytes[0], 2);
      // count + (3 + 1) + (3 + 2)
      expect(bytes.length, 1 + 4 + 5);
    });
  });
}
