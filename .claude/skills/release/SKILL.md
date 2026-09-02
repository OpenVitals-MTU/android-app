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

That consults Codeberg release markers plus the append-only `refs/version-code/*`
mirror (release bodies alone are not a safe database - the nightly release is
deleted and recreated every build, and a pipeline dying in that window rewinds
the counter; observed 2026-07). The result MUST exceed whatever nightly users
have installed, or Play rejects the rollout with "does not allow any existing
users to upgrade" (this bit 2.6.0's predecessor).

**Race caveat:** a nightly can mint a code between your preview and the release
run (every push to main triggers one). Compute the code and name the fastlane
files immediately before running the release script, and afterwards verify the
script's printed `versionCode` matches the filenames.

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
   (Codeberg release assets, Play upload from the approved Woodpecker
   deployment).
3. **`README.md`** - add highlight bullets for headline features; update any
   claims the release changes (e.g. the language list when a locale lands).
4. **`fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`** - one
   per Play listing locale: `en-US`, `es-ES`, `de-DE`, `it-IT`, `et`.
   Hard limit 500 characters, ASCII, single paragraph. Known follow-up: a
   `pt-PT` listing locale does not exist yet, so no Portuguese file.

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
changelog filenames; if a nightly stole the code, rename the files and amend
before anyone pulls.

CI (Woodpecker) takes it from the tag: signed APK, signed debug APK, and AAB
on the Codeberg release; Play production upload from the approved deployment,
which also toots the release from `@openvitals@techhub.social`
(`scripts/announce-mastodon.sh`). The toot's body is the narrative paragraph
of `docs/releases/X.Y.Z.md` - the paragraph right after `Released YYYY-MM-DD.` -
so write it to read well on its own; to fit 500 characters together with the
Codeberg and Play links it gets cut at a sentence end when that wastes little
room, and otherwise at a clause boundary with an ellipsis (front-load the
paragraph - the last clauses are what an overlong narrative loses). The same deployment
posts the full notes to the Zulip `releases` channel (`scripts/announce-zulip.sh`).

## 4. Companion repos (after the tag is pushed)

- **`../docs`** (Nextra site): prepend the English section to
  `docs/releases/changelog.md` (mirrors the CHANGELOG English section, minus
  app-repo-only details); add or update `docs/features/*.md` pages for new
  features and register new pages in `docs/features/_meta.ts`; fix any pages
  the release made stale. Commit `docs: X.Y.Z - <headline>` and push.
- **`../landing-page`**: update `app/page.tsx` copy (the `featureCards` grid
  and any card the release touches) when a headline feature warrants it.
  Commit and push.

## 5. Gotchas that have actually happened

- Unescaped `&` in a translated string breaks `verifyTranslations` (XML parse).
- `values-gl` (Galician) is Weblate-owned and below the 70% picker threshold -
  do not add keys there or count it against coverage.
- The fastlane changelog is per-versionCode, not per-versionName: a release
  whose code raced a nightly ships the wrong changelog silently if step 3's
  verification is skipped.
- Release notes and changelog entries are user-facing: name what the user
  sees, not the implementation ("the planned route drawn on the offline map",
  not "MapLibre LineLayer").
