# Prebaked CI image

`Dockerfile` here extends `ghcr.io/cirruslabs/android-sdk:tools` with the
Android platform and build-tools this app compiles against — the things
`scripts/ci-android-setup.sh` would otherwise install from the network on every
pipeline step. Baked in, its "already installed?" check passes instantly: no
`sdkmanager` at CI time, minutes saved per pipeline, one network-flake failure
mode gone.

This is the Android adaptation of the image the Flutter era ran; same design,
different base.

## Using it

Both pipelines run `ghcr.io/mmarca-tech/openvitals-android-ci:android-37`
(pushed 2026-08-04). The package is **private** on ghcr — the Woodpecker
runner's registry credentials cover it, exactly as they did the Flutter era's
image; making it public in the GitHub package settings would remove that
dependency, and nothing in the image is secret.

To rebuild after a `compileSdk` bump:

1. Update `ANDROID_PLATFORM` / `ANDROID_BUILD_TOOLS` here, in
   `app/build.gradle.kts`, and in `scripts/ci-android-setup.sh` — they must
   stay in lockstep.
2. `scripts/build-ci-image.sh --push` (needs `docker login ghcr.io` with a
   package-write token) and bump the tag in `.woodpecker/*.yml`.
