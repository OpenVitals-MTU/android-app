#!/usr/bin/env sh
set -eu

DEFAULT_FLOOR=107030327
MARKER_NAME="OpenVitals-Version-Code"

usage() {
    cat >&2 <<EOF
Usage:
  scripts/version-code.sh next [--floor N] [--forge-url URL] [--repo owner/repo]
  scripts/version-code.sh for-tag <tag> [--floor N] [--forge-url URL] [--repo owner/repo]
  scripts/version-code.sh marker <versionCode>

versionCode policy:
  versionCode is a monotonic install/update counter, independent of versionName.
  Nightly and vX.Y.Z builds both use the same counter line.
  New release artifacts use max(previous Codeberg release markers, floor) + 1.
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

api_base() {
    forge_url="$1"
    repo="$2"
    printf '%s/api/v1/repos/%s\n' "${forge_url%/}" "$repo"
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

max_known_code() {
    floor="$1"
    forge_url="$2"
    repo="$3"

    max_code="$floor"

    if ! command -v curl >/dev/null 2>&1 || ! command -v python3 >/dev/null 2>&1; then
        printf '%s\n' "$max_code"
        return 0
    fi

    base="$(api_base "$forge_url" "$repo")"
    page=1
    while :; do
        page_json="$(curl -fsS "$base/releases?page=$page&limit=50")"
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
    forge_url="$3"
    repo="$4"

    require_integer "floor" "$floor"

    if ! command -v curl >/dev/null 2>&1 || ! command -v python3 >/dev/null 2>&1; then
        print_version_code "$floor"
        return 0
    fi

    base="$(api_base "$forge_url" "$repo")"
    release_json="$(curl -fsS "$base/releases/tags/$tag" 2>/dev/null || true)"
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
forge_url="${FORGE_URL:-${CI_FORGE_URL:-https://codeberg.org}}"
repo="${CODEBERG_REPO:-${CI_REPO:-OpenVitals/android-app}}"
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
        --forge-url)
            forge_url="${2:-}"
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
        previous="$(max_known_code "$floor" "$forge_url" "$repo")"
        print_version_code "$((previous + 1))"
        ;;
    for-tag)
        code_for_tag "$tag" "$floor" "$forge_url" "$repo"
        ;;
esac
