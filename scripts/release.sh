#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Usage: scripts/release.sh <version>  (e.g. 0.7.0)}"
TAG="v$VERSION"
RELEASE_NOTES_FILE="docs/releases/$VERSION.md"

if ! echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "Error: version must be in X.Y.Z format" >&2
    exit 1
fi

MAJOR=$(echo "$VERSION" | cut -d. -f1)
MINOR=$(echo "$VERSION" | cut -d. -f2)
PATCH=$(echo "$VERSION" | cut -d. -f3)
VERSION_CODE=$((MAJOR * 10000 + MINOR * 1000 + PATCH))

sed -i "s/versionCode = [0-9]*/versionCode = $VERSION_CODE/" app/build.gradle.kts
sed -i "s/versionName = \"[^\"]*\"/versionName = \"$VERSION\"/" app/build.gradle.kts
sed -i "s/\\.orElse(\"[0-9][0-9]*\\.[0-9][0-9]*\\.[0-9][0-9]*-SNAPSHOT\")/.orElse(\"$VERSION-SNAPSHOT\")/" build.gradle.kts

git add app/build.gradle.kts build.gradle.kts CHANGELOG.md README.md docs fastlane .woodpecker/release.yml scripts/release.sh
git commit -m "chore: release $VERSION"
if [ -f "$RELEASE_NOTES_FILE" ]; then
    git tag -a "$TAG" -F "$RELEASE_NOTES_FILE"
else
    git tag -a "$TAG" -m "OpenVitals $VERSION"
fi
git push origin main "$TAG"

echo "Released v$VERSION (versionCode $VERSION_CODE)"
