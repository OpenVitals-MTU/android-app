import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_converter.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_models.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_parser.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_xml_support.dart';


/// A ZIP written the way a *streaming* writer does it: the local header's sizes
/// are unknown (flag bit 3) and a data descriptor follows the payload. The
/// sequential reader has to bound each entry itself, since there is no central
/// directory to look the sizes up in.
List<int> streamingZip(Map<String, String> entries) {
  final out = BytesBuilder();
  entries.forEach((name, contents) {
    final nameBytes = utf8.encode(name);
    final raw = utf8.encode(contents);
    final deflated = Deflate(raw).getBytes();
    final header = ByteData(30)
      ..setUint32(0, 0x04034b50, Endian.little)
      ..setUint16(4, 20, Endian.little)
      ..setUint16(6, 0x08, Endian.little) // sizes unknown, descriptor follows
      ..setUint16(8, 8, Endian.little) // deflate
      ..setUint16(26, nameBytes.length, Endian.little);
    out
      ..add(header.buffer.asUint8List())
      ..add(nameBytes)
      ..add(deflated);
    final descriptor = ByteData(16)
      ..setUint32(0, 0x08074b50, Endian.little)
      ..setUint32(4, getCrc32(raw), Endian.little)
      ..setUint32(8, deflated.length, Endian.little)
      ..setUint32(12, raw.length, Endian.little);
    out.add(descriptor.buffer.asUint8List());
  });
  // The central-directory signature terminates the last entry.
  out.add((ByteData(4)..setUint32(0, 0x02014b50, Endian.little))
      .buffer
      .asUint8List());
  return out.toBytes();
}

AppleParsedExport parseXml(String xml) =>
    AppleHealthImportParser.parse(utf8.encode(xml));

AppleHealthConversionResult convertParsed(AppleParsedExport parsed) =>
    AppleHealthImportConverter(mindfulnessAvailable: true).convert(parsed);

List<int> zipExport(
  String xml, {
  Map<String, String> extraFiles = const {},
  bool extraFilesBeforeXml = false,
}) {
  final archive = Archive();
  void writeExtras() {
    extraFiles.forEach((path, contents) {
      archive.add(ArchiveFile.string(path, contents));
    });
  }

  if (extraFilesBeforeXml) writeExtras();
  archive.add(ArchiveFile.string('apple_health_export/export.xml', xml));
  if (!extraFilesBeforeXml) writeExtras();
  return ZipEncoder().encodeBytes(archive);
}

