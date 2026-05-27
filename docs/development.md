# Development

## Build Requirements

- Android SDK 36
- JDK 17
- Gradle wrapper files, including `gradle/wrapper/gradle-wrapper.jar`

The wrapper jar is intentionally tracked. `.gitignore` allows this file even though other jars are ignored.

## Local Verification

Run the main checks before pushing architecture or feature changes:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
git diff --check
```

On Windows, use `gradlew.bat`:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
git diff --check
```

## Hilt And KSP

The app uses Hilt in the existing single `:app` module:

- `@HiltAndroidApp` on `OpenVitalsApp`
- `@AndroidEntryPoint` on `MainActivity`
- `@HiltViewModel` for screen ViewModels
- `hiltViewModel()` in navigation destinations
- KSP for Hilt code generation

AGP 9 built-in Kotlin currently requires `android.disallowKotlinSourceSets=false` so KSP generated sources are accepted. Keep this in `gradle.properties` unless the Android Gradle Plugin/KSP behavior changes.

## CI

Woodpecker uses the Gradle wrapper directly. The test pipeline runs:

```bash
./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
git diff --check
```

Release CI also uses the wrapper for test/lint and release artifact builds.

Release CI is beta-first. A `v*` tag publishes signed APK and Android App Bundle
assets to Codeberg as a prerelease, and publishes the signed App Bundle to the
Google Play open testing track with the Fastlane `android open_testing` lane. The
beta upload also uploads Play metadata and screenshots from
`fastlane/metadata/android`.

Production is an approved promotion, not a second upload. After the beta build is
accepted, start a Woodpecker deployment from the successful tag pipeline with the
deploy target set to `production`. The deployment pipeline promotes the existing
Google Play `beta` track version to `production` with the Fastlane
`android promote_production` lane, then marks the existing Codeberg release as
stable through the Forgejo API.
Prerelease-suffixed tags such as `-alpha`, `-beta`, and `-rc` are beta-only and
are rejected by the production promotion path.

Configure the Woodpecker secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64` with
the base64-encoded JSON key for a Google Play service account that has release
access to `tech.mmarca.openvitals`. For the current release pipeline, that
service account needs app-level Play Console permissions to release to testing
tracks, manage store presence, and release to production. The store-presence
permission is required because Fastlane uploads listing text, changelogs, icon,
and screenshots.

Configure `CODEBERG_RELEASE_API_KEY` with a Codeberg token that can create and
edit repository releases. In Woodpecker project settings, enable deployments so a
successful beta pipeline can be promoted with the `production` deploy target. In
Google Play Console, keep open testing configured for the app's countries and
tester enrollment so beta users can opt in from Play.

After a successful `main` push pipeline, Woodpecker mirrors the checked commit to
`git@github.com:mmarca-tech/OpenVitals.git`. Configure the Woodpecker secret
`GITHUB_MIRROR_SSH_KEY` with a private SSH key whose public key is installed as a
write-enabled deploy key on the GitHub mirror repository.

## Release Checklist

For a stable release:

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add Play changelog files under `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
3. Add Codeberg release notes under `docs/releases/<versionName>.md` and copy the user-facing summary to `CHANGELOG.md`.
4. Update README or docs when the user-facing navigation, permissions, screenshots, or bundled assets change.
5. Run:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
git diff --check
```

6. Commit the release prep, tag the commit as an annotated `v<versionName>` tag such as `v0.7.0` using `docs/releases/<versionName>.md` as the tag message, and push both the branch and tag. The tag pipeline publishes the beta release to Codeberg and Google Play open testing.
7. After beta approval, start a Woodpecker deployment from the successful tag pipeline with deploy target `production`. The deployment promotes the Play release to production and marks the Codeberg release stable.
