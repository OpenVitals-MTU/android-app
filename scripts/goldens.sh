#!/usr/bin/env sh
set -eu

# The chart snapshot suite.
#
#   scripts/goldens.sh              verify every chart still draws what it drew
#   scripts/goldens.sh --update     re-photograph them (review the diff!)
#   scripts/goldens.sh --docker …   run inside the pinned CI image instead
#
# WHY THIS SCRIPT EXISTS AT ALL. A golden is a PNG, and a PNG comes out of one
# specific Flutter engine build. CI pins ghcr.io/cirruslabs/flutter:3.44.0; the
# local toolchain is whatever it is (3.44.6 today). Those two can disagree about
# antialiasing on a curve nobody touched, and a red pipeline that nobody can
# reproduce is worse than no pipeline. So:
#
#   - By default these run LOCALLY and are EXCLUDED from CI. They exist to prove
#     that a refactor of the chart library changed no pixels, which is a job they
#     do perfectly well on one machine.
#   - To make them gate CI, generate them in the same image CI verifies them with:
#     `scripts/goldens.sh --docker --update`, commit the PNGs, then drop
#     `--exclude-tags golden` from .woodpecker/test.yml.
#
# --docker needs the invoking user in the `docker` group:
#   sudo usermod -aG docker "$USER"   # then log out and back in

FLUTTER_IMAGE="ghcr.io/cirruslabs/flutter:3.44.0"

use_docker=0
update=""

for arg in "$@"; do
    case "$arg" in
        --docker) use_docker=1 ;;
        --update|-u) update="--update-goldens" ;;
        -h|--help)
            sed -n '3,10p' "$0" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            echo "unknown option: $arg" >&2
            exit 1
            ;;
    esac
done

if [ "$use_docker" -eq 1 ]; then
    if ! docker info >/dev/null 2>&1; then
        echo "docker is not usable by this user." >&2
        echo "  sudo usermod -aG docker \"\$USER\"   # then log out and back in" >&2
        exit 1
    fi
    # ci-flutter.sh installs the native-assets toolchain (cmake/ninja) the image
    # lacks; `flutter test` needs it because vector_map_tiles 10.x has a build hook.
    exec docker run --rm \
        -v "$PWD":/app \
        -w /app \
        "$FLUTTER_IMAGE" \
        sh -c "scripts/ci-flutter.sh test --tags golden $update"
fi

exec flutter test --tags golden $update
