# OpenVitals Docs

Use these docs as the working index for product and architecture decisions.

- [App guide](app/README.md): user-facing install, getting started, Health Connect, permissions, privacy, FAQ, editions, and screenshot notes.
- [Architecture](architecture.md): current single-module architecture, Hilt wiring, manual-entry boundaries, period detail pattern, and data-access rules.
- [Code analysis](analysis/README.md): MVVM, Clean Architecture, Compose performance, testability, and refactor backlog (senior review).
- [Development](development.md): local build, verification, CI, and Windows cleanup notes.
- [Feature playbook](feature-playbook.md): checklist for adding or extending a metric feature.
- [Feature website gap matrix](website-feature-gap-matrix.md): comparison between this repo's feature docs and the sibling website docs.
- [Features](features/README.md): user-facing feature guide with focused pages for each major capability.
- [How-to guides](how-to/README.md): concrete workflows such as adding offline maps.
- [Release notes](releases/1.7.5.md): latest user-facing release summary.
- [Support](support.md): support boundaries, useful user docs, and privacy expectations for diagnostics.

If code and docs disagree, prefer the docs for new work and refactor incrementally toward them.
