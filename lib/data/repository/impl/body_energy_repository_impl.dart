import 'dart:async';
import 'dart:math' as math;

import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../prefs/preferences_repository.dart';
import '../body_energy_baseline_cache_store.dart';
import '../body_energy_timeline_store.dart';
import '../../../domain/dashboard/dashboard_aggregator.dart';
import '../../../domain/insights/body_energy_calibration_fit.dart';
import '../../../domain/insights/body_energy_timeline.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/preferences/body_energy_calibration.dart';
import '../../../domain/preferences/body_profile.dart';
import '../contract/body_energy_repository.dart';
import '../contract/activity_repository.dart';
import '../contract/body_repository.dart';
import '../contract/heart_repository.dart';
import '../contract/health_repository.dart';
import '../contract/sleep_repository.dart';
import '../contract/vitals_repository.dart';
import 'repository_time.dart';
import 'run_catching.dart';

/// Composes the heart / sleep / activity / vitals repositories to build a
/// per-day body-energy timeline via [calculateBodyEnergyTimeline].
///
/// Body Energy is a *chain*: each day opens where the previous one closed, so
/// the stored end score is an input to the next computation, not just a cache
/// entry. [_resolveSeed] is what makes that true — the original port only ever
/// read a cached predecessor and, because the detail screen asks for one day at
/// a time, never found one, so every day silently restarted at 50.
///
/// The expensive 28-day baselines stay in SharedPreferences; the day timelines
/// live in drift ([BodyEnergyTimelineStore]).
///
/// The heart, sleep, activity and vitals reads already return a [Result];
/// their failures are rethrown via `orThrow` inside [runCatching], so the
/// boundary stays a single wrap and any collaborator failing still fails the
/// whole timeline, exactly as before the Result migration.
class BodyEnergyRepositoryImpl implements BodyEnergyRepository {
  BodyEnergyRepositoryImpl({
    required HeartRepository heartRepository,
    required SleepRepository sleepRepository,
    required ActivityRepository activityRepository,
    required VitalsRepository vitalsRepository,
    required BodyRepository bodyRepository,
    required HealthRepository healthRepository,
    required PreferencesRepository preferencesRepository,
    required BodyEnergyBaselineCacheStore baselineCacheStore,
    // Nullable so the home-widget alarm isolate, which must not open drift, can
    // still build a repository. Without it the chain degrades to the prefs seed
    // mirror (see [_resolveSeed]).
    BodyEnergyTimelineStore? timelineStore,
    DateTime Function() now = DateTime.now,
  })  : _heart = heartRepository,
        _sleep = sleepRepository,
        _activity = activityRepository,
        _vitals = vitalsRepository,
        _body = bodyRepository,
        _health = healthRepository,
        _preferences = preferencesRepository,
        _baselines = baselineCacheStore,
        _store = timelineStore,
        // ignore: prefer_initializing_formals
        _now = now;

  final HeartRepository _heart;
  final SleepRepository _sleep;
  final ActivityRepository _activity;
  final VitalsRepository _vitals;
  final BodyRepository _body;
  final HealthRepository _health;
  final PreferencesRepository _preferences;
  final BodyEnergyBaselineCacheStore _baselines;
  final BodyEnergyTimelineStore? _store;
  final DateTime Function() _now;

  static const int _baselineDays = 28;
  // Kotlin `CurrentDayCacheMinutes` / `PastDayCacheHours` / `BaselineCacheHours`.
  static const int _currentDayCacheMinutes = 15;
  static const int _pastDayCacheHours = 24;
  static const int _baselineCacheHours = 24;

  /// How far back a stored chain anchor is looked for. Pure SQLite — one query
  /// covers the whole window — so this bound costs nothing to raise.
  static const int _chainLookbackDays = 14;

