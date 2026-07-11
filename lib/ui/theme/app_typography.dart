import 'package:flutter/material.dart';

/// Port of the Kotlin `ui/theme/Type.kt` `AppTypography`.
///
/// Compose `Typography` slots map onto Flutter's [TextTheme] slots one-to-one
/// (headlineLarge, titleMedium, bodySmall, labelLarge, …). Font sizes are given
/// in logical pixels (Compose `sp` ≈ Flutter logical px at default scaling) and
/// `lineHeight` becomes [TextStyle.height] expressed as a multiple of fontSize.
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
    ),
    titleSmall: TextStyle(
      fontWeight: FontWeight.w500,
      fontSize: 14,
      height: 20 / 14,
    ),
    bodyLarge: TextStyle(
      fontWeight: FontWeight.normal,
      fontSize: 16,
      height: 24 / 16,
    ),
    bodyMedium: TextStyle(
      fontWeight: FontWeight.normal,
      fontSize: 14,
      height: 20 / 14,
    ),
    bodySmall: TextStyle(
      fontWeight: FontWeight.normal,
      fontSize: 12,
      height: 16 / 12,
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
