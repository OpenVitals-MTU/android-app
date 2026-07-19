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
"""

import json
import subprocess
import sys
import time
import urllib.error
import urllib.request


def usage() -> None:
    print(
        "Usage: update-codeberg-tag.py <api-base> <token> <tag-name> <target> <message>",
        file=sys.stderr,
    )


def api_request(method: str, url: str, token: str):
    attempts = 5
    for attempt in range(1, attempts + 1):
        req = urllib.request.Request(url, method=method)
        req.add_header("Authorization", "token " + token)
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


def delete_release(api_base: str, token: str, tag_name: str) -> None:
    url = f"{api_base}/releases/tags/{tag_name}"
    status, body = api_request("DELETE", url, token)
    if status not in (204, 404):
        raise SystemExit(f"DELETE {url} failed: HTTP {status}: {body}")


def main() -> int:
    if len(sys.argv) != 6:
        usage()
        return 1

    api_base, token, tag_name, target, message = sys.argv[1:]
    api_base = api_base.rstrip("/")

    git_url = git_url_from_api(api_base)
    scheme, rest = git_url.split("://", 1)
    authed_url = f"{scheme}://{token}@{rest}"
    ref = f"refs/tags/{tag_name}"

    def scrub(text: str) -> str:
        return text.replace(token, "***")

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
        # The most likely cause is the existing release pinning the tag. Detach it
        # (the publish step recreates it) and retry the push once.
        print(f"git push of {ref} failed ({scrub(pushed.stderr.strip())}); detaching release and retrying", file=sys.stderr)
        delete_release(api_base, token, tag_name)
        pushed = git("push", "--force", authed_url, ref)
        if pushed.returncode != 0:
            raise SystemExit(f"git push of {ref} failed: {scrub(pushed.stderr.strip())}")

    print(f"Moved {tag_name} tag to {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
