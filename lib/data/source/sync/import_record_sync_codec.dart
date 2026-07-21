/// Serializes [ImportRecord]s to and from the sync wire, and derives each
/// record's deterministic content fingerprint (the dedup key).
///
/// WHY A FINGERPRINT
/// -----------------
/// Records read natively from Health Connect carry an HC id that differs per
/// device and usually a null `clientRecordId`. So dedup must key on CONTENT, not
/// the HC id. We hash the record's identifying fields into a `sync_<hex>` id —
/// the same construction the Apple Health importer uses for its `apple_health_`
/// ids ([buildStableClientRecordId]), including [appleInstantToStableString] so a
/// whole-second instant serializes without a trailing `.000`.
///
/// Both phones compute the SAME fingerprint for the same logical record, so the
/// bidirectional merge converges and a re-sync writes nothing. When received
/// records are written, they carry this `sync_<hex>` id as their
/// `clientRecordId`, so Health Connect upserts on it and a later re-sync dedups
/// via `filterExistingClientIds`.
library;

import 'dart:convert';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';

import '../../../domain/model/apple_health_import_records.dart';
import '../../../features/imports/applehealth/apple_health_import_conversion_support.dart'
    show appleInstantToStableString;

/// The prefix on every sync-assigned clientRecordId.
const String kSyncClientRecordIdPrefix = 'sync_';

/// Thrown when a record type has no codec entry (should never happen for a
/// negotiated type; a guard against a protocol/version mismatch).
class UnsupportedSyncRecordType implements Exception {
  const UnsupportedSyncRecordType(this.recordType);
  final String recordType;
  @override
  String toString() => 'UnsupportedSyncRecordType: $recordType';
}

/// Computes the deterministic `sync_<hex>` fingerprint for [record] from its
/// content. Independent of the record's current clientRecordId.
String syncFingerprint(ImportRecord record) {
  final parts = _fingerprintParts(record).join('|');
  final digest = sha256.convert(utf8.encode(parts)).bytes;
  final hex = StringBuffer();
  for (var i = 0; i < 16; i++) {
    hex.write(digest[i].toRadixString(16).padLeft(2, '0'));
  }
  return '$kSyncClientRecordIdPrefix$hex';
}

/// Encodes [record]'s times + values to wire bytes (JSON). The record type and
/// clientRecordId travel separately on the [SyncItem], so they are not repeated
/// here.
Uint8List encodeImportRecordPayload(ImportRecord record) =>
    Uint8List.fromList(utf8.encode(jsonEncode(_encode(record))));

/// Reconstructs an [ImportRecord] of [recordType] with [clientRecordId] from a
/// [payload] produced by [encodeImportRecordPayload].
ImportRecord decodeImportRecord({
  required String recordType,
  required String clientRecordId,
  required Uint8List payload,
}) {
  final json = jsonDecode(utf8.decode(payload)) as Map<String, Object?>;
  return _decode(recordType, clientRecordId, json);
}

// ── Time helpers ─────────────────────────────────────────────────────────────

int _ms(DateTime t) => t.toUtc().millisecondsSinceEpoch;
DateTime _dt(Object? ms) =>
    DateTime.fromMillisecondsSinceEpoch(ms! as int, isUtc: true);
int? _offSec(Duration? d) => d?.inSeconds;
Duration? _dur(Object? sec) => sec == null ? null : Duration(seconds: sec as int);
String _inst(DateTime t) => appleInstantToStableString(t);

Map<String, Object?> _interval(
  DateTime s,
  Duration? so,
  DateTime e,
  Duration? eo,
) =>
    {'s': _ms(s), 'so': _offSec(so), 'e': _ms(e), 'eo': _offSec(eo)};

Map<String, Object?> _instant(DateTime t, Duration? o) =>
    {'i': _ms(t), 'io': _offSec(o)};

// ── Fingerprint parts (identifying content per type) ─────────────────────────

