// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for German (`de`).
class AppLocalizationsDe extends AppLocalizations {
  AppLocalizationsDe([String locale = 'de']) : super(locale);

  @override
  String get appName => 'OpenVitals';

  @override
  String get cdBack => 'Zurück';

  @override
  String get cdSettings => 'Einstellungen';

  @override
  String get cdAchievements => 'Auszeichnungen';

  @override
  String get cdDailyReadiness => 'Tägliche Bereitschaft';

  @override
  String get cdSensorBatteryStatus => 'Sensor-Akkustatus';

  @override
  String get cdEditDashboard => 'Übersicht bearbeiten';

  @override
  String get cdFinishDashboardEditing => 'Übersichtsbearbeitung beenden';

  @override
  String get cdEditSavedDrinks => 'Gespeicherte Getränke bearbeiten';

  @override
  String get cdDoneEditingSavedDrinks =>
      'Bearbeitung gespeicherter Getränke beenden';

  @override
  String get cdEditDrink => 'Getränk bearbeiten';

  @override
  String get cdDeleteDrink => 'Getränk löschen';

  @override
  String get cdMoveDrinkCategory => 'Getränkekategorie verschieben';

  @override
  String cdExpandDrinkCategory(String arg0) {
    return '$arg0 erweitern';
  }

  @override
  String cdCollapseDrinkCategory(String arg0) {
    return '$arg0 einklappen';
  }

  @override
  String get cdEditManualEntryWidgets =>
      'Widgets zum Hinzufügen von Einträgen bearbeiten';

  @override
  String get cdFinishManualEntryEditing =>
      'Bearbeitung der Widgets zum Hinzufügen von Einträgen beenden';

  @override
  String get cdEditRecordingDashboard => 'Aufzeichnungsübersicht bearbeiten';

  @override
  String get cdFinishRecordingDashboardEditing =>
      'Bearbeitung der Aufzeichnungsübersicht beenden';

  @override
  String get cdMoveWidgetUp => 'Widget nach oben verschieben';

  @override
  String get cdMoveWidgetDown => 'Widget nach unten verschieben';

  @override
  String get cdEditMetricSections => 'Metrikabschnitte bearbeiten';

  @override
  String get cdFinishMetricSectionEditing =>
      'Bearbeitung der Metrikabschnitte beenden';

  @override
  String get cdMoveSectionUp => 'Abschnitt nach oben verschieben';

  @override
  String get cdMoveSectionDown => 'Abschnitt nach unten verschieben';

  @override
  String get cdRemoveWidget => 'Widget entfernen';

  @override
  String get cdDecreaseRecordingDashboardWidgetSize => 'Widget kleiner machen';

  @override
  String get cdIncreaseRecordingDashboardWidgetSize => 'Widget größer machen';

  @override
  String get cdExitRecordingFocusMode => 'Fokusmodus verlassen';

  @override
  String get cdToggleRecordingOutdoorMode =>
      'Outdoor-Lesbarkeitsmodus umschalten';

  @override
  String get cdRecenterMap => 'Karte neu zentrieren';

  @override
  String get cdDeleteEntry => 'Eintrag löschen';

  @override
  String get cdEditEntry => 'Eintrag bearbeiten';

  @override
  String get cdPreviousDay => 'Vorheriger Tag';

  @override
  String get cdNextDay => 'Nächster Tag';

  @override
  String get cdPreviousPeriod => 'Vorheriger Zeitraum';

  @override
  String get cdNextPeriod => 'Nächster Zeitraum';

  @override
  String get cdOpenCalendar => 'Kalender öffnen';

  @override
  String get actionCancel => 'Abbrechen';

  @override
  String get actionAdd => 'Hinzufügen';

  @override
  String get actionAddCustom => 'Benutzerdefiniert hinzufügen';

  @override
  String get actionSave => 'Speichern';

  @override
  String get actionClose => 'Schließen';

  @override
  String get actionContinue => 'Weiter';

  @override
  String get actionDelete => 'Löschen';

  @override
  String get actionDetails => 'Details';

  @override
  String get actionEdit => 'Bearbeiten';

  @override
  String get actionEnable => 'Aktivieren';

  @override
  String get actionGetStarted => 'Loslegen';

  @override
  String get actionGrant => 'Erteilen';

  @override
  String get actionGrantPermission => 'Berechtigung erteilen';

  @override
  String get actionLoadMoreEntries => '10 weitere laden';

  @override
  String get actionShowCalculation => 'Berechnung anzeigen';

  @override
  String get actionHideCalculation => 'Berechnung ausblenden';

  @override
  String get actionNotNow => 'Jetzt nicht';

  @override
  String get actionAccept => 'Ich akzeptiere';

  @override
  String get actionOpen => 'Öffnen';

  @override
  String get actionPause => 'Pausieren';

  @override
  String get actionReview => 'Prüfen';

  @override
  String get actionResume => 'Fortsetzen';

  @override
  String get actionRefresh => 'Aktualisieren';

  @override
  String get actionSelect => 'Auswählen';

  @override
  String get actionStart => 'Starten';

  @override
  String get actionFinish => 'Beenden';

  @override
  String get actionDiscard => 'Verwerfen';

  @override
  String get unknownError => 'Unbekannter Fehler';

  @override
  String get screenErrorNotFound =>
      'Der angeforderte Eintrag wurde nicht gefunden.';

  @override
  String get screenErrorMissingArgument =>
      'Erforderliche Informationen fehlen.';

  @override
  String get screenErrorPermissionDenied =>
      'Zum Laden dieser Daten ist eine Berechtigung erforderlich.';

  @override
  String get screenErrorHealthConnectUnavailable =>
      'Health Connect ist auf diesem Gerät nicht verfügbar.';

  @override
  String get screenErrorLoadSleepSession =>
      'Schlafsitzung konnte nicht geladen werden.';

  @override
  String get screenErrorLoadSleepPeriod =>
      'Schlafdaten konnten nicht geladen werden.';

  @override
  String get notAvailable => 'Nicht verfügbar';

  @override
  String get notRecorded => 'Nicht aufgezeichnet';

  @override
  String get noData => 'Keine Daten';

  @override
  String get loading => 'Wird geladen...';

  @override
  String get homeMetricWidgetDescription => 'OpenVitals-Metrik';

  @override
  String get homeMetricWidgetConfigTitle => 'Metrik auswählen';

  @override
  String get homeMetricWidgetConfigPrompt => 'Wähle die Metrik für das Widget:';

  @override
  String get homeMetricWidgetNoMetrics => 'Keine Metriken verfügbar.';

  @override
  String get homeMetricWidgetPermissionNeeded =>
      'Berechtigung in OpenVitals erteilen';

  @override
  String get homeMetricWidgetUpdateFailed => 'Aktualisierung nicht möglich';

  @override
  String get homeMetricWidgetOpenForDetails => 'Für Details öffnen';

  @override
  String get homeMetricWidgetNotConfigured => 'Metrik auswählen';

  @override
  String get homeQuickBeverageWidgetDescription => 'Schnelles Getränk';

  @override
  String get homeQuickBeverageOneTapWidgetDescription =>
      'Schnelles Getränk 1x1';

  @override
  String get homeQuickBeverageWidgetConfigTitle => 'Getränk auswählen';

  @override
  String get homeQuickBeverageWidgetConfigPrompt =>
      'Wähle das Getränk für das Widget:';

  @override
  String get homeQuickBeverageWidgetNoDrinks => 'Keine Getränke verfügbar.';

  @override
  String get homeQuickBeverageWidgetNotConfigured => 'Getränk auswählen';

  @override
  String get homeQuickBeverageWidgetTapToLog => 'Zum Protokollieren tippen';

  @override
  String get homeQuickBeverageWidgetSaved => 'Jetzt gespeichert';

  @override
  String get homeQuickBeverageWidgetSavedNutrition =>
      'Als Ernährung gespeichert';

  @override
  String get homeDailyReadinessWidgetDescription =>
      'OpenVitals tägliche Bereitschaft';

  @override
  String get homeBodyEnergyWidgetDescription => 'OpenVitals Körperenergie';

  @override
  String get homeTodayVitalsWidgetDescription => 'OpenVitals Vitalwerte heute';

  @override
  String get homeWidgetTodayTitle => 'Heute';

  @override
  String get homeWidgetContext => 'Kontext';

  @override
  String get homeWidgetHrvShort => 'HRV';

  @override
  String get homeWidgetBodyEnergyCharged => 'Geladen';

  @override
  String get homeWidgetBodyEnergySteady => 'Stabil';

  @override
  String get homeWidgetBodyEnergyLimited => 'Begrenzt';

  @override
  String get homeWidgetBodyEnergyLow => 'Niedrig';

  @override
  String get screenSteps => 'Schritte';

  @override
  String get screenActivities => 'Aktivitäten';

  @override
  String get screenCalories => 'Kalorien';

  @override
  String get screenActivityDetail => 'Aktivitätsdetails';

  @override
  String get screenSleep => 'Schlaf';

  @override
  String get screenSleepDetail => 'Schlafdetails';

  @override
  String get screenHeartVitals => 'Herz & Vitalwerte';

  @override
  String get screenStressTracking => 'Stress-Tracking';

  @override
  String get screenBodyEnergy => 'Körperenergie';

  @override
  String get screenTrainingReadiness => 'Trainingsbereitschaft';

  @override
  String get screenBody => 'Körper';

  @override
  String get screenHydration => 'Hydrierung';

  @override
  String get screenNutrition => 'Ernährung';

  @override
  String get screenMindfulness => 'Achtsamkeit';

  @override
  String get screenCycle => 'Zyklus';

  @override
  String get screenDailyReadiness => 'Tägliche Bereitschaft';

  @override
  String get screenSettings => 'Einstellungen';

  @override
  String get screenAchievements => 'Auszeichnungen';

  @override
  String get screenManualEntry => 'Eintrag hinzufügen';

  @override
  String get screenHydrationEntry => 'Hydrierungseintrag';

  @override
  String get screenActivityEntry => 'Aktivitätseintrag';

  @override
  String get screenMindfulnessEntry => 'Achtsamkeitseintrag';

  @override
  String get screenCarbsEntry => 'Kohlenhydrateintrag';

  @override
  String get screenBodyMeasurementEntry => 'Körpermessungseintrag';

  @override
  String get screenVitalsMeasurementEntry => 'Vitalwerteeintrag';

  @override
  String get bottomNavDashboard => 'Übersicht';

  @override
  String get manualEntryHydrationTitle => 'Hydrierung';

  @override
  String get manualEntryActivityTitle => 'Aktivität';

  @override
  String get manualEntryDateLabel => 'Eintragsdatum';

  @override
  String get manualEntryTimeLabel => 'Eintragszeit';

  @override
  String get manualEntrySelectTime => 'Eintragszeit auswählen';

  @override
  String get manualEntryAddWidgets => 'Eintrags-Widgets hinzufügen';

  @override
  String get manualEntryAllWidgetsAdded =>
      'Alle Eintrags-Widgets werden bereits angezeigt.';

  @override
  String get manualEntryWritePermissionTitle =>
      'Schreibberechtigung für Hydrierung';

  @override
  String get manualEntryActivityWritePermissionTitle =>
      'Schreibberechtigungen für Aktivitäten';

  @override
  String get manualEntryMindfulnessWritePermissionTitle =>
      'Schreibberechtigung für Achtsamkeit';

  @override
  String get manualEntryCarbsWritePermissionTitle =>
      'Schreibberechtigung für Kohlenhydrate';

  @override
  String manualEntryBodyWritePermissionTitle(String arg0) {
    return 'Schreibberechtigung für $arg0';
  }

  @override
  String manualEntryVitalsWritePermissionTitle(String arg0) {
    return 'Schreibberechtigung für $arg0';
  }

  @override
  String get mindfulnessEntrySubtitle =>
      'Achtsamkeitssitzungen werden direkt in Health Connect gespeichert.';

  @override
  String get mindfulnessEntryPermissionNeeded =>
      'Für die Übersicht fragt OpenVitals nur Leseberechtigungen an. Zum Hinzufügen von Achtsamkeitseinträgen benötigen wir eine Schreibberechtigung. OpenVitals speichert diese Sitzungen nicht; Einträge werden in Health Connect gespeichert.';

  @override
  String get activityEntrySubtitle =>
      'Erstelle eine Aktivitätssitzung in Health Connect. Routendateien werden nur geschrieben, wenn du eine importierst.';

  @override
  String get activityEntryPermissionNeeded =>
      'Für die Übersicht fragt OpenVitals nur Leseberechtigungen an. Um Aktivitäten ohne spätere Unterbrechungen hinzuzufügen, benötigen wir Health-Connect-Schreibberechtigungen für Sitzungen, Routen, Distanz, Höhenmeter, aktive Kalorien und Gesamtkalorien. OpenVitals speichert diese Einträge nicht; sie werden in Health Connect gespeichert.';

  @override
  String get activityEntrySourceBody =>
      'Erstelle eine Aktivität manuell, nutze einen vorhandenen Plan oder zeichne eine GPS-Route auf.';

  @override
  String get activityEntryCreateManual => 'Manuell erstellen';

  @override
  String get activityEntryCreateFromExistingPlan =>
      'Aus vorhandenem Plan erstellen';

  @override
  String get activityEntryRecordGps => 'Aktivität aufzeichnen';

  @override
  String get activityEntryChooseAnotherSource => 'Andere Methode wählen';

  @override
  String get activityEntryTypeLabel => 'Aktivitätstyp';

  @override
  String get activityEntryTitleLabel => 'Titel optional';

  @override
  String get activityEntryStartDateLabel => 'Startdatum';

  @override
  String get activityEntryStartTimeLabel => 'Startzeit';

  @override
  String get activityEntrySelectTime => 'Startzeit auswählen';

  @override
  String get activityEntryDurationLabel => 'Dauer min';

  @override
  String get activityEntryRepetitionsTitle => 'Wiederholungen';

  @override
  String get activityEntryStepsTitle => 'Schritte';

  @override
  String get activityEntryRepetitionModeTotal => 'Gesamt';

  @override
  String get activityEntryRepetitionModeSets => 'Sätze';

  @override
  String get activityEntryRepetitionsLabel => 'Wdh.';

  @override
  String get activityEntryStepsLabel => 'Schritte';

  @override
  String activityEntrySetRepetitionsLabel(int arg0) {
    return 'Satz $arg0 Wdh.';
  }

  @override
  String get activityEntrySetRestLabel => 'Pausenzeit';

  @override
  String get activityEntryAddSet => 'Satz hinzufügen';

  @override
  String get activityEntryTrainingPlansTitle => 'Trainingspläne';

  @override
  String get activityEntryTrainingPlansLoading =>
      'Health-Connect-Pläne werden geladen';

  @override
  String get activityEntryTrainingPlansEmpty =>
      'Keine Health-Connect-Pläne für dieses Datum und diesen Aktivitätstyp';

  @override
  String get activityEntryTrainingPlanLabel => 'Trainingsplan';

  @override
  String get activityEntryTrainingPlanSelect => 'Plan auswählen';

  @override
  String get activityEntryTrainingPlanNew => 'Neuer Plan';

  @override
  String get activityEntryTrainingPlanUnnamed => 'Unbenannter Plan';

  @override
  String get activityEntrySaveTrainingPlan => 'Plan speichern';

  @override
  String get activityEntryUpdateTrainingPlan => 'Plan aktualisieren';

  @override
  String get activityEntryPlanActivityPickerTitle => 'Aktivitäten mit Plänen';

  @override
  String get activityEntryPlanActivityPickerEmpty =>
      'Keine Health-Connect-Pläne gefunden';

  @override
  String get activityEntryPlanPickerTitle => 'Plan auswählen';

  @override
  String get activityEntryPlanPickerEmpty =>
      'Keine Pläne für diese Aktivität gefunden';

  @override
  String get activityEntryPlanChooseActivity => 'Andere Aktivität auswählen';

  @override
  String activityEntryPlanOneSetSummary(int arg0) {
    return '1 Satz • $arg0 Wdh.';
  }

  @override
  String activityEntryPlanSummary(int arg0, int arg1) {
    return '$arg0 Sätze • $arg1 Wdh.';
  }

  @override
  String activityEntryPlanPreviewReps(int arg0) {
    return '$arg0 Wdh.';
  }

  @override
  String activityEntryPlanPreviewRest(int arg0) {
    return 'Pause $arg0 Sek.';
  }

  @override
  String activityEntryPlanPreviewMore(int arg0) {
    return '+$arg0 weitere';
  }

  @override
  String activityEntryDistanceLabel(String arg0) {
    return 'Distanz $arg0 optional';
  }

  @override
  String activityEntryElevationLabel(String arg0) {
    return 'Höhe $arg0 optional';
  }

  @override
  String get activityEntryNotesLabel => 'Notizen optional';

  @override
  String get activityEntryFeelingLabel => 'Wie hat es sich angefühlt?';

  @override
  String get activityEntryFeelingGreat => 'Großartig';

  @override
  String get activityEntryFeelingGood => 'Gut';

  @override
  String get activityEntryFeelingHard => 'Anstrengend';

  @override
  String get activityEntryFeelingRough => 'Sehr hart';

  @override
  String get activityEntryImportedRoute => 'Importierte Route';

  @override
  String get activityEntryRecordingTitle => 'Aktivität wird aufgezeichnet';

  @override
  String get activityEntryRecordingReadyBody =>
      'Wähle den Aktivitätstyp und starte, wenn du bereit bist. Nach dem Beenden kannst du Details prüfen und ergänzen, bevor du speicherst.';

  @override
  String get activityEntryRecordingGoToActivityScreen =>
      'Zum Aktivitätsbildschirm';

  @override
  String get activityEntryRecordingActive => 'Aufzeichnung läuft';

  @override
  String get activityEntryRecordingPaused => 'Pausiert';

  @override
  String get activityEntryRecordingIdle => 'Leerlauf';

  @override
  String get activityEntryRecordingResting => 'Pause';

  @override
  String get activityEntryRecordingGpsFix => 'GPS bereit';

  @override
  String get activityEntryRecordingGpsPoor => 'Schwaches GPS';

  @override
  String get activityEntryRecordingGpsLost => 'GPS verloren';

  @override
  String get activityEntryRecordingGpsOff => 'GPS aus';

  @override
  String get activityEntryRecordingTabMap => 'Karte';

  @override
  String get activityEntryRecordingTabStats => 'Statistik';

  @override
  String get activityEntryRecordingTabIntervals => 'Intervalle';

  @override
  String get activityEntryRecordingTabByTime => 'Nach Zeit';

  @override
  String get activityEntryRecordingTabByDistance => 'Nach Distanz';

  @override
  String get activityEntryRecordingTimeSplit => 'Zeitabschnitt';

  @override
  String get activityEntryRecordingDistanceSplit => 'Distanzabschnitt';

  @override
  String activityEntryRecordingSplitMinutes(int arg0) {
    return '$arg0 min';
  }

  @override
  String activityEntryRecordingSplitInterval(int arg0) {
    return 'Intervall $arg0';
  }

  @override
  String activityEntryRecordingSplitTimeRange(int arg0, int arg1) {
    return '$arg0-$arg1 min';
  }

  @override
  String get activityEntryRecordingSplitElapsed => 'Zeit';

  @override
  String get activityEntryRecordingSplitAvg => 'Ø';

  @override
  String get activityEntryRecordingSplitMax => 'Max.';

  @override
  String get activityEntryRecordingNoIntervals => 'Noch keine Intervalle';

  @override
  String get activityEntryRecordingNoTimeSplits => 'Noch keine Zeitabschnitte';

  @override
  String get activityEntryRecordingNoDistanceSplits =>
      'Noch keine Distanzabschnitte';

  @override
  String get activityEntryRecordingLap => 'Runde';

  @override
  String get activityEntryRecordingMarker => 'Markierung';

  @override
  String activityEntryRecordingMarkerDefaultName(int arg0) {
    return 'Markierung $arg0';
  }

  @override
  String get activityEntryRecordingMarkersTitle => 'Markierungen';

  @override
  String get activityEntryRecordingMarkerName => 'Name';

  @override
  String get activityEntryRecordingMarkerNote => 'Notiz';

  @override
  String get activityEntryRecordingWaitingForGps =>
      'Warten auf eine genaue GPS-Position';

  @override
  String get activityEntryRecordingGpsWaiting =>
      'Warten auf eine genaue GPS-Position vor dem Start.';

  @override
  String activityEntryRecordingGpsWaitingAccuracy(String arg0) {
    return 'Warten auf bessere GPS-Genauigkeit • $arg0';
  }

  @override
  String activityEntryRecordingGpsReady(String arg0) {
    return 'GPS bereit • Genauigkeit $arg0';
  }

  @override
  String get activityEntryRecordingGpsDisabled =>
      'Aktiviere GPS, um die Aufzeichnung zu starten.';

  @override
  String get activityEntryRecordingDistance => 'Distanz';

  @override
  String get activityEntryRecordingTotalTime => 'Gesamtzeit';

  @override
  String get activityEntryRecordingMovingTime => 'Zeit in Bewegung';

  @override
  String get activityEntryRecordingRestTime => 'Pausenzeit';

  @override
  String get activityEntryRecordingSpeed => 'Geschwindigkeit';

  @override
  String get activityEntryRecordingMaxSpeed => 'Max. Geschwindigkeit';

  @override
  String get activityEntryRecordingAverageSpeed => 'Ø Geschwindigkeit';

  @override
  String get activityEntryRecordingAverageMovingSpeed =>
      'Ø Geschwindigkeit in Bewegung';

  @override
  String get activityEntryRecordingElevationGain => 'Anstieg';

  @override
  String get activityEntryRecordingPoints => 'Punkte';

  @override
  String get activityEntryRecordingRestSecondsLabel => 'Pausensekunden';

  @override
  String get activityEntryRecordingEndSet => 'Satz beenden';

  @override
  String get activityEntryRecordingStartNextSet => 'Nächsten Satz starten';

  @override
  String get activityEntryRecordingEndSession => 'Sitzung beenden';

  @override
  String activityEntryRecordingRestRemaining(String arg0) {
    return 'Pause $arg0';
  }

