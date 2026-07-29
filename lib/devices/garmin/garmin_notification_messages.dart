/// The vocabulary of Garmin's notification service (GNCS) and the pure encoder
/// for a notification's attribute blob.
///
/// GNCS is Garmin's rendering of Apple's ANCS — Gadgetbridge's enums still carry
/// `//was AncsCommand` / `//was AncsAttribute` comments — and it is a **pull**
/// protocol. The phone announces only that a notification exists (message 5033:
/// an id, a category and some flags, and NO text). The watch then asks for the
/// attributes it wants (5034), and only then does the text go across (5035).
///
/// So there is no "send a notification" call anywhere: there is an announcement,
/// and there is an answer to a question that may never be asked. If the watch
/// never asks — because notifications are switched off on the wrist, or the
/// wearer never looks — the text stays on the phone and nothing has gone wrong.
///
/// This file is deliberately I/O-free and frame-free: it turns a notification
/// into the bytes of one attribute blob, and nothing else. The GFDI framing,
/// the inbound decoders and the chunked upload live in `garmin_messages.dart`
/// and `garmin_notifications_handler.dart` respectively.
///
/// Ported from Gadgetbridge's `NotificationsHandler`, `NotificationUpdateMessage`
/// and `NotificationControlMessage` (AGPLv3).
library;

import 'dart:convert';
import 'dart:typed_data';

import 'garmin_byte_writer.dart';

/// Whether an announcement adds a notification, updates one already sent, or
/// withdraws it. Ordinal IS the wire value — do not reorder.
enum GarminNotificationUpdateType { add, modify, remove }

/// The notification's kind, as the watch understands it. Ordinal IS the wire
/// value, so the order is load-bearing — do not reorder or remove.
///
/// The watch groups and prioritises by this, and some faces show a per-category
/// count, which is what the `count` field of an announcement feeds.
enum GarminNotificationCategory {
  other, // 0
  incomingCall,
  missedCall,
  voicemail,
  social,
  schedule,
  email,
  news,
  healthAndFitness,
  businessAndFinance,
  location,
  entertainment,
  sms, // 12
}

/// Flags on an announcement's category byte. Bit is `1 << ordinal`.
enum GarminNotificationFlag {
  background,
  foreground,
  unk,
  actionAccept, // legacy actions only
  actionDecline; // legacy actions only

  int get bit => 1 << index;
}

/// Flags describing what the PHONE can offer for this notification. Bit is
/// `1 << ordinal`.
enum GarminNotificationPhoneFlag {
  legacyActions,
  newActions,
  hasAttachments;

  int get bit => 1 << index;
}

/// The category-flags byte for an announcement.
///
/// `foreground` is set unconditionally, and `actionDecline` always accompanies
/// it — both verbatim from Gadgetbridge, whose comment is worth keeping because
/// the reason is not guessable from the wire: marking notifications as
/// background "was generating bug reports", since most people expect every
/// notification to raise the watch rather than sit silently in a list.
int garminNotificationCategoryFlags() =>
    GarminNotificationFlag.foreground.bit |
    GarminNotificationFlag.actionDecline.bit;

/// The commands a watch can send on the notification control channel (5034).
///
/// [getAppAttributes] is the only one not acted on — Gadgetbridge marks it
/// "unknown/untested" and no watch here has ever sent one. Both action commands
/// are honoured: [performNotificationAction] carries an action code this app
/// chose, and [performLegacyNotificationAction] is the older accept/refuse pair
/// a watch sends for the control it draws from the ACTION_DECLINE category flag.
enum GarminNotificationCommand {
  getNotificationAttributes(0),
  getAppAttributes(1),
  performLegacyNotificationAction(2),
  performNotificationAction(128);

  const GarminNotificationCommand(this.code);
  final int code;

  static GarminNotificationCommand? fromCode(int code) {
    for (final command in values) {
      if (command.code == code) return command;
    }
    return null;
  }
}

