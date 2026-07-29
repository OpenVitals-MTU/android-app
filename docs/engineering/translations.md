# Translations

OpenVitals uses Flutter ARB catalogs for app UI translations. Codeberg Translate
is the preferred place for community translation work.

Translate the app here:

[translate.codeberg.org/projects/openvitals/mobile-app](https://translate.codeberg.org/projects/openvitals/mobile-app/)

## Where The Strings Live

- `lib/l10n/app_en.arb` is the **template and source language**.
- `lib/l10n/app_de.arb`, `app_es.arb`, `app_it.arb`, `app_et.arb` are the shipped
  translations.
- `lib/l10n/app_<lang>.arb` for any other language is an **in-progress**
  translation: hosted so Weblate can work on it, not offered to users. See
  [Shipping Policy](#shipping-policy).
- `lib/l10n/app_localizations*.dart` are **generated** by `flutter gen-l10n` from
  those ARB files (config: `l10n.yaml`). They are **not committed** — `generate:
  true` in `pubspec.yaml` regenerates them on every `pub get` and build, so there
  is nothing to keep in sync and no staleness gate. Tracking them was tried and
  reverted: Weblate edits the ARBs and cannot run `gen-l10n`, which made the
  committed output stale on every translation pull request by construction.

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
3. Regenerate, so your own checkout compiles against the new key (there is
   nothing to commit — the output is gitignored):

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
- Repository: the Codeberg `OpenVitals/mobile-app` repository
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

**Hosting a language and shipping it are two different things.**

A language is **hosted** in Codeberg Translate from **0%**. Its ARB lives in
`lib/l10n/` like any other, so translators have somewhere to put their work.

A language is **shipped** — offered in the in-app language picker and matched
against the device locale — only once someone adds an `AppLanguage` constant for
it. That is allowed only when it is **more than 70%** translated. Exactly 70.0%
fails; the threshold is strictly greater than.

|                   | in progress                    | shipped                        |
| ----------------- | ------------------------------ | ------------------------------ |
| ARB in `lib/l10n` | yes                            | yes                            |
| `AppLanguage` constant + autonym | no              | yes                            |
| Coverage          | anything, reported by CI       | must be **> 70%**, enforced    |
| Structural checks | **all of them**, enforced      | all of them, enforced          |
| Users see it      | never                          | yes                            |

Shipped app languages today are English, Spanish, German, Italian, and Estonian.

Why the split: Weblate edits the ARBs directly and opens **one pull request
containing every changed locale**. When the coverage floor also gated *hosting*,
a single 5%-translated newcomer failed that PR — and took an unrelated Spanish
fix down with it. So the floor now gates only what users can actually select.

The app therefore builds `supportedLocales` from `AppLanguage`, **not** from
`AppLocalizations.supportedLocales` (which gen-l10n derives from whichever ARB
files happen to be present). Otherwise a device set to an in-progress language
would resolve to it and get a mostly-English UI — with no escape, since that
language is not in the picker either.

### Promoting a language to shipped

`dart run tool/verify_l10n.dart` prints coverage for every locale and marks the
in-progress ones, so CI tells you when one crosses the line:

```text
  gl      74.2%  1245/1677  (in progress — READY TO SHIP: it is above 70%, so add
                             an AppLanguage constant + autonym to offer it in the picker)
```

Crossing 70% is a *notice*, never a failure — the pull request that finishes a
translation must not be the one that breaks the build. Two edits promote it:

1. Add the constant to `AppLanguage` in
   [`lib/domain/preferences/app_language.dart`](../../lib/domain/preferences/app_language.dart),
   with its BCP-47 language tag.
2. Add its autonym to `appLanguageLabel` in
   [`lib/ui/components/app_language_dropdown.dart`](../../lib/ui/components/app_language_dropdown.dart)
   (e.g. `Deutsch`, not `German` — an autonym is the same in every locale, which
   is what makes a language picker recognisable).

Both are hand edits because, unlike the Kotlin app, Dart ships **no ICU
display-name data**: `Locale.getDisplayName` has no equivalent, so a generated
picker entry would read `gl`. From then on the locale is gated like any other
shipped language — if it rots back below 70%, CI fails until it is fixed or the
constant is removed.

### What is still enforced for an in-progress locale

Only the *coverage* gate is lifted. `dart run tool/verify_l10n.dart` applies
every structural check to an in-progress ARB exactly as it does to a shipped one
— placeholder-set equality, stale keys, plural shape and `other` branch, ICU
syntax, duplicate keys, `@@locale` agreement — because `flutter gen-l10n` reads
every ARB in the directory, so a broken in-progress translation breaks the build
for everybody. A half-finished translation is fine; a malformed one is not.

The reverse direction is still an error: an `AppLanguage` constant whose ARB is
missing (or under-translated) fails, because that is a picker entry that silently
does nothing. Do not route the picker labels through the ARB catalog.

`tool/verify_l10n.dart` replaces the Kotlin app's `./gradlew verifyTranslations`.

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

This is the **only** l10n gate, and it is a separate CI step — the `app-preflight`
step of `.woodpecker/test.yml`. It does not run as part of `flutter test`, so a
green local suite proves nothing about the catalogs; run it yourself.

It validates the **ARBs** — coverage against the template, placeholder parity,
`@@locale` agreement. There is no gate on the generated Dart and none is needed:
`generate: true` means `gen-l10n` re-runs on every `pub get` and build, so what
compiles is always derived from the ARBs in the same checkout. The "Weblate
merged an ARB but nobody re-ran `gen-l10n`" case cannot occur.
