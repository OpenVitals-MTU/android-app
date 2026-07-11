/// Averages, minima and maxima over a sample list — with the empty case **stated
/// rather than accidental**.
///
/// This exists because the app had eleven hand-rolled copies of "average a list",
/// and they disagreed about the one case that matters. Three different contracts
/// were live at once:
///
/// - **NaN** (`0/0`, from an unguarded `fold`). Reachable nowhere, as it turned
///   out — but if it ever had been, `double.nan.round()` throws `UnsupportedError`,
///   and NaN compares false against everything, so it would have slipped past
///   `<= 0` guards and rendered a literal "NaN" in the UI.
/// - **0**, which is a real measurement. Right for a chart bar (a month with no
///   sleep logged is a zero-height bar), wrong for a reading (a day with no
///   resting heart rate is not 0 bpm).
/// - **null**, meaning "no samples, so no answer".
///
/// Both of the last two are legitimate, and they are **not interchangeable** —
/// which is why this file exports both rather than picking a winner. Choose by
/// what the absence *means* at the call site:
///
/// - [average] when "no samples" is **unknown**. The caller must then say what to
///   do about it, which is the point.
/// - [averageOrZero] when zero is a **real value**, not a stand-in for missing.
///
/// [minOf] and [maxOf] return null on empty rather than throwing, which is what
/// their hand-rolled `reduce` ancestors did.
library;

/// The mean of [values], or null when there are none.
///
/// Null means "no samples" — it is never zero. A caller that wants zero must ask
/// for it (`average(xs) ?? 0`, or [averageOrZero]).
double? average(Iterable<num> values) {
  var sum = 0.0;
  var count = 0;
  for (final value in values) {
    sum += value;
    count++;
  }
  return count == 0 ? null : sum / count;
}

/// The mean of [values], or 0.0 when there are none.
///
/// Only for callers where zero is a genuine value — a chart bar for a month with
/// nothing logged, say. If zero would be indistinguishable from missing data, use
/// [average] instead and handle the null.
double averageOrZero(Iterable<num> values) => average(values) ?? 0.0;

/// The smallest of [values], or null when there are none.
double? minOf(Iterable<num> values) {
  double? smallest;
  for (final value in values) {
    if (smallest == null || value < smallest) smallest = value.toDouble();
  }
  return smallest;
}

/// The largest of [values], or null when there are none.
double? maxOf(Iterable<num> values) {
  double? largest;
  for (final value in values) {
    if (largest == null || value > largest) largest = value.toDouble();
  }
  return largest;
}
