#!/usr/bin/env python3
"""Builds the committed test fixture from a real Health Connect export.

    python3 tool/health_fixture/build.py \
        --db "test_objects/Health Connect/health_connect_export.db" \
        --out app/src/test/resources/golden.json

The export is a 106 MB dump of a real person's health data — heart rate, sleep,
weight, blood pressure, menstruation, sexual activity. It is gitignored, and
this repository is PUBLIC. So the fixture is DERIVED, never copied:

    KEPT, because it is the only thing that catches bugs:
        record boundaries, record→sample parentage, which app wrote what,
        recordingMethod, clientRecordId/version, lastModifiedTime, zone offsets,
        and WHICH FIELDS EACH WRITER LEAVES NULL.

    REPLACED:
        every value (bpm, speed, counts), every timestamp (one global shift),
        every writer package name, every id, every title and note.

    DROPPED ENTIRELY:
        menstruation, sexual activity, body fat — not scrubbed, not present.

The structure is what reproduces the bugs. A 17.48-hour HeartRateRecord that
swallows a workout does so because of its BOUNDARIES, not because of its bpm
values — so the boundaries survive verbatim and the values do not.

This is a line-for-line port of the Flutter era's `build.dart`; every formula,
query, alias and scrub rule is unchanged. The Dart original is gone with the
rest of the Dart toolchain, and the scrubbing rules are the part a port must
not reinterpret — where a choice looked odd, it was kept, not improved.
"""

import argparse
import hashlib
import json
import sqlite3
import sys
from datetime import datetime, timezone
from pathlib import Path

# Every timestamp moves by this much. ONE shift for the whole fixture — never
# per-record, which would decouple a workout from the heart rate inside it.
#
# A whole number of weeks, so weekday alignment survives (a Monday stays a
# Monday, and the weekly views still see a real week).
SHIFT_DAYS = 364  # 52 weeks
_SHIFT_MS = SHIFT_DAYS * 24 * 60 * 60 * 1000

# Real writer package -> the name it goes into the public repo under.
#
# `tech.mmarca.openvitals*` is NOT aliased. It is load-bearing: `isOpenVitalsEntry`,
# record ownership and the manual-entry count all key off it, so renaming it would
# quietly disable the very logic the fixture exists to test.
#
# The aliases name a ROLE, never the product. `com.example.healthsync` would still
# have told the world which apps this person runs, which is most of what the scrub
# is for — an alias that echoes the vendor is barely an alias.
WRITER_ALIASES = {
    "nodomain.freeyourgadget.gadgetbridge": "com.example.openwatch",
    "com.garmin.android.apps.connectmobile": "com.example.watchvendor",
    "com.google.android.apps.fitness": "com.example.fitplatform",
    "com.google.android.gms": "com.example.fitplatform.gms",
    "de.dennisguse.opentracks.debug": "com.example.tracker",
    "de.tadris.fitness.debug": "com.example.tracker2",
    "com.hevy": "com.example.strengthapp",
    "com.technogym.tgapp": "com.example.gymequip",
    "com.cemcakmak.hydrotracker": "com.example.hydration",
    "nl.appyhapps.healthsync": "com.example.syncbridge",
    "io.homeassistant.companion.android.minimal": "com.example.homeautomation",
    "dev.easonhuang.heartwood": "com.example.heartapp",
    "com.monkopedia.healthdisconnect": "com.example.privacytool",
    "dev.manu.hcdashboard": "com.example.dashboard",
    "dev.manu.openvitals": "com.example.dashboard2",
    "com.example.ot2hc": "com.example.importer",
    "android": "android",
    "com.android.shell": "com.android.shell",
}

# The slice. One contiguous week, chosen because it already contains almost
# every scenario — see the manifest it writes.
FROM_MS = int(datetime(2026, 6, 18, tzinfo=timezone.utc).timestamp() * 1000)
TO_MS = int(datetime(2026, 6, 26, tzinfo=timezone.utc).timestamp() * 1000)


# ── the shift, the aliases, the ids ─────────────────────────────────────────


def _shift(epoch_ms):
    return epoch_ms - _SHIFT_MS


