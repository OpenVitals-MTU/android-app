# Mindfulness

The mindfulness feature owns period-based meditation/mindfulness session details, goals, reminders, and links to manual session entry.

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

