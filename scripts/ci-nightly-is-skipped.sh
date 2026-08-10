#!/usr/bin/env sh
# Exit 0 when resolve-release-context recorded a skipped cron nightly; else 1.
set -eu

env_file=".woodpecker/tmp/release-context.env"
if [ ! -f "$env_file" ]; then
    exit 1
fi

# Avoid `set -a` so we only read the skip flag.
skip_nightly="$(
    sed -n 's/^OPENVITALS_SKIP_NIGHTLY=//p' "$env_file" | head -n 1
)"
if [ "$skip_nightly" = "true" ]; then
    exit 0
fi
exit 1
