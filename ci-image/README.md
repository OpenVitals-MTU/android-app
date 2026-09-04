# Prebaked CI image

`Dockerfile` here extends `ghcr.io/cirruslabs/android-sdk:tools` with the
Android platform and build-tools this app compiles against — the things
`scripts/ci-android-setup.sh` would otherwise install from the network on every
pipeline step. Baked in, its "already installed?" check passes instantly: no
`sdkmanager` at CI time, minutes saved per pipeline, one network-flake failure
mode gone.

This is the Android adaptation of the image the Flutter era ran; same design,
different base.

## Local APK build

From the repository root, run:

```bash
scripts/build-apk-in-docker.sh
```

The script builds this image locally as
`openvitals-android-builder:android-37`, mounts the checkout into it, and runs
`:app:assembleDebug`. The resulting installable APK is copied to:

```text
dist/OpenVitals-debug.apk
```

The host does not need Java, Gradle, or an Android SDK. It only needs Docker
and internet access for the first image/dependency download. Gradle downloads
are retained under the ignored `.docker-cache/gradle` directory, so subsequent
builds are incremental. Files created by Gradle retain the invoking user's UID
instead of becoming root-owned. The generated debug signing key is retained in
`.docker-cache/android`; keep that directory while a Docker-built debug APK is
installed, otherwise Android will reject the next build as a differently signed
application.

To choose a different local image name:

```bash
OPENVITALS_ANDROID_BUILDER_IMAGE=my-openvitals-builder:latest \
  scripts/build-apk-in-docker.sh
```

This intentionally builds the Debug variant, which uses the separate
`tech.mmarca.openvitals.debug` application ID and needs no release keystore.
Release and Nightly artifacts must continue through the documented signing
workflow; do not copy signing secrets into an image.

## Using it

Both pipelines run `ghcr.io/mmarca-tech/openvitals-android-ci:android-37`
(pushed 2026-08-04). The package must be **public** on ghcr — the Woodpecker
runner pulls anonymously, exactly as it did the Flutter era's public
`mobile-app-ci` image. ghcr creates new packages private, so after the FIRST
push of a new package name, flip it in the GitHub package settings
(https://github.com/users/mmarca-tech/packages/container/openvitals-android-ci/settings
→ Change visibility → Public); a private package fails the pipeline at image
pull with `error from registry: unauthorized`. Nothing in the image is secret —
it is a public base plus the public Android SDK.

To rebuild after a `compileSdk` bump:

1. Update `ANDROID_PLATFORM` / `ANDROID_BUILD_TOOLS` here, in
   `app/build.gradle.kts`, and in `scripts/ci-android-setup.sh` — they must
   stay in lockstep.
2. `scripts/build-ci-image.sh --push` (needs `docker login ghcr.io` with a
   package-write token) and bump the tag in `.woodpecker/*.yml`.
