import 'package:flutter/material.dart';

/// Brand and metric-accent colours, ported verbatim from the Kotlin
/// `ui/theme/Color.kt`. Values are ARGB ints; `Color(0xFF......)` keeps the
/// opaque alpha channel to match Compose's `Color(0xFF...)`.
class AppColors {
  const AppColors._();

  // ── Primary brand ─────────────────────────────────────────────────────────
  static const Color openVitalsBlue = Color(0xFF006E8F);
  static const Color openVitalsGreen = Color(0xFF2F6F4F);
  static const Color openVitalsCoral = Color(0xFF9B4438);

  static const Color blue80 = Color(0xFF82D2F2);
  static const Color blueGrey80 = Color(0xFFC6C7D0);
  static const Color teal80 = Color(0xFF80D7BE);

  static const Color blue40 = openVitalsBlue;
  static const Color blueGrey40 = Color(0xFF5E5F68);
  static const Color teal40 = openVitalsGreen;

  // ── Metric accent colours ───────────────────────────────────────────────
  static const Color steps = Color(0xFF4CAF50);
  static const Color distance = Color(0xFF2196F3);
  static const Color sleep = Color(0xFF673AB7);
  static const Color heart = Color(0xFFE91E63);
  static const Color vitals = Color(0xFFD32F2F);
  static const Color weight = Color(0xFFFF9800);
  static const Color calories = Color(0xFFF44336);
  static const Color hydration = Color(0xFF03A9F4);
  static const Color nutrition = Color(0xFF43A047);
  static const Color workout = Color(0xFF00BCD4);
  static const Color bodyFat = Color(0xFF795548);
  static const Color floors = Color(0xFFFFC107);
  static const Color activeCalories = Color(0xFFFF5722);
  static const Color elevation = Color(0xFF8BC34A);
  static const Color wheelchairPushes = Color(0xFF00897B);
  static const Color mindfulness = Color(0xFF8E6C8A);
  static const Color cycle = Color(0xFFC35B7A);

  // ── Surface variants ──────────────────────────────────────────────────────
  static const Color surfaceDark = Color(0xFF1A1C1E);
  static const Color surfaceContainerDark = Color(0xFF2B2D30);

  // ── Activity recording outdoor (sunlight) readability ──────────────────────
  static const Color recordingOutdoorAccent = Color(0xFFFFB300);
  static const Color recordingOutdoorLightAccent = Color(0xFFE65100);
  static const Color recordingOutdoorAccentMuted = Color(0xFFFF8F00);
  static const Color recordingOutdoorBackground = Color(0xFF000000);
  static const Color recordingOutdoorSurface = Color(0xFF121212);
}
