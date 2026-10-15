# Scripts

Nothing in here ships in the app. The shell scripts at the top level are what
CI and a release run (`ci-*.sh`, `release.sh`, `version-code.sh`, the publish
and announce scripts) and the checks the gate calls (`verify-translations.py`,
`verify-r8-keeps.py`, `generate-translation-coverage.py`). The fixture builders that
once lived beside them (`tool/`, then here) were removed in 2026-10; the
committed fixtures they produced stay as they are.

## `re/` (untracked)

The watch reverse-engineering workspace — frida scripts, btsnoop capture
tooling, protocol triage. It is deliberately **outside version control**
(`.git/info/exclude`): captures contain real device identifiers and personal
health data, and this repository is public. The workspace travels with the
checkout by hand, not by git; see its own README.

## What did NOT move here from the Flutter era

Nothing Dart survives in this repository. `scripts/verify_l10n.dart` and its
checks were superseded by `scripts/verify-translations.py` plus the JVM tests
(`StringFormatSpecifierTest`, `TranslationCatalogTest`).

## Replaying a night from a watch

A debuggable watch build keeps its rows in `databases/metrics.db`, one table
whose sleep rows are already `SM` lines. To run the phone's pipeline on a real
night and see the wear state, the window, the labels per hour and the stages it
would have written:

```sh
adb -s <watch> shell 'run-as tech.mmarca.openvitals.debug cat databases/metrics.db' > metrics.db
sqlite3 metrics.db "select line from rows where metric = 'sm' order by time_ms" > night.sm
```

Then:

```sh
./gradlew :app:testCiUnitTest --tests 'tech.mmarca.openvitals.domain.insights.WearNightReplayTest' \
  -Dopenvitals.wearNightLines=$PWD/night.sm
```

The test is skipped without the property; its report is in the test output.
