# OpenVitals Docs

Use these docs as the working index for product and architecture decisions.

- [Architecture](architecture.md): current single-module architecture, Hilt wiring, manual-entry boundaries, period detail pattern, and data-access rules.
- [Development](development.md): local build, verification, CI, and Windows cleanup notes.
- [Feature playbook](feature-playbook.md): checklist for adding or extending a metric feature.
- [Manual activity entry plan](manual-activity-entry-plan.md): implementation status for activity logging, GPX/KML/KMZ import, and live GPS recording.
- [Metric insights report](metric-insights-report.md): notes on metric interpretation and insight behavior.
- [Metrics roadmap](metrics-roadmap.md): short list of remaining metric and cleanup work.
- [Release notes](releases/1.0.0-beta.1.md): Codeberg-facing release notes for the current release.
- [Units and localization plan](units-localization-plan.md): display unit and language cleanup notes.

If code and docs disagree, prefer the docs for new work and refactor incrementally toward them.
