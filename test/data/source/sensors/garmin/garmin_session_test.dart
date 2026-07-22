import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/source/sensors/garmin/garmin_byte_writer.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_crc.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_file_types.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_gfdi_frame.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_messages.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_session.dart';

Uint8List _b(List<int> xs) => Uint8List.fromList(xs);

/// A fake vívoactive 5 on the other end of the pipe.
///
/// Speaks the real wire format — every frame it emits goes through
/// [GarminGfdiFrame.build] and everything it receives through `.parse` — so the
/// session is exercised against bytes, not against a mock of itself.
class _FakeWatch {
  _FakeWatch({required this.files, this.refuseIndexes = const {}});

  /// fileIndex -> contents the watch will serve.
  final Map<int, Uint8List> files;

  /// Indexes the watch answers with a non-OK download status.
  final Set<int> refuseIndexes;

  /// Frames the session sent us, decoded.
  final List<GarminGfdiFrame> received = [];

  /// Frames to hand back to the session, in order.
  final List<Uint8List> outbox = [];

  /// Chunk size the watch streams at — small on purpose, so multi-chunk
  /// reassembly is exercised.
  int chunkSize = 8;


  void onFrame(GarminGfdiFrame frame) {
    received.add(frame);
    switch (frame.messageType) {
      case GarminMessageId.response:
        // Our ACKs; the watch has nothing to say back.
        break;
      case GarminMessageId.supportedFileTypesRequest:
        outbox.add(_supportedTypes());
      case GarminMessageId.downloadRequest:
        final index = frame.payload[0] | (frame.payload[1] << 8);
        _startServing(index);
      case GarminMessageId.setFileFlags:
      case GarminMessageId.systemEvent:
        break;
    }
  }

  void _startServing(int index) {
    if (refuseIndexes.contains(index)) {
      outbox.add(_downloadStatus(ok: false, size: 0));
      return;
    }
    final content = files[index];
    if (content == null) {
      outbox.add(_downloadStatus(ok: false, size: 0));
      return;
    }

    outbox.add(_downloadStatus(ok: true, size: content.length));
    // Stream it as chunks with a running CRC, exactly as the watch does.
    var offset = 0;
    var runningCrc = 0;
    while (offset < content.length) {
      final end = (offset + chunkSize).clamp(0, content.length);
      final chunk = Uint8List.sublistView(content, offset, end);
      runningCrc = GarminCrc.compute(chunk, initialCrc: runningCrc);
      outbox.add(_fileChunk(offset: offset, crc: runningCrc, data: chunk));
      offset = end;
    }
  }

  Uint8List deviceInformation() {
    final w = GarminByteWriter()
      ..writeShort(120) // protocol version
      ..writeShort(4315) // product number
      ..writeInt(123456) // unit number
      ..writeShort(1915) // software version -> 19.15
      ..writeShort(500) // max packet size
      ..writeString('vívoactive 5')
      ..writeString('vivoactive5')
      ..writeString('vívoactive 5');
    return GarminGfdiFrame.build(
        GarminMessageId.deviceInformation, w.toBytes());
  }

  Uint8List authNegotiation() {
    final w = GarminByteWriter()
      ..writeByte(0x07)
      ..writeInt(0x0000_00FF);
    return GarminGfdiFrame.build(GarminMessageId.authNegotiation, w.toBytes());
  }

  Uint8List _supportedTypes() {
    final w = GarminByteWriter()
      ..writeShort(GarminMessageId.supportedFileTypesRequest)
      ..writeByte(GarminStatus.ack.code)
      ..writeByte(2)
      ..writeByte(128)
      ..writeByte(49)
      ..writeString('SLEEP')
      ..writeByte(128)
      ..writeByte(32)
      ..writeString('MONITOR');
    return GarminGfdiFrame.build(GarminMessageId.response, w.toBytes());
  }

  Uint8List _downloadStatus({required bool ok, required int size}) {
    final w = GarminByteWriter()
      ..writeShort(GarminMessageId.downloadRequest)
      ..writeByte(GarminStatus.ack.code)
      ..writeByte(ok ? 0 : 1) // OK / INDEX_UNKNOWN
      ..writeInt(size);
    return GarminGfdiFrame.build(GarminMessageId.response, w.toBytes());
  }