  @override
  String get activityEntryRecordingFinishHint =>
      'Beenden öffnet das Formular mit Aktivitätsdetails, damit du vor dem Speichern Titel, Notizen, Kalorien ergänzen oder Werte anpassen kannst.';

  @override
  String get activityEntryRecordingRepetitionCorrectionHint =>
      'Nutze + oder -, wenn der Sensor eine Wiederholung auslässt oder hinzufügt.';

  @override
  String activityEntryRecordingAccuracy(String arg0) {
    return 'Letzte Genauigkeit $arg0';
  }

  @override
  String get activityEntryRecordingFocus => 'Fokus';

  @override
  String get activityEntryRecordingDashboardLayout => 'Dashboard-Layout';

  @override
  String get activityEntryRecordingDashboardLayoutTwoByFour => '2x4';

  @override
  String get activityEntryRecordingDashboardLayoutThreeByFour => '3x4';

  @override
  String get activityEntryRecordingDashboardLayoutLargeTop => 'Groß oben';

  @override
  String get activityEntryRecordingDashboardAddField => 'Widget hinzufügen';

  @override
  String activityEntryRouteSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  ) {
    return '$arg0 • $arg1 • $arg2 Anstieg • $arg3 Punkte';
  }

  @override
  String activityEntryRouteAverageMetrics(String arg0, String arg1) {
    return 'Durchschn. Tempo $arg0 • durchschn. Geschwindigkeit $arg1';
  }

  @override
  String get activityEntryAdd => 'Aktivität speichern';

  @override
  String get activityEntryInvalidValue =>
      'Korrigiere die markierten Felder, bevor du die Aktivität speicherst.';

  @override
  String get activityEntryErrorActivityTypeRoute =>
      'Wähle einen Aktivitätstyp, der GPS-Routen unterstützt.';

  @override
  String get activityEntryErrorTrainingPlanTitleRequired =>
      'Gib einen Titel ein, um diesen Trainingsplan zu speichern.';

  @override
  String get activityEntryErrorStartDate => 'Wähle ein gültiges Startdatum.';

  @override
  String get activityEntryErrorStartTime => 'Wähle eine gültige Startzeit.';

  @override
  String get activityEntryErrorStartTimeAfterRoute =>
      'Die Startzeit muss vor oder zum Start der importierten Route liegen.';

  @override
  String get activityEntryErrorDuration =>
      'Die Dauer muss zwischen 1 Minute und 7 Tagen liegen.';

  @override
  String get activityEntryErrorRepetitions =>
      'Positive Zählwerte eingeben. Die Pause muss in die Aktivitätsdauer passen.';

  @override
  String get activityEntryErrorDistance => 'Gib eine Distanz größer als 0 ein.';

  @override
  String get activityEntryErrorDistanceUnsupported =>
      'Dieser Aktivitätstyp unterstützt keine Distanz.';

  @override
  String get activityEntryErrorElevation => 'Gib eine Höhe größer als 0 ein.';

  @override
  String get activityEntryErrorElevationUnsupported =>
      'Dieser Aktivitätstyp unterstützt keinen Höhenanstieg.';

  @override
  String get activityEntryErrorActiveCalories =>
      'Gib aktive Kalorien größer als 0 ein.';

  @override
  String get activityEntryErrorTotalCalories =>
      'Gib Gesamtkalorien größer als 0 ein.';

  @override
  String get activityEntryErrorTotalCaloriesBelowActive =>
      'Gesamtkalorien dürfen nicht niedriger als aktive Kalorien sein.';

  @override
  String get activityEntryLocationPermissionNeeded =>
      'Für GPS-Aktivitäten ist die Berechtigung für den genauen Standort erforderlich.';

  @override
  String get activityEntryNotificationPermissionNeeded =>
      'Die Benachrichtigungsberechtigung ist erforderlich, damit OpenVitals eine laufende Aufzeichnungsbenachrichtigung anzeigen kann.';

  @override
  String get activityEntryActivityRecognitionPermissionNeeded =>
      'Die Aktivitätserkennung ist erforderlich, um Laufbandschritte zu zählen.';

  @override
  String activityEntryRouteImportFailed(String arg0) {
    return 'Routendatei konnte nicht importiert werden: $arg0';
  }

  @override
  String activityEntryRecordingFailed(String arg0) {
    return 'Aktivität konnte nicht aufgezeichnet werden: $arg0';
  }

  @override
  String activityEntryWriteFailed(String arg0) {
    return 'Aktivitätseintrag konnte nicht geschrieben werden: $arg0';
  }

  @override
  String get activityRouteOpenInMap => 'Route in Karten-App öffnen';

  @override
  String get activityRouteExportGpx => 'GPX speichern';

  @override
  String get activityRouteExportKmz => 'KMZ speichern';

  @override
  String get activityRouteExportSaved => 'Route gespeichert.';

  @override
  String get activityRouteExportFailed =>
      'Routendatei konnte nicht gespeichert werden.';

  @override
  String get activityRouteOpenChooserTitle => 'Route öffnen mit';

  @override
  String get activityRouteOpenFailed =>
      'Keine Karten-App konnte diese Route öffnen.';

  @override
  String get activityDetailAnalysisTitle => 'Routenanalyse';

  @override
  String get activityDetailTabMarkers => 'Markierungen';

  @override
  String get activityDetailNoMarkers => 'Noch keine Markierungen';

  @override
  String activityRecordingVoiceSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  ) {
    return 'Zeit $arg0. Distanz $arg1. Durchschnittsgeschwindigkeit $arg2. Aktuelle Runde $arg3.';
  }

  @override
  String activityRecordingVoiceLap(int arg0, String arg1) {
    return 'Runde $arg0. $arg1';
  }

  @override
  String get activityRecordingVoiceIdle => 'Leerlauf.';

  @override
  String get activityRecordingVoiceResumed => 'Aufzeichnung fortgesetzt.';

  @override
  String get activityRecordingNotificationChannel => 'Aktivitätsaufzeichnung';

  @override
  String get activityRecordingNotificationTitle =>
      'Aktivität wird aufgezeichnet';

  @override
  String activityRecordingNotificationRecording(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return 'Aufzeichnung läuft • $arg0 gesamt • $arg1 in Bewegung • $arg2 • $arg3';
  }

  @override
  String activityRecordingNotificationPaused(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return 'Pausiert • $arg0 gesamt • $arg1 in Bewegung • $arg2 • $arg3';
  }

  @override
  String activityRecordingNotificationRepetitionRecording(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Aufzeichnung läuft • $arg0 gesamt • $arg1 $arg2';
  }

  @override
  String activityRecordingNotificationRepetitionPaused(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Pausiert • $arg0 gesamt • $arg1 $arg2';
  }

  @override
  String activityRecordingNotificationRepetitionResting(
    String arg0,
    String arg1,
  ) {
    return 'Pause • $arg0 gesamt • $arg1 verbleibend';
  }

  @override
  String activityRecordingNotificationTimedRecording(String arg0) {
    return 'Aufzeichnung läuft • $arg0 gesamt';
  }

  @override
  String activityRecordingNotificationTimedPaused(String arg0) {
    return 'Pausiert • $arg0 gesamt';
  }

  @override
  String get activityRecordingErrorService =>
      'Aktivitätsaufzeichnungsdienst konnte nicht gestartet werden.';

  @override
  String get activityRecordingErrorPreciseLocationPermission =>
      'Der genaue Standort ist für zuverlässige GPS-Tracks erforderlich.';

  @override
  String get activityRecordingErrorNotificationPermission =>
      'Die Benachrichtigungsberechtigung ist erforderlich, um die laufende Aufzeichnungsbenachrichtigung anzuzeigen.';

  @override
  String get activityRecordingErrorActivityRecognitionPermission =>
      'Die Aktivitätserkennung ist erforderlich, um Laufbandschritte zu zählen.';

  @override
  String get activityRecordingErrorWaitingForGps =>
      'Warte auf eine genaue GPS-Position, bevor du startest.';

  @override
  String get activityRecordingErrorProvider =>
      'Aktiviere GPS, um eine Route aufzuzeichnen.';

  @override
  String get activityRecordingErrorUnsupportedType =>
      'Dieser Aktivitätstyp kann nicht live aufgezeichnet werden.';

  @override
  String get activityRecordingErrorProximitySensor =>
      'Dieses Gerät stellt keinen Näherungssensor zum Zählen von Liegestützen bereit.';

  @override
  String get activityRecordingErrorAccelerometer =>
      'Dieses Gerät stellt keinen Beschleunigungssensor für diese Aufzeichnung bereit.';

  @override
  String get activityRecordingErrorStepDetector =>
      'Dieses Gerät stellt keine Android-Schrittzählerereignisse bereit.';

  @override
  String get activityRecordingHowItWorks => 'So funktioniert die Aufzeichnung';

  @override
  String get activityRecordingGuidancePushUps =>
      'Lege das Telefon mit dem Display nach oben unter Brust oder Kopf. Der Näherungssensor zählt eine Wiederholung, wenn du dich dem Telefon näherst.';

  @override
  String get activityRecordingGuidancePullUps =>
      'Befestige das Telefon am Körper. Der Beschleunigungssensor zählt die Zug- und Entspannungsbewegung.';

  @override
  String get activityRecordingGuidanceRopeSkipping =>
      'Befestige das Telefon am Körper. Der Beschleunigungssensor zählt Sprünge.';

  @override
  String get activityRecordingGuidanceTrampolineJumping =>
      'Befestige das Telefon am Körper. Die Sprungerkennung nutzt ein längeres Sprungfenster als Seilspringen.';

  @override
  String get activityRecordingGuidanceTreadmill =>
      'Trage das Telefon am Körper. Der Android-Schrittdetektor zählt Schritte; es wird keine GPS-Route aufgezeichnet.';

  @override
  String get activityRecordingSensorReady => 'Sensor bereit';

  @override
  String get activityRecordingSensorUnavailableManual =>
      'Live-Zählung ist auf diesem Gerät nicht verfügbar. Manuelle Eingabe ist weiterhin verfügbar.';

  @override
  String get activityRecordingActivityRecognitionMissing =>
      'Aktivitätserkennung erlauben, um Laufbandschritte zu zählen.';

  @override
  String get exerciseTypeRunning => 'Laufen';

  @override
  String get exerciseTypeBiking => 'Radfahren';

  @override
  String get exerciseTypeWalking => 'Gehen';

  @override
  String get exerciseTypeHiking => 'Wandern';

  @override
  String get exerciseTypeWheelchair => 'Rollstuhl';

  @override
  String get exerciseTypeRowing => 'Rudern';

  @override
  String get exerciseTypePaddling => 'Paddeln';

  @override
  String get exerciseTypeSkiing => 'Skifahren';

  @override
  String get exerciseTypeSnowboarding => 'Snowboarden';

  @override
  String get exerciseTypeSnowshoeing => 'Schneeschuhwandern';

  @override
  String get exerciseTypeSkating => 'Skaten';

  @override
  String get exerciseTypeSailing => 'Segeln';

  @override
  String get exerciseTypeSurfing => 'Surfen';

  @override
  String get exerciseTypeSwimmingOpenWater => 'Schwimmen (Freiwasser)';

  @override
  String get exerciseTypeGolf => 'Golf';

  @override
  String get exerciseTypeStrengthTraining => 'Krafttraining';

  @override
  String get exerciseTypeTreadmill => 'Laufband';

  @override
  String get exerciseTypePushUps => 'Liegestütze';

  @override
  String get exerciseTypePullUps => 'Klimmzüge';

  @override
  String get exerciseTypeRopeSkipping => 'Seilspringen';

  @override
  String get exerciseTypeTrampolineJumping => 'Trampolinspringen';

  @override
  String get exerciseTypeOtherWorkout => 'Anderes Training';

  @override
  String get mindfulnessEntryUnavailable =>
      'Achtsamkeitssitzungen sind bei diesem Health-Connect-Anbieter nicht verfügbar.';

  @override
  String get mindfulnessEntryTimerTitle => 'Timer';

  @override
  String get mindfulnessEntryManualTitle => 'Manueller Eintrag';

  @override
  String get mindfulnessEntryIntervalBell => 'Intervallglocke';

  @override
  String get mindfulnessEntryIntervalMinutes => 'Intervall (min)';

  @override
  String get mindfulnessEntryBellSound => 'Glockenklang';

  @override
  String get mindfulnessEntryBackgroundSound => 'Hintergrundklang';

  @override
  String get mindfulnessBellStruck => 'Sanfter Anschlag';

  @override
  String get mindfulnessBellRubbed => 'Warme Klangschale';

  @override
  String get mindfulnessBellBright => 'Helle Klangschale';

  @override
  String get mindfulnessBellTemple => 'Tempel-Klangschale';

  @override
  String get mindfulnessBellHarmony => 'Harmonie';

  @override
  String get mindfulnessBackgroundNone => 'Keiner';

  @override
  String get mindfulnessBackgroundBowl => 'Klangschale';

  @override
  String get mindfulnessBackgroundMeditation => 'Meditation';

  @override
  String get mindfulnessBackgroundChimes => 'Glockenspiel';

  @override
  String get mindfulnessBackgroundDreamscape => 'Traumlandschaft';

  @override
  String get mindfulnessEntryStartTimer => 'Starten';

  @override
  String get mindfulnessEntryStopTimer => 'Stoppen';

  @override
  String get mindfulnessEntryResumeTimer => 'Fortsetzen';

  @override
  String get mindfulnessEntryDiscardTimer => 'Verwerfen';

  @override
  String get mindfulnessEntrySaveSession => 'Sitzung speichern';

  @override
  String get mindfulnessEntryMinutes => 'Minuten';

  @override
  String get mindfulnessEntryAddMinutes => 'Minuten hinzufügen';

  @override
  String get mindfulnessEntryInvalidTimer =>
      'Gib eine gültige Timerdauer und ein gültiges Intervall ein.';

  @override
  String get mindfulnessEntryInvalidManual =>
      'Gib gültige Achtsamkeitsminuten ein.';

  @override
  String get mindfulnessEntryTimerTooShort =>
      'Die Meditation muss mindestens 1 Minute dauern, um gespeichert zu werden.';

  @override
  String mindfulnessEntryWriteFailed(String arg0) {
    return 'Achtsamkeitssitzung konnte nicht gespeichert werden: $arg0';
  }

  @override
  String get mindfulnessEntryCompleted => 'Timer abgeschlossen';

  @override
  String get mindfulnessRemindersTitle => 'Achtsamkeitserinnerungen';

  @override
  String get mindfulnessRemindersSummaryOff =>
      'Standardmäßig aus. Aktiviere eine tägliche Erinnerung für dein Achtsamkeitsziel.';

  @override
  String mindfulnessRemindersSummaryOn(String arg0) {
    return 'Täglich um $arg0';
  }

  @override
  String get mindfulnessRemindersPermissionNeeded =>
      'Erteile die Benachrichtigungsberechtigung, um Achtsamkeitserinnerungen zu aktivieren.';

  @override
  String get mindfulnessRemindersTime => 'Erinnerungszeit';

  @override
  String get mindfulnessRemindersGoalNote =>
      'Erinnerungen pausieren, sobald das heutige Achtsamkeitsziel erreicht ist, und werden morgen fortgesetzt.';

  @override
  String get mindfulnessReminderNotificationChannel =>
      'Achtsamkeitserinnerungen';

  @override
  String get mindfulnessReminderNotificationChannelDesc =>
      'Optionale Erinnerungen, dein tägliches Achtsamkeitsziel zu erreichen.';

  @override
  String get mindfulnessReminderNotificationTitle => 'Achtsamkeitserinnerung';

  @override
  String mindfulnessReminderNotificationBody(String arg0) {
    return 'Dein Ziel heute ist $arg0. Nimm dir eine achtsame Pause, wenn du kannst.';
  }

  @override
  String mindfulnessReminderNotificationProgress(String arg0, String arg1) {
    return '$arg0 / $arg1';
  }

  @override
  String bodyEntrySubtitle(String arg0) {
    return '$arg0-Einträge werden direkt in Health Connect gespeichert.';
  }

  @override
  String bodyEntryPermissionNeeded(String arg0) {
    return 'Zum Hinzufügen von $arg0-Einträgen benötigt OpenVitals die Health-Connect-Schreibberechtigung. Die App speichert diese Daten nicht; Einträge werden in Health Connect gespeichert.';
  }

  @override
  String bodyEntryValueLabel(String arg0, String arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String bodyEntryAddSelected(String arg0) {
    return '$arg0 hinzufügen';
  }

  @override
  String get bodyEntryInvalidValue =>
      'Gib einen gültigen Wert für diese Messung ein.';

  @override
  String bodyEntryWriteFailed(String arg0) {
    return 'Körpermessung konnte nicht gespeichert werden: $arg0';
  }

  @override
  String get carbsEntrySubtitle =>
      'Kohlenhydrateinträge werden direkt in Health Connect gespeichert.';

  @override
  String get carbsEntryPermissionNeeded =>
      'Zum Hinzufügen von Kohlenhydrateinträgen benötigt OpenVitals die Health-Connect-Schreibberechtigung. Die App speichert diese Daten nicht; Einträge werden in Health Connect gespeichert.';

  @override
  String carbsEntryValueLabel(String arg0) {
    return 'Kohlenhydrate ($arg0)';
  }

  @override
  String get carbsEntryAdd => 'Kohlenhydrate hinzufügen';

  @override
  String get carbsEntryInvalidValue =>
      'Gib eine gültige Kohlenhydratmenge ein.';

  @override
  String carbsEntryWriteFailed(String arg0) {
    return 'Kohlenhydrate konnten nicht gespeichert werden: $arg0';
  }

  @override
  String vitalsEntrySubtitle(String arg0) {
    return '$arg0-Einträge werden direkt in Health Connect gespeichert.';
  }

  @override
  String vitalsEntryPermissionNeeded(String arg0) {
    return 'Zum Hinzufügen von $arg0-Einträgen benötigt OpenVitals die Health-Connect-Schreibberechtigung. Die App speichert diese Daten nicht; Einträge werden in Health Connect gespeichert.';
  }

  @override
  String vitalsEntryValueLabel(String arg0, String arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String get vitalsEntrySystolicLabel => 'Systolisch (mmHg)';

  @override
  String get vitalsEntryDiastolicLabel => 'Diastolisch (mmHg)';

  @override
  String vitalsEntryAddSelected(String arg0) {
    return '$arg0 hinzufügen';
  }

  @override
  String get vitalsEntryInvalidValue =>
      'Gib einen gültigen Wert für diesen Vitalwert ein.';

  @override
  String vitalsEntryWriteFailed(String arg0) {
    return 'Vitalwert konnte nicht gespeichert werden: $arg0';
  }

  @override
  String get rangeDay => 'Tag';

  @override
  String get rangeWeek => 'Woche';

  @override
  String get rangeMonth => 'Monat';

  @override
  String get rangeYear => 'Jahr';

  @override
  String get periodToday => 'Heute';

  @override
  String get periodYesterday => 'Gestern';

  @override
  String get periodThisWeek => 'Diese Woche';

  @override
  String periodWeekOf(String arg0) {
    return 'Woche vom $arg0';
  }

  @override
  String get periodThisMonth => 'Dieser Monat';

  @override
  String get periodThisYear => 'Dieses Jahr';

  @override
  String get periodLast7Days => 'Letzte 7 Tage';

  @override
  String get periodLast30Days => 'Letzte 30 Tage';

  @override
  String get periodLast365Days => 'Letzte 365 Tage';

  @override
  String get periodSelected => 'Ausgewählter Zeitraum';

  @override
  String get metricSteps => 'Schritte';

  @override
  String get metricDistance => 'Distanz';

  @override
  String get metricAveragePace => 'Durchschnittliches Tempo';

  @override
  String get metricAverageSpeed => 'Durchschnittsgeschwindigkeit';

  @override
  String get metricCaloriesBurned => 'Verbrannte Gesamtkalorien';

  @override
  String get metricCaloriesOut => 'Gesamtkalorien';

  @override
  String get metricCaloriesIn => 'Kalorienaufnahme';

  @override
  String get metricFloorsClimbed => 'Etagen gestiegen';

  @override
  String get metricActiveCalories => 'Aktive Kalorien';

  @override
  String get metricElevation => 'Höhe';

  @override
  String get metricElevationGained => 'Höhenmeter';

  @override
  String get metricWheelchairPushes => 'Rollstuhlschübe';

  @override
  String get metricWorkout => 'Training';

  @override
  String get metricSleep => 'Schlaf';

  @override
  String get metricHydration => 'Hydrierung';

  @override
  String get metricTotalHydration => 'Gesamthydrierung';

  @override
  String get metricHydrationTrend => 'Hydrierungstrend';

  @override
  String get metricLoggedDays => 'Protokollierte Tage';

  @override
  String get metricLatestWeight => 'Letztes Gewicht';

  @override
  String get metricBodyFat => 'Körperfett';

  @override
  String get metricAvgHeartRate => 'Durchschn. Herzfrequenz';

  @override
  String get metricAverageHeartRate => 'Durchschnittliche Herzfrequenz';

  @override
  String get metricRestingHeartRate => 'Ruheherzfrequenz';

  @override
  String get metricHrv => 'Herzfrequenzvariabilität (HRV)';

  @override
  String get metricCardioLoad => 'Cardio-Belastung';

  @override
  String get metricWeeklyCardioLoad => 'Wöchentliches Cardio';

  @override
  String get metricEnergyBurned => 'Gesamtkalorien';

  @override
  String get metricBloodPressure => 'Blutdruck';

  @override
  String get metricSpo2 => 'SpO2';

  @override
  String get metricOxygenSaturation => 'Sauerstoffsättigung';

  @override
  String get metricVo2Max => 'VO2 max';

  @override
  String get metricMindfulness => 'Achtsamkeit';

  @override
  String get metricTotalMindfulness => 'Achtsamkeit gesamt';

  @override
  String get metricCycle => 'Zyklus';

  @override
  String get metricCycleTracking => 'Zyklusverfolgung';

  @override
  String get metricPeriodDays => 'Menstruationstage';

  @override
  String get metricOvulationTests => 'Ovulationstests';

  @override
  String get metricLatestBbt => 'Letzte Basaltemperatur';

  @override
  String get metricWeight => 'Gewicht';

  @override
  String get metricHeight => 'Größe';

  @override
  String get metricBmi => 'BMI';

  @override
  String get metricFfmi => 'FFMI';

  @override
  String get metricLeanMass => 'Fettfreie Masse';

  @override
  String get metricBmr => 'BMR';

  @override
  String get metricBoneMass => 'Knochenmasse';

  @override
  String get metricBodyWaterMass => 'Körperwassermasse';

  @override
  String get metricLatest => 'Zuletzt';

  @override
  String get metricChange => 'Änderung';

  @override
  String get metricMacros => 'Makros';

  @override
  String get metricProtein => 'Protein';

  @override
  String get metricCarbs => 'Kohlenhydrate';

  @override
  String get metricFat => 'Fett';

  @override
  String get metricDietaryFiber => 'Ballaststoffe';

  @override
  String get metricSugar => 'Zucker';

  @override
  String get metricEnergyFromFat => 'Kalorien aus Fett';

  @override
  String get metricMonounsaturatedFat => 'Einfach ungesättigtes Fett';

  @override
  String get metricPolyunsaturatedFat => 'Mehrfach ungesättigtes Fett';

  @override
  String get metricSaturatedFat => 'Gesättigtes Fett';

  @override
  String get metricTransFat => 'Transfett';

  @override
  String get metricUnsaturatedFat => 'Ungesättigtes Fett';

  @override
  String get metricCholesterol => 'Cholesterin';

  @override
  String get metricBiotin => 'Biotin';

  @override
  String get metricFolate => 'Folat';

  @override
  String get metricFolicAcid => 'Folsäure';

  @override
  String get metricNiacin => 'Niacin';

  @override
  String get metricPantothenicAcid => 'Pantothensäure';

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
  String get metricChloride => 'Chlorid';

  @override
  String get metricChromium => 'Chrom';

  @override
  String get metricCopper => 'Kupfer';

  @override
  String get metricIodine => 'Jod';

  @override
  String get metricIron => 'Eisen';

  @override
  String get metricMagnesium => 'Magnesium';

  @override
  String get metricManganese => 'Mangan';

  @override
  String get metricMolybdenum => 'Molybdän';

  @override
  String get metricPhosphorus => 'Phosphor';

  @override
  String get metricPotassium => 'Kalium';

  @override
  String get metricSelenium => 'Selen';

  @override
  String get metricSodium => 'Natrium';

  @override
  String get metricZinc => 'Zink';

  @override
  String get metricCaffeine => 'Koffein';

  @override
  String get metricRespiratoryRate => 'Atemfrequenz';

  @override
  String get metricAvgRespiratoryRate => 'Durchschn. Atemfrequenz';

  @override
  String get metricBodyTemp => 'Körpertemp.';

  @override
  String get metricBloodGlucose => 'Blutzucker';

  @override
  String get metricSkinTemperature => 'Hauttemperatur';

  @override
  String get metricRecordedSpeed => 'Aufgezeichnete Geschwindigkeit';

  @override
  String get metricAveragePower => 'Durchschnittsleistung';

  @override
  String get metricStepsCadence => 'Schrittfrequenz';

  @override
  String get metricCyclingCadence => 'Trittfrequenz';

  @override
  String get unitSteps => 'Schritte';

  @override
  String get unitReps => 'Wdh.';

  @override
  String get unitPushes => 'Schübe';

  @override
  String get unitFloors => 'Etagen';

  @override
  String get unitDays => 'Tage';

  @override
  String get unitNights => 'Nächte';

  @override
  String get unitTests => 'Tests';

  @override
  String get unitTotal => 'gesamt';

  @override
  String get unitGrams => 'g';

  @override
  String get sectionActivities => 'Aktivitäten';

  @override
  String get sectionActivityTypeStats => 'Nach Aktivitätstyp';

  @override
  String activityTypeStatsActivityCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '# Aktivitäten',
      one: '# Aktivität',
    );
    return '$_temp0';
  }

  @override
  String get sectionPlannedWorkouts => 'Geplante Trainings';

  @override
  String get activitiesFilterActivityTypeLabel => 'Aktivitätstyp';

  @override
  String get activitiesFilterAll => 'Alle Aktivitäten';

  @override
  String get activitiesKeyMetrics => 'Wichtige Messwerte';

  @override
  String get recoverySleepScore => 'Schlafwert';

  @override
  String get recoverySleepDuration => 'Schlafdauer';

  @override
  String get recoverySleepSchedule => 'Schlafplan';

  @override
  String get recoveryRemSleep => 'REM-Schlaf';

  @override
  String get recoveryDeepSleep => 'Tiefschlaf';

  @override
  String get recoverySleepEfficiency => 'Schlafeffizienz';

  @override
  String get sleepScoreConfidenceHigh => 'Hohe Sicherheit';

  @override
  String get sleepScoreConfidenceMedium => 'Mittlere Sicherheit';

  @override
  String get sleepScoreConfidenceLow => 'Niedrige Sicherheit';

  @override
  String get sleepScoreConfidenceNoData => 'Keine Daten';

  @override
  String get sleepScoreRatingExcellent => 'Ausgezeichnet';

  @override
  String get sleepScoreRatingGood => 'Gut';

  @override
  String get sleepScoreRatingFair => 'Okay';

  @override
  String get sleepScoreRatingPoor => 'Schwach';

  @override
  String dashboardSleepScoreSubtitle(String arg0, String arg1) {
    return '$arg0 • $arg1';
  }

  @override
  String get sleepScoreCalculationTitle => 'So wird es berechnet';

  @override
  String get sleepScoreDayNumbersTitle => 'Heutige Werte';

  @override
  String get sleepScoreReferencesTitle => 'Belegte Links';

  @override
  String get sleepScoreCalculationSummary =>
      'OpenVitals bewertet objektive Schlafgesundheit anhand von Dauer, Effizienz, Kontinuität und Regelmäßigkeit. Es diagnostiziert keine Schlafstörungen.';

  @override
  String get sleepScoreFormula =>
      'Schlafwert = Dauer 35 + Effizienz 30 + Kontinuität 20 + Regelmäßigkeit 15';

  @override
  String get sleepScoreFormulaBody =>
      'Die Dauer erhält volle Punkte bei 7-9 h. Die Effizienz nutzt Gesamtschlafzeit geteilt durch Zeit im Bett. Die Kontinuität nutzt Wachzeit nach Schlafbeginn. Die Regelmäßigkeit vergleicht den heutigen Schlafmittelpunkt mit den letzten Nächten.';

  @override
  String get sleepScoreComponentsBody =>
      'Schlafphasendaten erhöhen die Sicherheit, aber REM und Tiefschlaf werden nicht stark gewichtet, weil Phasenschätzungen von Verbrauchergeräten variieren können. Wenn Regelmäßigkeitsverlauf fehlt, nutzt OpenVitals einen neutralen Wert und senkt die Sicherheit.';

  @override
  String get sleepScoreNotDiagnostic =>
      'Dieser Wert ist eine tägliche Orientierung aus Health-Connect-Datensätzen, keine Diagnose und keine Behandlungsempfehlung.';

  @override
  String get sleepScoreComponentDuration => 'Dauer';

  @override
  String get sleepScoreComponentEfficiency => 'Effizienz';

  @override
  String get sleepScoreComponentContinuity => 'Kontinuität';

  @override
  String get sleepScoreComponentRegularity => 'Regelmäßigkeit';

  @override
  String get sleepScoreTotalSleep => 'Gesamtschlaf';

  @override
  String get sleepScoreTimeInBed => 'Zeit im Bett';

  @override
  String get sleepScoreEfficiency => 'Effizienz';

  @override
  String get sleepScoreWaso => 'Wach nach Schlafbeginn';

  @override
  String get sleepScoreRegularity => 'Timing-Abweichung';

  @override
  String get sleepScoreBaselineNights => 'Basisnächte';

  @override
  String get sleepScoreStageRecords => 'Phasendatensätze';

  @override
  String get sleepScoreQualityNoData =>
      'Unzureichende Schlafdaten für einen Wert.';

  @override
  String get sleepScoreQualityStageAwake =>
      'Nutzt Schlafphasen und Wachphasen aus Health Connect.';

  @override
  String get sleepScoreQualityStageOnly =>
      'Nutzt Schlafphasen; Wachkontinuität kann geschätzt sein.';

  @override
  String get sleepScoreQualitySessionOnly =>
      'Nutzt nur das Timing der Schlafsitzung; die Sicherheit ist begrenzt.';

  @override
  String get sleepScoreReferenceAasm => 'AASM-Schlafdauer für Erwachsene';

  @override
  String get sleepScoreReferenceSleepHealth =>
      'Mehrdimensionale Schlafgesundheit';

  @override
  String get sleepScoreReferenceEfficiency => 'Definition der Schlafeffizienz';

  @override
  String get sleepScoreReferenceRegularity =>
      'Forschung zur Schlafregelmäßigkeit';

  @override
  String get sleepEfficiencyConfidenceHigh => 'Hohe Sicherheit';

  @override
  String get sleepEfficiencyConfidenceLow => 'Niedrige Sicherheit';

  @override
  String get sleepEfficiencyConfidenceNoData => 'Keine Daten';

  @override
  String get sleepEfficiencyCalculationTitle => 'So wird es berechnet';

  @override
  String get sleepEfficiencyDayNumbersTitle => 'Heutige Werte';

  @override
  String get sleepEfficiencyReferencesTitle => 'Belegte Links';

  @override
  String get sleepEfficiencyCalculationSummary =>
      'Schlafeffizienz ist der Prozentsatz des Schlaffensters, der schlafend verbracht wird. Höhere Werte bedeuten meist weniger Wachzeit im Bett.';

  @override
  String get sleepEfficiencyFormula =>
      'Schlafeffizienz = Gesamtschlaf / Zeit im Bett x 100';

  @override
  String get sleepEfficiencyFormulaBody =>
      'Gesamtschlaf ist die Summe der Health-Connect-Schlafphasen, wenn Phasen verfügbar sind. Zeit im Bett ist das Start-bis-Ende-Fenster der Hauptschlafsitzung.';

  @override
  String get sleepEfficiencyDataBody =>
      'Wenn Schlafphasen fehlen, liefert Health Connect möglicherweise nur eine Sitzungsdauer. OpenVitals kann trotzdem eine Schätzung anzeigen, aber die Sicherheit ist niedrig, weil Wachzeit im Bett fehlen kann.';

  @override
  String get sleepEfficiencyNotDiagnostic =>
      'Schlafeffizienz ist ein Signal für Schlafkontinuität, keine Diagnose. Dauerhaft niedrige Werte können ein Anlass für ärztliche Abklärung sein.';

  @override
  String get sleepEfficiencyQualityNoData =>
      'Unzureichende Schlafdaten für Effizienz.';

  @override
  String get sleepEfficiencyQualityStageBased =>
      'Nutzt Health-Connect-Schlafphasen für den Gesamtschlaf.';

  @override
  String get sleepEfficiencyQualitySessionOnly =>
      'Nutzt nur das Timing der Sitzung; Wachzeit kann fehlen.';

  @override
  String get sleepEfficiencyReferenceDefinition =>
      'Definition der Schlafeffizienz';

  @override
  String get sleepEfficiencyReferenceDenominator =>
      'Forschung zum Effizienz-Nenner';

  @override
  String get sleepEfficiencyReferenceMethods =>
      'Review zu Schlafbewertungsmethoden';

  @override
  String get cardioLoadConfidenceHigh => 'Hohe Sicherheit';

  @override
  String get cardioLoadConfidenceMedium => 'Mittlere Sicherheit';

  @override
  String get cardioLoadConfidenceLow => 'Niedrige Sicherheit';

  @override
  String get cardioLoadConfidenceNoData => 'Keine Daten';

  @override
  String get cardioLoadCalculationTitle => 'So wird es berechnet';

  @override
  String get cardioLoadDayNumbersTitle => 'Heutige Werte';

  @override
  String get cardioLoadReferencesTitle => 'Belegte Links';

  @override
  String get cardioLoadCalculationSummary =>
      'OpenVitals verwendet herzfrequenzbasiertes TRIMP, wenn Herzfrequenzdaten verfügbar sind, und nutzt Bewegung nur als Ersatz, wenn HF nicht verwendbar ist.';

  @override
  String get cardioLoadFormula =>
      'TRIMP = Minuten x HRR x 0.64 x e^(1.92 x HRR)';

  @override
  String get cardioLoadFormulaBody =>
      'HRR ist die Herzfrequenzreserve: (Herzfrequenz - Ruheherzfrequenz) / (maximale Herzfrequenz - Ruheherzfrequenz). OpenVitals summiert dies über verfügbare Herzfrequenzintervalle des Tages.';

  @override
  String get cardioLoadMappingBody =>
      'Wenn aufgezeichnete Aktivitäten vorhanden sind, werden Herzfrequenzproben per Zeitstempel dem Start- und Endfenster jeder Aktivität zugeordnet. Ohne Aktivitätsfenster zählen nur Intervalle mit erhöhter Herzfrequenz. Wenn HF nicht nutzbar ist, werden Bewegung und aktive Kalorien als niedrig vertrauenswürdiger Ersatz angezeigt.';

  @override
  String get cardioLoadMethod => 'Methode';

  @override
  String get cardioLoadTrimpScore => 'TRIMP-Wert';

  @override
  String get cardioLoadHrCoverage => 'HF-Abdeckung';

  @override
  String get cardioLoadExpectedCoverage => 'Erwartete Abdeckung';

  @override
  String get cardioLoadRestingHr => 'Ruhe-HF';

  @override
  String get cardioLoadMaxHr => 'Max. HF';

  @override
  String get cardioLoadHrSamples => 'HF-Proben';

  @override
  String get cardioLoadActivityWindows => 'Aktivitätsfenster';

  @override
  String get cardioLoadActivityMinutes => 'Aktivitätsminuten';

  @override
  String get cardioLoadMovementFallback => 'Bewegungsersatz';

  @override
  String get cardioLoadMethodActivityWindows => 'TRIMP aus Aktivitäts-HF';

  @override
  String get cardioLoadMethodElevatedHr => 'TRIMP aus erhöhter HF';

  @override
  String get cardioLoadMethodMovementFallback => 'Bewegungsersatz';

  @override
  String get cardioLoadMethodNoData => 'Unzureichende Daten';

  @override
  String get cardioLoadCalibrationObservedResting => 'Ruhe-HF beobachtet';

  @override
  String get cardioLoadCalibrationEstimatedResting => 'Ruhe-HF geschätzt';

  @override
  String get cardioLoadCalibrationObservedMax => 'Max. HF beobachtet';

  @override
  String get cardioLoadCalibrationEstimatedMax => 'Max. HF geschätzt';

  @override
  String get cardioLoadReferenceBanister => 'Banister-TRIMP-Gleichung';

  @override
  String get cardioLoadReferenceTrainingLoad => 'Review zur Trainingsbelastung';

  @override
  String get cardioLoadReferenceHealthConnect =>
      'Health-Connect-HF-Zuordnung für Trainings';

  @override
  String get sectionSleepSessions => 'Schlafsitzungen';

  @override
  String get sectionWeight => 'Gewicht';

  @override
  String get sectionEntries => 'Einträge';

  @override
  String get sectionMeals => 'Mahlzeiten';

  @override
  String get sectionSessions => 'Sitzungen';

  @override
  String get sectionDailyBreakdown => 'Tägliche Aufschlüsselung';

  @override
  String get sectionVitals => 'Vitalwerte';

  @override
  String get sectionHeart => 'Herz';

  @override
  String get sectionCardiovascular => 'Kardiovaskulär';

  @override
  String get sectionRespiratory => 'Atmung';

  @override
  String get sectionRespiratoryRateDailyBreakdown =>
      'Tägliche Aufschlüsselung der Atemfrequenz';

  @override
  String get sectionVo2MaxHistory => 'VO2-max-Verlauf';

  @override
  String get sectionDisplay => 'Anzeige';

  @override
  String get sectionPrivacy => 'Datenschutz';

  @override
  String get sectionCycleCalendar => 'Zykluskalender';

  @override
  String get sectionBasalBodyTemperature => 'Basaltemperatur';

  @override
  String get sectionStatistics => 'Statistiken';

  @override
  String get sectionCalorieTrends => 'Kalorientrends';

  @override
  String get sectionNutritionTrends => 'Ernährungstrends';

  @override
  String get sectionBodyTrends => 'Körpertrends';

  @override
  String get sectionCarbohydrates => 'Kohlenhydrate';

  @override
  String get sectionFats => 'Fette';

  @override
  String get sectionVitamins => 'Vitamine';

  @override
  String get sectionMinerals => 'Mineralstoffe';

  @override
  String get sectionOtherNutrients => 'Weitere Nährstoffe';

  @override
  String summaryDailyAverage(String arg0) {
    return '$arg0 Tagesdurchschnitt';
  }

  @override
  String summaryDaysInRange(String arg0) {
    return '$arg0 Tage im Zeitraum';
  }

  @override
  String summaryEntries(String arg0) {
    return '$arg0 Einträge';
  }

  @override
  String summaryReadings(String arg0) {
    return '$arg0 Messwerte';
  }

  @override
  String summaryNights(String arg0) {
    return '$arg0 Nächte';
  }

  @override
  String summaryRecordedStages(String arg0) {
    return '$arg0 aufgezeichnete Phasen';
  }

  @override
  String get summaryAverage => 'Durchschn.';

  @override
  String summaryAvgValue(String arg0) {
    return 'Durchschn. $arg0';
  }

  @override
  String summaryAvgValueRange(String arg0, String arg1, String arg2) {
    return 'Durchschn. $arg0 · Bereich $arg1-$arg2';
  }

  @override
  String summaryValueAvg(String arg0) {
    return '$arg0 durchschn.';
  }

  @override
  String get summaryRange => 'Bereich';

  @override
  String get summarySamples => 'Proben';

  @override
  String summaryRecorded(String arg0, String arg1) {
    return '$arg0-$arg1 aufgezeichnet';
  }

  @override
  String summaryRestingValue(String arg0) {
    return 'Ruhewert $arg0';
  }

  @override
  String summaryHrvValue(String arg0) {
    return 'HRV $arg0';
  }

  @override
  String summaryLastUpdate(String arg0) {
    return 'Letzte Aktualisierung $arg0';
  }

  @override
  String get summaryNow => 'Jetzt';

  @override
  String summaryToday(String arg0) {
    return '$arg0 heute';
  }

  @override
  String summaryOnDate(String arg0, String arg1) {
    return '$arg0 am $arg1';
  }

  @override
  String summaryEmptyToday(String arg0) {
    return '$arg0 heute bisher.';
  }

  @override
  String summaryEmptyDay(String arg0) {
    return '$arg0 an diesem Tag.';
  }

  @override
  String get summaryAcrossSelectedPeriod => 'Im ausgewählten Zeitraum';

  @override
  String summaryLatestTemperature(String arg0, String arg1) {
    return 'Letzte $arg0 · $arg1';
  }

  @override
  String summaryTemperatureRange(String arg0, String arg1, String arg2) {
    return 'Bereich $arg0-$arg1 · $arg2 Messwerte';
  }

  @override
  String get summarySleepEndingToday => 'Schlaf endet heute';

  @override
  String summarySleepEndingOn(String arg0) {
    return 'Schlaf endet am $arg0';
  }

  @override
  String get statTotal => 'Gesamt';

  @override
  String get statTime => 'Zeit';

  @override
  String get statActiveDays => 'Aktive Tage';

  @override
  String get statAverage => 'Durchschnitt';

  @override
  String get statLowest => 'Niedrigster Wert';

  @override
  String get statHighest => 'Höchster Wert';

  @override
  String get statReadings => 'Messwerte';

  @override
  String get statDailyAverage => 'Tagesdurchschnitt';

  @override
  String get caloriesStatActiveAverage => 'Aktiver Durchschnitt';

  @override
  String get caloriesStatBmrReadings => 'BMR-Messwerte';

  @override
  String get statAverageDuration => 'Durchschnittliche Dauer';

  @override
  String get statTotalIntake => 'Gesamtaufnahme';

  @override
  String get statBestDay => 'Bester Tag';

  @override
  String get statNightsLogged => 'Protokollierte Nächte';

  @override
  String get statLongestSleep => 'Längster Schlaf';

  @override
  String get statLongestWorkout => 'Längstes Training';

  @override
  String get statAverageMovingPace => 'Durchschn. Tempo in Bewegung';

  @override
  String get statFastestPace => 'Schnellstes Tempo';

  @override
  String get statBestSpeed => 'Beste Geschwindigkeit';

  @override
  String get statLongestSession => 'Längste Sitzung';

  @override
  String get statBbtReadings => 'Basaltemperatur-Messwerte';

  @override
  String get statGoalStreak => 'Zielserie';

  @override
  String get statLongestGoalStreak => 'Längste Serie';

  @override
  String get statGoalsMet => 'Ziele erreicht';

  @override
  String get statSuccessRate => 'Erfolgsrate';

  @override
  String get statAverageGap => 'Durchschn. Abstand';

  @override
  String get statVsPreviousDay => 'Gegenüber vorherigem Tag';

  @override
  String get statVsPreviousWeek => 'Gegenüber vorheriger Woche';

  @override
  String get statVsPreviousMonth => 'Gegenüber vorherigem Monat';

  @override
  String get statVsPreviousYear => 'Gegenüber vorherigem Jahr';

  @override
  String get statBaseline => 'Basiswert';

  @override
  String get stat30DayBaseline => '30-Tage-Durchschnitt';

  @override
  String get stat60DayBaseline => '60-Tage-Durchschnitt';

  @override
  String get stat90DayBaseline => '90-Tage-Durchschnitt';

  @override
  String get statUsualRange => 'Üblicher Bereich';

  @override
  String get statBaselineDeviation => 'Abweichung vom Basiswert';

  @override
  String get baselineStatusUsual => 'Üblich';

  @override
  String get baselineStatusAbove => 'Darüber';

  @override
  String get baselineStatusBelow => 'Darunter';

  @override
  String get baselineStatusUnusualHigh => 'Ungewöhnlich hoch';

  @override
  String get baselineStatusUnusualLow => 'Ungewöhnlich niedrig';

  @override
  String get sectionMetricContext => 'Kontext';

  @override
  String get interpretationBpTitle => 'Blutdruckkategorie';

  @override
  String get interpretationBpNormal => 'Normal';

  @override
  String get interpretationBpElevated => 'Erhöht';

  @override
  String get interpretationBpStage1 => 'Bluthochdruck Stufe 1';

  @override
  String get interpretationBpStage2 => 'Bluthochdruck Stufe 2';

  @override
  String get interpretationBpSevere => 'Referenz für schweren Bereich';

  @override
  String interpretationBpBody(String arg0) {
    return 'Dieser Messwert liegt im Bereich $arg0. Ein einzelner App-Messwert ist keine Diagnose.';
  }

  @override
  String get interpretationBpSevereBody =>
      'Dieser Messwert liegt über der Referenz für den schweren Bereich. Miss erneut; suche dringend medizinische Hilfe, wenn Symptome auftreten oder der Wert sehr hoch bleibt.';

  @override
  String get interpretationBpSource =>
      'Quelle: Blutdruckkategorien für Erwachsene der American Heart Association.';

  @override
  String get interpretationBmiTitle => 'BMI-Kategorie';

  @override
  String get interpretationBmiUnderweight => 'Untergewicht';

  @override
  String get interpretationBmiHealthy => 'Gesundes Gewicht';

  @override
  String get interpretationBmiOverweight => 'Übergewicht';

  @override
  String get interpretationBmiObesity1 => 'Adipositas Grad 1';

  @override
  String get interpretationBmiObesity2 => 'Adipositas Grad 2';

  @override
  String get interpretationBmiObesity3 => 'Adipositas Grad 3';

  @override
  String get interpretationBmiBody =>
      'Nur BMI-Screeningkategorie für Erwachsene; BMI misst nicht die Körperzusammensetzung.';

  @override
  String get interpretationBmiSource =>
      'Quelle: BMI-Kategorien für Erwachsene der CDC.';

  @override
  String get interpretationFfmiTitle => 'FFMI-Kategorie';

  @override
  String get interpretationFfmiBelowAverage => 'Unterdurchschnittlich';

  @override
  String get interpretationFfmiAverage => 'Durchschnittlich';

  @override
  String get interpretationFfmiAboveAverage => 'Überdurchschnittlich';

  @override
  String get interpretationFfmiExcellent => 'Ausgezeichnet';

  @override
  String get interpretationFfmiSuperior => 'Sehr hoch';

  @override
  String get interpretationFfmiExceptional => 'Außergewöhnlich';

  @override
  String get interpretationFfmiElite => 'Elite';

  @override
  String interpretationFfmiBody(String arg0, String arg1) {
    return 'FFMI $arg0; angepasster FFMI $arg1. Verwendet dein neuestes Gewicht, Körperfett und deine Größe.';
  }

  @override
  String get interpretationFfmiSource =>
      'Quelle: indikative angepasste FFMI-Kategorien von ffmicalculators.com.';

  @override
  String get interpretationSleepTitle => 'Schlafziel';

  @override
  String get interpretationSleepBelow => 'Unter Ziel';

  @override
  String get interpretationSleepNear => 'Nahe am Ziel';

  @override
  String get interpretationSleepMet => 'Ziel erreicht';

  @override
  String interpretationSleepBelowBody(String arg0) {
    return 'Der durchschnittliche Schlaf liegt $arg0 unter deinem konfigurierten Ziel.';
  }

  @override
  String interpretationSleepNearBody(String arg0, String arg1) {
    return 'Der durchschnittliche Schlaf liegt nahe an deinem konfigurierten Ziel: $arg0 vs. $arg1.';
  }

  @override
  String interpretationSleepMetBody(String arg0, String arg1) {
    return 'Der durchschnittliche Schlaf erreicht dein konfiguriertes Ziel: $arg0 vs. $arg1.';
  }

  @override
  String get interpretationSleepSource =>
      'Basierend auf deinem konfigurierten Schlafziel, keine medizinische Schlafbewertung.';

  @override
  String get interpretationMacroTitle => 'Makroverteilung';

  @override
  String get interpretationMacroWithin => 'Innerhalb der Referenzverteilung';

  @override
  String get interpretationMacroOutside => 'Außerhalb der Referenzverteilung';

  @override
  String interpretationMacroBody(String arg0, String arg1, String arg2) {
    return 'Protein $arg0, Kohlenhydrate $arg1, Fett $arg2 der protokollierten Makrokalorien.';
  }

  @override
  String get interpretationMacroSource =>
      'Quelle: AMDR-Referenz der National Academies für Erwachsene; nur protokollierte Makros.';

  @override
  String get interpretationWorkoutTitle =>
      'Fortschritt bei Trainingsrichtlinie';

  @override
  String get interpretationWorkoutNone => 'Keine protokollierten Minuten';

  @override
  String get interpretationWorkoutBelow => 'Unter wöchentlicher Referenz';

  @override
  String get interpretationWorkoutApproaching =>
      'Nähert sich wöchentlicher Referenz';

  @override
  String get interpretationWorkoutMet => 'Wöchentliche Referenz erreicht';

  @override
  String interpretationWorkoutBody(String arg0, String arg1) {
    return '$arg0 im Verhältnis zur Erwachsenenreferenz von 150 min/Woche protokolliert ($arg1). Die Intensität wird nicht geprüft.';
  }

  @override
  String interpretationWorkoutBodyWeeklyAverage(String arg0, String arg1) {
    return 'Wöchentlicher Durchschnitt $arg0 im Verhältnis zur Erwachsenenreferenz von 150 min/Woche ($arg1). Die Intensität wird nicht geprüft.';
  }

  @override
  String get interpretationWorkoutSource =>
      'Quelle: Referenz der HHS-Richtlinie für körperliche Aktivität bei Erwachsenen.';

  @override
  String get interpretationVitalTitle => 'Vitalwertkontext';

  @override
  String get interpretationVitalWithin =>
      'Innerhalb der breiten Erwachsenenreferenz';

  @override
  String get interpretationVitalBelow => 'Unter breiter Erwachsenenreferenz';

  @override
  String get interpretationVitalAbove => 'Über breiter Erwachsenenreferenz';

  @override
  String get interpretationVitalOxygenBelowTypical =>
      'Unter typischem Sauerstoffbereich';

  @override
  String get interpretationVitalOxygenLow =>
      'Referenz für niedrigen Sauerstoffwert';

  @override
  String get interpretationVitalOxygenVeryLow =>
      'Referenz für sehr niedrigen Sauerstoffwert';

  @override
  String get interpretationVitalRestingHrBody =>
      'Nur breite Erwachsenenreferenz; Fitness, Medikamente, Stress, Krankheit und Zeitpunkt können verändern, was für dich üblich ist.';

  @override
  String get interpretationVitalRespiratoryBody =>
      'Nur breite Erwachsenenreferenz; Aktivität, Angst, Krankheit und Messzeitpunkt können die Atemfrequenz beeinflussen.';

  @override
  String get interpretationVitalTemperatureBody =>
      'Die Temperatur variiert je nach Messstelle und Tageszeit; nutze dies nur als Kontext.';

  @override
  String get interpretationVitalOxygenBody =>
      'Pulsoximeter-Messwerte können durch Gerät, Haut, Durchblutung, Bewegung und Erkrankungen beeinflusst werden.';

  @override
  String get interpretationVitalSource =>
      'Quelle: MedlinePlus-Referenz für Vitalzeichen bei Erwachsenen.';

  @override
  String get interpretationOxygenSource =>
      'Quelle: MedlinePlus und FDA-Kontext zu Pulsoximetern.';

  @override
  String get sectionCrossMetricInsights => 'Metrikübergreifende Einblicke';

  @override
  String get crossMetricPositiveLink => 'Positiver Zusammenhang';

  @override
  String get crossMetricNegativeLink => 'Negativer Zusammenhang';

  @override
  String get crossMetricWeakLink => 'Schwacher Zusammenhang';

  @override
  String crossMetricCorrelation(String arg0) {
    return '$arg0';
  }

  @override
  String crossMetricPairedDays(int arg0) {
    return '$arg0 gepaarte Tage';
  }

  @override
  String get crossSleepHrvTitle => 'Schlaf vs. HRV';

  @override
  String get crossSleepHrvPositive =>
      'Mehr Schlaf geht in diesem Zeitraum tendenziell mit höherer HRV einher.';

  @override
  String get crossSleepHrvNegative =>
      'Mehr Schlaf geht in diesem Zeitraum tendenziell mit niedrigerer HRV einher.';

  @override
  String get crossSleepHrvNeutral =>
      'Schlaf und HRV zeigen in diesem Zeitraum kein klares Muster.';

  @override
  String get crossWorkoutRestingHrTitle => 'Trainings vs. Ruheherzfrequenz';

  @override
  String get crossWorkoutRestingHrPositive =>
      'Mehr Trainingsminuten gehen in diesem Zeitraum tendenziell mit höherer Ruheherzfrequenz einher.';

  @override
  String get crossWorkoutRestingHrNegative =>
      'Mehr Trainingsminuten gehen in diesem Zeitraum tendenziell mit niedrigerer Ruheherzfrequenz einher.';

  @override
  String get crossWorkoutRestingHrNeutral =>
      'Trainingsminuten und Ruheherzfrequenz zeigen in diesem Zeitraum kein klares Muster.';

  @override
  String get crossHydrationWeightTitle => 'Hydrierung vs. Gewichtsschwankung';

  @override
  String get crossHydrationWeightPositive =>
      'Mehr Hydrierung geht in diesem Zeitraum tendenziell mit größeren Gewichtsschwankungen einher.';

  @override
  String get crossHydrationWeightNegative =>
      'Mehr Hydrierung geht in diesem Zeitraum tendenziell mit kleineren Gewichtsschwankungen einher.';

  @override
  String get crossHydrationWeightNeutral =>
      'Hydrierung und Gewichtsschwankung zeigen in diesem Zeitraum kein klares Muster.';

  @override
  String get crossMindfulnessSleepTitle => 'Achtsamkeit vs. Schlaf';

  @override
  String get crossMindfulnessSleepPositive =>
      'Mehr Achtsamkeitsminuten gehen in diesem Zeitraum tendenziell mit längerem Schlaf einher.';

  @override
  String get crossMindfulnessSleepNegative =>
      'Mehr Achtsamkeitsminuten gehen in diesem Zeitraum tendenziell mit kürzerem Schlaf einher.';

  @override
  String get crossMindfulnessSleepNeutral =>
      'Achtsamkeit und Schlaf zeigen in diesem Zeitraum kein klares Muster.';

  @override
  String get legendLess => 'Weniger';

  @override
  String get legendMore => 'Mehr';

  @override
  String get dailyGoal => 'Tagesziel';

  @override
  String goalProgress(int arg0, int arg1) {
    return '$arg0 von $arg1 verfolgten Tagen erreicht';
  }

  @override
  String get cdDecreaseDailyGoal => 'Tagesziel verringern';

  @override
  String get cdIncreaseDailyGoal => 'Tagesziel erhöhen';

  @override
  String get hydrationDailyGoal => 'Tagesziel';

  @override
  String hydrationGoalProgress(int arg0, int arg1) {
    return '$arg0 von $arg1 verfolgten Tagen erreicht';
  }

  @override
  String get hydrationRemindersTitle => 'Hydrierungserinnerungen';

  @override
  String get hydrationRemindersSummaryOff =>
      'Standardmäßig aus. Aktiviere Erinnerungen während aktiver Stunden, bis das heutige Hydrierungsziel erreicht ist.';

  @override
  String hydrationRemindersSummaryOn(int arg0, String arg1, String arg2) {
    return 'Alle $arg0 Min. • $arg1-$arg2';
  }

  @override
  String get hydrationRemindersPermissionNeeded =>
      'Erteile die Benachrichtigungsberechtigung, um Hydrierungserinnerungen zu aktivieren.';

  @override
  String get hydrationRemindersInterval => 'Erinnerungsintervall';

  @override
  String hydrationRemindersIntervalValue(int arg0) {
    return 'Alle $arg0 Min.';
  }

  @override
  String get hydrationRemindersActiveStart => 'Aktiv ab';

  @override
  String get hydrationRemindersActiveEnd => 'Aktiv bis';

  @override
  String get hydrationRemindersGoalNote =>
      'Erinnerungen pausieren, sobald das heutige Ziel erreicht ist, und werden morgen fortgesetzt.';

  @override
  String get hydrationReminderNotificationChannel => 'Hydrierungserinnerungen';

  @override
  String get hydrationReminderNotificationChannelDesc =>
      'Optionale Erinnerungen, Hydrierung während aktiver Stunden zu protokollieren.';

  @override
  String get hydrationReminderNotificationTitle => 'Hydrierungserinnerung';

  @override
  String hydrationReminderNotificationBody(String arg0, String arg1) {
    return 'Du bist heute bei $arg0 von $arg1. Füge ein Getränk hinzu, wenn du kannst.';
  }

  @override
  String hydrationReminderNotificationProgress(String arg0, String arg1) {
    return '$arg0 / $arg1';
  }

  @override
  String get hydrationTrackerTitle => 'Hydrierung protokollieren';

  @override
  String get hydrationTrackerSubtitle => 'Direkt in Health Connect gespeichert';

  @override
  String get hydrationTrackerPermissionNeeded =>
      'Für die Übersicht fragt OpenVitals nur Leseberechtigungen an. Zum Hinzufügen dieses manuellen Eintrags benötigen wir eine Schreibberechtigung. OpenVitals speichert diese Daten nicht; Einträge werden in Health Connect gespeichert.';

  @override
  String get hydrationNutritionPermissionNeeded =>
      'Erteile die Schreibberechtigung für Ernährung, um Nährstoffe von Getränken in Health Connect zu speichern.';

  @override
  String get hydrationCustomDrinksTitle => 'Gespeicherte Getränke';

  @override
  String get hydrationCatalogDrinksTitle => 'Getränkekatalog';

  @override
  String get hydrationCatalogSearch => 'Getränke suchen';

  @override
  String get hydrationCatalogFrequentlyConsumed => 'Häufig getrunken';

  @override
  String get hydrationCatalogSavedOutside => 'Gespeicherte Getränke';

  @override
  String get hydrationCatalogSectionWater => 'Wasser';

  @override
  String get hydrationCatalogSectionCoffees => 'Kaffee';

  @override
  String get hydrationCatalogSectionEnergyDrinks => 'Energydrinks';

  @override
  String get hydrationCatalogSectionTeas => 'Tees';

  @override
  String get hydrationCatalogSectionChocolateDrinks => 'Schokoladengetränke';

  @override
  String get hydrationCatalogSectionCarbonatedSoftDrinks =>
      'Kohlensäurehaltige Softdrinks';

  @override
  String get hydrationCatalogSectionOtherDrinks => 'Andere Getränke';

  @override
  String hydrationCatalogSectionCount(int arg0) {
    return '$arg0 Getränke';
  }

  @override
  String get hydrationNewDrinkAction => 'Neues Getränk';

  @override
  String get hydrationNewDrinkTitle => 'Neues Getränk';

  @override
  String get hydrationEditDrinkTitle => 'Getränk bearbeiten';

  @override
  String hydrationLogSavedDrinkTitle(String arg0) {
    return '$arg0 protokollieren';
  }

  @override
  String get hydrationCustomDrinkName => 'Name';

  @override
  String get hydrationCustomDrinkCategory => 'Kategorie';

  @override
  String get hydrationCustomDrinkNoCategory => 'Keine Kategorie';

  @override
  String get hydrationCustomDrinkHydrationImpact => 'Auswirkung auf Hydrierung';

  @override
  String get hydrationImpactCountsFully => 'Zählt vollständig';

  @override
  String get hydrationImpactCountsPartially => 'Zählt teilweise';

  @override
  String get hydrationImpactDoesNotCount => 'Zählt nicht';

  @override
  String get hydrationImpactCountsFullyBody =>
      'Das gesamte Getränkevolumen zählt zur Hydrierung.';

  @override
  String get hydrationImpactCountsPartiallyBody =>
      'Einen Prozentsatz dieses Getränks verwenden.';

  @override
  String get hydrationImpactDoesNotCountBody =>
      'Speichern, ohne Hydrierung hinzuzufügen.';

  @override
  String get hydrationImpactPercentLabel => 'Zählt als Hydrierung (%)';

  @override
  String get hydrationImpactInvalidPercent =>
      'Gib einen Prozentsatz über 0 und unter 100 ein.';

  @override
  String get hydrationCustomDrinkNutrients => 'Nährstoffe';

  @override
  String get hydrationCustomDrinkAddNutrient => 'Nährstoff hinzufügen';

  @override
  String get hydrationCustomDrinkLiquidOnly => 'Nur Flüssigkeit';

  @override
  String hydrationCustomDrinkNutrientCount(int arg0) {
    return 'Nährstoffe: $arg0';
  }

  @override
  String hydrationSavedDrinkAmountNoHydration(String arg0) {
    return '$arg0 • Zählt nicht als Hydrierung';
  }

  @override
  String hydrationSavedDrinkAmountPartialHydration(String arg0, int arg1) {
    return '$arg0 • Zählt zu $arg1% als Hydrierung';
  }

  @override
  String get hydrationNonHydratingDrinkSavedHint =>
      'Als Ernährung gespeichert. Es wurde keine Hydrierung hinzugefügt.';

  @override
  String get hydrationEntryNutritionOnly => 'Nur Ernährung';

  @override
  String get hydrationEntryNoHydration => 'Keine Hydrierung';

  @override
  String get hydrationCustomDrinkAmountGrams => 'Menge (g)';

  @override
  String get hydrationCustomDrinkAmountKcal => 'Menge (kcal)';

  @override
  String get hydrationCustomDrinkInvalid =>
      'Gib einen Getränkenamen, eine Menge und positive Nährstoffmengen ein.';

  @override
  String get hydrationInvalidAmount =>
      'Gib eine Menge größer als null und höchstens 100 l ein.';

  @override
  String hydrationDrinkAmountLabel(String arg0) {
    return 'Menge ($arg0)';
  }

  @override
  String hydrationDrinkInvalidAmountRange(String arg0, String arg1) {
    return 'Gib eine Menge von $arg0 bis $arg1 ein.';
  }

  @override
  String hydrationWriteFailed(String arg0) {
    return 'Hydrierungseintrag konnte nicht gespeichert werden: $arg0';
  }

  @override
  String get cdDecreaseHydrationGoal => 'Hydrierungsziel verringern';

  @override
  String get cdIncreaseHydrationGoal => 'Hydrierungsziel erhöhen';

  @override
  String get cdDecreaseHydrationReminderInterval =>
      'Hydrierungserinnerungsintervall verringern';

  @override
  String get cdIncreaseHydrationReminderInterval =>
      'Hydrierungserinnerungsintervall erhöhen';

  @override
  String get unitPercentSymbol => '%';

  @override
  String get messageNoDashboardData => 'Keine Übersichtsdaten verfügbar.';

  @override
  String get messageMissingPermissionsTitle => 'Einige Berechtigungen fehlen';

  @override
  String get messageMissingPermissionsBody =>
      'Erteile die fehlenden Berechtigungen, um eine vollständige Übersicht zu sehen.';

  @override
  String messageHealthConnectRateLimited(int arg0) {
    return 'Health-Connect-Ratenlimit erreicht. Bitte warte etwa $arg0 min und versuche es erneut.';
  }

  @override
  String get messageNoWorkoutsDay =>
      'An diesem Tag wurden keine Trainings aufgezeichnet.';

  @override
  String get messageNoSleepDay => 'An diesem Tag endete keine Schlafsitzung.';

  @override
  String get messageNoBloodPressure => 'Kein Blutdruckmesswert.';

  @override
  String get messageNoOxygen => 'Kein Sauerstoffmesswert.';

  @override
  String get messageNoVo2Max => 'Kein VO2-max-Messwert.';

  @override
  String get messageNoBloodGlucose => 'Kein Blutzuckermesswert.';

  @override
  String get messageNoSkinTemperature => 'Kein Hauttemperaturmesswert.';

  @override
  String get messageCycleBrowse => 'Zykluskalender und Messwerte anzeigen.';

  @override
  String get dashboardAddWidgets => 'Widgets hinzufügen';

  @override
  String get dashboardAllWidgetsAdded =>
      'Alle Widgets sind bereits in der Übersicht.';

  @override
  String get dashboardActionLog => 'Eintragen';

  @override
  String get dashboardActionStartWorkout => 'Training starten';

  @override
  String get dashboardActivitiesToday => 'Aktivitäten';

  @override
  String get dashboardSensorStatusTitle => 'Sensorakku';

  @override
  String dashboardSensorBatteryLowest(int arg0) {
    return '$arg0% niedrigster Stand';
  }

  @override
  String get dashboardSensorBatteryUnknown => 'Akkustand wird ermittelt';

  @override
  String dashboardSensorStatusActiveConnected(int arg0, int arg1) {
    return '$arg0 aktiv • $arg1 verbunden';
  }

  @override
  String get dashboardSensorStatusAllDisabled => 'Alle Sensoren deaktiviert';

  @override
  String get dashboardDeleteActivityTitle => 'Aktivität löschen?';

  @override
  String dashboardDeleteActivityMessage(String arg0) {
    return 'Diese $arg0-Aktivität aus OpenVitals löschen?';
  }

  @override
  String get dashboardReadinessTitle => 'Tägliche Bereitschaft';

  @override
  String get dashboardReadinessScore => 'Bereitschaft';

  @override
  String get dashboardReadinessBodyEnergy => 'Körperenergie';

  @override
  String get dashboardReadinessTraining => 'Trainingsbereitschaft';

  @override
  String get dashboardReadinessHrvStatus => 'HRV-Status';

  @override
  String get dashboardReadinessIntensityMinutes => 'Intensitätsminuten';

  @override
  String get dashboardReadinessStressLevel => 'Stresslevel';

  @override
  String get dashboardReadinessRecommended => 'Empfohlen';

  @override
  String get dashboardReadinessAvoid => 'Vermeiden';

  @override
  String get dashboardReadinessAlternative => 'Alternative';

  @override
  String get dashboardReadinessStrain => 'Belastungsziel';

  @override
  String get dashboardReadinessGoal => 'Adaptives Ziel';

  @override
  String get dashboardReadinessRecoveryMode => 'Erholungsmodus';

  @override
  String get dashboardReadinessRecoveryModeBody =>
      'Aktivitätsziele werden reduziert, damit du dich auf Erholung konzentrieren kannst.';

  @override
  String get dashboardReadinessWhy => 'Warum diese Empfehlung';

  @override
  String get stressDetailsHowTracked => 'So wird es geschätzt';

  @override
  String get stressDetailsHowTrackedBody =>
      'OpenVitals schätzt physiologischen Stress lokal aus HRV gegenüber deiner Basislinie, Ruhepuls gegenüber deiner Basislinie und durchschnittlicher Herzfrequenz im Vergleich zum Ruhepuls. Es ist eine Belastungsschätzung, keine Diagnose und kein Detektor für mentalen Stress.';

  @override
  String get stressDetailsScale =>
      'Skala: 0-25 Ruhe, 26-50 niedrig, 51-75 mittel, 76-100 hoch.';

  @override
  String get stressDetailsInputs => 'Verwendete Eingaben';

  @override
  String get stressDetailsNoInputs =>
      'Keine nutzbaren HRV- oder Herzfrequenz-Basisliniensignale verfügbar.';

  @override
  String get stressDetailsDataCoverage => 'Datenabdeckung';

  @override
  String get stressDetailsNoDataCoverage =>
      'Keine HR- oder HRV-Stichprobenabdeckung für denselben Tag verfügbar.';

  @override
  String get stressDetailsCaveats => 'Hinweise';

  @override
  String get stressDetailsRelaxationPrompt =>
      'Wenn sich das passend anfühlt, probiere eine kurze Atem- oder Achtsamkeitssitzung und prüfe nach einer ruhigen Phase erneut.';

  @override
  String get readinessDetailsHowCalculated => 'So wird es berechnet';

  @override
  String get readinessDetailsSignalsUsed => 'Verwendete Signale';

  @override
  String get readinessDetailsGuidance => 'Was es bedeutet';

  @override
  String get readinessDetailsCaveats => 'Hinweise';

  @override
  String get readinessDetailsCaveatLocal =>
      'Dies ist eine lokale regelbasierte Schätzung aus den derzeit in OpenVitals verfügbaren Daten.';

  @override
  String get readinessDetailsCaveatNotMedical =>
      'Es ist keine Diagnose, kein medizinischer Rat, kein Coaching und keine Verletzungsvorhersage.';

  @override
  String get readinessDetailsCaveatMissingData =>
      'Fehlende Berechtigungen, wenige Stichproben oder fehlende Basislinien senken die Zuverlässigkeit.';

  @override
  String get readinessDetailsScoreStrong => 'Stark';

  @override
  String get readinessDetailsScoreSteady => 'Stabil';

  @override
  String get readinessDetailsScoreLimited => 'Begrenzt';

  @override
  String get readinessDetailsScoreLow => 'Niedrig';

  @override
  String get readinessDetailsScoreNeedsMoreData => 'Benötigt mehr Daten';

  @override
  String get bodyEnergyDetailsHowCalculatedBody =>
      'Körperenergie nutzt erholungsbezogene Signale: Schlafscore, HRV-Status, Ruhepuls, physiologischen Stress, Temperatur, Hydrierung, Ernährung und Achtsamkeit. Sie schätzt, wie viel Erholungskapazität heute sichtbar ist.';

  @override
  String get bodyEnergyDetailsScale =>
      'Skala: 80-100 stark, 60-79 stabil, 40-59 begrenzt, 0-39 niedrig.';

  @override
  String get bodyEnergyDetailsSummary =>
      'Ein erholungsbezogener Score dafür, wie viel Energie deine Körpersignale heute unterstützen.';

  @override
  String get bodyEnergyDetailsNoSignals =>
      'Keine nutzbaren erholungsbezogenen Signale verfügbar.';

  @override
  String get trainingReadinessDetailsHowCalculatedBody =>
      'Trainingsbereitschaft nutzt trainingsbezogene Signale: Schlaf, HRV-Status, Ruhepuls, Trainingsbelastung, Intensitätsminuten, physiologischen Stress, Temperatur und Aktivitätskontext. Sie schätzt, ob härteres Training heute passt.';

  @override
  String get trainingReadinessDetailsScale =>
      'Skala: 80-100 bereit für hartes Training, 60-79 kontrolliertes Training, 40-59 leichtes Training, 0-39 erholungsorientiert.';

  @override
  String get trainingReadinessDetailsSummary =>
      'Ein trainingsbezogener Score dafür, wie gut Erholung und Belastung heute Trainingsintensität unterstützen.';

  @override
  String get trainingReadinessDetailsNoSignals =>
      'Keine nutzbaren trainingsbezogenen Signale verfügbar.';

  @override
  String dashboardGoalOf(String arg0) {
    return 'von $arg0';
  }

  @override
  String get caloriesEstimatedActiveBmr =>
      'Kein Gesamteintrag, geschätzt aus aktiv + BMR';

  @override
  String caloriesEstimatedValue(String arg0) {
    return 'Gesch. $arg0';
  }

  @override
  String dashboardWeeklyCardioLoadProgress(int arg0, int arg1) {
    return '$arg0 von $arg1';
  }

  @override
  String dashboardCardioLoadPercentOnly(int arg0) {
    return '$arg0%';
  }

  @override
  String dashboardCardioLoadPercent(int arg0) {
    return '$arg0% Belastung';
  }

  @override
  String dashboardCardioLoadTodayDelta(int arg0) {
    return '+$arg0% heute';
  }

  @override
  String get messageNoActivitiesPeriod =>
      'Keine Aktivitäten im ausgewählten Zeitraum.';

  @override
  String get plannedWorkoutCompleted => 'Abgeschlossen';

  @override
  String plannedWorkoutBlocks(int arg0) {
    return '$arg0 Blöcke';
  }

  @override
  String get messageNoStepUpdates =>
      'Es wurden keine Schrittaktualisierungen aufgezeichnet';

  @override
  String get messageNoDistanceUpdates =>
      'Es wurden keine Distanzaktualisierungen aufgezeichnet';

  @override
  String get messageNoCaloriesBurned =>
      'Es wurden keine Daten zu Gesamtkalorien aufgezeichnet';

  @override
  String get messageNoFloorsClimbed =>
      'Es wurden keine Daten zu gestiegenen Etagen aufgezeichnet';

  @override
  String get messageNoActiveCalories =>
      'Es wurden keine Daten zu aktiven Kalorien aufgezeichnet';

  @override
  String get messageNoCalorieDataPeriod =>
      'Keine Daten zu Gesamtkalorien, aktiven Kalorien oder BMR in diesem Zeitraum.';

  @override
  String get messageNoElevation => 'Es wurden keine Höhendaten aufgezeichnet';

  @override
  String get messageNoWheelchairPushes =>
      'Es wurden keine Rollstuhlschübe aufgezeichnet';

  @override
  String get messageNoSleepDaySelected =>
      'Keine Schlafdaten für den ausgewählten Tag.';

  @override
  String get messageNoSleepPeriod =>
      'Keine Schlafdaten im ausgewählten Zeitraum.';

  @override
  String get messageNoHeartPeriod =>
      'Keine Herzfrequenzdaten im ausgewählten Zeitraum.\n\nStelle sicher, dass die Herzfrequenzberechtigung erteilt ist und ein verbundenes Gerät Daten synchronisiert hat.';

  @override
  String get messageNoHeartSamplesDay =>
      'An diesem Tag wurden keine Herzfrequenzproben aufgezeichnet.';

  @override
  String get messageHeartEmptyHint =>
      'Versuche ein anderes Datum oder prüfe, ob ein verbundenes Gerät punktuelle Herzdaten synchronisiert hat.';

  @override
  String get messageNoWeightPeriod =>
      'Keine Gewichtsdaten im ausgewählten Zeitraum.\n\nSynchronisiere eine Waage oder ein Wearable, das Gewicht an Health Connect meldet.';

  @override
  String get messageNoHydrationPeriod =>
      'Für diesen Zeitraum wurden keine Hydrierungseinträge aufgezeichnet.';

  @override
  String get messageNoHydrationAddedPeriod =>
      'Für diesen Zeitraum wurde keine Hydrierung hinzugefügt.';

  @override
  String get messageNoNutritionPeriod =>
      'Für diesen Zeitraum wurden keine Ernährungseinträge aufgezeichnet.';

  @override
  String get messageNoMindfulnessPeriod =>
      'Für diesen Zeitraum wurden keine Achtsamkeitssitzungen aufgezeichnet.';

  @override
  String get messageNoVitalsPeriod =>
      'Für diesen Zeitraum wurden keine Vitalwerte aufgezeichnet.';

  @override
  String get messageNoReadingsPeriod => 'Keine Messwerte in diesem Zeitraum.';

  @override
  String get messageNoCyclePeriod =>
      'Für diesen Zeitraum wurden keine Zyklusdaten aufgezeichnet.';

  @override
  String get messageNoSegments => 'Keine Segmente aufgezeichnet.';

  @override
  String get messageNoLaps => 'Keine Runden aufgezeichnet.';

  @override
  String get messageNoRoutePoints => 'Keine Routenpunkte aufgezeichnet.';

  @override
  String get messageRouteConsentRequired =>
      'Routendaten sind verfügbar, aber der Routenzugriff wurde noch nicht erteilt. Öffne die Health-Connect-Berechtigungen in den Einstellungen, um Routenvorschauen zu aktivieren.';

  @override
  String get messageNoRouteData => 'Keine Routendaten aufgezeichnet.';

  @override
  String get messageNoStages => 'Keine Phasen aufgezeichnet.';

  @override
  String get messageNoKcal => 'Keine kcal';

  @override
  String get onboardingTagline => 'Deine Gesundheitsdaten auf deinem Gerät';

  @override
  String get onboardingPrivacyTitle => 'Datenschutz zuerst';

  @override
  String get onboardingPrivacyBody =>
      'Kein Konto erforderlich. Daten bleiben auf deinem Gerät. Kein Cloud-Upload, keine Analysen, keine Werbung.';

  @override
  String get healthDisclaimerTitle => 'Gesundheitshinweis';

  @override
  String get healthDisclaimerBody =>
      'OpenVitals dient nur allgemeinen Wellness- und Informationszwecken. Es ist kein Medizinprodukt und bietet keine medizinische Beratung. Es diagnostiziert, behandelt, heilt oder verhindert keine Krankheit oder medizinische Erkrankung. Wende dich bei medizinischen Fragen, Diagnosen oder Behandlungen immer an qualifiziertes medizinisches Fachpersonal.';

  @override
  String get onboardingHealthConnectTitle => 'Unterstützt von Health Connect';

  @override
  String get onboardingHealthConnectBody =>
      'Liest aus dem sicheren Android-Gesundheitsspeicher auf dem Gerät und speichert Einträge, die du erstellst, wieder in Health Connect. Funktioniert mit allen Daten, die in Health Connect importiert wurden.';

  @override
  String get onboardingPermissionsHeader => 'HEALTH-CONNECT-BERECHTIGUNGEN';

  @override
  String get onboardingGrantCore =>
      'Erforderliche Health-Connect-Berechtigungen erteilen';

  @override
  String get onboardingGrantAll =>
      'Erforderliche Health-Connect-Berechtigungen erteilen';

  @override
  String get onboardingGrantRemaining =>
      'Verbleibende verfügbare Berechtigungen erteilen';

  @override
  String get onboardingOpenRequiredPermissions =>
      'Erforderliche Health-Connect-Berechtigungen öffnen';

  @override
  String get onboardingUnableOpenPermissions =>
      'Health-Connect-Berechtigungen können nicht geöffnet werden.';

  @override
  String get onboardingHealthConnectNotSupported =>
      'Health Connect wird auf diesem Gerät nicht unterstützt.';

  @override
  String get onboardingHealthConnectNeedsPlayStore =>
      'Auf diesem Gerät läuft Android 13 mit der eigenständigen Health-Connect-App. Leider hängt diese Version von Google-Play-Store-Diensten ab, die auf diesem Gerät fehlen. Daher lehnt Health Connect Anfragen ab, bevor OpenVitals deine Daten lesen kann. OpenVitals kann dieses geräteseitige Health-Connect-Problem nicht beheben oder umgehen. Die einzige Lösung ist, Google-Play-Dienste zu installieren oder auf Android 14 oder höher zu aktualisieren, wo Health Connect Teil des Betriebssystems ist und keine Google-Dienste benötigt.';

  @override
  String get onboardingHealthConnectUpdate =>
      'Health Connect muss installiert oder aktualisiert werden, um diese App zu verwenden.';

  @override
  String get onboardingInstallHealthConnect => 'Health Connect installieren';

  @override
  String get onboardingStatusNotSupported => 'Nicht unterstützt';

  @override
  String get onboardingStatusGranted => 'Erteilt';

  @override
  String onboardingStatusPartiallyGranted(int arg0, int arg1) {
    return '$arg0/$arg1 erteilt';
  }

  @override
  String get onboardingStatusManual => 'Einstellungen öffnen';

  @override
  String get onboardingStatusRequired => 'Erforderlich';

  @override
  String get onboardingStatusOptional => 'Optional';

  @override
  String get onboardingCategoryActivitySleep => 'Aktivität & Schlaf';

  @override
  String get onboardingCategoryActivitySleepDesc =>
      'Health Connect fragt nach:\n* Schritte\n* Distanz\n* Training\n* Schlaf';

  @override
  String get onboardingCategoryHeartRecovery => 'Herz & Vitalwerte';

  @override
  String get onboardingCategoryHeartRecoveryDesc =>
      'Health Connect fragt nach:\n* Herzfrequenz\n* Ruheherzfrequenz\n* Herzfrequenzvariabilität';

  @override
  String get onboardingCategoryBody => 'Körper';

  @override
  String get onboardingCategoryBodyDesc =>
      'Health Connect fragt nach:\n* Gewicht\n* Größe\n* Körperfett\n* Fettfreie Körpermasse\n* Basalmetabole Rate\n* Knochenmasse\n* Körperwassermasse';

  @override
  String get onboardingCategoryActivityExtras => 'Aktivitäts-Extras';

  @override
  String get onboardingCategoryActivityExtrasDesc =>
      'Health Connect fragt nach:\n* Verbrannte Gesamtkalorien\n* Verbrannte aktive Kalorien\n* Gestiegene Etagen\n* Gewonnene Höhe\n* Rollstuhlschübe\n* Geschwindigkeit\n* Leistung\n* Schrittfrequenz\n* Trittfrequenz beim Radfahren\n* Geplantes Training, falls unterstützt';

  @override
  String get onboardingCategoryNutritionHydration => 'Ernährung & Hydrierung';

  @override
  String get onboardingCategoryNutritionHydrationDesc =>
      'Health Connect fragt nach:\n* Hydrierung\n* Ernährung';

  @override
  String get onboardingCategoryManualEntryWrite =>
      'Schreibzugriff für manuelle Einträge';

  @override
  String get onboardingCategoryManualEntryWriteDesc =>
      'Health Connect fragt nach Schreibzugriff für:\n* Training\n* Distanz\n* Gewonnene Höhe\n* Verbrannte aktive Kalorien\n* Verbrannte Gesamtkalorien\n* Trainingsroute\n* Hydrierung\n* Gewicht\n* Größe\n* Körperfett\n* Blutdruck\n* Sauerstoffsättigung\n* Atemfrequenz\n* Körpertemperatur\n* Achtsamkeit, falls unterstützt';

  @override
  String get onboardingCategoryDataImportWrite =>
      'Schreibzugriff für Datenimporte';

  @override
  String get onboardingCategoryDataImportWriteDesc =>
      'Health Connect fragt nach Schreibzugriff für importierte Datensätze:\n* Aktivität, Training, Kalorien und Distanz\n* Herzfrequenz, Ruheherzfrequenz und Herzfrequenzvariabilität\n* Körpermessungen\n* Hydrierung und Ernährung\n* Schlaf\n* Vitalwerte\n* Achtsamkeit, falls unterstützt\n* Zyklusverfolgungsdaten';

  @override
  String get onboardingCategoryMindfulness => 'Achtsamkeit';

  @override
  String get onboardingCategoryMindfulnessDesc =>
      'Health Connect fragt nach:\n* Achtsamkeitssitzungen';

  @override
  String get onboardingCategoryMindfulnessUnavailable =>
      'Achtsamkeitssitzungen erfordern eine neuere Health-Connect-Version.';

  @override
  String get onboardingCategoryAdditionalDataAccess =>
      'Zusätzlicher Datenzugriff';

  @override
  String get onboardingCategoryAdditionalDataAccessDesc =>
      'Öffne in den Health-Connect-Berechtigungen OpenVitals > Zusätzlicher Zugriff und stelle ein:\n* Zugriff auf vergangene Daten: Aktivieren\n* Zugriff auf Daten im Hintergrund: Aktivieren\n* Zugriff auf Trainingsrouten: Immer';

  @override
  String onboardingCategoryAdditionalDataAccessManualNote(String arg0) {
    return '$arg0\n\nFalls Zugriff auf Trainingsrouten im Zugriffsdialog fehlt, öffne die Health-Connect-Einstellungen für OpenVitals und stelle ihn unter Zusätzlicher Zugriff ein.';
  }

  @override
  String get onboardingCategoryVitals => 'Vitalwerte';

  @override
  String get onboardingCategoryVitalsDesc =>
      'Health Connect fragt nach:\n* Blutdruck\n* Sauerstoffsättigung\n* Atemfrequenz\n* Körpertemperatur\n* VO2 max.\n* Blutzucker\n* Hauttemperatur, falls unterstützt';

  @override
  String get onboardingCategoryCycleTracking => 'Zyklusverfolgung';

  @override
  String get onboardingCategoryCycleTrackingDesc =>
      'Health Connect fragt nach sensiblen Zyklusdaten:\n* Menstruationsfluss\n* Menstruationsperioden\n* Ovulationstests\n* Zervixschleim\n* Basaltemperatur\n* Zwischenblutung\n* Sexuelle Aktivität';

  @override
  String get settingsAllRequestableGranted =>
      'Alle anforderbaren Berechtigungen erteilt';

  @override
  String get settingsManualPermissionsTitle =>
      'Manuelle Berechtigungen erforderlich';

  @override
  String get settingsManualPermissionsBody =>
      'Einige Health-Connect-Berechtigungen können nicht über den normalen Anfragedialog erteilt werden. Öffne Health Connect und aktiviere sie für OpenVitals.';

  @override
  String get settingsOpenHealthPermissions =>
      'Health-Connect-Berechtigungen öffnen';

  @override
  String get settingsDisplayGroupTitle => 'Anzeige';

  @override
  String get settingsDisplayGroupBody =>
      'Sprache, Einheiten und Erscheinungsbild';

  @override
  String get settingsActivitiesGroupTitle => 'Aktivitäten';

  @override
  String get settingsActivitiesGroupBody =>
      'Gleitende Daten, Lieblingsaktivität, Aufzeichnung und Offline-Karten';

  @override
  String get settingsSensorsGroupTitle => 'Sensoren & Geräte';

  @override
  String get settingsSensorsGroupBody =>
      'Herzfrequenz-, Trittfrequenz- und Leistungssensoren';

  @override
  String get settingsSensorsEmptyTitle => 'Noch keine Sensoren';

  @override
  String get settingsSensorsEmptyBody =>
      'Füge einen Bluetooth-Herzfrequenzgurt, Trittfrequenzsensor, Leistungsmesser oder Footpod hinzu.';

  @override
  String get settingsSensorsAddDevice => 'Sensor hinzufügen';

  @override
  String get settingsSensorsEditDevice => 'Sensor bearbeiten';

  @override
  String get settingsSensorsRemoveDevice => 'Sensor entfernen';

  @override
  String get settingsSensorsDeviceName => 'Gerätename';

  @override
  String get settingsSensorsEnabled => 'Aktiviert';

  @override
  String settingsSensorsBatteryPercent(int arg0) {
    return 'Akku $arg0%';
  }

  @override
  String get settingsSensorsBatteryUnknown => 'Akkustand wird ermittelt';

  @override
  String get settingsSensorsScanning => 'Suche nach Sensoren…';

  @override
  String get settingsSensorsScanStopped => 'Suche beendet';

  @override
  String get settingsSensorsScanEmpty =>
      'Noch keine Sensoren gefunden. Stelle sicher, dass der Sensor aktiv und in der Nähe ist.';

  @override
  String get settingsSensorsShowAllDevices => 'Alle Geräte anzeigen';

  @override
  String get settingsSensorsOpenBluetooth => 'Bluetooth-Einstellungen öffnen';

  @override
  String get settingsSensorsDiscovering => 'Sensorfunktionen werden erkannt…';

  @override
  String get settingsSensorsCapabilitiesTitle => 'Funktionen';

  @override
  String get settingsSensorsCapabilityHeartRate => 'Herzfrequenz';

  @override
  String get settingsSensorsCapabilityCyclingCadence => 'Trittfrequenz';

  @override
  String get settingsSensorsCapabilityCyclingPower => 'Leistung';

  @override
  String get settingsSensorsCapabilityCyclingSpeed => 'Radgeschwindigkeit';

  @override
  String get settingsSensorsCapabilityRunningSpeedCadence =>
      'Laufgeschwindigkeit/Trittfrequenz';

  @override
  String settingsSensorsCapabilityConflict(String arg0, String arg1) {
    return '$arg0 ist bereits $arg1 zugewiesen';
  }

  @override
  String get settingsSensorsWheelCircumference => 'Radumfang (mm)';

  @override
  String get activityRecordingSensorsTitle => 'Sensoren';

  @override
  String get activityRecordingSensorsAddInSettings =>
      'Sensoren in den Einstellungen hinzufügen';

  @override
  String get activityRecordingSensorsNotConfigured => 'Nicht konfiguriert';

  @override
  String get activityRecordingSensorsConnected => 'Verbunden';

  @override
  String get activityRecordingSensorsConnecting => 'Verbindet';

  @override
  String get activityRecordingSensorsReconnecting => 'Verbindet erneut';

  @override
  String get activityRecordingSensorsDisabled => 'Deaktiviert';

  @override
  String get activityRecordingSensorsWaitingForData => 'Warte auf Sensordaten…';

  @override
  String get activityRecordingSensorsWaitingShort => '—';

  @override
  String get activityRecordingSensorsNoSignalShort => 'Kein Signal';

  @override
  String get activityRecordingSensorsGarminBroadcastHint =>
      'Verbunden, aber die Uhr sendet keine Herzfrequenz. Bei Garmin: Einstellungen → Uhrsensoren → Handgelenks-Herzfrequenz → Herzfrequenz senden, dann auf der Uhr starten. Gadgetbridge zuerst trennen oder ein BLE-Brustgurt verwenden.';

  @override
  String get activityRecordingSensorsRecordedTitle =>
      'Aufgezeichnete Sensordaten';

  @override
  String get activityRecordingLiveHeartRate => 'Herzfrequenz';

  @override
  String get activityRecordingLiveCadence => 'Trittfrequenz';

  @override
  String get activityRecordingLivePower => 'Leistung';

  @override
  String get activityRecordingLiveSpeed => 'Geschwindigkeit';

  @override
  String activityRecordingNotificationHeartRate(String arg0) {
    return 'HF $arg0';
  }

  @override
  String get settingsNutritionGroupTitle => 'Ernährung';

  @override
  String get settingsNutritionGroupBody =>
      'Kaloriendaten und Koffein-Personalisierung';

  @override
  String get settingsCaloriesGroupTitle => 'Kalorien';

  @override
  String get settingsCaloriesGroupBody => 'Gesamtkaloriendaten';

  @override
  String get settingsCaffeineGroupTitle => 'Koffein';

  @override
  String get settingsCaffeineGroupBody =>
      'Halbwertszeit, Schlafenszeit, Schlafschwelle und Personalisierung.';

  @override
  String get settingsRecoveryGroupTitle => 'Erholung';

  @override
  String get settingsRecoveryGroupBody =>
      'Schlafzeitraum und Körperenergie-Kalibrierung';

  @override
  String get settingsSleepGroupTitle => 'Schlaf';

  @override
  String get settingsSleepGroupBody => 'Schlafzeitraum';

  @override
  String get settingsCycleGroupTitle => 'Menstruationszyklus';

  @override
  String get settingsCycleGroupBody =>
      'Zyklusdaten und Health-Connect-Berechtigungen';

  @override
  String get settingsDataImportGroupTitle => 'Datenimporteure';

  @override
  String get settingsDataImportGroupBody =>
      'Apple-Health-Exporte, Routendateien und FIT-Dateien importieren';

  @override
  String get settingsPermissionsGroupTitle => 'Berechtigungen';

  @override
  String get settingsPermissionsGroupBody =>
      'Zugriff auf Gesundheitsdaten und manuelle Berechtigungsschritte';

  @override
  String get settingsHealthConnectGroupTitle => 'Health Connect';

  @override
  String get settingsHealthConnectGroupBody =>
      'Synchronisation, Berechtigungen, Zugriff und App-Sperre';

  @override
  String get settingsDebugDiagnosticsGroupTitle => 'Debug-Diagnose';

  @override
  String get settingsDebugDiagnosticsGroupBody =>
      'Bereinigte Diagnoseprotokolle zur Fehlerbehebung speichern';

  @override
  String get settingsHealthConnectSyncTitle =>
      'Mit Health Connect synchronisieren';

  @override
  String get settingsHealthConnectSyncBody =>
      'Wenn aktiviert, liest und schreibt OpenVitals Gesundheitsdaten gemäß deinen Berechtigungen. Wenn deaktiviert, pausiert die Synchronisation, ohne den Zugriff zu widerrufen.';

  @override
  String get settingsHealthConnectManageAccess => 'Zugriff verwalten';

  @override
  String get settingsHealthConnectManageAccessBody =>
      'Health Connect öffnen, um zu prüfen oder zu ändern, welche Daten OpenVitals nutzen darf.';

  @override
  String get healthConnectAccessInsufficientTitle =>
      'Daten zum Teilen auswählen';

  @override
  String get healthConnectAccessInsufficientBody =>
      'OpenVitals benötigt Health-Connect-Zugriff, um diese Informationen anzuzeigen. Richte die Daten ein, die du teilen möchtest.';

  @override
  String get healthConnectAccessDoubleCancelTitle =>
      'Berechtigungen erfordern Aufmerksamkeit';

  @override
  String get healthConnectAccessDoubleCancelBody =>
      'Health-Connect-Berechtigungen wurden nicht erteilt. Öffne die Health-Connect-Einstellungen, um auszuwählen, welche Daten du mit OpenVitals teilen möchtest.';

  @override
  String get healthConnectSyncPaused =>
      'Health-Connect-Synchronisation pausiert';

  @override
  String get healthConnectSyncInProgress =>
      'Synchronisiere mit Health Connect…';

  @override
  String get healthConnectDataSourceManage => 'Datenquellen verwalten';

  @override
  String get healthConnectDataSourceManageBody =>
      'Sieh, welche Apps Daten in Health Connect schreiben, und verwalte deren Zugriff.';

  @override
  String get dashboardHealthConnectPromoTitle => 'Gesundheitsdaten einrichten';

  @override
  String get dashboardHealthConnectPromoBody =>
      'Erhalte eine einheitliche Ansicht deiner Aktivitäts-, Schlaf- und Herzfrequenzdaten aus den Apps und Geräten, die du bereits nutzt.';

  @override
  String get dashboardHealthConnectPromoAction => 'Loslegen';

  @override
  String get dashboardHealthConnectSyncPausedBody =>
      'Aktiviere die Synchronisation in den Einstellungen wieder, um dein Dashboard zu aktualisieren.';

  @override
  String get dashboardHealthConnectInstallAction =>
      'Health Connect installieren';

  @override
  String get healthConnectMatchmakingTitle => 'Apps verbinden';

  @override
  String get healthConnectMatchmakingBody =>
      'Finde Apps und Geräte, die Daten teilen können, die OpenVitals lesen kann.';

  @override
  String get healthConnectMatchmakingAction => 'Datenquellen finden';

  @override
  String get healthConnectPromoteActivityTitle =>
      'Aktivitätseinblicke freischalten';

  @override
  String get healthConnectPromoteActivityBody =>
      'Erlaube Aktivitätsdaten, um Schritte, Distanz, Workouts und Trends in OpenVitals zu sehen.';

  @override
  String get healthConnectPromoteActivitiesTitle => 'Deine Workouts ansehen';

  @override
  String get healthConnectPromoteActivitiesBody =>
      'Erlaube Zugriff auf Trainingseinheiten, um über Health Connect synchronisierte Aktivitäten zu durchsuchen.';

  @override
  String get healthConnectPromoteCaloriesTitle =>
      'Verbrannte Kalorien verfolgen';

  @override
  String get healthConnectPromoteCaloriesBody =>
      'Erlaube Kaloriendaten, um aktiven und gesamten Verbrauch im Zeitverlauf zu vergleichen.';

  @override
  String get healthConnectPromoteSleepTitle => 'Deinen Schlaf ansehen';

  @override
  String get healthConnectPromoteSleepBody =>
      'Erlaube Schlafdaten, um Phasen, Dauer und Schlafscore-Trends zu sehen.';

  @override
  String get healthConnectPromoteHeartTitle => 'Herzgesundheit überwachen';

  @override
  String get healthConnectPromoteHeartBody =>
      'Erlaube Herzfrequenz- und HRV-Daten, um Ruhepuls und Variabilität zu verfolgen.';

  @override
  String get healthConnectPromoteVitalsTitle => 'Vitalwerte freischalten';

  @override
  String get healthConnectPromoteVitalsBody =>
      'Erlaube Vitaldaten, um Blutdruck, SpO2 und verwandte Messwerte zu sehen.';

  @override
  String get healthConnectPromoteBodyTitle => 'Körperwerte verfolgen';

  @override
  String get healthConnectPromoteBodyBody =>
      'Erlaube Körperzusammensetzungsdaten, um Gewicht, BMI und verwandte Trends zu verfolgen.';

  @override
  String get healthConnectPromoteHydrationTitle => 'Hydration verfolgen';

  @override
  String get healthConnectPromoteHydrationBody =>
      'Erlaube Hydrationsdaten, um tägliche Aufnahme und Verlauf zu sehen.';

  @override
  String get healthConnectPromoteNutritionTitle => 'Ernährung ansehen';

  @override
  String get healthConnectPromoteNutritionBody =>
      'Erlaube Ernährungsdaten, um Kalorien und Makros aus deinen Quellen zu prüfen.';

  @override
  String get healthConnectPromoteMindfulnessTitle => 'Achtsamkeit verfolgen';

  @override
  String get healthConnectPromoteMindfulnessBody =>
      'Erlaube Achtsamkeitssitzungsdaten, um deine Praxis im Zeitverlauf zu sehen.';

  @override
  String get healthConnectPromoteCycleTitle => 'Zyklusdaten verfolgen';

  @override
  String get healthConnectPromoteCycleBody =>
      'Erlaube Menstruationszyklusdaten, um Blutung, Symptome und verwandte Einträge zu sehen.';

  @override
  String get healthConnectPromoteReadinessTitle =>
      'Readiness-Einblicke verbessern';

  @override
  String get healthConnectPromoteReadinessBody =>
      'Erlaube zusätzliche Health-Connect-Daten, um tägliche Readiness-Werte zu verfeinern.';

  @override
  String get healthConnectNewPermissionsTitle => 'Neue Daten verfügbar';

  @override
  String get healthConnectNewPermissionsBody =>
      'OpenVitals kann jetzt zusätzliche Gesundheitsdatentypen lesen. Erteile Zugriff, um die neuen Funktionen zu nutzen.';

  @override
  String get healthConnectNewPermissionsAction => 'Berechtigungen prüfen';

  @override
  String get privacyReconsentTitle => 'Datenschutzerklärung aktualisiert';

  @override
  String get privacyReconsentBody =>
      'Unsere Datenschutzerklärung hat sich geändert. Bitte prüfe und akzeptiere sie, um die Synchronisation mit Health Connect fortzusetzen.';

  @override
  String get privacyReconsentAction => 'Richtlinie ansehen';

  @override
  String get dashboardSummaryToday => 'Heute';

  @override
  String get settingsDebugLogsTitle => 'Bereinigte Diagnoseprotokolle';

  @override
  String get settingsDebugLogsBody =>
      'Teile oder speichere OpenVitals-Diagnoseprotokolle als Textdatei. Der Export entfernt oder maskiert Kennungen, Orte, Daten, URIs, rohe Sensordaten und fremde App-Protokolle vor dem Schreiben.';

  @override
  String get settingsDebugLogsShare => 'Protokolle teilen';

  @override
  String get settingsDebugLogsShareChooserTitle => 'Diagnoseprotokolle teilen';

  @override
  String get settingsDebugLogsShareFailed =>
      'Diagnoseprotokolle konnten nicht geteilt werden';

  @override
  String get settingsDebugLogsSave => 'Protokolle speichern';

  @override
  String get settingsDebugLogsSaved => 'Debug-Protokolle gespeichert';

  @override
  String get settingsDebugLogsSaveFailed =>
      'Diagnoseprotokolle konnten nicht gespeichert werden';

  @override
  String get settingsPrivacyPolicyLink => 'Datenschutzerklärung ansehen';

  @override
  String get settingsAppLockTitle => 'App-Sperre';

  @override
  String get settingsAppLockBody =>
      'Geräteentsperrung zum Öffnen von OpenVitals verlangen.';

  @override
  String get onboardingCoreRequired =>
      'Zugriff auf Aktivität, Schlaf und Herzfrequenz ist zum Start erforderlich. Weitere Datentypen kannst du später in den Einstellungen hinzufügen.';

  @override
  String get settingsLanguageTitle => 'Sprache';

  @override
  String get settingsLanguageBody =>
      'Wähle die App-Sprache oder folge deiner Systemeinstellung.';

  @override
  String get settingsLanguageSystem => 'System';

  @override
  String get settingsLanguageEnglish => 'Englisch';

  @override
  String get settingsLanguageSpanish => 'Spanisch';

  @override
  String get settingsLanguageGerman => 'Deutsch';

  @override
  String get settingsLanguageItalian => 'Italienisch';

  @override
  String get settingsLanguageEstonian => 'Estnisch';

  @override
  String get settingsUnitsTitle => 'Einheiten';

  @override
  String get settingsUnitsBody =>
      'Wähle, wie Distanzen, Gewichte, Hydrierung und Temperatur angezeigt werden.';

  @override
  String get settingsUnitMetric => 'Metrisch';

  @override
  String get settingsUnitImperial => 'Imperial';

  @override
  String get settingsThemeTitle => 'Erscheinungsbild';

  @override
  String get settingsThemeBody =>
      'Wähle das App-Design unabhängig vom Android-Dunkelmodus.';

  @override
  String get settingsThemeSystem => 'System';

  @override
  String get settingsThemeLight => 'Hell';

  @override
  String get settingsThemeDark => 'Dunkel';

  @override
  String get settingsThemeAmoled => 'AMOLED';

  @override
  String get settingsDynamicColorTitle => 'Dynamische Farben (Material You)';

  @override
  String get settingsDynamicColorBody =>
      'Färbe OpenVitals anhand deines Android-Hintergrundbilds ein. Deaktiviert verwendet die OpenVitals-Markenpalette in Blau und Türkis.';

  @override
  String get settingsActivityWeekTitle => 'Gleitende Daten';

  @override
  String get settingsActivityWeekBody =>
      'Verwende gleitende Fenster über 7, 30 und 365 Tage statt Kalenderwoche, Kalendermonat und Kalenderjahr.';

  @override
  String get settingsActivityWeekMondayToSunday => 'Kalender';

  @override
  String get settingsActivityWeekLast7Days => 'Gleitend';

  @override
  String get settingsFavoriteActivityTitle => 'Lieblingsaktivität';

  @override
  String get settingsFavoriteActivityBody =>
      'Verwende standardmäßig die zuletzt aufgezeichnete Aktivität oder wähle einen Aktivitätstyp, der immer vorausgewählt wird.';

  @override
  String get settingsFavoriteActivityLatest => 'Letzte verwenden';

  @override
  String get settingsActivityRecordingTitle => 'Aktivitätsaufzeichnung';

  @override
  String get settingsActivityRecordingBody =>
      'Passe die Live-GPS-Aufzeichnung an, ohne den Ablauf für gespeicherte Aktivitätsdetails zu ändern.';

  @override
  String get settingsActivityRecordingKeepScreenOnTitle =>
      'Bildschirm immer an';

  @override
  String get settingsActivityRecordingKeepScreenOnBody =>
      'Hält den Bildschirm wach, solange eine Aktivitätsaufzeichnung aktiv ist.';

  @override
  String get settingsActivityRecordingAutoIdleTitle => 'Automatischer Leerlauf';

  @override
  String get settingsActivityRecordingAutoIdleBody =>
      'Hält die Bewegungszeit an, wenn du länger als die gewählte Zeit stehst.';

  @override
  String get settingsActivityRecordingIdleTimeoutTitle => 'Leerlaufzeit';

  @override
  String get settingsActivityRecordingAccuracyTitle =>
      'Erforderliche GPS-Genauigkeit';

  @override
  String get settingsActivityRecordingRouteGapTitle =>
      'Neues Segment nach Lücke';

  @override
  String get settingsActivityRecordingTimeIntervalTitle =>
      'Aufzeichnungs-Zeitintervall';

  @override
  String get settingsActivityRecordingDistanceIntervalTitle =>
      'Aufzeichnungs-Distanzintervall';

  @override
  String get settingsActivityRecordingBarometerTitle => 'Anstieg per Barometer';

  @override
  String get settingsActivityRecordingBarometerBody =>
      'Nutzt Druckänderungen für den Anstieg, wenn das Gerät ein Barometer hat.';

  @override
  String get settingsActivityRecordingRestBellTitle => 'Pausen-Timer-Glocke';

  @override
  String get settingsActivityRecordingRestBellBody =>
      'Spielt einen leisen Glockenton, wenn der Pausen-Countdown zwischen Sätzen endet.';

  @override
  String get settingsActivityRecordingVoiceTitle => 'Sprachansagen';

  @override
  String get settingsActivityRecordingVoiceBody =>
      'Spricht Fortschritt, Leerlauf/Fortsetzung und Runden während der Aufzeichnung.';

  @override
  String get settingsActivityRecordingVoiceTimeTitle => 'Nach Zeit ansagen';

  @override
  String get settingsActivityRecordingVoiceDistanceTitle =>
      'Nach Distanz ansagen';

  @override
  String get settingsActivityRecordingVoiceIdleTitle => 'Leerlaufansagen';

  @override
  String get settingsActivityRecordingVoiceIdleBody =>
      'Ansage, wenn Auto-Leerlauf startet und die Aufzeichnung fortgesetzt wird.';

  @override
  String get settingsActivityRecordingVoiceLapTitle => 'Rundenansagen';

  @override
  String get settingsActivityRecordingVoiceLapBody =>
      'Spricht eine Zusammenfassung, wenn du eine Runde markierst.';

  @override
  String settingsActivityRecordingSeconds(int arg0) {
    return '$arg0 s';
  }

  @override
  String get settingsActivityRecordingHalfSecond => '0,5 s';

  @override
  String settingsActivityRecordingMeters(int arg0) {
    return '$arg0 m';
  }

  @override
  String get settingsActivityRecordingAuto => 'Auto';

  @override
  String get settingsActivityRecordingOff => 'Aus';

  @override
  String get settingsCalorieDataTitle => 'Gesamtkaloriendaten';

  @override
  String get settingsCalorieDataBody =>
      'Standardmäßig werden nur Health-Connect-Gesamtkalorien angezeigt. Aktiviere OpenVitals-Berechnungen, um fehlende Gesamtwerte aus aktiven Kalorien und BMR zu ergänzen.';

  @override
  String get settingsCaffeineTitle => 'Koffeinmodell';

  @override
  String get settingsCaffeineBody =>
      'Diese Werte personalisieren Koffeinspiegel, Schlafenszeitprognose und sichere Schlafhinweise. Einträge bleiben in Health Connect.';

  @override
  String get settingsBodyProfileTitle => 'Körperprofil';

  @override
  String get settingsBodyProfileBody =>
      'Alter, Gewicht und Herzfrequenz personalisieren die Schätzungen für Körperenergie und Koffein. Alle Felder sind optional.';

  @override
  String get settingsBodyProfileWeight => 'Gewicht';

  @override
  String get settingsSleepRangeTitle => 'Schlafzeitraum';

  @override
  String get settingsSleepRangeBody =>
      'Wähle, welchem Tag Schlafsitzungen zugeordnet werden.';

  @override
  String get settingsSleepRangeRolling24h => 'Gleitende 24 h';

  @override
  String get settingsSleepRangeNoon => 'Mittag';

  @override
  String get settingsSleepRangeEvening => '18:00';

  @override
  String get settingsCyclePermissionsTitle => 'Zyklusberechtigungen';

  @override
  String settingsCyclePermissionsGranted(int arg0, int arg1) {
    return '$arg0/$arg1 Zyklusberechtigungen erteilt.';
  }

  @override
  String get settingsAppleHealthImportTitle => 'Apple-Health-Importer';

  @override
  String get settingsAppleHealthImportBody =>
      'Importiere unterstützte Datensätze aus Apple Health export.xml oder export.zip in Health Connect.';

  @override
  String settingsAppleHealthImportPermissions(int arg0, int arg1) {
    return '$arg0/$arg1 Importberechtigungen erteilt.';
  }

  @override
  String get settingsAppleHealthImportGrant => 'Importberechtigungen erteilen';

  @override
  String get settingsAppleHealthImportAction =>
      'Apple-Health-Export importieren';

  @override
  String get settingsAppleHealthImportAnalyzeAction =>
      'Apple Health export analysieren';

  @override
  String get settingsAppleHealthImportChooseAnotherAction =>
      'Anderen Apple Health export wählen';

  @override
  String get settingsAppleHealthImportSelectedAction =>
      'Ausgewählte Kategorien importieren';

  @override
  String get settingsAppleHealthImportAnalyzing => 'Analyse läuft...';

  @override
  String get settingsAppleHealthImporting => 'Import läuft...';

  @override
  String get settingsAppleHealthImportProgressQueued => 'In Warteschlange';

  @override
  String get settingsAppleHealthImportProgressParsing => 'Export wird gelesen';

  @override
  String get settingsAppleHealthImportProgressConverting =>
      'Datensätze werden konvertiert';

  @override
  String get settingsAppleHealthImportProgressCheckingDuplicates =>
      'Duplikate werden geprüft';

  @override
  String get settingsAppleHealthImportProgressWriting =>
      'Datensätze werden geschrieben';

  @override
  String get settingsAppleHealthImportProgressFinishing =>
      'Import wird abgeschlossen';

  @override
  String get settingsAppleHealthImportProgressBuildingReport =>
      'Bericht wird erstellt';

  @override
  String get settingsAppleHealthImportProgressComplete => 'Abgeschlossen';

  @override
  String settingsAppleHealthImportProgress(String arg0, int arg1, int arg2) {
    return '$arg0. $arg1 Elemente gelesen, $arg2 Datensätze importiert.';
  }

  @override
  String settingsAppleHealthImportProgressWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Ausgewählt $arg2/$arg3 Datensätze, importiert $arg4.';
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
      'Der Import läuft im Hintergrund weiter, wenn du die App verlässt.';

  @override
  String get settingsAppleHealthImportNotificationChannel =>
      'Apple-Health-Importe';

  @override
  String get settingsAppleHealthImportNotificationTitle =>
      'Apple-Health-Export wird importiert';

  @override
  String settingsAppleHealthImportNotificationText(
    String arg0,
    int arg1,
    int arg2,
  ) {
    return '$arg0. $arg1 gelesen, $arg2 importiert.';
  }

  @override
  String settingsAppleHealthImportNotificationTextWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Ausgewählt $arg2/$arg3, importiert $arg4.';
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
    return 'Importiert $arg0. Duplikate $arg1. Nicht ausgewählt $arg2. Nicht unterstützt $arg3. Übersprungen $arg4. Fehlgeschlagen $arg5.';
  }

  @override
  String get settingsAppleHealthImportRoutesIncomplete =>
      'Die Gesundheitsdaten wurden importiert, jedoch waren einige Trainingsrouten nicht verfügbar, da die Postleitzahl unerwartet endete. Im Importbericht sind die betroffenen Aktivitäten zur manuellen Wiederherstellung aufgeführt.';

  @override
  String settingsAppleHealthImportAnalysisResult(
    int arg0,
    int arg1,
    int arg2,
    int arg3,
  ) {
    return '$arg0 Elemente gelesen. $arg1 kompatible Datensätze gefunden. Nicht unterstützt $arg2. Fehlgeschlagen $arg3.';
  }

  @override
  String get settingsAppleHealthImportChooseCategories =>
      'Wähle, was in Health Connect geschrieben wird.';

  @override
  String settingsAppleHealthImportCategoryCount(int arg0) {
    return '$arg0 Datensätze';
  }

  @override
  String settingsAppleHealthImportCategoryCountRoutes(int arg0, int arg1) {
    return '$arg0 Datensätze, $arg1 mit Routen';
  }

  @override
  String get settingsAppleHealthImportCategoryWorkouts => 'Workouts und Routen';

  @override
  String get settingsAppleHealthImportCategoryWorkoutsDesc =>
      'Trainingseinheiten und verknüpfte Routen-Geometrie.';

  @override
  String get settingsAppleHealthImportCategoryActivity => 'Aktivitätsmetriken';

  @override
  String get settingsAppleHealthImportCategoryActivityDesc =>
      'Schritte, Distanz, Kalorien, Etagen, Höhenmeter, Rollstuhlschübe und Geschwindigkeit.';

  @override
  String get settingsAppleHealthImportCategoryHeart => 'Herz';

  @override
  String get settingsAppleHealthImportCategoryHeartDesc =>
      'Herzfrequenz und Ruheherzfrequenz.';

  @override
  String get settingsAppleHealthImportCategorySleep => 'Schlaf';

  @override
  String get settingsAppleHealthImportCategorySleepDesc =>
      'Schlafsitzungen und Schlafphasen.';

  @override
  String get settingsAppleHealthImportCategoryBody => 'Körpermessungen';

  @override
  String get settingsAppleHealthImportCategoryBodyDesc =>
      'Gewicht, Größe, Körperfett, Magermasse, BMR, Knochenmasse und Körperwasser.';

  @override
  String get settingsAppleHealthImportCategoryVitals => 'Vitalwerte';

  @override
  String get settingsAppleHealthImportCategoryVitalsDesc =>
      'Blutdruck, Sauerstoffsättigung, Atemfrequenz, Körpertemperatur, Blutzucker und VO2 max.';

  @override
  String get settingsAppleHealthImportCategoryNutrition => 'Ernährung';

  @override
  String get settingsAppleHealthImportCategoryNutritionDesc =>
      'Nahrungsenergie, Makros, Koffein, Mineralstoffe und Vitamine.';

  @override
  String get settingsAppleHealthImportCategoryHydration => 'Hydration';

  @override
  String get settingsAppleHealthImportCategoryHydrationDesc =>
      'Wasseraufnahme.';

  @override
  String get settingsAppleHealthImportCategoryMindfulness => 'Mindfulness';

  @override
  String get settingsAppleHealthImportCategoryMindfulnessDesc =>
      'Mindfulness-Sitzungen, wenn Health Connect sie unterstützt.';

  @override
  String get settingsAppleHealthImportCategoryCycle => 'Zyklus';

  @override
  String get settingsAppleHealthImportCategoryCycleDesc =>
      'Menstruation, Ovulation, Zervixschleim, Blutungen, Basaltemperatur und sexuelle Aktivität.';

  @override
  String get settingsAppleHealthImportCopyReport => 'Bericht kopieren';

  @override
  String get settingsAppleHealthImportCopyError => 'Fehler kopieren';

  @override
  String get settingsAppleHealthImportSaveReport => 'Bericht speichern';

  @override
  String get settingsAppleHealthImportReportCopied => 'Importbericht kopiert.';

  @override
  String get settingsAppleHealthImportErrorCopied => 'Importfehler kopiert.';

  @override
  String get settingsAppleHealthImportReportSaved =>
      'Importbericht gespeichert.';

  @override
  String get settingsAppleHealthImportReportSaveFailed =>
      'Importbericht konnte nicht gespeichert werden.';

  @override
  String settingsAppleHealthImportError(String arg0) {
    return 'Import fehlgeschlagen: $arg0';
  }

  @override
  String get settingsAppleHealthImportPermissionDenied =>
      'Der Zugriff auf die ausgewählte Datei ist verloren gegangen, sodass der Import nicht fortgesetzt werden konnte. Wähle den gleichen Apple-Health-Export erneut aus, um genau dort weiterzumachen, wo du aufgehört hast.';

  @override
  String get settingsRouteImportTitle => 'GPX/KML/KMZ-Importer';

  @override
  String get settingsRouteImportBody =>
      'Importiere GPX-, KML- oder KMZ-Routendateien. Prüfe eine Datei vor dem Speichern oder importiere mehrere Dateien direkt in Health Connect.';

  @override
  String settingsRouteImportPermissions(int arg0, int arg1) {
    return '$arg0/$arg1 Routenimport-Berechtigungen erteilt.';
  }

  @override
  String get settingsRouteImportGrant => 'Routenimport-Berechtigungen erteilen';

  @override
  String get settingsRouteImportAction => 'GPX/KML/KMZ-Datei importieren';

  @override
  String get settingsRouteImportBulkAction =>
      'GPX/KML/KMZ-Dateien gesammelt importieren';

  @override
  String get settingsRouteImporting => 'Routen werden importiert...';

  @override
  String settingsRouteImportProgress(int arg0, int arg1, int arg2, int arg3) {
    return 'Datei $arg0/$arg1. Importiert $arg2, fehlgeschlagen $arg3.';
  }

  @override
  String settingsRouteImportResult(int arg0, int arg1, int arg2) {
    return 'Importiert $arg0. Fehlgeschlagen $arg1. Ausgewählt $arg2.';
  }

  @override
  String settingsRouteImportError(String arg0) {
    return 'Routenimport-Warnung: $arg0';
  }

  @override
  String get settingsFitImportTitle => 'FIT-Importer';

  @override
  String get settingsFitImportBody =>
      'Importiere FIT-Dateien für Aktivitäten, Kurse oder Workouts, prüfe die erkannten Details und wähle, ob sie in Health Connect gespeichert werden.';

  @override
  String get settingsFitImportAction => 'FIT-Datei importieren';

  @override
  String get settingsOfflineMapsTitle => 'Offline-Karten';

  @override
  String get settingsOfflineMapsBody =>
      'Importiere PMTiles- oder Mapsforge-.map/.maps-Pakete für vollständig offline nutzbare Aktivitätskarten. Protomaps-kompatible PMTiles-Basiskarten und Mapsforge-Karten werden unterstützt.';

  @override
  String get settingsOfflineMapsEmpty =>
      'Noch keine Offline-Karten importiert.';

  @override
  String get settingsOfflineMapsFormatPmtiles => 'PMTiles';

  @override
  String get settingsOfflineMapsFormatMapsforge => 'Mapsforge';

  @override
  String get settingsOfflineMapsRenderFormatTitle => 'Darstellungsformat';

  @override
  String settingsOfflineMapsRenderFormatOption(String arg0, int arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String get settingsOfflineMapsRenderFormatBody =>
      'OpenVitals rendert alle importierten Pakete im ausgewählten Format gemeinsam.';

  @override
  String settingsOfflineMapsPackDetail(String arg0, String arg1, String arg2) {
    return '$arg0 • $arg1 • $arg2';
  }

  @override
  String get settingsOfflineMapsImportAction => 'Offline-Karte importieren';

  @override
  String get settingsOfflineMapsImporting => 'Import läuft...';

  @override
  String get settingsOfflineMapsImportProgressQueued => 'In Warteschlange';

  @override
  String get settingsOfflineMapsImportProgressCopying => 'Karte wird kopiert';

  @override
  String get settingsOfflineMapsImportProgressComplete => 'Abgeschlossen';

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
      'Der Import läuft im Hintergrund weiter, wenn du die App verlässt.';

  @override
  String settingsOfflineMapsImportResult(String arg0, String arg1) {
    return '$arg0 ($arg1) importiert.';
  }

  @override
  String settingsOfflineMapsImportError(String arg0) {
    return 'Kartenimport fehlgeschlagen: $arg0';
  }

  @override
  String get settingsOfflineMapsImportNotificationChannel =>
      'Offline-Kartenimporte';

  @override
  String get settingsOfflineMapsImportNotificationTitle =>
      'Offline-Karte wird importiert';

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
      'Möchtest du lernen, wie du Offline-Karten hinzufügst? Gehe zu:';

  @override
  String get settingsOfflineMapsHelpLink =>
      'Anleitung für Offline-Karten öffnen';

  @override
  String get sectionSupport => 'Unterstützen';

  @override
  String get settingsSupportTitle => 'OpenVitals unterstützen';

  @override
  String get settingsSupportBody =>
      'Melde Fehler, diskutiere Support mit der Community oder hilf, die laufende Entwicklung zu finanzieren.';

  @override
  String get settingsSupportIssuesAction => 'Problem melden';

  @override
  String get settingsSupportDiscussionAction => 'Zulip-Diskussionen beitreten';

  @override
  String get settingsSupportAction => 'Liberapay öffnen';

  @override
  String get crashReportEmailChooserTitle =>
      'OpenVitals-Bericht per E-Mail senden';

  @override
  String get crashReportFallbackTitle => 'Keine E-Mail-App gefunden';

  @override
  String crashReportFallbackBody(String arg0) {
    return 'Kopiere den Bericht oder speichere ihn als Textdatei und sende ihn später an $arg0.';
  }

  @override
  String get crashReportFallbackCopy => 'Bericht kopieren';

  @override
  String get crashReportFallbackSave => 'Textdatei speichern';

  @override
  String get crashReportFallbackCopied => 'Bericht kopiert.';

  @override
  String get crashReportFallbackSaved => 'Bericht gespeichert.';

  @override
  String get crashReportFallbackSaveFailed =>
      'Bericht konnte nicht gespeichert werden.';

  @override
  String get crashReportFallbackSaveUnavailable =>
      'Keine App zum Speichern gefunden. Bericht kopiert.';

  @override
  String get crashReportClipboardLabel => 'OpenVitals-Bericht';

  @override
  String get settingsPrivacyNoAccount => 'Kein Konto erforderlich';

  @override
  String get settingsPrivacyNoCloud =>
      'Keine Cloud-Synchronisierung von Gesundheitsdaten';

  @override
  String get settingsPrivacyNoAnalytics => 'Kein Analyse-SDK';

  @override
  String get settingsPrivacyNoAds =>
      'Keine Werbung oder Drittanbieter-Tracking';

  @override
  String get settingsPrivacyOnDevice => 'Daten bleiben auf deinem Gerät';

  @override
  String get settingsPrivacyReadOnly =>
      'Schreibgeschützt, außer bei Einträgen, die du ausdrücklich protokollierst';

  @override
  String settingsAppVersion(String arg0, int arg1) {
    return 'Version $arg0 ($arg1)';
  }

  @override
  String get detailMetrics => 'Metriken';

  @override
  String get detailSessionDetails => 'Sitzungsdetails';

  @override
  String get detailDuration => 'Dauer';

  @override
  String get detailMovingTime => 'Zeit in Bewegung';

  @override
  String get detailType => 'Typ';

  @override
  String get detailStarted => 'Gestartet';

  @override
  String get detailEnded => 'Beendet';

  @override
  String get detailStartZone => 'Startzone';

  @override
  String get detailEndZone => 'Endzone';

  @override
  String get detailRecording => 'Aufzeichnung';

  @override
  String get detailSourcePackage => 'Quellpaket';

  @override
  String get detailDeviceType => 'Gerätetyp';

  @override
  String get detailDeviceMaker => 'Gerätehersteller';

  @override
  String get detailDeviceModel => 'Gerätemodell';

  @override
  String get detailLastModified => 'Zuletzt geändert';

  @override
  String get detailRecordId => 'Datensatz-ID';

  @override
  String get detailClientRecordId => 'Client-Datensatz-ID';

  @override
  String get detailClientVersion => 'Client-Version';

  @override
  String get detailPlannedSessionId => 'ID der geplanten Sitzung';

  @override
  String get detailNotes => 'Notizen';

  @override
  String get detailTitle => 'Titel';

  @override
  String get detailTime => 'Zeit';

  @override
  String get detailRepetitions => 'Wiederholungen';

  @override
  String get detailSet => 'Satz';

  @override
  String get detailLength => 'Länge';

  @override
  String get detailSegments => 'Segmente';

  @override
  String get detailLaps => 'Runden';

  @override
  String detailLap(int arg0) {
    return 'Runde $arg0';
  }

  @override
  String get detailRoute => 'Route';

  @override
  String get detailStatus => 'Status';

  @override
  String get detailStatusAvailable => 'Verfügbar';

  @override
  String get detailPoints => 'Punkte';

  @override
  String get detailStartPoint => 'Startpunkt';

  @override
  String get detailEndPoint => 'Endpunkt';

  @override
  String detailAltitude(String arg0) {
    return 'Höhe $arg0';
  }

  @override
  String detailHorizontalAccuracy(String arg0) {
    return 'Horizontale Genauigkeit $arg0';
  }

  @override
  String detailVerticalAccuracy(String arg0) {
    return 'Vertikale Genauigkeit $arg0';
  }

  @override
  String get detailStageEvents => 'Phasenereignisse';

  @override
  String get detailStages => 'Phasen';

  @override
  String get detailSleepSession => 'Schlafsitzung';

  @override
  String get recordingActivelyRecorded => 'Aktiv aufgezeichnet';

  @override
  String get recordingAutomaticallyRecorded => 'Automatisch aufgezeichnet';

  @override
  String get recordingManualEntry => 'Manueller Eintrag';

  @override
  String get recordingUnknown => 'Unbekannt';

  @override
  String get deviceWatch => 'Uhr';

  @override
  String get devicePhone => 'Telefon';

  @override
  String get deviceScale => 'Waage';

  @override
  String get deviceRing => 'Ring';

  @override
  String get deviceHeadMounted => 'Head-Mounted';

  @override
  String get deviceFitnessBand => 'Fitnessarmband';

  @override
  String get deviceChestStrap => 'Brustgurt';

  @override
  String get deviceSmartDisplay => 'Smart Display';

  @override
  String get sleepStageAwake => 'Wach';

  @override
  String get sleepStageSleeping => 'Schlafend';

  @override
  String get sleepStageOutOfBed => 'Außerhalb des Betts';

  @override
  String get sleepStageLight => 'Leicht';

  @override
  String get sleepStageDeep => 'Tief';

  @override
  String get sleepStageRem => 'REM';

  @override
  String get sleepStageAwakeInBed => 'Wach im Bett';

  @override
  String get sleepStageUnknown => 'Unbekannt';

  @override
  String get sleepStagesShareTitle => 'Anteil der Zeit im Bett';

  @override
  String get cyclePermissionsMissingTitle => 'Zyklusberechtigungen fehlen';

  @override
  String get cyclePermissionsMissingBody =>
      'Erteile Berechtigungen zur Zyklusverfolgung, um Menstruationstage, Ovulationstests, Zervixschleim und Basaltemperatur anzuzeigen.';

  @override
  String get cycleObservationMenstruationPeriod => 'Menstruationsperiode';

  @override
  String get cycleObservationMenstruationFlow => 'Menstruationsfluss';

  @override
  String get cycleObservationOvulationTest => 'Ovulationstest';

  @override
  String get cycleObservationCervicalMucus => 'Zervixschleim';

  @override
  String get cycleObservationBasalBodyTemperature => 'Basaltemperatur';

  @override
  String get cycleObservationIntermenstrualBleeding => 'Zwischenblutung';

  @override
  String get cycleObservationSexualActivity => 'Sexuelle Aktivität';

  @override
  String get cycleProtectionProtected => 'Geschützt';

  @override
  String get cycleProtectionUnprotected => 'Ungeschützt';

  @override
  String get cycleProtectionUnknown => 'Schutz unbekannt';

  @override
  String cycleBasalTemperatureValue(String arg1) {
    return '%1\$.1f C · $arg1';
  }

  @override
  String cycleDaysValue(int arg0, String arg1) {
    return '$arg0 $arg1';
  }

  @override
  String get cycleDaySingular => 'Tag';

  @override
  String get cycleDayPlural => 'Tage';

  @override
  String get cycleFlowLight => 'Leicht';

  @override
  String get cycleFlowMedium => 'Mittel';

  @override
  String get cycleFlowHeavy => 'Stark';

  @override
  String get cycleOvulationPositive => 'Positiv';

  @override
  String get cycleOvulationHigh => 'Hoch';

  @override
  String get cycleOvulationNegative => 'Negativ';

  @override
  String get cycleOvulationInconclusive => 'Nicht eindeutig';

  @override
  String get cycleMucusDry => 'Trocken';

  @override
  String get cycleMucusSticky => 'Klebrig';

  @override
  String get cycleMucusCreamy => 'Cremig';

  @override
  String get cycleMucusWatery => 'Wässrig';

  @override
  String get cycleMucusEggWhite => 'Eiklarartig';

  @override
  String get cycleMucusUnusual => 'Ungewöhnlich';

  @override
  String get cycleMucusLight => 'leicht';

  @override
  String get cycleMucusMedium => 'mittel';

  @override
  String get cycleMucusHeavy => 'stark';

  @override
  String cycleMucusValue(String arg0, String arg1) {
    return '$arg0, $arg1';
  }

  @override
  String get measurementLocationArmpit => 'Achselhöhle';

  @override
  String get measurementLocationFinger => 'Finger';

  @override
  String get measurementLocationForehead => 'Stirn';

  @override
  String get measurementLocationMouth => 'Mund';

  @override
  String get measurementLocationRectum => 'Rektum';

  @override
  String get measurementLocationTemporalArtery => 'Schläfenarterie';

  @override
  String get measurementLocationToe => 'Zeh';

  @override
  String get measurementLocationEar => 'Ohr';

  @override
  String get measurementLocationWrist => 'Handgelenk';

  @override
  String get measurementLocationVagina => 'Vagina';

  @override
  String get measurementLocationUnknown => 'Messstelle unbekannt';

  @override
  String get weekdayMondayShort => 'M';

  @override
  String get weekdayTuesdayShort => 'D';

  @override
  String get weekdayWednesdayShort => 'M';

  @override
  String get weekdayThursdayShort => 'D';

  @override
  String get weekdayFridayShort => 'F';

  @override
  String get weekdaySaturdayShort => 'S';

  @override
  String get weekdaySundayShort => 'S';

  @override
  String get vitalsPermissionsNeededTitle =>
      'Vitalwertberechtigungen erforderlich';

  @override
  String get vitalsPermissionsNeededBody =>
      'Erteile Berechtigungen für Blutdruck, Sauerstoffsättigung, Atemfrequenz, Temperatur, VO2 max und Blutzucker, um diese Ansicht zu füllen.';

  @override
  String get vitalsRespiratoryRateReadings => 'Atemfrequenz-Messwerte';

  @override
  String get vitalsBodyTemperatureReadings => 'Körpertemperatur-Messwerte';

  @override
  String get heartRateHealthChecksTitle => 'Herzfrequenzchecks';

  @override
  String get heartRateHighTitle => 'Hohe Herzfrequenz';

  @override
  String get heartRateLowTitle => 'Niedrige Herzfrequenz';

  @override
  String heartRateSamplesAtOrAbove(int arg0) {
    return 'Messpunkte bei/über $arg0 bpm';
  }

  @override
  String heartRateSamplesAtOrBelow(int arg0) {
    return 'Messpunkte bei/unter $arg0 bpm';
  }

  @override
  String heartRateDaysAtOrAbove(int arg0) {
    return 'Tage bei/über $arg0 bpm';
  }

  @override
  String heartRateDaysAtOrBelow(int arg0) {
    return 'Tage bei/unter $arg0 bpm';
  }

  @override
  String get cdDecreaseHrThreshold => 'Herzfrequenzschwelle senken';

  @override
  String get cdIncreaseHrThreshold => 'Herzfrequenzschwelle erhöhen';

  @override
  String get mealBreakfast => 'Frühstück';

  @override
  String get mealLunch => 'Mittagessen';

  @override
  String get mealDinner => 'Abendessen';

  @override
  String get mealSnack => 'Snack';

  @override
  String get mealGeneric => 'Mahlzeit';

  @override
  String macroProteinShort(String arg0) {
    return 'P ${arg0}g';
  }

  @override
  String macroCarbsShort(String arg0) {
    return 'KH ${arg0}g';
  }

  @override
  String macroFatShort(String arg0) {
    return 'F ${arg0}g';
  }

  @override
  String macroFiber(String arg0) {
    return 'Ballaststoffe ${arg0}g';
  }

  @override
  String macroSugar(String arg0) {
    return 'Zucker ${arg0}g';
  }

  @override
  String get caffeineSectionOverview => 'Überblick';

  @override
  String get caffeineSectionDashboard => 'Dashboard';

  @override
  String get caffeineSectionAnalytics => 'Analysen';

  @override
  String get caffeineSectionSleep => 'Schlafauswirkung';

  @override
  String get caffeineSectionSources => 'Quellen';

  @override
  String get caffeineSectionEntries => 'Einträge';

  @override
  String get caffeineSectionScience => 'Wissenschaft';

  @override
  String get caffeineSetupTitle => 'Koffein-Einblicke personalisieren';

  @override
  String get caffeineSetupBody =>
      'OpenVitals hat Koffeindaten gefunden. Personalisierung verbessert die Koffeinkurve und die Schlafenszeitprognose.';

  @override
  String get caffeineCurrentTitle => 'Aktives Koffein';

  @override
  String get caffeineTodayTotal => 'Heute gesamt';

  @override
  String get caffeineTimeToSafe => 'Zeit bis sicher';

  @override
  String get caffeineSleepStatusUnlikely => 'Schlafauswirkung unwahrscheinlich';

  @override
  String caffeineSleepStatusUnlikelyBody(String arg0, String arg1) {
    return '$arg0 jetzt aktiv, unter deiner Schlafschwelle von $arg1.';
  }

  @override
  String get caffeineSleepStatusElevatedNow => 'Jetzt erhöht';

  @override
  String caffeineSleepStatusElevatedNowBody(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return '$arg0 jetzt aktiv. Voraussichtlich in $arg1 unter der Schwelle; Schlafenszeitprognose: $arg2 um $arg3.';
  }

  @override
  String get caffeineSleepStatusMayAffect => 'Kann den Schlaf beeinflussen';

  @override
  String caffeineSleepStatusMayAffectBody(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Schlafenszeitprognose: $arg0 um $arg1, über deiner Schwelle von $arg2.';
  }

  @override
  String get caffeinePeriodTotal => 'Zeitraum gesamt';

  @override
  String get caffeineDailyAverage => 'Tagesdurchschnitt';

  @override
  String get caffeineLoggedDays => 'Protokollierte Tage';

  @override
  String get caffeinePeakDay => 'Spitzentag';

  @override
  String caffeinePeakDayValue(String arg0, String arg1) {
    return '$arg0 - $arg1';
  }

  @override
  String get caffeineCurveTitle => 'Koffeinkurve';

  @override
  String caffeineThresholdLine(String arg0) {
    return 'Schlafschwelle $arg0';
  }

  @override
  String get caffeineBedtimeForecast => 'Schlafenszeitprognose';

  @override
  String caffeineBedtimeSummary(String arg0, String arg1) {
    return 'Um $arg0 mit Schwelle $arg1';
  }

  @override
  String get caffeineSafeNights => 'Sichere Nächte';

  @override
  String get caffeineSafeStreak => 'Sichere Serie';

  @override
  String get caffeineTopSource => 'Wichtigste Quelle';

  @override
  String get caffeineSleepThreshold => 'Schlafschwelle';

  @override
  String get caffeineDailyImpact => 'Tages- und Schlafenszeitwirkung';

  @override
  String get caffeineSafeCalendar => 'Kalender sicherer Nächte';

  @override
  String get caffeineSources => 'Quell-Apps';

  @override
  String get caffeineItems => 'Elemente';

  @override
  String get caffeineInferredCategories => 'Abgeleitete Kategorien';

  @override
  String get caffeineTimeOfDay => 'Tageszeit';

  @override
  String get caffeineEntry => 'Koffeineintrag';

  @override
  String caffeineInferredCategory(String arg0) {
    return 'Kategorie: $arg0';
  }

  @override
  String caffeineCatalogMatch(String arg0) {
    return 'Katalog: $arg0';
  }

  @override
  String get caffeineCategory => 'Kategorie';

  @override
  String get caffeineCatalog => 'Katalog';

  @override
  String caffeineCatalogMatchDetail(String arg0, String arg1, String arg2) {
    return '$arg0, typisch $arg1, $arg2 Übereinstimmung';
  }

  @override
  String get caffeineHealthConnectSourceLabel => 'Quelle';

  @override
  String get caffeineHealthConnectMealLabel => 'Mahlzeit';

  @override
  String get caffeineHealthConnectDurationLabel => 'Dauer';

  @override
  String caffeineCurrentContribution(String arg0) {
    return '$arg0 aktiv';
  }

  @override
  String get caffeineCurrentContributionLabel => 'Aktuell';

  @override
  String get caffeineDose => 'Dosis';

  @override
  String get caffeinePeak => 'Spitze';

  @override
  String get caffeinePeakTime => 'Spitzenzeit';

  @override
  String get caffeineContributionCurve => 'Beitragskurve';

  @override
  String get caffeineEmpty =>
      'Keine Koffeineinträge für diesen Zeitraum. Koffeinhaltige Getränke, die über Hydrierung oder Ernährung hinzugefügt werden, erscheinen hier, wenn Health Connect Koffein enthält.';

  @override
  String get caffeineScienceTitle => 'So funktioniert die Schätzung';

  @override
  String get caffeineScienceBody =>
      'OpenVitals liest Koffein aus Health-Connect-Ernährungsdatensätzen in Milligramm und schätzt dann die Aufnahme über dein konfiguriertes Aufnahmefenster sowie den exponentiellen Abbau anhand deiner personalisierten Halbwertszeit.';

  @override
  String get caffeineScienceMeasurements => 'Verwendete Messwerte';

  @override
  String get caffeineScienceMeasurementsBody =>
      'Die aufgezeichnete Dosis stammt immer aus Health Connect. Start-/Endzeit, Eintragsname, Mahlzeitentyp und Datenquellenpaket werden für Timing, Zuordnung und Analysebeschriftungen verwendet. Katalogtreffer ergänzen Einträge nur; sie ersetzen nie die aufgezeichnete Dosis.';

  @override
  String get caffeineScienceLimits =>
      'Dies ist ein praktisches Populationsmodell, kein medizinischer Rat. Schwangerschaft, Medikamente, Lebererkrankungen, Genetik, Rauchen, Alkohol, Empfindlichkeit und Gewöhnung können die Koffeinreaktion verändern.';

  @override
  String get caffeineReferencesTitle => 'Forschung und Quellen';

  @override
  String get caffeineReferenceDrake => 'Koffein-Timing und Schlaf, Drake 2013';

  @override
  String get caffeineReferenceNehlig =>
      'Individueller Koffeinstoffwechsel, Nehlig 2018';

  @override
  String get caffeineReferenceEfsa =>
      'EFSA-Hinweise zu Koffeinsicherheit und Schlaf';

  @override
  String get caffeineReferenceHealthConnect =>
      'Felder des Health-Connect-Ernährungsdatensatzes';

  @override
  String get unknownSource => 'Unbekannte Quelle';

  @override
  String get achievementsLegacyTitle => 'Legacy-Aktivitätsabzeichen';

  @override
  String achievementsProgressSummary(int arg0, int arg1) {
    return '$arg0 von $arg1 freigeschaltet';
  }

  @override
  String achievementsDataWindow(String arg0, String arg1, String arg2) {
    return '$arg0 bis $arg1 · $arg2 verfolgte Tage';
  }

  @override
  String get achievementsTrackedDays => 'Verfolgte Tage';

  @override
  String get achievementsBestSteps => 'Beste Schritte';

  @override
  String get achievementsTotalDistance => 'Gesamtdistanz';

  @override
  String get achievementsBestFloors => 'Beste Etagen';

  @override
  String get achievementsTotalFloors => 'Etagen gesamt';

  @override
  String get achievementsFilterAll => 'Alle';

  @override
  String get achievementsCategoryDailySteps => 'Tagesschritte';

  @override
  String get achievementsCategoryLifetimeDistance => 'Lebenszeit-Distanz';

  @override
  String get achievementsCategoryDailyFloors => 'Tagesetagen';

  @override
  String get achievementsCategoryLifetimeFloors => 'Lebenszeit-Etagen';

  @override
  String achievementsDailyStepsRequirement(String arg0) {
    return '$arg0 Schritte an einem Tag';
  }

  @override
  String achievementsLifetimeDistanceRequirement(String arg0) {
    return '$arg0 Gesamtdistanz';
  }

  @override
  String achievementsDailyFloorsRequirement(String arg0) {
    return '$arg0 Etagen an einem Tag';
  }

  @override
  String achievementsLifetimeFloorsRequirement(String arg0) {
    return '$arg0 Etagen gesamt';
  }

  @override
  String achievementsProgressValue(String arg0, String arg1) {
    return '$arg0 von $arg1';
  }

  @override
  String achievementsAchievedOn(String arg0) {
    return 'Freigeschaltet $arg0';
  }

  @override
  String get achievementsEarnedOnce => 'Erreicht';

  @override
  String achievementsEarnedTimes(int arg0) {
    return '$arg0-mal';
  }

  @override
  String get achievementsLocked => 'Gesperrt';

  @override
  String get achievementsNoDataTitle => 'Keine Aktivitätshistorie';

  @override
  String get achievementsNoDataBody =>
      'Health Connect hat keine Schritt- oder Distanzdaten zurückgegeben. Prüfe, ob Aktivitätsdaten vorhanden sind und ob für ältere Einträge Verlaufszugriff gewährt ist.';

  @override
  String get achievementsNoFloorDataTitle => 'Keine Etagen-Daten';

  @override
  String get achievementsNoFloorDataBody =>
      'Etagenabzeichen werden freigeschaltet, wenn Health Connect Daten zu gestiegenen Etagen hat.';

  @override
  String get achievementsErrorTitle => 'Auszeichnungen nicht verfügbar';

  @override
  String get dataConfidenceTitle => 'Datenvertrauen';

  @override
  String get dataConfidenceHigh => 'Hohes Vertrauen';

  @override
  String get dataConfidenceMedium => 'Mittleres Vertrauen';

  @override
  String get dataConfidenceLow => 'Niedriges Vertrauen';

  @override
  String dataConfidenceCoverage(int arg0, int arg1, int arg2) {
    return '$arg0 von $arg1 Tagen verfolgt ($arg2%)';
  }

  @override
  String dataConfidenceSamples(int arg0) {
    return '$arg0 Datensätze';
  }

  @override
  String get dataConfidenceSourceUnavailable =>
      'Quelldetails für dieses Aggregat nicht verfügbar';

  @override
  String dataConfidenceSourceSingle(String arg0) {
    return 'Quelle: $arg0';
  }

  @override
  String dataConfidenceSourceMixed(String arg0) {
    return 'Gemischte Quellen: $arg0';
  }

  @override
  String get dataConfidenceKindMeasured =>
      'Gemessene Health-Connect-Datensätze';

  @override
  String get dataConfidenceKindAggregated =>
      'Aus Health-Connect-Datensätzen aggregiert';

  @override
  String get dataConfidenceKindCalculated => 'Von OpenVitals berechnet';

  @override
  String get dataConfidenceKindEstimated =>
      'Geschätzter oder abgeleiteter Wert';

  @override
  String get dataConfidenceKindMixed =>
      'Gemischte gemessene und berechnete Daten';

  @override
  String get dataConfidenceWarningLowCoverage =>
      'Fehlende Tage können Durchschnittswerte und Trends schwächen.';

  @override
  String get dataConfidenceWarningSparse =>
      'Spärliche Daten: Trends und Statistiken können instabil sein.';

  @override
  String get dataConfidenceWarningMixedSources =>
      'Quellenwechsel können Sprünge oder scheinbar doppelte Daten erklären.';

  @override
  String get dataConfidenceWarningManual =>
      'In diesem Zeitraum sind manuelle Einträge enthalten.';

  @override
  String get dataConfidenceWarningCalculated =>
      'Dieser Wert ist abgeleitet, nicht direkt gemessen.';

  @override
  String get dataConfidenceWarningNoSources =>
      'Dieses Aggregat stellt keine Details nach Quelle bereit.';

  @override
  String get settingsBodyEnergyGroupTitle => 'Körperenergie';

  @override
  String get settingsBodyEnergyGroupBody =>
      'Kalibrierung für geschätzte Energie im Tagesverlauf und Belastungszonen.';

  @override
  String get bodyEnergyCalibrationTitle =>
      'Körperenergie-Schätzungen verbessern';

  @override
  String get bodyEnergyCalibrationBody =>
      'OpenVitals schätzt den Verbrauch anhand der Herzfrequenzintensität über die Zeit. Alter, maximale Herzfrequenz, Ruhepuls und Zonen helfen, Belastung genauer einzuordnen.';

  @override
  String get bodyEnergyCalibrationOptionalBody =>
      'Dies ist optional. Wenn du es überspringst, verwendet OpenVitals automatische Schätzungen aus Health-Connect-Daten und zeigt geringeres Vertrauen, wenn die Kalibrierung unsicher ist. Diese Werte bleiben in den OpenVitals-Einstellungen.';

  @override
  String get bodyEnergyCalibrationBirthYear => 'Geburtsjahr';

  @override
  String get bodyEnergyCalibrationMaxHr => 'Maximale Herzfrequenz';

  @override
  String get bodyEnergyCalibrationRestingHr => 'Ruhepuls';

  @override
  String get bodyEnergyCalibrationManualZones => 'Manuelle Herzfrequenzzonen';

  @override
  String get bodyEnergyCalibrationManualZonesBody =>
      'Optionale bpm-Untergrenzen für Zonen 1-5.';

  @override
  String get bodyEnergyCalibrationZone1 => 'Untere bpm-Grenze für Zone 1';

  @override
  String get bodyEnergyCalibrationZone2 => 'Untere bpm-Grenze für Zone 2';

  @override
  String get bodyEnergyCalibrationZone3 => 'Untere bpm-Grenze für Zone 3';

  @override
  String get bodyEnergyCalibrationZone4 => 'Untere bpm-Grenze für Zone 4';

  @override
  String get bodyEnergyCalibrationZone5 => 'Untere bpm-Grenze für Zone 5';

  @override
  String get bodyEnergyCalibrationUseAuto =>
      'Automatische Schätzungen verwenden';

  @override
  String get bodyEnergyCalibrationSkip => 'Vorerst überspringen';

  @override
  String get bodyEnergyCalibrationSaved =>
      'Körperenergie-Kalibrierung gespeichert';

  @override
  String get bodyEnergyCalibrationReset =>
      'Körperenergie-Kalibrierung auf automatisch zurückgesetzt';

  @override
  String get bodyEnergyNotSetUp => 'Nicht eingerichtet';

  @override
  String get bodyEnergyTimelineEstimated => 'Von OpenVitals geschätzt';

  @override
  String get bodyEnergyTimelineCurrent => 'Aktuell';

  @override
  String get bodyEnergyTimelineStart => 'Start';

  @override
  String get bodyEnergyTimelineCharged => 'Aufgeladen';

  @override
  String get bodyEnergyTimelineDrained => 'Verbraucht';

  @override
  String get bodyEnergyTimelineConfidence => 'Vertrauen';

  @override
  String get bodyEnergyTimelineNoData =>
      'Keine verwendbare Körperenergie-Zeitleiste für diesen Zeitraum.';

  @override
  String get bodyEnergyTimelineDayTitle => 'Tagesverlauf';

  @override
  String get bodyEnergyTimelineLowConfidence =>
      'Einige Abschnitte sind geschätzt, weil Kalibrierung oder Health-Connect-Daten unvollständig sind.';

  @override
  String get bodyEnergyWhyTitle => 'Was es bewegt hat';

  @override
  String get bodyEnergyWhyEmpty =>
      'Noch hat kein klarer Auflade- oder Verbrauchsfaktor diesen Tag dominiert.';

  @override
  String get bodyEnergyInfluenceSleepRecovery => 'Schlaf-Erholung';

  @override
  String get bodyEnergyInfluenceQuietRest => 'Ruhige Erholung';

  @override
  String get bodyEnergyInfluenceExertion => 'Belastung';

  @override
  String get bodyEnergyInfluenceElevatedHr => 'Erhöhte Herzfrequenz';

  @override
  String get bodyEnergyInfluenceRecoveryDebt => 'Erholungsschuld';

  @override
  String get bodyEnergyInfluenceNoData => 'Keine Daten';

  @override
  String get bodyEnergyInfluenceSteady => 'Stabil';

  @override
  String get bodyEnergyReasonSleepRecoveryDetail =>
      'Schlafabschnitte haben die Schätzung vom vorherigen Wert aus aufgeladen.';

  @override
  String get bodyEnergyReasonQuietRestDetail =>
      'Niedrige Herzfrequenz im Wachzustand hat eine kleine Erholungsladung hinzugefügt.';

  @override
  String get bodyEnergyReasonExertionDetail =>
      'Herzfrequenzintensität oder aufgezeichnete Trainings haben die Schätzung verbraucht.';

  @override
  String get bodyEnergyReasonElevatedHrDetail =>
      'Herzfrequenz im Wachzustand über dem Ruhewert hat Stressverbrauch hinzugefügt.';

  @override
  String get bodyEnergyReasonRecoveryDebtDetail =>
      'Kürzliche stärkere Belastung hielt danach einen kleinen Verbrauch aktiv.';

  @override
  String get bodyEnergyReasonNoDataDetail =>
      'Health Connect lieferte für diesen Abschnitt nicht genug Signal.';

  @override
  String get bodyEnergyReasonSteadyDetail =>
      'Die Schätzung blieb überwiegend stabil.';

  @override
  String get bodyEnergyInputsTitle => 'Verwendete Eingaben';

  @override
  String bodyEnergyInputsSummary(int arg0, int arg1) {
    return 'Algorithmus v$arg0, $arg1-Minuten-Abschnitte';
  }

  @override
  String get bodyEnergyInputHeartRate => 'Herzfrequenzproben';

  @override
  String get bodyEnergyInputSleep => 'Schlafsitzungen';

  @override
  String get bodyEnergyInputWorkouts => 'Trainings';

  @override
  String get bodyEnergyInputRestingHr => 'Ruheherzfrequenz';

  @override
  String get bodyEnergyInputHrBaseline => 'Herzfrequenz-Basislinie';

  @override
  String get bodyEnergyInputHrv => 'HRV-Modifikator';

  @override
  String get bodyEnergyInputRespiratory => 'Atemfrequenz-Modifikator';

  @override
  String get bodyEnergyInputPreviousScore => 'Vorheriger Wert';

  @override
  String get bodyEnergyInputCalibration => 'Kalibrierung';

  @override
  String get bodyEnergyInputAvailable => 'Verfügbar';

  @override
  String get bodyEnergyInputMissing => 'Fehlt';

  @override
  String get bodyEnergyInputOptional => 'Nicht vorhanden';

  @override
  String bodyEnergyInputRecords(int arg0) {
    return '$arg0 Datensätze';
  }

  @override
  String bodyEnergyInputSessions(int arg0) {
    return '$arg0 Sitzungen';
  }

  @override
  String bodyEnergyInputWorkoutsValue(int arg0) {
    return '$arg0 Trainings';
  }

  @override
  String bodyEnergyInputPreviousScoreValue(String arg0) {
    return '$arg0 Start';
  }

  @override
  String get bodyEnergyCalibrationModeAuto => 'Automatische Schätzungen';

  @override
  String get bodyEnergyCalibrationModeManualValues => 'Manuelle Werte';

  @override
  String get bodyEnergyCalibrationModeManualZones => 'Manuelle Zonen';

  @override
  String get bodyEnergyCalculationTitle => 'Wie Körperenergie geschätzt wird';

  @override
  String get bodyEnergyCalculationBody =>
      'OpenVitals teilt den ausgewählten Tag in kurze Abschnitte, startet wenn möglich mit dem vorherigen Wert, addiert Aufladung durch Schlaf oder ruhige Erholung und zieht Verbrauch durch Belastung, erhöhte wache Herzfrequenz und Erholungsschuld nach stärkerer Belastung ab.';

  @override
  String get bodyEnergyCalculationInputsBody =>
      'Herzfrequenz, Ruheherzfrequenz, persönliche Zonen, Schlaf, Trainings, HRV und Atemfrequenz können die Schätzung verbessern. Fehlende Eingaben machen die Schätzung konservativer und senken das Vertrauen.';

  @override
  String get bodyEnergyCalculationLimitsBody =>
      'Dies ist eine Wellness-Schätzung auf dem Gerät, keine direkte Messung und kein medizinischer Rat. Die angezeigten Eingaben und Gründe werden offengelegt, damit die Methode geprüft und verbessert werden kann.';

  @override
  String get metricBodyEnergy => 'Körperenergie';

  @override
  String get privacyPolicyTitle => 'Datenschutzerklärung';

  @override
  String get privacyPolicyBody1 =>
      'OpenVitals liest Daten aus Health Connect, um Schritte, Trainings, Schlaf, Herzfrequenz, Gewicht, Kalorien, Hydrierung, Ernährung, Achtsamkeit und Vitalwerte auf deinem Gerät anzuzeigen. Einträge, die du ausdrücklich protokollierst, einschließlich importierter GPX/KML/KMZ-Routen und importierter FIT-Dateien, werden in Health Connect geschrieben.';

  @override
  String get privacyPolicyBody2 =>
      'Diese App lädt deine Gesundheitsdaten nicht in einen Cloud-Dienst hoch, enthält keine Werbung und teilt keine Daten mit Dritten.';

  @override
  String get privacyPolicyBody3 =>
      'OpenVitals ist kein Medizinprodukt und diagnostiziert, behandelt, heilt oder verhindert keine Krankheit oder medizinische Erkrankung. Es ersetzt keine medizinische Beratung, Diagnose oder Behandlung durch qualifiziertes medizinisches Fachpersonal.';

  @override
  String get linkCouldNotOpen => 'Der Link konnte nicht geöffnet werden.';

  @override
  String get activitySplitsLapsTitle => 'Laps';

  @override
  String get activitySplitsLapsBody =>
      'Recorded by the device or app that saved this activity.';

  @override
  String activitySplitsDerivedTitle(String distance) {
    return 'Splits · every $distance';
  }

  @override
  String get activitySplitsEstimatedTitle => 'Splits · estimated';

  @override
  String get activitySplitsEstimatedBody =>
      'This activity has no per-time distance data, so every split shows the activity\'s average pace.';

  @override
  String get activitySplitsHeaderSplit => 'Split';

  @override
  String get activitySplitsHeaderLap => 'Lap';

  @override
  String get activitySplitsPartial => 'partial';

  @override
  String activitySplitsFaster(String delta) {
    return '$delta faster than average';
  }

  @override
  String activitySplitsSlower(String delta) {
    return '$delta slower than average';
  }

  @override
  String get settingsActivitySplitDistanceTitle => 'Split distance';

  @override
  String get settingsActivitySplitDistanceBody =>
      'How often the activity details screen cuts a split when the recording has no laps of its own.';

  @override
  String get settingsActivitySplitDistanceChoice => 'Every split';

  @override
  String activitySplitDistanceKilometers(String distance) {
    return '$distance km';
  }

  @override
  String activitySplitDistanceMiles(String distance) {
    return '$distance mi';
  }
}
