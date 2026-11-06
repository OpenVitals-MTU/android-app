# Scripts

Nothing in here ships in the app. The shell scripts at the top level are what
CI and a release run (`ci-*.sh`, `release.sh`, `version-code.sh`, the publish
and announce scripts) and the checks the gate calls (`verify-translations.py`,
`verify-r8-keeps.py`, `generate-translation-coverage.py`). The fixture builders that
once lived beside them (`tool/`, then here) were removed in 2026-10; the
committed fixtures they produced stay as they are.

## What did NOT move here from the Flutter era

Nothing Dart survives in this repository. `scripts/verify_l10n.dart` and its
checks were superseded by `scripts/verify-translations.py` plus the JVM tests
(`StringFormatSpecifierTest`, `TranslationCatalogTest`).
