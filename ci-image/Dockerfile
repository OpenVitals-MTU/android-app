# syntax=docker/dockerfile:1
#
# Prebaked OpenVitals CI image.
#
# Extends the cirruslabs Android SDK image with the platform and build-tools
# this app compiles against — the two things scripts/ci-android-setup.sh would
# otherwise install from the network on EVERY pipeline step. With them baked
# in, its "already installed?" check passes instantly, so no sdkmanager runs at
# CI time — cutting minutes per pipeline and removing a network-flake failure
# mode. No script change is needed: it finds the packages present and skips.
#
# This is the Android adaptation of the image the Flutter era ran
# (mobile-app/ci-image); same design, different base.
#
# Rebuild (scripts/build-ci-image.sh) and bump the tag in the pipelines when
# ANDROID_PLATFORM / ANDROID_BUILD_TOOLS change — keep them in lockstep with
# compileSdk in app/build.gradle.kts and scripts/ci-android-setup.sh.
FROM ghcr.io/cirruslabs/android-sdk:tools

# Minor-versioned platform directory: `android-37.0`, matching what
# ci-android-setup.sh checks for.
ARG ANDROID_PLATFORM=android-37.0
ARG ANDROID_BUILD_TOOLS=37.0.0

RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true \
 && yes | sdkmanager "platforms;${ANDROID_PLATFORM}" "build-tools;${ANDROID_BUILD_TOOLS}"

# Fail the IMAGE BUILD (not a later CI run) if anything did not land where
# ci-android-setup.sh looks for it.
RUN sdk_root="${ANDROID_SDK_ROOT:-$ANDROID_HOME}" \
 && test -d "$sdk_root/platforms/${ANDROID_PLATFORM}"      || { echo "platform ${ANDROID_PLATFORM} missing"    >&2; exit 1; } \
 && test -d "$sdk_root/build-tools/${ANDROID_BUILD_TOOLS}" || { echo "build-tools ${ANDROID_BUILD_TOOLS} missing" >&2; exit 1; }
