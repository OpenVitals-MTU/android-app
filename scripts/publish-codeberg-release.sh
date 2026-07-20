#!/usr/bin/env bash
set -euo pipefail

usage() {
    echo "Usage: $0 <tag> <title-file> <notes-file> <prerelease:true|false> <target-commit> <asset>..." >&2
}

if [ "$#" -lt 6 ]; then
    usage
    exit 1
fi

release_tag="$1"
title_file="$2"
notes_file="$3"
prerelease="$4"
target_commit="$5"
shift 5

case "$prerelease" in
    true|false) ;;
    *)
        echo "prerelease must be true or false" >&2
        exit 1
        ;;
esac

if [ -z "${CODEBERG_RELEASE_API_KEY:-}" ]; then
    echo "CODEBERG_RELEASE_API_KEY is required" >&2
    exit 1
fi

if [ -z "${CI_FORGE_URL:-}" ] || [ -z "${CI_REPO:-}" ]; then
    echo "CI_FORGE_URL and CI_REPO are required" >&2
    exit 1
fi

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

api_base="${CI_FORGE_URL%/}/api/v1/repos/${CI_REPO}"
# Codeberg's write API (release/tag creation) returns intermittent 5xx. curl's
# --retry already backs off on 500/502/503/504; --retry-all-errors extends that to
# the transfer as a whole so a `-f` 500 is retried rather than failing the release.
retry_opts="--retry 5 --retry-delay 2 --retry-all-errors"
tmp_dir=".woodpecker/tmp"
mkdir -p "$tmp_dir"
release_response="$tmp_dir/codeberg-release-${release_tag}.json"
release_payload="$tmp_dir/codeberg-release-${release_tag}-payload.json"
release_title="$(sed -n '1p' "$title_file")"

print_response_body() {
    response_file="$1"
    if [ -s "$response_file" ]; then
        echo "Response body:" >&2
        sed -n '1,200p' "$response_file" >&2 || true
    else
        echo "Response body was empty." >&2
    fi
}

http_status="$(
    curl -sS $retry_opts -w '%{http_code}' -o "$release_response" \
        -H "Authorization: token ${CODEBERG_RELEASE_API_KEY}" \
        "$api_base/releases/tags/$release_tag" || true
)"

case "$http_status" in
    200)
        release_id="$(jq -r '.id' "$release_response")"
        jq -n \
            --arg name "$release_title" \
            --rawfile body "$notes_file" \
            --argjson prerelease "$prerelease" \
            '{name: $name, body: $body, draft: false, prerelease: $prerelease}' \
            > "$release_payload"
        curl -fsS $retry_opts -X PATCH \
            -H "Authorization: token ${CODEBERG_RELEASE_API_KEY}" \
            -H "Content-Type: application/json" \
            --data-binary @"$release_payload" \
            "$api_base/releases/$release_id" >/dev/null
        ;;
    404)
        # No target_commitish: the tag is force-moved to the built commit BEFORE
        # this step runs (update-codeberg-tag.py, over git), so it already exists.
        # Passing target_commitish makes Forgejo try to create the tag on top of the
        # existing one, which it answers with a 500. Reference the existing tag only.
        jq -n \
            --arg tag_name "$release_tag" \
            --arg name "$release_title" \
            --rawfile body "$notes_file" \
            --argjson prerelease "$prerelease" \
            '{
                tag_name: $tag_name,
                name: $name,
                body: $body,
                draft: false,
                prerelease: $prerelease
            }' \
            > "$release_payload"
        curl -fsS $retry_opts -X POST \
            -H "Authorization: token ${CODEBERG_RELEASE_API_KEY}" \
            -H "Content-Type: application/json" \
            --data-binary @"$release_payload" \
            "$api_base/releases" > "$release_response"
        release_id="$(jq -r '.id' "$release_response")"
        ;;
    *)
        echo "Failed to inspect Codeberg release $release_tag: HTTP $http_status" >&2
        print_response_body "$release_response"
        exit 1
        ;;
esac

if [ -z "$release_id" ] || [ "$release_id" = "null" ]; then
    echo "Could not resolve Codeberg release id for $release_tag" >&2
    exit 1
fi

for asset_path in "$@"; do
    asset_name="$(basename "$asset_path")"
    asset_size="$(wc -c < "$asset_path" | tr -d ' ')"
    echo "Uploading release asset $asset_name ($asset_size bytes)."
    asset_ids="$(jq -r --arg name "$asset_name" '.assets[]? | select(.name == $name) | .id' "$release_response")"
    if [ -n "$asset_ids" ]; then
        while IFS= read -r asset_id; do
            [ -n "$asset_id" ] || continue
            curl -fsS $retry_opts -X DELETE \
                -H "Authorization: token ${CODEBERG_RELEASE_API_KEY}" \
                "$api_base/releases/$release_id/assets/$asset_id" >/dev/null
        done <<EOF
$asset_ids
EOF
    fi

    asset_response="$tmp_dir/codeberg-release-${release_tag}-asset-${asset_name}.response"
    asset_status="$(
        curl -sS $retry_opts -w '%{http_code}' -o "$asset_response" -X POST \
            -H "Authorization: token ${CODEBERG_RELEASE_API_KEY}" \
            -F "attachment=@${asset_path}" \
            "$api_base/releases/$release_id/assets?name=$asset_name" || true
    )"
    case "$asset_status" in
        2??) ;;
        *)
            echo "Failed to upload release asset $asset_name: HTTP $asset_status" >&2
            print_response_body "$asset_response"
            exit 1
            ;;
    esac
done

echo "Published $release_tag with $(($#)) asset(s)."
