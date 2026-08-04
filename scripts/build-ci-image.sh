#!/usr/bin/env sh
set -eu

# Builds and pushes the prebaked CI image (see ci-image/Dockerfile).
#
#   scripts/build-ci-image.sh            build only
#   scripts/build-ci-image.sh --push     build and push
#
# Pushing needs `docker login ghcr.io` with a token that can write packages.
# After pushing, flip the &android_image variable in .woodpecker/*.yml to the
# prebaked tag.

image="ghcr.io/mmarca-tech/openvitals-android-ci:android-37"

docker build -t "$image" ci-image/

if [ "${1:-}" = "--push" ]; then
    docker push "$image"
    echo "Pushed $image — now point .woodpecker/*.yml's &android_image at it."
else
    echo "Built $image (not pushed; rerun with --push)."
fi
