import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/garmin/garmin_byte_writer.dart';
import 'package:openvitals/devices/garmin/garmin_gfdi_frame.dart';
import 'package:openvitals/devices/garmin/garmin_messages.dart';
import 'package:openvitals/devices/garmin/garmin_notification_messages.dart';

Uint8List _b(List<int> xs) => Uint8List.fromList(xs);

/// Round-trips an outgoing message through the frame layer, as the transport
/// will: build → parse → decode.
GarminInboundMessage _roundTrip(Uint8List wire) =>
    decodeGarminMessage(GarminGfdiFrame.parse(wire));

/// The payload of a built frame, without the length, type and CRC around it.
Uint8List _payload(Uint8List wire) =>
    GarminGfdiFrame.parse(wire).payload;

/// Builds an inbound NOTIFICATION_CONTROL frame the way a watch would.
Uint8List _controlFrame(List<int> payload) =>
    GarminGfdiFrame.build(GarminMessageId.notificationControl, _b(payload));

/// Builds the RESPONSE envelope a watch sends about a NOTIFICATION_DATA chunk.
Uint8List _dataStatusFrame({int status = 0, int transferStatus = 0}) {
  final writer = GarminByteWriter()
    ..writeShort(GarminMessageId.notificationData)
    ..writeByte(status)
    ..writeByte(transferStatus);
  return GarminGfdiFrame.build(GarminMessageId.response, writer.toBytes());
}

GarminNotification _notification({
  int id = 0x11223344,
  String packageName = 'com.example.chat',
  String title = 'Ada',
  String subtitle = '',
  String body = 'On my way',
  GarminNotificationCategory category = GarminNotificationCategory.sms,
}) =>
    GarminNotification(
      id: id,
      packageName: packageName,
      title: title,
      subtitle: subtitle,
      body: body,
      category: category,
      postedAt: DateTime(2026, 7, 28, 9, 5, 3),
    );

