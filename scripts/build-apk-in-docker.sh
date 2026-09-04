#!/usr/bin/env sh
set -eu

# Builds a locally installable Debug APK without requiring a host Android SDK.
# Docker still needs network access the first time it downloads the base image,
# SDK packages, Gradle distribution, and Maven dependencies. Later builds reuse
# both the Docker layer cache and .docker-cache/gradle.

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
image=${OPENVITALS_ANDROID_BUILDER_IMAGE:-openvitals-android-builder:android-37}
gradle_cache="$repo_dir/.docker-cache/gradle"
android_user_home="$repo_dir/.docker-cache/android"
output_dir="$repo_dir/dist"
source_apk="$repo_dir/app/build/outputs/apk/debug/app-debug.apk"
output_apk="$output_dir/OpenVitals-debug.apk"

mkdir -p "$gradle_cache" "$android_user_home" "$output_dir"

docker build \
    --tag "$image" \
    --file "$repo_dir/ci-image/Dockerfile" \
    "$repo_dir/ci-image"

docker run --rm \
    --user "$(id -u):$(id -g)" \
    --env GRADLE_USER_HOME=/workspace/.docker-cache/gradle \
    --env ANDROID_USER_HOME=/workspace/.docker-cache/android \
    --volume "$repo_dir:/workspace" \
    "$image"

if [ ! -f "$source_apk" ]; then
    echo "Gradle completed but no Debug APK was found at $source_apk" >&2
    exit 1
fi

cp "$source_apk" "$output_apk"
echo "Built $output_apk"
