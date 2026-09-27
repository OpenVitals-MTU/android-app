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
- Pain, mood, and energy on a 1 to 5 scale.
- Symptoms from a fixed catalog (cramps, headache, bloating, mood changes, and so on).
- Private notes.
- Pregnancy test result, temperature disturbance flags, cervical sensation.

## The day log

"+ Log" → Cycle, the cycle screen's "Log today" button, the calendar, and the home widget all open one day-log screen for a date. It shows bleeding first (not recorded, none, spotting, light, medium, heavy), then the three scales, then symptoms and notes behind a disclosure, then body observations (temperature with time and disturbances, cervical sensation, mucus, ovulation test, sexual activity). Blank means not logged.

Saving reconciles each Health Connect record kind separately: a record OpenVitals owns is updated or deleted, a new value is inserted, and an unchanged value is left alone. Records written by other apps are shown as read-only context and are never modified. Journal fields go to the local table. Leaving with unsaved changes asks first.

Menstruation period intervals are derived, not logged. After a flow change the app reconciles consecutive bleeding days (one-day gap tolerated) into period records it owns.

## Cycle screen

The screen is a metric detail scaffold with day, week, month, and year periods. The top of every period shows:

- **Hero**: recorded cycle day and the date it counts from, or an invitation to log the first period.
- **Phase**: menstrual, follicular, ovulatory transition, luteal, or a transition window with the reason it cannot be placed.
- **Today**: what is logged today, or a nudge to log.
- **Estimate**: the next period window with a bar showing where today sits, or why there is no estimate yet.
- **Tip or fact**: one phase tip picked by date, declared contexts, and recent symptoms, or a cycle fact with its source. The choice is stable within a day.
- **Stats**: count, mean, and range of recorded cycles.
- **Setup**: a link to cycle settings when no context or age band is set.

Below that, per period: the calendar (recorded flow, spotting, estimated window outline, cycle-start marker, observation dots), the cycle history bars against a 21 to 35 day reference band with a per-cycle detail sheet, the thermal chart with a coverline when a shift is confirmed, symptom patterns per phase, and the observation list. Tapping a calendar day opens its day log.

## Estimator

Cycle starts come from bleeding segments across all Health Connect sources. The estimator uses start-to-start intervals: the last six within 15 to 90 days, resetting on a gap over 90 days, skipping excluded cycles. The mean is shrunk toward an age-band prior (weight 2, or 0.5 when the history shows persistent 7-day swings), the spread is 1.96 standard deviations with a floor of 3 and a cap of 22 days, and a declared timing context (PCOS, perimenopause, thyroid) raises the variance floor. The window never starts before the day after the last recorded flow day.

Two recorded starts give a first estimate. With fewer, the screen says it is waiting for history. When intervals are out of range, the screen says so instead of guessing.

## Phase

Bleeding days are menstrual. Days 2 to 7 without a bleeding entry are indeterminate. The ovulation anchor is the estimated window's central day minus 13, plus or minus 2. The ovulatory label appears only on the central day and only with at least six intervals and at most 7 days of variability. From two days before the central day the phase is the next-period window. After the window ends the phase is expired until a new start is recorded.

## Thermal shift

Over the current cycle's basal temperatures: six lows followed by three highs, the third at least 0.2 °C above the highest low, no gap over three days, at most a two-day transition. Disturbed readings are shown but skipped. A confirmed shift is retrospective information, never a prediction.

## Cycle management

- **Add past period**: writes a light-flow record at noon on a chosen day up to 92 days back, so the estimator has history sooner.
- **Exclude a cycle**: keeps the cycle in history but out of the estimate, with an optional reason (illness, treatment, contraception change, stress or travel, other). Stored in `cycle_exclusions`, keyed by a date inside the cycle.
- **Delete a journal entry**: from the observation list. Health Connect records OpenVitals owns are deleted the same way.

## Settings

Settings → Cycle holds:

- **Tracking contexts**: PMS, PMDD, endometriosis (add symptom suggestions to the day log); PCOS, perimenopause, thyroid (widen the estimate).
- **Age band**: picks the variability prior. A birth year in the body profile sets it; the picker is the fallback when there is none.
- **Reminders**: see [Reminders](reminders.md).
- **Delete cycle journal**: wipes the journal, the exclusions and every cycle setting on the device after a confirmation. Health Connect records stay.

All of it is local. Nothing here is written to Health Connect.

## Report and backup

The health report offers a Cycle tracking section: the cycle-day chart, the length statistics over completed cycles, bleeding and pain counts, a table of the cycles in the range, the symptom counts on and off bleeding days, and the notes. See [Health report export](health-report-export.md).

Settings → Cycle exports the journal as a JSON file (day logs, excluded cycles, contexts and age band) and imports one back. An import merges: a day both sides hold keeps the newer edit, exclusions are added, and the contexts and age band fill a phone that declared none. Health Connect records are not in the file; Health Connect has its own export.

## Dashboard tile and widget

The dashboard cycle tile shows the recorded cycle day with the phase, or the estimate when the phase is unknown; it falls back to the day's counts when there is no current cycle. See [Home widgets](home-widgets.md) for the home-screen widget, which shows the cycle day, the estimated range, and whether today is logged, with a button that hides them.

## Permissions

Cycle permissions are managed as their own Health Connect category. The screen and widget show a permission state when the category is missing. Writing a day log needs the write permission for each record kind it touches; kinds without it are skipped and the screen says so.

## Privacy

Cycle data stays in Health Connect and in the app's local database on the device. The journal, the exclusions, and the declared contexts and age band move with the cycle category of phone-to-phone sync, like Health Connect records do; see [Sync with another phone](device-sync.md). Reminder settings stay on the phone whose alarms they are. Notifications default to a neutral text. OpenVitals has no internet permission.
