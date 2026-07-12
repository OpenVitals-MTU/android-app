import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/mindfulness_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/mindfulness_period_data.dart';

/// Port of the Kotlin `MindfulnessRepository` contract.
///
/// Fallible operations return [Result]; the synchronous probes
/// ([mindfulnessWritePermissions], [isMindfulnessAvailable]) read cached
/// state and cannot fail, so they stay bare.
abstract interface class MindfulnessRepository {
  Set<String> get mindfulnessWritePermissions;

  Future<Result<MindfulnessPeriodData>> loadMindfulnessPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<List<MindfulnessSession>>> loadMindfulnessSessions(
    LocalDate start,
    LocalDate end,
  );

  bool isMindfulnessAvailable();

  Future<Result<bool>> hasMindfulnessWritePermission();

  Future<Result<String>> writeMindfulnessSessionEntry(
    MindfulnessSessionWriteRequest request,
  );

  Future<Result<MindfulnessSession?>> loadMindfulnessSession(String id);

  Future<Result<void>> updateMindfulnessSessionEntry(
    String id,
    MindfulnessSessionWriteRequest request,
  );

  Future<Result<void>> deleteMindfulnessSessionEntry(String id);
}
