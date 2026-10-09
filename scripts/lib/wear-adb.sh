#!/usr/bin/env bash
# Shared by the wear-*.sh scripts: finding adb, reaching the watch over Wi-Fi
# adb, and reading its setup state. Source it; do not run it.

# adb's own mDNS finds nothing next to a system Avahi daemon; this backend does.
export ADB_MDNS_OPENSCREEN=1

SERIAL="${ANDROID_SERIAL:-}"
YES=no

say()  { printf '\033[1m%s\033[0m\n' "$*"; }
note() { printf '  %s\n' "$*"; }
warn() { printf '\033[33mwarning:\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[31merror:\033[0m %s\n' "$*" >&2; exit 1; }

confirm() {
    [ "$YES" = yes ] && return 0
    local answer
    read -r -p "$1 [y/N] " answer
    [[ "$answer" =~ ^[Yy]$ ]]
}

find_adb() {
    if command -v adb >/dev/null 2>&1; then
        echo adb
        return
    fi
    local candidate
    for candidate in \
        "${ANDROID_HOME:-}/platform-tools/adb" \
        "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
        "$HOME/Android/Sdk/platform-tools/adb" \
        "$HOME/Library/Android/sdk/platform-tools/adb"; do
        if [ -x "$candidate" ]; then
            echo "$candidate"
            return
        fi
    done
    die "adb not found. Install Android platform-tools, or set ANDROID_HOME to the SDK."
}

ADB="$(find_adb)"

adbw() {
    if [ -n "$SERIAL" ]; then
        "$ADB" -s "$SERIAL" "$@"
    else
        "$ADB" "$@"
    fi
}

shell() { adbw shell "$@" | tr -d '\r'; }

prop() { shell getprop "$1"; }

# Lists every device announcing adb over Wi-Fi, phones included.
discover() {
    say "Devices advertising adb over Wi-Fi (watches and phones alike)"
    note "Each line is an address to pass as --serial. The port changes on every boot."
    local found=no
    if command -v avahi-browse >/dev/null 2>&1; then
        avahi-browse -rtp _adb-tls-connect._tcp 2>/dev/null \
            | awk -F';' '$1 == "=" && $3 == "IPv4" { print "  " $8 ":" $9 "  (" $4 ")"; found = 1 }' \
            && found=yes
    fi
    if [ "$found" = no ]; then
        "$ADB" mdns services 2>/dev/null | awk '/_adb-tls-connect/ { print "  " $3 "  (" $1 ")"; found = 1 }' \
            && found=yes
    fi
    [ "$found" = yes ] || note "none. Is Wireless debugging on, and the watch awake and on this Wi-Fi network?"
}

# The current adb address of the device at an IP, or nothing.
discovered_address_for() {
    local ip="$1"
    if command -v avahi-browse >/dev/null 2>&1; then
        avahi-browse -rtp _adb-tls-connect._tcp 2>/dev/null \
            | awk -F';' -v ip="$ip" '$1 == "=" && $8 == ip { print $8 ":" $9; exit }'
    else
        "$ADB" mdns services 2>/dev/null | awk -v ip="$ip" '/_adb-tls-connect/ && index($3, ip ":") == 1 { print $3; exit }'
    fi
}

# Resolves SERIAL (flag, ANDROID_SERIAL, or the only device), connects, and
# refuses anything that is not a watch. Sets SDK.
require_watch() {
    if [ -z "$SERIAL" ]; then
        local devices
        devices="$("$ADB" devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
        case "$(printf '%s\n' "$devices" | grep -c .)" in
            0) die "no adb device. Run scripts/wear-pair.sh to find the watch's address." ;;
            1) SERIAL="$devices" ;;
            *) die "several adb devices; pick the watch with --serial <ip:port>." ;;
        esac
    fi
    local connect=""
    case "$SERIAL" in
        *:*) connect="$("$ADB" connect "$SERIAL" 2>&1 || true)" ;;
    esac
    local state
    state="$("$ADB" -s "$SERIAL" get-state 2>/dev/null || true)"
    [ "$state" = device ] || die "$SERIAL is not reachable (${connect:-state: ${state:-none}}).
       A watch drops Wi-Fi while asleep: raise the wrist, then run scripts/wear-pair.sh for the current port."
    local characteristics
    characteristics="$(prop ro.build.characteristics)"
    case ",$characteristics," in
        *,watch,*) ;;
        *) die "$SERIAL is not a watch (ro.build.characteristics=$characteristics)." ;;
    esac
    SDK="$(prop ro.build.version.sdk)"
    say "Watch $SERIAL"
    note "$(prop ro.product.manufacturer) $(prop ro.product.model), Android $(prop ro.build.version.release) (API $SDK)"
}

# Whether the watch reports its setup as complete.
setup_done() {
    [ "$(shell settings get global device_provisioned)" = 1 ] &&
        [ "$(shell settings get secure user_setup_complete)" = 1 ]
}
