#!/usr/bin/env bash
# Install the OpenVitals watch app on a Wear OS watch over Wi-Fi adb, grant
# its runtime permissions, launch it, and report what is running.
#
#   scripts/wear-install.sh [--serial <ip:port>] [options]
#
#   --apk <path>     APK to install (default: the debug build output, built when missing)
#   --release        use the release build output instead of the debug one
#   --package <id>   the installed package id, when the APK name does not say
#   --no-build       never run Gradle; fail when the APK is missing
#   --no-grant       do not pre-grant runtime permissions; the app asks on the wrist
#   --status         only report; install nothing
#
# Pair the computer first: scripts/wear-pair.sh. Guide: docs/how-to/wear-os-sideload.md
set -euo pipefail
# shellcheck source=lib/wear-adb.sh
. "$(dirname "${BASH_SOURCE[0]}")/lib/wear-adb.sh"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEBUG_APK="$REPO_ROOT/wear/build/outputs/apk/debug/wear-debug.apk"
RELEASE_APK="$REPO_ROOT/wear/build/outputs/apk/release/wear-release.apk"
RELEASE_PACKAGE="tech.mmarca.openvitals"
DEBUG_PACKAGE="tech.mmarca.openvitals.debug"
MAIN_ACTIVITY="tech.mmarca.openvitals.wear.MainActivity"
SERVICE_CLASS="tech.mmarca.openvitals.wear.WearAppService"

APK=""
VARIANT=debug
PACKAGE=""
BUILD=yes
GRANT=yes
STATUS_ONLY=no

while [ $# -gt 0 ]; do
    case "$1" in
        -s|--serial) SERIAL="$2"; shift 2 ;;
        --apk) APK="$2"; shift 2 ;;
        --release) VARIANT=release; shift ;;
        --package) PACKAGE="$2"; shift 2 ;;
        --no-build) BUILD=no; shift ;;
        --no-grant) GRANT=no; shift ;;
        --status) STATUS_ONLY=yes; shift ;;
        -h|--help) sed -n '2,14p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) die "unknown argument $1" ;;
    esac
done

resolve_apk() {
    if [ -z "$APK" ]; then
        [ "$VARIANT" = release ] && APK="$RELEASE_APK" || APK="$DEBUG_APK"
        if [ ! -f "$APK" ]; then
            [ "$BUILD" = yes ] || die "$APK is missing and --no-build was given."
            say "Building the watch app ($VARIANT)"
            (cd "$REPO_ROOT" && ./gradlew ":wear:assemble${VARIANT^}" --quiet)
        fi
    fi
    [ -f "$APK" ] || die "APK not found: $APK"
    if [ -z "$PACKAGE" ]; then
        case "$(basename "$APK")" in
            *debug*) PACKAGE="$DEBUG_PACKAGE" ;;
            *) PACKAGE="$RELEASE_PACKAGE" ;;
        esac
    fi
}

grant_permissions() {
    local permissions=(android.permission.BLUETOOTH_CONNECT android.permission.POST_NOTIFICATIONS)
    if [ "$SDK" -ge 36 ]; then
        permissions+=(android.permission.health.READ_HEART_RATE android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND)
    elif [ "$SDK" -ge 33 ]; then
        permissions+=(android.permission.BODY_SENSORS android.permission.BODY_SENSORS_BACKGROUND)
    else
        permissions+=(android.permission.BODY_SENSORS)
    fi
    local permission
    for permission in "${permissions[@]}"; do
        if shell pm grant "$PACKAGE" "$permission" 2>/dev/null; then
            note "granted ${permission##*.}"
        else
            warn "could not grant ${permission##*.}; the app will ask on the wrist"
        fi
    done
}

status() {
    say "Status of $PACKAGE"
    if setup_done; then
        note "setup wizard: complete"
    else
        note "setup wizard: NOT complete (scripts/wear-skip-onboarding.sh, or onboard with the vendor app)"
    fi
    local dump
    dump="$(shell dumpsys package "$PACKAGE" 2>/dev/null || true)"
    if ! echo "$dump" | grep -q 'versionName='; then
        note "app: not installed"
        return
    fi
    note "app: $(echo "$dump" | sed -n 's/^ *versionName=//p' | head -1) (code $(echo "$dump" | sed -n 's/^ *versionCode=\([0-9]*\).*/\1/p' | head -1))"
    local permission
    for permission in BLUETOOTH_CONNECT POST_NOTIFICATIONS READ_HEART_RATE READ_HEALTH_DATA_IN_BACKGROUND BODY_SENSORS BODY_SENSORS_BACKGROUND; do
        if echo "$dump" | grep -Eq "\.$permission: granted=true"; then
            note "granted: $permission"
        elif echo "$dump" | grep -Eq "\.$permission: granted=false"; then
            note "missing: $permission"
        fi
    done
    if shell dumpsys activity services "$PACKAGE" | grep -q "$SERVICE_CLASS"; then
        note "phone link service: running"
    else
        note "phone link service: not running (open the app on the watch and grant Nearby devices)"
    fi
}

require_watch

if [ "$STATUS_ONLY" = yes ]; then
    [ -n "$PACKAGE" ] || PACKAGE="$([ "$VARIANT" = release ] && echo "$RELEASE_PACKAGE" || echo "$DEBUG_PACKAGE")"
    status
    exit 0
fi

resolve_apk
say "Installing $(basename "$APK") as $PACKAGE"
adbw install -r "$APK" | sed 's/^/  /'
shell pm list packages "$PACKAGE" | grep -qx "package:$PACKAGE" \
    || die "$PACKAGE is not installed; is --package right for this APK?"
if [ "$GRANT" = yes ]; then
    say "Permissions"
    grant_permissions
fi
say "Launching"
shell am start -W -n "$PACKAGE/$MAIN_ACTIVITY" >/dev/null && note "the watch app is on screen"
sleep 3
status
cat <<TEXT

Next, on the phone:
  1. Bluetooth settings: pair the watch (it may be listed twice; either entry works).
  2. OpenVitals, Settings, Watches, add watch: it is offered as a Wear OS watch.
  3. On the watch's screen in OpenVitals, "Validate Wear OS App" should answer.
TEXT
