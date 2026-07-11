import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/presentation/unit_formatter.dart';
import '../../di/providers.dart';
import '../../l10n/app_localizations.dart';
import '../../navigation/app_routes.dart';
import '../../state/app_providers.dart';
import '../imports/pending_route_import.dart';
import 'activity/activity_entry_clock.dart';
import 'activity/activity_entry_form.dart';
import 'activity/activity_entry_notifier.dart';
import 'activity/activity_entry_providers.dart';
import 'activity/activity_entry_source_card.dart';
import 'activity/activity_entry_state.dart';
import 'activity/recording/activity_recording_device_support.dart';
import 'activity/recording/activity_recording_screen.dart';
import 'activity/recording/activity_recording_setup_screen.dart';
import 'activity/activity_plan_picker_cards.dart';
import 'activity/recording/activity_recording.dart';
import 'manual_entry_form_scaffold.dart';

/// Activity manual-entry screen. Riverpod + Flutter port of the Kotlin
/// `ActivityEntryScreen` / `ActivityEntryFormContent`. Backs the new-entry route
/// (optional [mode] / [planId] / [activityTypeId] intents) and the edit route
/// ([activityEntryId]).
class ActivityEntryScreen extends ConsumerStatefulWidget {
  const ActivityEntryScreen({
    super.key,
    this.mode,
    this.planId,
    this.activityTypeId,
    this.activityEntryId,
  });

  final ActivityEntryMode? mode;
  final String? planId;
  final String? activityTypeId;
  final String? activityEntryId;

  @override
  ConsumerState<ActivityEntryScreen> createState() =>
      _ActivityEntryScreenState();
}

