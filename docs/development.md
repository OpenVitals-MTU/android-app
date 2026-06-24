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

Release CI also uses the wrapper for local app test/lint and release artifact builds.
The tag prerelease pipeline runs `verifyLocalReleaseChecks` before publishing
signed APK and Android App Bundle assets to Codeberg as a prerelease. Production
deployments are approved from the successful tag pipeline and skip rerunning that
full prerelease test suite; they still rebuild the signed release artifacts from
the tagged commit before upload.

A production deployment publishes the signed App Bundle directly to the Google
Play production track with the Fastlane `android production` lane, including Play
metadata and screenshots from `fastlane/metadata/android`. If the deployment is
rerun after Google Play already has the release version code in production, the
Fastlane lane skips the duplicate AAB upload. After the Play upload succeeds, the
pipeline marks the existing Codeberg release as stable through the Forgejo API.
Prerelease-suffixed tags such as `-alpha`, `-beta`, and `-rc` are beta-only and
are rejected by the production deployment path.

Configure the Woodpecker secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64` with
the base64-encoded JSON key for a Google Play service account that has release
access to `tech.mmarca.openvitals`. For the current release pipeline, that
service account needs app-level Play Console permissions to manage store
presence and release to production. The store-presence
permission is required because Fastlane uploads listing text, changelogs, icon,
and screenshots. If Google Play fails at the final "Uploading all changes to
Google Play" step with `The caller does not have permission`, check that the
service account is linked in Play Console API access and has permissions for
viewing app information, managing store presence, releasing to production, and
sending changes for review. If the account is
allowed to stage edits but not send them for review, set the Woodpecker
environment variable `OPENVITALS_PLAY_CHANGES_NOT_SENT_FOR_REVIEW=true`; the
release will then need to be sent for review manually in Play Console.

Configure `CODEBERG_RELEASE_API_KEY` with a Codeberg token that can create and
edit repository releases. In Woodpecker project settings, enable deployments so a
successful tag pipeline can be released with the `production` deploy target.

After a successful `main` push pipeline, Woodpecker mirrors the checked commit to
`git@github.com:mmarca-tech/OpenVitals.git`. Configure the Woodpecker secret
`GITHUB_MIRROR_SSH_KEY` with a private SSH key whose public key is installed as a
write-enabled deploy key on the GitHub mirror repository.

## Release Checklist

For a stable release:

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add Play changelog files under `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
3. Add the user-facing release summary to `CHANGELOG.md`.
4. Update README or docs when the user-facing navigation, permissions, screenshots, or bundled assets change.
5. Run:

```bash
./gradlew verifyLocalApp
git diff --check
```

6. Commit the release prep, tag the commit as an annotated `v<versionName>` tag such as `v0.7.0` using the matching `CHANGELOG.md` section as the tag message, and push both the branch and tag. The tag pipeline runs the prerelease checks and publishes the Codeberg prerelease assets.
7. Start a Woodpecker deployment from the successful tag pipeline with deploy target `production`. The deployment skips rerunning the prerelease test suite, uploads the signed App Bundle to Google Play production, and marks the Codeberg release stable.

Use the exact `versionName` for release notes, changelog references, and tags.
For a final release, that means file name, `versionName`, and tag such as
`1.0.0` / `v1.0.0`; for beta-only prereleases, include the suffix everywhere,
such as `1.0.0-beta.1` / `v1.0.0-beta.1`. Keep the Play `versionCode` unique
and increasing, and add matching Fastlane changelog files for that exact code.
The tag pipeline publishes beta tags to Codeberg prerelease, while the production
deployment path rejects `-alpha`, `-beta`, and `-rc` tags.
