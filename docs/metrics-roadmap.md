# Metrics Roadmap

Most first-pass metric coverage is implemented. Current roadmap work is cleanup and depth, not broad product expansion.

## Near Term

- Continue migrating detail screens toward bundled period query APIs.
- Keep dashboard reads scoped to visible widgets and user-enabled categories.
- Keep manual-entry writes isolated to Add entry and metric entry routes.
- Split oversized feature screen files by route, content, cards, charts, and rows.
- Add tests around hidden dashboard widgets, manual-entry widget ordering, disabled cycle tracking, and stale-load cancellation.

## Later

- Add richer source attribution where Health Connect records provide useful origin metadata.
- Improve long-range chart density and empty-state language.
- Consider persistent caching only after there is a concrete product need.

## Out Of Scope For Now

- Room
- WorkManager
- multi-module split
- write-back to Health Connect outside explicit manual-entry flows
- account sync or cloud storage
