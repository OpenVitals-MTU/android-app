/// Recognises a wrist smartwatch (WearOS or otherwise) by its advertised
/// Bluetooth name, so the device list can present it as a smartwatch rather than
/// a generic heart-rate sensor.
///
/// **Presentation only.** A smartwatch discovered this way is still handled on
/// the live BLE-sensor path — it streams heart rate like any GATT sensor — and
/// is NOT a Garmin GFDI sync watch (`BleDeviceKind.watch`). Its all-day data
/// (sleep, HRV, steps) reaches the app through Health Connect, not here. See
/// docs/reference/wearos-phase3-decision.md.
library;

/// Name fragments that mark a device as a wrist smartwatch. Garmin sync watches
/// never reach here — they are classified as `BleDeviceKind.watch` upstream — so
/// this is the fallback for smartwatches the app treats as live sensors.
final List<RegExp> _smartwatchFamilies = [
  RegExp(r'galaxy\s*watch', caseSensitive: false),
  RegExp(r'pixel\s*watch', caseSensitive: false),
  RegExp(r'ticwatch', caseSensitive: false),
  RegExp(r'\bwatch\b', caseSensitive: false),
  RegExp(r'\bwear\s*os\b', caseSensitive: false),
  RegExp(r'amazfit', caseSensitive: false),
];

/// True when [name] looks like a wrist smartwatch. Presentational — a false
/// positive only swaps a sensor's icon, never its behaviour.
bool isSmartwatchName(String? name) {
  if (name == null) return false;
  final trimmed = name.trim();
  if (trimmed.isEmpty) return false;
  return _smartwatchFamilies.any((pattern) => pattern.hasMatch(trimmed));
}