  /// How many missing days the FOREGROUND load will recompute to close a gap.
  /// Deliberately far smaller than [_chainLookbackDays]: each day is ~8 Health
  /// Connect reads inside the shared [healthReadBudget]. Two days covers "I
  /// last opened the app the day before yesterday", which is the common gap;
  /// anything wider is [BodyEnergyChainSyncService]'s job.
  static const int _chainForegroundFillDays = 2;

  /// Sub-budget for the gap fill. A slow fill must degrade to a neutral seed,
  /// never fail the whole timeline load.
  static const Duration _chainFillBudget = Duration(seconds: 12);

  @override
  Future<Result<BodyEnergyTimelineResult>> loadTimeline(
    BodyEnergyTimelineQuery query,
  ) =>
      // Wrap in the shared read budget like loadVitalsPeriod/loadHeartPeriod: a
      // cold multi-day timeline is the read most likely to hang on a huge raw
      // heart-rate scan, and this was the one period load still unbounded.
      runCatching(() => _loadTimelineRaw(query).timeout(healthReadBudget));

  Future<BodyEnergyTimelineResult> _loadTimelineRaw(
    BodyEnergyTimelineQuery query,
  ) async {
    // Here rather than in the chain sync service, which is only kicked by the
    // Body Energy screen and a watch sync — the dashboard, the widgets and the
    // diagnostics all reach the model without it, so the reset silently never
    // ran for them. This is the one path every computation goes through.
    _resetGainsIfAlgorithmChanged();

    final context = _ChainContext(
      calibration: _preferences.bodyEnergyCalibrationListenable.value,
      bodyProfile: _preferences.bodyProfileListenable.value,
      permissionSignature: await _permissionSignature(),
    );

    // The day loop stays sequential and threads the previous day's freshly
    // computed end score forward, so only `period.start` pays the walk-back.
    // The within-day reads are what run concurrently (see _computeDay).
    final days = <BodyEnergyTimeline>[];
    var date = query.period.start;
    _ChainSeed? carried;
    while (!date.isAfter(query.period.end)) {
      final day = await _loadDay(
        date: date,
        refreshMode: query.refreshMode,
        context: context,
        seedOverride: carried,
      );
      days.add(day);
      carried = _ChainSeed.carried(day.currentScore);
      date = date.plusDays(1);
    }
    return BodyEnergyTimelineResult(query: query, days: days);
  }

  Future<BodyEnergyTimeline> _loadDay({
    required LocalDate date,
    required RefreshMode refreshMode,
    required _ChainContext context,
    _ChainSeed? seedOverride,
  }) async {
    final signature = _signatureFor(date, context);

    final cached = await _store?.load(date, signature);
    if (cached != null &&
        refreshMode == RefreshMode.normal &&
        _cacheIsUsable(cached) &&
        !_timelineIsStale(cached, date)) {
      return cached;
    }

    // A forced refresh applies to the requested day only — the chain fill below
    // always uses the normal staleness rules. Recomputing a fortnight of days
    // because the user pulled to refresh is exactly the runaway the fill bound
    // exists to prevent.
    final seed = seedOverride ?? await _resolveSeed(date, context);
    return _computeDay(
      date: date,
      context: context,
      seed: seed,
      // Only a past day can invalidate days after it; today has none.
      rippleForward: date.isBefore(LocalDate.fromDateTime(_now())),
    );
  }

