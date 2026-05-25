# Metrics Roadmap

Most first-pass metric coverage is implemented. Current roadmap work is cleanup and depth, not broad product expansion.

## Near Term

- Continue migrating detail screens toward bundled period query APIs.
- Keep dashboard reads scoped to visible widgets and user-enabled categories.
- Split oversized feature screen files by route, content, cards, charts, and rows.
- Add tests around hidden dashboard widgets, disabled cycle tracking, and stale-load cancellation.

## Later

- Add richer source attribution where Health Connect records provide useful origin metadata.
- Improve long-range chart density and empty-state language.
- Consider persistent caching only after there is a concrete product need.

## Out Of Scope For Now

- Room
- WorkManager
- multi-module split
- write-back to Health Connect beyond explicit hydration logging
- account sync or cloud storage
