import 'dart:convert';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/route_file_parser.dart';

/// Port of the Kotlin `RouteFileParserTest`.
void main() {
  group('RouteFileParser', () {
    test('parse extracts timestamped GPX track points and summaries', () {
      final result = RouteFileParser.parse(
        '''
<gpx version="1.1" creator="OpenTracks">
  <trk>
    <name>Morning ride</name>
    <desc>Easy commute</desc>
    <type>cycling</type>
    <trkseg>
      <trkpt lat="59.0000" lon="24.0000">
        <ele>10.0</ele>
        <time>2026-05-26T08:30:00Z</time>
      </trkpt>
      <trkpt lat="59.0010" lon="24.0020">
        <ele>18.0</ele>
        <time>2026-05-26T08:31:00Z</time>
      </trkpt>
    </trkseg>
  </trk>
</gpx>
''',
        fileName: 'run.gpx',
      );

      expect(result.fileName, 'run.gpx');
      expect(result.name, 'Morning ride');
      expect(result.description, 'Easy commute');
      expect(result.type, 'cycling');
      expect(result.points.length, 2);
      expect(result.elevationGainedMeters, closeTo(8.0, 0.001));
      expect(result.distanceMeters, greaterThan(0.0));
    });

    test('parseFile extracts timestamped KML gx track from KMZ', () {
      final result = RouteFileParser.parseFile(
        _kmzBytes('''
<kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2">
  <Document>
    <name>Archive route</name>
    <Placemark>
      <name>Evening run</name>
      <description>Progression effort</description>
      <gx:Track>
        <when>2026-05-26T18:00:00Z</when>
        <when>2026-05-26T18:01:00Z</when>
        <gx:coord>24.0000 59.0000 10.0</gx:coord>
        <gx:coord>24.0020 59.0010 22.0</gx:coord>
      </gx:Track>
    </Placemark>
  </Document>
</kml>
'''),
        fileName: 'run.kmz',
      );

      expect(result.fileName, 'run.kmz');
      expect(result.name, 'Evening run');
      expect(result.description, 'Progression effort');
      expect(result.points.length, 2);
      expect(result.elevationGainedMeters, closeTo(12.0, 0.001));
      expect(result.distanceMeters, greaterThan(0.0));
    });

    test('parseFile extracts timestamped FIT activity records and sport', () {
      final result = RouteFileParser.parseFile(
        _fitActivityBytes(
          sport: 2,
          points: [
            _FitTestPoint(
              time: DateTime.utc(2026, 5, 26, 8, 30),
              latitude: 59.0000,
              longitude: 24.0000,
              altitudeMeters: 10.0,
            ),
            _FitTestPoint(
              time: DateTime.utc(2026, 5, 26, 8, 31),
              latitude: 59.0010,
              longitude: 24.0020,
              altitudeMeters: 22.0,
            ),
          ],
        ),
        fileName: 'morning-ride.fit',
      );

      expect(result.fileName, 'morning-ride.fit');
      expect(result.type, 'cycling');
      expect(result.points.length, 2);
      expect(result.startTime, DateTime.utc(2026, 5, 26, 8, 30));
      expect(result.endTime, DateTime.utc(2026, 5, 26, 8, 31));
      expect(result.elevationGainedMeters, closeTo(12.0, 0.001));
      expect(result.distanceMeters, greaterThan(0.0));
    });

    test('parseFile imports FIT activity without GPS route', () {
      final result = RouteFileParser.parseFile(
        _fitActivityBytes(
          sport: 10,
          points: const [],
          sessionTime: DateTime.utc(2026, 5, 26, 8, 30),
          elapsedSeconds: 45 * 60,
          totalCaloriesKcal: 220,
        ),
        fileName: 'indoor-workout.fit',
      );

      expect(result.fileName, 'indoor-workout.fit');
      expect(result.type, 'training');
      expect(result.points, isEmpty);
      expect(result.originalPointCount, 0);
      expect(result.startTime, DateTime.utc(2026, 5, 26, 8, 30));
      expect(result.endTime, DateTime.utc(2026, 5, 26, 9, 15));

      // FIT session field 11 is `total_calories`. This test used to assert it came
      // out as ACTIVE calories, which is how the bug survived: the test was written
      // to match the code rather than the format, so it locked the swap in.
      //
      // The consequence was not cosmetic. Nothing filled `totalCalories`, so the
      // import form estimated one, and the estimate landed BELOW the total sitting
      // in the active field -- a real 511 kcal ride arrived as 511 active against an
      // estimated 376 total and refused to save: "Total calories cannot be lower
      // than active calories."
      expect(result.totalCaloriesKcal!, closeTo(220.0, 0.001));

      // And active stays UNKNOWN. The FIT session message has no active-calorie
      // field, so there is nothing to read; null is honest, a number would not be.
      expect(result.activeCaloriesKcal, isNull);
    });

    test('a FIT activity brings its heart rate, cadence and speed', () {
      // An imported activity had NO GRAPHS AT ALL. The parser read only lat/long/
      // altitude off each `record` message and threw away the heart rate, the
      // cadence and the speed sitting right beside them -- so the file arrived with
      // a route and nothing to plot.
      final result = RouteFileParser.parseFile(
        _fitActivityBytes(
          sport: 2, // cycling
          points: [
            _FitTestPoint(
              time: DateTime.utc(2026, 5, 26, 8, 30),
              latitude: 59.0000,
              longitude: 24.0000,
              altitudeMeters: 10.0,
              heartRateBpm: 132,
              cadence: 88,
              speedMetersPerSecond: 6.4,
            ),
            _FitTestPoint(
              time: DateTime.utc(2026, 5, 26, 8, 31),
              latitude: 59.0010,
              longitude: 24.0020,
              altitudeMeters: 22.0,
              heartRateBpm: 141,
              cadence: 92,
              speedMetersPerSecond: 7.1,
            ),
          ],
        ),
        fileName: 'ride.fit',
      );

      expect(result.bleSamples.heartRateSamples.map((s) => s.beatsPerMinute),
          [132, 141]);
      expect(result.bleSamples.speedSamples.map((s) => s.metersPerSecond),
          [closeTo(6.4, 0.001), closeTo(7.1, 0.001)]);

      // Cycling, so the cadence is PEDALLING cadence. Health Connect keeps step and
      // pedal cadence in different record types and FIT field 4 says only "cadence"
      // -- the sport is the only thing that can decide, and it is parsed after the
      // records, so the kind is resolved last.
      expect(result.bleSamples.cyclingCadenceSamples.map((s) => s.rpm), [88, 92]);
      expect(result.bleSamples.stepsCadenceSamples, isEmpty);
    });

    test('a RUNNING FIT file doubles the cadence into steps', () {
      // FIT reports running cadence as STRIDES per minute -- one leg. Health Connect
      // wants steps. A runner at 90 spm is taking 180 steps.
      final result = RouteFileParser.parseFile(
        _fitActivityBytes(
          sport: 1, // running
          points: [
            _FitTestPoint(
              time: DateTime.utc(2026, 5, 26, 8, 30),
              latitude: 59.0000,
              longitude: 24.0000,
              altitudeMeters: 10.0,
              cadence: 90,
            ),
          ],
          sessionTime: DateTime.utc(2026, 5, 26, 8, 30),
          elapsedSeconds: 600,
        ),
        fileName: 'run.fit',
      );

      expect(result.bleSamples.stepsCadenceSamples.single.stepsPerMinute, 180);
      expect(result.bleSamples.cyclingCadenceSamples, isEmpty);
    });

    test('an INDOOR FIT file with no GPS still brings its heart rate', () {
      // The old parser bailed out of the whole record the moment it found no
      // latitude -- so a turbo-trainer session, which has no GPS at all, arrived
      // with literally nothing. The heart rate is read BEFORE the position guard now.
      final result = RouteFileParser.parseFile(
        _fitActivityBytes(
          sport: 2,
          points: [
            _FitTestPoint(
              time: DateTime.utc(2026, 5, 26, 8, 30),
              latitude: 0.0, // no fix
              longitude: 0.0,
              altitudeMeters: 0.0,
              heartRateBpm: 128,
              cadence: 85,
            ),
          ],
          sessionTime: DateTime.utc(2026, 5, 26, 8, 30),
          elapsedSeconds: 1800,
        ),
        fileName: 'trainer.fit',
      );

      expect(result.points, isEmpty, reason: 'no usable GPS, as expected');
      expect(result.bleSamples.heartRateSamples.single.beatsPerMinute, 128);
      expect(result.bleSamples.cyclingCadenceSamples.single.rpm, 85);
    });

    test('parseFile imports FIT activity and ignores unusable one point route',
        () {
      final result = RouteFileParser.parseFile(
        _fitActivityBytes(
          sport: 2,
          points: [
            _FitTestPoint(
              time: DateTime.utc(2026, 5, 26, 8, 30),
              latitude: 59.0000,
              longitude: 24.0000,
              altitudeMeters: 10.0,
            ),
          ],
          sessionTime: DateTime.utc(2026, 5, 26, 8, 30),
          elapsedSeconds: 10 * 60,
          totalDistanceMeters: 2400.0,
        ),
        fileName: 'single-point.fit',
      );

      expect(result.type, 'cycling');
      expect(result.points, isEmpty);
      expect(result.originalPointCount, 1);
      expect(result.distanceMeters, closeTo(2400.0, 0.001));
      expect(result.endTime, DateTime.utc(2026, 5, 26, 8, 40));
    });

    test('parseFile imports FIT course as route without activity time range',
        () {
      final result = RouteFileParser.parseFile(
        _fitCourseBytes(
          name: 'Park Loop',
          sport: 2,
          points: [
            _FitTestPoint(
              time: DateTime.utc(2026, 5, 26, 8, 30),
              latitude: 59.0000,
              longitude: 24.0000,
              altitudeMeters: 10.0,
            ),
            _FitTestPoint(
              time: DateTime.utc(2026, 5, 26, 8, 35),
              latitude: 59.0010,
              longitude: 24.0020,
              altitudeMeters: 22.0,
            ),
          ],
          totalDistanceMeters: 2400.0,
          elapsedSeconds: 10 * 60,
        ),
        fileName: 'park-loop.fit',
      );

      expect(result.name, 'Park Loop');
      expect(result.type, 'cycling');
      expect(result.points.length, 2);
      expect(result.hasRecordedTimestamps, isFalse);
      expect(result.hasImportedTimeRange, isFalse);
      expect(result.distanceMeters, closeTo(2400.0, 0.001));
      expect(result.durationSeconds, 600);
    });

    test('parseFile imports sparse FIT course without route geometry', () {
      final result = RouteFileParser.parseFile(
        _fitCourseBytes(
          name: 'Tiny Course',
          sport: 11,
          points: [
            _FitTestPoint(
              time: DateTime.utc(2026, 5, 26, 8, 30),
              latitude: 59.0000,
              longitude: 24.0000,
              altitudeMeters: 10.0,
            ),
          ],
          totalDistanceMeters: 0.0,
          elapsedSeconds: 5 * 60,
        ),
        fileName: 'tiny-course.fit',
      );

      expect(result.name, 'Tiny Course');
      expect(result.type, 'walking');
      expect(result.points, isEmpty);
      expect(result.originalPointCount, 1);
      expect(result.hasRecordedTimestamps, isFalse);
      expect(result.hasImportedTimeRange, isFalse);
      expect(result.durationSeconds, 5 * 60);
    });

    test('parseFile imports FIT workout definition without activity session',
        () {
      final result = RouteFileParser.parseFile(
        _fitWorkoutBytes(
          name: 'Tempo Run',
          sport: 1,
          timeStepSeconds: const [10 * 60, 5 * 60],
        ),
        fileName: 'tempo-run.fit',
      );

      expect(result.name, 'Tempo Run');
      expect(result.type, 'running');
      expect(result.points, isEmpty);
      expect(result.hasRecordedTimestamps, isFalse);
      expect(result.hasImportedTimeRange, isFalse);
      expect(result.durationSeconds, 15 * 60);
    });

    test('parseFile extracts untimestamped KML line string with synthetic timing',
        () {
      final result = RouteFileParser.parseFile(
        Uint8List.fromList(utf8.encode('''
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <Placemark>
      <name>Manual route</name>
      <description>Imported path</description>
      <LineString>
        <coordinates>
          24.0000,59.0000,10.0
          24.0020,59.0010,22.0
          24.0040,59.0020,20.0
        </coordinates>
      </LineString>
    </Placemark>
  </Document>
</kml>
''')),
        fileName: 'route.kml',
      );

      expect(result.name, 'Manual route');
      expect(result.hasRecordedTimestamps, isFalse);
      expect(result.hasImportedTimeRange, isFalse);
      expect(result.points.length, 3);
      expect(result.distanceMeters, greaterThan(0.0));
    });

    test('parse simplifies very large route files', () {
      final start = DateTime.utc(2026, 5, 26, 8, 30);
      final buffer = StringBuffer();
      for (var index = 0; index < 2100; index++) {
        final time = start.add(Duration(seconds: index));
        buffer.writeln(
          '<trkpt lat="${59.0 + index * 0.00001}" lon="${24.0 + index * 0.00001}">'
          '<time>${_instantString(time)}</time></trkpt>',
        );
      }
      final result = RouteFileParser.parse(
        '<gpx version="1.1"><trk><trkseg>$buffer</trkseg></trk></gpx>',
        fileName: 'large.gpx',
      );

      expect(result.originalPointCount, 2100);
      expect(result.points.length, lessThan(result.originalPointCount));
      expect(result.points.length, 2000);
    });

    test('parse rejects GPX without two timestamped points', () {
      expect(
        () => RouteFileParser.parse(
          '<gpx version="1.1"><trk><trkseg>'
          '<trkpt lat="59.0" lon="24.0" /></trkseg></trk></gpx>',
        ),
        throwsA(isA<RouteImportException>()),
      );
    });

    test('parseFile rejects oversized raw route file before parsing', () {
      final failure = _captureRouteError(
        () => RouteFileParser.parseFile(
          Uint8List(maxRouteFileBytes + 1),
          fileName: 'large.gpx',
        ),
      );
      expect(failure, 'Activity file is too large.');
    });

    test('parseFile rejects oversized KMZ route entry before XML parsing', () {
      final failure = _captureRouteError(
        () => RouteFileParser.parseFile(_oversizedKmzBytes(), fileName: 'large.kmz'),
      );
      expect(failure, 'KMZ route entry is too large.');
    });
  });
}

