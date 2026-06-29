# Code Analysis

Senior Android architecture review of the OpenVitals codebase (June 2026).

A **P0–P3 gap-closure program** updated metric screens, dashboard boundaries, and cross-cutting docs. Start with [executive-summary.md](executive-summary.md) and the [migration tracker](refactor-backlog.md#migration-tracker-gap-closure-program) for current status.

This section documents how the project measures against MVVM + Repository, Clean Architecture, Compose performance, testability, and production practices. Use it alongside:

- [Architecture](../architecture.md) — target architecture and current snapshot
- [Feature playbook](../feature-playbook.md) — checklist for new metric work
- [AGENTS.md](../../AGENTS.md) — agent implementation guide

If code and these analysis docs disagree, prefer [architecture.md](../architecture.md) for new work and refactor incrementally.

## Index

| Document | Focus |
|----------|-------|
| [Executive summary](executive-summary.md) | Grades, strengths, and top priorities |
| [MVVM and Repository](mvvm-repository.md) | ViewModels, repositories, period driver, data boundaries |
| [ViewModel and StateFlow](viewmodel-stateflow.md) | UI state, orchestration, separation of concerns |
| [Testability and production readiness](testability-production.md) | Tests, scalability patterns, risks |
| [Error handling and null safety](error-handling-null-safety.md) | Error models, Kotlin safety, best practices |
| [Project structure](project-structure.md) | Packages, feature layout, recommended file organization |
| [Compose performance](compose-performance.md) | Recomposition, `remember`, state granularity |
| [Clean Architecture refactor](clean-architecture-refactor.md) | Current layers vs. target, phased migration |
| [Refactor backlog](refactor-backlog.md) | Prioritized improvement list |

## How to use this

1. **Adding a feature** — read [project-structure](project-structure.md) and [viewmodel-stateflow](viewmodel-stateflow.md), then follow the [feature playbook](../feature-playbook.md).
2. **Refactoring a large screen** — start with [compose-performance](compose-performance.md) and [refactor-backlog](refactor-backlog.md).
3. **Boundary changes** — read [mvvm-repository](mvvm-repository.md) and [clean-architecture-refactor](clean-architecture-refactor.md) before introducing interfaces or use cases.