class _ActivityEntryScreenState extends ConsumerState<ActivityEntryScreen>
    with RefreshPermissionOnResume {
  late final ActivityEntryController _controller;
  final _controllers = ActivityEntryTextControllers();

  @override
  void refreshPermission() => _controller.refreshPermission();

  @override
  void initState() {
    super.initState();
    _controller = ActivityEntryController(
      repository: ref.read(activityRepositoryProvider),
      heartRepository: ref.read(heartRepositoryProvider),
      routeFileImporter: ref.read(routeFileImporterProvider),
      activityRecorder: ref.read(activityRecordingControllerProvider),
      recordingDraftStore: ref.read(activityRecordingDraftStoreProvider),
      preferencesRepository: ref.read(preferencesRepositoryProvider),
      markerRepository: ref.read(activityMarkerRepositoryProvider),
      clock: ActivityEntryClock.system(),
      editActivityId: widget.activityEntryId,
      launchMode: widget.mode?.value,
      launchPlanId: widget.planId,
      launchActivityTypeId: widget.activityTypeId,
    );
    _controller.uiState.addListener(_onStateChanged);
    _controllers.syncFrom(_controller.value);
    if (widget.activityEntryId != null) {
      _controller.loadEditEntry(ref.read(unitSystemProvider));
    }
    // A route/FIT file picked in the Settings "Data import" cards is handed off
    // here for review, the Dart analogue of Kotlin's `ExternalRouteImportRequest`
    // consumption in `ActivityEntryScreen`. Deferred past the build phase
    // (Riverpod forbids mutating a provider during initState) and consumed
    // exactly once via `take()`.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final pendingRoute = ref.read(pendingRouteImportProvider.notifier).take();
      if (pendingRoute != null) {
        _controller.importRouteFile(pendingRoute, ref.read(unitSystemProvider));
      }
    });
  }

  @override
  void dispose() {
    _controller.uiState.removeListener(_onStateChanged);
    _controller.dispose();
    _controllers.dispose();
    super.dispose();
  }

  void _onStateChanged() {
    final state = _controller.value;
    _controllers.syncFrom(state);
    if (!state.saveCompleted || !mounted) return;
    _controller.onSaveCompletedHandled();
    // The write already landed; leave the entry route as the Kotlin
    // `onEntrySaved` does.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) Navigator.of(context).maybePop();
    });
  }

  /// Kotlin `performSourceActionAfterPermission`: every source action is gated
  /// on the Health Connect write permission, and only runs once it is granted.
  Future<void> _onSourceAction(ActivityEntrySourceAction action) async {
    if (!_controller.value.canWrite) {
      await ref
          .read(healthRepositoryProvider)
          .requestPermissions(_controller.value.writePermissions);
      _controller.refreshPermission();
      if (!mounted || !_controller.value.canWrite) return;
    }
    await _performSourceAction(action);
  }

  Future<void> _performSourceAction(ActivityEntrySourceAction action) async {
    switch (action) {
      case ActivityEntrySourceAction.manual:
        _controller.startManualEntry();
      case ActivityEntrySourceAction.existingPlan:
        _controller.startFromExistingPlan();
      case ActivityEntrySourceAction.recordGps:
        // Kotlin asks for POST_NOTIFICATIONS before opening the recorder: the
        // session runs in a foreground service, which cannot start without it.
        final support = ref.read(activityRecordingDeviceSupportProvider);
        if (!await support.hasNotificationPermission() &&
            !await support.requestNotificationPermission()) {
          _controller.reportNotificationPermissionNeeded();
          return;
        }
        _controller.prepareGpsRecording();
    }
  }

  Future<void> _requestWritePermission() async {
    await ref
        .read(healthRepositoryProvider)
        .requestPermissions(_controller.value.writePermissions);
    _controller.refreshPermission();
  }

  /// Kotlin `requestGpsLocationPermissions`: report the need only if refused.
  Future<void> _requestLocationPermission() async {
    final support = ref.read(activityRecordingDeviceSupportProvider);
    if (await support.requestPreciseLocationPermission()) {
      // The setup screen's fix stream keys off the permission, so restart it.
      ref.invalidate(preRecordingGpsFixProvider);
      return;
    }
    _controller.reportLocationPermissionNeeded();
  }

  /// Kotlin `requestActivityRecognitionPermission`.
  Future<void> _requestActivityRecognitionPermission() async {
    final support = ref.read(activityRecordingDeviceSupportProvider);
    if (await support.requestActivityRecognitionPermission()) {
      ref.invalidate(recordingSensorReadinessProvider);
      _controller.openRecordingDashboard();
      return;
    }
    _controller.reportActivityRecognitionPermissionNeeded();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final formatter = ref.watch(unitFormatterProvider);

    return ValueListenableBuilder<ActivityEntryUiState>(
      valueListenable: _controller.uiState,
      builder: (context, state, _) {
        return Scaffold(
          appBar: AppBar(title: Text(l10n.manualEntryActivityTitle)),
          body: SafeArea(child: _body(state, formatter)),
        );
      },
    );
  }

  /// The live recording dashboard REPLACES the screen — Kotlin swaps the form out
  /// for `ActivityEntryRecordingContent` at `fillMaxSize` — so it is the one thing
  /// here that must NOT go inside the form's scroll view.
  ///
  /// It lays out with [Expanded] (which a scrollable's unbounded height forbids)
  /// and it scrolls itself, so being a ListView child forced it to be boxed at a
  /// fraction of the screen instead. That fraction WAS the empty space: a dead band
  /// below the controls and the form's own padding above the dashboard, both of
  /// them glaring once outdoor mode paints the background pure black.
  ///
  /// Everything else on this screen is a card and still belongs in the scroll view.
  Widget _body(ActivityEntryUiState state, UnitFormatter formatter) {
    final recorder = _controller.activityRecorder;
    if (state.mode != ActivityEntryFormMode.recording || recorder == null) {
      return _scrollableForm(_content(state, formatter));
    }
    return ValueListenableBuilder<ActivityRecordingState>(
      valueListenable: recorder.state,
      builder: (context, recordingState, _) {
        final content = _ActivityEntryRecordingContent(
          controller: _controller,
          state: state,
          recordingState: recordingState,
          unitFormatter: formatter,
          onRequestLocationPermission: _requestLocationPermission,
          onRequestActivityRecognitionPermission:
              _requestActivityRecognitionPermission,
          onRequestWritePermission: _requestWritePermission,
        );
        // The setup card is a card like any other and keeps the padded scroll view.
        return isRecordingDashboardVisible(recordingState)
            ? content
            : _scrollableForm(content);
      },
    );
  }

  Widget _scrollableForm(Widget child) => ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: child,
          ),
        ],
      );

  /// Kotlin `ActivityEntryFormContent`'s mode switch.
  Widget _content(ActivityEntryUiState state, UnitFormatter formatter) {
    switch (state.mode) {
      case ActivityEntryFormMode.recording:
        // Handled by _body, which can see the recorder state that decides whether
        // this is the padded setup card or the full-bleed dashboard. Reachable
        // only with no recorder attached, in which case there is nothing to show.
        return const SizedBox.shrink();
      case ActivityEntryFormMode.chooseSource:
        return ActivityEntrySourceCard(
          state: state,
          onSourceAction: _onSourceAction,
          onRequestWritePermission: _requestWritePermission,
        );
      case ActivityEntryFormMode.planActivityPicker:
        return ActivityPlanActivityPickerCard(
          state: state,
          onSelectActivity: _controller.selectPlannedWorkoutActivity,
          onChooseSource: _controller.chooseSource,
        );
      case ActivityEntryFormMode.planPicker:
        return ActivityPlanPickerCard(
          state: state,
          onSelectPlan: _controller.applyPlannedWorkout,
          onChooseActivity: _controller.choosePlannedWorkoutActivity,
        );
      case ActivityEntryFormMode.manual:
      case ActivityEntryFormMode.routeImport:
        return ActivityEntryCard(
          state: state,
          unitFormatter: formatter,
          controllers: _controllers,
          callbacks: _callbacks(),
        );
    }
  }

  ActivityEntryCardCallbacks _callbacks() {
    final unitSystem = ref.read(unitSystemProvider);
    return ActivityEntryCardCallbacks(
      onSelectActivityType: _controller.selectActivityType,
      onTitleChanged: _controller.updateTitle,
      onFeelingChanged: _controller.updateFeeling,
      onNotesChanged: _controller.updateNotes,
      onStartDateChanged: _controller.updateStartDate,
      onStartTimeChanged: _controller.updateStartTime,
      onDurationChanged: _controller.updateDurationMinutes,
      onRepetitionModeChanged: _controller.updateRepetitionMode,
      onRepetitionTotalChanged: _controller.updateRepetitionTotal,
      onRepetitionSetRepetitionsChanged:
          _controller.updateRepetitionSetRepetitions,
      onRepetitionSetRestChanged: _controller.updateRepetitionSetRest,
      onAddRepetitionSet: _controller.addRepetitionSet,
      onRemoveRepetitionSet: _controller.removeRepetitionSet,
      onCreateNewPlannedWorkout: _controller.createNewPlannedWorkout,
      onApplyPlannedWorkout: _controller.applyPlannedWorkout,
      onSavePlannedWorkout: () =>
          _controller.saveCurrentAsPlannedWorkout(unitSystem),
      onUpdatePlannedWorkout: () => _controller.saveCurrentAsPlannedWorkout(
        unitSystem,
        updateSelected: true,
      ),
      onDistanceChanged: _controller.updateDistance,
      onElevationChanged: _controller.updateElevation,
      onActiveCaloriesChanged: _controller.updateActiveCalories,
      onTotalCaloriesChanged: _controller.updateTotalCalories,
      onClearRoute: _controller.clearImportedRoute,
      onChooseSource: _controller.chooseSource,
      onRequestWritePermission: _requestWritePermission,
      onAddEntry: () => _controller.addEntry(unitSystem),
      onDiscardRecordingDraft: _controller.discardRecordingDraft,
    );
  }
}

