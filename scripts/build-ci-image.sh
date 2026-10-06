#!/usr/bin/env sh
set -eu

# Builds and pushes the prebaked CI image (see ci-image/Dockerfile).
#
#   scripts/build-ci-image.sh            build only
#   scripts/build-ci-image.sh --push     build and push
#
# Pushing needs `docker login ghcr.io` with a token that can write packages.
# After pushing, point every `container:` in .github/workflows/*.yml at the
# prebaked tag.

image="ghcr.io/mmarca-tech/openvitals-android-ci:android-37"

docker build -t "$image" ci-image/

if [ "${1:-}" = "--push" ]; then
    docker push "$image"
    echo "Pushed $image — now point the container: lines in .github/workflows/*.yml at it."
else
    echo "Built $image (not pushed; rerun with --push)."
fi
