/// The subset of Garmin's `FileType.FILETYPE` table this app cares about.
///
/// Gadgetbridge enumerates ~90 file types; OpenVitals only needs the FIT files
/// its existing importer can turn into Health Connect records, plus the two
/// virtual types the transport itself uses (the root DIRECTORY, and DEVICE_XML).
/// Every FIT file has data type 128; the sub-type distinguishes them.
///
/// Ported from `FileType.java` (AGPLv3). The `wanted` flag is narrower than
/// Gadgetbridge's `pull`: it marks only the types
/// `fit_wellness_import.dart` / `FitRouteParser` can actually consume, so a sync
/// does not spend airtime pulling golf scorecards it would only skip.
enum GarminFileType {
  /// The root directory listing (file index 0). Not a FIT file.
  directory(0, 0, wanted: false),

  /// Per-device metadata XML (file index 0xFFFD). Not pulled by the sync.
  deviceXml(8, 255, wanted: false),

  /// Recorded activity/exercise session — the exercise import path.
  activity(128, 4),

  /// Intra-day monitoring (steps, HR, respiration, calories) — the three
  /// sub-types the watch may split it across.
  monitorA(128, 15),
  monitorDaily(128, 28),
  monitor(128, 32),

  /// Sleep session with stages.
  sleep(128, 49),

  /// Fitness metrics: VO2 max, recovery time, training readiness and load.
  ///
  /// The watch keeps these listed and re-offers them every sync — they were
  /// being skipped as an unrecognised type long after the transport worked.
  metrics(128, 44),

  /// HRV status readings.
  hrvStatus(128, 68),

  /// Health Snapshot: a two-minute on-demand recording of SpO2, stress,
  /// respiration and Body Battery, each as packed sample arrays.
  ///
  /// Only written when the wearer runs Health Snapshot on the watch, so an
  /// empty directory here means "none recorded", not "not supported".
  hsa(128, 70);

  const GarminFileType(this.dataType, this.subType, {this.wanted = true});

  final int dataType;
  final int subType;

  /// Whether the sync should download this type. False for the virtual types,
  /// which are handled by the transport itself rather than imported.
  final bool wanted;

  /// The type for a directory entry's `(dataType, subType)`, or null when it is
  /// one this app does not handle — which the caller skips, exactly as the bulk
  /// importer skips an unmappable FIT file (skipped, not failed).
  static GarminFileType? fromCodes(int dataType, int subType) {
    for (final type in values) {
      if (type.dataType == dataType && type.subType == subType) return type;
    }
    return null;
  }
}
