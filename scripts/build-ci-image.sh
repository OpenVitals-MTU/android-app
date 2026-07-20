#!/usr/bin/env sh
set -eu

# Build and push the prebaked CI image (see ci-image/Dockerfile).
#
# Run on any machine with docker and push access to your registry. Codeberg does
# NOT run a container registry, so use ghcr.io (aligns with the GitHub mirror),
# Docker Hub, or any OCI registry. Example with ghcr.io:
#
#   docker login ghcr.io               # GitHub username + a PAT with write:packages
#   REGISTRY_IMAGE=ghcr.io/mmarca-tech/mobile-app-ci sh scripts/build-ci-image.sh
#
# Afterwards:
#   1. Make the package PUBLIC (ghcr: package settings -> change visibility) so CI
#      can pull it without credentials.
#   2. Point the `&flutter_image` anchor in .woodpecker/test.yml and
#      .woodpecker/release.yml at the printed reference.
#
# Keep FLUTTER_VERSION in lockstep with that anchor's tag.

FLUTTER_VERSION="${FLUTTER_VERSION:-3.44.0}"

# Image reference WITHOUT the tag, e.g. codeberg.org/OpenVitals/mobile-app-ci
REGISTRY_IMAGE="${REGISTRY_IMAGE:?set REGISTRY_IMAGE, e.g. codeberg.org/<owner>/mobile-app-ci}"

# Tag encodes the wrapped Flutter version so it is obvious what is inside and so
# it stays in lockstep with the pipeline image tag.
TAG="${TAG:-flutter-${FLUTTER_VERSION}}"
ref="${REGISTRY_IMAGE}:${TAG}"

context="$(CDPATH= cd -- "$(dirname -- "$0")/../ci-image" && pwd)"

echo "Building ${ref} from ${context}/Dockerfile ..."
docker build \
  --build-arg "FLUTTER_VERSION=${FLUTTER_VERSION}" \
  --tag "$ref" \
  "$context"

echo "Pushing ${ref} ..."
docker push "$ref"

printf '\nPushed %s\n' "$ref"
printf 'Next: make the package public, then set the &flutter_image anchor in\n'
printf '      .woodpecker/test.yml and .woodpecker/release.yml to:\n\n    %s\n' "$ref"
