import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/mindfulness_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/mindfulness_period_data.dart';

/// Port of the Kotlin `MindfulnessRepository` contract.
abstract interface class MindfulnessRepository {
  Set<String> get mindfulnessWritePermissions;

  Future<MindfulnessPeriodData> loadMindfulnessPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<List<MindfulnessSession>> loadMindfulnessSessions(
    LocalDate start,
    LocalDate end,
  );

  bool isMindfulnessAvailable();

  Future<bool> hasMindfulnessWritePermission();

  Future<String> writeMindfulnessSessionEntry(
    MindfulnessSessionWriteRequest request,
  );

  Future<MindfulnessSession?> loadMindfulnessSession(String id);

  Future<void> updateMindfulnessSessionEntry(
    String id,
    MindfulnessSessionWriteRequest request,
  );

  Future<void> deleteMindfulnessSessionEntry(String id);
}
