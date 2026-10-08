#!/usr/bin/env python3
"""The sleep minute features (SM line, version 3), as the watch computes them.

The specification is docs/engineering/sleep-minute-features.md; this module
is its Python mirror and the watch's MinuteAggregator / AccelerationMinuteFeatures
are its Kotlin mirror. The two are held together by feature_vector.json:

    python3 -I tool/sleep_accel_fixture/features.py --self-check
        feeds the vector's raw samples through MinuteFeatureBuilder and fails
        when the lines differ from the vector's expected lines.

    python3 -I tool/sleep_accel_fixture/features.py --make-vector
        regenerates feature_vector.json: a few synthetic minutes covering a
        still watch on a table, a slow drift, a turn in bed, a sparse minute,
        off-body, charging, no heart rate contact and a screen wake-up.

Standard library only. Every integer field is floor(x + 0.5), never round().
"""

import argparse
import json
import math
import os
import random
import sys

MINUTE_MS = 60_000
EPOCH_MS = 5_000
EPOCHS_PER_MINUTE = 12
MIN_EPOCH_SAMPLES = 3
JERK_THRESHOLD_G = 0.051
NOMINAL_SAMPLES = 300
MIN_COUNTS = 3.0
MOVEMENT_PER_COUNT = 0.2
MAX_MOVEMENT = 30.0
SPARSE_SAMPLES = 150

FLAG_CHARGING = 1
FLAG_OFF_BODY = 2
FLAG_SCREEN_ON = 4
FLAG_HR_NO_CONTACT = 8
FLAG_HR_RECORDING = 16
FLAG_SPARSE = 32

VECTOR_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "feature_vector.json")


def rnd(x):
    """floor(x + 0.5): the one rounding both implementations use."""
    return int(math.floor(x + 0.5))


