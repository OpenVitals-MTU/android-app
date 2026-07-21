/// The sync session state machine: handshake → authenticate → bidirectional
/// record exchange → report. Pure Dart over a [SyncByteTransport], so the whole
/// protocol is testable over an in-memory pipe with no Bluetooth.
///
/// The session is SYMMETRIC — both phones run the same code, differing only in
/// [SyncRole] (which fixes the nonce order for the shared key). Records flow both
/// ways over the one socket at once: a sender loop pushes our records and waits
/// for per-batch acks (stop-and-wait backpressure), while a receiver loop dedups
/// and writes the peer's records and acks them. Inbound frames are demultiplexed
/// by type to whichever loop is waiting.
library;

import 'dart:async';
import 'dart:math';
import 'dart:typed_data';

import 'sync_frame.dart';
import 'sync_messages.dart';
import 'sync_pairing.dart';
import 'sync_report.dart';
import 'sync_transport.dart';

/// Which side of the pairing this phone is. Fixes the (host, guest) nonce order
/// so both derive the same session key.
enum SyncRole { host, guest }

/// Thrown when a session ends early — bad protocol version, failed auth, a
/// dropped link, or a peer/local abort.
class SyncAborted implements Exception {
  const SyncAborted(this.reason);
  final String reason;
  @override
  String toString() => 'SyncAborted: $reason';
}

/// The Health Connect side of a session, injected so the protocol stays free of
/// health dependencies (and testable with an in-memory fake). Phase 3/4 provides
/// the real implementation over `HealthDataSource`.
abstract interface class SyncRecordStore {
  /// Reads this phone's records for the negotiated [types] in the session's
  /// window, each already carrying its dedup [SyncItem.key]. These same keys are
  /// the session's dedup baseline: an incoming peer record is a duplicate iff we
  /// already hold a record with the same content fingerprint, i.e. its key is
  /// among the ones this returns.
  Future<List<SyncItem>> readItems(Set<String> types);

  /// Writes [items] to Health Connect (post-dedup).
  Future<void> writeItems(List<SyncItem> items);
}

/// Static configuration for a session.
class SyncSessionConfig {
  const SyncSessionConfig({
    required this.role,
    required this.code,
    required this.deviceName,
    required this.supportedTypes,
    this.selectedTypes,
    this.hcProviderVersion,
    this.batchSize = 200,
    this.handshakeTimeout = const Duration(seconds: 30),
    this.batchTimeout = const Duration(seconds: 60),
    this.nonce,
  });

  final SyncRole role;

  /// The 6-digit pairing code (host shows it, guest types it). Both feed it in.
  final String code;
  final String deviceName;

  /// Record types this device + its HC provider support.
  final List<String> supportedTypes;

  /// Types the user chose to sync; defaults to all [supportedTypes].
  final List<String>? selectedTypes;

  final int? hcProviderVersion;

  /// Records per batch (stop-and-wait unit).
  final int batchSize;

  final Duration handshakeTimeout;
  final Duration batchTimeout;

  /// Pre-seeded nonce for deterministic tests; production generates one.
  final Uint8List? nonce;
}

/// Drives one sync session to completion. Create per session; not reusable.
class SyncSession {
  SyncSession({
    required SyncByteTransport transport,
    required SyncRecordStore store,
    required this.config,
    Random? random,
  })  : _transport = transport, // ignore: prefer_initializing_formals
        _store = store, // ignore: prefer_initializing_formals
        _nonce = config.nonce ?? generateSyncNonce(random);

  final SyncByteTransport _transport;
  final SyncRecordStore _store;
  final SyncSessionConfig config;
  final Uint8List _nonce;

  final SyncReportBuilder _report = SyncReportBuilder();
  final Set<String> _seenKeys = {};

  final _progressController = StreamController<SyncProgress>.broadcast();
  SyncProgress _progress = const SyncProgress(phase: SyncPhase.handshake);

  /// Live progress for the UI.
  Stream<SyncProgress> get progress => _progressController.stream;

  // Handshake completers + exchange plumbing, driven by [_dispatch].
  final Completer<SyncHello> _peerHello = Completer<SyncHello>();
  final Completer<SyncAuthProof> _peerAuth = Completer<SyncAuthProof>();
  final _incomingBatches = StreamController<SyncBatch>();
  Completer<int>? _pendingAck;
  SyncHello? _receivedHello;
  bool _exchangeStarted = false;

  StreamSubscription<Uint8List>? _sub;
  final SyncFrameReader _reader = SyncFrameReader();
  bool _abortSent = false;
  String? _abortReason;