String? _captureRouteError(void Function() body) {
  try {
    body();
    return null;
  } on RouteImportException catch (error) {
    return error.message;
  }
}

String _instantString(DateTime time) {
  final iso = time.toUtc().toIso8601String();
  // DateTime.toIso8601String() emits milliseconds (…08:30:00.000Z); trim to a
  // FIT-style instant (…08:30:00Z) that the parser also accepts.
  return iso.replaceFirst(RegExp(r'\.000Z$'), 'Z');
}

Uint8List _kmzBytes(String kmlText) {
  final archive = Archive()
    ..addFile(
      ArchiveFile('doc.kml', utf8.encode(kmlText).length, utf8.encode(kmlText)),
    );
  return Uint8List.fromList(ZipEncoder().encode(archive));
}

Uint8List _oversizedKmzBytes() {
  final content = Uint8List(maxKmzRouteEntryBytes + 1)
    ..fillRange(0, maxKmzRouteEntryBytes + 1, 'a'.codeUnitAt(0));
  final archive = Archive()..addFile(ArchiveFile('doc.kml', content.length, content));
  return Uint8List.fromList(ZipEncoder().encode(archive));
}

class _FitTestPoint {
  const _FitTestPoint({
    required this.time,
    required this.latitude,
    required this.longitude,
    required this.altitudeMeters,
    this.heartRateBpm,
    this.cadence,
    this.speedMetersPerSecond,
  });

