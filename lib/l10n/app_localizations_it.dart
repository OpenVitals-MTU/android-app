// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Italian (`it`).
class AppLocalizationsIt extends AppLocalizations {
  AppLocalizationsIt([String locale = 'it']) : super(locale);

  @override
  String get appName => 'OpenVitals';

  @override
  String get cdBack => 'Indietro';

  @override
  String get cdSettings => 'Impostazioni';

  @override
  String get cdAchievements => 'Obiettivi';

  @override
  String get cdDailyReadiness => 'Prontezza Giornaliera';

  @override
  String get cdSensorBatteryStatus => 'Stato batteria sensore';

  @override
  String get cdEditDashboard => 'Modifica riepilogo';

  @override
  String get cdFinishDashboardEditing => 'Termina modifica riepilogo';

  @override
  String get cdEditSavedDrinks => 'Modifica bevande salvate';

  @override
  String get cdDoneEditingSavedDrinks => 'Termina modifica bevande salvate';

  @override
  String get cdEditDrink => 'Modifica bevanda';

  @override
  String get cdDeleteDrink => 'Elimina bevanda';

  @override
  String get cdMoveDrinkCategory => 'Sposta categoria bevanda';

  @override
  String cdExpandDrinkCategory(String arg0) {
    return 'Espandi $arg0';
  }

  @override
  String cdCollapseDrinkCategory(String arg0) {
    return 'Comprimi $arg0';
  }

  @override
  String get cdEditManualEntryWidgets => 'Modifica';

  @override
  String get cdFinishManualEntryEditing =>
      'Termina modifica widget di inserimento';

  @override
  String get cdEditRecordingDashboard => 'Modifica dashboard registrazione';

  @override
  String get cdFinishRecordingDashboardEditing =>
      'Termina modifica dashboard registrazione';

  @override
  String get cdMoveWidgetUp => 'Sposta widget su';

  @override
  String get cdMoveWidgetDown => 'Sposta widget giù';

  @override
  String get cdEditMetricSections => 'Modifica sezioni metriche';

  @override
  String get cdFinishMetricSectionEditing =>
      'Termina modifica sezioni metriche';

  @override
  String get cdMoveSectionUp => 'Sposta sezione su';

  @override
  String get cdMoveSectionDown => 'Sposta sezione giù';

  @override
  String get cdRemoveWidget => 'Rimuovi widget';

  @override
  String get cdDecreaseRecordingDashboardWidgetSize =>
      'Rendi widget più piccolo';

  @override
  String get cdIncreaseRecordingDashboardWidgetSize =>
      'Rendi widget più grande';

  @override
  String get cdExitRecordingFocusMode => 'Esci dalla modalità focus';

  @override
  String get cdToggleRecordingOutdoorMode =>
      'Cambia modalità leggibilità outdoor';

  @override
  String get cdRecenterMap => 'Ricentra mappa';

  @override
  String get cdDeleteEntry => 'Elimina voce';

  @override
  String get cdEditEntry => 'Modifica voce';

  @override
  String get cdPreviousDay => 'Giorno precedente';

  @override
  String get cdNextDay => 'Giorno successivo';

  @override
  String get cdPreviousPeriod => 'Periodo precedente';

  @override
  String get cdNextPeriod => 'Periodo successivo';

  @override
  String get cdOpenCalendar => 'Apri calendario';

  @override
  String get actionCancel => 'Annulla';

  @override
  String get actionAdd => 'Aggiungi';

  @override
  String get actionAddCustom => 'Aggiungi personalizzato';

  @override
  String get actionSave => 'Salva';

  @override
  String get actionClose => 'Chiudi';

  @override
  String get actionContinue => 'Continua';

  @override
  String get actionDelete => 'Elimina';

  @override
  String get actionDetails => 'Dettagli';

  @override
  String get actionEdit => 'Modifica';

  @override
  String get actionEnable => 'Abilita';

  @override
  String get actionGetStarted => 'Inizia';

  @override
  String get actionGrant => 'Concedi';

  @override
  String get actionGrantPermission => 'Concedi l\'autorizzazione';

  @override
  String get actionLoadMoreEntries => 'Carica altri 10';

  @override
  String get actionShowCalculation => 'Mostra calcolo';

  @override
  String get actionHideCalculation => 'Nascondi calcolo';

  @override
  String get actionNotNow => 'Non ora';

  @override
  String get actionAccept => 'Accetto';

  @override
  String get actionOpen => 'Apri';

  @override
  String get actionPause => 'Pausa';

  @override
  String get actionReview => 'Rivedi';

  @override
  String get actionResume => 'Riprendi';

  @override
  String get actionRefresh => 'Ricarica';

  @override
  String get actionSelect => 'Seleziona';

  @override
  String get actionStart => 'Inizia';

  @override
  String get actionFinish => 'Termina';

  @override
  String get actionDiscard => 'Scarta';

  @override
  String get unknownError => 'Errore sconosciuto';

  @override
  String get screenErrorNotFound =>
      'L\'elemento richiesto non è stato trovato.';

  @override
  String get screenErrorMissingArgument =>
      'Informazioni obbligatorie mancanti.';

  @override
  String get screenErrorPermissionDenied =>
      'È necessaria l\'autorizzazione per caricare questi dati.';

  @override
  String get screenErrorHealthConnectUnavailable =>
      'Health Connect non è disponibile su questo dispositivo.';

  @override
  String get screenErrorLoadSleepSession =>
      'Impossibile caricare la sessione di sonno.';

  @override
  String get screenErrorLoadSleepPeriod =>
      'Impossibile caricare i dati del sonno.';

  @override
  String get notAvailable => 'Non disponibile';

  @override
  String get notRecorded => 'Non registrato';

  @override
  String get noData => 'Nessun dato';

  @override
  String get loading => 'Caricamento…';

  @override
  String get homeMetricWidgetDescription => 'Metrica OpenVitals';

  @override
  String get homeMetricWidgetConfigTitle => 'Scegli metrica';

  @override
  String get homeMetricWidgetConfigPrompt => 'Scegli la metrica per il widget:';

  @override
  String get homeMetricWidgetNoMetrics => 'Nessuna metrica disponibile.';

  @override
  String get homeMetricWidgetPermissionNeeded =>
      'Concedi l\'autorizzazione in OpenVitals';

  @override
  String get homeMetricWidgetUpdateFailed => 'Impossibile aggiornare';

  @override
  String get homeMetricWidgetOpenForDetails => 'Apri per i dettagli';

  @override
  String get homeMetricWidgetNotConfigured => 'Seleziona una metrica';

  @override
  String get homeQuickBeverageWidgetDescription => 'Bevanda rapida';

  @override
  String get homeQuickBeverageOneTapWidgetDescription => 'Bevanda rapida 1x1';

  @override
  String get homeQuickBeverageWidgetConfigTitle => 'Scegli bevanda';

  @override
  String get homeQuickBeverageWidgetConfigPrompt =>
      'Scegli la bevanda per il widget:';

  @override
  String get homeQuickBeverageWidgetNoDrinks => 'Nessuna bevanda disponibile.';

  @override
  String get homeQuickBeverageWidgetNotConfigured => 'Seleziona una bevanda';

  @override
  String get homeQuickBeverageWidgetTapToLog => 'Tocca per registrare';

  @override
  String get homeQuickBeverageWidgetSaved => 'Salvata ora';

  @override
  String get homeQuickBeverageWidgetSavedNutrition => 'Salvata come nutrizione';

  @override
  String get homeDailyReadinessWidgetDescription =>
      'OpenVitals prontezza giornaliera';

  @override
  String get homeBodyEnergyWidgetDescription => 'OpenVitals energia corporea';

  @override
  String get homeTodayVitalsWidgetDescription =>
      'OpenVitals parametri vitali di oggi';

  @override
  String get homeWidgetTodayTitle => 'Oggi';

  @override
  String get homeWidgetContext => 'Contesto';

  @override
  String get homeWidgetHrvShort => 'HRV';

  @override
  String get homeWidgetBodyEnergyCharged => 'Caricato';

  @override
  String get homeWidgetBodyEnergySteady => 'Stabile';

  @override
  String get homeWidgetBodyEnergyLimited => 'Limitato';

  @override
  String get homeWidgetBodyEnergyLow => 'Basso';

  @override
  String get screenSteps => 'Passi';

  @override
  String get screenActivities => 'Attività';

  @override
  String get screenCalories => 'Calorie';

  @override
  String get screenActivityDetail => 'Dettaglio attività';

  @override
  String get screenSleep => 'Sonno';

  @override
  String get screenSleepDetail => 'Dettaglio sonno';

  @override
  String get screenHeartVitals => 'Cuore & Parametri Vitali';

  @override
  String get screenStressTracking => 'Tracciamento Stress';

  @override
  String get screenBodyEnergy => 'Energia Corporea';

  @override
  String get screenTrainingReadiness => 'Prontezza Allenamento';

  @override
  String get screenBody => 'Corpo';

  @override
  String get screenHydration => 'Idratazione';

  @override
  String get screenNutrition => 'Nutrizione';

  @override
  String get screenMindfulness => 'Consapevolezza';

  @override
  String get screenCycle => 'Ciclo';

  @override
  String get screenDailyReadiness => 'Prontezza Giornaliera';

  @override
  String get screenSettings => 'Impostazioni';

  @override
  String get screenAchievements => 'Obiettivi';

  @override
  String get screenManualEntry => 'Aggiungi voce';

  @override
  String get screenHydrationEntry => 'Voce idratazione';

  @override
  String get screenActivityEntry => 'Voce attività';

  @override
  String get screenMindfulnessEntry => 'Voce consapevolezza';

  @override
  String get screenCarbsEntry => 'Voce carboidrati';

  @override
  String get screenBodyMeasurementEntry => 'Voce misurazione corporea';

  @override
  String get screenVitalsMeasurementEntry => 'Voce parametri vitali';

  @override
  String get bottomNavDashboard => 'Sommario';

  @override
  String get manualEntryHydrationTitle => 'Idratazione';

  @override
  String get manualEntryActivityTitle => 'Attività';

  @override
  String get manualEntryDateLabel => 'Inserisci data';

  @override
  String get manualEntryTimeLabel => 'Inserisci ora';

  @override
  String get manualEntrySelectTime => 'Seleziona orario di inserimento';

  @override
  String get manualEntryAddWidgets => 'Aggiungi widget';

  @override
  String get manualEntryAllWidgetsAdded => 'Tutti i widget sono già mostrati.';

  @override
  String get manualEntryWritePermissionTitle =>
      'Permessi di scrittura idratazione';

  @override
  String get manualEntryActivityWritePermissionTitle =>
      'Autorizzazioni di scrittura attività';

  @override
  String get manualEntryMindfulnessWritePermissionTitle =>
      'Autorizzazione di scrittura consapevolezza';

  @override
  String get manualEntryCarbsWritePermissionTitle =>
      'Autorizzazione di scrittura carboidrati';

  @override
  String manualEntryBodyWritePermissionTitle(String arg0) {
    return 'Autorizzazione di scrittura $arg0';
  }

  @override
  String manualEntryVitalsWritePermissionTitle(String arg0) {
    return 'Autorizzazione di scrittura $arg0';
  }

  @override
  String get mindfulnessEntrySubtitle =>
      'Le sessioni di consapevolezza vengono salvate direttamente su Health Connect.';

  @override
  String get mindfulnessEntryPermissionNeeded =>
      'Per il riepilogo, OpenVitals chiede solo i permessi di visualizzazione. Per aggiungere voci di Consapevolezza, abbiamo bisogno del permesso di scrittura. OpenVitals non memorizzerà queste sessioni; le voci vengono salvate in Health Connect.';

  @override
  String get activityEntrySubtitle =>
      'Crea una sessione di attività Health Connect. I file di percorso sono scritti solo quando ne importi uno.';

  @override
  String get activityEntryPermissionNeeded =>
      'Per il riepilogo, OpenVitals richiede solo i permessi di visualizzazione. Per aggiungere attività, abbiamo bisogno di permessi di scrittura Health Connect per sessioni, percorsi, distanza, elevazione, calorie attive e calorie totali; le voci del tapis roulant chiedono i passaggi quando necessario. OpenVitals non memorizzerà queste voci; sono salvate in Health Connect.';

  @override
  String get activityEntrySourceBody =>
      'Crea un\'attività manualmente, usa un piano esistente o registra un percorso GPS.';

  @override
  String get activityEntryCreateManual => 'Crea manualmente';

  @override
  String get activityEntryCreateFromExistingPlan =>
      'Crea da programma esistente';

  @override
  String get activityEntryRecordGps => 'Registra attività';

  @override
  String get activityEntryChooseAnotherSource => 'Scegli un altro metodo';

  @override
  String get activityEntryTypeLabel => 'Tipo attività';

  @override
  String get activityEntryTitleLabel => 'Titolo';

  @override
  String get activityEntryStartDateLabel => 'Data d\'inizio';

  @override
  String get activityEntryStartTimeLabel => 'Ora d\'inizio';

  @override
  String get activityEntrySelectTime => 'Seleziona ora d\'inizio';

  @override
  String get activityEntryDurationLabel => 'Durata minima';

  @override
  String get activityEntryRepetitionsTitle => 'Ripetizioni';

  @override
  String get activityEntryStepsTitle => 'Passi';

  @override
  String get activityEntryRepetitionModeTotal => 'Totali';

  @override
  String get activityEntryRepetitionModeSets => 'Serie';

  @override
  String get activityEntryRepetitionsLabel => 'Ripetizioni';

  @override
  String get activityEntryStepsLabel => 'Passi';

  @override
  String activityEntrySetRepetitionsLabel(int arg0) {
    return 'Serie $arg0 rip.';
  }

  @override
  String get activityEntrySetRestLabel => 'Tempo di riposo';

  @override
  String get activityEntryAddSet => 'Aggiungi serie';

  @override
  String get activityEntryTrainingPlansTitle => 'Programmi di allenamento';

  @override
  String get activityEntryTrainingPlansLoading =>
      'Caricamento dei programmi Health Connect';

  @override
  String get activityEntryTrainingPlansEmpty =>
      'Nessun programma Health Connect per questa data e tipo di attività';

  @override
  String get activityEntryTrainingPlanLabel => 'Programma di allenamento';

  @override
  String get activityEntryTrainingPlanSelect => 'Seleziona un programma';

  @override
  String get activityEntryTrainingPlanNew => 'Nuovo programma';

  @override
  String get activityEntryTrainingPlanUnnamed => 'Programma senza nome';

  @override
  String get activityEntrySaveTrainingPlan => 'Salva programma';

  @override
  String get activityEntryUpdateTrainingPlan => 'Aggiorna programma';

  @override
  String get activityEntryPlanActivityPickerTitle => 'Attività con programmi';

  @override
  String get activityEntryPlanActivityPickerEmpty =>
      'Nessun programma Health Connect trovato';

  @override
  String get activityEntryPlanPickerTitle => 'Scegli un programma';

  @override
  String get activityEntryPlanPickerEmpty =>
      'Nessun programma trovato per questa attività';

  @override
  String get activityEntryPlanChooseActivity => 'Scegli un\'altra attività';

  @override
  String activityEntryPlanOneSetSummary(int arg0) {
    return '1 serie • $arg0 ripetizioni';
  }

  @override
  String activityEntryPlanSummary(int arg0, int arg1) {
    return '$arg0 serie • $arg1 ripetizioni';
  }

  @override
  String activityEntryPlanPreviewReps(int arg0) {
    return '$arg0 ripetizioni';
  }

  @override
  String activityEntryPlanPreviewRest(int arg0) {
    return 'riposo $arg0 sec';
  }

  @override
  String activityEntryPlanPreviewMore(int arg0) {
    return '+$arg0 in più';
  }

  @override
  String activityEntryDistanceLabel(String arg0) {
    return 'Distanza $arg0';
  }

  @override
  String activityEntryElevationLabel(String arg0) {
    return 'Salita $arg0';
  }

  @override
  String get activityEntryNotesLabel => 'Note';

  @override
  String get activityEntryFeelingLabel => 'Come ti sei sentito?';

  @override
  String get activityEntryFeelingGreat => 'Ottimo';

  @override
  String get activityEntryFeelingGood => 'Buono';

  @override
  String get activityEntryFeelingHard => 'Difficile';

  @override
  String get activityEntryFeelingRough => 'Tosto';

  @override
  String get activityEntryImportedRoute => 'Percorso importato';

  @override
  String get activityEntryRecordingTitle => 'Registrazione attività';

  @override
  String get activityEntryRecordingReadyBody =>
      'Scegli il tipo di attività, quindi inizia quando sei pronto. Dopo aver terminato, puoi rivedere e aggiungere dettagli prima di salvare.';

  @override
  String get activityEntryRecordingGoToActivityScreen =>
      'Vai alla schermata attività';

  @override
  String get activityEntryRecordingActive => 'Registrazione';

  @override
  String get activityEntryRecordingPaused => 'In pausa';

  @override
  String get activityEntryRecordingIdle => 'Inattivo';

  @override
  String get activityEntryRecordingResting => 'Riposo';

  @override
  String get activityEntryRecordingGpsFix => 'GPS pronto';

  @override
  String get activityEntryRecordingGpsPoor => 'GPS debole';

  @override
  String get activityEntryRecordingGpsLost => 'GPS perso';

  @override
  String get activityEntryRecordingGpsOff => 'GPS spento';

  @override
  String get activityEntryRecordingTabMap => 'Mappa';

  @override
  String get activityEntryRecordingTabStats => 'Statistiche';

  @override
  String get activityEntryRecordingTabIntervals => 'Intervalli';

  @override
  String get activityEntryRecordingTabByTime => 'Per tempo';

  @override
  String get activityEntryRecordingTabByDistance => 'Per distanza';

  @override
  String get activityEntryRecordingTimeSplit => 'Suddivisione per tempo';

  @override
  String get activityEntryRecordingDistanceSplit => 'Suddivisione per distanza';

  @override
  String activityEntryRecordingSplitMinutes(int arg0) {
    return '$arg0 min';
  }

  @override
  String activityEntryRecordingSplitInterval(int arg0) {
    return 'Intervallo $arg0';
  }

  @override
  String activityEntryRecordingSplitTimeRange(int arg0, int arg1) {
    return '$arg0-$arg1 min';
  }

  @override
  String get activityEntryRecordingSplitElapsed => 'Trascorso';

  @override
  String get activityEntryRecordingSplitAvg => 'Media';

  @override
  String get activityEntryRecordingSplitMax => 'Massimo';

  @override
  String get activityEntryRecordingNoIntervals => 'Ancora nessun intervallo';

  @override
  String get activityEntryRecordingNoTimeSplits =>
      'Ancora nessuna suddivisione di tempo';

  @override
  String get activityEntryRecordingNoDistanceSplits =>
      'Ancora nessuna suddivisione di distanza';

  @override
  String get activityEntryRecordingLap => 'Giro';

  @override
  String get activityEntryRecordingMarker => 'Aggiungi contrassegno';

  @override
  String activityEntryRecordingMarkerDefaultName(int arg0) {
    return 'Contrassegno $arg0';
  }

  @override
  String get activityEntryRecordingMarkersTitle => 'Contrassegni';

  @override
  String get activityEntryRecordingMarkerName => 'Nome';

  @override
  String get activityEntryRecordingMarkerNote => 'Nota';

  @override
  String get activityEntryRecordingWaitingForGps =>
      'In attesa di una correzione precisa del GPS';

  @override
  String get activityEntryRecordingGpsWaiting =>
      'In attesa di una correzione precisa del GPS prima d\'iniziare';

  @override
  String activityEntryRecordingGpsWaitingAccuracy(String arg0) {
    return 'In attesa di una migliore precisione del GPS • $arg0';
  }

  @override
  String activityEntryRecordingGpsReady(String arg0) {
    return 'GPS pronto • precisione $arg0';
  }

  @override
  String get activityEntryRecordingGpsDisabled =>
      'Attiva il GPS per iniziare la registrazione.';

  @override
  String get activityEntryRecordingDistance => 'Distanza';

  @override
  String get activityEntryRecordingTotalTime => 'Tempo totale';

  @override
  String get activityEntryRecordingMovingTime => 'Tempo di movimento';

  @override
  String get activityEntryRecordingRestTime => 'Tempo di riposo';

  @override
  String get activityEntryRecordingSpeed => 'Velocità';

  @override
  String get activityEntryRecordingMaxSpeed => 'Velocità massima';

  @override
  String get activityEntryRecordingAverageSpeed => 'Velocità media';

  @override
  String get activityEntryRecordingAverageMovingSpeed =>
      'Velocità media di movimento';

  @override
  String get activityEntryRecordingElevationGain => 'Dislivello';

  @override
  String get activityEntryRecordingPoints => 'Punti';

  @override
  String get activityEntryRecordingRestSecondsLabel => 'Secondi di riposo';

  @override
  String get activityEntryRecordingEndSet => 'Termina serie';

