#!/usr/bin/env python3
"""Builds the committed sleep-minute fixture from real Garmin SLEEP FIT files.

    python3 tool/sleep_fixture/build.py \
        --in 2026 \
        --out app/src/test/resources/fit/sleep/venu_sq_minutes.json

The input is a folder of SLEEP_*.fit files pulled off a Venu SQ, a watch that
leaves sleep staging to Garmin's servers. They hold real nights of a real
person and are gitignored; this repository is PUBLIC. So the fixture is
DERIVED, never copied:

    KEPT, because the estimator is tested against its shape:
        one row per minute, in order, and per row: whether the watch called
        the minute raw / awake / not worn, the heart rate (rounded to a whole
        beat), the movement count and the activity magnitude (two decimals),
        and the UTC offset of the stream.

    REPLACED:
        every timestamp, by ONE global shift to a fictional week in 2020, so
        the minutes keep their spacing and the nights keep their weekday.

    DROPPED ENTIRELY:
        serial numbers, product ids, file names and indices, the seven other
        packed features, and everything the FIT container carries besides
        messages 273, 274 and 275.

Decoding mirrors `GarminWellnessInterpreter` in the app: message 273 gives the
stream start and local offset, message 274 rows are ten little-endian float16
values at start + 60 * i (heart rate last, movement at index 8, activity at
index 0), and the 0/1 rows of message 275 take a slot of the same stream.
Needs only the Python 3 standard library.
"""

import argparse
import datetime as dt
import glob
import json
import os
import struct
import sys

GARMIN_EPOCH = 631065600
SLEEP_DATA_INFO = 273
SLEEP_DATA_RAW = 274
SLEEP_STAGE = 275
TIMESTAMP_FIELD = 253
LOCAL_TIMESTAMP_FIELD = 2
SAMPLE_LENGTH_FIELD = 1
RAW_BYTES = 20

# The first minute keeps its time of day and weekday; only the date moves, by
# whole weeks, to the week of this Monday.
FICTIONAL_WEEK = dt.date(2020, 1, 6)

KIND_CODES = {"raw": "R", "awake": "A", "unmeasurable": "U"}


def parse(path):
    """Yields (global_message_number, {field: bytes}) for one FIT file."""
    data = open(path, "rb").read()
    header = data[0]
    end = header + struct.unpack("<I", data[4:8])[0]
    pos = header
    definitions = {}
    while pos < end:
        record_header = data[pos]
        pos += 1
        if record_header & 0x80:
            raise ValueError(f"{path}: compressed timestamp headers are not handled")
        local = record_header & 0x0F
        if record_header & 0x40:
            architecture = data[pos + 1]
            pos += 2
            order = "<" if architecture == 0 else ">"
            global_number = struct.unpack(order + "H", data[pos : pos + 2])[0]
            pos += 2
            count = data[pos]
            pos += 1
            fields = []
            for _ in range(count):
                fields.append((data[pos], data[pos + 1]))
                pos += 3
            if record_header & 0x20:
                developer_count = data[pos]
                pos += 1
                pos += 3 * developer_count
            definitions[local] = (global_number, fields, order)
        else:
            global_number, fields, order = definitions[local]
            values = {}
            for number, size in fields:
                values[number] = (data[pos : pos + size], order)
                pos += size
            yield global_number, values


def u32(raw):
    payload, order = raw
    return struct.unpack(order + "I", payload)[0]


def u16(raw):
    payload, order = raw
    return struct.unpack(order + "H", payload)[0]


def half_floats(payload):
    return [struct.unpack("<e", payload[i : i + 2])[0] for i in range(0, len(payload), 2)]


def read_minutes(folder):
    """Every minute in the folder as (unix_seconds, kind, hr, movement, activity, offset)."""
    minutes = {}
    for path in sorted(glob.glob(os.path.join(folder, "SLEEP_*.fit"))):
        start = None
        offset = 0
        sample = 60
        index = 0
        for number, values in parse(path):
            if number == SLEEP_DATA_INFO:
                start = u32(values[TIMESTAMP_FIELD]) + GARMIN_EPOCH
                index = 0
                if LOCAL_TIMESTAMP_FIELD in values:
                    offset = u32(values[LOCAL_TIMESTAMP_FIELD]) - u32(values[TIMESTAMP_FIELD])
                if SAMPLE_LENGTH_FIELD in values and 0 < u16(values[SAMPLE_LENGTH_FIELD]) < 0xFFFF:
                    sample = u16(values[SAMPLE_LENGTH_FIELD])
            elif number == SLEEP_DATA_RAW:
                if start is not None:
                    payload = values[0][0]
                    if len(payload) == RAW_BYTES:
                        features = half_floats(payload)
                        minutes[start + sample * index] = (
                            "raw",
                            features[9],
                            features[8],
                            features[0],
                            offset,
                        )
                index += 1
            elif number == SLEEP_STAGE:
                level = values[0][0][0]
                if start is not None:
                    at = u32(values[TIMESTAMP_FIELD]) + GARMIN_EPOCH if TIMESTAMP_FIELD in values else start + sample * index
                    kind = {0: "unmeasurable", 1: "awake"}.get(level)
                    if kind is not None:
                        minutes[at] = (kind, None, None, None, offset)
                index += 1
    return [(at,) + minutes[at] for at in sorted(minutes)]


def build(folder):
    rows = read_minutes(folder)
    if not rows:
        sys.exit(f"no SLEEP_*.fit minutes found under {folder}")
    first = rows[0][0]
    first_local = dt.datetime.fromtimestamp(first + rows[0][5], dt.timezone.utc).date()
    week_start = first_local - dt.timedelta(days=first_local.weekday())
    shift_days = (week_start - FICTIONAL_WEEK).days
    origin = first - shift_days * 86400
    fixture = []
    for at, kind, hr, movement, activity, offset in rows:
        row = {"t": (at - first) // 60, "k": KIND_CODES[kind], "z": offset}
        if kind == "raw":
            row["hr"] = int(round(hr)) if hr is not None and hr == hr else None
            row["mv"] = round(movement, 2) if movement is not None and movement == movement else None
            row["act"] = round(activity, 2) if activity is not None and activity == activity else None
        fixture.append(row)
    return {
        "note": "Derived from real Venu SQ sleep files: values rounded, dates shifted, identifiers dropped.",
        "originEpochSeconds": origin,
        "minuteSeconds": 60,
        "minutes": fixture,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--in", dest="folder", required=True, help="folder of SLEEP_*.fit files")
    parser.add_argument("--out", required=True, help="fixture JSON to write")
    args = parser.parse_args()
    fixture = build(args.folder)
    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as handle:
        json.dump(fixture, handle, separators=(",", ":"))
        handle.write("\n")
    print(f"wrote {len(fixture['minutes'])} minutes to {args.out}")


if __name__ == "__main__":
    main()