  /// Runs the session and resolves with the report (both success and clean
  /// abort produce a report; a hard failure throws [SyncAborted]).
  Future<SyncReport> run() async {
    _sub = _transport.inbound.listen(
      _onChunk,
      onError: (Object e) => _failPending('link error: $e'),
      onDone: () => _failPending('connection lost'),
    );
    // A link drop can complete these with an error during the brief gap between
    // a send and the matching await (see _failPending). A sacrificial handler
    // keeps that from surfacing as an unhandled async error; the real awaits in
    // _handshake/_authenticate still receive and throw the error.
    _ignoreErrors(_peerHello.future);
    _ignoreErrors(_peerAuth.future);
    try {
      final peerHello = await _handshake();
      await _authenticate(peerHello);
      final negotiated = _negotiateTypes(peerHello);
      await _exchange(negotiated);
      _emit(phase: SyncPhase.complete);
      return _report.build(
        completed: true,
        peerDeviceName: peerHello.deviceName,
        negotiatedTypes: negotiated,
      );
    } on SyncAborted catch (e) {
      await _sendAbort(e.reason);
      _emit(phase: SyncPhase.aborted);
      return _report.build(
        completed: false,
        peerDeviceName: _receivedHello?.deviceName ?? 'unknown',
        negotiatedTypes: const [],
        abortReason: e.reason,
      );
    } finally {
      await _cleanup();
    }
  }

  // --- Phases -----------------------------------------------------------------

  Future<SyncHello> _handshake() async {
    _emit(phase: SyncPhase.handshake);
    // Attach the receive handler BEFORE sending, so a link drop between the two
    // can't complete _peerHello with an error that has no listener yet (which
    // Dart would report as an unhandled async error).
    final peerFuture = _await(_peerHello.future, config.handshakeTimeout,
        'timed out waiting for peer hello');
    await _send(
      SyncFrameType.hello,
      SyncHello(
        protocolVersion: kSyncProtocolVersion,
        deviceName: config.deviceName,
        hcProviderVersion: config.hcProviderVersion,
        supportedTypes: config.supportedTypes,
        nonce: _nonce,
      ).encode(),
    );
    final peer = await peerFuture;
    if (peer.protocolVersion != kSyncProtocolVersion) {
      throw SyncAborted(
        'incompatible protocol version ${peer.protocolVersion} '
        '(this app speaks $kSyncProtocolVersion)',
      );
    }
    return peer;
  }

  Future<void> _authenticate(SyncHello peer) async {
    _emit(phase: SyncPhase.authenticating);
    // Fix nonce order by role so both phones derive the same key.
    final hostNonce = config.role == SyncRole.host ? _nonce : peer.nonce;
    final guestNonce = config.role == SyncRole.host ? peer.nonce : _nonce;
    final sessionKey = deriveSessionKey(
      code: config.code,
      hostNonce: hostNonce,
      guestNonce: guestNonce,
    );
    // Prove over the peer's nonce; verify their proof over ours.
    final myProof =
        computeAuthProof(sessionKey: sessionKey, challengeNonce: peer.nonce);
    final proofFuture = _await(
        _peerAuth.future, config.handshakeTimeout, 'timed out waiting for auth');
    await _send(SyncFrameType.auth, SyncAuthProof(myProof).encode());
    final peerProof = await proofFuture;
    final expected =
        computeAuthProof(sessionKey: sessionKey, challengeNonce: _nonce);
    if (!constantTimeEquals(peerProof.proof, expected)) {
      throw const SyncAborted('pairing code did not match');
    }
  }

  List<String> _negotiateTypes(SyncHello peer) {
    final peerTypes = peer.supportedTypes.toSet();
    final selected = (config.selectedTypes ?? config.supportedTypes).toSet();
    // Order-stable intersection: keep this phone's declared order.
    return config.supportedTypes
        .where((t) => peerTypes.contains(t) && selected.contains(t))
        .toList();
  }

  Future<void> _exchange(List<String> negotiated) async {
    _exchangeStarted = true;
    _emit(phase: SyncPhase.exchanging);
    final localItems = await _store.readItems(negotiated.toSet());
    _report.itemsSent = localItems.length;
    // Seed the dedup baseline from the records we just read. A peer record is a
    // duplicate iff we already hold one with the same content fingerprint, so our
    // own item keys ARE that set — dedup is then an in-memory lookup. This
    // replaces a per-batch Health Connect re-read that paged the whole window
    // every batch (O(batches x history) — the on-device "stuck at ~1 batch / 17s"
    // crawl). It's also stricter: it catches records we hold natively (no
    // clientRecordId), which a clientRecordId-only check would miss and re-import.
    for (final item in localItems) {
      _seenKeys.add(item.key);
    }
    // Sender and receiver run concurrently over the one full-duplex link.
    await Future.wait([_runSender(localItems), _runReceiver()]);
  }

  Future<void> _runSender(List<SyncItem> items) async {
    var seq = 0;
    var sent = 0;
    for (final chunk in _chunk(items, config.batchSize)) {
      seq += 1;
      final ack = Completer<int>();
      _pendingAck = ack;
      _ignoreErrors(ack.future);
      final ackFuture =
          _await(ack.future, config.batchTimeout, 'timed out waiting for ack');
      await _send(SyncFrameType.batch, SyncBatch(seq: seq, items: chunk).encode());
      await ackFuture;
      sent += chunk.length;
      _emit(itemsSent: sent);
    }
    await _send(SyncFrameType.sendDone, Uint8List(0));
  }