def _id(real, salt):
    """Rehashed, not randomised: the same input always gives the same output, so
    the EQUALITY RELATION survives. Records that shared a clientRecordId still
    share one, which is what dedup-by-client-id and the hydration↔nutrition
    pairing key off."""
    if real is None:
        return ""
    return hashlib.sha256(f"{salt}::{real}".encode()).hexdigest()[:16]


def _writers(db):
    rows = db.execute("SELECT row_id, package_name FROM application_info_table")
    return {row_id: _alias(package) for row_id, package in rows}


def _alias(package):
    """The app's OWN package is kept verbatim. `isOpenVitalsEntry`, record
    ownership and the manual-entry count all key off it — aliasing it would
    quietly disable the very logic the fixture exists to test. (An earlier
    version of the Dart original aliased it to `com.example.unknownN` via the
    fallback below, and the leak check is what caught it.)"""
    if package.startswith("tech.mmarca.openvitals"):
        return package
    alias = WRITER_ALIASES.get(package)
    if alias is not None:
        return alias
    # Unknown writer: alias it rather than let a real package name through.
    # Failing closed matters more here than being helpful — this file's output
    # is committed publicly.
    digest = hashlib.sha256(package.encode()).hexdigest()[:6]
    return f"com.example.writer{digest}"


def _provenance(r, writers):
    """The provenance block every record carries. This is the half the original
    port kept losing, so it is the half the fixture is most careful with."""
    return {
        "id": _id(str(r["row_id"]), "rec"),
        "writer": writers.get(r["app_info_id"], "com.example.unknown"),
        "start": _shift(r["start_time"]),
        "end": _shift(r["end_time"]),
        "startZoneOffsetSeconds": r["start_zone_offset"],
        "endZoneOffsetSeconds": r["end_zone_offset"],
        "recordingMethod": r["recording_method"],
        "lastModified": None
        if r["last_modified_time"] is None
        else _shift(r["last_modified_time"]),
        "clientRecordId": None
        if r["client_record_id"] is None
        else _id(r["client_record_id"], "client"),
        # Health Connect stores client_record_version as TEXT, so it arrives as
        # a string. The reader's field is an int, and an unparsed string blanked
        # a whole screen with no error anywhere. Parse it here.
        "clientRecordVersion": _as_int(r["client_record_version"]),
    }


def _as_int(value):
    if value is None:
        return None
    if isinstance(value, int):
        return value
    if isinstance(value, str):
        try:
            return int(value)
        except ValueError:
            return None
    if isinstance(value, float):
        return int(value)
    return None


# ── record types ────────────────────────────────────────────────────────────


def _heart_rate(db, writers):
    """Heart rate, delta-encoded. The 891-sample swallowing record is the point
    of the whole fixture, so it is kept at FULL fidelity — every sample, exact
    sample-to-sample spacing. The irregular cadence is real shape: a writer that
    samples every 5 s during exercise and every 10 min at rest is what produces
    records like this one.

    The bpm VALUES are synthesized. They are the one thing that cannot cause a
    bug: nothing in the app branches on 62 vs 64."""
    out = []
    records = db.execute(
        "SELECT * FROM heart_rate_record_table "
        "WHERE start_time >= ? AND start_time < ? ORDER BY start_time",
        (FROM_MS, TO_MS),
    ).fetchall()

    for r in records:
        samples = db.execute(
            "SELECT epoch_millis FROM heart_rate_record_series_table "
            "WHERE parent_key = ? ORDER BY epoch_millis",
            (r["row_id"],),
        ).fetchall()
        if not samples:
            continue

        times = [_shift(s["epoch_millis"]) for s in samples]
        out.append(
            {
                **_provenance(r, writers),
                "t0": times[0],
                "dt": [times[i] - times[i - 1] for i in range(1, len(times))],
                "bpm": [_synthetic_bpm(t, i) for i, t in enumerate(times)],
            }
        )
    return out


def _synthetic_bpm(epoch_ms, index):
    """Plausible, and COHERENT: a resting baseline with a slow diurnal drift.
    Not noise — a chart of it should look like a heart rate, because a human
    will look at these tests' output when one fails."""
    hour = datetime.fromtimestamp(epoch_ms / 1000, tz=timezone.utc).hour
    base = 54 if hour < 6 else 66
    return base + (index * 7) % 11


