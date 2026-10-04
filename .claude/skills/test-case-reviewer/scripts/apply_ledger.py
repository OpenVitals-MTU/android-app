#!/usr/bin/env python3
"""Point the docs at the tests that survived a slimming pass.

Usage:
    python3 apply_ledger.py LEDGER.json [LEDGER.json ...] [--docs docs] [--dry-run]

A ledger is the JSON a reviewer writes for each test file it changed:

    {
      "file": "app/src/test/kotlin/.../FooTest.kt",
      "before": 37, "after": 12,
      "changes": [
        {"old": "metric distance uses meters below one kilometer",
         "new": "metric distance switches from meters to kilometers at one kilometer",
         "kind": "merged", "reason": "same formatter, one table"},
        {"old": "foo returns foo", "new": "foo returns foo and bar",
         "kind": "deleted", "reason": "rule 1: subset of the survivor"},
        {"old": "pure case pinned in the VM", "new": "the lower-level test",
         "new_file": "app/src/test/kotlin/.../LowerLevelTest.kt",
         "kind": "deleted", "reason": "rule 4"},
        {"old": "assertion-free test", "new": null, "kind": "deleted",
         "reason": "rule 6, nothing else to point at"}
      ]
    }

Kinds:
  merged       absorbed into the survivor named by `new`
  deleted      removed; `new` names the test that now covers it, or null
  renamed      same test, new name (`old` -> `new`)
  strengthened same name, real assertion added (`new` == `old`); no doc change

`new_file` is set only when the successor lives in another test file. The
citation `OldTest.kt: `old`` then becomes `NewTest.kt: `new``.

A file that changed with no name change still gets a ledger, with
"changes": []. Every cited `old` is rewritten; a cited deletion with a null
`new` is reported, because that row needs a human sentence. Every touched
doc line is printed: read its notes column, it may now be stale.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("ledgers", nargs="+")
    ap.add_argument("--docs", default="docs")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    # (old_file_basename, old_name) -> (new_file_basename, new_name)
    renames: list[tuple[str, str, str, str]] = []
    orphans: list[tuple[str, str]] = []
    for path in args.ledgers:
        led = json.load(open(path, encoding="utf-8"))
        old_file = os.path.basename(led["file"])
        for ch in led.get("changes", []):
            new = ch.get("new")
            if not new:
                orphans.append((old_file, ch["old"]))
                continue
            new_file = os.path.basename(ch.get("new_file") or led["file"])
            if new != ch["old"] or new_file != old_file:
                renames.append((old_file, ch["old"], new_file, new))

    docs = sorted(
        os.path.join(d, f)
        for d, _, fs in os.walk(args.docs)
        for f in fs
        if f.endswith(".md")
    )
    touched_lines = 0
    for doc in docs:
        lines = open(doc, encoding="utf-8", newline="").read().split("\n")
        changed = False
        for i, line in enumerate(lines):
            orig = line
            for old_file, old, new_file, new in renames:
                tick = f"`{old}`"
                if tick not in line:
                    continue
                if new_file != old_file:
                    # Move the citation to the successor's file when the row
                    # names the old file right before it; otherwise rename only.
                    pat = re.compile(re.escape(old_file) + r"(:\s*)" + re.escape(tick))
                    if pat.search(line):
                        line = pat.sub(lambda m: f"{new_file}{m.group(1)}`{new}`", line)
                        continue
                line = line.replace(tick, f"`{new}`")
            if line != orig:
                lines[i] = line
                changed = True
                touched_lines += 1
                print(f"{doc}:{i + 1}: {line.strip()[:220]}")
        if changed and not args.dry_run:
            open(doc, "w", encoding="utf-8", newline="").write("\n".join(lines))

    still_cited = []
    for file, old in orphans:
        for doc in docs:
            if f"`{old}`" in open(doc, encoding="utf-8").read():
                still_cited.append((doc, file, old))
    print(f"\n{len(renames)} renames, {touched_lines} doc lines touched; read each one's notes column")
    if still_cited:
        print("\nDeleted tests still cited with no successor (fix these rows by hand):")
        for doc, file, old in still_cited:
            print(f"  {doc}: {file}: `{old}`")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