  final DateTime time;
  final double latitude;
  final double longitude;
  final double altitudeMeters;

  /// FIT `record` fields 3, 4 and 6. The parser read none of them, so a FIT import
  /// arrived with a route and not one graph.
  final int? heartRateBpm;
  final int? cadence;
  final double? speedMetersPerSecond;

  int get altitudeRaw => ((altitudeMeters + 500.0) * 5.0).round();
}

class _FitField {
  const _FitField(this.number, this.size, this.baseType);
  final int number;
  final int size;
  final int baseType;
}

class _FitWriter {
  final BytesBuilder _bytes = BytesBuilder();

  void write(int value) => _bytes.addByte(value & 0xFF);

  void writeBytes(List<int> value) => _bytes.add(value);

  void writeUInt16(int value) {
    write(value & 0xFF);
    write((value >> 8) & 0xFF);
  }

  void writeUInt32(int value) {
    write(value & 0xFF);
    write((value >> 8) & 0xFF);
    write((value >> 16) & 0xFF);
    write((value >> 24) & 0xFF);
  }

  void writeNullableUInt32(int? value) => writeUInt32(value ?? 0xFFFFFFFF);

  void writeNullableUInt16(int? value) => writeUInt16(value ?? 0xFFFF);

  void writeInt32(int value) => writeUInt32(value & 0xFFFFFFFF);

