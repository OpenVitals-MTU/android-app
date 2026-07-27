import 'dart:async';

import 'package:clock/clock.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../domain/refresh/data_change_sink.dart';
import '../domain/refresh/data_domain.dart';

part 'refresh_coordinator.freezed.dart';

/// Why the app is being told to re-read.
///
/// Pull-to-refresh is deliberately absent: a manual refresh changes no data, so
/// no OTHER screen became stale by it. It stays a direct call on the screen's
/// own view-model, and broadcasting it would fan one gesture out into every
/// mounted screen's read.
enum RefreshReason {
  /// The app returned to the foreground. Everything may have moved underneath
  /// it — another app's Health Connect write, a home-screen widget tap, the day
  /// rolling over — so every domain is implicated.
  appOpen,

  /// This app wrote health data. Only the written domains and what derives from
  /// them are implicated.
  dataChanged,
}

/// Trailing debounce for write signals.
///
/// A bulk import writes hundreds of records one awaited call at a time; without
/// this, each one is a signal and each signal is a screen reload.
const Duration kDataChangeDebounce = Duration(milliseconds: 400);

/// The longest a continuous write burst may hold a signal back. A long import
/// would otherwise never reach the debounce's quiet window and the UI would sit
/// stale until it finished.
const Duration kDataChangeMaxWait = Duration(seconds: 2);

/// The app's "something changed, re-read" broadcast.
///
/// Immutable value state: a monotonically increasing [revision] plus the set of
/// [domains] implicated by the newest signal. Listeners compare against their
/// own interest with [touches]; the revision is what makes two consecutive
/// signals for the same domain set distinguishable.
@freezed
abstract class RefreshSignal with _$RefreshSignal {
  const factory RefreshSignal({
    @Default(0) int revision,
    @Default(<DataDomain>{}) Set<DataDomain> domains,
    @Default(RefreshReason.appOpen) RefreshReason reason,
  }) = _RefreshSignal;

  const RefreshSignal._();

  /// Whether a screen reading [interest] should act on this signal.
  ///
  /// The `revision > 0` guard is what stops the initial state from reading as a
  /// signal: a listener attached at startup must not fire before anything has
  /// happened (the cold-start load is the view-model's own job).
  bool touches(Set<DataDomain> interest) =>
      revision > 0 && domains.any(interest.contains);
}

/// The single place the three refresh triggers converge.
///
/// Trigger 1 (app open) calls [appOpened] from [DataRefreshBootstrap]; trigger 3
/// (insert/update/delete) reaches [changed] from the repository boundary, via
/// [DataChangeSink]. Trigger 2 (pull-to-refresh) does not pass through here at
/// all — see [RefreshReason].
class RefreshCoordinator extends Notifier<RefreshSignal>
    implements DataChangeSink {
  Timer? _debounce;
  DateTime? _burstStartedAt;
  final Set<DataDomain> _pendingDomains = <DataDomain>{};

  @override
  RefreshSignal build() {
    ref.onDispose(_cancelDebounce);
    return const RefreshSignal();
  }

  /// The app returned to the foreground. Emitted immediately — the user is
  /// looking at the screen right now, and there is no burst to coalesce.
  void appOpened() {
    _cancelDebounce();
    _emit(DataDomain.values.toSet(), RefreshReason.appOpen);
  }

  /// A repository wrote [domains]. Debounced, and expanded through
  /// [kDerivedDomains] so a hydration write also wakes the nutrition screen.
  ///
  /// The debounce is not only a coalescer. [changed] is called from inside a
  /// repository's `runCatching`, and a synchronous `state =` there would let a
  /// listener's exception surface as a *write failure* — driving a retry that
  /// duplicates a health record. Deferring the assignment onto a timer makes
  /// that structurally impossible. Do not "optimise" it into a direct emit.
  @override
  void changed(Set<DataDomain> domains) {
    if (domains.isEmpty) return;
    _pendingDomains.addAll(expandDomains(domains));
    // A burst longer than the max wait flushes on its own rather than holding
    // the UI stale until the last write lands.
    final startedAt = _burstStartedAt ??= clock.now();
    final waited = clock.now().difference(startedAt);
    if (waited >= kDataChangeMaxWait) {
      _flush();
      return;
    }
    final remaining = kDataChangeMaxWait - waited;
    _debounce?.cancel();
    _debounce = Timer(
      remaining < kDataChangeDebounce ? remaining : kDataChangeDebounce,
      _flush,
    );
  }

  void _flush() {
    _debounce?.cancel();
    _debounce = null;
    _burstStartedAt = null;
    if (_pendingDomains.isEmpty) return;
    final domains = <DataDomain>{..._pendingDomains};
    _pendingDomains.clear();
    _emit(domains, RefreshReason.dataChanged);
  }

  void _emit(Set<DataDomain> domains, RefreshReason reason) {
    state = RefreshSignal(
      revision: state.revision + 1,
      domains: domains,
      reason: reason,
    );
  }

  void _cancelDebounce() {
    _debounce?.cancel();
    _debounce = null;
    _burstStartedAt = null;
    _pendingDomains.clear();
  }
}

/// The read side: screens listen here.
final refreshCoordinatorProvider =
    NotifierProvider<RefreshCoordinator, RefreshSignal>(RefreshCoordinator.new);

/// The write side, as the data layer sees it — a bare [DataChangeSink] with no
/// hint that Riverpod is on the other end.
final dataChangeSinkProvider = Provider<DataChangeSink>(
  (ref) => ref.watch(refreshCoordinatorProvider.notifier),
);
