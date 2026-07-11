import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';

import '../../domain/preferences/app_theme_mode.dart';
import 'app_colors.dart';
import 'app_typography.dart';

/// Material 3 theme, ported from the Kotlin `ui/theme/Theme.kt`.
///
/// Provides the seed-based light/dark [ColorScheme]s (brand tokens copied
/// verbatim from the Kotlin `lightColorScheme`/`darkColorScheme`), the AMOLED
/// pure-black transform, and [ThemeData] builders. Dynamic colour is resolved
/// at the widget layer via `DynamicColorBuilder`; this class exposes the
/// fallback schemes and the resolver ([resolveScheme]) that folds the
/// dynamic/seed choice together with the AMOLED transform.
class AppTheme {
  const AppTheme._();

  // ── Fallback (seed brand) schemes ───────────────────────────────────────────
  static const ColorScheme darkScheme = ColorScheme(
    brightness: Brightness.dark,
    primary: AppColors.blue80,
    onPrimary: Color(0xFF003547),
    primaryContainer: Color(0xFF004E66),
    onPrimaryContainer: Color(0xFFBDEAFF),
    secondary: AppColors.blueGrey80,
    onSecondary: Color(0xFF30313A),
    secondaryContainer: Color(0xFF474852),
    onSecondaryContainer: Color(0xFFE2E1EC),
    tertiary: AppColors.teal80,
    onTertiary: Color(0xFF00382B),
    tertiaryContainer: Color(0xFF00513F),
    onTertiaryContainer: Color(0xFF9CF3D9),
    error: Color(0xFFFFB4AB),
    onError: Color(0xFF690005),
    errorContainer: Color(0xFF93000A),
    onErrorContainer: Color(0xFFFFDAD6),
    surface: AppColors.surfaceDark,
    onSurface: Color(0xFFE0E3E6),
    onSurfaceVariant: Color(0xFFC0C8CE),
    outline: Color(0xFF8A9298),
    outlineVariant: Color(0xFF40484D),
    surfaceContainerLowest: Color(0xFF0B0F11),
    surfaceContainerLow: Color(0xFF15191B),
    surfaceContainer: AppColors.surfaceContainerDark,
    surfaceContainerHigh: Color(0xFF303437),
    surfaceContainerHighest: Color(0xFF3B3F42),
  );

  static const ColorScheme lightScheme = ColorScheme(
    brightness: Brightness.light,
    primary: AppColors.blue40,
    onPrimary: Colors.white,
    primaryContainer: Color(0xFFC3E8FF),
    onPrimaryContainer: Color(0xFF001F2A),
    secondary: AppColors.blueGrey40,
    onSecondary: Colors.white,
    secondaryContainer: Color(0xFFE2E1EC),
    onSecondaryContainer: Color(0xFF1B1B23),
    tertiary: AppColors.teal40,
    onTertiary: Colors.white,
    tertiaryContainer: Color(0xFF9CF3D9),
    onTertiaryContainer: Color(0xFF002117),
    error: Color(0xFFBA1A1A),
    onError: Colors.white,
    errorContainer: Color(0xFFFFDAD6),
    onErrorContainer: Color(0xFF410002),
    surface: Color(0xFFFCFCFF),
    onSurface: Color(0xFF1A1C1E),
    onSurfaceVariant: Color(0xFF40484D),
    outline: Color(0xFF70787E),
    outlineVariant: Color(0xFFC0C8CE),
    surfaceContainerLowest: Colors.white,
    surfaceContainerLow: Color(0xFFF4F6F8),
    surfaceContainer: Color(0xFFEFF1F4),
    surfaceContainerHigh: Color(0xFFE9ECEF),
    surfaceContainerHighest: Color(0xFFE3E6E9),
  );

  /// Kotlin `ColorScheme.toAmoledColorScheme()`: pure-black background/surface
  /// with near-black surface containers, for OLED battery savings.
  static ColorScheme toAmoled(ColorScheme base) => base.copyWith(
        surface: Colors.black,
        surfaceContainerLowest: Colors.black,
        surfaceContainerLow: const Color(0xFF030303),
        surfaceContainer: const Color(0xFF080808),
        surfaceContainerHigh: const Color(0xFF101010),
        surfaceContainerHighest: const Color(0xFF181818),
        outlineVariant: const Color(0xFF3A3A3A),
      );

  /// Folds the dynamic/seed choice and the AMOLED transform into the final
  /// [ColorScheme] for the requested [brightness].
  ///
  /// [lightDynamic]/[darkDynamic] come from `DynamicColorBuilder`; they are used
  /// only when [dynamicColor] is enabled and the platform supplied them,
  /// otherwise the brand seed schemes are used (matching the Kotlin fallback to
  /// `DarkColorScheme`/`LightColorScheme`).
  static ColorScheme resolveScheme({
    required Brightness brightness,
    required AppThemeMode themeMode,
    required bool dynamicColor,
    ColorScheme? lightDynamic,
    ColorScheme? darkDynamic,
  }) {
    final bool useDynamic = dynamicColor &&
        lightDynamic != null &&
        darkDynamic != null;
    final ColorScheme base;
    if (brightness == Brightness.dark) {
      base = useDynamic ? darkDynamic.harmonized() : darkScheme;
    } else {
      base = useDynamic ? lightDynamic.harmonized() : lightScheme;
    }
    return themeMode == AppThemeMode.amoled && brightness == Brightness.dark
        ? toAmoled(base)
        : base;
  }

  /// Builds a [ThemeData] for the given [scheme]. AMOLED behaviour is already
  /// baked into the scheme via [resolveScheme]/[toAmoled], so the scaffold
  /// background simply follows `scheme.surface`.
  static ThemeData themeFrom(ColorScheme scheme) => ThemeData(
        useMaterial3: true,
        colorScheme: scheme,
        scaffoldBackgroundColor: scheme.surface,
        textTheme: AppTypography.textTheme.apply(
          bodyColor: scheme.onSurface,
          displayColor: scheme.onSurface,
        ),
      );
}
