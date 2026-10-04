#!/usr/bin/env python3
"""Rank Kotlin unit-test files by bloat signals.

Usage:
    python3 triage.py [ROOT ...] [--top N] [--json OUT.json] [--min-cluster K]

ROOT defaults to app/src/test. Prints a ranked table; --json writes the
per-file detail that the review step consumes.

Signals (all heuristics; a human or agent confirms every one):
  shape     3+ tests whose bodies are identical once string and number
            literals are blanked: the same scenario re-typed with different
            inputs. One-assert one-liners are excluded from shape and near:
            each pins its own rule.
            A cluster of k such tests is k-1 tests' worth of bloat.
  near      pairs of tests whose literal-blanked bodies are >= 90% similar but
            not identical: copy-paste with a tweak, often one subsumes another.
  noassert  tests with no assertion, verification or `fail` reachable in the
            body (helpers named assert*/expect*/verify*/check* count).
  constant  tests whose only assertion compares a literal with a SCREAMING_CASE
            constant: change-detectors unless the constant is a wire/storage
            contract.
  echo      tests that never assert, and only verify calls the test itself
            stubbed (no exactly = 0 guards). Informational, not scored: a
            stubbed write can still be the behavior. The other echo, a verify
            on a call the test's own fake makes, is invisible here.
  dup       a test whose literal-preserving body also appears in another file.
"""

from __future__ import annotations

import argparse
import difflib
import json
import os
import re
import subprocess
import sys
from collections import defaultdict

ASSERT_RE = re.compile(
    r"^(assert\w*|expect\w*|verify\w*|coVerify\w*|check\w*|fail|assertThat|"
    r"shouldBe|shouldEqual|assertFailsWith|assertThrows|confirmVerified)$"
)
CONST_RE = re.compile(r"^[A-Z][A-Z0-9_]{2,}$")
NEVER_TOUCH = {
    "LocalAppManifestPolicyTest", "TranslationCatalogTest", "StringFormatSpecifierTest",
    "SentenceCaseTest", "PrivacyPolicyVersionTest", "AppStartupActorsTest",
    "OpenVitalsDatabaseMigrationTest",
}


def tokenize(src: str) -> list[tuple[str, str]]:
    """Return (kind, text) tokens. kind: id, num, str, op. Comments dropped."""
    out: list[tuple[str, str]] = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c.isspace():
            i += 1
        elif src.startswith("//", i):
            j = src.find("\n", i)
            i = n if j < 0 else j
        elif src.startswith("/*", i):
            j = src.find("*/", i + 2)
            i = n if j < 0 else j + 2
        elif src.startswith('"""', i):
            j = src.find('"""', i + 3)
            j = n if j < 0 else j + 3
            while j < n and src[j] == '"':
                j += 1
            out.append(("str", src[i:j]))
            i = j
        elif c == '"' or c == "'":
            j = i + 1
            depth = 0
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if c == '"' and src.startswith("${", j):
                    depth += 1
                    j += 2
                    continue
                if depth and src[j] == "}":
                    depth -= 1
                elif not depth and src[j] == c:
                    break
                j += 1
            out.append(("str", src[i : j + 1]))
            i = j + 1
        elif c == "`":
            j = src.find("`", i + 1)
            j = n if j < 0 else j
            out.append(("id", src[i : j + 1]))
            i = j + 1
        elif c.isdigit():
            m = re.match(r"0[xX][0-9a-fA-F_]+[uUL]*|[0-9][0-9_]*(\.[0-9_]+)?([eE][+-]?\d+)?[fFLuU]*", src[i:])
            out.append(("num", m.group(0)))
            i += len(m.group(0))
        elif c.isalpha() or c == "_":
            m = re.match(r"[A-Za-z_][A-Za-z0-9_]*", src[i:])
            out.append(("id", m.group(0)))
            i += len(m.group(0))
        else:
            out.append(("op", c))
            i += 1
    return out


