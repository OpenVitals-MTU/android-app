import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_batch_channel.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_models.dart';

/// A batch is only identified by its first record's clientRecordId here.
List<ConvertedAppleRecord> _batch(String tag) => [
      ConvertedAppleRecord(
        appleType: 'HKQuantityTypeIdentifierStepCount',
        targetType: 'StepsRecord',
        fingerprint: tag,
        sourceTimeRange: AppleImportTimeRange(
          DateTime.utc(2026, 1, 1),
          DateTime.utc(2026, 1, 1, 1),
        ),
        record: StepsImportRecord(
          clientRecordId: tag,
          startTime: DateTime.utc(2026, 1, 1),
          startZoneOffset: null,
          endTime: DateTime.utc(2026, 1, 1, 1),
          endZoneOffset: null,
          count: 1,
        ),
      ),
    ];

String _tagOf(List<ConvertedAppleRecord> batch) => batch.first.clientRecordId!;

void main() {
  test('delivers batches to the consumer in FIFO order, then null on close',
      () async {
    final channel = AppleHealthImportBatchChannel(2);
    final received = <String>[];
    final writer = drainAppleHealthImportBatches(channel, (batch) async {
      received.add(_tagOf(batch));
    });

    channel.add(_batch('a'));
    channel.add(_batch('b'));
    channel.add(_batch('c'));
    channel.close();
    await writer;

    expect(received, ['a', 'b', 'c']);
  });

  test('the consumer waits for a batch that is added later', () async {
    final channel = AppleHealthImportBatchChannel(2);
    final received = <String>[];
    final writer = drainAppleHealthImportBatches(channel, (batch) async {
      received.add(_tagOf(batch));
    });

    await Future<void>.delayed(Duration.zero);
    expect(received, isEmpty); // nothing to consume yet

    channel.add(_batch('late'));
    await Future<void>.delayed(Duration.zero);
    channel.close();
    await writer;

    expect(received, ['late']);
  });

  test('awaitCapacity suspends the producer until the writer drains below cap',
      () async {
    final channel = AppleHealthImportBatchChannel(2);
    final gate = Completer<void>();
    final processed = <String>[];
    // A writer that blocks on the first batch until we release the gate.
    final writer = drainAppleHealthImportBatches(channel, (batch) async {
      if (_tagOf(batch) == 'a') await gate.future;
      processed.add(_tagOf(batch));
    });

    channel.add(_batch('a')); // taken by the writer, which now blocks on `gate`
    channel.add(_batch('b'));
    channel.add(_batch('c')); // buffer now holds b, c => at capacity (2)

    var capacityReturned = false;
    final backpressure =
        channel.awaitCapacity().then((_) => capacityReturned = true);

    await Future<void>.delayed(Duration.zero);
    expect(capacityReturned, isFalse, reason: 'buffer is full, must wait');

    gate.complete(); // writer processes a, then removes b -> space frees
    await backpressure;
    expect(capacityReturned, isTrue);

    channel.close();
    await writer;
    expect(processed, ['a', 'b', 'c']);
  });

  test('a writer failure releases a parked producer and re-throws on its turn',
      () async {
    final channel = AppleHealthImportBatchChannel(2);
    final boom = StateError('health connect gone');
    final writer = drainAppleHealthImportBatches(channel, (batch) async {
      throw boom;
    });
    // Attach the expectation before the writer errors, so its failure is handled
    // rather than reported as an unhandled async error.
    final writerFailed = expectLater(writer, throwsA(same(boom)));

    channel.add(_batch('a')); // the writer takes this and throws
    channel.add(_batch('b'));
    channel.add(_batch('c')); // buffer at capacity

    // Without the fail() release this await would hang forever.
    await channel.awaitCapacity();
    expect(() => channel.throwIfFailed(), throwsA(same(boom)));
    await writerFailed;
  });
}
