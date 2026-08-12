# OpenVitals Docs

Start here when you need to understand the app, a feature, or the implementation direction.

## Use The App

- [App guide](app/README.md): install, getting started, Health Connect, permissions, privacy, FAQ, editions, screenshots, and support.
- [How-to guides](how-to/README.md): concrete workflows that need step-by-step instructions.
- [Release notes](releases/2.5.0.md): latest user-facing release summary. Full history: [CHANGELOG.md](../CHANGELOG.md).

## Browse Features

- [Feature guide](features/README.md): grouped feature pages for current app behavior.
- [Feature map](features/feature-map.md): canonical mapping between docs, routes/widgets, and implementation packages.
- [Functional inventory](../Features.md): detailed checklist of view, write, import, settings, and privacy capabilities.

Recently added feature pages:

- [Watches](features/watches.md): experimental Garmin and WearOS support, watch sync, watch-only metrics, notification forwarding, watch settings, and find-my-watch.
- [Sync with another phone](features/device-sync.md): copying Health Connect records between two phones over Bluetooth.
- [CSV import](features/csv-import.md): mapped import of body measurements and vitals from a CSV file.

## Contribute And Architecture

- [Engineering guide](engineering/README.md): architecture, development setup, feature playbook, code analysis, and test parity.
- [Architecture](engineering/architecture.md): current single-module architecture, feature boundaries, period detail pattern, and data-access rules.
- [Feature playbook](engineering/feature-playbook.md): checklist for adding or extending a metric feature.
- [Translations](engineering/translations.md): translate OpenVitals in your language, Codeberg Translate setup, shipping policy, and validation checks.

## Reference And Archive

- [Proposals](proposals/README.md): non-current plans and design explorations.
- [Reference](reference/README.md): external or supporting design/reference material.
- [Images](images/): screenshots and image assets used by docs.

If code and docs disagree, prefer the docs for new work and refactor incrementally toward them.
