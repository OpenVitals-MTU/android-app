import 'package:flutter/material.dart';

/// The app's type scale. Two deliberate relationships to Material 3:
///
/// * **Weights are the brand and deviate upward** — headlines and titles are
///   heavier than M3's defaults (headlineLarge w700 vs w400, titleLarge w600
///   vs w400) because OpenVitals is numbers-first: the metric value is the
///   loudest thing on every screen.
/// * **Tracking follows M3 exactly.** The Compose port had silently dropped
///   Material's letter-spacing on the body and title styles (design-system
///   audit-1, F4); the values below restore `Typography.material2021`:
///   titleMedium +0.15, titleSmall +0.1, bodyLarge +0.5, bodyMedium +0.25,
///   bodySmall +0.4. Labels always carried theirs. Headlines track at 0 in M3,
///   so their absence of a `letterSpacing` is correct, not an omission.
///
/// Sizes are logical pixels; `height` is line-height as a multiple of size.
class AppTypography {
  const AppTypography._();

  static const TextTheme textTheme = TextTheme(
    headlineLarge: TextStyle(
      fontWeight: FontWeight.bold,
      fontSize: 32,
      height: 40 / 32,
    ),
    headlineMedium: TextStyle(
      fontWeight: FontWeight.w600,
      fontSize: 28,
      height: 36 / 28,
      fontFeatures: [FontFeature.tabularFigures()],
    ),
    headlineSmall: TextStyle(
      fontWeight: FontWeight.w600,
      fontSize: 24,
      height: 32 / 24,
    ),
    titleLarge: TextStyle(
      fontWeight: FontWeight.w600,
      fontSize: 22,
      height: 28 / 22,
    ),
    titleMedium: TextStyle(
      fontWeight: FontWeight.w500,
      fontSize: 16,
      height: 24 / 16,
      letterSpacing: 0.15,
    ),
    titleSmall: TextStyle(
      fontWeight: FontWeight.w500,
      fontSize: 14,
      height: 20 / 14,
      letterSpacing: 0.1,
    ),
    bodyLarge: TextStyle(
      fontWeight: FontWeight.normal,
      fontSize: 16,
      height: 24 / 16,
      letterSpacing: 0.5,
    ),
    bodyMedium: TextStyle(
      fontWeight: FontWeight.normal,
      fontSize: 14,
      height: 20 / 14,
      letterSpacing: 0.25,
    ),
    bodySmall: TextStyle(
      fontWeight: FontWeight.normal,
      fontSize: 12,
      height: 16 / 12,
      letterSpacing: 0.4,
    ),
    labelLarge: TextStyle(
      fontWeight: FontWeight.w500,
      fontSize: 14,
      height: 20 / 14,
      letterSpacing: 0.1,
    ),
    labelMedium: TextStyle(
      fontWeight: FontWeight.w500,
      fontSize: 12,
      height: 16 / 12,
      letterSpacing: 0.5,
    ),
    labelSmall: TextStyle(
      fontWeight: FontWeight.w500,
      fontSize: 11,
      height: 16 / 11,
      letterSpacing: 0.5,
    ),
  );
}