List<Object?> _fingerprintParts(ImportRecord r) {
  switch (r) {
    case StepsImportRecord():
      return [r.targetType, _inst(r.startTime), _inst(r.endTime), r.count];
    case DistanceImportRecord():
      return [r.targetType, _inst(r.startTime), _inst(r.endTime), r.meters];
    case ActiveCaloriesBurnedImportRecord():
      return [r.targetType, _inst(r.startTime), _inst(r.endTime), r.kilocalories];
    case BasalMetabolicRateImportRecord():
      return [r.targetType, _inst(r.time), r.kilocaloriesPerDay];
    case FloorsClimbedImportRecord():
      return [r.targetType, _inst(r.startTime), _inst(r.endTime), r.floors];
    case ElevationGainedImportRecord():
      return [r.targetType, _inst(r.startTime), _inst(r.endTime), r.meters];
    case WheelchairPushesImportRecord():
      return [r.targetType, _inst(r.startTime), _inst(r.endTime), r.count];
    case SpeedImportRecord():
      return [
        r.targetType,
        _inst(r.startTime),
        _inst(r.endTime),
        for (final s in r.samples) '${_inst(s.time)}:${s.metersPerSecond}',
      ];
    case HeartRateImportRecord():
      return [
        r.targetType,
        _inst(r.startTime),
        _inst(r.endTime),
        for (final s in r.samples) '${_inst(s.time)}:${s.beatsPerMinute}',
      ];
    case RestingHeartRateImportRecord():
      return [r.targetType, _inst(r.time), r.beatsPerMinute];
    case HeartRateVariabilityRmssdImportRecord():
      return [r.targetType, _inst(r.time), r.rmssdMillis];
    case WeightImportRecord():
      return [r.targetType, _inst(r.time), r.kilograms];
    case HeightImportRecord():
      return [r.targetType, _inst(r.time), r.meters];
    case BodyFatImportRecord():
      return [r.targetType, _inst(r.time), r.percent];
    case LeanBodyMassImportRecord():
      return [r.targetType, _inst(r.time), r.kilograms];
    case BoneMassImportRecord():
      return [r.targetType, _inst(r.time), r.kilograms];
    case BodyWaterMassImportRecord():
      return [r.targetType, _inst(r.time), r.kilograms];
    case HydrationImportRecord():
      return [r.targetType, _inst(r.startTime), _inst(r.endTime), r.milliliters];
    case OxygenSaturationImportRecord():
      return [r.targetType, _inst(r.time), r.percent];
    case RespiratoryRateImportRecord():
      return [r.targetType, _inst(r.time), r.rate];
    case BodyTemperatureImportRecord():
      return [r.targetType, _inst(r.time), r.celsius];
    case BloodGlucoseImportRecord():
      return [r.targetType, _inst(r.time), r.millimolesPerLiter];
    case Vo2MaxImportRecord():
      return [r.targetType, _inst(r.time), r.vo2MillilitersPerMinuteKilogram];
    case BasalBodyTemperatureImportRecord():
      return [r.targetType, _inst(r.time), r.celsius];
    case MindfulnessSessionImportRecord():
      return [r.targetType, _inst(r.startTime), _inst(r.endTime), r.title];
    case MenstruationFlowImportRecord():
      return [r.targetType, _inst(r.time), r.flow.name];
    case OvulationTestImportRecord():
      return [r.targetType, _inst(r.time), r.result.name];
    case CervicalMucusImportRecord():
      return [r.targetType, _inst(r.time), r.appearance.name, r.sensation.name];
    case IntermenstrualBleedingImportRecord():
      return [r.targetType, _inst(r.time)];
    case SexualActivityImportRecord():
      return [r.targetType, _inst(r.time), r.protectionUsed.name];
    case BloodPressureImportRecord():
      return [r.targetType, _inst(r.time), r.systolicMmHg, r.diastolicMmHg];
    case SleepSessionImportRecord():
      return [
        r.targetType,
        _inst(r.startTime),
        _inst(r.endTime),
        r.title,
        for (final s in r.stages)
          '${_inst(s.startTime)}:${_inst(s.endTime)}:${s.stage.name}',
      ];
    case NutritionImportRecord():
      final keys = r.nutrientGrams.keys.toList()..sort();
      return [
        r.targetType,
        _inst(r.startTime),
        _inst(r.endTime),
        r.name ?? '',
        r.energyKilocalories,
        for (final k in keys) '$k=${r.nutrientGrams[k]}',
      ];
    case ExerciseSessionImportRecord():
      return [
        r.targetType,
        _inst(r.startTime),
        _inst(r.endTime),
        r.exerciseType.name,
        r.title,
      ];
    case TotalCaloriesBurnedImportRecord():
      return [r.targetType, _inst(r.startTime), _inst(r.endTime), r.kilocalories];
    case PowerImportRecord():
      return [
        r.targetType,
        _inst(r.startTime),
        _inst(r.endTime),
        for (final s in r.samples) '${_inst(s.time)}:${s.watts}',
      ];
    case StepsCadenceImportRecord():
      return [
        r.targetType,
        _inst(r.startTime),
        _inst(r.endTime),
        for (final s in r.samples) '${_inst(s.time)}:${s.rate}',
      ];
    case CyclingPedalingCadenceImportRecord():
      return [
        r.targetType,
        _inst(r.startTime),
        _inst(r.endTime),
        for (final s in r.samples) '${_inst(s.time)}:${s.revolutionsPerMinute}',
      ];
    case SkinTemperatureImportRecord():
      return [
        r.targetType,
        _inst(r.startTime),
        _inst(r.endTime),
        r.baselineCelsius,
        r.measurementLocation,
        for (final s in r.deltas) '${_inst(s.time)}:${s.deltaCelsius}',
      ];
    case MenstruationPeriodImportRecord():
      return [r.targetType, _inst(r.startTime), _inst(r.endTime)];
    case PlannedExerciseSessionImportRecord():
      return [
        r.targetType,
        _inst(r.startTime),
        _inst(r.endTime),
        r.exerciseType,
        r.title ?? '',
        for (final b in r.blocks)
          '${b.repetitions}:${b.steps.map((s) => '${s.exerciseType}/${s.completionKind}/${s.completionRepetitions}/${s.completionSeconds}').join(',')}',
      ];
  }
}

