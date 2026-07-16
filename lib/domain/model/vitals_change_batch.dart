import '../../core/time/local_date.dart';

/// One page of Health Connect changes for a vitals record type, from a changes
/// token — the input to the local daily-aggregate cache's incremental sync.
///
/// [upsertedDays] are the local days that had records inserted or updated (the
/// cache recomputes just those). [hasDeletions] is true if any record was
/// deleted; deletions carry only an id, not a date, so the cache full-rebuilds
/// that metric when this is set. Continue paging with [nextToken] while
/// [hasMore]; [tokenExpired] means the token is stale — start over from a fresh
/// token and a full read.
class VitalsChangeBatch {
  const VitalsChangeBatch({
    required this.upsertedDays,
    required this.hasDeletions,
    required this.nextToken,
    required this.tokenExpired,
    required this.hasMore,
  });

  final List<LocalDate> upsertedDays;
  final bool hasDeletions;
  final String nextToken;
  final bool tokenExpired;
  final bool hasMore;
}
