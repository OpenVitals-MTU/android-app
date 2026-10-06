#!/usr/bin/env sh
set -eu

DEFAULT_FLOOR=107030327
MARKER_NAME="OpenVitals-Version-Code"
# Append-only mirror of the counter, pushed by scripts/move-nightly-tag.sh.
#
# Release bodies alone are not a safe database. The nightly release is DELETED and
# recreated on every nightly build (the tag move cannot force-push a tag a release
# pins), so between the delete and the publish step the counter has no memory above
# the last vX.Y.Z release. A pipeline that dies in that window rewinds the counter
# permanently, and the next nightly ships an APK with a LOWER versionCode than the
# one already installed -- which Android refuses with a bare "App not installed".
# Observed 2026-07: 107030418 fell back to 107030415.
#
# The nightly release on GitHub is edited in place rather than deleted, but release
# bodies can still be lost (a release deleted by hand, or by
# scripts/prune-github-releases.sh). Git refs are append-only, anonymous-readable,
# and touched by neither, so they survive what release bodies do not.
VERSION_CODE_REF_PREFIX="refs/version-code"
# An unretried API blip aborts the release workflow, so back off on 5xx and
# dropped connections.
CURL_RETRY_OPTS="--retry 5 --retry-delay 2 --retry-all-errors"

