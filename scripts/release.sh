#!/usr/bin/env bash
set -euo pipefail

# Cuts a release: bumps pubspec, commits whatever release notes you already wrote,
# annotated-tags, pushes. It builds and uploads NOTHING -- pushing the tag is what
# starts the Woodpecker release pipeline (.woodpecker/release.yml), and promotion to
# Play production is a separate manual Woodpecker deployment with target `production`.
#
# The version code is a monotonic counter whose source of truth is the Codeberg
# release notes, not this repo (see scripts/version-code.sh), so this needs network.

VERSION="${1:?Usage: scripts/release.sh <version>  (e.g. 1.10.0)}"
TAG="v$VERSION"
RELEASE_NOTES_FILE="docs/releases/$VERSION.md"

if ! echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "Error: version must be in X.Y.Z format" >&2
    exit 1
fi

# pubspec.yaml carries `version: <name>+<code>` -- the Flutter equivalent of the
# Kotlin app's baseVersionName / baseVersionCode.
CURRENT_VERSION_CODE="$(sed -n 's/^version:[[:space:]]*[0-9.]*+\([0-9][0-9]*\).*/\1/p' pubspec.yaml | head -n 1)"
if [ -z "$CURRENT_VERSION_CODE" ]; then
    echo "Could not read the version code from pubspec.yaml (expected 'version: X.Y.Z+CODE')" >&2
    exit 1
fi

VERSION_CODE="$(sh scripts/version-code.sh next --floor "$CURRENT_VERSION_CODE")"

sed -i "s/^version:[[:space:]]*.*/version: $VERSION+$VERSION_CODE/" pubspec.yaml

# Sweeps in whatever you hand-authored for this release: the changelog entry, the
# tag/release-notes body, and the per-locale Play "What's new" files, which must be
# named after the NEW version code (fastlane/metadata/android/<locale>/changelogs/<code>.txt).
git add pubspec.yaml CHANGELOG.md README.md docs fastlane .woodpecker scripts
git commit -m "chore: release $VERSION"

# The annotated tag's body becomes the Codeberg release notes (ci-release-context.sh
# reads it back with `git for-each-ref --format='%(contents)'`).
if [ -f "$RELEASE_NOTES_FILE" ]; then
    git tag -a "$TAG" -F "$RELEASE_NOTES_FILE"
else
    git tag -a "$TAG" -m "OpenVitals $VERSION"
fi
git push origin main "$TAG"

echo "Released v$VERSION (versionCode $VERSION_CODE)"
