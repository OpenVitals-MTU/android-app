import 'package:flutter/foundation.dart';

import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../../domain/health/health_permissions.dart';
import '../../domain/insights/body_energy_timeline.dart';
import '../../domain/model/refresh_mode.dart';
import '../prefs/preferences_repository.dart';
import '../repository/body_energy_baseline_cache_store.dart';
import '../repository/body_energy_timeline_store.dart';
import '../repository/contract/body_energy_repository.dart';
import '../repository/impl/health_connect_gating.dart';
import '../source/health/health_data_source.dart';

/// How many days back the warm window reaches. Matches the repository's chain
/// lookback, so any day inside the window finds a stored anchor.
const int bodyEnergyChainWarmDays = 14;

/// Keeps a rolling window of recent Body Energy days computed and stored, so
/// the chain the detail screen walks is almost never cold.
///
/// Body Energy carries across midnight: each day opens where the previous one
/// closed. Without a warm window, a user who last opened the app a week ago
/// would find no stored predecessor and the day would have to restart at the
/// neutral score — the foreground gap fill is deliberately bounded to two days
/// because each day costs ~8 Health Connect reads, and closing a week of them
/// while someone waits on a screen is not acceptable. That work belongs here.
///
/// Best-effort throughout, like `CaloriesHistorySyncService`: every failure is
/// swallowed and retried on the next pass, never surfaced.
class BodyEnergyChainSyncService {
  BodyEnergyChainSyncService(
    this._repository,
    this._store,
    this._baselineStore,
    this._dataSource,
    this._preferences, {
    this._clock = DateTime.now,
    this._windowDays = bodyEnergyChainWarmDays,
  });

  final BodyEnergyRepository _repository;
  final BodyEnergyTimelineStore _store;
  final BodyEnergyBaselineCacheStore _baselineStore;
  final HealthDataSource _dataSource;
  final PreferencesRepository _preferences;
  final DateTime Function() _clock;
  final int _windowDays;

  /// A cold install has to walk the whole window at ~8 Health Connect reads a
  /// day. That is fine in the background but must not run away, so a pass stops
  /// when the budget is spent and resumes where it left off — the days it
  /// already wrote are skipped as fresh next time.
  static const Duration _passBudget = Duration(seconds: 90);

  /// Every screen open calls [syncAll]; without this, opening Body Energy five
  /// times in a minute would re-walk the window five times.
  static const Duration _throttle = Duration(minutes: 30);

  /// How long a stored past day counts as fresh, matching the repository's own
  /// past-day staleness rule. Only used to skip work the repository would
  /// otherwise skip anyway; the repository stays the authority.
  static const Duration _dayFreshness = Duration(hours: 24);

  Future<void>? _running;

  /// Warm the chain. Concurrent calls share one run.
  ///
  /// Pass [force] to bypass the throttle — for a caller that has just made the
  /// stored chain wrong (a watch sync back-filling days) rather than one merely
  /// opening a screen. A forced call still joins an in-flight run: whatever is
  /// already walking will pick up the holes.
  Future<void> syncAll({bool force = false}) =>
      _running ??= _sync(force: force).whenComplete(() => _running = null);

  Future<void> _sync({bool force = false}) async {
    try {
      // One-shot cleanup of the retired SharedPreferences timelines. This is
      // the natural home for it: already best-effort, already on the UI
      // isolate, and it runs before any chain work needs the prefs.
      await _baselineStore.purgeLegacyTimelineEntries();

      final granted = await _dataSource.grantedIfAvailable();
      if (!granted.contains(HcPermissions.readHeartRate)) return;

      // Rows computed under a retired calibration are wrong, not merely stale,
      // so a signature change purges rather than letting them age out.
      final signature = _globalSignature(granted);
      if (await _store.storedGlobalSignature() != signature) {
        await _store.purgeAll();
        await _store.writeGlobalSignature(signature);
      }

      final now = _clock();
      final lastPass = await _store.lastPassAt();
      if (!force && lastPass != null && now.difference(lastPass) < _throttle) {
        return;
      }

      final today = LocalDate.fromDateTime(now);
      await _store.applyRetention(today);

      // Days the repository would serve from storage cost nothing to keep.
      // Skipping them here rather than letting its cache check do it saves a
      // permission round-trip and a store read per day — on the common warm
      // pass that is the difference between a dozen calls and none.
      //
      // The rule must match the repository's, or this would keep recomputing
      // settled days it would happily have served: a day is worth revisiting
      // only while it can still gain late-arriving data, or while today's copy
      // of it has aged past a day.
      final window = await _store.storedDaysBetween(
        today.minusDays(_windowDays - 1),
        today.minusDays(1),
      );
      final freshEpochDays = {
        for (final day in window)
          if (today.epochDay - day.date.epochDay > bodyEnergyChainSettlingDays ||
              now.difference(day.generatedAt) < _dayFreshness)
            day.date.epochDay,
      };

      // Oldest first, and that order is load-bearing: each day's seed must
      // already be stored by the time its successor is computed.
      final stopwatch = Stopwatch()..start();
      var completed = true;
      for (var back = _windowDays - 1; back >= 1; back--) {
        if (stopwatch.elapsed >= _passBudget) {
          completed = false;
          break;
        }
        // Today is skipped: the foreground load owns it, and recomputing it
        // here would fight its 15-minute freshness window.
        final date = today.minusDays(back);
        if (freshEpochDays.contains(date.epochDay)) continue;
        await _repository.loadTimeline(
          BodyEnergyTimelineQuery(
            period: DatePeriod(date, date),
            range: TimeRange.day,
            refreshMode: RefreshMode.normal,
          ),
        );
      }

      // Only a completed pass resets the throttle; a budget-truncated one lets
      // the next open pick up the remaining days immediately.
      if (completed) await _store.writeLastPassAt(now);
    } catch (e, s) {
      debugPrint(
        'BodyEnergyChainSyncService: warm pass failed, will retry: $e\n$s',
      );
    }
  }

  /// The chain-wide validity stamp: algorithm version plus the configured and
  /// permission inputs every day shares. The per-day signature additionally
  /// folds in the body profile, whose value varies by date — that belongs on
  /// the row, not here.
  ///
  /// The learned gains are deliberately NOT in it, because a mismatch here
  /// purges every stored day and every stored bucket. The watch fit nudges a
  /// gain by a fraction of a percent whenever it absorbs an observation, so
  /// including them meant the entire history — up to
  /// [bodyEnergyBucketRetentionDays] of it — was deleted on essentially every
  /// watch sync, which is no way to build the weekly view those buckets exist
  /// for. A gain change does not make a stored row wrong enough to destroy it:
  /// the per-day signature still refuses to SERVE one, so it is recomputed on
  /// demand, and the seed lookup can still anchor on it in the meantime.
  String _globalSignature(Set<String> granted) {
    final permissions = granted.toList()..sort();
    final configured =
        _preferences.bodyEnergyCalibrationListenable.value.zoneSignature();
    return 'v$bodyEnergyTimelineAlgorithmVersion'
        '|${configured.hashCode}|${permissions.join(',').hashCode}';
  }
}
