# Translations

OpenVitals uses Android XML resources for app UI translations. Codeberg Translate
is the preferred place for community translation work.

## Codeberg Translate Setup

Create one Weblate component for the Android app UI:

- Project: `OpenVitals`
- Component: `Android app`
- Repository: the Codeberg `OpenVitals/android-app` repository
- VCS mode: `Gitea pull request`
- File format: `Android String Resource`
- File mask: `app/src/main/res/values-*/strings.xml`
- Monolingual base language file: `app/src/main/res/values/strings.xml`
- Source language: English
- Edit base file: disabled
- License: `AGPL-3.0-or-later`

Add the Codeberg repository webhook target:

```text
https://translate.codeberg.org/hooks/gitea
```

The first component intentionally excludes `debug`, `nightly`, Fastlane
metadata, release notes, and docs. Add separate components later if those need
community translation.

## Shipping Policy

Existing app languages are English, Spanish, German, Italian, and Estonian.

New languages can be collected in Codeberg Translate before they are ready to
ship. A new `values-<lang>/strings.xml` file can be merged once it is more than
70% translated, reviewed, and passes CI. Languages above that threshold
are generated into the in-app language picker automatically.

Android's `MissingTranslation` lint check is disabled because partial Weblate
languages are allowed. The repository validator replaces that check by enforcing
the greater-than-70% coverage threshold, placeholder safety, plural shape, and
`translatable="false"` handling.

## Translator Notes

- Preserve placeholders exactly, including numbered placeholders like `%1$s`
  and percent values like `%1$d%%`.
- Preserve escaped newlines such as `\n` when they are part of the source text.
- Keep product names and platform names unchanged unless the language normally
  localizes them: `OpenVitals`, `Health Connect`, `Google Play`, `Codeberg`,
  `Zulip`, package names, URLs, and measurement unit symbols.
- Prefer short labels. Many strings appear in compact cards, buttons, tabs, and
  dashboard widgets.
- Do not translate resources marked `translatable="false"` in the base file.

## Local Verification

Run the translation validator before merging Weblate pull requests:

```bash
./gradlew verifyTranslations
```

The validator reports locale files at or below 70% coverage, stale extra keys,
translated `translatable="false"` resources, plural shape mismatches, and
placeholder mismatches. It is also part of `./gradlew verifyCi`.
