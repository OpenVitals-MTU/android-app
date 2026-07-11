/// Selectable in-app language. [languageTag] is null for "follow the system".
/// Applying the locale is a UI-layer concern (the Kotlin `LocaleListCompat`
/// helper is intentionally not ported into the domain layer).
///
/// This enum is also the definition of a SHIPPED locale: a language is offered
/// to users iff it has a constant here (and therefore an autonym in the
/// exhaustive switch in `lib/ui/components/app_language_dropdown.dart`). An ARB
/// catalog with no constant is IN PROGRESS — hosted in Weblate so translators
/// can work on it, structurally validated by `tool/verify_l10n.dart`, but never
/// offered to a user. See `docs/engineering/translations.md`.
enum AppLanguage {
  system(null),
  english('en'),
  spanish('es'),
  german('de'),
  italian('it'),
  estonian('et');

  const AppLanguage(this.languageTag);

  final String? languageTag;

  /// BCP-47 tags of every SHIPPED locale, in declaration order.
  ///
  /// [system] carries no tag (it defers to the platform) and is excluded. This
  /// — not `AppLocalizations.supportedLocales` — is what `MaterialApp` is
  /// given: gen-l10n derives its list from the ARB files that happen to be
  /// PRESENT, so an in-progress catalog would otherwise make the app claim a
  /// language it has barely translated, on a device with no way to pick another.
  static List<String> get shippedLanguageTags => <String>[
        for (final AppLanguage language in values)
          if (language.languageTag case final String tag) tag,
      ];
}
