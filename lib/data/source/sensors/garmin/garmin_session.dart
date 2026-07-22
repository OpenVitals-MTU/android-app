import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/foundation.dart';

import 'garmin_crc.dart';
import 'garmin_directory.dart';
import 'garmin_file_types.dart';
import 'garmin_gfdi_frame.dart';
import 'garmin_messages.dart';

/// One downloaded file and the directory entry it came from.
class GarminDownloadedFile {
  const GarminDownloadedFile({required this.entry, required this.bytes});
  final GarminDirectoryEntry entry;
  final Uint8List bytes;
}

/// Progress of a running sync, for the UI.
class GarminSyncProgress {
  const GarminSyncProgress({
    required this.phase,
    this.filesTotal = 0,
    this.filesDone = 0,
    this.currentFile,
  });

  final GarminSyncPhase phase;
  final int filesTotal;
  final int filesDone;
  final String? currentFile;
}

enum GarminSyncPhase { handshake, listing, downloading, complete, failed }

/// Drives one GFDI sync: handshake, list the directory, download each wanted
/// file, archive it, finish.
///
/// **Transport-free by construction.** It consumes decoded GFDI frames and emits
/// frames to send, so it is exercised end to end over an in-memory pipe with no
/// Bluetooth — the same split `lib/data/source/sync/` uses for the RFCOMM
/// protocol. The ML/BLE layer below it only moves bytes.
///
/// Ported from Gadgetbridge's `GarminSupport` + `FileTransferHandler` (AGPLv3),
/// narrowed to a read-only sync: no uploads, no notifications, no realtime.
class GarminSession {
  GarminSession({
    required this.send,
    required this.bluetoothName,
    required this.manufacturer,
    required this.model,
    this.alreadySynced = const {},
    this.onProgress,
    this.emptyGrace = const Duration(seconds: 6),
  });

  /// Hands one built GFDI frame to the transport below. The session never sees
  /// COBS, handles or characteristics — that is the ML/BLE layer's job.
  final Future<void> Function(Uint8List frame) send;

  final String bluetoothName;
  final String manufacturer;
  final String model;

  /// Dedup keys ([GarminDirectoryEntry.dedupKey]) already imported by a previous
  /// sync. Purely a bandwidth optimisation — Health Connect's `clientRecordId`
  /// already makes a re-import idempotent — so a stale set costs airtime, never
  /// correctness.
  final Set<String> alreadySynced;

  final void Function(GarminSyncProgress)? onProgress;

  /// How long to keep a fruitless sync open before giving up.
  ///
  /// The watch may announce what it holds (SYNCHRONIZATION) a moment after the
  /// listing is served. Finishing the instant an empty directory arrives races
  /// that announcement and throws it away, which looks identical to "the watch
  /// has nothing". Injectable so tests need not wait it out.
  final Duration emptyGrace;

  final Completer<List<GarminDownloadedFile>> _done =
      Completer<List<GarminDownloadedFile>>();

  /// Files fetched this run, handed to the importer when [run] completes.
  final List<GarminDownloadedFile> _downloaded = [];

  /// Entries still to fetch, filled from the directory.
  final List<GarminDirectoryEntry> _queue = [];

  GarminDeviceInformation? deviceInformation;
  List<GarminSupportedFileType> supportedTypes = const [];

  /// The transfer in flight, or null between files.
  _ActiveDownload? _active;

  /// Set once the directory has been fetched, so its own transfer is recognised.
  bool _directoryFetched = false;

  bool _finished = false;
  int _filesTotal = 0;

  /// Resolves with everything downloaded once the sync completes, or rejects on
  /// an unrecoverable protocol error.
  Future<List<GarminDownloadedFile>> get done => _done.future;

  void _report(GarminSyncPhase phase, {String? file}) => onProgress?.call(
        GarminSyncProgress(
          phase: phase,
          filesTotal: _filesTotal,
          filesDone: _downloaded.length,
          currentFile: file,
        ),
      );

  /// Starts the sync. The watch speaks first (device information), so this only
  /// arms the state machine; everything else is driven by [handleFrame].
  void start() => _report(GarminSyncPhase.handshake);

