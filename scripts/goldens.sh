#!/usr/bin/env sh
set -eu

# The visual golden suite.
#
#   scripts/goldens.sh              verify every golden still renders what it rendered
#   scripts/goldens.sh --update     re-photograph them, pull them off the device,
#                                   and copy them into the test assets (review the diff!)
#
# The Flutter era's goldens.sh compared engine-rendered PNGs and could run in a
# pinned Docker image. These goldens are INSTRUMENTATION renders: they need a
# connected device or emulator, and a baseline is only comparable to renders
# from the same device model — the committed set was recorded on a Pixel 6 Pro.
# That is also why CI does not run them (.woodpecker only compiles androidTest);
# they are the refactor's safety net, run by hand around visual work.
#
# --update flow: the record run writes every rendering to the device's
# Downloads, then this pulls them and overwrites the committed baselines under
# app/src/androidTest/assets/goldens/. ALWAYS review `git diff --stat` after —
# an unexpected golden moving is the suite catching something.

adb_bin="${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools/adb"
device_dir="/sdcard/Download/openvitals-goldens"
assets_dir="app/src/androidTest/assets/goldens"

command -v "$adb_bin" >/dev/null || adb_bin="adb"
"$adb_bin" get-state >/dev/null 2>&1 || { echo "No device connected." >&2; exit 1; }

if [ "${1:-}" = "--update" ]; then
    ./gradlew :app:connectedCiAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.openvitals.recordGoldens=true
    tmp_dir="$(mktemp -d)"
    "$adb_bin" pull "$device_dir" "$tmp_dir" >/dev/null
    copied=0
    for f in "$tmp_dir"/openvitals-goldens/*.png; do
        [ -f "$f" ] || continue
        cp "$f" "$assets_dir/$(basename "$f")"
        copied=$((copied + 1))
    done
    rm -rf "$tmp_dir"
    echo "Recorded $copied goldens into $assets_dir."
    echo "Review with: git diff --stat -- $assets_dir"
else
    ./gradlew :app:connectedCiAndroidTest
fi
