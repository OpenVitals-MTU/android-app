#!/usr/bin/env bash
# Mark a never-onboarded Wear OS watch as set up, so the vendor's phone app is
# never needed. Only reachable on watches whose welcome screen opens Settings
# (Wi-Fi adb lives there); the Galaxy Watch8 does not, see the guide.
#
#   scripts/wear-skip-onboarding.sh [--serial <ip:port>] [-y]
#
# Disables the two Wear OS setup wizard packages, writes the two "setup
# complete" settings, reboots, and waits for the watch to come back on Wi-Fi
# adb. Nothing is erased. Undo: adb shell pm enable <package> for each.
# Guide: docs/how-to/wear-os-sideload.md
set -euo pipefail
# shellcheck source=lib/wear-adb.sh
. "$(dirname "${BASH_SOURCE[0]}")/lib/wear-adb.sh"

SETUP_WIZARDS=(com.google.android.wearable.setupwizard com.google.android.setupwizard)
REBOOT_WAIT_SECONDS=180

while [ $# -gt 0 ]; do
    case "$1" in
        -s|--serial) SERIAL="$2"; shift 2 ;;
        -y|--yes) YES=yes; shift ;;
        -h|--help) sed -n '2,12p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) die "unknown argument $1" ;;
    esac
done

# After a reboot the watch comes back on a new Wi-Fi adb port: watch mDNS for
# the same IP and connect to whatever port it now announces.
reboot_and_reconnect() {
    case "$SERIAL" in
        *:*) ;;
        *) adbw reboot; adbw wait-for-device; return ;;
    esac
    local ip="${SERIAL%%:*}"
    adbw reboot || true
    note "waiting up to ${REBOOT_WAIT_SECONDS}s for $ip to announce adb again"
    local deadline=$((SECONDS + REBOOT_WAIT_SECONDS)) address=""
    while [ $SECONDS -lt $deadline ]; do
        address="$(discovered_address_for "$ip")"
        if [ -n "$address" ] && "$ADB" connect "$address" 2>/dev/null | grep -q connected; then
            SERIAL="$address"
            note "back at $SERIAL"
            return 0
        fi
        sleep 5
    done
    die "the watch did not come back on Wi-Fi adb. On the watch, turn Wireless debugging on again,
       then run scripts/wear-pair.sh and continue with scripts/wear-install.sh --serial <new ip:port>."
}

require_watch
say "Setup wizard"
if setup_done; then
    note "already marked as set up; nothing to do."
    exit 0
fi
note "device_provisioned=$(shell settings get global device_provisioned)" \
     "user_setup_complete=$(shell settings get secure user_setup_complete)"

present=()
for wizard in "${SETUP_WIZARDS[@]}"; do
    if shell pm list packages "$wizard" | grep -qx "package:$wizard"; then
        present+=("$wizard")
    fi
done
other="$(shell pm list packages | grep -i setupwizard | sed 's/^package://' \
    | grep -vxF -e "${SETUP_WIZARDS[0]}" -e "${SETUP_WIZARDS[1]}" || true)"
[ -n "$other" ] && note "other setup packages, left alone: $(echo "$other" | tr '\n' ' ')"
cat <<TEXT

  This marks the watch as set up without the vendor's phone app, by
  disabling: ${present[*]:-(no known wizard package found)}
  and writing the two "setup complete" settings. The watch then reboots.
  Undo: adb shell pm enable <package> for each, or a factory reset.
  Nothing on the watch is erased.

TEXT
confirm "Skip the setup wizard on this watch?" || die "aborted."

for wizard in ${present[@]+"${present[@]}"}; do
    if shell pm disable-user --user 0 "$wizard" >/dev/null; then
        note "disabled $wizard"
    else
        warn "could not disable $wizard"
    fi
done
shell settings put global device_provisioned 1
shell settings put secure user_setup_complete 1
shell am force-stop com.google.android.setupwizard >/dev/null 2>&1 || true
note "settings written; rebooting the watch"
reboot_and_reconnect
if setup_done; then
    note "the watch reports setup complete."
    note "next: scripts/wear-install.sh --serial $SERIAL"
else
    warn "the settings did not survive the reboot; the watch may re-run its wizard."
fi
