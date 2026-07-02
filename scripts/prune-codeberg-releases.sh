#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat >&2 <<'EOF'
Usage:
  KEEP_RELEASES=9 DRY_RUN=true scripts/prune-codeberg-releases.sh
  KEEP_RELEASES=9 DRY_RUN=false CODEBERG_RELEASE_API_KEY=... scripts/prune-codeberg-releases.sh

Environment:
  CODEBERG_RELEASE_API_KEY  Token used for deleting releases.
                            CODEBERG_TOKEN or FORGEJO_API_TOKEN also work.
  CODEBERG_REPO             owner/repo, defaults to OpenVitals/android-app.
  FORGE_URL                 Forge URL, defaults to https://codeberg.org.
  KEEP_RELEASES             Number of versioned releases to keep, defaults to 9.
  DRY_RUN                   true prints deletions, false deletes them.

The fixed nightly release is always preserved.
EOF
}

load_env_file() {
    env_file="$1"
    [ -f "$env_file" ] || return 0

    for env_key in \
        CODEBERG_RELEASE_API_KEY \
        CODEBERG_TOKEN \
        FORGEJO_API_TOKEN \
        CODEBERG_REPO \
        CI_REPO \
        FORGE_URL \
        CI_FORGE_URL \
        KEEP_RELEASES \
        DRY_RUN
    do
        [ -z "${!env_key:-}" ] || continue
        env_value="$(sed -nE "s/^[[:space:]]*(export[[:space:]]+)?${env_key}=//p" "$env_file" | tail -n 1)"
        [ -n "$env_value" ] || continue
        env_value="${env_value%$'\r'}"
        case "$env_value" in
            \"*\") env_value="${env_value#\"}"; env_value="${env_value%\"}" ;;
            \'*\') env_value="${env_value#\'}"; env_value="${env_value%\'}" ;;
        esac
        export "$env_key=$env_value"
    done
}

for env_file in .env .env.local .woodpecker/codeberg-release-secrets.env .woodpecker/local-release.env; do
    load_env_file "$env_file"
done

CODEBERG_RELEASE_API_KEY="${CODEBERG_RELEASE_API_KEY:-${CODEBERG_TOKEN:-${FORGEJO_API_TOKEN:-}}}"

FORGE_URL="${FORGE_URL:-${CI_FORGE_URL:-https://codeberg.org}}"
REPO="${CODEBERG_REPO:-${CI_REPO:-OpenVitals/android-app}}"
KEEP="${KEEP_RELEASES:-9}"
DRY_RUN="${DRY_RUN:-true}"

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
    usage
    exit 0
fi

if ! command -v curl >/dev/null 2>&1; then
    echo "curl is required." >&2
    exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "python3 is required." >&2
    exit 1
fi

case "$KEEP" in
    ''|*[!0-9]*)
        echo "KEEP_RELEASES must be a non-negative integer, got: $KEEP" >&2
        exit 1
        ;;
esac

case "$DRY_RUN" in
    true|false) ;;
    *)
        echo "DRY_RUN must be true or false, got: $DRY_RUN" >&2
        exit 1
        ;;
esac

if [ "$DRY_RUN" = "false" ] && [ -z "$CODEBERG_RELEASE_API_KEY" ]; then
    echo "CODEBERG_RELEASE_API_KEY is required when DRY_RUN=false." >&2
    echo "Create one in Codeberg, then run:" >&2
    echo "  export CODEBERG_RELEASE_API_KEY=..." >&2
    exit 1
fi

api_base="${FORGE_URL%/}/api/v1/repos/${REPO}"
tmp="$(mktemp)"
prune_list="$(mktemp)"
trap 'rm -f "$tmp" "$prune_list"' EXIT

curl_auth_args=()
if [ -n "$CODEBERG_RELEASE_API_KEY" ]; then
    curl_auth_args=(-H "Authorization: token ${CODEBERG_RELEASE_API_KEY}")
fi

page=1
while :; do
    page_json="$(
        curl -fsS \
            "${curl_auth_args[@]}" \
            "$api_base/releases?page=$page&limit=50"
    )"

    page_count="$(
        printf '%s' "$page_json" | python3 -c 'import json, sys; print(len(json.load(sys.stdin)))'
    )"
    [ "$page_count" -gt 0 ] || break
    printf '%s' "$page_json" | python3 -c 'import json, sys; [print(json.dumps(release, separators=(",", ":"))) for release in json.load(sys.stdin)]' >> "$tmp"
    page=$((page + 1))
done

if [ ! -s "$tmp" ]; then
    echo "No releases found for $REPO."
    exit 0
fi

python3 - "$KEEP" "$tmp" > "$prune_list" <<'PY'
import json
import re
import sys

keep = int(sys.argv[1])
path = sys.argv[2]
semver_tag = re.compile(r"^[vV][0-9]+\.[0-9]+\.[0-9]+$")

with open(path, "r", encoding="utf-8") as releases_file:
    releases = [
        json.loads(line)
        for line in releases_file
        if line.strip()
    ]

candidates = [
    release
    for release in releases
    if release.get("tag_name") != "nightly"
    and semver_tag.match(release.get("tag_name", ""))
]
candidates.sort(
    key=lambda release: release.get("published_at") or release.get("created_at") or "",
    reverse=True,
)

for release in candidates[keep:]:
    release_id = release["id"]
    tag = release.get("tag_name", "")
    date = release.get("published_at") or release.get("created_at") or ""
    print(f"{release_id}\t{tag}\t{date}")
PY

if [ ! -s "$prune_list" ]; then
    echo "No releases to prune; keeping newest $KEEP versioned releases and nightly."
    exit 0
fi

while IFS=$'\t' read -r id tag date; do
    if [ "$DRY_RUN" = "true" ]; then
        echo "Would delete release $tag id=$id $date"
    else
        echo "Deleting release $tag id=$id"
        curl -fsS -X DELETE \
            "${curl_auth_args[@]}" \
            "$api_base/releases/$id" >/dev/null
    fi
done < "$prune_list"
