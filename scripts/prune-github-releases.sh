#!/usr/bin/env bash
# Delete all but the newest versioned GitHub releases. The fixed nightly release
# and every git tag are preserved: `gh release delete` keeps the tag.
#
# Usage:
#   KEEP_RELEASES=9 DRY_RUN=true scripts/prune-github-releases.sh
#   KEEP_RELEASES=9 DRY_RUN=false scripts/prune-github-releases.sh
#
# Environment:
#   GH_TOKEN       token that can delete releases (gh auth login works locally)
#   GH_REPO        owner/repo, defaults to the checkout's origin
#   KEEP_RELEASES  number of versioned releases to keep, defaults to 9
#   DRY_RUN        true prints deletions, false deletes them; defaults to true
set -euo pipefail

KEEP="${KEEP_RELEASES:-9}"
DRY_RUN="${DRY_RUN:-true}"

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
    sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'
    exit 0
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

# Versioned vX.Y.Z / VX.Y.Z releases, newest first; nightly never matches.
prune_list="$(
    gh release list --limit 1000 --json tagName,publishedAt,createdAt --jq '
        map(select(.tagName | test("^[vV][0-9]+\\.[0-9]+\\.[0-9]+$")))
        | sort_by(.publishedAt // .createdAt) | reverse
        | .['"$KEEP"':][]
        | "\(.tagName)\t\(.publishedAt // .createdAt)"
    '
)"

if [ -z "$prune_list" ]; then
    echo "No releases to prune; keeping newest $KEEP versioned releases and nightly."
    exit 0
fi

while IFS=$'\t' read -r tag date; do
    if [ "$DRY_RUN" = "true" ]; then
        echo "Would delete release $tag $date"
    else
        echo "Deleting release $tag"
        gh release delete "$tag" --yes
    fi
done <<< "$prune_list"
