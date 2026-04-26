# Documentation Index

This folder documents the target architecture for future feature work.

## Read In This Order

1. [../plan.md](../plan.md)
2. [architecture.md](architecture.md)
3. [feature-playbook.md](feature-playbook.md)
4. [metrics-roadmap.md](metrics-roadmap.md)
5. [units-localization-plan.md](units-localization-plan.md)
6. [../AGENTS.md](../AGENTS.md)

## What Each Doc Does

- [architecture.md](architecture.md): target architecture, boundaries, and reusable patterns
- [feature-playbook.md](feature-playbook.md): the checklist for implementing a new metric feature
- [metrics-roadmap.md](metrics-roadmap.md): current Health Connect coverage gaps and future feature roadmap
- [units-localization-plan.md](units-localization-plan.md): canonical-unit, display-unit, and localization migration plan

## Important Context

The codebase is currently in a transition state:

- steps/activity, activities, sleep, heart, body, and browse all use the shared period-based detail shell
- `HealthRepository` is now focused on permissions and dashboard loading; feature data lives in feature repositories
- the main remaining cleanup is shared period state/orchestration across ViewModels and eventual `core/period` extraction

New work should follow the target architecture in these docs, even if some older files still use legacy patterns.
