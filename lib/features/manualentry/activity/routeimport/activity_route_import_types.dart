/// Port of the Kotlin `routeimport/ActivityRouteImportTypes.kt`.
///
/// The MIME types the system file picker is filtered by. `application/xml`,
/// `application/zip` and `application/octet-stream` are in the list because
/// providers label GPX, KMZ and FIT files inconsistently — a strict filter hides
/// files the parsers handle perfectly well.
library;

/// Kotlin `RouteImportMimeTypes`.
const List<String> kRouteImportMimeTypes = [
  'application/gpx',
  'application/gpx+xml',
  'application/vnd.google-earth.kml+xml',
  'application/vnd.google-earth.kmz',
  'application/vnd.google-earth.kmz+xml',
  'application/xml',
  'text/xml',
  'application/zip',
  'application/x-zip-compressed',
  'application/octet-stream',
];

/// Kotlin `FitImportMimeTypes`.
const List<String> kFitImportMimeTypes = [
  'application/vnd.ant.fit',
  'application/vnd.garmin.fit',
  'application/fit',
  'application/x-fit',
  'application/octet-stream',
  '*/*',
];