  Uint8List _fileChunk({
    required int offset,
    required int crc,
    required Uint8List data,
  }) {
    final w = GarminByteWriter()
      ..writeByte(0)
      ..writeShort(crc)
      ..writeInt(offset)
      ..writeBytes(data);
    return GarminGfdiFrame.build(
        GarminMessageId.fileTransferData, w.toBytes());
  }
}

/// Builds a directory file listing [entries] as `(index, dataType, subType,
/// number)`.
Uint8List _directory(List<(int, int, int, int)> entries) {
  final w = GarminByteWriter();
  for (final (index, dataType, subType, number) in entries) {
    w
      ..writeShort(index)
      ..writeByte(dataType)
      ..writeByte(subType)
      ..writeShort(number)
      ..writeByte(0)
      ..writeByte(0)
      ..writeInt(64)
      ..writeInt(0);
  }
  return w.toBytes();
}

/// Runs a session against [watch] until it settles, pumping the pipe both ways.
Future<List<GarminDownloadedFile>> _runSync(
  _FakeWatch watch, {
  Set<String> alreadySynced = const {},
  List<GarminSyncProgress>? progress,
}) async {
  late GarminSession session;
  session = GarminSession(
    send: (frame) async => watch.onFrame(GarminGfdiFrame.parse(frame)),
    bluetoothName: 'Pixel 6 Pro',
    manufacturer: 'Google',
    model: 'raven',
    alreadySynced: alreadySynced,
    onProgress: progress?.add,
    emptyGrace: Duration.zero,
  )..start();

  // The watch speaks first.
  watch.outbox
    ..add(watch.deviceInformation())
    ..add(watch.authNegotiation());

  // Pump until the watch stops producing and the session settles.
  var guard = 0;
  while (watch.outbox.isNotEmpty) {
    if (guard++ > 10000) fail('sync did not settle');
    final frame = watch.outbox.removeAt(0);
    await session.handleFrame(GarminGfdiFrame.parse(frame));
  }
  return session.done;
}