def median(values):
    ordered = sorted(values)
    n = len(ordered)
    if n % 2 == 1:
        return ordered[n // 2]
    return (ordered[n // 2 - 1] + ordered[n // 2]) / 2.0


def mean_sd(values):
    """Two-pass population mean and standard deviation."""
    n = len(values)
    if n == 0:
        return 0.0, 0.0
    mean = sum(values) / n
    var = sum((v - mean) * (v - mean) for v in values) / n
    return mean, math.sqrt(var) if var > 0 else 0.0


class _Minute:
    def __init__(self, start_ms):
        self.start_ms = start_ms
        self.ax = []
        self.ay = []
        self.az = []
        self.epochs = [([], [], []) for _ in range(EPOCHS_PER_MINUTE)]
        self.counts = 0
        self.bpm = []
        self.charging = False
        self.off_body = False
        self.screen_on = False
        self.hr_no_contact = False


class MinuteFeatureBuilder:
    """Feed samples and events in time order per source; close() yields lines.

    Minutes close when a later minute's accelerometer sample arrives or at
    flush(). Events and heart rate for a minute must arrive before it closes,
    as on the watch, where the 90-second close lag guarantees it.
    """

    def __init__(self, offset_seconds, hr_recording):
        self.offset_seconds = offset_seconds
        self.hr_recording = hr_recording
        self.minutes = {}
        self.order = []
        self.worn = True
        self.charging = False
        self.last_mag = None
        self.last_angle = None  # last valid epoch angle of the previous closed minute
        self.closed = []

    def _minute(self, t_ms):
        start = t_ms - t_ms % MINUTE_MS
        minute = self.minutes.get(start)
        if minute is None:
            minute = _Minute(start)
            minute.charging = self.charging
            minute.off_body = not self.worn
            self.minutes[start] = minute
            self.order.append(start)
        return minute

    def on_acceleration(self, t_ms, ax, ay, az):
        minute = self._minute(t_ms)
        minute.ax.append(ax)
        minute.ay.append(ay)
        minute.az.append(az)
        epoch = (t_ms - minute.start_ms) // EPOCH_MS
        if 0 <= epoch < EPOCHS_PER_MINUTE:
            ex, ey, ez = minute.epochs[epoch]
            ex.append(ax)
            ey.append(ay)
            ez.append(az)
        mag = math.sqrt(ax * ax + ay * ay + az * az)
        if self.last_mag is not None and abs(mag - self.last_mag) > JERK_THRESHOLD_G:
            minute.counts += 1
        self.last_mag = mag
        if not self.worn:
            minute.off_body = True
        if self.charging:
            minute.charging = True

    def on_heart_rate(self, t_ms, bpm):
        self._minute(t_ms).bpm.append(bpm)

    def on_worn(self, t_ms, worn):
        self.worn = worn
        if not worn:
            self._minute(t_ms).off_body = True

    def on_charging(self, t_ms, charging):
        self.charging = charging
        if charging:
            self._minute(t_ms).charging = True

    def on_screen_on(self, t_ms):
        self._minute(t_ms).screen_on = True

    def on_hr_contact(self, t_ms, contact):
        if not contact:
            self._minute(t_ms).hr_no_contact = True

    def close_before(self, t_ms):
        """Closes every minute that started before the minute of t_ms."""
        boundary = t_ms - t_ms % MINUTE_MS
        for start in list(self.order):
            if start < boundary:
                self._close(start)

    def flush(self):
        for start in list(self.order):
            self._close(start)
        return self.closed

    def _close(self, start):
        minute = self.minutes.pop(start)
        self.order.remove(start)
        self.closed.append(self._line(minute))

    def _line(self, m):
        n = len(m.ax)
        counts_norm = m.counts * NOMINAL_SAMPLES / n if n > 0 else 0.0
        mv = 0.0 if counts_norm < MIN_COUNTS else min(MAX_MOVEMENT, MOVEMENT_PER_COUNT * counts_norm)
        mx, sx = mean_sd(m.ax)
        my, sy = mean_sd(m.ay)
        mz, sz = mean_sd(m.az)

        angles = []
        for ex, ey, ez in m.epochs:
            if len(ez) < MIN_EPOCH_SAMPLES:
                angles.append(None)
                continue
            medx, medy, medz = median(ex), median(ey), median(ez)
            angles.append(math.atan2(medz, math.sqrt(medx * medx + medy * medy)) * 180.0 / math.pi)
        valid = [a for a in angles if a is not None]
        diffs = []
        prev = self.last_angle
        for a in valid:
            if prev is not None:
                diffs.append(abs(a - prev))
            prev = a
        if valid:
            self.last_angle = valid[-1]

        flags = 0
        if m.charging:
            flags |= FLAG_CHARGING
        if m.off_body:
            flags |= FLAG_OFF_BODY
        if m.screen_on:
            flags |= FLAG_SCREEN_ON
        if m.hr_no_contact:
            flags |= FLAG_HR_NO_CONTACT
        if self.hr_recording:
            flags |= FLAG_HR_RECORDING
        if n < SPARSE_SAMPLES:
            flags |= FLAG_SPARSE

        hn = len(m.bpm)
        if m.charging or m.off_body or (self.hr_recording and hn == 0):
            kind = "U"
        elif m.screen_on:
            kind = "A"
        else:
            kind = "R"

        if hn > 0:
            hmean, hsd = mean_sd(m.bpm)
            bpm, hsd10 = str(rnd(hmean)), str(rnd(hsd * 10))
        else:
            bpm, hsd10 = "-", "-"

        fields = [
            "SM", str(m.start_ms), kind, str(self.offset_seconds), str(flags), str(n), str(rnd(mv * 10)),
            bpm, hsd10, str(hn),
            str(rnd(mx * 1000)), str(rnd(my * 1000)), str(rnd(mz * 1000)),
            str(rnd(sx * 1000)), str(rnd(sy * 1000)), str(rnd(sz * 1000)),
            str(rnd(min(valid))) if valid else "-",
            str(rnd(max(valid))) if valid else "-",
            str(rnd(sum(diffs) / len(diffs) * 10)) if diffs else "-",
        ]
        return " ".join(fields)


def run_vector(vector):
    """Feeds a vector's samples and events through the builder; returns the lines."""
    builder = MinuteFeatureBuilder(vector["offsetSeconds"], vector["hrRecording"])
    events = sorted(vector["events"], key=lambda e: e["t"])
    heart = sorted(vector["heartRate"], key=lambda s: s[0])
    ei = hi = 0
    for t, ax, ay, az in vector["samples"]:
        while ei < len(events) and events[ei]["t"] <= t:
            e = events[ei]
            if e["type"] == "worn":
                builder.on_worn(e["t"], e["value"])
            elif e["type"] == "charging":
                builder.on_charging(e["t"], e["value"])
            elif e["type"] == "screenOn":
                builder.on_screen_on(e["t"])
            elif e["type"] == "hrContact":
                builder.on_hr_contact(e["t"], e["value"])
            ei += 1
        while hi < len(heart) and heart[hi][0] <= t:
            builder.on_heart_rate(heart[hi][0], heart[hi][1])
            hi += 1
        builder.close_before(t)
        builder.on_acceleration(t, ax, ay, az)
    return builder.flush()


def make_vector():
    """Seven synthetic minutes at 5 Hz, in g, with the events that exercise every flag."""
    rng = random.Random(20261008)
    g = 1.0
    t0 = 1_791_500_000_000 - 1_791_500_000_000 % MINUTE_MS  # a minute boundary in 2026
    samples = []
    heart = []
    events = []

    def noise():
        return rng.gauss(0.0, 0.003)

    def still(minute, count=300):
        for i in range(count):
            t = t0 + minute * MINUTE_MS + i * 200
            samples.append([t, round(noise(), 5), round(noise(), 5), round(g + noise(), 5)])

    def heart_rate(minute, bpm, jitter=2):
        for i in range(6):
            t = t0 + minute * MINUTE_MS + i * 10_000 + 3_000
            heart.append([t, bpm + rng.randint(-jitter, jitter)])

    # minute 0: on the table, no heart rate while recording -> U
    still(0)
    # minute 1: worn, slow drift of the arm angle, steady pulse -> R, small zd
    for i in range(300):
        t = t0 + 1 * MINUTE_MS + i * 200
        angle = math.radians(30 + i * 0.05)
        samples.append([t, round(math.cos(angle) + noise(), 5), round(noise(), 5), round(math.sin(angle) + noise(), 5)])
    heart_rate(1, 52)
    # minute 2: a turn in bed in the middle, otherwise still -> R with movement
    for i in range(300):
        t = t0 + 2 * MINUTE_MS + i * 200
        if 150 <= i < 170:
            # Every other reading swings the x axis: the magnitude jumps on each of the twenty.
            swing = 0.5 if i % 2 == 0 else 0.0
            samples.append([t, round(swing + noise(), 5), round(noise(), 5), round(g + noise(), 5)])
        else:
            samples.append([t, round(noise(), 5), round(noise(), 5), round(g + noise(), 5)])
    heart_rate(2, 55)
    # minute 3: the hub dropped most of the minute -> SPARSE
    still(3, count=100)
    heart_rate(3, 54)
    # minute 4: taken off at 30 s, the sensor loses contact at 40 s -> U, OFF_BODY | HR_NO_CONTACT
    still(4)
    heart.append([t0 + 4 * MINUTE_MS + 5_000, 56])
    events.append({"t": t0 + 4 * MINUTE_MS + 30_000, "type": "worn", "value": False})
    events.append({"t": t0 + 4 * MINUTE_MS + 40_000, "type": "hrContact", "value": False})
    # minute 5: on the charger (still off the wrist) -> U, CHARGING | OFF_BODY
    still(5)
    events.append({"t": t0 + 5 * MINUTE_MS + 1_000, "type": "charging", "value": True})
    # minute 6: back on the wrist, charger unplugged, the wearer looks at the watch -> A
    still(6)
    events.append({"t": t0 + 6 * MINUTE_MS, "type": "charging", "value": False})
    events.append({"t": t0 + 6 * MINUTE_MS, "type": "worn", "value": True})
    events.append({"t": t0 + 6 * MINUTE_MS + 20_000, "type": "screenOn", "value": True})
    heart_rate(6, 70, jitter=6)

    samples.sort(key=lambda s: s[0])
    vector = {
        "note": "Synthetic minutes for the SM v3 feature math; see docs/engineering/sleep-minute-features.md.",
        "offsetSeconds": 7200,
        "hrRecording": True,
        "samples": samples,
        "heartRate": heart,
        "events": events,
    }
    vector["expected"] = run_vector(vector)
    return vector


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--self-check", action="store_true", help="recompute the vector's lines and compare")
    parser.add_argument("--make-vector", action="store_true", help="regenerate feature_vector.json")
    args = parser.parse_args(argv)
    if args.make_vector:
        vector = make_vector()
        with open(VECTOR_PATH, "w", encoding="utf-8") as out:
            json.dump(vector, out, separators=(",", ":"))
            out.write("\n")
        print(f"wrote {VECTOR_PATH}: {len(vector['samples'])} samples, {len(vector['expected'])} lines")
        for line in vector["expected"]:
            print("  " + line)
        return 0
    if args.self_check:
        with open(VECTOR_PATH, encoding="utf-8") as src:
            vector = json.load(src)
        lines = run_vector(vector)
        if lines != vector["expected"]:
            print("feature drift:", file=sys.stderr)
            for got, want in zip(lines, vector["expected"]):
                if got != want:
                    print(f"  got  {got}\n  want {want}", file=sys.stderr)
            return 1
        print(f"ok: {len(lines)} lines match")
        return 0
    parser.print_help()
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
