#!/usr/bin/env bash
# Pair this computer with a Wear OS watch over Wi-Fi adb, then list the
# addresses on the network so the watch's connection address is on screen.
#
#   scripts/wear-pair.sh <ip:pairing-port> <code>   pair (once per computer), then list
#   scripts/wear-pair.sh                            list only
#
# On the watch: Settings, Developer options, Wireless debugging, Pair new
# device shows the pairing address and the six-digit code. Leave that screen
# open; the code expires with it. Guide: docs/how-to/wear-os-sideload.md
set -euo pipefail
# shellcheck source=lib/wear-adb.sh
. "$(dirname "${BASH_SOURCE[0]}")/lib/wear-adb.sh"

case $# in
    0) ;;
    2)
        say "Pairing with $1"
        "$ADB" pair "$1" "$2"
        note "Paired. The connection port is a different one; pick the watch's line below."
        echo
        ;;
    *) die "usage: scripts/wear-pair.sh [<ip:pairing-port> <code>]" ;;
esac

discover
echo
note "then: scripts/wear-install.sh --serial <ip:port>"
