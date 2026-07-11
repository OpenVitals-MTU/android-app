#!/usr/bin/env sh
set -eu

# Prepares the CI container for a Flutter/Android build, then execs `flutter "$@"`.
# Replaces the Kotlin app's ci-android-gradle.sh + ci-android-setup.sh + ci-gradle.sh.

# --- Native-assets toolchain (needed by EVERY flutter command) -----------------
# vector_map_tiles 10.x draws tiles on the GPU via flutter_scene, whose
# flutter_scene_importer package has a native-assets build hook that shells out to
# CMake (>= 3.21) and Ninja. Those hooks run for `flutter test` and `flutter analyze`
# too -- not just builds -- so without a toolchain every step dies with
# "Building native assets failed" and a bare `ProcessException: No such file or
# directory / Command: cmake`, which does not name the missing tool.
#
# The Flutter image does not ship CMake. Install it once, here.
CMAKE_MIN_MAJOR=3
CMAKE_MIN_MINOR=21

cmake_is_new_enough() {
    command -v cmake >/dev/null 2>&1 || return 1
    v="$(cmake --version 2>/dev/null | head -n 1 | sed -n 's/.* \([0-9][0-9]*\)\.\([0-9][0-9]*\).*/\1 \2/p')"
    [ -n "$v" ] || return 1
    maj="${v% *}"; min="${v#* }"
    [ "$maj" -gt "$CMAKE_MIN_MAJOR" ] && return 0
    [ "$maj" -eq "$CMAKE_MIN_MAJOR" ] && [ "$min" -ge "$CMAKE_MIN_MINOR" ]
}

if ! cmake_is_new_enough || ! command -v ninja >/dev/null 2>&1; then
    echo "Installing the native-assets toolchain (cmake >= $CMAKE_MIN_MAJOR.$CMAKE_MIN_MINOR, ninja, clang)"
    sudo_cmd=""
    [ "$(id -u)" -ne 0 ] && command -v sudo >/dev/null 2>&1 && sudo_cmd="sudo"
    if command -v apt-get >/dev/null 2>&1; then
        $sudo_cmd apt-get update -qq
        $sudo_cmd apt-get install -y -qq --no-install-recommends \
            cmake ninja-build clang pkg-config >/dev/null
    else
        echo "No apt-get available; cannot install the native-assets toolchain." >&2
        exit 1
    fi
fi

# Fail here, naming the tool, rather than inside a build hook that does not.
if ! cmake_is_new_enough; then
    echo "cmake >= $CMAKE_MIN_MAJOR.$CMAKE_MIN_MINOR is required by flutter_scene_importer's build hook." >&2
    echo "Found: $(cmake --version 2>/dev/null | head -n 1 || echo 'none')" >&2
    exit 1
fi
command -v ninja >/dev/null 2>&1 || { echo "ninja is required by the native-assets build hook." >&2; exit 1; }

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
