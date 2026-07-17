import 'dart:async';
import 'dart:collection';

import 'package:flutter/foundation.dart';

import 'apple_health_import_models.dart';

/// A bounded, single-consumer async hand-off from the parse/convert producer to
/// the writer — the Dart analogue of Kotlin's `Channel(capacity)` in
/// `AppleHealthImportService`. It is what lets parsing a chunk overlap writing
/// the previous chunk's batches instead of the two running strictly in turn.
///
/// FIFO, and the consumer pulls one batch at a time, so the writer keeps its
/// batch-N-before-batch-N+1 ordering (which is what preserves cross-batch import
/// dedup). Two differences from a Kotlin channel, both because a Dart isolate has
/// no thread the producer can block:
///
///  * [add] never suspends. The SAX-style convert loop that feeds it cannot
///    `await` between records, so a burst inside one parse chunk may briefly hold
///    more than [capacity] batches.
///  * The bound is enforced at chunk boundaries instead: the producer calls
///    [awaitCapacity] there, which suspends until the writer has drained back
///    under [capacity]. That await is also where the writer's I/O overlaps the
///    next chunk's parse.
class AppleHealthImportBatchChannel {
  AppleHealthImportBatchChannel(this.capacity)
      : assert(capacity > 0, 'capacity must be positive');

  final int capacity;
  final Queue<List<ConvertedAppleRecord>> _items = Queue();
  Completer<void>? _itemAdded; // the consumer waits on this when empty
  Completer<void>? _itemRemoved; // the producer waits on this when full
  bool _closed = false;
  Object? _failure;
  StackTrace? _failureStack;

  /// Enqueue a batch (producer side). Never suspends.
  void add(List<ConvertedAppleRecord> batch) {
    _items.add(batch);
    _complete(_itemAdded);
    _itemAdded = null;
  }

  /// No more batches will be added; the consumer's [next] returns null once the
  /// queue drains.
  void close() {
    _closed = true;
    _complete(_itemAdded);
    _itemAdded = null;
  }

  /// Records a writer failure and wakes a producer parked in [awaitCapacity] so
  /// it cannot deadlock waiting for a consumer that has died. The error is then
  /// re-surfaced to the producer through [throwIfFailed].
  void fail(Object error, StackTrace stackTrace) {
    _failure ??= error;
    _failureStack ??= stackTrace;
    _complete(_itemRemoved);
    _itemRemoved = null;
    _complete(_itemAdded);
    _itemAdded = null;
  }

  /// The writer's pull. Suspends while empty; returns null once [close]d and
  /// drained.
  Future<List<ConvertedAppleRecord>?> next() async {
    while (_items.isEmpty) {
      if (_closed || _failure != null) return null;
      final completer = _itemAdded ??= Completer<void>();
      await completer.future;
    }
    final batch = _items.removeFirst();
    _complete(_itemRemoved);
    _itemRemoved = null;
    return batch;
  }

  /// Producer backpressure: suspend until fewer than [capacity] batches are
  /// queued (or the writer has failed, so we don't wait forever).
  Future<void> awaitCapacity() async {
    while (_items.length >= capacity && _failure == null) {
      final completer = _itemRemoved ??= Completer<void>();
      await completer.future;
    }
  }

  /// Re-throws the writer's failure on the producer's turn, so a dead writer
  /// stops the parse promptly instead of letting it run the whole export first.
  void throwIfFailed() {
    final failure = _failure;
    if (failure != null) {
      Error.throwWithStackTrace(failure, _failureStack ?? StackTrace.current);
    }
  }

  @visibleForTesting
  int get bufferedCount => _items.length;

  static void _complete(Completer<void>? completer) {
    if (completer != null && !completer.isCompleted) completer.complete();
  }
}

/// Drains [channel] into [process], one batch at a time in order. Returns when
/// the channel is closed and empty; on a [process] failure it records the error
/// on the channel (to release a parked producer) and rethrows so the caller can
/// surface it.
Future<void> drainAppleHealthImportBatches(
  AppleHealthImportBatchChannel channel,
  Future<void> Function(List<ConvertedAppleRecord> batch) process,
) async {
  try {
    while (true) {
      final batch = await channel.next();
      if (batch == null) return;
      await process(batch);
    }
  } catch (error, stackTrace) {
    channel.fail(error, stackTrace);
    rethrow;
  }
}
