# Host-side tooling

Nothing in here ships in the app; these run on a development machine.

## `health_fixture/`

`build.py` regenerates the committed Health Connect test fixture
(`app/src/test/resources/golden.json`, read by `HcFixture.kt`) from a real
Health Connect export. The export is a database dump of a real person's health
data, so the fixture is **derived, never copied** — the script's header
documents exactly what is kept and what is scrubbed.

It is a line-for-line port of the Flutter era's `build.dart` (this repo keeps
no Dart): every query, formula, alias and scrub rule is unchanged, verified by
running both semantics against a synthetic export covering every table — the
per-record key sets also match the committed fixture exactly. Needs only the
Python 3 standard library.

```sh
python3 tool/health_fixture/build.py \
  --db "path/to/health_connect_export.db" \
  --out app/src/test/resources/golden.json
```

## `re/` (untracked)

The watch reverse-engineering workspace — frida scripts, btsnoop capture
tooling, protocol triage. It is deliberately **outside version control**
(`.git/info/exclude`): captures contain real device identifiers and personal
health data, and this repository is public. The workspace travels with the
checkout by hand, not by git; see its own README.

## What did NOT move here from the Flutter era

Nothing Dart survives in this repository. `tool/verify_l10n.dart` and its
checks were superseded by `scripts/verify-translations.py` plus the JVM tests
(`StringFormatSpecifierTest`, `TranslationCatalogTest`); `build.dart` was
ported to `build.py` above.
