import 'data_domain.dart';

/// Where a repository reports that it changed stored health data.
///
/// The write side of the refresh contract. A repository calls [changed] after a
/// successful insert/update/delete; what happens next — which screens reload —
/// is none of its business, which is why this is a bare domain interface with no
/// Riverpod, Flutter or UI type anywhere in it.
abstract interface class DataChangeSink {
  /// Reports that [domains] were written. Implementations must not throw: this
  /// is called from inside a repository's `runCatching`, where a throw would
  /// surface as a *write failure* and could drive a retry that duplicates a
  /// health record.
  void changed(Set<DataDomain> domains);
}

/// The sink for a caller that has nowhere to broadcast to.
///
/// A `const` default rather than a nullable field, so every repository call site
/// stays unconditional — the same argument [openBackgroundHealthAccess] makes
/// for its own construction: remembering to null-check does not work, and the
/// one forgotten site is the bug.
///
/// Two callers legitimately land here: a background isolate (the home-screen
/// widget's one-tap log, the reminder alarms) has no provider container at all,
/// and a repository unit test does not want one. Both are covered by the
/// app-open refresh when the user next foregrounds the app.
class NoopDataChangeSink implements DataChangeSink {
  const NoopDataChangeSink();

  @override
  void changed(Set<DataDomain> domains) {}
}
