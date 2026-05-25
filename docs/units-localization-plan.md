# Units And Localization Plan

OpenVitals keeps unit and language behavior local-first and preference-driven.

## Current Shape

- `PreferencesRepository` stores unit system and app language preferences.
- `UnitFormatter` prepares display values for metric and imperial units.
- `DateTimeFormatterProvider` centralizes date/time display formatting.
- `OpenVitalsApp` and `MainActivity` apply the selected app locale.

## Guidelines

- Do not format raw metric values directly in composables when a shared formatter exists.
- Keep medical/health semantics out of generic formatters.
- Prefer string resources for user-visible text.
- Keep feature-specific wording inside the feature package unless it becomes shared UI language.

## Follow-Up Work

- Move remaining hardcoded labels to resources.
- Add focused tests for formatter behavior when new unit displays are introduced.
- Keep chart axis formatting consistent with detail-card formatting.
