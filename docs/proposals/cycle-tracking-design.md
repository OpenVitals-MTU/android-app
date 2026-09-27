# Cycle Tracking: Design and Status

> **Status:** Feature catalog and build plan, 2026-09-27. Sections marked *done* are implemented on this branch; the rest are queued in the order given.
> **Related:** [Cycle tracking](../features/cycle-tracking.md) (current behavior), [Feature playbook](../engineering/feature-playbook.md), [Architecture](../engineering/architecture.md).

## Why

OpenVitals reads and writes the seven Health Connect cycle record types. Before this work it predicted the next period with a mean-and-spread rule and showed observations read-only. This design adds a better-sourced estimator, a conservative phase model, subjective daily logging, sourced tips, a variability view, a thermal chart, reminders, and a home-screen widget, all built around one rule: *what you recorded and what the app calculated are never presented as the same thing*.

## Ground rules

- Recorded facts and estimates get different labels and colours. Every estimate is a range with the history it came from.
- Missing days are unknown, never "no symptoms".
- Nothing assumes a 28-day cycle, ovulation, fertility intent, or a condition. Calendar data never confirms ovulation.
- Tracking contexts (PMS, PMDD, endometriosis, PCOS, perimenopause, thyroid) are statements by the user. They change what the app *offers* and how *wide* its uncertainty is. They never move a predicted date and are never inferred.
- No streaks, scores or rewards.
- Health Connect stays the source of truth for everything it has a record type for. Cycle starts are derived from flow days.
- Room holds only what Health Connect cannot: the subjective journal, cycle exclusions.
- No network. Partner sharing and crash reporting are out of scope; the journal moves with the phone-to-phone sync like everything else.
- Strings follow the app's sentence-case, plain-language copy. Sources are cited on the card that uses them.

## Catalog

Status legend: **exists** already in OpenVitals · **new** built for this design · **adapted** built on an existing piece · **skip** not built, with the reason.

### 1. Daily observations (the day log)

| Feature | Detail | Store | Status |
|---|---|---|---|
| Bleeding scale | Not recorded / none / spotting / light / medium / heavy. "None" is an explicit observation. | light..heavy: `MenstruationFlowRecord`; spotting: `IntermenstrualBleedingRecord`; none: journal | adapted |
| Pain, mood, energy | 1..5 each, with a glyph per step (dots, mood arcs, battery). Direction stated in copy. | journal | new |
| Symptoms | 15 sourced observations in three groups (pain, body, mood and energy). Endometriosis adds pelvic pain outside the period; PMS/PMDD add breast tenderness, mood changes, anxiety, skin. | journal | new |
| Copy yesterday's symptoms | One tap. | — | new |
| Private notes | Free text, never in a widget or notification. | journal | new |
| Basal body temperature | Value, measurement time, disturbance flags (fever, alcohol, poor sleep, time shift, late measurement, stress, medication). | value and time: `BasalBodyTemperatureRecord`; flags: journal | adapted |
| Cervical fluid | Sensation (dry, damp, wet, slippery) and texture (sticky, creamy, egg white, watery). | texture: `CervicalMucusRecord` appearance; sensation: journal | adapted |
| Ovulation test (LH) | Negative, high, positive, inconclusive. | `OvulationTestRecord` | exists |
| Pregnancy test (hCG) | Negative, positive, faint or uncertain. | journal (no Health Connect type) | new |
| Sexual activity | Protected, unprotected, unknown. | `SexualActivityRecord` | exists |
| Cycle start | A flow day after more than one free day starts a cycle. | derived | adapted |
| One sheet for the whole day | Everything above is entered and saved together, with a discard guard. | — | adapted: the entry screen becomes a day log |

### 2. Cycle management

| Feature | Detail | Status |
|---|---|---|
| Add a past period | Date picker limited to the last 92 days. | adapted: writes a light-flow day, which starts a cycle |
| Edit a cycle start | Moves the start; neighbours re-close. | adapted: edit the flow record from the entries list |
| Delete a cycle | Removes the start, keeps the day's observations. | adapted: delete the flow records from the entries list |
| Exclude a cycle from estimates | With a reason: illness, medical treatment, contraception change, stress or travel, other. | new: `cycle_exclusions` table |

### 3. Estimates and phase

