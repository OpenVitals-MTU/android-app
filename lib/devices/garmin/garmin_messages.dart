import 'dart:typed_data';

import 'garmin_byte_reader.dart';
import 'garmin_byte_writer.dart';
import 'garmin_gfdi_frame.dart';
import 'garmin_notification_messages.dart';

/// The GFDI message vocabulary for the file-sync flow.
///
/// A deliberately small slice of Gadgetbridge's ~30-message `GarminMessage`
/// enum: what a read-only FIT sync needs — request a download, receive its
/// chunks and acknowledge them, archive a finished file, and send system events
/// — plus the notification service (GNCS). Music, weather and uploads remain out
/// of scope.
///
/// Not Gadgetbridge's reflection-dispatched class-per-message design: parsing is
/// one [decode] switch on the frame's type id, and each outgoing message is a
/// small builder. Everything here is pure — it turns bytes into typed values and
/// back, with no I/O — so the session logic above it tests over an in-memory
/// pipe.
class GarminMessageId {
  const GarminMessageId._();

  static const int response = 5000; // status/ack envelope
  static const int downloadRequest = 5002;
  static const int fileTransferData = 5004;
  static const int filter = 5007;
  static const int setFileFlags = 5008;
  static const int deviceInformation = 5024;
  static const int systemEvent = 5030;
  static const int supportedFileTypesRequest = 5031;
  // The notification service (GNCS). UPDATE announces, CONTROL is the watch
  // asking, DATA carries the answer — see garmin_notification_messages.dart.
  static const int notificationUpdate = 5033;
  static const int notificationControl = 5034;
  static const int notificationData = 5035;
  static const int notificationSubscription = 5036;
  static const int synchronization = 5037;
  static const int protobufRequest = 5043;
  static const int protobufResponse = 5044;
  static const int configuration = 5050;
  static const int authNegotiation = 5101;
}

/// GFDI status codes (`GFDIMessage.Status`). Only ACK matters to the sync; any
/// other code means "did not proceed".
enum GarminStatus {
  ack(0),
  nak(1),
  unsupported(2),
  decodeError(3),
  crcError(4),
  lengthError(5);

  const GarminStatus(this.code);
  final int code;

  static GarminStatus fromCode(int code) => values.firstWhere(
        (s) => s.code == code,
        orElse: () => GarminStatus.nak,
      );
}

/// Per-file download outcome (`DownloadRequestStatusMessage.DownloadStatus`).
enum GarminDownloadStatus {
  ok,
  indexUnknown,
  indexNotReadable,
  noSpaceLeft,
  invalid,
  notReady,
  crcIncorrect;

  static GarminDownloadStatus fromOrdinal(int ordinal) =>
      ordinal >= 0 && ordinal < values.length
          ? values[ordinal]
          : GarminDownloadStatus.invalid;
}

/// System events the sync sends (`SystemEventMessage.GarminSystemEventType`).
/// Ordinal IS the wire value, so the order is load-bearing — do not reorder.
enum GarminSystemEventType {
  syncComplete, // 0
  syncFail,
  factoryReset,
  pairStart,
  pairComplete,
  pairFail,
  hostDidEnterForeground,
  hostDidEnterBackground,
  syncReady, // 8
  newDownloadAvailable,
  deviceSoftwareUpdate,
  deviceDisconnect,
  tutorialComplete,
  setupWizardStart,
  setupWizardComplete,
  setupWizardSkipped,
  timeUpdated;
}

/// A parsed inbound GFDI message the sync acts on. Anything outside the sync
/// vocabulary decodes to [GarminUnhandledMessage] rather than throwing — the
/// watch chatters other messages a read-only sync simply ignores.
sealed class GarminInboundMessage {
  const GarminInboundMessage();
}

/// A status/ack envelope (type 5000) whose subject was not one the sync tracks
/// specially — a plain ACK/NAK for a message we sent.
class GarminGenericStatus extends GarminInboundMessage {
  const GarminGenericStatus({
    required this.originalMessageType,
    required this.status,
  });
  final int originalMessageType;
  final GarminStatus status;
}

/// The response to a [GarminDownloadRequest]. When [canProceed], [maxFileSize]
/// is the total byte length the watch will stream.
class GarminDownloadRequestStatus extends GarminInboundMessage {
  const GarminDownloadRequestStatus({
    required this.status,
    required this.downloadStatus,
    required this.maxFileSize,
  });
  final GarminStatus status;
  final GarminDownloadStatus downloadStatus;
  final int maxFileSize;