void main() {
  group('AppleHealthImportParser streaming zip', () {
    test('reads entries whose local header has no sizes (data descriptors)',
        () {
      final zip = streamingZip({
        'apple_health_export/export.xml': '''
          <HealthData>
            <Workout workoutActivityType="HKWorkoutActivityTypeRunning"
              startDate="2026-01-01 09:00:00 +0000" endDate="2026-01-01 09:30:00 +0000">
              <WorkoutRoute>
                <FileReference path="/workout-routes/route1.gpx" />
              </WorkoutRoute>
            </Workout>
          </HealthData>
        ''',
        'apple_health_export/workout-routes/route1.gpx':
            '<gpx><trk><trkseg>'
                '<trkpt lat="45.0" lon="9.0"><ele>100</ele></trkpt>'
                '<trkpt lat="45.1" lon="9.1"><ele>110</ele></trkpt>'
                '</trkseg></trk></gpx>',
      });

      final parsed = AppleHealthImportParser.parse(zip);

      expect(parsed.parsedWorkouts, 1);
      final workout = parsed.workouts.single;
      expect(workout.routeReferencePaths, ['workout-routes/route1.gpx']);
      expect(workout.routes.single.points.length, 2);
      expect(workout.unavailableRoutePaths, isEmpty);
      expect(parsed.workoutRouteArchiveFailure, isNull);
    });
  });

  group('AppleHealthImportParser + converter', () {
    test('imports sleep category values as sleep stages', () {
      final parsed = parseXml('''
        <HealthData>
          <Record type="HKCategoryTypeIdentifierSleepAnalysis" sourceName="Apple Watch"
            startDate="2026-01-01 22:00:00 +0000" endDate="2026-01-02 06:00:00 +0000"
            value="HKCategoryValueSleepAnalysisAsleepCore" />
        </HealthData>
      ''');

      final result = convertParsed(parsed);

      expect(parsed.parsedRecords, 1);
      expect(result.converted.length, 1);
      final sleep = result.converted.single.record as SleepSessionImportRecord;
      expect(sleep.stages.single.stage, SleepStageType.light);
    });

    test('handles an apple export DOCTYPE without loading DTD grammar', () {
      final parsed = parseXml('''<?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE HealthData [
          <!ELEMENT HealthData (Record*)>
          <!ELEMENT Record EMPTY>
          <!ATTLIST Record type CDATA #IMPLIED>
        ]>
        <HealthData>
          <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
            unit="count" value="100" />
        </HealthData>
      ''');

      expect(parsed.parsedRecords, 1);
    });

    test('repairs raw control characters and unescaped ampersands', () {
      final xml = '<HealthData><Record type="HKQuantityTypeIdentifierStepCount" '
          'sourceName="NotesApp" device="AT&T Watch" '
          'startDate="2026-01-01 00:00:00 +0000" endDate="2026-01-01 00:01:00 +0000" '
          'unit="count" value="10" /></HealthData>';

      final parsed = parseXml(xml);

      expect(parsed.parsedRecords, 1);
      expect(parsed.sanitizedControlChars, 1);
      expect(parsed.sanitizedAmpersands, 1);
      final record = parsed.records.single;
      expect(record.sourceName, 'NotesApp');
      expect(record.device, 'AT&T Watch');
    });

    test('wraps a genuine well-formedness failure with surrounding text', () {
      final xml = '<HealthData><Record type="HKQuantityTypeIdentifierStepCount" '
          'startDate="2026-01-01 00:00:00 +0000" endDate="2026-01-01 00:01:00 +0000">'
          '</MismatchedClosingTag></HealthData>';

      Object? caught;
      try {
        parseXml(xml);
      } catch (error) {
        caught = error;
      }

      expect(caught, isA<AppleHealthXmlParseException>());
      final message = (caught! as AppleHealthXmlParseException).message;
      expect(message.contains('not well-formed'), isTrue);
      expect(message.contains('Text leading up to the error'), isTrue);
    });

    test('preserves timezone offsets on apple date strings', () {
      final parsed = parseXml('''
        <HealthData>
          <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
            startDate="2023-12-13 20:48:49 +0100" endDate="2023-12-13 20:58:49 +0100"
            unit="count" value="100" />
        </HealthData>
      ''');

      final record = parsed.records.single;
      expect(record.startDate!.instant, DateTime.utc(2023, 12, 13, 19, 48, 49));
      expect(record.startDate!.offset, const Duration(hours: 1));
      expect(record.endDate!.instant, DateTime.utc(2023, 12, 13, 19, 58, 49));
      expect(record.endDate!.offset, const Duration(hours: 1));
    });

    test('imports walking speed as speed samples (km/hr → m/s)', () {
      final parsed = parseXml('''
        <HealthData>
          <Record type="HKQuantityTypeIdentifierWalkingSpeed" sourceName="Phone"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:05 +0000"
            unit="km/hr" value="3.6" />
        </HealthData>
      ''');

      final result = convertParsed(parsed);

      expect(result.converted.length, 1);
      final speed = result.converted.single.record as SpeedImportRecord;
      expect(speed.samples.single.metersPerSecond, closeTo(1.0, 1e-9));
    });

    test('prefers blood pressure correlations', () {
      final parsed = parseXml('''
        <HealthData>
          <Correlation type="HKCorrelationTypeIdentifierBloodPressure" sourceName="Test"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000">
            <Record type="HKQuantityTypeIdentifierBloodPressureSystolic" sourceName="Test"
              startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000"
              unit="mmHg" value="120" />
            <Record type="HKQuantityTypeIdentifierBloodPressureDiastolic" sourceName="Test"
              startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000"
              unit="mmHg" value="80" />
          </Correlation>
        </HealthData>
      ''');

      final result = convertParsed(parsed);

      expect(parsed.parsedRecords, 2);
      expect(parsed.parsedCorrelations, 1);
      expect(result.converted.length, 1);
      final bp = result.converted.single.record as BloodPressureImportRecord;
      expect(bp.systolicMmHg, 120.0);
      expect(bp.diastolicMmHg, 80.0);
    });

    test('reads workout statistics as workout distance and energy', () {
      final parsed = parseXml('''
        <HealthData>
          <Workout workoutActivityType="HKWorkoutActivityTypeCycling" sourceName="Apple Watch"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
            duration="45" durationUnit="min">
            <WorkoutStatistics type="HKQuantityTypeIdentifierActiveEnergyBurned"
              startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
              sum="123.4" unit="kcal" />
            <WorkoutStatistics type="HKQuantityTypeIdentifierDistanceCycling"
              startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
              sum="6.5" unit="km" />
          </Workout>
        </HealthData>
      ''');

      final workout = parsed.workouts.single;
      expect(workout.totalDistance, 6.5);
      expect(workout.totalDistanceUnit, 'km');
      expect(workout.totalEnergyBurned, 123.4);
      expect(workout.totalEnergyBurnedUnit, 'kcal');

      final result = convertParsed(parsed);
      final byTarget = {for (final c in result.converted) c.targetType: c};

      expect(parsed.parsedWorkouts, 1);
      expect(result.converted.length, 3);
      expect(byTarget.containsKey('ExerciseSessionRecord'), isTrue);
      final distance = byTarget['DistanceRecord']!.record as DistanceImportRecord;
      final energy =
          byTarget['ActiveCaloriesBurnedRecord']!.record
              as ActiveCaloriesBurnedImportRecord;
      expect(distance.meters, 6500.0);
      expect(energy.kilocalories, 123.4);
      expect(result.typeStats['HKWorkoutActivityTypeCycling']!.converted, 3);
    });

    test('drops workout energy totals when overlapping records exist from '
        'another source', () {
      final parsed = parseXml('''
        <HealthData>
          <Record type="HKQuantityTypeIdentifierActiveEnergyBurned" sourceName="iPhone"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
            unit="kcal" value="123" />
          <Workout workoutActivityType="HKWorkoutActivityTypeCycling" sourceName="Apple Watch"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
            duration="45" durationUnit="min">
            <WorkoutStatistics type="HKQuantityTypeIdentifierActiveEnergyBurned"
              startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:45:00 +0000"
              sum="123" unit="kcal" />
          </Workout>
        </HealthData>
      ''');

      final result = convertParsed(parsed);

      expect(
        result.converted.map((it) => it.targetType).toList(),
        ['ExerciseSessionRecord', 'ActiveCaloriesBurnedRecord'],
      );
    });

    test('skips lower priority additive records mostly covered by another '
        'source', () {
      final parsed = parseXml('''
        <HealthData>
          <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Alesia's iPhone"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
            unit="count" value="100" />
          <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Health CoPilot"
            startDate="2026-01-01 08:01:00 +0000" endDate="2026-01-01 08:09:00 +0000"
            unit="count" value="95" />
        </HealthData>
      ''');

      final result = convertParsed(parsed);

      expect(result.converted.length, 1);
      expect(result.converted.single.targetType, 'StepsRecord');
      expect(result.typeStats['HKQuantityTypeIdentifierStepCount']!.converted, 1);
      expect(result.typeStats['HKQuantityTypeIdentifierStepCount']!.skipped, 1);
      expect(result.diagnostics.single.reasonCode, 'overlap_cross_source');
    });

    test('synthetic export fixture covers supported converter targets', () {
      final parsed = parseFixture('synthetic_supported_export.xml');

      final result = convertParsed(parsed);
      final targetTypes = result.converted.map((it) => it.targetType).toSet();

      expect(parsed.parsedRecords, 41);
      expect(parsed.parsedCorrelations, 1);
      expect(parsed.parsedWorkouts, 1);
      expect(parsed.parsedActivitySummaries, 1);
      expect(result.converted.length, 35);
      expect(
        targetTypes.containsAll({
          'StepsRecord',
          'DistanceRecord',
          'ActiveCaloriesBurnedRecord',
          'BasalMetabolicRateRecord',
          'FloorsClimbedRecord',
          'ElevationGainedRecord',
          'WheelchairPushesRecord',
          'HeartRateRecord',
          'RestingHeartRateRecord',
          'WeightRecord',
          'HeightRecord',
          'BodyFatRecord',
          'LeanBodyMassRecord',
          'BoneMassRecord',
          'BodyWaterMassRecord',
          'HydrationRecord',
          'OxygenSaturationRecord',
          'RespiratoryRateRecord',
          'BodyTemperatureRecord',
          'BloodGlucoseRecord',
          'Vo2MaxRecord',
          'BasalBodyTemperatureRecord',
          'MindfulnessSessionRecord',
          'MenstruationFlowRecord',
          'OvulationTestRecord',
          'CervicalMucusRecord',
          'IntermenstrualBleedingRecord',
          'SexualActivityRecord',
          'BloodPressureRecord',
          'SleepSessionRecord',
          'NutritionRecord',
          'ExerciseSessionRecord',
        }),
        isTrue,
      );
      expect(
        result.diagnostics.any((d) =>
            d.appleType == 'ActivitySummary' && d.reasonCode == 'unsupported'),
        isTrue,
      );
    });

    test('reads a zipped apple export', () {
      final parsed = AppleHealthImportParser.parse(zipExport('''
        <HealthData>
          <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:10:00 +0000"
            unit="count" value="100" />
        </HealthData>
      '''));

      expect(parsed.parsedRecords, 1);
      final result = convertParsed(parsed);
      expect(result.converted.single.targetType, 'StepsRecord');
    });

    test('imports an apple workout route with synthesized times', () {
      const xml = '''
        <HealthData>
          <Workout workoutActivityType="HKWorkoutActivityTypeRunning" sourceName="Apple Watch"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:30:00 +0000"
            duration="30" durationUnit="min">
            <WorkoutRoute sourceName="Apple Watch"
              startDate="2026-01-01 09:00:00 +0000" endDate="2026-01-01 09:00:00 +0000">
              <FileReference path="/workout-routes/route_2026-01-01_8.00am.gpx" />
            </WorkoutRoute>
          </Workout>
        </HealthData>
      ''';
      const gpx = '''<?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="Apple Health Export">
          <trk><trkseg>
            <trkpt lat="59.000000" lon="24.000000"><ele>0</ele><time>2026-07-05T08:34:11Z</time></trkpt>
            <trkpt lat="59.000000" lon="24.010000"><ele>0</ele><time>2026-07-05T08:34:11Z</time></trkpt>
            <trkpt lat="59.010000" lon="24.010000"><ele>0</ele><time>2026-07-05T08:34:11Z</time></trkpt>
          </trkseg></trk>
        </gpx>''';

      final parsed = AppleHealthImportParser.parse(zipExport(
        xml,
        extraFiles: {
          'apple_health_export/workout-routes/route_2026-01-01_8.00am.gpx': gpx,
        },
      ));
      final result = convertParsed(parsed);

      expect(parsed.parsedWorkouts, 1);
      expect(parsed.workouts.single.routes.length, 1);
      expect(parsed.workouts.single.routeReferences, 1);
      final session =
          result.converted.single.record as ExerciseSessionImportRecord;
      final locations = session.route!.route;
      expect(locations.length, 3);
      expect(locations.first.time, DateTime.utc(2026, 1, 1, 8));
      expect(locations[0].time.isBefore(locations[1].time), isTrue);
      expect(locations[1].time.isBefore(locations[2].time), isTrue);
      expect(
        locations.last.time.isBefore(DateTime.utc(2026, 1, 1, 8, 30)),
        isTrue,
      );
      expect(locations.first.latitude, 59.0);
      expect(locations[1].longitude, 24.01);
      expect(locations.first.altitudeMeters, isNull);
    });

    test('synthesized route times stay strictly increasing at millisecond '
        'precision', () {
      final pausedPoints = List.generate(
        50,
        (_) =>
            '<trkpt lat="59.000000" lon="24.000000"><ele>0</ele>'
            '<time>2026-07-05T08:34:11Z</time></trkpt>',
      ).join('\n');
      const xml = '''
        <HealthData>
          <Workout workoutActivityType="HKWorkoutActivityTypeRunning" sourceName="Apple Watch"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:30:00 +0000"
            duration="30" durationUnit="min">
            <WorkoutRoute sourceName="Apple Watch"
              startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:30:00 +0000">
              <FileReference path="/workout-routes/route_2026-01-01_8.00am.gpx" />
            </WorkoutRoute>
          </Workout>
        </HealthData>
      ''';
      final gpx = '<gpx version="1.1" creator="Apple Health Export"><trk><trkseg>'
          '$pausedPoints'
          '<trkpt lat="59.010000" lon="24.010000"><ele>0</ele>'
          '<time>2026-07-05T08:35:11Z</time></trkpt>'
          '</trkseg></trk></gpx>';

      final parsed = AppleHealthImportParser.parse(zipExport(
        xml,
        extraFiles: {
          'apple_health_export/workout-routes/route_2026-01-01_8.00am.gpx': gpx,
        },
      ));
      final result = convertParsed(parsed);

      final session =
          result.converted.single.record as ExerciseSessionImportRecord;
      final locations = session.route!.route;
      expect(locations.length, 51);
      for (var i = 0; i < locations.length - 1; i++) {
        expect(
          locations[i].time.millisecondsSinceEpoch <
              locations[i + 1].time.millisecondsSinceEpoch,
          isTrue,
          reason: 'route times must differ by >= 1ms: '
              '${locations[i].time} -> ${locations[i + 1].time}',
        );
      }
      expect(
        locations.last.time.isBefore(DateTime.utc(2026, 1, 1, 8, 30)),
        isTrue,
      );
    });

    test('light mode keeps counts but skips dates, metadata and numeric '
        'values', () {
      const xml = '''
        <HealthData>
          <Record type="HKQuantityTypeIdentifierHeartRate" sourceName="Watch"
            creationDate="2026-01-01 08:00:00 +0000"
            startDate="2026-01-01 08:00:00 +0000" endDate="2026-01-01 08:00:00 +0000"
            unit="count/min" value="62">
            <MetadataEntry key="HKMetadataKeyHeartRateMotionContext" value="1" />
          </Record>
          <Workout workoutActivityType="HKWorkoutActivityTypeRunning" sourceName="Watch"
            startDate="2026-01-01 09:00:00 +0000" endDate="2026-01-01 09:30:00 +0000"
            duration="30" durationUnit="min" />
        </HealthData>
      ''';

      final fullRecords = <AppleRecord>[];
      final lightRecords = <AppleRecord>[];
      final lightWorkouts = <AppleWorkout>[];

      final fullParsed = AppleHealthImportParser.parse(
        utf8.encode(xml),
        consumer: _CollectingConsumer(fullRecords, null),
      );
      final lightParsed = AppleHealthImportParser.parse(
        utf8.encode(xml),
        consumer: _CollectingConsumer(lightRecords, lightWorkouts),
        options: const AppleHealthParseOptions(
          parseRouteFiles: false,
          parseRecordDetails: false,
        ),
      );

      expect(fullParsed.parsedRecords, lightParsed.parsedRecords);
      expect(fullParsed.parsedWorkouts, lightParsed.parsedWorkouts);
      expect(fullParsed.parsedTypeCounts, lightParsed.parsedTypeCounts);

      final full = fullRecords.single;
      final light = lightRecords.single;
      expect(full.type, light.type);
      expect(full.rawValue, light.rawValue);
      expect(
        full.startDate != null &&
            full.numericValue != null &&
            full.metadata.isNotEmpty,
        isTrue,
      );
      expect(light.startDate, isNull);
      expect(light.endDate, isNull);
      expect(light.creationDate, isNull);
      expect(light.numericValue, isNull);
      expect(light.metadata.isEmpty, isTrue);
      expect(lightWorkouts.single.workoutActivityType,
          'HKWorkoutActivityTypeRunning');
      expect(lightWorkouts.single.startDate, isNull);
    });
  });
}

AppleParsedExport parseFixture(String name) {
  final file = File(
    'test/features/imports/applehealth/fixtures/$name',
  );
  return AppleHealthImportParser.parse(file.readAsBytesSync());
}

class _CollectingConsumer implements AppleHealthXmlEventConsumer {
  _CollectingConsumer(this.records, this.workouts);

  final List<AppleRecord> records;
  final List<AppleWorkout>? workouts;

  @override
  void onParsedType(String type) {}

  @override
  void onRecord(AppleRecord record) => records.add(record);

  @override
  void onWorkout(AppleWorkout workout) => workouts?.add(workout);

  @override
  void onCorrelation(AppleCorrelation correlation) {}

  @override
  void onActivitySummary() {}
}
