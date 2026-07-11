# Development

## Build Requirements

- Flutter SDK 3.44.x (CI pins `ghcr.io/cirruslabs/flutter:3.44.6`; Dart SDK `^3.12.2`)
- Android SDK Platform 37 with SDK Build-Tools 37.0.0 — `compileSdk = 37`, because the
  Health Connect `connect-client` alpha resolves its record/permission mappings against API 37
- JDK 17
- `minSdk` is 26 (Health Connect); `targetSdk` is 36

Generated code (`*.g.dart`, `*.freezed.dart`, `lib/l10n/app_localizations*.dart`) is **tracked**, and
CI does not regenerate it. The localization output has an explicit staleness gate (`flutter gen-l10n`
followed by `git diff --exit-code lib/l10n`); `build_runner` output does **not** — a stale
`*.freezed.dart` is usually caught by `flutter analyze`, but only because the generated members stop
matching. Re-run `build_runner` yourself after touching a model.

## Local Verification

Run the same checks CI runs, before pushing:

```bash
flutter analyze lib test
flutter test
dart run tool/verify_l10n.dart
flutter gen-l10n && git diff --exit-code lib/l10n   # generated Dart matches the ARBs
git diff --check
```

After changing a freezed model, a drift table, or a Riverpod generator annotation:

```bash
dart run build_runner build --delete-conflicting-outputs
```

`pubspec.yaml` carries `dependency_overrides` with a comment explaining each one. They are not
cruft — `sqlparser` in particular is pinned below 0.44.6 because it made `DartPlaceholder` sealed
and removed a helper `drift_dev` still calls, which breaks `build_runner` outright. Read the comment
before removing an override.

## Dependency Injection

Riverpod, not Hilt. There is no annotation processor in the app's critical path:

- Providers live in `lib/di/providers.dart` and `lib/state/app_providers.dart`.
- Screen state is a `Notifier` + an immutable `*State` (e.g. `lib/features/sleep/sleep_notifier.dart`).
- Route arguments come from go_router (`lib/navigation/app_router.dart`), not `SavedStateHandle`.
- Tests inject fakes with `ProviderContainer(overrides: [...])` / `overrideWithValue`. That override
  seam is the reason repositories take their data source as a constructor argument — keep it.

See `docs/engineering/architecture.md` for the layering, and `AGENTS.md` for the invariants that have
already been broken once.

### The AGP 9 Kotlin warning is expected

The build prints a warning that some plugins still apply the Kotlin Gradle Plugin instead of Flutter's
Built-in Kotlin. **This is not actionable from here** — it is emitted by third-party plugins, and it is
not a failure. Do not chase it.

What *is* actionable: adding an Android plugin can break `GeneratedPluginRegistrant` with a
"cannot find symbol" error that unit tests will never catch. **Always run `flutter build apk --debug`
after adding a plugin with native Android code.** This is also why `share_plus` is pinned to `^12.x` —
the 13.x line drags in a `package_info_plus` that breaks the AGP 9 build. The pin has a comment.

## CI

Two Woodpecker pipelines, both running `scripts/ci-flutter.sh` (which provisions Android platform 37
and the Gradle/pub caches, then execs `flutter`).

**`.woodpecker/test.yml`** — on every pull request and every push to `main`: `flutter test`, then
`flutter analyze`, the translation gate, the `gen-l10n` staleness gate, and `git diff --check`.

**`.woodpecker/release.yml`** — the release DAG. `resolve-release-context` computes everything ONCE
(`scripts/ci-release-context.sh`) and writes `.woodpecker/tmp/release-context.env`, which every later
step sources. This is deliberate: it is what stops the nightly and tag paths from drifting apart.

| Trigger | Builds | Publishes |
|---|---|---|
| Woodpecker cron `nightly` (00:00 UTC, default branch) or a manual run | release APK + AAB, version name `X.Y.Z-nightly.<pipeline>`, with `--dart-define=OPENVITALS_DIAGNOSTICS=true` | APK → the mutable Codeberg `nightly` release; AAB → Play **open testing** (API track name `beta`) |
| A pushed `vX.Y.Z` / `VX.Y.Z` tag | release APK | its own versioned Codeberg **prerelease** |
| An approved Woodpecker deployment with target `production`, from the tag commit | release AAB | Play **production**, and only then is the Codeberg prerelease flipped to stable |

The nightly release is intentionally **mutable**: each run force-moves the fixed `nightly` tag through
the Forgejo API and replaces the assets in place, so the download page URL never changes.

APKs ship only `armeabi-v7a` and `arm64-v8a` (`--target-platform android-arm,android-arm64`) to stay
under Codeberg's asset size limit. The production `applicationId` is used for every published build;
debug builds carry the `.debug` suffix and install side by side.

**Diagnostics are gated on `kDiagnosticsEnabled`, not `kDebugMode`** (`lib/core/diagnostics/diagnostics_build_config.dart`).
Nightly is a *release* build, so gating on `kDebugMode` would have shipped it with no diagnostics UI at
all — the one thing the nightly channel exists for. CI passes the dart-define; a plain release passes
nothing and the section stays compiled out.

### Version codes are not stored in git

