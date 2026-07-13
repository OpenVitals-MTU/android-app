#!/bin/sh
# Guards the vendored, Play-Services-free geolocator_android fork.
#
#   sh scripts/verify-geolocator-fork.sh               # every check, all fatal
#   sh scripts/verify-geolocator-fork.sh --offline     # skip the pub.dev lookup
#   sh scripts/verify-geolocator-fork.sh --warn-stale  # staleness warns, never fails
#
# Exit codes:
#   0  the fork is wired up and free of Google Play Services
#   1  a check failed (the findings go to stderr)
#
# Two things can go wrong with a vendored fork, and both are silent:
#
#   1. Upstream ships a new geolocator_android and we keep shipping the old one.
#      A fork that has quietly stopped tracking upstream is a fork nobody is
#      maintaining -- it just accumulates the bugs upstream already fixed.
#   2. Somebody re-adds the Play Services dependency, by merging upstream
#      wholesale or by pulling a plugin that drags it back in. F-Droid's scanner
#      would then DELETE geolocator's build.gradle and the build would die with
#      an unexplained NullPointerException configuring :geolocator_android.
#
# So this fails the build rather than letting either happen. See
# packages/geolocator_android/FORK.md for what the fork actually changes and how
# to move it to a new upstream release.
#
# The two failures are checked in different places, because they age differently.
#
# (2) is static, offline and deterministic, and it is the one that actually breaks
# the F-Droid build -- so it is ALWAYS fatal, everywhere, including under
# --warn-stale.
#
# (1) only becomes true when Baseflow publishes, which can happen on a day nobody
# touched this repo. Whether that should stop a build depends entirely on what is
# being built, so the caller picks:
#
#   --offline      push/PR (.woodpecker/test.yml). Not checked at all: upstream
#                  publishing must not turn main red for something no PR did.
#   --warn-stale   nightly. Warns loudly but still ships -- a nightly is a moving
#                  snapshot, and losing nightlies until someone ports the fork
#                  costs more than the staleness it would be nagging about.
#   (no flag)      tagged release. Fatal. This is the one artifact users install
#                  and keep, so it must not be built from a fork that has silently
#                  stopped tracking upstream.

set -eu

offline=0
warn_stale=0
for arg in "$@"; do
    case "$arg" in
        --offline) offline=1 ;;
        --warn-stale) warn_stale=1 ;;
        *)
            echo "Unknown argument: $arg" >&2
            echo "Usage: sh scripts/verify-geolocator-fork.sh [--offline] [--warn-stale]" >&2
            exit 1
            ;;
    esac
done

FORK_DIR="packages/geolocator_android"
PUBSPEC="$FORK_DIR/pubspec.yaml"
PKG="geolocator_android"

if [ ! -f "$PUBSPEC" ]; then
    echo "Cannot find $PUBSPEC -- run this from the repository root." >&2
    exit 1
fi

fail=0
stale=0      # upstream is ahead, but --warn-stale downgraded it to a warning
unchecked=0  # --warn-stale and pub.dev was unreachable, so staleness is unknown

# The fork's `version:` is the upstream release it was cut from.
base="$(sed -n 's/^version:[[:space:]]*\([0-9][0-9A-Za-z.+-]*\).*/\1/p' "$PUBSPEC" | head -n 1)"
if [ -z "$base" ]; then
    echo "Could not read the upstream version from $PUBSPEC." >&2
    exit 1
fi

# --- 1. Is upstream ahead of us? -------------------------------------------
if [ "$offline" -eq 1 ]; then
    echo "Skipping the pub.dev staleness check (--offline)."
else
    latest="$(curl -fsS --max-time 30 "https://pub.dev/api/packages/$PKG" \
        | sed -n 's/.*"latest":{[^}]*"version":"\([^"]*\)".*/\1/p')"

    if [ -z "$latest" ]; then
        if [ "$warn_stale" -eq 1 ]; then
            # A nightly must not die because pub.dev had a bad minute.
            echo "WARNING: could not reach pub.dev; skipped the $PKG staleness check." >&2
            unchecked=1
        else
            echo "Could not reach pub.dev to check the latest $PKG version." >&2
            echo "The staleness check needs network. Re-run when pub.dev is reachable," >&2
            echo "or pass --offline to run only the offline checks." >&2
            exit 1
        fi
    elif [ "$latest" != "$base" ]; then
        if [ "$warn_stale" -eq 1 ]; then
            echo "WARNING: the vendored $PKG fork is stale." >&2
            echo "  fork is based on: $base" >&2
            echo "  pub.dev now has:  $latest" >&2
            echo "This nightly still ships, but a TAGGED RELEASE will refuse to build until" >&2
            echo "the fork is ported to $latest. See $FORK_DIR/FORK.md." >&2
            stale=1
        else
            echo "The vendored $PKG fork is stale." >&2
            echo "  fork is based on: $base" >&2
            echo "  pub.dev now has:  $latest" >&2
            echo "" >&2
            echo "Port the fork to $latest before releasing. The fork exists only to keep" >&2
            echo "Google Play Services out of the build (F-Droid deletes the plugin's" >&2
            echo "build.gradle if it finds them), so it has to keep tracking upstream --" >&2
            echo "otherwise it silently accumulates the bugs upstream already fixed." >&2
            echo "" >&2
            echo "See $FORK_DIR/FORK.md for the three edits to re-apply." >&2
            fail=1
        fi
    fi
fi

# --- 2. Has Play Services crept back in? -----------------------------------
if grep -rq 'play-services\|com\.google\.android\.gms' "$FORK_DIR/android/build.gradle" 2>/dev/null; then
    echo "$FORK_DIR/android/build.gradle depends on Google Play Services again." >&2
    echo "F-Droid's scanner deletes that file when it sees this, and the build then" >&2
    echo "dies with a NullPointerException configuring :geolocator_android." >&2
    fail=1
fi

gms_imports="$(grep -rl '^import com\.google\.android\.gms' "$FORK_DIR/android" 2>/dev/null || true)"
if [ -n "$gms_imports" ]; then
    echo "These files import Google Play Services:" >&2
    echo "$gms_imports" | sed 's/^/  /' >&2
    fail=1
fi

# --- 3. Is the app actually using the fork? --------------------------------
# A vendored fork nothing depends on is theatre: pub would quietly resolve the
# real geolocator_android from pub.dev and put Play Services straight back.
if ! grep -q "path: $FORK_DIR" pubspec.yaml; then
    echo "pubspec.yaml does not override $PKG with $FORK_DIR." >&2
    echo "Without the override, pub resolves the real $PKG from pub.dev and Play" >&2
    echo "Services comes back -- the fork would be sitting there doing nothing." >&2
    fail=1
fi

if [ "$fail" -ne 0 ]; then
    exit 1
fi

if [ "$stale" -eq 1 ]; then
    echo "geolocator_android fork ($base) is free of Google Play Services, but is BEHIND upstream (see the warning above)."
elif [ "$offline" -eq 1 ] || [ "$unchecked" -eq 1 ]; then
    echo "geolocator_android fork ($base) is wired up and free of Google Play Services."
else
    echo "geolocator_android fork is current ($base, matching pub.dev) and free of Google Play Services."
fi
