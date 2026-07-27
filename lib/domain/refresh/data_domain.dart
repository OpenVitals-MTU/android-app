/// The kinds of health data a screen reads and a write can invalidate.
///
/// Deliberately coarser than [DashboardMetric] and coarser than the repository
/// split: this is the vocabulary of "who has to re-read", not of "what was
/// stored". A screen declares the domains it reads; a repository declares the
/// domain it wrote; the refresh coordinator matches them.
///
/// Pure — no Riverpod, no Flutter — because the data layer imports it.
enum DataDomain {
  steps,
  activities,
  calories,
  hydration,
  nutrition,
  caffeine,
  sleep,
  recovery,
  readiness,
  heart,
  vitals,
  body,
  mindfulness,
  cycle,
  bodyEnergy,
  achievements,
}

/// The domains DERIVED from a written one, and therefore stale alongside it.
///
/// This table is the whole point of signalling at the repository boundary: a
/// repository knows what it wrote and must not know who cares. Writing a drink
/// through `HydrationRepositoryImpl` also stores a paired nutrition record, so a
/// logged drink makes the nutrition and caffeine screens stale — a knowledge the
/// hydration entry form does not have and should not carry.
///
/// Not transitive on its own; [expandDomains] takes the closure.
const Map<DataDomain, Set<DataDomain>> kDerivedDomains =
    <DataDomain, Set<DataDomain>>{
  // A drink is stored as a hydration record plus its nutrition twin (caffeine
  // rides on the nutrition record).
  DataDomain.hydration: <DataDomain>{DataDomain.nutrition},
  DataDomain.nutrition: <DataDomain>{DataDomain.caffeine, DataDomain.calories},
  // A workout moves the calorie burn, the step total, the readiness inputs and
  // the achievement counters for its day.
  DataDomain.activities: <DataDomain>{
    DataDomain.calories,
    DataDomain.steps,
    DataDomain.bodyEnergy,
    DataDomain.achievements,
  },
  DataDomain.steps: <DataDomain>{
    DataDomain.calories,
    DataDomain.achievements,
    DataDomain.bodyEnergy,
  },
  DataDomain.sleep: <DataDomain>{
    DataDomain.recovery,
    DataDomain.readiness,
    DataDomain.bodyEnergy,
  },
  DataDomain.heart: <DataDomain>{DataDomain.bodyEnergy, DataDomain.readiness},
  DataDomain.vitals: <DataDomain>{DataDomain.bodyEnergy},
  DataDomain.body: <DataDomain>{DataDomain.bodyEnergy},
};

/// [written] plus everything derived from it, transitively.
///
/// A breadth-first closure rather than a single lookup, so a chain like
/// `hydration -> nutrition -> calories` reaches the calories screen without the
/// table having to spell every path out. Cycles in [kDerivedDomains] terminate:
/// a domain already in the result is never expanded twice.
Set<DataDomain> expandDomains(Set<DataDomain> written) {
  final result = <DataDomain>{...written};
  final pending = <DataDomain>[...written];
  while (pending.isNotEmpty) {
    final derived = kDerivedDomains[pending.removeLast()];
    if (derived == null) continue;
    for (final domain in derived) {
      if (result.add(domain)) pending.add(domain);
    }
  }
  return result;
}
