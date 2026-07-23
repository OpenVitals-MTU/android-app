import 'package:flutter/foundation.dart';

/// Protocol logging for the Garmin stack — DEBUG BUILDS ONLY.
///
/// `debugPrint` is not stripped from release builds; the name suggests
/// otherwise and it is the reason this exists. What this layer logs is a watch's
/// Bluetooth address, its name and firmware, the contents of its settings
/// screens — alarm times, profile rows — and raw protocol dumps. None of that
/// belongs in a shipped app's logcat, where it survives in bug reports.
///
/// Errors reach the person using the app through the UI, which is where they can
/// actually be seen. This is for the developer holding the watch.
void garminLog(String message) {
  if (kDebugMode) debugPrint(message);
}

/// The same, for a message that is expensive to BUILD.
///
/// A hex dump was being formatted for every frame the sync did not recognise,
/// allocating the string on the main isolate before `debugPrint` decided whether
/// anyone wanted it. Passing the work as a closure means a release build never
/// does it at all.
void garminLogLazy(String Function() message) {
  if (kDebugMode) debugPrint(message());
}
