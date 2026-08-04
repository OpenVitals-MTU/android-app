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
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", "token " + token)
    if data is not None:
        req.add_header("Content-Type", "application/json")

    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return response.status, response.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", "replace")
        return error.code, body


def require_ok(status: int, body: str, method: str, url: str, ok: tuple[int, ...]) -> None:
    if status not in ok:
        raise SystemExit(f"{method} {url} failed: HTTP {status}: {body}")


def delete_tag(api_base: str, token: str, tag_name: str) -> bool:
    url = f"{api_base}/tags/{tag_name}"
    status, body = request("DELETE", url, token)
    if status in (204, 404):
        return True
    if status == 409 and "release" in body.lower():
        return False
    require_ok(status, body, "DELETE", url, (204, 404))
    return True


def main() -> int:
    if len(sys.argv) != 6:
        usage()
        return 1

    api_base, token, tag_name, target, message = sys.argv[1:]
    api_base = api_base.rstrip("/")

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

    status, body = request(
        "POST",
        f"{api_base}/tags",
        token,
        {
            "tag_name": tag_name,
            "target": target,
            "message": message,
        },
    )
    require_ok(status, body, "POST", f"{api_base}/tags", (201,))
    print(f"Updated {tag_name} tag to {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
