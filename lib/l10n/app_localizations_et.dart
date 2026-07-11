// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Estonian (`et`).
class AppLocalizationsEt extends AppLocalizations {
  AppLocalizationsEt([String locale = 'et']) : super(locale);

  @override
  String get appName => 'OpenVitals';

  @override
  String get cdBack => 'Tagasi';

  @override
  String get cdSettings => 'Seaded';

  @override
  String get cdAchievements => 'Saavutused';

  @override
  String get cdDailyReadiness => 'Igapäevane valmisolek';

  @override
  String get cdSensorBatteryStatus => 'Anduri aku olek';

  @override
  String get cdEditDashboard => 'Muuda kokkuvõtet';

  @override
  String get cdFinishDashboardEditing => 'Lõpeta kokkuvõtte muutmine';

  @override
  String get cdEditSavedDrinks => 'Muuda salvestatud jooke';

  @override
  String get cdDoneEditingSavedDrinks => 'Salvestatud jookide muutmine valmis';

  @override
  String get cdEditDrink => 'Muuda jooki';

  @override
  String get cdDeleteDrink => 'Kustuta jook';

  @override
  String get cdMoveDrinkCategory => 'Liiguta joogi kategooriat';

  @override
  String cdExpandDrinkCategory(String arg0) {
    return 'Laienda $arg0';
  }

  @override
  String cdCollapseDrinkCategory(String arg0) {
    return 'Ahenda $arg0';
  }

  @override
  String get cdEditManualEntryWidgets => 'Muuda kirje lisamise vidinaid';

  @override
  String get cdFinishManualEntryEditing =>
      'Lõpeta kirje lisamise vidinate muutmine';

  @override
  String get cdEditRecordingDashboard => 'Muuda salvestuse töölauda';

  @override
  String get cdFinishRecordingDashboardEditing =>
      'Lõpeta salvestuse töölaua muutmine';

  @override
  String get cdMoveWidgetUp => 'Liiguta vidin üles';

  @override
  String get cdMoveWidgetDown => 'Liiguta vidin alla';

  @override
  String get cdEditMetricSections => 'Muuda näitajate jaotisi';

  @override
  String get cdFinishMetricSectionEditing =>
      'Lõpeta näitajate jaotiste muutmine';

  @override
  String get cdMoveSectionUp => 'Liiguta jaotis üles';

  @override
  String get cdMoveSectionDown => 'Liiguta jaotis alla';

  @override
  String get cdRemoveWidget => 'Eemalda vidin';

  @override
  String get cdDecreaseRecordingDashboardWidgetSize => 'Vähenda vidina suurust';

  @override
  String get cdIncreaseRecordingDashboardWidgetSize =>
      'Suurenda vidina suurust';

  @override
  String get cdExitRecordingFocusMode => 'Välju fookusrežiimist';

  @override
  String get cdToggleRecordingOutdoorMode =>
      'Lülita välitingimuste loetavusrežiim sisse või välja';

  @override
  String get cdRecenterMap => 'Keskenda kaart uuesti';

  @override
  String get cdDeleteEntry => 'Kustuta kirje';

  @override
  String get cdEditEntry => 'Muuda kirjet';

  @override
  String get cdPreviousDay => 'Eelmine päev';

  @override
  String get cdNextDay => 'Järgmine päev';

  @override
  String get cdPreviousPeriod => 'Eelmine periood';

  @override
  String get cdNextPeriod => 'Järgmine periood';

  @override
  String get cdOpenCalendar => 'Ava kalender';

  @override
  String get actionCancel => 'Tühista';

  @override
  String get actionAdd => 'Lisa';

  @override
  String get actionAddCustom => 'Lisa kohandatud';

  @override
  String get actionSave => 'Salvesta';

  @override
  String get actionClose => 'Sulge';

  @override
  String get actionContinue => 'Jätka';

  @override
  String get actionDelete => 'Kustuta';

  @override
  String get actionDetails => 'Üksikasjad';

  @override
  String get actionEdit => 'Muuda';

  @override
  String get actionEnable => 'Luba';

  @override
  String get actionGetStarted => 'Alusta';

  @override
  String get actionGrant => 'Anna luba';

  @override
  String get actionGrantPermission => 'Anna luba';

  @override
  String get actionLoadMoreEntries => 'Laadi 10 lisaks';

  @override
  String get actionShowCalculation => 'Näita arvutust';

  @override
  String get actionHideCalculation => 'Peida arvutus';

  @override
  String get actionNotNow => 'Mitte praegu';

  @override
  String get actionAccept => 'Nõustun';

  @override
  String get actionOpen => 'Ava';

  @override
  String get actionPause => 'Peata';

  @override
  String get actionReview => 'Vaata üle';

  @override
  String get actionResume => 'Jätka';

  @override
  String get actionRefresh => 'Värskenda';

  @override
  String get actionSelect => 'Vali';

  @override
  String get actionStart => 'Alusta';

  @override
  String get actionFinish => 'Lõpeta';

  @override
  String get actionDiscard => 'Loobu';

  @override
  String get unknownError => 'Tundmatu viga';

  @override
  String get screenErrorNotFound => 'Soovitud kirjet ei leitud.';

  @override
  String get screenErrorMissingArgument => 'Vajalik teave puudub.';

  @override
  String get screenErrorPermissionDenied =>
      'Nende andmete laadimiseks on vaja luba.';

  @override
  String get screenErrorHealthConnectUnavailable =>
      'Health Connect ei ole selles seadmes saadaval.';

  @override
  String get screenErrorLoadSleepSession =>
      'Unesessiooni ei õnnestunud laadida.';

  @override
  String get screenErrorLoadSleepPeriod => 'Uneandmeid ei õnnestunud laadida.';

  @override
  String get notAvailable => 'Ei ole saadaval';

  @override
  String get notRecorded => 'Salvestamata';

  @override
  String get noData => 'Andmed puuduvad';

  @override
  String get loading => 'Laadimine...';

  @override
  String get homeMetricWidgetDescription => 'OpenVitalsi näitaja';

  @override
  String get homeMetricWidgetConfigTitle => 'Vali näitaja';

  @override
  String get homeMetricWidgetConfigPrompt => 'Vali vidina jaoks näitaja:';

  @override
  String get homeMetricWidgetNoMetrics => 'Näitajad puuduvad.';

  @override
  String get homeMetricWidgetPermissionNeeded =>
      'Anna luba rakenduses OpenVitals';

  @override
  String get homeMetricWidgetUpdateFailed => 'Värskendamine ebaõnnestus';

  @override
  String get homeMetricWidgetOpenForDetails => 'Ava üksikasjade nägemiseks';

  @override
  String get homeMetricWidgetNotConfigured => 'Vali näitaja';

  @override
  String get homeQuickBeverageWidgetDescription => 'Kiirjook';

  @override
  String get homeQuickBeverageOneTapWidgetDescription => 'Kiirjook 1x1';

  @override
  String get homeQuickBeverageWidgetConfigTitle => 'Vali jook';

  @override
  String get homeQuickBeverageWidgetConfigPrompt => 'Vali vidina jaoks jook:';

  @override
  String get homeQuickBeverageWidgetNoDrinks => 'Jooke ei ole saadaval.';

  @override
  String get homeQuickBeverageWidgetNotConfigured => 'Vali jook';

  @override
  String get homeQuickBeverageWidgetTapToLog => 'Puuduta lisamiseks';

  @override
  String get homeQuickBeverageWidgetSaved => 'Salvestatud';

  @override
  String get homeQuickBeverageWidgetSavedNutrition => 'Salvestatud toitumisena';

  @override
  String get homeDailyReadinessWidgetDescription =>
      'OpenVitalsi igapäevane valmisolek';

  @override
  String get homeBodyEnergyWidgetDescription => 'OpenVitalsi kehaenergia';

  @override
  String get homeTodayVitalsWidgetDescription =>
      'OpenVitalsi tänased elunäitajad';

  @override
  String get homeWidgetTodayTitle => 'Täna';

  @override
  String get homeWidgetContext => 'Kontekst';

  @override
  String get homeWidgetHrvShort => 'HRV';

  @override
  String get homeWidgetBodyEnergyCharged => 'Laetud';

  @override
  String get homeWidgetBodyEnergySteady => 'Stabiilne';

  @override
  String get homeWidgetBodyEnergyLimited => 'Piiratud';

  @override
  String get homeWidgetBodyEnergyLow => 'Madal';

  @override
  String get screenSteps => 'Sammud';

  @override
  String get screenActivities => 'Treeningud';

  @override
  String get screenCalories => 'Kalorid';

  @override
  String get screenActivityDetail => 'Treeningu üksikasjad';

  @override
  String get screenSleep => 'Uni';

  @override
  String get screenSleepDetail => 'Une üksikasjad';

  @override
  String get screenHeartVitals => 'Süda ja elunäitajad';

  @override
  String get screenStressTracking => 'Stressi jälgimine';

  @override
  String get screenBodyEnergy => 'Kehaenergia';

  @override
  String get screenTrainingReadiness => 'Treeningvalmisolek';

  @override
  String get screenBody => 'Keha';

  @override
  String get screenHydration => 'Joogid';

  @override
  String get screenNutrition => 'Toitumine';

  @override
  String get screenMindfulness => 'Teadvelolek';

  @override
  String get screenCycle => 'Tsükkel';

  @override
  String get screenDailyReadiness => 'Igapäevane valmisolek';

  @override
  String get screenSettings => 'Seaded';

  @override
  String get screenAchievements => 'Saavutused';

  @override
  String get screenManualEntry => 'Lisa kirje';

  @override
  String get screenHydrationEntry => 'Joogi kirje';

  @override
  String get screenActivityEntry => 'Treeningu kirje';

  @override
  String get screenMindfulnessEntry => 'Teadveloleku kirje';

  @override
  String get screenCarbsEntry => 'Süsivesikute kirje';

  @override
  String get screenBodyMeasurementEntry => 'Kehamõõtmise kirje';

  @override
  String get screenVitalsMeasurementEntry => 'Elunäitajate kirje';

  @override
  String get bottomNavDashboard => 'Kokkuvõte';

  @override
  String get manualEntryHydrationTitle => 'Joogid';

  @override
  String get manualEntryActivityTitle => 'Treening';

  @override
  String get manualEntryDateLabel => 'Kirje kuupäev';

  @override
  String get manualEntryTimeLabel => 'Kirje kellaaeg';

  @override
  String get manualEntrySelectTime => 'Vali kirje kellaaeg';

  @override
  String get manualEntryAddWidgets => 'Lisa kirje vidinaid';

  @override
  String get manualEntryAllWidgetsAdded =>
      'Kõik kirje vidinad on juba lisatud.';

  @override
  String get manualEntryWritePermissionTitle => 'Joogi kirjutamisluba';

  @override
  String get manualEntryActivityWritePermissionTitle =>
      'Treeningu kirjutamisload';

  @override
  String get manualEntryMindfulnessWritePermissionTitle =>
      'Teadveloleku kirjutamisluba';

  @override
  String get manualEntryCarbsWritePermissionTitle =>
      'Süsivesikute kirjutamisluba';

  @override
  String manualEntryBodyWritePermissionTitle(String arg0) {
    return '$arg0 kirjutamisluba';
  }

  @override
  String manualEntryVitalsWritePermissionTitle(String arg0) {
    return '$arg0 kirjutamisluba';
  }

  @override
  String get mindfulnessEntrySubtitle =>
      'Teadveloleku seansid salvestatakse otse Health Connectisse.';

  @override
  String get mindfulnessEntryPermissionNeeded =>
      'Kokkuvõtte jaoks küsib OpenVitals ainult vaatamisõigust. Teadveloleku kirjete lisamiseks vajame kirjutamisluba. OpenVitals ei salvesta neid seansse ise; kirjed salvestatakse Health Connectisse.';

  @override
  String get activityEntrySubtitle =>
      'Loo Health Connecti treeningsessioon. Imporditud marsruut või treeningu üksikasjad kirjutatakse alles salvestamisel.';

  @override
  String get activityEntryPermissionNeeded =>
      'Kokkuvõtte jaoks küsib OpenVitals ainult vaatamisõigust. Treeningute lisamiseks vajame Health Connecti kirjutamisõigust sessioonidele, marsruutidele, vahemaale, kõrgusele, kaloritele ja salvestatud anduri näitajatele, näiteks pulsile; jooksulindi kirjed küsivad vajadusel samme. OpenVitals ei salvesta neid kirjeid ise; need salvestatakse Health Connectisse.';

  @override
  String get activityEntrySourceBody =>
      'Loo treening käsitsi, kasuta olemasolevat plaani või salvesta GPS-marsruut.';

  @override
  String get activityEntryCreateManual => 'Loo käsitsi';

  @override
  String get activityEntryCreateFromExistingPlan =>
      'Loo olemasolevast plaanist';

  @override
  String get activityEntryRecordGps => 'Salvesta treening';

  @override
  String get activityEntryChooseAnotherSource => 'Vali teine meetod';

  @override
  String get activityEntryTypeLabel => 'Treeningu liik';

  @override
  String get activityEntryTitleLabel => 'Pealkiri';

  @override
  String get activityEntryStartDateLabel => 'Alguskuupäev';

  @override
  String get activityEntryStartTimeLabel => 'Algusaeg';

  @override
  String get activityEntrySelectTime => 'Vali algusaeg';

  @override
  String get activityEntryDurationLabel => 'Kestus min';

  @override
  String get activityEntryRepetitionsTitle => 'Kordused';

  @override
  String get activityEntryStepsTitle => 'Sammud';

  @override
  String get activityEntryRepetitionModeTotal => 'Kokku';

  @override
  String get activityEntryRepetitionModeSets => 'Seeriad';

  @override
  String get activityEntryRepetitionsLabel => 'Kordused';

  @override
  String get activityEntryStepsLabel => 'Sammud';

  @override
  String activityEntrySetRepetitionsLabel(int arg0) {
    return 'Seeria $arg0 kordust';
  }

  @override
  String get activityEntrySetRestLabel => 'Puhkeaeg';

  @override
  String get activityEntryAddSet => 'Lisa seeria';

  @override
  String get activityEntryTrainingPlansTitle => 'Treeningplaanid';

  @override
  String get activityEntryTrainingPlansLoading =>
      'Health Connecti plaanide laadimine';

  @override
  String get activityEntryTrainingPlansEmpty =>
      'Selle kuupäeva ja treeningu liigi jaoks Health Connecti plaane ei ole';

  @override
  String get activityEntryTrainingPlanLabel => 'Treeningplaan';

  @override
  String get activityEntryTrainingPlanSelect => 'Vali plaan';

  @override
  String get activityEntryTrainingPlanNew => 'Uus plaan';

  @override
  String get activityEntryTrainingPlanUnnamed => 'Nimetu plaan';

  @override
  String get activityEntrySaveTrainingPlan => 'Salvesta plaan';

  @override
  String get activityEntryUpdateTrainingPlan => 'Uuenda plaani';

  @override
  String get activityEntryPlanActivityPickerTitle => 'Plaaniga treeningud';

  @override
  String get activityEntryPlanActivityPickerEmpty =>
      'Health Connecti plaane ei leitud';

  @override
  String get activityEntryPlanPickerTitle => 'Vali plaan';

  @override
  String get activityEntryPlanPickerEmpty =>
      'Selle treeningu jaoks plaane ei leitud';

  @override
  String get activityEntryPlanChooseActivity => 'Vali teine treening';

  @override
  String activityEntryPlanOneSetSummary(int arg0) {
    return '1 seeria • $arg0 kordust';
  }

  @override
  String activityEntryPlanSummary(int arg0, int arg1) {
    return '$arg0 seeriat • $arg1 kordust';
  }

  @override
  String activityEntryPlanPreviewReps(int arg0) {
    return '$arg0 kordust';
  }

  @override
  String activityEntryPlanPreviewRest(int arg0) {
    return 'puhkus $arg0 s';
  }

  @override
  String activityEntryPlanPreviewMore(int arg0) {
    return '+$arg0 veel';
  }

  @override
  String activityEntryDistanceLabel(String arg0) {
    return 'Vahemaa $arg0';
  }

  @override
  String activityEntryElevationLabel(String arg0) {
    return 'Tõus $arg0';
  }

  @override
  String get activityEntryNotesLabel => 'Märkmed';

  @override
  String get activityEntryFeelingLabel => 'Milline tunne oli?';

  @override
  String get activityEntryFeelingGreat => 'Suurepärane';

  @override
  String get activityEntryFeelingGood => 'Hea';

  @override
  String get activityEntryFeelingHard => 'Raske';

  @override
  String get activityEntryFeelingRough => 'Kohutav';

  @override
  String get activityEntryImportedRoute => 'Imporditud marsruut';

  @override
  String get activityEntryRecordingTitle => 'Treeningu salvestamine';

  @override
  String get activityEntryRecordingReadyBody =>
      'Vali treeningu liik ja alusta, kui oled valmis. Pärast lõpetamist saad üksikasjad üle vaadata ja lisada enne salvestamist.';

  @override
  String get activityEntryRecordingGoToActivityScreen => 'Ava treeningu ekraan';

  @override
  String get activityEntryRecordingActive => 'Salvestamine';

  @override
  String get activityEntryRecordingPaused => 'Peatatud';

  @override
  String get activityEntryRecordingIdle => 'Passiivne';

  @override
  String get activityEntryRecordingResting => 'Puhkab';

  @override
  String get activityEntryRecordingGpsFix => 'GPS valmis';

  @override
  String get activityEntryRecordingGpsPoor => 'Nõrk GPS';

  @override
  String get activityEntryRecordingGpsLost => 'GPS kadunud';

  @override
  String get activityEntryRecordingGpsOff => 'GPS väljas';

  @override
  String get activityEntryRecordingTabMap => 'Kaart';

  @override
  String get activityEntryRecordingTabStats => 'Statistika';

  @override
  String get activityEntryRecordingTabIntervals => 'Intervallid';

  @override
  String get activityEntryRecordingTabByTime => 'Aja järgi';

  @override
  String get activityEntryRecordingTabByDistance => 'Vahemaa järgi';

  @override
  String get activityEntryRecordingTimeSplit => 'Ajavahemik';

  @override
  String get activityEntryRecordingDistanceSplit => 'Vahemaa lõik';

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
  String get activityEntryRecordingSplitElapsed => 'Kulunud';

  @override
  String get activityEntryRecordingSplitAvg => 'Keskm';

  @override
  String get activityEntryRecordingSplitMax => 'Maks';

  @override
  String get activityEntryRecordingNoIntervals => 'Intervalle veel pole';

  @override
  String get activityEntryRecordingNoTimeSplits => 'Ajalõike veel pole';

  @override
  String get activityEntryRecordingNoDistanceSplits =>
      'Vahemaa lõike veel pole';

  @override
  String get activityEntryRecordingLap => 'Ring';

  @override
  String get activityEntryRecordingMarker => 'Lisa marker';

  @override
  String activityEntryRecordingMarkerDefaultName(int arg0) {
    return 'Marker $arg0';
  }

  @override
  String get activityEntryRecordingMarkersTitle => 'Markerid';

  @override
  String get activityEntryRecordingMarkerName => 'Nimi';

  @override
  String get activityEntryRecordingMarkerNote => 'Märkus';

  @override
  String get activityEntryRecordingWaitingForGps => 'Oota täpset GPS-signaali';

  @override
  String get activityEntryRecordingGpsWaiting =>
      'Enne alustamist oota täpset GPS-signaali.';

  @override
  String activityEntryRecordingGpsWaitingAccuracy(String arg0) {
    return 'Ootan paremat GPS-täpsust • $arg0';
  }

  @override
  String activityEntryRecordingGpsReady(String arg0) {
    return 'GPS valmis • täpsus $arg0';
  }

  @override
  String get activityEntryRecordingGpsDisabled =>
      'Lülita salvestamise alustamiseks GPS sisse.';

  @override
  String get activityEntryRecordingDistance => 'Vahemaa';

  @override
  String get activityEntryRecordingTotalTime => 'Koguaeg';

  @override
  String get activityEntryRecordingMovingTime => 'Liikumisaeg';

  @override
  String get activityEntryRecordingRestTime => 'Puhkeaeg';

  @override
  String get activityEntryRecordingSpeed => 'Kiirus';

  @override
  String get activityEntryRecordingMaxSpeed => 'Maks kiirus';

  @override
  String get activityEntryRecordingAverageSpeed => 'Keskm kiirus';

  @override
  String get activityEntryRecordingAverageMovingSpeed => 'Keskm liikumiskiirus';

  @override
  String get activityEntryRecordingElevationGain => 'Tõus';

  @override
  String get activityEntryRecordingPoints => 'Punktid';

  @override
  String get activityEntryRecordingRestSecondsLabel => 'Puhkesekundid';

  @override
  String get activityEntryRecordingEndSet => 'Lõpeta seeria';

  @override
  String get activityEntryRecordingStartNextSet => 'Alusta järgmist seeriat';

  @override
  String get activityEntryRecordingEndSession => 'Lõpeta sessioon';

  @override
  String activityEntryRecordingRestRemaining(String arg0) {
    return 'Puhkust jäänud $arg0';
  }

  @override
  String get activityEntryRecordingFinishHint =>
      'Lõpetamine avab treeningu üksikasjade vormi, kus saad lisada pealkirja, märkmeid, kaloreid või väärtusi kohandada enne salvestamist.';

  @override
  String get activityEntryRecordingRepetitionCorrectionHint =>
      'Kasuta + või -, kui andur jätab korduse vahele või lisab liiga palju.';

