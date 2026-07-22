import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/source/sensors/garmin/garmin_byte_writer.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_directory.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_file_types.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_gfdi_frame.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_messages.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_time.dart';

Uint8List _b(List<int> xs) => Uint8List.fromList(xs);

/// Round-trips an outgoing message through the frame layer, as the transport
/// will: build → parse → decode.
GarminInboundMessage _roundTrip(Uint8List wire) =>
    decodeGarminMessage(GarminGfdiFrame.parse(wire));

void main() {
  group('GarminTime', () {
    test('the Garmin epoch maps to 1989-12-31T00:00:00Z', () {
      expect(GarminTime.toDateTime(0), DateTime.utc(1989, 12, 31));
    });

    test('round-trips a real recording instant', () {
      final when = DateTime.utc(2026, 7, 22, 9, 30);
      expect(GarminTime.toDateTime(GarminTime.fromDateTime(when)), when);
    });
  });

  group('GarminFileType', () {
    test('maps the FIT sub-types the importer consumes', () {
      expect(GarminFileType.fromCodes(128, 4), GarminFileType.activity);
      expect(GarminFileType.fromCodes(128, 49), GarminFileType.sleep);
      expect(GarminFileType.fromCodes(128, 68), GarminFileType.hrvStatus);
      expect(GarminFileType.fromCodes(128, 32), GarminFileType.monitor);
    });

    test('an unmapped sub-type is null, not an error', () {
      // Golf scorecard (128,55) — a real type this app does not handle.
      expect(GarminFileType.fromCodes(128, 55), isNull);
    });

    test('virtual types are not wanted by the downloader', () {
      expect(GarminFileType.directory.wanted, isFalse);
      expect(GarminFileType.deviceXml.wanted, isFalse);
      expect(GarminFileType.activity.wanted, isTrue);
    });
  });

  group('GarminDirectory.parse', () {
    /// Builds one 16-byte directory record.
    Uint8List entry({
      required int index,
      required int dataType,
      required int subType,
      int number = 1,
      int size = 100,
      int timestamp = 0,
    }) =>
        (GarminByteWriter()
              ..writeShort(index)
              ..writeByte(dataType)
              ..writeByte(subType)
              ..writeShort(number)
              ..writeByte(0)
              ..writeByte(0)
              ..writeInt(size)
              ..writeInt(timestamp))
            .toBytes();

    test('keeps wanted FIT files and resolves their fields', () {
      final ts = GarminTime.fromDateTime(DateTime.utc(2026, 7, 20, 3));
      final data = _b([
        ...entry(index: 5, dataType: 128, subType: 49, number: 7, size: 2048,
            timestamp: ts),
      ]);

      final entries = GarminDirectory.parse(data);

      expect(entries, hasLength(1));
      final e = entries.single;
      expect(e.fileIndex, 5);
      expect(e.type, GarminFileType.sleep);
      expect(e.fileNumber, 7);
      expect(e.fileSize, 2048);
      expect(e.fileDate, DateTime.utc(2026, 7, 20, 3));
      expect(e.dedupKey, '128/49/7');
    });

    test('drops unmapped types and the all-zero sentinel', () {
      final data = _b([
        ...entry(index: 5, dataType: 128, subType: 49), // sleep — kept
        ...entry(index: 6, dataType: 128, subType: 55), // golf — unmapped
        ...entry(index: 0, dataType: 0, subType: 0, number: 0, size: 0), // pad
      ]);

      final entries = GarminDirectory.parse(data);

      expect(entries.map((e) => e.type), [GarminFileType.sleep]);
    });

    test('a zero wire timestamp becomes a null date, not the Garmin epoch', () {
      final data = entry(index: 5, dataType: 128, subType: 4, timestamp: 0);
      expect(GarminDirectory.parse(data).single.fileDate, isNull);
    });

    test('diagnostics distinguish empty from filtered-out', () {
      // "0 entries" has several causes; the listing must say which.
      final empty = GarminDirectory.parseWithDiagnostics(Uint8List(0));
      expect(empty.totalRecords, 0);
      expect(empty.entries, isEmpty);

      final filtered = GarminDirectory.parseWithDiagnostics(_b([
        ...entry(index: 6, dataType: 128, subType: 55), // unmapped
        ...entry(index: 7, dataType: 8, subType: 255), // known but not wanted
      ]));
      expect(filtered.totalRecords, 2);
      expect(filtered.entries, isEmpty);
      // The raw codes are what make an unmapped type diagnosable on a device.
      expect(filtered.skipped, contains('128/55?'));
      expect(filtered.skipped, contains('deviceXml!'));
    });

    test('a trailing partial record is ignored', () {
      final data = _b([
        ...entry(index: 5, dataType: 128, subType: 4),
        0x01, 0x02, 0x03, // 3 stray bytes — not a whole record
      ]);
      expect(GarminDirectory.parse(data), hasLength(1));
    });
  });

  group('outbound messages round-trip through the frame layer', () {
    test('download request carries the file index and fresh type', () {
      final status = _roundTrip(buildDownloadRequest(fileIndex: 5));
      // The watch replies with a status; the request itself is not inbound, so
      // assert on the raw frame instead.
      expect(status, isA<GarminUnhandledMessage>());
      final frame = GarminGfdiFrame.parse(buildDownloadRequest(fileIndex: 5));
      expect(frame.messageType, GarminMessageId.downloadRequest);
      // fileIndex is the first short of the payload.
      expect(frame.payload[0] | (frame.payload[1] << 8), 5);
    });

    test('file-transfer ack names FILE_TRANSFER_DATA with ACK/OK', () {
      final frame = GarminGfdiFrame.parse(buildFileTransferDataAck(1024));
      expect(frame.messageType, GarminMessageId.response);
      // [short originalType][byte ACK][byte OK][int offset]
      expect(frame.payload[0] | (frame.payload[1] << 8),
          GarminMessageId.fileTransferData);
      expect(frame.payload[2], GarminStatus.ack.code);
      expect(frame.payload[3], 0);
    });

    test('archive flag is 0x10', () {
      final frame = GarminGfdiFrame.parse(
        buildSetFileFlags(5, GarminFileFlag.archive),
      );
      expect(frame.messageType, GarminMessageId.setFileFlags);
      expect(frame.payload.last, 0x10);
    });

    test('SYNC_READY encodes as system-event ordinal 8', () {
      final frame = GarminGfdiFrame.parse(
        buildSystemEvent(GarminSystemEventType.syncReady),
      );
      expect(frame.messageType, GarminMessageId.systemEvent);
      expect(frame.payload[0], 8);
    });
  });

  group('inbound message decoding', () {
    /// Builds a RESPONSE (5000) frame carrying a download-request status.
    Uint8List downloadStatusFrame({
      required int status,
      required int downloadStatus,
      required int maxFileSize,
    }) {
      final payload = GarminByteWriter()
        ..writeShort(GarminMessageId.downloadRequest)
        ..writeByte(status)
        ..writeByte(downloadStatus)
        ..writeInt(maxFileSize);
      return GarminGfdiFrame.build(
          GarminMessageId.response, payload.toBytes());
    }

    test('a proceed-able download status carries the file size', () {
      final msg = _roundTrip(downloadStatusFrame(
        status: GarminStatus.ack.code,
        downloadStatus: 0, // OK
        maxFileSize: 4096,
      ));
      expect(msg, isA<GarminDownloadRequestStatus>());
      final status = msg as GarminDownloadRequestStatus;
      expect(status.canProceed, isTrue);
      expect(status.maxFileSize, 4096);
    });

    test('a non-OK download status does not proceed', () {
      final msg = _roundTrip(downloadStatusFrame(
        status: GarminStatus.ack.code,
        downloadStatus: 1, // INDEX_UNKNOWN
        maxFileSize: 0,
      )) as GarminDownloadRequestStatus;
      expect(msg.canProceed, isFalse);
      expect(msg.downloadStatus, GarminDownloadStatus.indexUnknown);
    });

    test('a file-transfer data chunk exposes offset, crc and payload', () {
      final payload = GarminByteWriter()
        ..writeByte(0) // flags
        ..writeShort(0xBEEF) // crc
        ..writeInt(2048) // dataOffset
        ..writeBytes(_b([1, 2, 3, 4]));
      final msg = _roundTrip(
        GarminGfdiFrame.build(
            GarminMessageId.fileTransferData, payload.toBytes()),
      );
      expect(msg, isA<GarminFileTransferData>());
      final chunk = msg as GarminFileTransferData;
      expect(chunk.dataOffset, 2048);
      expect(chunk.crc, 0xBEEF);
      expect(chunk.data, _b([1, 2, 3, 4]));
    });

    test('an out-of-vocabulary message decodes to unhandled, not an error', () {
      // Music control (5041) — a message the watch sends that a read-only sync
      // ignores.
      final msg = _roundTrip(GarminGfdiFrame.build(5041, _b([1, 2, 3])));
      expect(msg, isA<GarminUnhandledMessage>());
      expect((msg as GarminUnhandledMessage).messageType, 5041);
    });
  });
}
