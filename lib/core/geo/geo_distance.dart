import 'dart:math' as math;

const double _earthRadiusMeters = 6371000.0;

/// Great-circle (haversine) distance in meters between two WGS84 coordinates.
double haversineMeters(
  double startLatitude,
  double startLongitude,
  double endLatitude,
  double endLongitude,
) {
  final lat1 = _toRadians(startLatitude);
  final lat2 = _toRadians(endLatitude);
  final deltaLat = _toRadians(endLatitude - startLatitude);
  final deltaLon = _toRadians(endLongitude - startLongitude);
  final a = math.sin(deltaLat / 2.0) * math.sin(deltaLat / 2.0) +
      math.cos(lat1) *
          math.cos(lat2) *
          math.sin(deltaLon / 2.0) *
          math.sin(deltaLon / 2.0);
  final c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a));
  return _earthRadiusMeters * c;
}

double _toRadians(double degrees) => degrees * math.pi / 180.0;
