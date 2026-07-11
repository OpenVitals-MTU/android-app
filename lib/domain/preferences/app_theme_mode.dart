enum AppThemeMode {
  system,
  light,
  dark,
  amoled,
}

extension AppThemeModeDarkness on AppThemeMode {
  bool isDarkTheme(bool systemInDarkTheme) {
    switch (this) {
      case AppThemeMode.system:
        return systemInDarkTheme;
      case AppThemeMode.light:
        return false;
      case AppThemeMode.dark:
      case AppThemeMode.amoled:
        return true;
    }
  }
}
