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

After a successful `main` push pipeline, Woodpecker mirrors the checked commit to
`git@github.com:mmarca-tech/OpenVitals.git`. Configure the Woodpecker secret
`GITHUB_MIRROR_SSH_KEY` with a private SSH key whose public key is installed as a
write-enabled deploy key on the GitHub mirror repository.
