#!/usr/bin/env sh
# Post the production release notes to the OpenVitals Zulip "releases" channel.
#
# Usage: scripts/announce-zulip.sh <tag> <notes-file>
#
# Environment:
#   ZULIP_BOT_EMAIL     (required) the announcing bot's email
#   ZULIP_BOT_API_KEY   (required) the bot's API key
#   ZULIP_SITE          defaults to https://openvitals.zulipchat.com
#   ZULIP_CHANNEL_ID    defaults to 608765 (the "releases" channel)
#
# The message goes under a topic named after the tag and carries the full
# release notes (the tag message, minus the versionCode marker comment) plus
# the Codeberg release and Play links. Zulip caps messages at 10000 characters;
# longer notes are cut with a pointer to the Codeberg release.
#
# Re-running a production deployment must not post twice, so the topic is
# checked for an existing message from the bot before posting.
set -eu

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <tag> <notes-file>" >&2
    exit 1
fi

release_tag="$1"
notes_file="$2"
site="${ZULIP_SITE:-https://openvitals.zulipchat.com}"
site="${site%/}"
channel_id="${ZULIP_CHANNEL_ID:-608765}"
limit=10000

if [ -z "${ZULIP_BOT_EMAIL:-}" ] || [ -z "${ZULIP_BOT_API_KEY:-}" ]; then
    echo "ZULIP_BOT_EMAIL and ZULIP_BOT_API_KEY are required" >&2
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
topic="$release_tag"
release_url="${CI_FORGE_URL%/}/${CI_REPO}/releases/tag/${release_tag}"
play_url="https://play.google.com/store/apps/details?id=tech.mmarca.openvitals"
links="**Downloads**
- Codeberg release (signed APK, AAB, checksums): $release_url
- Google Play: $play_url"

# Drop the versionCode marker comment and trailing blank lines.
# Drop the "# OpenVitals X.Y.Z" title too: the lead-in line already says it.
notes="$(grep -v '^<!--.*-->[[:space:]]*$' "$notes_file" | grep -v '^# OpenVitals ' | sed -e :a -e '/^\n*$/{$d;N;ba' -e '}' | sed '/./,$!d')"

fixed="$(printf '**OpenVitals %s is out.**\n\n\n\n%s' "$version" "$links")"
room=$((limit - $(printf '%s' "$fixed" | wc -m) - 80))
if [ "$(printf '%s' "$notes" | wc -m)" -gt "$room" ]; then
    notes="$(printf '%s' "$notes" | cut -c1-"$room")
[...]
Full notes on the Codeberg release."
fi

content="$(printf '**OpenVitals %s is out.**\n\n%s\n\n%s' "$version" "$notes" "$links")"

auth="$ZULIP_BOT_EMAIL:$ZULIP_BOT_API_KEY"

narrow="$(jq -nc --argjson ch "$channel_id" --arg topic "$topic" --arg sender "$ZULIP_BOT_EMAIL" \
    '[{operator:"channel",operand:$ch},{operator:"topic",operand:$topic},{operator:"sender",operand:$sender}]')"
already="$(curl -fsS -u "$auth" -G "$site/api/v1/messages" \
    --data-urlencode "anchor=newest" \
    --data-urlencode "num_before=1" \
    --data-urlencode "num_after=0" \
    --data-urlencode "narrow=$narrow" \
    | jq -r '.messages | length')"
if [ "${already:-0}" -gt 0 ]; then
    echo "An announcement for $release_tag is already in the Zulip topic; skipping."
    exit 0
fi

response="$(curl -fsS -u "$auth" -X POST "$site/api/v1/messages" \
    --data-urlencode "type=stream" \
    --data-urlencode "to=[$channel_id]" \
    --data-urlencode "topic=$topic" \
    --data-urlencode "content=$content")"

message_id="$(printf '%s' "$response" | jq -r '.id')"
echo "Posted message $message_id to $site/#narrow/channel/$channel_id/topic/$topic"
