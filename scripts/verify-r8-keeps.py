#!/usr/bin/env python3
"""Checks that R8 kept what is reached by name at run time.

Nothing runs the minified build before it ships, so a class or a constructor
that only reflection reaches can vanish unseen. R8 once stripped the no-arg
constructor of a Glance ActionCallback, and every widget tap was dead from
2.7.0 to 2.7.1.

Three groups are checked against mapping.txt:
  1. Manifest components. Android makes them by name, with no arguments.
  2. Glance ActionCallback subclasses. Glance does the same at tap time.
  3. Health Connect record classes named in SyncRecordCodec.kt. Phone sync
     sends the simple name to the other phone, so it must not be renamed.

Exit 0 when all hold, 1 with one line per problem.
"""

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ANDROID = "{http://schemas.android.com/apk/res/android}"
COMPONENT_TAGS = ("application", "activity", "service", "receiver", "provider")
APP_PACKAGE = "tech.mmarca.openvitals"

CLASS_LINE = re.compile(r"^(\S+) -> (\S+):$")
NO_ARG_CONSTRUCTOR = re.compile(r"\bvoid <init>\(\)(?::\d+(?::\d+)?)? -> <init>$")
ACTION_CALLBACK = re.compile(r"\bclass\s+(\w+)\s*(?:\([^)]*\))?\s*:\s*[^{\n]*\bActionCallback\b")
PACKAGE_LINE = re.compile(r"^package\s+([\w.]+)", re.M)
RECORD_CLASS = re.compile(r"\b(\w+Record)::class\b")


def manifest_components(manifest: Path) -> set[str]:
    root = ET.parse(manifest).getroot()
    package = root.get("package", APP_PACKAGE)
    names = set()
    for element in root.iter():
        if element.tag in COMPONENT_TAGS:
            name = element.get(ANDROID + "name")
        elif element.tag == "activity-alias":
            name = element.get(ANDROID + "targetActivity")
        else:
            continue
        if not name:
            continue
        if name.startswith("."):
            name = package + name
        # Library components ship their own keep rules. This guards ours.
        if name.startswith(APP_PACKAGE):
            names.add(name)
    return names


def action_callbacks(sources: Path) -> set[str]:
    names = set()
    for path in sources.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        if "ActionCallback" not in text:
            continue
        package = PACKAGE_LINE.search(text)
        for match in ACTION_CALLBACK.finditer(text):
            names.add(f"{package.group(1)}.{match.group(1)}" if package else match.group(1))
    return names


def sync_record_classes(sources: Path) -> set[str]:
    codec = next(sources.rglob("SyncRecordCodec.kt"), None)
    if codec is None:
        return set()
    text = codec.read_text(encoding="utf-8")
    return {f"androidx.health.connect.client.records.{name}" for name in RECORD_CLASS.findall(text)}


def read_mapping(mapping: Path, wanted: set[str]) -> dict[str, dict]:
    """One pass: the file runs past 100 MB. Returns, per wanted class, its new name and whether <init>() survived."""
    found: dict[str, dict] = {}
    current = None
    with mapping.open(encoding="utf-8", errors="replace") as lines:
        for line in lines:
            if not line.startswith((" ", "#")):
                match = CLASS_LINE.match(line.rstrip("\n"))
                current = None
                if match and match.group(1) in wanted:
                    current = found.setdefault(match.group(1), {"renamed_to": match.group(2), "init": False})
            elif current is not None and NO_ARG_CONSTRUCTOR.search(line.rstrip("\n")):
                current["init"] = True
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--mapping", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--sources", type=Path, required=True)
    args = parser.parse_args()

    for path in (args.mapping, args.manifest, args.sources):
        if not path.exists():
            print(f"verify-r8-keeps: {path} does not exist", file=sys.stderr)
            return 1

    components = manifest_components(args.manifest)
    callbacks = action_callbacks(args.sources)
    records = sync_record_classes(args.sources)
    # A check that finds nothing to check has stopped checking.
    for label, group in (("manifest components", components), ("ActionCallback classes", callbacks),
                         ("sync record classes", records)):
        if not group:
            print(f"verify-r8-keeps: found no {label}; the scan is broken", file=sys.stderr)
            return 1

    found = read_mapping(args.mapping, components | callbacks | records)
    problems = []
    for name in sorted(components | callbacks | records):
        entry = found.get(name)
        if entry is None:
            problems.append(f"{name}: removed by R8")
        elif entry["renamed_to"] != name:
            problems.append(f"{name}: renamed to {entry['renamed_to']}")
        elif name not in records and not entry["init"]:
            problems.append(f"{name}: its no-arg constructor was removed, so it cannot be made by name")

    if problems:
        print("verify-r8-keeps: R8 removed or renamed what is reached by name:", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        print("Add a keep rule to app/proguard-rules.pro, with its reason.", file=sys.stderr)
        return 1

    print(
        f"verify-r8-keeps: {len(components)} manifest components, {len(callbacks)} ActionCallback "
        f"classes and {len(records)} sync record classes kept."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
