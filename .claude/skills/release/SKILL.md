---
name: release
description: Release a new OpenVitals version - changelogs, release notes, fastlane, version code, tag, push, and the companion docs/landing-page repos. Use for "release X.Y.Z" requests.
---

# OpenVitals release process

How a version ships, as done for 2.6.0 and 2.6.1. The order matters: every
document is written FIRST, because `scripts/release.sh` commits them, tags the
commit with the release notes, and pushes in one motion.

## 0. Pre-flight

- Everything for the release is committed and pushed on `main`; working tree clean.
- Gates are green: `./gradlew :app:testCiUnitTest verifyTranslations :app:compileCiAndroidTestKotlin`.
- Scope the release: `git log --oneline vLAST..HEAD` is the list of what the notes must cover.

## 1. Version code (read this before touching anything)

`versionCode` is a monotonic install counter, independent of `versionName`.
Nightlies and releases share one counter line. It is computed, never chosen:

```bash
sh scripts/version-code.sh next --floor <current baseVersionCode>
```

That consults GitHub release markers plus the append-only `refs/version-code/*`
refs (release bodies alone are not a safe database - a deleted release takes
its marker with it, and on Codeberg a pipeline dying while the nightly release
was recreated rewound the counter; observed 2026-07). The result MUST exceed whatever nightly users
have installed, or Play rejects the rollout with "does not allow any existing
users to upgrade" (this bit 2.6.0's predecessor).

**Race caveat:** a nightly can mint a code between your preview and the release
run. Only the release workflow's schedule (00:00 UTC, often started some
minutes late by GitHub) and manual runs of `.github/workflows/release.yml`
build nightlies; a push to main runs tests only. Near midnight UTC, or while a
manual run is going, check the runs first
(`https://github.com/OpenVitals-MTU/android-app/actions/workflows/release.yml`). Either way,
compute the code and name the fastlane files immediately before running the
release script, and afterwards verify the script's printed `versionCode`
matches the filenames.

## 2. Documents to write, all before running the script

1. **`CHANGELOG.md`** - prepend `## X.Y.Z - YYYY-MM-DD` with six language
   sections: `### English`, `### Espanol`, `### Deutsch`, `### Italiano`,
   `### Eesti`, `### Portugues`. House style: ASCII only (no diacritics, no
   em dashes - use `-`), bold-led bullets, one `**Fixes:**` bullet gathering
   the small ones.
2. **`docs/releases/X.Y.Z.md`** - the release notes; this file becomes the
   TAG MESSAGE. Shape: `# OpenVitals X.Y.Z`, `Released YYYY-MM-DD.`, one
   narrative paragraph saying what the release is about, then
   `### Added` / `### Changed` / `### Fixed`, then the standard footer: same
   package name and signing certificate note, and the distribution flow
   (GitHub release assets, Play upload from the approved production run of
   the release workflow).
3. **`README.md`** - add highlight bullets for headline features; update any
   claims the release changes (e.g. the language list when a locale lands).
4. **`fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`** - one
   per Play listing locale, all 14: `en-US`, `cs-CZ`, `de-DE`, `es-ES`, `et`,
   `fi-FI`, `fr-FR`, `gl-ES`, `it-IT`, `ja-JP`, `pl-PL`, `pt-PT`, `ru-RU`,
   `zh-CN`. Hard limit 500 characters, single paragraph. Write each in its
   own language and script (UTF-8; the ASCII rule is for `CHANGELOG.md`
   only), with the feature names from that locale's `strings.xml`. French
   says "Santé Connect", as the app does.
   - Each note is what a user of that language sees change. A fix that only
     touched the Czech strings goes in the Czech note, not the German one;
     the `en-US` note may summarise all of them.
   - `en-US`, `de-DE`, `es-ES`, `it-IT` and `et` had listings before 2.11.1;
     the other nine came with it. Do not call an older listing "new".
   - Japanese and Chinese have no plural forms: leave plural fixes out there.
   - Check before running the script (all 14 present, none over 500):
     ```bash
     code=<versionCode>; for d in fastlane/metadata/android/*/; do f="${d}changelogs/$code.txt"; test -f "$f" || echo "missing $f"; python3 -c "import sys; n=len(open(sys.argv[1],encoding='utf-8').read().rstrip(chr(10))); print(sys.argv[1], n, 'TOO LONG' if n > 500 else '')" "$f" 2>/dev/null; done
     ```

