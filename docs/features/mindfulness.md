# Mindfulness

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/mindfulness/` (incl. `mindfulness/reminders/`), `lib/features/manualentry/presentation/mindfulness_entry_screen.dart` + `lib/features/manualentry/mindfulness/`, `lib/data/repository/contract/mindfulness_repository.dart` (+ `impl/mindfulness_repository_impl.dart`).
> **Navigation:** `/metric/MINDFULNESS`; `/manual_entry/mindfulness` (+ `/edit/:mindfulnessEntryId`); `ManualEntryWidgetId.mindfulness`.
> **Related:** [Feature map](feature-map.md), [Manual entry of metrics](manual-entry-metrics.md), [Reminders](reminders.md).

The mindfulness feature owns period-based meditation/mindfulness session details, goals, reminders, and links to manual session entry.

## How to use it

### Log a session

Tap **Log › Mindfulness** on the dashboard (or the **Log session** button on the Mindfulness detail). The **Mindfulness entry** screen offers two ways:

- **Timer** — set **Minutes**, optionally turn on the **Interval bell**, and pick a **Bell sound** and **Background sound**. Use **Start**, then **Stop**, then **Resume**, **Save session**, or **Discard**. Sessions under one minute are rejected.
- **Manual entry** — type the **Minutes** and tap **Add minutes** to record a session that just finished.

Every session is saved to Health Connect with the title "Meditation".

### Review

1. Open the **Mindfulness** detail from its dashboard tile for total mindful minutes, session counts, average and longest session, and your session history — with the shared **Day / Week / Month / Year** controls (see [Statistics](statistics.md)).
2. Tap a session **you logged in OpenVitals** to edit it, or swipe to delete it. Sessions from other apps stay read-only.
3. Turn on reminders from the card at the bottom of this screen — see [Reminders](reminders.md).

> There is currently no in-app control to change the mindfulness goal (it is fixed at 10 minutes and used only by the reminder).

## What It Shows

Mindfulness can show:

- Total mindful minutes for the selected period.
- Daily session distribution.
- Goal progress.
- Previous-period comparison and baseline context.
- Session entry history.
- Sleep-adjacent context when enough sleep data is available.
- Local reminder settings.

## Detail Pattern

Mindfulness follows the canonical period-detail model:

- Day, week, month, and year ranges.
- Selected anchor date.
- Previous/next navigation and calendar selection.
- Pull to refresh.
- Period charts and selected-day session lists.
- Statistics, comparisons, confidence, and source labels.

OpenVitals-created mindfulness sessions can be edited or deleted when ownership can be verified. Records created by other apps remain read-only.

## Entry And Reminders

Manual mindfulness entry lives under `features/manualentry/mindfulness` and can save timed or manually entered sessions to Health Connect. Mindfulness reminders are local notifications controlled by app preferences and notification permission.

## Related Features

- [`manual-entry-metrics.md`](manual-entry-metrics.md): explicit Health Connect write flows.
- [`reminders.md`](reminders.md): shared reminder behavior.
