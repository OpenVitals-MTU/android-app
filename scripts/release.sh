#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Usage: scripts/release.sh <version>  (e.g. 0.7.0)}"
TAG="v$VERSION"
RELEASE_NOTES_FILE="docs/releases/$VERSION.md"

if ! echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "Error: version must be in X.Y.Z format" >&2
    exit 1
fi

CURRENT_VERSION_CODE="$(sed -n 's/.*baseVersionCode = \([0-9][0-9]*\).*/\1/p' app/build.gradle.kts | head -n 1)"
if [ -z "$CURRENT_VERSION_CODE" ]; then
    echo "Could not read baseVersionCode from app/build.gradle.kts" >&2
    exit 1
fi

VERSION_CODE="$(sh scripts/version-code.sh next --floor "$CURRENT_VERSION_CODE")"

sed -i "s/baseVersionCode = [0-9]*/baseVersionCode = $VERSION_CODE/" app/build.gradle.kts
sed -i "s/baseVersionName = \"[^\"]*\"/baseVersionName = \"$VERSION\"/" app/build.gradle.kts
sed -i "s/\\.orElse(\"[0-9][0-9]*\\.[0-9][0-9]*\\.[0-9][0-9]*-SNAPSHOT\")/.orElse(\"$VERSION-SNAPSHOT\")/" build.gradle.kts

git add app/build.gradle.kts build.gradle.kts CHANGELOG.md README.md docs fastlane .github/workflows/release.yml scripts/release.sh scripts/version-code.sh
git commit -m "chore: release $VERSION"
if [ -f "$RELEASE_NOTES_FILE" ]; then
    # The tag message becomes the GitHub release notes and the Zulip post. Git's
    # default cleanup drops every line starting with "#", the ### section headings
    # with it (2.13.0 and 2.14.0 lost them). The "# OpenVitals X.Y.Z" title goes,
    # since the GitHub release already carries that name.
    sed '1{/^# /d}' "$RELEASE_NOTES_FILE" | git tag -a "$TAG" --cleanup=whitespace -F -
else
    git tag -a "$TAG" -m "OpenVitals $VERSION"
fi
# Both or neither. A tag pushed without main starts a release CI cannot trace to a branch.
git push --atomic origin main "$TAG"

echo "Released v$VERSION (versionCode $VERSION_CODE)"