  void writeFitString(String value, int size) {
    final source = utf8.encode(value);
    final output = Uint8List(size);
    final end = source.length < size - 1 ? source.length : size - 1;
    for (var i = 0; i < end; i++) {
      output[i] = source[i];
    }
    writeBytes(output);
  }

  void writeFitDefinition({
    required int localMessageType,
    required int globalMessageNumber,
    required List<_FitField> fields,
  }) {
    write(0x40 | localMessageType);
    write(0);
    write(0);
    writeUInt16(globalMessageNumber);
    write(fields.length);
    for (final field in fields) {
      write(field.number);
      write(field.size);
      write(field.baseType);
    }
  }

  void writeFitFileId(int fileType) {
    writeFitDefinition(
      localMessageType: 3,
      globalMessageNumber: 0,
      fields: const [_FitField(0, 1, 0)],
    );
    write(3);
    write(fileType);
  }

  Uint8List toBytes() => _bytes.toBytes();
}

int _fitTimestamp(DateTime time) => time.millisecondsSinceEpoch ~/ 1000 - 631065600;

int _semicircles(double value) => (value * 2147483648.0 / 180.0).round();

Uint8List _wrapFitFile(Uint8List dataBytes) {
  final writer = _FitWriter()
    ..write(14)
    ..write(16)
    ..writeUInt16(0)
    ..writeUInt32(dataBytes.length)
    ..writeBytes([0x2E, 0x46, 0x49, 0x54])
    ..writeUInt16(0)
    ..writeBytes(dataBytes)
    ..writeUInt16(0);
  return writer.toBytes();
}