| Algorithm | Detail | Status |
|---|---|---|
| `CycleEstimateCalculator` | Start-to-start intervals, last six inside 15..90 days, excluded cycles skipped, a gap over 90 days resets the history. Central date = last start + rounded mean. Half-width = 1.96 × √(variance shrunk toward a population prior with weight 2, or 0.5 once the record shows persistent 7-day swings), floored at 3 days, capped at 22, floored again so the window never starts inside recorded flow. Prior SD by age band (Apple Women's Health Study table), 4.54 days undeclared. A declared timing context floors the variance at the prior. Results: available, needs more history (fewer than two starts), intervals out of range. | new, replaces the mean-and-spread rule |
| `CurrentCyclePhaseCalculator` | Menstrual is recorded (day 1 or flow today). Days 2–7 without flow detail are indeterminate. Follicular before the ovulation band, luteal after it, ovulatory only on the central day (central − 13) and only with six stable intervals varying at most 7 days. The two days before the central date and the estimate window are the "next period window"; past the window the estimate has expired. | new |
| Cycle facts | 24 population facts with sources, one per day by date hash, shown when the phase is indeterminate. | new |
| Phase tips | 30 sourced tips, filtered by phase, declared contexts and the last three days' symptoms; ties broken by date hash. | new |

### 4. History and review

| View | Detail | Status |
|---|---|---|
| Month calendar | Recorded flow (four fills), estimated window (outline), cycle-start dot, observation dot, legend, selected-day card. | adapted: the existing calendar gains the estimate window, observation dots and a legend |
| Timeline | Entries newest first with flow drop, level bars, symptom count, note marker. | adapted: journal rows join the entries list |
| Variability | Bar per cycle against a 21–35 day reference band, badges for current, excluded and a 7-day swing, median and mean, tap for detail and exclusion. | new |
| Thermal chart | Cycle-day axis, disturbed readings hollow, coverline once a sustained shift is found (six lows, three highs, third at least 0.2 °C above the highest low, readings at most three days apart). | adapted: the BBT card is rebuilt on this rule |
| Patterns | Symptom counts by phase over all cycles, descriptive only. | new |
| Cycle stats | Average, count, min–max of recent intervals. | new |

### 5. Reminders

| Reminder | Detail | Status |
|---|---|---|
| Daily check-in | Fixed time, skipped when today already has observations. | new |
| Period window | 1–3 days before the earliest estimated date, 09:00. | new |
| Late cycle | Grace days after the latest estimated date, 10:00. | new |
| Visibility | Concealed (neutral copy, default), descriptive, custom title and body. Lock-screen copy is always neutral. | new |
| Re-arm after reboot, update, clock and zone change | Same as the app's other reminders. | new |

### 6. Home-screen widget

| Widget | Detail | Status |
|---|---|---|
| Cycle widget | Starts concealed. Reveal shows cycle day (recorded), estimated range, and whether today is logged. Never shows notes, symptoms, mood or energy. Compact, standard, wide and expanded layouts. Re-renders after midnight. | new, done |

### 7. Clinical report

| Feature | Detail | Status |
|---|---|---|
| Consultation summary | The report's range instead of a cycle count. Cycle table (start, end, length, bleeding days, peak flow, pain days, excluded), mean, median, sample SD, range, mean bleeding days; bleeding, spotting and intermenstrual days (7+ days from a start); pain days on and off bleeding, severe days (4+), mean score; symptom frequency on and off bleeding; notes chronology; a disclaimer. | done: a section of the PDF health report |

### 8. Setup and settings

| Feature | Detail | Status |
|---|---|---|
| Setup | A Cycle settings section holds contexts, age band and reminders; the cycle screen links to it when nothing is set. | new |
| Tracking contexts | PMS, PMDD, endometriosis (observation group); PCOS, perimenopause, thyroid (timing group). | new |
| Age band | Optional, picks the variability prior. | new |
| Temperature unit | Celsius or Fahrenheit. | exists: unit overrides |
| App lock, screen masking | PIN, biometric, `FLAG_SECURE`. | exists: app lock |
| JSON export and import | Versioned backup of the journal, exclusions, contexts and age band; import merges, the newer edit per day wins. | done |
| Erase all data | — | adapted: the journal is erased with the app's data reset |

### 9. Out of scope

Network sync, end-to-end encryption, partner sharing, and crash reporting. OpenVitals has no `INTERNET` permission by design.

## Data model

Two Room tables, migration 12 → 13. Health Connect has no record type for any of these fields.

| Table | Key | Columns |
|---|---|---|
| `cycle_journal_entries` | `date` (ISO day) | `bleeding_none`, `pain`, `mood`, `energy`, `symptoms` (ids, comma-separated), `notes`, `hcg_test`, `bbt_disturbances`, `cervical_sensation`, `updated_at_millis` |
| `cycle_exclusions` | `start_date` (ISO day) | `reason` |

Preferences, through a `CyclePreferences` contract: declared contexts, age band, reminder configuration.

## Phases

1. Domain and tests: estimator, phase model, facts, tips, thermal shift, patterns, longitudinal stats, catalog. *done*
2. Storage: the two tables, the journal repository, the preferences contract, statistics that honour exclusions and contexts. *done*
3. Logging: the day log with scales, symptoms, notes, hCG, BBT flags, cervical sensation. *done*
4. Display: today card, phase, estimate, tip or fact, calendar upgrade, variability, thermal, patterns, exclusion and backfill dialogs. *done*
5. Settings: contexts, age band, reminders. *done*
6. Reminders. *done*
7. Home-screen widget. *done*
8. Alignment with the rest of the app: one day-log route, widget deep link and state, reminders restored and read strictly, dashboard tile, age band from the body profile, journal deletion, swipe-delete confirmation, accessibility, plurals, docs. *done*
9. Report section and journal export. *done*
