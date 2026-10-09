# Development

## Build Requirements

- Android SDK Platform 37.0 with SDK Build-Tools 37.0.0
- JDK 17
- Gradle wrapper files, including `gradle/wrapper/gradle-wrapper.jar`

The wrapper jar is intentionally tracked. `.gitignore` allows this file even though other jars are ignored.

## Local Verification

Run the main checks before pushing architecture or feature changes:

```bash
./gradlew verifyCi
git diff --check
```

`verifyCi` is an aggregate defined in the root `build.gradle.kts`:

- `verifyCiUnitTest` runs `:app:testCiUnitTest`.
- `verifyCiPreflight` runs `verifyTranslations`, `:app:lintCi`, `:app:assembleCi`,
  and `:app:compileCiAndroidTestKotlin`.

Unit tests run against the `ci` build type. There is no debug unit-test variant,
so the task is `:app:testCiUnitTest`; `:app:testDebugUnitTest` does not exist.
To run a single test class:

```bash
./gradlew :app:testCiUnitTest --tests 'tech.mmarca.openvitals.SomeTest'
```

`verifyAndroidTest` runs `:app:connectedCiAndroidTest`, but only locally and only
when `ANDROID_SERIAL` is set; it is disabled in CI.

### Wear OS Gate

The watch app in `wear/` has its own gate, separate from `verifyCi`. Both gates run the shared link module's tests (`:wearlink:test`), since both apps compile it:

```bash
./gradlew verifyWearCi
```

It runs `:wearlink:test`, `:wear:testDebugUnitTest`, `:wear:lintDebug`, `:wear:assembleDebug`,
and `:wear:compileDebugAndroidTestKotlin`. A dependency change in `wearlink/build.gradle.kts`
needs `./gradlew :wearlink:dependencies --write-locks` for its lockfile, like the other modules. The wear module uses the standard
`debug` build type; it has no `ci` variant.

CI mirrors the split. `.github/workflows/test.yml` runs `verifyCi` and skips
changes that only touch `wear/`. `.github/workflows/wear-test.yml` runs `verifyWearCi` and
triggers only on `wear/`, `wearlink/` and the shared build files they depend on. A change to
the root Gradle files runs both.

The watch app shares the phone app's `applicationId`, `tech.mmarca.openvitals`,
so Play lists it as a form factor of OpenVitals rather than a second app. The
Kotlin namespace stays `tech.mmarca.openvitals.wear`. The debug build type adds
the same `.debug` suffix as the phone app.

One listing means one versionCode space. The watch owns the range
`2000000001` to `2099999999`; phone codes stay below it. `wear/build.gradle.kts`
reads `OPENVITALS_WEAR_VERSION_CODE` and `OPENVITALS_WEAR_VERSION_NAME` the way
the phone build reads its own overrides, and fails the build when the code is
outside the watch range.

#### Installing the watch app without Play

There is no F-Droid client for Wear OS and a watch cannot install an APK by
itself, so the watch app is sideloaded over adb. A watch has no USB data
port, and debugging over Bluetooth goes through the vendor's phone app, so
the only way in is Wireless debugging over Wi-Fi: the watch and the computer
must be on the same Wi-Fi network. The watch joins Wi-Fi on its own, without
a phone or a Google account.

On the watch, all in Settings:

1. Connections, Wi-Fi: join the network the computer is on.
2. Unlock Developer options: About watch, Software, tap the software version
   five times until the watch says developer mode is on. (On a Pixel Watch:
   System, About, tap the build number seven times.)
3. Developer options, at the bottom of Settings: turn on ADB debugging, then
   Wireless debugging. Wireless debugging shows the address and port to
   connect to.
4. Wireless debugging, Pair new device: shows the pairing address, a separate
   pairing port, and a six-digit code. Leave this screen open while pairing;
   the code expires with it.

Then, once per computer:

```bash
adb pair <watch-ip>:<pairing-port> <code>
adb connect <watch-ip>:<port>          # the port Wireless debugging shows
./gradlew :wear:assembleDebug
adb -s <watch-ip>:<port> install -r wear/build/outputs/apk/debug/wear-debug.apk
```

