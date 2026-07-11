#!/usr/bin/env sh
set -eu

# Prepares the CI container for a Flutter/Android build, then execs `flutter "$@"`.
# Replaces the Kotlin app's ci-android-gradle.sh + ci-android-setup.sh + ci-gradle.sh.

# --- Android SDK (only when we are actually building for Android) -------------
# `flutter test` and `flutter analyze` run on the Dart VM and never touch the Android
# SDK, so provisioning it for them is pure cost -- and a failure point that has no
# business breaking a pull-request pipeline. Only `flutter build` needs it.
case "${1:-}" in
build)
    # compileSdk = 37 (android/app/build.gradle.kts): Health Connect connect-client
    # 1.2.0-alpha04 resolves its record/permission mappings against API 37. The image
    # ships an older platform, so install it if missing. Idempotent.
    #
    # The package is `android-37.0`, NOT `android-37`. Android now ships
    # minor-versioned platforms (android-36, android-36.1, android-37.0), and
    # sdkmanager only WARNS on an unknown package -- it exits 0. A wrong name here
    # therefore does not fail this step; it fails much later inside Gradle looking
    # like something else entirely. Hence the assertions.
    ANDROID_PLATFORM_DIR="${ANDROID_PLATFORM_DIR:-android-37.0}"
    ANDROID_BUILD_TOOLS="${ANDROID_BUILD_TOOLS:-37.0.0}"

    sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [ -z "$sdk_root" ] && [ -f local.properties ]; then
        sdk_root="$(sed -n 's/^sdk\.dir=\(.*\)$/\1/p' local.properties | head -n 1)"
    fi
    if [ -z "$sdk_root" ]; then
        echo "Android SDK not found: set ANDROID_HOME or ANDROID_SDK_ROOT." >&2
        exit 1
    fi

    if [ ! -d "$sdk_root/platforms/$ANDROID_PLATFORM_DIR" ] ||
       [ ! -d "$sdk_root/build-tools/$ANDROID_BUILD_TOOLS" ]; then
        echo "Installing Android platform $ANDROID_PLATFORM_DIR / build-tools $ANDROID_BUILD_TOOLS"
        yes | sdkmanager \
            "platforms;$ANDROID_PLATFORM_DIR" \
            "build-tools;$ANDROID_BUILD_TOOLS" >/dev/null || true
    fi

    # Fail here, loudly, rather than deep inside Gradle.
    if [ ! -d "$sdk_root/platforms/$ANDROID_PLATFORM_DIR" ]; then
        echo "Android platform $ANDROID_PLATFORM_DIR missing after sdkmanager ran." >&2
        echo "Available: $(ls "$sdk_root/platforms" 2>/dev/null | tr '\n' ' ')" >&2
        exit 1
    fi
    if [ ! -d "$sdk_root/build-tools/$ANDROID_BUILD_TOOLS" ]; then
        echo "Android build-tools $ANDROID_BUILD_TOOLS missing after sdkmanager ran." >&2
        echo "Available: $(ls "$sdk_root/build-tools" 2>/dev/null | tr '\n' ' ')" >&2
        exit 1
    fi

    printf 'sdk.dir=%s\n' "$sdk_root" > local.properties
    ;;
esac

# --- Caches ------------------------------------------------------------------
# Prefer a mounted Woodpecker cache volume when one exists; otherwise fall back to
# the pipeline workspace, which persists across STEPS of one pipeline but not across
# pipelines. Written this way so that re-adding a named volume at /woodpecker/cache
# restores cross-pipeline caching with no change to the YAML.
workspace="${CI_WORKSPACE:-$PWD}"

if [ -z "${GRADLE_USER_HOME:-}" ]; then
    if [ -d /woodpecker/cache/gradle ]; then
        GRADLE_USER_HOME=/woodpecker/cache/gradle
    else
        GRADLE_USER_HOME="$workspace/.gradle-ci"
    fi
fi
mkdir -p "$GRADLE_USER_HOME"
export GRADLE_USER_HOME

if [ -z "${PUB_CACHE:-}" ]; then
    if [ -d /woodpecker/cache/pub ]; then
        PUB_CACHE=/woodpecker/cache/pub
    else
        PUB_CACHE="$workspace/.pub-cache-ci"
    fi
fi
mkdir -p "$PUB_CACHE"
export PUB_CACHE

# The image's Flutter checkout is usually owned by another user; without this, every
# git-backed Flutter command fails with "detected dubious ownership".
if command -v git >/dev/null 2>&1; then
    git config --global --add safe.directory '*' || true
fi

flutter --version
flutter pub get

exec flutter "$@"
