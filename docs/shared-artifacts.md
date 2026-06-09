# Shared Maven Artifacts

## Purpose

The local OpenVitals Android app remains the no-internet, Health Connect-only app in this repository.

Code that should be reused by a separate connected app is extracted into Gradle modules and published as versioned Maven artifacts. The connected app should consume those artifacts, or use a Gradle composite build during local development, instead of copying source code from this repository.

## Current Artifacts

| Gradle project | Maven coordinate | Purpose |
| --- | --- | --- |
| `:openvitals-core-period` | `tech.mmarca.openvitals:openvitals-core-period:<version>` | Shared period range, date window, and period navigation primitives. |

Artifact versions default to `1.3.0-SNAPSHOT`.

Override the version with either:

```bash
./gradlew publishOpenVitalsArtifactsToMavenLocal -PopenVitalsArtifactVersion=1.3.0
```

or:

```bash
OPENVITALS_ARTIFACT_VERSION=1.3.0 ./gradlew publishOpenVitalsArtifactsToMavenLocal
```

## Local Publishing

Publish to the local Maven cache:

```bash
./gradlew publishOpenVitalsArtifactsToMavenLocal
```

Publish to a repository under this checkout:

```bash
./gradlew publishOpenVitalsArtifactsToLocalRepository
```

That writes artifacts to:

```text
build/openvitals-maven-repository
```

## Remote Publishing

The shared artifact module supports a configurable Maven repository:

```bash
./gradlew :openvitals-core-period:publishMavenPublicationToOpenVitalsReleaseRepository \
  -PopenVitalsArtifactVersion=1.3.0 \
  -PopenVitalsMavenRepositoryUrl=https://example.invalid/maven \
  -PopenVitalsMavenUsername=... \
  -PopenVitalsMavenPassword=...
```

The same values can be provided by environment variables:

- `OPENVITALS_ARTIFACT_VERSION`
- `OPENVITALS_MAVEN_REPOSITORY_URL`
- `OPENVITALS_MAVEN_USERNAME`
- `OPENVITALS_MAVEN_PASSWORD`

## Connected Repo Consumption

In the connected app repository:

```kotlin
dependencies {
    implementation("tech.mmarca.openvitals:openvitals-core-period:1.3.0")
}
```

During local development, prefer a composite build:

```kotlin
// connected repo settings.gradle.kts
includeBuild("../android-app")
```

Because the shared project is named `:openvitals-core-period`, Gradle can substitute the published coordinate from the included build.

## Rules

- Do not publish the local `:app` module as a dependency.
- Do not add connected app code, auth, API clients, or social features to this repository.
- Extract shared code into publishable modules when the connected app needs it.
- Keep artifact modules independent from app-only resources and app shell wiring.
