import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_de.dart';
import 'app_localizations_en.dart';
import 'app_localizations_es.dart';
import 'app_localizations_et.dart';
import 'app_localizations_it.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('de'),
    Locale('en'),
    Locale('es'),
    Locale('et'),
    Locale('it'),
  ];

  /// No description provided for @appName.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals'**
  String get appName;

  /// No description provided for @cdBack.
  ///
  /// In en, this message translates to:
  /// **'Back'**
  String get cdBack;

  /// No description provided for @cdSettings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get cdSettings;

  /// No description provided for @cdAchievements.
  ///
  /// In en, this message translates to:
  /// **'Achievements'**
  String get cdAchievements;

  /// No description provided for @cdDailyReadiness.
  ///
  /// In en, this message translates to:
  /// **'Daily Readiness'**
  String get cdDailyReadiness;

  /// No description provided for @cdSensorBatteryStatus.
  ///
  /// In en, this message translates to:
  /// **'Sensor battery status'**
  String get cdSensorBatteryStatus;

  /// No description provided for @cdEditDashboard.
  ///
  /// In en, this message translates to:
  /// **'Edit summary'**
  String get cdEditDashboard;

  /// No description provided for @cdFinishDashboardEditing.
  ///
  /// In en, this message translates to:
  /// **'Finish editing summary'**
  String get cdFinishDashboardEditing;

  /// No description provided for @cdEditSavedDrinks.
  ///
  /// In en, this message translates to:
  /// **'Edit saved drinks'**
  String get cdEditSavedDrinks;

  /// No description provided for @cdDoneEditingSavedDrinks.
  ///
  /// In en, this message translates to:
  /// **'Done editing saved drinks'**
  String get cdDoneEditingSavedDrinks;

  /// No description provided for @cdEditDrink.
  ///
  /// In en, this message translates to:
  /// **'Edit drink'**
  String get cdEditDrink;

  /// No description provided for @cdDeleteDrink.
  ///
  /// In en, this message translates to:
  /// **'Delete drink'**
  String get cdDeleteDrink;

  /// No description provided for @cdMoveDrinkCategory.
  ///
  /// In en, this message translates to:
  /// **'Move drink category'**
  String get cdMoveDrinkCategory;

  /// No description provided for @cdExpandDrinkCategory.
  ///
  /// In en, this message translates to:
  /// **'Expand {arg0}'**
  String cdExpandDrinkCategory(String arg0);

  /// No description provided for @cdCollapseDrinkCategory.
  ///
  /// In en, this message translates to:
  /// **'Collapse {arg0}'**
  String cdCollapseDrinkCategory(String arg0);

  /// No description provided for @cdEditManualEntryWidgets.
  ///
  /// In en, this message translates to:
  /// **'Edit add entry widgets'**
  String get cdEditManualEntryWidgets;

  /// No description provided for @cdFinishManualEntryEditing.
  ///
  /// In en, this message translates to:
  /// **'Finish editing add entry widgets'**
  String get cdFinishManualEntryEditing;

  /// No description provided for @cdEditRecordingDashboard.
  ///
  /// In en, this message translates to:
  /// **'Edit recording dashboard'**
  String get cdEditRecordingDashboard;

  /// No description provided for @cdFinishRecordingDashboardEditing.
  ///
  /// In en, this message translates to:
  /// **'Finish editing recording dashboard'**
  String get cdFinishRecordingDashboardEditing;

  /// No description provided for @cdMoveWidgetUp.
  ///
  /// In en, this message translates to:
  /// **'Move widget up'**
  String get cdMoveWidgetUp;

  /// No description provided for @cdMoveWidgetDown.
  ///
  /// In en, this message translates to:
  /// **'Move widget down'**
  String get cdMoveWidgetDown;

  /// No description provided for @cdEditMetricSections.
  ///
  /// In en, this message translates to:
  /// **'Edit metric sections'**
  String get cdEditMetricSections;

  /// No description provided for @cdFinishMetricSectionEditing.
  ///
  /// In en, this message translates to:
  /// **'Finish editing metric sections'**
  String get cdFinishMetricSectionEditing;

  /// No description provided for @cdMoveSectionUp.
  ///
  /// In en, this message translates to:
  /// **'Move section up'**
  String get cdMoveSectionUp;

  /// No description provided for @cdMoveSectionDown.
  ///
  /// In en, this message translates to:
  /// **'Move section down'**
  String get cdMoveSectionDown;

  /// No description provided for @cdRemoveWidget.
  ///
  /// In en, this message translates to:
  /// **'Remove widget'**
  String get cdRemoveWidget;

  /// No description provided for @cdDecreaseRecordingDashboardWidgetSize.
  ///
  /// In en, this message translates to:
  /// **'Make widget smaller'**
  String get cdDecreaseRecordingDashboardWidgetSize;

  /// No description provided for @cdIncreaseRecordingDashboardWidgetSize.
  ///
  /// In en, this message translates to:
  /// **'Make widget larger'**
  String get cdIncreaseRecordingDashboardWidgetSize;

  /// No description provided for @cdExitRecordingFocusMode.
  ///
  /// In en, this message translates to:
  /// **'Exit focus mode'**
  String get cdExitRecordingFocusMode;

  /// No description provided for @cdToggleRecordingOutdoorMode.
  ///
  /// In en, this message translates to:
  /// **'Toggle outdoor readability mode'**
  String get cdToggleRecordingOutdoorMode;

  /// No description provided for @cdRecenterMap.
  ///
  /// In en, this message translates to:
  /// **'Recenter map'**
  String get cdRecenterMap;

  /// No description provided for @cdDeleteEntry.
  ///
  /// In en, this message translates to:
  /// **'Delete entry'**
  String get cdDeleteEntry;

  /// No description provided for @cdEditEntry.
  ///
  /// In en, this message translates to:
  /// **'Edit entry'**
  String get cdEditEntry;

  /// No description provided for @cdPreviousDay.
  ///
  /// In en, this message translates to:
  /// **'Previous day'**
  String get cdPreviousDay;

  /// No description provided for @cdNextDay.
  ///
  /// In en, this message translates to:
  /// **'Next day'**
  String get cdNextDay;

  /// No description provided for @cdPreviousPeriod.
  ///
  /// In en, this message translates to:
  /// **'Previous period'**
  String get cdPreviousPeriod;

  /// No description provided for @cdNextPeriod.
  ///
  /// In en, this message translates to:
  /// **'Next period'**
  String get cdNextPeriod;

  /// No description provided for @cdOpenCalendar.
  ///
  /// In en, this message translates to:
  /// **'Open calendar'**
  String get cdOpenCalendar;

  /// No description provided for @actionCancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get actionCancel;

  /// No description provided for @actionAdd.
  ///
  /// In en, this message translates to:
  /// **'Add'**
  String get actionAdd;

  /// No description provided for @actionAddCustom.
  ///
  /// In en, this message translates to:
  /// **'Add custom'**
  String get actionAddCustom;

  /// No description provided for @actionSave.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get actionSave;

  /// No description provided for @actionClose.
  ///
  /// In en, this message translates to:
  /// **'Close'**
  String get actionClose;

  /// No description provided for @actionContinue.
  ///
  /// In en, this message translates to:
  /// **'Continue'**
  String get actionContinue;

  /// No description provided for @actionDelete.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get actionDelete;

  /// No description provided for @actionDetails.
  ///
  /// In en, this message translates to:
  /// **'Details'**
  String get actionDetails;

  /// No description provided for @actionEdit.
  ///
  /// In en, this message translates to:
  /// **'Edit'**
  String get actionEdit;

  /// No description provided for @actionEnable.
  ///
  /// In en, this message translates to:
  /// **'Enable'**
  String get actionEnable;

  /// No description provided for @actionGetStarted.
  ///
  /// In en, this message translates to:
  /// **'Get started'**
  String get actionGetStarted;

  /// No description provided for @actionGrant.
  ///
  /// In en, this message translates to:
  /// **'Grant'**
  String get actionGrant;

  /// No description provided for @actionGrantPermission.
  ///
  /// In en, this message translates to:
  /// **'Grant permission'**
  String get actionGrantPermission;

  /// No description provided for @actionLoadMoreEntries.
  ///
  /// In en, this message translates to:
  /// **'Load 10 more'**
  String get actionLoadMoreEntries;

  /// No description provided for @actionShowCalculation.
  ///
  /// In en, this message translates to:
  /// **'Show calculation'**
  String get actionShowCalculation;

  /// No description provided for @actionHideCalculation.
  ///
  /// In en, this message translates to:
  /// **'Hide calculation'**
  String get actionHideCalculation;

  /// No description provided for @actionNotNow.
  ///
  /// In en, this message translates to:
  /// **'Not now'**
  String get actionNotNow;

  /// No description provided for @actionAccept.
  ///
  /// In en, this message translates to:
  /// **'I accept'**
  String get actionAccept;

  /// No description provided for @actionOpen.
  ///
  /// In en, this message translates to:
  /// **'Open'**
  String get actionOpen;

  /// No description provided for @actionPause.
  ///
  /// In en, this message translates to:
  /// **'Pause'**
  String get actionPause;

  /// No description provided for @actionReview.
  ///
  /// In en, this message translates to:
  /// **'Review'**
  String get actionReview;

  /// No description provided for @actionResume.
  ///
  /// In en, this message translates to:
  /// **'Resume'**
  String get actionResume;

  /// No description provided for @actionRefresh.
  ///
  /// In en, this message translates to:
  /// **'Refresh'**
  String get actionRefresh;

  /// No description provided for @actionSelect.
  ///
  /// In en, this message translates to:
  /// **'Select'**
  String get actionSelect;

  /// No description provided for @actionStart.
  ///
  /// In en, this message translates to:
  /// **'Start'**
  String get actionStart;

  /// No description provided for @actionFinish.
  ///
  /// In en, this message translates to:
  /// **'Finish'**
  String get actionFinish;

  /// No description provided for @actionDiscard.
  ///
  /// In en, this message translates to:
  /// **'Discard'**
  String get actionDiscard;

  /// No description provided for @unknownError.
  ///
  /// In en, this message translates to:
  /// **'Unknown error'**
  String get unknownError;

  /// No description provided for @screenErrorNotFound.
  ///
  /// In en, this message translates to:
  /// **'The requested item was not found.'**
  String get screenErrorNotFound;

  /// No description provided for @screenErrorMissingArgument.
  ///
  /// In en, this message translates to:
  /// **'Required information is missing.'**
  String get screenErrorMissingArgument;

  /// No description provided for @screenErrorPermissionDenied.
  ///
  /// In en, this message translates to:
  /// **'Permission is required to load this data.'**
  String get screenErrorPermissionDenied;

  /// No description provided for @screenErrorHealthConnectUnavailable.
  ///
  /// In en, this message translates to:
  /// **'Health Connect is not available on this device.'**
  String get screenErrorHealthConnectUnavailable;

  /// No description provided for @screenErrorLoadSleepSession.
  ///
  /// In en, this message translates to:
  /// **'Unable to load sleep session.'**
  String get screenErrorLoadSleepSession;

  /// No description provided for @screenErrorLoadSleepPeriod.
  ///
  /// In en, this message translates to:
  /// **'Unable to load sleep data.'**
  String get screenErrorLoadSleepPeriod;

  /// No description provided for @notAvailable.
  ///
  /// In en, this message translates to:
  /// **'Not available'**
  String get notAvailable;

  /// No description provided for @notRecorded.
  ///
  /// In en, this message translates to:
  /// **'Not recorded'**
  String get notRecorded;

  /// No description provided for @noData.
  ///
  /// In en, this message translates to:
  /// **'No data'**
  String get noData;

  /// No description provided for @loading.
  ///
  /// In en, this message translates to:
  /// **'Loading...'**
  String get loading;

  /// No description provided for @homeMetricWidgetDescription.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals metric'**
  String get homeMetricWidgetDescription;

  /// No description provided for @homeMetricWidgetConfigTitle.
  ///
  /// In en, this message translates to:
  /// **'Choose metric'**
  String get homeMetricWidgetConfigTitle;

  /// No description provided for @homeMetricWidgetConfigPrompt.
  ///
  /// In en, this message translates to:
  /// **'Choose the metric for the widget:'**
  String get homeMetricWidgetConfigPrompt;

  /// No description provided for @homeMetricWidgetNoMetrics.
  ///
  /// In en, this message translates to:
  /// **'No metrics available.'**
  String get homeMetricWidgetNoMetrics;

  /// No description provided for @homeMetricWidgetPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'Grant permission in OpenVitals'**
  String get homeMetricWidgetPermissionNeeded;

  /// No description provided for @homeMetricWidgetUpdateFailed.
  ///
  /// In en, this message translates to:
  /// **'Unable to update'**
  String get homeMetricWidgetUpdateFailed;

  /// No description provided for @homeMetricWidgetOpenForDetails.
  ///
  /// In en, this message translates to:
  /// **'Open for details'**
  String get homeMetricWidgetOpenForDetails;

  /// No description provided for @homeMetricWidgetNotConfigured.
  ///
  /// In en, this message translates to:
  /// **'Select a metric'**
  String get homeMetricWidgetNotConfigured;

  /// No description provided for @homeQuickBeverageWidgetDescription.
  ///
  /// In en, this message translates to:
  /// **'Quick beverage'**
  String get homeQuickBeverageWidgetDescription;

  /// No description provided for @homeQuickBeverageOneTapWidgetDescription.
  ///
  /// In en, this message translates to:
  /// **'Quick beverage 1x1'**
  String get homeQuickBeverageOneTapWidgetDescription;

  /// No description provided for @homeQuickBeverageWidgetConfigTitle.
  ///
  /// In en, this message translates to:
  /// **'Choose beverage'**
  String get homeQuickBeverageWidgetConfigTitle;

  /// No description provided for @homeQuickBeverageWidgetConfigPrompt.
  ///
  /// In en, this message translates to:
  /// **'Choose the beverage for the widget:'**
  String get homeQuickBeverageWidgetConfigPrompt;

  /// No description provided for @homeQuickBeverageWidgetNoDrinks.
  ///
  /// In en, this message translates to:
  /// **'No beverages available.'**
  String get homeQuickBeverageWidgetNoDrinks;

  /// No description provided for @homeQuickBeverageWidgetNotConfigured.
  ///
  /// In en, this message translates to:
  /// **'Select a beverage'**
  String get homeQuickBeverageWidgetNotConfigured;

  /// No description provided for @homeQuickBeverageWidgetTapToLog.
  ///
  /// In en, this message translates to:
  /// **'Tap to log'**
  String get homeQuickBeverageWidgetTapToLog;

  /// No description provided for @homeQuickBeverageWidgetSaved.
  ///
  /// In en, this message translates to:
  /// **'Saved now'**
  String get homeQuickBeverageWidgetSaved;

  /// No description provided for @homeQuickBeverageWidgetSavedNutrition.
  ///
  /// In en, this message translates to:
  /// **'Saved as nutrition'**
  String get homeQuickBeverageWidgetSavedNutrition;

  /// No description provided for @homeDailyReadinessWidgetDescription.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals daily readiness'**
  String get homeDailyReadinessWidgetDescription;

  /// No description provided for @homeBodyEnergyWidgetDescription.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals body energy'**
  String get homeBodyEnergyWidgetDescription;

  /// No description provided for @homeTodayVitalsWidgetDescription.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals today vitals'**
  String get homeTodayVitalsWidgetDescription;

  /// No description provided for @homeWidgetTodayTitle.
  ///
  /// In en, this message translates to:
  /// **'Today'**
  String get homeWidgetTodayTitle;

  /// No description provided for @homeWidgetContext.
  ///
  /// In en, this message translates to:
  /// **'Context'**
  String get homeWidgetContext;

  /// No description provided for @homeWidgetHrvShort.
  ///
  /// In en, this message translates to:
  /// **'HRV'**
  String get homeWidgetHrvShort;

  /// No description provided for @homeWidgetBodyEnergyCharged.
  ///
  /// In en, this message translates to:
  /// **'Charged'**
  String get homeWidgetBodyEnergyCharged;

  /// No description provided for @homeWidgetBodyEnergySteady.
  ///
  /// In en, this message translates to:
  /// **'Steady'**
  String get homeWidgetBodyEnergySteady;

  /// No description provided for @homeWidgetBodyEnergyLimited.
  ///
  /// In en, this message translates to:
  /// **'Limited'**
  String get homeWidgetBodyEnergyLimited;

  /// No description provided for @homeWidgetBodyEnergyLow.
  ///
  /// In en, this message translates to:
  /// **'Low'**
  String get homeWidgetBodyEnergyLow;

  /// No description provided for @screenSteps.
  ///
  /// In en, this message translates to:
  /// **'Steps'**
  String get screenSteps;

  /// No description provided for @screenActivities.
  ///
  /// In en, this message translates to:
  /// **'Activities'**
  String get screenActivities;

  /// No description provided for @screenCalories.
  ///
  /// In en, this message translates to:
  /// **'Calories'**
  String get screenCalories;

  /// No description provided for @screenActivityDetail.
  ///
  /// In en, this message translates to:
  /// **'Activity detail'**
  String get screenActivityDetail;

  /// No description provided for @screenSleep.
  ///
  /// In en, this message translates to:
  /// **'Sleep'**
  String get screenSleep;

  /// No description provided for @screenSleepDetail.
  ///
  /// In en, this message translates to:
  /// **'Sleep detail'**
  String get screenSleepDetail;

  /// No description provided for @screenHeartVitals.
  ///
  /// In en, this message translates to:
  /// **'Heart & Vitals'**
  String get screenHeartVitals;

  /// No description provided for @screenStressTracking.
  ///
  /// In en, this message translates to:
  /// **'Stress Tracking'**
  String get screenStressTracking;

  /// No description provided for @screenBodyEnergy.
  ///
  /// In en, this message translates to:
  /// **'Body Energy'**
  String get screenBodyEnergy;

  /// No description provided for @screenTrainingReadiness.
  ///
  /// In en, this message translates to:
  /// **'Training Readiness'**
  String get screenTrainingReadiness;

  /// No description provided for @screenBody.
  ///
  /// In en, this message translates to:
  /// **'Body'**
  String get screenBody;

  /// No description provided for @screenHydration.
  ///
  /// In en, this message translates to:
  /// **'Beverages'**
  String get screenHydration;

  /// No description provided for @screenNutrition.
  ///
  /// In en, this message translates to:
  /// **'Nutrition'**
  String get screenNutrition;

  /// No description provided for @screenMindfulness.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness'**
  String get screenMindfulness;

  /// No description provided for @screenCycle.
  ///
  /// In en, this message translates to:
  /// **'Cycle'**
  String get screenCycle;

  /// No description provided for @screenDailyReadiness.
  ///
  /// In en, this message translates to:
  /// **'Daily Readiness'**
  String get screenDailyReadiness;

  /// No description provided for @screenSettings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get screenSettings;

  /// No description provided for @screenAchievements.
  ///
  /// In en, this message translates to:
  /// **'Achievements'**
  String get screenAchievements;

  /// No description provided for @screenManualEntry.
  ///
  /// In en, this message translates to:
  /// **'Add entry'**
  String get screenManualEntry;

  /// No description provided for @screenHydrationEntry.
  ///
  /// In en, this message translates to:
  /// **'Beverage entry'**
  String get screenHydrationEntry;

  /// No description provided for @screenActivityEntry.
  ///
  /// In en, this message translates to:
  /// **'Activity entry'**
  String get screenActivityEntry;

  /// No description provided for @screenMindfulnessEntry.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness entry'**
  String get screenMindfulnessEntry;

  /// No description provided for @screenCarbsEntry.
  ///
  /// In en, this message translates to:
  /// **'Carbs entry'**
  String get screenCarbsEntry;

  /// No description provided for @screenBodyMeasurementEntry.
  ///
  /// In en, this message translates to:
  /// **'Body measurement entry'**
  String get screenBodyMeasurementEntry;

  /// No description provided for @screenVitalsMeasurementEntry.
  ///
  /// In en, this message translates to:
  /// **'Vitals entry'**
  String get screenVitalsMeasurementEntry;

  /// No description provided for @bottomNavDashboard.
  ///
  /// In en, this message translates to:
  /// **'Summary'**
  String get bottomNavDashboard;

  /// No description provided for @manualEntryHydrationTitle.
  ///
  /// In en, this message translates to:
  /// **'Beverages'**
  String get manualEntryHydrationTitle;

  /// No description provided for @manualEntryActivityTitle.
  ///
  /// In en, this message translates to:
  /// **'Activity'**
  String get manualEntryActivityTitle;

  /// No description provided for @manualEntryDateLabel.
  ///
  /// In en, this message translates to:
  /// **'Entry date'**
  String get manualEntryDateLabel;

  /// No description provided for @manualEntryTimeLabel.
  ///
  /// In en, this message translates to:
  /// **'Entry time'**
  String get manualEntryTimeLabel;

  /// No description provided for @manualEntrySelectTime.
  ///
  /// In en, this message translates to:
  /// **'Select entry time'**
  String get manualEntrySelectTime;

  /// No description provided for @manualEntryAddWidgets.
  ///
  /// In en, this message translates to:
  /// **'Add entry widgets'**
  String get manualEntryAddWidgets;

  /// No description provided for @manualEntryAllWidgetsAdded.
  ///
  /// In en, this message translates to:
  /// **'All entry widgets are already shown.'**
  String get manualEntryAllWidgetsAdded;

  /// No description provided for @manualEntryWritePermissionTitle.
  ///
  /// In en, this message translates to:
  /// **'Beverage write permission'**
  String get manualEntryWritePermissionTitle;

  /// No description provided for @manualEntryActivityWritePermissionTitle.
  ///
  /// In en, this message translates to:
  /// **'Activity write permissions'**
  String get manualEntryActivityWritePermissionTitle;

  /// No description provided for @manualEntryMindfulnessWritePermissionTitle.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness write permission'**
  String get manualEntryMindfulnessWritePermissionTitle;

  /// No description provided for @manualEntryCarbsWritePermissionTitle.
  ///
  /// In en, this message translates to:
  /// **'Carbs write permission'**
  String get manualEntryCarbsWritePermissionTitle;

  /// No description provided for @manualEntryBodyWritePermissionTitle.
  ///
  /// In en, this message translates to:
  /// **'{arg0} write permission'**
  String manualEntryBodyWritePermissionTitle(String arg0);

  /// No description provided for @manualEntryVitalsWritePermissionTitle.
  ///
  /// In en, this message translates to:
  /// **'{arg0} write permission'**
  String manualEntryVitalsWritePermissionTitle(String arg0);

  /// No description provided for @mindfulnessEntrySubtitle.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness sessions are saved directly to Health Connect.'**
  String get mindfulnessEntrySubtitle;

  /// No description provided for @mindfulnessEntryPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'For the summary, OpenVitals only asks for view permissions. To add mindfulness entries, we need write permission. OpenVitals will not store these sessions; entries are saved in Health Connect.'**
  String get mindfulnessEntryPermissionNeeded;

  /// No description provided for @activityEntrySubtitle.
  ///
  /// In en, this message translates to:
  /// **'Create a Health Connect activity session. Imported route or activity details are written only when you save.'**
  String get activityEntrySubtitle;

  /// No description provided for @activityEntryPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'For the summary, OpenVitals only asks for view permissions. To add activities, we need Health Connect write permissions for sessions, routes, distance, elevation, calories, and recorded sensor metrics such as heart rate; treadmill entries ask for steps when needed. OpenVitals will not store these entries; they are saved in Health Connect.'**
  String get activityEntryPermissionNeeded;

  /// No description provided for @activityEntrySourceBody.
  ///
  /// In en, this message translates to:
  /// **'Create an activity manually, use an existing plan, or record a GPS route.'**
  String get activityEntrySourceBody;

  /// No description provided for @activityEntryCreateManual.
  ///
  /// In en, this message translates to:
  /// **'Create manually'**
  String get activityEntryCreateManual;

  /// No description provided for @activityEntryCreateFromExistingPlan.
  ///
  /// In en, this message translates to:
  /// **'Create from existing plan'**
  String get activityEntryCreateFromExistingPlan;

  /// No description provided for @activityEntryRecordGps.
  ///
  /// In en, this message translates to:
  /// **'Record activity'**
  String get activityEntryRecordGps;

  /// No description provided for @activityEntryChooseAnotherSource.
  ///
  /// In en, this message translates to:
  /// **'Choose another method'**
  String get activityEntryChooseAnotherSource;

  /// No description provided for @activityEntryTypeLabel.
  ///
  /// In en, this message translates to:
  /// **'Activity type'**
  String get activityEntryTypeLabel;

  /// No description provided for @activityEntryTitleLabel.
  ///
  /// In en, this message translates to:
  /// **'Title'**
  String get activityEntryTitleLabel;

  /// No description provided for @activityEntryStartDateLabel.
  ///
  /// In en, this message translates to:
  /// **'Start date'**
  String get activityEntryStartDateLabel;

  /// No description provided for @activityEntryStartTimeLabel.
  ///
  /// In en, this message translates to:
  /// **'Start time'**
  String get activityEntryStartTimeLabel;

  /// No description provided for @activityEntrySelectTime.
  ///
  /// In en, this message translates to:
  /// **'Select start time'**
  String get activityEntrySelectTime;

  /// No description provided for @activityEntryDurationLabel.
  ///
  /// In en, this message translates to:
  /// **'Duration min'**
  String get activityEntryDurationLabel;

  /// No description provided for @activityEntryRepetitionsTitle.
  ///
  /// In en, this message translates to:
  /// **'Repetitions'**
  String get activityEntryRepetitionsTitle;

  /// No description provided for @activityEntryStepsTitle.
  ///
  /// In en, this message translates to:
  /// **'Steps'**
  String get activityEntryStepsTitle;

  /// No description provided for @activityEntryRepetitionModeTotal.
  ///
  /// In en, this message translates to:
  /// **'Total'**
  String get activityEntryRepetitionModeTotal;

  /// No description provided for @activityEntryRepetitionModeSets.
  ///
  /// In en, this message translates to:
  /// **'Sets'**
  String get activityEntryRepetitionModeSets;

  /// No description provided for @activityEntryRepetitionsLabel.
  ///
  /// In en, this message translates to:
  /// **'Reps'**
  String get activityEntryRepetitionsLabel;

  /// No description provided for @activityEntryStepsLabel.
  ///
  /// In en, this message translates to:
  /// **'Steps'**
  String get activityEntryStepsLabel;

  /// No description provided for @activityEntrySetRepetitionsLabel.
  ///
  /// In en, this message translates to:
  /// **'Set {arg0} reps'**
  String activityEntrySetRepetitionsLabel(int arg0);

  /// No description provided for @activityEntrySetRestLabel.
  ///
  /// In en, this message translates to:
  /// **'Rest time'**
  String get activityEntrySetRestLabel;

  /// No description provided for @activityEntryAddSet.
  ///
  /// In en, this message translates to:
  /// **'Add set'**
  String get activityEntryAddSet;

  /// No description provided for @activityEntryTrainingPlansTitle.
  ///
  /// In en, this message translates to:
  /// **'Training plans'**
  String get activityEntryTrainingPlansTitle;

  /// No description provided for @activityEntryTrainingPlansLoading.
  ///
  /// In en, this message translates to:
  /// **'Loading Health Connect plans'**
  String get activityEntryTrainingPlansLoading;

  /// No description provided for @activityEntryTrainingPlansEmpty.
  ///
  /// In en, this message translates to:
  /// **'No Health Connect plans for this date and activity type'**
  String get activityEntryTrainingPlansEmpty;

  /// No description provided for @activityEntryTrainingPlanLabel.
  ///
  /// In en, this message translates to:
  /// **'Training plan'**
  String get activityEntryTrainingPlanLabel;

  /// No description provided for @activityEntryTrainingPlanSelect.
  ///
  /// In en, this message translates to:
  /// **'Select a plan'**
  String get activityEntryTrainingPlanSelect;

  /// No description provided for @activityEntryTrainingPlanNew.
  ///
  /// In en, this message translates to:
  /// **'New plan'**
  String get activityEntryTrainingPlanNew;

  /// No description provided for @activityEntryTrainingPlanUnnamed.
  ///
  /// In en, this message translates to:
  /// **'Unnamed plan'**
  String get activityEntryTrainingPlanUnnamed;

  /// No description provided for @activityEntrySaveTrainingPlan.
  ///
  /// In en, this message translates to:
  /// **'Save plan'**
  String get activityEntrySaveTrainingPlan;

  /// No description provided for @activityEntryUpdateTrainingPlan.
  ///
  /// In en, this message translates to:
  /// **'Update plan'**
  String get activityEntryUpdateTrainingPlan;

  /// No description provided for @activityEntryPlanActivityPickerTitle.
  ///
  /// In en, this message translates to:
  /// **'Activities with plans'**
  String get activityEntryPlanActivityPickerTitle;

  /// No description provided for @activityEntryPlanActivityPickerEmpty.
  ///
  /// In en, this message translates to:
  /// **'No Health Connect plans found'**
  String get activityEntryPlanActivityPickerEmpty;

  /// No description provided for @activityEntryPlanPickerTitle.
  ///
  /// In en, this message translates to:
  /// **'Choose a plan'**
  String get activityEntryPlanPickerTitle;

  /// No description provided for @activityEntryPlanPickerEmpty.
  ///
  /// In en, this message translates to:
  /// **'No plans found for this activity'**
  String get activityEntryPlanPickerEmpty;

  /// No description provided for @activityEntryPlanChooseActivity.
  ///
  /// In en, this message translates to:
  /// **'Choose another activity'**
  String get activityEntryPlanChooseActivity;

  /// No description provided for @activityEntryPlanOneSetSummary.
  ///
  /// In en, this message translates to:
  /// **'1 set • {arg0} reps'**
  String activityEntryPlanOneSetSummary(int arg0);

  /// No description provided for @activityEntryPlanSummary.
  ///
  /// In en, this message translates to:
  /// **'{arg0} sets • {arg1} reps'**
  String activityEntryPlanSummary(int arg0, int arg1);

  /// No description provided for @activityEntryPlanPreviewReps.
  ///
  /// In en, this message translates to:
  /// **'{arg0} reps'**
  String activityEntryPlanPreviewReps(int arg0);

  /// No description provided for @activityEntryPlanPreviewRest.
  ///
  /// In en, this message translates to:
  /// **'rest {arg0} sec'**
  String activityEntryPlanPreviewRest(int arg0);

  /// No description provided for @activityEntryPlanPreviewMore.
  ///
  /// In en, this message translates to:
  /// **'+{arg0} more'**
  String activityEntryPlanPreviewMore(int arg0);

  /// No description provided for @activityEntryDistanceLabel.
  ///
  /// In en, this message translates to:
  /// **'Distance {arg0}'**
  String activityEntryDistanceLabel(String arg0);

  /// No description provided for @activityEntryElevationLabel.
  ///
  /// In en, this message translates to:
  /// **'Climb {arg0}'**
  String activityEntryElevationLabel(String arg0);

  /// No description provided for @activityEntryNotesLabel.
  ///
  /// In en, this message translates to:
  /// **'Notes'**
  String get activityEntryNotesLabel;

  /// No description provided for @activityEntryFeelingLabel.
  ///
  /// In en, this message translates to:
  /// **'How did it feel?'**
  String get activityEntryFeelingLabel;

  /// No description provided for @activityEntryFeelingGreat.
  ///
  /// In en, this message translates to:
  /// **'Great'**
  String get activityEntryFeelingGreat;

  /// No description provided for @activityEntryFeelingGood.
  ///
  /// In en, this message translates to:
  /// **'Good'**
  String get activityEntryFeelingGood;

  /// No description provided for @activityEntryFeelingHard.
  ///
  /// In en, this message translates to:
  /// **'Hard'**
  String get activityEntryFeelingHard;

  /// No description provided for @activityEntryFeelingRough.
  ///
  /// In en, this message translates to:
  /// **'Rough'**
  String get activityEntryFeelingRough;

  /// No description provided for @activityEntryImportedRoute.
  ///
  /// In en, this message translates to:
  /// **'Imported route'**
  String get activityEntryImportedRoute;

  /// No description provided for @activityEntryRecordingTitle.
  ///
  /// In en, this message translates to:
  /// **'Recording activity'**
  String get activityEntryRecordingTitle;

  /// No description provided for @activityEntryRecordingReadyBody.
  ///
  /// In en, this message translates to:
  /// **'Choose the activity type, then start when you are ready. After finishing, you can review and add details before saving.'**
  String get activityEntryRecordingReadyBody;

  /// No description provided for @activityEntryRecordingGoToActivityScreen.
  ///
  /// In en, this message translates to:
  /// **'Go to activity screen'**
  String get activityEntryRecordingGoToActivityScreen;

  /// No description provided for @activityEntryRecordingActive.
  ///
  /// In en, this message translates to:
  /// **'Recording'**
  String get activityEntryRecordingActive;

  /// No description provided for @activityEntryRecordingPaused.
  ///
  /// In en, this message translates to:
  /// **'Paused'**
  String get activityEntryRecordingPaused;

  /// No description provided for @activityEntryRecordingIdle.
  ///
  /// In en, this message translates to:
  /// **'Idle'**
  String get activityEntryRecordingIdle;

  /// No description provided for @activityEntryRecordingResting.
  ///
  /// In en, this message translates to:
  /// **'Resting'**
  String get activityEntryRecordingResting;

  /// No description provided for @activityEntryRecordingGpsFix.
  ///
  /// In en, this message translates to:
  /// **'GPS ready'**
  String get activityEntryRecordingGpsFix;

  /// No description provided for @activityEntryRecordingGpsPoor.
  ///
  /// In en, this message translates to:
  /// **'Weak GPS'**
  String get activityEntryRecordingGpsPoor;

  /// No description provided for @activityEntryRecordingGpsLost.
  ///
  /// In en, this message translates to:
  /// **'GPS lost'**
  String get activityEntryRecordingGpsLost;

  /// No description provided for @activityEntryRecordingGpsOff.
  ///
  /// In en, this message translates to:
  /// **'GPS off'**
  String get activityEntryRecordingGpsOff;

  /// No description provided for @activityEntryRecordingTabMap.
  ///
  /// In en, this message translates to:
  /// **'Map'**
  String get activityEntryRecordingTabMap;

  /// No description provided for @activityEntryRecordingTabStats.
  ///
  /// In en, this message translates to:
  /// **'Stats'**
  String get activityEntryRecordingTabStats;

  /// No description provided for @activityEntryRecordingTabIntervals.
  ///
  /// In en, this message translates to:
  /// **'Intervals'**
  String get activityEntryRecordingTabIntervals;

  /// No description provided for @activityEntryRecordingTabByTime.
  ///
  /// In en, this message translates to:
  /// **'By time'**
  String get activityEntryRecordingTabByTime;

  /// No description provided for @activityEntryRecordingTabByDistance.
  ///
  /// In en, this message translates to:
  /// **'By distance'**
  String get activityEntryRecordingTabByDistance;

  /// No description provided for @activityEntryRecordingTimeSplit.
  ///
  /// In en, this message translates to:
  /// **'Time split'**
  String get activityEntryRecordingTimeSplit;

  /// No description provided for @activityEntryRecordingDistanceSplit.
  ///
  /// In en, this message translates to:
  /// **'Distance split'**
  String get activityEntryRecordingDistanceSplit;

  /// No description provided for @activityEntryRecordingSplitMinutes.
  ///
  /// In en, this message translates to:
  /// **'{arg0} min'**
  String activityEntryRecordingSplitMinutes(int arg0);

  /// No description provided for @activityEntryRecordingSplitInterval.
  ///
  /// In en, this message translates to:
  /// **'Interval {arg0}'**
  String activityEntryRecordingSplitInterval(int arg0);

  /// No description provided for @activityEntryRecordingSplitTimeRange.
  ///
  /// In en, this message translates to:
  /// **'{arg0}-{arg1} min'**
  String activityEntryRecordingSplitTimeRange(int arg0, int arg1);

  /// No description provided for @activityEntryRecordingSplitElapsed.
  ///
  /// In en, this message translates to:
  /// **'Elapsed'**
  String get activityEntryRecordingSplitElapsed;

  /// No description provided for @activityEntryRecordingSplitAvg.
  ///
  /// In en, this message translates to:
  /// **'Avg'**
  String get activityEntryRecordingSplitAvg;

  /// No description provided for @activityEntryRecordingSplitMax.
  ///
  /// In en, this message translates to:
  /// **'Max'**
  String get activityEntryRecordingSplitMax;

  /// No description provided for @activityEntryRecordingNoIntervals.
  ///
  /// In en, this message translates to:
  /// **'No intervals yet'**
  String get activityEntryRecordingNoIntervals;

  /// No description provided for @activityEntryRecordingNoTimeSplits.
  ///
  /// In en, this message translates to:
  /// **'No time splits yet'**
  String get activityEntryRecordingNoTimeSplits;

  /// No description provided for @activityEntryRecordingNoDistanceSplits.
  ///
  /// In en, this message translates to:
  /// **'No distance splits yet'**
  String get activityEntryRecordingNoDistanceSplits;

  /// No description provided for @activityEntryRecordingLap.
  ///
  /// In en, this message translates to:
  /// **'Lap'**
  String get activityEntryRecordingLap;

  /// No description provided for @activityEntryRecordingMarker.
  ///
  /// In en, this message translates to:
  /// **'Add Marker'**
  String get activityEntryRecordingMarker;

  /// No description provided for @activityEntryRecordingMarkerDefaultName.
  ///
  /// In en, this message translates to:
  /// **'Marker {arg0}'**
  String activityEntryRecordingMarkerDefaultName(int arg0);

  /// No description provided for @activityEntryRecordingMarkersTitle.
  ///
  /// In en, this message translates to:
  /// **'Markers'**
  String get activityEntryRecordingMarkersTitle;

  /// No description provided for @activityEntryRecordingMarkerName.
  ///
  /// In en, this message translates to:
  /// **'Name'**
  String get activityEntryRecordingMarkerName;

  /// No description provided for @activityEntryRecordingMarkerNote.
  ///
  /// In en, this message translates to:
  /// **'Note'**
  String get activityEntryRecordingMarkerNote;

  /// No description provided for @activityEntryRecordingWaitingForGps.
  ///
  /// In en, this message translates to:
  /// **'Waiting for a precise GPS fix'**
  String get activityEntryRecordingWaitingForGps;

  /// No description provided for @activityEntryRecordingGpsWaiting.
  ///
  /// In en, this message translates to:
  /// **'Waiting for a precise GPS fix before start.'**
  String get activityEntryRecordingGpsWaiting;

  /// No description provided for @activityEntryRecordingGpsWaitingAccuracy.
  ///
  /// In en, this message translates to:
  /// **'Waiting for better GPS accuracy • {arg0}'**
  String activityEntryRecordingGpsWaitingAccuracy(String arg0);

  /// No description provided for @activityEntryRecordingGpsReady.
  ///
  /// In en, this message translates to:
  /// **'GPS ready • accuracy {arg0}'**
  String activityEntryRecordingGpsReady(String arg0);

  /// No description provided for @activityEntryRecordingGpsDisabled.
  ///
  /// In en, this message translates to:
  /// **'Turn on GPS to start recording.'**
  String get activityEntryRecordingGpsDisabled;

  /// No description provided for @activityEntryRecordingDistance.
  ///
  /// In en, this message translates to:
  /// **'Distance'**
  String get activityEntryRecordingDistance;

  /// No description provided for @activityEntryRecordingTotalTime.
  ///
  /// In en, this message translates to:
  /// **'Total time'**
  String get activityEntryRecordingTotalTime;

  /// No description provided for @activityEntryRecordingMovingTime.
  ///
  /// In en, this message translates to:
  /// **'Moving time'**
  String get activityEntryRecordingMovingTime;

  /// No description provided for @activityEntryRecordingRestTime.
  ///
  /// In en, this message translates to:
  /// **'Rest time'**
  String get activityEntryRecordingRestTime;

  /// No description provided for @activityEntryRecordingSpeed.
  ///
  /// In en, this message translates to:
  /// **'Speed'**
  String get activityEntryRecordingSpeed;

  /// No description provided for @activityEntryRecordingMaxSpeed.
  ///
  /// In en, this message translates to:
  /// **'Max speed'**
  String get activityEntryRecordingMaxSpeed;

  /// No description provided for @activityEntryRecordingAverageSpeed.
  ///
  /// In en, this message translates to:
  /// **'Avg speed'**
  String get activityEntryRecordingAverageSpeed;

  /// No description provided for @activityEntryRecordingAverageMovingSpeed.
  ///
  /// In en, this message translates to:
  /// **'Avg moving speed'**
  String get activityEntryRecordingAverageMovingSpeed;

  /// No description provided for @activityEntryRecordingElevationGain.
  ///
  /// In en, this message translates to:
  /// **'Climb'**
  String get activityEntryRecordingElevationGain;

  /// No description provided for @activityEntryRecordingPoints.
  ///
  /// In en, this message translates to:
  /// **'Points'**
  String get activityEntryRecordingPoints;

  /// No description provided for @activityEntryRecordingRestSecondsLabel.
  ///
  /// In en, this message translates to:
  /// **'Rest seconds'**
  String get activityEntryRecordingRestSecondsLabel;

  /// No description provided for @activityEntryRecordingEndSet.
  ///
  /// In en, this message translates to:
  /// **'End set'**
  String get activityEntryRecordingEndSet;

  /// No description provided for @activityEntryRecordingStartNextSet.
  ///
  /// In en, this message translates to:
  /// **'Start next set'**
  String get activityEntryRecordingStartNextSet;

  /// No description provided for @activityEntryRecordingEndSession.
  ///
  /// In en, this message translates to:
  /// **'Finish session'**
  String get activityEntryRecordingEndSession;

  /// No description provided for @activityEntryRecordingRestRemaining.
  ///
  /// In en, this message translates to:
  /// **'Rest {arg0}'**
  String activityEntryRecordingRestRemaining(String arg0);

  /// No description provided for @activityEntryRecordingFinishHint.
  ///
  /// In en, this message translates to:
  /// **'Finish opens the activity details form so you can add title, notes, calories, or adjust values before saving.'**
  String get activityEntryRecordingFinishHint;

  /// No description provided for @activityEntryRecordingRepetitionCorrectionHint.
  ///
  /// In en, this message translates to:
  /// **'Use + or - if the sensor misses or adds a rep.'**
  String get activityEntryRecordingRepetitionCorrectionHint;

  /// No description provided for @activityEntryRecordingAccuracy.
  ///
  /// In en, this message translates to:
  /// **'Last accuracy {arg0}'**
  String activityEntryRecordingAccuracy(String arg0);

  /// No description provided for @activityEntryRecordingFocus.
  ///
  /// In en, this message translates to:
  /// **'Focus'**
  String get activityEntryRecordingFocus;

  /// No description provided for @activityEntryRecordingDashboardLayout.
  ///
  /// In en, this message translates to:
  /// **'Dashboard layout'**
  String get activityEntryRecordingDashboardLayout;

  /// No description provided for @activityEntryRecordingDashboardLayoutTwoByFour.
  ///
  /// In en, this message translates to:
  /// **'2x4'**
  String get activityEntryRecordingDashboardLayoutTwoByFour;

  /// No description provided for @activityEntryRecordingDashboardLayoutThreeByFour.
  ///
  /// In en, this message translates to:
  /// **'3x4'**
  String get activityEntryRecordingDashboardLayoutThreeByFour;

  /// No description provided for @activityEntryRecordingDashboardLayoutLargeTop.
  ///
  /// In en, this message translates to:
  /// **'Large top'**
  String get activityEntryRecordingDashboardLayoutLargeTop;

  /// No description provided for @activityEntryRecordingDashboardAddField.
  ///
  /// In en, this message translates to:
  /// **'Add widget'**
  String get activityEntryRecordingDashboardAddField;

  /// No description provided for @activityEntryRouteSummary.
  ///
  /// In en, this message translates to:
  /// **'{arg0} • {arg1} • {arg2} gain • {arg3} points'**
  String activityEntryRouteSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  );

  /// No description provided for @activityEntryRouteAverageMetrics.
  ///
  /// In en, this message translates to:
  /// **'Avg pace {arg0} • avg speed {arg1}'**
  String activityEntryRouteAverageMetrics(String arg0, String arg1);

  /// No description provided for @activityEntryAdd.
  ///
  /// In en, this message translates to:
  /// **'Save activity'**
  String get activityEntryAdd;

  /// No description provided for @activityEntryInvalidValue.
  ///
  /// In en, this message translates to:
  /// **'Fix the highlighted fields before saving the activity.'**
  String get activityEntryInvalidValue;

  /// No description provided for @activityEntryErrorActivityTypeRoute.
  ///
  /// In en, this message translates to:
  /// **'Choose an activity type that supports GPS routes.'**
  String get activityEntryErrorActivityTypeRoute;

  /// No description provided for @activityEntryErrorTrainingPlanTitleRequired.
  ///
  /// In en, this message translates to:
  /// **'Enter a title to save this training plan.'**
  String get activityEntryErrorTrainingPlanTitleRequired;

  /// No description provided for @activityEntryErrorStartDate.
  ///
  /// In en, this message translates to:
  /// **'Choose a valid start date.'**
  String get activityEntryErrorStartDate;

  /// No description provided for @activityEntryErrorStartTime.
  ///
  /// In en, this message translates to:
  /// **'Choose a valid start time.'**
  String get activityEntryErrorStartTime;

  /// No description provided for @activityEntryErrorStartTimeAfterRoute.
  ///
  /// In en, this message translates to:
  /// **'Start time must be at or before the imported route start.'**
  String get activityEntryErrorStartTimeAfterRoute;

  /// No description provided for @activityEntryErrorDuration.
  ///
  /// In en, this message translates to:
  /// **'Duration must be between 1 minute and 7 days.'**
  String get activityEntryErrorDuration;

  /// No description provided for @activityEntryErrorRepetitions.
  ///
  /// In en, this message translates to:
  /// **'Enter positive counts. Rest must fit inside the activity duration.'**
  String get activityEntryErrorRepetitions;

  /// No description provided for @activityEntryErrorDistance.
  ///
  /// In en, this message translates to:
  /// **'Enter a distance greater than 0.'**
  String get activityEntryErrorDistance;

  /// No description provided for @activityEntryErrorDistanceUnsupported.
  ///
  /// In en, this message translates to:
  /// **'This activity type does not support distance.'**
  String get activityEntryErrorDistanceUnsupported;

  /// No description provided for @activityEntryErrorElevation.
  ///
  /// In en, this message translates to:
  /// **'Enter elevation greater than 0.'**
  String get activityEntryErrorElevation;

  /// No description provided for @activityEntryErrorElevationUnsupported.
  ///
  /// In en, this message translates to:
  /// **'This activity type does not support elevation gain.'**
  String get activityEntryErrorElevationUnsupported;

  /// No description provided for @activityEntryErrorActiveCalories.
  ///
  /// In en, this message translates to:
  /// **'Enter active calories greater than 0.'**
  String get activityEntryErrorActiveCalories;

  /// No description provided for @activityEntryErrorTotalCalories.
  ///
  /// In en, this message translates to:
  /// **'Enter total calories greater than 0.'**
  String get activityEntryErrorTotalCalories;

  /// No description provided for @activityEntryErrorTotalCaloriesBelowActive.
  ///
  /// In en, this message translates to:
  /// **'Total calories cannot be lower than active calories.'**
  String get activityEntryErrorTotalCaloriesBelowActive;

  /// No description provided for @activityEntryLocationPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'Precise location permission is required to record GPS activities.'**
  String get activityEntryLocationPermissionNeeded;

  /// No description provided for @activityEntryNotificationPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'Notification permission is required so OpenVitals can show an ongoing recording notification.'**
  String get activityEntryNotificationPermissionNeeded;

  /// No description provided for @activityEntryActivityRecognitionPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'Activity recognition permission is required to count treadmill steps.'**
  String get activityEntryActivityRecognitionPermissionNeeded;

  /// No description provided for @activityEntryRouteImportFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not import activity file: {arg0}'**
  String activityEntryRouteImportFailed(String arg0);

  /// No description provided for @activityEntryRecordingFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not record activity: {arg0}'**
  String activityEntryRecordingFailed(String arg0);

  /// No description provided for @activityEntryWriteFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not write activity entry: {arg0}'**
  String activityEntryWriteFailed(String arg0);

  /// No description provided for @activityRouteOpenInMap.
  ///
  /// In en, this message translates to:
  /// **'Open route in map app'**
  String get activityRouteOpenInMap;

  /// No description provided for @activityRouteExportGpx.
  ///
  /// In en, this message translates to:
  /// **'Save GPX'**
  String get activityRouteExportGpx;

  /// No description provided for @activityRouteExportKmz.
  ///
  /// In en, this message translates to:
  /// **'Save KMZ'**
  String get activityRouteExportKmz;

  /// No description provided for @activityRouteExportSaved.
  ///
  /// In en, this message translates to:
  /// **'Route saved.'**
  String get activityRouteExportSaved;

  /// No description provided for @activityRouteExportFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not save route file.'**
  String get activityRouteExportFailed;

  /// No description provided for @activityRouteOpenChooserTitle.
  ///
  /// In en, this message translates to:
  /// **'Open route with'**
  String get activityRouteOpenChooserTitle;

  /// No description provided for @activityRouteOpenFailed.
  ///
  /// In en, this message translates to:
  /// **'No map app could open this route.'**
  String get activityRouteOpenFailed;

  /// No description provided for @activityDetailAnalysisTitle.
  ///
  /// In en, this message translates to:
  /// **'Route analysis'**
  String get activityDetailAnalysisTitle;

  /// No description provided for @activityDetailTabMarkers.
  ///
  /// In en, this message translates to:
  /// **'Markers'**
  String get activityDetailTabMarkers;

  /// No description provided for @activityDetailNoMarkers.
  ///
  /// In en, this message translates to:
  /// **'No markers yet'**
  String get activityDetailNoMarkers;

  /// No description provided for @activityRecordingVoiceSummary.
  ///
  /// In en, this message translates to:
  /// **'Time {arg0}. Distance {arg1}. Average speed {arg2}. Current lap {arg3}.'**
  String activityRecordingVoiceSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  );

  /// No description provided for @activityRecordingVoiceLap.
  ///
  /// In en, this message translates to:
  /// **'Lap {arg0}. {arg1}'**
  String activityRecordingVoiceLap(int arg0, String arg1);

  /// No description provided for @activityRecordingVoiceIdle.
  ///
  /// In en, this message translates to:
  /// **'Idle.'**
  String get activityRecordingVoiceIdle;

  /// No description provided for @activityRecordingVoiceResumed.
  ///
  /// In en, this message translates to:
  /// **'Recording resumed.'**
  String get activityRecordingVoiceResumed;

  /// No description provided for @activityRecordingNotificationChannel.
  ///
  /// In en, this message translates to:
  /// **'Activity recording'**
  String get activityRecordingNotificationChannel;

  /// No description provided for @activityRecordingNotificationTitle.
  ///
  /// In en, this message translates to:
  /// **'Recording activity'**
  String get activityRecordingNotificationTitle;

  /// No description provided for @activityRecordingNotificationRecording.
  ///
  /// In en, this message translates to:
  /// **'Recording • {arg0} total • {arg1} moving • {arg2} • {arg3}'**
  String activityRecordingNotificationRecording(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  );

  /// No description provided for @activityRecordingNotificationPaused.
  ///
  /// In en, this message translates to:
  /// **'Paused • {arg0} total • {arg1} moving • {arg2} • {arg3}'**
  String activityRecordingNotificationPaused(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  );

  /// No description provided for @activityRecordingNotificationRepetitionRecording.
  ///
  /// In en, this message translates to:
  /// **'Recording • {arg0} total • {arg1} {arg2}'**
  String activityRecordingNotificationRepetitionRecording(
    String arg0,
    String arg1,
    String arg2,
  );

  /// No description provided for @activityRecordingNotificationRepetitionPaused.
  ///
  /// In en, this message translates to:
  /// **'Paused • {arg0} total • {arg1} {arg2}'**
  String activityRecordingNotificationRepetitionPaused(
    String arg0,
    String arg1,
    String arg2,
  );

  /// No description provided for @activityRecordingNotificationRepetitionResting.
  ///
  /// In en, this message translates to:
  /// **'Resting • {arg0} total • {arg1} left'**
  String activityRecordingNotificationRepetitionResting(
    String arg0,
    String arg1,
  );

  /// No description provided for @activityRecordingNotificationTimedRecording.
  ///
  /// In en, this message translates to:
  /// **'Recording • {arg0} total'**
  String activityRecordingNotificationTimedRecording(String arg0);

  /// No description provided for @activityRecordingNotificationTimedPaused.
  ///
  /// In en, this message translates to:
  /// **'Paused • {arg0} total'**
  String activityRecordingNotificationTimedPaused(String arg0);

  /// No description provided for @activityRecordingErrorService.
  ///
  /// In en, this message translates to:
  /// **'Could not start activity recording service.'**
  String get activityRecordingErrorService;

  /// No description provided for @activityRecordingErrorPreciseLocationPermission.
  ///
  /// In en, this message translates to:
  /// **'Precise location permission is required for reliable GPS tracks.'**
  String get activityRecordingErrorPreciseLocationPermission;

  /// No description provided for @activityRecordingErrorNotificationPermission.
  ///
  /// In en, this message translates to:
  /// **'Notification permission is required to show the ongoing recording notification.'**
  String get activityRecordingErrorNotificationPermission;

  /// No description provided for @activityRecordingErrorActivityRecognitionPermission.
  ///
  /// In en, this message translates to:
  /// **'Activity recognition permission is required to count treadmill steps.'**
  String get activityRecordingErrorActivityRecognitionPermission;

  /// No description provided for @activityRecordingErrorWaitingForGps.
  ///
  /// In en, this message translates to:
  /// **'Wait for a precise GPS fix before starting.'**
  String get activityRecordingErrorWaitingForGps;

  /// No description provided for @activityRecordingErrorProvider.
  ///
  /// In en, this message translates to:
  /// **'Turn on GPS to record a route.'**
  String get activityRecordingErrorProvider;

  /// No description provided for @activityRecordingErrorUnsupportedType.
  ///
  /// In en, this message translates to:
  /// **'This activity type cannot be recorded live.'**
  String get activityRecordingErrorUnsupportedType;

  /// No description provided for @activityRecordingErrorProximitySensor.
  ///
  /// In en, this message translates to:
  /// **'This device does not expose a proximity sensor for push-up counting.'**
  String get activityRecordingErrorProximitySensor;

  /// No description provided for @activityRecordingErrorAccelerometer.
  ///
  /// In en, this message translates to:
  /// **'This device does not expose an accelerometer for this recording.'**
  String get activityRecordingErrorAccelerometer;

  /// No description provided for @activityRecordingErrorStepDetector.
  ///
  /// In en, this message translates to:
  /// **'This device does not expose Android step detector events.'**
  String get activityRecordingErrorStepDetector;

  /// No description provided for @activityRecordingHowItWorks.
  ///
  /// In en, this message translates to:
  /// **'How recording works'**
  String get activityRecordingHowItWorks;

  /// No description provided for @activityRecordingGuidancePushUps.
  ///
  /// In en, this message translates to:
  /// **'Place the phone screen-up under the chest or head area. The proximity sensor counts a rep when you move close to the phone.'**
  String get activityRecordingGuidancePushUps;

  /// No description provided for @activityRecordingGuidancePullUps.
  ///
  /// In en, this message translates to:
  /// **'Secure the phone on your body. The accelerometer counts the pull and relax motion.'**
  String get activityRecordingGuidancePullUps;

  /// No description provided for @activityRecordingGuidanceRopeSkipping.
  ///
  /// In en, this message translates to:
  /// **'Keep the phone secured on your body. The accelerometer counts jumps.'**
  String get activityRecordingGuidanceRopeSkipping;

  /// No description provided for @activityRecordingGuidanceTrampolineJumping.
  ///
  /// In en, this message translates to:
  /// **'Keep the phone secured on your body. Jump detection uses a longer jump window than rope skipping.'**
  String get activityRecordingGuidanceTrampolineJumping;

  /// No description provided for @activityRecordingGuidanceTreadmill.
  ///
  /// In en, this message translates to:
  /// **'Carry the phone on your body. Android’s step detector counts steps; no GPS route is recorded.'**
  String get activityRecordingGuidanceTreadmill;

  /// No description provided for @activityRecordingSensorReady.
  ///
  /// In en, this message translates to:
  /// **'Sensor ready'**
  String get activityRecordingSensorReady;

  /// No description provided for @activityRecordingSensorUnavailableManual.
  ///
  /// In en, this message translates to:
  /// **'Live counting is unavailable on this device. Manual entry is still available.'**
  String get activityRecordingSensorUnavailableManual;

  /// No description provided for @activityRecordingActivityRecognitionMissing.
  ///
  /// In en, this message translates to:
  /// **'Grant activity recognition to count treadmill steps.'**
  String get activityRecordingActivityRecognitionMissing;

  /// No description provided for @exerciseTypeRunning.
  ///
  /// In en, this message translates to:
  /// **'Running'**
  String get exerciseTypeRunning;

  /// No description provided for @exerciseTypeBiking.
  ///
  /// In en, this message translates to:
  /// **'Biking'**
  String get exerciseTypeBiking;

  /// No description provided for @exerciseTypeWalking.
  ///
  /// In en, this message translates to:
  /// **'Walking'**
  String get exerciseTypeWalking;

  /// No description provided for @exerciseTypeHiking.
  ///
  /// In en, this message translates to:
  /// **'Hiking'**
  String get exerciseTypeHiking;

  /// No description provided for @exerciseTypeWheelchair.
  ///
  /// In en, this message translates to:
  /// **'Wheelchair'**
  String get exerciseTypeWheelchair;

  /// No description provided for @exerciseTypeRowing.
  ///
  /// In en, this message translates to:
  /// **'Rowing'**
  String get exerciseTypeRowing;

  /// No description provided for @exerciseTypePaddling.
  ///
  /// In en, this message translates to:
  /// **'Paddling'**
  String get exerciseTypePaddling;

  /// No description provided for @exerciseTypeSkiing.
  ///
  /// In en, this message translates to:
  /// **'Skiing'**
  String get exerciseTypeSkiing;

  /// No description provided for @exerciseTypeSnowboarding.
  ///
  /// In en, this message translates to:
  /// **'Snowboarding'**
  String get exerciseTypeSnowboarding;

  /// No description provided for @exerciseTypeSnowshoeing.
  ///
  /// In en, this message translates to:
  /// **'Snowshoeing'**
  String get exerciseTypeSnowshoeing;

  /// No description provided for @exerciseTypeSkating.
  ///
  /// In en, this message translates to:
  /// **'Skating'**
  String get exerciseTypeSkating;

  /// No description provided for @exerciseTypeSailing.
  ///
  /// In en, this message translates to:
  /// **'Sailing'**
  String get exerciseTypeSailing;

  /// No description provided for @exerciseTypeSurfing.
  ///
  /// In en, this message translates to:
  /// **'Surfing'**
  String get exerciseTypeSurfing;

  /// No description provided for @exerciseTypeSwimmingOpenWater.
  ///
  /// In en, this message translates to:
  /// **'Swimming (open water)'**
  String get exerciseTypeSwimmingOpenWater;

  /// No description provided for @exerciseTypeGolf.
  ///
  /// In en, this message translates to:
  /// **'Golf'**
  String get exerciseTypeGolf;

  /// No description provided for @exerciseTypeStrengthTraining.
  ///
  /// In en, this message translates to:
  /// **'Strength training'**
  String get exerciseTypeStrengthTraining;

  /// No description provided for @exerciseTypeTreadmill.
  ///
  /// In en, this message translates to:
  /// **'Treadmill'**
  String get exerciseTypeTreadmill;

  /// No description provided for @exerciseTypePushUps.
  ///
  /// In en, this message translates to:
  /// **'Push-ups'**
  String get exerciseTypePushUps;

  /// No description provided for @exerciseTypePullUps.
  ///
  /// In en, this message translates to:
  /// **'Pull-ups'**
  String get exerciseTypePullUps;

  /// No description provided for @exerciseTypeRopeSkipping.
  ///
  /// In en, this message translates to:
  /// **'Rope skipping'**
  String get exerciseTypeRopeSkipping;

  /// No description provided for @exerciseTypeTrampolineJumping.
  ///
  /// In en, this message translates to:
  /// **'Trampoline jumping'**
  String get exerciseTypeTrampolineJumping;

  /// No description provided for @exerciseTypeOtherWorkout.
  ///
  /// In en, this message translates to:
  /// **'Other workout'**
  String get exerciseTypeOtherWorkout;

  /// No description provided for @mindfulnessEntryUnavailable.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness sessions are unavailable in this Health Connect provider.'**
  String get mindfulnessEntryUnavailable;

  /// No description provided for @mindfulnessEntryTimerTitle.
  ///
  /// In en, this message translates to:
  /// **'Timer'**
  String get mindfulnessEntryTimerTitle;

  /// No description provided for @mindfulnessEntryManualTitle.
  ///
  /// In en, this message translates to:
  /// **'Manual entry'**
  String get mindfulnessEntryManualTitle;

  /// No description provided for @mindfulnessEntryIntervalBell.
  ///
  /// In en, this message translates to:
  /// **'Interval bell'**
  String get mindfulnessEntryIntervalBell;

  /// No description provided for @mindfulnessEntryIntervalMinutes.
  ///
  /// In en, this message translates to:
  /// **'Interval (min)'**
  String get mindfulnessEntryIntervalMinutes;

  /// No description provided for @mindfulnessEntryBellSound.
  ///
  /// In en, this message translates to:
  /// **'Bell sound'**
  String get mindfulnessEntryBellSound;

  /// No description provided for @mindfulnessEntryBackgroundSound.
  ///
  /// In en, this message translates to:
  /// **'Background sound'**
  String get mindfulnessEntryBackgroundSound;

  /// No description provided for @mindfulnessBellStruck.
  ///
  /// In en, this message translates to:
  /// **'Soft strike'**
  String get mindfulnessBellStruck;

  /// No description provided for @mindfulnessBellRubbed.
  ///
  /// In en, this message translates to:
  /// **'Warm bowl'**
  String get mindfulnessBellRubbed;

  /// No description provided for @mindfulnessBellBright.
  ///
  /// In en, this message translates to:
  /// **'Bright bowl'**
  String get mindfulnessBellBright;

  /// No description provided for @mindfulnessBellTemple.
  ///
  /// In en, this message translates to:
  /// **'Temple bowl'**
  String get mindfulnessBellTemple;

  /// No description provided for @mindfulnessBellHarmony.
  ///
  /// In en, this message translates to:
  /// **'Harmony'**
  String get mindfulnessBellHarmony;

  /// No description provided for @mindfulnessBackgroundNone.
  ///
  /// In en, this message translates to:
  /// **'None'**
  String get mindfulnessBackgroundNone;

  /// No description provided for @mindfulnessBackgroundBowl.
  ///
  /// In en, this message translates to:
  /// **'Bowl'**
  String get mindfulnessBackgroundBowl;

  /// No description provided for @mindfulnessBackgroundMeditation.
  ///
  /// In en, this message translates to:
  /// **'Meditation'**
  String get mindfulnessBackgroundMeditation;

  /// No description provided for @mindfulnessBackgroundChimes.
  ///
  /// In en, this message translates to:
  /// **'Chimes'**
  String get mindfulnessBackgroundChimes;

  /// No description provided for @mindfulnessBackgroundDreamscape.
  ///
  /// In en, this message translates to:
  /// **'Dreamscape'**
  String get mindfulnessBackgroundDreamscape;

  /// No description provided for @mindfulnessEntryStartTimer.
  ///
  /// In en, this message translates to:
  /// **'Start'**
  String get mindfulnessEntryStartTimer;

  /// No description provided for @mindfulnessEntryStopTimer.
  ///
  /// In en, this message translates to:
  /// **'Stop'**
  String get mindfulnessEntryStopTimer;

  /// No description provided for @mindfulnessEntryResumeTimer.
  ///
  /// In en, this message translates to:
  /// **'Resume'**
  String get mindfulnessEntryResumeTimer;

  /// No description provided for @mindfulnessEntryDiscardTimer.
  ///
  /// In en, this message translates to:
  /// **'Discard'**
  String get mindfulnessEntryDiscardTimer;

  /// No description provided for @mindfulnessEntrySaveSession.
  ///
  /// In en, this message translates to:
  /// **'Save session'**
  String get mindfulnessEntrySaveSession;

  /// No description provided for @mindfulnessEntryMinutes.
  ///
  /// In en, this message translates to:
  /// **'Minutes'**
  String get mindfulnessEntryMinutes;

  /// No description provided for @mindfulnessEntryAddMinutes.
  ///
  /// In en, this message translates to:
  /// **'Add minutes'**
  String get mindfulnessEntryAddMinutes;

  /// No description provided for @mindfulnessEntryInvalidTimer.
  ///
  /// In en, this message translates to:
  /// **'Enter a valid timer duration and interval.'**
  String get mindfulnessEntryInvalidTimer;

  /// No description provided for @mindfulnessEntryInvalidManual.
  ///
  /// In en, this message translates to:
  /// **'Enter valid mindfulness minutes.'**
  String get mindfulnessEntryInvalidManual;

  /// No description provided for @mindfulnessEntryTimerTooShort.
  ///
  /// In en, this message translates to:
  /// **'Meditation must be at least 1 minute to save.'**
  String get mindfulnessEntryTimerTooShort;

  /// No description provided for @mindfulnessEntryWriteFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not save mindfulness session: {arg0}'**
  String mindfulnessEntryWriteFailed(String arg0);

  /// No description provided for @mindfulnessEntryCompleted.
  ///
  /// In en, this message translates to:
  /// **'Timer complete'**
  String get mindfulnessEntryCompleted;

  /// No description provided for @mindfulnessRemindersTitle.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness reminders'**
  String get mindfulnessRemindersTitle;

  /// No description provided for @mindfulnessRemindersSummaryOff.
  ///
  /// In en, this message translates to:
  /// **'Off by default. Enable a once-daily reminder for your mindfulness goal.'**
  String get mindfulnessRemindersSummaryOff;

  /// No description provided for @mindfulnessRemindersSummaryOn.
  ///
  /// In en, this message translates to:
  /// **'Daily at {arg0}'**
  String mindfulnessRemindersSummaryOn(String arg0);

  /// No description provided for @mindfulnessRemindersPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'Grant notification permission to enable mindfulness reminders.'**
  String get mindfulnessRemindersPermissionNeeded;

  /// No description provided for @mindfulnessRemindersTime.
  ///
  /// In en, this message translates to:
  /// **'Reminder time'**
  String get mindfulnessRemindersTime;

  /// No description provided for @mindfulnessRemindersGoalNote.
  ///
  /// In en, this message translates to:
  /// **'Reminders pause after today\'s mindfulness goal is met and resume tomorrow.'**
  String get mindfulnessRemindersGoalNote;

  /// No description provided for @mindfulnessReminderNotificationChannel.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness reminders'**
  String get mindfulnessReminderNotificationChannel;

  /// No description provided for @mindfulnessReminderNotificationChannelDesc.
  ///
  /// In en, this message translates to:
  /// **'Optional reminders to complete your daily mindfulness goal.'**
  String get mindfulnessReminderNotificationChannelDesc;

  /// No description provided for @mindfulnessReminderNotificationTitle.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness reminder'**
  String get mindfulnessReminderNotificationTitle;

  /// No description provided for @mindfulnessReminderNotificationBody.
  ///
  /// In en, this message translates to:
  /// **'Your goal is {arg0} today. Take a mindful pause when you can.'**
  String mindfulnessReminderNotificationBody(String arg0);

  /// No description provided for @mindfulnessReminderNotificationProgress.
  ///
  /// In en, this message translates to:
  /// **'{arg0} / {arg1}'**
  String mindfulnessReminderNotificationProgress(String arg0, String arg1);

  /// No description provided for @bodyEntrySubtitle.
  ///
  /// In en, this message translates to:
  /// **'{arg0} entries are saved directly to Health Connect.'**
  String bodyEntrySubtitle(String arg0);

  /// No description provided for @bodyEntryPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'To add {arg0} entries, OpenVitals needs Health Connect write permission. The app will not store this data; entries are saved in Health Connect.'**
  String bodyEntryPermissionNeeded(String arg0);

  /// No description provided for @bodyEntryValueLabel.
  ///
  /// In en, this message translates to:
  /// **'{arg0} ({arg1})'**
  String bodyEntryValueLabel(String arg0, String arg1);

  /// No description provided for @bodyEntryAddSelected.
  ///
  /// In en, this message translates to:
  /// **'Add {arg0}'**
  String bodyEntryAddSelected(String arg0);

  /// No description provided for @bodyEntryInvalidValue.
  ///
  /// In en, this message translates to:
  /// **'Enter a valid value for this measurement.'**
  String get bodyEntryInvalidValue;

  /// No description provided for @bodyEntryWriteFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not save body measurement: {arg0}'**
  String bodyEntryWriteFailed(String arg0);

  /// No description provided for @carbsEntrySubtitle.
  ///
  /// In en, this message translates to:
  /// **'Carbs entries are saved directly to Health Connect.'**
  String get carbsEntrySubtitle;

  /// No description provided for @carbsEntryPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'To add carbs entries, OpenVitals needs Health Connect write permission. The app will not store this data; entries are saved in Health Connect.'**
  String get carbsEntryPermissionNeeded;

  /// No description provided for @carbsEntryValueLabel.
  ///
  /// In en, this message translates to:
  /// **'Carbs ({arg0})'**
  String carbsEntryValueLabel(String arg0);

  /// No description provided for @carbsEntryAdd.
  ///
  /// In en, this message translates to:
  /// **'Add carbs'**
  String get carbsEntryAdd;

  /// No description provided for @carbsEntryInvalidValue.
  ///
  /// In en, this message translates to:
  /// **'Enter a valid carbs amount.'**
  String get carbsEntryInvalidValue;

  /// No description provided for @carbsEntryWriteFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not save carbs: {arg0}'**
  String carbsEntryWriteFailed(String arg0);

  /// No description provided for @vitalsEntrySubtitle.
  ///
  /// In en, this message translates to:
  /// **'{arg0} entries are saved directly to Health Connect.'**
  String vitalsEntrySubtitle(String arg0);

  /// No description provided for @vitalsEntryPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'To add {arg0} entries, OpenVitals needs Health Connect write permission. The app will not store this data; entries are saved in Health Connect.'**
  String vitalsEntryPermissionNeeded(String arg0);

  /// No description provided for @vitalsEntryValueLabel.
  ///
  /// In en, this message translates to:
  /// **'{arg0} ({arg1})'**
  String vitalsEntryValueLabel(String arg0, String arg1);

  /// No description provided for @vitalsEntrySystolicLabel.
  ///
  /// In en, this message translates to:
  /// **'Systolic (mmHg)'**
  String get vitalsEntrySystolicLabel;

  /// No description provided for @vitalsEntryDiastolicLabel.
  ///
  /// In en, this message translates to:
  /// **'Diastolic (mmHg)'**
  String get vitalsEntryDiastolicLabel;

  /// No description provided for @vitalsEntryAddSelected.
  ///
  /// In en, this message translates to:
  /// **'Add {arg0}'**
  String vitalsEntryAddSelected(String arg0);

  /// No description provided for @vitalsEntryInvalidValue.
  ///
  /// In en, this message translates to:
  /// **'Enter a valid value for this vital.'**
  String get vitalsEntryInvalidValue;

  /// No description provided for @vitalsEntryWriteFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not save vital: {arg0}'**
  String vitalsEntryWriteFailed(String arg0);

  /// No description provided for @rangeDay.
  ///
  /// In en, this message translates to:
  /// **'Day'**
  String get rangeDay;

  /// No description provided for @rangeWeek.
  ///
  /// In en, this message translates to:
  /// **'Week'**
  String get rangeWeek;

  /// No description provided for @rangeMonth.
  ///
  /// In en, this message translates to:
  /// **'Month'**
  String get rangeMonth;

  /// No description provided for @rangeYear.
  ///
  /// In en, this message translates to:
  /// **'Year'**
  String get rangeYear;

  /// No description provided for @periodToday.
  ///
  /// In en, this message translates to:
  /// **'Today'**
  String get periodToday;

  /// No description provided for @periodYesterday.
  ///
  /// In en, this message translates to:
  /// **'Yesterday'**
  String get periodYesterday;

  /// No description provided for @periodThisWeek.
  ///
  /// In en, this message translates to:
  /// **'This week'**
  String get periodThisWeek;

  /// No description provided for @periodWeekOf.
  ///
  /// In en, this message translates to:
  /// **'Week of {arg0}'**
  String periodWeekOf(String arg0);

  /// No description provided for @periodThisMonth.
  ///
  /// In en, this message translates to:
  /// **'This month'**
  String get periodThisMonth;

  /// No description provided for @periodThisYear.
  ///
  /// In en, this message translates to:
  /// **'This year'**
  String get periodThisYear;

  /// No description provided for @periodLast7Days.
  ///
  /// In en, this message translates to:
  /// **'Last 7 days'**
  String get periodLast7Days;

  /// No description provided for @periodLast30Days.
  ///
  /// In en, this message translates to:
  /// **'Last 30 days'**
  String get periodLast30Days;

  /// No description provided for @periodLast365Days.
  ///
  /// In en, this message translates to:
  /// **'Last 365 days'**
  String get periodLast365Days;

  /// No description provided for @periodSelected.
  ///
  /// In en, this message translates to:
  /// **'Selected period'**
  String get periodSelected;

  /// No description provided for @metricSteps.
  ///
  /// In en, this message translates to:
  /// **'Steps'**
  String get metricSteps;

  /// No description provided for @metricDistance.
  ///
  /// In en, this message translates to:
  /// **'Distance'**
  String get metricDistance;

  /// No description provided for @metricAveragePace.
  ///
  /// In en, this message translates to:
  /// **'Average pace'**
  String get metricAveragePace;

  /// No description provided for @metricAverageSpeed.
  ///
  /// In en, this message translates to:
  /// **'Average speed'**
  String get metricAverageSpeed;

  /// No description provided for @metricCaloriesBurned.
  ///
  /// In en, this message translates to:
  /// **'Total calories burned'**
  String get metricCaloriesBurned;

  /// No description provided for @metricCaloriesOut.
  ///
  /// In en, this message translates to:
  /// **'Total calories'**
  String get metricCaloriesOut;

  /// No description provided for @metricCaloriesIn.
  ///
  /// In en, this message translates to:
  /// **'Calories in'**
  String get metricCaloriesIn;

  /// No description provided for @metricFloorsClimbed.
  ///
  /// In en, this message translates to:
  /// **'Floors climbed'**
  String get metricFloorsClimbed;

  /// No description provided for @metricActiveCalories.
  ///
  /// In en, this message translates to:
  /// **'Active calories'**
  String get metricActiveCalories;

  /// No description provided for @metricElevation.
  ///
  /// In en, this message translates to:
  /// **'Elevation'**
  String get metricElevation;

  /// No description provided for @metricElevationGained.
  ///
  /// In en, this message translates to:
  /// **'Elevation gained'**
  String get metricElevationGained;

  /// No description provided for @metricWheelchairPushes.
  ///
  /// In en, this message translates to:
  /// **'Wheelchair pushes'**
  String get metricWheelchairPushes;

  /// No description provided for @metricWorkout.
  ///
  /// In en, this message translates to:
  /// **'Workout'**
  String get metricWorkout;

  /// No description provided for @metricSleep.
  ///
  /// In en, this message translates to:
  /// **'Sleep'**
  String get metricSleep;

  /// No description provided for @metricHydration.
  ///
  /// In en, this message translates to:
  /// **'Beverages'**
  String get metricHydration;

  /// No description provided for @metricTotalHydration.
  ///
  /// In en, this message translates to:
  /// **'Total hydration'**
  String get metricTotalHydration;

  /// No description provided for @metricHydrationTrend.
  ///
  /// In en, this message translates to:
  /// **'Beverage trend'**
  String get metricHydrationTrend;

  /// No description provided for @metricLoggedDays.
  ///
  /// In en, this message translates to:
  /// **'Logged days'**
  String get metricLoggedDays;

  /// No description provided for @metricLatestWeight.
  ///
  /// In en, this message translates to:
  /// **'Latest weight'**
  String get metricLatestWeight;

  /// No description provided for @metricBodyFat.
  ///
  /// In en, this message translates to:
  /// **'Body fat'**
  String get metricBodyFat;

  /// No description provided for @metricAvgHeartRate.
  ///
  /// In en, this message translates to:
  /// **'Avg heart rate'**
  String get metricAvgHeartRate;

  /// No description provided for @metricAverageHeartRate.
  ///
  /// In en, this message translates to:
  /// **'Average heart rate'**
  String get metricAverageHeartRate;

  /// No description provided for @metricRestingHeartRate.
  ///
  /// In en, this message translates to:
  /// **'Resting heart rate'**
  String get metricRestingHeartRate;

  /// No description provided for @metricHrv.
  ///
  /// In en, this message translates to:
  /// **'Heart rate variability (HRV)'**
  String get metricHrv;

  /// No description provided for @metricCardioLoad.
  ///
  /// In en, this message translates to:
  /// **'Cardio load'**
  String get metricCardioLoad;

  /// No description provided for @metricWeeklyCardioLoad.
  ///
  /// In en, this message translates to:
  /// **'Weekly cardio'**
  String get metricWeeklyCardioLoad;

  /// No description provided for @metricEnergyBurned.
  ///
  /// In en, this message translates to:
  /// **'Total calories'**
  String get metricEnergyBurned;

  /// No description provided for @metricBloodPressure.
  ///
  /// In en, this message translates to:
  /// **'Blood pressure'**
  String get metricBloodPressure;

  /// No description provided for @metricSpo2.
  ///
  /// In en, this message translates to:
  /// **'SpO2'**
  String get metricSpo2;

  /// No description provided for @metricOxygenSaturation.
  ///
  /// In en, this message translates to:
  /// **'Oxygen saturation'**
  String get metricOxygenSaturation;

  /// No description provided for @metricVo2Max.
  ///
  /// In en, this message translates to:
  /// **'VO2 max'**
  String get metricVo2Max;

  /// No description provided for @metricMindfulness.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness'**
  String get metricMindfulness;

  /// No description provided for @metricTotalMindfulness.
  ///
  /// In en, this message translates to:
  /// **'Total mindfulness'**
  String get metricTotalMindfulness;

  /// No description provided for @metricCycle.
  ///
  /// In en, this message translates to:
  /// **'Cycle'**
  String get metricCycle;

  /// No description provided for @metricCycleTracking.
  ///
  /// In en, this message translates to:
  /// **'Cycle tracking'**
  String get metricCycleTracking;

  /// No description provided for @metricPeriodDays.
  ///
  /// In en, this message translates to:
  /// **'Period days'**
  String get metricPeriodDays;

  /// No description provided for @metricOvulationTests.
  ///
  /// In en, this message translates to:
  /// **'Ovulation tests'**
  String get metricOvulationTests;

  /// No description provided for @metricLatestBbt.
  ///
  /// In en, this message translates to:
  /// **'Latest BBT'**
  String get metricLatestBbt;

  /// No description provided for @metricWeight.
  ///
  /// In en, this message translates to:
  /// **'Weight'**
  String get metricWeight;

  /// No description provided for @metricHeight.
  ///
  /// In en, this message translates to:
  /// **'Height'**
  String get metricHeight;

  /// No description provided for @metricBmi.
  ///
  /// In en, this message translates to:
  /// **'BMI'**
  String get metricBmi;

  /// No description provided for @metricFfmi.
  ///
  /// In en, this message translates to:
  /// **'FFMI'**
  String get metricFfmi;

  /// No description provided for @metricLeanMass.
  ///
  /// In en, this message translates to:
  /// **'Lean mass'**
  String get metricLeanMass;

  /// No description provided for @metricBmr.
  ///
  /// In en, this message translates to:
  /// **'BMR'**
  String get metricBmr;

  /// No description provided for @metricBoneMass.
  ///
  /// In en, this message translates to:
  /// **'Bone mass'**
  String get metricBoneMass;

  /// No description provided for @metricBodyWaterMass.
  ///
  /// In en, this message translates to:
  /// **'Body water mass'**
  String get metricBodyWaterMass;

  /// No description provided for @metricLatest.
  ///
  /// In en, this message translates to:
  /// **'Latest'**
  String get metricLatest;

  /// No description provided for @metricChange.
  ///
  /// In en, this message translates to:
  /// **'Change'**
  String get metricChange;

  /// No description provided for @metricMacros.
  ///
  /// In en, this message translates to:
  /// **'Macros'**
  String get metricMacros;

  /// No description provided for @metricProtein.
  ///
  /// In en, this message translates to:
  /// **'Protein'**
  String get metricProtein;

  /// No description provided for @metricCarbs.
  ///
  /// In en, this message translates to:
  /// **'Carbs'**
  String get metricCarbs;

  /// No description provided for @metricFat.
  ///
  /// In en, this message translates to:
  /// **'Fat'**
  String get metricFat;

  /// No description provided for @metricDietaryFiber.
  ///
  /// In en, this message translates to:
  /// **'Dietary fiber'**
  String get metricDietaryFiber;

  /// No description provided for @metricSugar.
  ///
  /// In en, this message translates to:
  /// **'Sugar'**
  String get metricSugar;

  /// No description provided for @metricEnergyFromFat.
  ///
  /// In en, this message translates to:
  /// **'Calories from fat'**
  String get metricEnergyFromFat;

  /// No description provided for @metricMonounsaturatedFat.
  ///
  /// In en, this message translates to:
  /// **'Monounsaturated fat'**
  String get metricMonounsaturatedFat;

  /// No description provided for @metricPolyunsaturatedFat.
  ///
  /// In en, this message translates to:
  /// **'Polyunsaturated fat'**
  String get metricPolyunsaturatedFat;

  /// No description provided for @metricSaturatedFat.
  ///
  /// In en, this message translates to:
  /// **'Saturated fat'**
  String get metricSaturatedFat;

  /// No description provided for @metricTransFat.
  ///
  /// In en, this message translates to:
  /// **'Trans fat'**
  String get metricTransFat;

  /// No description provided for @metricUnsaturatedFat.
  ///
  /// In en, this message translates to:
  /// **'Unsaturated fat'**
  String get metricUnsaturatedFat;

  /// No description provided for @metricCholesterol.
  ///
  /// In en, this message translates to:
  /// **'Cholesterol'**
  String get metricCholesterol;

  /// No description provided for @metricBiotin.
  ///
  /// In en, this message translates to:
  /// **'Biotin'**
  String get metricBiotin;

  /// No description provided for @metricFolate.
  ///
  /// In en, this message translates to:
  /// **'Folate'**
  String get metricFolate;

  /// No description provided for @metricFolicAcid.
  ///
  /// In en, this message translates to:
  /// **'Folic acid'**
  String get metricFolicAcid;

  /// No description provided for @metricNiacin.
  ///
  /// In en, this message translates to:
  /// **'Niacin'**
  String get metricNiacin;

  /// No description provided for @metricPantothenicAcid.
  ///
  /// In en, this message translates to:
  /// **'Pantothenic acid'**
  String get metricPantothenicAcid;

  /// No description provided for @metricRiboflavin.
  ///
  /// In en, this message translates to:
  /// **'Riboflavin'**
  String get metricRiboflavin;

  /// No description provided for @metricThiamin.
  ///
  /// In en, this message translates to:
  /// **'Thiamin'**
  String get metricThiamin;

  /// No description provided for @metricVitaminA.
  ///
  /// In en, this message translates to:
  /// **'Vitamin A'**
  String get metricVitaminA;

  /// No description provided for @metricVitaminB12.
  ///
  /// In en, this message translates to:
  /// **'Vitamin B12'**
  String get metricVitaminB12;

  /// No description provided for @metricVitaminB6.
  ///
  /// In en, this message translates to:
  /// **'Vitamin B6'**
  String get metricVitaminB6;

  /// No description provided for @metricVitaminC.
  ///
  /// In en, this message translates to:
  /// **'Vitamin C'**
  String get metricVitaminC;

  /// No description provided for @metricVitaminD.
  ///
  /// In en, this message translates to:
  /// **'Vitamin D'**
  String get metricVitaminD;

  /// No description provided for @metricVitaminE.
  ///
  /// In en, this message translates to:
  /// **'Vitamin E'**
  String get metricVitaminE;

  /// No description provided for @metricVitaminK.
  ///
  /// In en, this message translates to:
  /// **'Vitamin K'**
  String get metricVitaminK;

  /// No description provided for @metricCalcium.
  ///
  /// In en, this message translates to:
  /// **'Calcium'**
  String get metricCalcium;

  /// No description provided for @metricChloride.
  ///
  /// In en, this message translates to:
  /// **'Chloride'**
  String get metricChloride;

  /// No description provided for @metricChromium.
  ///
  /// In en, this message translates to:
  /// **'Chromium'**
  String get metricChromium;

  /// No description provided for @metricCopper.
  ///
  /// In en, this message translates to:
  /// **'Copper'**
  String get metricCopper;

  /// No description provided for @metricIodine.
  ///
  /// In en, this message translates to:
  /// **'Iodine'**
  String get metricIodine;

  /// No description provided for @metricIron.
  ///
  /// In en, this message translates to:
  /// **'Iron'**
  String get metricIron;

  /// No description provided for @metricMagnesium.
  ///
  /// In en, this message translates to:
  /// **'Magnesium'**
  String get metricMagnesium;

  /// No description provided for @metricManganese.
  ///
  /// In en, this message translates to:
  /// **'Manganese'**
  String get metricManganese;

  /// No description provided for @metricMolybdenum.
  ///
  /// In en, this message translates to:
  /// **'Molybdenum'**
  String get metricMolybdenum;

  /// No description provided for @metricPhosphorus.
  ///
  /// In en, this message translates to:
  /// **'Phosphorus'**
  String get metricPhosphorus;

  /// No description provided for @metricPotassium.
  ///
  /// In en, this message translates to:
  /// **'Potassium'**
  String get metricPotassium;

  /// No description provided for @metricSelenium.
  ///
  /// In en, this message translates to:
  /// **'Selenium'**
  String get metricSelenium;

  /// No description provided for @metricSodium.
  ///
  /// In en, this message translates to:
  /// **'Sodium'**
  String get metricSodium;

  /// No description provided for @metricZinc.
  ///
  /// In en, this message translates to:
  /// **'Zinc'**
  String get metricZinc;

  /// No description provided for @metricCaffeine.
  ///
  /// In en, this message translates to:
  /// **'Caffeine'**
  String get metricCaffeine;

  /// No description provided for @metricRespiratoryRate.
  ///
  /// In en, this message translates to:
  /// **'Respiratory rate'**
  String get metricRespiratoryRate;

  /// No description provided for @metricAvgRespiratoryRate.
  ///
  /// In en, this message translates to:
  /// **'Avg respiratory rate'**
  String get metricAvgRespiratoryRate;

  /// No description provided for @metricBodyTemp.
  ///
  /// In en, this message translates to:
  /// **'Body temp'**
  String get metricBodyTemp;

  /// No description provided for @metricBloodGlucose.
  ///
  /// In en, this message translates to:
  /// **'Blood glucose'**
  String get metricBloodGlucose;

  /// No description provided for @metricSkinTemperature.
  ///
  /// In en, this message translates to:
  /// **'Skin temperature'**
  String get metricSkinTemperature;

  /// No description provided for @metricRecordedSpeed.
  ///
  /// In en, this message translates to:
  /// **'Recorded speed'**
  String get metricRecordedSpeed;

  /// No description provided for @metricAveragePower.
  ///
  /// In en, this message translates to:
  /// **'Average power'**
  String get metricAveragePower;

  /// No description provided for @metricStepsCadence.
  ///
  /// In en, this message translates to:
  /// **'Step cadence'**
  String get metricStepsCadence;

  /// No description provided for @metricCyclingCadence.
  ///
  /// In en, this message translates to:
  /// **'Cycling cadence'**
  String get metricCyclingCadence;

  /// No description provided for @unitSteps.
  ///
  /// In en, this message translates to:
  /// **'steps'**
  String get unitSteps;

  /// No description provided for @unitReps.
  ///
  /// In en, this message translates to:
  /// **'reps'**
  String get unitReps;

  /// No description provided for @unitPushes.
  ///
  /// In en, this message translates to:
  /// **'pushes'**
  String get unitPushes;

  /// No description provided for @unitFloors.
  ///
  /// In en, this message translates to:
  /// **'floors'**
  String get unitFloors;

  /// No description provided for @unitDays.
  ///
  /// In en, this message translates to:
  /// **'days'**
  String get unitDays;

  /// No description provided for @unitNights.
  ///
  /// In en, this message translates to:
  /// **'nights'**
  String get unitNights;

  /// No description provided for @unitTests.
  ///
  /// In en, this message translates to:
  /// **'tests'**
  String get unitTests;

  /// No description provided for @unitTotal.
  ///
  /// In en, this message translates to:
  /// **'total'**
  String get unitTotal;

  /// No description provided for @unitGrams.
  ///
  /// In en, this message translates to:
  /// **'g'**
  String get unitGrams;

  /// No description provided for @sectionActivities.
  ///
  /// In en, this message translates to:
  /// **'Activities'**
  String get sectionActivities;

  /// No description provided for @sectionActivityTypeStats.
  ///
  /// In en, this message translates to:
  /// **'By activity type'**
  String get sectionActivityTypeStats;

  /// No description provided for @activityTypeStatsActivityCount.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, one{# activity} other{# activities}}'**
  String activityTypeStatsActivityCount(int count);

  /// No description provided for @sectionPlannedWorkouts.
  ///
  /// In en, this message translates to:
  /// **'Planned workouts'**
  String get sectionPlannedWorkouts;

  /// No description provided for @activitiesFilterActivityTypeLabel.
  ///
  /// In en, this message translates to:
  /// **'Activity type'**
  String get activitiesFilterActivityTypeLabel;

  /// No description provided for @activitiesFilterAll.
  ///
  /// In en, this message translates to:
  /// **'All activities'**
  String get activitiesFilterAll;

  /// No description provided for @activitiesKeyMetrics.
  ///
  /// In en, this message translates to:
  /// **'Key metrics'**
  String get activitiesKeyMetrics;

  /// No description provided for @recoverySleepScore.
  ///
  /// In en, this message translates to:
  /// **'Sleep score'**
  String get recoverySleepScore;

  /// No description provided for @recoverySleepDuration.
  ///
  /// In en, this message translates to:
  /// **'Sleep duration'**
  String get recoverySleepDuration;

  /// No description provided for @recoverySleepSchedule.
  ///
  /// In en, this message translates to:
  /// **'Sleep schedule'**
  String get recoverySleepSchedule;

  /// No description provided for @recoveryRemSleep.
  ///
  /// In en, this message translates to:
  /// **'REM sleep'**
  String get recoveryRemSleep;

  /// No description provided for @recoveryDeepSleep.
  ///
  /// In en, this message translates to:
  /// **'Deep sleep'**
  String get recoveryDeepSleep;

  /// No description provided for @recoverySleepEfficiency.
  ///
  /// In en, this message translates to:
  /// **'Sleep efficiency'**
  String get recoverySleepEfficiency;

  /// No description provided for @sleepScoreConfidenceHigh.
  ///
  /// In en, this message translates to:
  /// **'High confidence'**
  String get sleepScoreConfidenceHigh;

  /// No description provided for @sleepScoreConfidenceMedium.
  ///
  /// In en, this message translates to:
  /// **'Medium confidence'**
  String get sleepScoreConfidenceMedium;

  /// No description provided for @sleepScoreConfidenceLow.
  ///
  /// In en, this message translates to:
  /// **'Low confidence'**
  String get sleepScoreConfidenceLow;

  /// No description provided for @sleepScoreConfidenceNoData.
  ///
  /// In en, this message translates to:
  /// **'No data'**
  String get sleepScoreConfidenceNoData;

  /// No description provided for @sleepScoreRatingExcellent.
  ///
  /// In en, this message translates to:
  /// **'Excellent'**
  String get sleepScoreRatingExcellent;

  /// No description provided for @sleepScoreRatingGood.
  ///
  /// In en, this message translates to:
  /// **'Good'**
  String get sleepScoreRatingGood;

  /// No description provided for @sleepScoreRatingFair.
  ///
  /// In en, this message translates to:
  /// **'Fair'**
  String get sleepScoreRatingFair;

  /// No description provided for @sleepScoreRatingPoor.
  ///
  /// In en, this message translates to:
  /// **'Poor'**
  String get sleepScoreRatingPoor;

  /// No description provided for @dashboardSleepScoreSubtitle.
  ///
  /// In en, this message translates to:
  /// **'{arg0} • {arg1}'**
  String dashboardSleepScoreSubtitle(String arg0, String arg1);

  /// No description provided for @sleepScoreCalculationTitle.
  ///
  /// In en, this message translates to:
  /// **'How it is calculated'**
  String get sleepScoreCalculationTitle;

  /// No description provided for @sleepScoreDayNumbersTitle.
  ///
  /// In en, this message translates to:
  /// **'Today values'**
  String get sleepScoreDayNumbersTitle;

  /// No description provided for @sleepScoreReferencesTitle.
  ///
  /// In en, this message translates to:
  /// **'Backed links'**
  String get sleepScoreReferencesTitle;

  /// No description provided for @sleepScoreCalculationSummary.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals scores objective sleep health from duration, efficiency, continuity, and regularity. It does not diagnose sleep disorders.'**
  String get sleepScoreCalculationSummary;

  /// No description provided for @sleepScoreFormula.
  ///
  /// In en, this message translates to:
  /// **'Sleep score = duration 35 + efficiency 30 + continuity 20 + regularity 15'**
  String get sleepScoreFormula;

  /// No description provided for @sleepScoreFormulaBody.
  ///
  /// In en, this message translates to:
  /// **'Duration gives full credit for 7-9 h. Efficiency uses total sleep time divided by time in bed. Continuity uses wake after sleep onset. Regularity compares today\'s sleep midpoint with recent nights.'**
  String get sleepScoreFormulaBody;

  /// No description provided for @sleepScoreComponentsBody.
  ///
  /// In en, this message translates to:
  /// **'Sleep-stage data improves confidence, but REM and deep sleep are not heavily scored because consumer stage estimates can vary. If regularity history is missing, OpenVitals uses a neutral regularity value and lowers confidence.'**
  String get sleepScoreComponentsBody;

  /// No description provided for @sleepScoreNotDiagnostic.
  ///
  /// In en, this message translates to:
  /// **'This score is a daily guide from Health Connect records, not a diagnosis or treatment recommendation.'**
  String get sleepScoreNotDiagnostic;

  /// No description provided for @sleepScoreComponentDuration.
  ///
  /// In en, this message translates to:
  /// **'Duration'**
  String get sleepScoreComponentDuration;

  /// No description provided for @sleepScoreComponentEfficiency.
  ///
  /// In en, this message translates to:
  /// **'Efficiency'**
  String get sleepScoreComponentEfficiency;

  /// No description provided for @sleepScoreComponentContinuity.
  ///
  /// In en, this message translates to:
  /// **'Continuity'**
  String get sleepScoreComponentContinuity;

  /// No description provided for @sleepScoreComponentRegularity.
  ///
  /// In en, this message translates to:
  /// **'Regularity'**
  String get sleepScoreComponentRegularity;

  /// No description provided for @sleepScoreTotalSleep.
  ///
  /// In en, this message translates to:
  /// **'Total sleep'**
  String get sleepScoreTotalSleep;

  /// No description provided for @sleepScoreTimeInBed.
  ///
  /// In en, this message translates to:
  /// **'Time in bed'**
  String get sleepScoreTimeInBed;

  /// No description provided for @sleepScoreEfficiency.
  ///
  /// In en, this message translates to:
  /// **'Efficiency'**
  String get sleepScoreEfficiency;

  /// No description provided for @sleepScoreWaso.
  ///
  /// In en, this message translates to:
  /// **'Wake after sleep'**
  String get sleepScoreWaso;

  /// No description provided for @sleepScoreRegularity.
  ///
  /// In en, this message translates to:
  /// **'Timing difference'**
  String get sleepScoreRegularity;

  /// No description provided for @sleepScoreBaselineNights.
  ///
  /// In en, this message translates to:
  /// **'Baseline nights'**
  String get sleepScoreBaselineNights;

  /// No description provided for @sleepScoreStageRecords.
  ///
  /// In en, this message translates to:
  /// **'Stage records'**
  String get sleepScoreStageRecords;

  /// No description provided for @sleepScoreQualityNoData.
  ///
  /// In en, this message translates to:
  /// **'Insufficient sleep data for a score.'**
  String get sleepScoreQualityNoData;

  /// No description provided for @sleepScoreQualityStageAwake.
  ///
  /// In en, this message translates to:
  /// **'Uses sleep stages and awake stages from Health Connect.'**
  String get sleepScoreQualityStageAwake;

  /// No description provided for @sleepScoreQualityStageOnly.
  ///
  /// In en, this message translates to:
  /// **'Uses sleep stages; awake continuity may be estimated.'**
  String get sleepScoreQualityStageOnly;

  /// No description provided for @sleepScoreQualitySessionOnly.
  ///
  /// In en, this message translates to:
  /// **'Uses sleep session timing only; confidence is limited.'**
  String get sleepScoreQualitySessionOnly;

  /// No description provided for @sleepScoreReferenceAasm.
  ///
  /// In en, this message translates to:
  /// **'AASM adult sleep duration'**
  String get sleepScoreReferenceAasm;

  /// No description provided for @sleepScoreReferenceSleepHealth.
  ///
  /// In en, this message translates to:
  /// **'Multidimensional sleep health'**
  String get sleepScoreReferenceSleepHealth;

  /// No description provided for @sleepScoreReferenceEfficiency.
  ///
  /// In en, this message translates to:
  /// **'Sleep efficiency definition'**
  String get sleepScoreReferenceEfficiency;

  /// No description provided for @sleepScoreReferenceRegularity.
  ///
  /// In en, this message translates to:
  /// **'Sleep regularity research'**
  String get sleepScoreReferenceRegularity;

  /// No description provided for @sleepEfficiencyConfidenceHigh.
  ///
  /// In en, this message translates to:
  /// **'High confidence'**
  String get sleepEfficiencyConfidenceHigh;

  /// No description provided for @sleepEfficiencyConfidenceLow.
  ///
  /// In en, this message translates to:
  /// **'Low confidence'**
  String get sleepEfficiencyConfidenceLow;

  /// No description provided for @sleepEfficiencyConfidenceNoData.
  ///
  /// In en, this message translates to:
  /// **'No data'**
  String get sleepEfficiencyConfidenceNoData;

  /// No description provided for @sleepEfficiencyCalculationTitle.
  ///
  /// In en, this message translates to:
  /// **'How it is calculated'**
  String get sleepEfficiencyCalculationTitle;

  /// No description provided for @sleepEfficiencyDayNumbersTitle.
  ///
  /// In en, this message translates to:
  /// **'Today values'**
  String get sleepEfficiencyDayNumbersTitle;

  /// No description provided for @sleepEfficiencyReferencesTitle.
  ///
  /// In en, this message translates to:
  /// **'Backed links'**
  String get sleepEfficiencyReferencesTitle;

  /// No description provided for @sleepEfficiencyCalculationSummary.
  ///
  /// In en, this message translates to:
  /// **'Sleep efficiency is the percentage of the sleep window spent asleep. Higher values usually mean less time awake in bed.'**
  String get sleepEfficiencyCalculationSummary;

  /// No description provided for @sleepEfficiencyFormula.
  ///
  /// In en, this message translates to:
  /// **'Sleep efficiency = total sleep time / time in bed x 100'**
  String get sleepEfficiencyFormula;

  /// No description provided for @sleepEfficiencyFormulaBody.
  ///
  /// In en, this message translates to:
  /// **'Total sleep time is the sum of Health Connect sleep stages when stages are available. Time in bed is the main sleep session start-to-end window.'**
  String get sleepEfficiencyFormulaBody;

  /// No description provided for @sleepEfficiencyDataBody.
  ///
  /// In en, this message translates to:
  /// **'When sleep stages are missing, Health Connect may only provide a session duration. OpenVitals can still show an estimate, but confidence is low because awake time in bed may be hidden.'**
  String get sleepEfficiencyDataBody;

  /// No description provided for @sleepEfficiencyNotDiagnostic.
  ///
  /// In en, this message translates to:
  /// **'Sleep efficiency is a sleep-continuity signal, not a diagnosis. Persistently low values can be worth discussing with a clinician.'**
  String get sleepEfficiencyNotDiagnostic;

  /// No description provided for @sleepEfficiencyQualityNoData.
  ///
  /// In en, this message translates to:
  /// **'Insufficient sleep data for efficiency.'**
  String get sleepEfficiencyQualityNoData;

  /// No description provided for @sleepEfficiencyQualityStageBased.
  ///
  /// In en, this message translates to:
  /// **'Uses Health Connect sleep stages for total sleep time.'**
  String get sleepEfficiencyQualityStageBased;

  /// No description provided for @sleepEfficiencyQualitySessionOnly.
  ///
  /// In en, this message translates to:
  /// **'Uses session timing only; awake time may be missing.'**
  String get sleepEfficiencyQualitySessionOnly;

  /// No description provided for @sleepEfficiencyReferenceDefinition.
  ///
  /// In en, this message translates to:
  /// **'Sleep efficiency definition'**
  String get sleepEfficiencyReferenceDefinition;

  /// No description provided for @sleepEfficiencyReferenceDenominator.
  ///
  /// In en, this message translates to:
  /// **'Sleep efficiency denominator research'**
  String get sleepEfficiencyReferenceDenominator;

  /// No description provided for @sleepEfficiencyReferenceMethods.
  ///
  /// In en, this message translates to:
  /// **'Sleep assessment methods review'**
  String get sleepEfficiencyReferenceMethods;

  /// No description provided for @cardioLoadConfidenceHigh.
  ///
  /// In en, this message translates to:
  /// **'High confidence'**
  String get cardioLoadConfidenceHigh;

  /// No description provided for @cardioLoadConfidenceMedium.
  ///
  /// In en, this message translates to:
  /// **'Medium confidence'**
  String get cardioLoadConfidenceMedium;

  /// No description provided for @cardioLoadConfidenceLow.
  ///
  /// In en, this message translates to:
  /// **'Low confidence'**
  String get cardioLoadConfidenceLow;

  /// No description provided for @cardioLoadConfidenceNoData.
  ///
  /// In en, this message translates to:
  /// **'No data'**
  String get cardioLoadConfidenceNoData;

  /// No description provided for @cardioLoadCalculationTitle.
  ///
  /// In en, this message translates to:
  /// **'How it is calculated'**
  String get cardioLoadCalculationTitle;

  /// No description provided for @cardioLoadDayNumbersTitle.
  ///
  /// In en, this message translates to:
  /// **'Today values'**
  String get cardioLoadDayNumbersTitle;

  /// No description provided for @cardioLoadReferencesTitle.
  ///
  /// In en, this message translates to:
  /// **'Backed links'**
  String get cardioLoadReferencesTitle;

  /// No description provided for @cardioLoadCalculationSummary.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals uses HR-based TRIMP when heart-rate data is available, then falls back to movement only when HR is not usable.'**
  String get cardioLoadCalculationSummary;

  /// No description provided for @cardioLoadFormula.
  ///
  /// In en, this message translates to:
  /// **'TRIMP = minutes x HRR x 0.64 x e^(1.92 x HRR)'**
  String get cardioLoadFormula;

  /// No description provided for @cardioLoadFormulaBody.
  ///
  /// In en, this message translates to:
  /// **'HRR is heart-rate reserve: (heart rate - resting heart rate) / (max heart rate - resting heart rate). OpenVitals sums this over available heart-rate intervals for the day.'**
  String get cardioLoadFormulaBody;

  /// No description provided for @cardioLoadMappingBody.
  ///
  /// In en, this message translates to:
  /// **'When recorded activities exist, heart-rate samples are mapped by timestamp into each activity start and end window. Without activity windows, only elevated heart-rate intervals are counted. If HR is not usable, movement and active calories are shown as a low-confidence fallback.'**
  String get cardioLoadMappingBody;

  /// No description provided for @cardioLoadMethod.
  ///
  /// In en, this message translates to:
  /// **'Method'**
  String get cardioLoadMethod;

  /// No description provided for @cardioLoadTrimpScore.
  ///
  /// In en, this message translates to:
  /// **'TRIMP score'**
  String get cardioLoadTrimpScore;

  /// No description provided for @cardioLoadHrCoverage.
  ///
  /// In en, this message translates to:
  /// **'HR coverage'**
  String get cardioLoadHrCoverage;

  /// No description provided for @cardioLoadExpectedCoverage.
  ///
  /// In en, this message translates to:
  /// **'Expected coverage'**
  String get cardioLoadExpectedCoverage;

  /// No description provided for @cardioLoadRestingHr.
  ///
  /// In en, this message translates to:
  /// **'Resting HR'**
  String get cardioLoadRestingHr;

  /// No description provided for @cardioLoadMaxHr.
  ///
  /// In en, this message translates to:
  /// **'Max HR'**
  String get cardioLoadMaxHr;

  /// No description provided for @cardioLoadHrSamples.
  ///
  /// In en, this message translates to:
  /// **'HR samples'**
  String get cardioLoadHrSamples;

  /// No description provided for @cardioLoadActivityWindows.
  ///
  /// In en, this message translates to:
  /// **'Activity windows'**
  String get cardioLoadActivityWindows;

  /// No description provided for @cardioLoadActivityMinutes.
  ///
  /// In en, this message translates to:
  /// **'Activity minutes'**
  String get cardioLoadActivityMinutes;

  /// No description provided for @cardioLoadMovementFallback.
  ///
  /// In en, this message translates to:
  /// **'Movement fallback'**
  String get cardioLoadMovementFallback;

  /// No description provided for @cardioLoadMethodActivityWindows.
  ///
  /// In en, this message translates to:
  /// **'TRIMP from activity HR'**
  String get cardioLoadMethodActivityWindows;

  /// No description provided for @cardioLoadMethodElevatedHr.
  ///
  /// In en, this message translates to:
  /// **'TRIMP from elevated HR'**
  String get cardioLoadMethodElevatedHr;

  /// No description provided for @cardioLoadMethodMovementFallback.
  ///
  /// In en, this message translates to:
  /// **'Movement fallback'**
  String get cardioLoadMethodMovementFallback;

  /// No description provided for @cardioLoadMethodNoData.
  ///
  /// In en, this message translates to:
  /// **'Insufficient data'**
  String get cardioLoadMethodNoData;

  /// No description provided for @cardioLoadCalibrationObservedResting.
  ///
  /// In en, this message translates to:
  /// **'Resting HR observed'**
  String get cardioLoadCalibrationObservedResting;

  /// No description provided for @cardioLoadCalibrationEstimatedResting.
  ///
  /// In en, this message translates to:
  /// **'Resting HR estimated'**
  String get cardioLoadCalibrationEstimatedResting;

  /// No description provided for @cardioLoadCalibrationObservedMax.
  ///
  /// In en, this message translates to:
  /// **'Max HR observed'**
  String get cardioLoadCalibrationObservedMax;

  /// No description provided for @cardioLoadCalibrationEstimatedMax.
  ///
  /// In en, this message translates to:
  /// **'Max HR estimated'**
  String get cardioLoadCalibrationEstimatedMax;

  /// No description provided for @cardioLoadReferenceBanister.
  ///
  /// In en, this message translates to:
  /// **'Banister TRIMP equation'**
  String get cardioLoadReferenceBanister;

  /// No description provided for @cardioLoadReferenceTrainingLoad.
  ///
  /// In en, this message translates to:
  /// **'Training-load monitoring review'**
  String get cardioLoadReferenceTrainingLoad;

  /// No description provided for @cardioLoadReferenceHealthConnect.
  ///
  /// In en, this message translates to:
  /// **'Health Connect workout HR mapping'**
  String get cardioLoadReferenceHealthConnect;

  /// No description provided for @sectionSleepSessions.
  ///
  /// In en, this message translates to:
  /// **'Sleep sessions'**
  String get sectionSleepSessions;

  /// No description provided for @sectionWeight.
  ///
  /// In en, this message translates to:
  /// **'Weight'**
  String get sectionWeight;

  /// No description provided for @sectionEntries.
  ///
  /// In en, this message translates to:
  /// **'Entries'**
  String get sectionEntries;

  /// No description provided for @sectionMeals.
  ///
  /// In en, this message translates to:
  /// **'Meals'**
  String get sectionMeals;

  /// No description provided for @sectionSessions.
  ///
  /// In en, this message translates to:
  /// **'Sessions'**
  String get sectionSessions;

  /// No description provided for @sectionDailyBreakdown.
  ///
  /// In en, this message translates to:
  /// **'Daily breakdown'**
  String get sectionDailyBreakdown;

  /// No description provided for @sectionVitals.
  ///
  /// In en, this message translates to:
  /// **'Vitals'**
  String get sectionVitals;

  /// No description provided for @sectionHeart.
  ///
  /// In en, this message translates to:
  /// **'Heart'**
  String get sectionHeart;

  /// No description provided for @sectionCardiovascular.
  ///
  /// In en, this message translates to:
  /// **'Cardiovascular'**
  String get sectionCardiovascular;

  /// No description provided for @sectionRespiratory.
  ///
  /// In en, this message translates to:
  /// **'Respiratory'**
  String get sectionRespiratory;

  /// No description provided for @sectionRespiratoryRateDailyBreakdown.
  ///
  /// In en, this message translates to:
  /// **'Respiratory rate daily breakdown'**
  String get sectionRespiratoryRateDailyBreakdown;

  /// No description provided for @sectionVo2MaxHistory.
  ///
  /// In en, this message translates to:
  /// **'VO2 max history'**
  String get sectionVo2MaxHistory;

  /// No description provided for @sectionDisplay.
  ///
  /// In en, this message translates to:
  /// **'Display'**
  String get sectionDisplay;

  /// No description provided for @sectionPrivacy.
  ///
  /// In en, this message translates to:
  /// **'Privacy'**
  String get sectionPrivacy;

  /// No description provided for @sectionCycleCalendar.
  ///
  /// In en, this message translates to:
  /// **'Cycle calendar'**
  String get sectionCycleCalendar;

  /// No description provided for @sectionBasalBodyTemperature.
  ///
  /// In en, this message translates to:
  /// **'Basal body temperature'**
  String get sectionBasalBodyTemperature;

  /// No description provided for @sectionStatistics.
  ///
  /// In en, this message translates to:
  /// **'Statistics'**
  String get sectionStatistics;

  /// No description provided for @sectionCalorieTrends.
  ///
  /// In en, this message translates to:
  /// **'Calorie trends'**
  String get sectionCalorieTrends;

  /// No description provided for @sectionNutritionTrends.
  ///
  /// In en, this message translates to:
  /// **'Nutrition trends'**
  String get sectionNutritionTrends;

  /// No description provided for @sectionBodyTrends.
  ///
  /// In en, this message translates to:
  /// **'Body trends'**
  String get sectionBodyTrends;

  /// No description provided for @sectionCarbohydrates.
  ///
  /// In en, this message translates to:
  /// **'Carbohydrates'**
  String get sectionCarbohydrates;

  /// No description provided for @sectionFats.
  ///
  /// In en, this message translates to:
  /// **'Fats'**
  String get sectionFats;

  /// No description provided for @sectionVitamins.
  ///
  /// In en, this message translates to:
  /// **'Vitamins'**
  String get sectionVitamins;

  /// No description provided for @sectionMinerals.
  ///
  /// In en, this message translates to:
  /// **'Minerals'**
  String get sectionMinerals;

  /// No description provided for @sectionOtherNutrients.
  ///
  /// In en, this message translates to:
  /// **'Other nutrients'**
  String get sectionOtherNutrients;

  /// No description provided for @summaryDailyAverage.
  ///
  /// In en, this message translates to:
  /// **'{arg0} daily average'**
  String summaryDailyAverage(String arg0);

  /// No description provided for @summaryDaysInRange.
  ///
  /// In en, this message translates to:
  /// **'{arg0} days in range'**
  String summaryDaysInRange(String arg0);

  /// No description provided for @summaryEntries.
  ///
  /// In en, this message translates to:
  /// **'{arg0} entries'**
  String summaryEntries(String arg0);

  /// No description provided for @summaryReadings.
  ///
  /// In en, this message translates to:
  /// **'{arg0} readings'**
  String summaryReadings(String arg0);

  /// No description provided for @summaryNights.
  ///
  /// In en, this message translates to:
  /// **'{arg0} nights'**
  String summaryNights(String arg0);

  /// No description provided for @summaryRecordedStages.
  ///
  /// In en, this message translates to:
  /// **'{arg0} recorded stages'**
  String summaryRecordedStages(String arg0);

  /// No description provided for @summaryAverage.
  ///
  /// In en, this message translates to:
  /// **'Avg'**
  String get summaryAverage;

  /// No description provided for @summaryAvgValue.
  ///
  /// In en, this message translates to:
  /// **'Avg {arg0}'**
  String summaryAvgValue(String arg0);

  /// No description provided for @summaryAvgValueRange.
  ///
  /// In en, this message translates to:
  /// **'Avg {arg0} · range {arg1}-{arg2}'**
  String summaryAvgValueRange(String arg0, String arg1, String arg2);

  /// No description provided for @summaryValueAvg.
  ///
  /// In en, this message translates to:
  /// **'{arg0} avg'**
  String summaryValueAvg(String arg0);

  /// No description provided for @summaryRange.
  ///
  /// In en, this message translates to:
  /// **'Range'**
  String get summaryRange;

  /// No description provided for @summarySamples.
  ///
  /// In en, this message translates to:
  /// **'Samples'**
  String get summarySamples;

  /// No description provided for @summaryRecorded.
  ///
  /// In en, this message translates to:
  /// **'{arg0}-{arg1} recorded'**
  String summaryRecorded(String arg0, String arg1);

  /// No description provided for @summaryRestingValue.
  ///
  /// In en, this message translates to:
  /// **'Resting {arg0}'**
  String summaryRestingValue(String arg0);

  /// No description provided for @summaryHrvValue.
  ///
  /// In en, this message translates to:
  /// **'HRV {arg0}'**
  String summaryHrvValue(String arg0);

  /// No description provided for @summaryLastUpdate.
  ///
  /// In en, this message translates to:
  /// **'Last update {arg0}'**
  String summaryLastUpdate(String arg0);

  /// No description provided for @summaryNow.
  ///
  /// In en, this message translates to:
  /// **'Now'**
  String get summaryNow;

  /// No description provided for @summaryToday.
  ///
  /// In en, this message translates to:
  /// **'{arg0} today'**
  String summaryToday(String arg0);

  /// No description provided for @summaryOnDate.
  ///
  /// In en, this message translates to:
  /// **'{arg0} on {arg1}'**
  String summaryOnDate(String arg0, String arg1);

  /// No description provided for @summaryEmptyToday.
  ///
  /// In en, this message translates to:
  /// **'{arg0} yet today.'**
  String summaryEmptyToday(String arg0);

  /// No description provided for @summaryEmptyDay.
  ///
  /// In en, this message translates to:
  /// **'{arg0} on this day.'**
  String summaryEmptyDay(String arg0);

  /// No description provided for @summaryAcrossSelectedPeriod.
  ///
  /// In en, this message translates to:
  /// **'Across selected period'**
  String get summaryAcrossSelectedPeriod;

  /// No description provided for @summaryLatestTemperature.
  ///
  /// In en, this message translates to:
  /// **'Latest {arg0} · {arg1}'**
  String summaryLatestTemperature(String arg0, String arg1);

  /// No description provided for @summaryTemperatureRange.
  ///
  /// In en, this message translates to:
  /// **'Range {arg0}-{arg1} · {arg2} readings'**
  String summaryTemperatureRange(String arg0, String arg1, String arg2);

  /// No description provided for @summarySleepEndingToday.
  ///
  /// In en, this message translates to:
  /// **'Sleep ending today'**
  String get summarySleepEndingToday;

  /// No description provided for @summarySleepEndingOn.
  ///
  /// In en, this message translates to:
  /// **'Sleep ending on {arg0}'**
  String summarySleepEndingOn(String arg0);

  /// No description provided for @statTotal.
  ///
  /// In en, this message translates to:
  /// **'Total'**
  String get statTotal;

  /// No description provided for @statTime.
  ///
  /// In en, this message translates to:
  /// **'Time'**
  String get statTime;

  /// No description provided for @statActiveDays.
  ///
  /// In en, this message translates to:
  /// **'Active days'**
  String get statActiveDays;

  /// No description provided for @statAverage.
  ///
  /// In en, this message translates to:
  /// **'Average'**
  String get statAverage;

  /// No description provided for @statLowest.
  ///
  /// In en, this message translates to:
  /// **'Lowest'**
  String get statLowest;

  /// No description provided for @statHighest.
  ///
  /// In en, this message translates to:
  /// **'Highest'**
  String get statHighest;

  /// No description provided for @statReadings.
  ///
  /// In en, this message translates to:
  /// **'Readings'**
  String get statReadings;

  /// No description provided for @statDailyAverage.
  ///
  /// In en, this message translates to:
  /// **'Daily average'**
  String get statDailyAverage;

  /// No description provided for @caloriesStatActiveAverage.
  ///
  /// In en, this message translates to:
  /// **'Active average'**
  String get caloriesStatActiveAverage;

  /// No description provided for @caloriesStatBmrReadings.
  ///
  /// In en, this message translates to:
  /// **'BMR readings'**
  String get caloriesStatBmrReadings;

  /// No description provided for @statAverageDuration.
  ///
  /// In en, this message translates to:
  /// **'Average duration'**
  String get statAverageDuration;

  /// No description provided for @statTotalIntake.
  ///
  /// In en, this message translates to:
  /// **'Total intake'**
  String get statTotalIntake;

  /// No description provided for @statBestDay.
  ///
  /// In en, this message translates to:
  /// **'Best day'**
  String get statBestDay;

  /// No description provided for @statNightsLogged.
  ///
  /// In en, this message translates to:
  /// **'Nights logged'**
  String get statNightsLogged;

  /// No description provided for @statLongestSleep.
  ///
  /// In en, this message translates to:
  /// **'Longest sleep'**
  String get statLongestSleep;

  /// No description provided for @statLongestWorkout.
  ///
  /// In en, this message translates to:
  /// **'Longest workout'**
  String get statLongestWorkout;

  /// No description provided for @statAverageMovingPace.
  ///
  /// In en, this message translates to:
  /// **'Avg moving pace'**
  String get statAverageMovingPace;

  /// No description provided for @statFastestPace.
  ///
  /// In en, this message translates to:
  /// **'Fastest pace'**
  String get statFastestPace;

  /// No description provided for @statBestSpeed.
  ///
  /// In en, this message translates to:
  /// **'Best speed'**
  String get statBestSpeed;

  /// No description provided for @statLongestSession.
  ///
  /// In en, this message translates to:
  /// **'Longest session'**
  String get statLongestSession;

  /// No description provided for @statBbtReadings.
  ///
  /// In en, this message translates to:
  /// **'BBT readings'**
  String get statBbtReadings;

  /// No description provided for @statGoalStreak.
  ///
  /// In en, this message translates to:
  /// **'Goal streak'**
  String get statGoalStreak;

  /// No description provided for @statLongestGoalStreak.
  ///
  /// In en, this message translates to:
  /// **'Longest streak'**
  String get statLongestGoalStreak;

  /// No description provided for @statGoalsMet.
  ///
  /// In en, this message translates to:
  /// **'Goals met'**
  String get statGoalsMet;

  /// No description provided for @statSuccessRate.
  ///
  /// In en, this message translates to:
  /// **'Success rate'**
  String get statSuccessRate;

  /// No description provided for @statAverageGap.
  ///
  /// In en, this message translates to:
  /// **'Avg gap'**
  String get statAverageGap;

  /// No description provided for @statVsPreviousDay.
  ///
  /// In en, this message translates to:
  /// **'Vs previous day'**
  String get statVsPreviousDay;

  /// No description provided for @statVsPreviousWeek.
  ///
  /// In en, this message translates to:
  /// **'Vs previous week'**
  String get statVsPreviousWeek;

  /// No description provided for @statVsPreviousMonth.
  ///
  /// In en, this message translates to:
  /// **'Vs previous month'**
  String get statVsPreviousMonth;

  /// No description provided for @statVsPreviousYear.
  ///
  /// In en, this message translates to:
  /// **'Vs previous year'**
  String get statVsPreviousYear;

  /// No description provided for @statBaseline.
  ///
  /// In en, this message translates to:
  /// **'Baseline'**
  String get statBaseline;

  /// No description provided for @stat30DayBaseline.
  ///
  /// In en, this message translates to:
  /// **'30-day avg'**
  String get stat30DayBaseline;

  /// No description provided for @stat60DayBaseline.
  ///
  /// In en, this message translates to:
  /// **'60-day avg'**
  String get stat60DayBaseline;

  /// No description provided for @stat90DayBaseline.
  ///
  /// In en, this message translates to:
  /// **'90-day avg'**
  String get stat90DayBaseline;

  /// No description provided for @statUsualRange.
  ///
  /// In en, this message translates to:
  /// **'Usual range'**
  String get statUsualRange;

  /// No description provided for @statBaselineDeviation.
  ///
  /// In en, this message translates to:
  /// **'Baseline deviation'**
  String get statBaselineDeviation;

  /// No description provided for @baselineStatusUsual.
  ///
  /// In en, this message translates to:
  /// **'Usual'**
  String get baselineStatusUsual;

  /// No description provided for @baselineStatusAbove.
  ///
  /// In en, this message translates to:
  /// **'Above'**
  String get baselineStatusAbove;

  /// No description provided for @baselineStatusBelow.
  ///
  /// In en, this message translates to:
  /// **'Below'**
  String get baselineStatusBelow;

  /// No description provided for @baselineStatusUnusualHigh.
  ///
  /// In en, this message translates to:
  /// **'Unusual high'**
  String get baselineStatusUnusualHigh;

  /// No description provided for @baselineStatusUnusualLow.
  ///
  /// In en, this message translates to:
  /// **'Unusual low'**
  String get baselineStatusUnusualLow;

  /// No description provided for @sectionMetricContext.
  ///
  /// In en, this message translates to:
  /// **'Context'**
  String get sectionMetricContext;

  /// No description provided for @interpretationBpTitle.
  ///
  /// In en, this message translates to:
  /// **'Blood pressure category'**
  String get interpretationBpTitle;

  /// No description provided for @interpretationBpNormal.
  ///
  /// In en, this message translates to:
  /// **'Normal'**
  String get interpretationBpNormal;

  /// No description provided for @interpretationBpElevated.
  ///
  /// In en, this message translates to:
  /// **'Elevated'**
  String get interpretationBpElevated;

  /// No description provided for @interpretationBpStage1.
  ///
  /// In en, this message translates to:
  /// **'Stage 1 high blood pressure'**
  String get interpretationBpStage1;

  /// No description provided for @interpretationBpStage2.
  ///
  /// In en, this message translates to:
  /// **'Stage 2 high blood pressure'**
  String get interpretationBpStage2;

  /// No description provided for @interpretationBpSevere.
  ///
  /// In en, this message translates to:
  /// **'Severe range reference'**
  String get interpretationBpSevere;

  /// No description provided for @interpretationBpBody.
  ///
  /// In en, this message translates to:
  /// **'This reading falls in the {arg0} range. A single app reading is not a diagnosis.'**
  String interpretationBpBody(String arg0);

  /// No description provided for @interpretationBpSevereBody.
  ///
  /// In en, this message translates to:
  /// **'This reading is above the severe range reference. Recheck it; seek urgent care if symptoms are present or the reading stays very high.'**
  String get interpretationBpSevereBody;

  /// No description provided for @interpretationBpSource.
  ///
  /// In en, this message translates to:
  /// **'Source: American Heart Association adult blood pressure categories.'**
  String get interpretationBpSource;

  /// No description provided for @interpretationBmiTitle.
  ///
  /// In en, this message translates to:
  /// **'BMI category'**
  String get interpretationBmiTitle;

  /// No description provided for @interpretationBmiUnderweight.
  ///
  /// In en, this message translates to:
  /// **'Underweight'**
  String get interpretationBmiUnderweight;

  /// No description provided for @interpretationBmiHealthy.
  ///
  /// In en, this message translates to:
  /// **'Healthy weight'**
  String get interpretationBmiHealthy;

  /// No description provided for @interpretationBmiOverweight.
  ///
  /// In en, this message translates to:
  /// **'Overweight'**
  String get interpretationBmiOverweight;

  /// No description provided for @interpretationBmiObesity1.
  ///
  /// In en, this message translates to:
  /// **'Obesity class 1'**
  String get interpretationBmiObesity1;

  /// No description provided for @interpretationBmiObesity2.
  ///
  /// In en, this message translates to:
  /// **'Obesity class 2'**
  String get interpretationBmiObesity2;

  /// No description provided for @interpretationBmiObesity3.
  ///
  /// In en, this message translates to:
  /// **'Obesity class 3'**
  String get interpretationBmiObesity3;

  /// No description provided for @interpretationBmiBody.
  ///
  /// In en, this message translates to:
  /// **'Adult BMI screening category only; BMI does not measure body composition.'**
  String get interpretationBmiBody;

  /// No description provided for @interpretationBmiSource.
  ///
  /// In en, this message translates to:
  /// **'Source: CDC adult BMI categories.'**
  String get interpretationBmiSource;

  /// No description provided for @interpretationFfmiTitle.
  ///
  /// In en, this message translates to:
  /// **'FFMI category'**
  String get interpretationFfmiTitle;

  /// No description provided for @interpretationFfmiBelowAverage.
  ///
  /// In en, this message translates to:
  /// **'Below average'**
  String get interpretationFfmiBelowAverage;

  /// No description provided for @interpretationFfmiAverage.
  ///
  /// In en, this message translates to:
  /// **'Average'**
  String get interpretationFfmiAverage;

  /// No description provided for @interpretationFfmiAboveAverage.
  ///
  /// In en, this message translates to:
  /// **'Above average'**
  String get interpretationFfmiAboveAverage;

  /// No description provided for @interpretationFfmiExcellent.
  ///
  /// In en, this message translates to:
  /// **'Excellent'**
  String get interpretationFfmiExcellent;

  /// No description provided for @interpretationFfmiSuperior.
  ///
  /// In en, this message translates to:
  /// **'Superior'**
  String get interpretationFfmiSuperior;

  /// No description provided for @interpretationFfmiExceptional.
  ///
  /// In en, this message translates to:
  /// **'Exceptional'**
  String get interpretationFfmiExceptional;

  /// No description provided for @interpretationFfmiElite.
  ///
  /// In en, this message translates to:
  /// **'Elite'**
  String get interpretationFfmiElite;

  /// No description provided for @interpretationFfmiBody.
  ///
  /// In en, this message translates to:
  /// **'FFMI {arg0}; adjusted FFMI {arg1}. Uses your latest weight, body fat, and height.'**
  String interpretationFfmiBody(String arg0, String arg1);

  /// No description provided for @interpretationFfmiSource.
  ///
  /// In en, this message translates to:
  /// **'Source: ffmicalculators.com indicative adjusted FFMI categories.'**
  String get interpretationFfmiSource;

  /// No description provided for @interpretationSleepTitle.
  ///
  /// In en, this message translates to:
  /// **'Sleep target'**
  String get interpretationSleepTitle;

  /// No description provided for @interpretationSleepBelow.
  ///
  /// In en, this message translates to:
  /// **'Below target'**
  String get interpretationSleepBelow;

  /// No description provided for @interpretationSleepNear.
  ///
  /// In en, this message translates to:
  /// **'Near target'**
  String get interpretationSleepNear;

  /// No description provided for @interpretationSleepMet.
  ///
  /// In en, this message translates to:
  /// **'Target met'**
  String get interpretationSleepMet;

  /// No description provided for @interpretationSleepBelowBody.
  ///
  /// In en, this message translates to:
  /// **'Average sleep is {arg0} below your configured target.'**
  String interpretationSleepBelowBody(String arg0);

  /// No description provided for @interpretationSleepNearBody.
  ///
  /// In en, this message translates to:
  /// **'Average sleep is close to your configured target: {arg0} vs {arg1}.'**
  String interpretationSleepNearBody(String arg0, String arg1);

  /// No description provided for @interpretationSleepMetBody.
  ///
  /// In en, this message translates to:
  /// **'Average sleep meets your configured target: {arg0} vs {arg1}.'**
  String interpretationSleepMetBody(String arg0, String arg1);

  /// No description provided for @interpretationSleepSource.
  ///
  /// In en, this message translates to:
  /// **'Based on your configured sleep target, not a medical sleep assessment.'**
  String get interpretationSleepSource;

  /// No description provided for @interpretationMacroTitle.
  ///
  /// In en, this message translates to:
  /// **'Macro split'**
  String get interpretationMacroTitle;

  /// No description provided for @interpretationMacroWithin.
  ///
  /// In en, this message translates to:
  /// **'Within reference split'**
  String get interpretationMacroWithin;

  /// No description provided for @interpretationMacroOutside.
  ///
  /// In en, this message translates to:
  /// **'Outside reference split'**
  String get interpretationMacroOutside;

  /// No description provided for @interpretationMacroBody.
  ///
  /// In en, this message translates to:
  /// **'Protein {arg0}, carbs {arg1}, fat {arg2} of logged macro calories.'**
  String interpretationMacroBody(String arg0, String arg1, String arg2);

  /// No description provided for @interpretationMacroSource.
  ///
  /// In en, this message translates to:
  /// **'Source: National Academies AMDR adult reference; logged macros only.'**
  String get interpretationMacroSource;

  /// No description provided for @interpretationWorkoutTitle.
  ///
  /// In en, this message translates to:
  /// **'Workout guideline progress'**
  String get interpretationWorkoutTitle;

  /// No description provided for @interpretationWorkoutNone.
  ///
  /// In en, this message translates to:
  /// **'No logged minutes'**
  String get interpretationWorkoutNone;

  /// No description provided for @interpretationWorkoutBelow.
  ///
  /// In en, this message translates to:
  /// **'Below weekly reference'**
  String get interpretationWorkoutBelow;

  /// No description provided for @interpretationWorkoutApproaching.
  ///
  /// In en, this message translates to:
  /// **'Approaching weekly reference'**
  String get interpretationWorkoutApproaching;

  /// No description provided for @interpretationWorkoutMet.
  ///
  /// In en, this message translates to:
  /// **'Weekly reference met'**
  String get interpretationWorkoutMet;

  /// No description provided for @interpretationWorkoutBody.
  ///
  /// In en, this message translates to:
  /// **'Logged {arg0} toward the 150 min/week adult reference ({arg1}). Intensity is not verified.'**
  String interpretationWorkoutBody(String arg0, String arg1);

  /// No description provided for @interpretationWorkoutBodyWeeklyAverage.
  ///
  /// In en, this message translates to:
  /// **'Weekly average {arg0} toward the 150 min/week adult reference ({arg1}). Intensity is not verified.'**
  String interpretationWorkoutBodyWeeklyAverage(String arg0, String arg1);

  /// No description provided for @interpretationWorkoutSource.
  ///
  /// In en, this message translates to:
  /// **'Source: HHS adult physical activity guideline reference.'**
  String get interpretationWorkoutSource;

  /// No description provided for @interpretationVitalTitle.
  ///
  /// In en, this message translates to:
  /// **'Vital context'**
  String get interpretationVitalTitle;

  /// No description provided for @interpretationVitalWithin.
  ///
  /// In en, this message translates to:
  /// **'Within broad adult reference'**
  String get interpretationVitalWithin;

  /// No description provided for @interpretationVitalBelow.
  ///
  /// In en, this message translates to:
  /// **'Below broad adult reference'**
  String get interpretationVitalBelow;

  /// No description provided for @interpretationVitalAbove.
  ///
  /// In en, this message translates to:
  /// **'Above broad adult reference'**
  String get interpretationVitalAbove;

  /// No description provided for @interpretationVitalOxygenBelowTypical.
  ///
  /// In en, this message translates to:
  /// **'Below typical oxygen range'**
  String get interpretationVitalOxygenBelowTypical;

  /// No description provided for @interpretationVitalOxygenLow.
  ///
  /// In en, this message translates to:
  /// **'Low oxygen reference'**
  String get interpretationVitalOxygenLow;

  /// No description provided for @interpretationVitalOxygenVeryLow.
  ///
  /// In en, this message translates to:
  /// **'Very low oxygen reference'**
  String get interpretationVitalOxygenVeryLow;

  /// No description provided for @interpretationVitalRestingHrBody.
  ///
  /// In en, this message translates to:
  /// **'Broad adult reference only; fitness, medication, stress, illness, and timing can change what is usual for you.'**
  String get interpretationVitalRestingHrBody;

  /// No description provided for @interpretationVitalRespiratoryBody.
  ///
  /// In en, this message translates to:
  /// **'Broad adult reference only; activity, anxiety, illness, and measurement timing can affect respiratory rate.'**
  String get interpretationVitalRespiratoryBody;

  /// No description provided for @interpretationVitalTemperatureBody.
  ///
  /// In en, this message translates to:
  /// **'Temperature varies by measurement site and time of day; use this as context only.'**
  String get interpretationVitalTemperatureBody;

  /// No description provided for @interpretationVitalOxygenBody.
  ///
  /// In en, this message translates to:
  /// **'Pulse oximeter readings can be affected by device, skin, circulation, motion, and conditions.'**
  String get interpretationVitalOxygenBody;

  /// No description provided for @interpretationVitalSource.
  ///
  /// In en, this message translates to:
  /// **'Source: MedlinePlus adult vital sign reference.'**
  String get interpretationVitalSource;

  /// No description provided for @interpretationOxygenSource.
  ///
  /// In en, this message translates to:
  /// **'Source: MedlinePlus and FDA pulse oximeter context.'**
  String get interpretationOxygenSource;

  /// No description provided for @sectionCrossMetricInsights.
  ///
  /// In en, this message translates to:
  /// **'Cross-metric insights'**
  String get sectionCrossMetricInsights;

  /// No description provided for @crossMetricPositiveLink.
  ///
  /// In en, this message translates to:
  /// **'Positive link'**
  String get crossMetricPositiveLink;

  /// No description provided for @crossMetricNegativeLink.
  ///
  /// In en, this message translates to:
  /// **'Negative link'**
  String get crossMetricNegativeLink;

  /// No description provided for @crossMetricWeakLink.
  ///
  /// In en, this message translates to:
  /// **'Weak link'**
  String get crossMetricWeakLink;

  /// No description provided for @crossMetricCorrelation.
  ///
  /// In en, this message translates to:
  /// **'{arg0}'**
  String crossMetricCorrelation(String arg0);

  /// No description provided for @crossMetricPairedDays.
  ///
  /// In en, this message translates to:
  /// **'{arg0} paired days'**
  String crossMetricPairedDays(int arg0);

  /// No description provided for @crossSleepHrvTitle.
  ///
  /// In en, this message translates to:
  /// **'Sleep vs HRV'**
  String get crossSleepHrvTitle;

  /// No description provided for @crossSleepHrvPositive.
  ///
  /// In en, this message translates to:
  /// **'More sleep tends to align with higher HRV in this period.'**
  String get crossSleepHrvPositive;

  /// No description provided for @crossSleepHrvNegative.
  ///
  /// In en, this message translates to:
  /// **'More sleep tends to align with lower HRV in this period.'**
  String get crossSleepHrvNegative;

  /// No description provided for @crossSleepHrvNeutral.
  ///
  /// In en, this message translates to:
  /// **'Sleep and HRV do not show a clear pattern in this period.'**
  String get crossSleepHrvNeutral;

  /// No description provided for @crossWorkoutRestingHrTitle.
  ///
  /// In en, this message translates to:
  /// **'Workouts vs resting heart rate'**
  String get crossWorkoutRestingHrTitle;

  /// No description provided for @crossWorkoutRestingHrPositive.
  ///
  /// In en, this message translates to:
  /// **'More workout minutes tend to align with higher resting heart rate in this period.'**
  String get crossWorkoutRestingHrPositive;

  /// No description provided for @crossWorkoutRestingHrNegative.
  ///
  /// In en, this message translates to:
  /// **'More workout minutes tend to align with lower resting heart rate in this period.'**
  String get crossWorkoutRestingHrNegative;

  /// No description provided for @crossWorkoutRestingHrNeutral.
  ///
  /// In en, this message translates to:
  /// **'Workout minutes and resting heart rate do not show a clear pattern in this period.'**
  String get crossWorkoutRestingHrNeutral;

  /// No description provided for @crossHydrationWeightTitle.
  ///
  /// In en, this message translates to:
  /// **'Hydration vs weight fluctuation'**
  String get crossHydrationWeightTitle;

  /// No description provided for @crossHydrationWeightPositive.
  ///
  /// In en, this message translates to:
  /// **'More hydration tends to align with larger weight swings in this period.'**
  String get crossHydrationWeightPositive;

  /// No description provided for @crossHydrationWeightNegative.
  ///
  /// In en, this message translates to:
  /// **'More hydration tends to align with smaller weight swings in this period.'**
  String get crossHydrationWeightNegative;

  /// No description provided for @crossHydrationWeightNeutral.
  ///
  /// In en, this message translates to:
  /// **'Hydration and weight fluctuation do not show a clear pattern in this period.'**
  String get crossHydrationWeightNeutral;

  /// No description provided for @crossMindfulnessSleepTitle.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness vs sleep'**
  String get crossMindfulnessSleepTitle;

  /// No description provided for @crossMindfulnessSleepPositive.
  ///
  /// In en, this message translates to:
  /// **'More mindfulness minutes tend to align with longer sleep in this period.'**
  String get crossMindfulnessSleepPositive;

  /// No description provided for @crossMindfulnessSleepNegative.
  ///
  /// In en, this message translates to:
  /// **'More mindfulness minutes tend to align with shorter sleep in this period.'**
  String get crossMindfulnessSleepNegative;

  /// No description provided for @crossMindfulnessSleepNeutral.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness and sleep do not show a clear pattern in this period.'**
  String get crossMindfulnessSleepNeutral;

  /// No description provided for @legendLess.
  ///
  /// In en, this message translates to:
  /// **'Less'**
  String get legendLess;

  /// No description provided for @legendMore.
  ///
  /// In en, this message translates to:
  /// **'More'**
  String get legendMore;

  /// No description provided for @dailyGoal.
  ///
  /// In en, this message translates to:
  /// **'Daily goal'**
  String get dailyGoal;

  /// No description provided for @goalProgress.
  ///
  /// In en, this message translates to:
  /// **'{arg0} of {arg1} tracked days met'**
  String goalProgress(int arg0, int arg1);

  /// No description provided for @cdDecreaseDailyGoal.
  ///
  /// In en, this message translates to:
  /// **'Decrease daily goal'**
  String get cdDecreaseDailyGoal;

  /// No description provided for @cdIncreaseDailyGoal.
  ///
  /// In en, this message translates to:
  /// **'Increase daily goal'**
  String get cdIncreaseDailyGoal;

  /// No description provided for @hydrationDailyGoal.
  ///
  /// In en, this message translates to:
  /// **'Daily goal'**
  String get hydrationDailyGoal;

  /// No description provided for @hydrationGoalProgress.
  ///
  /// In en, this message translates to:
  /// **'{arg0} of {arg1} tracked days met'**
  String hydrationGoalProgress(int arg0, int arg1);

  /// No description provided for @hydrationRemindersTitle.
  ///
  /// In en, this message translates to:
  /// **'Beverage reminders'**
  String get hydrationRemindersTitle;

  /// No description provided for @hydrationRemindersSummaryOff.
  ///
  /// In en, this message translates to:
  /// **'Off by default. Enable reminders during active hours until today\'s hydration goal is met.'**
  String get hydrationRemindersSummaryOff;

  /// No description provided for @hydrationRemindersSummaryOn.
  ///
  /// In en, this message translates to:
  /// **'Every {arg0} min • {arg1}-{arg2}'**
  String hydrationRemindersSummaryOn(int arg0, String arg1, String arg2);

  /// No description provided for @hydrationRemindersPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'Grant notification permission to enable beverage reminders.'**
  String get hydrationRemindersPermissionNeeded;

  /// No description provided for @hydrationRemindersInterval.
  ///
  /// In en, this message translates to:
  /// **'Reminder interval'**
  String get hydrationRemindersInterval;

  /// No description provided for @hydrationRemindersIntervalValue.
  ///
  /// In en, this message translates to:
  /// **'Every {arg0} min'**
  String hydrationRemindersIntervalValue(int arg0);

  /// No description provided for @hydrationRemindersActiveStart.
  ///
  /// In en, this message translates to:
  /// **'Active from'**
  String get hydrationRemindersActiveStart;

  /// No description provided for @hydrationRemindersActiveEnd.
  ///
  /// In en, this message translates to:
  /// **'Active until'**
  String get hydrationRemindersActiveEnd;

  /// No description provided for @hydrationRemindersGoalNote.
  ///
  /// In en, this message translates to:
  /// **'Reminders pause after today\'s goal is met and resume tomorrow.'**
  String get hydrationRemindersGoalNote;

  /// No description provided for @hydrationReminderNotificationChannel.
  ///
  /// In en, this message translates to:
  /// **'Beverage reminders'**
  String get hydrationReminderNotificationChannel;

  /// No description provided for @hydrationReminderNotificationChannelDesc.
  ///
  /// In en, this message translates to:
  /// **'Optional reminders to log beverages during active hours.'**
  String get hydrationReminderNotificationChannelDesc;

  /// No description provided for @hydrationReminderNotificationTitle.
  ///
  /// In en, this message translates to:
  /// **'Beverage reminder'**
  String get hydrationReminderNotificationTitle;

  /// No description provided for @hydrationReminderNotificationBody.
  ///
  /// In en, this message translates to:
  /// **'You\'re at {arg0} of {arg1} today. Add a drink when you can.'**
  String hydrationReminderNotificationBody(String arg0, String arg1);

  /// No description provided for @hydrationReminderNotificationProgress.
  ///
  /// In en, this message translates to:
  /// **'{arg0} / {arg1}'**
  String hydrationReminderNotificationProgress(String arg0, String arg1);

  /// No description provided for @hydrationTrackerTitle.
  ///
  /// In en, this message translates to:
  /// **'Log beverage'**
  String get hydrationTrackerTitle;

  /// No description provided for @hydrationTrackerSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Saved directly to Health Connect'**
  String get hydrationTrackerSubtitle;

  /// No description provided for @hydrationTrackerPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'For the summary, OpenVitals only asks for view permissions. To add this manual entry, we need write permission. OpenVitals will not store this data; entries are saved in Health Connect.'**
  String get hydrationTrackerPermissionNeeded;

  /// No description provided for @hydrationNutritionPermissionNeeded.
  ///
  /// In en, this message translates to:
  /// **'Grant nutrition write permission to save drink nutrients in Health Connect.'**
  String get hydrationNutritionPermissionNeeded;

  /// No description provided for @hydrationCustomDrinksTitle.
  ///
  /// In en, this message translates to:
  /// **'Saved drinks'**
  String get hydrationCustomDrinksTitle;

  /// No description provided for @hydrationCatalogDrinksTitle.
  ///
  /// In en, this message translates to:
  /// **'Drink catalog'**
  String get hydrationCatalogDrinksTitle;

  /// No description provided for @hydrationCatalogSearch.
  ///
  /// In en, this message translates to:
  /// **'Search drinks'**
  String get hydrationCatalogSearch;

  /// No description provided for @hydrationCatalogFrequentlyConsumed.
  ///
  /// In en, this message translates to:
  /// **'Frequently consumed'**
  String get hydrationCatalogFrequentlyConsumed;

  /// No description provided for @hydrationCatalogSavedOutside.
  ///
  /// In en, this message translates to:
  /// **'Saved drinks'**
  String get hydrationCatalogSavedOutside;

  /// No description provided for @hydrationCatalogSectionWater.
  ///
  /// In en, this message translates to:
  /// **'Water'**
  String get hydrationCatalogSectionWater;

  /// No description provided for @hydrationCatalogSectionCoffees.
  ///
  /// In en, this message translates to:
  /// **'Coffees'**
  String get hydrationCatalogSectionCoffees;

  /// No description provided for @hydrationCatalogSectionEnergyDrinks.
  ///
  /// In en, this message translates to:
  /// **'Energy drinks'**
  String get hydrationCatalogSectionEnergyDrinks;

  /// No description provided for @hydrationCatalogSectionTeas.
  ///
  /// In en, this message translates to:
  /// **'Teas'**
  String get hydrationCatalogSectionTeas;

  /// No description provided for @hydrationCatalogSectionChocolateDrinks.
  ///
  /// In en, this message translates to:
  /// **'Chocolate drinks'**
  String get hydrationCatalogSectionChocolateDrinks;

  /// No description provided for @hydrationCatalogSectionCarbonatedSoftDrinks.
  ///
  /// In en, this message translates to:
  /// **'Carbonated soft drinks'**
  String get hydrationCatalogSectionCarbonatedSoftDrinks;

  /// No description provided for @hydrationCatalogSectionOtherDrinks.
  ///
  /// In en, this message translates to:
  /// **'Other drinks'**
  String get hydrationCatalogSectionOtherDrinks;

  /// No description provided for @hydrationCatalogSectionCount.
  ///
  /// In en, this message translates to:
  /// **'{arg0} drinks'**
  String hydrationCatalogSectionCount(int arg0);

  /// No description provided for @hydrationNewDrinkAction.
  ///
  /// In en, this message translates to:
  /// **'New drink'**
  String get hydrationNewDrinkAction;

  /// No description provided for @hydrationNewDrinkTitle.
  ///
  /// In en, this message translates to:
  /// **'New drink'**
  String get hydrationNewDrinkTitle;

  /// No description provided for @hydrationEditDrinkTitle.
  ///
  /// In en, this message translates to:
  /// **'Edit drink'**
  String get hydrationEditDrinkTitle;

  /// No description provided for @hydrationLogSavedDrinkTitle.
  ///
  /// In en, this message translates to:
  /// **'Log {arg0}'**
  String hydrationLogSavedDrinkTitle(String arg0);

  /// No description provided for @hydrationCustomDrinkName.
  ///
  /// In en, this message translates to:
  /// **'Name'**
  String get hydrationCustomDrinkName;

  /// No description provided for @hydrationCustomDrinkCategory.
  ///
  /// In en, this message translates to:
  /// **'Category'**
  String get hydrationCustomDrinkCategory;

  /// No description provided for @hydrationCustomDrinkNoCategory.
  ///
  /// In en, this message translates to:
  /// **'No category'**
  String get hydrationCustomDrinkNoCategory;

  /// No description provided for @hydrationCustomDrinkHydrationImpact.
  ///
  /// In en, this message translates to:
  /// **'Hydration impact'**
  String get hydrationCustomDrinkHydrationImpact;

  /// No description provided for @hydrationImpactCountsFully.
  ///
  /// In en, this message translates to:
  /// **'Counts fully'**
  String get hydrationImpactCountsFully;

  /// No description provided for @hydrationImpactCountsPartially.
  ///
  /// In en, this message translates to:
  /// **'Counts partially'**
  String get hydrationImpactCountsPartially;

  /// No description provided for @hydrationImpactDoesNotCount.
  ///
  /// In en, this message translates to:
  /// **'Does not count'**
  String get hydrationImpactDoesNotCount;

  /// No description provided for @hydrationImpactCountsFullyBody.
  ///
  /// In en, this message translates to:
  /// **'All drink volume counts toward hydration.'**
  String get hydrationImpactCountsFullyBody;

  /// No description provided for @hydrationImpactCountsPartiallyBody.
  ///
  /// In en, this message translates to:
  /// **'Use a percentage of this drink.'**
  String get hydrationImpactCountsPartiallyBody;

  /// No description provided for @hydrationImpactDoesNotCountBody.
  ///
  /// In en, this message translates to:
  /// **'Save it without adding hydration.'**
  String get hydrationImpactDoesNotCountBody;

  /// No description provided for @hydrationImpactPercentLabel.
  ///
  /// In en, this message translates to:
  /// **'Counts as hydration (%)'**
  String get hydrationImpactPercentLabel;

  /// No description provided for @hydrationImpactInvalidPercent.
  ///
  /// In en, this message translates to:
  /// **'Enter a percentage above 0 and below 100.'**
  String get hydrationImpactInvalidPercent;

  /// No description provided for @hydrationCustomDrinkNutrients.
  ///
  /// In en, this message translates to:
  /// **'Nutrients'**
  String get hydrationCustomDrinkNutrients;

  /// No description provided for @hydrationCustomDrinkAddNutrient.
  ///
  /// In en, this message translates to:
  /// **'Add nutrient'**
  String get hydrationCustomDrinkAddNutrient;

  /// No description provided for @hydrationCustomDrinkLiquidOnly.
  ///
  /// In en, this message translates to:
  /// **'Liquid only'**
  String get hydrationCustomDrinkLiquidOnly;

  /// No description provided for @hydrationCustomDrinkNutrientCount.
  ///
  /// In en, this message translates to:
  /// **'Nutrients: {arg0}'**
  String hydrationCustomDrinkNutrientCount(int arg0);

  /// No description provided for @hydrationSavedDrinkAmountNoHydration.
  ///
  /// In en, this message translates to:
  /// **'{arg0} • Does not count as hydration'**
  String hydrationSavedDrinkAmountNoHydration(String arg0);

  /// No description provided for @hydrationSavedDrinkAmountPartialHydration.
  ///
  /// In en, this message translates to:
  /// **'{arg0} • Counts {arg1}% as hydration'**
  String hydrationSavedDrinkAmountPartialHydration(String arg0, int arg1);

  /// No description provided for @hydrationNonHydratingDrinkSavedHint.
  ///
  /// In en, this message translates to:
  /// **'Saved as nutrition only. No hydration was added.'**
  String get hydrationNonHydratingDrinkSavedHint;

  /// No description provided for @hydrationEntryNutritionOnly.
  ///
  /// In en, this message translates to:
  /// **'Beverage'**
  String get hydrationEntryNutritionOnly;

  /// No description provided for @hydrationEntryNoHydration.
  ///
  /// In en, this message translates to:
  /// **'No hydration impact'**
  String get hydrationEntryNoHydration;

  /// No description provided for @hydrationCustomDrinkAmountGrams.
  ///
  /// In en, this message translates to:
  /// **'Amount (g)'**
  String get hydrationCustomDrinkAmountGrams;

  /// No description provided for @hydrationCustomDrinkAmountKcal.
  ///
  /// In en, this message translates to:
  /// **'Amount (kcal)'**
  String get hydrationCustomDrinkAmountKcal;

  /// No description provided for @hydrationCustomDrinkInvalid.
  ///
  /// In en, this message translates to:
  /// **'Enter a drink name, amount, and positive nutrient amounts.'**
  String get hydrationCustomDrinkInvalid;

  /// No description provided for @hydrationInvalidAmount.
  ///
  /// In en, this message translates to:
  /// **'Enter an amount greater than zero and no more than 100 L.'**
  String get hydrationInvalidAmount;

  /// No description provided for @hydrationDrinkAmountLabel.
  ///
  /// In en, this message translates to:
  /// **'Amount ({arg0})'**
  String hydrationDrinkAmountLabel(String arg0);

  /// No description provided for @hydrationDrinkInvalidAmountRange.
  ///
  /// In en, this message translates to:
  /// **'Enter an amount from {arg0} to {arg1}.'**
  String hydrationDrinkInvalidAmountRange(String arg0, String arg1);

  /// No description provided for @hydrationWriteFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not save hydration entry: {arg0}'**
  String hydrationWriteFailed(String arg0);

  /// No description provided for @cdDecreaseHydrationGoal.
  ///
  /// In en, this message translates to:
  /// **'Decrease hydration goal'**
  String get cdDecreaseHydrationGoal;

  /// No description provided for @cdIncreaseHydrationGoal.
  ///
  /// In en, this message translates to:
  /// **'Increase hydration goal'**
  String get cdIncreaseHydrationGoal;

  /// No description provided for @cdDecreaseHydrationReminderInterval.
  ///
  /// In en, this message translates to:
  /// **'Decrease hydration reminder interval'**
  String get cdDecreaseHydrationReminderInterval;

  /// No description provided for @cdIncreaseHydrationReminderInterval.
  ///
  /// In en, this message translates to:
  /// **'Increase hydration reminder interval'**
  String get cdIncreaseHydrationReminderInterval;

  /// No description provided for @unitPercentSymbol.
  ///
  /// In en, this message translates to:
  /// **'%'**
  String get unitPercentSymbol;

  /// No description provided for @messageNoDashboardData.
  ///
  /// In en, this message translates to:
  /// **'No summary data available.'**
  String get messageNoDashboardData;

  /// No description provided for @messageMissingPermissionsTitle.
  ///
  /// In en, this message translates to:
  /// **'Some permissions are missing'**
  String get messageMissingPermissionsTitle;

  /// No description provided for @messageMissingPermissionsBody.
  ///
  /// In en, this message translates to:
  /// **'Grant the missing permissions to see a complete summary.'**
  String get messageMissingPermissionsBody;

  /// No description provided for @messageHealthConnectRateLimited.
  ///
  /// In en, this message translates to:
  /// **'Health Connect rate limit reached. Please wait about {arg0} min and try again.'**
  String messageHealthConnectRateLimited(int arg0);

  /// No description provided for @messageNoWorkoutsDay.
  ///
  /// In en, this message translates to:
  /// **'No workouts recorded on this day.'**
  String get messageNoWorkoutsDay;

  /// No description provided for @messageNoSleepDay.
  ///
  /// In en, this message translates to:
  /// **'No sleep session ended on this day.'**
  String get messageNoSleepDay;

  /// No description provided for @messageNoBloodPressure.
  ///
  /// In en, this message translates to:
  /// **'No blood pressure reading.'**
  String get messageNoBloodPressure;

  /// No description provided for @messageNoOxygen.
  ///
  /// In en, this message translates to:
  /// **'No oxygen reading.'**
  String get messageNoOxygen;

  /// No description provided for @messageNoVo2Max.
  ///
  /// In en, this message translates to:
  /// **'No VO2 max reading.'**
  String get messageNoVo2Max;

  /// No description provided for @messageNoBloodGlucose.
  ///
  /// In en, this message translates to:
  /// **'No blood glucose reading.'**
  String get messageNoBloodGlucose;

  /// No description provided for @messageNoSkinTemperature.
  ///
  /// In en, this message translates to:
  /// **'No skin temperature reading.'**
  String get messageNoSkinTemperature;

  /// No description provided for @messageCycleBrowse.
  ///
  /// In en, this message translates to:
  /// **'View cycle calendar and readings.'**
  String get messageCycleBrowse;

  /// No description provided for @dashboardAddWidgets.
  ///
  /// In en, this message translates to:
  /// **'Add widgets'**
  String get dashboardAddWidgets;

  /// No description provided for @dashboardAllWidgetsAdded.
  ///
  /// In en, this message translates to:
  /// **'All widgets are already on the summary.'**
  String get dashboardAllWidgetsAdded;

  /// No description provided for @dashboardActionLog.
  ///
  /// In en, this message translates to:
  /// **'Log'**
  String get dashboardActionLog;

  /// No description provided for @dashboardActionStartWorkout.
  ///
  /// In en, this message translates to:
  /// **'Start workout'**
  String get dashboardActionStartWorkout;

  /// No description provided for @dashboardActivitiesToday.
  ///
  /// In en, this message translates to:
  /// **'Activities'**
  String get dashboardActivitiesToday;

  /// No description provided for @dashboardSensorStatusTitle.
  ///
  /// In en, this message translates to:
  /// **'Sensor battery'**
  String get dashboardSensorStatusTitle;

  /// No description provided for @dashboardSensorBatteryLowest.
  ///
  /// In en, this message translates to:
  /// **'{arg0}% lowest'**
  String dashboardSensorBatteryLowest(int arg0);

  /// No description provided for @dashboardSensorBatteryUnknown.
  ///
  /// In en, this message translates to:
  /// **'Battery pending'**
  String get dashboardSensorBatteryUnknown;

  /// No description provided for @dashboardSensorStatusActiveConnected.
  ///
  /// In en, this message translates to:
  /// **'{arg0} active • {arg1} connected'**
  String dashboardSensorStatusActiveConnected(int arg0, int arg1);

  /// No description provided for @dashboardSensorStatusAllDisabled.
  ///
  /// In en, this message translates to:
  /// **'All sensors disabled'**
  String get dashboardSensorStatusAllDisabled;

  /// No description provided for @dashboardDeleteActivityTitle.
  ///
  /// In en, this message translates to:
  /// **'Delete activity?'**
  String get dashboardDeleteActivityTitle;

  /// No description provided for @dashboardDeleteActivityMessage.
  ///
  /// In en, this message translates to:
  /// **'Delete this {arg0} activity from OpenVitals?'**
  String dashboardDeleteActivityMessage(String arg0);

  /// No description provided for @dashboardReadinessTitle.
  ///
  /// In en, this message translates to:
  /// **'Daily Readiness'**
  String get dashboardReadinessTitle;

  /// No description provided for @dashboardReadinessScore.
  ///
  /// In en, this message translates to:
  /// **'Readiness'**
  String get dashboardReadinessScore;

  /// No description provided for @dashboardReadinessBodyEnergy.
  ///
  /// In en, this message translates to:
  /// **'Body Energy'**
  String get dashboardReadinessBodyEnergy;

  /// No description provided for @dashboardReadinessTraining.
  ///
  /// In en, this message translates to:
  /// **'Training Readiness'**
  String get dashboardReadinessTraining;

  /// No description provided for @dashboardReadinessHrvStatus.
  ///
  /// In en, this message translates to:
  /// **'HRV Status'**
  String get dashboardReadinessHrvStatus;

  /// No description provided for @dashboardReadinessIntensityMinutes.
  ///
  /// In en, this message translates to:
  /// **'Intensity Minutes'**
  String get dashboardReadinessIntensityMinutes;

  /// No description provided for @dashboardReadinessStressLevel.
  ///
  /// In en, this message translates to:
  /// **'Stress Level'**
  String get dashboardReadinessStressLevel;

  /// No description provided for @dashboardReadinessRecommended.
  ///
  /// In en, this message translates to:
  /// **'Recommended'**
  String get dashboardReadinessRecommended;

  /// No description provided for @dashboardReadinessAvoid.
  ///
  /// In en, this message translates to:
  /// **'Avoid'**
  String get dashboardReadinessAvoid;

  /// No description provided for @dashboardReadinessAlternative.
  ///
  /// In en, this message translates to:
  /// **'Alternative'**
  String get dashboardReadinessAlternative;

  /// No description provided for @dashboardReadinessStrain.
  ///
  /// In en, this message translates to:
  /// **'Strain target'**
  String get dashboardReadinessStrain;

  /// No description provided for @dashboardReadinessGoal.
  ///
  /// In en, this message translates to:
  /// **'Adaptive goal'**
  String get dashboardReadinessGoal;

  /// No description provided for @dashboardReadinessRecoveryMode.
  ///
  /// In en, this message translates to:
  /// **'Recovery Mode'**
  String get dashboardReadinessRecoveryMode;

  /// No description provided for @dashboardReadinessRecoveryModeBody.
  ///
  /// In en, this message translates to:
  /// **'Activity goals are reduced so you can focus on rest.'**
  String get dashboardReadinessRecoveryModeBody;

  /// No description provided for @dashboardReadinessWhy.
  ///
  /// In en, this message translates to:
  /// **'Why this recommendation'**
  String get dashboardReadinessWhy;

  /// No description provided for @stressDetailsHowTracked.
  ///
  /// In en, this message translates to:
  /// **'How this is tracked'**
  String get stressDetailsHowTracked;

  /// No description provided for @stressDetailsHowTrackedBody.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals estimates physiological stress locally from HRV versus your baseline, resting heart rate versus your baseline, and average heart rate compared with resting heart rate. It is a strain estimate, not a diagnosis or a mental-stress detector.'**
  String get stressDetailsHowTrackedBody;

  /// No description provided for @stressDetailsScale.
  ///
  /// In en, this message translates to:
  /// **'Scale: 0-25 resting, 26-50 low, 51-75 medium, 76-100 high.'**
  String get stressDetailsScale;

  /// No description provided for @stressDetailsInputs.
  ///
  /// In en, this message translates to:
  /// **'Inputs used'**
  String get stressDetailsInputs;

  /// No description provided for @stressDetailsNoInputs.
  ///
  /// In en, this message translates to:
  /// **'No usable HRV or heart-rate baseline signals were available.'**
  String get stressDetailsNoInputs;

  /// No description provided for @stressDetailsDataCoverage.
  ///
  /// In en, this message translates to:
  /// **'Data coverage'**
  String get stressDetailsDataCoverage;

  /// No description provided for @stressDetailsNoDataCoverage.
  ///
  /// In en, this message translates to:
  /// **'No same-day HR or HRV sample coverage was available.'**
  String get stressDetailsNoDataCoverage;

  /// No description provided for @stressDetailsCaveats.
  ///
  /// In en, this message translates to:
  /// **'Caveats'**
  String get stressDetailsCaveats;

  /// No description provided for @stressDetailsRelaxationPrompt.
  ///
  /// In en, this message translates to:
  /// **'If this feels accurate, try a short breathing or mindfulness session and re-check after a quiet period.'**
  String get stressDetailsRelaxationPrompt;

  /// No description provided for @readinessDetailsHowCalculated.
  ///
  /// In en, this message translates to:
  /// **'How this is calculated'**
  String get readinessDetailsHowCalculated;

  /// No description provided for @readinessDetailsSignalsUsed.
  ///
  /// In en, this message translates to:
  /// **'Signals used'**
  String get readinessDetailsSignalsUsed;

  /// No description provided for @readinessDetailsGuidance.
  ///
  /// In en, this message translates to:
  /// **'What this means'**
  String get readinessDetailsGuidance;

  /// No description provided for @readinessDetailsCaveats.
  ///
  /// In en, this message translates to:
  /// **'Caveats'**
  String get readinessDetailsCaveats;

  /// No description provided for @readinessDetailsCaveatLocal.
  ///
  /// In en, this message translates to:
  /// **'This is a local rule-based estimate from the data currently available in OpenVitals.'**
  String get readinessDetailsCaveatLocal;

  /// No description provided for @readinessDetailsCaveatNotMedical.
  ///
  /// In en, this message translates to:
  /// **'It is not a diagnosis, medical advice, coaching, or injury prediction.'**
  String get readinessDetailsCaveatNotMedical;

  /// No description provided for @readinessDetailsCaveatMissingData.
  ///
  /// In en, this message translates to:
  /// **'Missing permissions, sparse samples, or missing baselines lower confidence.'**
  String get readinessDetailsCaveatMissingData;

  /// No description provided for @readinessDetailsScoreStrong.
  ///
  /// In en, this message translates to:
  /// **'Strong'**
  String get readinessDetailsScoreStrong;

  /// No description provided for @readinessDetailsScoreSteady.
  ///
  /// In en, this message translates to:
  /// **'Steady'**
  String get readinessDetailsScoreSteady;

  /// No description provided for @readinessDetailsScoreLimited.
  ///
  /// In en, this message translates to:
  /// **'Limited'**
  String get readinessDetailsScoreLimited;

  /// No description provided for @readinessDetailsScoreLow.
  ///
  /// In en, this message translates to:
  /// **'Low'**
  String get readinessDetailsScoreLow;

  /// No description provided for @readinessDetailsScoreNeedsMoreData.
  ///
  /// In en, this message translates to:
  /// **'Needs more data'**
  String get readinessDetailsScoreNeedsMoreData;

  /// No description provided for @bodyEnergyDetailsHowCalculatedBody.
  ///
  /// In en, this message translates to:
  /// **'Body Energy uses recovery-side signals: sleep score, HRV status, resting heart rate, physiological stress, temperature, hydration, nutrition, and mindfulness. It estimates how much recovery capacity is visible today.'**
  String get bodyEnergyDetailsHowCalculatedBody;

  /// No description provided for @bodyEnergyDetailsScale.
  ///
  /// In en, this message translates to:
  /// **'Scale: 80-100 strong, 60-79 steady, 40-59 limited, 0-39 low.'**
  String get bodyEnergyDetailsScale;

  /// No description provided for @bodyEnergyDetailsSummary.
  ///
  /// In en, this message translates to:
  /// **'A recovery-side score for how much energy your current body signals support today.'**
  String get bodyEnergyDetailsSummary;

  /// No description provided for @bodyEnergyDetailsNoSignals.
  ///
  /// In en, this message translates to:
  /// **'No usable recovery-side signals were available.'**
  String get bodyEnergyDetailsNoSignals;

  /// No description provided for @trainingReadinessDetailsHowCalculatedBody.
  ///
  /// In en, this message translates to:
  /// **'Training Readiness uses training-side signals: sleep, HRV status, resting heart rate, training load, intensity minutes, physiological stress, temperature, and activity context. It estimates whether harder training fits today.'**
  String get trainingReadinessDetailsHowCalculatedBody;

  /// No description provided for @trainingReadinessDetailsScale.
  ///
  /// In en, this message translates to:
  /// **'Scale: 80-100 ready for hard training, 60-79 controlled training, 40-59 light training, 0-39 rest-focused.'**
  String get trainingReadinessDetailsScale;

  /// No description provided for @trainingReadinessDetailsSummary.
  ///
  /// In en, this message translates to:
  /// **'A training-side score for how well current recovery and load signals support exercise intensity.'**
  String get trainingReadinessDetailsSummary;

  /// No description provided for @trainingReadinessDetailsNoSignals.
  ///
  /// In en, this message translates to:
  /// **'No usable training-side signals were available.'**
  String get trainingReadinessDetailsNoSignals;

  /// No description provided for @dashboardGoalOf.
  ///
  /// In en, this message translates to:
  /// **'of {arg0}'**
  String dashboardGoalOf(String arg0);

  /// No description provided for @caloriesEstimatedActiveBmr.
  ///
  /// In en, this message translates to:
  /// **'No total record, est. active + BMR'**
  String get caloriesEstimatedActiveBmr;

  /// No description provided for @caloriesEstimatedValue.
  ///
  /// In en, this message translates to:
  /// **'Est. {arg0}'**
  String caloriesEstimatedValue(String arg0);

  /// No description provided for @dashboardWeeklyCardioLoadProgress.
  ///
  /// In en, this message translates to:
  /// **'{arg0} of {arg1}'**
  String dashboardWeeklyCardioLoadProgress(int arg0, int arg1);

  /// No description provided for @dashboardCardioLoadPercentOnly.
  ///
  /// In en, this message translates to:
  /// **'{arg0}%'**
  String dashboardCardioLoadPercentOnly(int arg0);

  /// No description provided for @dashboardCardioLoadPercent.
  ///
  /// In en, this message translates to:
  /// **'{arg0}% load'**
  String dashboardCardioLoadPercent(int arg0);

  /// No description provided for @dashboardCardioLoadTodayDelta.
  ///
  /// In en, this message translates to:
  /// **'+{arg0}% today'**
  String dashboardCardioLoadTodayDelta(int arg0);

  /// No description provided for @messageNoActivitiesPeriod.
  ///
  /// In en, this message translates to:
  /// **'No activities in the selected period.'**
  String get messageNoActivitiesPeriod;

  /// No description provided for @plannedWorkoutCompleted.
  ///
  /// In en, this message translates to:
  /// **'Completed'**
  String get plannedWorkoutCompleted;

  /// No description provided for @plannedWorkoutBlocks.
  ///
  /// In en, this message translates to:
  /// **'{arg0} blocks'**
  String plannedWorkoutBlocks(int arg0);

  /// No description provided for @messageNoStepUpdates.
  ///
  /// In en, this message translates to:
  /// **'No step updates were recorded'**
  String get messageNoStepUpdates;

  /// No description provided for @messageNoDistanceUpdates.
  ///
  /// In en, this message translates to:
  /// **'No distance updates were recorded'**
  String get messageNoDistanceUpdates;

  /// No description provided for @messageNoCaloriesBurned.
  ///
  /// In en, this message translates to:
  /// **'No total calorie data was recorded'**
  String get messageNoCaloriesBurned;

  /// No description provided for @messageNoFloorsClimbed.
  ///
  /// In en, this message translates to:
  /// **'No floors climbed data was recorded'**
  String get messageNoFloorsClimbed;

  /// No description provided for @messageNoActiveCalories.
  ///
  /// In en, this message translates to:
  /// **'No active calories data was recorded'**
  String get messageNoActiveCalories;

  /// No description provided for @messageNoCalorieDataPeriod.
  ///
  /// In en, this message translates to:
  /// **'No total, active, or BMR calorie data in this period.'**
  String get messageNoCalorieDataPeriod;

  /// No description provided for @messageNoElevation.
  ///
  /// In en, this message translates to:
  /// **'No elevation data was recorded'**
  String get messageNoElevation;

  /// No description provided for @messageNoWheelchairPushes.
  ///
  /// In en, this message translates to:
  /// **'No wheelchair push data was recorded'**
  String get messageNoWheelchairPushes;

  /// No description provided for @messageNoSleepDaySelected.
  ///
  /// In en, this message translates to:
  /// **'No sleep data for the selected day.'**
  String get messageNoSleepDaySelected;

  /// No description provided for @messageNoSleepPeriod.
  ///
  /// In en, this message translates to:
  /// **'No sleep data in the selected period.'**
  String get messageNoSleepPeriod;

  /// No description provided for @messageNoHeartPeriod.
  ///
  /// In en, this message translates to:
  /// **'No heart rate data in the selected period.\n\nMake sure the heart rate permission is granted and a connected device has synced data.'**
  String get messageNoHeartPeriod;

  /// No description provided for @messageNoHeartSamplesDay.
  ///
  /// In en, this message translates to:
  /// **'No heart rate samples recorded on this day.'**
  String get messageNoHeartSamplesDay;

  /// No description provided for @messageHeartEmptyHint.
  ///
  /// In en, this message translates to:
  /// **'Try another date or check that a connected device synced point-in-time heart data.'**
  String get messageHeartEmptyHint;

  /// No description provided for @messageNoWeightPeriod.
  ///
  /// In en, this message translates to:
  /// **'No weight data in the selected period.\n\nSync a scale or wearable that reports weight to Health Connect.'**
  String get messageNoWeightPeriod;

  /// No description provided for @messageNoHydrationPeriod.
  ///
  /// In en, this message translates to:
  /// **'No beverage entries were recorded for this period.'**
  String get messageNoHydrationPeriod;

  /// No description provided for @messageNoHydrationAddedPeriod.
  ///
  /// In en, this message translates to:
  /// **'No hydration impact was added for this period.'**
  String get messageNoHydrationAddedPeriod;

  /// No description provided for @messageNoNutritionPeriod.
  ///
  /// In en, this message translates to:
  /// **'No nutrition entries were recorded for this period.'**
  String get messageNoNutritionPeriod;

  /// No description provided for @messageNoMindfulnessPeriod.
  ///
  /// In en, this message translates to:
  /// **'No mindfulness sessions were recorded for this period.'**
  String get messageNoMindfulnessPeriod;

  /// No description provided for @messageNoVitalsPeriod.
  ///
  /// In en, this message translates to:
  /// **'No vitals were recorded for this period.'**
  String get messageNoVitalsPeriod;

  /// No description provided for @messageNoReadingsPeriod.
  ///
  /// In en, this message translates to:
  /// **'No readings in this period.'**
  String get messageNoReadingsPeriod;

  /// No description provided for @messageNoCyclePeriod.
  ///
  /// In en, this message translates to:
  /// **'No cycle data was recorded for this period.'**
  String get messageNoCyclePeriod;

  /// No description provided for @messageNoSegments.
  ///
  /// In en, this message translates to:
  /// **'No segments recorded.'**
  String get messageNoSegments;

  /// No description provided for @messageNoLaps.
  ///
  /// In en, this message translates to:
  /// **'No laps recorded.'**
  String get messageNoLaps;

  /// No description provided for @messageNoRoutePoints.
  ///
  /// In en, this message translates to:
  /// **'No route points recorded.'**
  String get messageNoRoutePoints;

  /// No description provided for @messageRouteConsentRequired.
  ///
  /// In en, this message translates to:
  /// **'Route data is available, but route access has not been granted yet. Open Health Connect permissions from Settings to enable route previews.'**
  String get messageRouteConsentRequired;

  /// No description provided for @messageNoRouteData.
  ///
  /// In en, this message translates to:
  /// **'No route data recorded.'**
  String get messageNoRouteData;

  /// No description provided for @messageNoStages.
  ///
  /// In en, this message translates to:
  /// **'No stages recorded.'**
  String get messageNoStages;

  /// No description provided for @messageNoKcal.
  ///
  /// In en, this message translates to:
  /// **'No kcal'**
  String get messageNoKcal;

  /// No description provided for @onboardingTagline.
  ///
  /// In en, this message translates to:
  /// **'Your health data, on your device'**
  String get onboardingTagline;

  /// No description provided for @onboardingPrivacyTitle.
  ///
  /// In en, this message translates to:
  /// **'Privacy first'**
  String get onboardingPrivacyTitle;

  /// No description provided for @onboardingPrivacyBody.
  ///
  /// In en, this message translates to:
  /// **'No account required. Data stays on your device. No cloud upload, no analytics, no ads.'**
  String get onboardingPrivacyBody;

  /// No description provided for @healthDisclaimerTitle.
  ///
  /// In en, this message translates to:
  /// **'Health disclaimer'**
  String get healthDisclaimerTitle;

  /// No description provided for @healthDisclaimerBody.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals is for general wellness and informational use only. It is not a medical device and does not provide medical advice. It does not diagnose, treat, cure, or prevent any disease or medical condition. Always consult a qualified healthcare professional for medical advice, diagnosis, or treatment.'**
  String get healthDisclaimerBody;

  /// No description provided for @onboardingHealthConnectTitle.
  ///
  /// In en, this message translates to:
  /// **'Powered by Health Connect'**
  String get onboardingHealthConnectTitle;

  /// No description provided for @onboardingHealthConnectBody.
  ///
  /// In en, this message translates to:
  /// **'Reads from the secure on-device Android health store and saves entries you create back to Health Connect. Works with all data imported into Health Connect.'**
  String get onboardingHealthConnectBody;

  /// No description provided for @onboardingPermissionsHeader.
  ///
  /// In en, this message translates to:
  /// **'HEALTH CONNECT PERMISSIONS'**
  String get onboardingPermissionsHeader;

  /// No description provided for @onboardingGrantCore.
  ///
  /// In en, this message translates to:
  /// **'Grant required Health Connect permissions'**
  String get onboardingGrantCore;

  /// No description provided for @onboardingGrantAll.
  ///
  /// In en, this message translates to:
  /// **'Grant required Health Connect permissions'**
  String get onboardingGrantAll;

  /// No description provided for @onboardingGrantRemaining.
  ///
  /// In en, this message translates to:
  /// **'Grant remaining available permissions'**
  String get onboardingGrantRemaining;

  /// No description provided for @onboardingOpenRequiredPermissions.
  ///
  /// In en, this message translates to:
  /// **'Open required Health Connect permissions'**
  String get onboardingOpenRequiredPermissions;

  /// No description provided for @onboardingUnableOpenPermissions.
  ///
  /// In en, this message translates to:
  /// **'Unable to open Health Connect permissions.'**
  String get onboardingUnableOpenPermissions;

  /// No description provided for @onboardingHealthConnectNotSupported.
  ///
  /// In en, this message translates to:
  /// **'Health Connect is not supported on this device.'**
  String get onboardingHealthConnectNotSupported;

  /// No description provided for @onboardingHealthConnectNeedsPlayStore.
  ///
  /// In en, this message translates to:
  /// **'This device is running Android 13 with the standalone Health Connect app installed. Sadly, this version depends on Google Play Store services, which are missing on this device, so Health Connect rejects requests before OpenVitals can read your data. OpenVitals cannot fix or bypass this device-side Health Connect issue. The only way to solve it is to install Google Play services or upgrade to Android 14 or higher, where Health Connect is part of the operating system and does not need Google services.'**
  String get onboardingHealthConnectNeedsPlayStore;

  /// No description provided for @onboardingHealthConnectUpdate.
  ///
  /// In en, this message translates to:
  /// **'Health Connect needs to be installed or updated to use this app.'**
  String get onboardingHealthConnectUpdate;

  /// No description provided for @onboardingInstallHealthConnect.
  ///
  /// In en, this message translates to:
  /// **'Install Health Connect'**
  String get onboardingInstallHealthConnect;

  /// No description provided for @onboardingStatusNotSupported.
  ///
  /// In en, this message translates to:
  /// **'Not supported'**
  String get onboardingStatusNotSupported;

  /// No description provided for @onboardingStatusGranted.
  ///
  /// In en, this message translates to:
  /// **'Granted'**
  String get onboardingStatusGranted;

  /// No description provided for @onboardingStatusPartiallyGranted.
  ///
  /// In en, this message translates to:
  /// **'{arg0}/{arg1} granted'**
  String onboardingStatusPartiallyGranted(int arg0, int arg1);

  /// No description provided for @onboardingStatusManual.
  ///
  /// In en, this message translates to:
  /// **'Open settings'**
  String get onboardingStatusManual;

  /// No description provided for @onboardingStatusRequired.
  ///
  /// In en, this message translates to:
  /// **'Required'**
  String get onboardingStatusRequired;

  /// No description provided for @onboardingStatusOptional.
  ///
  /// In en, this message translates to:
  /// **'Optional'**
  String get onboardingStatusOptional;

  /// No description provided for @onboardingCategoryActivitySleep.
  ///
  /// In en, this message translates to:
  /// **'Activity & sleep'**
  String get onboardingCategoryActivitySleep;

  /// No description provided for @onboardingCategoryActivitySleepDesc.
  ///
  /// In en, this message translates to:
  /// **'Health Connect will ask for:\n* Steps\n* Distance\n* Exercise\n* Sleep'**
  String get onboardingCategoryActivitySleepDesc;

  /// No description provided for @onboardingCategoryHeartRecovery.
  ///
  /// In en, this message translates to:
  /// **'Heart & vitals'**
  String get onboardingCategoryHeartRecovery;

  /// No description provided for @onboardingCategoryHeartRecoveryDesc.
  ///
  /// In en, this message translates to:
  /// **'Health Connect will ask for:\n* Heart rate\n* Resting heart rate\n* Heart rate variability'**
  String get onboardingCategoryHeartRecoveryDesc;

  /// No description provided for @onboardingCategoryBody.
  ///
  /// In en, this message translates to:
  /// **'Body'**
  String get onboardingCategoryBody;

  /// No description provided for @onboardingCategoryBodyDesc.
  ///
  /// In en, this message translates to:
  /// **'Health Connect will ask for:\n* Weight\n* Height\n* Body fat\n* Lean body mass\n* Basal metabolic rate\n* Bone mass\n* Body water mass'**
  String get onboardingCategoryBodyDesc;

  /// No description provided for @onboardingCategoryActivityExtras.
  ///
  /// In en, this message translates to:
  /// **'Activity extras'**
  String get onboardingCategoryActivityExtras;

  /// No description provided for @onboardingCategoryActivityExtrasDesc.
  ///
  /// In en, this message translates to:
  /// **'Health Connect will ask for:\n* Total calories burned\n* Active calories burned\n* Floors climbed\n* Elevation gained\n* Wheelchair pushes\n* Speed\n* Power\n* Steps cadence\n* Cycling pedaling cadence\n* Planned exercise, when supported'**
  String get onboardingCategoryActivityExtrasDesc;

  /// No description provided for @onboardingCategoryNutritionHydration.
  ///
  /// In en, this message translates to:
  /// **'Nutrition & hydration'**
  String get onboardingCategoryNutritionHydration;

  /// No description provided for @onboardingCategoryNutritionHydrationDesc.
  ///
  /// In en, this message translates to:
  /// **'Health Connect will ask for:\n* Hydration\n* Nutrition'**
  String get onboardingCategoryNutritionHydrationDesc;

  /// No description provided for @onboardingCategoryManualEntryWrite.
  ///
  /// In en, this message translates to:
  /// **'Manual entry write access'**
  String get onboardingCategoryManualEntryWrite;

  /// No description provided for @onboardingCategoryManualEntryWriteDesc.
  ///
  /// In en, this message translates to:
  /// **'Health Connect will ask for write access to:\n* Exercise\n* Distance\n* Elevation gained\n* Active calories burned\n* Total calories burned\n* Exercise route\n* Hydration\n* Weight\n* Height\n* Body fat\n* Blood pressure\n* Oxygen saturation\n* Respiratory rate\n* Body temperature\n* Mindfulness, when supported'**
  String get onboardingCategoryManualEntryWriteDesc;

  /// No description provided for @onboardingCategoryDataImportWrite.
  ///
  /// In en, this message translates to:
  /// **'Data import write access'**
  String get onboardingCategoryDataImportWrite;

  /// No description provided for @onboardingCategoryDataImportWriteDesc.
  ///
  /// In en, this message translates to:
  /// **'Health Connect will ask for write access to imported records:\n* Activity, exercise, calories, and distance\n* Heart rate, resting heart rate, and heart rate variability\n* Body measurements\n* Hydration and nutrition\n* Sleep\n* Vitals\n* Mindfulness, when supported\n* Cycle tracking records'**
  String get onboardingCategoryDataImportWriteDesc;

  /// No description provided for @onboardingCategoryMindfulness.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness'**
  String get onboardingCategoryMindfulness;

  /// No description provided for @onboardingCategoryMindfulnessDesc.
  ///
  /// In en, this message translates to:
  /// **'Health Connect will ask for:\n* Mindfulness sessions'**
  String get onboardingCategoryMindfulnessDesc;

  /// No description provided for @onboardingCategoryMindfulnessUnavailable.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness sessions require a newer Health Connect version.'**
  String get onboardingCategoryMindfulnessUnavailable;

  /// No description provided for @onboardingCategoryAdditionalDataAccess.
  ///
  /// In en, this message translates to:
  /// **'Additional data access'**
  String get onboardingCategoryAdditionalDataAccess;

  /// No description provided for @onboardingCategoryAdditionalDataAccessDesc.
  ///
  /// In en, this message translates to:
  /// **'In Health Connect permissions, open OpenVitals > Additional access and set:\n* Access past data: Enable\n* Access data in the background: Enable\n* Access exercise routes: Always'**
  String get onboardingCategoryAdditionalDataAccessDesc;

  /// No description provided for @onboardingCategoryAdditionalDataAccessManualNote.
  ///
  /// In en, this message translates to:
  /// **'{arg0}\n\nIf Access exercise routes is missing from the access dialog, open Health Connect settings for OpenVitals and set it under Additional access.'**
  String onboardingCategoryAdditionalDataAccessManualNote(String arg0);

  /// No description provided for @onboardingCategoryVitals.
  ///
  /// In en, this message translates to:
  /// **'Vitals'**
  String get onboardingCategoryVitals;

  /// No description provided for @onboardingCategoryVitalsDesc.
  ///
  /// In en, this message translates to:
  /// **'Health Connect will ask for:\n* Blood pressure\n* Oxygen saturation\n* Respiratory rate\n* Body temperature\n* VO2 max\n* Blood glucose\n* Skin temperature, when supported'**
  String get onboardingCategoryVitalsDesc;

  /// No description provided for @onboardingCategoryCycleTracking.
  ///
  /// In en, this message translates to:
  /// **'Cycle tracking'**
  String get onboardingCategoryCycleTracking;

  /// No description provided for @onboardingCategoryCycleTrackingDesc.
  ///
  /// In en, this message translates to:
  /// **'Health Connect will ask for sensitive cycle data:\n* Menstruation flow\n* Menstruation periods\n* Ovulation tests\n* Cervical mucus\n* Basal body temperature\n* Intermenstrual bleeding\n* Sexual activity'**
  String get onboardingCategoryCycleTrackingDesc;

  /// No description provided for @settingsAllRequestableGranted.
  ///
  /// In en, this message translates to:
  /// **'All requestable permissions granted'**
  String get settingsAllRequestableGranted;

  /// No description provided for @settingsManualPermissionsTitle.
  ///
  /// In en, this message translates to:
  /// **'Manual permissions required'**
  String get settingsManualPermissionsTitle;

  /// No description provided for @settingsManualPermissionsBody.
  ///
  /// In en, this message translates to:
  /// **'Some Health Connect permissions cannot be granted from the normal request dialog. Open Health Connect and enable them for OpenVitals.'**
  String get settingsManualPermissionsBody;

  /// No description provided for @settingsOpenHealthPermissions.
  ///
  /// In en, this message translates to:
  /// **'Open Health Connect permissions'**
  String get settingsOpenHealthPermissions;

  /// No description provided for @settingsDisplayGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Display'**
  String get settingsDisplayGroupTitle;

  /// No description provided for @settingsDisplayGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Language, units, and theme'**
  String get settingsDisplayGroupBody;

  /// No description provided for @settingsActivitiesGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Activities'**
  String get settingsActivitiesGroupTitle;

  /// No description provided for @settingsActivitiesGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Rolling dates, favorite activity, recording, and offline maps'**
  String get settingsActivitiesGroupBody;

  /// No description provided for @settingsSensorsGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Sensors & devices'**
  String get settingsSensorsGroupTitle;

  /// No description provided for @settingsSensorsGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Heart rate, cadence, and power sensors'**
  String get settingsSensorsGroupBody;

  /// No description provided for @settingsSensorsEmptyTitle.
  ///
  /// In en, this message translates to:
  /// **'No sensors yet'**
  String get settingsSensorsEmptyTitle;

  /// No description provided for @settingsSensorsEmptyBody.
  ///
  /// In en, this message translates to:
  /// **'Add a Bluetooth heart rate strap, cadence sensor, power meter, or footpod to use during activity recording.'**
  String get settingsSensorsEmptyBody;

  /// No description provided for @settingsSensorsAddDevice.
  ///
  /// In en, this message translates to:
  /// **'Add sensor'**
  String get settingsSensorsAddDevice;

  /// No description provided for @settingsSensorsEditDevice.
  ///
  /// In en, this message translates to:
  /// **'Edit sensor'**
  String get settingsSensorsEditDevice;

  /// No description provided for @settingsSensorsRemoveDevice.
  ///
  /// In en, this message translates to:
  /// **'Remove sensor'**
  String get settingsSensorsRemoveDevice;

  /// No description provided for @settingsSensorsDeviceName.
  ///
  /// In en, this message translates to:
  /// **'Device name'**
  String get settingsSensorsDeviceName;

  /// No description provided for @settingsSensorsEnabled.
  ///
  /// In en, this message translates to:
  /// **'Enabled'**
  String get settingsSensorsEnabled;

  /// No description provided for @settingsSensorsBatteryPercent.
  ///
  /// In en, this message translates to:
  /// **'Battery {arg0}%'**
  String settingsSensorsBatteryPercent(int arg0);

  /// No description provided for @settingsSensorsBatteryUnknown.
  ///
  /// In en, this message translates to:
  /// **'Battery pending'**
  String get settingsSensorsBatteryUnknown;

  /// No description provided for @settingsSensorsScanning.
  ///
  /// In en, this message translates to:
  /// **'Scanning for nearby sensors…'**
  String get settingsSensorsScanning;

  /// No description provided for @settingsSensorsScanStopped.
  ///
  /// In en, this message translates to:
  /// **'Scan stopped'**
  String get settingsSensorsScanStopped;

  /// No description provided for @settingsSensorsScanEmpty.
  ///
  /// In en, this message translates to:
  /// **'No sensors found yet. Make sure your sensor is awake and close to your phone.'**
  String get settingsSensorsScanEmpty;

  /// No description provided for @settingsSensorsShowAllDevices.
  ///
  /// In en, this message translates to:
  /// **'Show all devices'**
  String get settingsSensorsShowAllDevices;

  /// No description provided for @settingsSensorsOpenBluetooth.
  ///
  /// In en, this message translates to:
  /// **'Open Bluetooth settings'**
  String get settingsSensorsOpenBluetooth;

  /// No description provided for @settingsSensorsDiscovering.
  ///
  /// In en, this message translates to:
  /// **'Discovering sensor capabilities…'**
  String get settingsSensorsDiscovering;

  /// No description provided for @settingsSensorsCapabilitiesTitle.
  ///
  /// In en, this message translates to:
  /// **'Capabilities'**
  String get settingsSensorsCapabilitiesTitle;

  /// No description provided for @settingsSensorsCapabilityHeartRate.
  ///
  /// In en, this message translates to:
  /// **'Heart rate'**
  String get settingsSensorsCapabilityHeartRate;

  /// No description provided for @settingsSensorsCapabilityCyclingCadence.
  ///
  /// In en, this message translates to:
  /// **'Cycling cadence'**
  String get settingsSensorsCapabilityCyclingCadence;

  /// No description provided for @settingsSensorsCapabilityCyclingPower.
  ///
  /// In en, this message translates to:
  /// **'Cycling power'**
  String get settingsSensorsCapabilityCyclingPower;

  /// No description provided for @settingsSensorsCapabilityCyclingSpeed.
  ///
  /// In en, this message translates to:
  /// **'Cycling speed'**
  String get settingsSensorsCapabilityCyclingSpeed;

  /// No description provided for @settingsSensorsCapabilityRunningSpeedCadence.
  ///
  /// In en, this message translates to:
  /// **'Running speed/cadence'**
  String get settingsSensorsCapabilityRunningSpeedCadence;

  /// No description provided for @settingsSensorsCapabilityConflict.
  ///
  /// In en, this message translates to:
  /// **'{arg0} is already assigned to {arg1}'**
  String settingsSensorsCapabilityConflict(String arg0, String arg1);

  /// No description provided for @settingsSensorsWheelCircumference.
  ///
  /// In en, this message translates to:
  /// **'Wheel circumference (mm)'**
  String get settingsSensorsWheelCircumference;

  /// No description provided for @activityRecordingSensorsTitle.
  ///
  /// In en, this message translates to:
  /// **'Sensors'**
  String get activityRecordingSensorsTitle;

  /// No description provided for @activityRecordingSensorsAddInSettings.
  ///
  /// In en, this message translates to:
  /// **'Add sensors in Settings'**
  String get activityRecordingSensorsAddInSettings;

  /// No description provided for @activityRecordingSensorsNotConfigured.
  ///
  /// In en, this message translates to:
  /// **'Not configured'**
  String get activityRecordingSensorsNotConfigured;

  /// No description provided for @activityRecordingSensorsConnected.
  ///
  /// In en, this message translates to:
  /// **'Connected'**
  String get activityRecordingSensorsConnected;

  /// No description provided for @activityRecordingSensorsConnecting.
  ///
  /// In en, this message translates to:
  /// **'Connecting'**
  String get activityRecordingSensorsConnecting;

  /// No description provided for @activityRecordingSensorsReconnecting.
  ///
  /// In en, this message translates to:
  /// **'Reconnecting'**
  String get activityRecordingSensorsReconnecting;

  /// No description provided for @activityRecordingSensorsDisabled.
  ///
  /// In en, this message translates to:
  /// **'Disabled'**
  String get activityRecordingSensorsDisabled;

  /// No description provided for @activityRecordingSensorsWaitingForData.
  ///
  /// In en, this message translates to:
  /// **'Waiting for sensor data…'**
  String get activityRecordingSensorsWaitingForData;

  /// No description provided for @activityRecordingSensorsWaitingShort.
  ///
  /// In en, this message translates to:
  /// **'—'**
  String get activityRecordingSensorsWaitingShort;

  /// No description provided for @activityRecordingSensorsNoSignalShort.
  ///
  /// In en, this message translates to:
  /// **'No signal'**
  String get activityRecordingSensorsNoSignalShort;

  /// No description provided for @activityRecordingSensorsGarminBroadcastHint.
  ///
  /// In en, this message translates to:
  /// **'Connected, but the watch is not broadcasting heart rate. On Garmin: Settings → Watch Sensors → Wrist Heart Rate → Broadcast Heart Rate, then start it on the watch. Disconnect Gadgetbridge first, or use a BLE chest strap instead.'**
  String get activityRecordingSensorsGarminBroadcastHint;

  /// No description provided for @activityRecordingSensorsRecordedTitle.
  ///
  /// In en, this message translates to:
  /// **'Recorded sensor data'**
  String get activityRecordingSensorsRecordedTitle;

  /// No description provided for @activityRecordingLiveHeartRate.
  ///
  /// In en, this message translates to:
  /// **'Heart rate'**
  String get activityRecordingLiveHeartRate;

  /// No description provided for @activityRecordingLiveCadence.
  ///
  /// In en, this message translates to:
  /// **'Cadence'**
  String get activityRecordingLiveCadence;

  /// No description provided for @activityRecordingLivePower.
  ///
  /// In en, this message translates to:
  /// **'Power'**
  String get activityRecordingLivePower;

  /// No description provided for @activityRecordingLiveSpeed.
  ///
  /// In en, this message translates to:
  /// **'Speed'**
  String get activityRecordingLiveSpeed;

  /// No description provided for @activityRecordingNotificationHeartRate.
  ///
  /// In en, this message translates to:
  /// **'HR {arg0}'**
  String activityRecordingNotificationHeartRate(String arg0);

  /// No description provided for @settingsNutritionGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Nutrition'**
  String get settingsNutritionGroupTitle;

  /// No description provided for @settingsNutritionGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Calories data and caffeine personalization'**
  String get settingsNutritionGroupBody;

  /// No description provided for @settingsCaloriesGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Calories'**
  String get settingsCaloriesGroupTitle;

  /// No description provided for @settingsCaloriesGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Total calories data'**
  String get settingsCaloriesGroupBody;

  /// No description provided for @settingsCaffeineGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Caffeine'**
  String get settingsCaffeineGroupTitle;

  /// No description provided for @settingsCaffeineGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Half-life, bedtime, sleep threshold, and personalization.'**
  String get settingsCaffeineGroupBody;

  /// No description provided for @settingsRecoveryGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Recovery'**
  String get settingsRecoveryGroupTitle;

  /// No description provided for @settingsRecoveryGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Sleep range and Body Energy calibration'**
  String get settingsRecoveryGroupBody;

  /// No description provided for @settingsSleepGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Sleep'**
  String get settingsSleepGroupTitle;

  /// No description provided for @settingsSleepGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Sleep range'**
  String get settingsSleepGroupBody;

  /// No description provided for @settingsCycleGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Menstrual cycle'**
  String get settingsCycleGroupTitle;

  /// No description provided for @settingsCycleGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Cycle data and Health Connect permissions'**
  String get settingsCycleGroupBody;

  /// No description provided for @settingsDataImportGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Data Importers'**
  String get settingsDataImportGroupTitle;

  /// No description provided for @settingsDataImportGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Import Apple Health exports, route files, and FIT files'**
  String get settingsDataImportGroupBody;

  /// No description provided for @settingsPermissionsGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Permissions'**
  String get settingsPermissionsGroupTitle;

  /// No description provided for @settingsPermissionsGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Health data access and manual permission steps'**
  String get settingsPermissionsGroupBody;

  /// No description provided for @settingsHealthConnectGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Health Connect'**
  String get settingsHealthConnectGroupTitle;

  /// No description provided for @settingsHealthConnectGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Sync, permissions, access, and app lock'**
  String get settingsHealthConnectGroupBody;

  /// No description provided for @settingsDebugDiagnosticsGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Debug diagnostics'**
  String get settingsDebugDiagnosticsGroupTitle;

  /// No description provided for @settingsDebugDiagnosticsGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Save sanitized diagnostics logs for troubleshooting'**
  String get settingsDebugDiagnosticsGroupBody;

  /// No description provided for @settingsHealthConnectSyncTitle.
  ///
  /// In en, this message translates to:
  /// **'Sync with Health Connect'**
  String get settingsHealthConnectSyncTitle;

  /// No description provided for @settingsHealthConnectSyncBody.
  ///
  /// In en, this message translates to:
  /// **'When on, OpenVitals reads and writes health data according to your permissions. When off, sync pauses without revoking access.'**
  String get settingsHealthConnectSyncBody;

  /// No description provided for @settingsHealthConnectManageAccess.
  ///
  /// In en, this message translates to:
  /// **'Manage access'**
  String get settingsHealthConnectManageAccess;

  /// No description provided for @settingsHealthConnectManageAccessBody.
  ///
  /// In en, this message translates to:
  /// **'Open Health Connect to review or change which data OpenVitals can use.'**
  String get settingsHealthConnectManageAccessBody;

  /// No description provided for @healthConnectAccessInsufficientTitle.
  ///
  /// In en, this message translates to:
  /// **'Choose data to share'**
  String get healthConnectAccessInsufficientTitle;

  /// No description provided for @healthConnectAccessInsufficientBody.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals needs Health Connect access to show this information. Set up the data you want to share to continue.'**
  String get healthConnectAccessInsufficientBody;

  /// No description provided for @healthConnectAccessDoubleCancelTitle.
  ///
  /// In en, this message translates to:
  /// **'Permissions need attention'**
  String get healthConnectAccessDoubleCancelTitle;

  /// No description provided for @healthConnectAccessDoubleCancelBody.
  ///
  /// In en, this message translates to:
  /// **'Health Connect permissions were not granted. Open Health Connect settings to choose which data to share with OpenVitals.'**
  String get healthConnectAccessDoubleCancelBody;

  /// No description provided for @healthConnectSyncPaused.
  ///
  /// In en, this message translates to:
  /// **'Health Connect sync is paused'**
  String get healthConnectSyncPaused;

  /// No description provided for @healthConnectSyncInProgress.
  ///
  /// In en, this message translates to:
  /// **'Syncing with Health Connect…'**
  String get healthConnectSyncInProgress;

  /// No description provided for @healthConnectDataSourceManage.
  ///
  /// In en, this message translates to:
  /// **'Manage data sources'**
  String get healthConnectDataSourceManage;

  /// No description provided for @healthConnectDataSourceManageBody.
  ///
  /// In en, this message translates to:
  /// **'See which apps write data to Health Connect and manage their access.'**
  String get healthConnectDataSourceManageBody;

  /// No description provided for @dashboardHealthConnectPromoTitle.
  ///
  /// In en, this message translates to:
  /// **'Set up your health data'**
  String get dashboardHealthConnectPromoTitle;

  /// No description provided for @dashboardHealthConnectPromoBody.
  ///
  /// In en, this message translates to:
  /// **'Get a unified view of your activity, sleep, and heart data from the apps and devices you already use.'**
  String get dashboardHealthConnectPromoBody;

  /// No description provided for @dashboardHealthConnectPromoAction.
  ///
  /// In en, this message translates to:
  /// **'Get started'**
  String get dashboardHealthConnectPromoAction;

  /// No description provided for @dashboardHealthConnectSyncPausedBody.
  ///
  /// In en, this message translates to:
  /// **'Turn sync back on in Settings to refresh your dashboard.'**
  String get dashboardHealthConnectSyncPausedBody;

  /// No description provided for @dashboardHealthConnectInstallAction.
  ///
  /// In en, this message translates to:
  /// **'Install Health Connect'**
  String get dashboardHealthConnectInstallAction;

  /// No description provided for @healthConnectMatchmakingTitle.
  ///
  /// In en, this message translates to:
  /// **'Connect your apps'**
  String get healthConnectMatchmakingTitle;

  /// No description provided for @healthConnectMatchmakingBody.
  ///
  /// In en, this message translates to:
  /// **'Find apps and devices that can share data OpenVitals is ready to read.'**
  String get healthConnectMatchmakingBody;

  /// No description provided for @healthConnectMatchmakingAction.
  ///
  /// In en, this message translates to:
  /// **'Find data sources'**
  String get healthConnectMatchmakingAction;

  /// No description provided for @healthConnectPromoteActivityTitle.
  ///
  /// In en, this message translates to:
  /// **'Unlock activity insights'**
  String get healthConnectPromoteActivityTitle;

  /// No description provided for @healthConnectPromoteActivityBody.
  ///
  /// In en, this message translates to:
  /// **'Allow activity data to see steps, distance, workouts, and trends in OpenVitals.'**
  String get healthConnectPromoteActivityBody;

  /// No description provided for @healthConnectPromoteActivitiesTitle.
  ///
  /// In en, this message translates to:
  /// **'See your workouts'**
  String get healthConnectPromoteActivitiesTitle;

  /// No description provided for @healthConnectPromoteActivitiesBody.
  ///
  /// In en, this message translates to:
  /// **'Allow exercise session access to browse activities synced through Health Connect.'**
  String get healthConnectPromoteActivitiesBody;

  /// No description provided for @healthConnectPromoteCaloriesTitle.
  ///
  /// In en, this message translates to:
  /// **'Track calories burned'**
  String get healthConnectPromoteCaloriesTitle;

  /// No description provided for @healthConnectPromoteCaloriesBody.
  ///
  /// In en, this message translates to:
  /// **'Allow calorie data to compare active and total burn over time.'**
  String get healthConnectPromoteCaloriesBody;

  /// No description provided for @healthConnectPromoteSleepTitle.
  ///
  /// In en, this message translates to:
  /// **'See your sleep'**
  String get healthConnectPromoteSleepTitle;

  /// No description provided for @healthConnectPromoteSleepBody.
  ///
  /// In en, this message translates to:
  /// **'Allow sleep data to view stages, duration, and sleep score trends.'**
  String get healthConnectPromoteSleepBody;

  /// No description provided for @healthConnectPromoteHeartTitle.
  ///
  /// In en, this message translates to:
  /// **'Monitor heart health'**
  String get healthConnectPromoteHeartTitle;

  /// No description provided for @healthConnectPromoteHeartBody.
  ///
  /// In en, this message translates to:
  /// **'Allow heart rate and HRV data to track resting rate and variability.'**
  String get healthConnectPromoteHeartBody;

  /// No description provided for @healthConnectPromoteVitalsTitle.
  ///
  /// In en, this message translates to:
  /// **'Unlock vitals'**
  String get healthConnectPromoteVitalsTitle;

  /// No description provided for @healthConnectPromoteVitalsBody.
  ///
  /// In en, this message translates to:
  /// **'Allow vitals data to see blood pressure, SpO2, and related measurements.'**
  String get healthConnectPromoteVitalsBody;

  /// No description provided for @healthConnectPromoteBodyTitle.
  ///
  /// In en, this message translates to:
  /// **'Track body metrics'**
  String get healthConnectPromoteBodyTitle;

  /// No description provided for @healthConnectPromoteBodyBody.
  ///
  /// In en, this message translates to:
  /// **'Allow body composition data to follow weight, BMI, and related trends.'**
  String get healthConnectPromoteBodyBody;

  /// No description provided for @healthConnectPromoteHydrationTitle.
  ///
  /// In en, this message translates to:
  /// **'Track beverages'**
  String get healthConnectPromoteHydrationTitle;

  /// No description provided for @healthConnectPromoteHydrationBody.
  ///
  /// In en, this message translates to:
  /// **'Allow hydration and nutrition data to see daily drinks and history.'**
  String get healthConnectPromoteHydrationBody;

  /// No description provided for @healthConnectPromoteNutritionTitle.
  ///
  /// In en, this message translates to:
  /// **'See nutrition'**
  String get healthConnectPromoteNutritionTitle;

  /// No description provided for @healthConnectPromoteNutritionBody.
  ///
  /// In en, this message translates to:
  /// **'Allow nutrition data to review calories and macros from your sources.'**
  String get healthConnectPromoteNutritionBody;

  /// No description provided for @healthConnectPromoteMindfulnessTitle.
  ///
  /// In en, this message translates to:
  /// **'Track mindfulness'**
  String get healthConnectPromoteMindfulnessTitle;

  /// No description provided for @healthConnectPromoteMindfulnessBody.
  ///
  /// In en, this message translates to:
  /// **'Allow mindfulness session data to see your practice over time.'**
  String get healthConnectPromoteMindfulnessBody;

  /// No description provided for @healthConnectPromoteCycleTitle.
  ///
  /// In en, this message translates to:
  /// **'Track cycle data'**
  String get healthConnectPromoteCycleTitle;

  /// No description provided for @healthConnectPromoteCycleBody.
  ///
  /// In en, this message translates to:
  /// **'Allow menstrual cycle data to view flow, symptoms, and related records.'**
  String get healthConnectPromoteCycleBody;

  /// No description provided for @healthConnectPromoteReadinessTitle.
  ///
  /// In en, this message translates to:
  /// **'Improve readiness insights'**
  String get healthConnectPromoteReadinessTitle;

  /// No description provided for @healthConnectPromoteReadinessBody.
  ///
  /// In en, this message translates to:
  /// **'Allow additional Health Connect data to refine daily readiness scores.'**
  String get healthConnectPromoteReadinessBody;

  /// No description provided for @healthConnectNewPermissionsTitle.
  ///
  /// In en, this message translates to:
  /// **'New data available'**
  String get healthConnectNewPermissionsTitle;

  /// No description provided for @healthConnectNewPermissionsBody.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals can now read additional health data types. Grant access to use the new features.'**
  String get healthConnectNewPermissionsBody;

  /// No description provided for @healthConnectNewPermissionsAction.
  ///
  /// In en, this message translates to:
  /// **'Review permissions'**
  String get healthConnectNewPermissionsAction;

  /// No description provided for @privacyReconsentTitle.
  ///
  /// In en, this message translates to:
  /// **'Privacy policy updated'**
  String get privacyReconsentTitle;

  /// No description provided for @privacyReconsentBody.
  ///
  /// In en, this message translates to:
  /// **'Our privacy policy has changed. Review and accept to continue syncing with Health Connect.'**
  String get privacyReconsentBody;

  /// No description provided for @privacyReconsentAction.
  ///
  /// In en, this message translates to:
  /// **'Review policy'**
  String get privacyReconsentAction;

  /// No description provided for @dashboardSummaryToday.
  ///
  /// In en, this message translates to:
  /// **'Today'**
  String get dashboardSummaryToday;

  /// No description provided for @settingsDebugLogsTitle.
  ///
  /// In en, this message translates to:
  /// **'Sanitized diagnostics logs'**
  String get settingsDebugLogsTitle;

  /// No description provided for @settingsDebugLogsBody.
  ///
  /// In en, this message translates to:
  /// **'Share or save OpenVitals diagnostics log entries as a text file. The export drops or redacts identifiers, locations, dates, URIs, raw sensor payloads, and unrelated app logs before writing.'**
  String get settingsDebugLogsBody;

  /// No description provided for @settingsDebugLogsShare.
  ///
  /// In en, this message translates to:
  /// **'Share logs'**
  String get settingsDebugLogsShare;

  /// No description provided for @settingsDebugLogsShareChooserTitle.
  ///
  /// In en, this message translates to:
  /// **'Share diagnostics logs'**
  String get settingsDebugLogsShareChooserTitle;

  /// No description provided for @settingsDebugLogsShareFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not share diagnostics logs'**
  String get settingsDebugLogsShareFailed;

  /// No description provided for @settingsDebugLogsSave.
  ///
  /// In en, this message translates to:
  /// **'Save logs'**
  String get settingsDebugLogsSave;

  /// No description provided for @settingsDebugLogsSaved.
  ///
  /// In en, this message translates to:
  /// **'Debug logs saved'**
  String get settingsDebugLogsSaved;

  /// No description provided for @settingsDebugLogsSaveFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not save diagnostics logs'**
  String get settingsDebugLogsSaveFailed;

  /// No description provided for @settingsPrivacyPolicyLink.
  ///
  /// In en, this message translates to:
  /// **'View privacy policy'**
  String get settingsPrivacyPolicyLink;

  /// No description provided for @settingsAppLockTitle.
  ///
  /// In en, this message translates to:
  /// **'App lock'**
  String get settingsAppLockTitle;

  /// No description provided for @settingsAppLockBody.
  ///
  /// In en, this message translates to:
  /// **'Require device unlock to open OpenVitals.'**
  String get settingsAppLockBody;

  /// No description provided for @onboardingCoreRequired.
  ///
  /// In en, this message translates to:
  /// **'Activity, sleep, and heart rate access are needed to get started. You can add more data types later from Settings.'**
  String get onboardingCoreRequired;

  /// No description provided for @settingsLanguageTitle.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get settingsLanguageTitle;

  /// No description provided for @settingsLanguageBody.
  ///
  /// In en, this message translates to:
  /// **'Choose app language or follow your system setting.'**
  String get settingsLanguageBody;

  /// No description provided for @settingsLanguageSystem.
  ///
  /// In en, this message translates to:
  /// **'System'**
  String get settingsLanguageSystem;

  /// No description provided for @settingsLanguageEnglish.
  ///
  /// In en, this message translates to:
  /// **'English'**
  String get settingsLanguageEnglish;

  /// No description provided for @settingsLanguageSpanish.
  ///
  /// In en, this message translates to:
  /// **'Spanish'**
  String get settingsLanguageSpanish;

  /// No description provided for @settingsLanguageGerman.
  ///
  /// In en, this message translates to:
  /// **'German'**
  String get settingsLanguageGerman;

  /// No description provided for @settingsLanguageItalian.
  ///
  /// In en, this message translates to:
  /// **'Italian'**
  String get settingsLanguageItalian;

  /// No description provided for @settingsLanguageEstonian.
  ///
  /// In en, this message translates to:
  /// **'Estonian'**
  String get settingsLanguageEstonian;

  /// No description provided for @settingsUnitsTitle.
  ///
  /// In en, this message translates to:
  /// **'Units'**
  String get settingsUnitsTitle;

  /// No description provided for @settingsUnitsBody.
  ///
  /// In en, this message translates to:
  /// **'Choose how distances, weights, hydration, and temperature are displayed.'**
  String get settingsUnitsBody;

  /// No description provided for @settingsUnitMetric.
  ///
  /// In en, this message translates to:
  /// **'Metric'**
  String get settingsUnitMetric;

  /// No description provided for @settingsUnitImperial.
  ///
  /// In en, this message translates to:
  /// **'Imperial'**
  String get settingsUnitImperial;

  /// No description provided for @settingsThemeTitle.
  ///
  /// In en, this message translates to:
  /// **'Theme'**
  String get settingsThemeTitle;

  /// No description provided for @settingsThemeBody.
  ///
  /// In en, this message translates to:
  /// **'Choose app appearance independently from Android dark mode.'**
  String get settingsThemeBody;

  /// No description provided for @settingsThemeSystem.
  ///
  /// In en, this message translates to:
  /// **'System'**
  String get settingsThemeSystem;

  /// No description provided for @settingsThemeLight.
  ///
  /// In en, this message translates to:
  /// **'Light'**
  String get settingsThemeLight;

  /// No description provided for @settingsThemeDark.
  ///
  /// In en, this message translates to:
  /// **'Dark'**
  String get settingsThemeDark;

  /// No description provided for @settingsThemeAmoled.
  ///
  /// In en, this message translates to:
  /// **'AMOLED'**
  String get settingsThemeAmoled;

  /// No description provided for @settingsDynamicColorTitle.
  ///
  /// In en, this message translates to:
  /// **'Dynamic color (Material You)'**
  String get settingsDynamicColorTitle;

  /// No description provided for @settingsDynamicColorBody.
  ///
  /// In en, this message translates to:
  /// **'Tint OpenVitals from your Android wallpaper. Off uses the OpenVitals blue and teal brand palette.'**
  String get settingsDynamicColorBody;

  /// No description provided for @settingsActivityWeekTitle.
  ///
  /// In en, this message translates to:
  /// **'Rolling dates'**
  String get settingsActivityWeekTitle;

  /// No description provided for @settingsActivityWeekBody.
  ///
  /// In en, this message translates to:
  /// **'Use rolling 7, 30, and 365-day windows instead of calendar week, month, and year.'**
  String get settingsActivityWeekBody;

  /// No description provided for @settingsActivityWeekMondayToSunday.
  ///
  /// In en, this message translates to:
  /// **'Calendar'**
  String get settingsActivityWeekMondayToSunday;

  /// No description provided for @settingsActivityWeekLast7Days.
  ///
  /// In en, this message translates to:
  /// **'Rolling'**
  String get settingsActivityWeekLast7Days;

  /// No description provided for @settingsFavoriteActivityTitle.
  ///
  /// In en, this message translates to:
  /// **'Favorite activity'**
  String get settingsFavoriteActivityTitle;

  /// No description provided for @settingsFavoriteActivityBody.
  ///
  /// In en, this message translates to:
  /// **'Use the latest recorded activity by default, or choose one activity type to always preselect.'**
  String get settingsFavoriteActivityBody;

  /// No description provided for @settingsFavoriteActivityLatest.
  ///
  /// In en, this message translates to:
  /// **'Use latest'**
  String get settingsFavoriteActivityLatest;

  /// No description provided for @settingsActivityRecordingTitle.
  ///
  /// In en, this message translates to:
  /// **'Activity recording'**
  String get settingsActivityRecordingTitle;

  /// No description provided for @settingsActivityRecordingBody.
  ///
  /// In en, this message translates to:
  /// **'Tune live GPS recording without changing the saved activity details workflow.'**
  String get settingsActivityRecordingBody;

  /// No description provided for @settingsActivityRecordingKeepScreenOnTitle.
  ///
  /// In en, this message translates to:
  /// **'Screen always on'**
  String get settingsActivityRecordingKeepScreenOnTitle;

  /// No description provided for @settingsActivityRecordingKeepScreenOnBody.
  ///
  /// In en, this message translates to:
  /// **'Keep the screen awake while an activity recording is active.'**
  String get settingsActivityRecordingKeepScreenOnBody;

  /// No description provided for @settingsActivityRecordingAutoIdleTitle.
  ///
  /// In en, this message translates to:
  /// **'Auto-idle'**
  String get settingsActivityRecordingAutoIdleTitle;

  /// No description provided for @settingsActivityRecordingAutoIdleBody.
  ///
  /// In en, this message translates to:
  /// **'Pause moving time when you stop for longer than the selected timeout.'**
  String get settingsActivityRecordingAutoIdleBody;

  /// No description provided for @settingsActivityRecordingIdleTimeoutTitle.
  ///
  /// In en, this message translates to:
  /// **'Idle timeout'**
  String get settingsActivityRecordingIdleTimeoutTitle;

  /// No description provided for @settingsActivityRecordingAccuracyTitle.
  ///
  /// In en, this message translates to:
  /// **'Required GPS accuracy'**
  String get settingsActivityRecordingAccuracyTitle;

  /// No description provided for @settingsActivityRecordingRouteGapTitle.
  ///
  /// In en, this message translates to:
  /// **'New route segment after gap'**
  String get settingsActivityRecordingRouteGapTitle;

  /// No description provided for @settingsActivityRecordingTimeIntervalTitle.
  ///
  /// In en, this message translates to:
  /// **'Recording time interval'**
  String get settingsActivityRecordingTimeIntervalTitle;

  /// No description provided for @settingsActivityRecordingDistanceIntervalTitle.
  ///
  /// In en, this message translates to:
  /// **'Recording distance interval'**
  String get settingsActivityRecordingDistanceIntervalTitle;

  /// No description provided for @settingsActivityRecordingBarometerTitle.
  ///
  /// In en, this message translates to:
  /// **'Barometer climb'**
  String get settingsActivityRecordingBarometerTitle;

  /// No description provided for @settingsActivityRecordingBarometerBody.
  ///
  /// In en, this message translates to:
  /// **'Use pressure changes for climb when the device has a barometer.'**
  String get settingsActivityRecordingBarometerBody;

  /// No description provided for @settingsActivityRecordingRestBellTitle.
  ///
  /// In en, this message translates to:
  /// **'Rest timer bell'**
  String get settingsActivityRecordingRestBellTitle;

  /// No description provided for @settingsActivityRecordingRestBellBody.
  ///
  /// In en, this message translates to:
  /// **'Play a soft bell when set rest countdowns finish.'**
  String get settingsActivityRecordingRestBellBody;

  /// No description provided for @settingsActivityRecordingVoiceTitle.
  ///
  /// In en, this message translates to:
  /// **'Voice announcements'**
  String get settingsActivityRecordingVoiceTitle;

  /// No description provided for @settingsActivityRecordingVoiceBody.
  ///
  /// In en, this message translates to:
  /// **'Speak periodic progress, idle/resume, and lap updates while recording.'**
  String get settingsActivityRecordingVoiceBody;

  /// No description provided for @settingsActivityRecordingVoiceTimeTitle.
  ///
  /// In en, this message translates to:
  /// **'Announce by time'**
  String get settingsActivityRecordingVoiceTimeTitle;

  /// No description provided for @settingsActivityRecordingVoiceDistanceTitle.
  ///
  /// In en, this message translates to:
  /// **'Announce by distance'**
  String get settingsActivityRecordingVoiceDistanceTitle;

  /// No description provided for @settingsActivityRecordingVoiceIdleTitle.
  ///
  /// In en, this message translates to:
  /// **'Idle announcements'**
  String get settingsActivityRecordingVoiceIdleTitle;

  /// No description provided for @settingsActivityRecordingVoiceIdleBody.
  ///
  /// In en, this message translates to:
  /// **'Say when auto-idle starts and when recording resumes.'**
  String get settingsActivityRecordingVoiceIdleBody;

  /// No description provided for @settingsActivityRecordingVoiceLapTitle.
  ///
  /// In en, this message translates to:
  /// **'Lap announcements'**
  String get settingsActivityRecordingVoiceLapTitle;

  /// No description provided for @settingsActivityRecordingVoiceLapBody.
  ///
  /// In en, this message translates to:
  /// **'Say a progress summary when you mark a lap.'**
  String get settingsActivityRecordingVoiceLapBody;

  /// No description provided for @settingsActivityRecordingSeconds.
  ///
  /// In en, this message translates to:
  /// **'{arg0} s'**
  String settingsActivityRecordingSeconds(int arg0);

  /// No description provided for @settingsActivityRecordingHalfSecond.
  ///
  /// In en, this message translates to:
  /// **'0.5 s'**
  String get settingsActivityRecordingHalfSecond;

  /// No description provided for @settingsActivityRecordingMeters.
  ///
  /// In en, this message translates to:
  /// **'{arg0} m'**
  String settingsActivityRecordingMeters(int arg0);

  /// No description provided for @settingsActivityRecordingAuto.
  ///
  /// In en, this message translates to:
  /// **'Auto'**
  String get settingsActivityRecordingAuto;

  /// No description provided for @settingsActivityRecordingOff.
  ///
  /// In en, this message translates to:
  /// **'Off'**
  String get settingsActivityRecordingOff;

  /// No description provided for @settingsCalorieDataTitle.
  ///
  /// In en, this message translates to:
  /// **'Total calories data'**
  String get settingsCalorieDataTitle;

  /// No description provided for @settingsCalorieDataBody.
  ///
  /// In en, this message translates to:
  /// **'Show plain Health Connect total calories by default. Turn on OpenVitals calculations to fill missing totals from active calories and BMR.'**
  String get settingsCalorieDataBody;

  /// No description provided for @settingsCaffeineTitle.
  ///
  /// In en, this message translates to:
  /// **'Caffeine model'**
  String get settingsCaffeineTitle;

  /// No description provided for @settingsCaffeineBody.
  ///
  /// In en, this message translates to:
  /// **'These values personalize caffeine level, bedtime forecast, and safe-sleep insights. Entries remain in Health Connect.'**
  String get settingsCaffeineBody;

  /// No description provided for @settingsBodyProfileTitle.
  ///
  /// In en, this message translates to:
  /// **'Body profile'**
  String get settingsBodyProfileTitle;

  /// No description provided for @settingsBodyProfileBody.
  ///
  /// In en, this message translates to:
  /// **'Age, weight, and heart rate personalize Body Energy and Caffeine estimates. All fields are optional.'**
  String get settingsBodyProfileBody;

  /// No description provided for @settingsBodyProfileWeight.
  ///
  /// In en, this message translates to:
  /// **'Weight'**
  String get settingsBodyProfileWeight;

  /// No description provided for @settingsSleepRangeTitle.
  ///
  /// In en, this message translates to:
  /// **'Sleep range'**
  String get settingsSleepRangeTitle;

  /// No description provided for @settingsSleepRangeBody.
  ///
  /// In en, this message translates to:
  /// **'Choose which day sleep sessions are assigned to.'**
  String get settingsSleepRangeBody;

  /// No description provided for @settingsSleepRangeRolling24h.
  ///
  /// In en, this message translates to:
  /// **'Rolling 24h'**
  String get settingsSleepRangeRolling24h;

  /// No description provided for @settingsSleepRangeNoon.
  ///
  /// In en, this message translates to:
  /// **'Noon'**
  String get settingsSleepRangeNoon;

  /// No description provided for @settingsSleepRangeEvening.
  ///
  /// In en, this message translates to:
  /// **'18:00'**
  String get settingsSleepRangeEvening;

  /// No description provided for @settingsCyclePermissionsTitle.
  ///
  /// In en, this message translates to:
  /// **'Cycle permissions'**
  String get settingsCyclePermissionsTitle;

  /// No description provided for @settingsCyclePermissionsGranted.
  ///
  /// In en, this message translates to:
  /// **'{arg0}/{arg1} cycle permissions granted.'**
  String settingsCyclePermissionsGranted(int arg0, int arg1);

  /// No description provided for @settingsAppleHealthImportTitle.
  ///
  /// In en, this message translates to:
  /// **'Apple Health Importer'**
  String get settingsAppleHealthImportTitle;

  /// No description provided for @settingsAppleHealthImportBody.
  ///
  /// In en, this message translates to:
  /// **'Import Health Connect-compatible records from Apple Health export.xml or export.zip, with duplicate checks and a shareable diagnostics report.'**
  String get settingsAppleHealthImportBody;

  /// No description provided for @settingsAppleHealthImportPermissions.
  ///
  /// In en, this message translates to:
  /// **'{arg0}/{arg1} import permissions granted.'**
  String settingsAppleHealthImportPermissions(int arg0, int arg1);

  /// No description provided for @settingsAppleHealthImportGrant.
  ///
  /// In en, this message translates to:
  /// **'Grant import permissions'**
  String get settingsAppleHealthImportGrant;

  /// No description provided for @settingsAppleHealthImportAction.
  ///
  /// In en, this message translates to:
  /// **'Import Apple Health export'**
  String get settingsAppleHealthImportAction;

  /// No description provided for @settingsAppleHealthImportAnalyzeAction.
  ///
  /// In en, this message translates to:
  /// **'Analyze Apple Health export'**
  String get settingsAppleHealthImportAnalyzeAction;

  /// No description provided for @settingsAppleHealthImportChooseAnotherAction.
  ///
  /// In en, this message translates to:
  /// **'Choose another Apple Health export'**
  String get settingsAppleHealthImportChooseAnotherAction;

  /// No description provided for @settingsAppleHealthImportSelectedAction.
  ///
  /// In en, this message translates to:
  /// **'Import selected categories'**
  String get settingsAppleHealthImportSelectedAction;

  /// No description provided for @settingsAppleHealthImportAnalyzing.
  ///
  /// In en, this message translates to:
  /// **'Analyzing...'**
  String get settingsAppleHealthImportAnalyzing;

  /// No description provided for @settingsAppleHealthImporting.
  ///
  /// In en, this message translates to:
  /// **'Importing...'**
  String get settingsAppleHealthImporting;

  /// No description provided for @settingsAppleHealthImportProgressQueued.
  ///
  /// In en, this message translates to:
  /// **'Queued'**
  String get settingsAppleHealthImportProgressQueued;

  /// No description provided for @settingsAppleHealthImportProgressParsing.
  ///
  /// In en, this message translates to:
  /// **'Scanning export'**
  String get settingsAppleHealthImportProgressParsing;

  /// No description provided for @settingsAppleHealthImportProgressConverting.
  ///
  /// In en, this message translates to:
  /// **'Converting records'**
  String get settingsAppleHealthImportProgressConverting;

  /// No description provided for @settingsAppleHealthImportProgressCheckingDuplicates.
  ///
  /// In en, this message translates to:
  /// **'Checking duplicates'**
  String get settingsAppleHealthImportProgressCheckingDuplicates;

  /// No description provided for @settingsAppleHealthImportProgressWriting.
  ///
  /// In en, this message translates to:
  /// **'Writing records'**
  String get settingsAppleHealthImportProgressWriting;

  /// No description provided for @settingsAppleHealthImportProgressFinishing.
  ///
  /// In en, this message translates to:
  /// **'Finalizing import'**
  String get settingsAppleHealthImportProgressFinishing;

  /// No description provided for @settingsAppleHealthImportProgressBuildingReport.
  ///
  /// In en, this message translates to:
  /// **'Building report'**
  String get settingsAppleHealthImportProgressBuildingReport;

  /// No description provided for @settingsAppleHealthImportProgressComplete.
  ///
  /// In en, this message translates to:
  /// **'Complete'**
  String get settingsAppleHealthImportProgressComplete;

  /// No description provided for @settingsAppleHealthImportProgress.
  ///
  /// In en, this message translates to:
  /// **'{arg0}. Scanned {arg1} items, imported {arg2} records.'**
  String settingsAppleHealthImportProgress(String arg0, int arg1, int arg2);

  /// No description provided for @settingsAppleHealthImportProgressWithPercent.
  ///
  /// In en, this message translates to:
  /// **'{arg0}%. {arg1}. Selected {arg2}/{arg3} records, imported {arg4}.'**
  String settingsAppleHealthImportProgressWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  );

  /// No description provided for @settingsAppleHealthImportProgressWithScanPercent.
  ///
  /// In en, this message translates to:
  /// **'{arg0}%. {arg1}. Scanned {arg2}/{arg3} items. Selected {arg4}/{arg5} records, imported {arg6}.'**
  String settingsAppleHealthImportProgressWithScanPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
    int arg5,
    int arg6,
  );

  /// No description provided for @settingsAppleHealthImportBackground.
  ///
  /// In en, this message translates to:
  /// **'Import continues in the background while you leave the app.'**
  String get settingsAppleHealthImportBackground;

  /// No description provided for @settingsAppleHealthImportNotificationChannel.
  ///
  /// In en, this message translates to:
  /// **'Apple Health imports'**
  String get settingsAppleHealthImportNotificationChannel;

  /// No description provided for @settingsAppleHealthImportNotificationTitle.
  ///
  /// In en, this message translates to:
  /// **'Importing Apple Health export'**
  String get settingsAppleHealthImportNotificationTitle;

  /// No description provided for @settingsAppleHealthImportNotificationText.
  ///
  /// In en, this message translates to:
  /// **'{arg0}. Scanned {arg1}, imported {arg2}.'**
  String settingsAppleHealthImportNotificationText(
    String arg0,
    int arg1,
    int arg2,
  );

  /// No description provided for @settingsAppleHealthImportNotificationTextWithPercent.
  ///
  /// In en, this message translates to:
  /// **'{arg0}%. {arg1}. Selected {arg2}/{arg3}, imported {arg4}.'**
  String settingsAppleHealthImportNotificationTextWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  );

  /// No description provided for @settingsAppleHealthImportNotificationTextWithScanPercent.
  ///
  /// In en, this message translates to:
  /// **'{arg0}%. {arg1}. Scanned {arg2}/{arg3}, imported {arg4}.'**
  String settingsAppleHealthImportNotificationTextWithScanPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  );

  /// No description provided for @settingsAppleHealthImportResult.
  ///
  /// In en, this message translates to:
  /// **'Imported {arg0}. Duplicates {arg1}. Not selected {arg2}. Unsupported {arg3}. Skipped {arg4}. Failed {arg5}.'**
  String settingsAppleHealthImportResult(
    int arg0,
    int arg1,
    int arg2,
    int arg3,
    int arg4,
    int arg5,
  );

  /// No description provided for @settingsAppleHealthImportRoutesIncomplete.
  ///
  /// In en, this message translates to:
  /// **'Health records were imported, but some workout routes were unavailable because the ZIP ended unexpectedly. The import report lists affected activities for manual recovery.'**
  String get settingsAppleHealthImportRoutesIncomplete;

  /// No description provided for @settingsAppleHealthImportAnalysisResult.
  ///
  /// In en, this message translates to:
  /// **'Scanned {arg0} items. Found {arg1} compatible records. Unsupported {arg2}. Failed {arg3}.'**
  String settingsAppleHealthImportAnalysisResult(
    int arg0,
    int arg1,
    int arg2,
    int arg3,
  );

  /// No description provided for @settingsAppleHealthImportChooseCategories.
  ///
  /// In en, this message translates to:
  /// **'Choose what to write to Health Connect.'**
  String get settingsAppleHealthImportChooseCategories;

  /// No description provided for @settingsAppleHealthImportCategoryCount.
  ///
  /// In en, this message translates to:
  /// **'{arg0} records'**
  String settingsAppleHealthImportCategoryCount(int arg0);

  /// No description provided for @settingsAppleHealthImportCategoryCountRoutes.
  ///
  /// In en, this message translates to:
  /// **'{arg0} records, {arg1} with routes'**
  String settingsAppleHealthImportCategoryCountRoutes(int arg0, int arg1);

  /// No description provided for @settingsAppleHealthImportCategoryWorkouts.
  ///
  /// In en, this message translates to:
  /// **'Workouts and routes'**
  String get settingsAppleHealthImportCategoryWorkouts;

  /// No description provided for @settingsAppleHealthImportCategoryWorkoutsDesc.
  ///
  /// In en, this message translates to:
  /// **'Exercise sessions and attached workout route geometry.'**
  String get settingsAppleHealthImportCategoryWorkoutsDesc;

  /// No description provided for @settingsAppleHealthImportCategoryActivity.
  ///
  /// In en, this message translates to:
  /// **'Activity metrics'**
  String get settingsAppleHealthImportCategoryActivity;

  /// No description provided for @settingsAppleHealthImportCategoryActivityDesc.
  ///
  /// In en, this message translates to:
  /// **'Steps, distance, calories, floors, elevation, wheelchair pushes, and speed.'**
  String get settingsAppleHealthImportCategoryActivityDesc;

  /// No description provided for @settingsAppleHealthImportCategoryHeart.
  ///
  /// In en, this message translates to:
  /// **'Heart'**
  String get settingsAppleHealthImportCategoryHeart;

  /// No description provided for @settingsAppleHealthImportCategoryHeartDesc.
  ///
  /// In en, this message translates to:
  /// **'Heart rate and resting heart rate records.'**
  String get settingsAppleHealthImportCategoryHeartDesc;

  /// No description provided for @settingsAppleHealthImportCategorySleep.
  ///
  /// In en, this message translates to:
  /// **'Sleep'**
  String get settingsAppleHealthImportCategorySleep;

  /// No description provided for @settingsAppleHealthImportCategorySleepDesc.
  ///
  /// In en, this message translates to:
  /// **'Sleep sessions and stages.'**
  String get settingsAppleHealthImportCategorySleepDesc;

  /// No description provided for @settingsAppleHealthImportCategoryBody.
  ///
  /// In en, this message translates to:
  /// **'Body measurements'**
  String get settingsAppleHealthImportCategoryBody;

  /// No description provided for @settingsAppleHealthImportCategoryBodyDesc.
  ///
  /// In en, this message translates to:
  /// **'Weight, height, body fat, lean mass, BMR, bone mass, and body water.'**
  String get settingsAppleHealthImportCategoryBodyDesc;

  /// No description provided for @settingsAppleHealthImportCategoryVitals.
  ///
  /// In en, this message translates to:
  /// **'Vitals'**
  String get settingsAppleHealthImportCategoryVitals;

  /// No description provided for @settingsAppleHealthImportCategoryVitalsDesc.
  ///
  /// In en, this message translates to:
  /// **'Blood pressure, oxygen saturation, respiratory rate, body temperature, blood glucose, and VO2 max.'**
  String get settingsAppleHealthImportCategoryVitalsDesc;

  /// No description provided for @settingsAppleHealthImportCategoryNutrition.
  ///
  /// In en, this message translates to:
  /// **'Nutrition'**
  String get settingsAppleHealthImportCategoryNutrition;

  /// No description provided for @settingsAppleHealthImportCategoryNutritionDesc.
  ///
  /// In en, this message translates to:
  /// **'Food energy, macros, caffeine, minerals, and vitamins.'**
  String get settingsAppleHealthImportCategoryNutritionDesc;

  /// No description provided for @settingsAppleHealthImportCategoryHydration.
  ///
  /// In en, this message translates to:
  /// **'Hydration'**
  String get settingsAppleHealthImportCategoryHydration;

  /// No description provided for @settingsAppleHealthImportCategoryHydrationDesc.
  ///
  /// In en, this message translates to:
  /// **'Water intake records.'**
  String get settingsAppleHealthImportCategoryHydrationDesc;

  /// No description provided for @settingsAppleHealthImportCategoryMindfulness.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness'**
  String get settingsAppleHealthImportCategoryMindfulness;

  /// No description provided for @settingsAppleHealthImportCategoryMindfulnessDesc.
  ///
  /// In en, this message translates to:
  /// **'Mindfulness session records when Health Connect supports them.'**
  String get settingsAppleHealthImportCategoryMindfulnessDesc;

  /// No description provided for @settingsAppleHealthImportCategoryCycle.
  ///
  /// In en, this message translates to:
  /// **'Cycle tracking'**
  String get settingsAppleHealthImportCategoryCycle;

  /// No description provided for @settingsAppleHealthImportCategoryCycleDesc.
  ///
  /// In en, this message translates to:
  /// **'Menstruation, ovulation, cervical mucus, bleeding, basal body temperature, and sexual activity records.'**
  String get settingsAppleHealthImportCategoryCycleDesc;

  /// No description provided for @settingsAppleHealthImportCopyReport.
  ///
  /// In en, this message translates to:
  /// **'Copy report'**
  String get settingsAppleHealthImportCopyReport;

  /// No description provided for @settingsAppleHealthImportCopyError.
  ///
  /// In en, this message translates to:
  /// **'Copy error'**
  String get settingsAppleHealthImportCopyError;

  /// No description provided for @settingsAppleHealthImportSaveReport.
  ///
  /// In en, this message translates to:
  /// **'Download full report'**
  String get settingsAppleHealthImportSaveReport;

  /// No description provided for @settingsAppleHealthImportReportCopied.
  ///
  /// In en, this message translates to:
  /// **'Import report copied.'**
  String get settingsAppleHealthImportReportCopied;

  /// No description provided for @settingsAppleHealthImportErrorCopied.
  ///
  /// In en, this message translates to:
  /// **'Import error copied.'**
  String get settingsAppleHealthImportErrorCopied;

  /// No description provided for @settingsAppleHealthImportReportSaved.
  ///
  /// In en, this message translates to:
  /// **'Import report saved.'**
  String get settingsAppleHealthImportReportSaved;

  /// No description provided for @settingsAppleHealthImportReportSaveFailed.
  ///
  /// In en, this message translates to:
  /// **'Unable to save import report.'**
  String get settingsAppleHealthImportReportSaveFailed;

  /// No description provided for @settingsAppleHealthImportError.
  ///
  /// In en, this message translates to:
  /// **'Import failed: {arg0}'**
  String settingsAppleHealthImportError(String arg0);

  /// No description provided for @settingsAppleHealthImportPermissionDenied.
  ///
  /// In en, this message translates to:
  /// **'Access to the selected file was lost, so the import couldn\'t continue. Select the same Apple Health export again to pick up right where it left off.'**
  String get settingsAppleHealthImportPermissionDenied;

  /// No description provided for @settingsRouteImportTitle.
  ///
  /// In en, this message translates to:
  /// **'GPX/KML/KMZ Importer'**
  String get settingsRouteImportTitle;

  /// No description provided for @settingsRouteImportBody.
  ///
  /// In en, this message translates to:
  /// **'Import GPX, KML, or KMZ route files. Review one file before saving, or bulk import multiple files directly into Health Connect.'**
  String get settingsRouteImportBody;

  /// No description provided for @settingsRouteImportPermissions.
  ///
  /// In en, this message translates to:
  /// **'{arg0}/{arg1} route import permissions granted.'**
  String settingsRouteImportPermissions(int arg0, int arg1);

  /// No description provided for @settingsRouteImportGrant.
  ///
  /// In en, this message translates to:
  /// **'Grant route import permissions'**
  String get settingsRouteImportGrant;

  /// No description provided for @settingsRouteImportAction.
  ///
  /// In en, this message translates to:
  /// **'Import GPX/KML/KMZ file'**
  String get settingsRouteImportAction;

  /// No description provided for @settingsRouteImportBulkAction.
  ///
  /// In en, this message translates to:
  /// **'Bulk import GPX/KML/KMZ files'**
  String get settingsRouteImportBulkAction;

  /// No description provided for @settingsRouteImporting.
  ///
  /// In en, this message translates to:
  /// **'Importing routes...'**
  String get settingsRouteImporting;

  /// No description provided for @settingsRouteImportProgress.
  ///
  /// In en, this message translates to:
  /// **'File {arg0}/{arg1}. Imported {arg2}, failed {arg3}.'**
  String settingsRouteImportProgress(int arg0, int arg1, int arg2, int arg3);

  /// No description provided for @settingsRouteImportResult.
  ///
  /// In en, this message translates to:
  /// **'Imported {arg0}. Failed {arg1}. Selected {arg2}.'**
  String settingsRouteImportResult(int arg0, int arg1, int arg2);

  /// No description provided for @settingsRouteImportError.
  ///
  /// In en, this message translates to:
  /// **'Route import warning: {arg0}'**
  String settingsRouteImportError(String arg0);

  /// No description provided for @settingsFitImportTitle.
  ///
  /// In en, this message translates to:
  /// **'FIT Importer'**
  String get settingsFitImportTitle;

  /// No description provided for @settingsFitImportBody.
  ///
  /// In en, this message translates to:
  /// **'Import FIT activity, course, or workout files, review detected details, and choose whether to save them to Health Connect.'**
  String get settingsFitImportBody;

  /// No description provided for @settingsFitImportAction.
  ///
  /// In en, this message translates to:
  /// **'Import FIT file'**
  String get settingsFitImportAction;

  /// No description provided for @settingsOfflineMapsTitle.
  ///
  /// In en, this message translates to:
  /// **'Offline maps'**
  String get settingsOfflineMapsTitle;

  /// No description provided for @settingsOfflineMapsBody.
  ///
  /// In en, this message translates to:
  /// **'Import PMTiles or Mapsforge .map/.maps packs for fully offline activity maps. Protomaps-compatible PMTiles basemaps and Mapsforge maps are supported.'**
  String get settingsOfflineMapsBody;

  /// No description provided for @settingsOfflineMapsEmpty.
  ///
  /// In en, this message translates to:
  /// **'No offline maps imported yet.'**
  String get settingsOfflineMapsEmpty;

  /// No description provided for @settingsOfflineMapsFormatPmtiles.
  ///
  /// In en, this message translates to:
  /// **'PMTiles'**
  String get settingsOfflineMapsFormatPmtiles;

  /// No description provided for @settingsOfflineMapsFormatMapsforge.
  ///
  /// In en, this message translates to:
  /// **'Mapsforge'**
  String get settingsOfflineMapsFormatMapsforge;

  /// No description provided for @settingsOfflineMapsRenderFormatTitle.
  ///
  /// In en, this message translates to:
  /// **'Render format'**
  String get settingsOfflineMapsRenderFormatTitle;

  /// No description provided for @settingsOfflineMapsRenderFormatOption.
  ///
  /// In en, this message translates to:
  /// **'{arg0} ({arg1})'**
  String settingsOfflineMapsRenderFormatOption(String arg0, int arg1);

  /// No description provided for @settingsOfflineMapsRenderFormatBody.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals renders every imported pack in the selected format together.'**
  String get settingsOfflineMapsRenderFormatBody;

  /// No description provided for @settingsOfflineMapsPackDetail.
  ///
  /// In en, this message translates to:
  /// **'{arg0} • {arg1} • {arg2}'**
  String settingsOfflineMapsPackDetail(String arg0, String arg1, String arg2);

  /// No description provided for @settingsOfflineMapsImportAction.
  ///
  /// In en, this message translates to:
  /// **'Import offline map'**
  String get settingsOfflineMapsImportAction;

  /// No description provided for @settingsOfflineMapsImporting.
  ///
  /// In en, this message translates to:
  /// **'Importing...'**
  String get settingsOfflineMapsImporting;

  /// No description provided for @settingsOfflineMapsImportProgressQueued.
  ///
  /// In en, this message translates to:
  /// **'Queued'**
  String get settingsOfflineMapsImportProgressQueued;

  /// No description provided for @settingsOfflineMapsImportProgressCopying.
  ///
  /// In en, this message translates to:
  /// **'Copying map'**
  String get settingsOfflineMapsImportProgressCopying;

  /// No description provided for @settingsOfflineMapsImportProgressComplete.
  ///
  /// In en, this message translates to:
  /// **'Complete'**
  String get settingsOfflineMapsImportProgressComplete;

  /// No description provided for @settingsOfflineMapsImportProgress.
  ///
  /// In en, this message translates to:
  /// **'{arg0}'**
  String settingsOfflineMapsImportProgress(String arg0);

  /// No description provided for @settingsOfflineMapsImportProgressWithPercent.
  ///
  /// In en, this message translates to:
  /// **'{arg0} • {arg1}%'**
  String settingsOfflineMapsImportProgressWithPercent(String arg0, int arg1);

  /// No description provided for @settingsOfflineMapsImportBackground.
  ///
  /// In en, this message translates to:
  /// **'Import continues in the background while you leave the app.'**
  String get settingsOfflineMapsImportBackground;

  /// No description provided for @settingsOfflineMapsImportResult.
  ///
  /// In en, this message translates to:
  /// **'Imported {arg0} ({arg1}).'**
  String settingsOfflineMapsImportResult(String arg0, String arg1);

  /// No description provided for @settingsOfflineMapsImportError.
  ///
  /// In en, this message translates to:
  /// **'Map import failed: {arg0}'**
  String settingsOfflineMapsImportError(String arg0);

  /// No description provided for @settingsOfflineMapsImportNotificationChannel.
  ///
  /// In en, this message translates to:
  /// **'Offline map imports'**
  String get settingsOfflineMapsImportNotificationChannel;

  /// No description provided for @settingsOfflineMapsImportNotificationTitle.
  ///
  /// In en, this message translates to:
  /// **'Importing offline map'**
  String get settingsOfflineMapsImportNotificationTitle;

  /// No description provided for @settingsOfflineMapsImportNotificationText.
  ///
  /// In en, this message translates to:
  /// **'{arg0}.'**
  String settingsOfflineMapsImportNotificationText(String arg0);

  /// No description provided for @settingsOfflineMapsImportNotificationTextWithPercent.
  ///
  /// In en, this message translates to:
  /// **'{arg0} • {arg1}%.'**
  String settingsOfflineMapsImportNotificationTextWithPercent(
    String arg0,
    int arg1,
  );

  /// No description provided for @settingsOfflineMapsHelpPrompt.
  ///
  /// In en, this message translates to:
  /// **'Do you want to learn how to add offline maps? Go to:'**
  String get settingsOfflineMapsHelpPrompt;

  /// No description provided for @settingsOfflineMapsHelpLink.
  ///
  /// In en, this message translates to:
  /// **'Open offline maps guide'**
  String get settingsOfflineMapsHelpLink;

  /// No description provided for @sectionSupport.
  ///
  /// In en, this message translates to:
  /// **'Support'**
  String get sectionSupport;

  /// No description provided for @settingsSupportTitle.
  ///
  /// In en, this message translates to:
  /// **'Support OpenVitals'**
  String get settingsSupportTitle;

  /// No description provided for @settingsSupportBody.
  ///
  /// In en, this message translates to:
  /// **'Report bugs, join community support discussions, or help fund ongoing development.'**
  String get settingsSupportBody;

  /// No description provided for @settingsSupportIssuesAction.
  ///
  /// In en, this message translates to:
  /// **'Report an issue'**
  String get settingsSupportIssuesAction;

  /// No description provided for @settingsSupportDiscussionAction.
  ///
  /// In en, this message translates to:
  /// **'Join Zulip discussions'**
  String get settingsSupportDiscussionAction;

  /// No description provided for @settingsSupportAction.
  ///
  /// In en, this message translates to:
  /// **'Open Liberapay'**
  String get settingsSupportAction;

  /// No description provided for @crashReportEmailChooserTitle.
  ///
  /// In en, this message translates to:
  /// **'Email OpenVitals report'**
  String get crashReportEmailChooserTitle;

  /// No description provided for @crashReportFallbackTitle.
  ///
  /// In en, this message translates to:
  /// **'No email app found'**
  String get crashReportFallbackTitle;

  /// No description provided for @crashReportFallbackBody.
  ///
  /// In en, this message translates to:
  /// **'Copy the report or save it as a text file, then send it to {arg0} later.'**
  String crashReportFallbackBody(String arg0);

  /// No description provided for @crashReportFallbackCopy.
  ///
  /// In en, this message translates to:
  /// **'Copy report'**
  String get crashReportFallbackCopy;

  /// No description provided for @crashReportFallbackSave.
  ///
  /// In en, this message translates to:
  /// **'Save text file'**
  String get crashReportFallbackSave;

  /// No description provided for @crashReportFallbackCopied.
  ///
  /// In en, this message translates to:
  /// **'Report copied.'**
  String get crashReportFallbackCopied;

  /// No description provided for @crashReportFallbackSaved.
  ///
  /// In en, this message translates to:
  /// **'Report saved.'**
  String get crashReportFallbackSaved;

  /// No description provided for @crashReportFallbackSaveFailed.
  ///
  /// In en, this message translates to:
  /// **'Could not save report.'**
  String get crashReportFallbackSaveFailed;

  /// No description provided for @crashReportFallbackSaveUnavailable.
  ///
  /// In en, this message translates to:
  /// **'No file saver found. Report copied.'**
  String get crashReportFallbackSaveUnavailable;

  /// No description provided for @crashReportClipboardLabel.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals report'**
  String get crashReportClipboardLabel;

  /// No description provided for @settingsPrivacyNoAccount.
  ///
  /// In en, this message translates to:
  /// **'No account required'**
  String get settingsPrivacyNoAccount;

  /// No description provided for @settingsPrivacyNoCloud.
  ///
  /// In en, this message translates to:
  /// **'No cloud sync of health data'**
  String get settingsPrivacyNoCloud;

  /// No description provided for @settingsPrivacyNoAnalytics.
  ///
  /// In en, this message translates to:
  /// **'No analytics SDK'**
  String get settingsPrivacyNoAnalytics;

  /// No description provided for @settingsPrivacyNoAds.
  ///
  /// In en, this message translates to:
  /// **'No ads or third-party tracking'**
  String get settingsPrivacyNoAds;

  /// No description provided for @settingsPrivacyOnDevice.
  ///
  /// In en, this message translates to:
  /// **'Data stays on your device'**
  String get settingsPrivacyOnDevice;

  /// No description provided for @settingsPrivacyReadOnly.
  ///
  /// In en, this message translates to:
  /// **'Read-only except entries you explicitly log'**
  String get settingsPrivacyReadOnly;

  /// No description provided for @settingsAppVersion.
  ///
  /// In en, this message translates to:
  /// **'Version {arg0} ({arg1})'**
  String settingsAppVersion(String arg0, int arg1);

  /// No description provided for @detailMetrics.
  ///
  /// In en, this message translates to:
  /// **'Metrics'**
  String get detailMetrics;

  /// No description provided for @detailSessionDetails.
  ///
  /// In en, this message translates to:
  /// **'Session details'**
  String get detailSessionDetails;

  /// No description provided for @detailDuration.
  ///
  /// In en, this message translates to:
  /// **'Duration'**
  String get detailDuration;

  /// No description provided for @detailMovingTime.
  ///
  /// In en, this message translates to:
  /// **'Moving time'**
  String get detailMovingTime;

  /// No description provided for @detailType.
  ///
  /// In en, this message translates to:
  /// **'Type'**
  String get detailType;

  /// No description provided for @detailStarted.
  ///
  /// In en, this message translates to:
  /// **'Started'**
  String get detailStarted;

  /// No description provided for @detailEnded.
  ///
  /// In en, this message translates to:
  /// **'Ended'**
  String get detailEnded;

  /// No description provided for @detailStartZone.
  ///
  /// In en, this message translates to:
  /// **'Start zone'**
  String get detailStartZone;

  /// No description provided for @detailEndZone.
  ///
  /// In en, this message translates to:
  /// **'End zone'**
  String get detailEndZone;

  /// No description provided for @detailRecording.
  ///
  /// In en, this message translates to:
  /// **'Recording'**
  String get detailRecording;

  /// No description provided for @detailSourcePackage.
  ///
  /// In en, this message translates to:
  /// **'Source package'**
  String get detailSourcePackage;

  /// No description provided for @detailDeviceType.
  ///
  /// In en, this message translates to:
  /// **'Device type'**
  String get detailDeviceType;

  /// No description provided for @detailDeviceMaker.
  ///
  /// In en, this message translates to:
  /// **'Device maker'**
  String get detailDeviceMaker;

  /// No description provided for @detailDeviceModel.
  ///
  /// In en, this message translates to:
  /// **'Device model'**
  String get detailDeviceModel;

  /// No description provided for @detailLastModified.
  ///
  /// In en, this message translates to:
  /// **'Last modified'**
  String get detailLastModified;

  /// No description provided for @detailRecordId.
  ///
  /// In en, this message translates to:
  /// **'Record id'**
  String get detailRecordId;

  /// No description provided for @detailClientRecordId.
  ///
  /// In en, this message translates to:
  /// **'Client record id'**
  String get detailClientRecordId;

  /// No description provided for @detailClientVersion.
  ///
  /// In en, this message translates to:
  /// **'Client version'**
  String get detailClientVersion;

  /// No description provided for @detailPlannedSessionId.
  ///
  /// In en, this message translates to:
  /// **'Planned session id'**
  String get detailPlannedSessionId;

  /// No description provided for @detailNotes.
  ///
  /// In en, this message translates to:
  /// **'Notes'**
  String get detailNotes;

  /// No description provided for @detailTitle.
  ///
  /// In en, this message translates to:
  /// **'Title'**
  String get detailTitle;

  /// No description provided for @detailTime.
  ///
  /// In en, this message translates to:
  /// **'Time'**
  String get detailTime;

  /// No description provided for @detailRepetitions.
  ///
  /// In en, this message translates to:
  /// **'Repetitions'**
  String get detailRepetitions;

  /// No description provided for @detailSet.
  ///
  /// In en, this message translates to:
  /// **'Set'**
  String get detailSet;

  /// No description provided for @detailLength.
  ///
  /// In en, this message translates to:
  /// **'Length'**
  String get detailLength;

  /// No description provided for @detailSegments.
  ///
  /// In en, this message translates to:
  /// **'Segments'**
  String get detailSegments;

  /// No description provided for @detailLaps.
  ///
  /// In en, this message translates to:
  /// **'Laps'**
  String get detailLaps;

  /// No description provided for @detailLap.
  ///
  /// In en, this message translates to:
  /// **'Lap {arg0}'**
  String detailLap(int arg0);

  /// No description provided for @detailRoute.
  ///
  /// In en, this message translates to:
  /// **'Route'**
  String get detailRoute;

  /// No description provided for @detailStatus.
  ///
  /// In en, this message translates to:
  /// **'Status'**
  String get detailStatus;

  /// No description provided for @detailStatusAvailable.
  ///
  /// In en, this message translates to:
  /// **'Available'**
  String get detailStatusAvailable;

  /// No description provided for @detailPoints.
  ///
  /// In en, this message translates to:
  /// **'Points'**
  String get detailPoints;

  /// No description provided for @detailStartPoint.
  ///
  /// In en, this message translates to:
  /// **'Start point'**
  String get detailStartPoint;

  /// No description provided for @detailEndPoint.
  ///
  /// In en, this message translates to:
  /// **'End point'**
  String get detailEndPoint;

  /// No description provided for @detailAltitude.
  ///
  /// In en, this message translates to:
  /// **'Altitude {arg0}'**
  String detailAltitude(String arg0);

  /// No description provided for @detailHorizontalAccuracy.
  ///
  /// In en, this message translates to:
  /// **'Horizontal accuracy {arg0}'**
  String detailHorizontalAccuracy(String arg0);

  /// No description provided for @detailVerticalAccuracy.
  ///
  /// In en, this message translates to:
  /// **'Vertical accuracy {arg0}'**
  String detailVerticalAccuracy(String arg0);

  /// No description provided for @detailStageEvents.
  ///
  /// In en, this message translates to:
  /// **'Stage events'**
  String get detailStageEvents;

  /// No description provided for @detailStages.
  ///
  /// In en, this message translates to:
  /// **'Stages'**
  String get detailStages;

  /// No description provided for @detailSleepSession.
  ///
  /// In en, this message translates to:
  /// **'Sleep session'**
  String get detailSleepSession;

  /// No description provided for @recordingActivelyRecorded.
  ///
  /// In en, this message translates to:
  /// **'Actively recorded'**
  String get recordingActivelyRecorded;

  /// No description provided for @recordingAutomaticallyRecorded.
  ///
  /// In en, this message translates to:
  /// **'Automatically recorded'**
  String get recordingAutomaticallyRecorded;

  /// No description provided for @recordingManualEntry.
  ///
  /// In en, this message translates to:
  /// **'Manual entry'**
  String get recordingManualEntry;

  /// No description provided for @recordingUnknown.
  ///
  /// In en, this message translates to:
  /// **'Unknown'**
  String get recordingUnknown;

  /// No description provided for @deviceWatch.
  ///
  /// In en, this message translates to:
  /// **'Watch'**
  String get deviceWatch;

  /// No description provided for @devicePhone.
  ///
  /// In en, this message translates to:
  /// **'Phone'**
  String get devicePhone;

  /// No description provided for @deviceScale.
  ///
  /// In en, this message translates to:
  /// **'Scale'**
  String get deviceScale;

  /// No description provided for @deviceRing.
  ///
  /// In en, this message translates to:
  /// **'Ring'**
  String get deviceRing;

  /// No description provided for @deviceHeadMounted.
  ///
  /// In en, this message translates to:
  /// **'Head-mounted'**
  String get deviceHeadMounted;

  /// No description provided for @deviceFitnessBand.
  ///
  /// In en, this message translates to:
  /// **'Fitness band'**
  String get deviceFitnessBand;

  /// No description provided for @deviceChestStrap.
  ///
  /// In en, this message translates to:
  /// **'Chest strap'**
  String get deviceChestStrap;

  /// No description provided for @deviceSmartDisplay.
  ///
  /// In en, this message translates to:
  /// **'Smart display'**
  String get deviceSmartDisplay;

  /// No description provided for @sleepStageAwake.
  ///
  /// In en, this message translates to:
  /// **'Awake'**
  String get sleepStageAwake;

  /// No description provided for @sleepStageSleeping.
  ///
  /// In en, this message translates to:
  /// **'Sleeping'**
  String get sleepStageSleeping;

  /// No description provided for @sleepStageOutOfBed.
  ///
  /// In en, this message translates to:
  /// **'Out of bed'**
  String get sleepStageOutOfBed;

  /// No description provided for @sleepStageLight.
  ///
  /// In en, this message translates to:
  /// **'Light'**
  String get sleepStageLight;

  /// No description provided for @sleepStageDeep.
  ///
  /// In en, this message translates to:
  /// **'Deep'**
  String get sleepStageDeep;

  /// No description provided for @sleepStageRem.
  ///
  /// In en, this message translates to:
  /// **'REM'**
  String get sleepStageRem;

  /// No description provided for @sleepStageAwakeInBed.
  ///
  /// In en, this message translates to:
  /// **'Awake in bed'**
  String get sleepStageAwakeInBed;

  /// No description provided for @sleepStageUnknown.
  ///
  /// In en, this message translates to:
  /// **'Unknown'**
  String get sleepStageUnknown;

  /// No description provided for @sleepStagesShareTitle.
  ///
  /// In en, this message translates to:
  /// **'Share of time in bed'**
  String get sleepStagesShareTitle;

  /// No description provided for @cyclePermissionsMissingTitle.
  ///
  /// In en, this message translates to:
  /// **'Cycle permissions missing'**
  String get cyclePermissionsMissingTitle;

  /// No description provided for @cyclePermissionsMissingBody.
  ///
  /// In en, this message translates to:
  /// **'Grant cycle tracking permissions to show period days, ovulation tests, cervical mucus, and basal temperature.'**
  String get cyclePermissionsMissingBody;

  /// No description provided for @cycleObservationMenstruationPeriod.
  ///
  /// In en, this message translates to:
  /// **'Menstruation period'**
  String get cycleObservationMenstruationPeriod;

  /// No description provided for @cycleObservationMenstruationFlow.
  ///
  /// In en, this message translates to:
  /// **'Menstruation flow'**
  String get cycleObservationMenstruationFlow;

  /// No description provided for @cycleObservationOvulationTest.
  ///
  /// In en, this message translates to:
  /// **'Ovulation test'**
  String get cycleObservationOvulationTest;

  /// No description provided for @cycleObservationCervicalMucus.
  ///
  /// In en, this message translates to:
  /// **'Cervical mucus'**
  String get cycleObservationCervicalMucus;

  /// No description provided for @cycleObservationBasalBodyTemperature.
  ///
  /// In en, this message translates to:
  /// **'Basal body temperature'**
  String get cycleObservationBasalBodyTemperature;

  /// No description provided for @cycleObservationIntermenstrualBleeding.
  ///
  /// In en, this message translates to:
  /// **'Intermenstrual bleeding'**
  String get cycleObservationIntermenstrualBleeding;

  /// No description provided for @cycleObservationSexualActivity.
  ///
  /// In en, this message translates to:
  /// **'Sexual activity'**
  String get cycleObservationSexualActivity;

  /// No description provided for @cycleProtectionProtected.
  ///
  /// In en, this message translates to:
  /// **'Protected'**
  String get cycleProtectionProtected;

  /// No description provided for @cycleProtectionUnprotected.
  ///
  /// In en, this message translates to:
  /// **'Unprotected'**
  String get cycleProtectionUnprotected;

  /// No description provided for @cycleProtectionUnknown.
  ///
  /// In en, this message translates to:
  /// **'Protection unknown'**
  String get cycleProtectionUnknown;

  /// No description provided for @cycleBasalTemperatureValue.
  ///
  /// In en, this message translates to:
  /// **'%1\$.1f C · {arg1}'**
  String cycleBasalTemperatureValue(String arg1);

  /// No description provided for @cycleDaysValue.
  ///
  /// In en, this message translates to:
  /// **'{arg0} {arg1}'**
  String cycleDaysValue(int arg0, String arg1);

  /// No description provided for @cycleDaySingular.
  ///
  /// In en, this message translates to:
  /// **'day'**
  String get cycleDaySingular;

  /// No description provided for @cycleDayPlural.
  ///
  /// In en, this message translates to:
  /// **'days'**
  String get cycleDayPlural;

  /// No description provided for @cycleFlowLight.
  ///
  /// In en, this message translates to:
  /// **'Light'**
  String get cycleFlowLight;

  /// No description provided for @cycleFlowMedium.
  ///
  /// In en, this message translates to:
  /// **'Medium'**
  String get cycleFlowMedium;

  /// No description provided for @cycleFlowHeavy.
  ///
  /// In en, this message translates to:
  /// **'Heavy'**
  String get cycleFlowHeavy;

  /// No description provided for @cycleOvulationPositive.
  ///
  /// In en, this message translates to:
  /// **'Positive'**
  String get cycleOvulationPositive;

  /// No description provided for @cycleOvulationHigh.
  ///
  /// In en, this message translates to:
  /// **'High'**
  String get cycleOvulationHigh;

  /// No description provided for @cycleOvulationNegative.
  ///
  /// In en, this message translates to:
  /// **'Negative'**
  String get cycleOvulationNegative;

  /// No description provided for @cycleOvulationInconclusive.
  ///
  /// In en, this message translates to:
  /// **'Inconclusive'**
  String get cycleOvulationInconclusive;

  /// No description provided for @cycleMucusDry.
  ///
  /// In en, this message translates to:
  /// **'Dry'**
  String get cycleMucusDry;

  /// No description provided for @cycleMucusSticky.
  ///
  /// In en, this message translates to:
  /// **'Sticky'**
  String get cycleMucusSticky;

  /// No description provided for @cycleMucusCreamy.
  ///
  /// In en, this message translates to:
  /// **'Creamy'**
  String get cycleMucusCreamy;

  /// No description provided for @cycleMucusWatery.
  ///
  /// In en, this message translates to:
  /// **'Watery'**
  String get cycleMucusWatery;

  /// No description provided for @cycleMucusEggWhite.
  ///
  /// In en, this message translates to:
  /// **'Egg white'**
  String get cycleMucusEggWhite;

  /// No description provided for @cycleMucusUnusual.
  ///
  /// In en, this message translates to:
  /// **'Unusual'**
  String get cycleMucusUnusual;

  /// No description provided for @cycleMucusLight.
  ///
  /// In en, this message translates to:
  /// **'light'**
  String get cycleMucusLight;

  /// No description provided for @cycleMucusMedium.
  ///
  /// In en, this message translates to:
  /// **'medium'**
  String get cycleMucusMedium;

  /// No description provided for @cycleMucusHeavy.
  ///
  /// In en, this message translates to:
  /// **'heavy'**
  String get cycleMucusHeavy;

  /// No description provided for @cycleMucusValue.
  ///
  /// In en, this message translates to:
  /// **'{arg0}, {arg1}'**
  String cycleMucusValue(String arg0, String arg1);

  /// No description provided for @measurementLocationArmpit.
  ///
  /// In en, this message translates to:
  /// **'Armpit'**
  String get measurementLocationArmpit;

  /// No description provided for @measurementLocationFinger.
  ///
  /// In en, this message translates to:
  /// **'Finger'**
  String get measurementLocationFinger;

  /// No description provided for @measurementLocationForehead.
  ///
  /// In en, this message translates to:
  /// **'Forehead'**
  String get measurementLocationForehead;

  /// No description provided for @measurementLocationMouth.
  ///
  /// In en, this message translates to:
  /// **'Mouth'**
  String get measurementLocationMouth;

  /// No description provided for @measurementLocationRectum.
  ///
  /// In en, this message translates to:
  /// **'Rectum'**
  String get measurementLocationRectum;

  /// No description provided for @measurementLocationTemporalArtery.
  ///
  /// In en, this message translates to:
  /// **'Temporal artery'**
  String get measurementLocationTemporalArtery;

  /// No description provided for @measurementLocationToe.
  ///
  /// In en, this message translates to:
  /// **'Toe'**
  String get measurementLocationToe;

  /// No description provided for @measurementLocationEar.
  ///
  /// In en, this message translates to:
  /// **'Ear'**
  String get measurementLocationEar;

  /// No description provided for @measurementLocationWrist.
  ///
  /// In en, this message translates to:
  /// **'Wrist'**
  String get measurementLocationWrist;

  /// No description provided for @measurementLocationVagina.
  ///
  /// In en, this message translates to:
  /// **'Vagina'**
  String get measurementLocationVagina;

  /// No description provided for @measurementLocationUnknown.
  ///
  /// In en, this message translates to:
  /// **'Measurement location unknown'**
  String get measurementLocationUnknown;

  /// No description provided for @weekdayMondayShort.
  ///
  /// In en, this message translates to:
  /// **'M'**
  String get weekdayMondayShort;

  /// No description provided for @weekdayTuesdayShort.
  ///
  /// In en, this message translates to:
  /// **'T'**
  String get weekdayTuesdayShort;

  /// No description provided for @weekdayWednesdayShort.
  ///
  /// In en, this message translates to:
  /// **'W'**
  String get weekdayWednesdayShort;

  /// No description provided for @weekdayThursdayShort.
  ///
  /// In en, this message translates to:
  /// **'T'**
  String get weekdayThursdayShort;

  /// No description provided for @weekdayFridayShort.
  ///
  /// In en, this message translates to:
  /// **'F'**
  String get weekdayFridayShort;

  /// No description provided for @weekdaySaturdayShort.
  ///
  /// In en, this message translates to:
  /// **'S'**
  String get weekdaySaturdayShort;

  /// No description provided for @weekdaySundayShort.
  ///
  /// In en, this message translates to:
  /// **'S'**
  String get weekdaySundayShort;

  /// No description provided for @vitalsPermissionsNeededTitle.
  ///
  /// In en, this message translates to:
  /// **'Vitals permissions needed'**
  String get vitalsPermissionsNeededTitle;

  /// No description provided for @vitalsPermissionsNeededBody.
  ///
  /// In en, this message translates to:
  /// **'Grant blood pressure, oxygen saturation, respiratory rate, temperature, VO2 max, and glucose permissions to fill this screen.'**
  String get vitalsPermissionsNeededBody;

  /// No description provided for @vitalsRespiratoryRateReadings.
  ///
  /// In en, this message translates to:
  /// **'Respiratory rate readings'**
  String get vitalsRespiratoryRateReadings;

  /// No description provided for @vitalsBodyTemperatureReadings.
  ///
  /// In en, this message translates to:
  /// **'Body temperature readings'**
  String get vitalsBodyTemperatureReadings;

  /// No description provided for @heartRateHealthChecksTitle.
  ///
  /// In en, this message translates to:
  /// **'Heart rate checks'**
  String get heartRateHealthChecksTitle;

  /// No description provided for @heartRateHighTitle.
  ///
  /// In en, this message translates to:
  /// **'High heart rate'**
  String get heartRateHighTitle;

  /// No description provided for @heartRateLowTitle.
  ///
  /// In en, this message translates to:
  /// **'Low heart rate'**
  String get heartRateLowTitle;

  /// No description provided for @heartRateSamplesAtOrAbove.
  ///
  /// In en, this message translates to:
  /// **'Samples at/above {arg0} bpm'**
  String heartRateSamplesAtOrAbove(int arg0);

  /// No description provided for @heartRateSamplesAtOrBelow.
  ///
  /// In en, this message translates to:
  /// **'Samples at/below {arg0} bpm'**
  String heartRateSamplesAtOrBelow(int arg0);

  /// No description provided for @heartRateDaysAtOrAbove.
  ///
  /// In en, this message translates to:
  /// **'Days at/above {arg0} bpm'**
  String heartRateDaysAtOrAbove(int arg0);

  /// No description provided for @heartRateDaysAtOrBelow.
  ///
  /// In en, this message translates to:
  /// **'Days at/below {arg0} bpm'**
  String heartRateDaysAtOrBelow(int arg0);

  /// No description provided for @cdDecreaseHrThreshold.
  ///
  /// In en, this message translates to:
  /// **'Decrease heart rate threshold'**
  String get cdDecreaseHrThreshold;

  /// No description provided for @cdIncreaseHrThreshold.
  ///
  /// In en, this message translates to:
  /// **'Increase heart rate threshold'**
  String get cdIncreaseHrThreshold;

  /// No description provided for @mealBreakfast.
  ///
  /// In en, this message translates to:
  /// **'Breakfast'**
  String get mealBreakfast;

  /// No description provided for @mealLunch.
  ///
  /// In en, this message translates to:
  /// **'Lunch'**
  String get mealLunch;

  /// No description provided for @mealDinner.
  ///
  /// In en, this message translates to:
  /// **'Dinner'**
  String get mealDinner;

  /// No description provided for @mealSnack.
  ///
  /// In en, this message translates to:
  /// **'Snack'**
  String get mealSnack;

  /// No description provided for @mealGeneric.
  ///
  /// In en, this message translates to:
  /// **'Meal'**
  String get mealGeneric;

  /// No description provided for @macroProteinShort.
  ///
  /// In en, this message translates to:
  /// **'P {arg0}g'**
  String macroProteinShort(String arg0);

  /// No description provided for @macroCarbsShort.
  ///
  /// In en, this message translates to:
  /// **'C {arg0}g'**
  String macroCarbsShort(String arg0);

  /// No description provided for @macroFatShort.
  ///
  /// In en, this message translates to:
  /// **'F {arg0}g'**
  String macroFatShort(String arg0);

  /// No description provided for @macroFiber.
  ///
  /// In en, this message translates to:
  /// **'fiber {arg0}g'**
  String macroFiber(String arg0);

  /// No description provided for @macroSugar.
  ///
  /// In en, this message translates to:
  /// **'sugar {arg0}g'**
  String macroSugar(String arg0);

  /// No description provided for @caffeineSectionOverview.
  ///
  /// In en, this message translates to:
  /// **'Overview'**
  String get caffeineSectionOverview;

  /// No description provided for @caffeineSectionDashboard.
  ///
  /// In en, this message translates to:
  /// **'Dashboard'**
  String get caffeineSectionDashboard;

  /// No description provided for @caffeineSectionAnalytics.
  ///
  /// In en, this message translates to:
  /// **'Analytics'**
  String get caffeineSectionAnalytics;

  /// No description provided for @caffeineSectionSleep.
  ///
  /// In en, this message translates to:
  /// **'Sleep impact'**
  String get caffeineSectionSleep;

  /// No description provided for @caffeineSectionSources.
  ///
  /// In en, this message translates to:
  /// **'Sources'**
  String get caffeineSectionSources;

  /// No description provided for @caffeineSectionEntries.
  ///
  /// In en, this message translates to:
  /// **'Entries'**
  String get caffeineSectionEntries;

  /// No description provided for @caffeineSectionScience.
  ///
  /// In en, this message translates to:
  /// **'Science'**
  String get caffeineSectionScience;

  /// No description provided for @caffeineSetupTitle.
  ///
  /// In en, this message translates to:
  /// **'Personalize caffeine insights'**
  String get caffeineSetupTitle;

  /// No description provided for @caffeineSetupBody.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals found caffeine data. Personalization improves the caffeine curve and bedtime forecast.'**
  String get caffeineSetupBody;

  /// No description provided for @caffeineCurrentTitle.
  ///
  /// In en, this message translates to:
  /// **'Active caffeine'**
  String get caffeineCurrentTitle;

  /// No description provided for @caffeineTodayTotal.
  ///
  /// In en, this message translates to:
  /// **'Today total'**
  String get caffeineTodayTotal;

  /// No description provided for @caffeineTimeToSafe.
  ///
  /// In en, this message translates to:
  /// **'Time to safe'**
  String get caffeineTimeToSafe;

  /// No description provided for @caffeineSleepStatusUnlikely.
  ///
  /// In en, this message translates to:
  /// **'Sleep impact unlikely'**
  String get caffeineSleepStatusUnlikely;

  /// No description provided for @caffeineSleepStatusUnlikelyBody.
  ///
  /// In en, this message translates to:
  /// **'{arg0} active now, below your {arg1} sleep threshold.'**
  String caffeineSleepStatusUnlikelyBody(String arg0, String arg1);

  /// No description provided for @caffeineSleepStatusElevatedNow.
  ///
  /// In en, this message translates to:
  /// **'Elevated now'**
  String get caffeineSleepStatusElevatedNow;

  /// No description provided for @caffeineSleepStatusElevatedNowBody.
  ///
  /// In en, this message translates to:
  /// **'{arg0} active now. Estimated below threshold in {arg1}; bedtime forecast is {arg2} at {arg3}.'**
  String caffeineSleepStatusElevatedNowBody(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  );

  /// No description provided for @caffeineSleepStatusMayAffect.
  ///
  /// In en, this message translates to:
  /// **'May affect sleep'**
  String get caffeineSleepStatusMayAffect;

  /// No description provided for @caffeineSleepStatusMayAffectBody.
  ///
  /// In en, this message translates to:
  /// **'Bedtime forecast is {arg0} at {arg1}, above your {arg2} threshold.'**
  String caffeineSleepStatusMayAffectBody(
    String arg0,
    String arg1,
    String arg2,
  );

  /// No description provided for @caffeinePeriodTotal.
  ///
  /// In en, this message translates to:
  /// **'Period total'**
  String get caffeinePeriodTotal;

  /// No description provided for @caffeineDailyAverage.
  ///
  /// In en, this message translates to:
  /// **'Daily average'**
  String get caffeineDailyAverage;

  /// No description provided for @caffeineLoggedDays.
  ///
  /// In en, this message translates to:
  /// **'Logged days'**
  String get caffeineLoggedDays;

  /// No description provided for @caffeinePeakDay.
  ///
  /// In en, this message translates to:
  /// **'Peak day'**
  String get caffeinePeakDay;

  /// No description provided for @caffeinePeakDayValue.
  ///
  /// In en, this message translates to:
  /// **'{arg0} - {arg1}'**
  String caffeinePeakDayValue(String arg0, String arg1);

  /// No description provided for @caffeineCurveTitle.
  ///
  /// In en, this message translates to:
  /// **'Caffeine curve'**
  String get caffeineCurveTitle;

  /// No description provided for @caffeineThresholdLine.
  ///
  /// In en, this message translates to:
  /// **'Sleep threshold {arg0}'**
  String caffeineThresholdLine(String arg0);

  /// No description provided for @caffeineBedtimeForecast.
  ///
  /// In en, this message translates to:
  /// **'Bedtime forecast'**
  String get caffeineBedtimeForecast;

  /// No description provided for @caffeineBedtimeSummary.
  ///
  /// In en, this message translates to:
  /// **'At {arg0} with threshold {arg1}'**
  String caffeineBedtimeSummary(String arg0, String arg1);

  /// No description provided for @caffeineSafeNights.
  ///
  /// In en, this message translates to:
  /// **'Safe nights'**
  String get caffeineSafeNights;

  /// No description provided for @caffeineSafeStreak.
  ///
  /// In en, this message translates to:
  /// **'Safe streak'**
  String get caffeineSafeStreak;

  /// No description provided for @caffeineTopSource.
  ///
  /// In en, this message translates to:
  /// **'Top source'**
  String get caffeineTopSource;

  /// No description provided for @caffeineSleepThreshold.
  ///
  /// In en, this message translates to:
  /// **'Sleep threshold'**
  String get caffeineSleepThreshold;

  /// No description provided for @caffeineDailyImpact.
  ///
  /// In en, this message translates to:
  /// **'Daily and bedtime impact'**
  String get caffeineDailyImpact;

  /// No description provided for @caffeineSafeCalendar.
  ///
  /// In en, this message translates to:
  /// **'Safe-night calendar'**
  String get caffeineSafeCalendar;

  /// No description provided for @caffeineSources.
  ///
  /// In en, this message translates to:
  /// **'Source apps'**
  String get caffeineSources;

  /// No description provided for @caffeineItems.
  ///
  /// In en, this message translates to:
  /// **'Items'**
  String get caffeineItems;

  /// No description provided for @caffeineInferredCategories.
  ///
  /// In en, this message translates to:
  /// **'Inferred categories'**
  String get caffeineInferredCategories;

  /// No description provided for @caffeineTimeOfDay.
  ///
  /// In en, this message translates to:
  /// **'Time of day'**
  String get caffeineTimeOfDay;

  /// No description provided for @caffeineEntry.
  ///
  /// In en, this message translates to:
  /// **'Caffeine entry'**
  String get caffeineEntry;

  /// No description provided for @caffeineInferredCategory.
  ///
  /// In en, this message translates to:
  /// **'Category: {arg0}'**
  String caffeineInferredCategory(String arg0);

  /// No description provided for @caffeineCatalogMatch.
  ///
  /// In en, this message translates to:
  /// **'Catalog: {arg0}'**
  String caffeineCatalogMatch(String arg0);

  /// No description provided for @caffeineCategory.
  ///
  /// In en, this message translates to:
  /// **'Category'**
  String get caffeineCategory;

  /// No description provided for @caffeineCatalog.
  ///
  /// In en, this message translates to:
  /// **'Catalog'**
  String get caffeineCatalog;

  /// No description provided for @caffeineCatalogMatchDetail.
  ///
  /// In en, this message translates to:
  /// **'{arg0}, typical {arg1}, {arg2} match'**
  String caffeineCatalogMatchDetail(String arg0, String arg1, String arg2);

  /// No description provided for @caffeineHealthConnectSourceLabel.
  ///
  /// In en, this message translates to:
  /// **'Source'**
  String get caffeineHealthConnectSourceLabel;

  /// No description provided for @caffeineHealthConnectMealLabel.
  ///
  /// In en, this message translates to:
  /// **'Meal'**
  String get caffeineHealthConnectMealLabel;

  /// No description provided for @caffeineHealthConnectDurationLabel.
  ///
  /// In en, this message translates to:
  /// **'Duration'**
  String get caffeineHealthConnectDurationLabel;

  /// No description provided for @caffeineCurrentContribution.
  ///
  /// In en, this message translates to:
  /// **'{arg0} active'**
  String caffeineCurrentContribution(String arg0);

  /// No description provided for @caffeineCurrentContributionLabel.
  ///
  /// In en, this message translates to:
  /// **'Current'**
  String get caffeineCurrentContributionLabel;

  /// No description provided for @caffeineDose.
  ///
  /// In en, this message translates to:
  /// **'Dose'**
  String get caffeineDose;

  /// No description provided for @caffeinePeak.
  ///
  /// In en, this message translates to:
  /// **'Peak'**
  String get caffeinePeak;

  /// No description provided for @caffeinePeakTime.
  ///
  /// In en, this message translates to:
  /// **'Peak time'**
  String get caffeinePeakTime;

  /// No description provided for @caffeineContributionCurve.
  ///
  /// In en, this message translates to:
  /// **'Contribution curve'**
  String get caffeineContributionCurve;

  /// No description provided for @caffeineEmpty.
  ///
  /// In en, this message translates to:
  /// **'No caffeine entries for this period. Caffeinated drinks added through hydration or nutrition will appear here when Health Connect includes caffeine.'**
  String get caffeineEmpty;

  /// No description provided for @caffeineScienceTitle.
  ///
  /// In en, this message translates to:
  /// **'How the estimate works'**
  String get caffeineScienceTitle;

  /// No description provided for @caffeineScienceBody.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals reads caffeine from Health Connect nutrition records in milligrams, then estimates absorption across your configured absorption window and exponential elimination from your personalized half-life.'**
  String get caffeineScienceBody;

  /// No description provided for @caffeineScienceMeasurements.
  ///
  /// In en, this message translates to:
  /// **'Measurements used'**
  String get caffeineScienceMeasurements;

  /// No description provided for @caffeineScienceMeasurementsBody.
  ///
  /// In en, this message translates to:
  /// **'The recorded dose always comes from Health Connect. Start/end time, entry name, meal type, and data-origin package are used for timing, matching, and analytics labels. Catalog matches only annotate entries; they never replace the recorded dose.'**
  String get caffeineScienceMeasurementsBody;

  /// No description provided for @caffeineScienceLimits.
  ///
  /// In en, this message translates to:
  /// **'This is a practical population model, not medical advice. Pregnancy, medications, liver disease, genetics, smoking, alcohol, sensitivity, and habituation can all shift caffeine response.'**
  String get caffeineScienceLimits;

  /// No description provided for @caffeineReferencesTitle.
  ///
  /// In en, this message translates to:
  /// **'Research and references'**
  String get caffeineReferencesTitle;

  /// No description provided for @caffeineReferenceDrake.
  ///
  /// In en, this message translates to:
  /// **'Caffeine timing and sleep, Drake 2013'**
  String get caffeineReferenceDrake;

  /// No description provided for @caffeineReferenceNehlig.
  ///
  /// In en, this message translates to:
  /// **'Individual caffeine metabolism, Nehlig 2018'**
  String get caffeineReferenceNehlig;

  /// No description provided for @caffeineReferenceEfsa.
  ///
  /// In en, this message translates to:
  /// **'EFSA caffeine safety and sleep notes'**
  String get caffeineReferenceEfsa;

  /// No description provided for @caffeineReferenceHealthConnect.
  ///
  /// In en, this message translates to:
  /// **'Health Connect nutrition record fields'**
  String get caffeineReferenceHealthConnect;

  /// No description provided for @unknownSource.
  ///
  /// In en, this message translates to:
  /// **'Unknown source'**
  String get unknownSource;

  /// No description provided for @achievementsLegacyTitle.
  ///
  /// In en, this message translates to:
  /// **'Legacy activity badges'**
  String get achievementsLegacyTitle;

  /// No description provided for @achievementsProgressSummary.
  ///
  /// In en, this message translates to:
  /// **'{arg0} of {arg1} unlocked'**
  String achievementsProgressSummary(int arg0, int arg1);

  /// No description provided for @achievementsDataWindow.
  ///
  /// In en, this message translates to:
  /// **'{arg0} to {arg1} · {arg2} tracked days'**
  String achievementsDataWindow(String arg0, String arg1, String arg2);

  /// No description provided for @achievementsTrackedDays.
  ///
  /// In en, this message translates to:
  /// **'Tracked days'**
  String get achievementsTrackedDays;

  /// No description provided for @achievementsBestSteps.
  ///
  /// In en, this message translates to:
  /// **'Best steps'**
  String get achievementsBestSteps;

  /// No description provided for @achievementsTotalDistance.
  ///
  /// In en, this message translates to:
  /// **'Total distance'**
  String get achievementsTotalDistance;

  /// No description provided for @achievementsBestFloors.
  ///
  /// In en, this message translates to:
  /// **'Best floors'**
  String get achievementsBestFloors;

  /// No description provided for @achievementsTotalFloors.
  ///
  /// In en, this message translates to:
  /// **'Total floors'**
  String get achievementsTotalFloors;

  /// No description provided for @achievementsFilterAll.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get achievementsFilterAll;

  /// No description provided for @achievementsCategoryDailySteps.
  ///
  /// In en, this message translates to:
  /// **'Daily steps'**
  String get achievementsCategoryDailySteps;

  /// No description provided for @achievementsCategoryLifetimeDistance.
  ///
  /// In en, this message translates to:
  /// **'Lifetime distance'**
  String get achievementsCategoryLifetimeDistance;

  /// No description provided for @achievementsCategoryDailyFloors.
  ///
  /// In en, this message translates to:
  /// **'Daily floors'**
  String get achievementsCategoryDailyFloors;

  /// No description provided for @achievementsCategoryLifetimeFloors.
  ///
  /// In en, this message translates to:
  /// **'Lifetime floors'**
  String get achievementsCategoryLifetimeFloors;

  /// No description provided for @achievementsDailyStepsRequirement.
  ///
  /// In en, this message translates to:
  /// **'{arg0} steps in one day'**
  String achievementsDailyStepsRequirement(String arg0);

  /// No description provided for @achievementsLifetimeDistanceRequirement.
  ///
  /// In en, this message translates to:
  /// **'{arg0} total distance'**
  String achievementsLifetimeDistanceRequirement(String arg0);

  /// No description provided for @achievementsDailyFloorsRequirement.
  ///
  /// In en, this message translates to:
  /// **'{arg0} floors in one day'**
  String achievementsDailyFloorsRequirement(String arg0);

  /// No description provided for @achievementsLifetimeFloorsRequirement.
  ///
  /// In en, this message translates to:
  /// **'{arg0} total floors'**
  String achievementsLifetimeFloorsRequirement(String arg0);

  /// No description provided for @achievementsProgressValue.
  ///
  /// In en, this message translates to:
  /// **'{arg0} of {arg1}'**
  String achievementsProgressValue(String arg0, String arg1);

  /// No description provided for @achievementsAchievedOn.
  ///
  /// In en, this message translates to:
  /// **'Unlocked {arg0}'**
  String achievementsAchievedOn(String arg0);

  /// No description provided for @achievementsEarnedOnce.
  ///
  /// In en, this message translates to:
  /// **'Earned'**
  String get achievementsEarnedOnce;

  /// No description provided for @achievementsEarnedTimes.
  ///
  /// In en, this message translates to:
  /// **'{arg0} times'**
  String achievementsEarnedTimes(int arg0);

  /// No description provided for @achievementsLocked.
  ///
  /// In en, this message translates to:
  /// **'Locked'**
  String get achievementsLocked;

  /// No description provided for @achievementsNoDataTitle.
  ///
  /// In en, this message translates to:
  /// **'No activity history'**
  String get achievementsNoDataTitle;

  /// No description provided for @achievementsNoDataBody.
  ///
  /// In en, this message translates to:
  /// **'No step or distance records were returned from Health Connect. Check that activity data exists and that history access is granted for older records.'**
  String get achievementsNoDataBody;

  /// No description provided for @achievementsNoFloorDataTitle.
  ///
  /// In en, this message translates to:
  /// **'No floor data'**
  String get achievementsNoFloorDataTitle;

  /// No description provided for @achievementsNoFloorDataBody.
  ///
  /// In en, this message translates to:
  /// **'Floor badges unlock when Health Connect has floors climbed data.'**
  String get achievementsNoFloorDataBody;

  /// No description provided for @achievementsErrorTitle.
  ///
  /// In en, this message translates to:
  /// **'Achievements unavailable'**
  String get achievementsErrorTitle;

  /// No description provided for @dataConfidenceTitle.
  ///
  /// In en, this message translates to:
  /// **'Data confidence'**
  String get dataConfidenceTitle;

  /// No description provided for @dataConfidenceHigh.
  ///
  /// In en, this message translates to:
  /// **'High confidence'**
  String get dataConfidenceHigh;

  /// No description provided for @dataConfidenceMedium.
  ///
  /// In en, this message translates to:
  /// **'Medium confidence'**
  String get dataConfidenceMedium;

  /// No description provided for @dataConfidenceLow.
  ///
  /// In en, this message translates to:
  /// **'Low confidence'**
  String get dataConfidenceLow;

  /// No description provided for @dataConfidenceCoverage.
  ///
  /// In en, this message translates to:
  /// **'{arg0} of {arg1} days tracked ({arg2}%)'**
  String dataConfidenceCoverage(int arg0, int arg1, int arg2);

  /// No description provided for @dataConfidenceSamples.
  ///
  /// In en, this message translates to:
  /// **'{arg0} records'**
  String dataConfidenceSamples(int arg0);

  /// No description provided for @dataConfidenceSourceUnavailable.
  ///
  /// In en, this message translates to:
  /// **'Source details not available for this aggregate'**
  String get dataConfidenceSourceUnavailable;

  /// No description provided for @dataConfidenceSourceSingle.
  ///
  /// In en, this message translates to:
  /// **'Source: {arg0}'**
  String dataConfidenceSourceSingle(String arg0);

  /// No description provided for @dataConfidenceSourceMixed.
  ///
  /// In en, this message translates to:
  /// **'Mixed sources: {arg0}'**
  String dataConfidenceSourceMixed(String arg0);

  /// No description provided for @dataConfidenceKindMeasured.
  ///
  /// In en, this message translates to:
  /// **'Measured Health Connect records'**
  String get dataConfidenceKindMeasured;

  /// No description provided for @dataConfidenceKindAggregated.
  ///
  /// In en, this message translates to:
  /// **'Aggregated from Health Connect records'**
  String get dataConfidenceKindAggregated;

  /// No description provided for @dataConfidenceKindCalculated.
  ///
  /// In en, this message translates to:
  /// **'Calculated by OpenVitals'**
  String get dataConfidenceKindCalculated;

  /// No description provided for @dataConfidenceKindEstimated.
  ///
  /// In en, this message translates to:
  /// **'Estimated or derived value'**
  String get dataConfidenceKindEstimated;

  /// No description provided for @dataConfidenceKindMixed.
  ///
  /// In en, this message translates to:
  /// **'Mixed measured and calculated data'**
  String get dataConfidenceKindMixed;

  /// No description provided for @dataConfidenceWarningLowCoverage.
  ///
  /// In en, this message translates to:
  /// **'Missing days can weaken averages and trends.'**
  String get dataConfidenceWarningLowCoverage;

  /// No description provided for @dataConfidenceWarningSparse.
  ///
  /// In en, this message translates to:
  /// **'Sparse data: trends and statistics may be unstable.'**
  String get dataConfidenceWarningSparse;

  /// No description provided for @dataConfidenceWarningMixedSources.
  ///
  /// In en, this message translates to:
  /// **'Source changes may explain jumps or duplicated-looking data.'**
  String get dataConfidenceWarningMixedSources;

  /// No description provided for @dataConfidenceWarningManual.
  ///
  /// In en, this message translates to:
  /// **'Manual entries are included in this period.'**
  String get dataConfidenceWarningManual;

  /// No description provided for @dataConfidenceWarningCalculated.
  ///
  /// In en, this message translates to:
  /// **'This value is derived, not directly measured.'**
  String get dataConfidenceWarningCalculated;

  /// No description provided for @dataConfidenceWarningNoSources.
  ///
  /// In en, this message translates to:
  /// **'This aggregate does not expose source-level details.'**
  String get dataConfidenceWarningNoSources;

  /// No description provided for @settingsBodyEnergyGroupTitle.
  ///
  /// In en, this message translates to:
  /// **'Body Energy'**
  String get settingsBodyEnergyGroupTitle;

  /// No description provided for @settingsBodyEnergyGroupBody.
  ///
  /// In en, this message translates to:
  /// **'Calibration for estimated intraday energy and effort zones.'**
  String get settingsBodyEnergyGroupBody;

  /// No description provided for @bodyEnergyCalibrationTitle.
  ///
  /// In en, this message translates to:
  /// **'Turn on Body Energy'**
  String get bodyEnergyCalibrationTitle;

  /// No description provided for @bodyEnergyCalibrationBody.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals estimates drain from heart-rate intensity over time, using your age, weight, and heart rate from the Body profile in Settings.'**
  String get bodyEnergyCalibrationBody;

  /// No description provided for @bodyEnergyCalibrationOptionalBody.
  ///
  /// In en, this message translates to:
  /// **'Manual heart-rate zones below are optional. If you skip them, OpenVitals uses automatic estimates from Health Connect data and shows lower confidence when calibration is uncertain.'**
  String get bodyEnergyCalibrationOptionalBody;

  /// No description provided for @bodyEnergyCalibrationBirthYear.
  ///
  /// In en, this message translates to:
  /// **'Birth year'**
  String get bodyEnergyCalibrationBirthYear;

  /// No description provided for @bodyEnergyCalibrationMaxHr.
  ///
  /// In en, this message translates to:
  /// **'Max heart rate'**
  String get bodyEnergyCalibrationMaxHr;

  /// No description provided for @bodyEnergyCalibrationRestingHr.
  ///
  /// In en, this message translates to:
  /// **'Resting heart rate'**
  String get bodyEnergyCalibrationRestingHr;

  /// No description provided for @bodyEnergyCalibrationManualZones.
  ///
  /// In en, this message translates to:
  /// **'Manual heart zones'**
  String get bodyEnergyCalibrationManualZones;

  /// No description provided for @bodyEnergyCalibrationManualZonesBody.
  ///
  /// In en, this message translates to:
  /// **'Optional bpm lower bounds for zones 1-5.'**
  String get bodyEnergyCalibrationManualZonesBody;

  /// No description provided for @bodyEnergyCalibrationZone1.
  ///
  /// In en, this message translates to:
  /// **'Zone 1 lower bpm'**
  String get bodyEnergyCalibrationZone1;

  /// No description provided for @bodyEnergyCalibrationZone2.
  ///
  /// In en, this message translates to:
  /// **'Zone 2 lower bpm'**
  String get bodyEnergyCalibrationZone2;

  /// No description provided for @bodyEnergyCalibrationZone3.
  ///
  /// In en, this message translates to:
  /// **'Zone 3 lower bpm'**
  String get bodyEnergyCalibrationZone3;

  /// No description provided for @bodyEnergyCalibrationZone4.
  ///
  /// In en, this message translates to:
  /// **'Zone 4 lower bpm'**
  String get bodyEnergyCalibrationZone4;

  /// No description provided for @bodyEnergyCalibrationZone5.
  ///
  /// In en, this message translates to:
  /// **'Zone 5 lower bpm'**
  String get bodyEnergyCalibrationZone5;

  /// No description provided for @bodyEnergyCalibrationUseAuto.
  ///
  /// In en, this message translates to:
  /// **'Use automatic estimates'**
  String get bodyEnergyCalibrationUseAuto;

  /// No description provided for @bodyEnergyCalibrationSkip.
  ///
  /// In en, this message translates to:
  /// **'Skip for now'**
  String get bodyEnergyCalibrationSkip;

  /// No description provided for @bodyEnergyCalibrationSaved.
  ///
  /// In en, this message translates to:
  /// **'Body Energy calibration saved'**
  String get bodyEnergyCalibrationSaved;

  /// No description provided for @bodyEnergyCalibrationReset.
  ///
  /// In en, this message translates to:
  /// **'Body Energy calibration reset to automatic'**
  String get bodyEnergyCalibrationReset;

  /// No description provided for @bodyEnergyNotSetUp.
  ///
  /// In en, this message translates to:
  /// **'Not set up'**
  String get bodyEnergyNotSetUp;

  /// No description provided for @bodyEnergyTimelineEstimated.
  ///
  /// In en, this message translates to:
  /// **'Estimated by OpenVitals'**
  String get bodyEnergyTimelineEstimated;

  /// No description provided for @bodyEnergyTimelineCurrent.
  ///
  /// In en, this message translates to:
  /// **'Current'**
  String get bodyEnergyTimelineCurrent;

  /// No description provided for @bodyEnergyTimelineStart.
  ///
  /// In en, this message translates to:
  /// **'Start'**
  String get bodyEnergyTimelineStart;

  /// No description provided for @bodyEnergyTimelineCharged.
  ///
  /// In en, this message translates to:
  /// **'Charged'**
  String get bodyEnergyTimelineCharged;

  /// No description provided for @bodyEnergyTimelineDrained.
  ///
  /// In en, this message translates to:
  /// **'Drained'**
  String get bodyEnergyTimelineDrained;

  /// No description provided for @bodyEnergyTimelineConfidence.
  ///
  /// In en, this message translates to:
  /// **'Confidence'**
  String get bodyEnergyTimelineConfidence;

  /// No description provided for @bodyEnergyTimelineNoData.
  ///
  /// In en, this message translates to:
  /// **'No usable Body Energy timeline for this period.'**
  String get bodyEnergyTimelineNoData;

  /// No description provided for @bodyEnergyTimelineDayTitle.
  ///
  /// In en, this message translates to:
  /// **'Daily timeline'**
  String get bodyEnergyTimelineDayTitle;

  /// No description provided for @bodyEnergyTimelineLowConfidence.
  ///
  /// In en, this message translates to:
  /// **'Some buckets are estimated because calibration or Health Connect data is incomplete.'**
  String get bodyEnergyTimelineLowConfidence;

  /// No description provided for @bodyEnergyWhyTitle.
  ///
  /// In en, this message translates to:
  /// **'What moved it'**
  String get bodyEnergyWhyTitle;

  /// No description provided for @bodyEnergyWhyEmpty.
  ///
  /// In en, this message translates to:
  /// **'No clear charge or drain dominated this day yet.'**
  String get bodyEnergyWhyEmpty;

  /// No description provided for @bodyEnergyInfluenceSleepRecovery.
  ///
  /// In en, this message translates to:
  /// **'Sleep recovery'**
  String get bodyEnergyInfluenceSleepRecovery;

  /// No description provided for @bodyEnergyInfluenceQuietRest.
  ///
  /// In en, this message translates to:
  /// **'Quiet rest'**
  String get bodyEnergyInfluenceQuietRest;

  /// No description provided for @bodyEnergyInfluenceExertion.
  ///
  /// In en, this message translates to:
  /// **'Exertion'**
  String get bodyEnergyInfluenceExertion;

  /// No description provided for @bodyEnergyInfluenceElevatedHr.
  ///
  /// In en, this message translates to:
  /// **'Elevated heart rate'**
  String get bodyEnergyInfluenceElevatedHr;

  /// No description provided for @bodyEnergyInfluenceRecoveryDebt.
  ///
  /// In en, this message translates to:
  /// **'Recovery debt'**
  String get bodyEnergyInfluenceRecoveryDebt;

  /// No description provided for @bodyEnergyInfluenceNoData.
  ///
  /// In en, this message translates to:
  /// **'No data'**
  String get bodyEnergyInfluenceNoData;

  /// No description provided for @bodyEnergyInfluenceSteady.
  ///
  /// In en, this message translates to:
  /// **'Steady'**
  String get bodyEnergyInfluenceSteady;

  /// No description provided for @bodyEnergyReasonSleepRecoveryDetail.
  ///
  /// In en, this message translates to:
  /// **'Sleep buckets charged the estimate from the previous score.'**
  String get bodyEnergyReasonSleepRecoveryDetail;

  /// No description provided for @bodyEnergyReasonQuietRestDetail.
  ///
  /// In en, this message translates to:
  /// **'Low heart rate while awake added a small recovery charge.'**
  String get bodyEnergyReasonQuietRestDetail;

  /// No description provided for @bodyEnergyReasonExertionDetail.
  ///
  /// In en, this message translates to:
  /// **'Heart-rate intensity or recorded workouts drained the estimate.'**
  String get bodyEnergyReasonExertionDetail;

  /// No description provided for @bodyEnergyReasonElevatedHrDetail.
  ///
  /// In en, this message translates to:
  /// **'Awake heart rate above resting level added stress drain.'**
  String get bodyEnergyReasonElevatedHrDetail;

  /// No description provided for @bodyEnergyReasonRecoveryDebtDetail.
  ///
  /// In en, this message translates to:
  /// **'Recent harder effort kept a small drain active afterward.'**
  String get bodyEnergyReasonRecoveryDebtDetail;

  /// No description provided for @bodyEnergyReasonNoDataDetail.
  ///
  /// In en, this message translates to:
  /// **'Health Connect did not provide enough signal for this bucket.'**
  String get bodyEnergyReasonNoDataDetail;

  /// No description provided for @bodyEnergyReasonSteadyDetail.
  ///
  /// In en, this message translates to:
  /// **'The estimate stayed mostly stable.'**
  String get bodyEnergyReasonSteadyDetail;

  /// No description provided for @bodyEnergyInputsTitle.
  ///
  /// In en, this message translates to:
  /// **'Inputs used'**
  String get bodyEnergyInputsTitle;

  /// No description provided for @bodyEnergyInputsSummary.
  ///
  /// In en, this message translates to:
  /// **'Algorithm v{arg0}, {arg1}-minute buckets'**
  String bodyEnergyInputsSummary(int arg0, int arg1);

  /// No description provided for @bodyEnergyInputHeartRate.
  ///
  /// In en, this message translates to:
  /// **'Heart rate samples'**
  String get bodyEnergyInputHeartRate;

  /// No description provided for @bodyEnergyInputSleep.
  ///
  /// In en, this message translates to:
  /// **'Sleep sessions'**
  String get bodyEnergyInputSleep;

  /// No description provided for @bodyEnergyInputWorkouts.
  ///
  /// In en, this message translates to:
  /// **'Workouts'**
  String get bodyEnergyInputWorkouts;

  /// No description provided for @bodyEnergyInputRestingHr.
  ///
  /// In en, this message translates to:
  /// **'Resting heart rate'**
  String get bodyEnergyInputRestingHr;

  /// No description provided for @bodyEnergyInputHrBaseline.
  ///
  /// In en, this message translates to:
  /// **'Heart-rate baseline'**
  String get bodyEnergyInputHrBaseline;

  /// No description provided for @bodyEnergyInputHrv.
  ///
  /// In en, this message translates to:
  /// **'HRV modifier'**
  String get bodyEnergyInputHrv;

  /// No description provided for @bodyEnergyInputRespiratory.
  ///
  /// In en, this message translates to:
  /// **'Respiration modifier'**
  String get bodyEnergyInputRespiratory;

  /// No description provided for @bodyEnergyInputPreviousScore.
  ///
  /// In en, this message translates to:
  /// **'Previous score'**
  String get bodyEnergyInputPreviousScore;

  /// No description provided for @bodyEnergyInputCalibration.
  ///
  /// In en, this message translates to:
  /// **'Calibration'**
  String get bodyEnergyInputCalibration;

  /// No description provided for @bodyEnergyInputAvailable.
  ///
  /// In en, this message translates to:
  /// **'Available'**
  String get bodyEnergyInputAvailable;

  /// No description provided for @bodyEnergyInputMissing.
  ///
  /// In en, this message translates to:
  /// **'Missing'**
  String get bodyEnergyInputMissing;

  /// No description provided for @bodyEnergyInputOptional.
  ///
  /// In en, this message translates to:
  /// **'Not present'**
  String get bodyEnergyInputOptional;

  /// No description provided for @bodyEnergyInputRecords.
  ///
  /// In en, this message translates to:
  /// **'{arg0} records'**
  String bodyEnergyInputRecords(int arg0);

  /// No description provided for @bodyEnergyInputSessions.
  ///
  /// In en, this message translates to:
  /// **'{arg0} sessions'**
  String bodyEnergyInputSessions(int arg0);

  /// No description provided for @bodyEnergyInputWorkoutsValue.
  ///
  /// In en, this message translates to:
  /// **'{arg0} workouts'**
  String bodyEnergyInputWorkoutsValue(int arg0);

  /// No description provided for @bodyEnergyInputPreviousScoreValue.
  ///
  /// In en, this message translates to:
  /// **'{arg0} start'**
  String bodyEnergyInputPreviousScoreValue(String arg0);

  /// No description provided for @bodyEnergyCalibrationModeAuto.
  ///
  /// In en, this message translates to:
  /// **'Automatic estimates'**
  String get bodyEnergyCalibrationModeAuto;

  /// No description provided for @bodyEnergyCalibrationModeManualValues.
  ///
  /// In en, this message translates to:
  /// **'Manual values'**
  String get bodyEnergyCalibrationModeManualValues;

  /// No description provided for @bodyEnergyCalibrationModeManualZones.
  ///
  /// In en, this message translates to:
  /// **'Manual zones'**
  String get bodyEnergyCalibrationModeManualZones;

  /// No description provided for @bodyEnergyCalculationTitle.
  ///
  /// In en, this message translates to:
  /// **'How Body Energy is estimated'**
  String get bodyEnergyCalculationTitle;

  /// No description provided for @bodyEnergyCalculationBody.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals divides the selected day into short buckets, starts from the previous available score when possible, then adds charge from sleep or quiet rest and subtracts drain from exertion, elevated awake heart rate, and recovery debt after harder effort.'**
  String get bodyEnergyCalculationBody;

  /// No description provided for @bodyEnergyCalculationInputsBody.
  ///
  /// In en, this message translates to:
  /// **'Heart rate, resting heart rate, personal zones, sleep, workouts, HRV, and respiratory rate can all improve the estimate. Missing inputs make the estimate more conservative and lower confidence.'**
  String get bodyEnergyCalculationInputsBody;

  /// No description provided for @bodyEnergyCalculationLimitsBody.
  ///
  /// In en, this message translates to:
  /// **'This is an on-device wellness estimate, not a direct measurement or medical advice. The displayed inputs and reasons are exposed so the method can be reviewed and improved.'**
  String get bodyEnergyCalculationLimitsBody;

  /// No description provided for @metricBodyEnergy.
  ///
  /// In en, this message translates to:
  /// **'Body Energy'**
  String get metricBodyEnergy;

  /// No description provided for @privacyPolicyTitle.
  ///
  /// In en, this message translates to:
  /// **'Privacy policy'**
  String get privacyPolicyTitle;

  /// No description provided for @privacyPolicyBody1.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals reads data from Health Connect to show steps, workouts, sleep, heart rate, weight, calories, hydration, nutrition, mindfulness, and vitals on your device. Entries you explicitly log, including imported GPX/KML/KMZ routes and imported FIT files, are written to Health Connect.'**
  String get privacyPolicyBody1;

  /// No description provided for @privacyPolicyBody2.
  ///
  /// In en, this message translates to:
  /// **'This app does not upload your health data to a cloud service, does not include ads, and does not share data with third parties.'**
  String get privacyPolicyBody2;

  /// No description provided for @privacyPolicyBody3.
  ///
  /// In en, this message translates to:
  /// **'OpenVitals is not a medical device and does not diagnose, treat, cure, or prevent any disease or medical condition. It is not a substitute for medical advice, diagnosis, or treatment from a qualified healthcare professional.'**
  String get privacyPolicyBody3;

  /// No description provided for @linkCouldNotOpen.
  ///
  /// In en, this message translates to:
  /// **'The link could not be opened.'**
  String get linkCouldNotOpen;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['de', 'en', 'es', 'et', 'it'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'de':
      return AppLocalizationsDe();
    case 'en':
      return AppLocalizationsEn();
    case 'es':
      return AppLocalizationsEs();
    case 'et':
      return AppLocalizationsEt();
    case 'it':
      return AppLocalizationsIt();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