  @override
  String get activityEntryRecordingStartNextSet => 'Avvia la prossima serie';

  @override
  String get activityEntryRecordingEndSession => 'Termina sessione';

  @override
  String activityEntryRecordingRestRemaining(String arg0) {
    return 'Riposo $arg0';
  }

  @override
  String get activityEntryRecordingFinishHint =>
      'Terminare apre il modulo dei dettagli attività per aggiungere titolo, note, calorie o modificare i valori prima di salvare.';

  @override
  String get activityEntryRecordingRepetitionCorrectionHint =>
      'Usa + o - se il sensore manca o aggiunge una ripetizione.';

  @override
  String activityEntryRecordingAccuracy(String arg0) {
    return 'Ultima precisione $arg0';
  }

  @override
  String get activityEntryRecordingFocus => 'Focus';

  @override
  String get activityEntryRecordingDashboardLayout => 'Layout dashboard';

  @override
  String get activityEntryRecordingDashboardLayoutTwoByFour => '2x4';

  @override
  String get activityEntryRecordingDashboardLayoutThreeByFour => '3x4';

  @override
  String get activityEntryRecordingDashboardLayoutLargeTop => 'Grande in alto';

  @override
  String get activityEntryRecordingDashboardAddField => 'Aggiungi widget';

  @override
  String activityEntryRouteSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  ) {
    return '$arg0 • $arg1 • $arg2 dislivello • $arg3 punti';
  }

  @override
  String activityEntryRouteAverageMetrics(String arg0, String arg1) {
    return 'Ritmo medio $arg0 • velocità media $arg1';
  }

  @override
  String get activityEntryAdd => 'Salva attività';

  @override
  String get activityEntryInvalidValue =>
      'Correggi i campi evidenziati prima di salvare l\'attività.';

  @override
  String get activityEntryErrorActivityTypeRoute =>
      'Scegli un tipo di attività compatibile con i percorsi GPS.';

  @override
  String get activityEntryErrorTrainingPlanTitleRequired =>
      'Inserisci un titolo per salvare questo programma di allenamento.';

  @override
  String get activityEntryErrorStartDate => 'Scegli una data di inizio valida.';

  @override
  String get activityEntryErrorStartTime => 'Scegli un\'ora di inizio valida.';

  @override
  String get activityEntryErrorStartTimeAfterRoute =>
      'L\'ora di inizio deve essere uguale o precedente all\'inizio del percorso importato.';

  @override
  String get activityEntryErrorDuration =>
      'La durata deve essere compresa tra 1 minuto e 7 giorni.';

  @override
  String get activityEntryErrorRepetitions =>
      'Inserisci conteggi positivi. Il riposo deve rientrare nella durata dell\'attività.';

  @override
  String get activityEntryErrorDistance =>
      'Inserisci una distanza maggiore di 0.';

  @override
  String get activityEntryErrorDistanceUnsupported =>
      'Questo tipo di attività non supporta la distanza.';

  @override
  String get activityEntryErrorElevation =>
      'Inserisci un\'elevazione maggiore di 0.';

  @override
  String get activityEntryErrorElevationUnsupported =>
      'Questo tipo di attività non supporta il dislivello.';

  @override
  String get activityEntryErrorActiveCalories =>
      'Inserisci calorie attive maggiori di 0.';

  @override
  String get activityEntryErrorTotalCalories =>
      'Inserisci calorie totali maggiori di 0.';

  @override
  String get activityEntryErrorTotalCaloriesBelowActive =>
      'Le calorie totali non possono essere inferiori alle calorie attive.';

  @override
  String get activityEntryLocationPermissionNeeded =>
      'Il permesso di accesso alla posizione precisa è necessario per registrare attività GPS.';

  @override
  String get activityEntryNotificationPermissionNeeded =>
      'Il permesso per le notifiche è necessario affinché OpenVitals mostri una notifica di registrazione in corso.';

  @override
  String get activityEntryActivityRecognitionPermissionNeeded =>
      'Il permesso di riconoscimento attività è necessario per contare i passi sul tapis roulant.';

  @override
  String activityEntryRouteImportFailed(String arg0) {
    return 'Impossibile importare il file del percorso: $arg0';
  }

  @override
  String activityEntryRecordingFailed(String arg0) {
    return 'Impossibile registrare l\'attività: $arg0';
  }

  @override
  String activityEntryWriteFailed(String arg0) {
    return 'Impossibile salvare la voce attività: $arg0';
  }

  @override
  String get activityRouteOpenInMap => 'Apri percorso nell\'app mappe';

  @override
  String get activityRouteExportGpx => 'Salva GPX';

  @override
  String get activityRouteExportKmz => 'Salva KMZ';

  @override
  String get activityRouteExportSaved => 'Percorso salvato.';

  @override
  String get activityRouteExportFailed =>
      'Impossibile salvare il file del percorso.';

  @override
  String get activityRouteOpenChooserTitle => 'Apri percorso con';

  @override
  String get activityRouteOpenFailed =>
      'Nessuna app mappe ha potuto aprire questo percorso.';

  @override
  String get activityDetailAnalysisTitle => 'Analisi percorso';

  @override
  String get activityDetailTabMarkers => 'Contrassegni';

  @override
  String get activityDetailNoMarkers => 'Ancora nessun contrassegno';

  @override
  String activityRecordingVoiceSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  ) {
    return 'Tempo $arg0. Distanza $arg1. Velocità media $arg2. Giro corrente $arg3.';
  }

  @override
  String activityRecordingVoiceLap(int arg0, String arg1) {
    return 'Giro $arg0. $arg1';
  }

  @override
  String get activityRecordingVoiceIdle => 'Inattivo.';

  @override
  String get activityRecordingVoiceResumed => 'Registrazione ripresa.';

  @override
  String get activityRecordingNotificationChannel => 'Registrazione attività';

  @override
  String get activityRecordingNotificationTitle =>
      'Registrazione attività in corso';

  @override
  String activityRecordingNotificationRecording(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return 'Registrazione • $arg0 totale • $arg1 movimento • $arg2 • $arg3';
  }

  @override
  String activityRecordingNotificationPaused(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return 'In pausa • $arg0 totale • $arg1 movimento • $arg2 • $arg3';
  }

  @override
  String activityRecordingNotificationRepetitionRecording(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Registrazione • $arg0 totale • $arg1 $arg2';
  }

  @override
  String activityRecordingNotificationRepetitionPaused(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'In pausa • $arg0 totale • $arg1 $arg2';
  }

  @override
  String activityRecordingNotificationRepetitionResting(
    String arg0,
    String arg1,
  ) {
    return 'Riposo • $arg0 totale • $arg1 rimanente';
  }

  @override
  String activityRecordingNotificationTimedRecording(String arg0) {
    return 'Registrazione • $arg0 totale';
  }

  @override
  String activityRecordingNotificationTimedPaused(String arg0) {
    return 'In pausa • $arg0 totale';
  }

  @override
  String get activityRecordingErrorService =>
      'Impossibile avviare il servizio di registrazione attività.';

  @override
  String get activityRecordingErrorPreciseLocationPermission =>
      'Il permesso di accesso alla posizione precisa è necessario per tracce GPS affidabili.';

  @override
  String get activityRecordingErrorNotificationPermission =>
      'Il permesso per le notifiche è necessario per mostrare la notifica di registrazione in corso.';

  @override
  String get activityRecordingErrorActivityRecognitionPermission =>
      'Il permesso di riconoscimento attività è necessario per contare i passi sul tapis roulant.';

  @override
  String get activityRecordingErrorWaitingForGps =>
      'Attendi una correzione GPS precisa prima di iniziare.';

  @override
  String get activityRecordingErrorProvider =>
      'Attiva il GPS per registrare un percorso.';

  @override
  String get activityRecordingErrorUnsupportedType =>
      'Questo tipo di attività non può essere registrato in tempo reale.';

  @override
  String get activityRecordingErrorProximitySensor =>
      'Questo dispositivo non dispone di un sensore di prossimità per il conteggio dei piegamenti.';

  @override
  String get activityRecordingErrorAccelerometer =>
      'Questo dispositivo non dispone di un accelerometro per questa registrazione.';

  @override
  String get activityRecordingErrorStepDetector =>
      'Questo dispositivo non supporta il rilevamento passi di Android.';

  @override
  String get activityRecordingHowItWorks => 'Come funziona la registrazione';

  @override
  String get activityRecordingGuidancePushUps =>
      'Posiziona il telefono con lo schermo verso l\'alto sotto il petto o la testa. Il sensore di prossimità conta una ripetizione quando ti avvicini al telefono.';

  @override
  String get activityRecordingGuidancePullUps =>
      'Fissa il telefono sul corpo. L\'accelerometro conta il movimento di trazione e rilascio.';

  @override
  String get activityRecordingGuidanceRopeSkipping =>
      'Tieni il telefono fisso sul corpo. L\'accelerometro conta i salti.';

  @override
  String get activityRecordingGuidanceTrampolineJumping =>
      'Tieni il telefono fisso sul corpo. Il rilevamento dei salti usa una finestra temporale più lunga rispetto alla corda.';

  @override
  String get activityRecordingGuidanceTreadmill =>
      'Porta il telefono sul corpo. Il rilevatore passi di Android conta i passi; nessun percorso GPS viene registrato.';

  @override
  String get activityRecordingSensorReady => 'Sensore pronto';

  @override
  String get activityRecordingSensorUnavailableManual =>
      'Il conteggio in tempo reale non è disponibile su questo dispositivo. L\'inserimento manuale è comunque disponibile.';

  @override
  String get activityRecordingActivityRecognitionMissing =>
      'Concedi il riconoscimento attività per contare i passi sul tapis roulant.';

  @override
  String get exerciseTypeRunning => 'Corsa';

  @override
  String get exerciseTypeBiking => 'Ciclismo';

  @override
  String get exerciseTypeWalking => 'Camminata';

  @override
  String get exerciseTypeHiking => 'Escursionismo';

  @override
  String get exerciseTypeWheelchair => 'Sedia a rotelle';

  @override
  String get exerciseTypeRowing => 'Canottaggio';

  @override
  String get exerciseTypePaddling => 'Pagaiata';

  @override
  String get exerciseTypeSkiing => 'Sci';

  @override
  String get exerciseTypeSnowboarding => 'Snowboard';

  @override
  String get exerciseTypeSnowshoeing => 'Ciaspolata';

  @override
  String get exerciseTypeSkating => 'Pattinaggio';

  @override
  String get exerciseTypeSailing => 'Vela';

  @override
  String get exerciseTypeSurfing => 'Surf';

  @override
  String get exerciseTypeSwimmingOpenWater => 'Nuoto (acque libere)';

  @override
  String get exerciseTypeGolf => 'Golf';

  @override
  String get exerciseTypeStrengthTraining => 'Allenamento di forza';

  @override
  String get exerciseTypeTreadmill => 'Tapis roulant';

  @override
  String get exerciseTypePushUps => 'Piegamenti';

  @override
  String get exerciseTypePullUps => 'Trazioni';

  @override
  String get exerciseTypeRopeSkipping => 'Salto della corda';

  @override
  String get exerciseTypeTrampolineJumping => 'Salto sul trampolino';

  @override
  String get exerciseTypeOtherWorkout => 'Altro allenamento';

  @override
  String get mindfulnessEntryUnavailable =>
      'Le sessioni di consapevolezza non sono disponibili in questo provider Health Connect.';

  @override
  String get mindfulnessEntryTimerTitle => 'Timer';

  @override
  String get mindfulnessEntryManualTitle => 'Inserimento manuale';

  @override
  String get mindfulnessEntryIntervalBell => 'Campana a intervalli';

  @override
  String get mindfulnessEntryIntervalMinutes => 'Intervallo (min)';

  @override
  String get mindfulnessEntryBellSound => 'Suono campana';

  @override
  String get mindfulnessEntryBackgroundSound => 'Suono di sottofondo';

  @override
  String get mindfulnessBellStruck => 'Colpo morbido';

  @override
  String get mindfulnessBellRubbed => 'Campana calda';

  @override
  String get mindfulnessBellBright => 'Campana brillante';

  @override
  String get mindfulnessBellTemple => 'Campana tempio';

  @override
  String get mindfulnessBellHarmony => 'Armonia';

  @override
  String get mindfulnessBackgroundNone => 'Nessuno';

  @override
  String get mindfulnessBackgroundBowl => 'Campana';

  @override
  String get mindfulnessBackgroundMeditation => 'Meditazione';

  @override
  String get mindfulnessBackgroundChimes => 'Campanelli';

  @override
  String get mindfulnessBackgroundDreamscape => 'Paesaggio onirico';

  @override
  String get mindfulnessEntryStartTimer => 'Avvia';

  @override
  String get mindfulnessEntryStopTimer => 'Ferma';

  @override
  String get mindfulnessEntryResumeTimer => 'Riprendi';

  @override
  String get mindfulnessEntryDiscardTimer => 'Scarta';

  @override
  String get mindfulnessEntrySaveSession => 'Salva sessione';

  @override
  String get mindfulnessEntryMinutes => 'Minuti';

  @override
  String get mindfulnessEntryAddMinutes => 'Aggiungi minuti';

  @override
  String get mindfulnessEntryInvalidTimer =>
      'Inserisci una durata e un intervallo validi per il timer.';

  @override
  String get mindfulnessEntryInvalidManual =>
      'Inserisci minuti di consapevolezza validi.';

  @override
  String get mindfulnessEntryTimerTooShort =>
      'La meditazione deve durare almeno 1 minuto per essere salvata.';

  @override
  String mindfulnessEntryWriteFailed(String arg0) {
    return 'Impossibile salvare la sessione di consapevolezza: $arg0';
  }

  @override
  String get mindfulnessEntryCompleted => 'Timer completato';

  @override
  String get mindfulnessRemindersTitle => 'Promemoria consapevolezza';

  @override
  String get mindfulnessRemindersSummaryOff =>
      'Disattivato. Abilita un promemoria giornaliero per il tuo obiettivo di consapevolezza.';

  @override
  String mindfulnessRemindersSummaryOn(String arg0) {
    return 'Ogni giorno alle $arg0';
  }

  @override
  String get mindfulnessRemindersPermissionNeeded =>
      'Concedi il permesso per le notifiche per abilitare i promemoria di consapevolezza.';

  @override
  String get mindfulnessRemindersTime => 'Orario promemoria';

  @override
  String get mindfulnessRemindersGoalNote =>
      'I promemoria si mettono in pausa dopo aver raggiunto l\'obiettivo giornaliero di consapevolezza e riprendono il giorno seguente.';

  @override
  String get mindfulnessReminderNotificationChannel =>
      'Promemoria consapevolezza';

  @override
  String get mindfulnessReminderNotificationChannelDesc =>
      'Promemoria facoltativi per completare il tuo obiettivo giornaliero di consapevolezza.';

  @override
  String get mindfulnessReminderNotificationTitle =>
      'Promemoria consapevolezza';

  @override
  String mindfulnessReminderNotificationBody(String arg0) {
    return 'Il tuo obiettivo è $arg0 oggi. Prenditi una pausa consapevole quando puoi.';
  }

  @override
  String mindfulnessReminderNotificationProgress(String arg0, String arg1) {
    return '$arg0 / $arg1';
  }

  @override
  String bodyEntrySubtitle(String arg0) {
    return 'Le voci $arg0 vengono salvate direttamente in Health Connect.';
  }

  @override
  String bodyEntryPermissionNeeded(String arg0) {
    return 'Per aggiungere voci di $arg0, OpenVitals necessita del permesso di scrittura su Health Connect. L\'app non memorizzerà questi dati; le voci vengono salvate in Health Connect.';
  }

  @override
  String bodyEntryValueLabel(String arg0, String arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String bodyEntryAddSelected(String arg0) {
    return 'Aggiungi $arg0';
  }

  @override
  String get bodyEntryInvalidValue =>
      'Inserisci un valore valido per questa misurazione.';

  @override
  String bodyEntryWriteFailed(String arg0) {
    return 'Impossibile salvare la misurazione corporea: $arg0';
  }

  @override
  String get carbsEntrySubtitle =>
      'Le voci carboidrati vengono salvate direttamente in Health Connect.';

  @override
  String get carbsEntryPermissionNeeded =>
      'Per aggiungere voci carboidrati, OpenVitals necessita del permesso di scrittura su Health Connect. L\'app non memorizzerà questi dati; le voci vengono salvate in Health Connect.';

  @override
  String carbsEntryValueLabel(String arg0) {
    return 'Carboidrati ($arg0)';
  }

  @override
  String get carbsEntryAdd => 'Aggiungi carboidrati';

  @override
  String get carbsEntryInvalidValue =>
      'Inserisci una quantità valida di carboidrati.';

  @override
  String carbsEntryWriteFailed(String arg0) {
    return 'Impossibile salvare i carboidrati: $arg0';
  }

  @override
  String vitalsEntrySubtitle(String arg0) {
    return 'Le voci $arg0 vengono salvate direttamente in Health Connect.';
  }

  @override
  String vitalsEntryPermissionNeeded(String arg0) {
    return 'Per aggiungere voci di $arg0, OpenVitals necessita del permesso di scrittura su Health Connect. L\'app non memorizzerà questi dati; le voci vengono salvate in Health Connect.';
  }

  @override
  String vitalsEntryValueLabel(String arg0, String arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String get vitalsEntrySystolicLabel => 'Sistolica (mmHg)';

  @override
  String get vitalsEntryDiastolicLabel => 'Diastolica (mmHg)';

  @override
  String vitalsEntryAddSelected(String arg0) {
    return 'Aggiungi $arg0';
  }

  @override
  String get vitalsEntryInvalidValue =>
      'Inserisci un valore valido per questo parametro vitale.';

  @override
  String vitalsEntryWriteFailed(String arg0) {
    return 'Impossibile salvare il parametro vitale: $arg0';
  }

  @override
  String get rangeDay => 'Giorno';

  @override
  String get rangeWeek => 'Settimana';

  @override
  String get rangeMonth => 'Mese';

  @override
  String get rangeYear => 'Anno';

  @override
  String get periodToday => 'Oggi';

  @override
  String get periodYesterday => 'Ieri';

  @override
  String get periodThisWeek => 'Questa settimana';

  @override
  String periodWeekOf(String arg0) {
    return 'Settimana del $arg0';
  }

  @override
  String get periodThisMonth => 'Questo mese';

  @override
  String get periodThisYear => 'Quest\'anno';

  @override
  String get periodLast7Days => 'Ultimi 7 giorni';

  @override
  String get periodLast30Days => 'Ultimi 30 giorni';

  @override
  String get periodLast365Days => 'Ultimi 365 giorni';

  @override
  String get periodSelected => 'Periodo selezionato';

  @override
  String get metricSteps => 'Passi';

  @override
  String get metricDistance => 'Distanza';

  @override
  String get metricAveragePace => 'Ritmo medio';

  @override
  String get metricAverageSpeed => 'Velocità media';

  @override
  String get metricCaloriesBurned => 'Calorie totali bruciate';

  @override
  String get metricCaloriesOut => 'Calorie totali';

  @override
  String get metricCaloriesIn => 'Calorie assunte';

  @override
  String get metricFloorsClimbed => 'Piani saliti';

  @override
  String get metricActiveCalories => 'Calorie attive';

  @override
  String get metricElevation => 'Elevazione';

  @override
  String get metricElevationGained => 'Dislivello positivo';

  @override
  String get metricWheelchairPushes => 'Spinte sedia a rotelle';

  @override
  String get metricWorkout => 'Allenamento';

  @override
  String get metricSleep => 'Sonno';

  @override
  String get metricHydration => 'Idratazione';

  @override
  String get metricTotalHydration => 'Idratazione totale';

  @override
  String get metricHydrationTrend => 'Tendenza idratazione';

  @override
  String get metricLoggedDays => 'Giorni registrati';

  @override
  String get metricLatestWeight => 'Ultimo peso';

  @override
  String get metricBodyFat => 'Grasso corporeo';

  @override
  String get metricAvgHeartRate => 'FC media';

  @override
  String get metricAverageHeartRate => 'Frequenza cardiaca media';

  @override
  String get metricRestingHeartRate => 'Frequenza cardiaca a riposo';

  @override
  String get metricHrv => 'Variabilità frequenza cardiaca (HRV)';

  @override
  String get metricCardioLoad => 'Carico cardiovascolare';

  @override
  String get metricWeeklyCardioLoad => 'Carico cardiovascolare settimanale';

  @override
  String get metricEnergyBurned => 'Calorie totali';

  @override
  String get metricBloodPressure => 'Pressione sanguigna';

  @override
  String get metricSpo2 => 'SpO2';

  @override
  String get metricOxygenSaturation => 'Saturazione ossigeno';

  @override
  String get metricVo2Max => 'VO2 max';

  @override
  String get metricMindfulness => 'Consapevolezza';

  @override
  String get metricTotalMindfulness => 'Consapevolezza totale';

  @override
  String get metricCycle => 'Ciclo';

  @override
  String get metricCycleTracking => 'Tracciamento ciclo';

  @override
  String get metricPeriodDays => 'Giorni mestruazione';

  @override
  String get metricOvulationTests => 'Test di ovulazione';

  @override
  String get metricLatestBbt => 'Ultima TBC';

  @override
  String get metricWeight => 'Peso';

  @override
  String get metricHeight => 'Altezza';

  @override
  String get metricBmi => 'IMC';

  @override
  String get metricFfmi => 'FFMI';

  @override
  String get metricLeanMass => 'Massa magra';

  @override
  String get metricBmr => 'MBR';

  @override
  String get metricBoneMass => 'Massa ossea';

  @override
  String get metricBodyWaterMass => 'Massa idrica corporea';

  @override
  String get metricLatest => 'Ultimo';

  @override
  String get metricChange => 'Variazione';

  @override
  String get metricMacros => 'Macronutrienti';

  @override
  String get metricProtein => 'Proteine';

  @override
  String get metricCarbs => 'Carboidrati';

  @override
  String get metricFat => 'Grassi';

  @override
  String get metricDietaryFiber => 'Fibra alimentare';

  @override
  String get metricSugar => 'Zucchero';

  @override
  String get metricEnergyFromFat => 'Calorie dai grassi';

  @override
  String get metricMonounsaturatedFat => 'Grassi monoinsaturi';

  @override
  String get metricPolyunsaturatedFat => 'Grassi polinsaturi';

  @override
  String get metricSaturatedFat => 'Grassi saturi';

  @override
  String get metricTransFat => 'Grassi trans';

  @override
  String get metricUnsaturatedFat => 'Grassi insaturi';

  @override
  String get metricCholesterol => 'Colesterolo';

  @override
  String get metricBiotin => 'Biotina';

  @override
  String get metricFolate => 'Folato';

  @override
  String get metricFolicAcid => 'Acido folico';

  @override
  String get metricNiacin => 'Niacina';

  @override
  String get metricPantothenicAcid => 'Acido pantotenico';

  @override
  String get metricRiboflavin => 'Riboflavina';

  @override
  String get metricThiamin => 'Tiamina';

  @override
  String get metricVitaminA => 'Vitamina A';

  @override
  String get metricVitaminB12 => 'Vitamina B12';

  @override
  String get metricVitaminB6 => 'Vitamina B6';

  @override
  String get metricVitaminC => 'Vitamina C';

  @override
  String get metricVitaminD => 'Vitamina D';

  @override
  String get metricVitaminE => 'Vitamina E';

  @override
  String get metricVitaminK => 'Vitamina K';

  @override
  String get metricCalcium => 'Calcio';

  @override
  String get metricChloride => 'Cloruro';

  @override
  String get metricChromium => 'Cromo';

  @override
  String get metricCopper => 'Rame';

  @override
  String get metricIodine => 'Iodio';

  @override
  String get metricIron => 'Ferro';

  @override
  String get metricMagnesium => 'Magnesio';

  @override
  String get metricManganese => 'Manganese';

  @override
  String get metricMolybdenum => 'Molibdeno';

  @override
  String get metricPhosphorus => 'Fosforo';

  @override
  String get metricPotassium => 'Potassio';

  @override
  String get metricSelenium => 'Selenio';

  @override
  String get metricSodium => 'Sodio';

  @override
  String get metricZinc => 'Zinco';

  @override
  String get metricCaffeine => 'Caffeina';

  @override
  String get metricRespiratoryRate => 'Frequenza respiratoria';

  @override
  String get metricAvgRespiratoryRate => 'Frequenza respiratoria media';

  @override
  String get metricBodyTemp => 'Temp. corporea';

  @override
  String get metricBloodGlucose => 'Glucosio nel sangue';

  @override
  String get metricSkinTemperature => 'Temperatura cutanea';

  @override
  String get metricRecordedSpeed => 'Velocità registrata';

  @override
  String get metricAveragePower => 'Potenza media';

  @override
  String get metricStepsCadence => 'Cadenza passi';

  @override
  String get metricCyclingCadence => 'Cadenza pedalata';

  @override
  String get unitSteps => 'passi';

  @override
  String get unitReps => 'rip.';

  @override
  String get unitPushes => 'spinte';

  @override
  String get unitFloors => 'piani';

  @override
  String get unitDays => 'giorni';

  @override
  String get unitNights => 'notti';

  @override
  String get unitTests => 'test';

  @override
  String get unitTotal => 'totale';

  @override
  String get unitGrams => 'g';

  @override
  String get sectionActivities => 'Attività';

  @override
  String get sectionActivityTypeStats => 'Per tipo di attività';

  @override
  String activityTypeStatsActivityCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '# attività',
      one: '# attività',
    );
    return '$_temp0';
  }

  @override
  String get sectionPlannedWorkouts => 'Allenamenti pianificati';

  @override
  String get activitiesFilterActivityTypeLabel => 'Tipo di attività';

  @override
  String get activitiesFilterAll => 'Tutte le attività';

  @override
  String get activitiesKeyMetrics => 'Metriche chiave';

  @override
  String get recoverySleepScore => 'Punteggio sonno';

  @override
  String get recoverySleepDuration => 'Durata sonno';

  @override
  String get recoverySleepSchedule => 'Orario sonno';

  @override
  String get recoveryRemSleep => 'Sonno REM';

  @override
  String get recoveryDeepSleep => 'Sonno profondo';

  @override
  String get recoverySleepEfficiency => 'Efficienza sonno';

  @override
  String get sleepScoreConfidenceHigh => 'Alta attendibilità';

  @override
  String get sleepScoreConfidenceMedium => 'Media attendibilità';

  @override
  String get sleepScoreConfidenceLow => 'Bassa attendibilità';

  @override
  String get sleepScoreConfidenceNoData => 'Nessun dato';

  @override
  String get sleepScoreRatingExcellent => 'Eccellente';

  @override
  String get sleepScoreRatingGood => 'Buono';

  @override
  String get sleepScoreRatingFair => 'Sufficiente';

  @override
  String get sleepScoreRatingPoor => 'Scarso';

  @override
  String dashboardSleepScoreSubtitle(String arg0, String arg1) {
    return '$arg0 • $arg1';
  }

  @override
  String get sleepScoreCalculationTitle => 'Come viene calcolato';

  @override
  String get sleepScoreDayNumbersTitle => 'Valori di oggi';

  @override
  String get sleepScoreReferencesTitle => 'Riferimenti';

  @override
  String get sleepScoreCalculationSummary =>
      'OpenVitals valuta la qualità oggettiva del sonno in base a durata, efficienza, continuità e regolarità. Non diagnostica disturbi del sonno.';

  @override
  String get sleepScoreFormula =>
      'Punteggio sonno = durata 35 + efficienza 30 + continuità 20 + regolarità 15';

  @override
  String get sleepScoreFormulaBody =>
      'La durata dà punteggio pieno per 7-9 ore. L\'efficienza usa il tempo totale di sonno diviso per il tempo a letto. La continuità usa il risveglio dopo l\'addormentamento. La regolarità confronta il punto medio del sonno odierno con le notti precedenti.';

  @override
  String get sleepScoreComponentsBody =>
      'I dati delle fasi del sonno migliorano l\'attendibilità, ma il sonno REM e profondo non pesano molto perché le stime consumer delle fasi possono variare. Se manca la cronologia della regolarità, OpenVitals usa un valore neutro e riduce l\'attendibilità.';

  @override
  String get sleepScoreNotDiagnostic =>
      'Questo punteggio è una guida giornaliera basata sui dati di Health Connect, non una diagnosi né un consiglio terapeutico.';

  @override
  String get sleepScoreComponentDuration => 'Durata';

  @override
  String get sleepScoreComponentEfficiency => 'Efficienza';

  @override
  String get sleepScoreComponentContinuity => 'Continuità';

  @override
  String get sleepScoreComponentRegularity => 'Regolarità';

  @override
  String get sleepScoreTotalSleep => 'Sonno totale';

  @override
  String get sleepScoreTimeInBed => 'Tempo a letto';

  @override
  String get sleepScoreEfficiency => 'Efficienza';

  @override
  String get sleepScoreWaso => 'Risveglio dopo l\'addormentamento';

  @override
  String get sleepScoreRegularity => 'Differenza di orario';

  @override
  String get sleepScoreBaselineNights => 'Notti di riferimento';

  @override
  String get sleepScoreStageRecords => 'Registrazioni fasi';

  @override
  String get sleepScoreQualityNoData =>
      'Dati sonno insufficienti per un punteggio.';

  @override
  String get sleepScoreQualityStageAwake =>
      'Usa le fasi del sonno e le fasi di veglia da Health Connect.';

  @override
  String get sleepScoreQualityStageOnly =>
      'Usa le fasi del sonno; la continuità della veglia potrebbe essere stimata.';

  @override
  String get sleepScoreQualitySessionOnly =>
      'Usa solo i tempi della sessione di sonno; l\'attendibilità è limitata.';

  @override
  String get sleepScoreReferenceAasm => 'Durata del sonno adulto AASM';

  @override
  String get sleepScoreReferenceSleepHealth =>
      'Salute del sonno multidimensionale';

  @override
  String get sleepScoreReferenceEfficiency =>
      'Definizione efficienza del sonno';

  @override
  String get sleepScoreReferenceRegularity =>
      'Ricerca sulla regolarità del sonno';

  @override
  String get sleepEfficiencyConfidenceHigh => 'Alta attendibilità';

  @override
  String get sleepEfficiencyConfidenceLow => 'Bassa attendibilità';

  @override
  String get sleepEfficiencyConfidenceNoData => 'Nessun dato';

  @override
  String get sleepEfficiencyCalculationTitle => 'Come viene calcolato';

  @override
  String get sleepEfficiencyDayNumbersTitle => 'Valori di oggi';

  @override
  String get sleepEfficiencyReferencesTitle => 'Riferimenti';

  @override
  String get sleepEfficiencyCalculationSummary =>
      'L\'efficienza del sonno è la percentuale della finestra temporale di sonno effettivamente dormita. Valori più alti indicano generalmente meno tempo sveglio a letto.';

  @override
  String get sleepEfficiencyFormula =>
      'Efficienza sonno = tempo totale dormito / tempo a letto x 100';

  @override
  String get sleepEfficiencyFormulaBody =>
      'Il tempo totale dormito è la somma delle fasi del sonno di Health Connect quando disponibili. Il tempo a letto è la finestra dall\'inizio alla fine della sessione di sonno principale.';

  @override
  String get sleepEfficiencyDataBody =>
      'Quando le fasi del sonno mancano, Health Connect potrebbe fornire solo la durata della sessione. OpenVitals può comunque mostrare una stima, ma l\'attendibilità è bassa perché il tempo sveglio a letto potrebbe non essere rilevato.';

  @override
  String get sleepEfficiencyNotDiagnostic =>
      'L\'efficienza del sonno è un segnale di continuità del sonno, non una diagnosi. Valori persistentemente bassi possono essere utili da discutere con un medico.';

  @override
  String get sleepEfficiencyQualityNoData =>
      'Dati sonno insufficienti per l\'efficienza.';

  @override
  String get sleepEfficiencyQualityStageBased =>
      'Usa le fasi del sonno di Health Connect per il tempo totale dormito.';

  @override
  String get sleepEfficiencyQualitySessionOnly =>
      'Usa solo i tempi della sessione; il tempo sveglio potrebbe mancare.';

  @override
  String get sleepEfficiencyReferenceDefinition =>
      'Definizione efficienza del sonno';

  @override
  String get sleepEfficiencyReferenceDenominator =>
      'Ricerca sul denominatore dell\'efficienza del sonno';

  @override
  String get sleepEfficiencyReferenceMethods =>
      'Revisione dei metodi di valutazione del sonno';

  @override
  String get cardioLoadConfidenceHigh => 'Alta attendibilità';

  @override
  String get cardioLoadConfidenceMedium => 'Media attendibilità';

  @override
  String get cardioLoadConfidenceLow => 'Bassa attendibilità';

  @override
  String get cardioLoadConfidenceNoData => 'Nessun dato';

  @override
  String get cardioLoadCalculationTitle => 'Come viene calcolato';

  @override
  String get cardioLoadDayNumbersTitle => 'Valori di oggi';

  @override
  String get cardioLoadReferencesTitle => 'Riferimenti';

  @override
  String get cardioLoadCalculationSummary =>
      'OpenVitals usa il TRIMP basato sulla FC quando i dati di frequenza cardiaca sono disponibili, altrimenti ricade sul solo movimento quando la FC non è utilizzabile.';

  @override
  String get cardioLoadFormula =>
      'TRIMP = minuti x FCR x 0,64 x e^(1,92 x FCR)';

  @override
  String get cardioLoadFormulaBody =>
      'FCR è la riserva di frequenza cardiaca: (frequenza cardiaca - FC a riposo) / (FC massima - FC a riposo). OpenVitals somma questo valore sugli intervalli di frequenza cardiaca disponibili per la giornata.';

  @override
  String get cardioLoadMappingBody =>
      'Quando esistono attività registrate, i campioni di frequenza cardiaca vengono associati per timestamp alla finestra di inizio e fine di ciascuna attività. In assenza di finestre di attività, vengono contati solo gli intervalli di frequenza cardiaca elevata. Se la FC non è utilizzabile, il movimento e le calorie attive vengono mostrati come alternativa a bassa attendibilità.';

  @override
  String get cardioLoadMethod => 'Metodo';

  @override
  String get cardioLoadTrimpScore => 'Punteggio TRIMP';

  @override
  String get cardioLoadHrCoverage => 'Copertura FC';

  @override
  String get cardioLoadExpectedCoverage => 'Copertura attesa';

  @override
  String get cardioLoadRestingHr => 'FC a riposo';

  @override
  String get cardioLoadMaxHr => 'FC massima';

  @override
  String get cardioLoadHrSamples => 'Campioni FC';

  @override
  String get cardioLoadActivityWindows => 'Finestre attività';

  @override
  String get cardioLoadActivityMinutes => 'Minuti attività';

  @override
  String get cardioLoadMovementFallback => 'Alternativa movimento';

  @override
  String get cardioLoadMethodActivityWindows => 'TRIMP da FC attività';

  @override
  String get cardioLoadMethodElevatedHr => 'TRIMP da FC elevata';

  @override
  String get cardioLoadMethodMovementFallback => 'Alternativa movimento';

  @override
  String get cardioLoadMethodNoData => 'Dati insufficienti';

  @override
  String get cardioLoadCalibrationObservedResting => 'FC a riposo osservata';

  @override
  String get cardioLoadCalibrationEstimatedResting => 'FC a riposo stimata';

  @override
  String get cardioLoadCalibrationObservedMax => 'FC massima osservata';

  @override
  String get cardioLoadCalibrationEstimatedMax => 'FC massima stimata';

  @override
  String get cardioLoadReferenceBanister => 'Equazione TRIMP di Banister';

  @override
  String get cardioLoadReferenceTrainingLoad =>
      'Revisione monitoraggio carico allenamento';

  @override
  String get cardioLoadReferenceHealthConnect =>
      'Mappatura FC allenamento Health Connect';

  @override
  String get sectionSleepSessions => 'Sessioni di sonno';

  @override
  String get sectionWeight => 'Peso';

  @override
  String get sectionEntries => 'Voci';

  @override
  String get sectionMeals => 'Pasti';

  @override
  String get sectionSessions => 'Sessioni';

  @override
  String get sectionDailyBreakdown => 'Dettaglio giornaliero';

  @override
  String get sectionVitals => 'Parametri vitali';

  @override
  String get sectionHeart => 'Cuore';

  @override
  String get sectionCardiovascular => 'Cardiovascolare';

  @override
  String get sectionRespiratory => 'Respiratorio';

  @override
  String get sectionRespiratoryRateDailyBreakdown =>
      'Dettaglio giornaliero frequenza respiratoria';

  @override
  String get sectionVo2MaxHistory => 'Storico VO2 max';

  @override
  String get sectionDisplay => 'Visualizzazione';

  @override
  String get sectionPrivacy => 'Privacy';

  @override
  String get sectionCycleCalendar => 'Calendario ciclo';

  @override
  String get sectionBasalBodyTemperature => 'Temperatura basale corporea';

  @override
  String get sectionStatistics => 'Statistiche';

  @override
  String get sectionCalorieTrends => 'Tendenze calorie';

  @override
  String get sectionNutritionTrends => 'Tendenze nutrizione';

  @override
  String get sectionBodyTrends => 'Tendenze corporee';

  @override
  String get sectionCarbohydrates => 'Carboidrati';

  @override
  String get sectionFats => 'Grassi';

  @override
  String get sectionVitamins => 'Vitamine';

  @override
  String get sectionMinerals => 'Minerali';

  @override
  String get sectionOtherNutrients => 'Altri nutrienti';

  @override
  String summaryDailyAverage(String arg0) {
    return 'Media giornaliera $arg0';
  }

  @override
  String summaryDaysInRange(String arg0) {
    return '$arg0 giorni nell\'intervallo';
  }

  @override
  String summaryEntries(String arg0) {
    return '$arg0 voci';
  }

  @override
  String summaryReadings(String arg0) {
    return '$arg0 rilevazioni';
  }

  @override
  String summaryNights(String arg0) {
    return '$arg0 notti';
  }

  @override
  String summaryRecordedStages(String arg0) {
    return '$arg0 fasi registrate';
  }

  @override
  String get summaryAverage => 'Media';

  @override
  String summaryAvgValue(String arg0) {
    return 'Media $arg0';
  }

  @override
  String summaryAvgValueRange(String arg0, String arg1, String arg2) {
    return 'Media $arg0 · intervallo $arg1-$arg2';
  }

  @override
  String summaryValueAvg(String arg0) {
    return '$arg0 media';
  }

  @override
  String get summaryRange => 'Intervallo';

  @override
  String get summarySamples => 'Campioni';

  @override
  String summaryRecorded(String arg0, String arg1) {
    return '$arg0-$arg1 registrati';
  }

  @override
  String summaryRestingValue(String arg0) {
    return 'A riposo $arg0';
  }

  @override
  String summaryHrvValue(String arg0) {
    return 'HRV $arg0';
  }

  @override
  String summaryLastUpdate(String arg0) {
    return 'Ultimo aggiornamento $arg0';
  }

  @override
  String get summaryNow => 'Adesso';

  @override
  String summaryToday(String arg0) {
    return '$arg0 oggi';
  }

  @override
  String summaryOnDate(String arg0, String arg1) {
    return '$arg0 il $arg1';
  }

  @override
  String summaryEmptyToday(String arg0) {
    return 'Ancora nessun dato per $arg0 oggi.';
  }

  @override
  String summaryEmptyDay(String arg0) {
    return 'Nessun dato per $arg0 in questo giorno.';
  }

  @override
  String get summaryAcrossSelectedPeriod => 'Nel periodo selezionato';

  @override
  String summaryLatestTemperature(String arg0, String arg1) {
    return 'Ultima $arg0 · $arg1';
  }

  @override
  String summaryTemperatureRange(String arg0, String arg1, String arg2) {
    return 'Intervallo $arg0-$arg1 · $arg2 rilevazioni';
  }

  @override
  String get summarySleepEndingToday => 'Sonno terminato oggi';

  @override
  String summarySleepEndingOn(String arg0) {
    return 'Sonno terminato il $arg0';
  }

  @override
  String get statTotal => 'Totale';

  @override
  String get statTime => 'Tempo';

  @override
  String get statActiveDays => 'Giorni attivi';

  @override
  String get statAverage => 'Media';

  @override
  String get statLowest => 'Minimo';

  @override
  String get statHighest => 'Massimo';

  @override
  String get statReadings => 'Rilevazioni';

  @override
  String get statDailyAverage => 'Media giornaliera';

  @override
  String get caloriesStatActiveAverage => 'Media attiva';

  @override
  String get caloriesStatBmrReadings => 'Rilevazioni MBR';

  @override
  String get statAverageDuration => 'Durata media';

  @override
  String get statTotalIntake => 'Assunzione totale';

  @override
  String get statBestDay => 'Giorno migliore';

  @override
  String get statNightsLogged => 'Notti registrate';

  @override
  String get statLongestSleep => 'Sonno più lungo';

  @override
  String get statLongestWorkout => 'Allenamento più lungo';

  @override
  String get statAverageMovingPace => 'Passo medio in movimento';

  @override
  String get statFastestPace => 'Passo più veloce';

  @override
  String get statBestSpeed => 'Velocità migliore';

  @override
  String get statLongestSession => 'Sessione più lunga';

  @override
  String get statBbtReadings => 'Rilevazioni TBC';

  @override
  String get statGoalStreak => 'Serie obiettivo';

  @override
  String get statLongestGoalStreak => 'Serie più lunga';

  @override
  String get statGoalsMet => 'Obiettivi raggiunti';

  @override
  String get statSuccessRate => 'Tasso di successo';

  @override
  String get statAverageGap => 'Intervallo medio';

  @override
  String get statVsPreviousDay => 'Rispetto al giorno precedente';

  @override
  String get statVsPreviousWeek => 'Rispetto alla settimana precedente';

  @override
  String get statVsPreviousMonth => 'Rispetto al mese precedente';

  @override
  String get statVsPreviousYear => 'Rispetto all\'anno precedente';

  @override
  String get statBaseline => 'Riferimento';

  @override
  String get stat30DayBaseline => 'Media 30 giorni';

  @override
  String get stat60DayBaseline => 'Media 60 giorni';

  @override
  String get stat90DayBaseline => 'Media 90 giorni';

  @override
  String get statUsualRange => 'Intervallo abituale';

  @override
  String get statBaselineDeviation => 'Scostamento dal riferimento';

  @override
  String get baselineStatusUsual => 'Abituale';

  @override
  String get baselineStatusAbove => 'Superiore';

  @override
  String get baselineStatusBelow => 'Inferiore';

  @override
  String get baselineStatusUnusualHigh => 'Insolito alto';

  @override
  String get baselineStatusUnusualLow => 'Insolito basso';

  @override
  String get sectionMetricContext => 'Contesto';

  @override
  String get interpretationBpTitle => 'Categoria pressione sanguigna';

  @override
  String get interpretationBpNormal => 'Normale';

  @override
  String get interpretationBpElevated => 'Elevata';

  @override
  String get interpretationBpStage1 => 'Ipertensione di stadio 1';

  @override
  String get interpretationBpStage2 => 'Ipertensione di stadio 2';

  @override
  String get interpretationBpSevere => 'Riferimento intervallo critico';

  @override
  String interpretationBpBody(String arg0) {
    return 'Questa rilevazione rientra nell\'intervallo $arg0. Una singola lettura dell\'app non è una diagnosi.';
  }

  @override
  String get interpretationBpSevereBody =>
      'Questa rilevazione supera il riferimento dell\'intervallo critico. Rimisurarla; cercare assistenza medica urgente se sono presenti sintomi o il valore rimane molto elevato.';

  @override
  String get interpretationBpSource =>
      'Fonte: categorie della pressione sanguigna per adulti dell\'American Heart Association.';

  @override
  String get interpretationBmiTitle => 'Categoria IMC';

  @override
  String get interpretationBmiUnderweight => 'Sottopeso';

  @override
  String get interpretationBmiHealthy => 'Peso sano';

  @override
  String get interpretationBmiOverweight => 'Sovrappeso';

  @override
  String get interpretationBmiObesity1 => 'Obesità classe 1';

  @override
  String get interpretationBmiObesity2 => 'Obesità classe 2';

  @override
  String get interpretationBmiObesity3 => 'Obesità classe 3';

  @override
  String get interpretationBmiBody =>
      'Categoria di screening IMC per adulti; l\'IMC non misura la composizione corporea.';

  @override
  String get interpretationBmiSource =>
      'Fonte: categorie IMC per adulti del CDC.';

  @override
  String get interpretationFfmiTitle => 'Categoria FFMI';

  @override
  String get interpretationFfmiBelowAverage => 'Sotto la media';

  @override
  String get interpretationFfmiAverage => 'Nella media';

  @override
  String get interpretationFfmiAboveAverage => 'Sopra la media';

  @override
  String get interpretationFfmiExcellent => 'Eccellente';

  @override
  String get interpretationFfmiSuperior => 'Superiore';

  @override
  String get interpretationFfmiExceptional => 'Eccezionale';

  @override
  String get interpretationFfmiElite => 'Elite';

  @override
  String interpretationFfmiBody(String arg0, String arg1) {
    return 'FFMI $arg0; FFMI $arg1 aggiustato. Utilizza il tuo peso più recente, il grasso corporeo e l\'altezza.';
  }

  @override
  String get interpretationFfmiSource =>
      'Fonte: ffmicalculators.com indicativa aggiustata delle categorie FFMI.';

  @override
  String get interpretationSleepTitle => 'Obiettivo sonno';

  @override
  String get interpretationSleepBelow => 'Sotto l\'obiettivo';

  @override
  String get interpretationSleepNear => 'Vicino all\'obiettivo';

  @override
  String get interpretationSleepMet => 'Obiettivo raggiunto';

  @override
  String interpretationSleepBelowBody(String arg0) {
    return 'Il sonno medio è di $arg0 inferiore all\'obiettivo configurato.';
  }

  @override
  String interpretationSleepNearBody(String arg0, String arg1) {
    return 'Il sonno medio è vicino all\'obiettivo configurato: $arg0 rispetto a $arg1.';
  }

  @override
  String interpretationSleepMetBody(String arg0, String arg1) {
    return 'Il sonno medio soddisfa l\'obiettivo configurato: $arg0 rispetto a $arg1.';
  }

  @override
  String get interpretationSleepSource =>
      'Basato sull\'obiettivo di sonno configurato, non su una valutazione medica del sonno.';

  @override
  String get interpretationMacroTitle => 'Ripartizione macronutrienti';

  @override
  String get interpretationMacroWithin => 'Nella ripartizione di riferimento';

  @override
  String get interpretationMacroOutside =>
      'Fuori dalla ripartizione di riferimento';

  @override
  String interpretationMacroBody(String arg0, String arg1, String arg2) {
    return 'Proteine $arg0, carboidrati $arg1, grassi $arg2 delle calorie da macronutrienti registrate.';
  }

  @override
  String get interpretationMacroSource =>
      'Fonte: riferimento AMDR per adulti delle Accademie Nazionali; solo macronutrienti registrati.';

  @override
  String get interpretationWorkoutTitle => 'Progresso linee guida allenamento';

  @override
  String get interpretationWorkoutNone => 'Nessun minuto registrato';

  @override
  String get interpretationWorkoutBelow => 'Sotto il riferimento settimanale';

  @override
  String get interpretationWorkoutApproaching =>
      'Vicino al riferimento settimanale';

  @override
  String get interpretationWorkoutMet => 'Riferimento settimanale raggiunto';

  @override
  String interpretationWorkoutBody(String arg0, String arg1) {
    return 'Registrati $arg0 verso il riferimento di 150 min/settimana per adulti ($arg1). L\'intensità non è verificata.';
  }

  @override
  String interpretationWorkoutBodyWeeklyAverage(String arg0, String arg1) {
    return 'Media settimanale $arg0 verso il riferimento di 150 min/settimana per adulti ($arg1). L\'intensità non è verificata.';
  }

  @override
  String get interpretationWorkoutSource =>
      'Fonte: riferimento linee guida attività fisica per adulti dell\'HHS.';

  @override
  String get interpretationVitalTitle => 'Contesto parametri vitali';

  @override
  String get interpretationVitalWithin => 'Nel riferimento ampio per adulti';

  @override
  String get interpretationVitalBelow =>
      'Sotto il riferimento ampio per adulti';

  @override
  String get interpretationVitalAbove =>
      'Sopra il riferimento ampio per adulti';

  @override
  String get interpretationVitalOxygenBelowTypical =>
      'Sotto il tipico intervallo di ossigenazione';

  @override
  String get interpretationVitalOxygenLow => 'Riferimento ossigeno basso';

  @override
  String get interpretationVitalOxygenVeryLow =>
      'Riferimento ossigeno molto basso';

  @override
  String get interpretationVitalRestingHrBody =>
      'Solo riferimento ampio per adulti; forma fisica, farmaci, stress, malattia e momento della rilevazione possono influire su ciò che è normale per te.';

  @override
  String get interpretationVitalRespiratoryBody =>
      'Solo riferimento ampio per adulti; attività, ansia, malattia e momento della rilevazione possono influire sulla frequenza respiratoria.';

  @override
  String get interpretationVitalTemperatureBody =>
      'La temperatura varia in base al sito di misurazione e all\'ora del giorno; usare questo dato solo come contesto.';

  @override
  String get interpretationVitalOxygenBody =>
      'Le rilevazioni del pulsossimetro possono essere influenzate dal dispositivo, dalla pelle, dalla circolazione, dal movimento e da condizioni fisiche.';

  @override
  String get interpretationVitalSource =>
      'Fonte: riferimento parametri vitali per adulti di MedlinePlus.';

  @override
  String get interpretationOxygenSource =>
      'Fonte: contesto pulsossimetro di MedlinePlus e FDA.';

  @override
  String get sectionCrossMetricInsights => 'Correlazioni tra metriche';

  @override
  String get crossMetricPositiveLink => 'Correlazione positiva';

  @override
  String get crossMetricNegativeLink => 'Correlazione negativa';

  @override
  String get crossMetricWeakLink => 'Correlazione debole';

  @override
  String crossMetricCorrelation(String arg0) {
    return '$arg0';
  }

  @override
  String crossMetricPairedDays(int arg0) {
    return '$arg0 giorni abbinati';
  }

  @override
  String get crossSleepHrvTitle => 'Sonno vs HRV';

  @override
  String get crossSleepHrvPositive =>
      'Più sonno tende ad allinearsi con un HRV più alto in questo periodo.';

  @override
  String get crossSleepHrvNegative =>
      'Più sonno tende ad allinearsi con un HRV più basso in questo periodo.';

  @override
  String get crossSleepHrvNeutral =>
      'Sonno e HRV non mostrano un chiaro schema in questo periodo.';

  @override
  String get crossWorkoutRestingHrTitle =>
      'Allenamenti vs frequenza cardiaca a riposo';

  @override
  String get crossWorkoutRestingHrPositive =>
      'Più minuti di allenamento tendono ad allinearsi con una frequenza cardiaca a riposo più alta in questo periodo.';

  @override
  String get crossWorkoutRestingHrNegative =>
      'Più minuti di allenamento tendono ad allinearsi con una frequenza cardiaca a riposo più bassa in questo periodo.';

  @override
  String get crossWorkoutRestingHrNeutral =>
      'I minuti di allenamento e la frequenza cardiaca a riposo non mostrano un chiaro schema in questo periodo.';

  @override
  String get crossHydrationWeightTitle => 'Idratazione vs variazione peso';

  @override
  String get crossHydrationWeightPositive =>
      'Maggiore idratazione tende ad allinearsi con oscillazioni di peso più ampie in questo periodo.';

  @override
  String get crossHydrationWeightNegative =>
      'Maggiore idratazione tende ad allinearsi con oscillazioni di peso più ridotte in questo periodo.';

  @override
  String get crossHydrationWeightNeutral =>
      'Idratazione e variazione di peso non mostrano un chiaro schema in questo periodo.';

  @override
  String get crossMindfulnessSleepTitle => 'Consapevolezza vs sonno';

  @override
  String get crossMindfulnessSleepPositive =>
      'Più minuti di consapevolezza tendono ad allinearsi con un sonno più lungo in questo periodo.';

  @override
  String get crossMindfulnessSleepNegative =>
      'Più minuti di consapevolezza tendono ad allinearsi con un sonno più breve in questo periodo.';

  @override
  String get crossMindfulnessSleepNeutral =>
      'Consapevolezza e sonno non mostrano un chiaro schema in questo periodo.';

  @override
  String get legendLess => 'Meno';

  @override
  String get legendMore => 'Più';

  @override
  String get dailyGoal => 'Obiettivo giornaliero';

  @override
  String goalProgress(int arg0, int arg1) {
    return '$arg0 di $arg1 giorni tracciati raggiunti';
  }

  @override
  String get cdDecreaseDailyGoal => 'Diminuisci obiettivo giornaliero';

  @override
  String get cdIncreaseDailyGoal => 'Aumenta obiettivo giornaliero';

  @override
  String get hydrationDailyGoal => 'Obiettivo giornaliero';

  @override
  String hydrationGoalProgress(int arg0, int arg1) {
    return '$arg0 di $arg1 giorni tracciati raggiunti';
  }

  @override
  String get hydrationRemindersTitle => 'Promemoria idratazione';

  @override
  String get hydrationRemindersSummaryOff =>
      'Disattivato. Abilita i promemoria nelle ore attive fino al raggiungimento dell\'obiettivo idrico giornaliero.';

  @override
  String hydrationRemindersSummaryOn(int arg0, String arg1, String arg2) {
    return 'Ogni $arg0 min • $arg1-$arg2';
  }

  @override
  String get hydrationRemindersPermissionNeeded =>
      'Concedi il permesso per le notifiche per abilitare i promemoria di idratazione.';

  @override
  String get hydrationRemindersInterval => 'Intervallo promemoria';

  @override
  String hydrationRemindersIntervalValue(int arg0) {
    return 'Ogni $arg0 min';
  }

  @override
  String get hydrationRemindersActiveStart => 'Attivo dalle';

  @override
  String get hydrationRemindersActiveEnd => 'Attivo fino alle';

  @override
  String get hydrationRemindersGoalNote =>
      'I promemoria si mettono in pausa dopo aver raggiunto l\'obiettivo odierno e riprendono il giorno seguente.';

  @override
  String get hydrationReminderNotificationChannel => 'Promemoria idratazione';

  @override
  String get hydrationReminderNotificationChannelDesc =>
      'Promemoria facoltativi per registrare l\'idratazione nelle ore attive.';

  @override
  String get hydrationReminderNotificationTitle => 'Promemoria idratazione';

  @override
  String hydrationReminderNotificationBody(String arg0, String arg1) {
    return 'Sei a $arg0 di $arg1 oggi. Aggiungi una bevanda quando puoi.';
  }

  @override
  String hydrationReminderNotificationProgress(String arg0, String arg1) {
    return '$arg0 / $arg1';
  }

  @override
  String get hydrationTrackerTitle => 'Registra idratazione';

  @override
  String get hydrationTrackerSubtitle =>
      'Salvata direttamente in Health Connect';

  @override
  String get hydrationTrackerPermissionNeeded =>
      'Per il riepilogo, OpenVitals chiede solo i permessi di visualizzazione. Per aggiungere questa voce manuale, abbiamo bisogno del permesso di scrittura. OpenVitals non memorizzerà questi dati; le voci vengono salvate in Health Connect.';

  @override
  String get hydrationNutritionPermissionNeeded =>
      'Concedi l\'autorizzazione di scrittura nutrizione per salvare i nutrienti delle bevande in Health Connect.';

  @override
  String get hydrationCustomDrinksTitle => 'Bevande salvate';

  @override
  String get hydrationCatalogDrinksTitle => 'Catalogo bevande';

  @override
  String get hydrationCatalogSearch => 'Cerca bevande';

  @override
  String get hydrationCatalogFrequentlyConsumed => 'Bevande frequenti';

  @override
  String get hydrationCatalogSavedOutside => 'Bevande salvate';

  @override
  String get hydrationCatalogSectionWater => 'Acqua';

  @override
  String get hydrationCatalogSectionCoffees => 'Caffè';

  @override
  String get hydrationCatalogSectionEnergyDrinks => 'Bevande energetiche';

  @override
  String get hydrationCatalogSectionTeas => 'Tè';

  @override
  String get hydrationCatalogSectionChocolateDrinks => 'Bevande al cioccolato';

  @override
  String get hydrationCatalogSectionCarbonatedSoftDrinks => 'Bibite gassate';

  @override
  String get hydrationCatalogSectionOtherDrinks => 'Altre bevande';

  @override
  String hydrationCatalogSectionCount(int arg0) {
    return '$arg0 bevande';
  }

  @override
  String get hydrationNewDrinkAction => 'Nuova bevanda';

  @override
  String get hydrationNewDrinkTitle => 'Nuova bevanda';

  @override
  String get hydrationEditDrinkTitle => 'Modifica bevanda';

  @override
  String hydrationLogSavedDrinkTitle(String arg0) {
    return 'Registra $arg0';
  }

  @override
  String get hydrationCustomDrinkName => 'Nome';

  @override
  String get hydrationCustomDrinkCategory => 'Categoria';

  @override
  String get hydrationCustomDrinkNoCategory => 'Nessuna categoria';

  @override
  String get hydrationCustomDrinkHydrationImpact => 'Impatto sull\'idratazione';

  @override
  String get hydrationImpactCountsFully => 'Conta completamente';

  @override
  String get hydrationImpactCountsPartially => 'Conta parzialmente';

  @override
  String get hydrationImpactDoesNotCount => 'Non conta';

  @override
  String get hydrationImpactCountsFullyBody =>
      'Tutto il volume della bevanda conta per l\'idratazione.';

  @override
  String get hydrationImpactCountsPartiallyBody =>
      'Usa una percentuale di questa bevanda.';

  @override
  String get hydrationImpactDoesNotCountBody =>
      'Salvala senza aggiungere idratazione.';

  @override
  String get hydrationImpactPercentLabel => 'Conta come idratazione (%)';

  @override
  String get hydrationImpactInvalidPercent =>
      'Inserisci una percentuale maggiore di 0 e minore di 100.';

  @override
  String get hydrationCustomDrinkNutrients => 'Nutrienti';

  @override
  String get hydrationCustomDrinkAddNutrient => 'Aggiungi nutriente';

  @override
  String get hydrationCustomDrinkLiquidOnly => 'Solo liquido';

  @override
  String hydrationCustomDrinkNutrientCount(int arg0) {
    return 'Nutrienti: $arg0';
  }

  @override
  String hydrationSavedDrinkAmountNoHydration(String arg0) {
    return '$arg0 • Non conta come idratazione';
  }

  @override
  String hydrationSavedDrinkAmountPartialHydration(String arg0, int arg1) {
    return '$arg0 • Conta al $arg1% come idratazione';
  }

  @override
  String get hydrationNonHydratingDrinkSavedHint =>
      'Salvata solo come nutrizione. Non è stata aggiunta idratazione.';

  @override
  String get hydrationEntryNutritionOnly => 'Solo nutrizione';

  @override
  String get hydrationEntryNoHydration => 'Nessuna idratazione';

  @override
  String get hydrationCustomDrinkAmountGrams => 'Quantità (g)';

  @override
  String get hydrationCustomDrinkAmountKcal => 'Quantità (kcal)';

  @override
  String get hydrationCustomDrinkInvalid =>
      'Inserisci un nome bevanda, una quantità e quantità positive di nutrienti.';

  @override
  String get hydrationInvalidAmount =>
      'Inserisci una quantità maggiore di zero e non superiore a 100 L.';

  @override
  String hydrationDrinkAmountLabel(String arg0) {
    return 'Quantità ($arg0)';
  }

  @override
  String hydrationDrinkInvalidAmountRange(String arg0, String arg1) {
    return 'Inserisci una quantità da $arg0 a $arg1.';
  }

  @override
  String hydrationWriteFailed(String arg0) {
    return 'Impossibile salvare la voce di idratazione: $arg0';
  }

  @override
  String get cdDecreaseHydrationGoal => 'Diminuisci obiettivo idratazione';

  @override
  String get cdIncreaseHydrationGoal => 'Aumenta obiettivo idratazione';

  @override
  String get cdDecreaseHydrationReminderInterval =>
      'Diminuisci intervallo promemoria idratazione';

  @override
  String get cdIncreaseHydrationReminderInterval =>
      'Aumenta intervallo promemoria idratazione';

  @override
  String get unitPercentSymbol => '%';

  @override
  String get messageNoDashboardData => 'Nessun dato di riepilogo disponibile.';

  @override
  String get messageMissingPermissionsTitle => 'Alcune autorizzazioni mancanti';

  @override
  String get messageMissingPermissionsBody =>
      'Concedi le autorizzazioni mancanti per visualizzare un riepilogo completo.';

  @override
  String messageHealthConnectRateLimited(int arg0) {
    return 'Limite di richieste Health Connect raggiunto. Attendi circa $arg0 min e riprova.';
  }

  @override
  String get messageNoWorkoutsDay =>
      'Nessun allenamento registrato in questo giorno.';

  @override
  String get messageNoSleepDay =>
      'Nessuna sessione di sonno terminata in questo giorno.';

  @override
  String get messageNoBloodPressure =>
      'Nessuna rilevazione della pressione sanguigna.';

  @override
  String get messageNoOxygen => 'Nessuna rilevazione dell\'ossigeno.';

  @override
  String get messageNoVo2Max => 'Nessuna rilevazione del VO2 max.';

  @override
  String get messageNoBloodGlucose =>
      'Nessuna rilevazione del glucosio nel sangue.';

  @override
  String get messageNoSkinTemperature =>
      'Nessuna rilevazione della temperatura cutanea.';

  @override
  String get messageCycleBrowse => 'Visualizza calendario ciclo e rilevazioni.';

  @override
  String get dashboardAddWidgets => 'Aggiungi widget';

  @override
  String get dashboardAllWidgetsAdded =>
      'Tutti i widget sono già nel riepilogo.';

  @override
  String get dashboardActionLog => 'Registra';

  @override
  String get dashboardActionStartWorkout => 'Inizia allenamento';

  @override
  String get dashboardActivitiesToday => 'Attività';

  @override
  String get dashboardSensorStatusTitle => 'Batteria sensori';

  @override
  String dashboardSensorBatteryLowest(int arg0) {
    return '$arg0% minimo';
  }

  @override
  String get dashboardSensorBatteryUnknown => 'Batteria non ancora disponibile';

  @override
  String dashboardSensorStatusActiveConnected(int arg0, int arg1) {
    return '$arg0 attivi • $arg1 connessi';
  }

  @override
  String get dashboardSensorStatusAllDisabled => 'Tutti i sensori disabilitati';

  @override
  String get dashboardDeleteActivityTitle => 'Eliminare l\'attività?';

  @override
  String dashboardDeleteActivityMessage(String arg0) {
    return 'Eliminare questa attività $arg0 da OpenVitals?';
  }

  @override
  String get dashboardReadinessTitle => 'Prontezza Giornaliera';

  @override
  String get dashboardReadinessScore => 'Prontezza';

  @override
  String get dashboardReadinessBodyEnergy => 'Energia Corporea';

  @override
  String get dashboardReadinessTraining => 'Prontezza Allenamento';

  @override
  String get dashboardReadinessHrvStatus => 'Stato HRV';

  @override
  String get dashboardReadinessIntensityMinutes => 'Minuti di intensità';

  @override
  String get dashboardReadinessStressLevel => 'Livello di stress';

  @override
  String get dashboardReadinessRecommended => 'Consigliato';

  @override
  String get dashboardReadinessAvoid => 'Da evitare';

  @override
  String get dashboardReadinessAlternative => 'Alternativa';

  @override
  String get dashboardReadinessStrain => 'Obiettivo di sforzo';

  @override
  String get dashboardReadinessGoal => 'Obiettivo adattivo';

  @override
  String get dashboardReadinessRecoveryMode => 'Modalità recupero';

  @override
  String get dashboardReadinessRecoveryModeBody =>
      'Gli obiettivi di attività sono ridotti per concentrarsi sul riposo.';

  @override
  String get dashboardReadinessWhy => 'Perché questo consiglio';

  @override
  String get stressDetailsHowTracked => 'Come viene rilevato';

  @override
  String get stressDetailsHowTrackedBody =>
      'OpenVitals stima lo stress fisiologico localmente confrontando l\'HRV con il riferimento personale, la frequenza cardiaca a riposo con il riferimento e la frequenza cardiaca media rispetto a quella a riposo. È una stima dello sforzo, non una diagnosi né un rilevatore di stress mentale.';

  @override
  String get stressDetailsScale =>
      'Scala: 0-25 riposo, 26-50 basso, 51-75 medio, 76-100 alto.';

  @override
  String get stressDetailsInputs => 'Input utilizzati';

  @override
  String get stressDetailsNoInputs =>
      'Nessun segnale HRV o di riferimento della frequenza cardiaca utilizzabile era disponibile.';

  @override
  String get stressDetailsDataCoverage => 'Copertura dati';

  @override
  String get stressDetailsNoDataCoverage =>
      'Nessuna copertura di campioni FC o HRV nella giornata disponibile.';

  @override
  String get stressDetailsCaveats => 'Avvertenze';

  @override
  String get stressDetailsRelaxationPrompt =>
      'Se questo sembra accurato, prova una breve sessione di respirazione o consapevolezza e ricontrolla dopo un periodo di quiete.';

  @override
  String get readinessDetailsHowCalculated => 'Come viene calcolato';

  @override
  String get readinessDetailsSignalsUsed => 'Segnali utilizzati';

  @override
  String get readinessDetailsGuidance => 'Cosa significa';

  @override
  String get readinessDetailsCaveats => 'Avvertenze';

  @override
  String get readinessDetailsCaveatLocal =>
      'Si tratta di una stima locale basata su regole dai dati attualmente disponibili in OpenVitals.';

  @override
  String get readinessDetailsCaveatNotMedical =>
      'Non è una diagnosi, un consiglio medico, una guida all\'allenamento né una previsione di infortuni.';

  @override
  String get readinessDetailsCaveatMissingData =>
      'Autorizzazioni mancanti, campioni scarsi o riferimenti assenti riducono l\'attendibilità.';

  @override
  String get readinessDetailsScoreStrong => 'Elevata';

  @override
  String get readinessDetailsScoreSteady => 'Stabile';

  @override
  String get readinessDetailsScoreLimited => 'Limitata';

  @override
  String get readinessDetailsScoreLow => 'Bassa';

  @override
  String get readinessDetailsScoreNeedsMoreData => 'Servono più dati';

  @override
  String get bodyEnergyDetailsHowCalculatedBody =>
      'L\'Energia Corporea usa segnali di recupero: punteggio sonno, stato HRV, frequenza cardiaca a riposo, stress fisiologico, temperatura, idratazione, nutrizione e consapevolezza. Stima la capacità di recupero visibile oggi.';

  @override
  String get bodyEnergyDetailsScale =>
      'Scala: 80-100 elevata, 60-79 stabile, 40-59 limitata, 0-39 bassa.';

  @override
  String get bodyEnergyDetailsSummary =>
      'Un punteggio di recupero che indica quanta energia i segnali corporei attuali supportano oggi.';

  @override
  String get bodyEnergyDetailsNoSignals =>
      'Nessun segnale di recupero utilizzabile era disponibile.';

  @override
  String get trainingReadinessDetailsHowCalculatedBody =>
      'La Prontezza Allenamento usa segnali legati all\'allenamento: sonno, stato HRV, frequenza cardiaca a riposo, carico di allenamento, minuti di intensità, stress fisiologico, temperatura e contesto attività. Stima se un allenamento più intenso è appropriato oggi.';

  @override
  String get trainingReadinessDetailsScale =>
      'Scala: 80-100 pronto per allenamento intenso, 60-79 allenamento controllato, 40-59 allenamento leggero, 0-39 focalizzato sul riposo.';

  @override
  String get trainingReadinessDetailsSummary =>
      'Un punteggio legato all\'allenamento che indica quanto i segnali di recupero e carico attuali supportano l\'intensità dell\'esercizio.';

  @override
  String get trainingReadinessDetailsNoSignals =>
      'Nessun segnale legato all\'allenamento utilizzabile era disponibile.';

  @override
  String dashboardGoalOf(String arg0) {
    return 'di $arg0';
  }

  @override
  String get caloriesEstimatedActiveBmr =>
      'Nessun totale registrato, stima attivo + MBR';

  @override
  String caloriesEstimatedValue(String arg0) {
    return 'Stima $arg0';
  }

  @override
  String dashboardWeeklyCardioLoadProgress(int arg0, int arg1) {
    return '$arg0 di $arg1';
  }

  @override
  String dashboardCardioLoadPercentOnly(int arg0) {
    return '$arg0%';
  }

  @override
  String dashboardCardioLoadPercent(int arg0) {
    return '$arg0% carico';
  }

  @override
  String dashboardCardioLoadTodayDelta(int arg0) {
    return '+$arg0% oggi';
  }

  @override
  String get messageNoActivitiesPeriod =>
      'Nessuna attività nel periodo selezionato.';

  @override
  String get plannedWorkoutCompleted => 'Completato';

  @override
  String plannedWorkoutBlocks(int arg0) {
    return '$arg0 blocchi';
  }

  @override
  String get messageNoStepUpdates =>
      'Nessun aggiornamento dei passi registrato';

  @override
  String get messageNoDistanceUpdates =>
      'Nessun aggiornamento della distanza registrato';

  @override
  String get messageNoCaloriesBurned =>
      'Nessun dato sulle calorie totali registrato';

  @override
  String get messageNoFloorsClimbed =>
      'Nessun dato sui piani saliti registrato';

  @override
  String get messageNoActiveCalories =>
      'Nessun dato sulle calorie attive registrato';

  @override
  String get messageNoCalorieDataPeriod =>
      'Nessun dato su calorie totali, attive o MBR in questo periodo.';

  @override
  String get messageNoElevation => 'Nessun dato sull\'elevazione registrato';

  @override
  String get messageNoWheelchairPushes =>
      'Nessun dato sulle spinte della sedia a rotelle registrato';

  @override
  String get messageNoSleepDaySelected =>
      'Nessun dato sul sonno per il giorno selezionato.';

  @override
  String get messageNoSleepPeriod =>
      'Nessun dato sul sonno nel periodo selezionato.';

  @override
  String get messageNoHeartPeriod =>
      'Nessun dato sulla frequenza cardiaca nel periodo selezionato.\n\nVerificare che il permesso per la frequenza cardiaca sia concesso e che un dispositivo connesso abbia sincronizzato i dati.';

  @override
  String get messageNoHeartSamplesDay =>
      'Nessun campione di frequenza cardiaca registrato in questo giorno.';

  @override
  String get messageHeartEmptyHint =>
      'Prova un\'altra data o verifica che un dispositivo connesso abbia sincronizzato dati puntuali di frequenza cardiaca.';

  @override
  String get messageNoWeightPeriod =>
      'Nessun dato sul peso nel periodo selezionato.\n\nSincronizza una bilancia o un indossabile che riporti il peso su Health Connect.';

  @override
  String get messageNoHydrationPeriod =>
      'Nessuna voce di idratazione registrata per questo periodo.';

  @override
  String get messageNoHydrationAddedPeriod =>
      'Nessuna idratazione aggiunta per questo periodo.';

  @override
  String get messageNoNutritionPeriod =>
      'Nessuna voce nutrizionale registrata per questo periodo.';

  @override
  String get messageNoMindfulnessPeriod =>
      'Nessuna sessione di consapevolezza registrata per questo periodo.';

  @override
  String get messageNoVitalsPeriod =>
      'Nessun parametro vitale registrato per questo periodo.';

  @override
  String get messageNoReadingsPeriod =>
      'Nessuna rilevazione in questo periodo.';

  @override
  String get messageNoCyclePeriod =>
      'Nessun dato del ciclo registrato per questo periodo.';

  @override
  String get messageNoSegments => 'Nessun segmento registrato.';

  @override
  String get messageNoLaps => 'Nessun giro registrato.';

  @override
  String get messageNoRoutePoints => 'Nessun punto del percorso registrato.';

  @override
  String get messageRouteConsentRequired =>
      'I dati del percorso sono disponibili, ma l\'accesso al percorso non è stato ancora autorizzato. Apri le autorizzazioni di Health Connect dalle Impostazioni per abilitare le anteprime del percorso.';

  @override
  String get messageNoRouteData => 'Nessun dato del percorso registrato.';

  @override
  String get messageNoStages => 'Nessuna fase registrata.';

  @override
  String get messageNoKcal => 'Nessuna kcal';

  @override
  String get onboardingTagline => 'I tuoi dati di salute, sul tuo dispositivo';

  @override
  String get onboardingPrivacyTitle => 'Privacy prima di tutto';

  @override
  String get onboardingPrivacyBody =>
      'Nessun account richiesto. I dati rimangono sul dispositivo. Nessun caricamento in cloud, nessuna analisi, nessuna pubblicità.';

  @override
  String get healthDisclaimerTitle => 'Avviso sulla salute';

  @override
  String get healthDisclaimerBody =>
      'OpenVitals è destinato esclusivamente al benessere generale e a scopo informativo. Non è un dispositivo medico e non fornisce consigli medici. Non diagnostica, tratta, cura né previene alcuna malattia o condizione medica. Consultare sempre un professionista sanitario qualificato per consigli medici, diagnosi o trattamenti.';

  @override
  String get onboardingHealthConnectTitle => 'Basato su Health Connect';

  @override
  String get onboardingHealthConnectBody =>
      'Legge dall\'archivio sanitario Android sicuro sul dispositivo e salva le voci create in Health Connect. Compatibile con tutti i dati importati in Health Connect.';

  @override
  String get onboardingPermissionsHeader => 'AUTORIZZAZIONI HEALTH CONNECT';

  @override
  String get onboardingGrantCore =>
      'Concedi le autorizzazioni richieste di Health Connect';

  @override
  String get onboardingGrantAll =>
      'Concedi le autorizzazioni richieste di Health Connect';

  @override
  String get onboardingGrantRemaining =>
      'Concedi le autorizzazioni rimanenti disponibili';

  @override
  String get onboardingOpenRequiredPermissions =>
      'Apri le autorizzazioni richieste di Health Connect';

  @override
  String get onboardingUnableOpenPermissions =>
      'Impossibile aprire le autorizzazioni di Health Connect.';

  @override
  String get onboardingHealthConnectNotSupported =>
      'Health Connect non è supportato su questo dispositivo.';

  @override
  String get onboardingHealthConnectNeedsPlayStore =>
      'Questo dispositivo esegue Android 13 con l\'app Health Connect autonoma installata. Purtroppo questa versione dipende dai servizi Google Play Store, assenti su questo dispositivo, quindi Health Connect rifiuta le richieste prima che OpenVitals possa leggere i dati. OpenVitals non può risolvere né aggirare questo problema di Health Connect lato dispositivo. L\'unico modo per risolverlo è installare i servizi Google Play o aggiornare ad Android 14 o versione successiva, dove Health Connect è integrato nel sistema operativo e non richiede i servizi Google.';

  @override
  String get onboardingHealthConnectUpdate =>
      'Health Connect deve essere installato o aggiornato per utilizzare questa app.';

  @override
  String get onboardingInstallHealthConnect => 'Installa Health Connect';

  @override
  String get onboardingStatusNotSupported => 'Non supportato';

  @override
  String get onboardingStatusGranted => 'Concesso';

  @override
  String onboardingStatusPartiallyGranted(int arg0, int arg1) {
    return '$arg0/$arg1 concesse';
  }

  @override
  String get onboardingStatusManual => 'Apri impostazioni';

  @override
  String get onboardingStatusRequired => 'Obbligatorio';

  @override
  String get onboardingStatusOptional => 'Facoltativo';

  @override
  String get onboardingCategoryActivitySleep => 'Attività & sonno';

  @override
  String get onboardingCategoryActivitySleepDesc =>
      'Health Connect richiederà:\n* Passi\n* Distanza\n* Esercizio\n* Sonno';

  @override
  String get onboardingCategoryHeartRecovery => 'Cuore & parametri vitali';

  @override
  String get onboardingCategoryHeartRecoveryDesc =>
      'Health Connect richiederà:\n* Frequenza cardiaca\n* Frequenza cardiaca a riposo\n* Variabilità della frequenza cardiaca';

  @override
  String get onboardingCategoryBody => 'Corpo';

  @override
  String get onboardingCategoryBodyDesc =>
      'Health Connect richiederà:\n* Peso\n* Altezza\n* Grasso corporeo\n* Massa magra\n* Metabolismo basale\n* Massa ossea\n* Massa idrica corporea';

  @override
  String get onboardingCategoryActivityExtras => 'Extra attività';

  @override
  String get onboardingCategoryActivityExtrasDesc =>
      'Health Connect richiederà:\n* Calorie totali bruciate\n* Calorie attive bruciate\n* Piani saliti\n* Dislivello positivo\n* Spinte sedia a rotelle\n* Velocità\n* Potenza\n* Cadenza passi\n* Cadenza pedalata\n* Esercizio pianificato, se supportato';

  @override
  String get onboardingCategoryNutritionHydration => 'Nutrizione & idratazione';

  @override
  String get onboardingCategoryNutritionHydrationDesc =>
      'Health Connect richiederà:\n* Idratazione\n* Nutrizione';

  @override
  String get onboardingCategoryManualEntryWrite =>
      'Accesso scrittura inserimento manuale';

  @override
  String get onboardingCategoryManualEntryWriteDesc =>
      'Health Connect richiederà accesso in scrittura per:\n* Esercizio\n* Distanza\n* Dislivello positivo\n* Calorie attive bruciate\n* Calorie totali bruciate\n* Percorso esercizio\n* Idratazione\n* Peso\n* Altezza\n* Grasso corporeo\n* Pressione sanguigna\n* Saturazione ossigeno\n* Frequenza respiratoria\n* Temperatura corporea\n* Consapevolezza, se supportata';

  @override
  String get onboardingCategoryDataImportWrite =>
      'Accesso scrittura importazione dati';

  @override
  String get onboardingCategoryDataImportWriteDesc =>
      'Health Connect richiederà accesso in scrittura per i record importati:\n* Attività, esercizio, calorie e distanza\n* Frequenza cardiaca, FC a riposo e HRV\n* Misurazioni corporee\n* Idratazione e nutrizione\n* Sonno\n* Parametri vitali\n* Consapevolezza, se supportata\n* Record tracciamento ciclo';

  @override
  String get onboardingCategoryMindfulness => 'Consapevolezza';

  @override
  String get onboardingCategoryMindfulnessDesc =>
      'Health Connect richiederà:\n* Sessioni di consapevolezza';

  @override
  String get onboardingCategoryMindfulnessUnavailable =>
      'Le sessioni di consapevolezza richiedono una versione più recente di Health Connect.';

  @override
  String get onboardingCategoryAdditionalDataAccess =>
      'Accesso aggiuntivo ai dati';

  @override
  String get onboardingCategoryAdditionalDataAccessDesc =>
      'Nelle autorizzazioni di Health Connect, apri OpenVitals > Accesso aggiuntivo e imposta:\n* Accesso ai dati passati: Abilita\n* Accesso ai dati in background: Abilita\n* Accesso ai percorsi di esercizio: Sempre';

  @override
  String onboardingCategoryAdditionalDataAccessManualNote(String arg0) {
    return '$arg0\n\nSe l\'accesso ai percorsi di esercizio non è presente nella finestra di accesso, apri le impostazioni di Health Connect per OpenVitals e impostalo in Accesso aggiuntivo.';
  }

  @override
  String get onboardingCategoryVitals => 'Parametri vitali';

  @override
  String get onboardingCategoryVitalsDesc =>
      'Health Connect richiederà:\n* Pressione sanguigna\n* Saturazione ossigeno\n* Frequenza respiratoria\n* Temperatura corporea\n* VO2 max\n* Glucosio nel sangue\n* Temperatura cutanea, se supportata';

  @override
  String get onboardingCategoryCycleTracking => 'Tracciamento ciclo';

  @override
  String get onboardingCategoryCycleTrackingDesc =>
      'Health Connect richiederà dati sensibili sul ciclo:\n* Flusso mestruale\n* Periodi mestruali\n* Test di ovulazione\n* Muco cervicale\n* Temperatura basale corporea\n* Sanguinamento intermestruale\n* Attività sessuale';

  @override
  String get settingsAllRequestableGranted =>
      'Tutte le autorizzazioni richiedibili concesse';

  @override
  String get settingsManualPermissionsTitle =>
      'Autorizzazioni manuali richieste';

  @override
  String get settingsManualPermissionsBody =>
      'Alcune autorizzazioni di Health Connect non possono essere concesse dalla finestra di richiesta standard. Apri Health Connect e abilitale per OpenVitals.';

  @override
  String get settingsOpenHealthPermissions =>
      'Apri autorizzazioni Health Connect';

  @override
  String get settingsDisplayGroupTitle => 'Visualizzazione';

  @override
  String get settingsDisplayGroupBody => 'Lingua, unità di misura e tema';

  @override
  String get settingsActivitiesGroupTitle => 'Attività';

  @override
  String get settingsActivitiesGroupBody =>
      'Date mobili, attività preferita, registrazione e mappe offline';

  @override
  String get settingsSensorsGroupTitle => 'Sensori & dispositivi';

  @override
  String get settingsSensorsGroupBody =>
      'Frequenza cardiaca, cadenza e sensori di potenza';

  @override
  String get settingsSensorsEmptyTitle => 'Ancora nessun sensore';

  @override
  String get settingsSensorsEmptyBody =>
      'Aggiungere una fascia cardiaca Bluetooth, sensore di cadenza, misuratore di potenza o sensore di pressione, da utilizzare durante la registrazione dell\'attività.';

  @override
  String get settingsSensorsAddDevice => 'Aggiungi sensore';

  @override
  String get settingsSensorsEditDevice => 'Modifica sensore';

  @override
  String get settingsSensorsRemoveDevice => 'Rimuovi sensore';

  @override
  String get settingsSensorsDeviceName => 'Nome dispositivo';

  @override
  String get settingsSensorsEnabled => 'Abilita';

  @override
  String settingsSensorsBatteryPercent(int arg0) {
    return 'Batteria $arg0%';
  }

  @override
  String get settingsSensorsBatteryUnknown => 'Batteria non ancora disponibile';

  @override
  String get settingsSensorsScanning =>
      'Scansione per sensori nelle vicinanze…';

  @override
  String get settingsSensorsScanStopped => 'Scansione interrotta';

  @override
  String get settingsSensorsScanEmpty =>
      'Nessun sensore trovato. Assicurati che il sensore sia attivo e vicino al tuo telefono.';

  @override
  String get settingsSensorsShowAllDevices => 'Mostra tutti i dispositivi';

  @override
  String get settingsSensorsOpenBluetooth => 'Apri impostazioni Bluetooth';

  @override
  String get settingsSensorsDiscovering =>
      'Scopri le funzionalità del sensore…';

  @override
  String get settingsSensorsCapabilitiesTitle => 'Funzionalità';

  @override
  String get settingsSensorsCapabilityHeartRate => 'Frequenza cardiaca';

  @override
  String get settingsSensorsCapabilityCyclingCadence => 'Cadenza pedalata';

  @override
  String get settingsSensorsCapabilityCyclingPower => 'Potenza pedalata';

  @override
  String get settingsSensorsCapabilityCyclingSpeed => 'Velocità pedalata';

  @override
  String get settingsSensorsCapabilityRunningSpeedCadence =>
      'Velocità di marcia/cadenza';

  @override
  String settingsSensorsCapabilityConflict(String arg0, String arg1) {
    return '$arg0 è già assegnato a $arg1';
  }

  @override
  String get settingsSensorsWheelCircumference =>
      'Circonferenza della ruota (mm)';

  @override
  String get activityRecordingSensorsTitle => 'Sensori';

  @override
  String get activityRecordingSensorsAddInSettings =>
      'Aggiungi sensori alle impostazioni';

  @override
  String get activityRecordingSensorsNotConfigured => 'Non configurato';

  @override
  String get activityRecordingSensorsConnected => 'Connesso';

  @override
  String get activityRecordingSensorsConnecting => 'Connessione in corso';

  @override
  String get activityRecordingSensorsReconnecting => 'Riconnessione';

  @override
  String get activityRecordingSensorsDisabled => 'Disabilitato';

  @override
  String get activityRecordingSensorsWaitingForData =>
      'In attesa di dati del sensore…';

  @override
  String get activityRecordingSensorsWaitingShort => '—';

  @override
  String get activityRecordingSensorsNoSignalShort => 'Nessun segnale';

  @override
  String get activityRecordingSensorsGarminBroadcastHint =>
      'Connesso, ma l\'orologio non trasmette frequenza cardiaca. Su Garmin: Impostazioni → Vedi Sensori → Frequenza Cardiaca al Polso → Trasmetti Frequenza Cardiaca, quindi avvia dall\'orologio. Disconnetti prima Gadgetbridge o utilizza invece una fascia cardiaca BLE.';

  @override
  String get activityRecordingSensorsRecordedTitle =>
      'Dati del sensore registrati';

  @override
  String get activityRecordingLiveHeartRate => 'Frequenza cardiaca';

  @override
  String get activityRecordingLiveCadence => 'Cadenza';

  @override
  String get activityRecordingLivePower => 'Potenza';

  @override
  String get activityRecordingLiveSpeed => 'Velocità';

  @override
  String activityRecordingNotificationHeartRate(String arg0) {
    return 'HR $arg0';
  }

  @override
  String get settingsNutritionGroupTitle => 'Nutrizione';

  @override
  String get settingsNutritionGroupBody =>
      'Dati calorie e personalizzazione caffeina';

  @override
  String get settingsCaloriesGroupTitle => 'Calorie';

  @override
  String get settingsCaloriesGroupBody => 'Dati calorie totali';

  @override
  String get settingsCaffeineGroupTitle => 'Caffeina';

  @override
  String get settingsCaffeineGroupBody =>
      'Emivita, ora di andare a letto, soglia del sonno e personalizzazione.';

  @override
  String get settingsRecoveryGroupTitle => 'Recupero';

  @override
  String get settingsRecoveryGroupBody =>
      'Intervallo sonno e calibrazione Energia corporea';

  @override
  String get settingsSleepGroupTitle => 'Sonno';

  @override
  String get settingsSleepGroupBody => 'Intervallo sonno';

  @override
  String get settingsCycleGroupTitle => 'Ciclo mestruale';

  @override
  String get settingsCycleGroupBody =>
      'Dati del ciclo e autorizzazioni Health Connect';

  @override
  String get settingsDataImportGroupTitle => 'Importatori dati';

  @override
  String get settingsDataImportGroupBody =>
      'Importa esportazioni Apple Health, file di percorso e file FIT';

  @override
  String get settingsPermissionsGroupTitle => 'Autorizzazioni';

  @override
  String get settingsPermissionsGroupBody =>
      'Accesso ai dati sanitari e procedure per autorizzazioni manuali';

  @override
  String get settingsHealthConnectGroupTitle => 'Health Connect';

  @override
  String get settingsHealthConnectGroupBody =>
      'Sincronizzazione, autorizzazioni, accesso e blocco app';

  @override
  String get settingsDebugDiagnosticsGroupTitle => 'Diagnostica di debug';

  @override
  String get settingsDebugDiagnosticsGroupBody =>
      'Salva i log diagnostici igienizzati per risolvere i problemi';

  @override
  String get settingsHealthConnectSyncTitle => 'Sincronizza con Health Connect';

  @override
  String get settingsHealthConnectSyncBody =>
      'Quando attivo, OpenVitals legge e scrive i dati di salute in base ai tuoi permessi. Quando disattivo, sincronizza la pausa senza revocare l\'accesso.';

  @override
  String get settingsHealthConnectManageAccess => 'Gestisci accesso';

  @override
  String get settingsHealthConnectManageAccessBody =>
      'Apri Health Connect per rivedere o modificare quali dati OpenVitals possono utilizzare.';

  @override
  String get healthConnectAccessInsufficientTitle =>
      'Scegli i dati da condividere';

  @override
  String get healthConnectAccessInsufficientBody =>
      'OpenVitals ha bisogno dell\'accesso Health Connect per mostrare queste informazioni. Configura i dati che vuoi condividere per continuare.';

  @override
  String get healthConnectAccessDoubleCancelTitle =>
      'I permessi richiedono attenzione';

  @override
  String get healthConnectAccessDoubleCancelBody =>
      'I permessi Health Connect non sono stati concessi. Apri le impostazioni Health Connect per scegliere quali dati condividere con OpenVitals.';

  @override
  String get healthConnectSyncPaused =>
      'La sincronizzazione Health Connect è in pausa';

  @override
  String get healthConnectSyncInProgress => 'Sincronizza con Health Connect';

  @override
  String get healthConnectDataSourceManage => 'Gestisci fonti dati';

  @override
  String get healthConnectDataSourceManageBody =>
      'Vedere quali app scrivono i dati su Health Connect e gestirne l\'accesso.';

  @override
  String get dashboardHealthConnectPromoTitle =>
      'Imposta i tuoi dati di salute';

  @override
  String get dashboardHealthConnectPromoBody =>
      'Ottieni una vista unificata della tua attività, del sonno e dei dati cardiaci dalle app e dai dispositivi che hai già utilizzato.';

  @override
  String get dashboardHealthConnectPromoAction => 'Inizia';

  @override
  String get dashboardHealthConnectSyncPausedBody =>
      'Riattiva la sincronizzazione nelle Impostazioni per aggiornare la tua dashboard.';

  @override
  String get dashboardHealthConnectInstallAction => 'Installa Health Connect';

  @override
  String get healthConnectMatchmakingTitle => 'Connetti le tue app';

  @override
  String get healthConnectMatchmakingBody =>
      'Trova app e dispositivi che possono condividere i dati che OpenVitals può leggere.';

  @override
  String get healthConnectMatchmakingAction => 'Trova fonti dati';

  @override
  String get healthConnectPromoteActivityTitle =>
      'Sblocca informazioni sulle attività';

  @override
  String get healthConnectPromoteActivityBody =>
      'Consenti ai dati di attività di vedere passaggi, distanza, allenamenti e tendenze in OpenVitals.';

  @override
  String get healthConnectPromoteActivitiesTitle => 'Vedi i tuoi allenamenti';

  @override
  String get healthConnectPromoteActivitiesBody =>
      'Consenti alla sessione di allenamento di accedere alle attività di navigazione sincronizzate tramite Health Connect.';

  @override
  String get healthConnectPromoteCaloriesTitle => 'Calorie totali bruciate';

  @override
  String get healthConnectPromoteCaloriesBody =>
      'Permetti ai dati calorici di confrontare i bruciori attivi e totali nel tempo.';

  @override
  String get healthConnectPromoteSleepTitle => 'Vedi il tuo sonno';

  @override
  String get healthConnectPromoteSleepBody =>
      'Consenti ai dati sul sonno di accedere alle medie sugli stadi, durata e punteggi del sonno.';

  @override
  String get healthConnectPromoteHeartTitle => 'Monitora la salute cardiaca';

  @override
  String get healthConnectPromoteHeartBody =>
      'Consenti alla frequenza cardiaca e ai dati HRV di monitorare la frequenza di riposo e la variabilità.';

  @override
  String get healthConnectPromoteVitalsTitle => 'Sblocca dati vitali';

  @override
  String get healthConnectPromoteVitalsBody =>
      'Consentire ai dati vitali di vedere la pressione sanguigna, SpO2 e le relative misurazioni.';

  @override
  String get healthConnectPromoteBodyTitle => 'Traccia metriche corporee';

  @override
  String get healthConnectPromoteBodyBody =>
      'Consenti ai dati sulla composizione corporea di seguire il peso, la BMI e le tendenze correlate.';

  @override
  String get healthConnectPromoteHydrationTitle => 'Traccia idratazione';

  @override
  String get healthConnectPromoteHydrationBody =>
      'Consentire ai dati sull’idratazione di vedere l’assunzione giornaliera e lo storico.';

  @override
  String get healthConnectPromoteNutritionTitle => 'Vedi alimentazione';

  @override
  String get healthConnectPromoteNutritionBody =>
      'Permetti ai dati nutrizionali di rivedere calorie e macro dalle tue fonti.';

  @override
  String get healthConnectPromoteMindfulnessTitle => 'Traccia Consapevolezza';

  @override
  String get healthConnectPromoteMindfulnessBody =>
      'Permetti ai dati della sessione di consapevolezza di vedere la tua pratica nel tempo.';

  @override
  String get healthConnectPromoteCycleTitle => 'Traccia dati ciclo';

  @override
  String get healthConnectPromoteCycleBody =>
      'Permetti ai dati del ciclo mestruale di visualizzare il flusso, i sintomi e le relative registrazioni.';

  @override
  String get healthConnectPromoteReadinessTitle =>
      'Migliora informazioni sulla prontezza';

  @override
  String get healthConnectPromoteReadinessBody =>
      'Fornisci a Health Connect dati aggiuntivi per affinare i punteggi di prontezza giornalieri.';

  @override
  String get healthConnectNewPermissionsTitle => 'Nuovi dati disponibili';

  @override
  String get healthConnectNewPermissionsBody =>
      'OpenVitals può ora leggere ulteriori tipi di dati sanitari. Consenti l\'accesso per utilizzare le nuove funzionalità.';

  @override
  String get healthConnectNewPermissionsAction => 'Verifica permessi';

  @override
  String get privacyReconsentTitle => 'Informativa sulla privacy aggiornata';

  @override
  String get privacyReconsentBody =>
      'La nostra politica sulla privacy è cambiata. Controlla e accetta di continuare la sincronizzazione con Health Connect.';

  @override
  String get privacyReconsentAction => 'Politica di revisione';

  @override
  String get dashboardSummaryToday => 'Oggi';

  @override
  String get settingsDebugLogsTitle => 'Log diagnostici igienizzati';

  @override
  String get settingsDebugLogsBody =>
      'Condividi o salva le voci diagnostiche di OpenVitals come file di testo. L\'esportazione rimuove o oscura identificatori, posizioni, date, URI, dati grezzi dei sensori e log di app non correlate prima di scrivere.';

  @override
  String get settingsDebugLogsShare => 'Condividi log';

  @override
  String get settingsDebugLogsShareChooserTitle => 'Condividi log diagnostici';

  @override
  String get settingsDebugLogsShareFailed =>
      'Impossibile condividere i log diagnostici';

  @override
  String get settingsDebugLogsSave => 'Salva log';

  @override
  String get settingsDebugLogsSaved => 'Log di debug salvati';

  @override
  String get settingsDebugLogsSaveFailed =>
      'Impossibile salvare i log diagnostici';

  @override
  String get settingsPrivacyPolicyLink => 'Visualizza l\'informativa privacy';

  @override
  String get settingsAppLockTitle => 'Blocco app';

  @override
  String get settingsAppLockBody =>
      'Richiede sblocco dispositivo per aprire OpenVitals.';

  @override
  String get onboardingCoreRequired =>
      'Attività, sonno e frequenza cardiaca sono necessari per iniziare. È possibile aggiungere più tipi di dati in seguito dalle Impostazioni.';

  @override
  String get settingsLanguageTitle => 'Lingua';

  @override
  String get settingsLanguageBody =>
      'Scegli la lingua dell\'app o segui l\'impostazione di sistema.';

  @override
  String get settingsLanguageSystem => 'Sistema';

  @override
  String get settingsLanguageEnglish => 'Inglese';

  @override
  String get settingsLanguageSpanish => 'Spagnolo';

  @override
  String get settingsLanguageGerman => 'Tedesco';

  @override
  String get settingsLanguageItalian => 'Italiano';

  @override
  String get settingsLanguageEstonian => 'Estone';

  @override
  String get settingsUnitsTitle => 'Unità di misura';

  @override
  String get settingsUnitsBody =>
      'Scegli come vengono visualizzate distanze, pesi, idratazione e temperatura.';

  @override
  String get settingsUnitMetric => 'Metrico';

  @override
  String get settingsUnitImperial => 'Imperiale';

  @override
  String get settingsThemeTitle => 'Tema';

  @override
  String get settingsThemeBody =>
      'Scegli l\'aspetto dell\'app indipendentemente dalla modalità scura di Android.';

  @override
  String get settingsThemeSystem => 'Sistema';

  @override
  String get settingsThemeLight => 'Chiaro';

  @override
  String get settingsThemeDark => 'Scuro';

  @override
  String get settingsThemeAmoled => 'AMOLED';

  @override
  String get settingsDynamicColorTitle => 'Colore dinamico (Material You)';

  @override
  String get settingsDynamicColorBody =>
      'Tinge OpenVitals in base allo sfondo Android. Se disattivato, usa la palette del brand OpenVitals in blu e verde acqua.';

  @override
  String get settingsActivityWeekTitle => 'Date mobili';

  @override
  String get settingsActivityWeekBody =>
      'Usa finestre mobili di 7, 30 e 365 giorni invece di settimana, mese e anno di calendario.';

  @override
  String get settingsActivityWeekMondayToSunday => 'Calendario';

  @override
  String get settingsActivityWeekLast7Days => 'Mobile';

  @override
  String get settingsFavoriteActivityTitle => 'Attività preferita';

  @override
  String get settingsFavoriteActivityBody =>
      'Usa l\'ultima attività registrata come predefinita, oppure scegli un tipo di attività da preselezionare sempre.';

  @override
  String get settingsFavoriteActivityLatest => 'Usa l\'ultima';

  @override
  String get settingsActivityRecordingTitle => 'Registrazione attività';

  @override
  String get settingsActivityRecordingBody =>
      'Ottimizza la registrazione GPS in tempo reale senza modificare il flusso di salvataggio dei dettagli dell\'attività.';

  @override
  String get settingsActivityRecordingKeepScreenOnTitle =>
      'Schermo sempre acceso';

  @override
  String get settingsActivityRecordingKeepScreenOnBody =>
      'Mantiene lo schermo attivo mentre una registrazione attività è in corso.';

  @override
  String get settingsActivityRecordingAutoIdleTitle => 'Pausa automatica';

  @override
  String get settingsActivityRecordingAutoIdleBody =>
      'Metti in pausa il tempo di movimento quando ti fermi per più del timeout selezionato.';

  @override
  String get settingsActivityRecordingIdleTimeoutTitle => 'Timeout inattività';

  @override
  String get settingsActivityRecordingAccuracyTitle =>
      'Precisione GPS richiesta';

  @override
  String get settingsActivityRecordingRouteGapTitle =>
      'Nuovo segmento percorso dopo interruzione';

  @override
  String get settingsActivityRecordingTimeIntervalTitle =>
      'Intervallo di registrazione per tempo';

  @override
  String get settingsActivityRecordingDistanceIntervalTitle =>
      'Intervallo di registrazione per distanza';

  @override
  String get settingsActivityRecordingBarometerTitle =>
      'Dislivello barometrico';

  @override
  String get settingsActivityRecordingBarometerBody =>
      'Usa le variazioni di pressione per il dislivello quando il dispositivo ha un barometro.';

  @override
  String get settingsActivityRecordingRestBellTitle =>
      'Campana timer di recupero';

  @override
  String get settingsActivityRecordingRestBellBody =>
      'Riproduce una campana leggera quando termina il conto alla rovescia del recupero tra le serie.';

  @override
  String get settingsActivityRecordingVoiceTitle => 'Annunci vocali';

  @override
  String get settingsActivityRecordingVoiceBody =>
      'Pronuncia aggiornamenti periodici sul progresso, sulla pausa/ripresa e sui giri durante la registrazione.';

  @override
  String get settingsActivityRecordingVoiceTimeTitle => 'Annuncia per tempo';

  @override
  String get settingsActivityRecordingVoiceDistanceTitle =>
      'Annuncia per distanza';

  @override
  String get settingsActivityRecordingVoiceIdleTitle => 'Annunci di inattività';

  @override
  String get settingsActivityRecordingVoiceIdleBody =>
      'Comunica quando la pausa automatica si attiva e quando la registrazione riprende.';

  @override
  String get settingsActivityRecordingVoiceLapTitle => 'Annunci di giro';

  @override
  String get settingsActivityRecordingVoiceLapBody =>
      'Pronuncia un riepilogo del progresso quando segni un giro.';

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
  String get settingsActivityRecordingOff => 'Disattivato';

  @override
  String get settingsCalorieDataTitle => 'Dati calorie totali';

  @override
  String get settingsCalorieDataBody =>
      'Mostra le calorie totali semplici di Health Connect. Attiva i calcoli di OpenVitals per completare i totali mancanti con calorie attive e MBR.';

  @override
  String get settingsCaffeineTitle => 'Modello caffeina';

  @override
  String get settingsCaffeineBody =>
      'Questi valori personalizzano il livello di caffeina, la previsione dell\'ora di andare a letto e gli insight per un sonno sicuro. Le voci restano in Health Connect.';

  @override
  String get settingsBodyProfileTitle => 'Profilo corporeo';

  @override
  String get settingsBodyProfileBody =>
      'Età, peso e frequenza cardiaca personalizzano le stime di energia corporea e caffeina. Tutti i campi sono facoltativi.';

  @override
  String get settingsBodyProfileWeight => 'Peso';

  @override
  String get settingsSleepRangeTitle => 'Intervallo sonno';

  @override
  String get settingsSleepRangeBody =>
      'Scegli a quale giorno vengono assegnate le sessioni di sonno.';

  @override
  String get settingsSleepRangeRolling24h => '24 ore mobili';

  @override
  String get settingsSleepRangeNoon => 'Mezzogiorno';

  @override
  String get settingsSleepRangeEvening => '18:00';

  @override
  String get settingsCyclePermissionsTitle => 'Autorizzazioni ciclo';

  @override
  String settingsCyclePermissionsGranted(int arg0, int arg1) {
    return '$arg0/$arg1 autorizzazioni ciclo concesse.';
  }

  @override
  String get settingsAppleHealthImportTitle => 'Importatore Apple Health';

  @override
  String get settingsAppleHealthImportBody =>
      'Importa record compatibili con Health Connect dall\'esportazione Apple Health (export.xml o export.zip), con controllo duplicati e un report diagnostico condivisibile.';

  @override
  String settingsAppleHealthImportPermissions(int arg0, int arg1) {
    return '$arg0/$arg1 autorizzazioni di importazione concesse.';
  }

  @override
  String get settingsAppleHealthImportGrant =>
      'Concedi autorizzazioni di importazione';

  @override
  String get settingsAppleHealthImportAction =>
      'Importa esportazione Apple Health';

  @override
  String get settingsAppleHealthImportAnalyzeAction =>
      'Analizza esportazione Apple Health';

  @override
  String get settingsAppleHealthImportChooseAnotherAction =>
      'Scegli un\'altra esportazione Apple Health';

  @override
  String get settingsAppleHealthImportSelectedAction =>
      'Importa categorie selezionate';

  @override
  String get settingsAppleHealthImportAnalyzing => 'Analisi in corso…';

  @override
  String get settingsAppleHealthImporting => 'Importazione in corso…';

  @override
  String get settingsAppleHealthImportProgressQueued => 'In coda';

  @override
  String get settingsAppleHealthImportProgressParsing =>
      'Scansione esportazione';

  @override
  String get settingsAppleHealthImportProgressConverting =>
      'Conversione record';

  @override
  String get settingsAppleHealthImportProgressCheckingDuplicates =>
      'Controllo duplicati';

  @override
  String get settingsAppleHealthImportProgressWriting => 'Scrittura record';

  @override
  String get settingsAppleHealthImportProgressFinishing =>
      'Finalizzazione importazione';

  @override
  String get settingsAppleHealthImportProgressBuildingReport =>
      'Creazione report';

  @override
  String get settingsAppleHealthImportProgressComplete => 'Completato';

  @override
  String settingsAppleHealthImportProgress(String arg0, int arg1, int arg2) {
    return '$arg0. Scansionati $arg1 elementi, importati $arg2 record.';
  }

  @override
  String settingsAppleHealthImportProgressWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Selezionati $arg2/$arg3 record, importati $arg4.';
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
      'L\'importazione continua in background mentre esci dall\'app.';

  @override
  String get settingsAppleHealthImportNotificationChannel =>
      'Importazioni Apple Health';

  @override
  String get settingsAppleHealthImportNotificationTitle =>
      'Importazione esportazione Apple Health';

  @override
  String settingsAppleHealthImportNotificationText(
    String arg0,
    int arg1,
    int arg2,
  ) {
    return '$arg0. Scansionati $arg1, importati $arg2.';
  }

  @override
  String settingsAppleHealthImportNotificationTextWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Selezionati $arg2/$arg3, importati $arg4.';
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
    return 'Importati $arg0. Duplicati $arg1. Non selezionati $arg2. Non supportati $arg3. Ignorati $arg4. Falliti $arg5.';
  }

  @override
  String get settingsAppleHealthImportRoutesIncomplete =>
      'Le cartelle cliniche sono state importate, ma alcuni percorsi di allenamento non erano disponibili perché il codice postale terminava in modo imprevisto. Il report di importazione elenca le attività interessate, che possono essere recuperate manualmente.';

  @override
  String settingsAppleHealthImportAnalysisResult(
    int arg0,
    int arg1,
    int arg2,
    int arg3,
  ) {
    return 'Scansionati $arg0 elementi. Trovati $arg1 record compatibili. Non supportati $arg2. Falliti $arg3.';
  }

  @override
  String get settingsAppleHealthImportChooseCategories =>
      'Scegli cosa scrivere in Health Connect.';

  @override
  String settingsAppleHealthImportCategoryCount(int arg0) {
    return '$arg0 record';
  }

  @override
  String settingsAppleHealthImportCategoryCountRoutes(int arg0, int arg1) {
    return '$arg0 record, $arg1 con percorsi';
  }

  @override
  String get settingsAppleHealthImportCategoryWorkouts =>
      'Allenamenti e percorsi';

  @override
  String get settingsAppleHealthImportCategoryWorkoutsDesc =>
      'Sessioni di allenamento e geometria dei percorsi collegata.';

  @override
  String get settingsAppleHealthImportCategoryActivity => 'Metriche attività';

  @override
  String get settingsAppleHealthImportCategoryActivityDesc =>
      'Passi, distanza, calorie, piani, dislivello, spinte sedia a rotelle e velocità.';

  @override
  String get settingsAppleHealthImportCategoryHeart => 'Cuore';

  @override
  String get settingsAppleHealthImportCategoryHeartDesc =>
      'Frequenza cardiaca e frequenza cardiaca a riposo.';

  @override
  String get settingsAppleHealthImportCategorySleep => 'Sonno';

  @override
  String get settingsAppleHealthImportCategorySleepDesc =>
      'Sessioni e fasi del sonno.';

  @override
  String get settingsAppleHealthImportCategoryBody => 'Misure corporee';

  @override
  String get settingsAppleHealthImportCategoryBodyDesc =>
      'Peso, altezza, grasso corporeo, massa magra, BMR, massa ossea e acqua corporea.';

  @override
  String get settingsAppleHealthImportCategoryVitals => 'Parametri vitali';

  @override
  String get settingsAppleHealthImportCategoryVitalsDesc =>
      'Pressione, saturazione, frequenza respiratoria, temperatura, glicemia e VO2 max.';

  @override
  String get settingsAppleHealthImportCategoryNutrition => 'Nutrizione';

  @override
  String get settingsAppleHealthImportCategoryNutritionDesc =>
      'Energia, macro, caffeina, minerali e vitamine.';

  @override
  String get settingsAppleHealthImportCategoryHydration => 'Idratazione';

  @override
  String get settingsAppleHealthImportCategoryHydrationDesc =>
      'Record di acqua assunta.';

  @override
  String get settingsAppleHealthImportCategoryMindfulness => 'Mindfulness';

  @override
  String get settingsAppleHealthImportCategoryMindfulnessDesc =>
      'Sessioni mindfulness quando supportate da Health Connect.';

  @override
  String get settingsAppleHealthImportCategoryCycle => 'Monitoraggio ciclo';

  @override
  String get settingsAppleHealthImportCategoryCycleDesc =>
      'Mestruazioni, ovulazione, muco cervicale, sanguinamenti, temperatura basale e attività sessuale.';

  @override
  String get settingsAppleHealthImportCopyReport => 'Copia report';

  @override
  String get settingsAppleHealthImportCopyError => 'Copia errore';

  @override
  String get settingsAppleHealthImportSaveReport => 'Salva report';

  @override
  String get settingsAppleHealthImportReportCopied =>
      'Report di importazione copiato.';

  @override
  String get settingsAppleHealthImportErrorCopied =>
      'Errore di importazione copiato.';

  @override
  String get settingsAppleHealthImportReportSaved =>
      'Report di importazione salvato.';

  @override
  String get settingsAppleHealthImportReportSaveFailed =>
      'Impossibile salvare il report di importazione.';

  @override
  String settingsAppleHealthImportError(String arg0) {
    return 'Importazione fallita: $arg0';
  }

  @override
  String get settingsAppleHealthImportPermissionDenied =>
      'L\'accesso al file selezionato è andato perso, quindi l\'importazione non ha potuto continuare. Seleziona di nuovo la stessa esportazione di Apple Health per riprendere esattamente da dove eri rimasto.';

  @override
  String get settingsRouteImportTitle => 'Importatore GPX/KML/KMZ';

  @override
  String get settingsRouteImportBody =>
      'Importa file di percorso GPX, KML o KMZ. Rivedi un file prima di salvarlo oppure importa più file direttamente in Health Connect.';

  @override
  String settingsRouteImportPermissions(int arg0, int arg1) {
    return '$arg0/$arg1 autorizzazioni di importazione percorsi concesse.';
  }

  @override
  String get settingsRouteImportGrant =>
      'Concedi autorizzazioni di importazione percorsi';

  @override
  String get settingsRouteImportAction => 'Importa file GPX/KML/KMZ';

  @override
  String get settingsRouteImportBulkAction =>
      'Importa file GPX/KML/KMZ in blocco';

  @override
  String get settingsRouteImporting => 'Importazione percorsi...';

  @override
  String settingsRouteImportProgress(int arg0, int arg1, int arg2, int arg3) {
    return 'File $arg0/$arg1. Importati $arg2, falliti $arg3.';
  }

  @override
  String settingsRouteImportResult(int arg0, int arg1, int arg2) {
    return 'Importati $arg0. Falliti $arg1. Selezionati $arg2.';
  }

  @override
  String settingsRouteImportError(String arg0) {
    return 'Avviso importazione percorsi: $arg0';
  }

  @override
  String get settingsFitImportTitle => 'Importatore FIT';

  @override
  String get settingsFitImportBody =>
      'Importa file FIT di attività, percorso o allenamento, rivedi i dettagli rilevati e scegli se salvarli in Health Connect.';

  @override
  String get settingsFitImportAction => 'Importa file FIT';

  @override
  String get settingsOfflineMapsTitle => 'Mappe offline';

  @override
  String get settingsOfflineMapsBody =>
      'Importa pacchetti PMTiles o Mapsforge .map/.maps per mappe attività completamente offline. Sono supportate mappe base PMTiles compatibili con Protomaps e mappe Mapsforge.';

  @override
  String get settingsOfflineMapsEmpty =>
      'Ancora nessuna mappa offline importata.';

  @override
  String get settingsOfflineMapsFormatPmtiles => 'PMTiles';

  @override
  String get settingsOfflineMapsFormatMapsforge => 'Mapsforge';

  @override
  String get settingsOfflineMapsRenderFormatTitle => 'Formato di rendering';

  @override
  String settingsOfflineMapsRenderFormatOption(String arg0, int arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String get settingsOfflineMapsRenderFormatBody =>
      'OpenVitals renderizza insieme tutti i pacchetti importati nel formato selezionato.';

  @override
  String settingsOfflineMapsPackDetail(String arg0, String arg1, String arg2) {
    return '$arg0 • $arg1 • $arg2';
  }

  @override
  String get settingsOfflineMapsImportAction => 'Importa mappa offline';

  @override
  String get settingsOfflineMapsImporting => 'Importazione in corso…';

  @override
  String get settingsOfflineMapsImportProgressQueued => 'In coda';

  @override
  String get settingsOfflineMapsImportProgressCopying => 'Copia mappa';

  @override
  String get settingsOfflineMapsImportProgressComplete => 'Completato';

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
      'L\'importazione continua in background quando esci dall\'app.';

  @override
  String settingsOfflineMapsImportResult(String arg0, String arg1) {
    return 'Importato $arg0 ($arg1).';
  }

  @override
  String settingsOfflineMapsImportError(String arg0) {
    return 'Importazione mappa fallita: $arg0';
  }

  @override
  String get settingsOfflineMapsImportNotificationChannel =>
      'Importazioni mappe offline';

  @override
  String get settingsOfflineMapsImportNotificationTitle =>
      'Importazione mappa offline';

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
      'Vuoi imparare come aggiungere mappe offline? Vai a:';

  @override
  String get settingsOfflineMapsHelpLink => 'Apri la guida alle mappe offline';

  @override
  String get sectionSupport => 'Supporto';

  @override
  String get settingsSupportTitle => 'Supporta OpenVitals';

  @override
  String get settingsSupportBody =>
      'Segnala bug, partecipa alle discussioni di supporto della community o aiuta a finanziare lo sviluppo continuo.';

  @override
  String get settingsSupportIssuesAction => 'Segnala un problema';

  @override
  String get settingsSupportDiscussionAction =>
      'Unisciti alle discussioni Zulip';

  @override
  String get settingsSupportAction => 'Apri Liberapay';

  @override
  String get crashReportEmailChooserTitle =>
      'Invia report OpenVitals via email';

  @override
  String get crashReportFallbackTitle => 'Nessuna app email trovata';

  @override
  String crashReportFallbackBody(String arg0) {
    return 'Copia il report o salvalo come file di testo, poi invialo a $arg0 più tardi.';
  }

  @override
  String get crashReportFallbackCopy => 'Copia report';

  @override
  String get crashReportFallbackSave => 'Salva file di testo';

  @override
  String get crashReportFallbackCopied => 'Report copiato.';

  @override
  String get crashReportFallbackSaved => 'Report salvato.';

  @override
  String get crashReportFallbackSaveFailed => 'Impossibile salvare il report.';

  @override
  String get crashReportFallbackSaveUnavailable =>
      'Nessuna app per salvare file trovata. Report copiato.';

  @override
  String get crashReportClipboardLabel => 'Report OpenVitals';

  @override
  String get settingsPrivacyNoAccount => 'Nessun account richiesto';

  @override
  String get settingsPrivacyNoCloud =>
      'Nessuna sincronizzazione cloud dei dati sanitari';

  @override
  String get settingsPrivacyNoAnalytics => 'Nessun SDK di analisi';

  @override
  String get settingsPrivacyNoAds =>
      'Nessuna pubblicità né tracciamento di terze parti';

  @override
  String get settingsPrivacyOnDevice => 'I dati rimangono sul dispositivo';

  @override
  String get settingsPrivacyReadOnly =>
      'Solo lettura, tranne le voci esplicitamente registrate';

  @override
  String settingsAppVersion(String arg0, int arg1) {
    return 'Versione $arg0 ($arg1)';
  }

  @override
  String get detailMetrics => 'Metriche';

  @override
  String get detailSessionDetails => 'Dettagli sessione';

  @override
  String get detailDuration => 'Durata';

  @override
  String get detailMovingTime => 'Tempo di movimento';

  @override
  String get detailType => 'Tipo';

  @override
  String get detailStarted => 'Iniziato';

  @override
  String get detailEnded => 'Terminato';

  @override
  String get detailStartZone => 'Zona di partenza';

  @override
  String get detailEndZone => 'Zona di arrivo';

  @override
  String get detailRecording => 'Registrazione';

  @override
  String get detailSourcePackage => 'Pacchetto sorgente';

  @override
  String get detailDeviceType => 'Tipo dispositivo';

  @override
  String get detailDeviceMaker => 'Produttore dispositivo';

  @override
  String get detailDeviceModel => 'Modello dispositivo';

  @override
  String get detailLastModified => 'Ultima modifica';

  @override
  String get detailRecordId => 'ID record';

  @override
  String get detailClientRecordId => 'ID record client';

  @override
  String get detailClientVersion => 'Versione client';

  @override
  String get detailPlannedSessionId => 'ID sessione pianificata';

  @override
  String get detailNotes => 'Note';

  @override
  String get detailTitle => 'Titolo';

  @override
  String get detailTime => 'Ora';

  @override
  String get detailRepetitions => 'Ripetizioni';

  @override
  String get detailSet => 'Serie';

  @override
  String get detailLength => 'Lunghezza';

  @override
  String get detailSegments => 'Segmenti';

  @override
  String get detailLaps => 'Giri';

  @override
  String detailLap(int arg0) {
    return 'Giro $arg0';
  }

  @override
  String get detailRoute => 'Percorso';

  @override
  String get detailStatus => 'Stato';

  @override
  String get detailStatusAvailable => 'Disponibile';

  @override
  String get detailPoints => 'Punti';

  @override
  String get detailStartPoint => 'Punto di partenza';

  @override
  String get detailEndPoint => 'Punto di arrivo';

  @override
  String detailAltitude(String arg0) {
    return 'Altitudine $arg0';
  }

  @override
  String detailHorizontalAccuracy(String arg0) {
    return 'Precisione orizzontale $arg0';
  }

  @override
  String detailVerticalAccuracy(String arg0) {
    return 'Precisione verticale $arg0';
  }

  @override
  String get detailStageEvents => 'Eventi di fase';

  @override
  String get detailStages => 'Fasi';

  @override
  String get detailSleepSession => 'Sessione di sonno';

  @override
  String get recordingActivelyRecorded => 'Registrato attivamente';

  @override
  String get recordingAutomaticallyRecorded => 'Registrato automaticamente';

  @override
  String get recordingManualEntry => 'Inserimento manuale';

  @override
  String get recordingUnknown => 'Sconosciuto';

  @override
  String get deviceWatch => 'Orologio';

  @override
  String get devicePhone => 'Telefono';

  @override
  String get deviceScale => 'Bilancia';

  @override
  String get deviceRing => 'Anello';

  @override
  String get deviceHeadMounted => 'Dispositivo testa';

  @override
  String get deviceFitnessBand => 'Fascia fitness';

  @override
  String get deviceChestStrap => 'Fascia pettorale';

  @override
  String get deviceSmartDisplay => 'Display intelligente';

  @override
  String get sleepStageAwake => 'Sveglio';

  @override
  String get sleepStageSleeping => 'Addormentato';

  @override
  String get sleepStageOutOfBed => 'Fuori dal letto';

  @override
  String get sleepStageLight => 'Leggero';

  @override
  String get sleepStageDeep => 'Profondo';

  @override
  String get sleepStageRem => 'REM';

  @override
  String get sleepStageAwakeInBed => 'Sveglio a letto';

  @override
  String get sleepStageUnknown => 'Sconosciuto';

  @override
  String get sleepStagesShareTitle => 'Percentuale del tempo a letto';

  @override
  String get cyclePermissionsMissingTitle => 'Autorizzazioni ciclo mancanti';

  @override
  String get cyclePermissionsMissingBody =>
      'Concedi le autorizzazioni di tracciamento ciclo per visualizzare i giorni mestruali, i test di ovulazione, il muco cervicale e la temperatura basale.';

  @override
  String get cycleObservationMenstruationPeriod => 'Periodo mestruale';

  @override
  String get cycleObservationMenstruationFlow => 'Flusso mestruale';

  @override
  String get cycleObservationOvulationTest => 'Test di ovulazione';

  @override
  String get cycleObservationCervicalMucus => 'Muco cervicale';

  @override
  String get cycleObservationBasalBodyTemperature =>
      'Temperatura basale corporea';

  @override
  String get cycleObservationIntermenstrualBleeding =>
      'Sanguinamento intermestruale';

  @override
  String get cycleObservationSexualActivity => 'Attività sessuale';

  @override
  String get cycleProtectionProtected => 'Protetto';

  @override
  String get cycleProtectionUnprotected => 'Non protetto';

  @override
  String get cycleProtectionUnknown => 'Protezione sconosciuta';

  @override
  String cycleBasalTemperatureValue(String arg1) {
    return '%1\$.1f C · $arg1';
  }

  @override
  String cycleDaysValue(int arg0, String arg1) {
    return '$arg0 $arg1';
  }

  @override
  String get cycleDaySingular => 'giorno';

  @override
  String get cycleDayPlural => 'giorni';

  @override
  String get cycleFlowLight => 'Leggero';

  @override
  String get cycleFlowMedium => 'Medio';

  @override
  String get cycleFlowHeavy => 'Abbondante';

  @override
  String get cycleOvulationPositive => 'Positivo';

  @override
  String get cycleOvulationHigh => 'Alto';

  @override
  String get cycleOvulationNegative => 'Negativo';

  @override
  String get cycleOvulationInconclusive => 'Non conclusivo';

  @override
  String get cycleMucusDry => 'Secco';

  @override
  String get cycleMucusSticky => 'Appiccicoso';

  @override
  String get cycleMucusCreamy => 'Cremoso';

  @override
  String get cycleMucusWatery => 'Acquoso';

  @override
  String get cycleMucusEggWhite => 'Albume d\'uovo';

  @override
  String get cycleMucusUnusual => 'Insolito';

  @override
  String get cycleMucusLight => 'leggero';

  @override
  String get cycleMucusMedium => 'medio';

  @override
  String get cycleMucusHeavy => 'abbondante';

  @override
  String cycleMucusValue(String arg0, String arg1) {
    return '$arg0, $arg1';
  }

  @override
  String get measurementLocationArmpit => 'Ascella';

  @override
  String get measurementLocationFinger => 'Dito';

  @override
  String get measurementLocationForehead => 'Fronte';

  @override
  String get measurementLocationMouth => 'Bocca';

  @override
  String get measurementLocationRectum => 'Retto';

  @override
  String get measurementLocationTemporalArtery => 'Arteria temporale';

  @override
  String get measurementLocationToe => 'Alluce';

  @override
  String get measurementLocationEar => 'Orecchio';

  @override
  String get measurementLocationWrist => 'Polso';

  @override
  String get measurementLocationVagina => 'Vagina';

  @override
  String get measurementLocationUnknown =>
      'Posizione di misurazione sconosciuta';

  @override
  String get weekdayMondayShort => 'L';

  @override
  String get weekdayTuesdayShort => 'M';

  @override
  String get weekdayWednesdayShort => 'M';

  @override
  String get weekdayThursdayShort => 'G';

  @override
  String get weekdayFridayShort => 'V';

  @override
  String get weekdaySaturdayShort => 'D';

  @override
  String get weekdaySundayShort => 'D';

  @override
  String get vitalsPermissionsNeededTitle =>
      'Autorizzazioni parametri vitali richieste';

  @override
  String get vitalsPermissionsNeededBody =>
      'Concedi le autorizzazioni per pressione sanguigna, saturazione ossigeno, frequenza respiratoria, temperatura, VO2 max e glucosio per visualizzare questa schermata.';

  @override
  String get vitalsRespiratoryRateReadings =>
      'Rilevazioni frequenza respiratoria';

  @override
  String get vitalsBodyTemperatureReadings =>
      'Rilevazioni temperatura corporea';

  @override
  String get heartRateHealthChecksTitle => 'Controlli frequenza cardiaca';

  @override
  String get heartRateHighTitle => 'Frequenza cardiaca alta';

  @override
  String get heartRateLowTitle => 'Frequenza cardiaca bassa';

  @override
  String heartRateSamplesAtOrAbove(int arg0) {
    return 'Campioni a/sopra $arg0 bpm';
  }

  @override
  String heartRateSamplesAtOrBelow(int arg0) {
    return 'Campioni a/sotto $arg0 bpm';
  }

  @override
  String heartRateDaysAtOrAbove(int arg0) {
    return 'Giorni a/sopra $arg0 bpm';
  }

  @override
  String heartRateDaysAtOrBelow(int arg0) {
    return 'Giorni a/sotto $arg0 bpm';
  }

  @override
  String get cdDecreaseHrThreshold => 'Diminuisci soglia frequenza cardiaca';

  @override
  String get cdIncreaseHrThreshold => 'Aumenta soglia frequenza cardiaca';

  @override
  String get mealBreakfast => 'Colazione';

  @override
  String get mealLunch => 'Pranzo';

  @override
  String get mealDinner => 'Cena';

  @override
  String get mealSnack => 'Spuntino';

  @override
  String get mealGeneric => 'Pasto';

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
    return 'G ${arg0}g';
  }

  @override
  String macroFiber(String arg0) {
    return 'fibra ${arg0}g';
  }

  @override
  String macroSugar(String arg0) {
    return 'zucchero ${arg0}g';
  }

  @override
  String get caffeineSectionOverview => 'Panoramica';

  @override
  String get caffeineSectionDashboard => 'Dashboard';

  @override
  String get caffeineSectionAnalytics => 'Analisi';

  @override
  String get caffeineSectionSleep => 'Impatto sul sonno';

  @override
  String get caffeineSectionSources => 'Fonti';

  @override
  String get caffeineSectionEntries => 'Voci';

  @override
  String get caffeineSectionScience => 'Scienza';

  @override
  String get caffeineSetupTitle => 'Personalizza insight sulla caffeina';

  @override
  String get caffeineSetupBody =>
      'OpenVitals ha trovato dati sulla caffeina. La personalizzazione migliora la curva della caffeina e la previsione dell\'ora di andare a letto.';

  @override
  String get caffeineCurrentTitle => 'Caffeina attiva';

  @override
  String get caffeineTodayTotal => 'Totale di oggi';

  @override
  String get caffeineTimeToSafe => 'Tempo fino al livello sicuro';

  @override
  String get caffeineSleepStatusUnlikely => 'Impatto sul sonno improbabile';

  @override
  String caffeineSleepStatusUnlikelyBody(String arg0, String arg1) {
    return '$arg0 attiva ora, sotto la tua soglia sonno di $arg1.';
  }

  @override
  String get caffeineSleepStatusElevatedNow => 'Elevata ora';

  @override
  String caffeineSleepStatusElevatedNowBody(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return '$arg0 attiva ora. Stimata sotto la soglia tra $arg1; previsione per andare a letto: $arg2 alle $arg3.';
  }

  @override
  String get caffeineSleepStatusMayAffect => 'Può influire sul sonno';

  @override
  String caffeineSleepStatusMayAffectBody(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'La previsione per andare a letto è $arg0 alle $arg1, sopra la tua soglia di $arg2.';
  }

  @override
  String get caffeinePeriodTotal => 'Totale periodo';

  @override
  String get caffeineDailyAverage => 'Media giornaliera';

  @override
  String get caffeineLoggedDays => 'Giorni registrati';

  @override
  String get caffeinePeakDay => 'Giorno di picco';

  @override
  String caffeinePeakDayValue(String arg0, String arg1) {
    return '$arg0 - $arg1';
  }

  @override
  String get caffeineCurveTitle => 'Curva della caffeina';

  @override
  String caffeineThresholdLine(String arg0) {
    return 'Soglia sonno $arg0';
  }

  @override
  String get caffeineBedtimeForecast => 'Previsione ora di andare a letto';

  @override
  String caffeineBedtimeSummary(String arg0, String arg1) {
    return 'Alle $arg0 con soglia $arg1';
  }

  @override
  String get caffeineSafeNights => 'Notti sicure';

  @override
  String get caffeineSafeStreak => 'Serie sicura';

  @override
  String get caffeineTopSource => 'Fonte principale';

  @override
  String get caffeineSleepThreshold => 'Soglia sonno';

  @override
  String get caffeineDailyImpact => 'Impatto giornaliero e sul sonno';

  @override
  String get caffeineSafeCalendar => 'Calendario notti sicure';

  @override
  String get caffeineSources => 'App fonte';

  @override
  String get caffeineItems => 'Elementi';

  @override
  String get caffeineInferredCategories => 'Categorie dedotte';

  @override
  String get caffeineTimeOfDay => 'Ora del giorno';

  @override
  String get caffeineEntry => 'Voce caffeina';

  @override
  String caffeineInferredCategory(String arg0) {
    return 'Categoria: $arg0';
  }

  @override
  String caffeineCatalogMatch(String arg0) {
    return 'Catalogo: $arg0';
  }

  @override
  String get caffeineCategory => 'Categoria';

  @override
  String get caffeineCatalog => 'Catalogo';

  @override
  String caffeineCatalogMatchDetail(String arg0, String arg1, String arg2) {
    return '$arg0, tipico $arg1, corrispondenza $arg2';
  }

  @override
  String get caffeineHealthConnectSourceLabel => 'Fonte';

  @override
  String get caffeineHealthConnectMealLabel => 'Pasto';

  @override
  String get caffeineHealthConnectDurationLabel => 'Durata';

  @override
  String caffeineCurrentContribution(String arg0) {
    return '$arg0 attiva';
  }

  @override
  String get caffeineCurrentContributionLabel => 'Attuale';

  @override
  String get caffeineDose => 'Dose';

  @override
  String get caffeinePeak => 'Picco';

  @override
  String get caffeinePeakTime => 'Ora di picco';

  @override
  String get caffeineContributionCurve => 'Curva di contributo';

  @override
  String get caffeineEmpty =>
      'Nessuna voce di caffeina per questo periodo. Le bevande con caffeina aggiunte tramite idratazione o nutrizione appariranno qui quando Health Connect include la caffeina.';

  @override
  String get caffeineScienceTitle => 'Come funziona la stima';

  @override
  String get caffeineScienceBody =>
      'OpenVitals legge la caffeina dai record nutrizionali di Health Connect in milligrammi, poi stima l\'assorbimento nella finestra di assorbimento configurata e l\'eliminazione esponenziale in base alla tua emivita personalizzata.';

  @override
  String get caffeineScienceMeasurements => 'Misurazioni usate';

  @override
  String get caffeineScienceMeasurementsBody =>
      'La dose registrata proviene sempre da Health Connect. Ora di inizio/fine, nome voce, tipo di pasto e pacchetto di origine dati vengono usati per tempistica, corrispondenze ed etichette di analisi. Le corrispondenze del catalogo annotano solo le voci; non sostituiscono mai la dose registrata.';

  @override
  String get caffeineScienceLimits =>
      'Questo è un modello pratico di popolazione, non un consiglio medico. Gravidanza, farmaci, malattie epatiche, genetica, fumo, alcol, sensibilità e abitudine possono tutti modificare la risposta alla caffeina.';

  @override
  String get caffeineReferencesTitle => 'Ricerca e riferimenti';

  @override
  String get caffeineReferenceDrake =>
      'Tempistica della caffeina e sonno, Drake 2013';

  @override
  String get caffeineReferenceNehlig =>
      'Metabolismo individuale della caffeina, Nehlig 2018';

  @override
  String get caffeineReferenceEfsa =>
      'Note EFSA su sicurezza della caffeina e sonno';

  @override
  String get caffeineReferenceHealthConnect =>
      'Campi dei record nutrizionali di Health Connect';

  @override
  String get unknownSource => 'Fonte sconosciuta';

  @override
  String get achievementsLegacyTitle => 'Medaglie attività legacy';

  @override
  String achievementsProgressSummary(int arg0, int arg1) {
    return '$arg0 di $arg1 sbloccati';
  }

  @override
  String achievementsDataWindow(String arg0, String arg1, String arg2) {
    return '$arg0 a $arg1 · $arg2 giorni tracciati';
  }

  @override
  String get achievementsTrackedDays => 'Giorni tracciati';

  @override
  String get achievementsBestSteps => 'Passi migliori';

  @override
  String get achievementsTotalDistance => 'Distanza totale';

  @override
  String get achievementsBestFloors => 'Piani migliori';

  @override
  String get achievementsTotalFloors => 'Piani totali';

  @override
  String get achievementsFilterAll => 'Tutti';

  @override
  String get achievementsCategoryDailySteps => 'Passi giornalieri';

  @override
  String get achievementsCategoryLifetimeDistance => 'Distanza complessiva';

  @override
  String get achievementsCategoryDailyFloors => 'Piani giornalieri';

  @override
  String get achievementsCategoryLifetimeFloors => 'Piani complessivi';

  @override
  String achievementsDailyStepsRequirement(String arg0) {
    return '$arg0 passi in un giorno';
  }

  @override
  String achievementsLifetimeDistanceRequirement(String arg0) {
    return '$arg0 distanza totale';
  }

  @override
  String achievementsDailyFloorsRequirement(String arg0) {
    return '$arg0 piani in un giorno';
  }

  @override
  String achievementsLifetimeFloorsRequirement(String arg0) {
    return '$arg0 piani totali';
  }

  @override
  String achievementsProgressValue(String arg0, String arg1) {
    return '$arg0 di $arg1';
  }

  @override
  String achievementsAchievedOn(String arg0) {
    return 'Sbloccato il $arg0';
  }

  @override
  String get achievementsEarnedOnce => 'Ottenuto';

  @override
  String achievementsEarnedTimes(int arg0) {
    return '$arg0 volte';
  }

  @override
  String get achievementsLocked => 'Bloccato';

  @override
  String get achievementsNoDataTitle => 'Nessuno storico attività';

  @override
  String get achievementsNoDataBody =>
      'Nessun record di passi o distanza è stato restituito da Health Connect. Verifica che i dati di attività esistano e che l\'accesso alla cronologia sia concesso per i record più vecchi.';

  @override
  String get achievementsNoFloorDataTitle => 'Nessun dato sui piani';

  @override
  String get achievementsNoFloorDataBody =>
      'Le medaglie per i piani si sbloccano quando Health Connect dispone dei dati sui piani saliti.';

  @override
  String get achievementsErrorTitle => 'Obiettivi non disponibili';

  @override
  String get dataConfidenceTitle => 'Attendibilità dati';

  @override
  String get dataConfidenceHigh => 'Alta attendibilità';

  @override
  String get dataConfidenceMedium => 'Media attendibilità';

  @override
  String get dataConfidenceLow => 'Bassa attendibilità';

  @override
  String dataConfidenceCoverage(int arg0, int arg1, int arg2) {
    return '$arg0 di $arg1 giorni tracciati ($arg2%)';
  }

  @override
  String dataConfidenceSamples(int arg0) {
    return '$arg0 record';
  }

  @override
  String get dataConfidenceSourceUnavailable =>
      'Dettagli sorgente non disponibili per questo aggregato';

  @override
  String dataConfidenceSourceSingle(String arg0) {
    return 'Sorgente: $arg0';
  }

  @override
  String dataConfidenceSourceMixed(String arg0) {
    return 'Sorgenti miste: $arg0';
  }

  @override
  String get dataConfidenceKindMeasured => 'Record misurati da Health Connect';

  @override
  String get dataConfidenceKindAggregated =>
      'Aggregato da record Health Connect';

  @override
  String get dataConfidenceKindCalculated => 'Calcolato da OpenVitals';

  @override
  String get dataConfidenceKindEstimated => 'Valore stimato o derivato';

  @override
  String get dataConfidenceKindMixed => 'Dati misti misurati e calcolati';

  @override
  String get dataConfidenceWarningLowCoverage =>
      'I giorni mancanti possono indebolire medie e tendenze.';

  @override
  String get dataConfidenceWarningSparse =>
      'Dati scarsi: tendenze e statistiche potrebbero essere instabili.';

  @override
  String get dataConfidenceWarningMixedSources =>
      'I cambi di sorgente possono spiegare salti o dati apparentemente duplicati.';

  @override
  String get dataConfidenceWarningManual =>
      'Le voci manuali sono incluse in questo periodo.';

  @override
  String get dataConfidenceWarningCalculated =>
      'Questo valore è derivato, non misurato direttamente.';

  @override
  String get dataConfidenceWarningNoSources =>
      'Questo aggregato non espone i dettagli a livello di sorgente.';

  @override
  String get settingsBodyEnergyGroupTitle => 'Energia corporea';

  @override
  String get settingsBodyEnergyGroupBody =>
      'Calibrazione per energia stimata durante la giornata e zone di sforzo.';

  @override
  String get bodyEnergyCalibrationTitle =>
      'Migliora le stime di energia corporea';

  @override
  String get bodyEnergyCalibrationBody =>
      'OpenVitals stima il consumo in base alla intensità della frequenza cardiaca nel tempo. Età, frequenza cardiaca massima, frequenza a riposo e zone aiutano a classificare lo sforzo con maggiore precisione.';

  @override
  String get bodyEnergyCalibrationOptionalBody =>
      'Questo è facoltativo. Se salti questo passaggio, OpenVitals usa stime automatiche dai dati di Health Connect e mostra minore attendibilità quando la calibrazione è incerta. Questi valori restano nelle impostazioni di OpenVitals.';

  @override
  String get bodyEnergyCalibrationBirthYear => 'Anno di nascita';

  @override
  String get bodyEnergyCalibrationMaxHr => 'Frequenza cardiaca massima';

  @override
  String get bodyEnergyCalibrationRestingHr => 'Frequenza cardiaca a riposo';

  @override
  String get bodyEnergyCalibrationManualZones => 'Zone cardiache manuali';

  @override
  String get bodyEnergyCalibrationManualZonesBody =>
      'Limiti inferiori opzionali in bpm per le zone 1-5.';

  @override
  String get bodyEnergyCalibrationZone1 => 'Limite bpm inferiore zona 1';

  @override
  String get bodyEnergyCalibrationZone2 => 'Limite bpm inferiore zona 2';

  @override
  String get bodyEnergyCalibrationZone3 => 'Limite bpm inferiore zona 3';

  @override
  String get bodyEnergyCalibrationZone4 => 'Limite bpm inferiore zona 4';

  @override
  String get bodyEnergyCalibrationZone5 => 'Limite bpm inferiore zona 5';

  @override
  String get bodyEnergyCalibrationUseAuto => 'Usa stime automatiche';

  @override
  String get bodyEnergyCalibrationSkip => 'Salta per ora';

  @override
  String get bodyEnergyCalibrationSaved =>
      'Calibrazione energia corporea salvata';

  @override
  String get bodyEnergyCalibrationReset =>
      'Calibrazione energia corporea ripristinata su automatico';

  @override
  String get bodyEnergyNotSetUp => 'Non configurato';

  @override
  String get bodyEnergyTimelineEstimated => 'Stimato da OpenVitals';

  @override
  String get bodyEnergyTimelineCurrent => 'Attuale';

  @override
  String get bodyEnergyTimelineStart => 'Inizio';

  @override
  String get bodyEnergyTimelineCharged => 'Ricaricato';

  @override
  String get bodyEnergyTimelineDrained => 'Consumata';

  @override
  String get bodyEnergyTimelineConfidence => 'Attendibilità';

  @override
  String get bodyEnergyTimelineNoData =>
      'Nessuna cronologia di energia corporea utilizzabile per questo periodo.';

  @override
  String get bodyEnergyTimelineDayTitle => 'Cronologia giornaliera';

  @override
  String get bodyEnergyTimelineLowConfidence =>
      'Alcuni intervalli sono stimati perché calibrazione o dati Health Connect sono incompleti.';

  @override
  String get bodyEnergyWhyTitle => 'Cosa l\'ha spostata';

  @override
  String get bodyEnergyWhyEmpty =>
      'Nessun fattore chiaro di ricarica o consumo ha ancora dominato questa giornata.';

  @override
  String get bodyEnergyInfluenceSleepRecovery => 'Recupero dal sonno';

  @override
  String get bodyEnergyInfluenceQuietRest => 'Riposo tranquillo';

  @override
  String get bodyEnergyInfluenceExertion => 'Sforzo';

  @override
  String get bodyEnergyInfluenceElevatedHr => 'Frequenza cardiaca elevata';

  @override
  String get bodyEnergyInfluenceRecoveryDebt => 'Debito di recupero';

  @override
  String get bodyEnergyInfluenceNoData => 'Nessun dato';

  @override
  String get bodyEnergyInfluenceSteady => 'Stabile';

  @override
  String get bodyEnergyReasonSleepRecoveryDetail =>
      'Gli intervalli di sonno hanno ricaricato la stima dal punteggio precedente.';

  @override
  String get bodyEnergyReasonQuietRestDetail =>
      'Una frequenza bassa da svegli ha aggiunto una piccola ricarica di recupero.';

  @override
  String get bodyEnergyReasonExertionDetail =>
      'L\'intensità cardiaca o gli allenamenti registrati hanno consumato la stima.';

  @override
  String get bodyEnergyReasonElevatedHrDetail =>
      'La frequenza da svegli sopra il riposo ha aggiunto consumo da stress.';

  @override
  String get bodyEnergyReasonRecoveryDebtDetail =>
      'Uno sforzo recente più intenso ha mantenuto attivo un piccolo consumo successivo.';

  @override
  String get bodyEnergyReasonNoDataDetail =>
      'Health Connect non ha fornito segnale sufficiente per questo intervallo.';

  @override
  String get bodyEnergyReasonSteadyDetail =>
      'La stima è rimasta per lo più stabile.';

  @override
  String get bodyEnergyInputsTitle => 'Input usati';

  @override
  String bodyEnergyInputsSummary(int arg0, int arg1) {
    return 'Algoritmo v$arg0, intervalli di $arg1 minuti';
  }

  @override
  String get bodyEnergyInputHeartRate => 'Campioni di frequenza cardiaca';

  @override
  String get bodyEnergyInputSleep => 'Sessioni di sonno';

  @override
  String get bodyEnergyInputWorkouts => 'Allenamenti';

  @override
  String get bodyEnergyInputRestingHr => 'Frequenza cardiaca a riposo';

  @override
  String get bodyEnergyInputHrBaseline => 'Baseline frequenza cardiaca';

  @override
  String get bodyEnergyInputHrv => 'Modificatore HRV';

  @override
  String get bodyEnergyInputRespiratory => 'Modificatore respiratorio';

  @override
  String get bodyEnergyInputPreviousScore => 'Punteggio precedente';

  @override
  String get bodyEnergyInputCalibration => 'Calibrazione';

  @override
  String get bodyEnergyInputAvailable => 'Disponibile';

  @override
  String get bodyEnergyInputMissing => 'Mancante';

  @override
  String get bodyEnergyInputOptional => 'Non presente';

  @override
  String bodyEnergyInputRecords(int arg0) {
    return '$arg0 record';
  }

  @override
  String bodyEnergyInputSessions(int arg0) {
    return '$arg0 sessioni';
  }

  @override
  String bodyEnergyInputWorkoutsValue(int arg0) {
    return '$arg0 allenamenti';
  }

  @override
  String bodyEnergyInputPreviousScoreValue(String arg0) {
    return '$arg0 inizio';
  }

  @override
  String get bodyEnergyCalibrationModeAuto => 'Stime automatiche';

  @override
  String get bodyEnergyCalibrationModeManualValues => 'Valori manuali';

  @override
  String get bodyEnergyCalibrationModeManualZones => 'Zone manuali';

  @override
  String get bodyEnergyCalculationTitle =>
      'Come viene stimata l\'energia corporea';

  @override
  String get bodyEnergyCalculationBody =>
      'OpenVitals divide il giorno selezionato in brevi intervalli, parte dal punteggio precedente disponibile quando possibile, aggiunge ricarica da sonno o riposo tranquillo e sottrae consumo da sforzo, frequenza da svegli elevata e debito di recupero dopo sforzi più intensi.';

  @override
  String get bodyEnergyCalculationInputsBody =>
      'Frequenza cardiaca, frequenza a riposo, zone personali, sonno, allenamenti, HRV e frequenza respiratoria possono migliorare la stima. Gli input mancanti rendono la stima più conservativa e riducono l\'attendibilità.';

  @override
  String get bodyEnergyCalculationLimitsBody =>
      'Questa è una stima di benessere sul dispositivo, non una misurazione diretta o un consiglio medico. Gli input e i motivi mostrati sono esposti per poter rivedere e migliorare il metodo.';

  @override
  String get metricBodyEnergy => 'Energia corporea';

  @override
  String get privacyPolicyTitle => 'Informativa sulla privacy';

  @override
  String get privacyPolicyBody1 =>
      'OpenVitals legge i dati da Health Connect per mostrare passi, allenamenti, sonno, frequenza cardiaca, peso, calorie, idratazione, nutrizione, consapevolezza e parametri vitali sul dispositivo. Le voci registrate esplicitamente, inclusi i percorsi GPX/KML/KMZ importati e i file FIT importati, vengono scritte in Health Connect.';

  @override
  String get privacyPolicyBody2 =>
      'Questa app non carica i dati sanitari su un servizio cloud, non include pubblicità e non condivide dati con terze parti.';

  @override
  String get privacyPolicyBody3 =>
      'OpenVitals non è un dispositivo medico e non diagnostica, tratta, cura né previene alcuna malattia o condizione medica. Non sostituisce il parere medico, la diagnosi o il trattamento di un professionista sanitario qualificato.';

  @override
  String get linkCouldNotOpen => 'Impossibile aprire il link.';

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