  bool get canProceed =>
      status == GarminStatus.ack && downloadStatus == GarminDownloadStatus.ok;
}

/// One chunk of a downloading file (type 5004). [dataOffset] is where this chunk
/// sits in the file; [crc] is the running CRC of everything up to and including
/// it, which the session verifies before appending.
class GarminFileTransferData extends GarminInboundMessage {
  const GarminFileTransferData({
    required this.dataOffset,
    required this.crc,
    required this.data,
  });
  final int dataOffset;
  final int crc;
  final Uint8List data;
}

/// The watch introducing itself (type 5024). Sent unprompted on connect and
/// answered with [buildDeviceInformationResponse].
///
/// [maxPacketSize] is the one field the transport needs: it caps how much of a
/// GFDI frame fits in a single write.
class GarminDeviceInformation extends GarminInboundMessage {
  const GarminDeviceInformation({
    required this.protocolVersion,
    required this.productNumber,
    required this.unitNumber,
    required this.softwareVersion,
    required this.maxPacketSize,
    required this.bluetoothFriendlyName,
    required this.deviceName,
    required this.deviceModel,
  });

  final int protocolVersion;
  final int productNumber;
  final int unitNumber;
  final int softwareVersion;
  final int maxPacketSize;
  final String bluetoothFriendlyName;
  final String deviceName;
  final String deviceModel;

  /// e.g. `19.15` — major/minor split at 100, as Gadgetbridge renders it.
  String get softwareVersionText =>
      '${softwareVersion ~/ 100}.${(softwareVersion % 100).toString().padLeft(2, '0')}';
}

/// The watch's authentication challenge (type 5101).
///
/// GFDI has no real authentication: the answer is "yes, fine" with the flags
/// echoed back. The Bluetooth bond is the actual security boundary, which is why
/// onboarding refuses to register a watch that would not bond.
class GarminAuthNegotiation extends GarminInboundMessage {
  const GarminAuthNegotiation({required this.unknown, required this.authFlags});
  final int unknown;
  final int authFlags;
}

/// The watch's answer to [buildSupportedFileTypesRequest]: which
/// `(dataType, subType)` pairs it holds.
class GarminSupportedFileTypes extends GarminInboundMessage {
  const GarminSupportedFileTypes({required this.status, required this.types});
  final GarminStatus status;

  /// Every advertised pair, including ones this app does not map — the raw list
  /// is what makes an empty-vs-unmapped diagnosis possible.
  final List<GarminSupportedFileType> types;
}

class GarminSupportedFileType {
  const GarminSupportedFileType({
    required this.dataType,
    required this.subType,
    required this.name,
  });
  final int dataType;
  final int subType;
  final String name;
}

/// The watch announcing what it has to offer (type 5037), as a bitmask over
/// `SynchronizationMessage.FileType` ordinals.
///
/// Gadgetbridge answers this with a [buildFilterMessage] and only then downloads
/// the directory — which is the exchange that appears to make the watch
/// populate its listing at all.
class GarminSynchronization extends GarminInboundMessage {
  const GarminSynchronization({
    required this.syncType,
    required this.bitmask,
  });

  final int syncType;
  final int bitmask;

  /// Ordinals of the categories worth acting on
  /// (`SynchronizationMessage.shouldProceed`).
  static const int _workouts = 3;
  static const int _activities = 5;
  static const int _activitySummary = 21;
  static const int _sleep = 26;

  bool _has(int ordinal) => (bitmask >> ordinal) & 1 == 1;

  /// Whether the announcement contains anything this app would want.
  bool get shouldProceed =>
      _has(_workouts) ||
      _has(_activities) ||
      _has(_activitySummary) ||
      _has(_sleep);

  /// The set bits, for the log — the raw evidence of what the watch is holding.
  List<int> get setBits =>
      [for (var i = 0; i < 64; i++) if (_has(i)) i];
}

/// The watch's capability bitmap (type 5050) — the capabilities exchange.
///
/// This is not informational. The watch expects OUR capabilities back, and in
/// Gadgetbridge receiving it is what raises the event that completes
/// initialisation. Answering it with only a bare ACK left a real vívoactive 5
/// re-sending it and never populating its directory.
class GarminConfiguration extends GarminInboundMessage {
  const GarminConfiguration(this.capabilityBits);