Uint8List _fitActivityBytes({
  required int sport,
  required List<_FitTestPoint> points,
  DateTime? sessionTime,
  int? elapsedSeconds,
  double? totalDistanceMeters,
  int? totalCaloriesKcal,
  int? totalAscentMeters,
}) {
  final resolvedSessionTime = sessionTime ??
      (points.isNotEmpty ? points.first.time : DateTime.utc(2026, 5, 26, 8, 30));
  final resolvedElapsed = elapsedSeconds ??
      (points.isNotEmpty
          ? ((points.last.time.millisecondsSinceEpoch ~/ 1000) -
                  (resolvedSessionTime.millisecondsSinceEpoch ~/ 1000))
              .clamp(1, 1 << 62)
          : 60);

  final data = _FitWriter()..writeFitFileId(4);
  data.writeFitDefinition(
    localMessageType: 1,
    globalMessageNumber: 18,
    fields: const [
      _FitField(253, 4, 134),
      _FitField(2, 4, 134),
      _FitField(5, 1, 0),
      _FitField(7, 4, 134),
      _FitField(8, 4, 134),
      _FitField(9, 4, 134),
      _FitField(11, 2, 132),
      _FitField(21, 2, 132),
    ],
  );
  data.write(1);
  data.writeUInt32(
    _fitTimestamp(resolvedSessionTime.add(Duration(seconds: resolvedElapsed))),
  );
  data.writeUInt32(_fitTimestamp(resolvedSessionTime));
  data.write(sport);
  data.writeUInt32(resolvedElapsed * 1000);
  data.writeUInt32(resolvedElapsed * 1000);
  data.writeNullableUInt32(
    totalDistanceMeters == null ? null : (totalDistanceMeters * 100.0).round(),
  );
  data.writeNullableUInt16(totalCaloriesKcal);
  data.writeNullableUInt16(totalAscentMeters);

  data.writeFitDefinition(
    localMessageType: 0,
    globalMessageNumber: 20,
    fields: const [
      _FitField(253, 4, 134),
      _FitField(0, 4, 133),
      _FitField(1, 4, 133),
      _FitField(2, 2, 132),
      _FitField(3, 1, 2), // heart_rate, uint8
      _FitField(4, 1, 2), // cadence, uint8
      _FitField(6, 2, 132), // speed, uint16, mm/s
    ],
  );
  for (final point in points) {
    data.write(0);
    data.writeUInt32(_fitTimestamp(point.time));
    data.writeInt32(_semicircles(point.latitude));
    data.writeInt32(_semicircles(point.longitude));
    data.writeUInt16(point.altitudeRaw);
    data.write(point.heartRateBpm ?? 0xFF);
    data.write(point.cadence ?? 0xFF);
    data.writeNullableUInt16(
      point.speedMetersPerSecond == null
          ? null
          : (point.speedMetersPerSecond! * 1000.0).round(),
    );
  }

  return _wrapFitFile(data.toBytes());
}