  /// The score [date] opens on: the previous day's end, chained.
  ///
  /// Costs ONE SQLite query on the warm path — yesterday stored and valid — and
  /// no Health Connect read at all. That is what the day-summary table buys.
  Future<_ChainSeed> _resolveSeed(
    LocalDate date,
    _ChainContext context,
  ) async {
    final store = _store;
    if (store == null) return _seedFromMirror(date);

    final window = await store.storedDaysBetween(
      date.minusDays(_chainLookbackDays),
      date.minusDays(1),
    );
    final byEpochDay = {for (final day in window) day.date.epochDay: day};

    // The newest stored day strictly before `date` whose CHAIN signature still
    // validates against ITS OWN date's. Deliberately not the full signature: a
    // row computed under gains the learner has since nudged is still an honest
    // carry-over, and rejecting it strands the day on the neutral 50.
    BodyEnergyStoredDay? anchor;
    for (var back = 1; back <= _chainLookbackDays; back++) {
      final candidate = date.minusDays(back);
      final stored = byEpochDay[candidate.epochDay];
      if (stored != null &&
          _chainPartOf(stored.signature) ==
              _chainSignatureFor(candidate, context)) {
        anchor = stored;
        break;
      }
    }

    // Nothing stored in the window. Seeding neutral is correct here: there is
    // no previous day to be continuous with. The warm service builds history so
    // the next open is chained.
    if (anchor == null) return const _ChainSeed.neutral();

    final gap = date.epochDay - anchor.date.epochDay - 1;
    if (gap == 0) return _ChainSeed.carried(anchor.endScore);

    if (gap > _chainForegroundFillDays) {
      // Too wide to close inside the read budget. Carrying a score from over a
      // week ago through a field the screen labels as the previous day's would
      // be a worse lie than an honest reset, so the day starts neutral and says
      // so. The warm service closes the gap for next time.
      return const _ChainSeed.chainGap();
    }

    // Close the gap forward, oldest first, persisting each day so the next open
    // is warm. No forward ripple inside the fill: the days after each one are
    // exactly the days being written next.
    try {
      return await _fillGap(
        from: anchor.date.plusDays(1),
        until: date,
        seed: _ChainSeed.carried(anchor.endScore),
        context: context,
      ).timeout(_chainFillBudget);
    } catch (_) {
      // A gap day that times out or fails must not fail the day the user asked
      // for; it just leaves the chain broken until the warm service retries.
      return const _ChainSeed.chainGap();
    }
  }

  /// Computes and persists `[from, until)` so the requested day has a stored
  /// predecessor, returning the seed it should open on.
  Future<_ChainSeed> _fillGap({
    required LocalDate from,
    required LocalDate until,
    required _ChainSeed seed,
    required _ChainContext context,
  }) async {
    var carried = seed;
    var date = from;
    while (date.isBefore(until)) {
      final day = await _computeDay(
        date: date,
        context: context,
        seed: carried,
        rippleForward: false,
      );
      carried = _ChainSeed.carried(day.currentScore);
      date = date.plusDays(1);
    }
    return carried;
  }

  /// Returns the learned gains to neutral once, when the algorithm they were
  /// fitted against has been replaced.
  ///
  /// A gain is a multiplier on a component, so it only means anything relative
  /// to the model that produced the errors it came from. v8 added a waking-rest
  /// charge and gave recovery-debt drain a gain, so a `sleepChargeGain` of 0.80
  /// fitted under v7 now suppresses exactly the charge v8 introduced. Keeping it
  /// would leave the model fighting its own correction while the watch fit
  /// crawled back at 0.1 per observation.
  ///
  /// Only the four multipliers and the feel-check count reset — manual heart
  /// zones, the body profile and the feel-check log are untouched.
  ///
  /// The watch fit watermark rewinds with them, and must. It records how far the
  /// watch evidence has already been consumed, so leaving it ahead while the
  /// gains go back to 1.0 means the model is told to relearn and then denied
  /// everything it would relearn from: only the handful of buckets synced since
  /// the bump are still readable. Measured after the v10 bump — 5472 stored
  /// samples, roughly a hundred pairable hours, and a watch observation count of
  /// 2. Rewinding costs nothing because the samples are already in drift; the
  /// next fit re-reads its own lookback window and refits against the new model.
  void _resetGainsIfAlgorithmChanged() {
    _rewindWatchFitIfEpochChanged();
    if (_preferences.bodyEnergyGainsAlgorithmVersion ==
        bodyEnergyTimelineAlgorithmVersion) {
      return;
    }
    final current = _preferences.bodyEnergyCalibrationListenable.value;
    if (current.hasPersonalGains) {
      _preferences.setBodyEnergyCalibration(
        current.copyWith(
          sleepChargeGain: 1.0,
          activityDrainGain: 1.0,
          basalDrainGain: 1.0,
          stressDrainGain: 1.0,
          watchObservationCount: 0,
        ),
      );
    }
    _preferences.bodyEnergyGainsAlgorithmVersion =
        bodyEnergyTimelineAlgorithmVersion;
  }