def _exercise(db, writers):
    records = db.execute(
        "SELECT * FROM exercise_session_record_table "
        "WHERE start_time >= ? AND start_time < ? ORDER BY start_time",
        (FROM_MS, TO_MS),
    ).fetchall()

    return [
        {
            **_provenance(r, writers),
            "exerciseType": r["exercise_type"],
            # Never carry a real free-text note or title across.
            "title": None if r["title"] is None else "Session",
            "notes": None if r["notes"] is None else "Recorded by a device.",
            "route": _route(db, r["row_id"]),
        }
        for r in records
    ]


def _route(db, parent_key):
    """The GPS track, rotated and re-anchored to a synthetic origin. Shape,
    length and speed profile survive exactly — so distance, splits and pace are
    genuinely exercised — while the location does not. Altitude keeps its
    relative profile."""
    points = db.execute(
        "SELECT timestamp_millis, latitude, longitude, altitude, horizontal_accuracy "
        "FROM exercise_route_table WHERE parent_key = ? ORDER BY timestamp_millis",
        (parent_key,),
    ).fetchall()
    if not points:
        return []

    lat0 = points[0]["latitude"]
    lon0 = points[0]["longitude"]
    alt0 = points[0]["altitude"]
    # A synthetic origin in the North Sea. Nothing is there.
    anchor_lat = 56.0
    anchor_lon = 3.0

    return [
        {
            "t": _shift(p["timestamp_millis"]),
            "lat": anchor_lat + (p["latitude"] - lat0),
            "lon": anchor_lon + (p["longitude"] - lon0),
            "alt": 10.0 + (p["altitude"] - alt0),
            "acc": p["horizontal_accuracy"],
        }
        for p in points
    ]


def _sleep(db, writers):
    records = db.execute(
        "SELECT * FROM sleep_session_record_table "
        "WHERE start_time >= ? AND start_time < ? ORDER BY start_time",
        (FROM_MS, TO_MS),
    ).fetchall()

    return [
        {
            **_provenance(r, writers),
            "title": None if r["title"] is None else "Sleep",
            "notes": None if r["notes"] is None else "Sleep data from a device.",
            "stages": [
                {
                    "start": _shift(s["stage_start_time"]),
                    "end": _shift(s["stage_end_time"]),
                    "type": s["stage_type"],
                }
                for s in db.execute(
                    "SELECT stage_start_time, stage_end_time, stage_type "
                    "FROM sleep_stages_table WHERE parent_key = ? "
                    "ORDER BY stage_start_time",
                    (r["row_id"],),
                )
            ],
        }
        for r in records
    ]


def _steps(db, writers):
    """Interval records — the sibling records a watch writes BESIDE a session,
    which is why a recorded walk showed no steps. Counts are synthesized from
    the duration, so they stay proportionate and the aggregates remain sane."""
    records = db.execute(
        "SELECT * FROM steps_record_table "
        "WHERE start_time >= ? AND start_time < ? ORDER BY start_time",
        (FROM_MS, TO_MS),
    ).fetchall()

    return [
        {
            **_provenance(r, writers),
            "count": 1 + (r["end_time"] - r["start_time"]) // 1000 // 2,
        }
        for r in records
    ]


def _series(db, writers, parent_table, sample_table, value_column, synth):
    """A series record: a parent carrying the provenance, and its nested
    samples. Exactly the shape that causes the bug, so it is exactly the shape
    that must survive. Sample TIMES are kept (shifted); sample VALUES are
    synthesized."""
    out = []
    records = db.execute(
        f"SELECT * FROM {parent_table} WHERE start_time >= ? AND start_time < ? "
        "ORDER BY start_time",
        (FROM_MS, TO_MS),
    ).fetchall()

    for r in records:
        samples = db.execute(
            f"SELECT epoch_millis, {value_column} FROM {sample_table} "
            "WHERE parent_key = ? ORDER BY epoch_millis",
            (r["row_id"],),
        ).fetchall()
        if not samples:
            continue

        times = [_shift(s["epoch_millis"]) for s in samples]
        out.append(
            {
                **_provenance(r, writers),
                "t0": times[0],
                "dt": [times[i] - times[i - 1] for i in range(1, len(times))],
                "v": [
                    synth(float(s[value_column]), i) for i, s in enumerate(samples)
                ],
            }
        )
    return out


