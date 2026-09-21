#!/usr/bin/env sh
# CI has no device, so the instrumented tests gate nothing there. This keeps a local
# record instead: "write" after a passing run, "check" before a release is tagged.
#
#   scripts/device-tests-stamp.sh write
#   scripts/device-tests-stamp.sh check
set -eu

mode="${1:-}"
stamp_file="build/device-tests-passed"

# The stamp names the app sources as committed, so a change to docs or the changelog
# after the run does not undo it.
if [ -n "$(git status --porcelain -- app)" ]; then
    echo "app/ has uncommitted changes. Commit them, then run the instrumented tests." >&2
    exit 1
fi
app_tree="$(git rev-parse HEAD:app)"

case "$mode" in
    write)
        mkdir -p build
        printf '%s\n' "$app_tree" > "$stamp_file"
        echo "Recorded a passing instrumented test run for app tree $app_tree."
        ;;
    check)
        if [ ! -f "$stamp_file" ] || [ "$(cat "$stamp_file")" != "$app_tree" ]; then
            echo "The instrumented tests have not passed for these app sources." >&2
            echo "Connect a device and run: ANDROID_SERIAL=<serial> ./gradlew verifyAndroidTest" >&2
            exit 1
        fi
        ;;
    *)
        echo "Usage: $0 write|check" >&2
        exit 1
        ;;
esac
