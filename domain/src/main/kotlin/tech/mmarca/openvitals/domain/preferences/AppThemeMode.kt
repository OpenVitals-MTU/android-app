package tech.mmarca.openvitals.domain.preferences

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}

fun AppThemeMode.isDarkTheme(systemInDarkTheme: Boolean): Boolean =
    when (this) {
        AppThemeMode.SYSTEM -> systemInDarkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK,
        AppThemeMode.AMOLED -> true
    }
