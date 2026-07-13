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
  //
  // Refreshed. These were the stock Material 2 500 swatches, ported verbatim from
  // the Kotlin app — #4CAF50, #2196F3, #E91E63 — which is the 2014 palette, and no
  // amount of gradient or animation makes a 2014 palette look like anything else.
  //
  // But they were not only dated, they were UNREADABLE, and that is what actually
  // forced this: measured against the app's own two surfaces, EIGHT of the
  // seventeen fell below the 3:1 contrast ratio WCAG asks of a graphical object.
  // `floors` (#FFC107, gold) scored 1.59:1 on the light surface — a chart line
  // that a sighted user has to hunt for and a low-vision user simply does not get.
  // `weight` 2.10, `elevation` 2.05, `workout` 2.24, `hydration` 2.57. The bright
  // ones were the worst, because bright is not the same as legible: a saturated
  // yellow is nearly as light as white.
  //
  // Every colour below now clears 3:1 against BOTH `scheme.surface` values, worst
  // case 3.09:1, while keeping the hue each metric has always had — steps are still
  // green, water is still blue, the heart is still rose. They are deeper and less
  // saturated, which is what a health chart wants: the line should read as a
  // measurement, not as a highlighter.
  static const Color steps = Color(0xFF3F9A63);
  static const Color distance = Color(0xFF3B7DD8);
  static const Color sleep = Color(0xFF6C5CD6);
  static const Color heart = Color(0xFFD2497B);
  static const Color vitals = Color(0xFFC4453E);
  static const Color weight = Color(0xFFBE7A2C);
  static const Color calories = Color(0xFFDD5F3E);
  static const Color hydration = Color(0xFF2E97C9);
  static const Color nutrition = Color(0xFF5C9E4B);
  static const Color workout = Color(0xFF2AA0A0);
  static const Color bodyFat = Color(0xFF8A6A55);
  static const Color floors = Color(0xFFA8881F);
  static const Color activeCalories = Color(0xFFDE6C39);
  static const Color elevation = Color(0xFF6E9440);
  static const Color wheelchairPushes = Color(0xFF2E8C7F);
  static const Color mindfulness = Color(0xFF8A6E9C);
  static const Color cycle = Color(0xFFBE5C85);

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
