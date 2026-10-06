#!/usr/bin/env bash
# Create or update the GitHub release for a tag and (re)upload its assets.
#
# Usage: scripts/publish-github-release.sh <tag> <title-file> <notes-file> <prerelease:true|false> <asset>...
#
# Needs GH_TOKEN (and GH_REPO outside a checkout). The tag must already exist:
# a vX.Y.Z tag is pushed by scripts/release.sh, and the nightly tag is moved by
# scripts/move-nightly-tag.sh before this runs.
set -euo pipefail

usage() {
    echo "Usage: $0 <tag> <title-file> <notes-file> <prerelease:true|false> <asset>..." >&2
}

if [ "$#" -lt 5 ]; then
    usage
    exit 1
fi

release_tag="$1"
title_file="$2"
notes_file="$3"
prerelease="$4"
shift 4

case "$prerelease" in
    true|false) ;;
    *)
        echo "prerelease must be true or false" >&2
        exit 1
        ;;
esac

if [ ! -s "$title_file" ] || [ ! -f "$notes_file" ]; then
    echo "title and notes files are required" >&2
    exit 1
fi

for asset_path in "$@"; do
    if [ ! -s "$asset_path" ]; then
        echo "asset is missing or empty: $asset_path" >&2
        exit 1
    fi
done

release_title="$(sed -n '1p' "$title_file")"

# A dropped connection must not leave a release without its APK.
retry() {
    attempt=1
    until "$@"; do
        if [ "$attempt" -ge 5 ]; then
            return 1
        fi
        echo "Retrying ($attempt/4): $*" >&2
        sleep $((attempt * 3))
        attempt=$((attempt + 1))
    done
}

if gh release view "$release_tag" >/dev/null 2>&1; then
    retry gh release edit "$release_tag" \
        --title "$release_title" \
        --notes-file "$notes_file" \
        --prerelease="$prerelease" \
        --draft=false
else
    retry gh release create "$release_tag" \
        --verify-tag \
        --title "$release_title" \
        --notes-file "$notes_file" \
        --prerelease="$prerelease" \
        --latest=false
fi

# --clobber replaces same-named assets, which is what keeps the nightly page's
# download links stable.
retry gh release upload "$release_tag" --clobber "$@"

echo "Published $release_tag with $# asset(s)."
