#!/usr/bin/env sh
set -eu

# Resolves everything the release pipeline needs to know -- ONCE -- and hands it to
# the later steps as a sourceable env file in the shared Woodpecker workspace. Keeping
# this in one place (rather than recomputing per step) is what stops the nightly and
# tag paths from drifting apart.
#
# Outputs, all under .woodpecker/tmp/:
#   release-context.env  sourced by every later step
#   release-title.txt    Codeberg release title
#   release-notes.md     Codeberg release body (carries the version-code marker)

git fetch --tags --force origin
mkdir -p .woodpecker/tmp

# ABI filters, as Kotlin expressed them (ndk.abiFilters). Flutter takes the same
# intent as --target-platform, so we translate below. Two ABIs keeps the APK under
# Codeberg's release-asset size limit.
apk_abi_filters="${OPENVITALS_APK_ABI_FILTERS:-armeabi-v7a,arm64-v8a}"
target_platforms=""
for abi in $(printf '%s' "$apk_abi_filters" | tr ',' ' '); do
    case "$abi" in
        armeabi-v7a) platform="android-arm" ;;
        arm64-v8a)   platform="android-arm64" ;;
        x86_64)      platform="android-x64" ;;
        *)
            echo "Unknown ABI filter '$abi'." >&2
            exit 1
            ;;
    esac
    if [ -z "$target_platforms" ]; then
        target_platforms="$platform"
    else
        target_platforms="$target_platforms,$platform"
    fi
done

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

aab_basename=""
build_aab="false"
play_track=""
version_code=""
version_name_override=""
# Kotlin used a dedicated `nightly` AGP build type whose only real job was
# buildConfigField OPENVITALS_DIAGNOSTICS=true. Flutter has no custom build types, so
# nightly is an ordinary release build plus this dart-define. Without it a nightly
# would ship with NO diagnostics UI, which is the whole point of the channel.
dart_defines=""

# pubspec.yaml carries `version: <name>+<code>` -- the Flutter equivalent of the
# Kotlin app's baseVersionName / baseVersionCode.
configured_version_code="$(sed -n 's/^version:[[:space:]]*[0-9.]*+\([0-9][0-9]*\).*/\1/p' pubspec.yaml | head -n 1)"
if [ -z "$configured_version_code" ]; then
    echo "Could not read the version code from pubspec.yaml (expected 'version: X.Y.Z+CODE')." >&2
    exit 1
fi

version_name="$(sed -n 's/^version:[[:space:]]*\([0-9][0-9.]*\)+.*/\1/p' pubspec.yaml | head -n 1)"
if ! printf '%s\n' "$version_name" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "Release builds require a numeric version name in pubspec.yaml, found $version_name." >&2
    exit 1
fi

if [ "$release_tag" = "nightly" ]; then
    release_channel="nightly"
    aab_basename="OpenVitals-nightly.aab"
    release_title="OpenVitals Nightly"
    prerelease="true"
    build_aab="true"
    play_track="beta"
    dart_defines="--dart-define=OPENVITALS_DIAGNOSTICS=true"
    pipeline_number="${CI_PIPELINE_NUMBER:-1}"

    if ! printf '%s\n' "$pipeline_number" | grep -Eq '^[0-9][0-9]*$'; then
        echo "CI_PIPELINE_NUMBER must be numeric for nightly versionName generation, found $pipeline_number." >&2
        exit 1
    fi

    version_code="$(sh scripts/version-code.sh next --floor "$configured_version_code")"
    version_name_override="$version_name-nightly.$pipeline_number"
else
    release_channel="release"
    release_title="OpenVitals $release_tag"
    prerelease="true"

    if [ -z "$release_tag" ]; then
        echo "Release tag is required." >&2
        exit 1
    fi

    if ! printf '%s\n' "$release_tag" | grep -Eq '^[vV][0-9]+\.[0-9]+\.[0-9]+$'; then
        echo "Release tags must use vX.Y.Z semantic version format, for example v1.10.0 or V1.10.0." >&2
        exit 1
    fi

    if [ "$version_name" != "${release_tag#[vV]}" ]; then
        echo "Release tag $release_tag does not match the pubspec version name $version_name." >&2
        exit 1
    fi

    if [ "${CI_PIPELINE_EVENT:-}" = "deployment" ]; then
        # Promotion to Play production reuses the code already published in this tag's
        # Codeberg release notes, so the AAB carries exactly the code the APK had.
        prerelease="false"
        aab_basename="OpenVitals-$release_tag.aab"
        build_aab="true"
        play_track="production"
        version_code="$(sh scripts/version-code.sh for-tag "$release_tag" --floor "$configured_version_code")"
    else
        # Guard: a nightly may have consumed a code between the pubspec bump and the
        # tag. If so the committed code is stale and would collide on Play.
        previous_configured_version_code="$((configured_version_code - 1))"
        expected_version_code="$(sh scripts/version-code.sh next --floor "$previous_configured_version_code")"
        if [ "$expected_version_code" != "$configured_version_code" ]; then
            echo "pubspec version code $configured_version_code is stale; next expected versionCode is $expected_version_code." >&2
            exit 1
        fi
        version_code="$configured_version_code"
    fi
fi

printf '%s\n' "$release_title" > .woodpecker/tmp/release-title.txt

