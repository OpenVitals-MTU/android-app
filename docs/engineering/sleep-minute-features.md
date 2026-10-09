# Sleep minute features (SM line, version 3)

The OpenVitals Wear OS app records one row per clock minute as the input of the phone's sleep pipeline. The row is computed twice, from the same specification: in Kotlin on the watch (`wear/.../MinuteAggregator.kt` and `AccelerationMinuteFeatures.kt`) and in Python for the offline evaluation (`tool/sleep_accel_fixture/features.py`). The committed test vector `tool/sleep_accel_fixture/feature_vector.json` holds raw samples and the expected lines; the watch test `SleepMinuteFeatureVectorTest` and `features.py --self-check` both fail when either side drifts from this page.

The pipeline that consumes the rows is described in [architecture.md](architecture.md) under `devices/wearos` and `domain/insights`; the research behind each rule is in [watches.md](../features/watches.md).

## The line

```
SM <epochMillis> <kind> <offsetSeconds> <flags> <n> <mv10> <bpm|-> <hsd10|-> <hn> <mx> <my> <mz> <sx> <sy> <sz> <zmin|-> <zmax|-> <zd10|->
```

Nineteen space-separated fields. Every numeric field is an integer, so both implementations format identically; `-` means "none". A line the receiver cannot parse is skipped.

| Field | Meaning |
|---|---|
| `epochMillis` | Start of the minute: `t - t mod 60000` of the samples in it. |
| `kind` | `R` raw, `A` awake, `U` unmeasurable. See *Kind*. |
| `offsetSeconds` | The watch's UTC offset at the minute, seconds. Decides which night the minute belongs to. |
| `flags` | Bit field. See *Flags*. |
| `n` | Accelerometer samples in the minute. |
| `mv10` | Movement × 10. See *Movement*. |
| `bpm` | Mean heart rate of the minute's samples, or `-` when `hn` is 0. |
| `hsd10` | Population standard deviation of the minute's heart rate samples × 10, or `-` when `hn` is 0. |
| `hn` | Heart rate samples in the minute. |
| `mx my mz` | Per-axis mean acceleration, milli-g, signed. |
| `sx sy sz` | Per-axis population standard deviation of acceleration, milli-g. |
| `zmin zmax` | Lowest and highest 5-second z-angle in the minute, degrees, or `-` when no epoch is valid. |
| `zd10` | Mean absolute change between successive valid 5-second z-angles × 10, or `-` when there is no change to measure. |

## Inputs

- Accelerometer samples `(t, ax, ay, az)` in **g**. The watch divides the sensor's m/s² by 9.80665 before anything else. A sample belongs to the minute `floor(t / 60000)`.
- Heart rate samples `(t, bpm)`: the samples the watch stores, one every ten seconds at most, never a no-contact reading. A sample belongs to the minute of its `t`.
- Events, each stamped: `worn(bool)` from the off-body sensor (sticky until the next event; initially worn), `charging(bool)` (sticky; initially from the battery state), `screenOn` (instantaneous), `hrContact(false)` when the heart rate sensor reported no contact, an unreliable reading or 0 bpm.
- `hrRecording`: whether heart rate is being recorded at all while the minute closes.

## Rounding

Every integer field is `floor(x + 0.5)` of the real value: `Math.round(x: Double)` in Kotlin, `math.floor(x + 0.5)` in Python. Never a language's default `round`, which ties to even in Python.

## Movement

The actigraphy count on the scale the sleep estimator was fitted on (zero for a still minute, around ten for a restless one, from the Garmin nights in `app/src/test/resources/fit/sleep/`).

1. `mag_i = sqrt(ax² + ay² + az²)` for every sample, in g.
2. `counts` = the number of samples in the minute whose `|mag_i - mag_{i-1}| > 0.051` (g; the previous sample may belong to the previous minute, so the first sample of a minute is compared across the boundary; the first sample ever is not counted).
3. `countsNorm = counts × 300 / n` (the count at the nominal five readings a second; the Watch8 delivers 6.25 Hz when 5 Hz is asked). 0 when `n` is 0.
4. `mv = 0` when `countsNorm < 3`; otherwise `min(30, 0.2 × countsNorm)`.
5. `mv10 = round(mv × 10)`.

## Per-axis mean and standard deviation

Over the minute's samples, in g, two-pass (mean first, then the mean of squared deviations), population form (divide by `n`). `mx = round(mean × 1000)`, `sx = round(sd × 1000)`; the same for y and z. All zero when `n` is 0.

These let the phone pool a window's variance exactly: `var_window = mean(sd_i² + mean_i²) - mean(mean_i)²` over the minutes, weighted by `n`.

## z-angle

The angle of the watch's z axis against the horizontal, the HDCZA input (van Hees 2018).

1. The minute is cut into twelve epochs of five seconds: epoch `e` holds the samples with `floor((t - epochMillis) / 5000) = e`.
2. An epoch is valid when it holds at least three samples. Its angle is `atan2(median(az), sqrt(median(ax)² + median(ay)²)) × 180 / π`, degrees, from the per-axis medians (sorted; the middle value, or the mean of the two middle values).
3. `zmin`, `zmax` = `round` of the lowest and highest valid angle. `-` when no epoch is valid.
4. `zd` = the mean of `|angle_e - angle_prev|` over every valid epoch that has a previous valid epoch, where the previous one is the nearest earlier valid epoch in the same minute, or for the first valid epoch of the minute the last valid epoch of the previous minute (carried; nothing before the first minute ever). `-` when there is no pair. `zd10 = round(zd × 10)`.

## Heart rate

`hn` = samples in the minute. `bpm = round(mean)`, `hsd10 = round(populationSd × 10)`, both `-` when `hn` is 0.

## Flags

| Bit | Value | Name | Set when |
|---|---|---|---|
| 0 | 1 | `CHARGING` | charging at any point of the minute |
| 1 | 2 | `OFF_BODY` | not worn at any point of the minute |
| 2 | 4 | `SCREEN_ON` | the screen became interactive during the minute |
| 3 | 8 | `HR_NO_CONTACT` | the heart rate sensor reported no contact, unreliable, or 0 bpm during the minute |
| 4 | 16 | `HR_RECORDING` | heart rate was being recorded when the minute closed |
| 5 | 32 | `SPARSE` | `n < 150`, half the nominal budget: the sensor hub dropped part of the minute |

## Kind

Instantaneous evidence only; windowed rules belong to the phone.

- `U` when `CHARGING` or `OFF_BODY` is set, or when `HR_RECORDING` is set and `hn` is 0.
- otherwise `A` when `SCREEN_ON` is set.
- otherwise `R`.

## Closing a minute

The watch closes a minute 90 seconds after it started; by then every batch that belongs to it has arrived (batches are at most 60 seconds late). The Python implementation closes a minute when it sees the first sample of a later minute, or at the end of the input.

## What the watch actually delivers

The Watch8's accelerometer cannot wake the watch and buffers 300 readings, 48 seconds at the 6.25 Hz the hub delivers; readings older than that are lost when the buffer wraps. The heart rate sensor can wake the watch, so its report latency sets how often the watch wakes at night and is kept at 40 seconds so the accelerometer buffer is drained before it wraps. A minute still ends up sparse now and then (`SPARSE` flag, `n` under 150), with few or no valid five-second epochs; the phone's window detector carries the last known angle change across such minutes rather than reading them as movement. The first recorded night, before the latency change, kept a median of 30 readings a minute.
