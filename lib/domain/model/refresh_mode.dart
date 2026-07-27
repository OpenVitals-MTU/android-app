/// How hard a load should work to avoid reusing what it already has.
///
/// ## What force does and does not bypass
///
/// [force] bypasses the DERIVED caches — the body-energy timeline store, whose
/// entries are recomputed model output and cheap to rebuild. It deliberately
/// does NOT bypass the durable daily-aggregate mirror (the vitals daily cache
/// and the calories-burned cache in the same table). Those are not a shortcut
/// around a fast read: they exist *because* the underlying read is unusable.
/// Health Connect's `TotalCaloriesBurned` year aggregate runs 13-24 seconds, and
/// a year of vitals is seven 730-day reads. Bypassing them would turn a
/// pull-to-refresh into a half-minute stall.
///
/// They stay correct another way: the app's own writes patch the affected days
/// through (`VitalsRepositoryImpl._patchCachedDays`,
/// `ActivityRepositoryImpl._patchCachedCaloriesDays`), and anything written by
/// another app is picked up by the Changes-API drains in `lib/data/sync/`. A
/// screen whose data comes from one of those caches therefore drains first and
/// then reads — see the vitals overview, calories and body-energy screens — so a
/// forced refresh there costs one incremental Changes poll instead of a full
/// re-aggregation.
enum RefreshMode {
  /// Reuse a cached or derived value when it is still considered fresh.
  normal,

  /// Recompute the derived values rather than reusing them.
  force,
}