// ── Encode (value fields to JSON) ────────────────────────────────────────────

Map<String, Object?> _encode(ImportRecord r) {
  switch (r) {
    case StepsImportRecord():
      return {..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset), 'count': r.count};
    case DistanceImportRecord():
      return {..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset), 'meters': r.meters};
    case ActiveCaloriesBurnedImportRecord():
      return {..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset), 'kcal': r.kilocalories};
    case BasalMetabolicRateImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'kcalDay': r.kilocaloriesPerDay};
    case FloorsClimbedImportRecord():
      return {..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset), 'floors': r.floors};
    case ElevationGainedImportRecord():
      return {..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset), 'meters': r.meters};
    case WheelchairPushesImportRecord():
      return {..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset), 'count': r.count};
    case SpeedImportRecord():
      return {
        ..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset),
        'samples': [for (final s in r.samples) {'t': _ms(s.time), 'v': s.metersPerSecond}],
      };
    case HeartRateImportRecord():
      return {
        ..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset),
        'samples': [for (final s in r.samples) {'t': _ms(s.time), 'v': s.beatsPerMinute}],
      };
    case RestingHeartRateImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'bpm': r.beatsPerMinute};
    case HeartRateVariabilityRmssdImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'rmssd': r.rmssdMillis};
    case WeightImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'kg': r.kilograms};
    case HeightImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'meters': r.meters};
    case BodyFatImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'pct': r.percent};
    case LeanBodyMassImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'kg': r.kilograms};
    case BoneMassImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'kg': r.kilograms};
    case BodyWaterMassImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'kg': r.kilograms};
    case HydrationImportRecord():
      return {..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset), 'ml': r.milliliters};
    case OxygenSaturationImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'pct': r.percent};
    case RespiratoryRateImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'rate': r.rate};
    case BodyTemperatureImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'c': r.celsius};
    case BloodGlucoseImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'mmol': r.millimolesPerLiter};
    case Vo2MaxImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'vo2': r.vo2MillilitersPerMinuteKilogram};
    case BasalBodyTemperatureImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'c': r.celsius};
    case MindfulnessSessionImportRecord():
      return {..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset), 'title': r.title};
    case MenstruationFlowImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'flow': r.flow.name};
    case OvulationTestImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'result': r.result.name};
    case CervicalMucusImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'appearance': r.appearance.name, 'sensation': r.sensation.name};
    case IntermenstrualBleedingImportRecord():
      return _instant(r.time, r.zoneOffset);
    case SexualActivityImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'protection': r.protectionUsed.name};
    case BloodPressureImportRecord():
      return {..._instant(r.time, r.zoneOffset), 'sys': r.systolicMmHg, 'dia': r.diastolicMmHg};
    case SleepSessionImportRecord():
      return {
        ..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset),
        'title': r.title,
        'stages': [
          for (final s in r.stages)
            {'s': _ms(s.startTime), 'e': _ms(s.endTime), 'stage': s.stage.name},
        ],
      };
    case NutritionImportRecord():
      return {
        ..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset),
        'name': r.name,
        'kcal': r.energyKilocalories,
        'nutrients': r.nutrientGrams,
      };
    case ExerciseSessionImportRecord():
      return {
        ..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset),
        'exerciseType': r.exerciseType.name,
        'title': r.title,
        'route': r.route == null
            ? null
            : [
                for (final p in r.route!.route)
                  {
                    't': _ms(p.time),
                    'lat': p.latitude,
                    'lng': p.longitude,
                    'alt': p.altitudeMeters,
                    'ha': p.horizontalAccuracyMeters,
                    'va': p.verticalAccuracyMeters,
                  },
              ],
      };
    case TotalCaloriesBurnedImportRecord():
      return {..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset), 'kcal': r.kilocalories};
    case PowerImportRecord():
      return {
        ..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset),
        'samples': [for (final s in r.samples) {'t': _ms(s.time), 'v': s.watts}],
      };
    case StepsCadenceImportRecord():
      return {
        ..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset),
        'samples': [for (final s in r.samples) {'t': _ms(s.time), 'v': s.rate}],
      };
    case CyclingPedalingCadenceImportRecord():
      return {
        ..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset),
        'samples': [for (final s in r.samples) {'t': _ms(s.time), 'v': s.revolutionsPerMinute}],
      };
    case SkinTemperatureImportRecord():
      return {
        ..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset),
        'baseline': r.baselineCelsius,
        'loc': r.measurementLocation,
        'deltas': [for (final s in r.deltas) {'t': _ms(s.time), 'v': s.deltaCelsius}],
      };
    case MenstruationPeriodImportRecord():
      return _interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset);
    case PlannedExerciseSessionImportRecord():
      return {
        ..._interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset),
        'et': r.exerciseType,
        'title': r.title,
        'notes': r.notes,
        'blocks': [
          for (final b in r.blocks)
            {
              'reps': b.repetitions,
              'desc': b.description,
              'steps': [
                for (final s in b.steps)
                  {
                    'et': s.exerciseType,
                    'phase': s.exercisePhase,
                    'desc': s.description,
                    'ck': s.completionKind,
                    'cr': s.completionRepetitions,
                    'cs': s.completionSeconds,
                  },
              ],
            },
        ],
      };
  }
}

