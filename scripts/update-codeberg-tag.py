#!/usr/bin/env python3
"""Force-move the mutable ``nightly`` tag to the built commit.

The tag is moved over GIT rather than the Forgejo REST tag API. Codeberg's
``POST /repos/.../tags`` returns HTTP 500 -- intermittently after the write has
already committed, and (observed 2026-07) persistently -- which makes it
unusable as a release gate. A git ref update goes straight to the repository and
is reliable.

If the push is refused because a release still pins the tag, the release is
detached through the API (that endpoint is healthy) and the push retried; the
publish step recreates the release on the moved tag.

That delete is why this script also owns the version-code counter. The marker in
the nightly release body is the counter's database, and deleting the release
destroys it: until the publish step runs there is no record of any code issued
since the last vX.Y.Z release. A pipeline that dies in that window rewinds the
counter for good, and the next nightly builds an APK with a LOWER versionCode
than the one already on the phone -- which Android refuses with nothing more
helpful than "App not installed". Observed 2026-07, 107030418 -> 107030415.

So the code is mirrored into an append-only `refs/version-code/<code>` ref BEFORE
anything destructive happens, the body is carried across the delete rather than
thrown away, and a code that has gone backwards fails the build instead of
shipping. See scripts/version-code.sh, which folds those refs into its max.
"""

# `X | None` annotations below would be evaluated at import time and raise on any
# runner whose python3 predates 3.10. Deferring them costs nothing and keeps this
# script's reach as wide as its `#!/usr/bin/env python3`.
from __future__ import annotations

import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request

# Kept in step with VERSION_CODE_REF_PREFIX and MARKER_NAME in scripts/version-code.sh.
VERSION_CODE_REF_PREFIX = "refs/version-code"
MARKER_PATTERN = re.compile(r"OpenVitals-Version-Code:\s*([0-9]+)")


def usage() -> None:
    print(
        "Usage: update-codeberg-tag.py <api-base> <token> <tag-name> <target> "
        "<message> <version-code>",
        file=sys.stderr,
    )


def api_request(method: str, url: str, token: str, payload: dict | None = None):
    attempts = 5
    body_bytes = None if payload is None else json.dumps(payload).encode("utf-8")
    for attempt in range(1, attempts + 1):
        req = urllib.request.Request(url, data=body_bytes, method=method)
        req.add_header("Authorization", "token " + token)
        if body_bytes is not None:
            req.add_header("Content-Type", "application/json")
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                return response.status, response.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as error:
            body = error.read().decode("utf-8", "replace")
            if error.code >= 500 and attempt < attempts:
                print(f"{method} {url} -> HTTP {error.code}; retry {attempt}/{attempts - 1}", file=sys.stderr)
                time.sleep(2 * attempt)
                continue
            return error.code, body
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            if attempt == attempts:
                raise
            reason = getattr(error, "reason", error)
            print(f"{method} {url} failed ({reason}); retry {attempt}/{attempts - 1}", file=sys.stderr)
            time.sleep(2 * attempt)
    raise SystemExit(f"{method} {url} failed after {attempts} attempts.")


def git(*args: str) -> subprocess.CompletedProcess:
    return subprocess.run(["git", *args], capture_output=True, text=True)


def git_url_from_api(api_base: str) -> str:
    """https://host/api/v1/repos/owner/repo -> https://host/owner/repo.git"""
    marker = "/api/v1/repos/"
    if marker not in api_base:
        raise SystemExit(f"Cannot derive a git URL from api-base {api_base!r}")
    host, repo = api_base.split(marker, 1)
    return f"{host}/{repo.rstrip('/')}.git"


def fetch_release(api_base: str, token: str, tag_name: str) -> dict | None:
    """The release currently on the tag, or None when there is not one."""
    url = f"{api_base}/releases/tags/{tag_name}"
    status, body = api_request("GET", url, token)
    if status == 404:
        return None
    if status != 200:
        raise SystemExit(f"GET {url} failed: HTTP {status}: {body}")
    try:
        return json.loads(body)
    except json.JSONDecodeError as error:
        raise SystemExit(f"GET {url} returned unreadable JSON: {error}") from error


def marker_code(body: str | None) -> int | None:
    """The highest version-code marker in a release body."""
    codes = [int(match) for match in MARKER_PATTERN.findall(body or "")]
    return max(codes) if codes else None


def delete_release(api_base: str, token: str, tag_name: str) -> None:
    url = f"{api_base}/releases/tags/{tag_name}"
    status, body = api_request("DELETE", url, token)
    if status not in (204, 404):
        raise SystemExit(f"DELETE {url} failed: HTTP {status}: {body}")


