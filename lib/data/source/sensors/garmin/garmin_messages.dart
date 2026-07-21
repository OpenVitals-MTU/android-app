import 'dart:typed_data';

import 'garmin_byte_reader.dart';
import 'garmin_byte_writer.dart';
import 'garmin_gfdi_frame.dart';

/// The GFDI message vocabulary for the file-sync flow.
///
/// A deliberately small slice of Gadgetbridge's ~30-message `GarminMessage`
/// enum: only what a read-only FIT sync needs — request a download, receive its
/// chunks and acknowledge them, archive a finished file, and send system events.
/// Notifications, music, weather, uploads and protobuf are all out of scope.
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
  static const int setFileFlags = 5008;
  static const int deviceInformation = 5024;
  static const int systemEvent = 5030;
  static const int supportedFileTypesRequest = 5031;
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

/// A message outside the sync vocabulary — kept (with its type) for logging,
/// never acted on.
class GarminUnhandledMessage extends GarminInboundMessage {
  const GarminUnhandledMessage(this.messageType);
  final int messageType;
}

/// Parses a decoded [GarminGfdiFrame] into a typed [GarminInboundMessage].
GarminInboundMessage decodeGarminMessage(GarminGfdiFrame frame) {
  switch (frame.messageType) {
    case GarminMessageId.response:
      return _decodeStatus(frame.payload);
    case GarminMessageId.fileTransferData:
      return _decodeFileTransferData(frame.payload);
    default:
      return GarminUnhandledMessage(frame.messageType);
  }
}

GarminInboundMessage _decodeStatus(Uint8List payload) {
  final reader = GarminByteReader(payload);
  final originalType = reader.readShort();
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