def find_tests(tokens: list[tuple[str, str]]) -> list[dict]:
    """Locate @Test functions and slice their body tokens."""
    tests = []
    i = 0
    while i < len(tokens):
        if tokens[i] == ("op", "@") and i + 1 < len(tokens) and tokens[i + 1] == ("id", "Test"):
            j = i + 2
            while j < len(tokens) and tokens[j] != ("id", "fun"):
                j += 1
            if j + 1 >= len(tokens):
                break
            name = tokens[j + 1][1].strip("`")
            k = j + 2
            depth = 0
            while k < len(tokens):  # skip the parameter list
                if tokens[k] == ("op", "("):
                    depth += 1
                elif tokens[k] == ("op", ")"):
                    depth -= 1
                    if depth == 0:
                        break
                k += 1
            while k < len(tokens) and tokens[k] != ("op", "{"):
                k += 1
            start = k
            depth = 0
            while k < len(tokens):
                if tokens[k] == ("op", "{"):
                    depth += 1
                elif tokens[k] == ("op", "}"):
                    depth -= 1
                    if depth == 0:
                        break
                k += 1
            tests.append({"name": name, "body": tokens[start + 1 : k]})
            i = k
        i += 1
    return tests


def is_one_liner(body) -> bool:
    """A single assertion on a single call: a distinct-rule test, never a clone."""
    asserts = sum(1 for k, t in body if k == "id" and ASSERT_RE.match(t))
    return asserts <= 1 and len(body) <= 30


def skeleton(body) -> tuple:
    return tuple("S" if k == "str" else "N" if k == "num" else t for k, t in body)


def literal_body(body) -> tuple:
    return tuple(t for _, t in body)


def has_assertion(body, local_asserting: set[str]) -> bool:
    for idx, (k, t) in enumerate(body):
        if k != "id":
            continue
        nxt = body[idx + 1][1] if idx + 1 < len(body) else ""
        if ASSERT_RE.match(t) or (t in local_asserting and nxt in "({"):
            return True
        if t == "assertThrows" or t == "assertFailsWith":
            return True
    return False


def is_constant_only(body) -> bool:
    calls = [i for i, (k, t) in enumerate(body) if k == "id" and ASSERT_RE.match(t)]
    if len(calls) != 1 or len(body) > 25:
        return False
    args = body[calls[0] + 1 :]
    kinds = {k for k, _ in args}
    ids = [t for k, t in args if k == "id"]
    return bool(ids) and all(CONST_RE.match(t) or t in {"assertEquals"} for t in ids) and (
        "num" in kinds or "str" in kinds
    )


def _block_calls(body, keywords) -> tuple[set[str], bool]:
    """Method names called inside every/verify blocks, and whether any is exactly = 0."""
    names: set[str] = set()
    zero = False
    i = 0
    while i < len(body):
        if body[i][0] == "id" and body[i][1] in keywords:
            j = i + 1
            while j < len(body) and body[j] != ("op", "{"):
                if body[j] == ("id", "exactly") and j + 2 < len(body) and body[j + 2][1] == "0":
                    zero = True
                j += 1
            depth = 0
            while j < len(body):
                if body[j] == ("op", "{"):
                    depth += 1
                elif body[j] == ("op", "}"):
                    depth -= 1
                    if depth == 0:
                        break
                elif body[j][0] == "id" and j + 1 < len(body) and body[j + 1] == ("op", "("):
                    names.add(body[j][1])
                j += 1
            i = j
        i += 1
    return names, zero


def is_echo(body) -> bool:
    """Only verifies, and every verified call is one the test itself stubbed."""
    ids = [t for k, t in body if k == "id"]
    if any(ASSERT_RE.match(t) and t not in ("verify", "coVerify") for t in ids):
        return False
    stubbed, _ = _block_calls(body, ("every", "coEvery"))
    verified, zero = _block_calls(body, ("verify", "coVerify"))
    return bool(verified) and not zero and verified <= stubbed


