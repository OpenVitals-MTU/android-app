import '../../core/period/period_load_query.dart';
import '../../core/result/result.dart';
import '../../data/repository/contract/heart_repository.dart';
import '../../data/repository/contract/vitals_repository.dart';
import '../model/heart_models.dart';
import '../model/refresh_mode.dart';
import '../model/vitals_models.dart';
import '../query/heart_period_data.dart';
import '../query/vitals_period_data.dart';

/// What a heart-period load should populate. Port of the Kotlin sealed
/// `HeartPeriodLoadRequest`.
sealed class HeartPeriodLoadRequest {
  const HeartPeriodLoadRequest();
}

class HeartPeriodLoadCombined extends HeartPeriodLoadRequest {
  const HeartPeriodLoadCombined();
}

class HeartPeriodLoadHeartOnly extends HeartPeriodLoadRequest {
  const HeartPeriodLoadHeartOnly(this.metric);

  final HeartPeriodMetric metric;
}

class HeartPeriodLoadVitalsOnly extends HeartPeriodLoadRequest {
  const HeartPeriodLoadVitalsOnly(this.metric);

  final VitalsPeriodMetric metric;
}

/// Combined heart + vitals period result. Port of the Kotlin
/// `HeartPeriodLoadResult`.
class HeartPeriodLoadResult {
  const HeartPeriodLoadResult({
    this.daySamples = const [],
    this.previousDaySamples = const [],
    this.dailySummaries = const [],
    this.previousDailySummaries = const [],
    this.baselineDailySummaries = const [],
    this.dayRestingSamples = const [],
    this.dayRestingBpm,
    this.previousDayRestingBpm,
    this.dayHrvSamples = const [],
    this.dayHrvMs,
    this.previousDayHrvMs,
    this.dailyRestingHR = const [],
    this.previousDailyRestingHR = const [],
    this.baselineDailyRestingHR = const [],
    this.dailyHrv = const [],
    this.previousDailyHrv = const [],
    this.baselineDailyHrv = const [],
    this.missingVitalsPermissions = const {},
    this.bloodPressure = const [],
    this.previousBloodPressure = const [],
    this.baselineBloodPressure = const [],
    this.spO2 = const [],
    this.previousSpO2 = const [],
    this.baselineSpO2 = const [],
    this.respiratoryRate = const [],
    this.previousRespiratoryRate = const [],
    this.baselineRespiratoryRate = const [],
    this.bodyTemperature = const [],
    this.previousBodyTemperature = const [],
    this.baselineBodyTemperature = const [],
    this.vo2Max = const [],
    this.previousVo2Max = const [],
    this.baselineVo2Max = const [],
    this.bloodGlucose = const [],
    this.previousBloodGlucose = const [],
    this.baselineBloodGlucose = const [],
    this.skinTemperature = const [],
    this.previousSkinTemperature = const [],
    this.baselineSkinTemperature = const [],
  });

  final List<HeartRateSample> daySamples;
  final List<HeartRateSample> previousDaySamples;
  final List<HeartRateSummary> dailySummaries;
  final List<HeartRateSummary> previousDailySummaries;
  final List<HeartRateSummary> baselineDailySummaries;
  final List<RestingHeartRateSample> dayRestingSamples;
  final int? dayRestingBpm;
  final int? previousDayRestingBpm;
  final List<HrvSample> dayHrvSamples;
  final double? dayHrvMs;
  final double? previousDayHrvMs;
  final List<DailyRestingHR> dailyRestingHR;
  final List<DailyRestingHR> previousDailyRestingHR;
  final List<DailyRestingHR> baselineDailyRestingHR;
  final List<DailyHrv> dailyHrv;
  final List<DailyHrv> previousDailyHrv;
  final List<DailyHrv> baselineDailyHrv;
  final Set<String> missingVitalsPermissions;
  final List<BloodPressureEntry> bloodPressure;
  final List<BloodPressureEntry> previousBloodPressure;
  final List<BloodPressureEntry> baselineBloodPressure;
  final List<SpO2Entry> spO2;
  final List<SpO2Entry> previousSpO2;
  final List<SpO2Entry> baselineSpO2;
  final List<RespiratoryRateEntry> respiratoryRate;
  final List<RespiratoryRateEntry> previousRespiratoryRate;
  final List<RespiratoryRateEntry> baselineRespiratoryRate;
  final List<BodyTempEntry> bodyTemperature;
  final List<BodyTempEntry> previousBodyTemperature;
  final List<BodyTempEntry> baselineBodyTemperature;
  final List<Vo2MaxEntry> vo2Max;
  final List<Vo2MaxEntry> previousVo2Max;
  final List<Vo2MaxEntry> baselineVo2Max;
  final List<BloodGlucoseEntry> bloodGlucose;
  final List<BloodGlucoseEntry> previousBloodGlucose;
  final List<BloodGlucoseEntry> baselineBloodGlucose;
  final List<SkinTemperatureEntry> skinTemperature;
  final List<SkinTemperatureEntry> previousSkinTemperature;
  final List<SkinTemperatureEntry> baselineSkinTemperature;

