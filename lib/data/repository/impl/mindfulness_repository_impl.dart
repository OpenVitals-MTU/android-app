import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/mindfulness_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/mindfulness_period_data.dart';
import '../../../health/health_data_source.dart';
import '../contract/mindfulness_repository.dart';
import '../contract/repository_exceptions.dart';
import 'repository_time.dart';
import 'health_connect_gating.dart';

/// Port of the Kotlin `MindfulnessRepositoryImpl`.
class MindfulnessRepositoryImpl implements MindfulnessRepository {
  MindfulnessRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  /// Delegates to the permission service rather than hardcoding the string, so
  /// it is **empty** when the provider does not expose mindfulness sessions
  /// (Kotlin 1.9.0, 1f2b435). Hardcoding it meant the UI could offer to request a
  /// permission the provider does not define, which can never be granted.
  @override
  Set<String> get mindfulnessWritePermissions =>
      _dataSource.permissionService.mindfulnessWritePermissions;

  /// The read permissions, likewise empty when mindfulness is unavailable.
  Set<String> get _mindfulnessReadPermissions =>
      _dataSource.permissionService.mindfulnessPermissions;

  @override
  Future<MindfulnessPeriodData> loadMindfulnessPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final w = query.windows;
    return MindfulnessPeriodData(
      sessions: await loadMindfulnessSessions(w.current.start, w.current.end),
      previousSessions:
          await loadMindfulnessSessions(w.previous.start, w.previous.end),
      baselineSessions:
          await loadMindfulnessSessions(w.baseline.start, w.baseline.end),
    );
  }

  @override
  Future<List<MindfulnessSession>> loadMindfulnessSessions(
    LocalDate start,
    LocalDate end,
  ) async {
    // An empty permission set means the provider does not expose mindfulness at
    // all — distinct from "supported but not granted" (Kotlin 1f2b435).
    final required = _mindfulnessReadPermissions;
    if (required.isEmpty) return const [];
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.containsAll(required)) return const [];
    return _dataSource.readMindfulnessSessions(
        localDayStart(start), localDayEnd(end));
  }

  @override
  bool isMindfulnessAvailable() => _dataSource.isMindfulnessSessionAvailable();

  @override
  Future<bool> hasMindfulnessWritePermission() async {
    // The availability guard is NOT redundant: now that the permission set is
    // empty on an unsupported provider, `containsAll({})` is vacuously true and
    // this would claim we hold a write permission that does not exist. Kotlin
    // guards it the same way (`isMindfulnessAvailable() && ...`).
    if (!isMindfulnessAvailable()) return false;
    final granted = await _dataSource.grantedIfAvailable();
    return granted.containsAll(mindfulnessWritePermissions);
  }

  @override
  Future<String> writeMindfulnessSessionEntry(
    MindfulnessSessionWriteRequest request,
  ) async {
    await _requireWrite();
    return _dataSource.writeMindfulnessSessionEntry(request);
  }

  @override
  Future<MindfulnessSession?> loadMindfulnessSession(String id) async {
    // Kotlin 1f2b435 gates the single-session read the same way as the list read;
    // this had no permission check at all.
    final required = _mindfulnessReadPermissions;
    if (required.isEmpty) return null;
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.containsAll(required)) return null;
    return _dataSource.readMindfulnessSession(id);
  }

  @override
  Future<void> updateMindfulnessSessionEntry(
    String id,
    MindfulnessSessionWriteRequest request,
  ) async {
    await _requireWrite();
    await _dataSource.updateMindfulnessSessionEntry(id, request);
  }

  @override
  Future<void> deleteMindfulnessSessionEntry(String id) async {
    await _requireWrite();
    await _dataSource.deleteMindfulnessSessionEntry(id);
  }

  Future<void> _requireWrite() async {
    if (!isMindfulnessAvailable()) {
      throw const MissingHealthPermissionException(
        'Mindfulness sessions are not available on this platform.',
      );
    }
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.containsAll(mindfulnessWritePermissions)) {
      throw const MissingHealthPermissionException(
        'Missing Health Connect mindfulness write permission.',
      );
    }
  }
}
