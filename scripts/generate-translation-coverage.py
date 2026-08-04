#!/usr/bin/env python3
"""Generate Android resources listing languages eligible for the app picker."""

from __future__ import annotations

import pathlib
import sys
import xml.etree.ElementTree as ET


RES_DIR = pathlib.Path("app/src/main/res")
BASE_FILE = RES_DIR / "values" / "strings.xml"
MIN_TRANSLATION_COVERAGE = 0.70


def parse_resource_names(path: pathlib.Path, *, translatable_only: bool) -> set[str]:
    root = ET.parse(path).getroot()
    names: set[str] = set()
    for child in root:
        if child.tag not in {"string", "plurals"}:
            continue
        if translatable_only and child.attrib.get("translatable") == "false":
            continue
        name = child.attrib.get("name")
        if name:
            names.add(name)
    return names


def android_qualifier_to_language_tag(qualifier: str) -> str:
    value = qualifier.removeprefix("values-")
    if value.startswith("b+"):
        return "-".join(value.removeprefix("b+").split("+"))

    parts = value.split("-")
    language = parts[0]
    region = next((part[1:] for part in parts[1:] if part.startswith("r")), None)
    return f"{language}-{region}" if region else language


def write_xml(output_dir: pathlib.Path, coverage: dict[str, int]) -> None:
    values_dir = output_dir / "values"
    values_dir.mkdir(parents=True, exist_ok=True)

    tags = sorted(coverage)
    if "en" not in tags:
        tags.insert(0, "en")
    else:
        tags.remove("en")
        tags.insert(0, "en")

    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<resources>",
        (
            f'    <integer name="translation_picker_minimum_coverage_percent">'
            f"{int(MIN_TRANSLATION_COVERAGE * 100)}</integer>"
        ),
        '    <string-array name="translation_picker_language_tags" translatable="false">',
    ]
    lines.extend(f"        <item>{tag}</item>" for tag in tags)
    lines.append("    </string-array>")
    lines.append('    <string-array name="translation_picker_language_coverage" translatable="false">')
    lines.extend(f"        <item>{tag}:{coverage[tag]}</item>" for tag in tags)
    lines.extend(["    </string-array>", "</resources>", ""])

    (values_dir / "translation_coverage.xml").write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: generate-translation-coverage.py <generated-res-dir>", file=sys.stderr)
        return 2

    output_dir = pathlib.Path(sys.argv[1])
    base_names = parse_resource_names(BASE_FILE, translatable_only=True)
    coverage_percent_by_tag: dict[str, int] = {"en": 100}

    for path in sorted(RES_DIR.glob("values-*/strings.xml")):
        language_tag = android_qualifier_to_language_tag(path.parent.name)
        names = parse_resource_names(path, translatable_only=False)
        coverage_ratio = len(base_names & names) / len(base_names)
        percent = round(coverage_ratio * 100)
        if coverage_ratio > MIN_TRANSLATION_COVERAGE:
            coverage_percent_by_tag[language_tag] = percent

    write_xml(output_dir, coverage_percent_by_tag)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