def restore_release(api_base: str, token: str, tag_name: str, release: dict) -> None:
    """Put the deleted release back, marker and all, before doing anything else.

    The publish step will PATCH this with the new notes a few minutes later. The
    point of restoring it now is the minutes in between: without this the counter's
    only record of every code issued since the last vX.Y.Z release is gone for as
    long as the rest of the pipeline takes, and gone for good if that pipeline
    fails. The ref mirror covers the same hole; this keeps the release body -- the
    human-readable half -- honest too.
    """
    payload = {
        "tag_name": tag_name,
        "name": release.get("name") or tag_name,
        "body": release.get("body") or "",
        "draft": False,
        "prerelease": bool(release.get("prerelease", True)),
    }
    status, body = api_request("POST", f"{api_base}/releases", token, payload)
    if status not in (200, 201):
        # Not fatal: the publish step recreates the release either way, and the ref
        # mirror already holds the counter. Say so loudly rather than losing a build.
        print(
            f"Could not restore the {tag_name} release body: HTTP {status}: {body}",
            file=sys.stderr,
        )


def record_version_code_ref(
    authed_url: str, version_code: int, target: str, scrub
) -> None:
    """Mirror the code into an append-only ref before the release can be deleted.

    Forced because a re-run may legitimately reissue a code against a different
    commit; what the counter reads is the ref NAME, so where it points is incidental.
    """
    ref = f"{VERSION_CODE_REF_PREFIX}/{version_code}"
    updated = git("update-ref", ref, target)
    if updated.returncode != 0:
        raise SystemExit(f"git update-ref {ref} failed: {updated.stderr.strip()}")
    pushed = git("push", "--force", authed_url, ref)
    if pushed.returncode != 0:
        raise SystemExit(f"git push of {ref} failed: {scrub(pushed.stderr.strip())}")
    print(f"Recorded versionCode {version_code} at {ref}")


def main() -> int:
    if len(sys.argv) != 7:
        usage()
        return 1

    api_base, token, tag_name, target, message, version_code_arg = sys.argv[1:]
    api_base = api_base.rstrip("/")

    if not version_code_arg.isdigit():
        raise SystemExit(f"version-code must be a positive integer, got: {version_code_arg!r}")
    version_code = int(version_code_arg)

    git_url = git_url_from_api(api_base)
    scheme, rest = git_url.split("://", 1)
    authed_url = f"{scheme}://{token}@{rest}"
    ref = f"refs/tags/{tag_name}"

    def scrub(text: str) -> str:
        return text.replace(token, "***")

    # Burn the code first, so it is on record whatever happens to the release or to
    # the rest of this pipeline. Reusing a code is harmless; rewinding is not.
    record_version_code_ref(authed_url, version_code, target, scrub)

    # The release about to be deleted still carries the previous marker, which makes
    # this the one place that can see a rewind at all: by the time a rewound build
    # reaches the publish step the evidence has already been destroyed. Refuse to
    # ship an APK Android cannot install over the one it replaces.
    existing_release = fetch_release(api_base, token, tag_name)
    previous_code = marker_code(existing_release.get("body") if existing_release else None)
    if previous_code is not None and version_code <= previous_code:
        raise SystemExit(
            f"versionCode {version_code} is not above the {previous_code} already "
            f"published on {tag_name}. The counter has rewound -- see the ref mirror "
            f"note in scripts/version-code.sh -- and this build would install nowhere."
        )

    # Annotated local tag at the built commit; identity is inline so no global
    # git config is touched. `-f` moves it if a stale local tag exists.
    tagged = git(
        "-c", "user.name=OpenVitals CI",
        "-c", "user.email=ci@openvitals.invalid",
        "tag", "-f", "-a", tag_name, "-m", message, target,
    )
    if tagged.returncode != 0:
        raise SystemExit(f"git tag failed: {scrub(tagged.stderr.strip())}")

    # Force-push the ref. Pushing to the same commit is a no-op ("up-to-date"),
    # so this is safe to re-run.
    pushed = git("push", "--force", authed_url, ref)
    if pushed.returncode != 0:
        # The most likely cause is the existing release pinning the tag. Detach it,
        # retry the push once, then put the body straight back: the publish step
        # would recreate it eventually, but "eventually" is the whole bug -- the
        # marker must not be missing for the length of a build.
        print(f"git push of {ref} failed ({scrub(pushed.stderr.strip())}); detaching release and retrying", file=sys.stderr)
        delete_release(api_base, token, tag_name)
        pushed = git("push", "--force", authed_url, ref)
        if existing_release is not None:
            restore_release(api_base, token, tag_name, existing_release)
        if pushed.returncode != 0:
            raise SystemExit(f"git push of {ref} failed: {scrub(pushed.stderr.strip())}")

    print(f"Moved {tag_name} tag to {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