  /// The raw bitmap, one bit per capability ordinal.
  final Uint8List capabilityBits;
}

/// The watch asking whether to route phone notifications (type 5036).
///
/// Needs a purpose-built status reply, not the generic ACK: the watch expects
/// four payload bytes after the message id and retransmits about once a second
/// until it gets them.
class GarminNotificationSubscription extends GarminInboundMessage {
  const GarminNotificationSubscription({
    required this.enable,
    required this.unknown,
  });
  final bool enable;
  final int unknown;
}

/// The watch asking something about a notification (type 5034).
///
/// Which fields are populated depends on [command]:
/// * `getNotificationAttributes` — [notificationId] and [attributes], the
///   fields it wants and the maximum length it will accept for each (0 = no
///   limit).
/// * `getAppAttributes` — [appIdentifier] and [appAttributes].
/// * either `perform…Action` — [notificationId], [actionCode] and, for a reply,
///   [actionText].
///
/// Actions are decoded but never acted on: this app announces none, so a watch
/// has nothing to invoke. They are still parsed rather than dropped because a
/// watch that sends one is telling us something we got wrong.
class GarminNotificationControl extends GarminInboundMessage {
  const GarminNotificationControl({
    required this.command,
    this.notificationId = 0,
    this.attributes = const {},
    this.appIdentifier,
    this.appAttributes = const [],
    this.actionCode,
    this.actionText,
  });

  final GarminNotificationCommand command;
  final int notificationId;

  /// Requested attribute → maximum length, in the watch's own order. A
  /// `LinkedHashMap` by construction, because that order is reproduced in the
  /// answer.
  final Map<GarminNotificationAttribute, int> attributes;

  final String? appIdentifier;
  final List<int> appAttributes;
  final int? actionCode;
  final String? actionText;
}

/// The watch's verdict on one chunk of an attribute blob — a RESPONSE envelope
/// naming NOTIFICATION_DATA.
///
/// [canProceed] is the flow control: the next chunk goes out only when the watch
/// has said it kept the last one.
class GarminNotificationDataStatus extends GarminInboundMessage {
  const GarminNotificationDataStatus({
    required this.status,
    required this.transferStatus,
  });

  final GarminStatus status;
  final GarminNotificationTransferStatus transferStatus;

  bool get canProceed =>
      status == GarminStatus.ack &&
      transferStatus == GarminNotificationTransferStatus.ok;
}

/// A message outside the sync vocabulary. Carries its payload so an unexpected
/// message can be identified from a device log rather than vanishing — the
/// blind spot that hid whether the watch was talking to us at all.
class GarminUnhandledMessage extends GarminInboundMessage {
  const GarminUnhandledMessage(this.messageType, this.payload);
  final int messageType;
  final Uint8List payload;
}

/// Parses a decoded [GarminGfdiFrame] into a typed [GarminInboundMessage].
GarminInboundMessage decodeGarminMessage(GarminGfdiFrame frame) {
  switch (frame.messageType) {
    case GarminMessageId.response:
      return _decodeStatus(frame.payload);
    case GarminMessageId.fileTransferData:
      return _decodeFileTransferData(frame.payload);
    case GarminMessageId.deviceInformation:
      return _decodeDeviceInformation(frame.payload);
    case GarminMessageId.authNegotiation:
      return _decodeAuthNegotiation(frame.payload);
    case GarminMessageId.synchronization:
      return _decodeSynchronization(frame.payload);
    case GarminMessageId.configuration:
      return _decodeConfiguration(frame.payload);
    case GarminMessageId.notificationSubscription:
      return _decodeNotificationSubscription(frame.payload);
    case GarminMessageId.notificationControl:
      return _decodeNotificationControl(frame.payload);
    default:
      return GarminUnhandledMessage(frame.messageType, frame.payload);
  }
}

GarminInboundMessage _decodeSynchronization(Uint8List payload) {
  final reader = GarminByteReader(payload);
  final syncType = reader.readByte();
  final size = reader.readByte();
  // The watch sends the bitmask as either 4 or 8 bytes.
  final bitmask = switch (size) {
    8 => reader.readLong(),
    4 => reader.readInt(),
    _ => 0,
  };
  return GarminSynchronization(syncType: syncType, bitmask: bitmask);
}

