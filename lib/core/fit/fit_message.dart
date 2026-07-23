/// FIT epoch (1989-12-31T00:00:00Z) in Unix seconds.
const int _fitEpochUnixSeconds = 631065600;

/// A FIT timestamp — seconds since the FIT epoch — as a UTC [DateTime].
DateTime fitDateTimeInstant(int value) => DateTime.fromMillisecondsSinceEpoch(
      (_fitEpochUnixSeconds + value) * 1000,
      isUtc: true,
    );

/// Thrown when a byte stream is not a well-formed FIT file. Generic to the FIT
/// container — callers that turn it into a user-facing message catch broadly.
class FitFormatException implements Exception {
  const FitFormatException(this.message);

  final String message;

  @override
  String toString() => 'FitFormatException: $message';
}

/// One decoded FIT data message: its global number, the field maps the generic
/// [FitReader] walk extracted (keyed by field number), and the resolved record
/// timestamp in FIT-epoch seconds (null when the message carried none).
///
/// This is the seam between the generic FIT container walk and any domain
/// interpretation: the reader emits these knowing no message types, and each
/// interpreter switches on [globalMessageNumber] and reads the fields it knows.
class FitMessage {
  const FitMessage(
    this.globalMessageNumber,
    this.values,
    this.strings,
    this.arrays,
    this.timestamp,
  );

  final int globalMessageNumber;
  final Map<int, int> values;
  final Map<int, String> strings;
  final Map<int, List<int>> arrays;
  final int? timestamp;
}
