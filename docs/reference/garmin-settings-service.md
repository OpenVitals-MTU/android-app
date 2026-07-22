# Garmin settings service (protobuf) — what the watch actually does

Everything here was read off a real vívoactive 5 (firmware 17.05), not inferred
from Gadgetbridge's schema. Where the two disagreed, the watch won every time.

Companion to `docs/reference/garmin-fit-files.md`, which covers the file sync.

## Why this exists

Alarms. Gadgetbridge returns an alarm slot count of **zero** for a watch with
`REALTIME_SETTINGS`, meaning alarms are not set by uploading a FIT settings file
— they are a screen inside the watch's own settings tree. This watch has that
capability, so the outbound FIT upload path is unnecessary and the settings
service is the whole job.

## Transport

Protobuf rides GFDI messages **5043** (request) and **5044** (response), with the
envelope:

```
[u16 requestId][u32 dataOffset][u32 totalLength][u32 chunkLength][bytes]
```

Four things about this cost a debugging round each:

1. **Replies do not always echo the request id.** Find my watch answers under
   ours; the settings service answers under an id *of its own*, so a settings
   reply is indistinguishable from traffic the watch started. Settings replies
   must be correlated by CONTENT — and by the specific response field, because
   the watch emits several settings messages unprompted and the first to arrive
   was a five-byte one on field 7 answering nothing.
2. **A chunk acknowledgement echoes the offset the chunk DECLARED**, not the
   next one expected. Sending `dataOffset + chunkLength` left the watch resending
   chunk zero forever. See `buildProtobufChunkAck`.
3. **Chunks must be held by offset, not appended.** The watch retransmits
   anything it thinks went unacknowledged, and appending made three copies of
   chunk zero look like 487 → 974 → 1461 bytes of progress against a 1017-byte
   message. It "parsed" only because the decode never read past the first copy —
   which is how the stall stayed hidden.
4. **Do not await both reply sources.** Racing them mapped the tree in fourteen
   seconds; `Future.wait` made every screen cost the full timeout, because the
   id-based future never completes on this watch.

The watch takes **longer than ten seconds** to build a screen on a cold request,
so settings use a 30-second timeout (`GarminSettingsService.replyTimeout`).

## The tree, as measured

Root is **36352**. Walking it (24 screens, two levels, ~14s):

| id | title | notes |
|----|-------|-------|
| 204 | Clocks | → Alarms |
| 920 | Glances | 1160 B |
| 972 | Controls | |
| 1123 | Activities | |
| 1103 | Apps | 1438 B |
| 512 | Notifications & Alerts | |
| 822 | Watch Sensors | |
| 12 | Accessories | |
| 493 | Music | |
| 118 | Audio Prompts | 1489 B |
| 797 | User Profile | |
| 608 | Safety & Tracking | |
| 58 | Activity Tracking | |
| 688 | System | 2006 B |
| 68 | **Alarms** | under Clocks |
| 738 | Time | under Clocks |
| 25 / 327705 / 851993 / 917529 / 2949145 | Run / Bike / Strength / Cardio / Meditation | per-activity, ids in the millions |

Target types seen on entries: **0** subscreen, **6** opens something ON the watch
(Garmin Pay), **7** hidden (Stopwatch, Timer — nothing to configure from a
phone), **9** subscreen with options.

## The Alarms screen (68)

```
screenId 68  "Alarms"
  entry id=0  type=5  title "7:00 am" (twice: label and summary)
              target{type:9, subscreen:64}     ← a real alarm
  entry id=1  type=4  target{type:0, subscreen:0}   ← empty slot
  entry id=2… same
```

A **list of slots**. A populated one carries the time already rendered by the
watch — no format to compose or parse — and opens its own screen. An empty slot
is a subscreen pointing at screen **zero**, which is how "nothing here" is
represented and would look like a parse bug if guessed at.

So alarms need no alarm-specific protocol. They are screens in the tree like
anything else, and a generic browser reaches them.

## Next steps

1. **Fetch screen 64** — one alarm's own screen. Expect time, repeat, label and
   sound as entries with number-picker / list-picker targets
   (`TargetNumberPicker`, `TargetOptions` in `gdi_settings_service.proto`). This
   is the last shape needed before an editor can be designed.
2. **Screen STATE, not just definition.** `ScreenStateRequest` is implemented and
   its reply was seen (168 B for the root), but nothing reads it yet. Definition
   is the layout; state is the current value behind each entry. Both are needed
   to render a control that shows what it is set to.
3. **`ChangeRequest` (field 5)** is the write path and is NOT implemented.
   Nothing has ever been written to the watch — the whole stack is read-only so
   far, and the first write is the first chance to corrupt a user's settings.
   Worth a careful look at the response status before shipping.
4. **The watch retransmits COMPLETE messages too** — `#410` arrived three times
   identically. Dedup absorbs it, so it is harmless today, but it means the
   acknowledgement for complete protobuf messages is not fully satisfying the
   watch either. Worth fixing before a browser walks many screens and multiplies
   the redundant traffic.
5. **The link drops under sustained use** (`[GARMIN-BLE] link dropped`,
   `packet for unknown handle`). It did not recur once the walk got fast, so it
   may have been a symptom of the stall rather than its own fault — but a
   browser that holds the link while a user reads will find out.

## Diagnostic traps met along the way

- **logcat truncates a long line.** The capability list printed 51 of 63 entries
  and cut off mid-name, with `REALTIME_SETTINGS` (bit 92) in the truncated tail —
  so the log said the opposite of the truth. Read the persisted value instead:
  `adb shell run-as tech.mmarca.openvitals.debug cat
  shared_prefs/FlutterSharedPreferences.xml`.
- The same truncation hides the tail of any long hex dump.
- A test whose fake `send` answers every frame will recurse once chunk
  acknowledgements start going out through it. Answer requests only.

Reached by long-pressing the disabled **Alarms** action on the device view, in
debug builds only.