def cited_names(repo_root: str) -> set[str]:
    try:
        out = subprocess.run(
            ["grep", "-rhoE", "`[^`]{6,}`", os.path.join(repo_root, "docs")],
            capture_output=True, text=True, check=False,
        ).stdout
    except FileNotFoundError:
        return set()
    return {line.strip("`") for line in out.splitlines()}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("roots", nargs="*", default=["app/src/test"])
    ap.add_argument("--top", type=int, default=40)
    ap.add_argument("--json")
    ap.add_argument("--min-cluster", type=int, default=3)
    ap.add_argument("--near", type=float, default=0.9)
    args = ap.parse_args()

    repo_root = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True
    ).stdout.strip() or "."
    cited = cited_names(repo_root)

    files = []
    for root in args.roots:
        for dirpath, _, names in os.walk(root):
            for nm in names:
                if nm.endswith(".kt"):
                    files.append(os.path.join(dirpath, nm))

    by_literal: dict[tuple, list[tuple[str, str]]] = defaultdict(list)
    reports = []
    for path in sorted(files):
        if "/architecture/" in path or os.path.basename(path)[:-3] in NEVER_TOUCH:
            continue  # ratchets, policy tests and migration contracts: see SKILL.md
        src = open(path, encoding="utf-8").read()
        toks = tokenize(src)
        tests = find_tests(toks)
        if not tests:
            continue
        local_asserting = {
            toks[i + 1][1]
            for i in range(len(toks) - 1)
            if toks[i] == ("id", "fun") and re.match(r"(assert|expect|verify|check)", toks[i + 1][1])
        }
        shapes: dict[tuple, list[str]] = defaultdict(list)
        for t in tests:
            sk = skeleton(t["body"])
            t["sk"] = sk
            t["one"] = is_one_liner(t["body"])
            if len(sk) >= 8 and not t["one"]:
                shapes[sk].append(t["name"])
            lit = literal_body(t["body"])
            if len(lit) >= 15:
                by_literal[lit].append((path, t["name"]))

        clusters = [v for v in shapes.values() if len(v) >= args.min_cluster]
        clustered = {n for c in clusters for n in c}

        near = []
        cand = [t for t in tests if t["name"] not in clustered and len(t["sk"]) >= 8 and not t["one"]]
        for a in range(len(cand)):
            for b in range(a + 1, len(cand)):
                sa, sb = cand[a]["sk"], cand[b]["sk"]
                if min(len(sa), len(sb)) / max(len(sa), len(sb)) < args.near:
                    continue
                sm = difflib.SequenceMatcher(None, sa, sb, autojunk=False)
                if sm.real_quick_ratio() >= args.near and sm.quick_ratio() >= args.near and sm.ratio() >= args.near:
                    near.append([cand[a]["name"], cand[b]["name"], round(sm.ratio(), 3)])

        noassert = [t["name"] for t in tests if not has_assertion(t["body"], local_asserting)]
        constant = [t["name"] for t in tests if is_constant_only(t["body"])]
        echo = [t["name"] for t in tests if is_echo(t["body"])]

        removable = sum(len(c) - 1 for c in clusters)
        # near-duplicate pairs: count one removal per connected component edge set
        parent: dict[str, str] = {}

        def find(x):
            while parent.setdefault(x, x) != x:
                x = parent[x]
            return x

        for a, b, _ in near:
            parent[find(a)] = find(b)
        removable += len(parent) - len({find(x) for x in parent})
        removable += len((set(noassert) | set(constant)) - clustered)

        reports.append({
            "file": path,
            "tests": len(tests),
            "lines": src.count("\n") + 1,
            "score": removable,
            "shape_clusters": clusters,
            "near_pairs": near,
            "noassert": noassert,
            "constant": constant,
            "echo": echo,
            "cited_in_docs": sorted(t["name"] for t in tests if t["name"] in cited),
        })

    cross = defaultdict(list)
    for occ in by_literal.values():
        if len({p for p, _ in occ}) > 1:
            for p, n in occ:
                cross[p].append([n, [f"{q}::{m}" for q, m in occ if q != p]])
    for r in reports:
        r["cross_file_dups"] = cross.get(r["file"], [])
        r["score"] += len(r["cross_file_dups"]) // 2 if r["cross_file_dups"] else 0

    reports.sort(key=lambda r: (-r["score"], -r["tests"]))
    total_tests = sum(r["tests"] for r in reports)
    total_score = sum(r["score"] for r in reports)
    print(f"{len(reports)} files, {total_tests} tests, ~{total_score} flagged as candidate removals/merges\n")
    print(f"{'score':>5} {'tests':>5} {'shape':>5} {'near':>4} {'noas':>4} {'const':>5} {'echo':>4} {'cited':>5}  file")
    for r in reports[: args.top]:
        print(
            f"{r['score']:>5} {r['tests']:>5} {sum(len(c) for c in r['shape_clusters']):>5} "
            f"{len(r['near_pairs']):>4} {len(r['noassert']):>4} {len(r['constant']):>5} "
            f"{len(r['echo']):>4} {len(r['cited_in_docs']):>5}  {os.path.relpath(r['file'])}"
        )
    if args.json:
        with open(args.json, "w", encoding="utf-8") as fh:
            json.dump(reports, fh, indent=1)
    return 0


if __name__ == "__main__":
    sys.exit(main())