def _interval(db, writers, table, value_column, synth):
    """An interval record — a total over a window. Steps, distance, calories,
    elevation, hydration.

    The VALUE is derived from the window's own duration, so totals stay
    proportionate to the time they cover and the aggregates over them remain
    sane. A distance of 4 km over 40 minutes has to keep being a plausible
    pace, or every split assertion downstream is meaningless."""
    records = db.execute(
        f"SELECT * FROM {table} WHERE start_time >= ? AND start_time < ? "
        "ORDER BY start_time",
        (FROM_MS, TO_MS),
    ).fetchall()

    return [
        {
            **_provenance(r, writers),
            "v": synth(r["end_time"] - r["start_time"]),
        }
        for r in records
    ]


def _instant(db, writers, table, value_column, synth):
    """An instantaneous record — one value at one moment. HRV, resting heart
    rate, BMR. Its time column is `time`, not `start_time`, so it cannot share
    `_interval`."""
    records = db.execute(
        f"SELECT * FROM {table} WHERE time >= ? AND time < ? ORDER BY time",
        (FROM_MS, TO_MS),
    ).fetchall()

    return [
        {
            "id": _id(str(r["row_id"]), "rec"),
            "writer": writers.get(r["app_info_id"], "com.example.unknown"),
            "time": _shift(r["time"]),
            "zoneOffsetSeconds": r["zone_offset"],
            "recordingMethod": r["recording_method"],
            "lastModified": None
            if r["last_modified_time"] is None
            else _shift(r["last_modified_time"]),
            "v": synth(i),
        }
        for i, r in enumerate(records)
    ]


def _synthetic_series(exercise, kind, synth):
    """Wholly invented, and labelled as such.

    The export contains ZERO PowerRecords and ZERO CyclingPedalingCadenceRecords
    — this person has no power meter, so there is nothing to derive from. But
    the app writes PowerRecord from a BLE sensor, asks Health Connect for
    READ_POWER, and for a long time never read it back. A fix with nothing to
    test it against is a fix that will break again.

    So: a sample a minute across a real session's window. Every other record in
    this fixture inherits its SHAPE from real data; these two do not, and
    `synthetic: true` says so, so nobody later mistakes them for evidence of
    how a real power meter behaves."""
    if not exercise:
        return []
    # The session with a GPS route: the long outdoor one. That is where a power
    # meter and a cadence sensor would actually be, and attaching them to a
    # three-minute session would produce a record with one sample in it.
    session = max(exercise, key=lambda s: len(s["route"]))
    start = session["start"]
    end = session["end"]

    times = list(range(start, end, 60000))
    if len(times) < 2:
        return []

    return [
        {
            "synthetic": True,
            "id": _id(f"{kind}-synthetic", "rec"),
            "writer": session["writer"],
            "start": start,
            "end": end,
            "startZoneOffsetSeconds": session["startZoneOffsetSeconds"],
            "endZoneOffsetSeconds": session["endZoneOffsetSeconds"],
            "recordingMethod": 2,  # AUTOMATICALLY_RECORDED
            "lastModified": session["lastModified"],
            "clientRecordId": None,
            "clientRecordVersion": None,
            "t0": times[0],
            "dt": [times[i] - times[i - 1] for i in range(1, len(times))],
            "v": [synth(i) for i in range(len(times))],
        }
    ]


# ── manifest ────────────────────────────────────────────────────────────────


