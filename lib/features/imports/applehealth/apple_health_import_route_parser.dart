/// GPX workout-route parser, ported from the Kotlin
/// `AppleHealthImportRouteParser.kt`. Reads `<trkpt>`/`<rtept>` geometry with
/// altitude and accuracy children.
library;

import 'package:xml/xml_events.dart';

import 'apple_health_import_models.dart';

const String _appleWorkoutRoutesDirectory = 'workout-routes/';
const int _minAppleWorkoutRoutePoints = 2;
const double _placeholderAltitudeToleranceMeters = 0.1;
const Set<String> _pointTags = {'trkpt', 'rtept'};
const Set<String> _pointChildTags = {'ele', 'hAcc', 'vAcc'};

String normalizedAppleWorkoutRoutePath(String path) {
  final normalized = path.replaceAll('\\', '/').trim().replaceAll(RegExp(r'^/+'), '');
  final routeIndex = normalized.indexOf(_appleWorkoutRoutesDirectory);
  final result = routeIndex >= 0
      ? normalized.substring(routeIndex)
      : normalized.substring(normalized.lastIndexOf('/') + 1);
  return result.toLowerCase();
}

class AppleHealthImportRouteParser {
  const AppleHealthImportRouteParser._();

  static AppleWorkoutRouteFile? parse(String path, String gpx) {
    final handler = _RouteGpxHandler(normalizedAppleWorkoutRoutePath(path));
    for (final event in parseEvents(gpx)) {
      if (event is XmlStartElementEvent) {
        handler.startElement(event.name, event.attributes);
        if (event.isSelfClosing) handler.endElement(event.name);
      } else if (event is XmlEndElementEvent) {
        handler.endElement(event.name);
      } else if (event is XmlTextEvent) {
        handler.characters(event.value);
      } else if (event is XmlCDATAEvent) {
        handler.characters(event.value);
      }
    }
    return handler.routeFile();
  }
}

String _localName(String name) {
  final index = name.lastIndexOf(':');
  return index < 0 ? name : name.substring(index + 1);
}

class _MutableRoutePoint {
  _MutableRoutePoint({required this.latitude, required this.longitude});

  final double? latitude;
  final double? longitude;
  double? altitudeMeters;
  double? horizontalAccuracyMeters;
  double? verticalAccuracyMeters;
}

class _RouteGpxHandler {
  _RouteGpxHandler(this.path);

  final String path;
  final List<_MutableRoutePoint> _points = [];
  final StringBuffer _text = StringBuffer();
  _MutableRoutePoint? _currentPoint;
  String? _currentElement;

  void startElement(String rawName, List<XmlEventAttribute> attributes) {
    final name = _localName(rawName);
    if (_pointTags.contains(name)) {
      _currentPoint = _MutableRoutePoint(
        latitude: _attr(attributes, 'lat')
            ?.let((v) => double.tryParse(v))
            ?.let((v) => v >= -90.0 && v <= 90.0 ? v : null),
        longitude: _attr(attributes, 'lon')
            ?.let((v) => double.tryParse(v))
            ?.let((v) => v >= -180.0 && v <= 180.0 ? v : null),
      );
    }
    _currentElement = name;
    _text.clear();
  }

  void characters(String value) {
    if (_currentPoint != null && _pointChildTags.contains(_currentElement)) {
      _text.write(value);
    }
  }

  void endElement(String rawName) {
    final name = _localName(rawName);
    final point = _currentPoint;
    if (point != null) {
      final value = double.tryParse(_text.toString().trim());
      switch (name) {
        case 'ele':
          point.altitudeMeters = value;
        case 'hAcc':
          point.horizontalAccuracyMeters =
              (value != null && value > 0.0) ? value : null;
        case 'vAcc':
          point.verticalAccuracyMeters =
              (value != null && value > 0.0) ? value : null;
      }
    }
    if (_pointTags.contains(name)) {
      if (point != null && point.latitude != null && point.longitude != null) {
        _points.add(point);
      }
      _currentPoint = null;
    }
    _currentElement = null;
    _text.clear();
  }

  AppleWorkoutRouteFile? routeFile() {
    final hasUsefulAltitude = _points.any((point) {
      final altitude = point.altitudeMeters;
      return altitude != null &&
          altitude.abs() > _placeholderAltitudeToleranceMeters;
    });
    final immutablePoints = <AppleWorkoutRoutePoint>[];
    for (final point in _points) {
      final latitude = point.latitude;
      final longitude = point.longitude;
      if (latitude == null || longitude == null) continue;
      immutablePoints.add(AppleWorkoutRoutePoint(
        latitude: latitude,
        longitude: longitude,
        altitudeMeters: hasUsefulAltitude ? point.altitudeMeters : null,
        horizontalAccuracyMeters: point.horizontalAccuracyMeters,
        verticalAccuracyMeters: point.verticalAccuracyMeters,
      ));
    }
    if (immutablePoints.length < _minAppleWorkoutRoutePoints) return null;
    return AppleWorkoutRouteFile(path: path, points: immutablePoints);
  }
}

String? _attr(List<XmlEventAttribute> attributes, String name) {
  for (final attribute in attributes) {
    if (attribute.name == name) {
      final value = attribute.value;
      return value.trim().isEmpty ? null : value;
    }
  }
  return null;
}

extension _Let<T> on T {
  R let<R>(R Function(T) block) => block(this);
}
