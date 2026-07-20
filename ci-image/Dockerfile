# syntax=docker/dockerfile:1
#
# Prebaked OpenVitals CI image.
#
# Extends the cirruslabs Flutter image with the two things scripts/ci-flutter.sh
# would otherwise install from the network on EVERY pipeline step:
#   * the native-assets toolchain (cmake >= 3.21, ninja, clang) that the
#     vector_map_tiles / flutter_scene build hooks shell out to, and
#   * the Android platform + build-tools this app compiles against.
#
# With these baked in, ci-flutter.sh's "already installed?" checks pass instantly,
# so no apt-get / sdkmanager runs at CI time -- cutting a couple of minutes per
# pipeline and removing a whole network-flake failure mode. No ci-flutter.sh change
# is needed: it simply finds the tools already present and skips the installs.
#
# Rebuild (scripts/build-ci-image.sh) and bump the image tag in the pipelines when:
#   * FLUTTER_VERSION changes -- keep it in lockstep with the &flutter_image tag in
#     .woodpecker/test.yml and .woodpecker/release.yml, and
#   * ANDROID_PLATFORM / ANDROID_BUILD_TOOLS change -- keep them in lockstep with
#     compileSdk in android/app/build.gradle.kts and the defaults in ci-flutter.sh.
ARG FLUTTER_VERSION=3.44.0
FROM ghcr.io/cirruslabs/flutter:${FLUTTER_VERSION}

# Minor-versioned platform package: `android-37.0`, NOT `android-37` (see the long
# note in scripts/ci-flutter.sh).
ARG ANDROID_PLATFORM=android-37.0
ARG ANDROID_BUILD_TOOLS=37.0.0

# Native-assets toolchain -- mirrors the apt-get in scripts/ci-flutter.sh.
RUN apt-get update -qq \
 && apt-get install -y -qq --no-install-recommends \
      cmake ninja-build clang pkg-config \
 && rm -rf /var/lib/apt/lists/*

# Android platform + build-tools -- mirrors the sdkmanager in scripts/ci-flutter.sh.
# Accept licenses first so the installs are non-interactive.
RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true \
 && yes | sdkmanager "platforms;${ANDROID_PLATFORM}" "build-tools;${ANDROID_BUILD_TOOLS}"

# Fail the IMAGE BUILD (not a later CI run) if anything did not land where
# ci-flutter.sh looks for it.
RUN sdk_root="${ANDROID_SDK_ROOT:-$ANDROID_HOME}" \
 && command -v cmake  >/dev/null || { echo "cmake missing"  >&2; exit 1; } \
 && command -v ninja  >/dev/null || { echo "ninja missing"  >&2; exit 1; } \
 && command -v clang  >/dev/null || { echo "clang missing"  >&2; exit 1; } \
 && test -d "$sdk_root/platforms/${ANDROID_PLATFORM}"     || { echo "platform ${ANDROID_PLATFORM} missing"    >&2; exit 1; } \
 && test -d "$sdk_root/build-tools/${ANDROID_BUILD_TOOLS}" || { echo "build-tools ${ANDROID_BUILD_TOOLS} missing" >&2; exit 1; }