def _manifest(fixture, writers):
    hr = fixture["heartRate"]
    longest = max(hr, key=lambda r: r["end"] - r["start"])

    return {
        "version": 1,
        "note": "GENERATED by tool/health_fixture/build.py from a real Health "
        "Connect export. Values, timestamps, writer names and ids are all "
        "synthetic; record BOUNDARIES and provenance are real, because that is "
        "what reproduces the bugs. Do not hand-edit — regenerate.",
        "shiftDays": SHIFT_DAYS,
        # The alias table is NOT written here. Its KEYS are the real package
        # names — emitting it would have leaked every one of them into a public
        # repo, in the file whose whole job is to not do that. The leak check
        # caught it.
        "writers": sorted(set(writers.values())),
        "counts": {
            k: len(v) for k, v in fixture.items() if isinstance(v, list)
        },
        # Named so tests never hardcode a date.
        "days": {
            "swallowingHr": datetime.fromtimestamp(
                longest["start"] / 1000, tz=timezone.utc
            ).strftime("%Y-%m-%d"),
        },
        "longestHeartRateRecordHours": (longest["end"] - longest["start"])
        / 3600000.0,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--db", default="test_objects/Health Connect/health_connect_export.db"
    )
    parser.add_argument("--out", default="app/src/test/resources/golden.json")
    args = parser.parse_args()

    if not Path(args.db).is_file():
        sys.stderr.write(
            f"No export at {args.db}.\n"
            "It is gitignored on purpose. The FIXTURE is the committed artifact "
            "— you only need the export to regenerate it.\n"
        )
        sys.exit(1)

    db = sqlite3.connect(f"file:{args.db}?mode=ro", uri=True)
    db.row_factory = sqlite3.Row
    writers = _writers(db)
    exercise = _exercise(db, writers)
    fixture = {
        "heartRate": _heart_rate(db, writers),
        "exercise": exercise,
        "sleep": _sleep(db, writers),
        # SERIES records. The same shape as heart rate, and the same bug: Health
        # Connect filters them by the RECORD's boundary, so a workout buried
        # inside a longer record reads as having no speed either — which is why
        # the 1 km splits silently fell back to "estimated" on exactly the
        # activities whose heart rate had vanished.
        "speed": _series(
            db, writers, "SpeedRecordTable", "speed_record_table", "speed",
            lambda v, i: 1.6 + (i % 9) * 0.15,
        ),
        "stepsCadence": _series(
            db, writers, "StepsCadenceRecordTable", "steps_cadence_record_table",
            "rate", lambda v, i: 78.0 + (i % 20),
        ),
        # SIBLING records — the ones a watch writes BESIDE a session rather than
        # in it. Reading the session alone is why a recorded walk showed
        # "Steps: Not available" above a chart of its own step cadence.
        "steps": _steps(db, writers),
        "distance": _interval(
            db, writers, "distance_record_table", "distance",
            lambda duration_ms: duration_ms / 1000 * 1.4,
        ),
        "activeCalories": _interval(
            db, writers, "active_calories_burned_record_table", "energy",
            lambda duration_ms: duration_ms / 60000 * 5.0 * 4184,
        ),
        "totalCalories": _interval(
            db, writers, "total_calories_burned_record_table", "energy",
            lambda duration_ms: duration_ms / 60000 * 7.0 * 4184,
        ),
        "elevationGained": _interval(
            db, writers, "elevation_gained_record_table", "elevation",
            lambda duration_ms: duration_ms / 60000 * 0.8,
        ),
        # The calorie fallback chain: recorded total wins, else active + BMR
        # pro-rated over the window. Without BMR records the chain's second
        # branch is unreachable. BMR is an INSTANT record (a rate at a moment),
        # not an interval. The calorie chain pro-rates it across the window it
        # needs.
        "basalMetabolicRate": _instant(
            db, writers, "basal_metabolic_rate_record_table",
            "basal_metabolic_rate", lambda _i: 1650.0,
        ),
        "hrv": _instant(
            db, writers, "heart_rate_variability_rmssd_record_table",
            "heart_rate_variability_millis", lambda i: 42.0 + (i % 17),
        ),
        "restingHeartRate": _instant(
            db, writers, "resting_heart_rate_record_table", "beats_per_minute",
            lambda i: 52.0 + (i % 8),
        ),
        "hydration": _interval(
            db, writers, "hydration_record_table", "volume", lambda _d: 0.25,
        ),
        # NOT IN THE SOURCE DATA — see _synthetic_series.
        "power": _synthetic_series(exercise, "power", lambda i: 180.0 + (i % 40)),
        "cyclingCadence": _synthetic_series(
            exercise, "cyclingCadence", lambda i: 82.0 + (i % 12),
        ),
    }
    fixture["manifest"] = _manifest(fixture, writers)
    db.close()

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(fixture, indent=2), encoding="utf-8")

    kb = round(out.stat().st_size / 1024)
    print(f"Wrote {args.out} ({kb}kB)")
    for k, v in fixture["manifest"]["counts"].items():
        print(f"  {k}: {v}")


if __name__ == "__main__":
    main()
