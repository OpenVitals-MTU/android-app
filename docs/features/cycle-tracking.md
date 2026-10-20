# Cycle Tracking

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/cycle`, `features/manualentry/cycle`, `features/cycle/reminders`, `domain/cycle`, `data/repository/CycleRepository.kt`, `data/local/cycle`.
> **Navigation:** `Screen.Metric`, `Screen.CycleEntry`, `Screen.SettingsCycle`, widget `CYCLE`, Health Connect permission category.
> **Related:** [Feature map](feature-map.md), [Reminders](reminders.md), [Home widgets](home-widgets.md), [Settings](settings-and-preferences.md), [Privacy](../app/privacy.md), [Design and status](../proposals/cycle-tracking-design.md).

Cycle tracking reads Health Connect cycle records, adds a local day log for what Health Connect cannot hold, and shows recorded facts and computed estimates side by side.

## Two kinds of information

Every screen labels each item as one of two things:

- **Recorded**: something the user or another app wrote. Flow, spotting, symptoms, notes, temperatures, cycle day counted from a recorded period start.
- **Estimated**: something computed from recorded history. The next period window, the current phase when it is not a bleeding day, the ovulation transition.

The app never turns an estimate into a diagnosis or a fertility claim.

## Supported data

From Health Connect, when the permission is granted:

- Menstruation flow entries and period intervals.
- Intermenstrual bleeding (spotting).
- Ovulation tests.
- Cervical mucus (appearance and amount).
- Basal body temperature.
- Sexual activity entries.

From the local journal (Room, `cycle_journal_entries`), because Health Connect has no record type for them:

- A "no bleeding" mark that separates "checked, none" from "not logged".
- Pain, mood, energy, and desire on a 1 to 5 scale.
- Symptoms from a fixed catalog (cramps, headache, bloating, mood changes, and so on).
- Private notes.
- Pregnancy test result, temperature disturbance flags, cervical sensation, and the cervix as felt: opening, firmness, position.

## The day log

"+ Log" → Cycle, the cycle screen's Log action and the home widget open a chooser: one card each for bleeding, how you feel, a pregnancy test, an ovulation test, sexual activity, basal body temperature and cervical mucus. A card opens the day log on that section alone; "Log everything for the day" opens the full log. The calendar, the entry list's pencil and the Today card of a day that already has entries open the full log for that date: bleeding first (not recorded, none, spotting, light, medium, heavy), then the four scales, then symptoms and notes behind a disclosure, then body observations (temperature with time and disturbances, cervical sensation, mucus, cervix, ovulation test, sexual activity). Blank means not logged.

Saving reconciles each Health Connect record kind separately: a record OpenVitals owns is updated or deleted, a new value is inserted, and an unchanged value is left alone. Records written by other apps are shown as read-only context and are never modified. Journal fields go to the local table. Leaving with unsaved changes asks first.

Menstruation period intervals are derived, not logged. After a flow change the app reconciles consecutive bleeding days (one-day gap tolerated) into period records it owns.

## Cycle screen

The screen is a metric detail scaffold fixed to the month: the arrows and the date picker move the calendar, and the sections can be arranged from the app bar like on any other metric screen. In the default order it shows:

- **Hero**: recorded cycle day and the date it counts from, or an invitation to log the first period.
- **Phase**: menstrual, follicular, ovulatory transition, luteal, or a transition window with the reason it cannot be placed.
- **Calendar**: recorded flow, spotting, the estimated window outline, cycle-start markers and observation dots. Tapping a day opens its day log.
- **Today**: what is logged today. Hidden on an empty day, which is logged from the Log action.
- **Pill**: which day of the pack it is and whether today's pill is taken, when the pill is tracked.
- **Estimate**: the next period window with a bar showing where today sits, or why there is no estimate yet.
- **Tip or fact**: one phase tip picked by date, declared contexts, and recent symptoms, or a cycle fact with its source. The choice is stable within a day.
- **Stats**: count, mean, and range of recorded cycles.
- **Setup**: a link to cycle settings when no context or age band is set.

Below that: the observation list, the cycle history bars against a 21 to 35 day reference band with a per-cycle detail sheet, newest first, twelve at a time with a Load 10 more button, over the whole recorded history (until 2.14.2 the history stopped at eighteen months, about nineteen cycles), the thermal chart with a coverline when a shift is confirmed, symptom patterns per phase, and the [nightly vitals](#nightly-vitals) card.

## Nightly vitals

The card "Night pulse, HRV and breathing across your cycle" draws the current cycle's overnight resting pulse, HRV (RMSSD) and breathing rate by cycle day. It is **information only**: it names no phase, no fertile day, no ovulation and no contraceptive effect, and nothing on it feeds the estimate, the phase or any other card. It shows only with a current cycle to count days from.

- **Reuse.** `LoadCycleNightlyVitalsUseCase` reads sleep sessions through `SleepRepository`, resting-pulse samples and HRV samples through `HeartRepository`, and breathing rate through `VitalsRepository`, the same seams the heart, sleep and vitals screens use. It runs after the calendar is shown, so the Health Connect reads never hold the screen. A failed read costs only its signal: a permission failure marks the signal "not allowed", any other failure leaves it empty, and cancellation is never swallowed. The reads need sleep, resting heart rate, HRV and respiratory rate permissions, an optional group (`cycleNightlyPermissions` in `HealthConnectManager`) that is **not** part of the cycle feature's permissions: the screen works without it, and while any of them is missing the card shows one button, "Allow reading overnight values", through `rememberHealthConnectPermissionLauncher`, then reads again. Month navigation does not read the nights again; a refresh, a saved entry or a grant does.
- **One value per night.** `NightlyVitalsCalculator` (`domain/cycle`) cuts the samples by the sleep session. The session's wake-up day is its cycle day, counted from the cycle start as day 1; of several sessions ending on one day the longest is the night, the others are naps. Pieces of one night that a source splits (less than 3 hours apart) are one night, so the readings in all pieces count; a short waking between two pieces lies inside it. The night's value is the **median** of the readings inside the night. Studies aggregate differently (the HRV review cites Oura ring studies using 5-minute intervals overnight, a WHOOP study using the last slow-wave sleep cycle and another using all non-wake time of the main sleep; the WHOOP cohort of Gonzalez et al. weights the end of sleep and slow-wave windows), so absolute values do not compare with those studies.
- **Sources are never mixed.** Devices disagree on the absolute level, so a signal uses one source for the whole cycle: the one covering the most nights, then the most readings, then the name. Nights only another source recorded are left out and the card says which sources were not mixed in.
- **Missing signals are named, never filled in.** If the device does not record a signal or the permission is not granted, the card shows the others and says in plain words which one is missing and why that can be (many watches do not record it). Without any sleep session it says that nights cannot be placed. Resting pulse is the one signal that falls back: many devices write one `RestingHeartRateRecord` a day, stamped outside the sleep window, so a night with no reading inside it takes that source's record on the wake-up day. The card labels it ("Hollow points are the device's daily value, not a reading from the night") and draws it hollow; it is never presented as a night value. The other signals have no such fallback.
- **Own colour, own axis, one chip each.** Each signal has a colour and its own axis (the span of its own values); the first two visible signals label the left and right axes and every visible signal's range is spelled out under the chart. A chip per signal shows or hides its line; the choice survives rotation (`rememberSaveable`).
- **Why it is shown.** A "Why this is shown" toggle opens the evidence, each statement with its source and a link (`NightlyVitalsEvidence`, `CycleSources`), how the night value is made, and the caveats: sleep, alcohol and hormonal contraception move these lines, and the gain from combining several wearable signals was small in the cited review (pooled accuracy 0.89 versus 0.87, overlapping intervals). The card does not discuss fertile-window detection. See [the sources table](../proposals/cycle-tracking-design.md#sources-for-nightly-vitals).

## Estimator

Cycle starts come from bleeding segments across all Health Connect sources. The estimator uses start-to-start intervals: the last six within 15 to 90 days, resetting on a gap over 90 days, skipping excluded cycles. The mean is shrunk toward an age-band prior (weight 2, or 0.5 when the history shows persistent 7-day swings), the spread is 1.96 standard deviations with a floor of 3 and a cap of 22 days, and a declared timing context (PCOS, perimenopause, thyroid) raises the variance floor. The window never starts before the day after the last recorded flow day.

Two recorded starts give a first estimate. With fewer, the screen says it is waiting for history. When intervals are out of range, the screen says so instead of guessing.

### From a recorded marker

Once the current cycle holds a recorded sign that ovulation has passed, the estimate counts from that sign instead of from the last start. The days from a marker to the next start vary less within a woman than whole cycles do, so the window narrows. Three markers count, each read back from the record:

- **LH test**: the first positive test of the cycle. A "high" result does not count.
- **Temperature**: the first high day of a confirmed thermal shift (the rule below).
- **Mucus peak**: the last day of best-quality mucus, once three observed days of lesser quality follow it. Best quality is egg-white mucus, or a wet or slippery sensation in the day log. Watery mucus does not count. A day without a mucus entry leaves the peak unknown.

Each marker has its own interval to the next start, learned from the user's earlier cycles that hold the same marker (the last six, 7 to 20 days) and shrunk toward a published prior with weight 2: LH 13.8 ± 1.86 days, temperature 11.0 ± 2.07, mucus 13.0 ± 2.5. The window is 1.96 standard deviations, at least 2 and at most 22 days, and never starts inside recorded flow. A declared timing context floors the variance at the prior.

When an LH test or a mucus peak falls zero to three days before the first high day, the shift is confirmed and the card says another sign agrees. The LH test anchors when there is one, the temperature otherwise. When a shift and another marker are recorded and none agrees, the estimate keeps the calendar and the card says the signs disagree. An excluded current cycle always keeps the calendar.

The card names what the range counts from. A marker estimate is still an estimate of the next period: it never marks fertile or infertile days.

## Phase

Bleeding days are menstrual. Days 2 to 7 without a bleeding entry are indeterminate. The ovulation anchor is the estimated window's central day minus 13, plus or minus 2. The ovulatory label appears only on the central day and only with at least six intervals and at most 7 days of variability. From two days before the central day the phase is the next-period window. After the window ends the phase is expired until a new start is recorded.

## Thermal shift

Over the current cycle's basal temperatures: six lows followed by three highs, the third at least 0.2 °C above the highest low, no gap over three days, at most a two-day transition. Disturbed readings are shown but skipped. A confirmed shift is retrospective information, never a prediction.

### Skin temperature information card

Under the thermal chart, a card explains what night-time skin temperature from a watch or ring can and cannot say. The estimates and the thermal shift do not use it: the card is background information. It says the rise after ovulation is read afterwards from the whole cycle, never in advance, and says the app makes no fertility, contraception or medical claim.

A toggle shows the numbers and the papers (Wang 2025, Thigpen 2025, Maijala 2019, Shilaih 2018, Zhu 2021, and in the hormonal paragraph Dittmar 2024, Salkeld 2001, Charkoudian 2014, Rivera 1999, each with its DOI link). Two notes are always visible when they apply: a hormonal method (any kind, set in Settings, also in the 60 days after its stop date; `HormonalContraception.isInfluencing(today)`, carried to the card as `CycleTodayDisplay.hormonalInfluencing`) and a declared PCOS context (no study of temperature-based ovulation detection in PCOS was found). The hormonal note (shown for a set method or one stopped within 60 days) explains in plain words that hormonal methods act on the same temperature rhythm and that many of them stop ovulation, so the pattern the card describes may be missing; that the large wrist study left out hormonal methods and the two months after, so its results say little about the reader; and that it is information, not advice. A copper IUD, condoms or a diaphragm use no hormones and show no note. With the toggle open, a further paragraph quotes two pill studies (Dittmar et al. 2024, wrist temperature rhythm of pill users; Salkeld et al. 2001, morning oral temperature in active-pill and pill-free weeks), a review of reproductive hormones and thermoregulation (Charkoudian & Stachenfeld 2014: estrogens generally lower, progestins generally raise body temperature) and a review of how hormonal methods work (Rivera et al. 1999: ovulation inhibition and changes in cervical mucus are among the main mechanisms). It names the ring study (Thigpen), the finger-ring study (Maijala) and the 2021 wrist study (Zhu) as also leaving hormone users out. It never blocks anything and makes no fertility or contraception claim. Every number on the card was read in the paper's abstract or full text; see the design document, *Skin temperature sources*.

## Cycle management

- **Add past period**: writes a light-flow record at noon on a chosen day up to 92 days back, so the estimator has history sooner.
- **Exclude a cycle**: keeps the cycle in history but out of the estimate, with an optional reason (illness, treatment, contraception change, stress or travel, other). Stored in `cycle_exclusions`, keyed by a date inside the cycle.
- **Delete a journal entry**: from the observation list. Health Connect records OpenVitals owns are deleted the same way.

## Settings

Settings → Cycle holds:

- **Tracking contexts**: PMS, PMDD, endometriosis (add symptom suggestions to the day log); PCOS, perimenopause, thyroid (widen the estimate).
- **Age band**: picks the variability prior. A birth year in the body profile sets it; the picker is the fallback when there is none.
- **Reminders**: see [Reminders](reminders.md).
- **Hormonal contraception**: a declared context, none by default: pill, hormonal IUD (hormone coil), implant, ring or patch, injection or another hormonal method, with an optional day it was stopped. Only methods that use hormones are listed; copper IUDs, condoms and diaphragms do not use hormones, so they are not listed here. The card says in one plain line that hormonal methods can change the cycle and the temperature pattern, with no numbers and no claim about contraception or fertility. An enabled pill scheme means the pill: the effective method is derived as pill whenever the scheme is enabled (also for schemes saved before this field existed), the card says so, and the picker returns when the scheme is turned off. A stop date in the future counts as still running, and the card says so. `HormonalContraception.isInfluencing(today)` (in `:domain`, `domain.cycle`, part of `CycleTrackingProfile`) is true while a hormonal kind is set with no stop date and for 60 days after the stop date. That window follows the exclusion criterion of Wang 2025 (Hum Reprod, 10.1093/humrep/deaf005), not a measured washout. The skin-temperature card reads it.
- **Contraceptive pill**: days taking and days pausing (21 and 7 by default), the first day of a pack, and a daily reminder at a chosen time on taking days, quiet once today is marked as taken. The scheme lives in the cycle preferences; the taken days in `pill_intakes`.
- **Delete cycle journal**: wipes the journal, the exclusions and every cycle setting on the device after a confirmation. Health Connect records stay.

All of it is local. Nothing here is written to Health Connect.

## Report and backup

The health report offers a Cycle tracking section: the cycle-day chart, the length statistics over completed cycles, bleeding and pain counts, a table of the cycles in the range, the symptom counts on and off bleeding days, and the notes. See [Health report export](health-report-export.md).

A history kept in the drip cycle tracker comes in through the CSV importer, which recognises drip's export and writes its bleeding, temperatures, mucus and sex to Health Connect and its pain, mood, cervix, desire and notes to the journal; see [CSV import](csv-import.md#drip-export).

Settings → Cycle exports the journal as a JSON file (day logs, excluded cycles, contexts, age band and hormonal method) and imports one back. An import merges: a day both sides hold keeps the newer edit, exclusions are added, and each of the contexts, age band and hormonal method fills in a phone that has not declared it. Health Connect records are not in the file; Health Connect has its own export.

## Dashboard tile and widget

The dashboard cycle tile shows the recorded cycle day with the phase, or the estimate when the phase is unknown; it falls back to the day's counts when there is no current cycle. See [Home widgets](home-widgets.md) for the home-screen widget, which shows the cycle day, the estimated range, and whether today is logged, with a button that hides them.

## Permissions

Cycle permissions are managed as their own Health Connect category. The screen and widget show a permission state when the category is missing. Writing a day log needs the write permission for each record kind it touches; kinds without it are skipped and the screen says so.

## Privacy

Cycle data stays in Health Connect and in the app's local database on the device. The journal, the exclusions, the declared contexts, age band and hormonal method, the pill scheme and the taken days move with the cycle category of phone-to-phone sync, like Health Connect records do; see [Sync with another phone](device-sync.md). Reminder settings stay on the phone whose alarms they are. Notifications default to a neutral text. OpenVitals has no internet permission.
