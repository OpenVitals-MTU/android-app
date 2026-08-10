#!/usr/bin/env sh
# Exit 0 when a cron nightly would republish nothing new; exit 1 when a build
# is warranted.
#
# Cron only. Manual nightlies always build so an operator can force a refresh.
# Skips when:
# - the mutable `nightly` tag already points at HEAD, or
# - HEAD has no commits after the latest versioned vX.Y.Z / VX.Y.Z release.
set -eu

if [ "${CI_PIPELINE_EVENT:-}" != "cron" ]; then
    exit 1
fi

head_sha="$(git rev-parse "${CI_COMMIT_SHA:?}")"

if git rev-parse -q --verify refs/tags/nightly >/dev/null 2>&1; then
    nightly_sha="$(git rev-parse "refs/tags/nightly^{commit}")"
    if [ "$nightly_sha" = "$head_sha" ]; then
        echo "Nightly tag already points at ${head_sha}; skipping cron nightly."
        exit 0
    fi
fi

latest_release_tag="$(
    git tag -l 'v[0-9]*' 'V[0-9]*' --sort=-version:refname | head -n 1 || true
)"
if [ -n "$latest_release_tag" ] &&
    git rev-parse -q --verify "refs/tags/${latest_release_tag}" >/dev/null 2>&1
then
    commits_since_release="$(git rev-list --count "${latest_release_tag}..${head_sha}")"
    if [ "$commits_since_release" = "0" ]; then
        echo "No commits since last release ${latest_release_tag}; skipping cron nightly."
        exit 0
    fi
fi

exit 1
