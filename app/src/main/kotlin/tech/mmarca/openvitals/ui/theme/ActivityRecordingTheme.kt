package tech.mmarca.openvitals.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.isDarkTheme

val LocalActivityRecordingOutdoorMode = compositionLocalOf { false }

val LocalActivityRecordingOutdoorLightScheme = compositionLocalOf { false }

val RecordingOutdoorDarkColorScheme = darkColorScheme(
    primary = RecordingOutdoorAccent,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF3D2E00),
    onPrimaryContainer = RecordingOutdoorAccent,
    secondary = RecordingOutdoorAccentMuted,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3D2E00),
    onSecondaryContainer = RecordingOutdoorAccent,
    tertiary = RecordingOutdoorAccent,
    onTertiary = Color(0xFF1A1A1A),
    tertiaryContainer = Color(0xFF3D2E00),
    onTertiaryContainer = RecordingOutdoorAccent,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = RecordingOutdoorBackground,
    onBackground = Color.White,
    surface = RecordingOutdoorSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF424242),
    surfaceContainerLowest = RecordingOutdoorBackground,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = RecordingOutdoorSurface,
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF242424),
)

val RecordingOutdoorLightColorScheme = lightColorScheme(
    primary = RecordingOutdoorLightAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF341100),
    secondary = RecordingOutdoorLightAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC8),
    onSecondaryContainer = Color(0xFF341100),
    tertiary = RecordingOutdoorLightAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBC8),
    onTertiaryContainer = Color(0xFF341100),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF424242),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFBDBDBD),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF5F5F5),
)

@Composable
fun ActivityRecordingTheme(
    outdoorModeEnabled: Boolean,
    appThemeMode: AppThemeMode,
    content: @Composable () -> Unit,
) {
    val outdoorUsesLightScheme = outdoorModeEnabled &&
        !appThemeMode.isDarkTheme(isSystemInDarkTheme())
    CompositionLocalProvider(
        LocalActivityRecordingOutdoorMode provides outdoorModeEnabled,
        LocalActivityRecordingOutdoorLightScheme provides outdoorUsesLightScheme,
    ) {
        if (outdoorModeEnabled) {
            MaterialTheme(
                colorScheme = if (outdoorUsesLightScheme) {
                    RecordingOutdoorLightColorScheme
                } else {
                    RecordingOutdoorDarkColorScheme
                },
                typography = AppTypography,
                shapes = MaterialTheme.shapes,
                content = content,
            )
        } else {
            content()
        }
    }
}

@Composable
fun activityRecordingAccentColor(): Color =
    when {
        !LocalActivityRecordingOutdoorMode.current -> WorkoutColor
        LocalActivityRecordingOutdoorLightScheme.current -> RecordingOutdoorLightAccent
        else -> RecordingOutdoorAccent
    }

@Composable
fun recordingOutdoorAccentForAppTheme(appThemeMode: AppThemeMode): Color =
    if (appThemeMode.isDarkTheme(isSystemInDarkTheme())) {
        RecordingOutdoorAccent
    } else {
        RecordingOutdoorLightAccent
    }