  /// Rewinds the watch fit watermark once per [bodyEnergyWatchFitEpoch].
  ///
  /// The rewind used to hang off the algorithm-version reset, which made it
  /// dead code on every install that already had the current version recorded —
  /// which is all of them, since the fit bug did not change the algorithm. The
  /// device report proved it: 6030 stored samples, roughly a hundred pairable
  /// hours, and a watch observation count still stuck at 2 after the fix
  /// shipped, with every gain at exactly 1.00.
  ///
  /// An epoch of its own says what is actually meant — "the fit machinery
  /// changed, re-read the evidence" — without claiming a model change or
  /// discarding the stored chain the way an algorithm bump would.
  void _rewindWatchFitIfEpochChanged() {
    if (_preferences.bodyEnergyWatchFitEpoch == bodyEnergyWatchFitEpoch) return;
    _preferences.bodyEnergyWatchFitWatermarkMillis = 0;
    _preferences.bodyEnergyWatchFitEpoch = bodyEnergyWatchFitEpoch;
  }

  /// The alarm-isolate fallback: the mirrored end score, accepted only when it
  /// belongs to the day immediately before [date].
  _ChainSeed _seedFromMirror(LocalDate date) {
    final encoded = _preferences.bodyEnergyChainSeedMirror;
    if (encoded == null) return const _ChainSeed.neutral();
    final parts = encoded.split('|');
    if (parts.length != 2) return const _ChainSeed.neutral();
    final epochDay = int.tryParse(parts[0]);
    final endScore = int.tryParse(parts[1]);
    if (epochDay == null || endScore == null) return const _ChainSeed.neutral();
    return epochDay == date.minusDays(1).epochDay
        ? _ChainSeed.carried(endScore)
        : const _ChainSeed.neutral();
  }

