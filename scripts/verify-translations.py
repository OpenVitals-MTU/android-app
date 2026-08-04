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
# Flags, width and precision are part of the specifier, and a check that cannot
# see them cannot see the difference between "%1$.1f" and "%1$.1d" — the second
# throws IllegalFormatPrecisionException the moment the string is drawn. The
# narrower pattern this replaces matched only "%2$s" in "%1$.1f C · %2$s", so
# dropping or retyping the temperature passed the gate cleanly.
PLACEHOLDER_PATTERN = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]")
# A literal percent is not a placeholder. Consumed before the scan, because it
# can never MATCH the pattern above and so cannot be filtered out afterwards.
ESCAPED_PERCENT = "%%"
MIN_TRANSLATION_COVERAGE = 0.70

# The plural categories CLDR defines. Which subset a language uses is its own
# business; anything outside this set is a typo.
CLDR_PLURAL_QUANTITIES = {"zero", "one", "two", "few", "many", "other"}


@dataclasses.dataclass(frozen=True)
class ResourceEntry:
    kind: str
    translatable: bool
    values: dict[str, str]


def element_text(element: ET.Element) -> str:
    return "".join(element.itertext())


def placeholders(value: str) -> collections.Counter[str]:
    scanned = value.replace(ESCAPED_PERCENT, "")
    return collections.Counter(
        match.group(0)
        for match in PLACEHOLDER_PATTERN.finditer(scanned)
        # A specifier whose flags contain a space is what a LITERAL percent
        # decays into: Spanish writes "entre 50 % y 60 %", and "% y" lexes as
        # the conversion "y" with a space flag. Counting those would report a
        # placeholder mismatch against a base string that has no arguments at
        # all. The genuinely dangerous case — a bare % inside a string that IS
        # formatted, which is how stress_factor_hrv_above crashed four locales —
        # belongs to StringFormatSpecifierTest, which scans every locale for
        # exactly this shape and knows whether the string takes arguments.
        if " " not in match.group(0)[:-1]
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

    # A translation may legitimately carry MORE plural categories than the base
    # file: CLDR assigns them per language, and English only has one/other.
    # Spanish gained "many" for large numbers, Russian and Polish need few/many,
    # Arabic has six. Rejecting extras forbids correct translations. What is
    # worth catching is a quantity that is not a CLDR category at all, which is
    # a typo Android will silently ignore at runtime.
    if base.kind == "plurals":
        unknown_quantities = sorted(set(translated.values) - CLDR_PLURAL_QUANTITIES)
        for quantity in unknown_quantities:
            errors.append(f"{path}: {name} has unknown plural quantity {quantity}")

        # Those legitimate extra branches still have to carry the right
        # arguments. The loop above walks the BASE quantities, so a branch the
        # base does not have — Spanish's "many", Polish's "few" — was compared
        # against nothing at all and could hold any specifier it liked, or none.
        # Android resolves a count to whichever branch CLDR picks, so a broken
        # one crashes exactly as a broken "other" would. Compare it against
        # "other", which is the branch the base is guaranteed to have.
        base_other = base.values.get("other")
        if base_other is not None:
            base_placeholders = placeholders(base_other)
            for quantity in sorted(set(translated.values) - set(base.values)):
                if quantity not in CLDR_PLURAL_QUANTITIES:
                    continue
                extra_placeholders = placeholders(translated.values[quantity])
                if extra_placeholders != base_placeholders:
                    errors.append(
                        f"{path}: {name}[{quantity}] placeholder mismatch: "
                        f"expected {dict(base_placeholders)}, "
                        f"got {dict(extra_placeholders)}"
                    )

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
    # The floor decides whether a language is OFFERED, not whether its file may
    # exist. generate-translation-coverage.py keeps a language out of the picker
    # until it clears the same threshold, so a half-finished translation is
    # already invisible to users. Failing the build for one as well would mean a
    # language cannot be worked on incrementally at all — a translator's first
    # commit would break CI. What still has to hold for every file, offered or
    # not, is placeholder and plural safety, which is checked above.
    coverage = len(translated_base_names) / len(translatable_base_names)
    if coverage <= MIN_TRANSLATION_COVERAGE:
        print(
            f"note: {path} is at {coverage:.1%} coverage and is not offered in the "
            f"language picker until it passes {MIN_TRANSLATION_COVERAGE:.0%}."
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
