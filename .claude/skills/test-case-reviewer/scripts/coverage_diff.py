#!/usr/bin/env python3
"""Fail when a test cut lost production coverage.

Usage:
    python3 coverage_diff.py BASELINE.xml AFTER.xml

Both files are Kover (JaCoCo-format) XML reports, e.g.
app/build/reports/kover/reportCi.xml. Compares covered LINE and BRANCH
counters per source file and per method. Prints every method whose covered
lines or branches went down and exits 1 if there is any.

Coverage is a floor, not proof: an assertion can be weakened without moving
coverage. The review step guards assertions; this guards reach.
"""

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET


def counters(node) -> dict[str, int]:
    return {c.get("type"): int(c.get("covered")) for c in node.findall("counter")}


def load(path: str) -> dict[str, dict[str, int]]:
    out: dict[str, dict[str, int]] = {}
    root = ET.parse(path).getroot()
    for pkg in root.iter("package"):
        for cls in pkg.findall("class"):
            for m in cls.findall("method"):
                key = f"{cls.get('name')}#{m.get('name')}{m.get('desc')}"
                out[key] = counters(m)
    return out


def total(path: str) -> dict[str, int]:
    return counters(ET.parse(path).getroot())


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    base, after = load(sys.argv[1]), load(sys.argv[2])
    lost = []
    for key, b in base.items():
        a = after.get(key, {})
        for kind in ("LINE", "BRANCH"):
            if a.get(kind, 0) < b.get(kind, 0):
                lost.append((key, kind, b.get(kind, 0), a.get(kind, 0)))
    tb, ta = total(sys.argv[1]), total(sys.argv[2])
    for kind in ("LINE", "BRANCH", "INSTRUCTION"):
        print(f"{kind:<11} covered {tb.get(kind, 0):>8} -> {ta.get(kind, 0):>8}")
    if not lost:
        print("OK: no method lost line or branch coverage")
        return 0
    print(f"\n{len(lost)} coverage losses:")
    for key, kind, b, a in sorted(lost):
        print(f"  {kind:<6} {b:>4} -> {a:<4} {key}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
