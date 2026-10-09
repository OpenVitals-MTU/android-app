#!/usr/bin/env python3
"""Reads a raw PPG log the watch app wrote (PpgRawLogger) and describes its columns.

    python3 -I tool/ppg_raw/inspect.py ppg_raw_20261009-0030.csv [--window 60]

For each of the sixteen value columns: how many distinct values, min, max,
mean, standard deviation, and the lag of the strongest autocorrelation peak
between 0.4 and 2 seconds over [--window] seconds of the middle of the file,
turned into beats per minute. A waveform column shows a clear peak near the
true pulse; a status or counter column shows none. Standard library only.
"""

import argparse
import csv
import math
import statistics
import sys


def autocorrelation_peak(values, rate_hz, low_s=0.4, high_s=2.0):
    n = len(values)
    if n < int(high_s * rate_hz) * 2:
        return None
    mean = sum(values) / n
    centred = [v - mean for v in values]
    var = sum(v * v for v in centred)
    if var == 0:
        return None
    best_lag, best = None, -1.0
    for lag in range(int(low_s * rate_hz), int(high_s * rate_hz) + 1):
        acc = sum(centred[i] * centred[i + lag] for i in range(n - lag)) / var
        if acc > best:
            best, best_lag = acc, lag
    return best_lag, best


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("csv")
    parser.add_argument("--window", type=float, default=60.0, help="seconds analysed for the pulse, from the middle")
    args = parser.parse_args(argv)
    with open(args.csv, newline="", encoding="utf-8") as src:
        reader = csv.reader(src)
        header = next(reader)
        rows = [r for r in reader if len(r) == len(header)]
    if not rows:
        print("no rows", file=sys.stderr)
        return 1
    nanos = [int(r[0]) for r in rows]
    span_s = (nanos[-1] - nanos[0]) / 1e9
    rate = (len(rows) - 1) / span_s if span_s > 0 else 0.0
    print(f"{len(rows)} events over {span_s:.1f} s: {rate:.2f} Hz; accuracy values {sorted(set(r[2] for r in rows))}")
    value_columns = [i for i, name in enumerate(header) if name.startswith("v")]
    mid = len(rows) // 2
    half = int(args.window * rate / 2) if rate else 0
    window = rows[max(0, mid - half):mid + half]
    print(f"{'col':>4} {'distinct':>8} {'min':>12} {'max':>12} {'mean':>12} {'sd':>10} {'peak lag':>9} {'bpm':>6} {'corr':>6}")
    for col in value_columns:
        values = [float(r[col]) for r in rows if r[col] != ""]
        if not values:
            print(f"{header[col]:>4} {'empty':>8}")
            continue
        sd = statistics.pstdev(values) if len(values) > 1 else 0.0
        peak = autocorrelation_peak([float(r[col]) for r in window if r[col] != ""], rate) if rate else None
        lag, corr = peak if peak else (None, None)
        bpm = 60.0 * rate / lag if lag else None
        print(
            f"{header[col]:>4} {len(set(values)):>8} {min(values):>12.4g} {max(values):>12.4g} "
            f"{statistics.fmean(values):>12.4g} {sd:>10.4g} "
            f"{(lag if lag is not None else '-'):>9} {(f'{bpm:.0f}' if bpm else '-'):>6} {(f'{corr:.2f}' if corr is not None else '-'):>6}"
        )
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