void main() {
  group('GarminSession happy path', () {
    late _FakeWatch watch;
    late List<GarminDownloadedFile> files;
    late List<GarminSyncProgress> progress;

    setUp(() async {
      final sleep = _b([for (var i = 0; i < 20; i++) 0xA0 + (i % 16)]);
      final monitor = _b([for (var i = 0; i < 35; i++) i]);
      watch = _FakeWatch(files: {
        0: _directory([
          (5, 128, 49, 1), // sleep
          (6, 128, 32, 2), // monitor
        ]),
        5: sleep,
        6: monitor,
      });
      progress = [];
      files = await _runSync(watch, progress: progress);
    });

    test('downloads every wanted file, byte-exact across chunks', () {
      expect(files, hasLength(2));
      expect(files[0].entry.type, GarminFileType.sleep);
      expect(files[0].bytes, watch.files[5]);
      expect(files[1].entry.type, GarminFileType.monitor);
      expect(files[1].bytes, watch.files[6]);
    });

    test('answers the introduction and the auth challenge', () {
      final responses = watch.received
          .where((f) => f.messageType == GarminMessageId.response)
          .map((f) => f.payload[0] | (f.payload[1] << 8))
          .toList();
      expect(responses, contains(GarminMessageId.deviceInformation));
      expect(responses, contains(GarminMessageId.authNegotiation));
    });

    test('records what the watch said about itself', () async {
      // The transport needs maxPacketSize; the rest is for diagnostics.
      expect(watch.received.first.messageType, GarminMessageId.response);
      expect(files, isNotEmpty);
    });

    test('archives each downloaded file so it is not re-offered', () {
      final archived = watch.received
          .where((f) => f.messageType == GarminMessageId.setFileFlags)
          .map((f) => f.payload[0] | (f.payload[1] << 8))
          .toList();
      expect(archived, [5, 6]);
      // The directory itself is never archived.
      expect(archived, isNot(contains(0)));
    });

    test('brackets the sync with SYNC_READY and SYNC_COMPLETE', () {
      final events = watch.received
          .where((f) => f.messageType == GarminMessageId.systemEvent)
          .map((f) => f.payload[0])
          .toList();
      expect(events.first, GarminSystemEventType.syncReady.index);
      expect(events.last, GarminSystemEventType.syncComplete.index);
    });

    test('acknowledges every data chunk with the offset reached', () {
      final acks = watch.received
          .where((f) =>
              f.messageType == GarminMessageId.response &&
              (f.payload[0] | (f.payload[1] << 8)) ==
                  GarminMessageId.fileTransferData)
          .toList();
      // 20 bytes at 8/chunk = 3, 35 bytes = 5, directory 32 bytes = 4.
      expect(acks, hasLength(12));
    });

    test('reports progress through every phase', () {
      final phases = progress.map((p) => p.phase).toSet();
      expect(phases, contains(GarminSyncPhase.handshake));
      expect(phases, contains(GarminSyncPhase.listing));
      expect(phases, contains(GarminSyncPhase.downloading));
      expect(phases, contains(GarminSyncPhase.complete));
      expect(progress.last.filesDone, 2);
    });
  });

  group('GarminSession resilience', () {
    test('skips a file the watch refuses, and still gets the others', () async {
      final watch = _FakeWatch(
        files: {
          0: _directory([
            (5, 128, 49, 1),
            (6, 128, 32, 2),
          ]),
          5: _b([1, 2, 3]),
          6: _b([4, 5, 6]),
        },
        refuseIndexes: {5},
      );

      final files = await _runSync(watch);

      // One unreadable file must not cost the night's other data.
      expect(files, hasLength(1));
      expect(files.single.entry.type, GarminFileType.monitor);
    });

    test('skips a file whose chunk CRC is wrong', () async {
      final watch = _CorruptingWatch(files: {
        0: _directory([
          (5, 128, 49, 1),
          (6, 128, 32, 2),
        ]),
        5: _b([1, 2, 3, 4, 5]),
        6: _b([9, 9, 9]),
      }, corruptIndex: 5);

      final files = await _runSync(watch);

      expect(files.map((f) => f.entry.fileIndex), [6]);
    });

    test('files with no dedup key are always fetched, never skipped', () async {
      final watch = _FakeWatch(files: {
        0: _directory([(5, 128, 49, 0xFFFF)]), // sleep, unset file number
        5: _b([1, 2, 3]),
      });

      // Even with a key that WOULD match if one existed, an unkeyed file must
      // still be fetched — the alternative is losing every future sleep file.
      final files = await _runSync(watch, alreadySynced: {'128/49/65535'});

      expect(files, hasLength(1));
    });

    test('a directory with nothing new completes without downloading', () async {
      final watch = _FakeWatch(files: {
        0: _directory([(5, 128, 49, 1)]),
        5: _b([1, 2, 3]),
      });

      final files = await _runSync(watch, alreadySynced: {'128/49/1'});

      expect(files, isEmpty);
      // Still a clean, bracketed sync.
      final events = watch.received
          .where((f) => f.messageType == GarminMessageId.systemEvent)
          .map((f) => f.payload[0]);
      expect(events.last, GarminSystemEventType.syncComplete.index);
      // And nothing was requested beyond the directory itself.
      final requested = watch.received
          .where((f) => f.messageType == GarminMessageId.downloadRequest)
          .map((f) => f.payload[0] | (f.payload[1] << 8));
      expect(requested, [0]);
    });

    test('an empty directory completes cleanly', () async {
      final watch = _FakeWatch(files: {0: _directory([])});
      expect(await _runSync(watch), isEmpty);
    });

    test('unmapped file types are never requested', () async {
      final watch = _FakeWatch(files: {
        0: _directory([
          (5, 128, 55, 1), // golf scorecard — unmapped
          (6, 128, 49, 2), // sleep — wanted
        ]),
        6: _b([1, 2, 3]),
      });

      final files = await _runSync(watch);

      expect(files.map((f) => f.entry.fileIndex), [6]);
      final requested = watch.received
          .where((f) => f.messageType == GarminMessageId.downloadRequest)
          .map((f) => f.payload[0] | (f.payload[1] << 8));
      expect(requested, [0, 6]);
    });

    test('the capabilities exchange is answered with our own bitmap', () async {
      final watch = _ChattyWatch(files: {0: _directory([])});

      await _runSync(watch);

      // Not just an ACK: the watch waits for a CONFIGURATION of our own, and
      // without it a real device re-sent its own and listed nothing.
      final config = watch.received
          .where((f) => f.messageType == GarminMessageId.configuration)
          .toList();
      expect(config, hasLength(1));
      // [byte length][bitmap] — 15 bytes, matching what the watch sends.
      expect(config.single.payload.first, 15);
      expect(config.single.payload.length, 16);
    });

    test('notification subscription gets a full status, not a bare ACK',
        () async {
      final watch = _ChattyWatch(files: {0: _directory([])});

      await _runSync(watch);

      final replies = watch.received
          .where((f) => f.messageType == GarminMessageId.response)
          .where((f) =>
              (f.payload[0] | (f.payload[1] << 8)) ==
              GarminMessageId.notificationSubscription)
          .toList();
      expect(replies, hasLength(1));
      // [short type][status][notificationStatus][enable][unk] — the short form
      // is what made the watch ask again every second.
      expect(replies.single.payload, hasLength(6));
      expect(replies.single.payload[3], 1, reason: 'DISABLED — we forward none');
    });

    test('every unanswered inbound message gets a generic ACK', () async {
      // The watch retransmits anything it thinks was lost and will not move on,
      // which is exactly how a real vívoactive 5 stalled with an empty
      // directory while re-sending its CONFIGURATION message.
      final watch = _ChattyWatch(files: {0: _directory([])});

      await _runSync(watch);

      final acked = watch.received
          .where((f) => f.messageType == GarminMessageId.response)
          .map((f) => f.payload[0] | (f.payload[1] << 8))
          .toList();
      expect(acked, contains(GarminMessageId.configuration));
      expect(acked, contains(5043)); // PROTOBUF_REQUEST
    });

    test('an ACK is never itself ACKed', () async {
      final watch = _FakeWatch(files: {0: _directory([])});

      await _runSync(watch);

      // A RESPONSE naming RESPONSE would bounce between the two forever.
      final acked = watch.received
          .where((f) => f.messageType == GarminMessageId.response)
          .map((f) => f.payload[0] | (f.payload[1] << 8));
      expect(acked, isNot(contains(GarminMessageId.response)));
    });

    test('messages with their own response are not double-acked', () async {
      final watch = _FakeWatch(files: {0: _directory([])});

      await _runSync(watch);

      // Device information and auth each get exactly one reply — the response
      // that carries our details IS the acknowledgement.
      for (final type in [
        GarminMessageId.deviceInformation,
        GarminMessageId.authNegotiation,
      ]) {
        final replies = watch.received
            .where((f) => f.messageType == GarminMessageId.response)
            .where((f) => (f.payload[0] | (f.payload[1] << 8)) == type);
        expect(replies, hasLength(1), reason: 'type $type');
      }
    });

    test('a FILTER is sent before the directory is requested', () async {
      final watch = _FakeWatch(files: {0: _directory([])});

      await _runSync(watch);

      final order = watch.received.map((f) => f.messageType).toList();
      final filterAt = order.indexOf(GarminMessageId.filter);
      final directoryAt = order.indexOf(GarminMessageId.downloadRequest);
      expect(filterAt, isNonNegative, reason: 'the filter must be sent');
      expect(
        filterAt,
        lessThan(directoryAt),
        reason: 'the watch processes writes in order, so the filter has to '
            'land before the listing is asked for',
      );
    });

    test('a synchronization announcement re-reads the listing', () async {
      // First listing empty, then the watch announces it holds sleep data.
      final watch = _AnnouncingWatch(files: {
        0: _directory([(5, 128, 49, 1)]),
        5: _b([1, 2, 3]),
      });

      final files = await _runSync(watch);

      // The re-read must find the file the first pass could not.
      expect(files.map((f) => f.entry.fileIndex), [5]);
      // Two directory requests: the initial one and the post-announcement one.
      final directoryRequests = watch.received
          .where((f) => f.messageType == GarminMessageId.downloadRequest)
          .where((f) => (f.payload[0] | (f.payload[1] << 8)) == 0);
      expect(directoryRequests, hasLength(2));
    });

    test('an announcement with nothing we want does not re-read', () async {
      final watch = _AnnouncingWatch(
        files: {0: _directory([])},
        // Bit 1 is SETTINGS — not one of the categories worth acting on.
        bitmask: 1 << 1,
      );

      await _runSync(watch);

      final directoryRequests = watch.received
          .where((f) => f.messageType == GarminMessageId.downloadRequest);
      expect(directoryRequests, hasLength(1));
    });

    test('a link that dies during the empty grace still settles the sync',
        () async {
      // The grace window waits out a watch that announces late — and it is
      // exactly when a watch walks out of range. The send inside the timer
      // throws then, and an unawaited future that rejects is an unhandled async
      // error that leaves `done` pending forever.
      final watch = _FakeWatch(files: {0: _directory([])});
      var connected = true;
      late final GarminSession session;
      session = GarminSession(
        send: (frame) async {
          if (!connected) throw StateError('link dropped');
          watch.onFrame(GarminGfdiFrame.parse(frame));
        },
        bluetoothName: 'Pixel',
        manufacturer: 'Google',
        model: 'raven',
        emptyGrace: const Duration(milliseconds: 20),
      )..start();

      watch.outbox
        ..add(watch.deviceInformation())
        ..add(watch.authNegotiation());
      while (watch.outbox.isNotEmpty) {
        await session.handleFrame(
          GarminGfdiFrame.parse(watch.outbox.removeAt(0)),
        );
      }
      // The listing came back empty, so the grace timer is now armed. Take the
      // link away before it fires.
      connected = false;

      await expectLater(
        session.done.timeout(const Duration(seconds: 2)),
        completion(isEmpty),
      );
    });

    test('a file is kept before it is archived', () async {
      final watch = _FakeWatch(files: {
        0: _directory([(5, 128, 49, 1)]),
        5: _b([1, 2, 3]),
      });
      final order = <String>[];

      final session = GarminSession(
        send: (frame) async {
          final parsed = GarminGfdiFrame.parse(frame);
          if (parsed.messageType == GarminMessageId.setFileFlags) {
            order.add('archive');
          }
          watch.onFrame(parsed);
        },
        bluetoothName: 'Pixel',
        manufacturer: 'Google',
        model: 'raven',
        emptyGrace: Duration.zero,
        onFileDownloaded: (_) async => order.add('kept'),
      )..start();
      watch.outbox
        ..add(watch.deviceInformation())
        ..add(watch.authNegotiation());
      while (watch.outbox.isNotEmpty) {
        await session.handleFrame(
          GarminGfdiFrame.parse(watch.outbox.removeAt(0)),
        );
      }
      await session.done;

      // Archiving is irreversible from our side, so the copy must land first.
      expect(order, ['kept', 'archive']);
    });

    test('a file that could not be kept is NOT archived', () async {
      final watch = _FakeWatch(files: {
        0: _directory([(5, 128, 49, 1)]),
        5: _b([1, 2, 3]),
      });

      final session = GarminSession(
        send: (frame) async => watch.onFrame(GarminGfdiFrame.parse(frame)),
        bluetoothName: 'Pixel',
        manufacturer: 'Google',
        model: 'raven',
        emptyGrace: Duration.zero,
        onFileDownloaded: (_) async => throw const FileSystemException('disk'),
      )..start();
      watch.outbox
        ..add(watch.deviceInformation())
        ..add(watch.authNegotiation());
      while (watch.outbox.isNotEmpty) {
        await session.handleFrame(
          GarminGfdiFrame.parse(watch.outbox.removeAt(0)),
        );
      }
      final files = await session.done;

      // The file is still returned for import, but the watch keeps offering it
      // — better a redundant download than data we can never fetch again.
      expect(files, hasLength(1));
      expect(
        watch.received.where((f) =>
            f.messageType == GarminMessageId.setFileFlags),
        isEmpty,
      );
    });

    test('abort keeps what was already downloaded', () async {
      final session = GarminSession(
        send: (_) async {},
        bluetoothName: 'Pixel',
        manufacturer: 'Google',
        model: 'raven',
      )..start();

      session.abort('link dropped');

      expect(await session.done, isEmpty);
    });

    test('ignores frames that arrive after completion', () async {
      final watch = _FakeWatch(files: {0: _directory([])});
      final session = GarminSession(
        send: (frame) async => watch.onFrame(GarminGfdiFrame.parse(frame)),
        bluetoothName: 'Pixel',
        manufacturer: 'Google',
        model: 'raven',
        emptyGrace: Duration.zero,
      )..start();
      watch.outbox
        ..add(watch.deviceInformation())
        ..add(watch.authNegotiation());
      while (watch.outbox.isNotEmpty) {
        await session.handleFrame(
          GarminGfdiFrame.parse(watch.outbox.removeAt(0)),
        );
      }
      await session.done;

      // A watch that keeps chattering must not throw or reopen the sync.
      await session.handleFrame(GarminGfdiFrame.parse(watch.authNegotiation()));
      expect(await session.done, isEmpty);
    });

    test('keeps acknowledging after completion when listening', () async {
      final watch = _FakeWatch(files: {0: _directory([])});
      final session = GarminSession(
        send: (frame) async => watch.onFrame(GarminGfdiFrame.parse(frame)),
        bluetoothName: 'Pixel',
        manufacturer: 'Google',
        model: 'raven',
        emptyGrace: Duration.zero,
        keepAnsweringAfterSync: true,
      )..start();
      watch.outbox
        ..add(watch.deviceInformation())
        ..add(watch.authNegotiation());
      while (watch.outbox.isNotEmpty) {
        await session.handleFrame(
          GarminGfdiFrame.parse(watch.outbox.removeAt(0)),
        );
      }
      await session.done;

      final before = watch.received.length;
      await session.handleFrame(GarminGfdiFrame.parse(watch.authNegotiation()));

      // The point of the diagnostic window: a frame arriving after the sync is
      // still answered. Silence here would make the watch retransmit on a timer
      // and eventually drop the link the pass depends on.
      expect(watch.received.length, greaterThan(before));
      expect(await session.done, isEmpty);
    });
  });
}

