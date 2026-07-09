#!/usr/bin/env python3
"""Validate Android translation resource files for Weblate contributions."""

from __future__ import annotations

import collections
import dataclasses
import pathlib
import re
import sys
import xml.etree.ElementTree as ET


RES_DIR = pathlib.Path("app/src/main/res")
BASE_FILE = RES_DIR / "values" / "strings.xml"
PLACEHOLDER_PATTERN = re.compile(r"%(?:\d+\$)?[a-zA-Z]")
MIN_TRANSLATION_COVERAGE = 0.70


@dataclasses.dataclass(frozen=True)
class ResourceEntry:
    kind: str
    translatable: bool
    values: dict[str, str]


def element_text(element: ET.Element) -> str:
    return "".join(element.itertext())


def placeholders(value: str) -> collections.Counter[str]:
    return collections.Counter(
        match.group(0)
        for match in PLACEHOLDER_PATTERN.finditer(value)
        if match.group(0) != "%%"
    )


def parse_resource_file(path: pathlib.Path) -> tuple[dict[str, ResourceEntry], list[str]]:
    errors: list[str] = []
    entries: dict[str, ResourceEntry] = {}

    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as exc:
        return {}, [f"{path}: XML parse error: {exc}"]

    for child in root:
        if child.tag not in {"string", "plurals"}:
            continue

        name = child.attrib.get("name")
        if not name:
            errors.append(f"{path}: <{child.tag}> is missing a name attribute")
            continue

        if name in entries:
            errors.append(f"{path}: duplicate resource name {name}")
            continue

        translatable = child.attrib.get("translatable", "true") != "false"
        if child.tag == "string":
            values = {"value": element_text(child)}
        else:
            values = {}
            for item in child.findall("item"):
                quantity = item.attrib.get("quantity")
                if not quantity:
                    errors.append(f"{path}: plurals/{name} has an item without quantity")
                    continue
                values[quantity] = element_text(item)

        entries[name] = ResourceEntry(child.tag, translatable, values)

    return entries, errors


def compare_placeholders(
    path: pathlib.Path,
    name: str,
    base: ResourceEntry,
    translated: ResourceEntry,
) -> list[str]:
    errors: list[str] = []

    for key, base_value in base.values.items():
        if key not in translated.values:
            errors.append(f"{path}: {name} is missing plural quantity {key}")
            continue

        base_placeholders = placeholders(base_value)
        translated_placeholders = placeholders(translated.values[key])
        if base_placeholders != translated_placeholders:
            suffix = "" if key == "value" else f"[{key}]"
            errors.append(
                f"{path}: {name}{suffix} placeholder mismatch: "
                f"expected {dict(base_placeholders)}, got {dict(translated_placeholders)}"
            )

    extra_quantities = sorted(set(translated.values) - set(base.values))
    for quantity in extra_quantities:
        errors.append(f"{path}: {name} has extra plural quantity {quantity}")

    return errors


def validate_locale(path: pathlib.Path, base_entries: dict[str, ResourceEntry]) -> list[str]:
    locale_entries, errors = parse_resource_file(path)
    if errors:
        return errors

    base_names = set(base_entries)
    translatable_base_names = {
        name for name, entry in base_entries.items() if entry.translatable
    }
    non_translatable_base_names = base_names - translatable_base_names
    locale_names = set(locale_entries)

    translated_base_names = locale_names & translatable_base_names
    coverage = len(translated_base_names) / len(translatable_base_names)
    if coverage <= MIN_TRANSLATION_COVERAGE:
        errors.append(
            f"{path}: translation coverage is {coverage:.1%}; "
            f"must be greater than {MIN_TRANSLATION_COVERAGE:.0%}"
        )

    for name in sorted(locale_names - base_names):
        errors.append(f"{path}: extra translation not present in base file: {name}")

    for name in sorted(locale_names & non_translatable_base_names):
        errors.append(f"{path}: includes non-translatable base resource: {name}")

    for name in sorted(locale_names & translatable_base_names):
        base_entry = base_entries[name]
        locale_entry = locale_entries[name]
        if base_entry.kind != locale_entry.kind:
            errors.append(
                f"{path}: {name} has kind {locale_entry.kind}, expected {base_entry.kind}"
            )
            continue

        errors.extend(compare_placeholders(path, name, base_entry, locale_entry))

    return errors


def main() -> int:
    base_entries, errors = parse_resource_file(BASE_FILE)
    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1

    locale_files = sorted(RES_DIR.glob("values-*/strings.xml"))
    all_errors: list[str] = []
    for path in locale_files:
        all_errors.extend(validate_locale(path, base_entries))

    if all_errors:
        print("Translation validation failed:", file=sys.stderr)
        for error in all_errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        f"Translation validation passed for {len(locale_files)} locale files "
        f"with coverage greater than {MIN_TRANSLATION_COVERAGE:.0%}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
