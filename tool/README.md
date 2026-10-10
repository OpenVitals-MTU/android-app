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

## `sleep_fixture/`

`build.py` derives the committed sleep-minute fixture
(`app/src/test/resources/fit/sleep/venu_sq_minutes.json`, read by
`SleepStageEstimatorTest`) from a folder of raw `SLEEP_*.fit` files pulled off
a Garmin watch that leaves sleep staging to Garmin's servers. The files are
real nights of a real person and are gitignored, so the fixture is **derived,
never copied**: heart rate is rounded to whole beats, movement and activity to
two decimals, every timestamp moves by one whole-week shift into 2020, and
serial numbers, file names and the other packed features are dropped. The
script's header lists the rules. Needs only the Python 3 standard library.

```sh
python3 tool/sleep_fixture/build.py \
  --in 2026 \
  --out app/src/test/resources/fit/sleep/venu_sq_minutes.json
```

## `sleep_accel_fixture/`

The Wear OS sleep pipeline's feature specification as code, and the local
evaluation fixture against polysomnography.

- `features.py` mirrors `docs/engineering/sleep-minute-features.md`: it turns
  accelerometer samples in g, stored heart rate samples and the worn,
  charging, screen and contact events into `SM` lines exactly as the watch's
  `MinuteAggregator` does. `feature_vector.json` is the committed lock
  between the two: `python3 -I tool/sleep_accel_fixture/features.py
  --self-check` fails when the Python drifts, and the watch test
  `SleepMinuteFeatureVectorTest` fails when the Kotlin drifts.
  `--make-vector` regenerates the vector (synthetic minutes, no real data).
- `build.py` converts PhysioNet's `sleep-accel` dataset (Walch 2019, 31
  subjects, Apple Watch motion and heart rate with polysomnography labels,
  ODC-By licence, doi:10.13026/hmhs-py35) into per-minute `SM` lines with a
  label per minute. **Neither the download nor the output is committed**
  (`data/` and `out/` are gitignored): the fixture is for local evaluation
  only. Download the archive from
  https://physionet.org/content/sleep-accel/1.0.0/, unpack it under
  `tool/sleep_accel_fixture/data/`, then:

  ```sh
  python3 -I tool/sleep_accel_fixture/build.py \
    --in tool/sleep_accel_fixture/data/sleep-accel-1.0.0 \
    --out tool/sleep_accel_fixture/out/sleep_accel_minutes.json
  ```

  The evaluation test `SleepAccelEvaluationTest` reads that file only when
  pointed at it and is skipped otherwise:

  ```sh
  ./gradlew :app:testCiUnitTest --tests 'tech.mmarca.openvitals.domain.insights.SleepAccelEvaluationTest' \
    -Dopenvitals.sleepAccelFixture=$PWD/tool/sleep_accel_fixture/out/sleep_accel_minutes.json
  ```

  It prints the table (sleep sensitivity, wake specificity, accuracy, onset
  and offset error, total sleep time error, three-class agreement) that a
  threshold change quotes in its commit message, since CI never sees it.

### Replaying a night from a watch

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

## `ppg_raw/`

`inspect.py` reads the CSV the watch app's `PpgRawLogger` writes (a debuggable
build only, a long press on the watch's status screen starts and stops it; the
file lives under the app's `files/ppg_raw/` and comes off the watch with
`adb shell run-as tech.mmarca.openvitals.debug cat files/ppg_raw/<name>.csv`).
It prints, per raw value column, the distinct count, range, mean, standard
deviation and the strongest autocorrelation peak between 0.4 and 2 seconds as
beats per minute, so the sixteen undocumented floats of Samsung's
`com.samsung.sensor.hr_raw` can be told apart: waveform channels peak near the
pulse, status and counter columns do not. Standard library only. A research
spike; nothing in the app depends on it.
