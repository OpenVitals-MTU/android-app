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
are three release channels:

- A manually triggered Woodpecker run builds `:app:assembleDebug` and publishes
  `OpenVitals-diagnostics.apk` to the fixed Codeberg `diagnostics` prerelease.
- The Woodpecker cron job named `nightly` builds `:app:assembleNightly` and
  publishes `OpenVitals-nightly.apk` to the fixed Codeberg `nightly`
  prerelease. The same run builds `:app:bundleNightly` and uploads the signed
  AAB to the Google Play open testing track, whose Play Developer API track name
  is `beta`.
- A pushed `vX.Y.Z` tag builds `:app:assembleRelease` and publishes
  `OpenVitals-vX.Y.Z.apk` to its own stable Codeberg release.

The diagnostics and nightly releases are intentionally mutable: each successful
run replaces the existing APK and checksum assets instead of creating another
release page. Published CI diagnostics APKs are signed with the stable release
signing configuration for the separate `tech.mmarca.openvitals.debug`
application ID, so Codeberg diagnostics-to-diagnostics updates keep the same
certificate across ephemeral runners. The CI diagnostics APK keeps the debug
build type and diagnostics flag, but is non-debuggable and minified so the
mutable Codeberg release asset stays below the forge upload limit. APK release
artifacts compress bundled native libraries so the all-ABI direct-download APKs
stay below the forge upload limit. Nightly uses the production
`tech.mmarca.openvitals` application ID so the signed AAB can be published to
the existing Play app's open testing track.
The Codeberg nightly APK and Play open testing AAB are both signed with the same
stable release signing configuration when CI secrets are present. CI assigns
nightly builds a unique Play-safe `versionCode` using
`major * 100000000 + minor * 1000000 + patch * 10000 + nightlySequence`; stable
releases use the same formula without the nightly sequence.

Configure `CODEBERG_RELEASE_API_KEY` with a Codeberg token that can create and
edit repository releases. Configure the release signing secrets
`OPENVITALS_RELEASE_KEYSTORE_BASE64`, `OPENVITALS_RELEASE_STORE_PASSWORD`,
`OPENVITALS_RELEASE_KEY_ALIAS`, and `OPENVITALS_RELEASE_KEY_PASSWORD` so CI can
produce updateable debug, nightly, and release APKs. Configure
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64` with the base64-encoded JSON key for a
Google Play service account that can release to open testing for
`tech.mmarca.openvitals`. If the account is allowed to stage edits but not send
them for review, set `OPENVITALS_PLAY_CHANGES_NOT_SENT_FOR_REVIEW=true`; the
open testing release will then need to be sent for review manually in Play
Console.

After a successful `main` push pipeline, Woodpecker mirrors the checked commit to
`git@github.com:mmarca-tech/OpenVitals.git`. Configure the Woodpecker secret
`GITHUB_MIRROR_SSH_KEY` with a private SSH key whose public key is installed as a
write-enabled deploy key on the GitHub mirror repository.

## Release Checklist

For a stable release:

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`. Use
   `major * 100000000 + minor * 1000000 + patch * 10000` for stable release
   version codes.
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
   checks and publishes the stable Codeberg release APK.

Use the exact `versionName` for release notes, changelog references, and tags.
For a final release, that means file name, `versionName`, and tag such as
`1.0.0` / `v1.0.0`. Keep the Play `versionCode` unique and increasing, and add
matching Fastlane changelog files for that exact code when preparing a store
release.