  Future<void> _runReceiver() async {
    var received = 0;
    var written = 0;
    await for (final batch in _incomingBatches.stream) {
      final fresh = <SyncItem>[];
      for (final item in batch.items) {
        // _seenKeys holds every record we already had (seeded from readItems)
        // plus everything written earlier this session, so this one lookup covers
        // both cross-device and within-session dedup.
        final duplicate = _seenKeys.contains(item.key);
        _report.recordReceived(item.recordType, wasDuplicate: duplicate);
        if (!duplicate) {
          fresh.add(item);
          _seenKeys.add(item.key);
        }
      }
      if (fresh.isNotEmpty) {
        _emit(phase: SyncPhase.writing);
        await _store.writeItems(fresh);
      }
      received += batch.items.length;
      written += fresh.length;
      _emit(
        phase: SyncPhase.exchanging,
        itemsReceived: received,
        itemsWritten: written,
      );
      await _send(SyncFrameType.batchAck, SyncBatchAck(batch.seq).encode());
    }
  }

  // --- Frame dispatch ---------------------------------------------------------

  void _onChunk(Uint8List chunk) {
    final List<SyncFrame> frames;
    try {
      frames = _reader.addChunk(chunk);
    } on SyncFrameFormatException catch (e) {
      _failPending('malformed frame: ${e.message}');
      return;
    }
    for (final frame in frames) {
      _dispatch(frame);
    }
  }

  void _dispatch(SyncFrame frame) {
    try {
      switch (frame.type) {
        case SyncFrameType.hello:
          if (!_peerHello.isCompleted) {
            final hello = SyncHello.decode(frame.payload);
            _receivedHello = hello;
            _peerHello.complete(hello);
          }
        case SyncFrameType.auth:
          if (!_peerAuth.isCompleted) {
            _peerAuth.complete(SyncAuthProof.decode(frame.payload));
          }
        case SyncFrameType.batch:
          if (!_incomingBatches.isClosed) {
            _incomingBatches.add(SyncBatch.decode(frame.payload));
          }
        case SyncFrameType.batchAck:
          final ack = _pendingAck;
          _pendingAck = null;
          ack?.complete(SyncBatchAck.decode(frame.payload).seq);
        case SyncFrameType.sendDone:
          if (!_incomingBatches.isClosed) _incomingBatches.close();
        case SyncFrameType.abort:
          _handleAbort(SyncAbort.decode(frame.payload).reason);
      }
    } on FormatException catch (e) {
      _failPending('malformed ${frame.type.name} payload: $e');
    }
  }

  void _handleAbort(String reason) {
    _abortReason ??= reason;
    _failPending('peer aborted: $reason');
  }

  /// Propagates a fatal condition to whichever awaiter is live so [run] unwinds.
  void _failPending(String reason) {
    final error = SyncAborted(reason);
    if (!_peerHello.isCompleted) _peerHello.completeError(error);
    if (!_peerAuth.isCompleted) _peerAuth.completeError(error);
    final ack = _pendingAck;
    _pendingAck = null;
    if (ack != null && !ack.isCompleted) ack.completeError(error);
    // Only surface the error on the batch stream once the receiver loop exists
    // to consume it; pre-exchange there is no listener and the completers above
    // are what unblock the handshake/auth awaits.
    if (_exchangeStarted && !_incomingBatches.isClosed) {
      _incomingBatches.addError(error);
    }
  }

  // --- Helpers ----------------------------------------------------------------

  Future<void> _send(SyncFrameType type, Uint8List payload) =>
      _transport.send(SyncFrame(type, payload).encode());

  Future<void> _sendAbort(String reason) async {
    if (_abortSent) return;
    _abortSent = true;
    try {
      await _send(SyncFrameType.abort, SyncAbort(reason).encode());
    } catch (_) {
      // Link may already be gone; nothing more to do.
    }
  }

  Future<T> _await<T>(Future<T> future, Duration timeout, String message) =>
      future.timeout(timeout, onTimeout: () => throw SyncAborted(message));

  /// Registers a no-op error handler so a completer that errors during an
  /// await-send gap is not reported as an unhandled async error. The primary
  /// `await` on the same future still receives and throws the error.
  static void _ignoreErrors<T>(Future<T> future) {
    future.then((_) {}, onError: (_) {});
  }

  void _emit({
    SyncPhase? phase,
    int? itemsSent,
    int? itemsReceived,
    int? itemsWritten,
  }) {
    _progress = _progress.copyWith(
      phase: phase,
      itemsSent: itemsSent,
      itemsReceived: itemsReceived,
      itemsWritten: itemsWritten,
    );
    if (!_progressController.isClosed) _progressController.add(_progress);
  }

  Future<void> _cleanup() async {
    await _sub?.cancel();
    if (!_incomingBatches.isClosed) {
      // A never-listened single-subscription controller's close() future only
      // completes once someone listens — which never happens on the abort path
      // (exchange never started). Fire it without awaiting so cleanup can't hang.
      unawaited(_incomingBatches.close());
    }
    if (!_progressController.isClosed) await _progressController.close();
  }

  static Iterable<List<T>> _chunk<T>(List<T> items, int size) sync* {
    for (var i = 0; i < items.length; i += size) {
      yield items.sublist(i, min(i + size, items.length));
    }
  }
}