GarminInboundMessage _decodeDeviceInformation(Uint8List payload) {
  final reader = GarminByteReader(payload);
  return GarminDeviceInformation(
    protocolVersion: reader.readShort(),
    productNumber: reader.readShort(),
    unitNumber: reader.readInt(),
    softwareVersion: reader.readShort(),
    maxPacketSize: reader.readShort(),
    bluetoothFriendlyName: reader.readString(),
    deviceName: reader.readString(),
    deviceModel: reader.readString(),
  );
}

GarminInboundMessage _decodeAuthNegotiation(Uint8List payload) {
  final reader = GarminByteReader(payload);
  return GarminAuthNegotiation(
    unknown: reader.readByte(),
    authFlags: reader.readInt(),
  );
}

GarminInboundMessage _decodeConfiguration(Uint8List payload) {
  final reader = GarminByteReader(payload);
  final length = reader.readByte();
  final available = length > reader.remaining ? reader.remaining : length;
  return GarminConfiguration(reader.readBytes(available));
}

GarminInboundMessage _decodeNotificationSubscription(Uint8List payload) {
  final reader = GarminByteReader(payload);
  final enable = reader.readByte() == 1;
  final unknown = reader.remaining > 0 ? reader.readByte() : 0;
  return GarminNotificationSubscription(enable: enable, unknown: unknown);
}

GarminInboundMessage _decodeNotificationControl(Uint8List payload) {
  final reader = GarminByteReader(payload);
  final command = GarminNotificationCommand.fromCode(reader.readByte());
  if (command == null) {
    return GarminUnhandledMessage(GarminMessageId.notificationControl, payload);
  }
  switch (command) {
    case GarminNotificationCommand.getNotificationAttributes:
      final notificationId = reader.readInt();
      final attributes = <GarminNotificationAttribute, int>{};
      while (reader.remaining > 0) {
        final attribute =
            GarminNotificationAttribute.fromCode(reader.readByte());
        // An attribute this app does not know may or may not be followed by a
        // length, so there is no safe way to find the next id. Stop and answer
        // what was understood rather than mis-parsing the rest as attributes.
        if (attribute == null) break;
        var maxLength = 0;
        if (attribute.hasLengthParam) {
          if (reader.remaining < 2) break;
          maxLength = reader.readShort();
        } else if (attribute.hasAdditionalParams) {
          if (reader.remaining < 3) break;
          maxLength = reader.readShort();
          reader.readByte(); // Unidentified; read to stay in step.
        }
        attributes[attribute] = maxLength;
      }
      return GarminNotificationControl(
        command: command,
        notificationId: notificationId,
        attributes: attributes,
      );

    case GarminNotificationCommand.getAppAttributes:
      final appIdentifier = reader.readNullTerminatedString();
      final appAttributes = <int>[];
      while (reader.remaining > 0) {
        appAttributes.add(reader.readByte());
      }
      return GarminNotificationControl(
        command: command,
        appIdentifier: appIdentifier,
        appAttributes: appAttributes,
      );

    case GarminNotificationCommand.performLegacyNotificationAction:
    case GarminNotificationCommand.performNotificationAction:
      final notificationId = reader.readInt();
      final actionCode = reader.remaining > 0 ? reader.readByte() : null;
      // A non-reply action carries no text at all on recent firmware, so its
      // absence is normal rather than a short frame.
      final actionText =
          reader.remaining > 0 ? reader.readNullTerminatedString() : null;
      return GarminNotificationControl(
        command: command,
        notificationId: notificationId,
        actionCode: actionCode,
        actionText: actionText,
      );
  }
}

GarminInboundMessage _decodeSupportedFileTypes(GarminByteReader reader) {
  final status = GarminStatus.fromCode(reader.readByte());
  if (status != GarminStatus.ack) {
    return GarminSupportedFileTypes(status: status, types: const []);
  }
  final count = reader.readByte();
  final types = <GarminSupportedFileType>[];
  for (var i = 0; i < count && reader.remaining >= 2; i++) {
    types.add(GarminSupportedFileType(
      dataType: reader.readByte(),
      subType: reader.readByte(),
      name: reader.readString(),
    ));
  }
  return GarminSupportedFileTypes(status: status, types: types);
}