usage() {
    cat >&2 <<EOF
Usage:
  scripts/version-code.sh next [--floor N] [--repo owner/repo]
  scripts/version-code.sh for-tag <tag> [--floor N] [--repo owner/repo]
  scripts/version-code.sh marker <versionCode>
  scripts/version-code.sh ref <versionCode>

versionCode policy:
  versionCode is a monotonic install/update counter, independent of versionName.
  Nightly and vX.Y.Z builds both use the same counter line.
  New release artifacts use max(release markers, $VERSION_CODE_REF_PREFIX/* refs, floor) + 1.
  Production deployment reuses the marker from the already published vX.Y.Z release.

The default floor is $DEFAULT_FLOOR, one less than the first corrected code after
the historical 107030327 nightly.
EOF
}

require_integer() {
    name="$1"
    value="$2"
    case "$value" in
        ''|*[!0-9]*)
            echo "$name must be a non-negative integer, got: ${value:-<empty>}" >&2
            exit 1
            ;;
    esac
}

print_version_code() {
    version_code="$1"
    require_integer "versionCode" "$version_code"
    if [ "$version_code" -gt 2100000000 ]; then
        echo "versionCode $version_code exceeds the Google Play maximum of 2100000000." >&2
        exit 1
    fi
    printf '%s\n' "$version_code"
}

print_marker() {
    version_code="$1"
    version_code="$(print_version_code "$version_code")"
    printf '<!-- %s: %s -->\n' "$MARKER_NAME" "$version_code"
}

print_ref() {
    version_code="$1"
    version_code="$(print_version_code "$version_code")"
    printf '%s/%s\n' "$VERSION_CODE_REF_PREFIX" "$version_code"
}

git_url() {
    repo="$1"
    printf '%s/%s.git\n' "${server_url%/}" "$repo"
}

api_base() {
    repo="$1"
    printf '%s/repos/%s\n' "${api_url%/}" "$repo"
}

# Anonymous GitHub API calls share a 60-an-hour limit per IP, which hosted
# runners exhaust. Send the workflow token when there is one.
api_get() {
    token="${GH_TOKEN:-${GITHUB_TOKEN:-}}"
    if [ -n "$token" ]; then
        curl -fsS $CURL_RETRY_OPTS \
            -H "Accept: application/vnd.github+json" \
            -H "Authorization: Bearer $token" \
            "$1"
    else
        curl -fsS $CURL_RETRY_OPTS -H "Accept: application/vnd.github+json" "$1"
    fi
}

extract_codes() {
    python3 -c '
import json
import re
import sys

marker = re.escape(sys.argv[1])
pattern = re.compile(marker + r":\s*([0-9]+)")
releases = json.load(sys.stdin)
for release in releases:
    body = release.get("body") or ""
    for match in pattern.finditer(body):
        print(match.group(1))
' "$MARKER_NAME"
}

extract_code_for_tag() {
    tag="$1"
    python3 -c '
import json
import re
import sys

marker = re.escape(sys.argv[1])
tag = sys.argv[2]
pattern = re.compile(marker + r":\s*([0-9]+)")
release = json.load(sys.stdin)
if release.get("tag_name") != tag:
    sys.exit(0)
body = release.get("body") or ""
matches = pattern.findall(body)
if matches:
    print(matches[-1])
' "$MARKER_NAME" "$tag"
}

ref_codes() {
    # `git ls-remote` prints "<sha>\t<ref>"; keep the numeric leaf of our namespace and
    # drop anything else, so a hand-made ref cannot poison the counter. Leading zeros
    # are rejected along with the rest: `test -gt` reads some of them as octal.
    url="$1"
    command -v git >/dev/null 2>&1 || return 0
    # Never let a private or moved repo turn into a credential prompt on a runner.
    GIT_TERMINAL_PROMPT=0 git ls-remote --refs "$url" "$VERSION_CODE_REF_PREFIX/*" 2>/dev/null |
        sed -n "s#^.*[[:space:]]$VERSION_CODE_REF_PREFIX/\([1-9][0-9]*\)\$#\1#p" || true
}

# Surveys the refs/version-code/* mirror and the GitHub release markers. Codes
# that exist solely on Google Play are still invisible here (the Flutter era's
# Play AABs carried base*10 while its markers and refs recorded the 9-digit
# base), and this step has no Play credentials to ask. The baseVersionCode floor
# in app/build.gradle.kts is the defense — it must clear every code any channel
# has ever served — and the fastlane lanes double-check the live Play track
# before uploading.
max_known_code() {
    floor="$1"
    repo="$2"

    max_code="$floor"

    # The ref mirror first: it is the one store that survives the nightly release
    # being deleted, so it is also the one that must be consulted even when the
    # API is unreachable.
    codes="$(ref_codes "$(git_url "$repo")" || true)"
    if [ -n "$codes" ]; then
        while IFS= read -r code; do
            [ -n "$code" ] || continue
            if [ "$code" -gt "$max_code" ]; then
                max_code="$code"
            fi
        done <<EOF_REF_CODES
$codes
EOF_REF_CODES
    fi

    if ! command -v curl >/dev/null 2>&1 || ! command -v python3 >/dev/null 2>&1; then
        printf '%s\n' "$max_code"
        return 0
    fi

    base="$(api_base "$repo")"
    page=1
    while :; do
        page_json="$(api_get "$base/releases?page=$page&per_page=100")"
        page_count="$(printf '%s' "$page_json" | python3 -c 'import json, sys; print(len(json.load(sys.stdin)))')"
        [ "$page_count" -gt 0 ] || break

        codes="$(printf '%s' "$page_json" | extract_codes || true)"
        if [ -n "$codes" ]; then
            while IFS= read -r code; do
                [ -n "$code" ] || continue
                if [ "$code" -gt "$max_code" ]; then
                    max_code="$code"
                fi
            done <<EOF_CODES
$codes
EOF_CODES
        fi

        page=$((page + 1))
    done

    printf '%s\n' "$max_code"
}

code_for_tag() {
    tag="$1"
    floor="$2"
    repo="$3"

    require_integer "floor" "$floor"

    if ! command -v curl >/dev/null 2>&1 || ! command -v python3 >/dev/null 2>&1; then
        print_version_code "$floor"
        return 0
    fi

    base="$(api_base "$repo")"
    release_json="$(api_get "$base/releases/tags/$tag" 2>/dev/null || true)"
    if [ -n "$release_json" ]; then
        code="$(printf '%s' "$release_json" | extract_code_for_tag "$tag" || true)"
        if [ -n "$code" ]; then
            print_version_code "$code"
            return 0
        fi
    fi

    print_version_code "$floor"
}

mode="${1:-}"
[ -n "$mode" ] || {
    usage
    exit 1
}
shift || true

floor="${OPENVITALS_VERSION_CODE_FLOOR:-$DEFAULT_FLOOR}"
server_url="${GITHUB_SERVER_URL:-https://github.com}"
api_url="${GITHUB_API_URL:-https://api.github.com}"
repo="${GITHUB_REPOSITORY:-OpenVitals-MTU/android-app}"
tag=""

case "$mode" in
    next)
        ;;
    for-tag)
        tag="${1:-}"
        [ -n "$tag" ] || {
            usage
            exit 1
        }
        shift
        ;;
    marker)
        print_marker "${1:-}"
        exit 0
        ;;
    ref)
        print_ref "${1:-}"
        exit 0
        ;;
    -h|--help)
        usage
        exit 0
        ;;
    *)
        usage
        exit 1
        ;;
esac

while [ "$#" -gt 0 ]; do
    case "$1" in
        --floor)
            floor="${2:-}"
            shift 2
            ;;
        --repo)
            repo="${2:-}"
            shift 2
            ;;
        *)
            usage
            exit 1
            ;;
    esac
done

require_integer "floor" "$floor"

case "$mode" in
    next)
        previous="$(max_known_code "$floor" "$repo")"
        print_version_code "$((previous + 1))"
        ;;
    for-tag)
        code_for_tag "$tag" "$floor" "$repo"
        ;;
esac
