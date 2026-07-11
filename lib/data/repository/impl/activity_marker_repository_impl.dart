import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../../../domain/model/activity_models.dart';
import '../contract/activity_repository.dart';

/// Port of the Kotlin `ActivityMarkerRepository` — a SharedPreferences-backed
/// per-activity marker store (not Health Connect). Encoding mirrors the Kotlin
/// newline/comma-separated, Base64url layout.
class ActivityMarkerRepositoryImpl implements ActivityMarkerRepository {
  ActivityMarkerRepositoryImpl(this._prefs);

  final SharedPreferences _prefs;

  String _key(String activityId) => 'activity_markers_$activityId';

  @override
  List<ActivityRecordingMarker> markersForActivity(String activityId) {
    final raw = _prefs.getString(_key(activityId));
    if (raw == null || raw.isEmpty) return const [];
    final markers = <ActivityRecordingMarker>[];
    for (final line in const LineSplitter().convert(raw)) {
      final parts = line.split(',');
      if (parts.length < 8) continue;
      final id = parts[0];
      final millis = int.tryParse(parts[1]);
      final latitude = double.tryParse(parts[2]);
      final longitude = double.tryParse(parts[3]);
      if (id.isEmpty || millis == null || latitude == null || longitude == null) {
        continue;
      }
      markers.add(
        ActivityRecordingMarker(
          id: id,
          time: DateTime.fromMillisecondsSinceEpoch(millis, isUtc: true),
          latitude: latitude,
          longitude: longitude,
          altitudeMeters: double.tryParse(parts[4]),
          name: _decode(parts[5]).isEmpty ? 'Marker' : _decode(parts[5]),
          note: _decode(parts[6]),
          type: _decode(parts[7]).isEmpty
              ? ActivityRecordingMarkerType.generic.value
              : _decode(parts[7]),
        ),
      );
    }
    markers.sort((a, b) => a.time.compareTo(b.time));
    return markers;
  }

  @override
  void setMarkersForActivity(
    String activityId,
    List<ActivityRecordingMarker> markers,
  ) {
    if (activityId.trim().isEmpty || markers.isEmpty) {
      _prefs.remove(_key(activityId));
      return;
    }
    final encoded = markers
        .map(
          (m) => [
            m.id,
            m.time.millisecondsSinceEpoch.toString(),
            m.latitude.toString(),
            m.longitude.toString(),
            m.altitudeMeters?.toString() ?? '',
            _encode(m.name),
            _encode(m.note),
            _encode(m.type),
          ].join(','),
        )
        .join('\n');
    _prefs.setString(_key(activityId), encoded);
  }

  @override
  void deleteMarkersForActivity(String activityId) {
    if (activityId.trim().isEmpty) return;
    _prefs.remove(_key(activityId));
  }

  String _encode(String value) =>
      base64Url.encode(utf8.encode(value)).replaceAll('=', '');

  String _decode(String value) {
    if (value.isEmpty) return '';
    try {
      final padded = value.padRight((value.length + 3) & ~3, '=');
      return utf8.decode(base64Url.decode(padded));
    } catch (_) {
      return '';
    }
  }
}
