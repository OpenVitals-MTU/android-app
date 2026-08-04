# Engineering

These docs describe how the app is built and how new work should fit into the current architecture.

- [Architecture](architecture.md): current single-module architecture, feature boundaries, period detail pattern, and data-access rules.
- [Development](development.md): local build, verification tasks, the translation gate and its current state, CI, and release.
- [Feature playbook](feature-playbook.md): checklist for adding or extending a metric feature.
- [Translations](translations.md): Codeberg Translate setup, translation policy, and validation checks.
- [Design system](design-system.md): how the app's tokens map onto the OpenVitals design system, the two deliberate Material 3 deviations, why the metric accents are an accessibility floor rather than a palette, and the reduced-motion contract. Read it before changing a colour, a radius, or an animation duration.
- [Code analysis](analysis/README.md): architecture and implementation review material.
- [Test parity](test-parity/README.md): case-level Flutter ↔ Kotlin test comparison from the back-migration, with the outstanding portable gaps and the behavior divergences that no test can close. Check it before assuming a scenario is untested, and update the relevant row when you close a gap.

For user-facing behavior, start with [App guide](../app/README.md) and [Features](../features/README.md).