`versionName` and `versionCode` are deliberately detached. The name is human (`1.9.0`,
`1.9.0-nightly.412`); the code is only a monotonic Android update counter, shared by the nightly and
release lines.

**The counter's source of truth is the Codeberg release notes.** `scripts/version-code.sh` pages the
Codeberg API, reads `<!-- OpenVitals-Version-Code: N -->` markers out of every release body, and
returns `max(marker, floor) + 1` — then CI writes the chosen code back into the new release's notes.
A production deployment instead *reuses* the marker already published on that `vX.Y.Z` release, so the
Play AAB carries exactly the code the Codeberg APK had.

Two consequences worth internalising:

- **Publishing from a different Codeberg repo resets the counter**, and Play then rejects every upload
  as a duplicate. This is the main reason the Flutter app replaces the Kotlin app on the *same* repo.
- A nightly can consume a code between a `pubspec.yaml` bump and the tag. `scripts/ci-release-context.sh`
  detects this and fails the tag build with "version code … is stale" rather than colliding on Play.

The nightly job also prunes old versioned Codeberg release pages, keeping the newest nine plus the
fixed `nightly` page. Git tags are never deleted.

### Secrets

All are Woodpecker repository secrets:

| Secret | Purpose |
|---|---|
| `OPENVITALS_RELEASE_KEYSTORE_BASE64` | base64 of the PKCS12 release keystore |
| `OPENVITALS_RELEASE_STORE_PASSWORD` | keystore password |
| `OPENVITALS_RELEASE_KEY_ALIAS` | signing key alias |
| `OPENVITALS_RELEASE_KEY_PASSWORD` | declared, but a `.p12` has one password, so CI overwrites it with the store password |
| `CODEBERG_RELEASE_API_KEY` | Codeberg token with release create/edit **and repo write** (needed to move `refs/tags/nightly`) |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64` | base64 of the Play service-account JSON |

If the Play account may stage edits but not submit them, set
`OPENVITALS_PLAY_CHANGES_NOT_SENT_FOR_REVIEW=true` and send the release for review by hand in Play
Console.

The GitHub mirror is configured as a **Codeberg push-mirror on the repository**, not as a pipeline
step — you will not find it in either YAML.

### Signing, and why a missing secret must fail loudly

`android/app/build.gradle.kts` reads the four `OPENVITALS_RELEASE_*` variables from the environment.
Nothing is read from `gradle.properties` or `local.properties`, and no keystore is ever committed.

Two details are load-bearing:

- **PKCS12 has a single password**, so the store password is reused as the key password. Remove that
  and CI fails with the thoroughly misleading "keystore password was incorrect".
- **There is no debug-key fallback.** With no signing environment the release build comes out
  *unsigned*. That is intentional: an unsigned artifact is an obvious failure, whereas a debug-signed
  one looks fine right up until it reaches a real device and cannot update the installed app.

The published app's certificate is the same one the Kotlin app shipped with. A different certificate
means every existing user gets `INSTALL_FAILED_UPDATE_INCOMPATIBLE` and the update simply will not
install. Before any release, check the build against the certificate that is actually live:

```bash
apksigner verify --print-certs build/app/outputs/flutter-apk/app-release.apk
```

It must print exactly:

```
certificate DN: CN=Manuel Marcatili, OU=OpenVitals, O=mmarca.tech, L=Tallinn, ST=Harjumaa, C=EE
certificate SHA-256 digest: 0416c0651543e951ef3b6c1ed9beb13833bff3a2be9ddacf492270712861e05d
```

That fingerprint is taken from the published `v1.9.0` APK on Codeberg — the last release built from
the Kotlin sources. If your build prints anything else, **stop**: it is signed with the wrong key and
cannot update the installed app.

## Release Checklist

1. Author the release notes first — they are inputs, not afterthoughts:
   - `CHANGELOG.md` (the user-facing summary)
   - `docs/releases/<version>.md` (becomes the annotated tag body, and thence the Codeberg release notes)
   - `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt` (Play's "What's new").
     These must be named after the **new** version code, which you can get in advance with
     `sh scripts/version-code.sh next --floor <current>`.
2. Update README/docs if navigation, permissions, screenshots, or bundled assets changed.
3. Run the local verification block above.
4. `scripts/release.sh <version>` — it bumps `pubspec.yaml` (`version: X.Y.Z+CODE`), sweeps in the files
   from step 1, commits, annotated-tags, and pushes. It builds and uploads nothing; pushing the tag is
   what starts the pipeline.
5. Validate the published prerelease APK on a device — including **installing it over an existing
   install**, which is the only check that proves the signing certificate still matches.
6. Press the Woodpecker deployment button with target `production` from the tag commit. That uploads
   the AAB to Play production and then promotes the Codeberg prerelease to stable.

Do not hand-pick a version code, and do not derive one from `vX.Y.Z` — let `scripts/version-code.sh` do it.

## Known gaps

- The Kotlin app had a desktop JVM smoke test that could run the Apple Health importer against a real
  `export.zip` without building the app (`-PappleHealthExport`). **There is no Flutter equivalent yet.**
  The importer is covered by unit tests over synthetic exports
  (`test/features/imports/applehealth/`), but nothing exercises a real multi-GB export off-device.
