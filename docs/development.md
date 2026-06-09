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

Release CI also uses the wrapper for local app test/lint and release artifact builds. Shared Maven artifacts are built as dependencies and tested, but publishing them is a separate explicit Gradle action.

## Shared Artifacts

Publish reusable OpenVitals modules to Maven local with:

```bash
./gradlew publishOpenVitalsArtifactsToMavenLocal -PopenVitalsArtifactVersion=1.3.0-SNAPSHOT
```

See [`shared-artifacts.md`](shared-artifacts.md) for remote publishing and connected-repo consumption.

Release CI is beta-first. A `v*` tag publishes signed APK and Android App Bundle
assets to Codeberg as a prerelease, and publishes the signed App Bundle to the
Google Play open testing track with the Fastlane `android open_testing` lane. The
beta upload also uploads Play metadata and screenshots from
`fastlane/metadata/android`. Codeberg prerelease publishing depends only on the
signed artifact build, so a Play Console permission failure should not block the
Codeberg beta assets. If a tag pipeline is rerun after Google Play already has
the release version code in open testing or production, the Fastlane
`android open_testing` lane skips the duplicate AAB upload.

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
and screenshots. If Google Play fails at the final "Uploading all changes to
Google Play" step with `The caller does not have permission`, check that the
service account is linked in Play Console API access and has permissions for
viewing app information, managing store presence, releasing to testing tracks,
releasing to production, and sending changes for review. If the account is
allowed to stage edits but not send them for review, set the Woodpecker
environment variable `OPENVITALS_PLAY_CHANGES_NOT_SENT_FOR_REVIEW=true`; the
release will then need to be sent for review manually in Play Console.

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
./gradlew verifyLocalApp
git diff --check
```

6. Commit the release prep, tag the commit as an annotated `v<versionName>` tag such as `v0.7.0` using `docs/releases/<versionName>.md` as the tag message, and push both the branch and tag. The tag pipeline publishes the beta release to Codeberg and Google Play open testing.
7. After beta approval, start a Woodpecker deployment from the successful tag pipeline with deploy target `production`. The deployment promotes the Play release to production and marks the Codeberg release stable.

Use the exact `versionName` for release notes, changelog references, and tags.
For a final release, that means file name, `versionName`, and tag such as
`1.0.0` / `v1.0.0`; for beta-only prereleases, include the suffix everywhere,
such as `1.0.0-beta.1` / `v1.0.0-beta.1`. Keep the Play `versionCode` unique
and increasing, and add matching Fastlane changelog files for that exact code.
The tag pipeline publishes beta tags to Codeberg prerelease and Google Play open
testing, while the production deployment path rejects `-alpha`, `-beta`, and
`-rc` tags.