Uint8List _fitCourseBytes({
  required String name,
  required int sport,
  required List<_FitTestPoint> points,
  required int elapsedSeconds,
  required double totalDistanceMeters,
}) {
  final lapStart =
      points.isNotEmpty ? points.first.time : DateTime.utc(2026, 5, 26, 8, 30);
  final data = _FitWriter()..writeFitFileId(6);

  data.writeFitDefinition(
    localMessageType: 1,
    globalMessageNumber: 31,
    fields: const [
      _FitField(4, 1, 0),
      _FitField(5, 32, 7),
    ],
  );
  data.write(1);
  data.write(sport);
  data.writeFitString(name, 32);

  data.writeFitDefinition(
    localMessageType: 2,
    globalMessageNumber: 19,
    fields: const [
      _FitField(253, 4, 134),
      _FitField(2, 4, 134),
      _FitField(7, 4, 134),
      _FitField(8, 4, 134),
      _FitField(9, 4, 134),
      _FitField(21, 2, 132),
    ],
  );
  data.write(2);
  data.writeUInt32(_fitTimestamp(lapStart.add(Duration(seconds: elapsedSeconds))));
  data.writeUInt32(_fitTimestamp(lapStart));
  data.writeUInt32(elapsedSeconds * 1000);
  data.writeUInt32(elapsedSeconds * 1000);
  data.writeUInt32((totalDistanceMeters * 100.0).round());
  data.writeUInt16(12);

  data.writeFitDefinition(
    localMessageType: 0,
    globalMessageNumber: 20,
    fields: const [
      _FitField(253, 4, 134),
      _FitField(0, 4, 133),
      _FitField(1, 4, 133),
      _FitField(2, 2, 132),
    ],
  );
  for (final point in points) {
    data.write(0);
    data.writeUInt32(_fitTimestamp(point.time));
    data.writeInt32(_semicircles(point.latitude));
    data.writeInt32(_semicircles(point.longitude));
    data.writeUInt16(point.altitudeRaw);
  }

  return _wrapFitFile(data.toBytes());
}

Uint8List _fitWorkoutBytes({
  required String name,
  required int sport,
  required List<int> timeStepSeconds,
}) {
  final data = _FitWriter()..writeFitFileId(5);

  data.writeFitDefinition(
    localMessageType: 1,
    globalMessageNumber: 26,
    fields: const [
      _FitField(4, 1, 0),
      _FitField(6, 2, 132),
      _FitField(8, 32, 7),
    ],
  );
  data.write(1);
  data.write(sport);
  data.writeUInt16(timeStepSeconds.length);
  data.writeFitString(name, 32);

  data.writeFitDefinition(
    localMessageType: 0,
    globalMessageNumber: 27,
    fields: const [
      _FitField(254, 2, 132),
      _FitField(1, 1, 0),
      _FitField(2, 4, 134),
    ],
  );
  for (var index = 0; index < timeStepSeconds.length; index++) {
    data.write(0);
    data.writeUInt16(index);
    data.write(0);
    data.writeUInt32(timeStepSeconds[index] * 1000);
  }

  return _wrapFitFile(data.toBytes());
}