  /// Serialises frame handling. Frames arrive from the transport as they land,
  /// but [_dispatch] awaits its sends — so without this a second notification
  /// could enter the state machine while the first is still mid-flight and
  /// mutate `_active` underneath it, appending chunks out of order.
  Future<void> _chain = Future<void>.value();

  /// Feeds one decoded GFDI frame in. Safe to call after completion — late
  /// frames from a watch that is still talking are ignored rather than throwing.
  ///
  /// Frames are processed strictly in arrival order, however fast they arrive.
  Future<void> handleFrame(GarminGfdiFrame frame) {
    _chain = _chain.then((_) => _handleFrameSerially(frame));
    return _chain;
  }

  Future<void> _handleFrameSerially(GarminGfdiFrame frame) async {
    if (_finished) return;
    try {
      // Acknowledge FIRST, as Gadgetbridge does: an unacknowledged message is
      // treated as lost, and the watch retransmits it on a timer instead of
      // moving on. Types that get their own response envelope are excluded —
      // that response IS their acknowledgement.
      if (!garminSelfAcknowledgedTypes.contains(frame.messageType)) {
        await send(buildGenericAck(frame.messageType));
      }
      await _dispatch(decodeGarminMessage(frame));
    } catch (error, stack) {
      _fail(error, stack);
    }
  }

  Future<void> _dispatch(GarminInboundMessage message) async {
    switch (message) {
      case GarminDeviceInformation():
        deviceInformation = message;
        debugPrint('[GARMIN-SYNC] device ${message.deviceName} '
            '${message.deviceModel} sw=${message.softwareVersionText} '
            'maxPacket=${message.maxPacketSize}');
        await send(buildDeviceInformationResponse(
          incoming: message,
          bluetoothName: bluetoothName,
          manufacturer: manufacturer,
          model: model,
        ));
        // The watch will not serve files until it knows what we support, so the
        // request goes out as soon as the introduction is answered.
        await send(buildSupportedFileTypesRequest());

      case GarminAuthNegotiation():
        await send(buildAuthNegotiationResponse(message));

      case GarminConfiguration():
        // The capabilities exchange. The watch has told us what it can do and
        // is waiting to hear what WE can do; a bare ACK left it re-sending this
        // and never listing any files.
        debugPrint('[GARMIN-SYNC] configuration: '
            '${message.capabilityBits.length}B of capabilities from the watch');
        await send(buildConfigurationResponse());

      case GarminNotificationSubscription():
        // Answered honestly (we forward nothing) but answered — the watch asks
        // roughly once a second until it gets a properly shaped status.
        debugPrint('[GARMIN-SYNC] notification subscription '
            'enable=${message.enable}; replying disabled');
        await send(buildNotificationSubscriptionStatus(message));

      case GarminSupportedFileTypes():
        supportedTypes = message.types;
        // The raw pairs, not just a count: they are the ground truth for which
        // GarminFileType codes a real watch actually offers.
        debugPrint('[GARMIN-SYNC] watch supports ${message.types.length} types: '
            '${message.types.map((t) => "${t.dataType}/${t.subType}:${t.name}").join(", ")}');
        await send(buildSystemEvent(GarminSystemEventType.syncReady));
        // FILTER before the listing. Gadgetbridge only ever sends this in reply
        // to a SYNCHRONIZATION announcement, but an unfiltered listing came back
        // empty from a watch that demonstrably held a night of sleep — and the
        // watch processes our writes in order, so by the time it answers the
        // directory request it has already seen the filter.
        await send(buildFilterMessage());
        _report(GarminSyncPhase.listing);
        await _requestDirectory();

      case GarminDownloadRequestStatus():
        await _onDownloadStatus(message);

      case GarminFileTransferData():
        await _onFileChunk(message);

      case GarminSynchronization():
        // The watch announcing what it holds. Gadgetbridge answers this with a
        // FILTER and only then lists files — and an unfiltered listing came
        // back empty on a watch that demonstrably had a night of sleep on it,
        // so this exchange looks like what actually populates the directory.
        debugPrint('[GARMIN-SYNC] synchronization type=${message.syncType} '
            'bits=${message.setBits} proceed=${message.shouldProceed}');
        if (message.shouldProceed) {
          // Cancel any pending give-up: the watch has just told us it holds
          // something, so re-read the listing rather than finishing empty.
          _graceTimer?.cancel();
          _graceTimer = null;
          await send(buildFilterMessage());
          _directoryFetched = false;
          await _requestDirectory();
        }

      case GarminGenericStatus():
        // ACKs for our own sends. A NAK is logged because it is the only
        // visible sign the watch rejected something we asked for.
        if (message.status != GarminStatus.ack) {
          debugPrint('[GARMIN-SYNC] NAK ${message.status.name} for '
              'message ${message.originalMessageType}');
          break;
        }
        if (message.originalMessageType == GarminMessageId.filter) {
          debugPrint('[GARMIN-SYNC] filter accepted');
        }

      case GarminUnhandledMessage():
        // Logged, not silent: a read-only sync ignores music and notification
        // chatter, but "the watch said something we did not expect" is exactly
        // the evidence a stalled sync needs, and swallowing it hid whether the
        // watch was talking to us at all.
        debugPrint('[GARMIN-SYNC] unhandled message ${message.messageType} '
            '(${message.payload.length}B) ${_hex(message.payload, max: 32)}');
    }
  }

