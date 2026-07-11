# Translations

OpenVitals uses Flutter ARB catalogs for app UI translations. Codeberg Translate
is the preferred place for community translation work.

Translate the app here:

[translate.codeberg.org/projects/openvitals/android-app](https://translate.codeberg.org/projects/openvitals/android-app/)

## Where The Strings Live

- `lib/l10n/app_en.arb` is the **template and source language**.
- `lib/l10n/app_de.arb`, `app_es.arb`, `app_it.arb`, `app_et.arb` are the translations.
- `lib/l10n/app_localizations*.dart` are **generated** by `flutter gen-l10n` from
  those ARB files (config: `l10n.yaml`). They are committed, and CI fails if they
  are stale.

The ARB catalogs are the source of truth. Weblate edits them directly, so a hand
edit and a Weblate edit are the same kind of change.

> The Kotlin-era generator `tool/xml_to_arb.dart` is **gone and must not be
> resurrected.** It rebuilt the ARBs from the Android `strings.xml`, which would
> now silently destroy every translation Weblate has landed since that snapshot.
> Strings come from `app_en.arb` and nowhere else.

## Adding Or Changing A String

1. Add or edit the key in `lib/l10n/app_en.arb` (plus its `@key` metadata when it
   takes placeholders).
2. Do **not** hand-translate the other locales; leave that to Weblate. A missing
   key falls back to English.
3. Regenerate and commit the generated Dart:

   ```bash
   flutter gen-l10n
   ```

4. Run the gate:

   ```bash
   dart run tool/verify_l10n.dart
   ```

## Codeberg Translate Setup

One Weblate component for the app UI:

- Project: `OpenVitals`
- Component: `App`
- Repository: the Codeberg `OpenVitals/android-app` repository
- VCS mode: `Gitea pull request`
- File format: `ARB file`
- File mask: `lib/l10n/app_*.arb`
- Monolingual base language file: `lib/l10n/app_en.arb`
- Source language: English
- Edit base file: disabled
- Flags: `icu-message-format`
- License: `AGPL-3.0-or-later`

Add the Codeberg repository webhook target:

```text
https://translate.codeberg.org/hooks/gitea
```

The component intentionally excludes Fastlane metadata, release notes, and docs.
Add separate components later if those need community translation.

`icu-message-format` matters: ARB messages are ICU, so Weblate must validate
placeholders, plurals, and selects as ICU rather than as printf.

## Shipping Policy

Existing app languages are English, Spanish, German, Italian, and Estonian.

New languages can be collected in Codeberg Translate before they are ready to
ship. A new `lib/l10n/app_<lang>.arb` can be merged once it is **more than 70%**
translated, reviewed, and passes CI. Exactly 70.0% fails; the threshold is
strictly greater than.

`dart run tool/verify_l10n.dart` enforces the threshold, placeholder safety,
plural/select shape, and stale keys. It replaces the Kotlin app's
`./gradlew verifyTranslations`.

### Known regression: the language picker is not automatic any more

In the Kotlin app, a locale that crossed the threshold appeared in the in-app
language picker automatically, because the JVM supplies display names
(`Locale.getDisplayName`).

Dart ships **no ICU display-name data**, so the autonym (the language's name in
its own language) has to be added by hand. Shipping a new locale therefore takes
two extra edits:

1. Add the constant to `AppLanguage` in
   [`lib/domain/preferences/app_language.dart`](../../lib/domain/preferences/app_language.dart),
   with its BCP-47 language tag.
2. Add its autonym to `appLanguageLabel` in
   [`lib/ui/components/app_language_dropdown.dart`](../../lib/ui/components/app_language_dropdown.dart)
   (e.g. `Deutsch`, not `German` — an autonym is the same in every locale, which
   is what makes a language picker recognisable).

The validator reports when a locale is above threshold but missing from the
picker, so CI tells you when this is needed. Do not route the picker labels
through the ARB catalog.

## Translator Notes

- Preserve placeholders exactly. They are ICU-style braces such as `{arg0}`,
  `{count}`, or `{value}` — **not** the old Android `%1$s` / `%1$d` forms. The
  doubled `%%` escape no longer exists; a literal percent sign is just `%`.
- Preserve plural and select structures (`{count, plural, ...}`) including every
  category the source uses.
- Do **not** use a literal `{` or `}` in a message. `use-escaping` is off in
  `l10n.yaml`, so braces are *always* placeholder syntax and there is no way to
  escape one — `'{'` does not work and fails the build. Reword instead.
- Apostrophes are safe and literal: write `Couldn't`, not `Couldn''t`.
- Preserve escaped newlines such as `\n` when they are part of the source text.
- Keep product names and platform names unchanged unless the language normally
  localizes them: `OpenVitals`, `Health Connect`, `Google Play`, `Codeberg`,
  `Zulip`, package names, URLs, and measurement unit symbols.
- Prefer short labels. Many strings appear in compact cards, buttons, tabs, and
  home-screen widgets.

## Local Verification

Run the validator before merging Weblate pull requests:

```bash
dart run tool/verify_l10n.dart
```

The same check also runs as part of `flutter test`, and CI additionally proves the
generated Dart is not stale:

```bash
flutter gen-l10n
git diff --exit-code lib/l10n
```

That second gate exists for the "Weblate merged an ARB but nobody re-ran
`gen-l10n`" case, which would otherwise ship stale generated strings against
fresh translations.