Both ports change on every boot, and the watch drops Wi-Fi while it sleeps
off the charger, so wake it before connecting. adb's own mDNS discovery finds nothing
next to a system Avahi daemon; run adb with `ADB_MDNS_OPENSCREEN=1`, or read
the address from `avahi-browse -rt _adb-tls-connect._tcp`. The debug watch app
installs as `tech.mmarca.openvitals.debug`, so it pairs with the debug phone
app.

Three scripts wrap this: `scripts/wear-pair.sh` (pairing and discovery),
`scripts/wear-install.sh` (build if needed, install, the permission grants, a
status check with `--status`), and `scripts/wear-skip-onboarding.sh`, which
marks a never-onboarded watch as set up so the vendor's phone app is not
needed at all. They share `scripts/lib/wear-adb.sh`. The last one only works on watches whose welcome screen opens Settings, since
Wi-Fi adb is behind it; the Galaxy Watch8 does not, so Samsung watches get
onboarded once with Galaxy Wearable and the app sideloaded afterwards. The
user-facing walkthrough, with the per-watch table, is
[docs/how-to/wear-os-sideload.md](../how-to/wear-os-sideload.md).

### Translation Gate

For translation-only changes, the fast local check is:

```bash
./gradlew verifyTranslations
```

That task shells out to `scripts/verify-translations.py`, which enforces
greater-than-70% coverage per locale, placeholder safety, plural shape, and
`translatable="false"` handling. `verifyCi` includes it, so Weblate pull requests
must keep locale files more than 70% translated and placeholders intact.

Current state: `verifyTranslations` passes and every locale file is at 100%.
A locale under the threshold only prints a note — the coverage floor decides
whether a language is *offered* in the picker, not whether its file may exist,
so a translator's first commit does not break CI. Galician looked like this
before it was completed:

```text
note: app/src/main/res/values-gl/strings.xml is at 3.4% coverage and is not offered in the language picker until it passes 70%.
```

What still fails the build for every locale file, offered or not, is placeholder
and plural-shape safety. A locale may carry *more* CLDR plural categories than
the English base (Czech and Spanish need `few`/`many`); those extra branches are
checked against the base `other` branch so they cannot drop an argument.

A new key goes to every `values-*/strings.xml` together with the base file, in
the plural shape that locale needs; see [translations.md](translations.md).
Weblate then refines the wording.

For Apple Health importer work, there is also a desktop JVM smoke test that can exercise the Kotlin importer against a real local export without building or installing the app:

```bash
./gradlew app:testCiUnitTest \
  --tests tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportSmokeTest \
  -PappleHealthExport=/path/to/export.zip \
  --console=plain
```

`-PappleHealthExport` can point to an Apple Health `export.zip`, `export.xml`, or an unzipped export directory. The test parses XML, parses GPX route files when present, runs supported Health Connect conversion logic, and prints import-shape counts. It is skipped automatically when no export path is provided.

On Windows, use `gradlew.bat`:

```powershell
.\gradlew.bat verifyCi
git diff --check
```

### R8 Keep Check

Unit tests run on the unminified `ci` variant, and no pipeline installs an APK, so nothing runs the R8 build before it ships. Every `minify<Variant>WithR8` task is therefore followed by `verify<Variant>R8Keeps`, which runs [`scripts/verify-r8-keeps.py`](../../scripts/verify-r8-keeps.py) against that build's `mapping.txt`. It fails the build when R8 removed or renamed something that is only reached by name at run time:

- a manifest component of ours, or its no-arg constructor
- a Glance `ActionCallback` subclass, or its no-arg constructor (R8 once stripped one; widget taps were dead from 2.7.0 to 2.7.1)
- a Health Connect record class named in `SyncRecordCodec.kt`, whose simple name is the phone-sync wire format

It needs no device, so it runs in the release pipeline as part of the build. To run it by hand: `./gradlew :app:minifyReleaseWithR8`. When it fails, add a keep rule to `app/proguard-rules.pro` and say why. A new kind of by-name lookup needs a new check in the script.

## Hilt And KSP

The local app uses Hilt in the `:app` module:

- `@HiltAndroidApp` on `OpenVitalsApp`
- `@AndroidEntryPoint` on `MainActivity`
- `@HiltViewModel` for screen ViewModels
- `hiltViewModel()` in navigation destinations
- KSP for Hilt code generation