  @override
  String activityEntryRecordingAccuracy(String arg0) {
    return 'Viimane täpsus $arg0';
  }

  @override
  String get activityEntryRecordingFocus => 'Fookus';

  @override
  String get activityEntryRecordingDashboardLayout => 'Töölaua paigutus';

  @override
  String get activityEntryRecordingDashboardLayoutTwoByFour => '2x4';

  @override
  String get activityEntryRecordingDashboardLayoutThreeByFour => '3x4';

  @override
  String get activityEntryRecordingDashboardLayoutLargeTop => 'Suur ülal';

  @override
  String get activityEntryRecordingDashboardAddField => 'Lisa vidin';

  @override
  String activityEntryRouteSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  ) {
    return '$arg0 • $arg1 • $arg2 tõusu • $arg3 punkti';
  }

  @override
  String activityEntryRouteAverageMetrics(String arg0, String arg1) {
    return 'Keskm tempo $arg0 • keskm kiirus $arg1';
  }

  @override
  String get activityEntryAdd => 'Salvesta treening';

  @override
  String get activityEntryInvalidValue =>
      'Enne treeningu salvestamist paranda esiletõstetud väljad.';

  @override
  String get activityEntryErrorActivityTypeRoute =>
      'Vali treeningu liik, mis toetab GPS-marsruute.';

  @override
  String get activityEntryErrorTrainingPlanTitleRequired =>
      'Selle treeningplaani salvestamiseks sisesta pealkiri.';

  @override
  String get activityEntryErrorStartDate => 'Vali kehtiv alguskuupäev.';

  @override
  String get activityEntryErrorStartTime => 'Vali kehtiv algusaeg.';

  @override
  String get activityEntryErrorStartTimeAfterRoute =>
      'Algusaeg peab olema enne imporditud marsruudi algust või sellega samal ajal.';

  @override
  String get activityEntryErrorDuration =>
      'Kestus peab olema 1 minuti ja 7 päeva vahel.';

  @override
  String get activityEntryErrorRepetitions =>
      'Sisesta positiivsed arvud. Puhkus peab mahtuma treeningu kestuse sisse.';

  @override
  String get activityEntryErrorDistance =>
      'Sisesta vahemaa, mis on suurem kui 0.';

  @override
  String get activityEntryErrorDistanceUnsupported =>
      'See treeningu liik ei toeta vahemaad.';

  @override
  String get activityEntryErrorElevation =>
      'Sisesta tõus, mis on suurem kui 0.';

  @override
  String get activityEntryErrorElevationUnsupported =>
      'See treeningu liik ei toeta tõusu.';

  @override
  String get activityEntryErrorActiveCalories =>
      'Sisesta aktiivsed kalorid, mis on suuremad kui 0.';

  @override
  String get activityEntryErrorTotalCalories =>
      'Sisesta kalorid kokku, mis on suuremad kui 0.';

  @override
  String get activityEntryErrorTotalCaloriesBelowActive =>
      'Kalorid kokku ei saa olla väiksemad kui aktiivsed kalorid.';

  @override
  String get activityEntryLocationPermissionNeeded =>
      'GPS-treeningute salvestamiseks on vaja täpse asukoha luba.';

  @override
  String get activityEntryNotificationPermissionNeeded =>
      'Teavitusluba on vajalik, et OpenVitals saaks näidata käimasoleva salvestuse teavitust.';

  @override
  String get activityEntryActivityRecognitionPermissionNeeded =>
      'Jooksulindi sammude lugemiseks on vaja treeningu tuvastamise luba.';

  @override
  String activityEntryRouteImportFailed(String arg0) {
    return 'Treeningufaili ei õnnestunud importida: $arg0';
  }

  @override
  String activityEntryRecordingFailed(String arg0) {
    return 'Treeningut ei õnnestunud salvestada: $arg0';
  }

  @override
  String activityEntryWriteFailed(String arg0) {
    return 'Treeningu kirjet ei õnnestunud kirjutada: $arg0';
  }

  @override
  String get activityRouteOpenInMap => 'Ava marsruut kaardirakenduses';

  @override
  String get activityRouteExportGpx => 'Salvesta GPX';

  @override
  String get activityRouteExportKmz => 'Salvesta KMZ';

  @override
  String get activityRouteExportSaved => 'Marsruut salvestatud.';

  @override
  String get activityRouteExportFailed =>
      'Marsruudifaili ei õnnestunud salvestada.';

  @override
  String get activityRouteOpenChooserTitle => 'Ava marsruut rakendusega';

  @override
  String get activityRouteOpenFailed =>
      'Ükski kaardirakendus ei suutnud seda marsruuti avada.';

  @override
  String get activityDetailAnalysisTitle => 'Marsruudi analüüs';

  @override
  String get activityDetailTabMarkers => 'Markerid';

  @override
  String get activityDetailNoMarkers => 'Markereid veel pole';

  @override
  String activityRecordingVoiceSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  ) {
    return 'Aeg $arg0. Vahemaa $arg1. Keskmine kiirus $arg2. Praegune ring $arg3.';
  }

  @override
  String activityRecordingVoiceLap(int arg0, String arg1) {
    return 'Ring $arg0. $arg1';
  }

  @override
  String get activityRecordingVoiceIdle => 'Passiivne.';

  @override
  String get activityRecordingVoiceResumed => 'Salvestamine jätkub.';

  @override
  String get activityRecordingNotificationChannel => 'Treeningu salvestamine';

  @override
  String get activityRecordingNotificationTitle => 'Treeningu salvestamine';

  @override
  String activityRecordingNotificationRecording(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return 'Salvestamine • $arg0 kokku • $arg1 liikumist • $arg2 • $arg3';
  }

  @override
  String activityRecordingNotificationPaused(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return 'Peatatud • $arg0 kokku • $arg1 liikumist • $arg2 • $arg3';
  }

  @override
  String activityRecordingNotificationRepetitionRecording(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Salvestamine • $arg0 kokku • $arg1 $arg2';
  }

  @override
  String activityRecordingNotificationRepetitionPaused(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Peatatud • $arg0 kokku • $arg1 $arg2';
  }

  @override
  String activityRecordingNotificationRepetitionResting(
    String arg0,
    String arg1,
  ) {
    return 'Puhkab • $arg0 kokku • $arg1 jäänud';
  }

  @override
  String activityRecordingNotificationTimedRecording(String arg0) {
    return 'Salvestamine • $arg0 kokku';
  }

  @override
  String activityRecordingNotificationTimedPaused(String arg0) {
    return 'Peatatud • $arg0 kokku';
  }

  @override
  String get activityRecordingErrorService =>
      'Treeningu salvestusteenust ei õnnestunud käivitada.';

  @override
  String get activityRecordingErrorPreciseLocationPermission =>
      'Usaldusväärsete GPS-jälgede jaoks on vaja täpse asukoha luba.';

  @override
  String get activityRecordingErrorNotificationPermission =>
      'Käimasoleva salvestuse teavituse näitamiseks on vaja teavitusluba.';

  @override
  String get activityRecordingErrorActivityRecognitionPermission =>
      'Jooksulindi sammude lugemiseks on vaja treeningu tuvastamise luba.';

  @override
  String get activityRecordingErrorWaitingForGps =>
      'Enne alustamist oota täpset GPS-signaali.';

  @override
  String get activityRecordingErrorProvider =>
      'Lülita marsruudi salvestamiseks GPS sisse.';

  @override
  String get activityRecordingErrorUnsupportedType =>
      'Seda treeningu liiki ei saa reaalajas salvestada.';

  @override
  String get activityRecordingErrorProximitySensor =>
      'Sellel seadmel ei ole kätesurvete lugemiseks lähedusandurit.';

  @override
  String get activityRecordingErrorAccelerometer =>
      'Sellel seadmel ei ole selle salvestuse jaoks kiirendusandurit.';

  @override
  String get activityRecordingErrorStepDetector =>
      'Sellel seadmel ei ole Androidi sammuloenduri sündmusi.';

  @override
  String get activityRecordingHowItWorks => 'Kuidas salvestamine toimib';

  @override
  String get activityRecordingGuidancePushUps =>
      'Aseta telefoni ekraan üleval rindkere või pea alla. Lähedusandur loeb korduse, kui liigud telefonile lähedale.';

  @override
  String get activityRecordingGuidancePullUps =>
      'Kinnita telefon oma keha külge. Kiirendusandur loeb tõmbe- ja lõdvestusliigutused.';

  @override
  String get activityRecordingGuidanceRopeSkipping =>
      'Hoia telefon oma keha küljes kinnitatuna. Kiirendusandur loeb hüpped.';

  @override
  String get activityRecordingGuidanceTrampolineJumping =>
      'Hoia telefon oma keha küljes kinnitatuna. Hüppetuvastus kasutab nöörihüppest pikemat hüppeakent.';

  @override
  String get activityRecordingGuidanceTreadmill =>
      'Kanna telefoni kaasas. Androidi sammuloendur loeb sammud; GPS-marsruuti ei salvestata.';

  @override
  String get activityRecordingSensorReady => 'Andur valmis';

  @override
  String get activityRecordingSensorUnavailableManual =>
      'Reaalajas loendamine ei ole selles seadmes saadaval. Käsitsi sisestamine on siiski võimalik.';

  @override
  String get activityRecordingActivityRecognitionMissing =>
      'Jooksulindi sammude lugemiseks anna treeningu tuvastamise luba.';

  @override
  String get exerciseTypeRunning => 'Jooksmine';

  @override
  String get exerciseTypeBiking => 'Rattasõit';

  @override
  String get exerciseTypeWalking => 'Kõndimine';

  @override
  String get exerciseTypeHiking => 'Matkamine';

  @override
  String get exerciseTypeWheelchair => 'Ratastool';

  @override
  String get exerciseTypeRowing => 'Aerutamine';

  @override
  String get exerciseTypePaddling => 'Aerutamine (kanuu/kajakk)';

  @override
  String get exerciseTypeSkiing => 'Suusatamine';

  @override
  String get exerciseTypeSnowboarding => 'Lumelauasõit';

  @override
  String get exerciseTypeSnowshoeing => 'Lumereketel käimine';

  @override
  String get exerciseTypeSkating => 'Uisutamine';

  @override
  String get exerciseTypeSailing => 'Purjetamine';

  @override
  String get exerciseTypeSurfing => 'Lainelaudasõit';

  @override
  String get exerciseTypeSwimmingOpenWater => 'Ujumine (avavesi)';

  @override
  String get exerciseTypeGolf => 'Golf';

  @override
  String get exerciseTypeStrengthTraining => 'Jõutreening';

  @override
  String get exerciseTypeTreadmill => 'Jooksulint';

  @override
  String get exerciseTypePushUps => 'Kätekõverdused';

  @override
  String get exerciseTypePullUps => 'Tõmbed';

  @override
  String get exerciseTypeRopeSkipping => 'Nööri hüppamine';

  @override
  String get exerciseTypeTrampolineJumping => 'Trampoliinihüpped';

  @override
  String get exerciseTypeOtherWorkout => 'Muu treening';

  @override
  String get mindfulnessEntryUnavailable =>
      'Teadveloleku seansid ei ole selles Health Connecti pakkujas saadaval.';

  @override
  String get mindfulnessEntryTimerTitle => 'Taimer';

  @override
  String get mindfulnessEntryManualTitle => 'Käsitsi sisestamine';

  @override
  String get mindfulnessEntryIntervalBell => 'Intervallikell';

  @override
  String get mindfulnessEntryIntervalMinutes => 'Intervall (min)';

  @override
  String get mindfulnessEntryBellSound => 'Kella heli';

  @override
  String get mindfulnessEntryBackgroundSound => 'Taustaheli';

  @override
  String get mindfulnessBellStruck => 'Pehme löök';

  @override
  String get mindfulnessBellRubbed => 'Soe kauss';

  @override
  String get mindfulnessBellBright => 'Hele kauss';

  @override
  String get mindfulnessBellTemple => 'Templikauss';

  @override
  String get mindfulnessBellHarmony => 'Harmoonia';

  @override
  String get mindfulnessBackgroundNone => 'Puudub';

  @override
  String get mindfulnessBackgroundBowl => 'Kauss';

  @override
  String get mindfulnessBackgroundMeditation => 'Meditatsioon';

  @override
  String get mindfulnessBackgroundChimes => 'Tuulekellad';

  @override
  String get mindfulnessBackgroundDreamscape => 'Unenäomaastik';

  @override
  String get mindfulnessEntryStartTimer => 'Alusta';

  @override
  String get mindfulnessEntryStopTimer => 'Peata';

  @override
  String get mindfulnessEntryResumeTimer => 'Jätka';

  @override
  String get mindfulnessEntryDiscardTimer => 'Loobu';

  @override
  String get mindfulnessEntrySaveSession => 'Salvesta seanss';

  @override
  String get mindfulnessEntryMinutes => 'Minutid';

  @override
  String get mindfulnessEntryAddMinutes => 'Lisa minuteid';

  @override
  String get mindfulnessEntryInvalidTimer =>
      'Sisesta kehtiv taimeri kestus ja intervall.';

  @override
  String get mindfulnessEntryInvalidManual =>
      'Sisesta kehtivad teadveloleku minutid.';

  @override
  String get mindfulnessEntryTimerTooShort =>
      'Meditatsioon peab salvestamiseks kestma vähemalt 1 minuti.';

  @override
  String mindfulnessEntryWriteFailed(String arg0) {
    return 'Teadveloleku seanssi ei õnnestunud salvestada: $arg0';
  }

  @override
  String get mindfulnessEntryCompleted => 'Taimer valmis';

  @override
  String get mindfulnessRemindersTitle => 'Teadveloleku meeldetuletused';

  @override
  String get mindfulnessRemindersSummaryOff =>
      'Vaikimisi väljas. Luba kord päevas meeldetuletus oma teadveloleku eesmärgi jaoks.';

  @override
  String mindfulnessRemindersSummaryOn(String arg0) {
    return 'Iga päev kell $arg0';
  }

  @override
  String get mindfulnessRemindersPermissionNeeded =>
      'Teadveloleku meeldetuletuste lubamiseks anna teavitusluba.';

  @override
  String get mindfulnessRemindersTime => 'Meeldetuletuse aeg';

  @override
  String get mindfulnessRemindersGoalNote =>
      'Meeldetuletused peatuvad, kui tänane teadveloleku eesmärk on täidetud, ja jätkuvad homme.';

  @override
  String get mindfulnessReminderNotificationChannel =>
      'Teadveloleku meeldetuletused';

  @override
  String get mindfulnessReminderNotificationChannelDesc =>
      'Valikulised meeldetuletused igapäevase teadveloleku eesmärgi täitmiseks.';

  @override
  String get mindfulnessReminderNotificationTitle =>
      'Teadveloleku meeldetuletus';

  @override
  String mindfulnessReminderNotificationBody(String arg0) {
    return 'Sinu tänane eesmärk on $arg0. Tee teadvel hetk, kui saad.';
  }

  @override
  String mindfulnessReminderNotificationProgress(String arg0, String arg1) {
    return '$arg0 / $arg1';
  }

  @override
  String bodyEntrySubtitle(String arg0) {
    return '$arg0 kirjed salvestatakse otse Health Connectisse.';
  }

  @override
  String bodyEntryPermissionNeeded(String arg0) {
    return '$arg0 kirjete lisamiseks vajab OpenVitals Health Connecti kirjutamisõigust. Rakendus ei salvesta neid andmeid ise; kirjed salvestatakse Health Connectisse.';
  }

  @override
  String bodyEntryValueLabel(String arg0, String arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String bodyEntryAddSelected(String arg0) {
    return 'Lisa $arg0';
  }

  @override
  String get bodyEntryInvalidValue =>
      'Sisesta selle mõõtmise jaoks kehtiv väärtus.';

  @override
  String bodyEntryWriteFailed(String arg0) {
    return 'Kehamõõtmist ei õnnestunud salvestada: $arg0';
  }

  @override
  String get carbsEntrySubtitle =>
      'Süsivesikute kirjed salvestatakse otse Health Connectisse.';

  @override
  String get carbsEntryPermissionNeeded =>
      'Süsivesikute kirjete lisamiseks vajab OpenVitals Health Connecti kirjutamisõigust. Rakendus ei salvesta neid andmeid ise; kirjed salvestatakse Health Connectisse.';

  @override
  String carbsEntryValueLabel(String arg0) {
    return 'Süsivesikud ($arg0)';
  }

  @override
  String get carbsEntryAdd => 'Lisa süsivesikud';

  @override
  String get carbsEntryInvalidValue => 'Sisesta kehtiv süsivesikute kogus.';

  @override
  String carbsEntryWriteFailed(String arg0) {
    return 'Süsivesikuid ei õnnestunud salvestada: $arg0';
  }

  @override
  String vitalsEntrySubtitle(String arg0) {
    return '$arg0 kirjed salvestatakse otse Health Connectisse.';
  }

  @override
  String vitalsEntryPermissionNeeded(String arg0) {
    return '$arg0 kirjete lisamiseks vajab OpenVitals Health Connecti kirjutamisõigust. Rakendus ei salvesta neid andmeid ise; kirjed salvestatakse Health Connectisse.';
  }

  @override
  String vitalsEntryValueLabel(String arg0, String arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String get vitalsEntrySystolicLabel => 'Süstoolne (mmHg)';

  @override
  String get vitalsEntryDiastolicLabel => 'Diastoolne (mmHg)';

  @override
  String vitalsEntryAddSelected(String arg0) {
    return 'Lisa $arg0';
  }

  @override
  String get vitalsEntryInvalidValue =>
      'Sisesta selle näitaja jaoks kehtiv väärtus.';

  @override
  String vitalsEntryWriteFailed(String arg0) {
    return 'Näitajat ei õnnestunud salvestada: $arg0';
  }

  @override
  String get rangeDay => 'Päev';

  @override
  String get rangeWeek => 'Nädal';

  @override
  String get rangeMonth => 'Kuu';

  @override
  String get rangeYear => 'Aasta';

  @override
  String get periodToday => 'Täna';

  @override
  String get periodYesterday => 'Eile';

  @override
  String get periodThisWeek => 'Sel nädalal';

  @override
  String periodWeekOf(String arg0) {
    return 'Nädal $arg0';
  }

  @override
  String get periodThisMonth => 'Sel kuul';

  @override
  String get periodThisYear => 'Sel aastal';

  @override
  String get periodLast7Days => 'Viimased 7 päeva';

  @override
  String get periodLast30Days => 'Viimased 30 päeva';

  @override
  String get periodLast365Days => 'Viimased 365 päeva';

  @override
  String get periodSelected => 'Valitud periood';

  @override
  String get metricSteps => 'Sammud';

  @override
  String get metricDistance => 'Vahemaa';

  @override
  String get metricAveragePace => 'Keskmine tempo';

  @override
  String get metricAverageSpeed => 'Keskmine kiirus';

  @override
  String get metricCaloriesBurned => 'Kulutatud kalorid kokku';

  @override
  String get metricCaloriesOut => 'Kalorid kokku';

  @override
  String get metricCaloriesIn => 'Kalorid sisse';

  @override
  String get metricFloorsClimbed => 'Läbitud korrused';

  @override
  String get metricActiveCalories => 'Aktiivsed kalorid';

  @override
  String get metricElevation => 'Kõrgus';

  @override
  String get metricElevationGained => 'Kõrgusekasv';

  @override
  String get metricWheelchairPushes => 'Ratastooli lükked';

  @override
  String get metricWorkout => 'Treening';

  @override
  String get metricSleep => 'Uni';

  @override
  String get metricHydration => 'Joogid';

  @override
  String get metricTotalHydration => 'Vedeliku kogus kokku';

  @override
  String get metricHydrationTrend => 'Joogitrend';

  @override
  String get metricLoggedDays => 'Logitud päevad';

  @override
  String get metricLatestWeight => 'Viimane kaal';

  @override
  String get metricBodyFat => 'Keharasv';

  @override
  String get metricAvgHeartRate => 'Keskm pulss';

  @override
  String get metricAverageHeartRate => 'Keskmine pulss';

  @override
  String get metricRestingHeartRate => 'Puhkeoleku pulss';

  @override
  String get metricHrv => 'Pulsi varieeruvus (HRV)';

  @override
  String get metricCardioLoad => 'Kardiokoormus';

  @override
  String get metricWeeklyCardioLoad => 'Nädala kardiokoormus';

  @override
  String get metricEnergyBurned => 'Kalorid kokku';

  @override
  String get metricBloodPressure => 'Vererõhk';

  @override
  String get metricSpo2 => 'SpO2';

  @override
  String get metricOxygenSaturation => 'Hapnikusaturatsioon';

  @override
  String get metricVo2Max => 'VO2 max';

  @override
  String get metricMindfulness => 'Teadvelolek';

  @override
  String get metricTotalMindfulness => 'Teadvelolekut kokku';

  @override
  String get metricCycle => 'Tsükkel';

  @override
  String get metricCycleTracking => 'Tsükli jälgimine';

  @override
  String get metricPeriodDays => 'Menstruatsioonipäevad';

  @override
  String get metricOvulationTests => 'Ovulatsioonitestid';

  @override
  String get metricLatestBbt => 'Viimane BBT';

  @override
  String get metricWeight => 'Kaal';

  @override
  String get metricHeight => 'Pikkus';

  @override
  String get metricBmi => 'BMI';

  @override
  String get metricFfmi => 'FFMI';

  @override
  String get metricLeanMass => 'Rasvavaba mass';

  @override
  String get metricBmr => 'BMR';

  @override
  String get metricBoneMass => 'Luumass';

  @override
  String get metricBodyWaterMass => 'Keha veemass';

  @override
  String get metricLatest => 'Viimane';

  @override
  String get metricChange => 'Muutus';

  @override
  String get metricMacros => 'Makrotoitained';

  @override
  String get metricProtein => 'Valgud';

  @override
  String get metricCarbs => 'Süsivesikud';

  @override
  String get metricFat => 'Rasvad';

  @override
  String get metricDietaryFiber => 'Kiudained';

  @override
  String get metricSugar => 'Suhkur';

  @override
  String get metricEnergyFromFat => 'Rasvast saadud kalorid';

  @override
  String get metricMonounsaturatedFat => 'Monoküllastumata rasv';

  @override
  String get metricPolyunsaturatedFat => 'Polüküllastumata rasv';

  @override
  String get metricSaturatedFat => 'Küllastunud rasv';

  @override
  String get metricTransFat => 'Transrasv';

  @override
  String get metricUnsaturatedFat => 'Küllastumata rasv';

  @override
  String get metricCholesterol => 'Kolesterool';

  @override
  String get metricBiotin => 'Biotiin';

  @override
  String get metricFolate => 'Folaat';

  @override
  String get metricFolicAcid => 'Foolhape';

  @override
  String get metricNiacin => 'Niatsiin';

  @override
  String get metricPantothenicAcid => 'Pantoteenhape';

  @override
  String get metricRiboflavin => 'Riboflaviin';

  @override
  String get metricThiamin => 'Tiamiin';

  @override
  String get metricVitaminA => 'A-vitamiin';

  @override
  String get metricVitaminB12 => 'B12-vitamiin';

  @override
  String get metricVitaminB6 => 'B6-vitamiin';

  @override
  String get metricVitaminC => 'C-vitamiin';

  @override
  String get metricVitaminD => 'D-vitamiin';

  @override
  String get metricVitaminE => 'E-vitamiin';

  @override
  String get metricVitaminK => 'K-vitamiin';

  @override
  String get metricCalcium => 'Kaltsium';

  @override
  String get metricChloride => 'Kloriid';

  @override
  String get metricChromium => 'Kroom';

  @override
  String get metricCopper => 'Vask';

  @override
  String get metricIodine => 'Jood';

  @override
  String get metricIron => 'Raud';

  @override
  String get metricMagnesium => 'Magneesium';

  @override
  String get metricManganese => 'Mangaan';

  @override
  String get metricMolybdenum => 'Molübdeen';

  @override
  String get metricPhosphorus => 'Fosfor';

  @override
  String get metricPotassium => 'Kaalium';

  @override
  String get metricSelenium => 'Seleen';

  @override
  String get metricSodium => 'Naatrium';

  @override
  String get metricZinc => 'Tsink';

  @override
  String get metricCaffeine => 'Kofeiin';

  @override
  String get metricRespiratoryRate => 'Hingamissagedus';

  @override
  String get metricAvgRespiratoryRate => 'Keskm hingamissagedus';

  @override
  String get metricBodyTemp => 'Kehatemperatuur';

  @override
  String get metricBloodGlucose => 'Veresuhkur';

  @override
  String get metricSkinTemperature => 'Nahatemperatuur';

  @override
  String get metricRecordedSpeed => 'Salvestatud kiirus';

  @override
  String get metricAveragePower => 'Keskmine võimsus';

  @override
  String get metricStepsCadence => 'Sammusagedus';

  @override
  String get metricCyclingCadence => 'Rattasõidu sagedus';

  @override
  String get unitSteps => 'sammu';

  @override
  String get unitReps => 'kordust';

  @override
  String get unitPushes => 'lükkeid';

  @override
  String get unitFloors => 'korrust';

  @override
  String get unitDays => 'päeva';

  @override
  String get unitNights => 'ööd';

  @override
  String get unitTests => 'testi';

  @override
  String get unitTotal => 'kokku';

  @override
  String get unitGrams => 'g';

  @override
  String get sectionActivities => 'Treeningud';

  @override
  String get sectionActivityTypeStats => 'Treeningu tüübi järgi';

  @override
  String activityTypeStatsActivityCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '# treeningut',
      one: '# treening',
    );
    return '$_temp0';
  }

  @override
  String get sectionPlannedWorkouts => 'Planeeritud treeningud';

  @override
  String get activitiesFilterActivityTypeLabel => 'Treeningu tüüp';

  @override
  String get activitiesFilterAll => 'Kõik treeningud';

  @override
  String get activitiesKeyMetrics => 'Põhinäitajad';

  @override
  String get recoverySleepScore => 'Unehinne';

  @override
  String get recoverySleepDuration => 'Une kestus';

  @override
  String get recoverySleepSchedule => 'Unegraafik';

  @override
  String get recoveryRemSleep => 'REM-uni';

  @override
  String get recoveryDeepSleep => 'Sügav uni';

  @override
  String get recoverySleepEfficiency => 'Une efektiivsus';

  @override
  String get sleepScoreConfidenceHigh => 'Kõrge usaldusväärsus';

  @override
  String get sleepScoreConfidenceMedium => 'Keskmine usaldusväärsus';

  @override
  String get sleepScoreConfidenceLow => 'Madal usaldusväärsus';

  @override
  String get sleepScoreConfidenceNoData => 'Andmed puuduvad';

  @override
  String get sleepScoreRatingExcellent => 'Suurepärane';

  @override
  String get sleepScoreRatingGood => 'Hea';

  @override
  String get sleepScoreRatingFair => 'Rahuldav';

  @override
  String get sleepScoreRatingPoor => 'Halb';

  @override
  String dashboardSleepScoreSubtitle(String arg0, String arg1) {
    return '$arg0 • $arg1';
  }

  @override
  String get sleepScoreCalculationTitle => 'Kuidas seda arvutatakse';

  @override
  String get sleepScoreDayNumbersTitle => 'Tänased väärtused';

  @override
  String get sleepScoreReferencesTitle => 'Kasutatud allikad';

  @override
  String get sleepScoreCalculationSummary =>
      'OpenVitals hindab objektiivset une tervist kestuse, efektiivsuse, järjepidevuse ja regulaarsuse põhjal. See ei diagnoosi unehäireid.';

  @override
  String get sleepScoreFormula =>
      'Unehinne = kestus 35 + efektiivsus 30 + järjepidevus 20 + regulaarsus 15';

  @override
  String get sleepScoreFormulaBody =>
      'Kestuse eest antakse täispunktid 7-9 tunni kohta. Efektiivsus kasutab koguuneaega jagatuna voodis oldud ajaga. Järjepidevus kasutab ärkveloleku aega pärast uinumist. Regulaarsus võrdleb tänase une keskpunkti viimaste öödega.';

  @override
  String get sleepScoreComponentsBody =>
      'Unefaaside andmed suurendavad usaldusväärsust, kuid REM- ja sügavat und ei hinnata kõrgelt, kuna tarbijaseadmete faasihinnangud võivad varieeruda. Kui regulaarsuse ajalugu puudub, kasutab OpenVitals neutraalset regulaarsuse väärtust ja langetab usaldusväärsust.';

  @override
  String get sleepScoreNotDiagnostic =>
      'See hinne on igapäevane suunis Health Connecti andmete põhjal, mitte diagnoos ega ravisoovitus.';

  @override
  String get sleepScoreComponentDuration => 'Kestus';

  @override
  String get sleepScoreComponentEfficiency => 'Efektiivsus';

  @override
  String get sleepScoreComponentContinuity => 'Järjepidevus';

  @override
  String get sleepScoreComponentRegularity => 'Regulaarsus';

  @override
  String get sleepScoreTotalSleep => 'Uni kokku';

  @override
  String get sleepScoreTimeInBed => 'Voodis oldud aeg';

  @override
  String get sleepScoreEfficiency => 'Efektiivsus';

  @override
  String get sleepScoreWaso => 'Ärkvelolek pärast uinumist';

  @override
  String get sleepScoreRegularity => 'Ajaline erinevus';

  @override
  String get sleepScoreBaselineNights => 'Võrdlusööd';

  @override
  String get sleepScoreStageRecords => 'Unefaaside kirjed';

  @override
  String get sleepScoreQualityNoData =>
      'Hinde arvutamiseks ei ole piisavalt uneandmeid.';

  @override
  String get sleepScoreQualityStageAwake =>
      'Kasutab Health Connecti unefaase ja ärkveloleku faase.';

  @override
  String get sleepScoreQualityStageOnly =>
      'Kasutab unefaase; ärkveloleku järjepidevus võib olla hinnanguline.';

  @override
  String get sleepScoreQualitySessionOnly =>
      'Kasutab ainult unesessiooni ajastust; usaldusväärsus on piiratud.';

  @override
  String get sleepScoreReferenceAasm => 'AASM täiskasvanute une kestus';

  @override
  String get sleepScoreReferenceSleepHealth => 'Mitmemõõtmeline une tervis';

  @override
  String get sleepScoreReferenceEfficiency => 'Une efektiivsuse määratlus';

  @override
  String get sleepScoreReferenceRegularity => 'Une regulaarsuse uuring';

  @override
  String get sleepEfficiencyConfidenceHigh => 'Kõrge usaldusväärsus';

  @override
  String get sleepEfficiencyConfidenceLow => 'Madal usaldusväärsus';

  @override
  String get sleepEfficiencyConfidenceNoData => 'Andmed puuduvad';

  @override
  String get sleepEfficiencyCalculationTitle => 'Kuidas seda arvutatakse';

  @override
  String get sleepEfficiencyDayNumbersTitle => 'Tänased väärtused';

  @override
  String get sleepEfficiencyReferencesTitle => 'Kasutatud allikad';

  @override
  String get sleepEfficiencyCalculationSummary =>
      'Une efektiivsus on uneakna see protsent, mille jooksul magati. Kõrgemad väärtused tähendavad tavaliselt vähem ärkvelolekuaega voodis.';

  @override
  String get sleepEfficiencyFormula =>
      'Une efektiivsus = koguuneaeg / voodis oldud aeg x 100';

  @override
  String get sleepEfficiencyFormulaBody =>
      'Koguuneaeg on Health Connecti unefaaside summa, kui faasid on saadaval. Voodis oldud aeg on peamise unesessiooni algus-lõpp aken.';

  @override
  String get sleepEfficiencyDataBody =>
      'Kui unefaasid puuduvad, võib Health Connect anda ainult sessiooni kestuse. OpenVitals võib siiski näidata hinnangu, kuid usaldusväärsus on madal, kuna ärkvelolekuaeg voodis võib olla varjatud.';

  @override
  String get sleepEfficiencyNotDiagnostic =>
      'Une efektiivsus on une järjepidevuse näitaja, mitte diagnoos. Püsivalt madalaid väärtusi tasub arstiga arutada.';

  @override
  String get sleepEfficiencyQualityNoData =>
      'Efektiivsuse arvutamiseks ei ole piisavalt uneandmeid.';

  @override
  String get sleepEfficiencyQualityStageBased =>
      'Kasutab koguuneaja jaoks Health Connecti unefaase.';

  @override
  String get sleepEfficiencyQualitySessionOnly =>
      'Kasutab ainult sessiooni ajastust; ärkvelolekuaeg võib puududa.';

  @override
  String get sleepEfficiencyReferenceDefinition => 'Une efektiivsuse määratlus';

  @override
  String get sleepEfficiencyReferenceDenominator =>
      'Une efektiivsuse nimetaja uuring';

  @override
  String get sleepEfficiencyReferenceMethods =>
      'Une hindamise meetodite ülevaade';

  @override
  String get cardioLoadConfidenceHigh => 'Kõrge usaldusväärsus';

  @override
  String get cardioLoadConfidenceMedium => 'Keskmine usaldusväärsus';

  @override
  String get cardioLoadConfidenceLow => 'Madal usaldusväärsus';

  @override
  String get cardioLoadConfidenceNoData => 'Andmed puuduvad';

  @override
  String get cardioLoadCalculationTitle => 'Kuidas seda arvutatakse';

  @override
  String get cardioLoadDayNumbersTitle => 'Tänased väärtused';

  @override
  String get cardioLoadReferencesTitle => 'Kasutatud allikad';

  @override
  String get cardioLoadCalculationSummary =>
      'OpenVitals kasutab pulsipõhist TRIMP-i, kui pulsiandmed on saadaval, ja langeb tagasi ainult liikumisele, kui pulss ei ole kasutatav.';

  @override
  String get cardioLoadFormula =>
      'TRIMP = minutid x HRR x 0,64 x e^(1,92 x HRR)';

  @override
  String get cardioLoadFormulaBody =>
      'HRR on pulsivaru: (pulss - puhkeoleku pulss) / (maksimaalne pulss - puhkeoleku pulss). OpenVitals liidab selle kokku kättesaadavate pulsi-intervallide kohta terve päeva jooksul.';

  @override
  String get cardioLoadMappingBody =>
      'Kui salvestatud treeningud on olemas, seotakse pulsinäidud ajatempli järgi iga treeningu algus- ja lõpuaknaga. Ilma treeninguakendeta loetakse ainult tõusnud pulsi intervalle. Kui pulss ei ole kasutatav, näidatakse madala usaldusväärsusega tagavarana liikumist ja aktiivseid kaloreid.';

  @override
  String get cardioLoadMethod => 'Meetod';

  @override
  String get cardioLoadTrimpScore => 'TRIMP-tulemus';

  @override
  String get cardioLoadHrCoverage => 'Pulsi kaetus';

  @override
  String get cardioLoadExpectedCoverage => 'Oodatav kaetus';

  @override
  String get cardioLoadRestingHr => 'Puhkepulss';

  @override
  String get cardioLoadMaxHr => 'Maks pulss';

  @override
  String get cardioLoadHrSamples => 'Pulsinäidud';

  @override
  String get cardioLoadActivityWindows => 'Treeninguaknad';

  @override
  String get cardioLoadActivityMinutes => 'Treeningu minutid';

  @override
  String get cardioLoadMovementFallback => 'Liikumise tagavaravariant';

  @override
  String get cardioLoadMethodActivityWindows => 'TRIMP treeningu pulsist';

  @override
  String get cardioLoadMethodElevatedHr => 'TRIMP tõusnud pulsist';

  @override
  String get cardioLoadMethodMovementFallback => 'Liikumise tagavaravariant';

  @override
  String get cardioLoadMethodNoData => 'Ebapiisavad andmed';

  @override
  String get cardioLoadCalibrationObservedResting => 'Puhkepulss mõõdetud';

  @override
  String get cardioLoadCalibrationEstimatedResting => 'Puhkepulss hinnanguline';

  @override
  String get cardioLoadCalibrationObservedMax => 'Maks pulss mõõdetud';

  @override
  String get cardioLoadCalibrationEstimatedMax => 'Maks pulss hinnanguline';

  @override
  String get cardioLoadReferenceBanister => 'Banisteri TRIMP-valem';

  @override
  String get cardioLoadReferenceTrainingLoad =>
      'Treeningkoormuse jälgimise ülevaade';

  @override
  String get cardioLoadReferenceHealthConnect =>
      'Health Connecti treeningu pulsi seostamine';

  @override
  String get sectionSleepSessions => 'Unesessioonid';

  @override
  String get sectionWeight => 'Kaal';

  @override
  String get sectionEntries => 'Kirjed';

  @override
  String get sectionMeals => 'Toidukorrad';

  @override
  String get sectionSessions => 'Sessioonid';

  @override
  String get sectionDailyBreakdown => 'Igapäevane jaotus';

  @override
  String get sectionVitals => 'Elunäitajad';

  @override
  String get sectionHeart => 'Süda';

  @override
  String get sectionCardiovascular => 'Südame-veresoonkond';

  @override
  String get sectionRespiratory => 'Hingamine';

  @override
  String get sectionRespiratoryRateDailyBreakdown =>
      'Hingamissageduse igapäevane jaotus';

  @override
  String get sectionVo2MaxHistory => 'VO2 max ajalugu';

  @override
  String get sectionDisplay => 'Kuva';

  @override
  String get sectionPrivacy => 'Privaatsus';

  @override
  String get sectionCycleCalendar => 'Tsükli kalender';

  @override
  String get sectionBasalBodyTemperature => 'Basaaltemperatuur';

  @override
  String get sectionStatistics => 'Statistika';

  @override
  String get sectionCalorieTrends => 'Kalorite trendid';

  @override
  String get sectionNutritionTrends => 'Toitumise trendid';

  @override
  String get sectionBodyTrends => 'Keha trendid';

  @override
  String get sectionCarbohydrates => 'Süsivesikud';

  @override
  String get sectionFats => 'Rasvad';

  @override
  String get sectionVitamins => 'Vitamiinid';

  @override
  String get sectionMinerals => 'Mineraalid';

  @override
  String get sectionOtherNutrients => 'Muud toitained';

  @override
  String summaryDailyAverage(String arg0) {
    return '$arg0 keskmine päevas';
  }

  @override
  String summaryDaysInRange(String arg0) {
    return '$arg0 päeva vahemikus';
  }

  @override
  String summaryEntries(String arg0) {
    return '$arg0 kirjet';
  }

  @override
  String summaryReadings(String arg0) {
    return '$arg0 näitu';
  }

  @override
  String summaryNights(String arg0) {
    return '$arg0 ööd';
  }

  @override
  String summaryRecordedStages(String arg0) {
    return '$arg0 salvestatud faasi';
  }

  @override
  String get summaryAverage => 'Keskm';

  @override
  String summaryAvgValue(String arg0) {
    return 'Keskm $arg0';
  }

  @override
  String summaryAvgValueRange(String arg0, String arg1, String arg2) {
    return 'Keskm $arg0 · vahemik $arg1-$arg2';
  }

  @override
  String summaryValueAvg(String arg0) {
    return '$arg0 keskmine';
  }

  @override
  String get summaryRange => 'Vahemik';

  @override
  String get summarySamples => 'Näidud';

  @override
  String summaryRecorded(String arg0, String arg1) {
    return '$arg0-$arg1 salvestatud';
  }

  @override
  String summaryRestingValue(String arg0) {
    return 'Puhkeoleku $arg0';
  }

  @override
  String summaryHrvValue(String arg0) {
    return 'HRV $arg0';
  }

  @override
  String summaryLastUpdate(String arg0) {
    return 'Viimane värskendus $arg0';
  }

  @override
  String get summaryNow => 'Praegu';

  @override
  String summaryToday(String arg0) {
    return '$arg0 täna';
  }

  @override
  String summaryOnDate(String arg0, String arg1) {
    return '$arg0 kuupäeval $arg1';
  }

  @override
  String summaryEmptyToday(String arg0) {
    return '$arg0 täna veel pole.';
  }

  @override
  String summaryEmptyDay(String arg0) {
    return '$arg0 sel päeval pole.';
  }

  @override
  String get summaryAcrossSelectedPeriod => 'Valitud perioodi jooksul';

  @override
  String summaryLatestTemperature(String arg0, String arg1) {
    return 'Viimane $arg0 · $arg1';
  }

  @override
  String summaryTemperatureRange(String arg0, String arg1, String arg2) {
    return 'Vahemik $arg0-$arg1 · $arg2 näitu';
  }

  @override
  String get summarySleepEndingToday => 'Uni lõpeb täna';

  @override
  String summarySleepEndingOn(String arg0) {
    return 'Uni lõpeb kuupäeval $arg0';
  }

  @override
  String get statTotal => 'Kokku';

  @override
  String get statTime => 'Aeg';

  @override
  String get statActiveDays => 'Aktiivseid päevi';

  @override
  String get statAverage => 'Keskmine';

  @override
  String get statLowest => 'Madalaim';

  @override
  String get statHighest => 'Kõrgeim';

  @override
  String get statReadings => 'Näidud';

  @override
  String get statDailyAverage => 'Päevane keskmine';

  @override
  String get caloriesStatActiveAverage => 'Aktiivne keskmine';

  @override
  String get caloriesStatBmrReadings => 'BMR-näidud';

  @override
  String get statAverageDuration => 'Keskmine kestus';

  @override
  String get statTotalIntake => 'Kogutarbimine';

  @override
  String get statBestDay => 'Parim päev';

  @override
  String get statNightsLogged => 'Logitud öid';

  @override
  String get statLongestSleep => 'Pikim uni';

  @override
  String get statLongestWorkout => 'Pikim treening';

  @override
  String get statAverageMovingPace => 'Keskm liikumistempo';

  @override
  String get statFastestPace => 'Kiireim tempo';

  @override
  String get statBestSpeed => 'Parim kiirus';

  @override
  String get statLongestSession => 'Pikim sessioon';

  @override
  String get statBbtReadings => 'BBT-näidud';

  @override
  String get statGoalStreak => 'Eesmärgi seeria';

  @override
  String get statLongestGoalStreak => 'Pikim seeria';

  @override
  String get statGoalsMet => 'Täidetud eesmärgid';

  @override
  String get statSuccessRate => 'Edukuse määr';

  @override
  String get statAverageGap => 'Keskmine vahe';

  @override
  String get statVsPreviousDay => 'Vs eelmine päev';

  @override
  String get statVsPreviousWeek => 'Vs eelmine nädal';

  @override
  String get statVsPreviousMonth => 'Vs eelmine kuu';

  @override
  String get statVsPreviousYear => 'Vs eelmine aasta';

  @override
  String get statBaseline => 'Lähtetase';

  @override
  String get stat30DayBaseline => '30-päeva keskmine';

  @override
  String get stat60DayBaseline => '60-päeva keskmine';

  @override
  String get stat90DayBaseline => '90-päeva keskmine';

  @override
  String get statUsualRange => 'Tavapärane vahemik';

  @override
  String get statBaselineDeviation => 'Kõrvalekalle lähtetasemest';

  @override
  String get baselineStatusUsual => 'Tavapärane';

  @override
  String get baselineStatusAbove => 'Üle';

  @override
  String get baselineStatusBelow => 'Alla';

  @override
  String get baselineStatusUnusualHigh => 'Ebatavaliselt kõrge';

  @override
  String get baselineStatusUnusualLow => 'Ebatavaliselt madal';

  @override
  String get sectionMetricContext => 'Kontekst';

  @override
  String get interpretationBpTitle => 'Vererõhu kategooria';

  @override
  String get interpretationBpNormal => 'Normaalne';

  @override
  String get interpretationBpElevated => 'Kõrgenenud';

  @override
  String get interpretationBpStage1 => '1. astme kõrge vererõhk';

  @override
  String get interpretationBpStage2 => '2. astme kõrge vererõhk';

  @override
  String get interpretationBpSevere => 'Raske vahemiku viide';

  @override
  String interpretationBpBody(String arg0) {
    return 'See näit jääb vahemikku $arg0. Üksik rakenduse näit ei ole diagnoos.';
  }

  @override
  String get interpretationBpSevereBody =>
      'See näit ületab raske vahemiku viite. Kontrolli üle; kui esineb sümptomeid või näit püsib väga kõrge, otsi kiiret abi.';

  @override
  String get interpretationBpSource =>
      'Allikas: American Heart Associationi täiskasvanute vererõhu kategooriad.';

  @override
  String get interpretationBmiTitle => 'BMI kategooria';

  @override
  String get interpretationBmiUnderweight => 'Alakaaluline';

  @override
  String get interpretationBmiHealthy => 'Normaalkaal';

  @override
  String get interpretationBmiOverweight => 'Ülekaaluline';

  @override
  String get interpretationBmiObesity1 => '1. astme rasvumine';

  @override
  String get interpretationBmiObesity2 => '2. astme rasvumine';

  @override
  String get interpretationBmiObesity3 => '3. astme rasvumine';

  @override
  String get interpretationBmiBody =>
      'Ainult täiskasvanute BMI sõeluuringu kategooria; BMI ei mõõda kehakoostist.';

  @override
  String get interpretationBmiSource =>
      'Allikas: CDC täiskasvanute BMI kategooriad.';

  @override
  String get interpretationFfmiTitle => 'FFMI kategooria';

  @override
  String get interpretationFfmiBelowAverage => 'Alla keskmise';

  @override
  String get interpretationFfmiAverage => 'Keskmine';

  @override
  String get interpretationFfmiAboveAverage => 'Üle keskmise';

  @override
  String get interpretationFfmiExcellent => 'Suurepärane';

  @override
  String get interpretationFfmiSuperior => 'Väga hea';

  @override
  String get interpretationFfmiExceptional => 'Erakordne';

  @override
  String get interpretationFfmiElite => 'Tipptasemel';

  @override
  String interpretationFfmiBody(String arg0, String arg1) {
    return 'FFMI $arg0; kohandatud FFMI $arg1. Kasutab sinu viimast kaalu, keharasva ja pikkust.';
  }

  @override
  String get interpretationFfmiSource =>
      'Allikas: ffmicalculators.com soovituslikud kohandatud FFMI kategooriad.';

  @override
  String get interpretationSleepTitle => 'Une eesmärk';

  @override
  String get interpretationSleepBelow => 'Alla eesmärgi';

  @override
  String get interpretationSleepNear => 'Eesmärgi lähedal';

  @override
  String get interpretationSleepMet => 'Eesmärk täidetud';

  @override
  String interpretationSleepBelowBody(String arg0) {
    return 'Keskmine uni on $arg0 alla sinu seatud eesmärgi.';
  }

  @override
  String interpretationSleepNearBody(String arg0, String arg1) {
    return 'Keskmine uni on lähedal sinu seatud eesmärgile: $arg0 vs $arg1.';
  }

  @override
  String interpretationSleepMetBody(String arg0, String arg1) {
    return 'Keskmine uni vastab sinu seatud eesmärgile: $arg0 vs $arg1.';
  }

  @override
  String get interpretationSleepSource =>
      'Põhineb sinu seatud une eesmärgil, mitte meditsiinilisel unehindamisel.';

  @override
  String get interpretationMacroTitle => 'Makrotoitainete jaotus';

  @override
  String get interpretationMacroWithin => 'Vastab võrdlusjaotusele';

  @override
  String get interpretationMacroOutside => 'Väljaspool võrdlusjaotust';

  @override
  String interpretationMacroBody(String arg0, String arg1, String arg2) {
    return 'Valgud $arg0, süsivesikud $arg1, rasvad $arg2 logitud makrokaloritest.';
  }

  @override
  String get interpretationMacroSource =>
      'Allikas: National Academies AMDR täiskasvanute võrdlusväärtus; ainult logitud makrotoitained.';

  @override
  String get interpretationWorkoutTitle => 'Treeningjuhise edenemine';

  @override
  String get interpretationWorkoutNone => 'Logitud minuteid pole';

  @override
  String get interpretationWorkoutBelow => 'Alla nädala võrdlusväärtuse';

  @override
  String get interpretationWorkoutApproaching =>
      'Läheneb nädala võrdlusväärtusele';

  @override
  String get interpretationWorkoutMet => 'Nädala võrdlusväärtus täidetud';

  @override
  String interpretationWorkoutBody(String arg0, String arg1) {
    return 'Logitud $arg0 150 min/nädalas täiskasvanute võrdlusväärtusest ($arg1). Intensiivsust ei ole kontrollitud.';
  }

  @override
  String interpretationWorkoutBodyWeeklyAverage(String arg0, String arg1) {
    return 'Nädala keskmine $arg0 150 min/nädalas täiskasvanute võrdlusväärtusest ($arg1). Intensiivsust ei ole kontrollitud.';
  }

  @override
  String get interpretationWorkoutSource =>
      'Allikas: HHS täiskasvanute kehalise aktiivsuse juhise võrdlusväärtus.';

  @override
  String get interpretationVitalTitle => 'Elunäitaja kontekst';

  @override
  String get interpretationVitalWithin =>
      'Laia täiskasvanute võrdlusvahemiku piires';

  @override
  String get interpretationVitalBelow =>
      'Alla laia täiskasvanute võrdlusvahemiku';

  @override
  String get interpretationVitalAbove =>
      'Üle laia täiskasvanute võrdlusvahemiku';

  @override
  String get interpretationVitalOxygenBelowTypical =>
      'Alla tüüpilise hapnikuvahemiku';

  @override
  String get interpretationVitalOxygenLow => 'Madal hapniku võrdlusväärtus';

  @override
  String get interpretationVitalOxygenVeryLow =>
      'Väga madal hapniku võrdlusväärtus';

  @override
  String get interpretationVitalRestingHrBody =>
      'Ainult lai täiskasvanute võrdlusväärtus; vorm, ravimid, stress, haigus ja kellaaeg võivad muuta seda, mis on sinu jaoks tavapärane.';

  @override
  String get interpretationVitalRespiratoryBody =>
      'Ainult lai täiskasvanute võrdlusväärtus; aktiivsus, ärevus, haigus ja mõõtmise ajastus võivad mõjutada hingamissagedust.';

  @override
  String get interpretationVitalTemperatureBody =>
      'Temperatuur varieerub mõõtmiskoha ja kellaaja järgi; kasuta seda ainult kontekstina.';

  @override
  String get interpretationVitalOxygenBody =>
      'Pulssoksümeetri näite võivad mõjutada seade, nahk, vereringe, liikumine ja tingimused.';

  @override
  String get interpretationVitalSource =>
      'Allikas: MedlinePlus täiskasvanute elutähtsate näitajate võrdlusväärtus.';

  @override
  String get interpretationOxygenSource =>
      'Allikas: MedlinePlus ja FDA pulssoksümeetri kontekst.';

  @override
  String get sectionCrossMetricInsights => 'Näitajatevahelised seosed';

  @override
  String get crossMetricPositiveLink => 'Positiivne seos';

  @override
  String get crossMetricNegativeLink => 'Negatiivne seos';

  @override
  String get crossMetricWeakLink => 'Nõrk seos';

  @override
  String crossMetricCorrelation(String arg0) {
    return '$arg0';
  }

  @override
  String crossMetricPairedDays(int arg0) {
    return '$arg0 paaritud päeva';
  }

  @override
  String get crossSleepHrvTitle => 'Uni vs HRV';

  @override
  String get crossSleepHrvPositive =>
      'Rohkem und kaldub selles perioodis kaasnema kõrgema HRV-ga.';

  @override
  String get crossSleepHrvNegative =>
      'Rohkem und kaldub selles perioodis kaasnema madalama HRV-ga.';

  @override
  String get crossSleepHrvNeutral =>
      'Uni ja HRV ei näita selles perioodis selget mustrit.';

  @override
  String get crossWorkoutRestingHrTitle => 'Treeningud vs puhkeoleku pulss';

  @override
  String get crossWorkoutRestingHrPositive =>
      'Rohkem treeningminuteid kaldub selles perioodis kaasnema kõrgema puhkeoleku pulsiga.';

  @override
  String get crossWorkoutRestingHrNegative =>
      'Rohkem treeningminuteid kaldub selles perioodis kaasnema madalama puhkeoleku pulsiga.';

  @override
  String get crossWorkoutRestingHrNeutral =>
      'Treeningminutid ja puhkeoleku pulss ei näita selles perioodis selget mustrit.';

  @override
  String get crossHydrationWeightTitle =>
      'Vedelikutarbimine vs kaalu kõikumine';

  @override
  String get crossHydrationWeightPositive =>
      'Rohkem vedelikutarbimist kaldub selles perioodis kaasnema suuremate kaalukõikumistega.';

  @override
  String get crossHydrationWeightNegative =>
      'Rohkem vedelikutarbimist kaldub selles perioodis kaasnema väiksemate kaalukõikumistega.';

  @override
  String get crossHydrationWeightNeutral =>
      'Vedelikutarbimine ja kaalu kõikumine ei näita selles perioodis selget mustrit.';

  @override
  String get crossMindfulnessSleepTitle => 'Teadvelolek vs uni';

  @override
  String get crossMindfulnessSleepPositive =>
      'Rohkem teadveloleku minuteid kaldub selles perioodis kaasnema pikema unega.';

  @override
  String get crossMindfulnessSleepNegative =>
      'Rohkem teadveloleku minuteid kaldub selles perioodis kaasnema lühema unega.';

  @override
  String get crossMindfulnessSleepNeutral =>
      'Teadvelolek ja uni ei näita selles perioodis selget mustrit.';

  @override
  String get legendLess => 'Vähem';

  @override
  String get legendMore => 'Rohkem';

  @override
  String get dailyGoal => 'Igapäevane eesmärk';

  @override
  String goalProgress(int arg0, int arg1) {
    return '$arg0 päeva $arg1-st jälgitud päevast täidetud';
  }

  @override
  String get cdDecreaseDailyGoal => 'Vähenda igapäevast eesmärki';

  @override
  String get cdIncreaseDailyGoal => 'Suurenda igapäevast eesmärki';

  @override
  String get hydrationDailyGoal => 'Igapäevane eesmärk';

  @override
  String hydrationGoalProgress(int arg0, int arg1) {
    return '$arg0 päeva $arg1-st jälgitud päevast täidetud';
  }

  @override
  String get hydrationRemindersTitle => 'Joogimeeldetuletused';

  @override
  String get hydrationRemindersSummaryOff =>
      'Vaikimisi väljas. Luba meeldetuletused aktiivsetel tundidel, kuni tänane vedelikueesmärk on täidetud.';

  @override
  String hydrationRemindersSummaryOn(int arg0, String arg1, String arg2) {
    return 'Iga $arg0 min • $arg1-$arg2';
  }

  @override
  String get hydrationRemindersPermissionNeeded =>
      'Joogimeeldetuletuste lubamiseks anna teavitusluba.';

  @override
  String get hydrationRemindersInterval => 'Meeldetuletuse intervall';

  @override
  String hydrationRemindersIntervalValue(int arg0) {
    return 'Iga $arg0 min';
  }

  @override
  String get hydrationRemindersActiveStart => 'Aktiivne alates';

  @override
  String get hydrationRemindersActiveEnd => 'Aktiivne kuni';

  @override
  String get hydrationRemindersGoalNote =>
      'Meeldetuletused peatuvad, kui tänane eesmärk on täidetud, ja jätkuvad homme.';

  @override
  String get hydrationReminderNotificationChannel => 'Joogimeeldetuletused';

  @override
  String get hydrationReminderNotificationChannelDesc =>
      'Valikulised meeldetuletused jookide logimiseks aktiivsetel tundidel.';

  @override
  String get hydrationReminderNotificationTitle => 'Joogimeeldetuletus';

  @override
  String hydrationReminderNotificationBody(String arg0, String arg1) {
    return 'Oled täna $arg0/$arg1 juures. Lisa jook, kui saad.';
  }

  @override
  String hydrationReminderNotificationProgress(String arg0, String arg1) {
    return '$arg0 / $arg1';
  }

  @override
  String get hydrationTrackerTitle => 'Logi jook';

  @override
  String get hydrationTrackerSubtitle => 'Salvestatud otse Health Connectisse';

  @override
  String get hydrationTrackerPermissionNeeded =>
      'Kokkuvõtte jaoks küsib OpenVitals ainult vaatamisõigust. Selle käsitsi kirje lisamiseks vajame kirjutamisluba. OpenVitals ei salvesta neid andmeid ise; kirjed salvestatakse Health Connectisse.';

  @override
  String get hydrationNutritionPermissionNeeded =>
      'Anna toitumise kirjutamisluba, et salvestada joogi toitaineid Health Connectisse.';

  @override
  String get hydrationCustomDrinksTitle => 'Salvestatud joogid';

  @override
  String get hydrationCatalogDrinksTitle => 'Jookide kataloog';

  @override
  String get hydrationCatalogSearch => 'Otsi jooke';

  @override
  String get hydrationCatalogFrequentlyConsumed => 'Sageli tarbitud';

  @override
  String get hydrationCatalogSavedOutside => 'Salvestatud joogid';

  @override
  String get hydrationCatalogSectionWater => 'Vesi';

  @override
  String get hydrationCatalogSectionCoffees => 'Kohvid';

  @override
  String get hydrationCatalogSectionEnergyDrinks => 'Energiajoogid';

  @override
  String get hydrationCatalogSectionTeas => 'Teed';

  @override
  String get hydrationCatalogSectionChocolateDrinks => 'Kakaojoogid';

  @override
  String get hydrationCatalogSectionCarbonatedSoftDrinks =>
      'Karboniseeritud karastusjoogid';

  @override
  String get hydrationCatalogSectionOtherDrinks => 'Muud joogid';

  @override
  String hydrationCatalogSectionCount(int arg0) {
    return '$arg0 jooki';
  }

  @override
  String get hydrationNewDrinkAction => 'Uus jook';

  @override
  String get hydrationNewDrinkTitle => 'Uus jook';

  @override
  String get hydrationEditDrinkTitle => 'Muuda jooki';

  @override
  String hydrationLogSavedDrinkTitle(String arg0) {
    return 'Logi $arg0';
  }

  @override
  String get hydrationCustomDrinkName => 'Nimi';

  @override
  String get hydrationCustomDrinkCategory => 'Kategooria';

  @override
  String get hydrationCustomDrinkNoCategory => 'Kategooria puudub';

  @override
  String get hydrationCustomDrinkHydrationImpact => 'Mõju vedelikutarbimisele';

  @override
  String get hydrationImpactCountsFully => 'Loetakse täielikult';

  @override
  String get hydrationImpactCountsPartially => 'Loetakse osaliselt';

  @override
  String get hydrationImpactDoesNotCount => 'Ei loeta';

  @override
  String get hydrationImpactCountsFullyBody =>
      'Kogu joogi kogus loetakse vedelikutarbimise hulka.';

  @override
  String get hydrationImpactCountsPartiallyBody =>
      'Kasuta protsenti sellest joogist.';

  @override
  String get hydrationImpactDoesNotCountBody =>
      'Salvesta ilma vedelikutarbimist lisamata.';

  @override
  String get hydrationImpactPercentLabel => 'Loetakse vedelikutarbimiseks (%)';

  @override
  String get hydrationImpactInvalidPercent =>
      'Sisesta protsent üle 0 ja alla 100.';

  @override
  String get hydrationCustomDrinkNutrients => 'Toitained';

  @override
  String get hydrationCustomDrinkAddNutrient => 'Lisa toitaine';

  @override
  String get hydrationCustomDrinkLiquidOnly => 'Ainult vedelik';

  @override
  String hydrationCustomDrinkNutrientCount(int arg0) {
    return 'Toitaineid: $arg0';
  }

  @override
  String hydrationSavedDrinkAmountNoHydration(String arg0) {
    return '$arg0 • Ei loeta vedelikutarbimiseks';
  }

  @override
  String hydrationSavedDrinkAmountPartialHydration(String arg0, int arg1) {
    return '$arg0 • Loetakse $arg1% vedelikutarbimiseks';
  }

  @override
  String get hydrationNonHydratingDrinkSavedHint =>
      'Salvestatud ainult toitumisena. Vedelikutarbimist ei lisatud.';

  @override
  String get hydrationEntryNutritionOnly => 'Jook';

  @override
  String get hydrationEntryNoHydration => 'Vedelikutarbimisele mõju puudub';

  @override
  String get hydrationCustomDrinkAmountGrams => 'Kogus (g)';

  @override
  String get hydrationCustomDrinkAmountKcal => 'Kogus (kcal)';

  @override
  String get hydrationCustomDrinkInvalid =>
      'Sisesta joogi nimi, kogus ja positiivsed toitainekogused.';

  @override
  String get hydrationInvalidAmount =>
      'Sisesta kogus, mis on suurem kui null ja mitte üle 100 l.';

  @override
  String hydrationDrinkAmountLabel(String arg0) {
    return 'Kogus ($arg0)';
  }

  @override
  String hydrationDrinkInvalidAmountRange(String arg0, String arg1) {
    return 'Sisesta kogus vahemikus $arg0 kuni $arg1.';
  }

  @override
  String hydrationWriteFailed(String arg0) {
    return 'Vedelikutarbimise kirjet ei õnnestunud salvestada: $arg0';
  }

  @override
  String get cdDecreaseHydrationGoal => 'Vähenda vedelikueesmärki';

  @override
  String get cdIncreaseHydrationGoal => 'Suurenda vedelikueesmärki';

  @override
  String get cdDecreaseHydrationReminderInterval =>
      'Vähenda joogimeeldetuletuse intervalli';

  @override
  String get cdIncreaseHydrationReminderInterval =>
      'Suurenda joogimeeldetuletuse intervalli';

  @override
  String get unitPercentSymbol => '%';

  @override
  String get messageNoDashboardData => 'Kokkuvõtte andmed puuduvad.';

  @override
  String get messageMissingPermissionsTitle => 'Mõned load puuduvad';

  @override
  String get messageMissingPermissionsBody =>
      'Anna puuduvad load, et näha täielikku kokkuvõtet.';

  @override
  String messageHealthConnectRateLimited(int arg0) {
    return 'Health Connecti piirmäär saavutatud. Palun oota umbes $arg0 min ja proovi uuesti.';
  }

  @override
  String get messageNoWorkoutsDay =>
      'Sel päeval ei ole treeninguid salvestatud.';

  @override
  String get messageNoSleepDay => 'Sel päeval ei lõppenud ühtegi unesessiooni.';

  @override
  String get messageNoBloodPressure => 'Vererõhu näit puudub.';

  @override
  String get messageNoOxygen => 'Hapnikunäit puudub.';

  @override
  String get messageNoVo2Max => 'VO2 max näit puudub.';

  @override
  String get messageNoBloodGlucose => 'Veresuhkru näit puudub.';

  @override
  String get messageNoSkinTemperature => 'Nahatemperatuuri näit puudub.';

  @override
  String get messageCycleBrowse => 'Vaata tsükli kalendrit ja näitusid.';

  @override
  String get dashboardAddWidgets => 'Lisa vidinaid';

  @override
  String get dashboardAllWidgetsAdded => 'Kõik vidinad on juba kokkuvõttes.';

  @override
  String get dashboardActionLog => 'Logi';

  @override
  String get dashboardActionStartWorkout => 'Alusta treeningut';

  @override
  String get dashboardActivitiesToday => 'Treeningud';

  @override
  String get dashboardSensorStatusTitle => 'Anduri aku';

  @override
  String dashboardSensorBatteryLowest(int arg0) {
    return '$arg0% madalaim';
  }

  @override
  String get dashboardSensorBatteryUnknown => 'Aku ootel';

  @override
  String dashboardSensorStatusActiveConnected(int arg0, int arg1) {
    return '$arg0 aktiivne • $arg1 ühendatud';
  }

  @override
  String get dashboardSensorStatusAllDisabled => 'Kõik andurid keelatud';

  @override
  String get dashboardDeleteActivityTitle => 'Kustuta treening?';

  @override
  String dashboardDeleteActivityMessage(String arg0) {
    return 'Kustutada see $arg0 treening OpenVitalsist?';
  }

  @override
  String get dashboardReadinessTitle => 'Igapäevane valmisolek';

  @override
  String get dashboardReadinessScore => 'Valmisolek';

  @override
  String get dashboardReadinessBodyEnergy => 'Kehaenergia';

  @override
  String get dashboardReadinessTraining => 'Treeningvalmisolek';

  @override
  String get dashboardReadinessHrvStatus => 'HRV olek';

  @override
  String get dashboardReadinessIntensityMinutes => 'Intensiivsuse minutid';

  @override
  String get dashboardReadinessStressLevel => 'Stressitase';

  @override
  String get dashboardReadinessRecommended => 'Soovitatud';

  @override
  String get dashboardReadinessAvoid => 'Väldi';

  @override
  String get dashboardReadinessAlternative => 'Alternatiiv';

  @override
  String get dashboardReadinessStrain => 'Koormuse eesmärk';

  @override
  String get dashboardReadinessGoal => 'Kohanduv eesmärk';

  @override
  String get dashboardReadinessRecoveryMode => 'Taastumisrežiim';

  @override
  String get dashboardReadinessRecoveryModeBody =>
      'Treeningu eesmärke on vähendatud, et saaksid keskenduda puhkusele.';

  @override
  String get dashboardReadinessWhy => 'Miks see soovitus';

  @override
  String get stressDetailsHowTracked => 'Kuidas seda jälgitakse';

  @override
  String get stressDetailsHowTrackedBody =>
      'OpenVitals hindab füsioloogilist stressi kohapeal HRV võrdluses sinu lähtetasemega, puhkeoleku pulsi võrdluses sinu lähtetasemega ja keskmise pulsi võrdluses puhkeoleku pulsiga. See on koormuse hinnang, mitte diagnoos ega vaimse stressi tuvastaja.';

  @override
  String get stressDetailsScale =>
      'Skaala: 0-25 puhkeolek, 26-50 madal, 51-75 keskmine, 76-100 kõrge.';

  @override
  String get stressDetailsInputs => 'Kasutatud sisendid';

  @override
  String get stressDetailsNoInputs =>
      'Kasutuskõlblikke HRV ega pulsi lähtetaseme signaale ei olnud saadaval.';

  @override
  String get stressDetailsDataCoverage => 'Andmete kaetus';

  @override
  String get stressDetailsNoDataCoverage =>
      'Samapäevast pulsi ega HRV näidu kaetust ei olnud saadaval.';

  @override
  String get stressDetailsCaveats => 'Hoiatused';

  @override
  String get stressDetailsRelaxationPrompt =>
      'Kui see tundub täpne, proovi lühikest hingamis- või teadveloleku seanssi ja kontrolli pärast rahulikku perioodi uuesti.';

  @override
  String get readinessDetailsHowCalculated => 'Kuidas seda arvutatakse';

  @override
  String get readinessDetailsSignalsUsed => 'Kasutatud signaalid';

  @override
  String get readinessDetailsGuidance => 'Mida see tähendab';

  @override
  String get readinessDetailsCaveats => 'Hoiatused';

  @override
  String get readinessDetailsCaveatLocal =>
      'See on kohalik reeglipõhine hinnang OpenVitalsis praegu saadaolevatest andmetest.';

  @override
  String get readinessDetailsCaveatNotMedical =>
      'See ei ole diagnoos, meditsiiniline nõuanne, treeningjuhendamine ega vigastuse ennustamine.';

  @override
  String get readinessDetailsCaveatMissingData =>
      'Puuduvad load, hõredad näidud või puuduvad lähtetasemed vähendavad usaldusväärsust.';

  @override
  String get readinessDetailsScoreStrong => 'Tugev';

  @override
  String get readinessDetailsScoreSteady => 'Stabiilne';

  @override
  String get readinessDetailsScoreLimited => 'Piiratud';

  @override
  String get readinessDetailsScoreLow => 'Madal';

  @override
  String get readinessDetailsScoreNeedsMoreData => 'Vajab rohkem andmeid';

  @override
  String get bodyEnergyDetailsHowCalculatedBody =>
      'Kehaenergia kasutab taastumispoole signaale: unehinne, HRV olek, puhkeoleku pulss, füsioloogiline stress, temperatuur, vedelikutarbimine, toitumine ja teadvelolek. See hindab, kui palju taastumisvõimekust on täna näha.';

  @override
  String get bodyEnergyDetailsScale =>
      'Skaala: 80-100 tugev, 60-79 stabiilne, 40-59 piiratud, 0-39 madal.';

  @override
  String get bodyEnergyDetailsSummary =>
      'Taastumispoole hinne selle kohta, kui palju energiat sinu praegused kehasignaalid täna toetavad.';

  @override
  String get bodyEnergyDetailsNoSignals =>
      'Kasutuskõlblikke taastumispoole signaale ei olnud saadaval.';

  @override
  String get trainingReadinessDetailsHowCalculatedBody =>
      'Treeningvalmisolek kasutab treeningupoole signaale: uni, HRV olek, puhkeoleku pulss, treeningkoormus, intensiivsuse minutid, füsioloogiline stress, temperatuur ja treeningu kontekst. See hindab, kas raskem treening sobib täna.';

  @override
  String get trainingReadinessDetailsScale =>
      'Skaala: 80-100 valmis raskeks treeninguks, 60-79 kontrollitud treening, 40-59 kerge treening, 0-39 keskendu puhkusele.';

  @override
  String get trainingReadinessDetailsSummary =>
      'Treeningupoole hinne selle kohta, kui hästi praegused taastumis- ja koormussignaalid toetavad treeningu intensiivsust.';

  @override
  String get trainingReadinessDetailsNoSignals =>
      'Kasutuskõlblikke treeningupoole signaale ei olnud saadaval.';

  @override
  String dashboardGoalOf(String arg0) {
    return '$arg0-st';
  }

  @override
  String get caloriesEstimatedActiveBmr =>
      'Kogukirje puudub, hinnanguline aktiivne + BMR';

  @override
  String caloriesEstimatedValue(String arg0) {
    return 'Hinnang $arg0';
  }

  @override
  String dashboardWeeklyCardioLoadProgress(int arg0, int arg1) {
    return '$arg0/$arg1';
  }

  @override
  String dashboardCardioLoadPercentOnly(int arg0) {
    return '$arg0%';
  }

  @override
  String dashboardCardioLoadPercent(int arg0) {
    return '$arg0% koormus';
  }

  @override
  String dashboardCardioLoadTodayDelta(int arg0) {
    return '+$arg0% täna';
  }

  @override
  String get messageNoActivitiesPeriod =>
      'Valitud perioodil treeninguid ei ole.';

  @override
  String get plannedWorkoutCompleted => 'Lõpetatud';

  @override
  String plannedWorkoutBlocks(int arg0) {
    return '$arg0 plokki';
  }

  @override
  String get messageNoStepUpdates => 'Sammude uuendusi ei salvestatud';

  @override
  String get messageNoDistanceUpdates => 'Vahemaa uuendusi ei salvestatud';

  @override
  String get messageNoCaloriesBurned => 'Kogukalorite andmeid ei salvestatud';

  @override
  String get messageNoFloorsClimbed =>
      'Läbitud korruste andmeid ei salvestatud';

  @override
  String get messageNoActiveCalories =>
      'Aktiivsete kalorite andmeid ei salvestatud';

  @override
  String get messageNoCalorieDataPeriod =>
      'Sel perioodil pole kogu-, aktiivseid ega BMR-kaloreid.';

  @override
  String get messageNoElevation => 'Kõrguse andmeid ei salvestatud';

  @override
  String get messageNoWheelchairPushes =>
      'Ratastooli lükete andmeid ei salvestatud';

  @override
  String get messageNoSleepDaySelected => 'Valitud päeval uneandmeid pole.';

  @override
  String get messageNoSleepPeriod => 'Valitud perioodil uneandmeid pole.';

  @override
  String get messageNoHeartPeriod =>
      'Valitud perioodil pulsiandmeid pole.\n\nVeendu, et pulsiluba on antud ja ühendatud seade on andmed sünkroonis.';

  @override
  String get messageNoHeartSamplesDay =>
      'Sel päeval pulsinäite ei salvestatud.';

  @override
  String get messageHeartEmptyHint =>
      'Proovi mõnda muud kuupäeva või kontrolli, et ühendatud seade sünkroonis hetkelisi pulsiandmeid.';

  @override
  String get messageNoWeightPeriod =>
      'Valitud perioodil kaaluandmeid pole.\n\nSünkrooni kaal või nutiseade, mis edastab kaalu Health Connectisse.';

  @override
  String get messageNoHydrationPeriod =>
      'Sel perioodil joogikirjeid ei salvestatud.';

  @override
  String get messageNoHydrationAddedPeriod =>
      'Sel perioodil vedelikutarbimist ei lisatud.';

  @override
  String get messageNoNutritionPeriod =>
      'Sel perioodil toitumiskirjeid ei salvestatud.';

  @override
  String get messageNoMindfulnessPeriod =>
      'Sel perioodil teadveloleku seansse ei salvestatud.';

  @override
  String get messageNoVitalsPeriod =>
      'Sel perioodil elunäitajaid ei salvestatud.';

  @override
  String get messageNoReadingsPeriod => 'Sel perioodil näite pole.';

  @override
  String get messageNoCyclePeriod =>
      'Sel perioodil tsükliandmeid ei salvestatud.';

  @override
  String get messageNoSegments => 'Lõike ei salvestatud.';

  @override
  String get messageNoLaps => 'Ringe ei salvestatud.';

  @override
  String get messageNoRoutePoints => 'Marsruudi punkte ei salvestatud.';

  @override
  String get messageRouteConsentRequired =>
      'Marsruudi andmed on saadaval, kuid marsruudi juurdepääsu ei ole veel antud. Ava Health Connecti load seadetest, et lubada marsruudi eelvaated.';

  @override
  String get messageNoRouteData => 'Marsruudi andmeid ei salvestatud.';

  @override
  String get messageNoStages => 'Faase ei salvestatud.';

  @override
  String get messageNoKcal => 'Kcal puudub';

  @override
  String get onboardingTagline => 'Sinu terviseandmed sinu seadmes';

  @override
  String get onboardingPrivacyTitle => 'Privaatsus esikohal';

  @override
  String get onboardingPrivacyBody =>
      'Kontot ei ole vaja. Andmed jäävad sinu seadmesse. Pilve üleslaadimist, analüütikat ega reklaame ei ole.';

  @override
  String get healthDisclaimerTitle => 'Terviseteatis';

  @override
  String get healthDisclaimerBody =>
      'OpenVitals on mõeldud ainult üldiseks heaolu- ja teabekasutuseks. See ei ole meditsiiniseade ega anna meditsiinilist nõu. See ei diagnoosi, ravi ega ennetab ühtegi haigust ega terviseseisundit. Meditsiinilise nõu, diagnoosi või ravi saamiseks pöördu alati kvalifitseeritud tervishoiutöötaja poole.';

  @override
  String get onboardingHealthConnectTitle => 'Toetatud Health Connecti poolt';

  @override
  String get onboardingHealthConnectBody =>
      'Loeb andmeid turvalisest seadmesisesest Androidi terviseandmehoidlast ja salvestab sinu loodud kirjed tagasi Health Connectisse. Töötab kõigi Health Connectisse imporditud andmetega.';

  @override
  String get onboardingPermissionsHeader => 'HEALTH CONNECTI LOAD';

  @override
  String get onboardingGrantCore => 'Anna vajalikud Health Connecti load';

  @override
  String get onboardingGrantAll => 'Anna vajalikud Health Connecti load';

  @override
  String get onboardingGrantRemaining => 'Anna ülejäänud saadaolevad load';

  @override
  String get onboardingOpenRequiredPermissions =>
      'Ava vajalikud Health Connecti load';

  @override
  String get onboardingUnableOpenPermissions =>
      'Health Connecti lube ei õnnestunud avada.';

  @override
  String get onboardingHealthConnectNotSupported =>
      'Health Connect ei ole selles seadmes toetatud.';

  @override
  String get onboardingHealthConnectNeedsPlayStore =>
      'See seade töötab Android 13-ga, millel on installitud eraldiseisev Health Connecti rakendus. Kahjuks sõltub see versioon Google Play teenustest, mis sellel seadmel puuduvad, mistõttu Health Connect lükkab päringud tagasi enne, kui OpenVitals saab sinu andmeid lugeda. OpenVitals ei saa seda seadmepoolset Health Connecti probleemi parandada ega sellest mööda minna. Ainus lahendus on installida Google Play teenused või uuendada Android 14 või uuemale versioonile, kus Health Connect on osa operatsioonisüsteemist ega vaja Google\'i teenuseid.';

  @override
  String get onboardingHealthConnectUpdate =>
      'Selle rakenduse kasutamiseks tuleb Health Connect installida või uuendada.';

  @override
  String get onboardingInstallHealthConnect => 'Installi Health Connect';

  @override
  String get onboardingStatusNotSupported => 'Ei toetata';

  @override
  String get onboardingStatusGranted => 'Antud';

  @override
  String onboardingStatusPartiallyGranted(int arg0, int arg1) {
    return '$arg0/$arg1 antud';
  }

  @override
  String get onboardingStatusManual => 'Ava seaded';

  @override
  String get onboardingStatusRequired => 'Nõutav';

  @override
  String get onboardingStatusOptional => 'Valikuline';

  @override
  String get onboardingCategoryActivitySleep => 'Treening ja uni';

  @override
  String get onboardingCategoryActivitySleepDesc =>
      'Health Connect küsib:\n* Sammud\n* Vahemaa\n* Treening\n* Uni';

  @override
  String get onboardingCategoryHeartRecovery => 'Süda ja elunäitajad';

  @override
  String get onboardingCategoryHeartRecoveryDesc =>
      'Health Connect küsib:\n* Pulss\n* Puhkeoleku pulss\n* Pulsi varieeruvus';

  @override
  String get onboardingCategoryBody => 'Keha';

  @override
  String get onboardingCategoryBodyDesc =>
      'Health Connect küsib:\n* Kaal\n* Pikkus\n* Keharasv\n* Rasvavaba mass\n* Baasainevahetuse kiirus\n* Luumass\n* Keha veemass';

  @override
  String get onboardingCategoryActivityExtras => 'Treeningu lisandmed';

  @override
  String get onboardingCategoryActivityExtrasDesc =>
      'Health Connect küsib:\n* Kulutatud kalorid kokku\n* Kulutatud aktiivsed kalorid\n* Läbitud korrused\n* Kõrgusekasv\n* Ratastooli lükked\n* Kiirus\n* Võimsus\n* Sammusagedus\n* Rattasõidu pedaalisagedus\n* Planeeritud treening, kui toetatud';

  @override
  String get onboardingCategoryNutritionHydration =>
      'Toitumine ja vedelikutarbimine';

  @override
  String get onboardingCategoryNutritionHydrationDesc =>
      'Health Connect küsib:\n* Vedelikutarbimine\n* Toitumine';

  @override
  String get onboardingCategoryManualEntryWrite =>
      'Käsitsi kirje kirjutamisõigus';

  @override
  String get onboardingCategoryManualEntryWriteDesc =>
      'Health Connect küsib kirjutamisõigust:\n* Treening\n* Vahemaa\n* Kõrgusekasv\n* Kulutatud aktiivsed kalorid\n* Kulutatud kalorid kokku\n* Treeningu marsruut\n* Vedelikutarbimine\n* Kaal\n* Pikkus\n* Keharasv\n* Vererõhk\n* Hapnikusaturatsioon\n* Hingamissagedus\n* Kehatemperatuur\n* Teadvelolek, kui toetatud';

  @override
  String get onboardingCategoryDataImportWrite =>
      'Andmete impordi kirjutamisõigus';

  @override
  String get onboardingCategoryDataImportWriteDesc =>
      'Health Connect küsib kirjutamisõigust imporditud kirjetele:\n* Treening, harjutus, kalorid ja vahemaa\n* Pulss, puhkeoleku pulss ja pulsi varieeruvus\n* Kehamõõtmised\n* Vedelikutarbimine ja toitumine\n* Uni\n* Elunäitajad\n* Teadvelolek, kui toetatud\n* Tsükli jälgimise kirjed';

  @override
  String get onboardingCategoryMindfulness => 'Teadvelolek';

  @override
  String get onboardingCategoryMindfulnessDesc =>
      'Health Connect küsib:\n* Teadveloleku seansid';

  @override
  String get onboardingCategoryMindfulnessUnavailable =>
      'Teadveloleku seansid vajavad uuemat Health Connecti versiooni.';

  @override
  String get onboardingCategoryAdditionalDataAccess =>
      'Täiendav andmejuurdepääs';

  @override
  String get onboardingCategoryAdditionalDataAccessDesc =>
      'Health Connecti lubades ava OpenVitals > Täiendav juurdepääs ja määra:\n* Varasemate andmete juurdepääs: Luba\n* Andmejuurdepääs taustal: Luba\n* Treeningmarsruutide juurdepääs: Alati';

  @override
  String onboardingCategoryAdditionalDataAccessManualNote(String arg0) {
    return '$arg0\n\nKui Treeningmarsruutide juurdepääs puudub juurdepääsudialoogist, ava OpenVitalsi Health Connecti seaded ja määra see Täiendava juurdepääsu alt.';
  }

  @override
  String get onboardingCategoryVitals => 'Elunäitajad';

  @override
  String get onboardingCategoryVitalsDesc =>
      'Health Connect küsib:\n* Vererõhk\n* Hapnikusaturatsioon\n* Hingamissagedus\n* Kehatemperatuur\n* VO2 max\n* Veresuhkur\n* Nahatemperatuur, kui toetatud';

  @override
  String get onboardingCategoryCycleTracking => 'Tsükli jälgimine';

  @override
  String get onboardingCategoryCycleTrackingDesc =>
      'Health Connect küsib tundlikke tsükliandmeid:\n* Menstruatsiooni voolus\n* Menstruatsiooniperioodid\n* Ovulatsioonitestid\n* Emakakaelalima\n* Basaaltemperatuur\n* Menstruatsioonidevaheline veritsus\n* Seksuaalne aktiivsus';

  @override
  String get settingsAllRequestableGranted => 'Kõik taotletavad load on antud';

  @override
  String get settingsManualPermissionsTitle => 'Vajalikud käsitsi load';

  @override
  String get settingsManualPermissionsBody =>
      'Mõnda Health Connecti luba ei saa anda tavalisest päringudialoogist. Ava Health Connect ja luba need OpenVitalsi jaoks.';

  @override
  String get settingsOpenHealthPermissions => 'Ava Health Connecti load';

  @override
  String get settingsDisplayGroupTitle => 'Kuva';

  @override
  String get settingsDisplayGroupBody => 'Keel, ühikud ja teema';

  @override
  String get settingsActivitiesGroupTitle => 'Treeningud';

  @override
  String get settingsActivitiesGroupBody =>
      'Veerevad kuupäevad, lemmiktreening, salvestamine ja võrguühenduseta kaardid';

  @override
  String get settingsSensorsGroupTitle => 'Andurid ja seadmed';

  @override
  String get settingsSensorsGroupBody => 'Pulsi-, sagedus- ja võimsusandurid';

  @override
  String get settingsSensorsEmptyTitle => 'Andureid veel pole';

  @override
  String get settingsSensorsEmptyBody =>
      'Lisa Bluetoothi pulsivöö, sagedusandur, võimsusmõõtja või jalaandur treeningu salvestamiseks.';

  @override
  String get settingsSensorsAddDevice => 'Lisa andur';

  @override
  String get settingsSensorsEditDevice => 'Muuda andurit';

  @override
  String get settingsSensorsRemoveDevice => 'Eemalda andur';

  @override
  String get settingsSensorsDeviceName => 'Seadme nimi';

  @override
  String get settingsSensorsEnabled => 'Lubatud';

  @override
  String settingsSensorsBatteryPercent(int arg0) {
    return 'Aku $arg0%';
  }

  @override
  String get settingsSensorsBatteryUnknown => 'Aku ootel';

  @override
  String get settingsSensorsScanning => 'Otsin läheduses olevaid andureid…';

  @override
  String get settingsSensorsScanStopped => 'Otsing peatatud';

  @override
  String get settingsSensorsScanEmpty =>
      'Andureid ei leitud. Veendu, et su andur on aktiivne ja telefoni lähedal.';

  @override
  String get settingsSensorsShowAllDevices => 'Näita kõiki seadmeid';

  @override
  String get settingsSensorsOpenBluetooth => 'Ava Bluetoothi seaded';

  @override
  String get settingsSensorsDiscovering => 'Anduri võimekuste tuvastamine…';

  @override
  String get settingsSensorsCapabilitiesTitle => 'Võimekused';

  @override
  String get settingsSensorsCapabilityHeartRate => 'Pulss';

  @override
  String get settingsSensorsCapabilityCyclingCadence => 'Rattasõidu sagedus';

  @override
  String get settingsSensorsCapabilityCyclingPower => 'Rattasõidu võimsus';

  @override
  String get settingsSensorsCapabilityCyclingSpeed => 'Rattasõidu kiirus';

  @override
  String get settingsSensorsCapabilityRunningSpeedCadence =>
      'Jooksu kiirus/sagedus';

  @override
  String settingsSensorsCapabilityConflict(String arg0, String arg1) {
    return '$arg0 on juba määratud seadmele $arg1';
  }

  @override
  String get settingsSensorsWheelCircumference => 'Ratta ümbermõõt (mm)';

  @override
  String get activityRecordingSensorsTitle => 'Andurid';

  @override
  String get activityRecordingSensorsAddInSettings => 'Lisa andureid seadetes';

  @override
  String get activityRecordingSensorsNotConfigured => 'Pole seadistatud';

  @override
  String get activityRecordingSensorsConnected => 'Ühendatud';

  @override
  String get activityRecordingSensorsConnecting => 'Ühendumine';

  @override
  String get activityRecordingSensorsReconnecting => 'Taasühendumine';

  @override
  String get activityRecordingSensorsDisabled => 'Keelatud';

  @override
  String get activityRecordingSensorsWaitingForData => 'Ootan anduriandmeid…';

  @override
  String get activityRecordingSensorsWaitingShort => '—';

  @override
  String get activityRecordingSensorsNoSignalShort => 'Signaal puudub';

  @override
  String get activityRecordingSensorsGarminBroadcastHint =>
      'Ühendatud, kuid kell ei edasta pulssi. Garminil: Seaded → Randmeandurid → Randme pulss → Edasta pulssi, seejärel käivita see kellal. Ühenda enne Gadgetbridge lahti või kasuta hoopis BLE rinnavööd.';

  @override
  String get activityRecordingSensorsRecordedTitle =>
      'Salvestatud anduriandmed';

  @override
  String get activityRecordingLiveHeartRate => 'Pulss';

  @override
  String get activityRecordingLiveCadence => 'Sagedus';

  @override
  String get activityRecordingLivePower => 'Võimsus';

  @override
  String get activityRecordingLiveSpeed => 'Kiirus';

  @override
  String activityRecordingNotificationHeartRate(String arg0) {
    return 'Pulss $arg0';
  }

  @override
  String get settingsNutritionGroupTitle => 'Toitumine';

  @override
  String get settingsNutritionGroupBody =>
      'Kaloriandmed ja kofeiini isikupärastamine';

  @override
  String get settingsCaloriesGroupTitle => 'Kalorid';

  @override
  String get settingsCaloriesGroupBody => 'Kalorite koguandmed';

  @override
  String get settingsCaffeineGroupTitle => 'Kofeiin';

  @override
  String get settingsCaffeineGroupBody =>
      'Poolestusaeg, magamaminekuaeg, unelävi ja isikupärastamine.';

  @override
  String get settingsRecoveryGroupTitle => 'Taastumine';

  @override
  String get settingsRecoveryGroupBody =>
      'Unevahemik ja kehaenergia kalibreerimine';

  @override
  String get settingsSleepGroupTitle => 'Uni';

  @override
  String get settingsSleepGroupBody => 'Unevahemik';

  @override
  String get settingsCycleGroupTitle => 'Menstruaaltsükkel';

  @override
  String get settingsCycleGroupBody => 'Tsükliandmed ja Health Connecti load';

  @override
  String get settingsDataImportGroupTitle => 'Andmete importijad';

  @override
  String get settingsDataImportGroupBody =>
      'Impordi Apple Healthi eksporte, marsruudifaile ja FIT-faile';

  @override
  String get settingsPermissionsGroupTitle => 'Load';

  @override
  String get settingsPermissionsGroupBody =>
      'Terviseandmete juurdepääs ja käsitsi lubade sammud';

  @override
  String get settingsHealthConnectGroupTitle => 'Health Connect';

  @override
  String get settingsHealthConnectGroupBody =>
      'Sünkroonimine, load, juurdepääs ja rakenduse lukk';

  @override
  String get settingsDebugDiagnosticsGroupTitle => 'Silumisdiagnostika';

  @override
  String get settingsDebugDiagnosticsGroupBody =>
      'Salvesta tõrkeotsinguks anonümiseeritud diagnostikalogid';

  @override
  String get settingsHealthConnectSyncTitle => 'Sünkrooni Health Connectiga';

  @override
  String get settingsHealthConnectSyncBody =>
      'Kui sees, loeb ja kirjutab OpenVitals terviseandmeid vastavalt sinu lubadele. Kui väljas, sünkroonimine peatub ilma juurdepääsu tühistamata.';

  @override
  String get settingsHealthConnectManageAccess => 'Halda juurdepääsu';

  @override
  String get settingsHealthConnectManageAccessBody =>
      'Ava Health Connect, et vaadata või muuta, milliseid andmeid OpenVitals saab kasutada.';

  @override
  String get healthConnectAccessInsufficientTitle => 'Vali jagatavad andmed';

  @override
  String get healthConnectAccessInsufficientBody =>
      'OpenVitals vajab selle teabe näitamiseks Health Connecti juurdepääsu. Seadista andmed, mida soovid jagada, et jätkata.';

  @override
  String get healthConnectAccessDoubleCancelTitle => 'Load vajavad tähelepanu';

  @override
  String get healthConnectAccessDoubleCancelBody =>
      'Health Connecti lube ei antud. Ava Health Connecti seaded, et valida, milliseid andmeid OpenVitalsiga jagada.';

  @override
  String get healthConnectSyncPaused =>
      'Health Connecti sünkroonimine on peatatud';

  @override
  String get healthConnectSyncInProgress => 'Sünkroonimine Health Connectiga…';

  @override
  String get healthConnectDataSourceManage => 'Halda andmeallikaid';

  @override
  String get healthConnectDataSourceManageBody =>
      'Vaata, millised rakendused kirjutavad andmeid Health Connectisse, ja halda nende juurdepääsu.';

  @override
  String get dashboardHealthConnectPromoTitle => 'Seadista oma terviseandmed';

  @override
  String get dashboardHealthConnectPromoBody =>
      'Saa ühtne ülevaade oma treeningutest, unest ja pulsiandmetest rakendustest ja seadmetest, mida juba kasutad.';

  @override
  String get dashboardHealthConnectPromoAction => 'Alusta';

  @override
  String get dashboardHealthConnectSyncPausedBody =>
      'Lülita seadetes sünkroonimine tagasi sisse, et värskendada oma kokkuvõtet.';

  @override
  String get dashboardHealthConnectInstallAction => 'Installi Health Connect';

  @override
  String get healthConnectMatchmakingTitle => 'Ühenda oma rakendused';

  @override
  String get healthConnectMatchmakingBody =>
      'Leia rakendused ja seadmed, mis saavad jagada andmeid, mida OpenVitals on valmis lugema.';

  @override
  String get healthConnectMatchmakingAction => 'Leia andmeallikad';

  @override
  String get healthConnectPromoteActivityTitle => 'Ava treeningu ülevaated';

  @override
  String get healthConnectPromoteActivityBody =>
      'Luba treeninguandmed, et näha samme, vahemaad, treeninguid ja trende OpenVitalsis.';

  @override
  String get healthConnectPromoteActivitiesTitle => 'Vaata oma treeninguid';

  @override
  String get healthConnectPromoteActivitiesBody =>
      'Luba treeningsessioonide juurdepääs, et sirvida Health Connecti kaudu sünkroonitud treeninguid.';

  @override
  String get healthConnectPromoteCaloriesTitle => 'Jälgi kulutatud kaloreid';

  @override
  String get healthConnectPromoteCaloriesBody =>
      'Luba kaloriandmed, et võrrelda aktiivset ja kogukulu aja jooksul.';

  @override
  String get healthConnectPromoteSleepTitle => 'Vaata oma und';

  @override
  String get healthConnectPromoteSleepBody =>
      'Luba uneandmed, et näha faase, kestust ja unehinde trende.';

  @override
  String get healthConnectPromoteHeartTitle => 'Jälgi südame tervist';

  @override
  String get healthConnectPromoteHeartBody =>
      'Luba pulsi- ja HRV-andmed, et jälgida puhkeoleku pulssi ja varieeruvust.';

  @override
  String get healthConnectPromoteVitalsTitle => 'Ava elunäitajad';

  @override
  String get healthConnectPromoteVitalsBody =>
      'Luba elunäitajate andmed, et näha vererõhku, SpO2 ja seotud mõõtmisi.';

  @override
  String get healthConnectPromoteBodyTitle => 'Jälgi kehanäitajaid';

  @override
  String get healthConnectPromoteBodyBody =>
      'Luba kehakoostise andmed, et jälgida kaalu, BMI-d ja seotud trende.';

  @override
  String get healthConnectPromoteHydrationTitle => 'Jälgi jooke';

  @override
  String get healthConnectPromoteHydrationBody =>
      'Luba vedelikutarbimise ja toitumise andmed, et näha igapäevaseid jooke ja ajalugu.';

  @override
  String get healthConnectPromoteNutritionTitle => 'Vaata toitumist';

  @override
  String get healthConnectPromoteNutritionBody =>
      'Luba toitumisandmed, et vaadata kaloreid ja makrotoitaineid oma allikatest.';

  @override
  String get healthConnectPromoteMindfulnessTitle => 'Jälgi teadvelolekut';

  @override
  String get healthConnectPromoteMindfulnessBody =>
      'Luba teadveloleku seansiandmed, et näha oma praktikat aja jooksul.';

  @override
  String get healthConnectPromoteCycleTitle => 'Jälgi tsükliandmeid';

  @override
  String get healthConnectPromoteCycleBody =>
      'Luba menstruaaltsükli andmed, et vaadata voolust, sümptomeid ja seotud kirjeid.';

  @override
  String get healthConnectPromoteReadinessTitle =>
      'Paranda valmisoleku ülevaateid';

  @override
  String get healthConnectPromoteReadinessBody =>
      'Luba täiendavad Health Connecti andmed, et täpsustada igapäevase valmisoleku hindeid.';

  @override
  String get healthConnectNewPermissionsTitle => 'Uued andmed saadaval';

  @override
  String get healthConnectNewPermissionsBody =>
      'OpenVitals saab nüüd lugeda täiendavaid terviseandmete tüüpe. Anna juurdepääs uute funktsioonide kasutamiseks.';

  @override
  String get healthConnectNewPermissionsAction => 'Vaata load üle';

  @override
  String get privacyReconsentTitle => 'Privaatsuspoliitika on uuendatud';

  @override
  String get privacyReconsentBody =>
      'Meie privaatsuspoliitika on muutunud. Vaata üle ja nõustu, et jätkata Health Connectiga sünkroonimist.';

  @override
  String get privacyReconsentAction => 'Vaata poliitika üle';

  @override
  String get dashboardSummaryToday => 'Täna';

  @override
  String get settingsDebugLogsTitle => 'Anonümiseeritud diagnostikalogid';

  @override
  String get settingsDebugLogsBody =>
      'Jaga või salvesta OpenVitalsi diagnostikalogi kirjed tekstifailina. Eksport eemaldab või varjab identifikaatorid, asukohad, kuupäevad, URI-d, toored anduriandmed ja mitteseotud rakenduslogid enne kirjutamist.';

  @override
  String get settingsDebugLogsShare => 'Jaga logisid';

  @override
  String get settingsDebugLogsShareChooserTitle => 'Jaga diagnostikalogisid';

  @override
  String get settingsDebugLogsShareFailed =>
      'Diagnostikalogide jagamine ebaõnnestus';

  @override
  String get settingsDebugLogsSave => 'Salvesta logid';

  @override
  String get settingsDebugLogsSaved => 'Silumislogid salvestatud';

  @override
  String get settingsDebugLogsSaveFailed =>
      'Diagnostikalogide salvestamine ebaõnnestus';

  @override
  String get settingsPrivacyPolicyLink => 'Vaata privaatsuspoliitikat';

  @override
  String get settingsAppLockTitle => 'Rakenduse lukk';

  @override
  String get settingsAppLockBody =>
      'Nõua OpenVitalsi avamiseks seadme lukust vabastamist.';

  @override
  String get onboardingCoreRequired =>
      'Alustamiseks on vaja treeningu, une ja pulsi juurdepääsu. Rohkem andmetüüpe saad hiljem lisada seadetest.';

  @override
  String get settingsLanguageTitle => 'Keel';

  @override
  String get settingsLanguageBody =>
      'Vali rakenduse keel või järgi süsteemi seadistust.';

  @override
  String get settingsLanguageSystem => 'Süsteem';

  @override
  String get settingsLanguageEnglish => 'Inglise';

  @override
  String get settingsLanguageSpanish => 'Hispaania';

  @override
  String get settingsLanguageGerman => 'Saksa';

  @override
  String get settingsLanguageItalian => 'Itaalia';

  @override
  String get settingsLanguageEstonian => 'Eesti';

  @override
  String get settingsUnitsTitle => 'Ühikud';

  @override
  String get settingsUnitsBody =>
      'Vali, kuidas vahemaid, kaalu, vedelikutarbimist ja temperatuuri kuvatakse.';

  @override
  String get settingsUnitMetric => 'Meetriline';

  @override
  String get settingsUnitImperial => 'Inglise';

  @override
  String get settingsThemeTitle => 'Teema';

  @override
  String get settingsThemeBody =>
      'Vali rakenduse välimus Androidi tumeda režiimi seadest sõltumatult.';

  @override
  String get settingsThemeSystem => 'Süsteem';

  @override
  String get settingsThemeLight => 'Hele';

  @override
  String get settingsThemeDark => 'Tume';

  @override
  String get settingsThemeAmoled => 'AMOLED';

  @override
  String get settingsDynamicColorTitle => 'Dünaamilised värvid (Material You)';

  @override
  String get settingsDynamicColorBody =>
      'Tooni OpenVitals Androidi taustapildi järgi. Väljas kasutatakse OpenVitalsi sinist ja türkiissinist brändipaletti.';

  @override
  String get settingsActivityWeekTitle => 'Veerevad kuupäevad';

  @override
  String get settingsActivityWeekBody =>
      'Kasuta kalendrinädala, -kuu ja -aasta asemel veerevaid 7, 30 ja 365 päeva aknaid.';

  @override
  String get settingsActivityWeekMondayToSunday => 'Kalender';

  @override
  String get settingsActivityWeekLast7Days => 'Veerev';

  @override
  String get settingsFavoriteActivityTitle => 'Lemmiktreening';

  @override
  String get settingsFavoriteActivityBody =>
      'Kasuta vaikimisi viimati salvestatud treeningut või vali üks treeningu liik, mis alati eelnevalt valitakse.';

  @override
  String get settingsFavoriteActivityLatest => 'Kasuta viimast';

  @override
  String get settingsActivityRecordingTitle => 'Treeningu salvestamine';

  @override
  String get settingsActivityRecordingBody =>
      'Häälesta reaalajas GPS-salvestamist, muutmata salvestatud treeningu üksikasjade töövoogu.';

  @override
  String get settingsActivityRecordingKeepScreenOnTitle => 'Ekraan alati sees';

  @override
  String get settingsActivityRecordingKeepScreenOnBody =>
      'Hoia ekraan ärkvel, kui treeningu salvestamine on aktiivne.';

  @override
  String get settingsActivityRecordingAutoIdleTitle => 'Automaatne passiivsus';

  @override
  String get settingsActivityRecordingAutoIdleBody =>
      'Peata liikumisaeg, kui peatud kauemaks kui valitud ajapiirang.';

  @override
  String get settingsActivityRecordingIdleTimeoutTitle =>
      'Passiivsuse ajapiirang';

  @override
  String get settingsActivityRecordingAccuracyTitle => 'Nõutav GPS-täpsus';

  @override
  String get settingsActivityRecordingRouteGapTitle =>
      'Uus marsruudilõik pärast katkestust';

  @override
  String get settingsActivityRecordingTimeIntervalTitle =>
      'Salvestuse ajaintervall';

  @override
  String get settingsActivityRecordingDistanceIntervalTitle =>
      'Salvestuse vahemaaintervall';

  @override
  String get settingsActivityRecordingBarometerTitle => 'Baromeetriline tõus';

  @override
  String get settingsActivityRecordingBarometerBody =>
      'Kasuta tõusuks rõhumuutusi, kui seadmel on baromeeter.';

  @override
  String get settingsActivityRecordingRestBellTitle => 'Puhkeaja kell';

  @override
  String get settingsActivityRecordingRestBellBody =>
      'Mängi pehmet kellahelinat, kui seeria puhkeaja loendus lõpeb.';

  @override
  String get settingsActivityRecordingVoiceTitle => 'Häälteated';

  @override
  String get settingsActivityRecordingVoiceBody =>
      'Räägi perioodilist edenemist, passiivsust/jätkamist ja ringiuuendusi salvestamise ajal.';

  @override
  String get settingsActivityRecordingVoiceTimeTitle => 'Teata aja järgi';

  @override
  String get settingsActivityRecordingVoiceDistanceTitle =>
      'Teata vahemaa järgi';

  @override
  String get settingsActivityRecordingVoiceIdleTitle => 'Passiivsusteated';

  @override
  String get settingsActivityRecordingVoiceIdleBody =>
      'Ütle, millal automaatne passiivsus algab ja millal salvestamine jätkub.';

  @override
  String get settingsActivityRecordingVoiceLapTitle => 'Ringiteated';

  @override
  String get settingsActivityRecordingVoiceLapBody =>
      'Ütle edenemise kokkuvõte, kui märgid ringi.';

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
  String get settingsActivityRecordingAuto => 'Automaatne';

  @override
  String get settingsActivityRecordingOff => 'Väljas';

  @override
  String get settingsCalorieDataTitle => 'Kalorite koguandmed';

  @override
  String get settingsCalorieDataBody =>
      'Näita vaikimisi Health Connecti tavalisi kogukaloreid. Lülita sisse OpenVitalsi arvutused, et täita puuduvad kogusummad aktiivsete kalorite ja BMR-i põhjal.';

  @override
  String get settingsCaffeineTitle => 'Kofeiinimudel';

  @override
  String get settingsCaffeineBody =>
      'Need väärtused isikupärastavad kofeiinitaset, magamaminekuprognoosi ja ohutu une ülevaateid. Kirjed jäävad Health Connectisse.';

  @override
  String get settingsBodyProfileTitle => 'Kehaprofiil';

  @override
  String get settingsBodyProfileBody =>
      'Vanus, kaal ja pulss isikupärastavad kehaenergia ja kofeiini hinnanguid. Kõik väljad on valikulised.';

  @override
  String get settingsBodyProfileWeight => 'Kaal';

  @override
  String get settingsSleepRangeTitle => 'Unevahemik';

  @override
  String get settingsSleepRangeBody =>
      'Vali, millisele päevale unesessioonid määratakse.';

  @override
  String get settingsSleepRangeRolling24h => 'Libisev 24h';

  @override
  String get settingsSleepRangeNoon => 'Keskpäev';

  @override
  String get settingsSleepRangeEvening => '18:00';

  @override
  String get settingsCyclePermissionsTitle => 'Tsükli load';

  @override
  String settingsCyclePermissionsGranted(int arg0, int arg1) {
    return '$arg0/$arg1 tsükli luba antud.';
  }

  @override
  String get settingsAppleHealthImportTitle => 'Apple Healthi importija';

  @override
  String get settingsAppleHealthImportBody =>
      'Impordi Health Connectiga ühilduvad kirjed Apple Healthi export.xml või export.zip failist, koos duplikaatide kontrolli ja jagatava diagnostikaraportiga.';

  @override
  String settingsAppleHealthImportPermissions(int arg0, int arg1) {
    return '$arg0/$arg1 impordi luba antud.';
  }

  @override
  String get settingsAppleHealthImportGrant => 'Anna impordi load';

  @override
  String get settingsAppleHealthImportAction => 'Impordi Apple Healthi eksport';

  @override
  String get settingsAppleHealthImportAnalyzeAction =>
      'Analüüsi Apple Healthi eksporti';

  @override
  String get settingsAppleHealthImportChooseAnotherAction =>
      'Vali teine Apple Healthi eksport';

  @override
  String get settingsAppleHealthImportSelectedAction =>
      'Impordi valitud kategooriad';

  @override
  String get settingsAppleHealthImportAnalyzing => 'Analüüsimine...';

  @override
  String get settingsAppleHealthImporting => 'Importimine...';

  @override
  String get settingsAppleHealthImportProgressQueued => 'Järjekorras';

  @override
  String get settingsAppleHealthImportProgressParsing =>
      'Ekspordi skaneerimine';

  @override
  String get settingsAppleHealthImportProgressConverting =>
      'Kirjete teisendamine';

  @override
  String get settingsAppleHealthImportProgressCheckingDuplicates =>
      'Duplikaatide kontrollimine';

  @override
  String get settingsAppleHealthImportProgressWriting => 'Kirjete kirjutamine';

  @override
  String get settingsAppleHealthImportProgressFinishing => 'Impordi lõpetamine';

  @override
  String get settingsAppleHealthImportProgressBuildingReport =>
      'Raporti koostamine';

  @override
  String get settingsAppleHealthImportProgressComplete => 'Valmis';

  @override
  String settingsAppleHealthImportProgress(String arg0, int arg1, int arg2) {
    return '$arg0. Skaneeritud $arg1 elementi, imporditud $arg2 kirjet.';
  }

  @override
  String settingsAppleHealthImportProgressWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Valitud $arg2/$arg3 kirjet, imporditud $arg4.';
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
      'Import jätkub taustal, kui lahkud rakendusest.';

  @override
  String get settingsAppleHealthImportNotificationChannel =>
      'Apple Healthi impordid';

  @override
  String get settingsAppleHealthImportNotificationTitle =>
      'Apple Healthi ekspordi importimine';

  @override
  String settingsAppleHealthImportNotificationText(
    String arg0,
    int arg1,
    int arg2,
  ) {
    return '$arg0. Skaneeritud $arg1, imporditud $arg2.';
  }

  @override
  String settingsAppleHealthImportNotificationTextWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Valitud $arg2/$arg3, imporditud $arg4.';
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
    return 'Imporditud $arg0. Duplikaate $arg1. Valimata $arg2. Toetamata $arg3. Vahele jäetud $arg4. Ebaõnnestus $arg5.';
  }

  @override
  String get settingsAppleHealthImportRoutesIncomplete =>
      'Tervisandmed imporditi, kuid mõned treeningmarsruudid jäid kättesaamatuks, kuna ZIP-fail lõppes ootamatult. Impordiaruandes on loetletud asjaomased tegevused, mida saab käsitsi taastada.';

  @override
  String settingsAppleHealthImportAnalysisResult(
    int arg0,
    int arg1,
    int arg2,
    int arg3,
  ) {
    return 'Skaneeritud $arg0 elementi. Leiti $arg1 ühilduvat kirjet. Toetamata $arg2. Ebaõnnestus $arg3.';
  }

  @override
  String get settingsAppleHealthImportChooseCategories =>
      'Vali, mis kirjutatakse Health Connectisse.';

  @override
  String settingsAppleHealthImportCategoryCount(int arg0) {
    return '$arg0 kirjet';
  }

  @override
  String settingsAppleHealthImportCategoryCountRoutes(int arg0, int arg1) {
    return '$arg0 kirjet, $arg1 koos marsruudiga';
  }

  @override
  String get settingsAppleHealthImportCategoryWorkouts =>
      'Treeningud ja marsruudid';

  @override
  String get settingsAppleHealthImportCategoryWorkoutsDesc =>
      'Treeningsessioonid ja lisatud treeningu marsruudi geomeetria.';

  @override
  String get settingsAppleHealthImportCategoryActivity => 'Treeninguandmed';

  @override
  String get settingsAppleHealthImportCategoryActivityDesc =>
      'Sammud, vahemaa, kalorid, korrused, kõrgus, ratastooli lükked ja kiirus.';

  @override
  String get settingsAppleHealthImportCategoryHeart => 'Süda';

  @override
  String get settingsAppleHealthImportCategoryHeartDesc =>
      'Pulsi ja puhkeoleku pulsi kirjed.';

  @override
  String get settingsAppleHealthImportCategorySleep => 'Uni';

  @override
  String get settingsAppleHealthImportCategorySleepDesc =>
      'Unesessioonid ja -faasid.';

  @override
  String get settingsAppleHealthImportCategoryBody => 'Kehamõõtmised';

  @override
  String get settingsAppleHealthImportCategoryBodyDesc =>
      'Kaal, pikkus, keharasv, rasvavaba mass, BMR, luumass ja kehavesi.';

  @override
  String get settingsAppleHealthImportCategoryVitals => 'Elunäitajad';

  @override
  String get settingsAppleHealthImportCategoryVitalsDesc =>
      'Vererõhk, hapnikusaturatsioon, hingamissagedus, kehatemperatuur, veresuhkur ja VO2 max.';

  @override
  String get settingsAppleHealthImportCategoryNutrition => 'Toitumine';

  @override
  String get settingsAppleHealthImportCategoryNutritionDesc =>
      'Toiduenergia, makrotoitained, kofeiin, mineraalid ja vitamiinid.';

  @override
  String get settingsAppleHealthImportCategoryHydration => 'Vedelikutarbimine';

  @override
  String get settingsAppleHealthImportCategoryHydrationDesc =>
      'Veetarbimise kirjed.';

  @override
  String get settingsAppleHealthImportCategoryMindfulness => 'Teadvelolek';

  @override
  String get settingsAppleHealthImportCategoryMindfulnessDesc =>
      'Teadveloleku seansi kirjed, kui Health Connect neid toetab.';

  @override
  String get settingsAppleHealthImportCategoryCycle => 'Tsükli jälgimine';

  @override
  String get settingsAppleHealthImportCategoryCycleDesc =>
      'Menstruatsiooni, ovulatsiooni, emakakaelalima, veritsuse, basaaltemperatuuri ja seksuaalse aktiivsuse kirjed.';

  @override
  String get settingsAppleHealthImportCopyReport => 'Kopeeri raport';

  @override
  String get settingsAppleHealthImportCopyError => 'Kopeeri viga';

  @override
  String get settingsAppleHealthImportSaveReport => 'Laadi alla täielik raport';

  @override
  String get settingsAppleHealthImportReportCopied =>
      'Impordiraport kopeeritud.';

  @override
  String get settingsAppleHealthImportErrorCopied => 'Impordi viga kopeeritud.';

  @override
  String get settingsAppleHealthImportReportSaved =>
      'Impordiraport salvestatud.';

  @override
  String get settingsAppleHealthImportReportSaveFailed =>
      'Impordiraportit ei õnnestunud salvestada.';

  @override
  String settingsAppleHealthImportError(String arg0) {
    return 'Import ebaõnnestus: $arg0';
  }

  @override
  String get settingsAppleHealthImportPermissionDenied =>
      'Ligipääs valitud failile kadus, mistõttu importimist ei saanud jätkata. Vali sama Apple Health\'i eksport uuesti, et jätkata täpselt sealt, kus pooleli jäi.';

  @override
  String get settingsRouteImportTitle => 'GPX/KML/KMZ-importija';

  @override
  String get settingsRouteImportBody =>
      'Impordi GPX-, KML- või KMZ-marsruudifaile. Vaata üks fail enne salvestamist üle või impordi mitu faili otse Health Connectisse.';

  @override
  String settingsRouteImportPermissions(int arg0, int arg1) {
    return '$arg0/$arg1 marsruudiimpordi õigust antud.';
  }

  @override
  String get settingsRouteImportGrant => 'Anna marsruudiimpordi õigused';

  @override
  String get settingsRouteImportAction => 'Impordi GPX/KML/KMZ-fail';

  @override
  String get settingsRouteImportBulkAction =>
      'Impordi GPX/KML/KMZ-failid hulgana';

  @override
  String get settingsRouteImporting => 'Marsruute imporditakse...';

  @override
  String settingsRouteImportProgress(int arg0, int arg1, int arg2, int arg3) {
    return 'Fail $arg0/$arg1. Imporditud $arg2, ebaõnnestunud $arg3.';
  }

  @override
  String settingsRouteImportResult(int arg0, int arg1, int arg2) {
    return 'Imporditud $arg0. Ebaõnnestunud $arg1. Valitud $arg2.';
  }

  @override
  String settingsRouteImportError(String arg0) {
    return 'Marsruudiimpordi hoiatus: $arg0';
  }

  @override
  String get settingsFitImportTitle => 'FIT-importija';

  @override
  String get settingsFitImportBody =>
      'Impordi FIT treeningu-, marsruudi- või harjutusfaile, vaata tuvastatud üksikasjad üle ja vali, kas salvestada need Health Connectisse.';

  @override
  String get settingsFitImportAction => 'Impordi FIT-fail';

  @override
  String get settingsOfflineMapsTitle => 'Võrguühenduseta kaardid';

  @override
  String get settingsOfflineMapsBody =>
      'Impordi PMTiles või Mapsforge .map/.maps paketid täielikult võrguühenduseta treeningukaartide jaoks. Toetatud on Protomapsiga ühilduvad PMTiles-i põhikaardid ja Mapsforge kaardid.';

  @override
  String get settingsOfflineMapsEmpty =>
      'Võrguühenduseta kaarte pole veel imporditud.';

  @override
  String get settingsOfflineMapsFormatPmtiles => 'PMTiles';

  @override
  String get settingsOfflineMapsFormatMapsforge => 'Mapsforge';

  @override
  String get settingsOfflineMapsRenderFormatTitle => 'Renderdusvorming';

  @override
  String settingsOfflineMapsRenderFormatOption(String arg0, int arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String get settingsOfflineMapsRenderFormatBody =>
      'OpenVitals renderdab kõik imporditud paketid koos valitud vormingus.';

  @override
  String settingsOfflineMapsPackDetail(String arg0, String arg1, String arg2) {
    return '$arg0 • $arg1 • $arg2';
  }

  @override
  String get settingsOfflineMapsImportAction => 'Impordi võrguühenduseta kaart';

  @override
  String get settingsOfflineMapsImporting => 'Importimine...';

  @override
  String get settingsOfflineMapsImportProgressQueued => 'Järjekorras';

  @override
  String get settingsOfflineMapsImportProgressCopying => 'Kaardi kopeerimine';

  @override
  String get settingsOfflineMapsImportProgressComplete => 'Valmis';

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
      'Import jätkub taustal, kui lahkud rakendusest.';

  @override
  String settingsOfflineMapsImportResult(String arg0, String arg1) {
    return 'Imporditud $arg0 ($arg1).';
  }

  @override
  String settingsOfflineMapsImportError(String arg0) {
    return 'Kaardi import ebaõnnestus: $arg0';
  }

  @override
  String get settingsOfflineMapsImportNotificationChannel =>
      'Võrguühenduseta kaartide impordid';

  @override
  String get settingsOfflineMapsImportNotificationTitle =>
      'Võrguühenduseta kaardi importimine';

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
      'Kas soovid teada, kuidas võrguühenduseta kaarte lisada? Mine:';

  @override
  String get settingsOfflineMapsHelpLink =>
      'Ava võrguühenduseta kaartide juhend';

  @override
  String get sectionSupport => 'Tugi';

  @override
  String get settingsSupportTitle => 'Toeta OpenVitalsi';

  @override
  String get settingsSupportBody =>
      'Teata vigadest, liitu kogukonna tugiaruteludega või aita rahastada arendustööd.';

  @override
  String get settingsSupportIssuesAction => 'Teata probleemist';

  @override
  String get settingsSupportDiscussionAction => 'Liitu Zulipi aruteludega';

  @override
  String get settingsSupportAction => 'Ava Liberapay';

  @override
  String get crashReportEmailChooserTitle =>
      'Saada OpenVitalsi raport e-kirjaga';

  @override
  String get crashReportFallbackTitle => 'E-posti rakendust ei leitud';

  @override
  String crashReportFallbackBody(String arg0) {
    return 'Kopeeri raport või salvesta see tekstifailina ning saada see hiljem aadressile $arg0.';
  }

  @override
  String get crashReportFallbackCopy => 'Kopeeri raport';

  @override
  String get crashReportFallbackSave => 'Salvesta tekstifail';

  @override
  String get crashReportFallbackCopied => 'Raport kopeeritud.';

  @override
  String get crashReportFallbackSaved => 'Raport salvestatud.';

  @override
  String get crashReportFallbackSaveFailed =>
      'Raportit ei õnnestunud salvestada.';

  @override
  String get crashReportFallbackSaveUnavailable =>
      'Failisalvestajat ei leitud. Raport kopeeritud.';

  @override
  String get crashReportClipboardLabel => 'OpenVitalsi raport';

  @override
  String get settingsPrivacyNoAccount => 'Kontot ei ole vaja';

  @override
  String get settingsPrivacyNoCloud =>
      'Terviseandmete pilvesünkroonimist ei ole';

  @override
  String get settingsPrivacyNoAnalytics => 'Analüütika SDK-d ei ole';

  @override
  String get settingsPrivacyNoAds =>
      'Reklaame ega kolmandate osapoolte jälgimist ei ole';

  @override
  String get settingsPrivacyOnDevice => 'Andmed jäävad sinu seadmesse';

  @override
  String get settingsPrivacyReadOnly =>
      'Ainult loetav, välja arvatud kirjed, mida sa ise logid';

  @override
  String settingsAppVersion(String arg0, int arg1) {
    return 'Versioon $arg0 ($arg1)';
  }

  @override
  String get detailMetrics => 'Näitajad';

  @override
  String get detailSessionDetails => 'Sessiooni üksikasjad';

  @override
  String get detailDuration => 'Kestus';

  @override
  String get detailMovingTime => 'Liikumisaeg';

  @override
  String get detailType => 'Liik';

  @override
  String get detailStarted => 'Alustatud';

  @override
  String get detailEnded => 'Lõpetatud';

  @override
  String get detailStartZone => 'Alustsoon';

  @override
  String get detailEndZone => 'Lõputsoon';

  @override
  String get detailRecording => 'Salvestus';

  @override
  String get detailSourcePackage => 'Allika pakett';

  @override
  String get detailDeviceType => 'Seadme liik';

  @override
  String get detailDeviceMaker => 'Seadme tootja';

  @override
  String get detailDeviceModel => 'Seadme mudel';

  @override
  String get detailLastModified => 'Viimati muudetud';

  @override
  String get detailRecordId => 'Kirje ID';

  @override
  String get detailClientRecordId => 'Kliendi kirje ID';

  @override
  String get detailClientVersion => 'Kliendi versioon';

  @override
  String get detailPlannedSessionId => 'Planeeritud sessiooni ID';

  @override
  String get detailNotes => 'Märkmed';

  @override
  String get detailTitle => 'Pealkiri';

  @override
  String get detailTime => 'Aeg';

  @override
  String get detailRepetitions => 'Kordused';

  @override
  String get detailSet => 'Seeria';

  @override
  String get detailLength => 'Pikkus';

  @override
  String get detailSegments => 'Lõigud';

  @override
  String get detailLaps => 'Ringid';

  @override
  String detailLap(int arg0) {
    return 'Ring $arg0';
  }

  @override
  String get detailRoute => 'Marsruut';

  @override
  String get detailStatus => 'Olek';

  @override
  String get detailStatusAvailable => 'Saadaval';

  @override
  String get detailPoints => 'Punktid';

  @override
  String get detailStartPoint => 'Alguspunkt';

  @override
  String get detailEndPoint => 'Lõpp-punkt';

  @override
  String detailAltitude(String arg0) {
    return 'Kõrgus $arg0';
  }

  @override
  String detailHorizontalAccuracy(String arg0) {
    return 'Horisontaalne täpsus $arg0';
  }

  @override
  String detailVerticalAccuracy(String arg0) {
    return 'Vertikaalne täpsus $arg0';
  }

  @override
  String get detailStageEvents => 'Faasi sündmused';

  @override
  String get detailStages => 'Faasid';

  @override
  String get detailSleepSession => 'Unesessioon';

  @override
  String get recordingActivelyRecorded => 'Aktiivselt salvestatud';

  @override
  String get recordingAutomaticallyRecorded => 'Automaatselt salvestatud';

  @override
  String get recordingManualEntry => 'Käsitsi kirje';

  @override
  String get recordingUnknown => 'Tundmatu';

  @override
  String get deviceWatch => 'Nutikell';

  @override
  String get devicePhone => 'Telefon';

  @override
  String get deviceScale => 'Kaal';

  @override
  String get deviceRing => 'Sõrmus';

  @override
  String get deviceHeadMounted => 'Peakinnitusega';

  @override
  String get deviceFitnessBand => 'Fitnessikäevõru';

  @override
  String get deviceChestStrap => 'Rinnavöö';

  @override
  String get deviceSmartDisplay => 'Nutikuvar';

  @override
  String get sleepStageAwake => 'Ärkvel';

  @override
  String get sleepStageSleeping => 'Magab';

  @override
  String get sleepStageOutOfBed => 'Väljas voodist';

  @override
  String get sleepStageLight => 'Kerge';

  @override
  String get sleepStageDeep => 'Sügav';

  @override
  String get sleepStageRem => 'REM';

  @override
  String get sleepStageAwakeInBed => 'Ärkvel voodis';

  @override
  String get sleepStageUnknown => 'Tundmatu';

  @override
  String get sleepStagesShareTitle => 'Osakaal voodis veedetud ajast';

  @override
  String get cyclePermissionsMissingTitle => 'Tsükli load puuduvad';

  @override
  String get cyclePermissionsMissingBody =>
      'Anna tsükli jälgimise load, et näidata menstruatsioonipäevi, ovulatsioonitesti, emakakaelalima ja basaaltemperatuuri.';

  @override
  String get cycleObservationMenstruationPeriod => 'Menstruatsiooniperiood';

  @override
  String get cycleObservationMenstruationFlow => 'Menstruatsiooni voolus';

  @override
  String get cycleObservationOvulationTest => 'Ovulatsioonitest';

  @override
  String get cycleObservationCervicalMucus => 'Emakakaelalima';

  @override
  String get cycleObservationBasalBodyTemperature => 'Basaaltemperatuur';

  @override
  String get cycleObservationIntermenstrualBleeding =>
      'Menstruatsioonidevaheline veritsus';

  @override
  String get cycleObservationSexualActivity => 'Seksuaalne aktiivsus';

  @override
  String get cycleProtectionProtected => 'Kaitstud';

  @override
  String get cycleProtectionUnprotected => 'Kaitsmata';

  @override
  String get cycleProtectionUnknown => 'Kaitse teadmata';

  @override
  String cycleBasalTemperatureValue(String arg1) {
    return '%1\$.1f C · $arg1';
  }

  @override
  String cycleDaysValue(int arg0, String arg1) {
    return '$arg0 $arg1';
  }

  @override
  String get cycleDaySingular => 'päev';

  @override
  String get cycleDayPlural => 'päeva';

  @override
  String get cycleFlowLight => 'Kerge';

  @override
  String get cycleFlowMedium => 'Keskmine';

  @override
  String get cycleFlowHeavy => 'Tugev';

  @override
  String get cycleOvulationPositive => 'Positiivne';

  @override
  String get cycleOvulationHigh => 'Kõrge';

  @override
  String get cycleOvulationNegative => 'Negatiivne';

  @override
  String get cycleOvulationInconclusive => 'Ebaselge';

  @override
  String get cycleMucusDry => 'Kuiv';

  @override
  String get cycleMucusSticky => 'Kleepuv';

  @override
  String get cycleMucusCreamy => 'Kreemjas';

  @override
  String get cycleMucusWatery => 'Vesine';

  @override
  String get cycleMucusEggWhite => 'Munavalgejas';

  @override
  String get cycleMucusUnusual => 'Ebatavaline';

  @override
  String get cycleMucusLight => 'kerge';

  @override
  String get cycleMucusMedium => 'keskmine';

  @override
  String get cycleMucusHeavy => 'tugev';

  @override
  String cycleMucusValue(String arg0, String arg1) {
    return '$arg0, $arg1';
  }

  @override
  String get measurementLocationArmpit => 'Kaenlaalune';

  @override
  String get measurementLocationFinger => 'Sõrm';

  @override
  String get measurementLocationForehead => 'Laup';

  @override
  String get measurementLocationMouth => 'Suu';

  @override
  String get measurementLocationRectum => 'Pärasool';

  @override
  String get measurementLocationTemporalArtery => 'Oimuarter';

  @override
  String get measurementLocationToe => 'Varvas';

  @override
  String get measurementLocationEar => 'Kõrv';

  @override
  String get measurementLocationWrist => 'Randme';

  @override
  String get measurementLocationVagina => 'Tupp';

  @override
  String get measurementLocationUnknown => 'Mõõtmiskoht teadmata';

  @override
  String get weekdayMondayShort => 'E';

  @override
  String get weekdayTuesdayShort => 'T';

  @override
  String get weekdayWednesdayShort => 'K';

  @override
  String get weekdayThursdayShort => 'N';

  @override
  String get weekdayFridayShort => 'R';

  @override
  String get weekdaySaturdayShort => 'L';

  @override
  String get weekdaySundayShort => 'P';

  @override
  String get vitalsPermissionsNeededTitle => 'Elunäitajate load vajalikud';

  @override
  String get vitalsPermissionsNeededBody =>
      'Anna vererõhu, hapnikusaturatsiooni, hingamissageduse, temperatuuri, VO2 max ja veresuhkru load, et täita see ekraan.';

  @override
  String get vitalsRespiratoryRateReadings => 'Hingamissageduse näidud';

  @override
  String get vitalsBodyTemperatureReadings => 'Kehatemperatuuri näidud';

  @override
  String get heartRateHealthChecksTitle => 'Pulsikontrollid';

  @override
  String get heartRateHighTitle => 'Kõrge pulss';

  @override
  String get heartRateLowTitle => 'Madal pulss';

  @override
  String heartRateSamplesAtOrAbove(int arg0) {
    return 'Näidud tasemel $arg0 bpm või üle selle';
  }

  @override
  String heartRateSamplesAtOrBelow(int arg0) {
    return 'Näidud tasemel $arg0 bpm või alla selle';
  }

  @override
  String heartRateDaysAtOrAbove(int arg0) {
    return 'Päevad tasemel $arg0 bpm või üle selle';
  }

  @override
  String heartRateDaysAtOrBelow(int arg0) {
    return 'Päevad tasemel $arg0 bpm või alla selle';
  }

  @override
  String get cdDecreaseHrThreshold => 'Vähenda pulsiläve';

  @override
  String get cdIncreaseHrThreshold => 'Suurenda pulsiläve';

  @override
  String get mealBreakfast => 'Hommikusöök';

  @override
  String get mealLunch => 'Lõunasöök';

  @override
  String get mealDinner => 'Õhtusöök';

  @override
  String get mealSnack => 'Vahepala';

  @override
  String get mealGeneric => 'Toidukord';

  @override
  String macroProteinShort(String arg0) {
    return 'V ${arg0}g';
  }

  @override
  String macroCarbsShort(String arg0) {
    return 'S ${arg0}g';
  }

  @override
  String macroFatShort(String arg0) {
    return 'R ${arg0}g';
  }

  @override
  String macroFiber(String arg0) {
    return 'kiudaineid ${arg0}g';
  }

  @override
  String macroSugar(String arg0) {
    return 'suhkrut ${arg0}g';
  }

  @override
  String get caffeineSectionOverview => 'Ülevaade';

  @override
  String get caffeineSectionDashboard => 'Kokkuvõte';

  @override
  String get caffeineSectionAnalytics => 'Analüütika';

  @override
  String get caffeineSectionSleep => 'Mõju unele';

  @override
  String get caffeineSectionSources => 'Allikad';

  @override
  String get caffeineSectionEntries => 'Kirjed';

  @override
  String get caffeineSectionScience => 'Teadus';

  @override
  String get caffeineSetupTitle => 'Isikupärasta kofeiini ülevaated';

  @override
  String get caffeineSetupBody =>
      'OpenVitals leidis kofeiiniandmeid. Isikupärastamine parandab kofeiinikõverat ja magamaminekuprognoosi.';

  @override
  String get caffeineCurrentTitle => 'Aktiivne kofeiin';

  @override
  String get caffeineTodayTotal => 'Tänane kokku';

  @override
  String get caffeineTimeToSafe => 'Aeg ohutuni';

  @override
  String get caffeineSleepStatusUnlikely => 'Mõju unele ebatõenäoline';

  @override
  String caffeineSleepStatusUnlikelyBody(String arg0, String arg1) {
    return '$arg0 aktiivne praegu, alla sinu $arg1 unelävet.';
  }

  @override
  String get caffeineSleepStatusElevatedNow => 'Praegu kõrgenenud';

  @override
  String caffeineSleepStatusElevatedNowBody(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return '$arg0 aktiivne praegu. Hinnanguliselt alla läve $arg1 pärast; magamaminekuprognoos on $arg2 kell $arg3.';
  }

  @override
  String get caffeineSleepStatusMayAffect => 'Võib mõjutada und';

  @override
  String caffeineSleepStatusMayAffectBody(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Magamaminekuprognoos on $arg0 kell $arg1, üle sinu $arg2 läve.';
  }

  @override
  String get caffeinePeriodTotal => 'Perioodi kokku';

  @override
  String get caffeineDailyAverage => 'Päevane keskmine';

  @override
  String get caffeineLoggedDays => 'Logitud päevad';

  @override
  String get caffeinePeakDay => 'Tipppäev';

  @override
  String caffeinePeakDayValue(String arg0, String arg1) {
    return '$arg0 - $arg1';
  }

  @override
  String get caffeineCurveTitle => 'Kofeiinikõver';

  @override
  String caffeineThresholdLine(String arg0) {
    return 'Unelävi $arg0';
  }

  @override
  String get caffeineBedtimeForecast => 'Magamaminekuprognoos';

  @override
  String caffeineBedtimeSummary(String arg0, String arg1) {
    return 'Kell $arg0 läveväärtusega $arg1';
  }

  @override
  String get caffeineSafeNights => 'Ohutud ööd';

  @override
  String get caffeineSafeStreak => 'Ohutu seeria';

  @override
  String get caffeineTopSource => 'Peamine allikas';

  @override
  String get caffeineSleepThreshold => 'Unelävi';

  @override
  String get caffeineDailyImpact => 'Igapäevane ja magamaminekumõju';

  @override
  String get caffeineSafeCalendar => 'Ohutute ööde kalender';

  @override
  String get caffeineSources => 'Allikarakendused';

  @override
  String get caffeineItems => 'Elemendid';

  @override
  String get caffeineInferredCategories => 'Tuletatud kategooriad';

  @override
  String get caffeineTimeOfDay => 'Kellaaeg';

  @override
  String get caffeineEntry => 'Kofeiini kirje';

  @override
  String caffeineInferredCategory(String arg0) {
    return 'Kategooria: $arg0';
  }

  @override
  String caffeineCatalogMatch(String arg0) {
    return 'Kataloog: $arg0';
  }

  @override
  String get caffeineCategory => 'Kategooria';

  @override
  String get caffeineCatalog => 'Kataloog';

  @override
  String caffeineCatalogMatchDetail(String arg0, String arg1, String arg2) {
    return '$arg0, tüüpiline $arg1, $arg2 vaste';
  }

  @override
  String get caffeineHealthConnectSourceLabel => 'Allikas';

  @override
  String get caffeineHealthConnectMealLabel => 'Toidukord';

  @override
  String get caffeineHealthConnectDurationLabel => 'Kestus';

  @override
  String caffeineCurrentContribution(String arg0) {
    return '$arg0 aktiivne';
  }

  @override
  String get caffeineCurrentContributionLabel => 'Praegu';

  @override
  String get caffeineDose => 'Annus';

  @override
  String get caffeinePeak => 'Tipp';

  @override
  String get caffeinePeakTime => 'Tipuaeg';

  @override
  String get caffeineContributionCurve => 'Panuse kõver';

  @override
  String get caffeineEmpty =>
      'Sel perioodil kofeiinikirjeid pole. Vedelikutarbimise või toitumise kaudu lisatud kofeiiniga joogid ilmuvad siia, kui Health Connect sisaldab kofeiini.';

  @override
  String get caffeineScienceTitle => 'Kuidas hinnang töötab';

  @override
  String get caffeineScienceBody =>
      'OpenVitals loeb kofeiini Health Connecti toitumiskirjetest milligrammides, seejärel hindab imendumist sinu seadistatud imendumisakna jooksul ja eksponentsiaalset eliminatsiooni sinu isikupärastatud poolestusaja põhjal.';

  @override
  String get caffeineScienceMeasurements => 'Kasutatud mõõtmised';

  @override
  String get caffeineScienceMeasurementsBody =>
      'Salvestatud annus pärineb alati Health Connectist. Alguse/lõpu aega, kirje nime, toidukorra liiki ja andmete päritolupaketti kasutatakse ajastuse, sobitamise ja analüütikasiltide jaoks. Kataloogivasted ainult annoteerivad kirjeid; need ei asenda kunagi salvestatud annust.';

  @override
  String get caffeineScienceLimits =>
      'See on praktiline populatsioonimudel, mitte meditsiiniline nõuanne. Rasedus, ravimid, maksahaigus, geneetika, suitsetamine, alkohol, tundlikkus ja harjumine võivad kõik muuta kofeiinireaktsiooni.';

  @override
  String get caffeineReferencesTitle => 'Uuringud ja viited';

  @override
  String get caffeineReferenceDrake => 'Kofeiini ajastus ja uni, Drake 2013';

  @override
  String get caffeineReferenceNehlig =>
      'Individuaalne kofeiini metabolism, Nehlig 2018';

  @override
  String get caffeineReferenceEfsa => 'EFSA kofeiini ohutuse ja une märkused';

  @override
  String get caffeineReferenceHealthConnect =>
      'Health Connecti toitumiskirje väljad';

  @override
  String get unknownSource => 'Tundmatu allikas';

  @override
  String get achievementsLegacyTitle => 'Vanad treeningumärgid';

  @override
  String achievementsProgressSummary(int arg0, int arg1) {
    return '$arg0 $arg1-st avatud';
  }

  @override
  String achievementsDataWindow(String arg0, String arg1, String arg2) {
    return '$arg0 kuni $arg1 · $arg2 jälgitud päeva';
  }

  @override
  String get achievementsTrackedDays => 'Jälgitud päevad';

  @override
  String get achievementsBestSteps => 'Parim sammude arv';

  @override
  String get achievementsTotalDistance => 'Vahemaa kokku';

  @override
  String get achievementsBestFloors => 'Parim korruste arv';

  @override
  String get achievementsTotalFloors => 'Korrused kokku';

  @override
  String get achievementsFilterAll => 'Kõik';

  @override
  String get achievementsCategoryDailySteps => 'Igapäevased sammud';

  @override
  String get achievementsCategoryLifetimeDistance => 'Eluaegne vahemaa';

  @override
  String get achievementsCategoryDailyFloors => 'Igapäevased korrused';

  @override
  String get achievementsCategoryLifetimeFloors => 'Eluaegsed korrused';

  @override
  String achievementsDailyStepsRequirement(String arg0) {
    return '$arg0 sammu ühel päeval';
  }

  @override
  String achievementsLifetimeDistanceRequirement(String arg0) {
    return '$arg0 vahemaad kokku';
  }

  @override
  String achievementsDailyFloorsRequirement(String arg0) {
    return '$arg0 korrust ühel päeval';
  }

  @override
  String achievementsLifetimeFloorsRequirement(String arg0) {
    return '$arg0 korrust kokku';
  }

  @override
  String achievementsProgressValue(String arg0, String arg1) {
    return '$arg0 $arg1-st';
  }

  @override
  String achievementsAchievedOn(String arg0) {
    return 'Avatud $arg0';
  }

  @override
  String get achievementsEarnedOnce => 'Teenitud';

  @override
  String achievementsEarnedTimes(int arg0) {
    return '$arg0 korda';
  }

  @override
  String get achievementsLocked => 'Lukustatud';

  @override
  String get achievementsNoDataTitle => 'Treeningu ajalugu puudub';

  @override
  String get achievementsNoDataBody =>
      'Health Connectist ei tulnud tagasi ühtegi sammu- ega vahemaakirjet. Kontrolli, et treeninguandmed on olemas ja vanemate kirjete jaoks on ajaloo juurdepääs antud.';

  @override
  String get achievementsNoFloorDataTitle => 'Korruste andmed puuduvad';

  @override
  String get achievementsNoFloorDataBody =>
      'Korruste märgid avanevad, kui Health Connectis on läbitud korruste andmed.';

  @override
  String get achievementsErrorTitle => 'Saavutused ei ole saadaval';

  @override
  String get dataConfidenceTitle => 'Andmete usaldusväärsus';

  @override
  String get dataConfidenceHigh => 'Kõrge usaldusväärsus';

  @override
  String get dataConfidenceMedium => 'Keskmine usaldusväärsus';

  @override
  String get dataConfidenceLow => 'Madal usaldusväärsus';

  @override
  String dataConfidenceCoverage(int arg0, int arg1, int arg2) {
    return '$arg0 päeva $arg1-st jälgitud ($arg2%)';
  }

  @override
  String dataConfidenceSamples(int arg0) {
    return '$arg0 kirjet';
  }

  @override
  String get dataConfidenceSourceUnavailable =>
      'Selle koondandme jaoks allika üksikasjad ei ole saadaval';

  @override
  String dataConfidenceSourceSingle(String arg0) {
    return 'Allikas: $arg0';
  }

  @override
  String dataConfidenceSourceMixed(String arg0) {
    return 'Segatud allikad: $arg0';
  }

  @override
  String get dataConfidenceKindMeasured => 'Mõõdetud Health Connecti kirjed';

  @override
  String get dataConfidenceKindAggregated =>
      'Koondatud Health Connecti kirjetest';

  @override
  String get dataConfidenceKindCalculated => 'OpenVitalsi arvutatud';

  @override
  String get dataConfidenceKindEstimated =>
      'Hinnanguline või tuletatud väärtus';

  @override
  String get dataConfidenceKindMixed => 'Mõõdetud ja arvutatud andmete segu';

  @override
  String get dataConfidenceWarningLowCoverage =>
      'Puuduvad päevad võivad nõrgendada keskmisi ja trende.';

  @override
  String get dataConfidenceWarningSparse =>
      'Hõredad andmed: trendid ja statistika võivad olla ebastabiilsed.';

  @override
  String get dataConfidenceWarningMixedSources =>
      'Allikamuudatused võivad seletada hüppeid või dubleeruvana tunduvaid andmeid.';

  @override
  String get dataConfidenceWarningManual =>
      'Sel perioodil sisalduvad käsitsi kirjed.';

  @override
  String get dataConfidenceWarningCalculated =>
      'See väärtus on tuletatud, mitte otseselt mõõdetud.';

  @override
  String get dataConfidenceWarningNoSources =>
      'See koondandmestik ei näita allikapõhiseid üksikasju.';

  @override
  String get settingsBodyEnergyGroupTitle => 'Kehaenergia';

  @override
  String get settingsBodyEnergyGroupBody =>
      'Kalibreerimine hinnangulise päevasisese energia ja pingutustsoonide jaoks.';

  @override
  String get bodyEnergyCalibrationTitle => 'Paranda kehaenergia hinnanguid';

  @override
  String get bodyEnergyCalibrationBody =>
      'OpenVitals hindab kulumist pulsi intensiivsuse põhjal aja jooksul. Vanus, maksimaalne pulss, puhkeoleku pulss ja tsoonid aitavad pingutust täpsemalt liigitada.';

  @override
  String get bodyEnergyCalibrationOptionalBody =>
      'See on valikuline. Kui jätad selle vahele, kasutab OpenVitals automaatseid hinnanguid Health Connecti andmetest ja näitab madalamat usaldusväärsust, kui kalibreerimine on ebakindel. Need väärtused jäävad OpenVitalsi seadetesse.';

  @override
  String get bodyEnergyCalibrationBirthYear => 'Sünniaasta';

  @override
  String get bodyEnergyCalibrationMaxHr => 'Maksimaalne pulss';

  @override
  String get bodyEnergyCalibrationRestingHr => 'Puhkeoleku pulss';

  @override
  String get bodyEnergyCalibrationManualZones => 'Käsitsi pulsitsoonid';

  @override
  String get bodyEnergyCalibrationManualZonesBody =>
      'Valikulised bpm alammäärad tsoonidele 1-5.';

  @override
  String get bodyEnergyCalibrationZone1 => 'Tsoon 1 alammäär bpm';

  @override
  String get bodyEnergyCalibrationZone2 => 'Tsoon 2 alammäär bpm';

  @override
  String get bodyEnergyCalibrationZone3 => 'Tsoon 3 alammäär bpm';

  @override
  String get bodyEnergyCalibrationZone4 => 'Tsoon 4 alammäär bpm';

  @override
  String get bodyEnergyCalibrationZone5 => 'Tsoon 5 alammäär bpm';

  @override
  String get bodyEnergyCalibrationUseAuto => 'Kasuta automaatseid hinnanguid';

  @override
  String get bodyEnergyCalibrationSkip => 'Jäta praegu vahele';

  @override
  String get bodyEnergyCalibrationSaved =>
      'Kehaenergia kalibreerimine salvestatud';

  @override
  String get bodyEnergyCalibrationReset =>
      'Kehaenergia kalibreerimine lähtestatud automaatseks';

  @override
  String get bodyEnergyNotSetUp => 'Pole seadistatud';

  @override
  String get bodyEnergyTimelineEstimated => 'OpenVitalsi hinnang';

  @override
  String get bodyEnergyTimelineCurrent => 'Praegune';

  @override
  String get bodyEnergyTimelineStart => 'Algus';

  @override
  String get bodyEnergyTimelineCharged => 'Laetud';

  @override
  String get bodyEnergyTimelineDrained => 'Tühjenenud';

  @override
  String get bodyEnergyTimelineConfidence => 'Usaldusväärsus';

  @override
  String get bodyEnergyTimelineNoData =>
      'Sel perioodil ei ole kasutatavat kehaenergia ajajoont.';

  @override
  String get bodyEnergyTimelineDayTitle => 'Päeva ajajoon';

  @override
  String get bodyEnergyTimelineLowConfidence =>
      'Mõned lõigud on hinnangulised, kuna kalibreerimine või Health Connecti andmed on puudulikud.';

  @override
  String get bodyEnergyWhyTitle => 'Mis seda mõjutas';

  @override
  String get bodyEnergyWhyEmpty =>
      'Miski ei valitsenud sel päeval selgelt laadimist ega tühjenemist veel.';

  @override
  String get bodyEnergyInfluenceSleepRecovery => 'Une taastumine';

  @override
  String get bodyEnergyInfluenceQuietRest => 'Rahulik puhkus';

  @override
  String get bodyEnergyInfluenceExertion => 'Pingutus';

  @override
  String get bodyEnergyInfluenceElevatedHr => 'Tõusnud pulss';

  @override
  String get bodyEnergyInfluenceRecoveryDebt => 'Taastumisvõlg';

  @override
  String get bodyEnergyInfluenceNoData => 'Andmed puuduvad';

  @override
  String get bodyEnergyInfluenceSteady => 'Stabiilne';

  @override
  String get bodyEnergyReasonSleepRecoveryDetail =>
      'Unelõigud laadisid hinnangut eelmisest tulemusest.';

  @override
  String get bodyEnergyReasonQuietRestDetail =>
      'Madal pulss ärkveloleku ajal lisas väikese taastumislaengu.';

  @override
  String get bodyEnergyReasonExertionDetail =>
      'Pulsi intensiivsus või salvestatud treeningud tühjendasid hinnangut.';

  @override
  String get bodyEnergyReasonElevatedHrDetail =>
      'Ärkveloleku pulss üle puhkeoleku taseme lisas stressitühjenemist.';

  @override
  String get bodyEnergyReasonRecoveryDebtDetail =>
      'Hiljutine raskem pingutus hoidis pärast väikest tühjenemist aktiivsena.';

  @override
  String get bodyEnergyReasonNoDataDetail =>
      'Health Connect ei andnud selle lõigu jaoks piisavalt signaali.';

  @override
  String get bodyEnergyReasonSteadyDetail =>
      'Hinnang püsis enamasti stabiilsena.';

  @override
  String get bodyEnergyInputsTitle => 'Kasutatud sisendid';

  @override
  String bodyEnergyInputsSummary(int arg0, int arg1) {
    return 'Algoritm v$arg0, $arg1-minutilised lõigud';
  }

  @override
  String get bodyEnergyInputHeartRate => 'Pulsinäidud';

  @override
  String get bodyEnergyInputSleep => 'Unesessioonid';

  @override
  String get bodyEnergyInputWorkouts => 'Treeningud';

  @override
  String get bodyEnergyInputRestingHr => 'Puhkeoleku pulss';

  @override
  String get bodyEnergyInputHrBaseline => 'Pulsi lähtetase';

  @override
  String get bodyEnergyInputHrv => 'HRV modifikaator';

  @override
  String get bodyEnergyInputRespiratory => 'Hingamise modifikaator';

  @override
  String get bodyEnergyInputPreviousScore => 'Eelmine tulemus';

  @override
  String get bodyEnergyInputCalibration => 'Kalibreerimine';

  @override
  String get bodyEnergyInputAvailable => 'Saadaval';

  @override
  String get bodyEnergyInputMissing => 'Puudub';

  @override
  String get bodyEnergyInputOptional => 'Ei ole olemas';

  @override
  String bodyEnergyInputRecords(int arg0) {
    return '$arg0 kirjet';
  }

  @override
  String bodyEnergyInputSessions(int arg0) {
    return '$arg0 sessiooni';
  }

  @override
  String bodyEnergyInputWorkoutsValue(int arg0) {
    return '$arg0 treeningut';
  }

  @override
  String bodyEnergyInputPreviousScoreValue(String arg0) {
    return '$arg0 algus';
  }

  @override
  String get bodyEnergyCalibrationModeAuto => 'Automaatsed hinnangud';

  @override
  String get bodyEnergyCalibrationModeManualValues => 'Käsitsi väärtused';

  @override
  String get bodyEnergyCalibrationModeManualZones => 'Käsitsi tsoonid';

  @override
  String get bodyEnergyCalculationTitle => 'Kuidas kehaenergiat hinnatakse';

  @override
  String get bodyEnergyCalculationBody =>
      'OpenVitals jagab valitud päeva lühikesteks lõikudeks, alustab võimalusel eelmisest saadaolevast tulemusest, seejärel lisab laengut unest või rahulikust puhkusest ning lahutab tühjenemist pingutusest, tõusnud ärkveloleku pulsist ja taastumisvõlast pärast raskemat pingutust.';

  @override
  String get bodyEnergyCalculationInputsBody =>
      'Pulss, puhkeoleku pulss, isiklikud tsoonid, uni, treeningud, HRV ja hingamissagedus võivad kõik hinnangut parandada. Puuduvad sisendid muudavad hinnangu ettevaatlikumaks ja vähendavad usaldusväärsust.';

  @override
  String get bodyEnergyCalculationLimitsBody =>
      'See on seadmesisene heaoluhinnang, mitte otsene mõõtmine ega meditsiiniline nõuanne. Kuvatud sisendid ja põhjused on nähtavad, et meetodit saaks üle vaadata ja parandada.';

  @override
  String get metricBodyEnergy => 'Kehaenergia';

  @override
  String get privacyPolicyTitle => 'Privaatsuspoliitika';

  @override
  String get privacyPolicyBody1 =>
      'OpenVitals loeb andmeid Health Connectist, et näidata sinu seadmes samme, treeninguid, und, pulssi, kaalu, kaloreid, vedelikutarbimist, toitumist, teadvelolekut ja elunäitajaid. Kirjed, mida sa ise logid, sealhulgas imporditud GPX/KML/KMZ marsruudid ja imporditud FIT-failid, kirjutatakse Health Connectisse.';

  @override
  String get privacyPolicyBody2 =>
      'See rakendus ei laadi sinu terviseandmeid pilveteenusesse, ei sisalda reklaame ega jaga andmeid kolmandate osapooltega.';

  @override
  String get privacyPolicyBody3 =>
      'OpenVitals ei ole meditsiiniseade ega diagnoosi, ravi ega ennetab ühtegi haigust ega terviseseisundit. See ei asenda kvalifitseeritud tervishoiutöötaja meditsiinilist nõuannet, diagnoosi ega ravi.';

  @override
  String get linkCouldNotOpen => 'Linki ei saanud avada.';

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
