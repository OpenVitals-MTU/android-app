// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appName => 'OpenVitals';

  @override
  String get cdBack => 'Back';

  @override
  String get cdSettings => 'Settings';

  @override
  String get cdAchievements => 'Achievements';

  @override
  String get cdDailyReadiness => 'Daily Readiness';

  @override
  String get cdSensorBatteryStatus => 'Sensor battery status';

  @override
  String get cdEditDashboard => 'Edit summary';

  @override
  String get cdFinishDashboardEditing => 'Finish editing summary';

  @override
  String get cdEditSavedDrinks => 'Edit saved drinks';

  @override
  String get cdDoneEditingSavedDrinks => 'Done editing saved drinks';

  @override
  String get cdEditDrink => 'Edit drink';

  @override
  String get cdDeleteDrink => 'Delete drink';

  @override
  String get cdMoveDrinkCategory => 'Move drink category';

  @override
  String cdExpandDrinkCategory(String arg0) {
    return 'Expand $arg0';
  }

  @override
  String cdCollapseDrinkCategory(String arg0) {
    return 'Collapse $arg0';
  }

  @override
  String get cdEditManualEntryWidgets => 'Edit add entry widgets';

  @override
  String get cdFinishManualEntryEditing => 'Finish editing add entry widgets';

  @override
  String get cdEditRecordingDashboard => 'Edit recording dashboard';

  @override
  String get cdFinishRecordingDashboardEditing =>
      'Finish editing recording dashboard';

  @override
  String get cdMoveWidgetUp => 'Move widget up';

  @override
  String get cdMoveWidgetDown => 'Move widget down';

  @override
  String get cdEditMetricSections => 'Edit metric sections';

  @override
  String get cdFinishMetricSectionEditing => 'Finish editing metric sections';

  @override
  String get cdMoveSectionUp => 'Move section up';

  @override
  String get cdMoveSectionDown => 'Move section down';

  @override
  String get cdRemoveWidget => 'Remove widget';

  @override
  String get cdDecreaseRecordingDashboardWidgetSize => 'Make widget smaller';

  @override
  String get cdIncreaseRecordingDashboardWidgetSize => 'Make widget larger';

  @override
  String get cdExitRecordingFocusMode => 'Exit focus mode';

  @override
  String get cdToggleRecordingOutdoorMode => 'Toggle outdoor readability mode';

  @override
  String get cdRecenterMap => 'Recenter map';

  @override
  String get cdDeleteEntry => 'Delete entry';

  @override
  String get cdEditEntry => 'Edit entry';

  @override
  String get cdPreviousDay => 'Previous day';

  @override
  String get cdNextDay => 'Next day';

  @override
  String get cdPreviousPeriod => 'Previous period';

  @override
  String get cdNextPeriod => 'Next period';

  @override
  String get cdOpenCalendar => 'Open calendar';

  @override
  String get actionCancel => 'Cancel';

  @override
  String get actionAdd => 'Add';

  @override
  String get actionAddCustom => 'Add custom';

  @override
  String get actionSave => 'Save';

  @override
  String get actionClose => 'Close';

  @override
  String get actionContinue => 'Continue';

  @override
  String get actionDelete => 'Delete';

  @override
  String get actionDetails => 'Details';

  @override
  String get actionEdit => 'Edit';

  @override
  String get actionEnable => 'Enable';

  @override
  String get actionGetStarted => 'Get started';

  @override
  String get actionGrant => 'Grant';

  @override
  String get actionGrantPermission => 'Grant permission';

  @override
  String get actionLoadMoreEntries => 'Load 10 more';

  @override
  String get actionShowCalculation => 'Show calculation';

  @override
  String get actionHideCalculation => 'Hide calculation';

  @override
  String get actionNotNow => 'Not now';

  @override
  String get actionAccept => 'I accept';

  @override
  String get actionOpen => 'Open';

  @override
  String get actionPause => 'Pause';

  @override
  String get actionReview => 'Review';

  @override
  String get actionResume => 'Resume';

  @override
  String get actionRefresh => 'Refresh';

  @override
  String get actionSelect => 'Select';

  @override
  String get actionStart => 'Start';

  @override
  String get actionFinish => 'Finish';

  @override
  String get actionDiscard => 'Discard';

  @override
  String get unknownError => 'Unknown error';

  @override
  String get screenErrorNotFound => 'The requested item was not found.';

  @override
  String get screenErrorMissingArgument => 'Required information is missing.';

  @override
  String get screenErrorPermissionDenied =>
      'Permission is required to load this data.';

  @override
  String get screenErrorHealthConnectUnavailable =>
      'Health Connect is not available on this device.';

  @override
  String get screenErrorLoadSleepSession => 'Unable to load sleep session.';

  @override
  String get screenErrorLoadSleepPeriod => 'Unable to load sleep data.';

  @override
  String get notAvailable => 'Not available';

  @override
  String get notRecorded => 'Not recorded';

  @override
  String get noData => 'No data';

  @override
  String get loading => 'Loading...';

  @override
  String get homeMetricWidgetDescription => 'OpenVitals metric';

  @override
  String get homeMetricWidgetConfigTitle => 'Choose metric';

  @override
  String get homeMetricWidgetConfigPrompt =>
      'Choose the metric for the widget:';

  @override
  String get homeMetricWidgetNoMetrics => 'No metrics available.';

  @override
  String get homeMetricWidgetPermissionNeeded =>
      'Grant permission in OpenVitals';

  @override
  String get homeMetricWidgetUpdateFailed => 'Unable to update';

  @override
  String get homeMetricWidgetOpenForDetails => 'Open for details';

  @override
  String get homeMetricWidgetNotConfigured => 'Select a metric';

  @override
  String get homeQuickBeverageWidgetDescription => 'Quick beverage';

  @override
  String get homeQuickBeverageOneTapWidgetDescription => 'Quick beverage 1x1';

  @override
  String get homeQuickBeverageWidgetConfigTitle => 'Choose beverage';

  @override
  String get homeQuickBeverageWidgetConfigPrompt =>
      'Choose the beverage for the widget:';

  @override
  String get homeQuickBeverageWidgetNoDrinks => 'No beverages available.';

  @override
  String get homeQuickBeverageWidgetNotConfigured => 'Select a beverage';

  @override
  String get homeQuickBeverageWidgetTapToLog => 'Tap to log';

  @override
  String get homeQuickBeverageWidgetSaved => 'Saved now';

  @override
  String get homeQuickBeverageWidgetSavedNutrition => 'Saved as nutrition';

  @override
  String get homeDailyReadinessWidgetDescription =>
      'OpenVitals daily readiness';

  @override
  String get homeBodyEnergyWidgetDescription => 'OpenVitals body energy';

  @override
  String get homeTodayVitalsWidgetDescription => 'OpenVitals today vitals';

  @override
  String get homeWidgetTodayTitle => 'Today';

  @override
  String get homeWidgetContext => 'Context';

  @override
  String get homeWidgetHrvShort => 'HRV';

  @override
  String get homeWidgetBodyEnergyCharged => 'Charged';

  @override
  String get homeWidgetBodyEnergySteady => 'Steady';

  @override
  String get homeWidgetBodyEnergyLimited => 'Limited';

  @override
  String get homeWidgetBodyEnergyLow => 'Low';

  @override
  String get screenSteps => 'Steps';

  @override
  String get screenActivities => 'Activities';

  @override
  String get screenCalories => 'Calories';

  @override
  String get screenActivityDetail => 'Activity detail';

  @override
  String get screenSleep => 'Sleep';

  @override
  String get screenSleepDetail => 'Sleep detail';

  @override
  String get screenHeartVitals => 'Heart & Vitals';

  @override
  String get screenStressTracking => 'Stress Tracking';

  @override
  String get screenBodyEnergy => 'Body Energy';

  @override
  String get screenTrainingReadiness => 'Training Readiness';

  @override
  String get screenBody => 'Body';

  @override
  String get screenHydration => 'Beverages';

  @override
  String get screenNutrition => 'Nutrition';

  @override
  String get screenMindfulness => 'Mindfulness';

  @override
  String get screenCycle => 'Cycle';

  @override
  String get screenDailyReadiness => 'Daily Readiness';

  @override
  String get screenSettings => 'Settings';

  @override
  String get screenAchievements => 'Achievements';

  @override
  String get screenManualEntry => 'Add entry';

  @override
  String get screenHydrationEntry => 'Beverage entry';

  @override
  String get screenActivityEntry => 'Activity entry';

  @override
  String get screenMindfulnessEntry => 'Mindfulness entry';

  @override
  String get screenCarbsEntry => 'Carbs entry';

  @override
  String get screenBodyMeasurementEntry => 'Body measurement entry';

  @override
  String get screenVitalsMeasurementEntry => 'Vitals entry';

  @override
  String get bottomNavDashboard => 'Summary';

  @override
  String get manualEntryHydrationTitle => 'Beverages';

  @override
  String get manualEntryActivityTitle => 'Activity';

  @override
  String get manualEntryDateLabel => 'Entry date';

  @override
  String get manualEntryTimeLabel => 'Entry time';

  @override
  String get manualEntrySelectTime => 'Select entry time';

  @override
  String get manualEntryAddWidgets => 'Add entry widgets';

  @override
  String get manualEntryAllWidgetsAdded =>
      'All entry widgets are already shown.';

  @override
  String get manualEntryWritePermissionTitle => 'Beverage write permission';

  @override
  String get manualEntryActivityWritePermissionTitle =>
      'Activity write permissions';

  @override
  String get manualEntryMindfulnessWritePermissionTitle =>
      'Mindfulness write permission';

  @override
  String get manualEntryCarbsWritePermissionTitle => 'Carbs write permission';

  @override
  String manualEntryBodyWritePermissionTitle(String arg0) {
    return '$arg0 write permission';
  }

  @override
  String manualEntryVitalsWritePermissionTitle(String arg0) {
    return '$arg0 write permission';
  }

  @override
  String get mindfulnessEntrySubtitle =>
      'Mindfulness sessions are saved directly to Health Connect.';

  @override
  String get mindfulnessEntryPermissionNeeded =>
      'For the summary, OpenVitals only asks for view permissions. To add mindfulness entries, we need write permission. OpenVitals will not store these sessions; entries are saved in Health Connect.';

  @override
  String get activityEntrySubtitle =>
      'Create a Health Connect activity session. Imported route or activity details are written only when you save.';

  @override
  String get activityEntryPermissionNeeded =>
      'For the summary, OpenVitals only asks for view permissions. To add activities, we need Health Connect write permissions for sessions, routes, distance, elevation, calories, and recorded sensor metrics such as heart rate; treadmill entries ask for steps when needed. OpenVitals will not store these entries; they are saved in Health Connect.';

  @override
  String get activityEntrySourceBody =>
      'Create an activity manually, use an existing plan, or record a GPS route.';

  @override
  String get activityEntryCreateManual => 'Create manually';

  @override
  String get activityEntryCreateFromExistingPlan => 'Create from existing plan';

  @override
  String get activityEntryRecordGps => 'Record activity';

  @override
  String get activityEntryChooseAnotherSource => 'Choose another method';

  @override
  String get activityEntryTypeLabel => 'Activity type';

  @override
  String get activityEntryTitleLabel => 'Title';

  @override
  String get activityEntryStartDateLabel => 'Start date';

  @override
  String get activityEntryStartTimeLabel => 'Start time';

  @override
  String get activityEntrySelectTime => 'Select start time';

  @override
  String get activityEntryDurationLabel => 'Duration min';

  @override
  String get activityEntryRepetitionsTitle => 'Repetitions';

  @override
  String get activityEntryStepsTitle => 'Steps';

  @override
  String get activityEntryRepetitionModeTotal => 'Total';

  @override
  String get activityEntryRepetitionModeSets => 'Sets';

  @override
  String get activityEntryRepetitionsLabel => 'Reps';

  @override
  String get activityEntryStepsLabel => 'Steps';

  @override
  String activityEntrySetRepetitionsLabel(int arg0) {
    return 'Set $arg0 reps';
  }

  @override
  String get activityEntrySetRestLabel => 'Rest time';

  @override
  String get activityEntryAddSet => 'Add set';

  @override
  String get activityEntryTrainingPlansTitle => 'Training plans';

  @override
  String get activityEntryTrainingPlansLoading =>
      'Loading Health Connect plans';

  @override
  String get activityEntryTrainingPlansEmpty =>
      'No Health Connect plans for this date and activity type';

  @override
  String get activityEntryTrainingPlanLabel => 'Training plan';

  @override
  String get activityEntryTrainingPlanSelect => 'Select a plan';

  @override
  String get activityEntryTrainingPlanNew => 'New plan';

  @override
  String get activityEntryTrainingPlanUnnamed => 'Unnamed plan';

  @override
  String get activityEntrySaveTrainingPlan => 'Save plan';

  @override
  String get activityEntryUpdateTrainingPlan => 'Update plan';

  @override
  String get activityEntryPlanActivityPickerTitle => 'Activities with plans';

  @override
  String get activityEntryPlanActivityPickerEmpty =>
      'No Health Connect plans found';

  @override
  String get activityEntryPlanPickerTitle => 'Choose a plan';

  @override
  String get activityEntryPlanPickerEmpty => 'No plans found for this activity';

  @override
  String get activityEntryPlanChooseActivity => 'Choose another activity';

  @override
  String activityEntryPlanOneSetSummary(int arg0) {
    return '1 set • $arg0 reps';
  }

  @override
  String activityEntryPlanSummary(int arg0, int arg1) {
    return '$arg0 sets • $arg1 reps';
  }

  @override
  String activityEntryPlanPreviewReps(int arg0) {
    return '$arg0 reps';
  }

  @override
  String activityEntryPlanPreviewRest(int arg0) {
    return 'rest $arg0 sec';
  }

  @override
  String activityEntryPlanPreviewMore(int arg0) {
    return '+$arg0 more';
  }

  @override
  String activityEntryDistanceLabel(String arg0) {
    return 'Distance $arg0';
  }

  @override
  String activityEntryElevationLabel(String arg0) {
    return 'Climb $arg0';
  }

  @override
  String get activityEntryNotesLabel => 'Notes';

  @override
  String get activityEntryFeelingLabel => 'How did it feel?';

  @override
  String get activityEntryFeelingGreat => 'Great';

  @override
  String get activityEntryFeelingGood => 'Good';

  @override
  String get activityEntryFeelingHard => 'Hard';

  @override
  String get activityEntryFeelingRough => 'Rough';

  @override
  String get activityEntryImportedRoute => 'Imported route';

  @override
  String get activityEntryRecordingTitle => 'Recording activity';

  @override
  String get activityEntryRecordingReadyBody =>
      'Choose the activity type, then start when you are ready. After finishing, you can review and add details before saving.';

  @override
  String get activityEntryRecordingGoToActivityScreen =>
      'Go to activity screen';

  @override
  String get activityEntryRecordingActive => 'Recording';

  @override
  String get activityEntryRecordingPaused => 'Paused';

  @override
  String get activityEntryRecordingIdle => 'Idle';

  @override
  String get activityEntryRecordingResting => 'Resting';

  @override
  String get activityEntryRecordingGpsFix => 'GPS ready';

  @override
  String get activityEntryRecordingGpsPoor => 'Weak GPS';

  @override
  String get activityEntryRecordingGpsLost => 'GPS lost';

  @override
  String get activityEntryRecordingGpsOff => 'GPS off';

  @override
  String get activityEntryRecordingTabMap => 'Map';

  @override
  String get activityEntryRecordingTabStats => 'Stats';

  @override
  String get activityEntryRecordingTabIntervals => 'Intervals';

  @override
  String get activityEntryRecordingTabByTime => 'By time';

  @override
  String get activityEntryRecordingTabByDistance => 'By distance';

  @override
  String get activityEntryRecordingTimeSplit => 'Time split';

  @override
  String get activityEntryRecordingDistanceSplit => 'Distance split';

  @override
  String activityEntryRecordingSplitMinutes(int arg0) {
    return '$arg0 min';
  }

  @override
  String activityEntryRecordingSplitInterval(int arg0) {
    return 'Interval $arg0';
  }

  @override
  String activityEntryRecordingSplitTimeRange(int arg0, int arg1) {
    return '$arg0-$arg1 min';
  }

  @override
  String get activityEntryRecordingSplitElapsed => 'Elapsed';

  @override
  String get activityEntryRecordingSplitAvg => 'Avg';

  @override
  String get activityEntryRecordingSplitMax => 'Max';

  @override
  String get activityEntryRecordingNoIntervals => 'No intervals yet';

  @override
  String get activityEntryRecordingNoTimeSplits => 'No time splits yet';

  @override
  String get activityEntryRecordingNoDistanceSplits => 'No distance splits yet';

  @override
  String get activityEntryRecordingLap => 'Lap';

  @override
  String get activityEntryRecordingMarker => 'Add Marker';

  @override
  String activityEntryRecordingMarkerDefaultName(int arg0) {
    return 'Marker $arg0';
  }

  @override
  String get activityEntryRecordingMarkersTitle => 'Markers';

  @override
  String get activityEntryRecordingMarkerName => 'Name';

  @override
  String get activityEntryRecordingMarkerNote => 'Note';

  @override
  String get activityEntryRecordingWaitingForGps =>
      'Waiting for a precise GPS fix';

  @override
  String get activityEntryRecordingGpsWaiting =>
      'Waiting for a precise GPS fix before start.';

  @override
  String activityEntryRecordingGpsWaitingAccuracy(String arg0) {
    return 'Waiting for better GPS accuracy • $arg0';
  }

  @override
  String activityEntryRecordingGpsReady(String arg0) {
    return 'GPS ready • accuracy $arg0';
  }

  @override
  String get activityEntryRecordingGpsDisabled =>
      'Turn on GPS to start recording.';

  @override
  String get activityEntryRecordingDistance => 'Distance';

  @override
  String get activityEntryRecordingTotalTime => 'Total time';

  @override
  String get activityEntryRecordingMovingTime => 'Moving time';

  @override
  String get activityEntryRecordingRestTime => 'Rest time';

  @override
  String get activityEntryRecordingSpeed => 'Speed';

  @override
  String get activityEntryRecordingMaxSpeed => 'Max speed';

  @override
  String get activityEntryRecordingAverageSpeed => 'Avg speed';

  @override
  String get activityEntryRecordingAverageMovingSpeed => 'Avg moving speed';

  @override
  String get activityEntryRecordingElevationGain => 'Climb';

  @override
  String get activityEntryRecordingPoints => 'Points';

  @override
  String get activityEntryRecordingRestSecondsLabel => 'Rest seconds';

  @override
  String get activityEntryRecordingEndSet => 'End set';

  @override
  String get activityEntryRecordingStartNextSet => 'Start next set';

  @override
  String get activityEntryRecordingEndSession => 'Finish session';

  @override
  String activityEntryRecordingRestRemaining(String arg0) {
    return 'Rest $arg0';
  }

  @override
  String get activityEntryRecordingFinishHint =>
      'Finish opens the activity details form so you can add title, notes, calories, or adjust values before saving.';

  @override
  String get activityEntryRecordingRepetitionCorrectionHint =>
      'Use + or - if the sensor misses or adds a rep.';

  @override
  String activityEntryRecordingAccuracy(String arg0) {
    return 'Last accuracy $arg0';
  }

  @override
  String get activityEntryRecordingFocus => 'Focus';

  @override
  String get activityEntryRecordingDashboardLayout => 'Dashboard layout';

  @override
  String get activityEntryRecordingDashboardLayoutTwoByFour => '2x4';

  @override
  String get activityEntryRecordingDashboardLayoutThreeByFour => '3x4';

  @override
  String get activityEntryRecordingDashboardLayoutLargeTop => 'Large top';

  @override
  String get activityEntryRecordingDashboardAddField => 'Add widget';

  @override
  String activityEntryRouteSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  ) {
    return '$arg0 • $arg1 • $arg2 gain • $arg3 points';
  }

  @override
  String activityEntryRouteAverageMetrics(String arg0, String arg1) {
    return 'Avg pace $arg0 • avg speed $arg1';
  }

  @override
  String get activityEntryAdd => 'Save activity';

  @override
  String get activityEntryInvalidValue =>
      'Fix the highlighted fields before saving the activity.';

  @override
  String get activityEntryErrorActivityTypeRoute =>
      'Choose an activity type that supports GPS routes.';

  @override
  String get activityEntryErrorTrainingPlanTitleRequired =>
      'Enter a title to save this training plan.';

  @override
  String get activityEntryErrorStartDate => 'Choose a valid start date.';

  @override
  String get activityEntryErrorStartTime => 'Choose a valid start time.';

  @override
  String get activityEntryErrorStartTimeAfterRoute =>
      'Start time must be at or before the imported route start.';

  @override
  String get activityEntryErrorDuration =>
      'Duration must be between 1 minute and 7 days.';

  @override
  String get activityEntryErrorRepetitions =>
      'Enter positive counts. Rest must fit inside the activity duration.';

  @override
  String get activityEntryErrorDistance => 'Enter a distance greater than 0.';

  @override
  String get activityEntryErrorDistanceUnsupported =>
      'This activity type does not support distance.';

  @override
  String get activityEntryErrorElevation => 'Enter elevation greater than 0.';

  @override
  String get activityEntryErrorElevationUnsupported =>
      'This activity type does not support elevation gain.';

  @override
  String get activityEntryErrorActiveCalories =>
      'Enter active calories greater than 0.';

  @override
  String get activityEntryErrorTotalCalories =>
      'Enter total calories greater than 0.';

  @override
  String get activityEntryErrorTotalCaloriesBelowActive =>
      'Total calories cannot be lower than active calories.';

  @override
  String get activityEntryLocationPermissionNeeded =>
      'Precise location permission is required to record GPS activities.';

  @override
  String get activityEntryNotificationPermissionNeeded =>
      'Notification permission is required so OpenVitals can show an ongoing recording notification.';

  @override
  String get activityEntryActivityRecognitionPermissionNeeded =>
      'Activity recognition permission is required to count treadmill steps.';

  @override
  String activityEntryRouteImportFailed(String arg0) {
    return 'Could not import activity file: $arg0';
  }

  @override
  String activityEntryRecordingFailed(String arg0) {
    return 'Could not record activity: $arg0';
  }

  @override
  String activityEntryWriteFailed(String arg0) {
    return 'Could not write activity entry: $arg0';
  }

  @override
  String get activityRouteOpenInMap => 'Open route in map app';

  @override
  String get activityRouteExportGpx => 'Save GPX';

  @override
  String get activityRouteExportKmz => 'Save KMZ';

  @override
  String get activityRouteExportSaved => 'Route saved.';

  @override
  String get activityRouteExportFailed => 'Could not save route file.';

  @override
  String get activityRouteOpenChooserTitle => 'Open route with';

  @override
  String get activityRouteOpenFailed => 'No map app could open this route.';

  @override
  String get activityDetailAnalysisTitle => 'Route analysis';

  @override
  String get activityDetailTabMarkers => 'Markers';

  @override
  String get activityDetailNoMarkers => 'No markers yet';

  @override
  String activityRecordingVoiceSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  ) {
    return 'Time $arg0. Distance $arg1. Average speed $arg2. Current lap $arg3.';
  }

  @override
  String activityRecordingVoiceLap(int arg0, String arg1) {
    return 'Lap $arg0. $arg1';
  }

  @override
  String get activityRecordingVoiceIdle => 'Idle.';

  @override
  String get activityRecordingVoiceResumed => 'Recording resumed.';

  @override
  String get activityRecordingNotificationChannel => 'Activity recording';

  @override
  String get activityRecordingNotificationTitle => 'Recording activity';

  @override
  String activityRecordingNotificationRecording(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return 'Recording • $arg0 total • $arg1 moving • $arg2 • $arg3';
  }

  @override
  String activityRecordingNotificationPaused(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return 'Paused • $arg0 total • $arg1 moving • $arg2 • $arg3';
  }

  @override
  String activityRecordingNotificationRepetitionRecording(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Recording • $arg0 total • $arg1 $arg2';
  }

  @override
  String activityRecordingNotificationRepetitionPaused(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Paused • $arg0 total • $arg1 $arg2';
  }

  @override
  String activityRecordingNotificationRepetitionResting(
    String arg0,
    String arg1,
  ) {
    return 'Resting • $arg0 total • $arg1 left';
  }

  @override
  String activityRecordingNotificationTimedRecording(String arg0) {
    return 'Recording • $arg0 total';
  }

  @override
  String activityRecordingNotificationTimedPaused(String arg0) {
    return 'Paused • $arg0 total';
  }

  @override
  String get activityRecordingErrorService =>
      'Could not start activity recording service.';

  @override
  String get activityRecordingErrorPreciseLocationPermission =>
      'Precise location permission is required for reliable GPS tracks.';

  @override
  String get activityRecordingErrorNotificationPermission =>
      'Notification permission is required to show the ongoing recording notification.';

  @override
  String get activityRecordingErrorActivityRecognitionPermission =>
      'Activity recognition permission is required to count treadmill steps.';

  @override
  String get activityRecordingErrorWaitingForGps =>
      'Wait for a precise GPS fix before starting.';

  @override
  String get activityRecordingErrorProvider => 'Turn on GPS to record a route.';

  @override
  String get activityRecordingErrorUnsupportedType =>
      'This activity type cannot be recorded live.';

  @override
  String get activityRecordingErrorProximitySensor =>
      'This device does not expose a proximity sensor for push-up counting.';

  @override
  String get activityRecordingErrorAccelerometer =>
      'This device does not expose an accelerometer for this recording.';

  @override
  String get activityRecordingErrorStepDetector =>
      'This device does not expose Android step detector events.';

  @override
  String get activityRecordingHowItWorks => 'How recording works';

  @override
  String get activityRecordingGuidancePushUps =>
      'Place the phone screen-up under the chest or head area. The proximity sensor counts a rep when you move close to the phone.';

  @override
  String get activityRecordingGuidancePullUps =>
      'Secure the phone on your body. The accelerometer counts the pull and relax motion.';

  @override
  String get activityRecordingGuidanceRopeSkipping =>
      'Keep the phone secured on your body. The accelerometer counts jumps.';

  @override
  String get activityRecordingGuidanceTrampolineJumping =>
      'Keep the phone secured on your body. Jump detection uses a longer jump window than rope skipping.';

  @override
  String get activityRecordingGuidanceTreadmill =>
      'Carry the phone on your body. Android’s step detector counts steps; no GPS route is recorded.';

  @override
  String get activityRecordingSensorReady => 'Sensor ready';

  @override
  String get activityRecordingSensorUnavailableManual =>
      'Live counting is unavailable on this device. Manual entry is still available.';

  @override
  String get activityRecordingActivityRecognitionMissing =>
      'Grant activity recognition to count treadmill steps.';

  @override
  String get exerciseTypeRunning => 'Running';

  @override
  String get exerciseTypeBiking => 'Biking';

  @override
  String get exerciseTypeWalking => 'Walking';

  @override
  String get exerciseTypeHiking => 'Hiking';

  @override
  String get exerciseTypeWheelchair => 'Wheelchair';

  @override
  String get exerciseTypeRowing => 'Rowing';

  @override
  String get exerciseTypePaddling => 'Paddling';

  @override
  String get exerciseTypeSkiing => 'Skiing';

  @override
  String get exerciseTypeSnowboarding => 'Snowboarding';

  @override
  String get exerciseTypeSnowshoeing => 'Snowshoeing';

  @override
  String get exerciseTypeSkating => 'Skating';

  @override
  String get exerciseTypeSailing => 'Sailing';

  @override
  String get exerciseTypeSurfing => 'Surfing';

  @override
  String get exerciseTypeSwimmingOpenWater => 'Swimming (open water)';

  @override
  String get exerciseTypeGolf => 'Golf';

  @override
  String get exerciseTypeStrengthTraining => 'Strength training';

  @override
  String get exerciseTypeTreadmill => 'Treadmill';

  @override
  String get exerciseTypePushUps => 'Push-ups';

  @override
  String get exerciseTypePullUps => 'Pull-ups';

  @override
  String get exerciseTypeRopeSkipping => 'Rope skipping';

  @override
  String get exerciseTypeTrampolineJumping => 'Trampoline jumping';

  @override
  String get exerciseTypeOtherWorkout => 'Other workout';

  @override
  String get mindfulnessEntryUnavailable =>
      'Mindfulness sessions are unavailable in this Health Connect provider.';

  @override
  String get mindfulnessEntryTimerTitle => 'Timer';

  @override
  String get mindfulnessEntryManualTitle => 'Manual entry';

  @override
  String get mindfulnessEntryIntervalBell => 'Interval bell';

  @override
  String get mindfulnessEntryIntervalMinutes => 'Interval (min)';

  @override
  String get mindfulnessEntryBellSound => 'Bell sound';

  @override
  String get mindfulnessEntryBackgroundSound => 'Background sound';

  @override
  String get mindfulnessBellStruck => 'Soft strike';

  @override
  String get mindfulnessBellRubbed => 'Warm bowl';

  @override
  String get mindfulnessBellBright => 'Bright bowl';

  @override
  String get mindfulnessBellTemple => 'Temple bowl';

  @override
  String get mindfulnessBellHarmony => 'Harmony';

  @override
  String get mindfulnessBackgroundNone => 'None';

  @override
  String get mindfulnessBackgroundBowl => 'Bowl';

  @override
  String get mindfulnessBackgroundMeditation => 'Meditation';

  @override
  String get mindfulnessBackgroundChimes => 'Chimes';

  @override
  String get mindfulnessBackgroundDreamscape => 'Dreamscape';

  @override
  String get mindfulnessEntryStartTimer => 'Start';

  @override
  String get mindfulnessEntryStopTimer => 'Stop';

  @override
  String get mindfulnessEntryResumeTimer => 'Resume';

  @override
  String get mindfulnessEntryDiscardTimer => 'Discard';

  @override
  String get mindfulnessEntrySaveSession => 'Save session';

  @override
  String get mindfulnessEntryMinutes => 'Minutes';

  @override
  String get mindfulnessEntryAddMinutes => 'Add minutes';

  @override
  String get mindfulnessEntryInvalidTimer =>
      'Enter a valid timer duration and interval.';

  @override
  String get mindfulnessEntryInvalidManual =>
      'Enter valid mindfulness minutes.';

  @override
  String get mindfulnessEntryTimerTooShort =>
      'Meditation must be at least 1 minute to save.';

  @override
  String mindfulnessEntryWriteFailed(String arg0) {
    return 'Could not save mindfulness session: $arg0';
  }

  @override
  String get mindfulnessEntryCompleted => 'Timer complete';

  @override
  String get mindfulnessRemindersTitle => 'Mindfulness reminders';

  @override
  String get mindfulnessRemindersSummaryOff =>
      'Off by default. Enable a once-daily reminder for your mindfulness goal.';

  @override
  String mindfulnessRemindersSummaryOn(String arg0) {
    return 'Daily at $arg0';
  }

  @override
  String get mindfulnessRemindersPermissionNeeded =>
      'Grant notification permission to enable mindfulness reminders.';

  @override
  String get mindfulnessRemindersTime => 'Reminder time';

  @override
  String get mindfulnessRemindersGoalNote =>
      'Reminders pause after today\'s mindfulness goal is met and resume tomorrow.';

  @override
  String get mindfulnessReminderNotificationChannel => 'Mindfulness reminders';

  @override
  String get mindfulnessReminderNotificationChannelDesc =>
      'Optional reminders to complete your daily mindfulness goal.';

  @override
  String get mindfulnessReminderNotificationTitle => 'Mindfulness reminder';

  @override
  String mindfulnessReminderNotificationBody(String arg0) {
    return 'Your goal is $arg0 today. Take a mindful pause when you can.';
  }

  @override
  String mindfulnessReminderNotificationProgress(String arg0, String arg1) {
    return '$arg0 / $arg1';
  }

  @override
  String bodyEntrySubtitle(String arg0) {
    return '$arg0 entries are saved directly to Health Connect.';
  }

  @override
  String bodyEntryPermissionNeeded(String arg0) {
    return 'To add $arg0 entries, OpenVitals needs Health Connect write permission. The app will not store this data; entries are saved in Health Connect.';
  }

  @override
  String bodyEntryValueLabel(String arg0, String arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String bodyEntryAddSelected(String arg0) {
    return 'Add $arg0';
  }

  @override
  String get bodyEntryInvalidValue =>
      'Enter a valid value for this measurement.';

  @override
  String bodyEntryWriteFailed(String arg0) {
    return 'Could not save body measurement: $arg0';
  }

  @override
  String get carbsEntrySubtitle =>
      'Carbs entries are saved directly to Health Connect.';

  @override
  String get carbsEntryPermissionNeeded =>
      'To add carbs entries, OpenVitals needs Health Connect write permission. The app will not store this data; entries are saved in Health Connect.';

  @override
  String carbsEntryValueLabel(String arg0) {
    return 'Carbs ($arg0)';
  }

  @override
  String get carbsEntryAdd => 'Add carbs';

  @override
  String get carbsEntryInvalidValue => 'Enter a valid carbs amount.';

  @override
  String carbsEntryWriteFailed(String arg0) {
    return 'Could not save carbs: $arg0';
  }

  @override
  String vitalsEntrySubtitle(String arg0) {
    return '$arg0 entries are saved directly to Health Connect.';
  }

  @override
  String vitalsEntryPermissionNeeded(String arg0) {
    return 'To add $arg0 entries, OpenVitals needs Health Connect write permission. The app will not store this data; entries are saved in Health Connect.';
  }

  @override
  String vitalsEntryValueLabel(String arg0, String arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String get vitalsEntrySystolicLabel => 'Systolic (mmHg)';

  @override
  String get vitalsEntryDiastolicLabel => 'Diastolic (mmHg)';

  @override
  String vitalsEntryAddSelected(String arg0) {
    return 'Add $arg0';
  }

  @override
  String get vitalsEntryInvalidValue => 'Enter a valid value for this vital.';

  @override
  String vitalsEntryWriteFailed(String arg0) {
    return 'Could not save vital: $arg0';
  }

  @override
  String get rangeDay => 'Day';

  @override
  String get rangeWeek => 'Week';

  @override
  String get rangeMonth => 'Month';

  @override
  String get rangeYear => 'Year';

  @override
  String get periodToday => 'Today';

  @override
  String get periodYesterday => 'Yesterday';

  @override
  String get periodThisWeek => 'This week';

  @override
  String periodWeekOf(String arg0) {
    return 'Week of $arg0';
  }

  @override
  String get periodThisMonth => 'This month';

  @override
  String get periodThisYear => 'This year';

  @override
  String get periodLast7Days => 'Last 7 days';

  @override
  String get periodLast30Days => 'Last 30 days';

  @override
  String get periodLast365Days => 'Last 365 days';

  @override
  String get periodSelected => 'Selected period';

  @override
  String get metricSteps => 'Steps';

  @override
  String get metricDistance => 'Distance';

  @override
  String get metricAveragePace => 'Average pace';

  @override
  String get metricAverageSpeed => 'Average speed';

  @override
  String get metricCaloriesBurned => 'Total calories burned';

  @override
  String get metricCaloriesOut => 'Total calories';

  @override
  String get metricCaloriesIn => 'Calories in';

  @override
  String get metricFloorsClimbed => 'Floors climbed';

  @override
  String get metricActiveCalories => 'Active calories';

  @override
  String get metricElevation => 'Elevation';

  @override
  String get metricElevationGained => 'Elevation gained';

  @override
  String get metricWheelchairPushes => 'Wheelchair pushes';

  @override
  String get metricWorkout => 'Workout';

  @override
  String get metricSleep => 'Sleep';

  @override
  String get metricHydration => 'Beverages';

  @override
  String get metricTotalHydration => 'Total hydration';

  @override
  String get metricHydrationTrend => 'Beverage trend';

  @override
  String get metricLoggedDays => 'Logged days';

  @override
  String get metricLatestWeight => 'Latest weight';

  @override
  String get metricBodyFat => 'Body fat';

  @override
  String get metricAvgHeartRate => 'Avg heart rate';

  @override
  String get metricAverageHeartRate => 'Average heart rate';

  @override
  String get metricRestingHeartRate => 'Resting heart rate';

  @override
  String get metricHrv => 'Heart rate variability (HRV)';

  @override
  String get metricCardioLoad => 'Cardio load';

  @override
  String get metricWeeklyCardioLoad => 'Weekly cardio';

  @override
  String get metricEnergyBurned => 'Total calories';

  @override
  String get metricBloodPressure => 'Blood pressure';

  @override
  String get metricSpo2 => 'SpO2';

  @override
  String get metricOxygenSaturation => 'Oxygen saturation';

  @override
  String get metricVo2Max => 'VO2 max';

  @override
  String get metricMindfulness => 'Mindfulness';

  @override
  String get metricTotalMindfulness => 'Total mindfulness';

  @override
  String get metricCycle => 'Cycle';

  @override
  String get metricCycleTracking => 'Cycle tracking';

  @override
  String get metricPeriodDays => 'Period days';

  @override
  String get metricOvulationTests => 'Ovulation tests';

  @override
  String get metricLatestBbt => 'Latest BBT';

  @override
  String get metricWeight => 'Weight';

  @override
  String get metricHeight => 'Height';

  @override
  String get metricBmi => 'BMI';

  @override
  String get metricFfmi => 'FFMI';

  @override
  String get metricLeanMass => 'Lean mass';

  @override
  String get metricBmr => 'BMR';

  @override
  String get metricBoneMass => 'Bone mass';

  @override
  String get metricBodyWaterMass => 'Body water mass';

  @override
  String get metricLatest => 'Latest';

  @override
  String get metricChange => 'Change';

  @override
  String get metricMacros => 'Macros';

  @override
  String get metricProtein => 'Protein';

  @override
  String get metricCarbs => 'Carbs';

  @override
  String get metricFat => 'Fat';

  @override
  String get metricDietaryFiber => 'Dietary fiber';

  @override
  String get metricSugar => 'Sugar';

  @override
  String get metricEnergyFromFat => 'Calories from fat';

  @override
  String get metricMonounsaturatedFat => 'Monounsaturated fat';

  @override
  String get metricPolyunsaturatedFat => 'Polyunsaturated fat';

  @override
  String get metricSaturatedFat => 'Saturated fat';

  @override
  String get metricTransFat => 'Trans fat';

  @override
  String get metricUnsaturatedFat => 'Unsaturated fat';

  @override
  String get metricCholesterol => 'Cholesterol';

  @override
  String get metricBiotin => 'Biotin';

  @override
  String get metricFolate => 'Folate';

  @override
  String get metricFolicAcid => 'Folic acid';

  @override
  String get metricNiacin => 'Niacin';

  @override
  String get metricPantothenicAcid => 'Pantothenic acid';

  @override
  String get metricRiboflavin => 'Riboflavin';

  @override
  String get metricThiamin => 'Thiamin';

  @override
  String get metricVitaminA => 'Vitamin A';

  @override
  String get metricVitaminB12 => 'Vitamin B12';

  @override
  String get metricVitaminB6 => 'Vitamin B6';

  @override
  String get metricVitaminC => 'Vitamin C';

  @override
  String get metricVitaminD => 'Vitamin D';

  @override
  String get metricVitaminE => 'Vitamin E';

  @override
  String get metricVitaminK => 'Vitamin K';

  @override
  String get metricCalcium => 'Calcium';

  @override
  String get metricChloride => 'Chloride';

  @override
  String get metricChromium => 'Chromium';

  @override
  String get metricCopper => 'Copper';

  @override
  String get metricIodine => 'Iodine';

  @override
  String get metricIron => 'Iron';

  @override
  String get metricMagnesium => 'Magnesium';

  @override
  String get metricManganese => 'Manganese';

  @override
  String get metricMolybdenum => 'Molybdenum';

  @override
  String get metricPhosphorus => 'Phosphorus';

  @override
  String get metricPotassium => 'Potassium';

  @override
  String get metricSelenium => 'Selenium';

  @override
  String get metricSodium => 'Sodium';

  @override
  String get metricZinc => 'Zinc';

  @override
  String get metricCaffeine => 'Caffeine';

  @override
  String get metricRespiratoryRate => 'Respiratory rate';

  @override
  String get metricAvgRespiratoryRate => 'Avg respiratory rate';

  @override
  String get metricBodyTemp => 'Body temp';

  @override
  String get metricBloodGlucose => 'Blood glucose';

  @override
  String get metricSkinTemperature => 'Skin temperature';

  @override
  String get metricRecordedSpeed => 'Recorded speed';

  @override
  String get metricAveragePower => 'Average power';

  @override
  String get metricStepsCadence => 'Step cadence';

  @override
  String get metricCyclingCadence => 'Cycling cadence';

  @override
  String get unitSteps => 'steps';

  @override
  String get unitReps => 'reps';

  @override
  String get unitPushes => 'pushes';

  @override
  String get unitFloors => 'floors';

  @override
  String get unitDays => 'days';

  @override
  String get unitNights => 'nights';

  @override
  String get unitTests => 'tests';

  @override
  String get unitTotal => 'total';

  @override
  String get unitGrams => 'g';

  @override
  String get sectionActivities => 'Activities';

  @override
  String get sectionActivityTypeStats => 'By activity type';

  @override
  String activityTypeStatsActivityCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '# activities',
      one: '# activity',
    );
    return '$_temp0';
  }

  @override
  String get sectionPlannedWorkouts => 'Planned workouts';

  @override
  String get activitiesFilterActivityTypeLabel => 'Activity type';

  @override
  String get activitiesFilterAll => 'All activities';

  @override
  String get activitiesKeyMetrics => 'Key metrics';

  @override
  String get recoverySleepScore => 'Sleep score';

  @override
  String get recoverySleepDuration => 'Sleep duration';

  @override
  String get recoverySleepSchedule => 'Sleep schedule';

  @override
  String get recoveryRemSleep => 'REM sleep';

  @override
  String get recoveryDeepSleep => 'Deep sleep';

  @override
  String get recoverySleepEfficiency => 'Sleep efficiency';

  @override
  String get sleepScoreConfidenceHigh => 'High confidence';

  @override
  String get sleepScoreConfidenceMedium => 'Medium confidence';

  @override
  String get sleepScoreConfidenceLow => 'Low confidence';

  @override
  String get sleepScoreConfidenceNoData => 'No data';

  @override
  String get sleepScoreRatingExcellent => 'Excellent';

  @override
  String get sleepScoreRatingGood => 'Good';

  @override
  String get sleepScoreRatingFair => 'Fair';

  @override
  String get sleepScoreRatingPoor => 'Poor';

  @override
  String dashboardSleepScoreSubtitle(String arg0, String arg1) {
    return '$arg0 • $arg1';
  }

  @override
  String get sleepScoreCalculationTitle => 'How it is calculated';

  @override
  String get sleepScoreDayNumbersTitle => 'Today values';

  @override
  String get sleepScoreReferencesTitle => 'Backed links';

  @override
  String get sleepScoreCalculationSummary =>
      'OpenVitals scores objective sleep health from duration, efficiency, continuity, and regularity. It does not diagnose sleep disorders.';

  @override
  String get sleepScoreFormula =>
      'Sleep score = duration 35 + efficiency 30 + continuity 20 + regularity 15';

  @override
  String get sleepScoreFormulaBody =>
      'Duration gives full credit for 7-9 h. Efficiency uses total sleep time divided by time in bed. Continuity uses wake after sleep onset. Regularity compares today\'s sleep midpoint with recent nights.';

  @override
  String get sleepScoreComponentsBody =>
      'Sleep-stage data improves confidence, but REM and deep sleep are not heavily scored because consumer stage estimates can vary. If regularity history is missing, OpenVitals uses a neutral regularity value and lowers confidence.';

  @override
  String get sleepScoreNotDiagnostic =>
      'This score is a daily guide from Health Connect records, not a diagnosis or treatment recommendation.';

  @override
  String get sleepScoreComponentDuration => 'Duration';

  @override
  String get sleepScoreComponentEfficiency => 'Efficiency';

  @override
  String get sleepScoreComponentContinuity => 'Continuity';

  @override
  String get sleepScoreComponentRegularity => 'Regularity';

  @override
  String get sleepScoreTotalSleep => 'Total sleep';

  @override
  String get sleepScoreTimeInBed => 'Time in bed';

  @override
  String get sleepScoreEfficiency => 'Efficiency';

  @override
  String get sleepScoreWaso => 'Wake after sleep';

  @override
  String get sleepScoreRegularity => 'Timing difference';

  @override
  String get sleepScoreBaselineNights => 'Baseline nights';

  @override
  String get sleepScoreStageRecords => 'Stage records';

  @override
  String get sleepScoreQualityNoData => 'Insufficient sleep data for a score.';

  @override
  String get sleepScoreQualityStageAwake =>
      'Uses sleep stages and awake stages from Health Connect.';

  @override
  String get sleepScoreQualityStageOnly =>
      'Uses sleep stages; awake continuity may be estimated.';

  @override
  String get sleepScoreQualitySessionOnly =>
      'Uses sleep session timing only; confidence is limited.';

  @override
  String get sleepScoreReferenceAasm => 'AASM adult sleep duration';

  @override
  String get sleepScoreReferenceSleepHealth => 'Multidimensional sleep health';

  @override
  String get sleepScoreReferenceEfficiency => 'Sleep efficiency definition';

  @override
  String get sleepScoreReferenceRegularity => 'Sleep regularity research';

  @override
  String get sleepEfficiencyConfidenceHigh => 'High confidence';

  @override
  String get sleepEfficiencyConfidenceLow => 'Low confidence';

  @override
  String get sleepEfficiencyConfidenceNoData => 'No data';

  @override
  String get sleepEfficiencyCalculationTitle => 'How it is calculated';

  @override
  String get sleepEfficiencyDayNumbersTitle => 'Today values';

  @override
  String get sleepEfficiencyReferencesTitle => 'Backed links';

  @override
  String get sleepEfficiencyCalculationSummary =>
      'Sleep efficiency is the percentage of the sleep window spent asleep. Higher values usually mean less time awake in bed.';

  @override
  String get sleepEfficiencyFormula =>
      'Sleep efficiency = total sleep time / time in bed x 100';

  @override
  String get sleepEfficiencyFormulaBody =>
      'Total sleep time is the sum of Health Connect sleep stages when stages are available. Time in bed is the main sleep session start-to-end window.';

  @override
  String get sleepEfficiencyDataBody =>
      'When sleep stages are missing, Health Connect may only provide a session duration. OpenVitals can still show an estimate, but confidence is low because awake time in bed may be hidden.';

  @override
  String get sleepEfficiencyNotDiagnostic =>
      'Sleep efficiency is a sleep-continuity signal, not a diagnosis. Persistently low values can be worth discussing with a clinician.';

  @override
  String get sleepEfficiencyQualityNoData =>
      'Insufficient sleep data for efficiency.';

  @override
  String get sleepEfficiencyQualityStageBased =>
      'Uses Health Connect sleep stages for total sleep time.';

  @override
  String get sleepEfficiencyQualitySessionOnly =>
      'Uses session timing only; awake time may be missing.';

  @override
  String get sleepEfficiencyReferenceDefinition =>
      'Sleep efficiency definition';

  @override
  String get sleepEfficiencyReferenceDenominator =>
      'Sleep efficiency denominator research';

  @override
  String get sleepEfficiencyReferenceMethods =>
      'Sleep assessment methods review';

  @override
  String get cardioLoadConfidenceHigh => 'High confidence';

  @override
  String get cardioLoadConfidenceMedium => 'Medium confidence';

  @override
  String get cardioLoadConfidenceLow => 'Low confidence';

  @override
  String get cardioLoadConfidenceNoData => 'No data';

  @override
  String get cardioLoadCalculationTitle => 'How it is calculated';

  @override
  String get cardioLoadDayNumbersTitle => 'Today values';

  @override
  String get cardioLoadReferencesTitle => 'Backed links';

  @override
  String get cardioLoadCalculationSummary =>
      'OpenVitals uses HR-based TRIMP when heart-rate data is available, then falls back to movement only when HR is not usable.';

  @override
  String get cardioLoadFormula =>
      'TRIMP = minutes x HRR x 0.64 x e^(1.92 x HRR)';

  @override
  String get cardioLoadFormulaBody =>
      'HRR is heart-rate reserve: (heart rate - resting heart rate) / (max heart rate - resting heart rate). OpenVitals sums this over available heart-rate intervals for the day.';

  @override
  String get cardioLoadMappingBody =>
      'When recorded activities exist, heart-rate samples are mapped by timestamp into each activity start and end window. Without activity windows, only elevated heart-rate intervals are counted. If HR is not usable, movement and active calories are shown as a low-confidence fallback.';

  @override
  String get cardioLoadMethod => 'Method';

  @override
  String get cardioLoadTrimpScore => 'TRIMP score';

  @override
  String get cardioLoadHrCoverage => 'HR coverage';

  @override
  String get cardioLoadExpectedCoverage => 'Expected coverage';

  @override
  String get cardioLoadRestingHr => 'Resting HR';

  @override
  String get cardioLoadMaxHr => 'Max HR';

  @override
  String get cardioLoadHrSamples => 'HR samples';

  @override
  String get cardioLoadActivityWindows => 'Activity windows';

  @override
  String get cardioLoadActivityMinutes => 'Activity minutes';

  @override
  String get cardioLoadMovementFallback => 'Movement fallback';

  @override
  String get cardioLoadMethodActivityWindows => 'TRIMP from activity HR';

  @override
  String get cardioLoadMethodElevatedHr => 'TRIMP from elevated HR';

  @override
  String get cardioLoadMethodMovementFallback => 'Movement fallback';

  @override
  String get cardioLoadMethodNoData => 'Insufficient data';

  @override
  String get cardioLoadCalibrationObservedResting => 'Resting HR observed';

  @override
  String get cardioLoadCalibrationEstimatedResting => 'Resting HR estimated';

  @override
  String get cardioLoadCalibrationObservedMax => 'Max HR observed';

  @override
  String get cardioLoadCalibrationEstimatedMax => 'Max HR estimated';

  @override
  String get cardioLoadReferenceBanister => 'Banister TRIMP equation';

  @override
  String get cardioLoadReferenceTrainingLoad =>
      'Training-load monitoring review';

  @override
  String get cardioLoadReferenceHealthConnect =>
      'Health Connect workout HR mapping';

  @override
  String get sectionSleepSessions => 'Sleep sessions';

  @override
  String get sectionWeight => 'Weight';

  @override
  String get sectionEntries => 'Entries';

  @override
  String get sectionMeals => 'Meals';

  @override
  String get sectionSessions => 'Sessions';

  @override
  String get sectionDailyBreakdown => 'Daily breakdown';

  @override
  String get sectionVitals => 'Vitals';

  @override
  String get sectionHeart => 'Heart';

  @override
  String get sectionCardiovascular => 'Cardiovascular';

  @override
  String get sectionRespiratory => 'Respiratory';

  @override
  String get sectionRespiratoryRateDailyBreakdown =>
      'Respiratory rate daily breakdown';

  @override
  String get sectionVo2MaxHistory => 'VO2 max history';

  @override
  String get sectionDisplay => 'Display';

  @override
  String get sectionPrivacy => 'Privacy';

  @override
  String get sectionCycleCalendar => 'Cycle calendar';

  @override
  String get sectionBasalBodyTemperature => 'Basal body temperature';

  @override
  String get sectionStatistics => 'Statistics';

  @override
  String get sectionCalorieTrends => 'Calorie trends';

  @override
  String get sectionNutritionTrends => 'Nutrition trends';

  @override
  String get sectionBodyTrends => 'Body trends';

  @override
  String get sectionCarbohydrates => 'Carbohydrates';

  @override
  String get sectionFats => 'Fats';

  @override
  String get sectionVitamins => 'Vitamins';

  @override
  String get sectionMinerals => 'Minerals';

  @override
  String get sectionOtherNutrients => 'Other nutrients';

  @override
  String summaryDailyAverage(String arg0) {
    return '$arg0 daily average';
  }

  @override
  String summaryDaysInRange(String arg0) {
    return '$arg0 days in range';
  }

  @override
  String summaryEntries(String arg0) {
    return '$arg0 entries';
  }

  @override
  String summaryReadings(String arg0) {
    return '$arg0 readings';
  }

  @override
  String summaryNights(String arg0) {
    return '$arg0 nights';
  }

  @override
  String summaryRecordedStages(String arg0) {
    return '$arg0 recorded stages';
  }

  @override
  String get summaryAverage => 'Avg';

  @override
  String summaryAvgValue(String arg0) {
    return 'Avg $arg0';
  }

  @override
  String summaryAvgValueRange(String arg0, String arg1, String arg2) {
    return 'Avg $arg0 · range $arg1-$arg2';
  }

  @override
  String summaryValueAvg(String arg0) {
    return '$arg0 avg';
  }

  @override
  String get summaryRange => 'Range';

  @override
  String get summarySamples => 'Samples';

  @override
  String summaryRecorded(String arg0, String arg1) {
    return '$arg0-$arg1 recorded';
  }

  @override
  String summaryRestingValue(String arg0) {
    return 'Resting $arg0';
  }

  @override
  String summaryHrvValue(String arg0) {
    return 'HRV $arg0';
  }

  @override
  String summaryLastUpdate(String arg0) {
    return 'Last update $arg0';
  }

  @override
  String get summaryNow => 'Now';

  @override
  String summaryToday(String arg0) {
    return '$arg0 today';
  }

  @override
  String summaryOnDate(String arg0, String arg1) {
    return '$arg0 on $arg1';
  }

  @override
  String summaryEmptyToday(String arg0) {
    return '$arg0 yet today.';
  }

  @override
  String summaryEmptyDay(String arg0) {
    return '$arg0 on this day.';
  }

  @override
  String get summaryAcrossSelectedPeriod => 'Across selected period';

  @override
  String summaryLatestTemperature(String arg0, String arg1) {
    return 'Latest $arg0 · $arg1';
  }

  @override
  String summaryTemperatureRange(String arg0, String arg1, String arg2) {
    return 'Range $arg0-$arg1 · $arg2 readings';
  }

  @override
  String get summarySleepEndingToday => 'Sleep ending today';

  @override
  String summarySleepEndingOn(String arg0) {
    return 'Sleep ending on $arg0';
  }

  @override
  String get statTotal => 'Total';

  @override
  String get statTime => 'Time';

  @override
  String get statActiveDays => 'Active days';

  @override
  String get statAverage => 'Average';

  @override
  String get statLowest => 'Lowest';

  @override
  String get statHighest => 'Highest';

  @override
  String get statReadings => 'Readings';

  @override
  String get statDailyAverage => 'Daily average';

  @override
  String get caloriesStatActiveAverage => 'Active average';

  @override
  String get caloriesStatBmrReadings => 'BMR readings';

  @override
  String get statAverageDuration => 'Average duration';

  @override
  String get statTotalIntake => 'Total intake';

  @override
  String get statBestDay => 'Best day';

  @override
  String get statNightsLogged => 'Nights logged';

  @override
  String get statLongestSleep => 'Longest sleep';

  @override
  String get statLongestWorkout => 'Longest workout';

  @override
  String get statAverageMovingPace => 'Avg moving pace';

  @override
  String get statFastestPace => 'Fastest pace';

  @override
  String get statBestSpeed => 'Best speed';

  @override
  String get statLongestSession => 'Longest session';

  @override
  String get statBbtReadings => 'BBT readings';

  @override
  String get statGoalStreak => 'Goal streak';

  @override
  String get statLongestGoalStreak => 'Longest streak';

  @override
  String get statGoalsMet => 'Goals met';

  @override
  String get statSuccessRate => 'Success rate';

  @override
  String get statAverageGap => 'Avg gap';

  @override
  String get statVsPreviousDay => 'Vs previous day';

  @override
  String get statVsPreviousWeek => 'Vs previous week';

  @override
  String get statVsPreviousMonth => 'Vs previous month';

  @override
  String get statVsPreviousYear => 'Vs previous year';

  @override
  String get statBaseline => 'Baseline';

  @override
  String get stat30DayBaseline => '30-day avg';

  @override
  String get stat60DayBaseline => '60-day avg';

  @override
  String get stat90DayBaseline => '90-day avg';

  @override
  String get statUsualRange => 'Usual range';

  @override
  String get statBaselineDeviation => 'Baseline deviation';

  @override
  String get baselineStatusUsual => 'Usual';

  @override
  String get baselineStatusAbove => 'Above';

  @override
  String get baselineStatusBelow => 'Below';

  @override
  String get baselineStatusUnusualHigh => 'Unusual high';

  @override
  String get baselineStatusUnusualLow => 'Unusual low';

  @override
  String get sectionMetricContext => 'Context';

  @override
  String get interpretationBpTitle => 'Blood pressure category';

  @override
  String get interpretationBpNormal => 'Normal';

  @override
  String get interpretationBpElevated => 'Elevated';

  @override
  String get interpretationBpStage1 => 'Stage 1 high blood pressure';

  @override
  String get interpretationBpStage2 => 'Stage 2 high blood pressure';

  @override
  String get interpretationBpSevere => 'Severe range reference';

  @override
  String interpretationBpBody(String arg0) {
    return 'This reading falls in the $arg0 range. A single app reading is not a diagnosis.';
  }

  @override
  String get interpretationBpSevereBody =>
      'This reading is above the severe range reference. Recheck it; seek urgent care if symptoms are present or the reading stays very high.';

  @override
  String get interpretationBpSource =>
      'Source: American Heart Association adult blood pressure categories.';

  @override
  String get interpretationBmiTitle => 'BMI category';

  @override
  String get interpretationBmiUnderweight => 'Underweight';

  @override
  String get interpretationBmiHealthy => 'Healthy weight';

  @override
  String get interpretationBmiOverweight => 'Overweight';

  @override
  String get interpretationBmiObesity1 => 'Obesity class 1';

  @override
  String get interpretationBmiObesity2 => 'Obesity class 2';

  @override
  String get interpretationBmiObesity3 => 'Obesity class 3';

  @override
  String get interpretationBmiBody =>
      'Adult BMI screening category only; BMI does not measure body composition.';

  @override
  String get interpretationBmiSource => 'Source: CDC adult BMI categories.';

  @override
  String get interpretationFfmiTitle => 'FFMI category';

  @override
  String get interpretationFfmiBelowAverage => 'Below average';

  @override
  String get interpretationFfmiAverage => 'Average';

  @override
  String get interpretationFfmiAboveAverage => 'Above average';

  @override
  String get interpretationFfmiExcellent => 'Excellent';

  @override
  String get interpretationFfmiSuperior => 'Superior';

  @override
  String get interpretationFfmiExceptional => 'Exceptional';

  @override
  String get interpretationFfmiElite => 'Elite';

  @override
  String interpretationFfmiBody(String arg0, String arg1) {
    return 'FFMI $arg0; adjusted FFMI $arg1. Uses your latest weight, body fat, and height.';
  }

  @override
  String get interpretationFfmiSource =>
      'Source: ffmicalculators.com indicative adjusted FFMI categories.';

  @override
  String get interpretationSleepTitle => 'Sleep target';

  @override
  String get interpretationSleepBelow => 'Below target';

  @override
  String get interpretationSleepNear => 'Near target';

  @override
  String get interpretationSleepMet => 'Target met';

  @override
  String interpretationSleepBelowBody(String arg0) {
    return 'Average sleep is $arg0 below your configured target.';
  }

  @override
  String interpretationSleepNearBody(String arg0, String arg1) {
    return 'Average sleep is close to your configured target: $arg0 vs $arg1.';
  }

  @override
  String interpretationSleepMetBody(String arg0, String arg1) {
    return 'Average sleep meets your configured target: $arg0 vs $arg1.';
  }

  @override
  String get interpretationSleepSource =>
      'Based on your configured sleep target, not a medical sleep assessment.';

  @override
  String get interpretationMacroTitle => 'Macro split';

  @override
  String get interpretationMacroWithin => 'Within reference split';

  @override
  String get interpretationMacroOutside => 'Outside reference split';

  @override
  String interpretationMacroBody(String arg0, String arg1, String arg2) {
    return 'Protein $arg0, carbs $arg1, fat $arg2 of logged macro calories.';
  }

  @override
  String get interpretationMacroSource =>
      'Source: National Academies AMDR adult reference; logged macros only.';

  @override
  String get interpretationWorkoutTitle => 'Workout guideline progress';

  @override
  String get interpretationWorkoutNone => 'No logged minutes';

  @override
  String get interpretationWorkoutBelow => 'Below weekly reference';

  @override
  String get interpretationWorkoutApproaching => 'Approaching weekly reference';

  @override
  String get interpretationWorkoutMet => 'Weekly reference met';

  @override
  String interpretationWorkoutBody(String arg0, String arg1) {
    return 'Logged $arg0 toward the 150 min/week adult reference ($arg1). Intensity is not verified.';
  }

  @override
  String interpretationWorkoutBodyWeeklyAverage(String arg0, String arg1) {
    return 'Weekly average $arg0 toward the 150 min/week adult reference ($arg1). Intensity is not verified.';
  }

  @override
  String get interpretationWorkoutSource =>
      'Source: HHS adult physical activity guideline reference.';

  @override
  String get interpretationVitalTitle => 'Vital context';

  @override
  String get interpretationVitalWithin => 'Within broad adult reference';

  @override
  String get interpretationVitalBelow => 'Below broad adult reference';

  @override
  String get interpretationVitalAbove => 'Above broad adult reference';

  @override
  String get interpretationVitalOxygenBelowTypical =>
      'Below typical oxygen range';

  @override
  String get interpretationVitalOxygenLow => 'Low oxygen reference';

  @override
  String get interpretationVitalOxygenVeryLow => 'Very low oxygen reference';

  @override
  String get interpretationVitalRestingHrBody =>
      'Broad adult reference only; fitness, medication, stress, illness, and timing can change what is usual for you.';

  @override
  String get interpretationVitalRespiratoryBody =>
      'Broad adult reference only; activity, anxiety, illness, and measurement timing can affect respiratory rate.';

  @override
  String get interpretationVitalTemperatureBody =>
      'Temperature varies by measurement site and time of day; use this as context only.';

  @override
  String get interpretationVitalOxygenBody =>
      'Pulse oximeter readings can be affected by device, skin, circulation, motion, and conditions.';

  @override
  String get interpretationVitalSource =>
      'Source: MedlinePlus adult vital sign reference.';

  @override
  String get interpretationOxygenSource =>
      'Source: MedlinePlus and FDA pulse oximeter context.';

  @override
  String get sectionCrossMetricInsights => 'Cross-metric insights';

  @override
  String get crossMetricPositiveLink => 'Positive link';

  @override
  String get crossMetricNegativeLink => 'Negative link';

  @override
  String get crossMetricWeakLink => 'Weak link';

  @override
  String crossMetricCorrelation(String arg0) {
    return '$arg0';
  }

  @override
  String crossMetricPairedDays(int arg0) {
    return '$arg0 paired days';
  }

  @override
  String get crossSleepHrvTitle => 'Sleep vs HRV';

  @override
  String get crossSleepHrvPositive =>
      'More sleep tends to align with higher HRV in this period.';

  @override
  String get crossSleepHrvNegative =>
      'More sleep tends to align with lower HRV in this period.';

  @override
  String get crossSleepHrvNeutral =>
      'Sleep and HRV do not show a clear pattern in this period.';

  @override
  String get crossWorkoutRestingHrTitle => 'Workouts vs resting heart rate';

  @override
  String get crossWorkoutRestingHrPositive =>
      'More workout minutes tend to align with higher resting heart rate in this period.';

  @override
  String get crossWorkoutRestingHrNegative =>
      'More workout minutes tend to align with lower resting heart rate in this period.';

  @override
  String get crossWorkoutRestingHrNeutral =>
      'Workout minutes and resting heart rate do not show a clear pattern in this period.';

  @override
  String get crossHydrationWeightTitle => 'Hydration vs weight fluctuation';

  @override
  String get crossHydrationWeightPositive =>
      'More hydration tends to align with larger weight swings in this period.';

  @override
  String get crossHydrationWeightNegative =>
      'More hydration tends to align with smaller weight swings in this period.';

  @override
  String get crossHydrationWeightNeutral =>
      'Hydration and weight fluctuation do not show a clear pattern in this period.';

  @override
  String get crossMindfulnessSleepTitle => 'Mindfulness vs sleep';

  @override
  String get crossMindfulnessSleepPositive =>
      'More mindfulness minutes tend to align with longer sleep in this period.';

  @override
  String get crossMindfulnessSleepNegative =>
      'More mindfulness minutes tend to align with shorter sleep in this period.';

  @override
  String get crossMindfulnessSleepNeutral =>
      'Mindfulness and sleep do not show a clear pattern in this period.';

  @override
  String get legendLess => 'Less';

  @override
  String get legendMore => 'More';

  @override
  String get dailyGoal => 'Daily goal';

  @override
  String goalProgress(int arg0, int arg1) {
    return '$arg0 of $arg1 tracked days met';
  }

  @override
  String get cdDecreaseDailyGoal => 'Decrease daily goal';

  @override
  String get cdIncreaseDailyGoal => 'Increase daily goal';

  @override
  String get hydrationDailyGoal => 'Daily goal';

  @override
  String hydrationGoalProgress(int arg0, int arg1) {
    return '$arg0 of $arg1 tracked days met';
  }

  @override
  String get hydrationRemindersTitle => 'Beverage reminders';

  @override
  String get hydrationRemindersSummaryOff =>
      'Off by default. Enable reminders during active hours until today\'s hydration goal is met.';

  @override
  String hydrationRemindersSummaryOn(int arg0, String arg1, String arg2) {
    return 'Every $arg0 min • $arg1-$arg2';
  }

  @override
  String get hydrationRemindersPermissionNeeded =>
      'Grant notification permission to enable beverage reminders.';

  @override
  String get hydrationRemindersInterval => 'Reminder interval';

  @override
  String hydrationRemindersIntervalValue(int arg0) {
    return 'Every $arg0 min';
  }

  @override
  String get hydrationRemindersActiveStart => 'Active from';

  @override
  String get hydrationRemindersActiveEnd => 'Active until';

  @override
  String get hydrationRemindersGoalNote =>
      'Reminders pause after today\'s goal is met and resume tomorrow.';

  @override
  String get hydrationReminderNotificationChannel => 'Beverage reminders';

  @override
  String get hydrationReminderNotificationChannelDesc =>
      'Optional reminders to log beverages during active hours.';

  @override
  String get hydrationReminderNotificationTitle => 'Beverage reminder';

  @override
  String hydrationReminderNotificationBody(String arg0, String arg1) {
    return 'You\'re at $arg0 of $arg1 today. Add a drink when you can.';
  }

  @override
  String hydrationReminderNotificationProgress(String arg0, String arg1) {
    return '$arg0 / $arg1';
  }

  @override
  String get hydrationTrackerTitle => 'Log beverage';

  @override
  String get hydrationTrackerSubtitle => 'Saved directly to Health Connect';

  @override
  String get hydrationTrackerPermissionNeeded =>
      'For the summary, OpenVitals only asks for view permissions. To add this manual entry, we need write permission. OpenVitals will not store this data; entries are saved in Health Connect.';

  @override
  String get hydrationNutritionPermissionNeeded =>
      'Grant nutrition write permission to save drink nutrients in Health Connect.';

  @override
  String get hydrationCustomDrinksTitle => 'Saved drinks';

  @override
  String get hydrationCatalogDrinksTitle => 'Drink catalog';

  @override
  String get hydrationCatalogSearch => 'Search drinks';

  @override
  String get hydrationCatalogFrequentlyConsumed => 'Frequently consumed';

  @override
  String get hydrationCatalogSavedOutside => 'Saved drinks';

  @override
  String get hydrationCatalogSectionWater => 'Water';

  @override
  String get hydrationCatalogSectionCoffees => 'Coffees';

  @override
  String get hydrationCatalogSectionEnergyDrinks => 'Energy drinks';

  @override
  String get hydrationCatalogSectionTeas => 'Teas';

  @override
  String get hydrationCatalogSectionChocolateDrinks => 'Chocolate drinks';

  @override
  String get hydrationCatalogSectionCarbonatedSoftDrinks =>
      'Carbonated soft drinks';

  @override
  String get hydrationCatalogSectionOtherDrinks => 'Other drinks';

  @override
  String hydrationCatalogSectionCount(int arg0) {
    return '$arg0 drinks';
  }

  @override
  String get hydrationNewDrinkAction => 'New drink';

  @override
  String get hydrationNewDrinkTitle => 'New drink';

  @override
  String get hydrationEditDrinkTitle => 'Edit drink';

  @override
  String hydrationLogSavedDrinkTitle(String arg0) {
    return 'Log $arg0';
  }

  @override
  String get hydrationCustomDrinkName => 'Name';

  @override
  String get hydrationCustomDrinkCategory => 'Category';

  @override
  String get hydrationCustomDrinkNoCategory => 'No category';

  @override
  String get hydrationCustomDrinkHydrationImpact => 'Hydration impact';

  @override
  String get hydrationImpactCountsFully => 'Counts fully';

  @override
  String get hydrationImpactCountsPartially => 'Counts partially';

  @override
  String get hydrationImpactDoesNotCount => 'Does not count';

  @override
  String get hydrationImpactCountsFullyBody =>
      'All drink volume counts toward hydration.';

  @override
  String get hydrationImpactCountsPartiallyBody =>
      'Use a percentage of this drink.';

  @override
  String get hydrationImpactDoesNotCountBody =>
      'Save it without adding hydration.';

  @override
  String get hydrationImpactPercentLabel => 'Counts as hydration (%)';

  @override
  String get hydrationImpactInvalidPercent =>
      'Enter a percentage above 0 and below 100.';

  @override
  String get hydrationCustomDrinkNutrients => 'Nutrients';

  @override
  String get hydrationCustomDrinkAddNutrient => 'Add nutrient';

  @override
  String get hydrationCustomDrinkLiquidOnly => 'Liquid only';

  @override
  String hydrationCustomDrinkNutrientCount(int arg0) {
    return 'Nutrients: $arg0';
  }

  @override
  String hydrationSavedDrinkAmountNoHydration(String arg0) {
    return '$arg0 • Does not count as hydration';
  }

  @override
  String hydrationSavedDrinkAmountPartialHydration(String arg0, int arg1) {
    return '$arg0 • Counts $arg1% as hydration';
  }

  @override
  String get hydrationNonHydratingDrinkSavedHint =>
      'Saved as nutrition only. No hydration was added.';

  @override
  String get hydrationEntryNutritionOnly => 'Beverage';

  @override
  String get hydrationEntryNoHydration => 'No hydration impact';

  @override
  String get hydrationCustomDrinkAmountGrams => 'Amount (g)';

  @override
  String get hydrationCustomDrinkAmountKcal => 'Amount (kcal)';

  @override
  String get hydrationCustomDrinkInvalid =>
      'Enter a drink name, amount, and positive nutrient amounts.';

  @override
  String get hydrationInvalidAmount =>
      'Enter an amount greater than zero and no more than 100 L.';

  @override
  String hydrationDrinkAmountLabel(String arg0) {
    return 'Amount ($arg0)';
  }

  @override
  String hydrationDrinkInvalidAmountRange(String arg0, String arg1) {
    return 'Enter an amount from $arg0 to $arg1.';
  }

  @override
  String hydrationWriteFailed(String arg0) {
    return 'Could not save hydration entry: $arg0';
  }

  @override
  String get cdDecreaseHydrationGoal => 'Decrease hydration goal';

  @override
  String get cdIncreaseHydrationGoal => 'Increase hydration goal';

  @override
  String get cdDecreaseHydrationReminderInterval =>
      'Decrease hydration reminder interval';

  @override
  String get cdIncreaseHydrationReminderInterval =>
      'Increase hydration reminder interval';

  @override
  String get unitPercentSymbol => '%';

  @override
  String get messageNoDashboardData => 'No summary data available.';

  @override
  String get messageMissingPermissionsTitle => 'Some permissions are missing';

  @override
  String get messageMissingPermissionsBody =>
      'Grant the missing permissions to see a complete summary.';

  @override
  String messageHealthConnectRateLimited(int arg0) {
    return 'Health Connect rate limit reached. Please wait about $arg0 min and try again.';
  }

  @override
  String get messageNoWorkoutsDay => 'No workouts recorded on this day.';

  @override
  String get messageNoSleepDay => 'No sleep session ended on this day.';

  @override
  String get messageNoBloodPressure => 'No blood pressure reading.';

  @override
  String get messageNoOxygen => 'No oxygen reading.';

  @override
  String get messageNoVo2Max => 'No VO2 max reading.';

  @override
  String get messageNoBloodGlucose => 'No blood glucose reading.';

  @override
  String get messageNoSkinTemperature => 'No skin temperature reading.';

  @override
  String get messageCycleBrowse => 'View cycle calendar and readings.';

  @override
  String get dashboardAddWidgets => 'Add widgets';

  @override
  String get dashboardAllWidgetsAdded =>
      'All widgets are already on the summary.';

  @override
  String get dashboardActionLog => 'Log';

  @override
  String get dashboardActionStartWorkout => 'Start workout';

  @override
  String get dashboardActivitiesToday => 'Activities';

  @override
  String get dashboardSensorStatusTitle => 'Sensor battery';

  @override
  String dashboardSensorBatteryLowest(int arg0) {
    return '$arg0% lowest';
  }

  @override
  String get dashboardSensorBatteryUnknown => 'Battery pending';

  @override
  String dashboardSensorStatusActiveConnected(int arg0, int arg1) {
    return '$arg0 active • $arg1 connected';
  }

  @override
  String get dashboardSensorStatusAllDisabled => 'All sensors disabled';

  @override
  String get dashboardDeleteActivityTitle => 'Delete activity?';

  @override
  String dashboardDeleteActivityMessage(String arg0) {
    return 'Delete this $arg0 activity from OpenVitals?';
  }

  @override
  String get dashboardReadinessTitle => 'Daily Readiness';

  @override
  String get dashboardReadinessScore => 'Readiness';

  @override
  String get dashboardReadinessBodyEnergy => 'Body Energy';

  @override
  String get dashboardReadinessTraining => 'Training Readiness';

  @override
  String get dashboardReadinessHrvStatus => 'HRV Status';

  @override
  String get dashboardReadinessIntensityMinutes => 'Intensity Minutes';

  @override
  String get dashboardReadinessStressLevel => 'Stress Level';

  @override
  String get dashboardReadinessRecommended => 'Recommended';

  @override
  String get dashboardReadinessAvoid => 'Avoid';

  @override
  String get dashboardReadinessAlternative => 'Alternative';

  @override
  String get dashboardReadinessStrain => 'Strain target';

  @override
  String get dashboardReadinessGoal => 'Adaptive goal';

  @override
  String get dashboardReadinessRecoveryMode => 'Recovery Mode';

  @override
  String get dashboardReadinessRecoveryModeBody =>
      'Activity goals are reduced so you can focus on rest.';

  @override
  String get dashboardReadinessWhy => 'Why this recommendation';

  @override
  String get stressDetailsHowTracked => 'How this is tracked';

  @override
  String get stressDetailsHowTrackedBody =>
      'OpenVitals estimates physiological stress locally from HRV versus your baseline, resting heart rate versus your baseline, and average heart rate compared with resting heart rate. It is a strain estimate, not a diagnosis or a mental-stress detector.';

  @override
  String get stressDetailsScale =>
      'Scale: 0-25 resting, 26-50 low, 51-75 medium, 76-100 high.';

  @override
  String get stressDetailsInputs => 'Inputs used';

  @override
  String get stressDetailsNoInputs =>
      'No usable HRV or heart-rate baseline signals were available.';

  @override
  String get stressDetailsDataCoverage => 'Data coverage';

  @override
  String get stressDetailsNoDataCoverage =>
      'No same-day HR or HRV sample coverage was available.';

  @override
  String get stressDetailsCaveats => 'Caveats';

  @override
  String get stressDetailsRelaxationPrompt =>
      'If this feels accurate, try a short breathing or mindfulness session and re-check after a quiet period.';

  @override
  String get readinessDetailsHowCalculated => 'How this is calculated';

  @override
  String get readinessDetailsSignalsUsed => 'Signals used';

  @override
  String get readinessDetailsGuidance => 'What this means';

  @override
  String get readinessDetailsCaveats => 'Caveats';

  @override
  String get readinessDetailsCaveatLocal =>
      'This is a local rule-based estimate from the data currently available in OpenVitals.';

  @override
  String get readinessDetailsCaveatNotMedical =>
      'It is not a diagnosis, medical advice, coaching, or injury prediction.';

  @override
  String get readinessDetailsCaveatMissingData =>
      'Missing permissions, sparse samples, or missing baselines lower confidence.';

  @override
  String get readinessDetailsScoreStrong => 'Strong';

  @override
  String get readinessDetailsScoreSteady => 'Steady';

  @override
  String get readinessDetailsScoreLimited => 'Limited';

  @override
  String get readinessDetailsScoreLow => 'Low';

  @override
  String get readinessDetailsScoreNeedsMoreData => 'Needs more data';

  @override
  String get bodyEnergyDetailsHowCalculatedBody =>
      'Body Energy uses recovery-side signals: sleep score, HRV status, resting heart rate, physiological stress, temperature, hydration, nutrition, and mindfulness. It estimates how much recovery capacity is visible today.';

  @override
  String get bodyEnergyDetailsScale =>
      'Scale: 80-100 strong, 60-79 steady, 40-59 limited, 0-39 low.';

  @override
  String get bodyEnergyDetailsSummary =>
      'A recovery-side score for how much energy your current body signals support today.';

  @override
  String get bodyEnergyDetailsNoSignals =>
      'No usable recovery-side signals were available.';

  @override
  String get trainingReadinessDetailsHowCalculatedBody =>
      'Training Readiness uses training-side signals: sleep, HRV status, resting heart rate, training load, intensity minutes, physiological stress, temperature, and activity context. It estimates whether harder training fits today.';

  @override
  String get trainingReadinessDetailsScale =>
      'Scale: 80-100 ready for hard training, 60-79 controlled training, 40-59 light training, 0-39 rest-focused.';

  @override
  String get trainingReadinessDetailsSummary =>
      'A training-side score for how well current recovery and load signals support exercise intensity.';

  @override
  String get trainingReadinessDetailsNoSignals =>
      'No usable training-side signals were available.';

  @override
  String dashboardGoalOf(String arg0) {
    return 'of $arg0';
  }

  @override
  String get caloriesEstimatedActiveBmr => 'No total record, est. active + BMR';

  @override
  String caloriesEstimatedValue(String arg0) {
    return 'Est. $arg0';
  }

  @override
  String dashboardWeeklyCardioLoadProgress(int arg0, int arg1) {
    return '$arg0 of $arg1';
  }

  @override
  String dashboardCardioLoadPercentOnly(int arg0) {
    return '$arg0%';
  }

  @override
  String dashboardCardioLoadPercent(int arg0) {
    return '$arg0% load';
  }

  @override
  String dashboardCardioLoadTodayDelta(int arg0) {
    return '+$arg0% today';
  }

  @override
  String get messageNoActivitiesPeriod =>
      'No activities in the selected period.';

  @override
  String get plannedWorkoutCompleted => 'Completed';

  @override
  String plannedWorkoutBlocks(int arg0) {
    return '$arg0 blocks';
  }

  @override
  String get messageNoStepUpdates => 'No step updates were recorded';

  @override
  String get messageNoDistanceUpdates => 'No distance updates were recorded';

  @override
  String get messageNoCaloriesBurned => 'No total calorie data was recorded';

  @override
  String get messageNoFloorsClimbed => 'No floors climbed data was recorded';

  @override
  String get messageNoActiveCalories => 'No active calories data was recorded';

  @override
  String get messageNoCalorieDataPeriod =>
      'No total, active, or BMR calorie data in this period.';

  @override
  String get messageNoElevation => 'No elevation data was recorded';

  @override
  String get messageNoWheelchairPushes =>
      'No wheelchair push data was recorded';

  @override
  String get messageNoSleepDaySelected => 'No sleep data for the selected day.';

  @override
  String get messageNoSleepPeriod => 'No sleep data in the selected period.';

  @override
  String get messageNoHeartPeriod =>
      'No heart rate data in the selected period.\n\nMake sure the heart rate permission is granted and a connected device has synced data.';

  @override
  String get messageNoHeartSamplesDay =>
      'No heart rate samples recorded on this day.';

  @override
  String get messageHeartEmptyHint =>
      'Try another date or check that a connected device synced point-in-time heart data.';

  @override
  String get messageNoWeightPeriod =>
      'No weight data in the selected period.\n\nSync a scale or wearable that reports weight to Health Connect.';

  @override
  String get messageNoHydrationPeriod =>
      'No beverage entries were recorded for this period.';

  @override
  String get messageNoHydrationAddedPeriod =>
      'No hydration impact was added for this period.';

  @override
  String get messageNoNutritionPeriod =>
      'No nutrition entries were recorded for this period.';

  @override
  String get messageNoMindfulnessPeriod =>
      'No mindfulness sessions were recorded for this period.';

  @override
  String get messageNoVitalsPeriod =>
      'No vitals were recorded for this period.';

  @override
  String get messageNoReadingsPeriod => 'No readings in this period.';

  @override
  String get messageNoCyclePeriod =>
      'No cycle data was recorded for this period.';

  @override
  String get messageNoSegments => 'No segments recorded.';

  @override
  String get messageNoLaps => 'No laps recorded.';

  @override
  String get messageNoRoutePoints => 'No route points recorded.';

  @override
  String get messageRouteConsentRequired =>
      'Route data is available, but route access has not been granted yet. Open Health Connect permissions from Settings to enable route previews.';

  @override
  String get messageNoRouteData => 'No route data recorded.';

  @override
  String get messageNoStages => 'No stages recorded.';

  @override
  String get messageNoKcal => 'No kcal';

  @override
  String get onboardingTagline => 'Your health data, on your device';

  @override
  String get onboardingPrivacyTitle => 'Privacy first';

  @override
  String get onboardingPrivacyBody =>
      'No account required. Data stays on your device. No cloud upload, no analytics, no ads.';

  @override
  String get healthDisclaimerTitle => 'Health disclaimer';

  @override
  String get healthDisclaimerBody =>
      'OpenVitals is for general wellness and informational use only. It is not a medical device and does not provide medical advice. It does not diagnose, treat, cure, or prevent any disease or medical condition. Always consult a qualified healthcare professional for medical advice, diagnosis, or treatment.';

  @override
  String get onboardingHealthConnectTitle => 'Powered by Health Connect';

  @override
  String get onboardingHealthConnectBody =>
      'Reads from the secure on-device Android health store and saves entries you create back to Health Connect. Works with all data imported into Health Connect.';

  @override
  String get onboardingPermissionsHeader => 'HEALTH CONNECT PERMISSIONS';

  @override
  String get onboardingGrantCore => 'Grant required Health Connect permissions';

  @override
  String get onboardingGrantAll => 'Grant required Health Connect permissions';

  @override
  String get onboardingGrantRemaining =>
      'Grant remaining available permissions';

  @override
  String get onboardingOpenRequiredPermissions =>
      'Open required Health Connect permissions';

  @override
  String get onboardingUnableOpenPermissions =>
      'Unable to open Health Connect permissions.';

  @override
  String get onboardingHealthConnectNotSupported =>
      'Health Connect is not supported on this device.';

  @override
  String get onboardingHealthConnectNeedsPlayStore =>
      'This device is running Android 13 with the standalone Health Connect app installed. Sadly, this version depends on Google Play Store services, which are missing on this device, so Health Connect rejects requests before OpenVitals can read your data. OpenVitals cannot fix or bypass this device-side Health Connect issue. The only way to solve it is to install Google Play services or upgrade to Android 14 or higher, where Health Connect is part of the operating system and does not need Google services.';

  @override
  String get onboardingHealthConnectUpdate =>
      'Health Connect needs to be installed or updated to use this app.';

  @override
  String get onboardingInstallHealthConnect => 'Install Health Connect';

  @override
  String get onboardingStatusNotSupported => 'Not supported';

  @override
  String get onboardingStatusGranted => 'Granted';

  @override
  String onboardingStatusPartiallyGranted(int arg0, int arg1) {
    return '$arg0/$arg1 granted';
  }

  @override
  String get onboardingStatusManual => 'Open settings';

  @override
  String get onboardingStatusRequired => 'Required';

  @override
  String get onboardingStatusOptional => 'Optional';

  @override
  String get onboardingCategoryActivitySleep => 'Activity & sleep';

  @override
  String get onboardingCategoryActivitySleepDesc =>
      'Health Connect will ask for:\n* Steps\n* Distance\n* Exercise\n* Sleep';

  @override
  String get onboardingCategoryHeartRecovery => 'Heart & vitals';

  @override
  String get onboardingCategoryHeartRecoveryDesc =>
      'Health Connect will ask for:\n* Heart rate\n* Resting heart rate\n* Heart rate variability';

  @override
  String get onboardingCategoryBody => 'Body';

  @override
  String get onboardingCategoryBodyDesc =>
      'Health Connect will ask for:\n* Weight\n* Height\n* Body fat\n* Lean body mass\n* Basal metabolic rate\n* Bone mass\n* Body water mass';

  @override
  String get onboardingCategoryActivityExtras => 'Activity extras';

  @override
  String get onboardingCategoryActivityExtrasDesc =>
      'Health Connect will ask for:\n* Total calories burned\n* Active calories burned\n* Floors climbed\n* Elevation gained\n* Wheelchair pushes\n* Speed\n* Power\n* Steps cadence\n* Cycling pedaling cadence\n* Planned exercise, when supported';

  @override
  String get onboardingCategoryNutritionHydration => 'Nutrition & hydration';

  @override
  String get onboardingCategoryNutritionHydrationDesc =>
      'Health Connect will ask for:\n* Hydration\n* Nutrition';

  @override
  String get onboardingCategoryManualEntryWrite => 'Manual entry write access';

  @override
  String get onboardingCategoryManualEntryWriteDesc =>
      'Health Connect will ask for write access to:\n* Exercise\n* Distance\n* Elevation gained\n* Active calories burned\n* Total calories burned\n* Exercise route\n* Hydration\n* Weight\n* Height\n* Body fat\n* Blood pressure\n* Oxygen saturation\n* Respiratory rate\n* Body temperature\n* Mindfulness, when supported';

  @override
  String get onboardingCategoryDataImportWrite => 'Data import write access';

  @override
  String get onboardingCategoryDataImportWriteDesc =>
      'Health Connect will ask for write access to imported records:\n* Activity, exercise, calories, and distance\n* Heart rate, resting heart rate, and heart rate variability\n* Body measurements\n* Hydration and nutrition\n* Sleep\n* Vitals\n* Mindfulness, when supported\n* Cycle tracking records';

  @override
  String get onboardingCategoryMindfulness => 'Mindfulness';

  @override
  String get onboardingCategoryMindfulnessDesc =>
      'Health Connect will ask for:\n* Mindfulness sessions';

  @override
  String get onboardingCategoryMindfulnessUnavailable =>
      'Mindfulness sessions require a newer Health Connect version.';

  @override
  String get onboardingCategoryAdditionalDataAccess => 'Additional data access';

  @override
  String get onboardingCategoryAdditionalDataAccessDesc =>
      'In Health Connect permissions, open OpenVitals > Additional access and set:\n* Access past data: Enable\n* Access data in the background: Enable\n* Access exercise routes: Always';

  @override
  String onboardingCategoryAdditionalDataAccessManualNote(String arg0) {
    return '$arg0\n\nIf Access exercise routes is missing from the access dialog, open Health Connect settings for OpenVitals and set it under Additional access.';
  }

  @override
  String get onboardingCategoryVitals => 'Vitals';

  @override
  String get onboardingCategoryVitalsDesc =>
      'Health Connect will ask for:\n* Blood pressure\n* Oxygen saturation\n* Respiratory rate\n* Body temperature\n* VO2 max\n* Blood glucose\n* Skin temperature, when supported';

  @override
  String get onboardingCategoryCycleTracking => 'Cycle tracking';

  @override
  String get onboardingCategoryCycleTrackingDesc =>
      'Health Connect will ask for sensitive cycle data:\n* Menstruation flow\n* Menstruation periods\n* Ovulation tests\n* Cervical mucus\n* Basal body temperature\n* Intermenstrual bleeding\n* Sexual activity';

  @override
  String get settingsAllRequestableGranted =>
      'All requestable permissions granted';

  @override
  String get settingsManualPermissionsTitle => 'Manual permissions required';

  @override
  String get settingsManualPermissionsBody =>
      'Some Health Connect permissions cannot be granted from the normal request dialog. Open Health Connect and enable them for OpenVitals.';

  @override
  String get settingsOpenHealthPermissions => 'Open Health Connect permissions';

  @override
  String get settingsDisplayGroupTitle => 'Display';

  @override
  String get settingsDisplayGroupBody => 'Language, units, and theme';

  @override
  String get settingsActivitiesGroupTitle => 'Activities';

  @override
  String get settingsActivitiesGroupBody =>
      'Rolling dates, favorite activity, recording, and offline maps';

  @override
  String get settingsSensorsGroupTitle => 'Sensors & devices';

  @override
  String get settingsSensorsGroupBody =>
      'Heart rate, cadence, and power sensors';

  @override
  String get settingsSensorsEmptyTitle => 'No sensors yet';

  @override
  String get settingsSensorsEmptyBody =>
      'Add a Bluetooth heart rate strap, cadence sensor, power meter, or footpod to use during activity recording.';

  @override
  String get settingsSensorsAddDevice => 'Add sensor';

  @override
  String get settingsSensorsEditDevice => 'Edit sensor';

  @override
  String get settingsSensorsRemoveDevice => 'Remove sensor';

  @override
  String get settingsSensorsDeviceName => 'Device name';

  @override
  String get settingsSensorsEnabled => 'Enabled';

  @override
  String settingsSensorsBatteryPercent(int arg0) {
    return 'Battery $arg0%';
  }

  @override
  String get settingsSensorsBatteryUnknown => 'Battery pending';

  @override
  String get settingsSensorsScanning => 'Scanning for nearby sensors…';

  @override
  String get settingsSensorsScanStopped => 'Scan stopped';

  @override
  String get settingsSensorsScanEmpty =>
      'No sensors found yet. Make sure your sensor is awake and close to your phone.';

  @override
  String get settingsSensorsShowAllDevices => 'Show all devices';

  @override
  String get settingsSensorsOpenBluetooth => 'Open Bluetooth settings';

  @override
  String get settingsSensorsDiscovering => 'Discovering sensor capabilities…';

  @override
  String get settingsSensorsCapabilitiesTitle => 'Capabilities';

  @override
  String get settingsSensorsCapabilityHeartRate => 'Heart rate';

  @override
  String get settingsSensorsCapabilityCyclingCadence => 'Cycling cadence';

  @override
  String get settingsSensorsCapabilityCyclingPower => 'Cycling power';

  @override
  String get settingsSensorsCapabilityCyclingSpeed => 'Cycling speed';

  @override
  String get settingsSensorsCapabilityRunningSpeedCadence =>
      'Running speed/cadence';

  @override
  String settingsSensorsCapabilityConflict(String arg0, String arg1) {
    return '$arg0 is already assigned to $arg1';
  }

  @override
  String get settingsSensorsWheelCircumference => 'Wheel circumference (mm)';

  @override
  String get activityRecordingSensorsTitle => 'Sensors';

  @override
  String get activityRecordingSensorsAddInSettings => 'Add sensors in Settings';

  @override
  String get activityRecordingSensorsNotConfigured => 'Not configured';

  @override
  String get activityRecordingSensorsConnected => 'Connected';

  @override
  String get activityRecordingSensorsConnecting => 'Connecting';

  @override
  String get activityRecordingSensorsReconnecting => 'Reconnecting';

  @override
  String get activityRecordingSensorsDisabled => 'Disabled';

  @override
  String get activityRecordingSensorsWaitingForData =>
      'Waiting for sensor data…';

  @override
  String get activityRecordingSensorsWaitingShort => '—';

  @override
  String get activityRecordingSensorsNoSignalShort => 'No signal';

  @override
  String get activityRecordingSensorsGarminBroadcastHint =>
      'Connected, but the watch is not broadcasting heart rate. On Garmin: Settings → Watch Sensors → Wrist Heart Rate → Broadcast Heart Rate, then start it on the watch. Disconnect Gadgetbridge first, or use a BLE chest strap instead.';

  @override
  String get activityRecordingSensorsRecordedTitle => 'Recorded sensor data';

  @override
  String get activityRecordingLiveHeartRate => 'Heart rate';

  @override
  String get activityRecordingLiveCadence => 'Cadence';

  @override
  String get activityRecordingLivePower => 'Power';

  @override
  String get activityRecordingLiveSpeed => 'Speed';

  @override
  String activityRecordingNotificationHeartRate(String arg0) {
    return 'HR $arg0';
  }

  @override
  String get settingsNutritionGroupTitle => 'Nutrition';

  @override
  String get settingsNutritionGroupBody =>
      'Calories data and caffeine personalization';

  @override
  String get settingsCaloriesGroupTitle => 'Calories';

  @override
  String get settingsCaloriesGroupBody => 'Total calories data';

  @override
  String get settingsCaffeineGroupTitle => 'Caffeine';

  @override
  String get settingsCaffeineGroupBody =>
      'Half-life, bedtime, sleep threshold, and personalization.';

  @override
  String get settingsRecoveryGroupTitle => 'Recovery';

  @override
  String get settingsRecoveryGroupBody =>
      'Sleep range and Body Energy calibration';

  @override
  String get settingsSleepGroupTitle => 'Sleep';

  @override
  String get settingsSleepGroupBody => 'Sleep range';

  @override
  String get settingsCycleGroupTitle => 'Menstrual cycle';

  @override
  String get settingsCycleGroupBody =>
      'Cycle data and Health Connect permissions';

  @override
  String get settingsDataImportGroupTitle => 'Data Importers';

  @override
  String get settingsDataImportGroupBody =>
      'Import Apple Health exports, route files, and FIT files';

  @override
  String get settingsPermissionsGroupTitle => 'Permissions';

  @override
  String get settingsPermissionsGroupBody =>
      'Health data access and manual permission steps';

  @override
  String get settingsHealthConnectGroupTitle => 'Health Connect';

  @override
  String get settingsHealthConnectGroupBody =>
      'Sync, permissions, access, and app lock';

  @override
  String get settingsDebugDiagnosticsGroupTitle => 'Debug diagnostics';

  @override
  String get settingsDebugDiagnosticsGroupBody =>
      'Save sanitized diagnostics logs for troubleshooting';

  @override
  String get settingsHealthConnectSyncTitle => 'Sync with Health Connect';

  @override
  String get settingsHealthConnectSyncBody =>
      'When on, OpenVitals reads and writes health data according to your permissions. When off, sync pauses without revoking access.';

  @override
  String get settingsHealthConnectManageAccess => 'Manage access';

  @override
  String get settingsHealthConnectManageAccessBody =>
      'Open Health Connect to review or change which data OpenVitals can use.';

  @override
  String get healthConnectAccessInsufficientTitle => 'Choose data to share';

  @override
  String get healthConnectAccessInsufficientBody =>
      'OpenVitals needs Health Connect access to show this information. Set up the data you want to share to continue.';

  @override
  String get healthConnectAccessDoubleCancelTitle =>
      'Permissions need attention';

  @override
  String get healthConnectAccessDoubleCancelBody =>
      'Health Connect permissions were not granted. Open Health Connect settings to choose which data to share with OpenVitals.';

  @override
  String get healthConnectSyncPaused => 'Health Connect sync is paused';

  @override
  String get healthConnectSyncInProgress => 'Syncing with Health Connect…';

  @override
  String get healthConnectDataSourceManage => 'Manage data sources';

  @override
  String get healthConnectDataSourceManageBody =>
      'See which apps write data to Health Connect and manage their access.';

  @override
  String get dashboardHealthConnectPromoTitle => 'Set up your health data';

  @override
  String get dashboardHealthConnectPromoBody =>
      'Get a unified view of your activity, sleep, and heart data from the apps and devices you already use.';

  @override
  String get dashboardHealthConnectPromoAction => 'Get started';

  @override
  String get dashboardHealthConnectSyncPausedBody =>
      'Turn sync back on in Settings to refresh your dashboard.';

  @override
  String get dashboardHealthConnectInstallAction => 'Install Health Connect';

  @override
  String get healthConnectMatchmakingTitle => 'Connect your apps';

  @override
  String get healthConnectMatchmakingBody =>
      'Find apps and devices that can share data OpenVitals is ready to read.';

  @override
  String get healthConnectMatchmakingAction => 'Find data sources';

  @override
  String get healthConnectPromoteActivityTitle => 'Unlock activity insights';

  @override
  String get healthConnectPromoteActivityBody =>
      'Allow activity data to see steps, distance, workouts, and trends in OpenVitals.';

  @override
  String get healthConnectPromoteActivitiesTitle => 'See your workouts';

  @override
  String get healthConnectPromoteActivitiesBody =>
      'Allow exercise session access to browse activities synced through Health Connect.';

  @override
  String get healthConnectPromoteCaloriesTitle => 'Track calories burned';

  @override
  String get healthConnectPromoteCaloriesBody =>
      'Allow calorie data to compare active and total burn over time.';

  @override
  String get healthConnectPromoteSleepTitle => 'See your sleep';

  @override
  String get healthConnectPromoteSleepBody =>
      'Allow sleep data to view stages, duration, and sleep score trends.';

  @override
  String get healthConnectPromoteHeartTitle => 'Monitor heart health';

  @override
  String get healthConnectPromoteHeartBody =>
      'Allow heart rate and HRV data to track resting rate and variability.';

  @override
  String get healthConnectPromoteVitalsTitle => 'Unlock vitals';

  @override
  String get healthConnectPromoteVitalsBody =>
      'Allow vitals data to see blood pressure, SpO2, and related measurements.';

  @override
  String get healthConnectPromoteBodyTitle => 'Track body metrics';

  @override
  String get healthConnectPromoteBodyBody =>
      'Allow body composition data to follow weight, BMI, and related trends.';

  @override
  String get healthConnectPromoteHydrationTitle => 'Track beverages';

  @override
  String get healthConnectPromoteHydrationBody =>
      'Allow hydration and nutrition data to see daily drinks and history.';

  @override
  String get healthConnectPromoteNutritionTitle => 'See nutrition';

  @override
  String get healthConnectPromoteNutritionBody =>
      'Allow nutrition data to review calories and macros from your sources.';

  @override
  String get healthConnectPromoteMindfulnessTitle => 'Track mindfulness';

  @override
  String get healthConnectPromoteMindfulnessBody =>
      'Allow mindfulness session data to see your practice over time.';

  @override
  String get healthConnectPromoteCycleTitle => 'Track cycle data';

  @override
  String get healthConnectPromoteCycleBody =>
      'Allow menstrual cycle data to view flow, symptoms, and related records.';

  @override
  String get healthConnectPromoteReadinessTitle => 'Improve readiness insights';

  @override
  String get healthConnectPromoteReadinessBody =>
      'Allow additional Health Connect data to refine daily readiness scores.';

  @override
  String get healthConnectNewPermissionsTitle => 'New data available';

  @override
  String get healthConnectNewPermissionsBody =>
      'OpenVitals can now read additional health data types. Grant access to use the new features.';

  @override
  String get healthConnectNewPermissionsAction => 'Review permissions';

  @override
  String get privacyReconsentTitle => 'Privacy policy updated';

  @override
  String get privacyReconsentBody =>
      'Our privacy policy has changed. Review and accept to continue syncing with Health Connect.';

  @override
  String get privacyReconsentAction => 'Review policy';

  @override
  String get dashboardSummaryToday => 'Today';

  @override
  String get settingsDebugLogsTitle => 'Sanitized diagnostics logs';

  @override
  String get settingsDebugLogsBody =>
      'Share or save OpenVitals diagnostics log entries as a text file. The export drops or redacts identifiers, locations, dates, URIs, raw sensor payloads, and unrelated app logs before writing.';

  @override
  String get settingsDebugLogsShare => 'Share logs';

  @override
  String get settingsDebugLogsShareChooserTitle => 'Share diagnostics logs';

  @override
  String get settingsDebugLogsShareFailed => 'Could not share diagnostics logs';

  @override
  String get settingsDebugLogsSave => 'Save logs';

  @override
  String get settingsDebugLogsSaved => 'Debug logs saved';

  @override
  String get settingsDebugLogsSaveFailed => 'Could not save diagnostics logs';

  @override
  String get settingsPrivacyPolicyLink => 'View privacy policy';

  @override
  String get settingsAppLockTitle => 'App lock';

  @override
  String get settingsAppLockBody => 'Require device unlock to open OpenVitals.';

  @override
  String get onboardingCoreRequired =>
      'Activity, sleep, and heart rate access are needed to get started. You can add more data types later from Settings.';

  @override
  String get settingsLanguageTitle => 'Language';

  @override
  String get settingsLanguageBody =>
      'Choose app language or follow your system setting.';

  @override
  String get settingsLanguageSystem => 'System';

  @override
  String get settingsLanguageEnglish => 'English';

  @override
  String get settingsLanguageSpanish => 'Spanish';

  @override
  String get settingsLanguageGerman => 'German';

  @override
  String get settingsLanguageItalian => 'Italian';

  @override
  String get settingsLanguageEstonian => 'Estonian';

  @override
  String get settingsUnitsTitle => 'Units';

  @override
  String get settingsUnitsBody =>
      'Choose how distances, weights, hydration, and temperature are displayed.';

  @override
  String get settingsUnitMetric => 'Metric';

  @override
  String get settingsUnitImperial => 'Imperial';

  @override
  String get settingsThemeTitle => 'Theme';

  @override
  String get settingsThemeBody =>
      'Choose app appearance independently from Android dark mode.';

  @override
  String get settingsThemeSystem => 'System';

  @override
  String get settingsThemeLight => 'Light';

  @override
  String get settingsThemeDark => 'Dark';

  @override
  String get settingsThemeAmoled => 'AMOLED';

  @override
  String get settingsDynamicColorTitle => 'Dynamic color (Material You)';

  @override
  String get settingsDynamicColorBody =>
      'Tint OpenVitals from your Android wallpaper. Off uses the OpenVitals blue and teal brand palette.';

  @override
  String get settingsActivityWeekTitle => 'Rolling dates';

  @override
  String get settingsActivityWeekBody =>
      'Use rolling 7, 30, and 365-day windows instead of calendar week, month, and year.';

  @override
  String get settingsActivityWeekMondayToSunday => 'Calendar';

  @override
  String get settingsActivityWeekLast7Days => 'Rolling';

  @override
  String get settingsFavoriteActivityTitle => 'Favorite activity';

  @override
  String get settingsFavoriteActivityBody =>
      'Use the latest recorded activity by default, or choose one activity type to always preselect.';

  @override
  String get settingsFavoriteActivityLatest => 'Use latest';

  @override
  String get settingsActivityRecordingTitle => 'Activity recording';

  @override
  String get settingsActivityRecordingBody =>
      'Tune live GPS recording without changing the saved activity details workflow.';

  @override
  String get settingsActivityRecordingKeepScreenOnTitle => 'Screen always on';

  @override
  String get settingsActivityRecordingKeepScreenOnBody =>
      'Keep the screen awake while an activity recording is active.';

  @override
  String get settingsActivityRecordingAutoIdleTitle => 'Auto-idle';

  @override
  String get settingsActivityRecordingAutoIdleBody =>
      'Pause moving time when you stop for longer than the selected timeout.';

  @override
  String get settingsActivityRecordingIdleTimeoutTitle => 'Idle timeout';

  @override
  String get settingsActivityRecordingAccuracyTitle => 'Required GPS accuracy';

  @override
  String get settingsActivityRecordingRouteGapTitle =>
      'New route segment after gap';

  @override
  String get settingsActivityRecordingTimeIntervalTitle =>
      'Recording time interval';

  @override
  String get settingsActivityRecordingDistanceIntervalTitle =>
      'Recording distance interval';

  @override
  String get settingsActivityRecordingBarometerTitle => 'Barometer climb';

  @override
  String get settingsActivityRecordingBarometerBody =>
      'Use pressure changes for climb when the device has a barometer.';

  @override
  String get settingsActivityRecordingRestBellTitle => 'Rest timer bell';

  @override
  String get settingsActivityRecordingRestBellBody =>
      'Play a soft bell when set rest countdowns finish.';

  @override
  String get settingsActivityRecordingVoiceTitle => 'Voice announcements';

  @override
  String get settingsActivityRecordingVoiceBody =>
      'Speak periodic progress, idle/resume, and lap updates while recording.';

  @override
  String get settingsActivityRecordingVoiceTimeTitle => 'Announce by time';

  @override
  String get settingsActivityRecordingVoiceDistanceTitle =>
      'Announce by distance';

  @override
  String get settingsActivityRecordingVoiceIdleTitle => 'Idle announcements';

  @override
  String get settingsActivityRecordingVoiceIdleBody =>
      'Say when auto-idle starts and when recording resumes.';

  @override
  String get settingsActivityRecordingVoiceLapTitle => 'Lap announcements';

  @override
  String get settingsActivityRecordingVoiceLapBody =>
      'Say a progress summary when you mark a lap.';

  @override
  String settingsActivityRecordingSeconds(int arg0) {
    return '$arg0 s';
  }

  @override
  String get settingsActivityRecordingHalfSecond => '0.5 s';

  @override
  String settingsActivityRecordingMeters(int arg0) {
    return '$arg0 m';
  }

  @override
  String get settingsActivityRecordingAuto => 'Auto';

  @override
  String get settingsActivityRecordingOff => 'Off';

  @override
  String get settingsCalorieDataTitle => 'Total calories data';

  @override
  String get settingsCalorieDataBody =>
      'Show plain Health Connect total calories by default. Turn on OpenVitals calculations to fill missing totals from active calories and BMR.';

  @override
  String get settingsCaffeineTitle => 'Caffeine model';

  @override
  String get settingsCaffeineBody =>
      'These values personalize caffeine level, bedtime forecast, and safe-sleep insights. Entries remain in Health Connect.';

  @override
  String get settingsBodyProfileTitle => 'Body profile';

  @override
  String get settingsBodyProfileBody =>
      'Age, weight, and heart rate personalize Body Energy and Caffeine estimates. All fields are optional.';

  @override
  String get settingsBodyProfileWeight => 'Weight';

  @override
  String get settingsSleepRangeTitle => 'Sleep range';

  @override
  String get settingsSleepRangeBody =>
      'Choose which day sleep sessions are assigned to.';

  @override
  String get settingsSleepRangeRolling24h => 'Rolling 24h';

  @override
  String get settingsSleepRangeNoon => 'Noon';

  @override
  String get settingsSleepRangeEvening => '18:00';

  @override
  String get settingsCyclePermissionsTitle => 'Cycle permissions';

  @override
  String settingsCyclePermissionsGranted(int arg0, int arg1) {
    return '$arg0/$arg1 cycle permissions granted.';
  }

  @override
  String get settingsAppleHealthImportTitle => 'Apple Health Importer';

  @override
  String get settingsAppleHealthImportBody =>
      'Import Health Connect-compatible records from Apple Health export.xml or export.zip, with duplicate checks and a shareable diagnostics report.';

  @override
  String settingsAppleHealthImportPermissions(int arg0, int arg1) {
    return '$arg0/$arg1 import permissions granted.';
  }

  @override
  String get settingsAppleHealthImportGrant => 'Grant import permissions';

  @override
  String get settingsAppleHealthImportAction => 'Import Apple Health export';

  @override
  String get settingsAppleHealthImportAnalyzeAction =>
      'Analyze Apple Health export';

  @override
  String get settingsAppleHealthImportChooseAnotherAction =>
      'Choose another Apple Health export';

  @override
  String get settingsAppleHealthImportSelectedAction =>
      'Import selected categories';

  @override
  String get settingsAppleHealthImportAnalyzing => 'Analyzing...';

  @override
  String get settingsAppleHealthImporting => 'Importing...';

  @override
  String get settingsAppleHealthImportProgressQueued => 'Queued';

  @override
  String get settingsAppleHealthImportProgressParsing => 'Scanning export';

  @override
  String get settingsAppleHealthImportProgressConverting =>
      'Converting records';

  @override
  String get settingsAppleHealthImportProgressCheckingDuplicates =>
      'Checking duplicates';

  @override
  String get settingsAppleHealthImportProgressWriting => 'Writing records';

  @override
  String get settingsAppleHealthImportProgressFinishing => 'Finalizing import';

  @override
  String get settingsAppleHealthImportProgressBuildingReport =>
      'Building report';

  @override
  String get settingsAppleHealthImportProgressComplete => 'Complete';

  @override
  String settingsAppleHealthImportProgress(String arg0, int arg1, int arg2) {
    return '$arg0. Scanned $arg1 items, imported $arg2 records.';
  }

  @override
  String settingsAppleHealthImportProgressWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Selected $arg2/$arg3 records, imported $arg4.';
  }

  @override
  String settingsAppleHealthImportProgressWithScanPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
    int arg5,
    int arg6,
  ) {
    return '$arg0%. $arg1. Scanned $arg2/$arg3 items. Selected $arg4/$arg5 records, imported $arg6.';
  }

  @override
  String get settingsAppleHealthImportBackground =>
      'Import continues in the background while you leave the app.';

  @override
  String get settingsAppleHealthImportNotificationChannel =>
      'Apple Health imports';

  @override
  String get settingsAppleHealthImportNotificationTitle =>
      'Importing Apple Health export';

  @override
  String settingsAppleHealthImportNotificationText(
    String arg0,
    int arg1,
    int arg2,
  ) {
    return '$arg0. Scanned $arg1, imported $arg2.';
  }

  @override
  String settingsAppleHealthImportNotificationTextWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Selected $arg2/$arg3, imported $arg4.';
  }

  @override
  String settingsAppleHealthImportNotificationTextWithScanPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Scanned $arg2/$arg3, imported $arg4.';
  }

  @override
  String settingsAppleHealthImportResult(
    int arg0,
    int arg1,
    int arg2,
    int arg3,
    int arg4,
    int arg5,
  ) {
    return 'Imported $arg0. Duplicates $arg1. Not selected $arg2. Unsupported $arg3. Skipped $arg4. Failed $arg5.';
  }

  @override
  String get settingsAppleHealthImportRoutesIncomplete =>
      'Health records were imported, but some workout routes were unavailable because the ZIP ended unexpectedly. The import report lists affected activities for manual recovery.';

  @override
  String settingsAppleHealthImportAnalysisResult(
    int arg0,
    int arg1,
    int arg2,
    int arg3,
  ) {
    return 'Scanned $arg0 items. Found $arg1 compatible records. Unsupported $arg2. Failed $arg3.';
  }

  @override
  String get settingsAppleHealthImportChooseCategories =>
      'Choose what to write to Health Connect.';

  @override
  String settingsAppleHealthImportCategoryCount(int arg0) {
    return '$arg0 records';
  }

  @override
  String settingsAppleHealthImportCategoryCountRoutes(int arg0, int arg1) {
    return '$arg0 records, $arg1 with routes';
  }

  @override
  String get settingsAppleHealthImportCategoryWorkouts => 'Workouts and routes';

  @override
  String get settingsAppleHealthImportCategoryWorkoutsDesc =>
      'Exercise sessions and attached workout route geometry.';

  @override
  String get settingsAppleHealthImportCategoryActivity => 'Activity metrics';

  @override
  String get settingsAppleHealthImportCategoryActivityDesc =>
      'Steps, distance, calories, floors, elevation, wheelchair pushes, and speed.';

  @override
  String get settingsAppleHealthImportCategoryHeart => 'Heart';

  @override
  String get settingsAppleHealthImportCategoryHeartDesc =>
      'Heart rate and resting heart rate records.';

  @override
  String get settingsAppleHealthImportCategorySleep => 'Sleep';

  @override
  String get settingsAppleHealthImportCategorySleepDesc =>
      'Sleep sessions and stages.';

  @override
  String get settingsAppleHealthImportCategoryBody => 'Body measurements';

  @override
  String get settingsAppleHealthImportCategoryBodyDesc =>
      'Weight, height, body fat, lean mass, BMR, bone mass, and body water.';

  @override
  String get settingsAppleHealthImportCategoryVitals => 'Vitals';

  @override
  String get settingsAppleHealthImportCategoryVitalsDesc =>
      'Blood pressure, oxygen saturation, respiratory rate, body temperature, blood glucose, and VO2 max.';

  @override
  String get settingsAppleHealthImportCategoryNutrition => 'Nutrition';

  @override
  String get settingsAppleHealthImportCategoryNutritionDesc =>
      'Food energy, macros, caffeine, minerals, and vitamins.';

  @override
  String get settingsAppleHealthImportCategoryHydration => 'Hydration';

  @override
  String get settingsAppleHealthImportCategoryHydrationDesc =>
      'Water intake records.';

  @override
  String get settingsAppleHealthImportCategoryMindfulness => 'Mindfulness';

  @override
  String get settingsAppleHealthImportCategoryMindfulnessDesc =>
      'Mindfulness session records when Health Connect supports them.';

  @override
  String get settingsAppleHealthImportCategoryCycle => 'Cycle tracking';

  @override
  String get settingsAppleHealthImportCategoryCycleDesc =>
      'Menstruation, ovulation, cervical mucus, bleeding, basal body temperature, and sexual activity records.';

  @override
  String get settingsAppleHealthImportCopyReport => 'Copy report';

  @override
  String get settingsAppleHealthImportCopyError => 'Copy error';

  @override
  String get settingsAppleHealthImportSaveReport => 'Download full report';

  @override
  String get settingsAppleHealthImportReportCopied => 'Import report copied.';

  @override
  String get settingsAppleHealthImportErrorCopied => 'Import error copied.';

  @override
  String get settingsAppleHealthImportReportSaved => 'Import report saved.';

  @override
  String get settingsAppleHealthImportReportSaveFailed =>
      'Unable to save import report.';

  @override
  String settingsAppleHealthImportError(String arg0) {
    return 'Import failed: $arg0';
  }

  @override
  String get settingsAppleHealthImportPermissionDenied =>
      'Access to the selected file was lost, so the import couldn\'t continue. Select the same Apple Health export again to pick up right where it left off.';

  @override
  String get settingsRouteImportTitle => 'GPX/KML/KMZ Importer';

  @override
  String get settingsRouteImportBody =>
      'Import GPX, KML, or KMZ route files. Review one file before saving, or bulk import multiple files directly into Health Connect.';

  @override
  String settingsRouteImportPermissions(int arg0, int arg1) {
    return '$arg0/$arg1 route import permissions granted.';
  }

  @override
  String get settingsRouteImportGrant => 'Grant route import permissions';

  @override
  String get settingsRouteImportAction => 'Import GPX/KML/KMZ file';

  @override
  String get settingsRouteImportBulkAction => 'Bulk import GPX/KML/KMZ files';

  @override
  String get settingsRouteImporting => 'Importing routes...';

  @override
  String settingsRouteImportProgress(int arg0, int arg1, int arg2, int arg3) {
    return 'File $arg0/$arg1. Imported $arg2, failed $arg3.';
  }

  @override
  String settingsRouteImportResult(int arg0, int arg1, int arg2) {
    return 'Imported $arg0. Failed $arg1. Selected $arg2.';
  }

  @override
  String settingsRouteImportError(String arg0) {
    return 'Route import warning: $arg0';
  }

  @override
  String get settingsFitImportTitle => 'FIT Importer';

  @override
  String get settingsFitImportBody =>
      'Import FIT activity, course, or workout files, review detected details, and choose whether to save them to Health Connect.';

  @override
  String get settingsFitImportAction => 'Import FIT file';

  @override
  String get settingsOfflineMapsTitle => 'Offline maps';

  @override
  String get settingsOfflineMapsBody =>
      'Import PMTiles or Mapsforge .map/.maps packs for fully offline activity maps. Protomaps-compatible PMTiles basemaps and Mapsforge maps are supported.';

  @override
  String get settingsOfflineMapsEmpty => 'No offline maps imported yet.';

  @override
  String get settingsOfflineMapsFormatPmtiles => 'PMTiles';

  @override
  String get settingsOfflineMapsFormatMapsforge => 'Mapsforge';

  @override
  String get settingsOfflineMapsRenderFormatTitle => 'Render format';

  @override
  String settingsOfflineMapsRenderFormatOption(String arg0, int arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String get settingsOfflineMapsRenderFormatBody =>
      'OpenVitals renders every imported pack in the selected format together.';

  @override
  String settingsOfflineMapsPackDetail(String arg0, String arg1, String arg2) {
    return '$arg0 • $arg1 • $arg2';
  }

  @override
  String get settingsOfflineMapsImportAction => 'Import offline map';

  @override
  String get settingsOfflineMapsImporting => 'Importing...';

  @override
  String get settingsOfflineMapsImportProgressQueued => 'Queued';

  @override
  String get settingsOfflineMapsImportProgressCopying => 'Copying map';

  @override
  String get settingsOfflineMapsImportProgressComplete => 'Complete';

  @override
  String settingsOfflineMapsImportProgress(String arg0) {
    return '$arg0';
  }

  @override
  String settingsOfflineMapsImportProgressWithPercent(String arg0, int arg1) {
    return '$arg0 • $arg1%';
  }

  @override
  String get settingsOfflineMapsImportBackground =>
      'Import continues in the background while you leave the app.';

  @override
  String settingsOfflineMapsImportResult(String arg0, String arg1) {
    return 'Imported $arg0 ($arg1).';
  }

  @override
  String settingsOfflineMapsImportError(String arg0) {
    return 'Map import failed: $arg0';
  }

  @override
  String get settingsOfflineMapsImportNotificationChannel =>
      'Offline map imports';

  @override
  String get settingsOfflineMapsImportNotificationTitle =>
      'Importing offline map';

  @override
  String settingsOfflineMapsImportNotificationText(String arg0) {
    return '$arg0.';
  }

  @override
  String settingsOfflineMapsImportNotificationTextWithPercent(
    String arg0,
    int arg1,
  ) {
    return '$arg0 • $arg1%.';
  }

  @override
  String get settingsOfflineMapsHelpPrompt =>
      'Do you want to learn how to add offline maps? Go to:';

  @override
  String get settingsOfflineMapsHelpLink => 'Open offline maps guide';

  @override
  String get sectionSupport => 'Support';

  @override
  String get settingsSupportTitle => 'Support OpenVitals';

  @override
  String get settingsSupportBody =>
      'Report bugs, join community support discussions, or help fund ongoing development.';

  @override
  String get settingsSupportIssuesAction => 'Report an issue';

  @override
  String get settingsSupportDiscussionAction => 'Join Zulip discussions';

  @override
  String get settingsSupportAction => 'Open Liberapay';

  @override
  String get crashReportEmailChooserTitle => 'Email OpenVitals report';

  @override
  String get crashReportFallbackTitle => 'No email app found';

  @override
  String crashReportFallbackBody(String arg0) {
    return 'Copy the report or save it as a text file, then send it to $arg0 later.';
  }

  @override
  String get crashReportFallbackCopy => 'Copy report';

  @override
  String get crashReportFallbackSave => 'Save text file';

  @override
  String get crashReportFallbackCopied => 'Report copied.';

  @override
  String get crashReportFallbackSaved => 'Report saved.';

  @override
  String get crashReportFallbackSaveFailed => 'Could not save report.';

  @override
  String get crashReportFallbackSaveUnavailable =>
      'No file saver found. Report copied.';

  @override
  String get crashReportClipboardLabel => 'OpenVitals report';

  @override
  String get settingsPrivacyNoAccount => 'No account required';

  @override
  String get settingsPrivacyNoCloud => 'No cloud sync of health data';

  @override
  String get settingsPrivacyNoAnalytics => 'No analytics SDK';

  @override
  String get settingsPrivacyNoAds => 'No ads or third-party tracking';

  @override
  String get settingsPrivacyOnDevice => 'Data stays on your device';

  @override
  String get settingsPrivacyReadOnly =>
      'Read-only except entries you explicitly log';

  @override
  String settingsAppVersion(String arg0, int arg1) {
    return 'Version $arg0 ($arg1)';
  }

  @override
  String get detailMetrics => 'Metrics';

  @override
  String get detailSessionDetails => 'Session details';

  @override
  String get detailDuration => 'Duration';

  @override
  String get detailMovingTime => 'Moving time';

  @override
  String get detailType => 'Type';

  @override
  String get detailStarted => 'Started';

  @override
  String get detailEnded => 'Ended';

  @override
  String get detailStartZone => 'Start zone';

  @override
  String get detailEndZone => 'End zone';

  @override
  String get detailRecording => 'Recording';

  @override
  String get detailSourcePackage => 'Source package';

  @override
  String get detailDeviceType => 'Device type';

  @override
  String get detailDeviceMaker => 'Device maker';

  @override
  String get detailDeviceModel => 'Device model';

  @override
  String get detailLastModified => 'Last modified';

  @override
  String get detailRecordId => 'Record id';

  @override
  String get detailClientRecordId => 'Client record id';

  @override
  String get detailClientVersion => 'Client version';

  @override
  String get detailPlannedSessionId => 'Planned session id';

  @override
  String get detailNotes => 'Notes';

  @override
  String get detailTitle => 'Title';

  @override
  String get detailTime => 'Time';

  @override
  String get detailRepetitions => 'Repetitions';

  @override
  String get detailSet => 'Set';

  @override
  String get detailLength => 'Length';

  @override
  String get detailSegments => 'Segments';

  @override
  String get detailLaps => 'Laps';

  @override
  String detailLap(int arg0) {
    return 'Lap $arg0';
  }

  @override
  String get detailRoute => 'Route';

  @override
  String get detailStatus => 'Status';

  @override
  String get detailStatusAvailable => 'Available';

  @override
  String get detailPoints => 'Points';

  @override
  String get detailStartPoint => 'Start point';

  @override
  String get detailEndPoint => 'End point';

  @override
  String detailAltitude(String arg0) {
    return 'Altitude $arg0';
  }

  @override
  String detailHorizontalAccuracy(String arg0) {
    return 'Horizontal accuracy $arg0';
  }

  @override
  String detailVerticalAccuracy(String arg0) {
    return 'Vertical accuracy $arg0';
  }

  @override
  String get detailStageEvents => 'Stage events';

  @override
  String get detailStages => 'Stages';

  @override
  String get detailSleepSession => 'Sleep session';

  @override
  String get recordingActivelyRecorded => 'Actively recorded';

  @override
  String get recordingAutomaticallyRecorded => 'Automatically recorded';

  @override
  String get recordingManualEntry => 'Manual entry';

  @override
  String get recordingUnknown => 'Unknown';

  @override
  String get deviceWatch => 'Watch';

  @override
  String get devicePhone => 'Phone';

  @override
  String get deviceScale => 'Scale';

  @override
  String get deviceRing => 'Ring';

  @override
  String get deviceHeadMounted => 'Head-mounted';

  @override
  String get deviceFitnessBand => 'Fitness band';

  @override
  String get deviceChestStrap => 'Chest strap';

  @override
  String get deviceSmartDisplay => 'Smart display';

  @override
  String get sleepStageAwake => 'Awake';

  @override
  String get sleepStageSleeping => 'Sleeping';

  @override
  String get sleepStageOutOfBed => 'Out of bed';

  @override
  String get sleepStageLight => 'Light';

  @override
  String get sleepStageDeep => 'Deep';

  @override
  String get sleepStageRem => 'REM';

  @override
  String get sleepStageAwakeInBed => 'Awake in bed';

  @override
  String get sleepStageUnknown => 'Unknown';

  @override
  String get sleepStagesShareTitle => 'Share of time in bed';

  @override
  String get cyclePermissionsMissingTitle => 'Cycle permissions missing';

  @override
  String get cyclePermissionsMissingBody =>
      'Grant cycle tracking permissions to show period days, ovulation tests, cervical mucus, and basal temperature.';

  @override
  String get cycleObservationMenstruationPeriod => 'Menstruation period';

  @override
  String get cycleObservationMenstruationFlow => 'Menstruation flow';

  @override
  String get cycleObservationOvulationTest => 'Ovulation test';

  @override
  String get cycleObservationCervicalMucus => 'Cervical mucus';

  @override
  String get cycleObservationBasalBodyTemperature => 'Basal body temperature';

  @override
  String get cycleObservationIntermenstrualBleeding =>
      'Intermenstrual bleeding';

  @override
  String get cycleObservationSexualActivity => 'Sexual activity';

  @override
  String get cycleProtectionProtected => 'Protected';

  @override
  String get cycleProtectionUnprotected => 'Unprotected';

  @override
  String get cycleProtectionUnknown => 'Protection unknown';

  @override
  String cycleBasalTemperatureValue(String arg1) {
    return '%1\$.1f C · $arg1';
  }

  @override
  String cycleDaysValue(int arg0, String arg1) {
    return '$arg0 $arg1';
  }

  @override
  String get cycleDaySingular => 'day';

  @override
  String get cycleDayPlural => 'days';

  @override
  String get cycleFlowLight => 'Light';

  @override
  String get cycleFlowMedium => 'Medium';

  @override
  String get cycleFlowHeavy => 'Heavy';

  @override
  String get cycleOvulationPositive => 'Positive';

  @override
  String get cycleOvulationHigh => 'High';

  @override
  String get cycleOvulationNegative => 'Negative';

  @override
  String get cycleOvulationInconclusive => 'Inconclusive';

  @override
  String get cycleMucusDry => 'Dry';

  @override
  String get cycleMucusSticky => 'Sticky';

  @override
  String get cycleMucusCreamy => 'Creamy';

  @override
  String get cycleMucusWatery => 'Watery';

  @override
  String get cycleMucusEggWhite => 'Egg white';

  @override
  String get cycleMucusUnusual => 'Unusual';

  @override
  String get cycleMucusLight => 'light';

  @override
  String get cycleMucusMedium => 'medium';

  @override
  String get cycleMucusHeavy => 'heavy';

  @override
  String cycleMucusValue(String arg0, String arg1) {
    return '$arg0, $arg1';
  }

  @override
  String get measurementLocationArmpit => 'Armpit';

  @override
  String get measurementLocationFinger => 'Finger';

  @override
  String get measurementLocationForehead => 'Forehead';

  @override
  String get measurementLocationMouth => 'Mouth';

  @override
  String get measurementLocationRectum => 'Rectum';

  @override
  String get measurementLocationTemporalArtery => 'Temporal artery';

  @override
  String get measurementLocationToe => 'Toe';

  @override
  String get measurementLocationEar => 'Ear';

  @override
  String get measurementLocationWrist => 'Wrist';

  @override
  String get measurementLocationVagina => 'Vagina';

  @override
  String get measurementLocationUnknown => 'Measurement location unknown';

  @override
  String get weekdayMondayShort => 'M';

  @override
  String get weekdayTuesdayShort => 'T';

  @override
  String get weekdayWednesdayShort => 'W';

  @override
  String get weekdayThursdayShort => 'T';

  @override
  String get weekdayFridayShort => 'F';

  @override
  String get weekdaySaturdayShort => 'S';

  @override
  String get weekdaySundayShort => 'S';

  @override
  String get vitalsPermissionsNeededTitle => 'Vitals permissions needed';

  @override
  String get vitalsPermissionsNeededBody =>
      'Grant blood pressure, oxygen saturation, respiratory rate, temperature, VO2 max, and glucose permissions to fill this screen.';

  @override
  String get vitalsRespiratoryRateReadings => 'Respiratory rate readings';

  @override
  String get vitalsBodyTemperatureReadings => 'Body temperature readings';

  @override
  String get heartRateHealthChecksTitle => 'Heart rate checks';

  @override
  String get heartRateHighTitle => 'High heart rate';

  @override
  String get heartRateLowTitle => 'Low heart rate';

  @override
  String heartRateSamplesAtOrAbove(int arg0) {
    return 'Samples at/above $arg0 bpm';
  }

  @override
  String heartRateSamplesAtOrBelow(int arg0) {
    return 'Samples at/below $arg0 bpm';
  }

  @override
  String heartRateDaysAtOrAbove(int arg0) {
    return 'Days at/above $arg0 bpm';
  }

  @override
  String heartRateDaysAtOrBelow(int arg0) {
    return 'Days at/below $arg0 bpm';
  }

  @override
  String get cdDecreaseHrThreshold => 'Decrease heart rate threshold';

  @override
  String get cdIncreaseHrThreshold => 'Increase heart rate threshold';

  @override
  String get mealBreakfast => 'Breakfast';

  @override
  String get mealLunch => 'Lunch';

  @override
  String get mealDinner => 'Dinner';

  @override
  String get mealSnack => 'Snack';

  @override
  String get mealGeneric => 'Meal';

  @override
  String macroProteinShort(String arg0) {
    return 'P ${arg0}g';
  }

  @override
  String macroCarbsShort(String arg0) {
    return 'C ${arg0}g';
  }

  @override
  String macroFatShort(String arg0) {
    return 'F ${arg0}g';
  }

  @override
  String macroFiber(String arg0) {
    return 'fiber ${arg0}g';
  }

  @override
  String macroSugar(String arg0) {
    return 'sugar ${arg0}g';
  }

  @override
  String get caffeineSectionOverview => 'Overview';

  @override
  String get caffeineSectionDashboard => 'Dashboard';

  @override
  String get caffeineSectionAnalytics => 'Analytics';

  @override
  String get caffeineSectionSleep => 'Sleep impact';

  @override
  String get caffeineSectionSources => 'Sources';

  @override
  String get caffeineSectionEntries => 'Entries';

  @override
  String get caffeineSectionScience => 'Science';

  @override
  String get caffeineSetupTitle => 'Personalize caffeine insights';

  @override
  String get caffeineSetupBody =>
      'OpenVitals found caffeine data. Personalization improves the caffeine curve and bedtime forecast.';

  @override
  String get caffeineCurrentTitle => 'Active caffeine';

  @override
  String get caffeineTodayTotal => 'Today total';

  @override
  String get caffeineTimeToSafe => 'Time to safe';

  @override
  String get caffeineSleepStatusUnlikely => 'Sleep impact unlikely';

  @override
  String caffeineSleepStatusUnlikelyBody(String arg0, String arg1) {
    return '$arg0 active now, below your $arg1 sleep threshold.';
  }

  @override
  String get caffeineSleepStatusElevatedNow => 'Elevated now';

  @override
  String caffeineSleepStatusElevatedNowBody(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return '$arg0 active now. Estimated below threshold in $arg1; bedtime forecast is $arg2 at $arg3.';
  }

  @override
  String get caffeineSleepStatusMayAffect => 'May affect sleep';

  @override
  String caffeineSleepStatusMayAffectBody(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Bedtime forecast is $arg0 at $arg1, above your $arg2 threshold.';
  }

  @override
  String get caffeinePeriodTotal => 'Period total';

  @override
  String get caffeineDailyAverage => 'Daily average';

  @override
  String get caffeineLoggedDays => 'Logged days';

  @override
  String get caffeinePeakDay => 'Peak day';

  @override
  String caffeinePeakDayValue(String arg0, String arg1) {
    return '$arg0 - $arg1';
  }

  @override
  String get caffeineCurveTitle => 'Caffeine curve';

  @override
  String caffeineThresholdLine(String arg0) {
    return 'Sleep threshold $arg0';
  }

  @override
  String get caffeineBedtimeForecast => 'Bedtime forecast';

  @override
  String caffeineBedtimeSummary(String arg0, String arg1) {
    return 'At $arg0 with threshold $arg1';
  }

  @override
  String get caffeineSafeNights => 'Safe nights';

  @override
  String get caffeineSafeStreak => 'Safe streak';

  @override
  String get caffeineTopSource => 'Top source';

  @override
  String get caffeineSleepThreshold => 'Sleep threshold';

  @override
  String get caffeineDailyImpact => 'Daily and bedtime impact';

  @override
  String get caffeineSafeCalendar => 'Safe-night calendar';

  @override
  String get caffeineSources => 'Source apps';

  @override
  String get caffeineItems => 'Items';

  @override
  String get caffeineInferredCategories => 'Inferred categories';

  @override
  String get caffeineTimeOfDay => 'Time of day';

  @override
  String get caffeineEntry => 'Caffeine entry';

  @override
  String caffeineInferredCategory(String arg0) {
    return 'Category: $arg0';
  }

  @override
  String caffeineCatalogMatch(String arg0) {
    return 'Catalog: $arg0';
  }

  @override
  String get caffeineCategory => 'Category';

  @override
  String get caffeineCatalog => 'Catalog';

  @override
  String caffeineCatalogMatchDetail(String arg0, String arg1, String arg2) {
    return '$arg0, typical $arg1, $arg2 match';
  }

  @override
  String get caffeineHealthConnectSourceLabel => 'Source';

  @override
  String get caffeineHealthConnectMealLabel => 'Meal';

  @override
  String get caffeineHealthConnectDurationLabel => 'Duration';

  @override
  String caffeineCurrentContribution(String arg0) {
    return '$arg0 active';
  }

  @override
  String get caffeineCurrentContributionLabel => 'Current';

  @override
  String get caffeineDose => 'Dose';

  @override
  String get caffeinePeak => 'Peak';

  @override
  String get caffeinePeakTime => 'Peak time';

  @override
  String get caffeineContributionCurve => 'Contribution curve';

  @override
  String get caffeineEmpty =>
      'No caffeine entries for this period. Caffeinated drinks added through hydration or nutrition will appear here when Health Connect includes caffeine.';

  @override
  String get caffeineScienceTitle => 'How the estimate works';

  @override
  String get caffeineScienceBody =>
      'OpenVitals reads caffeine from Health Connect nutrition records in milligrams, then estimates absorption across your configured absorption window and exponential elimination from your personalized half-life.';

  @override
  String get caffeineScienceMeasurements => 'Measurements used';

  @override
  String get caffeineScienceMeasurementsBody =>
      'The recorded dose always comes from Health Connect. Start/end time, entry name, meal type, and data-origin package are used for timing, matching, and analytics labels. Catalog matches only annotate entries; they never replace the recorded dose.';

  @override
  String get caffeineScienceLimits =>
      'This is a practical population model, not medical advice. Pregnancy, medications, liver disease, genetics, smoking, alcohol, sensitivity, and habituation can all shift caffeine response.';

  @override
  String get caffeineReferencesTitle => 'Research and references';

  @override
  String get caffeineReferenceDrake => 'Caffeine timing and sleep, Drake 2013';

  @override
  String get caffeineReferenceNehlig =>
      'Individual caffeine metabolism, Nehlig 2018';

  @override
  String get caffeineReferenceEfsa => 'EFSA caffeine safety and sleep notes';

  @override
  String get caffeineReferenceHealthConnect =>
      'Health Connect nutrition record fields';

  @override
  String get unknownSource => 'Unknown source';

  @override
  String get achievementsLegacyTitle => 'Legacy activity badges';

  @override
  String achievementsProgressSummary(int arg0, int arg1) {
    return '$arg0 of $arg1 unlocked';
  }

  @override
  String achievementsDataWindow(String arg0, String arg1, String arg2) {
    return '$arg0 to $arg1 · $arg2 tracked days';
  }

  @override
  String get achievementsTrackedDays => 'Tracked days';

  @override
  String get achievementsBestSteps => 'Best steps';

  @override
  String get achievementsTotalDistance => 'Total distance';

  @override
  String get achievementsBestFloors => 'Best floors';

  @override
  String get achievementsTotalFloors => 'Total floors';

  @override
  String get achievementsFilterAll => 'All';

  @override
  String get achievementsCategoryDailySteps => 'Daily steps';

  @override
  String get achievementsCategoryLifetimeDistance => 'Lifetime distance';

  @override
  String get achievementsCategoryDailyFloors => 'Daily floors';

  @override
  String get achievementsCategoryLifetimeFloors => 'Lifetime floors';

  @override
  String achievementsDailyStepsRequirement(String arg0) {
    return '$arg0 steps in one day';
  }

  @override
  String achievementsLifetimeDistanceRequirement(String arg0) {
    return '$arg0 total distance';
  }

  @override
  String achievementsDailyFloorsRequirement(String arg0) {
    return '$arg0 floors in one day';
  }

  @override
  String achievementsLifetimeFloorsRequirement(String arg0) {
    return '$arg0 total floors';
  }

  @override
  String achievementsProgressValue(String arg0, String arg1) {
    return '$arg0 of $arg1';
  }

  @override
  String achievementsAchievedOn(String arg0) {
    return 'Unlocked $arg0';
  }

  @override
  String get achievementsEarnedOnce => 'Earned';

  @override
  String achievementsEarnedTimes(int arg0) {
    return '$arg0 times';
  }

  @override
  String get achievementsLocked => 'Locked';

  @override
  String get achievementsNoDataTitle => 'No activity history';

  @override
  String get achievementsNoDataBody =>
      'No step or distance records were returned from Health Connect. Check that activity data exists and that history access is granted for older records.';

  @override
  String get achievementsNoFloorDataTitle => 'No floor data';

  @override
  String get achievementsNoFloorDataBody =>
      'Floor badges unlock when Health Connect has floors climbed data.';

  @override
  String get achievementsErrorTitle => 'Achievements unavailable';

  @override
  String get dataConfidenceTitle => 'Data confidence';

  @override
  String get dataConfidenceHigh => 'High confidence';

  @override
  String get dataConfidenceMedium => 'Medium confidence';

  @override
  String get dataConfidenceLow => 'Low confidence';

  @override
  String dataConfidenceCoverage(int arg0, int arg1, int arg2) {
    return '$arg0 of $arg1 days tracked ($arg2%)';
  }

  @override
  String dataConfidenceSamples(int arg0) {
    return '$arg0 records';
  }

  @override
  String get dataConfidenceSourceUnavailable =>
      'Source details not available for this aggregate';

  @override
  String dataConfidenceSourceSingle(String arg0) {
    return 'Source: $arg0';
  }

  @override
  String dataConfidenceSourceMixed(String arg0) {
    return 'Mixed sources: $arg0';
  }

  @override
  String get dataConfidenceKindMeasured => 'Measured Health Connect records';

  @override
  String get dataConfidenceKindAggregated =>
      'Aggregated from Health Connect records';

  @override
  String get dataConfidenceKindCalculated => 'Calculated by OpenVitals';

  @override
  String get dataConfidenceKindEstimated => 'Estimated or derived value';

  @override
  String get dataConfidenceKindMixed => 'Mixed measured and calculated data';

  @override
  String get dataConfidenceWarningLowCoverage =>
      'Missing days can weaken averages and trends.';

  @override
  String get dataConfidenceWarningSparse =>
      'Sparse data: trends and statistics may be unstable.';

  @override
  String get dataConfidenceWarningMixedSources =>
      'Source changes may explain jumps or duplicated-looking data.';

  @override
  String get dataConfidenceWarningManual =>
      'Manual entries are included in this period.';

  @override
  String get dataConfidenceWarningCalculated =>
      'This value is derived, not directly measured.';

  @override
  String get dataConfidenceWarningNoSources =>
      'This aggregate does not expose source-level details.';

  @override
  String get settingsBodyEnergyGroupTitle => 'Body Energy';

  @override
  String get settingsBodyEnergyGroupBody =>
      'Calibration for estimated intraday energy and effort zones.';

  @override
  String get bodyEnergyCalibrationTitle => 'Turn on Body Energy';

  @override
  String get bodyEnergyCalibrationBody =>
      'OpenVitals estimates drain from heart-rate intensity over time, using your age, weight, and heart rate from the Body profile in Settings.';

  @override
  String get bodyEnergyCalibrationOptionalBody =>
      'Manual heart-rate zones below are optional. If you skip them, OpenVitals uses automatic estimates from Health Connect data and shows lower confidence when calibration is uncertain.';

  @override
  String get bodyEnergyCalibrationBirthYear => 'Birth year';

  @override
  String get bodyEnergyCalibrationMaxHr => 'Max heart rate';

  @override
  String get bodyEnergyCalibrationRestingHr => 'Resting heart rate';

  @override
  String get bodyEnergyCalibrationManualZones => 'Manual heart zones';

  @override
  String get bodyEnergyCalibrationManualZonesBody =>
      'Optional bpm lower bounds for zones 1-5.';

  @override
  String get bodyEnergyCalibrationZone1 => 'Zone 1 lower bpm';

  @override
  String get bodyEnergyCalibrationZone2 => 'Zone 2 lower bpm';

  @override
  String get bodyEnergyCalibrationZone3 => 'Zone 3 lower bpm';

  @override
  String get bodyEnergyCalibrationZone4 => 'Zone 4 lower bpm';

  @override
  String get bodyEnergyCalibrationZone5 => 'Zone 5 lower bpm';

  @override
  String get bodyEnergyCalibrationUseAuto => 'Use automatic estimates';

  @override
  String get bodyEnergyCalibrationSkip => 'Skip for now';

  @override
  String get bodyEnergyCalibrationSaved => 'Body Energy calibration saved';

  @override
  String get bodyEnergyCalibrationReset =>
      'Body Energy calibration reset to automatic';

  @override
  String get bodyEnergyNotSetUp => 'Not set up';

  @override
  String get bodyEnergyTimelineEstimated => 'Estimated by OpenVitals';

  @override
  String get bodyEnergyTimelineCurrent => 'Current';

  @override
  String get bodyEnergyTimelineStart => 'Start';

  @override
  String get bodyEnergyTimelineCharged => 'Charged';

  @override
  String get bodyEnergyTimelineDrained => 'Drained';

  @override
  String get bodyEnergyTimelineConfidence => 'Confidence';

  @override
  String get bodyEnergyTimelineNoData =>
      'No usable Body Energy timeline for this period.';

  @override
  String get bodyEnergyTimelineDayTitle => 'Daily timeline';

  @override
  String get bodyEnergyTimelineLowConfidence =>
      'Some buckets are estimated because calibration or Health Connect data is incomplete.';

  @override
  String get bodyEnergyWhyTitle => 'What moved it';

  @override
  String get bodyEnergyWhyEmpty =>
      'No clear charge or drain dominated this day yet.';

  @override
  String get bodyEnergyInfluenceSleepRecovery => 'Sleep recovery';

  @override
  String get bodyEnergyInfluenceQuietRest => 'Quiet rest';

  @override
  String get bodyEnergyInfluenceExertion => 'Exertion';

  @override
  String get bodyEnergyInfluenceElevatedHr => 'Elevated heart rate';

  @override
  String get bodyEnergyInfluenceRecoveryDebt => 'Recovery debt';

  @override
  String get bodyEnergyInfluenceNoData => 'No data';

  @override
  String get bodyEnergyInfluenceSteady => 'Steady';

  @override
  String get bodyEnergyReasonSleepRecoveryDetail =>
      'Sleep buckets charged the estimate from the previous score.';

  @override
  String get bodyEnergyReasonQuietRestDetail =>
      'Low heart rate while awake added a small recovery charge.';

  @override
  String get bodyEnergyReasonExertionDetail =>
      'Heart-rate intensity or recorded workouts drained the estimate.';

  @override
  String get bodyEnergyReasonElevatedHrDetail =>
      'Awake heart rate above resting level added stress drain.';

  @override
  String get bodyEnergyReasonRecoveryDebtDetail =>
      'Recent harder effort kept a small drain active afterward.';

  @override
  String get bodyEnergyReasonNoDataDetail =>
      'Health Connect did not provide enough signal for this bucket.';

  @override
  String get bodyEnergyReasonSteadyDetail =>
      'The estimate stayed mostly stable.';

  @override
  String get bodyEnergyInputsTitle => 'Inputs used';

  @override
  String bodyEnergyInputsSummary(int arg0, int arg1) {
    return 'Algorithm v$arg0, $arg1-minute buckets';
  }

  @override
  String get bodyEnergyInputHeartRate => 'Heart rate samples';

  @override
  String get bodyEnergyInputSleep => 'Sleep sessions';

  @override
  String get bodyEnergyInputWorkouts => 'Workouts';

  @override
  String get bodyEnergyInputRestingHr => 'Resting heart rate';

  @override
  String get bodyEnergyInputHrBaseline => 'Heart-rate baseline';

  @override
  String get bodyEnergyInputHrv => 'HRV modifier';

  @override
  String get bodyEnergyInputRespiratory => 'Respiration modifier';

  @override
  String get bodyEnergyInputPreviousScore => 'Previous score';

  @override
  String get bodyEnergyInputCalibration => 'Calibration';

  @override
  String get bodyEnergyInputAvailable => 'Available';

  @override
  String get bodyEnergyInputMissing => 'Missing';

  @override
  String get bodyEnergyInputOptional => 'Not present';

  @override
  String bodyEnergyInputRecords(int arg0) {
    return '$arg0 records';
  }

  @override
  String bodyEnergyInputSessions(int arg0) {
    return '$arg0 sessions';
  }

  @override
  String bodyEnergyInputWorkoutsValue(int arg0) {
    return '$arg0 workouts';
  }

  @override
  String bodyEnergyInputPreviousScoreValue(String arg0) {
    return '$arg0 start';
  }

  @override
  String get bodyEnergyCalibrationModeAuto => 'Automatic estimates';

  @override
  String get bodyEnergyCalibrationModeManualValues => 'Manual values';

  @override
  String get bodyEnergyCalibrationModeManualZones => 'Manual zones';

  @override
  String get bodyEnergyCalculationTitle => 'How Body Energy is estimated';

  @override
  String get bodyEnergyCalculationBody =>
      'OpenVitals divides the selected day into short buckets, starts from the previous available score when possible, then adds charge from sleep or quiet rest and subtracts drain from exertion, elevated awake heart rate, and recovery debt after harder effort.';

  @override
  String get bodyEnergyCalculationInputsBody =>
      'Heart rate, resting heart rate, personal zones, sleep, workouts, HRV, and respiratory rate can all improve the estimate. Missing inputs make the estimate more conservative and lower confidence.';

  @override
  String get bodyEnergyCalculationLimitsBody =>
      'This is an on-device wellness estimate, not a direct measurement or medical advice. The displayed inputs and reasons are exposed so the method can be reviewed and improved.';

  @override
  String get metricBodyEnergy => 'Body Energy';

  @override
  String get privacyPolicyTitle => 'Privacy policy';

  @override
  String get privacyPolicyBody1 =>
      'OpenVitals reads data from Health Connect to show steps, workouts, sleep, heart rate, weight, calories, hydration, nutrition, mindfulness, and vitals on your device. Entries you explicitly log, including imported GPX/KML/KMZ routes and imported FIT files, are written to Health Connect.';

  @override
  String get privacyPolicyBody2 =>
      'This app does not upload your health data to a cloud service, does not include ads, and does not share data with third parties.';

  @override
  String get privacyPolicyBody3 =>
      'OpenVitals is not a medical device and does not diagnose, treat, cure, or prevent any disease or medical condition. It is not a substitute for medical advice, diagnosis, or treatment from a qualified healthcare professional.';

  @override
  String get linkCouldNotOpen => 'The link could not be opened.';
}