// ── Decode (JSON to a typed record with the given clientRecordId) ────────────

ImportRecord _decode(String type, String cid, Map<String, Object?> j) {
  DateTime s() => _dt(j['s']);
  Duration? so() => _dur(j['so']);
  DateTime e() => _dt(j['e']);
  Duration? eo() => _dur(j['eo']);
  DateTime i() => _dt(j['i']);
  Duration? io() => _dur(j['io']);
  double d(String k) => (j[k]! as num).toDouble();
  double? dn(String k) => (j[k] as num?)?.toDouble();
  int n(String k) => j[k]! as int;

  switch (type) {
    case 'StepsRecord':
      return StepsImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), count: n('count'));
    case 'DistanceRecord':
      return DistanceImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), meters: d('meters'));
    case 'ActiveCaloriesBurnedRecord':
      return ActiveCaloriesBurnedImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), kilocalories: d('kcal'));
    case 'BasalMetabolicRateRecord':
      return BasalMetabolicRateImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), kilocaloriesPerDay: d('kcalDay'));
    case 'FloorsClimbedRecord':
      return FloorsClimbedImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), floors: d('floors'));
    case 'ElevationGainedRecord':
      return ElevationGainedImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), meters: d('meters'));
    case 'WheelchairPushesRecord':
      return WheelchairPushesImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), count: n('count'));
    case 'SpeedRecord':
      return SpeedImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), samples: [for (final x in j['samples']! as List) SpeedSampleValue(_dt((x as Map)['t']), (x['v'] as num).toDouble())]);
    case 'HeartRateRecord':
      return HeartRateImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), samples: [for (final x in j['samples']! as List) HeartRateSampleValue(_dt((x as Map)['t']), (x['v'] as num).toInt())]);
    case 'RestingHeartRateRecord':
      return RestingHeartRateImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), beatsPerMinute: n('bpm'));
    case 'HeartRateVariabilityRmssdRecord':
      return HeartRateVariabilityRmssdImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), rmssdMillis: d('rmssd'));
    case 'WeightRecord':
      return WeightImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), kilograms: d('kg'));
    case 'HeightRecord':
      return HeightImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), meters: d('meters'));
    case 'BodyFatRecord':
      return BodyFatImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), percent: d('pct'));
    case 'LeanBodyMassRecord':
      return LeanBodyMassImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), kilograms: d('kg'));
    case 'BoneMassRecord':
      return BoneMassImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), kilograms: d('kg'));
    case 'BodyWaterMassRecord':
      return BodyWaterMassImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), kilograms: d('kg'));
    case 'HydrationRecord':
      return HydrationImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), milliliters: d('ml'));
    case 'OxygenSaturationRecord':
      return OxygenSaturationImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), percent: d('pct'));
    case 'RespiratoryRateRecord':
      return RespiratoryRateImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), rate: d('rate'));
    case 'BodyTemperatureRecord':
      return BodyTemperatureImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), celsius: d('c'));
    case 'BloodGlucoseRecord':
      return BloodGlucoseImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), millimolesPerLiter: d('mmol'));
    case 'Vo2MaxRecord':
      return Vo2MaxImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), vo2MillilitersPerMinuteKilogram: d('vo2'));
    case 'BasalBodyTemperatureRecord':
      return BasalBodyTemperatureImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), celsius: d('c'));
    case 'MindfulnessSessionRecord':
      return MindfulnessSessionImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), title: j['title']! as String);
    case 'MenstruationFlowRecord':
      return MenstruationFlowImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), flow: _enum(MenstruationFlowType.values, j['flow']));
    case 'OvulationTestRecord':
      return OvulationTestImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), result: _enum(OvulationResultType.values, j['result']));
    case 'CervicalMucusRecord':
      return CervicalMucusImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), appearance: _enum(CervicalMucusAppearance.values, j['appearance']), sensation: _enum(CervicalMucusSensation.values, j['sensation']));
    case 'IntermenstrualBleedingRecord':
      return IntermenstrualBleedingImportRecord(clientRecordId: cid, time: i(), zoneOffset: io());
    case 'SexualActivityRecord':
      return SexualActivityImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), protectionUsed: _enum(SexualActivityProtection.values, j['protection']));
    case 'BloodPressureRecord':
      return BloodPressureImportRecord(clientRecordId: cid, time: i(), zoneOffset: io(), systolicMmHg: d('sys'), diastolicMmHg: d('dia'));
    case 'SleepSessionRecord':
      return SleepSessionImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), title: j['title']! as String, stages: [for (final x in j['stages']! as List) SleepStageValue(startTime: _dt((x as Map)['s']), endTime: _dt(x['e']), stage: _enum(SleepStageType.values, x['stage']))]);
    case 'NutritionRecord':
      return NutritionImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), name: j['name'] as String?, energyKilocalories: dn('kcal'), nutrientGrams: {for (final entry in (j['nutrients']! as Map).entries) entry.key as String: (entry.value as num).toDouble()});
    case 'ExerciseSessionRecord':
      final route = j['route'] as List?;
      return ExerciseSessionImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), exerciseType: _enum(ImportExerciseType.values, j['exerciseType']), title: j['title']! as String, route: route == null ? null : ExerciseRoute([for (final p in route) ExerciseRouteLocation(time: _dt((p as Map)['t']), latitude: (p['lat'] as num).toDouble(), longitude: (p['lng'] as num).toDouble(), altitudeMeters: (p['alt'] as num?)?.toDouble(), horizontalAccuracyMeters: (p['ha'] as num?)?.toDouble(), verticalAccuracyMeters: (p['va'] as num?)?.toDouble())]));
    case 'TotalCaloriesBurnedRecord':
      return TotalCaloriesBurnedImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), kilocalories: d('kcal'));
    case 'PowerRecord':
      return PowerImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), samples: [for (final x in j['samples']! as List) PowerSampleValue(_dt((x as Map)['t']), (x['v'] as num).toDouble())]);
    case 'StepsCadenceRecord':
      return StepsCadenceImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), samples: [for (final x in j['samples']! as List) StepsCadenceSampleValue(_dt((x as Map)['t']), (x['v'] as num).toDouble())]);
    case 'CyclingPedalingCadenceRecord':
      return CyclingPedalingCadenceImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), samples: [for (final x in j['samples']! as List) CyclingPedalingCadenceSampleValue(_dt((x as Map)['t']), (x['v'] as num).toDouble())]);
    case 'SkinTemperatureRecord':
      return SkinTemperatureImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), baselineCelsius: dn('baseline'), measurementLocation: n('loc'), deltas: [for (final x in j['deltas']! as List) SkinTemperatureDeltaValue(_dt((x as Map)['t']), (x['v'] as num).toDouble())]);
    case 'MenstruationPeriodRecord':
      return MenstruationPeriodImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo());
    case 'PlannedExerciseSessionRecord':
      return PlannedExerciseSessionImportRecord(clientRecordId: cid, startTime: s(), startZoneOffset: so(), endTime: e(), endZoneOffset: eo(), exerciseType: n('et'), title: j['title'] as String?, notes: j['notes'] as String?, blocks: [
        for (final b in (j['blocks']! as List).cast<Map>())
          PlannedExerciseBlockValue(repetitions: (b['reps'] as num).toInt(), description: b['desc'] as String?, steps: [
            for (final st in (b['steps']! as List).cast<Map>())
              PlannedExerciseStepValue(exerciseType: (st['et'] as num).toInt(), exercisePhase: (st['phase'] as num).toInt(), description: st['desc'] as String?, completionKind: (st['ck'] as num).toInt(), completionRepetitions: (st['cr'] as num?)?.toInt(), completionSeconds: (st['cs'] as num?)?.toInt()),
          ]),
      ]);
  }
  throw UnsupportedSyncRecordType(type);
}

T _enum<T extends Enum>(List<T> values, Object? name) =>
    values.firstWhere((v) => v.name == name);
