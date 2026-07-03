#!/usr/bin/env sh
set -eu

git fetch --tags --force origin
mkdir -p .woodpecker/tmp

apk_abi_filters="${OPENVITALS_APK_ABI_FILTERS:-armeabi-v7a,arm64-v8a}"
release_tag="${CI_COMMIT_TAG:-}"

if [ "${CI_PIPELINE_EVENT:-}" = "cron" ] || [ "${CI_PIPELINE_EVENT:-}" = "manual" ]; then
    release_tag="nightly"
elif [ -z "$release_tag" ]; then
    case "${CI_COMMIT_REF:-}" in
        refs/tags/*)
            release_tag="${CI_COMMIT_REF#refs/tags/}"
            ;;
        *)
            if [ "${CI_PIPELINE_EVENT:-}" = "deployment" ]; then
                release_tag="$(git tag --points-at "${CI_COMMIT_SHA:?}" | grep -E '^[vV][0-9]+\.[0-9]+\.[0-9]+$' | tail -n 1 || true)"
            fi
            ;;
    esac
fi

gradle_task=":app:assembleRelease"
bundle_task=""
apk_variant_dir="release"
aab_variant_dir=""
aab_basename=""
build_aab="false"
build_debug_apk="false"
play_track=""
version_code=""
version_name_override=""
debug_apk_basename=""

configured_version_code="$(sed -n 's/.*baseVersionCode = \([0-9][0-9]*\).*/\1/p' app/build.gradle.kts | head -n 1)"
if [ -z "$configured_version_code" ]; then
    echo "Could not read baseVersionCode from app/build.gradle.kts." >&2
    exit 1
fi

version_name="$(sed -n 's/.*baseVersionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)"
if ! printf '%s\n' "$version_name" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "Release builds require a numeric baseVersionName, found $version_name." >&2
    exit 1
fi

if [ "$release_tag" = "nightly" ]; then
    release_channel="nightly"
    gradle_task=":app:assembleNightly"
    apk_variant_dir="nightly"
    apk_basename="OpenVitals-nightly.apk"
    bundle_task=":app:bundleNightly"
    aab_variant_dir="nightly"
    aab_basename="OpenVitals-nightly.aab"
    release_title="OpenVitals Nightly"
    prerelease="true"
    build_aab="true"
    build_debug_apk="true"
    play_track="beta"
    debug_apk_basename="OpenVitals-nightly-debug.apk"
    pipeline_number="${CI_PIPELINE_NUMBER:-1}"

    if ! printf '%s\n' "$pipeline_number" | grep -Eq '^[0-9][0-9]*$'; then
        echo "CI_PIPELINE_NUMBER must be numeric for nightly versionName generation, found $pipeline_number." >&2
        exit 1
    fi

    version_code="$(sh scripts/version-code.sh next --floor "$configured_version_code")"
    version_name_override="$version_name-nightly.$pipeline_number"
else
    release_channel="release"
    apk_basename="OpenVitals-$release_tag.apk"
    release_title="OpenVitals $release_tag"
    prerelease="true"

    if [ -z "$release_tag" ]; then
        echo "Release tag is required." >&2
        exit 1
    fi

    if ! printf '%s\n' "$release_tag" | grep -Eq '^[vV][0-9]+\.[0-9]+\.[0-9]+$'; then
        echo "Release tags must use vX.Y.Z semantic version format, for example v1.8.0 or V1.8.0." >&2
        exit 1
    fi

    if [ "$version_name" != "${release_tag#[vV]}" ]; then
        echo "Release tag $release_tag does not match app versionName $version_name." >&2
        exit 1
    fi

    if [ "${CI_PIPELINE_EVENT:-}" = "deployment" ]; then
        prerelease="false"
        bundle_task=":app:bundleRelease"
        aab_variant_dir="release"
        aab_basename="OpenVitals-$release_tag.aab"
        build_aab="true"
        play_track="production"
        version_code="$(sh scripts/version-code.sh for-tag "$release_tag" --floor "$configured_version_code")"
    else
        build_debug_apk="true"
        debug_apk_basename="OpenVitals-$release_tag-debug.apk"
        previous_configured_version_code="$((configured_version_code - 1))"
        expected_version_code="$(sh scripts/version-code.sh next --floor "$previous_configured_version_code")"
        if [ "$expected_version_code" != "$configured_version_code" ]; then
            echo "baseVersionCode $configured_version_code is stale; next expected versionCode is $expected_version_code." >&2
            exit 1
        fi
        version_code="$configured_version_code"
    fi