  Future<void> _requestDirectory() async {
    _active = _ActiveDownload(
      entry: const GarminDirectoryEntry(
        fileIndex: 0,
        type: GarminFileType.directory,
        fileNumber: 0,
        specificFlags: 0,
        fileFlags: 0,
        fileSize: 0,
        fileDate: null,
      ),
    );
    await send(buildDownloadRequest(fileIndex: 0));
  }

  Future<void> _onDownloadStatus(GarminDownloadRequestStatus status) async {
    final active = _active;
    if (active == null) return; // Status for a transfer we already abandoned.

    if (!status.canProceed) {
      debugPrint('[GARMIN-SYNC] download refused for index '
          '${active.entry.fileIndex}: ${status.downloadStatus.name}');
      // One unreadable file must not end the sync — skip to the next, exactly
      // as the bulk importer tolerates one bad file in a batch.
      _active = null;
      await _next();
      return;
    }
    active.begin(status.maxFileSize);
    // A zero-length file is complete the moment its size is known — no chunk
    // will ever arrive to trigger the completion path below.
    if (active.isComplete) {
      _active = null;
      await _onFileComplete(active);
    }
  }

  Future<void> _onFileChunk(GarminFileTransferData chunk) async {
    final active = _active;
    if (active == null) return;

    final appended = active.append(chunk);
    if (!appended) {
      // A CRC or offset mismatch means the stream desynchronised. Abandoning
      // this file (rather than the sync) keeps the rest of the night's data.
      debugPrint('[GARMIN-SYNC] chunk rejected for index '
          '${active.entry.fileIndex}; skipping file');
      _active = null;
      await _next();
      return;
    }

    await send(buildFileTransferDataAck(active.received));
    if (!active.isComplete) return;

    _active = null;
    await _onFileComplete(active);
  }

  Future<void> _onFileComplete(_ActiveDownload active) async {
    final bytes = active.bytes;

    if (active.entry.type == GarminFileType.directory) {
      _directoryFetched = true;
      final listing = GarminDirectory.parseWithDiagnostics(bytes);
      // Skip what a previous sync already imported — bandwidth only.
      final fresh = listing.entries
          .where((e) => !alreadySynced.contains(e.dedupKey))
          .toList();
      _queue
        ..clear()
        ..addAll(fresh);
      _filesTotal = fresh.length;
      // Full diagnostics: "0 files" has several very different causes and only
      // the raw record counts and rejected type codes tell them apart.
      debugPrint('[GARMIN-SYNC] directory ${bytes.length}B '
          '${listing.describe()} new=${fresh.length}');
      if (listing.entries.isEmpty && bytes.isNotEmpty) {
        // Nothing usable came back. The raw listing is small (16 bytes a
        // record) and is the only thing that separates "the watch has nothing"
        // from "the watch answers somewhere else" — dump it rather than guess.
        debugPrint('[GARMIN-SYNC] raw directory: ${_hex(bytes)}');
      }
      _report(GarminSyncPhase.downloading);
      await _next();
      return;
    }

    _downloaded.add(
      GarminDownloadedFile(entry: active.entry, bytes: bytes),
    );
    debugPrint('[GARMIN-SYNC] got ${active.entry.type.name} '
        'index=${active.entry.fileIndex} bytes=${bytes.length}');
    // Archive so the watch stops offering it next time.
    await send(
      buildSetFileFlags(active.entry.fileIndex, GarminFileFlag.archive),
    );
    await _next();
  }

