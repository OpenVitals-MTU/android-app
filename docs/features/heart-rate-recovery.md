# Heart Rate Recovery

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `domain/insights/HeartRateRecovery.kt`, `features/recovery`, `features/activity`, `features/manualentry/activity/recording`.
> **Navigation:** `heart/recovery`, a card on an activity's detail screen, and a surface on the heart screen.
> **Related:** [Feature map](feature-map.md), [Heart and vitals](heart-and-vitals.md), [Activity recording](activity-recording.md).

How far your heart rate falls in the minute after hard effort stops is a
well-studied marker of cardiovascular fitness. OpenVitals measures it from the
heart rate samples attached to a workout, and is deliberately conservative: a
number that would mislead is withheld rather than shown.

## Why The Test Is Deliberate

The measurement needs an abrupt, recorded stop. Nothing in an ordinary workout
says where the effort ended, so measuring from the session end instead gives a
recovery computed from a heart rate that had already been falling for minutes —
sometimes a negative one.

That is why OpenVitals offers a [guided test](activity-recording.md#guided-heart-rate-recovery-test)
during recording. It needs a connected heart rate sensor, because the
measurement wants a reading every few seconds through the minutes after you
stop; data that arrives from a watch after the fact cannot drive it.

The guided test writes the moment the effort stopped into the saved session, and
that mark is what the reading is measured from later.

## The Marks

Readings are taken at 30 seconds and at 1, 2, 3, 4 and 5 minutes after the stop.

The **one-minute drop is the headline**: it is the mark with a body of normative
literature behind it, and the one comparable across monitors. There is
deliberately no ten-second mark — optical sensors smooth over several seconds, so
a figure that early is unreliable on the monitors people actually wear.

A sample counts as a mark only if it lands within 3 seconds of the 30-second mark
or 5 seconds of the others. Heart rate falls fast right after cessation, so a
looser window would cost several bpm of a number that is only tens of bpm wide. A
mark with no sample near enough is reported as missing; it is never invented.

## Quality

Every reading carries a verdict, so a weak measurement is never presented as a
strong one:

- **Clean** — near-maximal effort, a peak taken close to the stop, and at least
  the one-minute mark present.
- **Approximate** — usable, but something was estimated or coarse: the headline
  mark is missing, the peak rests on a single sample, or no maximum heart rate
  could be resolved.
- **Not comparable** — a real drop, from an effort too easy to compare against
  your other readings.
- **Invalid** — the number would mislead, so it is not charted. Either you eased
  off before the stop, so the "drop" measures your cool-down, or the heart rate
  did not fall at all, which means the recording stopped before you did.
- **No data** — there was nothing to measure.

Only readings good enough to compare are charted; the rest are shown with their
reason.

## Effort And Maximum Heart Rate

Judging whether effort was near-maximal needs a maximum heart rate. OpenVitals
resolves one in this order:

1. The maximum you entered in [Body profile](settings-and-preferences.md).
2. The highest rate actually observed, when it is plausible against your resting
   rate.
3. An age estimate, using Tanaka (208 − 0.7 × age), which is more accurate across
   ages than the older 220 − age rule.

An estimated maximum widens the band that counts as near-maximal, because the
estimate itself is uncertain. If none of the three can be resolved, the reading
is marked approximate rather than being judged on a guess.

## Notes

- Duplicate samples on the same instant — a strap and a watch both recording —
  keep the higher reading. That is the conservative choice in both directions: a
  higher peak is harder to clear the effort gate with, and a higher recovery
  reading means a smaller reported drop.
- The reading is an OpenVitals wellness estimate computed on your device. It is
  not a Health Connect record and not medical advice.
