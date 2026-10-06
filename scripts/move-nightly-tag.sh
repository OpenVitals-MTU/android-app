#!/usr/bin/env sh
# Force-move the mutable `nightly` tag to the built commit, and record the
# versionCode it ships before anything else happens.
#
# Usage: scripts/move-nightly-tag.sh <tag> <target-sha> <message> <version-code>
#
# Needs push access to origin (actions/checkout leaves the workflow token in the
# git config) and GH_TOKEN for the gh CLI.
#
# The code is mirrored into an append-only `refs/version-code/<code>` ref first.
# Release bodies alone are not a safe counter: a release deleted by hand or by
# scripts/prune-github-releases.sh takes its marker with it, and a counter that
# rewinds ships an APK with a LOWER versionCode than the one already installed --
# which Android refuses with nothing more helpful than "App not installed".
# Observed 2026-07 on Codeberg, 107030418 -> 107030415. scripts/version-code.sh
# folds those refs into its max.
#
# GitHub does not pin a tag to its release, so unlike Codeberg the tag moves with
# a plain force-push and the nightly release is edited in place afterwards.
set -eu

if [ "$#" -ne 4 ]; then
    echo "Usage: $0 <tag> <target-sha> <message> <version-code>" >&2
    exit 1
fi

tag_name="$1"
target="$2"
message="$3"
version_code="$4"

case "$version_code" in
    ''|*[!0-9]*)
        echo "version-code must be a positive integer, got: ${version_code:-<empty>}" >&2
        exit 1
        ;;
esac

# Burn the code first, so it is on record whatever happens to the release or to
# the rest of this workflow. Reusing a code is harmless; rewinding is not.
# Forced because a re-run may legitimately reissue a code against a different
# commit; what the counter reads is the ref NAME.
ref="$(sh scripts/version-code.sh ref "$version_code")"
git update-ref "$ref" "$target"
git push --force origin "$ref"
echo "Recorded versionCode $version_code at $ref"

# The release still carries the previous marker, which makes this the one place
# that can see a rewind before it ships. Refuse an APK Android cannot install
# over the one it replaces.
previous_code="$(
    gh release view "$tag_name" --json body --jq .body 2>/dev/null |
        sed -n 's/.*OpenVitals-Version-Code:[[:space:]]*\([0-9][0-9]*\).*/\1/p' |
        sort -n | tail -n 1 || true
)"
if [ -n "$previous_code" ] && [ "$version_code" -le "$previous_code" ]; then
    echo "versionCode $version_code is not above the $previous_code already published on $tag_name." >&2
    echo "The counter has rewound -- see scripts/version-code.sh -- and this build would install nowhere." >&2
    exit 1
fi

# Annotated tag at the built commit; identity is inline so no git config is touched.
git -c user.name="OpenVitals CI" -c user.email="ci@openvitals.invalid" \
    tag -f -a "$tag_name" -m "$message" "$target"
# Pushing the same commit again is a no-op, so this is safe to re-run.
git push --force origin "refs/tags/$tag_name"
echo "Moved $tag_name tag to $target"