notes_file=".woodpecker/tmp/release-notes.md"
if [ "$release_channel" = "release" ]; then
    : > "$notes_file"
    # The annotated tag's body (written by scripts/release.sh from docs/releases/X.Y.Z.md)
    # is the release notes.
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
            "- Signed release APK per ABI, named by version code:" \
            "  - OpenVitals-$release_tag-$((version_code * 10 + 2)).apk - arm64-v8a (64-bit; almost every phone)" \
            "  - OpenVitals-$release_tag-$((version_code * 10 + 1)).apk - armeabi-v7a (32-bit; older devices)" \
            "- A SHA-256 checksum beside each" \
            > "$notes_file"
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
        "- Signed $release_channel APK per ABI (arm64-v8a for almost every phone; armeabi-v7a for older 32-bit ones)" \
        "- A SHA-256 checksum beside each" \
        > "$notes_file"
fi

# The marker in the release body IS the version-code database: scripts/version-code.sh
# pages the Codeberg API and takes max(marker) + 1. Dropping it silently resets the
# counter and Play then rejects every subsequent upload.
if [ -n "$version_code" ]; then
    {
        printf '\n'
        sh scripts/version-code.sh marker "$version_code"
    } >> "$notes_file"
fi

# ── The ABI split ──────────────────────────────────────────────────────────────
#
# One APK per ABI, each with its own versionCode, because F-Droid asked for it and
# because the alternative was a 60 MB download of which half was for the other
# architecture. The scheme is F-Droid's:
#
#     versionCode * 10 + abiCode        armeabi-v7a 1, arm64-v8a 2, x86_64 3
#
# arm64 has to outrank armeabi-v7a: a 64-bit phone offered both must take the 64-bit
# one, and the store ranks by versionCode.
#
# It is applied HERE, by building each ABI on its own with `--build-number`, rather
# than in Gradle where F-Droid's own snippet puts it. Gradle cannot: the Flutter Gradle
# Plugin already rewrites the versionCode of every ABI output when it sees
# `--split-per-abi`, from an `afterEvaluate` that runs last, and the two COMPOUND rather
# than replace each other (see the note in android/app/build.gradle.kts).
#
# The app bundle takes `versionCode * 10` with no ABI digit. That keeps Play interleaved
# with the split APKs instead of a billion below them: leave Play on the bare counter and
# anyone who ever installs an F-Droid or Codeberg APK is stranded on that channel, since
# Play's next release would look like a downgrade to Android and never install.
bundle_version_code=""
apk_builds=""
if [ -n "$version_code" ]; then
    bundle_version_code=$((version_code * 10))
    for platform in $(printf '%s' "$target_platforms" | tr ',' ' '); do
        case "$platform" in
            android-arm)   abi="armeabi-v7a"; abi_code=1 ;;
            android-arm64) abi="arm64-v8a";   abi_code=2 ;;
            android-x64)   abi="x86_64";      abi_code=3 ;;
            *)
                echo "No ABI version code is defined for target platform '$platform'." >&2
                exit 1
                ;;
        esac
        abi_version_code=$((version_code * 10 + abi_code))
        # A release names its APKs by version CODE, because that is the only thing
        # F-Droid can template: its `Binaries:` URL takes the version name (%v) and the
        # version code (%c) and knows nothing about ABIs. A nightly names them by ABI
        # instead — its download page is a stable URL that each build overwrites, and a
        # name that changed every night would just pile up dead assets.
        if [ "$release_channel" = "nightly" ]; then
            abi_apk_basename="OpenVitals-nightly-$abi.apk"
        else
            abi_apk_basename="OpenVitals-$release_tag-$abi_version_code.apk"
        fi
        apk_builds="$apk_builds $platform:$abi_version_code:$abi_apk_basename"
    done
    apk_builds="${apk_builds# }"
fi

{
    printf 'OPENVITALS_RELEASE_CHANNEL=%s\n' "$release_channel"
    printf 'OPENVITALS_RELEASE_TAG=%s\n' "$release_tag"
    # Quoted, alone among these: it is the only value holding more than one word (one
    # spec per ABI). This file is SOURCED, so an unquoted space would end the assignment
    # and run the rest of the line as a command.
    printf "OPENVITALS_APK_BUILDS='%s'\n" "$apk_builds"
    printf 'OPENVITALS_BUNDLE_VERSION_CODE=%s\n' "$bundle_version_code"
    printf 'OPENVITALS_AAB_BASENAME=%s\n' "$aab_basename"
    printf 'OPENVITALS_APK_ABI_FILTERS=%s\n' "$apk_abi_filters"
    printf 'OPENVITALS_TARGET_PLATFORMS=%s\n' "$target_platforms"
    printf 'OPENVITALS_DART_DEFINES=%s\n' "$dart_defines"
    printf 'OPENVITALS_RELEASE_PRERELEASE=%s\n' "$prerelease"
    printf 'OPENVITALS_RELEASE_TARGET=%s\n' "${CI_COMMIT_SHA:?}"
    printf 'OPENVITALS_BUILD_AAB=%s\n' "$build_aab"
    printf 'OPENVITALS_PLAY_TRACK=%s\n' "$play_track"
    printf 'OPENVITALS_VERSION_CODE=%s\n' "$version_code"
    printf 'OPENVITALS_VERSION_NAME=%s\n' "$version_name_override"
} > .woodpecker/tmp/release-context.env