  Future<BodyEnergyTimeline> _computeDay({
    required LocalDate date,
    required _ChainContext context,
    required _ChainSeed seed,
    required bool rippleForward,
  }) async {
    final signature = _signatureFor(date, context);
    final dayStart = localDayStart(date);
    final dayEnd = localDayEnd(date);
    final baselineStart = date.minusDays(_baselineDays);
    final baselineEnd = date.minusDays(1);

    // Independent reads run concurrently: start every future, then await. This
    // is the within-repo parallelism the heart/vitals halves already have —
    // body energy was the one day load still issuing ~8 reads one after the
    // next. The baseline runs alongside them; only respiratory depends on it.
    final baselinesF = _loadBaselines(
      date: date,
      baselineStart: baselineStart,
      baselineEnd: baselineEnd,
      dayStart: dayStart,
      signature: _baselineSignature(context.permissionSignature),
    );
    final heartRateF = _heart.loadRawHeartRateSamplesForDayGraph(date);
    final hrvF = _heart.loadHrvSamples(dayStart, dayEnd);
    final sleepF = _sleep.loadSleepSessions(date.minusDays(1), date);
    final workoutsF = _activity.loadWorkouts(date, date);
    // Hourly steps + active calories, and the basal rate — the energy-balance
    // inputs the heart-rate-zone model alone was missing.
    final activityProgressF = _activity.loadActivityProgress(date: date);
    final basalMetabolicRateF = _body.loadLatestBMR();
    final restingHrF = _heart.loadRestingHeartRate(date);

    final baselines = await baselinesF;
    // Kotlin loads respiratory only when a respiratory baseline exists (the
    // stress factor is inert without one), so it can only start after the
    // baseline resolves.
    final respiratoryF = baselines.respiratoryRateBaseline != null
        ? _vitals.loadRespiratoryRate(date, date)
        : null;

    final heartRateSamples = (await heartRateF).orThrow();
    final hrvSamples = (await hrvF).orThrow();
    final sleepSessions = (await sleepF).orThrow();
    final workouts = (await workoutsF).orThrow();
    final activityProgress = (await activityProgressF).orThrow();
    final basalMetabolicRate = (await basalMetabolicRateF).orThrow();
    final restingHr = (await restingHrF).orThrow();
    final List<RespiratoryRateEntry> respiratory = respiratoryF != null
        ? (await respiratoryF).orThrow()
        : const <RespiratoryRateEntry>[];

    final timeline = calculateBodyEnergyTimeline(
      BodyEnergyTimelineInputs(
        date: date,
        heartRateSamples: heartRateSamples,
        hrvSamples: hrvSamples,
        sleepSessions: sleepSessions,
        workouts: workouts,
        respiratoryRateSamples: respiratory,
        activityProgress: activityProgress,
        basalMetabolicRateKcalPerDay: basalMetabolicRate,
        restingHeartRateBpm: restingHr,
        baselineRestingHeartRateBpm: baselines.baselineRestingHeartRateBpm,
        observedMaxHeartRateBpm: baselines.observedMaxHeartRateBpm,
        hrvBaselineRmssdMs: baselines.hrvBaselineRmssdMs,
        respiratoryRateBaseline: baselines.respiratoryRateBaseline,
        previousEndScore: seed.score,
        seedSource: seed.source,
        calibration: context.calibration,
        bodyProfile: context.bodyProfile,
      ),
    ).copyWith(signature: signature, generatedAt: _now());

    final store = _store;
    if (store != null) {
      // A recompute that found nothing must not replace a day we already have.
      // Without the (user-optional, dialog-ungrantable) history grant Health
      // Connect serves only ~30 days, so an old day can come back empty purely
      // because its data is out of reach — and `save` deletes that day's
      // buckets before writing, so the stored timeline would be the thing lost.
      // Skip the ripple too: nothing downstream changed, because nothing here
      // did.
      if (timeline.points.isEmpty && await store.hasStoredPoints(date)) {
        final kept = await store.load(date, signature);
        if (kept != null) return kept;
        return timeline;
      }

      final stored =
          rippleForward ? await store.storedDaysBetween(date, date) : const [];
      final previousEnd = stored.isEmpty ? null : stored.first.endScore;
      await store.save(timeline);
      // Ripple only when the end score actually moved. A routine recompute that
      // lands on the same number changes nothing downstream, and wiping a week
      // of stored days for a no-op would guarantee a chain gap on the next open.
      if (rippleForward &&
          previousEnd != null &&
          previousEnd != timeline.currentScore) {
        await store.invalidateForward(
          date.plusDays(1),
          LocalDate.fromDateTime(_now()),
        );
      }
    }
    _writeSeedMirror(timeline);
    return timeline;
  }

  /// Mirrors the newest completed day's end score for the alarm isolate. Only
  /// moves forward, so an old day being backfilled cannot overwrite it.
  void _writeSeedMirror(BodyEnergyTimeline timeline) {
    final today = LocalDate.fromDateTime(_now());
    if (!timeline.date.isBefore(today)) return;
    final existing = _preferences.bodyEnergyChainSeedMirror;
    final existingEpochDay =
        existing == null ? null : int.tryParse(existing.split('|').first);
    if (existingEpochDay != null && existingEpochDay > timeline.date.epochDay) {
      return;
    }
    _preferences.bodyEnergyChainSeedMirror =
        '${timeline.date.epochDay}|${timeline.currentScore}';
  }