/// A watch that corrupts the CRC of one file's first chunk.
class _CorruptingWatch extends _FakeWatch {
  _CorruptingWatch({required super.files, required this.corruptIndex});

  final int corruptIndex;

  @override
  void _startServing(int index) {
    if (index != corruptIndex) {
      super._startServing(index);
      return;
    }
    final content = files[index]!;
    outbox.add(_downloadStatus(ok: true, size: content.length));
    // Deliberately wrong running CRC.
    outbox.add(_fileChunk(offset: 0, crc: 0xDEAD, data: content));
  }
}

/// A watch that serves an EMPTY listing first, then announces it holds sleep
/// data — the shape observed on a real vívoactive 5.
class _AnnouncingWatch extends _FakeWatch {
  _AnnouncingWatch({required super.files, this.bitmask = 1 << 26});

  /// Bit 26 is SLEEP in SynchronizationMessage.FileType.
  final int bitmask;

  bool _announced = false;

  @override
  void _startServing(int index) {
    if (index == 0 && !_announced) {
      // The first listing is empty; the announcement follows it.
      _announced = true;
      outbox.add(_downloadStatus(ok: true, size: 0));
      outbox.add(_synchronization());
      return;
    }
    super._startServing(index);
  }

  Uint8List _synchronization() {
    final w = GarminByteWriter()
      ..writeByte(0) // TYPE_0
      ..writeByte(8) // 8-byte bitmask
      ..writeLong(bitmask);
    return GarminGfdiFrame.build(
        GarminMessageId.synchronization, w.toBytes());
  }
}

/// A watch that also emits the chatter a real vívoactive 5 sends during the
/// handshake — configuration, protobuf requests, notification subscription —
/// none of which this app answers with a response of its own.
class _ChattyWatch extends _FakeWatch {
  _ChattyWatch({required super.files});

  bool _chattered = false;

  @override
  void _startServing(int index) {
    if (index == 0 && !_chattered) {
      _chattered = true;
      // Queued BEFORE the listing, as observed on the device.
      outbox
        // CONFIGURATION: [length][15 capability bytes], as the real watch sends.
        ..add(GarminGfdiFrame.build(
            GarminMessageId.configuration,
            _b([15, ...List<int>.filled(15, 0xAA)])))
        ..add(GarminGfdiFrame.build(5043, _b([0x8f, 0x03, 0x00, 0x00])))
        ..add(GarminGfdiFrame.build(
            GarminMessageId.notificationSubscription, _b([0x00, 0x00])));
    }
    super._startServing(index);
  }
}
