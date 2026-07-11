import 'package:flutter/foundation.dart';

/// Whether the diagnostics UI (the Settings "Debug diagnostics" section, its
/// route, and the sanitized-logs share/save actions) is compiled into this
/// build.
///
/// Dart analogue of the Kotlin `BuildConfig.OPENVITALS_DIAGNOSTICS` flag
/// (`app/build.gradle.kts`), which Gradle sets to `true` for the **debug**,
/// **ci** and **nightly** build types — deliberately *not* `BuildConfig.DEBUG`.
///
/// The Flutter equivalent of a non-debug build type that still wants
/// diagnostics is a release build with a `--dart-define`, so:
///
/// * a debug build (`flutter run`, `flutter test`) is diagnostics-enabled via
///   [kDebugMode], exactly as before;
/// * a nightly/CI build is a plain **release** build that CI compiles with
///   `--dart-define=OPENVITALS_DIAGNOSTICS=true`;
/// * a store release passes nothing, so this stays `false` and the section,
///   route and card are hidden — and, because the whole expression is a
///   compile-time constant, Dart's tree shaker drops the guarded code entirely
///   instead of shipping dead diagnostics paths.
///
/// Keeping this a `const` (rather than reading `Platform.environment` or a
/// runtime provider) is what makes that tree-shaking possible: `kDebugMode` is
/// a `const bool`, and `bool.fromEnvironment` is implicitly `const` inside a
/// `const` variable's initializer, so `||` folds at compile time.
const bool kDiagnosticsEnabled =
    kDebugMode || bool.fromEnvironment('OPENVITALS_DIAGNOSTICS');
