import 'package:flutter/foundation.dart';

import 'body_energy_chain_sync_service.dart';
import 'calories_history_sync_service.dart';
import 'vitals_history_sync_service.dart';

/// Runs the three cache drains once per app open, one after another.
///
/// The daily-aggregate caches are only as fresh as their last Changes-API
/// drain, and each drain used to be kicked from exactly one screen — so a watch
/// sync or an import followed by opening only the dashboard left the calories
/// and vitals caches stale until the user happened to visit those screens. The
/// app-open trigger is the natural place to catch them up.
///
/// **Sequential, never concurrent, and never alongside a foreground read.**
/// Health Connect serializes concurrent reads: a drain running next to the
/// dashboard's own load is the documented 30s→80s contention that the
/// per-screen "kick only after the first load settles" sequencing exists to
/// avoid. This reproduces that rule at app scope — [DataRefreshBootstrap] waits
/// for the dashboard's load to settle before calling in.
///
/// The drains are incremental only. A first full sync is a multi-minute history
/// read and stays owned by the screen that needs the cache: a user who never
/// opens the vitals overview never pays for its 730-day reads.
class HistorySyncScheduler {
  HistorySyncScheduler(this._vitals, this._calories, this._bodyEnergy);

  final VitalsHistorySyncService _vitals;
  final CaloriesHistorySyncService _calories;
  final BodyEnergyChainSyncService _bodyEnergy;

  Future<void>? _running;

  /// Drain what already has a cursor. Concurrent calls share one run.
  Future<void> drainIncremental() =>
      _running ??= _drain().whenComplete(() => _running = null);

  Future<void> _drain() async {
    // Each stage is caught on its own: one failing drain must not starve the
    // rest, and none of them is worth surfacing to the user — the screen behind
    // them falls back to its live read either way.
    await _step('vitals', _vitals.syncIncremental);
    await _step('calories', _calories.syncIncremental);
    // Body energy last: it is a chain WALK rather than a poll, and it carries
    // its own 30-minute throttle and per-pass time budget, so a quiet app open
    // costs nothing and a busy one is bounded.
    await _step('bodyEnergy', _bodyEnergy.syncAll);
  }

  Future<void> _step(String name, Future<void> Function() run) async {
    try {
      await run();
    } catch (error, stack) {
      debugPrint(
          'HistorySyncScheduler: $name drain failed, will retry on the next '
          'app open: $error\n$stack');
    }
  }
}