void main() {
  group('garminNotificationDate', () {
    test('formats as yyyyMMddTHHmmss with every field zero-padded', () {
      expect(
        garminNotificationDate(DateTime(2026, 1, 2, 3, 4, 5)),
        '20260102T030405',
      );
    });
  });

  group('garminNotificationAttributeBytes', () {
    test('MESSAGE_SIZE counts the body characters, not its bytes', () {
      // Three characters, but five bytes once the accents are UTF-8 encoded.
      final value = garminNotificationAttributeBytes(
        _notification(body: 'áéí'),
        GarminNotificationAttribute.messageSize,
      );
      expect(String.fromCharCodes(value), '3');
    });

    test('a maxLength of zero means no limit', () {
      final value = garminNotificationAttributeBytes(
        _notification(body: 'a long enough body'),
        GarminNotificationAttribute.message,
      );
      expect(String.fromCharCodes(value), 'a long enough body');
    });

    test('a body longer than maxLength is cut to that many characters', () {
      final value = garminNotificationAttributeBytes(
        _notification(body: 'abcdefghij'),
        GarminNotificationAttribute.message,
        maxLength: 4,
      );
      expect(String.fromCharCodes(value), 'abcd');
    });

    test('a cut that would split an emoji drops it rather than half of it', () {
      // '👍' is one rune but two UTF-16 code units, so a cut at 3 lands between
      // its halves. A lone surrogate makes the whole attribute undecodable.
      final value = garminNotificationAttributeBytes(
        _notification(body: 'ok👍!'),
        GarminNotificationAttribute.message,
        maxLength: 3,
      );
      expect(utf8Decode(value), 'ok');
    });

    test('ACTIONS is the four-zero-byte "none" sentinel, not an empty value',
        () {
      final value = garminNotificationAttributeBytes(
        _notification(),
        GarminNotificationAttribute.actions,
      );
      expect(value, _b([0, 0, 0, 0]));
    });
  });

  group('encodeGarminNotificationAttributes', () {
    test('writes the command byte and the notification id first', () {
      final blob = encodeGarminNotificationAttributes(
        notification: _notification(id: 0x11223344),
        requested: const {GarminNotificationAttribute.title: 0},
      );
      // GET_NOTIFICATION_ATTRIBUTES, then the id little-endian.
      expect(blob.sublist(0, 5), _b([0x00, 0x44, 0x33, 0x22, 0x11]));
    });

    test('each attribute is a code, a 16-bit byte length, then the value', () {
      final blob = encodeGarminNotificationAttributes(
        notification: _notification(title: 'Ada'),
        requested: const {GarminNotificationAttribute.title: 0},
      );
      expect(
        blob.sublist(5),
        _b([0x01, 0x03, 0x00, 0x41, 0x64, 0x61]),
      );
    });

    test('MESSAGE_SIZE is encoded last even when the watch asked for it first',
        () {
      final blob = encodeGarminNotificationAttributes(
        notification: _notification(title: 'Ada', body: 'hey'),
        requested: const {
          GarminNotificationAttribute.messageSize: 0,
          GarminNotificationAttribute.title: 0,
          GarminNotificationAttribute.message: 0,
        },
      );
      // Attribute codes in the order they landed on the wire.
      final codes = <int>[];
      var i = 5;
      while (i < blob.length) {
        codes.add(blob[i]);
        final length = blob[i + 1] | (blob[i + 2] << 8);
        i += 3 + length;
      }
      expect(codes, [
        GarminNotificationAttribute.title.code,
        GarminNotificationAttribute.message.code,
        GarminNotificationAttribute.messageSize.code,
      ]);
    });

    test('a value length is the BYTE count, not the character count', () {
      final blob = encodeGarminNotificationAttributes(
        notification: _notification(title: 'áé'),
        requested: const {GarminNotificationAttribute.title: 0},
      );
      expect(blob[6] | (blob[7] << 8), 4); // two characters, four bytes
    });
  });

  group('buildNotificationUpdate', () {
    test('carries the update type, category and id with no text at all', () {
      final wire = buildNotificationUpdate(
        updateType: GarminNotificationUpdateType.add,
        category: GarminNotificationCategory.sms,
        count: 2,
        notificationId: 0x11223344,
      );
      final frame = GarminGfdiFrame.parse(wire);
      expect(frame.messageType, GarminMessageId.notificationUpdate);
      expect(
        frame.payload,
        _b([
          0x00, // ADD
          0x12, // FOREGROUND | ACTION_DECLINE
          0x0C, // SMS
          0x02, // count
          0x44, 0x33, 0x22, 0x11, // id, little-endian
          0x00, // no actions, no attachments
        ]),
      );
    });

    test('MODIFY and REMOVE use ordinals 1 and 2', () {
      int updateTypeByte(GarminNotificationUpdateType type) =>
          _payload(buildNotificationUpdate(
            updateType: type,
            category: GarminNotificationCategory.other,
            count: 1,
            notificationId: 1,
          ))[0];
      expect(updateTypeByte(GarminNotificationUpdateType.modify), 1);
      expect(updateTypeByte(GarminNotificationUpdateType.remove), 2);
    });

    test('the phone flags byte announces actions and attachments separately',
        () {
      Uint8List payloadFor({bool actions = false, bool attachments = false}) =>
          _payload(buildNotificationUpdate(
            updateType: GarminNotificationUpdateType.add,
            category: GarminNotificationCategory.other,
            count: 1,
            notificationId: 1,
            hasActions: actions,
            hasAttachments: attachments,
          ));
      expect(payloadFor(actions: true).last, 0x02); // NEW_ACTIONS
      expect(payloadFor(attachments: true).last, 0x04); // HAS_ATTACHMENTS
    });
  });

  group('buildNotificationData', () {
    test('declares the total size, the running CRC and the offset', () {
      final wire = buildNotificationData(
        chunk: _b([0xAA, 0xBB]),
        totalSize: 0x0102,
        dataOffset: 0x0304,
        runningCrc: 0x0506,
      );
      final frame = GarminGfdiFrame.parse(wire);
      expect(frame.messageType, GarminMessageId.notificationData);
      expect(
        frame.payload,
        _b([0x02, 0x01, 0x06, 0x05, 0x04, 0x03, 0xAA, 0xBB]),
      );
    });
  });

  group('buildNotificationSubscriptionStatus', () {
    const incoming = GarminNotificationSubscription(enable: true, unknown: 7);

    test('reports ENABLED as 0 and echoes the watch back', () {
      expect(
        _payload(
          buildNotificationSubscriptionStatus(incoming, enabled: true),
        ),
        // 5036 little-endian, ACK, ENABLED, then the watch's own two bytes.
        _b([0xAC, 0x13, 0x00, 0x00, 0x01, 0x07]),
      );
    });

    test('reports DISABLED as 1', () {
      expect(
        _payload(
          buildNotificationSubscriptionStatus(incoming, enabled: false),
        )[3],
        1,
      );
    });
  });

  group('buildNotificationControlStatus', () {
    test('names NOTIFICATION_CONTROL with ACK, chunk OK and no error', () {
      expect(
        _payload(buildNotificationControlStatus()),
        // 5034 little-endian, ACK, chunk OK, no error.
        _b([0xAA, 0x13, 0x00, 0x00, 0x00]),
      );
    });
  });

  group('decoding NOTIFICATION_CONTROL', () {
    test('an attribute request reads the id and every requested field', () {
      final message = _roundTrip(_controlFrame([
        0x00, // GET_NOTIFICATION_ATTRIBUTES
        0x44, 0x33, 0x22, 0x11, // id
        0x00, // APP_IDENTIFIER, no length param
        0x01, 0x20, 0x00, // TITLE, max 32
        0x03, 0x00, 0x01, // MESSAGE, max 256
        0x04, // MESSAGE_SIZE
      ])) as GarminNotificationControl;

      expect(message.command,
          GarminNotificationCommand.getNotificationAttributes);
      expect(message.notificationId, 0x11223344);
      expect(message.attributes, {
        GarminNotificationAttribute.appIdentifier: 0,
        GarminNotificationAttribute.title: 32,
        GarminNotificationAttribute.message: 256,
        GarminNotificationAttribute.messageSize: 0,
      });
    });

    test('the requested order is preserved, because the answer reproduces it',
        () {
      final message = _roundTrip(_controlFrame([
        0x00,
        0x01, 0x00, 0x00, 0x00,
        0x04, // MESSAGE_SIZE first
        0x01, 0x10, 0x00, // then TITLE
      ])) as GarminNotificationControl;
      expect(message.attributes.keys.toList(), [
        GarminNotificationAttribute.messageSize,
        GarminNotificationAttribute.title,
      ]);
    });

    test('ACTIONS consumes its length AND its extra byte, so the next '
        'attribute still parses', () {
      // The asymmetry most likely to desynchronise the decoder: attribute 127
      // is followed by a u16 and then one more byte nobody has identified.
      final message = _roundTrip(_controlFrame([
        0x00,
        0x01, 0x00, 0x00, 0x00,
        0x7F, 0x40, 0x00, 0x02, // ACTIONS, max 64, extra byte
        0x01, 0x10, 0x00, // TITLE, max 16
      ])) as GarminNotificationControl;

      expect(message.attributes, {
        GarminNotificationAttribute.actions: 64,
        GarminNotificationAttribute.title: 16,
      });
    });

    test('an unknown attribute stops the walk instead of mis-parsing the rest',
        () {
      // 0x63 is not an attribute this app knows, and nothing says how many
      // bytes follow it — so guessing would turn the remainder into nonsense.
      final message = _roundTrip(_controlFrame([
        0x00,
        0x01, 0x00, 0x00, 0x00,
        0x01, 0x10, 0x00, // TITLE
        0x63, 0xFF, 0xFF, // unknown
        0x03, 0x10, 0x00, // MESSAGE — deliberately not reached
      ])) as GarminNotificationControl;

      expect(message.attributes, {GarminNotificationAttribute.title: 16});
    });

    test('an app-attributes request reads the NUL-terminated package name', () {
      final message = _roundTrip(_controlFrame([
        0x01, // GET_APP_ATTRIBUTES
        0x61, 0x2E, 0x62, 0x00, // "a.b\0"
        0x00, // APP_NAME
      ])) as GarminNotificationControl;

      expect(message.command, GarminNotificationCommand.getAppAttributes);
      expect(message.appIdentifier, 'a.b');
      expect(message.appAttributes, [0]);
    });

    test('an action with no text is decoded, not treated as a short frame', () {
      // Recent firmware omits the string entirely for non-reply actions.
      final message = _roundTrip(_controlFrame([
        0x80, // PERFORM_NOTIFICATION_ACTION
        0x44, 0x33, 0x22, 0x11,
        0x62, // DISMISS_NOTIFICATION
      ])) as GarminNotificationControl;

      expect(message.command,
          GarminNotificationCommand.performNotificationAction);
      expect(message.notificationId, 0x11223344);
      expect(message.actionCode, 0x62);
      expect(message.actionText, isNull);
    });

    test('an unknown command decodes to unhandled, not an error', () {
      final message = _roundTrip(_controlFrame([0x7A, 0x00]));
      expect(message, isA<GarminUnhandledMessage>());
    });
  });

  group('decoding a NOTIFICATION_DATA transfer status', () {
    test('OK can proceed', () {
      final message = _roundTrip(_dataStatusFrame())
          as GarminNotificationDataStatus;
      expect(message.transferStatus, GarminNotificationTransferStatus.ok);
      expect(message.canProceed, isTrue);
    });

    test('each non-OK transfer status is named, so the upload can tell them '
        'apart', () {
      GarminNotificationTransferStatus statusFor(int code) =>
          (_roundTrip(_dataStatusFrame(transferStatus: code))
                  as GarminNotificationDataStatus)
              .transferStatus;
      expect(statusFor(1), GarminNotificationTransferStatus.resend);
      expect(statusFor(2), GarminNotificationTransferStatus.abort);
      expect(statusFor(3), GarminNotificationTransferStatus.crcMismatch);
      expect(statusFor(4), GarminNotificationTransferStatus.offsetMismatch);
    });

    test('a NAK cannot proceed even when the transfer status says OK', () {
      final message = _roundTrip(_dataStatusFrame(status: 1))
          as GarminNotificationDataStatus;
      expect(message.canProceed, isFalse);
    });
  });

  group('acknowledgement policy', () {
    test('NOTIFICATION_CONTROL is self-acknowledged, so no generic ACK is sent',
        () {
      // It gets a three-byte control status; a generic ACK as well would be a
      // second reply to one question.
      expect(
        garminSelfAcknowledgedTypes,
        contains(GarminMessageId.notificationControl),
      );
    });
  });
}

/// UTF-8 decode helper kept local so the expectation reads as text.
String utf8Decode(Uint8List bytes) =>
    const Utf8Decoder(allowMalformed: true).convert(bytes);