  HeartPeriodLoadResult merge(HeartPeriodLoadResult other) =>
      HeartPeriodLoadResult(
        daySamples: [...daySamples, ...other.daySamples],
        previousDaySamples: [...previousDaySamples, ...other.previousDaySamples],
        dailySummaries: [...dailySummaries, ...other.dailySummaries],
        previousDailySummaries: [
          ...previousDailySummaries,
          ...other.previousDailySummaries,
        ],
        baselineDailySummaries: [
          ...baselineDailySummaries,
          ...other.baselineDailySummaries,
        ],
        dayRestingSamples: [...dayRestingSamples, ...other.dayRestingSamples],
        dayRestingBpm: dayRestingBpm ?? other.dayRestingBpm,
        previousDayRestingBpm: previousDayRestingBpm ?? other.previousDayRestingBpm,
        dayHrvSamples: [...dayHrvSamples, ...other.dayHrvSamples],
        dayHrvMs: dayHrvMs ?? other.dayHrvMs,
        previousDayHrvMs: previousDayHrvMs ?? other.previousDayHrvMs,
        dailyRestingHR: [...dailyRestingHR, ...other.dailyRestingHR],
        previousDailyRestingHR: [
          ...previousDailyRestingHR,
          ...other.previousDailyRestingHR,
        ],
        baselineDailyRestingHR: [
          ...baselineDailyRestingHR,
          ...other.baselineDailyRestingHR,
        ],
        dailyHrv: [...dailyHrv, ...other.dailyHrv],
        previousDailyHrv: [...previousDailyHrv, ...other.previousDailyHrv],
        baselineDailyHrv: [...baselineDailyHrv, ...other.baselineDailyHrv],
        missingVitalsPermissions: {
          ...missingVitalsPermissions,
          ...other.missingVitalsPermissions,
        },
        bloodPressure: [...bloodPressure, ...other.bloodPressure],
        previousBloodPressure: [
          ...previousBloodPressure,
          ...other.previousBloodPressure,
        ],
        baselineBloodPressure: [
          ...baselineBloodPressure,
          ...other.baselineBloodPressure,
        ],
        spO2: [...spO2, ...other.spO2],
        previousSpO2: [...previousSpO2, ...other.previousSpO2],
        baselineSpO2: [...baselineSpO2, ...other.baselineSpO2],
        respiratoryRate: [...respiratoryRate, ...other.respiratoryRate],
        previousRespiratoryRate: [
          ...previousRespiratoryRate,
          ...other.previousRespiratoryRate,
        ],
        baselineRespiratoryRate: [
          ...baselineRespiratoryRate,
          ...other.baselineRespiratoryRate,
        ],
        bodyTemperature: [...bodyTemperature, ...other.bodyTemperature],
        previousBodyTemperature: [
          ...previousBodyTemperature,
          ...other.previousBodyTemperature,
        ],
        baselineBodyTemperature: [
          ...baselineBodyTemperature,
          ...other.baselineBodyTemperature,
        ],
        vo2Max: [...vo2Max, ...other.vo2Max],
        previousVo2Max: [...previousVo2Max, ...other.previousVo2Max],
        baselineVo2Max: [...baselineVo2Max, ...other.baselineVo2Max],
        bloodGlucose: [...bloodGlucose, ...other.bloodGlucose],
        previousBloodGlucose: [
          ...previousBloodGlucose,
          ...other.previousBloodGlucose,
        ],
        baselineBloodGlucose: [
          ...baselineBloodGlucose,
          ...other.baselineBloodGlucose,
        ],
        skinTemperature: [...skinTemperature, ...other.skinTemperature],
        previousSkinTemperature: [
          ...previousSkinTemperature,
          ...other.previousSkinTemperature,
        ],
        baselineSkinTemperature: [
          ...baselineSkinTemperature,
          ...other.baselineSkinTemperature,
        ],
      );
}

/// Latest-vitals summary, port of the Kotlin `HeartVitalsSummary` + the
/// `vitalsSummary()` extension.
class HeartVitalsSummary {
  const HeartVitalsSummary({
    required this.hasVitalsData,
    required this.latestBloodPressure,
    required this.latestSpO2,
    required this.latestRespiratoryRate,
    required this.latestBodyTemperature,
    required this.latestVo2Max,
    required this.latestBloodGlucose,
    required this.latestSkinTemperature,
  });

  final bool hasVitalsData;
  final BloodPressureEntry? latestBloodPressure;
  final SpO2Entry? latestSpO2;
  final RespiratoryRateEntry? latestRespiratoryRate;
  final BodyTempEntry? latestBodyTemperature;
  final Vo2MaxEntry? latestVo2Max;
  final BloodGlucoseEntry? latestBloodGlucose;
  final SkinTemperatureEntry? latestSkinTemperature;
}

