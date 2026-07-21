import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import 'domain/preferences/app_language.dart';
import 'domain/preferences/app_theme_mode.dart';
import 'l10n/app_localizations.dart';
import 'navigation/app_router.dart';
import 'state/app_providers.dart';
import 'ui/theme/app_theme.dart';

/// Root of the OpenVitals app — the Flutter analogue of the Kotlin
/// `MainActivity` + `OpenVitalsTheme` composition.
///
/// Watches the theme-mode / dynamic-colour / language providers so a settings
/// change rebuilds `MaterialApp.router` (and hence the whole tree) with the new
/// theme or locale, while the [GoRouter] instance itself is cached in
/// [goRouterProvider] so navigation state survives those rebuilds.
class OpenVitalsApp extends ConsumerWidget {
  const OpenVitalsApp({super.key});

  /// The locales the app OFFERS, derived from [AppLanguage] — deliberately NOT
  /// from `AppLocalizations.supportedLocales`.
  ///
  /// gen-l10n derives its list from the ARB files that are PRESENT, and
  /// `lib/l10n` also hosts IN-PROGRESS catalogs that Weblate is still filling
  /// in (see `docs/engineering/translations.md`). Handing that list to
  /// `MaterialApp` would let a 5%-translated locale win the platform-locale
  /// resolution below, giving that user a mostly-English UI *and* no way to
  /// choose otherwise, since the language picker only lists [AppLanguage]. A
  /// locale therefore reaches users only once it is shipped: a constant here,
  /// plus its autonym in `appLanguageLabel`.
  static final List<Locale> supportedLocales = <Locale>[
    for (final String tag in AppLanguage.shippedLanguageTags) Locale(tag),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(appThemeModeProvider);
    final dynamicColor = ref.watch(dynamicColorEnabledProvider);
    final language = ref.watch(appLanguageProvider);
    final router = ref.watch(goRouterProvider);

    // `DynamicColorBuilder` supplies the platform's Material You palettes when
    // available (Android 12+); the resolver falls back to the brand seed schemes
    // when they are absent or when the dynamic-colour preference is off.
    return DynamicColorBuilder(
      builder: (ColorScheme? lightDynamic, ColorScheme? darkDynamic) {
        final lightScheme = AppTheme.resolveScheme(
          brightness: Brightness.light,
          themeMode: themeMode,
          dynamicColor: dynamicColor,
          lightDynamic: lightDynamic,
          darkDynamic: darkDynamic,
        );
        final darkScheme = AppTheme.resolveScheme(
          brightness: Brightness.dark,
          themeMode: themeMode,
          dynamicColor: dynamicColor,
          lightDynamic: lightDynamic,
          darkDynamic: darkDynamic,
        );

        return MaterialApp.router(
          title: 'OpenVitals',
          debugShowCheckedModeBanner: false,
          theme: AppTheme.themeFrom(lightScheme),
          darkTheme: AppTheme.themeFrom(darkScheme),
          themeMode: _materialThemeMode(themeMode),
          // Localization (Phase 7). The generated [AppLocalizations] delegates
          // supply the app strings (ARB catalogs under `lib/l10n`) plus the
          // Material/Cupertino/Widgets delegates for framework strings. The
          // selected [AppLanguage] drives [locale]; `system` maps to null so the
          // platform locale wins, resolved against [supportedLocales].
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: supportedLocales,
          locale: _localeFor(language),
          // Point `Intl.defaultLocale` at the locale MaterialApp actually
          // resolved (including `system` → the platform locale), so every
          // DateFormat across the app localizes weekday/month names instead of
          // defaulting to en_US. Runs below Localizations, so localeOf resolves;
          // reruns on a language switch. Data for all locales is loaded in main().
          builder: (context, child) {
            Intl.defaultLocale = Localizations.localeOf(context).toString();
            return child ?? const SizedBox.shrink();
          },
          routerConfig: router,
        );
      },
    );
  }

  static ThemeMode _materialThemeMode(AppThemeMode mode) {
    switch (mode) {
      case AppThemeMode.system:
        return ThemeMode.system;
      case AppThemeMode.light:
        return ThemeMode.light;
      case AppThemeMode.dark:
      case AppThemeMode.amoled:
        // AMOLED is a dark variant baked into `darkTheme`, so force dark.
        return ThemeMode.dark;
    }
  }

  static Locale? _localeFor(AppLanguage language) {
    final tag = language.languageTag;
    return tag == null ? null : Locale(tag);
  }
}