/// Port of the Kotlin `ActivityEntryRecordingContent`: the setup card until the
/// recorder has an activity prepared, then the live recording screen.
class _ActivityEntryRecordingContent extends ConsumerWidget {
  const _ActivityEntryRecordingContent({
    required this.controller,
    required this.state,
    required this.recordingState,
    required this.unitFormatter,
    required this.onRequestLocationPermission,
    required this.onRequestActivityRecognitionPermission,
    required this.onRequestWritePermission,
  });

  final ActivityEntryController controller;
  final ActivityEntryUiState state;
  final ActivityRecordingState recordingState;
  final UnitFormatter unitFormatter;
  final VoidCallback onRequestLocationPermission;
  final VoidCallback onRequestActivityRecognitionPermission;
  final VoidCallback onRequestWritePermission;

  /// The recording runs in a foreground service, which cannot post its
  /// notification without POST_NOTIFICATIONS. Kotlin only asks on the way in
  /// from the source card, so a session launched from the dashboard's "Start
  /// workout" never gets asked; checking here covers both entry points.
  Future<void> _startRecording(
    WidgetRef ref,
    ActivityEntryController controller, {
    required ActivityRecordingInitialFix? initialFix,
    required int restSeconds,
  }) async {
    final support = ref.read(activityRecordingDeviceSupportProvider);
    if (!await support.hasNotificationPermission() &&
        !await support.requestNotificationPermission()) {
      controller.reportNotificationPermissionNeeded();
      return;
    }
    controller.startGpsRecording(
      initialFix: initialFix,
      repetitionRestSeconds: restSeconds,
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (!isRecordingDashboardVisible(recordingState)) {
      return ActivityRecordingSetupScreen(
        state: state,
        recordingState: recordingState,
        unitFormatter: unitFormatter,
        onSelectActivityType: controller.selectActivityType,
        // Kotlin's setup Start begins the session immediately, handing the
        // pre-start fix over as the first route point.
        onStartRecording: (initialFix, restSeconds) => _startRecording(
          ref,
          controller,
          initialFix: initialFix,
          restSeconds: restSeconds,
        ),
        onRequestLocationPermission: onRequestLocationPermission,
        onRequestActivityRecognitionPermission:
            onRequestActivityRecognitionPermission,
        onChooseSource: controller.chooseSource,
        onRequestWritePermission: onRequestWritePermission,
      );
    }

    return ActivityRecordingScreen(
      state: recordingState,
      unitFormatter: unitFormatter,
      onStartRecording: (initialFix) =>
          controller.startGpsRecording(initialFix: initialFix),
      onPauseRecording: controller.pauseGpsRecording,
      onResumeRecording: controller.resumeGpsRecording,
      onAddLap: controller.addRecordingLap,
      onAddMarker: controller.addRecordingMarker,
      onUpdateMarker: controller.updateRecordingMarker,
      onDeleteMarker: controller.deleteRecordingMarker,
      onUpdateDashboardLayout: controller.updateRecordingDashboardLayout,
      onChooseSource: controller.chooseSource,
      onAdjustRepetitionCount: controller.adjustRepetitionRecording,
      onEndRepetitionSet: controller.endRepetitionSet,
      onStartNextRepetitionSet: controller.startNextRepetitionSet,
      onFinishRecording: () =>
          controller.finishGpsRecording(ref.read(unitSystemProvider)),
      // Kotlin threads the theme-mode preference through so the outdoor
      // high-contrast theme can pick its light or dark scheme.
      appThemeMode: ref.watch(appThemeModeProvider),
    );
  }
}

/// Kotlin's `isRecordingDashboardVisible`: the dashboard replaces the setup card
/// once a session is live or an activity has been prepared.
bool isRecordingDashboardVisible(ActivityRecordingState state) =>
    state.isActive || state.activityTypeId != null;
