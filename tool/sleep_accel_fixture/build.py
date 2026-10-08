#!/usr/bin/env python3
"""Builds the local sleep evaluation fixture from PhysioNet's sleep-accel dataset.

    python3 -I tool/sleep_accel_fixture/build.py \
        --in tool/sleep_accel_fixture/data/sleep-accel-1.0.0 \
        --out tool/sleep_accel_fixture/out/sleep_accel_minutes.json

The dataset (Walch et al. 2019, "Motion and heart rate from a wrist-worn
wearable and labeled sleep from polysomnography", PhysioNet v1.0.0,
doi:10.13026/hmhs-py35, Open Data Commons Attribution License v1.0) holds,
per subject: motion/<id>_acceleration.txt (seconds from PSG start, x y z in
g, about 50 Hz), heart_rate/<id>_heartrate.txt (seconds, bpm, irregular),
labels/<id>_labeled_sleep.txt (seconds, stage: 0 wake, 1-3 N1-N3, 5 REM,
-1 unscored; 30-second epochs). Both the download and this script's output
are gitignored: the fixture is for local evaluation only, never distributed.

What the script does, so the fixture is what the watch would have recorded:
  - motion is down-sampled to one sample per 200 ms bin (the last sample at
    or before each bin tick), the watch's nominal rate, BEFORE any feature;
  - heart rate goes through the same path as the watch's store: at most one
    sample per ten seconds, contact assumed, heart rate recording on;
  - the time axis is placed on a fictional local clock: PSG second 0 becomes
    22:00 of 2020-01-06 plus the subject's index in days, UTC offset 0, so
    the phone's night window (18:00 to 14:00) applies unchanged;
  - labels are per minute by majority of the two 30-second epochs: W, L
    (N1 and N2), D (N3), R, or - when unscored or absent;
  - features come from features.py, the specification's mirror.

Output: {"note", "source", "licence", "subjects": [{"id", "lines": [...],
"labels": "WWWLLLD..."}]} with one label character per line.
Standard library only.
"""

import argparse
import datetime as dt
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import features  # noqa: E402

BIN_MS = 200
HR_GAP_MS = 10_000
START_LOCAL = dt.datetime(2020, 1, 6, 22, 0, tzinfo=dt.timezone.utc)
LABEL_OF_STAGE = {0: "W", 1: "L", 2: "L", 3: "D", 5: "R"}


def read_rows(path):
    with open(path, encoding="utf-8") as src:
        for line in src:
            parts = line.replace(",", " ").split()
            if parts:
                yield [float(p) for p in parts]


def subject_ids(root):
    ids = []
    for name in sorted(os.listdir(os.path.join(root, "labels"))):
        if name.endswith("_labeled_sleep.txt"):
            ids.append(name[: -len("_labeled_sleep.txt")])
    return ids


def build_subject(root, subject, index):
    t0_ms = int(START_LOCAL.timestamp() * 1000) + index * 86_400_000
    builder = features.MinuteFeatureBuilder(offset_seconds=0, hr_recording=True)

    # Heart rate, thinned like the watch's store, fed before the motion of its minute (close_before guards it).
    heart = []
    last_hr_ms = None
    for t_s, bpm in read_rows(os.path.join(root, "heart_rate", f"{subject}_heartrate.txt")):
        t_ms = t0_ms + int(t_s * 1000)
        if last_hr_ms is not None and t_ms - last_hr_ms < HR_GAP_MS:
            continue
        if not (features.MIN_BPM <= bpm <= features.MAX_BPM):
            continue
        last_hr_ms = t_ms
        heart.append((t_ms, int(round(bpm))))
    hi = 0

    # Motion, down-sampled to the watch's bins.
    current_bin = None
    current_sample = None
    for t_s, ax, ay, az in read_rows(os.path.join(root, "motion", f"{subject}_acceleration.txt")):
        t_ms = t0_ms + int(t_s * 1000)
        b = t_ms // BIN_MS
        if current_bin is None:
            current_bin = b
        if b != current_bin:
            tick = current_bin * BIN_MS
            while hi < len(heart) and heart[hi][0] <= tick:
                builder.on_heart_rate(*heart[hi])
                hi += 1
            builder.close_before(tick)
            builder.on_acceleration(tick, *current_sample)
            current_bin = b
        current_sample = (ax, ay, az)
    if current_sample is not None:
        tick = current_bin * BIN_MS
        while hi < len(heart) and heart[hi][0] <= tick:
            builder.on_heart_rate(*heart[hi])
            hi += 1
        builder.close_before(tick)
        builder.on_acceleration(tick, *current_sample)
    lines = builder.flush()

    # Labels per minute: majority of the epochs inside it, ties to the earlier epoch.
    stages_by_minute = {}
    for t_s, stage in read_rows(os.path.join(root, "labels", f"{subject}_labeled_sleep.txt")):
        t_ms = t0_ms + int(t_s * 1000)
        minute = t_ms - t_ms % features.MINUTE_MS
        stages_by_minute.setdefault(minute, []).append(LABEL_OF_STAGE.get(int(stage), "-"))
    labels = []
    for line in lines:
        minute = int(line.split(" ")[1])
        stages = stages_by_minute.get(minute, [])
        if not stages or all(s == "-" for s in stages):
            labels.append("-")
            continue
        scored = [s for s in stages if s != "-"]
        labels.append(max(scored, key=lambda s: (scored.count(s), -scored.index(s))))
    return {"id": subject, "lines": lines, "labels": "".join(labels)}


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--in", dest="root", required=True, help="the unpacked dataset folder")
    parser.add_argument("--out", required=True, help="the fixture to write (gitignored)")
    parser.add_argument("--subjects", help="comma-separated subject ids; default all")
    args = parser.parse_args(argv)
    ids = args.subjects.split(",") if args.subjects else subject_ids(args.root)
    subjects = []
    for index, subject in enumerate(ids):
        built = build_subject(args.root, subject, index)
        subjects.append(built)
        labelled = sum(1 for c in built["labels"] if c != "-")
        print(f"{subject}: {len(built['lines'])} minutes, {labelled} labelled", file=sys.stderr)
    fixture = {
        "note": "Local evaluation fixture derived from PhysioNet sleep-accel; see tool/sleep_accel_fixture/build.py. Not for distribution.",
        "source": "Walch, O. (2019). Motion and heart rate from a wrist-worn wearable and labeled sleep from polysomnography (version 1.0.0). PhysioNet. https://doi.org/10.13026/hmhs-py35",
        "licence": "Open Data Commons Attribution License v1.0",
        "subjects": subjects,
    }
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as out:
        json.dump(fixture, out, separators=(",", ":"))
        out.write("\n")
    print(f"wrote {args.out}: {len(subjects)} subjects", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