## 3. Run the release

```bash
bash scripts/release.sh X.Y.Z
```

The script computes the version code, patches `baseVersionCode` and
`baseVersionName` in `app/build.gradle.kts` and the `-SNAPSHOT` fallback in
`build.gradle.kts`, commits `chore: release X.Y.Z` (staging CHANGELOG, README,
docs, fastlane and the release machinery), tags `vX.Y.Z` with
`docs/releases/X.Y.Z.md` as the annotation, and pushes `main` plus the tag.

Verify the printed `Released vX.Y.Z (versionCode N)` matches the fastlane
changelog filenames; if a nightly stole the code, rename all 14 files and
amend before anyone pulls:

```bash
for f in fastlane/metadata/android/*/changelogs/OLD.txt; do git mv "$f" "${f%OLD.txt}NEW.txt"; done
```

CI (GitHub Actions) takes it from the tag: signed APK, signed debug APK, and
AAB on the GitHub release. Play production upload comes from a manual run of
the release workflow with target `production`, started from the tag and
approved through the `production` environment; it also posts the full notes to
the Zulip `releases` channel (`scripts/announce-zulip.sh`).

The Mastodon toot is disabled since 2.9.1: the `announce-mastodon` step is
commented out in `.github/workflows/release.yml`. If it comes back, the toot's body
is the narrative paragraph of `docs/releases/X.Y.Z.md` - the paragraph right
after `Released YYYY-MM-DD.` - cut to fit 500 characters with the GitHub release and
Play links. Write that paragraph to read well on its own and front-load it
either way: Zulip readers see it first too.

## 4. Companion repos (after the tag is pushed)

- **`../docs`** (Nextra site): prepend the English section to
  `docs/releases/changelog.md` (mirrors the CHANGELOG English section, minus
  app-repo-only details); add or update `docs/features/*.md` pages for new
  features and register new pages in `docs/features/_meta.ts`; fix any pages
  the release made stale. Commit `docs: X.Y.Z - <headline>` and push.
- **`../landing-page`**: update the copy (the `features.cards` grid and any
  card the release touches) when a headline feature warrants it. Each of the
  14 languages is one file, `lib/locales/<code>.ts`; `en.ts` is the source.
  Edit all 14, with the app's terms from that locale's `strings.xml` (French
  says "Santé Connect"). Check with `npm run typecheck`; there is no system
  Node, so use a portable one or the CI result. A push to `main` deploys to
  openvitals.health. The repo has no git identity: commit with
  `-c user.name=Manuel -c user.email=manuel@mmarca.tech`. Commit
  `content: <what changed>, for X.Y.Z` and push.

## 5. Gotchas that have actually happened

- Unescaped `&` in a translated string breaks `verifyTranslations` (XML parse).
- Every locale, Finnish and Polish included, is at 100% since 2026-09-25. New
  keys go to all 13 locale files; a locale that slips under 70% drops out of
  the picker silently.
- The fastlane changelog is per-versionCode, not per-versionName: a release
  whose code raced a nightly ships the wrong changelog silently if step 3's
  verification is skipped.
- Release notes and changelog entries are user-facing: name what the user
  sees, not the implementation ("the planned route drawn on the offline map",
  not "MapLibre LineLayer").
- New strings land English-only and `verifyTranslations` still passes, since
  its floor is 70%. Before writing the notes, count the keys each locale above
  70% is missing, and translate them or say so in the notes (2.9.1 had 35).
- `scripts/generate-translation-coverage.py` takes its first argument as an
  output directory. `--help` creates a `--help/` folder in the repo root.
