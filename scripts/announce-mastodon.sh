#!/usr/bin/env sh
# Post the production release announcement from the OpenVitals Mastodon account.
#
# Usage: scripts/announce-mastodon.sh <tag> <notes-file>
#
# Environment:
#   MASTODON_ACCESS_TOKEN  (required) token for the announcing account, scope write:statuses
#                          and read:accounts (the duplicate check reads the account timeline)
#   MASTODON_INSTANCE_URL  defaults to https://techhub.social
#   MASTODON_STATUS_LIMIT  character cap for the post, defaults to 500 (the instance default)
#
# The post opens with the release's narrative paragraph (the first paragraph
# after "Released YYYY-MM-DD." in docs/releases/X.Y.Z.md, which is what the tag
# message carries) and links the Codeberg release and the Play listing.
#
# Re-running a production deployment must not toot twice, so the account's
# recent statuses are checked for this tag before posting, and the request also
# carries an Idempotency-Key so a retry within Mastodon's window is a no-op.
set -eu

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <tag> <notes-file>" >&2
    exit 1
fi

release_tag="$1"
notes_file="$2"
instance="${MASTODON_INSTANCE_URL:-https://techhub.social}"
instance="${instance%/}"
limit="${MASTODON_STATUS_LIMIT:-500}"

if [ -z "${MASTODON_ACCESS_TOKEN:-}" ] && [ -z "${MASTODON_DRY_RUN:-}" ]; then
    echo "MASTODON_ACCESS_TOKEN is required" >&2
    exit 1
fi
if [ -z "${CI_FORGE_URL:-}" ] || [ -z "${CI_REPO:-}" ]; then
    echo "CI_FORGE_URL and CI_REPO are required" >&2
    exit 1
fi
if ! printf '%s\n' "$release_tag" | grep -Eq '^[vV][0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "Refusing to announce non-release tag: $release_tag" >&2
    exit 1
fi
if [ ! -f "$notes_file" ]; then
    echo "notes file does not exist: $notes_file" >&2
    exit 1
fi
for tool in curl jq; do
    command -v "$tool" >/dev/null 2>&1 || { echo "$tool is required" >&2; exit 1; }
done

version="${release_tag#[vV]}"
headline="OpenVitals $version is out."
release_url="${CI_FORGE_URL%/}/${CI_REPO}/releases/tag/${release_tag}"
play_url="https://play.google.com/store/apps/details?id=tech.mmarca.openvitals"
links="Codeberg release (APK): $release_url
Google Play: $play_url

#OpenVitals #Android #FOSS #HealthConnect"

# Narrative paragraph: first non-empty line after the "Released ..." line, up to
# the next blank line or heading. Falls back to an empty paragraph when the notes
# do not follow the release template (for example a tag without an annotation).
narrative="$(awk '
    /^Released / { seen = 1; next }
    seen && /^[[:space:]]*$/ { if (started) exit; next }
    seen && /^#/ { exit }
    seen { started = 1; printf "%s%s", (n++ ? " " : ""), $0 }
' "$notes_file")"

# Mastodon counts every link as 23 characters whatever its length, so the fixed
# part is measured with the URLs replaced by 23-character stand-ins; counting
# the real Codeberg URL threw away ~40 characters of narrative per post.
url_stand_in="xxxxxxxxxxxxxxxxxxxxxxx"
links_counted="$(printf '%s' "$links" | sed "s#https\{0,1\}://[^[:space:]]*#$url_stand_in#g")"
fixed_length="$(printf '%s\n\n\n\n%s' "$headline" "$links_counted" | wc -m)"
room=$((limit - fixed_length))
if [ "$room" -lt 0 ]; then
    echo "Status limit $limit is too small for the fixed part of the announcement." >&2
    exit 1
