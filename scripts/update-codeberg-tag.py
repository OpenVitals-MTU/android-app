#!/usr/bin/env python3
import json
import sys
import time
import urllib.error
import urllib.request


def usage() -> None:
    print(
        "Usage: update-codeberg-tag.py <api-base> <token> <tag-name> <target> <message>",
        file=sys.stderr,
    )


def request(method: str, url: str, token: str, payload=None) -> tuple[int, str]:
    data = None if payload is None else json.dumps(payload).encode("utf-8")

    attempts = 5
    for attempt in range(1, attempts + 1):
        req = urllib.request.Request(url, data=data, method=method)
        req.add_header("Authorization", "token " + token)
        if data is not None:
            req.add_header("Content-Type", "application/json")
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                return response.status, response.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as error:
            body = error.read().decode("utf-8", "replace")
            # Codeberg/Forgejo returns intermittent 5xx on tag and release
            # operations -- and has been seen to return one AFTER the write has
            # already committed. Retry like a network blip; create_tag() makes the
            # POST idempotent so a 500-after-success does not become a failure.
            if error.code >= 500 and attempt < attempts:
                print(
                    f"{method} {url} -> HTTP {error.code}; retry {attempt}/{attempts - 1}",
                    file=sys.stderr,
                )
                time.sleep(2 * attempt)
                continue
            return error.code, body
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            if attempt == attempts:
                raise
            reason = getattr(error, "reason", error)
            print(
                f"{method} {url} failed ({reason}); retry {attempt}/{attempts - 1}",
                file=sys.stderr,
            )
            time.sleep(2 * attempt)

    raise SystemExit(f"{method} {url} failed after {attempts} attempts.")


def require_ok(status: int, body: str, method: str, url: str, ok: tuple[int, ...]) -> None:
    if status not in ok:
        raise SystemExit(f"{method} {url} failed: HTTP {status}: {body}")


def tag_target_sha(api_base: str, token: str, tag_name: str):
    """Commit SHA the tag currently points at, or None if the tag is absent."""
    url = f"{api_base}/tags/{tag_name}"
    status, body = request("GET", url, token)
    if status == 404:
        return None
    require_ok(status, body, "GET", url, (200,))
    return json.loads(body).get("commit", {}).get("sha")


def delete_tag(api_base: str, token: str, tag_name: str) -> bool:
    url = f"{api_base}/tags/{tag_name}"
    status, body = request("DELETE", url, token)
    if status in (204, 404):
        return True
    if status == 409 and "release" in body.lower():
        return False
    require_ok(status, body, "DELETE", url, (204, 404))
    return True


def create_tag(api_base: str, token: str, tag_name: str, target: str, message: str) -> None:
    url = f"{api_base}/tags"
    status, body = request(
        "POST",
        url,
        token,
        {"tag_name": tag_name, "target": target, "message": message},
    )
    if status == 201:
        print(f"Updated {tag_name} tag to {target}")
        return
    # Two ways to get here with the tag actually in place: request() retried a 5xx
    # whose first attempt had already written the tag (now "already exists"), or
    # Codeberg returned a terminal 5xx on a POST that nonetheless committed. If the
    # tag now points where we asked, the move succeeded regardless of the status.
    if status in (409, 422) or status >= 500:
        if tag_target_sha(api_base, token, tag_name) == target:
            print(f"Updated {tag_name} tag to {target} (server returned HTTP {status})")
            return
    require_ok(status, body, "POST", f"{api_base}/tags", (201,))


def main() -> int:
    if len(sys.argv) != 6:
        usage()
        return 1

    api_base, token, tag_name, target, message = sys.argv[1:]
    api_base = api_base.rstrip("/")

    # A re-run on the same commit (or a recovery after a spurious 5xx that had
    # already moved the tag) finds it right where we want it -- leave it, and let
    # the publish step attach the release.
    if tag_target_sha(api_base, token, tag_name) == target:
        print(f"{tag_name} already at {target}; nothing to do")
        return 0

    if not delete_tag(api_base, token, tag_name):
        release_url = f"{api_base}/releases/tags/{tag_name}"
        status, body = request("DELETE", release_url, token)
        require_ok(status, body, "DELETE", release_url, (204, 404))

        for _ in range(5):
            if delete_tag(api_base, token, tag_name):
                break
            time.sleep(1)
        else:
            raise SystemExit(f"Tag {tag_name} is still attached to a release after deleting the release.")

    create_tag(api_base, token, tag_name, target, message)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