/// One field of a notification the watch can ask for.
///
/// [hasLengthParam] and [hasAdditionalParams] are not cosmetic: they say how
/// many bytes follow the attribute's own id in a REQUEST, so getting one wrong
/// desynchronises the rest of the request. Codes 0–7 are ANCS's; 127 and 128 are
/// Garmin's own additions.
enum GarminNotificationAttribute {
  appIdentifier(0),
  title(1, hasLengthParam: true),
  subtitle(2, hasLengthParam: true),
  message(3, hasLengthParam: true),
  messageSize(4),
  date(5),
  negativeActionLabel(7), // legacy actions only
  actions(127, hasAdditionalParams: true), // Garmin extension
  attachments(128); // Garmin extension

  const GarminNotificationAttribute(
    this.code, {
    this.hasLengthParam = false,
    this.hasAdditionalParams = false,
  });

  final int code;

  /// A `u16` maximum length follows this attribute's id in a request.
  final bool hasLengthParam;

  /// A `u16` follows, and then one more byte nobody has identified. Gadgetbridge
  /// reads it and marks the read `//TODO this is wrong`; it is reproduced here
  /// because a request that includes attribute 127 has to be walked past
  /// correctly whether or not the byte means anything.
  final bool hasAdditionalParams;

  static GarminNotificationAttribute? fromCode(int code) {
    for (final attribute in values) {
      if (attribute.code == code) return attribute;
    }
    return null;
  }
}

/// The watch's verdict on one chunk of an attribute blob. Ordinal IS the wire
/// value — do not reorder.
enum GarminNotificationTransferStatus {
  ok,
  resend,
  abort,
  crcMismatch,
  offsetMismatch;

  static GarminNotificationTransferStatus fromOrdinal(int ordinal) =>
      ordinal >= 0 && ordinal < values.length
          ? values[ordinal]
          : GarminNotificationTransferStatus.abort;
}

/// What a wearer can do to a notification, as the watch understands it.
///
/// The code IS the wire value and also decides where the watch draws the
/// control, which is why the icon position is fixed per code rather than chosen:
/// on a vívoactive the left button is the dismiss position, and putting a reply
/// there would be wrong however it is labelled.
enum GarminNotificationActionKind {
  custom1(1),
  custom2(2),
  custom3(3),
  custom4(4),
  custom5(5),
  replyIncomingCall(94, GarminActionIconPosition.bottom),
  reply(95, GarminActionIconPosition.bottom),
  acceptCall(96, GarminActionIconPosition.right),
  rejectCall(97, GarminActionIconPosition.left),
  dismiss(98, GarminActionIconPosition.left),
  blockApplication(99);

  const GarminNotificationActionKind(this.code, [this.iconPosition]);

  final int code;
  final GarminActionIconPosition? iconPosition;

  /// The five slots an app's own buttons are handed out from, in order.
  static const List<GarminNotificationActionKind> customSlots = [
    custom1,
    custom2,
    custom3,
    custom4,
    custom5,
  ];

  static GarminNotificationActionKind? fromCode(int code) {
    for (final kind in values) {
      if (kind.code == code) return kind;
    }
    return null;
  }
}

/// Where the watch draws an action's control. Bit is `1 << ordinal`.
///
/// Gadgetbridge's values, and its comment is worth keeping: these are "educated
/// guesses based on the icons' positions on vívomove style". Nobody has the
/// documentation.
enum GarminActionIconPosition {
  bottom,
  right,
  left;

  int get bit => 1 << index;
}

/// One action offered on the wrist.
class GarminNotificationAction {
  const GarminNotificationAction({
    required this.kind,
    required this.label,
    required this.androidIndex,
    this.isReply = false,
  });

  final GarminNotificationActionKind kind;

  /// What the posting app called it. Shown on the watch for custom actions; the
  /// call and dismiss controls are drawn as icons and ignore it.
  final String label;

  /// Which of the Android notification's own actions this is, so one invoked
  /// from the wrist resolves back without re-deriving anything.
  ///
  /// -1 for an action this app synthesised — [GarminNotificationActionKind.dismiss]
  /// is ours, not the posting app's, and is performed by clearing the
  /// notification rather than by firing one of its intents.
  final int androidIndex;