  /// Kotlin `loadBaselines`: reuse a fresh cached baseline (this day or an
  /// adjacent one), else recompute the 28-day medians + observed max and cache.
  Future<BodyEnergyBaselineCacheEntry> _loadBaselines({
    required LocalDate date,
    required LocalDate baselineStart,
    required LocalDate baselineEnd,
    required DateTime dayStart,
    required String signature,
  }) async {
    final reusable = _loadReusableBaseline(date, signature);
    if (reusable != null && !_baselineIsStale(reusable)) return reusable;

    final baselineResting = DashboardAggregator.medianLongOrNull(
      (await _heart.loadDailyRestingHR(baselineStart, baselineEnd))
          .orThrow()
          .map((e) => e.bpm)
          .where((v) => v > 0)
          .toList(),
    );
    final baselineHrv = DashboardAggregator.medianDoubleValuesOrNull(
      (await _heart.loadDailyHRV(baselineStart, baselineEnd))
          .orThrow()
          .map((e) => e.rmssdMs)
          .where((v) => v > 0)
          .toList(),
    );
    // Observed max is taken over the whole baseline window (Kotlin), not just
    // the current day's samples.
    final baselineSamples = (await _heart.loadHeartRateSamplesInstant(
      localDayStart(baselineStart),
      dayStart,
    ))
        .orThrow();
    final observedMax = baselineSamples.isEmpty
        ? null
        : baselineSamples
            .map((s) => s.beatsPerMinute)
            .reduce((a, b) => math.max(a, b));

    final baseline = BodyEnergyBaselineCacheEntry(
      baselineRestingHeartRateBpm: baselineResting?.round(),
      observedMaxHeartRateBpm: observedMax,
      hrvBaselineRmssdMs: baselineHrv,
      respiratoryRateBaseline: reusable?.respiratoryRateBaseline,
      generatedAt: _now(),
    );
    await _baselines.saveBaseline(date, signature, baseline);
    return baseline;
  }

  BodyEnergyBaselineCacheEntry? _loadReusableBaseline(
    LocalDate date,
    String signature,
  ) {
    final exact = _baselines.loadBaseline(date, signature);
    if (exact != null && !_baselineIsStale(exact)) return exact;

    for (final adjacentDate in [date.minusDays(1), date.plusDays(1)]) {
      final adjacent = _baselines.loadBaseline(adjacentDate, signature);
      if (adjacent != null && !_baselineIsStale(adjacent)) {
        unawaited(_baselines.saveBaseline(date, signature, adjacent));
        return adjacent;
      }
    }
    return null;
  }

  Future<int> _permissionSignature() async {
    try {
      if (_health.availability() != HealthConnectAvailability.available) {
        return 0;
      }
      final granted = (await _health.grantedPermissions()).orThrow().toList()
        ..sort();
      return granted.join(',').hashCode;
    } catch (_) {
      return 0;
    }
  }

  /// The signature a row for [date] is stored under: the chain part, plus the
  /// learned gains the row was actually computed with.
  ///
  /// Always computed from the row's OWN date. The body profile's signature
  /// varies by date (its age gate is relative to the day being asked about), so
  /// validating yesterday's row against today's signature — what the original
  /// seed lookup did — silently breaks the chain across a birthday.
  String _signatureFor(LocalDate date, _ChainContext context) =>
      '${_chainSignatureFor(date, context)}'
      '|${context.calibration.gainSignature().hashCode}';

  /// Everything a CARRY-OVER SCORE depends on, with the learned gains left out.
  ///
  /// A seed is one number from the previous day, not a timeline, and it has to
  /// survive the watch fit nudging a gain by a fraction of a percent. Folding
  /// the gains in meant every observation the learner absorbed invalidated all
  /// fourteen stored days at once, so the next load found no valid predecessor
  /// and fell back to the neutral 50 — turning a sub-percent model change into a
  /// visible 40-point jump, which is the exact discontinuity the chain exists to
  /// remove. Observed on 2026-07-26: one fit had moved `stressDrainGain` to
  /// 1.04, and the day opened at 50 with the previous day sitting at 0.
  ///
  /// Serving a cached timeline still requires the full signature. This is only
  /// for deciding whether a stored day is a legitimate ANCHOR to continue from.
  String _chainSignatureFor(LocalDate date, _ChainContext context) {
    final combined = '${context.calibration.zoneSignature()}'
        '|${context.bodyProfile.signature(today: date)}';
    return 'v$bodyEnergyTimelineAlgorithmVersion'
        '|${combined.hashCode}|${context.permissionSignature}';
  }