  /// Starts the next queued file, or finishes the sync when the queue is empty.
  Future<void> _next() async {
    if (!_directoryFetched) return; // Still waiting on the listing.
    if (_queue.isEmpty) {
      await _complete();
      return;
    }
    final entry = _queue.removeAt(0);
    _active = _ActiveDownload(entry: entry);
    _report(GarminSyncPhase.downloading, file: entry.type.name);
    await send(buildDownloadRequest(fileIndex: entry.fileIndex));
  }

  Timer? _graceTimer;
  bool _graceUsed = false;

  Future<void> _complete() async {
    if (_finished) return;
    if (_downloaded.isEmpty && !_graceUsed) {
      _graceUsed = true;
      debugPrint('[GARMIN-SYNC] nothing listed; waiting '
          '${emptyGrace.inSeconds}s in case the watch announces');
      _graceTimer = Timer(emptyGrace, () => unawaited(_finish()));
      return;
    }
    await _finish();
  }

  Future<void> _finish() async {
    if (_finished) return;
    _finished = true;
    _graceTimer?.cancel();
    _graceTimer = null;
    await send(buildSystemEvent(GarminSystemEventType.syncComplete));
    _report(GarminSyncPhase.complete);
    debugPrint('[GARMIN-SYNC] complete: ${_downloaded.length} files');
    if (!_done.isCompleted) _done.complete(List.unmodifiable(_downloaded));
  }

  void _fail(Object error, StackTrace stack) {
    if (_finished) return;
    _finished = true;
    _report(GarminSyncPhase.failed);
    debugPrint('[GARMIN-SYNC] failed: $error');
    if (!_done.isCompleted) _done.completeError(error, stack);
  }

  /// Ends the sync early (link dropped, user cancelled). Whatever was already
  /// downloaded is still returned — a night of sleep already on the phone should
  /// not be thrown away because the walk home ended the connection.
  void abort([Object? reason]) {
    if (_finished) return;
    _finished = true;
    _report(GarminSyncPhase.failed);
    debugPrint('[GARMIN-SYNC] aborted: ${reason ?? "no reason given"}');
    if (!_done.isCompleted) _done.complete(List.unmodifiable(_downloaded));
  }
}

/// Renders bytes as space-separated hex, capped so a stray large buffer cannot
/// flood the log.
String _hex(Uint8List bytes, {int max = 256}) {
  final shown = bytes.length > max ? Uint8List.sublistView(bytes, 0, max) : bytes;
  final text =
      shown.map((b) => b.toRadixString(16).padLeft(2, '0')).join(' ');
  return bytes.length > max ? '$text … (+${bytes.length - max}B)' : text;
}

/// One file being received: the expected size, the bytes so far, and the running
/// CRC the watch checks each chunk against.
class _ActiveDownload {
  _ActiveDownload({required this.entry});

  final GarminDirectoryEntry entry;

  int _size = 0;
  int _runningCrc = 0;

  /// Whether the watch has reported the size yet. Tracked separately from
  /// [_size] because a size of ZERO is legitimate — a watch with nothing new
  /// serves an empty directory — and keying completion off `_size > 0` left that
  /// sync waiting forever for a chunk that was never coming.
  bool _begun = false;

  final BytesBuilder _data = BytesBuilder(copy: false);

  int get received => _data.length;
  bool get isComplete => _begun && received >= _size;
  Uint8List get bytes => _data.toBytes();

  /// The watch reports the real size in the download status; the directory's
  /// size field is not authoritative.
  void begin(int size) {
    _begun = true;
    _size = size;
  }

  /// Appends a chunk after verifying its offset and running CRC. Returns false
  /// when either check fails, which the caller treats as "skip this file".
  bool append(GarminFileTransferData chunk) {
    if (chunk.dataOffset != received) return false;
    final crc = GarminCrc.compute(chunk.data, initialCrc: _runningCrc);
    if (crc != chunk.crc) return false;
    _runningCrc = crc;
    _data.add(chunk.data);
    return true;
  }
}