  /// Whether the watch should collect text before invoking it.
  final bool isReply;

  bool get isSynthetic => androidIndex < 0;
}

/// What the wearer invoked, handed up for something platform-aware to perform.
class GarminNotificationActionRequest {
  const GarminNotificationActionRequest({
    required this.notificationId,
    required this.action,
    this.replyText,
  });

  final int notificationId;
  final GarminNotificationAction action;

  /// What the wearer dictated or picked, for a reply. Null for a plain button.
  final String? replyText;
}

/// Encodes the action list for attribute 127.
///
/// Layout: a count byte, then per action `{u8 code, u8 iconPositionBits,
/// u8 labelByteLength, label}`.
///
/// An empty list encodes as four zero bytes rather than a bare zero — Garmin's
/// own "no actions" sentinel, which Gadgetbridge sends verbatim.
Uint8List encodeGarminNotificationActions(
  List<GarminNotificationAction> actions,
) {
  if (actions.isEmpty) return Uint8List.fromList(const [0, 0, 0, 0]);
  final writer = GarminByteWriter()..writeByte(actions.length);
  for (final action in actions) {
    final label = Uint8List.fromList(utf8.encode(action.label));
    // One length byte, so a long label would wrap and corrupt every action
    // after it.
    final trimmed =
        label.length > 255 ? Uint8List.sublistView(label, 0, 255) : label;
    writer
      ..writeByte(action.kind.code)
      ..writeByte(action.kind.iconPosition?.bit ?? 0)
      ..writeByte(trimmed.length)
      ..writeBytes(trimmed);
  }
  return writer.toBytes();
}

/// One notification, as the phone holds it while the watch decides whether to
/// ask about it.
///
/// Deliberately a plain immutable class rather than `freezed`: nothing under
/// `lib/devices/garmin/` is generated today, and keeping the protocol directory
/// build_runner-free is worth more than a `copyWith` this never needs.
class GarminNotification {
  const GarminNotification({
    required this.id,
    required this.packageName,
    this.title = '',
    this.subtitle = '',
    this.body = '',
    this.category = GarminNotificationCategory.other,
    required this.postedAt,
    this.actions = const [],
  });

  /// Announced as a `u32`. Stable for the lifetime of the notification on the
  /// phone, because a MODIFY and a REMOVE are matched to an ADD by this alone.
  final int id;

  /// The posting app's package name, sent as APP_IDENTIFIER. Some watch faces
  /// resolve an icon from it.
  final String packageName;

  final String title;
  final String subtitle;
  final String body;
  final GarminNotificationCategory category;

  /// When the notification was posted, sent as DATE in the watch's local time.
  final DateTime postedAt;

  /// What the wearer can do to it. Empty means the watch is told there are no
  /// actions, and draws none.
  final List<GarminNotificationAction> actions;

  bool get hasActions => actions.isNotEmpty;
}

/// Formats a timestamp the way GNCS wants it: `yyyyMMdd'T'HHmmss`, local time.
///
/// A pure function taking the value rather than reading the clock, so a test can
/// assert the exact bytes without a fixed-clock harness.
String garminNotificationDate(DateTime when) {
  final local = when.toLocal();
  String pad(int value, [int width = 2]) =>
      value.toString().padLeft(width, '0');
  return '${pad(local.year, 4)}${pad(local.month)}${pad(local.day)}'
      'T${pad(local.hour)}${pad(local.minute)}${pad(local.second)}';
}

