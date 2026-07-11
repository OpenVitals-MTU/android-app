/// Selectable in-app language. [languageTag] is null for "follow the system".
/// Applying the locale is a UI-layer concern (the Kotlin `LocaleListCompat`
/// helper is intentionally not ported into the domain layer).
enum AppLanguage {
  system(null),
  english('en'),
  spanish('es'),
  german('de'),
  italian('it'),
  estonian('et');

  const AppLanguage(this.languageTag);

  final String? languageTag;
}