GarminInboundMessage _decodeStatus(Uint8List payload) {
  final reader = GarminByteReader(payload);
  final originalType = reader.readShort();
  if (originalType == GarminMessageId.supportedFileTypesRequest) {
    return _decodeSupportedFileTypes(reader);
  }
  if (originalType == GarminMessageId.downloadRequest) {
    final status = GarminStatus.fromCode(reader.readByte());
    if (status != GarminStatus.ack) {
      return GarminDownloadRequestStatus(
        status: status,
        downloadStatus: GarminDownloadStatus.invalid,
        maxFileSize: 0,
      );
    }
    final downloadStatus = GarminDownloadStatus.fromOrdinal(reader.readByte());
    final maxFileSize = reader.readInt();
    return GarminDownloadRequestStatus(
      status: status,
      downloadStatus: downloadStatus,
      maxFileSize: maxFileSize,
    );
  }
  if (originalType == GarminMessageId.notificationData) {
    final status = GarminStatus.fromCode(reader.readByte());
    // The watch names WHY it will not take the next chunk, and the upload acts
    // on the difference — a RESEND is recoverable, a CRC mismatch is not. A
    // status with no transfer byte is treated as OK: the only observed sender
    // of that shape is our own final acknowledgement bouncing back.
    final transferStatus = reader.remaining > 0
        ? GarminNotificationTransferStatus.fromOrdinal(reader.readByte())
        : GarminNotificationTransferStatus.ok;
    return GarminNotificationDataStatus(
      status: status,
      transferStatus: transferStatus,
    );
  }
  // Generic ACK/NAK: a single status byte follows the original type.
  final status =
      reader.remaining > 0 ? GarminStatus.fromCode(reader.readByte()) : GarminStatus.ack;
  return GarminGenericStatus(originalMessageType: originalType, status: status);
}

GarminInboundMessage _decodeFileTransferData(Uint8List payload) {
  final reader = GarminByteReader(payload);
  reader.readByte(); // flags — unused on the read path
  final crc = reader.readShort();
  final dataOffset = reader.readInt();
  final data = reader.readBytes(reader.remaining);
  return GarminFileTransferData(dataOffset: dataOffset, crc: crc, data: data);
}

// ── Outgoing message builders — each returns a ready-to-COBS GFDI frame ──────

/// Whether a download starts fresh or continues (`REQUEST_TYPE` ordinal).
enum GarminDownloadRequestType { continueTransfer, fresh }

/// Requests file [fileIndex]. For a fresh whole-file download the watch fills in
/// the size, so [dataOffset]/[dataSize]/[crcSeed] are 0 — the shape
/// `initiateDownload`/`downloadDirectoryEntry` use.
Uint8List buildDownloadRequest({
  required int fileIndex,
  GarminDownloadRequestType requestType = GarminDownloadRequestType.fresh,
  int dataOffset = 0,
  int crcSeed = 0,
  int dataSize = 0,
}) {
  final writer = GarminByteWriter()
    ..writeShort(fileIndex)
    ..writeInt(dataOffset)
    ..writeByte(requestType.index)
    ..writeShort(crcSeed)
    ..writeInt(dataSize);
  return GarminGfdiFrame.build(GarminMessageId.downloadRequest, writer.toBytes());
}

/// Acknowledges any inbound message: a `RESPONSE` envelope naming what is being
/// acknowledged, plus ACK (`GenericStatusMessage`).
///
/// Gadgetbridge sends one of these for EVERY message it receives
/// (`GarminSupport.onMessage` → `sendAck`). Without them the watch treats its
/// message as lost and retransmits, and will not move on — which is exactly what
/// a real vívoactive 5 did, re-sending its CONFIGURATION message on a timer
/// while its directory stayed empty.
Uint8List buildGenericAck(int originalMessageType) {
  final writer = GarminByteWriter()
    ..writeShort(originalMessageType)
    ..writeByte(GarminStatus.ack.code);
  return GarminGfdiFrame.build(GarminMessageId.response, writer.toBytes());
}

