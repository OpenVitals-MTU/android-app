import 'package:flutter/material.dart';

import '../../domain/preferences/app_theme_mode.dart';
import 'app_colors.dart';
import 'app_theme.dart';

/// Port of the Kotlin `ui/theme/ActivityRecordingTheme.kt`: a high-contrast
/// theme for recording outdoors in direct sunlight — amber-on-black when the
/// app is dark, burnt-orange-on-white when it is light.

/// Kotlin `RecordingOutdoorDarkColorScheme`.
const ColorScheme recordingOutdoorDarkColorScheme = ColorScheme(
  brightness: Brightness.dark,
  primary: AppColors.recordingOutdoorAccent,
  onPrimary: Color(0xFF1A1A1A),
  primaryContainer: Color(0xFF3D2E00),
  onPrimaryContainer: AppColors.recordingOutdoorAccent,
  secondary: AppColors.recordingOutdoorAccentMuted,
  onSecondary: Colors.black,
  secondaryContainer: Color(0xFF3D2E00),
  onSecondaryContainer: AppColors.recordingOutdoorAccent,
  tertiary: AppColors.recordingOutdoorAccent,
  onTertiary: Color(0xFF1A1A1A),
  tertiaryContainer: Color(0xFF3D2E00),
  onTertiaryContainer: AppColors.recordingOutdoorAccent,
  error: Color(0xFFFFB4AB),
  onError: Color(0xFF690005),
  errorContainer: Color(0xFF93000A),
  onErrorContainer: Color(0xFFFFDAD6),
  surface: AppColors.recordingOutdoorSurface,
  onSurface: Colors.white,
  onSurfaceVariant: Color(0xFFBDBDBD),
  outline: Color(0xFF757575),
  outlineVariant: Color(0xFF424242),
  surfaceContainerLowest: AppColors.recordingOutdoorBackground,
  surfaceContainerLow: Color(0xFF0A0A0A),
  surfaceContainer: AppColors.recordingOutdoorSurface,
  surfaceContainerHigh: Color(0xFF1A1A1A),
  surfaceContainerHighest: Color(0xFF242424),
);

/// Kotlin `RecordingOutdoorLightColorScheme`.
const ColorScheme recordingOutdoorLightColorScheme = ColorScheme(
  brightness: Brightness.light,
  primary: AppColors.recordingOutdoorLightAccent,
  onPrimary: Colors.white,
  primaryContainer: Color(0xFFFFDBC8),
  onPrimaryContainer: Color(0xFF341100),
  secondary: AppColors.recordingOutdoorLightAccent,
  onSecondary: Colors.white,
  secondaryContainer: Color(0xFFFFDBC8),
  onSecondaryContainer: Color(0xFF341100),
  tertiary: AppColors.recordingOutdoorLightAccent,
  onTertiary: Colors.white,
  tertiaryContainer: Color(0xFFFFDBC8),
  onTertiaryContainer: Color(0xFF341100),
  error: Color(0xFFBA1A1A),
  onError: Colors.white,
  errorContainer: Color(0xFFFFDAD6),
  onErrorContainer: Color(0xFF410002),
  surface: Colors.white,
  onSurface: Colors.black,
  onSurfaceVariant: Color(0xFF424242),
  outline: Color(0xFF757575),
  outlineVariant: Color(0xFFBDBDBD),
  surfaceContainerLowest: Colors.white,
  surfaceContainerLow: Colors.white,
  surfaceContainer: Colors.white,
  surfaceContainerHigh: Colors.white,
  surfaceContainerHighest: Color(0xFFF5F5F5),
);

/// Whether the outdoor theme should use its light (black-on-white) scheme.
/// Kotlin: `outdoorModeEnabled && !appThemeMode.isDarkTheme(isSystemInDarkTheme())`.
bool recordingOutdoorUsesLightScheme(
  BuildContext context, {
  required bool outdoorModeEnabled,
  required AppThemeMode appThemeMode,
}) {
  final systemInDarkTheme =
      MediaQuery.platformBrightnessOf(context) == Brightness.dark;
  return outdoorModeEnabled && !appThemeMode.isDarkTheme(systemInDarkTheme);
}

/// Kotlin `MaterialTheme.colorScheme.background` under the outdoor theme:
/// pure black for the dark scheme, pure white for the light one.
Color recordingOutdoorBackgroundColor({required bool outdoorUsesLightScheme}) =>
    outdoorUsesLightScheme ? Colors.white : AppColors.recordingOutdoorBackground;

/// Kotlin `recordingOutdoorAccentForAppTheme(appThemeMode)`: the accent the
/// outdoor toggle takes on while enabled, resolved against the app theme (not
/// the outdoor theme itself).
Color recordingOutdoorAccentForAppTheme(
  BuildContext context,
  AppThemeMode appThemeMode,
) {
  final systemInDarkTheme =
      MediaQuery.platformBrightnessOf(context) == Brightness.dark;
  return appThemeMode.isDarkTheme(systemInDarkTheme)
      ? AppColors.recordingOutdoorAccent
      : AppColors.recordingOutdoorLightAccent;
}

/// Kotlin `ActivityRecordingTheme`: swaps the ambient [Theme] for the
/// high-contrast outdoor scheme while outdoor mode is enabled; otherwise the
/// child renders under the app theme untouched.
class ActivityRecordingTheme extends StatelessWidget {
  const ActivityRecordingTheme({
    super.key,
    required this.outdoorModeEnabled,
    required this.appThemeMode,
    required this.child,
  });

  final bool outdoorModeEnabled;
  final AppThemeMode appThemeMode;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    if (!outdoorModeEnabled) return child;
    final usesLightScheme = recordingOutdoorUsesLightScheme(
      context,
      outdoorModeEnabled: outdoorModeEnabled,
      appThemeMode: appThemeMode,
    );
    // `AppTheme.themeFrom` keeps the app typography, as the Kotlin theme keeps
    // `AppTypography`.
    return Theme(
      data: AppTheme.themeFrom(
        usesLightScheme
            ? recordingOutdoorLightColorScheme
            : recordingOutdoorDarkColorScheme,
      ),
      child: child,
    );
  }
}