  /// The chain part of a stored signature — everything before the gain hash.
  static String _chainPartOf(String signature) {
    final cut = signature.lastIndexOf('|');
    return cut < 0 ? signature : signature.substring(0, cut);
  }

  String _baselineSignature(int permissionSignature) =>
      'v$bodyEnergyTimelineAlgorithmVersion|baseline|$permissionSignature';

  /// Whether [timeline] should be recomputed rather than served.
  ///
  /// Three tiers. Today is volatile and re-reads every 15 minutes. A day inside
  /// [bodyEnergyChainSettlingDays] can still gain late-arriving watch data, so
  /// it re-reads daily. A settled day never does: nothing new will arrive for
  /// it, and recomputing would spend ~8 Health Connect reads to reproduce what
  /// is already stored — which is what made the whole bucket table write-only,
  /// since retention keeps 120 days but nothing read one older than a day.
  ///
  /// "Never stale" is not "never updated": [BodyEnergyTimelineStore.load] still
  /// requires a signature match, so a calibration edit, a permission change or
  /// an algorithm-version bump all rebuild a settled day, and
  /// [RefreshMode.force] bypasses this entirely.
  bool _timelineIsStale(BodyEnergyTimeline timeline, LocalDate date) {
    final now = _now();
    final generatedAt = timeline.generatedAt ?? now;
    final age = now.difference(generatedAt);
    final today = LocalDate.fromDateTime(now);
    if (date == today) return age.inMinutes >= _currentDayCacheMinutes;
    final daysOld = today.epochDay - date.epochDay;
    if (daysOld > bodyEnergyChainSettlingDays) return false;
    return age.inHours >= _pastDayCacheHours;
  }

  /// Whether a cached timeline still has what it claims.
  ///
  /// Retention purges buckets past [bodyEnergyBucketRetentionDays] but keeps the
  /// summary row, so such a day still carries a score and a confidence with
  /// nothing to draw. Serving it would put a headline above a blank chart — a
  /// hole that only stayed hidden while every old day was recomputed anyway. A
  /// genuinely data-less day is the exception: `noData` with no points is the
  /// whole truth about it.
  bool _cacheIsUsable(BodyEnergyTimeline cached) =>
      cached.points.isNotEmpty ||
      cached.confidence == BodyEnergyConfidence.noData;

  bool _baselineIsStale(BodyEnergyBaselineCacheEntry baseline) =>
      _now().difference(baseline.generatedAt).inHours >= _baselineCacheHours;
}

/// The per-load inputs every day in a chain walk shares. Only the date varies,
/// which is what lets [BodyEnergyRepositoryImpl._signatureFor] be called for an
/// arbitrary day rather than just the requested one.
class _ChainContext {
  const _ChainContext({
    required this.calibration,
    required this.bodyProfile,
    required this.permissionSignature,
  });

  final BodyEnergyCalibration calibration;
  final BodyProfile bodyProfile;
  final int permissionSignature;
}

/// A resolved starting score, and where it came from.
class _ChainSeed {
  const _ChainSeed.neutral()
      : score = null,
        source = BodyEnergySeedSource.neutral;

  const _ChainSeed.chainGap()
      : score = null,
        source = BodyEnergySeedSource.chainGap;

  const _ChainSeed.carried(int this.score)
      : source = BodyEnergySeedSource.carriedOver;

  /// The previous day's end score, or null when there is nothing to carry — in
  /// which case the calculator falls back to [bodyEnergyNeutralStartScore].
  final int? score;
  final BodyEnergySeedSource source;
}
