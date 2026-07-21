/// Outcome + progress models for a sync session, mirroring the Apple Health
/// import's counters so the report screen can render them the same way.
library;

/// Live progress emitted during a session (drives the progress screen).
class SyncProgress {
  const SyncProgress({
    required this.phase,
    this.itemsSent = 0,
    this.itemsReceived = 0,
    this.itemsWritten = 0,
  });

  final SyncPhase phase;

  /// Our records handed to the peer so far.
  final int itemsSent;

  /// Peer records received so far.
  final int itemsReceived;

  /// Peer records actually written to Health Connect so far (post-dedup).
  final int itemsWritten;

  SyncProgress copyWith({
    SyncPhase? phase,
    int? itemsSent,
    int? itemsReceived,
    int? itemsWritten,
  }) =>
      SyncProgress(
        phase: phase ?? this.phase,
        itemsSent: itemsSent ?? this.itemsSent,
        itemsReceived: itemsReceived ?? this.itemsReceived,
        itemsWritten: itemsWritten ?? this.itemsWritten,
      );
}

/// Coarse phase of a session, for the progress label.
enum SyncPhase {
  handshake,
  authenticating,
  exchanging,
  writing,
  complete,
  aborted,
}

/// Per-record-type tally in the final report.
class SyncTypeSummary {
  const SyncTypeSummary({
    required this.recordType,
    this.received = 0,
    this.imported = 0,
    this.duplicateSkipped = 0,
  });

  final String recordType;
  final int received;
  final int imported;
  final int duplicateSkipped;

  SyncTypeSummary _add({
    int received = 0,
    int imported = 0,
    int duplicateSkipped = 0,
  }) =>
      SyncTypeSummary(
        recordType: recordType,
        received: this.received + received,
        imported: this.imported + imported,
        duplicateSkipped: this.duplicateSkipped + duplicateSkipped,
      );
}

/// The final result of a session, shown on the report screen. Each device
/// reports what IT wrote (bidirectional merge means both sides have one).
class SyncReport {
  const SyncReport({
    required this.completed,
    required this.peerDeviceName,
    required this.negotiatedTypes,
    required this.itemsSent,
    required this.itemsReceived,
    required this.imported,
    required this.duplicateSkipped,
    required this.typeSummaries,
    this.abortReason,
  });

  /// True if the session finished cleanly; false if it aborted.
  final bool completed;
  final String peerDeviceName;
  final List<String> negotiatedTypes;

  /// Our records sent to the peer.
  final int itemsSent;

  /// Peer records received.
  final int itemsReceived;

  /// Peer records written to Health Connect (new).
  final int imported;

  /// Peer records skipped because Health Connect already had them.
  final int duplicateSkipped;

  final List<SyncTypeSummary> typeSummaries;

  /// Set when [completed] is false — why the session ended early.
  final String? abortReason;
}

/// Accumulates per-type tallies during a run and folds them into a [SyncReport].
class SyncReportBuilder {
  final Map<String, SyncTypeSummary> _byType = {};
  int itemsSent = 0;
  int itemsReceived = 0;
  int imported = 0;
  int duplicateSkipped = 0;

  /// Records one received item. [imported] is true only when it was actually
  /// written to Health Connect (a fresh item whose write succeeded); [duplicate]
  /// is true when it was skipped as already present. A fresh item whose write
  /// FAILED is neither — counted as received but not imported, so the report can
  /// no longer claim "imported N" when nothing landed.
  void recordReceived(
    String recordType, {
    bool imported = false,
    bool duplicate = false,
  }) {
    itemsReceived += 1;
    if (imported) this.imported += 1;
    if (duplicate) duplicateSkipped += 1;
    final current =
        _byType[recordType] ?? SyncTypeSummary(recordType: recordType);
    _byType[recordType] = current._add(
      received: 1,
      imported: imported ? 1 : 0,
      duplicateSkipped: duplicate ? 1 : 0,
    );
  }

  SyncReport build({
    required bool completed,
    required String peerDeviceName,
    required List<String> negotiatedTypes,
    String? abortReason,
  }) {
    final summaries = _byType.values.toList()
      ..sort((a, b) => a.recordType.compareTo(b.recordType));
    return SyncReport(
      completed: completed,
      peerDeviceName: peerDeviceName,
      negotiatedTypes: negotiatedTypes,
      itemsSent: itemsSent,
      itemsReceived: itemsReceived,
      imported: imported,
      duplicateSkipped: duplicateSkipped,
      typeSummaries: summaries,
      abortReason: abortReason,
    );
  }
}
