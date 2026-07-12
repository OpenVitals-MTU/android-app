import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/mindfulness_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/mindfulness_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../contract/mindfulness_repository.dart';
import '../contract/repository_exceptions.dart';
import 'repository_time.dart';
import 'health_connect_gating.dart';
import 'run_catching.dart';

/// Port of the Kotlin `MindfulnessRepositoryImpl`.
///
/// Public methods convert exceptions to failures via [runCatching] at the
/// boundary; the private `_raw` bodies keep the original throwing flow so
/// internal composition stays plain awaits.
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
  Future<Result<MindfulnessPeriodData>> loadMindfulnessPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(() async {
        final w = query.windows;
        return MindfulnessPeriodData(
          sessions: await _loadSessionsRaw(w.current.start, w.current.end),
          previousSessions:
              await _loadSessionsRaw(w.previous.start, w.previous.end),
          baselineSessions:
              await _loadSessionsRaw(w.baseline.start, w.baseline.end),
        );
      });

  @override
  Future<Result<List<MindfulnessSession>>> loadMindfulnessSessions(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() => _loadSessionsRaw(start, end));

  Future<List<MindfulnessSession>> _loadSessionsRaw(
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
  Future<Result<bool>> hasMindfulnessWritePermission() =>
      runCatching(() async {
        // The availability guard is NOT redundant: now that the permission set
        // is empty on an unsupported provider, `containsAll({})` is vacuously
        // true and this would claim we hold a write permission that does not
        // exist. Kotlin guards it the same way (`isMindfulnessAvailable() && ...`).
        if (!isMindfulnessAvailable()) return false;
        final granted = await _dataSource.grantedIfAvailable();
        return granted.containsAll(mindfulnessWritePermissions);
      });

  @override
  Future<Result<String>> writeMindfulnessSessionEntry(
    MindfulnessSessionWriteRequest request,
  ) =>
      runCatching(() async {
        await _requireWrite();
        return _dataSource.writeMindfulnessSessionEntry(request);
      });

  @override
  Future<Result<MindfulnessSession?>> loadMindfulnessSession(String id) =>
      runCatching(() async {
        // Kotlin 1f2b435 gates the single-session read the same way as the list
        // read; this had no permission check at all.
        final required = _mindfulnessReadPermissions;
        if (required.isEmpty) return null;
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.containsAll(required)) return null;
        return _dataSource.readMindfulnessSession(id);
      });

  @override
  Future<Result<void>> updateMindfulnessSessionEntry(
    String id,
    MindfulnessSessionWriteRequest request,
  ) =>
      runCatching(() async {
        await _requireWrite();
        await _dataSource.updateMindfulnessSessionEntry(id, request);
      });

  @override
  Future<Result<void>> deleteMindfulnessSessionEntry(String id) =>
      runCatching(() async {
        await _requireWrite();
        await _dataSource.deleteMindfulnessSessionEntry(id);
      });

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