/// The acknowledgement a protobuf message needs — chunked or not.
///
/// A generic ACK says the FRAME arrived; this says the protobuf message with
/// that request id was kept. Both are needed, and sending only the generic one
/// is why the watch retransmitted its own settings messages every five seconds
/// for as long as the link stayed open: at the protobuf layer they had never
/// been acknowledged at all. Gadgetbridge starts a complete message with a
/// generic status and then replaces it with this one the moment its handler
/// takes the message (`ProtocolBufferHandler.processIncoming`).
///
/// For a chunk it also unblocks the next piece: without it the watch sends the
/// first 487 bytes of a 1013-byte screen and waits forever.
///
/// Shape from Gadgetbridge's `ProtobufStatusMessage`: the usual response
/// envelope, then the request id, THE OFFSET THE MESSAGE DECLARED, and two
/// status bytes (kept = 0, no error = 0).
///
/// [dataOffset] is the offset received, not the next one expected — echoing the
/// next left the watch resending chunk zero indefinitely.
Uint8List buildProtobufAck({
  required int originalMessageType,
  required int requestId,
  required int dataOffset,
}) {
  final writer = GarminByteWriter()
    ..writeShort(originalMessageType)
    ..writeByte(GarminStatus.ack.code)
    ..writeShort(requestId)
    ..writeInt(dataOffset)
    ..writeByte(0) // ProtobufChunkStatus.KEPT
    ..writeByte(0); // ProtobufStatusCode.NO_ERROR
  return GarminGfdiFrame.build(GarminMessageId.response, writer.toBytes());
}

/// Message types this app answers with their OWN response envelope, which
/// already serves as the acknowledgement — sending a second, generic one would
/// be a duplicate reply to the same message.
///
/// `response` itself is here because an ack must never be acked (Gadgetbridge's
/// "don't ack the ack"), which would otherwise bounce forever.
const Set<int> garminSelfAcknowledgedTypes = {
  GarminMessageId.response,
  GarminMessageId.deviceInformation,
  GarminMessageId.authNegotiation,
  GarminMessageId.fileTransferData,
  // Gets a purpose-built status carrying four extra payload bytes; a generic
  // ACK is too short and the watch keeps asking.
  GarminMessageId.notificationSubscription,
  // Likewise: a notification control request is answered by a three-byte
  // control status. Sending a generic ACK as well would be a second reply to
  // one question — the same double-reply that made the watch retransmit its
  // protobuf messages below.
  GarminMessageId.notificationControl,
  // Acknowledged by the protobuf transport itself, which is the only thing that
  // knows the request id and offset a protobuf status has to name — complete or
  // chunked, both get one. Acking here as well sent TWO for every message, and
  // the watch answered that by retransmitting.
  GarminMessageId.protobufRequest,
  GarminMessageId.protobufResponse,
};

/// Acknowledges a received file-transfer chunk: `RESPONSE` envelope naming
/// FILE_TRANSFER_DATA, ACK + OK, and the offset reached
/// (`FileTransferDataStatusMessage`).
Uint8List buildFileTransferDataAck(int dataOffsetReached) {
  final writer = GarminByteWriter()
    ..writeShort(GarminMessageId.fileTransferData)
    ..writeByte(GarminStatus.ack.code)
    ..writeByte(0) // TransferStatus.OK
    ..writeInt(dataOffsetReached);
  return GarminGfdiFrame.build(GarminMessageId.response, writer.toBytes());
}

/// Bit for [GarminFileFlag.archive]/`delete` — the value is `1 << ordinal`, so
/// ARCHIVE (ordinal 4) is 0x10, matching `SetFileFlagsMessage.FileFlags`.
enum GarminFileFlag {
  archive(0x10),
  delete(0x20);

  const GarminFileFlag(this.bit);
  final int bit;
}

/// Marks a downloaded file archived, so the watch does not re-offer it next
/// sync (`SetFileFlagsMessage`).
Uint8List buildSetFileFlags(int fileIndex, GarminFileFlag flag) {
  final writer = GarminByteWriter()
    ..writeShort(fileIndex)
    ..writeByte(flag.bit);
  return GarminGfdiFrame.build(GarminMessageId.setFileFlags, writer.toBytes());
}

/// A system event carrying a single byte value (0 for the sync lifecycle events
/// like SYNC_READY / SYNC_COMPLETE).
Uint8List buildSystemEvent(GarminSystemEventType event, {int value = 0}) {
  final writer = GarminByteWriter()
    ..writeByte(event.index)
    ..writeByte(value);
  return GarminGfdiFrame.build(GarminMessageId.systemEvent, writer.toBytes());
}

/// Asks the watch for the file types it supports (`SupportedFileTypesMessage` —
/// no payload).
Uint8List buildSupportedFileTypesRequest() =>
    GarminGfdiFrame.build(
      GarminMessageId.supportedFileTypesRequest,
      Uint8List(0),
    );