/// The text this app sends for [attribute], before any truncation.
///
/// MESSAGE_SIZE is the *character* count of the body, not its byte count —
/// Gadgetbridge sends `String.length`, which is UTF-16 code units, and Dart's
/// `String.length` is the same measure, so the two agree by construction.
String garminNotificationAttributeText(
  GarminNotification notification,
  GarminNotificationAttribute attribute,
) =>
    switch (attribute) {
      GarminNotificationAttribute.appIdentifier => notification.packageName,
      GarminNotificationAttribute.title => notification.title,
      GarminNotificationAttribute.subtitle => notification.subtitle,
      GarminNotificationAttribute.message => notification.body,
      GarminNotificationAttribute.messageSize =>
        notification.body.length.toString(),
      GarminNotificationAttribute.date =>
        garminNotificationDate(notification.postedAt),
      // Actions are not implemented, so nothing can be declined.
      GarminNotificationAttribute.negativeActionLabel => '',
      // Handled as bytes, not text — see [garminNotificationAttributeBytes].
      GarminNotificationAttribute.actions => '',
      // The number of attachments, as text. Always none: pictures ride a
      // separate protobuf channel this app does not implement.
      GarminNotificationAttribute.attachments => '0',
    };

/// The bytes this app sends for [attribute], truncated to [maxLength] when the
/// watch named one.
///
/// [maxLength] is a count of CHARACTERS, not bytes — it is applied to the string
/// before UTF-8 encoding, as Gadgetbridge does, because that is what the watch
/// has been observed to expect. The cut is nudged back by one when it would land
/// between the halves of a surrogate pair, which would otherwise emit a lone
/// surrogate and make the whole attribute undecodable. (Gadgetbridge does not do
/// this, and an emoji at the truncation point is enough to hit it.)
Uint8List garminNotificationAttributeBytes(
  GarminNotification notification,
  GarminNotificationAttribute attribute, {
  int maxLength = 0,
}) {
  if (attribute == GarminNotificationAttribute.actions) {
    // NOT truncated to maxLength. The watch names a limit for TEXT it will
    // render; this value is a packed structure, and cutting it mid-record would
    // leave the watch parsing a label as an action code.
    return encodeGarminNotificationActions(notification.actions);
  }
  final text = garminNotificationAttributeText(notification, attribute);
  if (maxLength <= 0 || text.length <= maxLength) {
    return Uint8List.fromList(utf8.encode(text));
  }
  var cut = maxLength;
  final unit = text.codeUnitAt(cut - 1);
  const highSurrogateStart = 0xD800;
  const highSurrogateEnd = 0xDBFF;
  if (unit >= highSurrogateStart && unit <= highSurrogateEnd) cut -= 1;
  return Uint8List.fromList(utf8.encode(text.substring(0, cut)));
}

/// Builds the attribute blob that answers one GET_NOTIFICATION_ATTRIBUTES
/// request. The result is what gets chunked into 5035 messages.
///
/// Layout: the command byte, the notification id, then each requested attribute
/// as `{u8 code, u16 byteLength, bytes}`.
///
/// [requested] must preserve the watch's own order (a `LinkedHashMap`, which is
/// Dart's default) with one exception: **MESSAGE_SIZE is always encoded last**,
/// wherever the watch asked for it. Gadgetbridge does the same, and a watch that
/// receives it mid-blob has been observed to render an empty body.
Uint8List encodeGarminNotificationAttributes({
  required GarminNotification notification,
  required Map<GarminNotificationAttribute, int> requested,
}) {
  final writer = GarminByteWriter()
    ..writeByte(GarminNotificationCommand.getNotificationAttributes.code)
    ..writeInt(notification.id);

  void encode(GarminNotificationAttribute attribute, int maxLength) {
    final value = garminNotificationAttributeBytes(
      notification,
      attribute,
      maxLength: maxLength,
    );
    writer
      ..writeByte(attribute.code)
      ..writeShort(value.length)
      ..writeBytes(value);
  }

  int? messageSizeMaxLength;
  for (final entry in requested.entries) {
    if (entry.key == GarminNotificationAttribute.messageSize) {
      messageSizeMaxLength = entry.value;
      continue;
    }
    encode(entry.key, entry.value);
  }
  if (messageSizeMaxLength != null) {
    encode(GarminNotificationAttribute.messageSize, messageSizeMaxLength);
  }
  return writer.toBytes();
}