AGP 9 built-in Kotlin currently requires `android.disallowKotlinSourceSets=false` so KSP generated sources are accepted. Keep this in `gradle.properties` unless the Android Gradle Plugin/KSP behavior changes.

## CI

CI is GitHub Actions on GitHub-hosted runners. The Gradle jobs run in the
prebaked `ghcr.io/mmarca-tech/openvitals-android-ci` image (see
[`ci-image/README.md`](../../ci-image/README.md)), cache Gradle with
`gradle/actions/setup-gradle`, and go through `scripts/ci-android-gradle.sh`,
which sizes the JVMs for the runner's 16 GB. The test workflow runs:

```bash
./gradlew --no-daemon verifyCi
git diff --check
```

`.github/workflows/release.yml` builds and publishes. There are two GitHub
release outputs:

- The `00:00 UTC` schedule, or a manual run of the release workflow with target
  `nightly`, builds `:app:assembleNightly` and `:app:assembleDebug`, then
  publishes `OpenVitals-nightly.apk` and `OpenVitals-nightly-debug.apk` to the
  fixed GitHub `nightly` prerelease. The same run builds `:app:bundleNightly`
  and uploads the signed AAB to the Google Play open testing track, whose Play
  Developer API track name is `beta`. The AAB is also attached to the `nightly`
  prerelease as `OpenVitals-nightly.aab`.
- A pushed `vX.Y.Z` or `VX.Y.Z` tag builds `:app:assembleRelease` and
  `:app:assembleDebug`, then publishes `OpenVitals-vX.Y.Z.apk` and
  `OpenVitals-vX.Y.Z-debug.apk` to its own versioned GitHub prerelease, which
  can be promoted after validation by a manual run of the release workflow with
  target `production`, started from the tag. The tag build also runs
  `:app:bundleRelease` and attaches `OpenVitals-vX.Y.Z.aab`. It does not upload
  to Play.

Each attached AAB has a `.sha256` file beside it. The AAB is there for a Play
upload by hand, when Play refuses the upload from the workflow. The GitHub
release job does not wait for the Play job, so the AAB is attached even then.

Nightly and release APKs use the release-style production application ID,
minification, packaging, and signing model. Published Debug APKs use the
separate `tech.mmarca.openvitals.debug` application ID and are signed with the
stable release signing configuration so Debug-to-Debug updates keep the same
certificate across ephemeral runners. Release APKs compress bundled native
libraries and include only ARM 32/64-bit ABIs (`armeabi-v7a` and `arm64-v8a`)
so direct-download APKs stay small.