extension HeartPeriodLoadResultVitals on HeartPeriodLoadResult {
  HeartVitalsSummary vitalsSummary() {
    T? latest<T>(List<T> items, DateTime Function(T) time) {
      if (items.isEmpty) return null;
      return items.reduce((a, b) => time(a).isAfter(time(b)) ? a : b);
    }

    return HeartVitalsSummary(
      hasVitalsData: bloodPressure.isNotEmpty ||
          spO2.isNotEmpty ||
          respiratoryRate.isNotEmpty ||
          bodyTemperature.isNotEmpty ||
          vo2Max.isNotEmpty ||
          bloodGlucose.isNotEmpty ||
          skinTemperature.isNotEmpty,
      latestBloodPressure: latest(bloodPressure, (e) => e.time),
      latestSpO2: latest(spO2, (e) => e.time),
      latestRespiratoryRate: latest(respiratoryRate, (e) => e.time),
      latestBodyTemperature: latest(bodyTemperature, (e) => e.time),
      latestVo2Max: latest(vo2Max, (e) => e.time),
      latestBloodGlucose: latest(bloodGlucose, (e) => e.time),
      latestSkinTemperature: latest(skinTemperature, (e) => e.time),
    );
  }
}

/// Port of the Kotlin `LoadHeartPeriodUseCase`.
class LoadHeartPeriodUseCase {
  const LoadHeartPeriodUseCase(this._heartRepository, this._vitalsRepository);

  final HeartRepository _heartRepository;
  final VitalsRepository _vitalsRepository;

  Future<Result<HeartPeriodLoadResult>> call(
    PeriodLoadQuery query,
    HeartPeriodLoadRequest request, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    switch (request) {
      case HeartPeriodLoadCombined():
        // Both halves are the screen's content, so the composition is STRICT:
        // either half's failure fails the combined load, exactly as the
        // pre-Result `Future.wait` sank it on either throw.
        final heart = _heartRepository
            .loadHeartPeriod(query, HeartPeriodMetric.all, refreshMode: refreshMode)
            .then((loaded) => loaded.map(_heartToResult));
        final vitals = _vitalsRepository
            .loadVitalsPeriod(query, VitalsPeriodMetric.all, refreshMode: refreshMode)
            .then((loaded) => loaded.map(_vitalsToResult));
        final results = await Future.wait([heart, vitals]);
        return results[0].flatMap(
          (heartResult) async => results[1].map(heartResult.merge),
        );
      case HeartPeriodLoadHeartOnly(:final metric):
        final loaded = await _heartRepository.loadHeartPeriod(query, metric,
            refreshMode: refreshMode);
        return loaded.map(_heartToResult);
      case HeartPeriodLoadVitalsOnly(:final metric):
        final loaded = await _vitalsRepository.loadVitalsPeriod(query, metric,
            refreshMode: refreshMode);
        return loaded.map(_vitalsToResult);
    }
  }

  HeartPeriodLoadResult _heartToResult(HeartPeriodData data) =>
      HeartPeriodLoadResult(
        daySamples: data.daySamples,
        previousDaySamples: data.previousDaySamples,
        dailySummaries: data.dailySummaries,
        previousDailySummaries: data.previousDailySummaries,
        baselineDailySummaries: data.baselineDailySummaries,
        dayRestingSamples: data.dayRestingSamples,
        dayRestingBpm: data.dayRestingBpm,
        previousDayRestingBpm: data.previousDayRestingBpm,
        dayHrvSamples: data.dayHrvSamples,
        dayHrvMs: data.dayHrvMs,
        previousDayHrvMs: data.previousDayHrvMs,
        dailyRestingHR: data.dailyRestingHR,
        previousDailyRestingHR: data.previousDailyRestingHR,
        baselineDailyRestingHR: data.baselineDailyRestingHR,
        dailyHrv: data.dailyHrv,
        previousDailyHrv: data.previousDailyHrv,
        baselineDailyHrv: data.baselineDailyHrv,
      );

  HeartPeriodLoadResult _vitalsToResult(VitalsPeriodData data) =>
      HeartPeriodLoadResult(
        missingVitalsPermissions: data.missingVitalsPermissions,
        bloodPressure: data.bloodPressure,
        previousBloodPressure: data.previousBloodPressure,
        baselineBloodPressure: data.baselineBloodPressure,
        spO2: data.spO2,
        previousSpO2: data.previousSpO2,
        baselineSpO2: data.baselineSpO2,
        respiratoryRate: data.respiratoryRate,
        previousRespiratoryRate: data.previousRespiratoryRate,
        baselineRespiratoryRate: data.baselineRespiratoryRate,
        bodyTemperature: data.bodyTemperature,
        previousBodyTemperature: data.previousBodyTemperature,
        baselineBodyTemperature: data.baselineBodyTemperature,
        vo2Max: data.vo2Max,
        previousVo2Max: data.previousVo2Max,
        baselineVo2Max: data.baselineVo2Max,
        bloodGlucose: data.bloodGlucose,
        previousBloodGlucose: data.previousBloodGlucose,
        baselineBloodGlucose: data.baselineBloodGlucose,
        skinTemperature: data.skinTemperature,
        previousSkinTemperature: data.previousSkinTemperature,
        baselineSkinTemperature: data.baselineSkinTemperature,
      );
}
