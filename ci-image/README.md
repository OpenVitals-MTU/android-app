# Prebaked CI image

`Dockerfile` here extends `ghcr.io/cirruslabs/flutter:<version>` with the
native-assets toolchain (cmake, ninja, clang) and the Android platform +
build-tools this app compiles against — the things `scripts/ci-flutter.sh` would
otherwise `apt-get` / `sdkmanager` from the network on **every** pipeline step.

With them baked in, `ci-flutter.sh`'s "already installed?" checks pass instantly,
so nothing is installed at CI time. That removes a couple of minutes per pipeline
and a recurring network-flake failure mode. No `ci-flutter.sh` change is needed.

## Build & push

```sh
docker login codeberg.org        # username + a token with package:write scope
REGISTRY_IMAGE=codeberg.org/OpenVitals/mobile-app-ci sh scripts/build-ci-image.sh
```

Then make the package **public** (so CI pulls it without credentials) and point
the `&flutter_image` anchor in `.woodpecker/test.yml` and
`.woodpecker/release.yml` at the pushed reference.

## Rebuild when

- **Flutter version changes** — rebuild with the new `FLUTTER_VERSION` and bump
  both the image tag here and the `&flutter_image` tag in the pipelines together.
- **`compileSdk` changes** (`android/app/build.gradle.kts`) — bump
  `ANDROID_PLATFORM` / `ANDROID_BUILD_TOOLS` here to match the defaults in
  `scripts/ci-flutter.sh`.

The image is a pure convenience layer: if it is ever unavailable, reverting the
`&flutter_image` anchors to `ghcr.io/cirruslabs/flutter:<version>` restores the
old behaviour, because `ci-flutter.sh` still installs the tools when they are
missing.
