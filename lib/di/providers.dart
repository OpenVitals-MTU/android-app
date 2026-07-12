/// The application's object graph.
///
/// A barrel over the three halves of the graph, so that the ~200 files which
/// `import '../../di/providers.dart'` keep working unchanged and a reader can
/// still find every provider from one place:
///
/// - [data_providers.dart] — bootstrap singletons, the Health Connect data
///   source, and the repositories (contract type → impl instance)
/// - [usecase_providers.dart] — the read orchestrator and the use cases
/// - [service_providers.dart] — reminders, home-screen widgets, offline maps
///
/// Feature-level providers do NOT live here: a view-model declares its own
/// `NotifierProvider` next to itself, in its feature's `application/` file.
library;

export 'data_providers.dart';
export 'service_providers.dart';
export 'usecase_providers.dart';