fi

printf '%s\n' "$release_title" > .woodpecker/tmp/release-title.txt

notes_file=".woodpecker/tmp/release-notes.md"
if [ "$release_channel" = "release" ]; then
    : > "$notes_file"
    if git rev-parse -q --verify "refs/tags/$release_tag" >/dev/null; then
        tag_object_type="$(git cat-file -t "$release_tag")"
        if [ "$tag_object_type" = "tag" ]; then
            git for-each-ref "refs/tags/$release_tag" --format='%(contents)' > "$notes_file"
        fi
    fi
    if [ ! -s "$notes_file" ]; then
        printf '%s\n' \
            "$release_title" \
            "" \
            "Versioned prerelease build from commit ${CI_COMMIT_SHA:?}." \
            "" \
            "Assets:" \
            "- Signed release APK ($apk_abi_filters)" \
            > "$notes_file"
        if [ "$build_debug_apk" = "true" ]; then
            printf '%s\n' "- Signed Debug APK ($apk_abi_filters)" >> "$notes_file"
            printf '%s\n' "- SHA-256 checksums" >> "$notes_file"
        else
            printf '%s\n' "- SHA-256 checksum" >> "$notes_file"
        fi
    fi
else
    printf '%s\n' \
        "$release_title" \
        "" \
        "Mutable $release_channel build from commit ${CI_COMMIT_SHA:?}." \
        "" \
        "This release keeps a stable download page. The APK and checksum assets are replaced by the next $release_channel build." \
        "" \
        "Assets:" \
        "- Signed $release_channel APK ($apk_abi_filters)" \
        > "$notes_file"
    if [ "$build_debug_apk" = "true" ]; then
        printf '%s\n' "- Signed Debug APK ($apk_abi_filters)" >> "$notes_file"
        printf '%s\n' "- SHA-256 checksums" >> "$notes_file"
    else
        printf '%s\n' "- SHA-256 checksum" >> "$notes_file"
    fi
fi

if [ -n "$version_code" ]; then
    {
        printf '\n'
        sh scripts/version-code.sh marker "$version_code"
    } >> "$notes_file"
fi

{
    printf 'OPENVITALS_RELEASE_CHANNEL=%s\n' "$release_channel"
    printf 'OPENVITALS_RELEASE_TAG=%s\n' "$release_tag"
    printf 'OPENVITALS_GRADLE_TASK=%s\n' "$gradle_task"
    printf 'OPENVITALS_BUNDLE_TASK=%s\n' "$bundle_task"
    printf 'OPENVITALS_APK_VARIANT_DIR=%s\n' "$apk_variant_dir"
    printf 'OPENVITALS_APK_BASENAME=%s\n' "$apk_basename"
    printf 'OPENVITALS_AAB_VARIANT_DIR=%s\n' "$aab_variant_dir"
    printf 'OPENVITALS_AAB_BASENAME=%s\n' "$aab_basename"
    printf 'OPENVITALS_APK_ABI_FILTERS=%s\n' "$apk_abi_filters"
    printf 'OPENVITALS_RELEASE_PRERELEASE=%s\n' "$prerelease"
    printf 'OPENVITALS_RELEASE_TARGET=%s\n' "${CI_COMMIT_SHA:?}"
    printf 'OPENVITALS_BUILD_AAB=%s\n' "$build_aab"
    printf 'OPENVITALS_BUILD_DEBUG_APK=%s\n' "$build_debug_apk"
    printf 'OPENVITALS_PLAY_TRACK=%s\n' "$play_track"
    printf 'OPENVITALS_VERSION_CODE=%s\n' "$version_code"
    printf 'OPENVITALS_VERSION_NAME=%s\n' "$version_name_override"
    printf 'OPENVITALS_DEBUG_APK_BASENAME=%s\n' "$debug_apk_basename"
} > .woodpecker/tmp/release-context.env