fi
if [ "$(printf '%s' "$narrative" | wc -m)" -gt "$room" ]; then
    # Cut at the last sentence end that fits - but only if that keeps a
    # worthwhile share of the room. A short opener followed by one long
    # sentence used to leave "This release is about training with a plan."
    # and nothing else. Below that share, cut at the last clause boundary
    # (semicolon, colon, dash, comma) and, failing that, at a word, with an
    # ellipsis either way so the cut reads as one.
    cut="$(printf '%s' "$narrative" | cut -c1-"$room")"
    min_keep=$((room * 6 / 10))
    sentence="$(printf '%s' "$cut" | sed -n 's/^\(.*[.!?]\)[^.!?]*$/\1/p')"
    if [ -n "$sentence" ] && [ "$(printf '%s' "$sentence" | wc -m)" -ge "$min_keep" ]; then
        narrative="$sentence"
    else
        short="$(printf '%s' "$cut" | cut -c1-$((room - 3)))"
        # Last "; " ": " ", " and last " - " that fit; the longer of the two wins.
        clause_p="$(printf '%s' "$short" | sed -n 's/^\(.*[;:,]\)[[:space:]].*$/\1/p' | sed 's/[;:,]$//')"
        clause_d="$(printf '%s' "$short" | sed -n 's/^\(.*[^[:space:]]\)[[:space:]]-[[:space:]].*$/\1/p')"
        clause="$clause_p"
        if [ "$(printf '%s' "$clause_d" | wc -m)" -gt "$(printf '%s' "$clause_p" | wc -m)" ]; then
            clause="$clause_d"
        fi
        if [ -n "$clause" ] && [ "$(printf '%s' "$clause" | wc -m)" -ge "$min_keep" ]; then
            narrative="$clause..."
        else
            narrative="$(printf '%s' "$short" | sed 's/[[:space:]]*[^[:space:]]*$//')..."
        fi
    fi
fi

if [ -n "$narrative" ]; then
    status="$(printf '%s\n\n%s\n\n%s' "$headline" "$narrative" "$links")"
else
    status="$(printf '%s\n\n%s' "$headline" "$links")"
fi

# MASTODON_DRY_RUN=1 prints the status that would be posted and stops before
# touching the instance - no token needed.
if [ -n "${MASTODON_DRY_RUN:-}" ]; then
    printf '%s\n' "$status"
    printf -- '--- %s characters as Mastodon counts them ---\n' \
        "$(printf '%s' "$status" | sed "s#https\{0,1\}://[^[:space:]]*#$url_stand_in#g" | wc -m)" >&2
    exit 0
fi

auth="Authorization: Bearer $MASTODON_ACCESS_TOKEN"

account_id="$(curl -fsS -H "$auth" "$instance/api/v1/accounts/verify_credentials" | jq -r '.id')"
if [ -z "$account_id" ] || [ "$account_id" = "null" ]; then
    echo "Could not resolve the Mastodon account behind MASTODON_ACCESS_TOKEN." >&2
    exit 1
fi

# The duplicate check reads the account's PUBLIC statuses, so it is made
# without the token: authenticated it would need read:statuses, and a token
# without that scope got a 403 here. A failed read must stop the script - the
# whole point of the check is to never post twice, so an empty answer is an
# error, not "nothing found" (a 403 inside $(...) once fell through to a post).
already="$(curl -fsS \
    "$instance/api/v1/accounts/$account_id/statuses?limit=40&exclude_replies=true&exclude_reblogs=true" \
    | jq -r --arg needle "$headline" '[.[] | select(.content | test($needle; "i"))] | length')" || already=""
if [ -z "$already" ]; then
    echo "Could not read $instance/@$account_id's statuses to check for an existing announcement; not posting." >&2
    exit 1
fi
if [ "$already" -gt 0 ]; then
    echo "An announcement for $release_tag is already on $instance; skipping."
    exit 0
fi

payload="$(jq -n --arg status "$status" '{status: $status, visibility: "public", language: "en"}')"
response="$(printf '%s' "$payload" | curl -fsS -X POST \
    -H "$auth" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: openvitals-release-$release_tag" \
    --data-binary @- \
    "$instance/api/v1/statuses")"

printf '%s' "$response" | jq -r '"Posted " + .url'