/// Our half of the device-information exchange: a RESPONSE envelope that both
/// ACKs the watch's message and describes this phone.
///
/// The sentinel values are Gadgetbridge's, kept verbatim because they are what a
/// watch has been observed to accept: protocol 150, software 7791, and `-1`
/// (all-ones) for unit/product/max-packet, i.e. "unspecified".
///
/// `protocolFlags` mirrors the watch's own protocol generation — 1 when it
/// reports a 1xx protocol, else 0.
Uint8List buildDeviceInformationResponse({
  required GarminDeviceInformation incoming,
  required String bluetoothName,
  required String manufacturer,
  required String model,
}) {
  const ourProtocolVersion = 150;
  const ourSoftwareVersion = 7791;
  const unspecifiedShort = 0xFFFF;
  const unspecifiedInt = 0xFFFFFFFF;

  final writer = GarminByteWriter()
    ..writeShort(GarminMessageId.deviceInformation)
    ..writeByte(GarminStatus.ack.code)
    ..writeShort(ourProtocolVersion)
    ..writeShort(unspecifiedShort) // product number
    ..writeInt(unspecifiedInt) // unit number
    ..writeShort(ourSoftwareVersion)
    ..writeShort(unspecifiedShort) // max packet size
    ..writeString(bluetoothName)
    ..writeString(manufacturer)
    ..writeString(model)
    ..writeByte(incoming.protocolVersion ~/ 100 == 1 ? 1 : 0);
  return GarminGfdiFrame.build(GarminMessageId.response, writer.toBytes());
}

/// This app's capability bitmap, the reply to a [GarminConfiguration].
///
/// One bit per `GarminCapability` ordinal, 120 capabilities in 15 bytes —
/// exactly the length a real vívoactive 5 sends. The value is Gadgetbridge's
/// `OUR_CAPABILITIES`: everything set except `UNK_104..UNK_111` and
/// `UNK_114..UNK_119`, which its authors note have never been seen in a Garmin
/// Connect dump. Computed from that enum rather than hand-transcribed, because
/// getting one of 120 bit positions wrong would be invisible here and baffling
/// on the wire.
///
/// Claiming capabilities this app does not implement (music, LiveTrack, ConnectIQ)
/// is deliberate and matches Gadgetbridge: the watch uses the bitmap to decide
/// what it may OFFER, and a narrower claim has been observed to make devices
/// withhold data. Nothing is obliged to act on an offer it never accepts.
final Uint8List garminOurCapabilities = Uint8List.fromList(const [
  0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, //
  0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x00, 0x03,
]);

/// Sends our capabilities back (`ConfigurationMessage`). Note this is a
/// CONFIGURATION message in its own right, not a RESPONSE envelope — the watch
/// gets both this and a plain ACK, as Gadgetbridge sends both.
Uint8List buildConfigurationResponse() {
  final writer = GarminByteWriter()
    ..writeByte(garminOurCapabilities.length)
    ..writeBytes(garminOurCapabilities);
  return GarminGfdiFrame.build(
      GarminMessageId.configuration, writer.toBytes());
}

/// Answers a notification-subscription request
/// (`NotificationSubscriptionStatusMessage`).
///
/// [enabled] is what actually turns forwarding on: until the watch has been told
/// ENABLED it sends no control requests at all, so nothing else in the
/// notification path can happen. A session with no notifications handler answers
/// DISABLED, which is what every sync, find and settings session does and did
/// before this existed.
///
/// The watch's own flag and unknown byte are echoed back, as Gadgetbridge does.
Uint8List buildNotificationSubscriptionStatus(
  GarminNotificationSubscription incoming, {
  required bool enabled,
}) {
  // `NotificationStatus` ordinals: ENABLED is 0, DISABLED is 1.
  const notificationStatusEnabled = 0;
  const notificationStatusDisabled = 1;
  final writer = GarminByteWriter()
    ..writeShort(GarminMessageId.notificationSubscription)
    ..writeByte(GarminStatus.ack.code)
    ..writeByte(enabled ? notificationStatusEnabled : notificationStatusDisabled)
    ..writeByte(incoming.enable ? 1 : 0)
    ..writeByte(incoming.unknown);
  return GarminGfdiFrame.build(GarminMessageId.response, writer.toBytes());
}

