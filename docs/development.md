# Development

## Build Requirements

- Android SDK Platform 37.0 with SDK Build-Tools 37.0.0
- JDK 17
- Gradle wrapper files, including `gradle/wrapper/gradle-wrapper.jar`

The wrapper jar is intentionally tracked. `.gitignore` allows this file even though other jars are ignored.

## Local Verification

Run the main checks before pushing architecture or feature changes:

```bash
./gradlew verifyLocalApp
git diff --check
```

On Windows, use `gradlew.bat`:

```powershell
.\gradlew.bat verifyLocalApp
git diff --check
```

## Hilt And KSP

The local app uses Hilt in the `:app` module:

- `@HiltAndroidApp` on `OpenVitalsApp`
- `@AndroidEntryPoint` on `MainActivity`
- `@HiltViewModel` for screen ViewModels
- `hiltViewModel()` in navigation destinations
- KSP for Hilt code generation

AGP 9 built-in Kotlin currently requires `android.disallowKotlinSourceSets=false` so KSP generated sources are accepted. Keep this in `gradle.properties` unless the Android Gradle Plugin/KSP behavior changes.

## CI

Woodpecker uses the Gradle wrapper directly. The test pipeline runs:

```bash
./gradlew --no-daemon verifyLocalApp
git diff --check
```

Release CI also uses the wrapper for local app test/lint and APK builds. There
are two release channels:

- A pushed or moved `nightly` tag builds `:app:assembleRelease` and
  publishes `OpenVitals-nightly.apk` to the fixed Codeberg `nightly`
  prerelease. The same run builds `:app:bundleRelease` and uploads the signed
  AAB to the Google Play open testing track, whose Play Developer API track name
  is `beta`.
- A pushed `vX.Y.Z` or `VX.Y.Z` tag builds `:app:assembleRelease` and publishes
  `OpenVitals-vX.Y.Z.apk` to its own versioned Codeberg prerelease, which can
  be promoted after validation by an approved Woodpecker deployment to
  `production`.

Both Codeberg APKs use the release build type with the same production
application ID, minification, packaging, and signing model. Codeberg APK
artifacts compress bundled native libraries and include only ARM 32/64-bit ABIs
(`armeabi-v7a` and `arm64-v8a`) so direct-download APKs stay below the forge
upload limit.

The nightly release is intentionally mutable: each successful `nightly` tag run
replaces the existing APK and checksum assets instead of creating another
release page. Its Play AAB uses the same release build type with a CI-generated
Play-safe `versionCode` using
`major * 100000000 + minor * 1000000 + patch * 10000 + nightlySequence`.
Version tags create or update the matching versioned prerelease page. The
approved production deployment uploads the signed release AAB to Google Play
production and then promotes the matching Codeberg prerelease to stable.

Configure the Woodpecker cron named `nightly` to run at `00:00 UTC`. The
cron-only workflow moves the fixed `nightly` tag to the cron commit and pushes
it back to Codeberg; that tag push triggers the normal nightly release workflow.
Configure `CODEBERG_NIGHTLY_TAG_SSH_KEY` with a write-enabled Codeberg deploy
key that can move `refs/tags/nightly`.

Configure `CODEBERG_RELEASE_API_KEY` with a Codeberg token that can create and
edit repository releases. Configure the release signing secrets
`OPENVITALS_RELEASE_KEYSTORE_BASE64`, `OPENVITALS_RELEASE_STORE_PASSWORD`,
`OPENVITALS_RELEASE_KEY_ALIAS`, and `OPENVITALS_RELEASE_KEY_PASSWORD` so CI can
produce updateable nightly and release APKs. Configure
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64` with the base64-encoded JSON key for a
Google Play service account that can release to open testing and production for
`tech.mmarca.openvitals`. If the account is allowed to stage edits but not send
them for review, set `OPENVITALS_PLAY_CHANGES_NOT_SENT_FOR_REVIEW=true`; the
open testing or production release will then need to be sent for review manually
in Play Console.

After a successful `main` push pipeline, Woodpecker mirrors the checked commit to
`git@github.com:mmarca-tech/OpenVitals.git`. Configure the Woodpecker secret
`GITHUB_MIRROR_SSH_KEY` with a private SSH key whose public key is installed as a
write-enabled deploy key on the GitHub mirror repository.

## Release Checklist

For a versioned prerelease:

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`. Use
   `major * 100000000 + minor * 1000000 + patch * 10000` for versioned release
   codes.
2. When preparing a store release, add Play changelog files under
   `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
3. Add the user-facing release summary to `CHANGELOG.md`.
4. Update README or docs when the user-facing navigation, permissions, screenshots, or bundled assets change.
5. Run:

```bash
./gradlew verifyLocalApp
git diff --check
```

6. Commit the release prep, tag the commit as an annotated `v<versionName>` tag
   such as `v0.7.0` using the matching `CHANGELOG.md` section as the tag
   message, and push both the branch and tag. The tag pipeline runs the release
   checks and publishes the versioned Codeberg prerelease APK.
7. After validation, use the approved Woodpecker deployment button with target
   `production` from the version tag commit. The deployment uploads the signed
   release AAB to Google Play production and then promotes the matching Codeberg
   prerelease to stable.

For an immediate nightly release, move the fixed `nightly` tag to the desired
commit and push it. The tag pipeline runs the same release checks, publishes the
APK to the mutable Codeberg `nightly` prerelease, and uploads the signed AAB to
Google Play open testing. The scheduled Woodpecker cron performs the same tag
move automatically at midnight UTC.

Use the exact `versionName` for release notes, changelog references, and tags.
For a final release, that means file name, `versionName`, and tag such as
`1.0.0` / `v1.0.0`. Keep the Play `versionCode` unique and increasing, and add
matching Fastlane changelog files for that exact code when preparing a store
release.