The nightly release is intentionally mutable: each successful scheduled or
manual nightly run moves the fixed `nightly` tag and replaces the existing APK
and checksum assets instead of creating another release page. `versionName`
and `versionCode` are intentionally detached: `versionName` carries the human
release name (`1.7.7`, `1.7.7-nightly.335`, where the suffix is the workflow
run number), while `versionCode` is only a monotonic Android update counter.
Both nightly and versioned releases use the same counter line. CI reads
`OpenVitals-Version-Code` markers from existing GitHub release notes plus the
append-only `refs/version-code/*` refs, uses
`max(markers, refs, baseVersionCode) + 1` for new release artifacts, stores the
chosen code back into the release notes, and records it as a
`refs/version-code/<code>` ref before the nightly tag moves (see
`scripts/move-nightly-tag.sh`; a counter rewind on Codeberg in 2026-07 is why
the refs exist, and they were copied over when the project moved to GitHub).
The survey only sees GitHub: codes that exist solely on Google Play (such as
the Flutter era's `base*10` AAB codes, whose markers recorded the 9-digit base)
are invisible to it, so `baseVersionCode` must be kept at or above every code
any channel has ever served; the fastlane publish lanes additionally compare
the incoming code against the live Play track before uploading. Production runs
reuse the marker from the already published `vX.Y.Z` release so the Play AAB
matches the GitHub APK's install order. The nightly run also prunes old
versioned GitHub releases so only the newest nine remain, while preserving the
fixed `nightly` release and all Git tags. The production run uploads the signed
release AAB to Google Play production, promotes the matching GitHub prerelease
to stable, and then posts the full release notes to the Zulip `releases`
channel under a `vX.Y.Z` topic (`scripts/announce-zulip.sh`). A Mastodon
announcement from `https://techhub.social/@openvitals`
(`scripts/announce-mastodon.sh`, the release notes' narrative paragraph plus
links to the GitHub release and the Play listing) is disabled for now: its job
is commented out in `.github/workflows/release.yml`. Both announcement scripts
check for an existing post for the tag first, so re-running production never
announces twice.

The scheduled run only skips the build when there is nothing new to publish:
either the `nightly` tag already points at HEAD, or HEAD has no commits after
the latest versioned `vX.Y.Z` release. Manual runs always build so an operator
can force a refresh.

The workflow's own `GITHUB_TOKEN` creates and edits releases, pushes the
`nightly` tag and the `refs/version-code/*` refs; a tag ruleset that blocks
`nightly` or `refs/version-code/*` breaks the nightly. Configure these
repository Actions secrets:

- `OPENVITALS_RELEASE_KEYSTORE_BASE64`, `OPENVITALS_RELEASE_STORE_PASSWORD`,
  `OPENVITALS_RELEASE_KEY_ALIAS`, and `OPENVITALS_RELEASE_KEY_PASSWORD`, so CI
  can produce updateable Debug, nightly, and release APKs.
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64`, the base64-encoded JSON key for a
  Google Play service account that can release to open testing and production
  for `tech.mmarca.openvitals`. If the account is allowed to stage edits but not
  send them for review, set the repository Actions variable
  `OPENVITALS_PLAY_CHANGES_NOT_SENT_FOR_REVIEW=true`; the open testing or
  production release will then need to be sent for review manually in Play
  Console.
- `ZULIP_BOT_EMAIL` and `ZULIP_BOT_API_KEY` for a generic bot on
  `openvitals.zulipchat.com` (Personal settings > Bots > Add a new bot) that is
  subscribed to the `releases` channel; only the `announce-zulip` job sees them.
- `MASTODON_ACCESS_TOKEN`, only if the Mastodon job comes back: an access token
  for the `@openvitals@techhub.social` account (Preferences > Development > New
  application, scopes `write:statuses` and `read:accounts`, redirect URI left at
  the default `urn:ietf:wg:oauth:2.0:oob` - no OAuth flow is used; copy the
  "Your access token" shown on the application page).

The Play production upload runs in the `production` environment. Add required
reviewers to that environment so a production run waits for approval.

## Release Checklist

For a versioned prerelease:

1. Bump `baseVersionName` in `app/build.gradle.kts`. Let `scripts/release.sh`
   bump `baseVersionCode` with `scripts/version-code.sh next`; do not derive the
   code from `vX.Y.Z`.
2. When preparing a store release, add Play changelog files under
   `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
3. Add the user-facing release summary to `CHANGELOG.md`.
4. Update README or docs when the user-facing navigation, permissions, screenshots, or bundled assets change.
5. Run:

```bash
./gradlew verifyCi
git diff --check
```

   CI has no device, so it never runs the instrumented tests. A release does
   not need them. To run them anyway, connect a phone or emulator:

```bash
ANDROID_SERIAL=<serial> ./gradlew verifyAndroidTest
```

6. Commit the release prep, tag the commit as an annotated `v<versionName>` tag
   such as `v0.7.0` using the matching `CHANGELOG.md` section as the tag
   message, and push both the branch and tag. The tag run of the release
   workflow runs the release checks and publishes the versioned GitHub
   prerelease APK.
7. After validation, run the release workflow by hand (Actions > release > Run
   workflow) from the version tag, with target `production`. Once the
   `production` environment approves it, the run uploads the signed release AAB
   to Google Play production, promotes the matching GitHub prerelease to
   stable, and posts the release notes to the Zulip `releases` channel.

For an immediate nightly release, run the release workflow by hand from `main`
with target `nightly`. It runs the same release checks, moves the `nightly` tag
to the commit, publishes the APK to the mutable GitHub `nightly` prerelease, and
uploads the signed AAB to Google Play open testing. The schedule does the same
at midnight UTC.

Use the exact `versionName` for release notes, changelog references, and tags.
For a final release, that means file name, `versionName`, and tag such as
`1.0.0` / `v1.0.0`. Keep the Play `versionCode` unique and increasing, and add
matching Fastlane changelog files for that exact code when preparing a store
release.