/// Announces a notification to the watch (`NotificationUpdateMessage`, 5033).
///
/// Carries NO text — only an id, a category and some counters. The watch decides
/// from this whether it wants the notification at all, and asks for the words
/// separately. See `garmin_notification_messages.dart`.
///
/// [count] is how many notifications of the same category are currently
/// outstanding, which is what a watch face's per-category badge shows.
Uint8List buildNotificationUpdate({
  required GarminNotificationUpdateType updateType,
  required GarminNotificationCategory category,
  required int count,
  required int notificationId,
  bool hasActions = false,
  bool hasAttachments = false,
}) {
  var phoneFlags = 0;
  if (hasActions) phoneFlags |= GarminNotificationPhoneFlag.newActions.bit;
  if (hasAttachments) {
    phoneFlags |= GarminNotificationPhoneFlag.hasAttachments.bit;
  }
  final writer = GarminByteWriter()
    ..writeByte(updateType.index)
    ..writeByte(garminNotificationCategoryFlags())
    ..writeByte(category.index)
    ..writeByte(count)
    ..writeInt(notificationId)
    ..writeByte(phoneFlags);
  return GarminGfdiFrame.build(
      GarminMessageId.notificationUpdate, writer.toBytes());
}

/// One chunk of an attribute blob (`NotificationDataMessage`, 5035).
///
/// [runningCrc] is cumulative over everything sent so far, not over this chunk
/// alone — the same running-CRC scheme the download path verifies on the way in
/// (`_ActiveDownload.append`), just run in the other direction.
Uint8List buildNotificationData({
  required Uint8List chunk,
  required int totalSize,
  required int dataOffset,
  required int runningCrc,
}) {
  final writer = GarminByteWriter()
    ..writeShort(totalSize)
    ..writeShort(runningCrc)
    ..writeShort(dataOffset)
    ..writeBytes(chunk);
  return GarminGfdiFrame.build(
      GarminMessageId.notificationData, writer.toBytes());
}

/// Acknowledges a notification control request
/// (`NotificationControlStatusMessage`).
///
/// Three payload bytes after the message id, not the one a generic ACK carries —
/// which is why 5034 is in [garminSelfAcknowledgedTypes].
Uint8List buildNotificationControlStatus({bool ok = true}) {
  const chunkStatusOk = 0;
  const chunkStatusError = 1;
  const statusCodeNoError = 0;
  const statusCodeUnknownCommand = 160;
  final writer = GarminByteWriter()
    ..writeShort(GarminMessageId.notificationControl)
    ..writeByte(GarminStatus.ack.code)
    ..writeByte(ok ? chunkStatusOk : chunkStatusError)
    ..writeByte(ok ? statusCodeNoError : statusCodeUnknownCommand);
  return GarminGfdiFrame.build(GarminMessageId.response, writer.toBytes());
}

/// Tells the watch the attribute blob is fully sent — the phone's own
/// `NotificationDataStatusMessage`, ACK + OK.
///
/// Sent once the last chunk has been acknowledged. Without it the watch keeps
/// the transfer open waiting for more.
Uint8List buildNotificationDataFinalAck() {
  const transferStatusOk = 0;
  final writer = GarminByteWriter()
    ..writeShort(GarminMessageId.notificationData)
    ..writeByte(GarminStatus.ack.code)
    ..writeByte(transferStatusOk);
  return GarminGfdiFrame.build(GarminMessageId.response, writer.toBytes());
}

/// Answers a [GarminSynchronization] announcement (`FilterMessage`).
///
/// The single payload byte is `FilterType.UNK_3` — Gadgetbridge's name for it,
/// meaning nobody has worked out what the other values do. It is sent verbatim
/// because it is what a real watch is known to accept.
Uint8List buildFilterMessage() {
  const filterTypeUnk3 = 3;
  final writer = GarminByteWriter()..writeByte(filterTypeUnk3);
  return GarminGfdiFrame.build(GarminMessageId.filter, writer.toBytes());
}

/// Answers the auth challenge with ACK + `GUESS_OK`, echoing the watch's own
/// unknown byte and flags back at it — the "no authentication" handshake.
Uint8List buildAuthNegotiationResponse(GarminAuthNegotiation incoming) {
  const guessOk = 0;
  final writer = GarminByteWriter()
    ..writeShort(GarminMessageId.authNegotiation)
    ..writeByte(GarminStatus.ack.code)
    ..writeByte(guessOk)
    ..writeByte(incoming.unknown)
    ..writeInt(incoming.authFlags);
  return GarminGfdiFrame.build(GarminMessageId.response, writer.toBytes());
}
