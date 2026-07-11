// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Spanish Castilian (`es`).
class AppLocalizationsEs extends AppLocalizations {
  AppLocalizationsEs([String locale = 'es']) : super(locale);

  @override
  String get appName => 'OpenVitals';

  @override
  String get cdBack => 'Atrás';

  @override
  String get cdSettings => 'Ajustes';

  @override
  String get cdAchievements => 'Logros';

  @override
  String get cdDailyReadiness => 'Preparación diaria';

  @override
  String get cdSensorBatteryStatus => 'Estado de batería del sensor';

  @override
  String get cdEditDashboard => 'Editar resumen';

  @override
  String get cdFinishDashboardEditing => 'Terminar edición del resumen';

  @override
  String get cdEditSavedDrinks => 'Editar bebidas guardadas';

  @override
  String get cdDoneEditingSavedDrinks => 'Bebidas guardadas editadas';

  @override
  String get cdEditDrink => 'Editar bebida';

  @override
  String get cdDeleteDrink => 'Eliminar bebida';

  @override
  String get cdMoveDrinkCategory => 'Mover categoría de bebida';

  @override
  String cdExpandDrinkCategory(String arg0) {
    return 'Expandir $arg0';
  }

  @override
  String cdCollapseDrinkCategory(String arg0) {
    return 'Contraer $arg0';
  }

  @override
  String get cdEditManualEntryWidgets => 'Editar y añadir widgets';

  @override
  String get cdFinishManualEntryEditing =>
      'Terminar de editar y añadir widgets';

  @override
  String get cdEditRecordingDashboard => 'Editar panel de grabación';

  @override
  String get cdFinishRecordingDashboardEditing =>
      'Terminar edición del panel de grabación';

  @override
  String get cdMoveWidgetUp => 'Subir el widget';

  @override
  String get cdMoveWidgetDown => 'Bajar el widget';

  @override
  String get cdEditMetricSections => 'Editar métricas';

  @override
  String get cdFinishMetricSectionEditing => 'Terminar de editar las métricas';

  @override
  String get cdMoveSectionUp => 'Subir la sección';

  @override
  String get cdMoveSectionDown => 'Bajar la sección';

  @override
  String get cdRemoveWidget => 'Quitar widget';

  @override
  String get cdDecreaseRecordingDashboardWidgetSize => 'Reducir el widget';

  @override
  String get cdIncreaseRecordingDashboardWidgetSize => 'Ampliar el widget';

  @override
  String get cdExitRecordingFocusMode => 'Salir del modo de concentración';

  @override
  String get cdToggleRecordingOutdoorMode =>
      'Activar/desactivar modo exteriores';

  @override
  String get cdRecenterMap => 'Volver a centrar el mapa';

  @override
  String get cdDeleteEntry => 'Eliminar entrada';

  @override
  String get cdEditEntry => 'Editar entrada';

  @override
  String get cdPreviousDay => 'Día anterior';

  @override
  String get cdNextDay => 'Día siguiente';

  @override
  String get cdPreviousPeriod => 'Periodo anterior';

  @override
  String get cdNextPeriod => 'Periodo siguiente';

  @override
  String get cdOpenCalendar => 'Abrir calendario';

  @override
  String get actionCancel => 'Cancelar';

  @override
  String get actionAdd => 'Añadir';

  @override
  String get actionAddCustom => 'Añadir a medida';

  @override
  String get actionSave => 'Guardar';

  @override
  String get actionClose => 'Cerrar';

  @override
  String get actionContinue => 'Continuar';

  @override
  String get actionDelete => 'Eliminar';

  @override
  String get actionDetails => 'Detalles';

  @override
  String get actionEdit => 'Editar';

  @override
  String get actionEnable => 'Activar';

  @override
  String get actionGetStarted => 'Empezar';

  @override
  String get actionGrant => 'Conceder';

  @override
  String get actionGrantPermission => 'Conceder permiso';

  @override
  String get actionLoadMoreEntries => 'Cargar 10 más';

  @override
  String get actionShowCalculation => 'Mostrar cálculo';

  @override
  String get actionHideCalculation => 'Ocultar cálculo';

  @override
  String get actionNotNow => 'Ahora no';

  @override
  String get actionAccept => 'Acepto';

  @override
  String get actionOpen => 'Abrir';

  @override
  String get actionPause => 'Pausar';

  @override
  String get actionReview => 'Revisar';

  @override
  String get actionResume => 'Reanudar';

  @override
  String get actionRefresh => 'Actualizar';

  @override
  String get actionSelect => 'Seleccionar';

  @override
  String get actionStart => 'Empezar';

  @override
  String get actionFinish => 'Terminar';

  @override
  String get actionDiscard => 'Descartar';

  @override
  String get unknownError => 'Error desconocido';

  @override
  String get screenErrorNotFound => 'No se encontró el elemento solicitado.';

  @override
  String get screenErrorMissingArgument => 'Falta información obligatoria.';

  @override
  String get screenErrorPermissionDenied =>
      'Se requiere permiso para cargar estos datos.';

  @override
  String get screenErrorHealthConnectUnavailable =>
      'Health Connect no está disponible en este dispositivo.';

  @override
  String get screenErrorLoadSleepSession =>
      'No se pudo cargar la sesión de sueño.';

  @override
  String get screenErrorLoadSleepPeriod =>
      'No se pudieron cargar los datos de sueño.';

  @override
  String get notAvailable => 'No disponible';

  @override
  String get notRecorded => 'No registrado';

  @override
  String get noData => 'Sin datos';

  @override
  String get loading => 'Cargando...';

  @override
  String get homeMetricWidgetDescription => 'Métrica de OpenVitals';

  @override
  String get homeMetricWidgetConfigTitle => 'Elegir métrica';

  @override
  String get homeMetricWidgetConfigPrompt => 'Elige la métrica para el widget:';

  @override
  String get homeMetricWidgetNoMetrics => 'No hay métricas disponibles.';

  @override
  String get homeMetricWidgetPermissionNeeded =>
      'Concede el permiso en OpenVitals';

  @override
  String get homeMetricWidgetUpdateFailed => 'No se puede actualizar';

  @override
  String get homeMetricWidgetOpenForDetails => 'Abrir detalles';

  @override
  String get homeMetricWidgetNotConfigured => 'Selecciona una métrica';

  @override
  String get homeQuickBeverageWidgetDescription => 'Bebida rápida';

  @override
  String get homeQuickBeverageOneTapWidgetDescription => 'Bebida rápida 1x1';

  @override
  String get homeQuickBeverageWidgetConfigTitle => 'Elegir bebida';

  @override
  String get homeQuickBeverageWidgetConfigPrompt =>
      'Elige la bebida para el widget:';

  @override
  String get homeQuickBeverageWidgetNoDrinks => 'No hay bebidas disponibles.';

  @override
  String get homeQuickBeverageWidgetNotConfigured => 'Selecciona una bebida';

  @override
  String get homeQuickBeverageWidgetTapToLog => 'Toca para registrar';

  @override
  String get homeQuickBeverageWidgetSaved => 'Guardada ahora';

  @override
  String get homeQuickBeverageWidgetSavedNutrition => 'Guardada como nutrición';

  @override
  String get homeDailyReadinessWidgetDescription =>
      'Preparación diaria de OpenVitals';

  @override
  String get homeBodyEnergyWidgetDescription =>
      'Energía corporal de OpenVitals';

  @override
  String get homeTodayVitalsWidgetDescription =>
      'Constantes de hoy en OpenVitals';

  @override
  String get homeWidgetTodayTitle => 'Hoy';

  @override
  String get homeWidgetContext => 'Contexto';

  @override
  String get homeWidgetHrvShort => 'VFC';

  @override
  String get homeWidgetBodyEnergyCharged => 'Cargada';

  @override
  String get homeWidgetBodyEnergySteady => 'Estable';

  @override
  String get homeWidgetBodyEnergyLimited => 'Limitada';

  @override
  String get homeWidgetBodyEnergyLow => 'Baja';

  @override
  String get screenSteps => 'Pasos';

  @override
  String get screenActivities => 'Actividades';

  @override
  String get screenCalories => 'Calorías';

  @override
  String get screenActivityDetail => 'Detalle de actividad';

  @override
  String get screenSleep => 'Sueño';

  @override
  String get screenSleepDetail => 'Detalle de sueño';

  @override
  String get screenHeartVitals => 'Corazón y signos vitales';

  @override
  String get screenStressTracking => 'Seguimiento de estrés';

  @override
  String get screenBodyEnergy => 'Energía corporal';

  @override
  String get screenTrainingReadiness => 'Preparación para entrenar';

  @override
  String get screenBody => 'Cuerpo';

  @override
  String get screenHydration => 'Hidratación';

  @override
  String get screenNutrition => 'Nutrición';

  @override
  String get screenMindfulness => 'Mindfulness';

  @override
  String get screenCycle => 'Ciclo';

  @override
  String get screenDailyReadiness => 'Preparación diaria';

  @override
  String get screenSettings => 'Ajustes';

  @override
  String get screenAchievements => 'Logros';

  @override
  String get screenManualEntry => 'Añadir entrada';

  @override
  String get screenHydrationEntry => 'Entrada de hidratación';

  @override
  String get screenActivityEntry => 'Entrada de actividad';

  @override
  String get screenMindfulnessEntry => 'Entrada de mindfulness';

  @override
  String get screenCarbsEntry => 'Entrada de carbohidratos';

  @override
  String get screenBodyMeasurementEntry => 'Entrada de medida corporal';

  @override
  String get screenVitalsMeasurementEntry => 'Entrada de constantes';

  @override
  String get bottomNavDashboard => 'Resumen';

  @override
  String get manualEntryHydrationTitle => 'Hidratación';

  @override
  String get manualEntryActivityTitle => 'Actividad';

  @override
  String get manualEntryDateLabel => 'Fecha de entrada';

  @override
  String get manualEntryTimeLabel => 'Hora de entrada';

  @override
  String get manualEntrySelectTime => 'Seleccionar hora de entrada';

  @override
  String get manualEntryAddWidgets => 'Añadir widgets de entrada';

  @override
  String get manualEntryAllWidgetsAdded =>
      'Todos los widgets de entrada ya están visibles.';

  @override
  String get manualEntryWritePermissionTitle =>
      'Acceso de escritura para hidratación';

  @override
  String get manualEntryActivityWritePermissionTitle =>
      'Permisos de escritura de actividad';

  @override
  String get manualEntryMindfulnessWritePermissionTitle =>
      'Permiso de escritura de mindfulness';

  @override
  String get manualEntryCarbsWritePermissionTitle =>
      'Permiso de escritura de carbohidratos';

  @override
  String manualEntryBodyWritePermissionTitle(String arg0) {
    return 'Permiso de escritura de $arg0';
  }

  @override
  String manualEntryVitalsWritePermissionTitle(String arg0) {
    return 'Permiso de escritura de $arg0';
  }

  @override
  String get mindfulnessEntrySubtitle =>
      'Las sesiones de mindfulness se guardan directamente en Health Connect.';

  @override
  String get mindfulnessEntryPermissionNeeded =>
      'Para el resumen, OpenVitals solo pide permisos de lectura. Para añadir entradas de mindfulness, necesitamos permiso de escritura. OpenVitals no almacenará estas sesiones; las entradas se guardan en Health Connect.';

  @override
  String get activityEntrySubtitle =>
      'Crea una sesión de actividad en Health Connect. Los archivos de ruta solo se escriben cuando importas uno.';

  @override
  String get activityEntryPermissionNeeded =>
      'Para el resumen, OpenVitals solo pide permisos de lectura. Para añadir actividades sin interrupciones posteriores, necesitamos permisos de escritura en Health Connect para sesiones, rutas, distancia, elevación, calorías activas y calorías totales. OpenVitals no almacenará estas entradas; se guardan en Health Connect.';

  @override
  String get activityEntrySourceBody =>
      'Crea una actividad manualmente, usa un plan existente o graba una ruta GPS.';

  @override
  String get activityEntryCreateManual => 'Crear manualmente';

  @override
  String get activityEntryCreateFromExistingPlan =>
      'Crear desde un plan existente';

  @override
  String get activityEntryRecordGps => 'Grabar actividad';

  @override
  String get activityEntryChooseAnotherSource => 'Elegir otro método';

  @override
  String get activityEntryTypeLabel => 'Tipo de actividad';

  @override
  String get activityEntryTitleLabel => 'Título opcional';

  @override
  String get activityEntryStartDateLabel => 'Fecha de inicio';

  @override
  String get activityEntryStartTimeLabel => 'Hora de inicio';

  @override
  String get activityEntrySelectTime => 'Seleccionar hora de inicio';

  @override
  String get activityEntryDurationLabel => 'Duración min';

  @override
  String get activityEntryRepetitionsTitle => 'Repeticiones';

  @override
  String get activityEntryStepsTitle => 'Pasos';

  @override
  String get activityEntryRepetitionModeTotal => 'Total';

  @override
  String get activityEntryRepetitionModeSets => 'Series';

  @override
  String get activityEntryRepetitionsLabel => 'Reps';

  @override
  String get activityEntryStepsLabel => 'Pasos';

  @override
  String activityEntrySetRepetitionsLabel(int arg0) {
    return 'Serie $arg0 repeticiones';
  }

  @override
  String get activityEntrySetRestLabel => 'Tiempo de descanso';

  @override
  String get activityEntryAddSet => 'Añadir serie';

  @override
  String get activityEntryTrainingPlansTitle => 'Planes de entrenamiento';

  @override
  String get activityEntryTrainingPlansLoading =>
      'Cargando planes de Health Connect';

  @override
  String get activityEntryTrainingPlansEmpty =>
      'No hay planes de Health Connect para esta fecha y tipo de actividad';

  @override
  String get activityEntryTrainingPlanLabel => 'Plan de entrenamiento';

  @override
  String get activityEntryTrainingPlanSelect => 'Seleccionar plan';

  @override
  String get activityEntryTrainingPlanNew => 'Nuevo plan';

  @override
  String get activityEntryTrainingPlanUnnamed => 'Plan sin nombre';

  @override
  String get activityEntrySaveTrainingPlan => 'Guardar plan';

  @override
  String get activityEntryUpdateTrainingPlan => 'Actualizar plan';

  @override
  String get activityEntryPlanActivityPickerTitle => 'Actividades con planes';

  @override
  String get activityEntryPlanActivityPickerEmpty =>
      'No se encontraron planes de Health Connect';

  @override
  String get activityEntryPlanPickerTitle => 'Elegir un plan';

  @override
  String get activityEntryPlanPickerEmpty =>
      'No hay planes para esta actividad';

  @override
  String get activityEntryPlanChooseActivity => 'Elegir otra actividad';

  @override
  String activityEntryPlanOneSetSummary(int arg0) {
    return '1 serie • $arg0 repeticiones';
  }

  @override
  String activityEntryPlanSummary(int arg0, int arg1) {
    return '$arg0 series • $arg1 repeticiones';
  }

  @override
  String activityEntryPlanPreviewReps(int arg0) {
    return '$arg0 repeticiones';
  }

  @override
  String activityEntryPlanPreviewRest(int arg0) {
    return 'descanso $arg0 s';
  }

  @override
  String activityEntryPlanPreviewMore(int arg0) {
    return '+$arg0 más';
  }

  @override
  String activityEntryDistanceLabel(String arg0) {
    return 'Distancia $arg0 opcional';
  }

  @override
  String activityEntryElevationLabel(String arg0) {
    return 'Elevación $arg0 opcional';
  }

  @override
  String get activityEntryNotesLabel => 'Notas opcionales';

  @override
  String get activityEntryFeelingLabel => '¿Cómo te sentiste?';

  @override
  String get activityEntryFeelingGreat => 'Genial';

  @override
  String get activityEntryFeelingGood => 'Bien';

  @override
  String get activityEntryFeelingHard => 'Difícil';

  @override
  String get activityEntryFeelingRough => 'Muy duro';

  @override
  String get activityEntryImportedRoute => 'Ruta importada';

  @override
  String get activityEntryRecordingTitle => 'Grabando actividad';

  @override
  String get activityEntryRecordingReadyBody =>
      'Elige el tipo de actividad y empieza cuando estés listo. Al terminar, podrás revisar y añadir detalles antes de guardar.';

  @override
  String get activityEntryRecordingGoToActivityScreen =>
      'Ir a la pantalla de actividad';

  @override
  String get activityEntryRecordingActive => 'Grabando';

  @override
  String get activityEntryRecordingPaused => 'Pausada';

  @override
  String get activityEntryRecordingIdle => 'En pausa automática';

  @override
  String get activityEntryRecordingResting => 'Descanso';

  @override
  String get activityEntryRecordingGpsFix => 'GPS listo';

  @override
  String get activityEntryRecordingGpsPoor => 'GPS débil';

  @override
  String get activityEntryRecordingGpsLost => 'GPS perdido';

  @override
  String get activityEntryRecordingGpsOff => 'GPS apagado';

  @override
  String get activityEntryRecordingTabMap => 'Mapa';

  @override
  String get activityEntryRecordingTabStats => 'Estadísticas';

  @override
  String get activityEntryRecordingTabIntervals => 'Intervalos';

  @override
  String get activityEntryRecordingTabByTime => 'Por tiempo';

  @override
  String get activityEntryRecordingTabByDistance => 'Por distancia';

  @override
  String get activityEntryRecordingTimeSplit => 'Tramo de tiempo';

  @override
  String get activityEntryRecordingDistanceSplit => 'Tramo de distancia';

  @override
  String activityEntryRecordingSplitMinutes(int arg0) {
    return '$arg0 min';
  }

  @override
  String activityEntryRecordingSplitInterval(int arg0) {
    return 'Intervalo $arg0';
  }

  @override
  String activityEntryRecordingSplitTimeRange(int arg0, int arg1) {
    return '$arg0-$arg1 min';
  }

  @override
  String get activityEntryRecordingSplitElapsed => 'Tiempo';

  @override
  String get activityEntryRecordingSplitAvg => 'Media';

  @override
  String get activityEntryRecordingSplitMax => 'Máx.';

  @override
  String get activityEntryRecordingNoIntervals => 'Aún no hay intervalos';

  @override
  String get activityEntryRecordingNoTimeSplits =>
      'Aún no hay tramos de tiempo';

  @override
  String get activityEntryRecordingNoDistanceSplits =>
      'Aún no hay tramos de distancia';

  @override
  String get activityEntryRecordingLap => 'Vuelta';

  @override
  String get activityEntryRecordingMarker => 'Marcador';

  @override
  String activityEntryRecordingMarkerDefaultName(int arg0) {
    return 'Marcador $arg0';
  }

  @override
  String get activityEntryRecordingMarkersTitle => 'Marcadores';

  @override
  String get activityEntryRecordingMarkerName => 'Nombre';

  @override
  String get activityEntryRecordingMarkerNote => 'Nota';

  @override
  String get activityEntryRecordingWaitingForGps =>
      'Esperando una señal GPS precisa';

  @override
  String get activityEntryRecordingGpsWaiting =>
      'Esperando una señal GPS precisa antes de empezar.';

  @override
  String activityEntryRecordingGpsWaitingAccuracy(String arg0) {
    return 'Esperando mejor precisión GPS • $arg0';
  }

  @override
  String activityEntryRecordingGpsReady(String arg0) {
    return 'GPS listo • precisión $arg0';
  }

  @override
  String get activityEntryRecordingGpsDisabled =>
      'Activa el GPS para empezar a grabar.';

  @override
  String get activityEntryRecordingDistance => 'Distancia';

  @override
  String get activityEntryRecordingTotalTime => 'Tiempo total';

  @override
  String get activityEntryRecordingMovingTime => 'Tiempo en movimiento';

  @override
  String get activityEntryRecordingRestTime => 'Tiempo de descanso';

  @override
  String get activityEntryRecordingSpeed => 'Velocidad';

  @override
  String get activityEntryRecordingMaxSpeed => 'Velocidad máxima';

  @override
  String get activityEntryRecordingAverageSpeed => 'Velocidad media';

  @override
  String get activityEntryRecordingAverageMovingSpeed =>
      'Velocidad media en movimiento';

  @override
  String get activityEntryRecordingElevationGain => 'Ascenso';

  @override
  String get activityEntryRecordingPoints => 'Puntos';

  @override
  String get activityEntryRecordingRestSecondsLabel => 'Segundos de descanso';

  @override
  String get activityEntryRecordingEndSet => 'Terminar serie';

  @override
  String get activityEntryRecordingStartNextSet => 'Iniciar siguiente serie';

  @override
  String get activityEntryRecordingEndSession => 'Finalizar sesión';

  @override
  String activityEntryRecordingRestRemaining(String arg0) {
    return 'Descanso $arg0';
  }

  @override
  String get activityEntryRecordingFinishHint =>
      'Terminar abre el formulario de detalles de actividad para añadir título, notas, calorías o ajustar valores antes de guardar.';

  @override
  String get activityEntryRecordingRepetitionCorrectionHint =>
      'Usa + o - si el sensor omite o añade una repetición.';

  @override
  String activityEntryRecordingAccuracy(String arg0) {
    return 'Última precisión $arg0';
  }

  @override
  String get activityEntryRecordingFocus => 'Enfoque';

  @override
  String get activityEntryRecordingDashboardLayout => 'Diseño del panel';

  @override
  String get activityEntryRecordingDashboardLayoutTwoByFour => '2x4';

  @override
  String get activityEntryRecordingDashboardLayoutThreeByFour => '3x4';

  @override
  String get activityEntryRecordingDashboardLayoutLargeTop => 'Grande arriba';

  @override
  String get activityEntryRecordingDashboardAddField => 'Añadir widget';

  @override
  String activityEntryRouteSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  ) {
    return '$arg0 • $arg1 • $arg2 desnivel • $arg3 puntos';
  }

  @override
  String activityEntryRouteAverageMetrics(String arg0, String arg1) {
    return 'Ritmo medio $arg0 • velocidad media $arg1';
  }

  @override
  String get activityEntryAdd => 'Guardar actividad';

  @override
  String get activityEntryInvalidValue =>
      'Corrige los campos resaltados antes de guardar la actividad.';

  @override
  String get activityEntryErrorActivityTypeRoute =>
      'Elige un tipo de actividad compatible con rutas GPS.';

  @override
  String get activityEntryErrorTrainingPlanTitleRequired =>
      'Introduce un título para guardar este plan de entrenamiento.';

  @override
  String get activityEntryErrorStartDate => 'Elige una fecha de inicio válida.';

  @override
  String get activityEntryErrorStartTime => 'Elige una hora de inicio válida.';

  @override
  String get activityEntryErrorStartTimeAfterRoute =>
      'La hora de inicio debe ser igual o anterior al inicio de la ruta importada.';

  @override
  String get activityEntryErrorDuration =>
      'La duración debe estar entre 1 minuto y 7 días.';

  @override
  String get activityEntryErrorRepetitions =>
      'Introduce recuentos positivos. El descanso debe caber dentro de la duración de la actividad.';

  @override
  String get activityEntryErrorDistance =>
      'Introduce una distancia mayor que 0.';

  @override
  String get activityEntryErrorDistanceUnsupported =>
      'Este tipo de actividad no admite distancia.';

  @override
  String get activityEntryErrorElevation =>
      'Introduce una elevación mayor que 0.';

  @override
  String get activityEntryErrorElevationUnsupported =>
      'Este tipo de actividad no admite desnivel positivo.';

  @override
  String get activityEntryErrorActiveCalories =>
      'Introduce calorías activas mayores que 0.';

  @override
  String get activityEntryErrorTotalCalories =>
      'Introduce calorías totales mayores que 0.';

  @override
  String get activityEntryErrorTotalCaloriesBelowActive =>
      'Las calorías totales no pueden ser menores que las activas.';

  @override
  String get activityEntryLocationPermissionNeeded =>
      'Se necesita permiso de ubicación precisa para grabar actividades GPS.';

  @override
  String get activityEntryNotificationPermissionNeeded =>
      'Se necesita permiso de notificaciones para que OpenVitals muestre una notificación de grabación permanente.';

  @override
  String get activityEntryActivityRecognitionPermissionNeeded =>
      'Se necesita permiso de reconocimiento de actividad para contar pasos en cinta.';

  @override
  String activityEntryRouteImportFailed(String arg0) {
    return 'No se pudo importar el archivo de ruta: $arg0';
  }

  @override
  String activityEntryRecordingFailed(String arg0) {
    return 'No se pudo grabar la actividad: $arg0';
  }

  @override
  String activityEntryWriteFailed(String arg0) {
    return 'No se pudo escribir la entrada de actividad: $arg0';
  }

  @override
  String get activityRouteOpenInMap => 'Abrir ruta en una app de mapas';

  @override
  String get activityRouteExportGpx => 'Guardar GPX';

  @override
  String get activityRouteExportKmz => 'Guardar KMZ';

  @override
  String get activityRouteExportSaved => 'Ruta guardada.';

  @override
  String get activityRouteExportFailed =>
      'No se pudo guardar el archivo de ruta.';

  @override
  String get activityRouteOpenChooserTitle => 'Abrir ruta con';

  @override
  String get activityRouteOpenFailed =>
      'Ninguna app de mapas pudo abrir esta ruta.';

  @override
  String get activityDetailAnalysisTitle => 'Análisis de ruta';

  @override
  String get activityDetailTabMarkers => 'Marcadores';

  @override
  String get activityDetailNoMarkers => 'Aún no hay marcadores';

  @override
  String activityRecordingVoiceSummary(
    String arg0,
    String arg1,
    String arg2,
    int arg3,
  ) {
    return 'Tiempo $arg0. Distancia $arg1. Velocidad media $arg2. Vuelta actual $arg3.';
  }

  @override
  String activityRecordingVoiceLap(int arg0, String arg1) {
    return 'Vuelta $arg0. $arg1';
  }

  @override
  String get activityRecordingVoiceIdle => 'Pausa automática.';

  @override
  String get activityRecordingVoiceResumed => 'Grabación reanudada.';

  @override
  String get activityRecordingNotificationChannel => 'Grabación de actividad';

  @override
  String get activityRecordingNotificationTitle => 'Grabando actividad';

  @override
  String activityRecordingNotificationRecording(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return 'Grabando • $arg0 total • $arg1 en movimiento • $arg2 • $arg3';
  }

  @override
  String activityRecordingNotificationPaused(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return 'Pausada • $arg0 total • $arg1 en movimiento • $arg2 • $arg3';
  }

  @override
  String activityRecordingNotificationRepetitionRecording(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Grabando • $arg0 total • $arg1 $arg2';
  }

  @override
  String activityRecordingNotificationRepetitionPaused(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'Pausada • $arg0 total • $arg1 $arg2';
  }

  @override
  String activityRecordingNotificationRepetitionResting(
    String arg0,
    String arg1,
  ) {
    return 'Descanso • $arg0 total • quedan $arg1';
  }

  @override
  String activityRecordingNotificationTimedRecording(String arg0) {
    return 'Grabando • $arg0 total';
  }

  @override
  String activityRecordingNotificationTimedPaused(String arg0) {
    return 'Pausada • $arg0 total';
  }

  @override
  String get activityRecordingErrorService =>
      'No se pudo iniciar el servicio de grabación de actividad.';

  @override
  String get activityRecordingErrorPreciseLocationPermission =>
      'Se necesita ubicación precisa para obtener rutas GPS fiables.';

  @override
  String get activityRecordingErrorNotificationPermission =>
      'Se necesita permiso de notificaciones para mostrar la notificación de grabación permanente.';

  @override
  String get activityRecordingErrorActivityRecognitionPermission =>
      'Se necesita permiso de reconocimiento de actividad para contar pasos en cinta.';

  @override
  String get activityRecordingErrorWaitingForGps =>
      'Espera una señal GPS precisa antes de empezar.';

  @override
  String get activityRecordingErrorProvider =>
      'Activa el GPS para grabar una ruta.';

  @override
  String get activityRecordingErrorUnsupportedType =>
      'Este tipo de actividad no se puede grabar en vivo.';

  @override
  String get activityRecordingErrorProximitySensor =>
      'Este dispositivo no expone un sensor de proximidad para contar flexiones.';

  @override
  String get activityRecordingErrorAccelerometer =>
      'Este dispositivo no expone un acelerómetro para esta grabación.';

  @override
  String get activityRecordingErrorStepDetector =>
      'Este dispositivo no expone eventos del detector de pasos de Android.';

  @override
  String get activityRecordingHowItWorks => 'Cómo funciona la grabación';

  @override
  String get activityRecordingGuidancePushUps =>
      'Coloca el teléfono boca arriba bajo el pecho o la cabeza. El sensor de proximidad cuenta una repetición cuando te acercas al teléfono.';

  @override
  String get activityRecordingGuidancePullUps =>
      'Sujeta el teléfono al cuerpo. El acelerómetro cuenta el movimiento de subida y relajación.';

  @override
  String get activityRecordingGuidanceRopeSkipping =>
      'Mantén el teléfono sujeto al cuerpo. El acelerómetro cuenta los saltos.';

  @override
  String get activityRecordingGuidanceTrampolineJumping =>
      'Mantén el teléfono sujeto al cuerpo. La detección de saltos usa una ventana más larga que saltar a la cuerda.';

  @override
  String get activityRecordingGuidanceTreadmill =>
      'Lleva el teléfono en el cuerpo. El detector de pasos de Android cuenta los pasos; no se graba ruta GPS.';

  @override
  String get activityRecordingSensorReady => 'Sensor listo';

  @override
  String get activityRecordingSensorUnavailableManual =>
      'El conteo en vivo no está disponible en este dispositivo. La entrada manual sigue disponible.';

  @override
  String get activityRecordingActivityRecognitionMissing =>
      'Concede reconocimiento de actividad para contar pasos en cinta.';

  @override
  String get exerciseTypeRunning => 'Correr';

  @override
  String get exerciseTypeBiking => 'Bicicleta';

  @override
  String get exerciseTypeWalking => 'Caminar';

  @override
  String get exerciseTypeHiking => 'Senderismo';

  @override
  String get exerciseTypeWheelchair => 'Silla de ruedas';

  @override
  String get exerciseTypeRowing => 'Remo';

  @override
  String get exerciseTypePaddling => 'Paleo';

  @override
  String get exerciseTypeSkiing => 'Esquí';

  @override
  String get exerciseTypeSnowboarding => 'Snowboard';

  @override
  String get exerciseTypeSnowshoeing => 'Raquetas de nieve';

  @override
  String get exerciseTypeSkating => 'Patinaje';

  @override
  String get exerciseTypeSailing => 'Vela';

  @override
  String get exerciseTypeSurfing => 'Surf';

  @override
  String get exerciseTypeSwimmingOpenWater => 'Natación (aguas abiertas)';

  @override
  String get exerciseTypeGolf => 'Golf';

  @override
  String get exerciseTypeStrengthTraining => 'Entrenamiento de fuerza';

  @override
  String get exerciseTypeTreadmill => 'Cinta de correr';

  @override
  String get exerciseTypePushUps => 'Flexiones';

  @override
  String get exerciseTypePullUps => 'Dominadas';

  @override
  String get exerciseTypeRopeSkipping => 'Saltar a la cuerda';

  @override
  String get exerciseTypeTrampolineJumping => 'Saltos en trampolín';

  @override
  String get exerciseTypeOtherWorkout => 'Otra actividad';

  @override
  String get mindfulnessEntryUnavailable =>
      'Las sesiones de mindfulness no están disponibles en este proveedor de Health Connect.';

  @override
  String get mindfulnessEntryTimerTitle => 'Temporizador';

  @override
  String get mindfulnessEntryManualTitle => 'Entrada manual';

  @override
  String get mindfulnessEntryIntervalBell => 'Campana de intervalo';

  @override
  String get mindfulnessEntryIntervalMinutes => 'Intervalo (min)';

  @override
  String get mindfulnessEntryBellSound => 'Sonido de campana';

  @override
  String get mindfulnessEntryBackgroundSound => 'Sonido de fondo';

  @override
  String get mindfulnessBellStruck => 'Golpe suave';

  @override
  String get mindfulnessBellRubbed => 'Cuenco cálido';

  @override
  String get mindfulnessBellBright => 'Cuenco claro';

  @override
  String get mindfulnessBellTemple => 'Cuenco de templo';

  @override
  String get mindfulnessBellHarmony => 'Armonía';

  @override
  String get mindfulnessBackgroundNone => 'Ninguno';

  @override
  String get mindfulnessBackgroundBowl => 'Cuenco';

  @override
  String get mindfulnessBackgroundMeditation => 'Meditación';

  @override
  String get mindfulnessBackgroundChimes => 'Campanillas';

  @override
  String get mindfulnessBackgroundDreamscape => 'Paisaje sonoro';

  @override
  String get mindfulnessEntryStartTimer => 'Iniciar';

  @override
  String get mindfulnessEntryStopTimer => 'Detener';

  @override
  String get mindfulnessEntryResumeTimer => 'Reanudar';

  @override
  String get mindfulnessEntryDiscardTimer => 'Descartar';

  @override
  String get mindfulnessEntrySaveSession => 'Guardar sesión';

  @override
  String get mindfulnessEntryMinutes => 'Minutos';

  @override
  String get mindfulnessEntryAddMinutes => 'Añadir minutos';

  @override
  String get mindfulnessEntryInvalidTimer =>
      'Introduce una duración e intervalo válidos.';

  @override
  String get mindfulnessEntryInvalidManual =>
      'Introduce minutos de mindfulness válidos.';

  @override
  String get mindfulnessEntryTimerTooShort =>
      'La meditación debe durar al menos 1 minuto para guardarla.';

  @override
  String mindfulnessEntryWriteFailed(String arg0) {
    return 'No se pudo guardar la sesión de mindfulness: $arg0';
  }

  @override
  String get mindfulnessEntryCompleted => 'Temporizador completado';

  @override
  String get mindfulnessRemindersTitle => 'Recordatorios de mindfulness';

  @override
  String get mindfulnessRemindersSummaryOff =>
      'Desactivados por defecto. Activa un recordatorio diario para tu objetivo de mindfulness.';

  @override
  String mindfulnessRemindersSummaryOn(String arg0) {
    return 'Cada día a las $arg0';
  }

  @override
  String get mindfulnessRemindersPermissionNeeded =>
      'Concede permiso de notificaciones para activar los recordatorios de mindfulness.';

  @override
  String get mindfulnessRemindersTime => 'Hora del recordatorio';

  @override
  String get mindfulnessRemindersGoalNote =>
      'Los recordatorios se pausan cuando alcanzas el objetivo de mindfulness de hoy y se reanudan mañana.';

  @override
  String get mindfulnessReminderNotificationChannel =>
      'Recordatorios de mindfulness';

  @override
  String get mindfulnessReminderNotificationChannelDesc =>
      'Recordatorios opcionales para completar tu objetivo diario de mindfulness.';

  @override
  String get mindfulnessReminderNotificationTitle =>
      'Recordatorio de mindfulness';

  @override
  String mindfulnessReminderNotificationBody(String arg0) {
    return 'Tu objetivo de hoy es $arg0. Haz una pausa consciente cuando puedas.';
  }

  @override
  String mindfulnessReminderNotificationProgress(String arg0, String arg1) {
    return '$arg0 / $arg1';
  }

  @override
  String bodyEntrySubtitle(String arg0) {
    return 'Las entradas de $arg0 se guardan directamente en Health Connect.';
  }

  @override
  String bodyEntryPermissionNeeded(String arg0) {
    return 'Para añadir entradas de $arg0, OpenVitals necesita permiso de escritura en Health Connect. La app no almacenará estos datos; las entradas se guardan en Health Connect.';
  }

  @override
  String bodyEntryValueLabel(String arg0, String arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String bodyEntryAddSelected(String arg0) {
    return 'Añadir $arg0';
  }

  @override
  String get bodyEntryInvalidValue =>
      'Introduce un valor válido para esta medición.';

  @override
  String bodyEntryWriteFailed(String arg0) {
    return 'No se pudo guardar la medición corporal: $arg0';
  }

  @override
  String get carbsEntrySubtitle =>
      'Las entradas de carbohidratos se guardan directamente en Health Connect.';

  @override
  String get carbsEntryPermissionNeeded =>
      'Para añadir entradas de carbohidratos, OpenVitals necesita permiso de escritura en Health Connect. La app no almacenará estos datos; las entradas se guardan en Health Connect.';

  @override
  String carbsEntryValueLabel(String arg0) {
    return 'Carbohidratos ($arg0)';
  }

  @override
  String get carbsEntryAdd => 'Añadir carbohidratos';

  @override
  String get carbsEntryInvalidValue =>
      'Introduce una cantidad válida de carbohidratos.';

  @override
  String carbsEntryWriteFailed(String arg0) {
    return 'No se pudieron guardar los carbohidratos: $arg0';
  }

  @override
  String vitalsEntrySubtitle(String arg0) {
    return 'Las entradas de $arg0 se guardan directamente en Health Connect.';
  }

  @override
  String vitalsEntryPermissionNeeded(String arg0) {
    return 'Para añadir entradas de $arg0, OpenVitals necesita permiso de escritura en Health Connect. La app no almacenará estos datos; las entradas se guardan en Health Connect.';
  }

  @override
  String vitalsEntryValueLabel(String arg0, String arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String get vitalsEntrySystolicLabel => 'Sistólica (mmHg)';

  @override
  String get vitalsEntryDiastolicLabel => 'Diastólica (mmHg)';

  @override
  String vitalsEntryAddSelected(String arg0) {
    return 'Añadir $arg0';
  }

  @override
  String get vitalsEntryInvalidValue =>
      'Introduce un valor válido para esta constante.';

  @override
  String vitalsEntryWriteFailed(String arg0) {
    return 'No se pudo guardar la constante: $arg0';
  }

  @override
  String get rangeDay => 'Día';

  @override
  String get rangeWeek => 'Semana';

  @override
  String get rangeMonth => 'Mes';

  @override
  String get rangeYear => 'Año';

  @override
  String get periodToday => 'Hoy';

  @override
  String get periodYesterday => 'Ayer';

  @override
  String get periodThisWeek => 'Esta semana';

  @override
  String periodWeekOf(String arg0) {
    return 'Semana del $arg0';
  }

  @override
  String get periodThisMonth => 'Este mes';

  @override
  String get periodThisYear => 'Este año';

  @override
  String get periodLast7Days => 'Últimos 7 días';

  @override
  String get periodLast30Days => 'Últimos 30 días';

  @override
  String get periodLast365Days => 'Últimos 365 días';

  @override
  String get periodSelected => 'Periodo seleccionado';

  @override
  String get metricSteps => 'Pasos';

  @override
  String get metricDistance => 'Distancia';

  @override
  String get metricAveragePace => 'Ritmo medio';

  @override
  String get metricAverageSpeed => 'Velocidad media';

  @override
  String get metricCaloriesBurned => 'Calorías totales quemadas';

  @override
  String get metricCaloriesOut => 'Calorías totales';

  @override
  String get metricCaloriesIn => 'Calorías ingeridas';

  @override
  String get metricFloorsClimbed => 'Pisos subidos';

  @override
  String get metricActiveCalories => 'Calorías activas';

  @override
  String get metricElevation => 'Elevación';

  @override
  String get metricElevationGained => 'Elevación ganada';

  @override
  String get metricWheelchairPushes => 'Impulsos de silla de ruedas';

  @override
  String get metricWorkout => 'Entrenamiento';

  @override
  String get metricSleep => 'Sueño';

  @override
  String get metricHydration => 'Hidratación';

  @override
  String get metricTotalHydration => 'Hidratación total';

  @override
  String get metricHydrationTrend => 'Tendencia de hidratación';

  @override
  String get metricLoggedDays => 'Días registrados';

  @override
  String get metricLatestWeight => 'Último peso';

  @override
  String get metricBodyFat => 'Grasa corporal';

  @override
  String get metricAvgHeartRate => 'Frecuencia cardíaca promedio';

  @override
  String get metricAverageHeartRate => 'Frecuencia cardíaca normal';

  @override
  String get metricRestingHeartRate => 'Frecuencia en reposo';

  @override
  String get metricHrv => 'Variabilidad de la frecuencia cardiaca (HRV)';

  @override
  String get metricCardioLoad => 'Carga cardiovascular';

  @override
  String get metricWeeklyCardioLoad => 'Cardio semanal';

  @override
  String get metricEnergyBurned => 'Calorías totales';

  @override
  String get metricBloodPressure => 'Presión arterial';

  @override
  String get metricSpo2 => 'SpO2';

  @override
  String get metricOxygenSaturation => 'Saturación de oxígeno';

  @override
  String get metricVo2Max => 'VO2 máx (consumo máximo de oxígeno)';

  @override
  String get metricMindfulness => 'Mindfulness';

  @override
  String get metricTotalMindfulness => 'Mindfulness total';

  @override
  String get metricCycle => 'Ciclo';

  @override
  String get metricCycleTracking => 'Seguimiento del ciclo';

  @override
  String get metricPeriodDays => 'Días de periodo';

  @override
  String get metricOvulationTests => 'Pruebas de ovulación';

  @override
  String get metricLatestBbt => 'Última TCB';

  @override
  String get metricWeight => 'Peso';

  @override
  String get metricHeight => 'Altura';

  @override
  String get metricBmi => 'IMC';

  @override
  String get metricFfmi => 'FFMI';

  @override
  String get metricLeanMass => 'Masa magra';

  @override
  String get metricBmr => 'TMB';

  @override
  String get metricBoneMass => 'Masa ósea';

  @override
  String get metricBodyWaterMass => 'Masa de agua corporal';

  @override
  String get metricLatest => 'Último';

  @override
  String get metricChange => 'Cambio';

  @override
  String get metricMacros => 'Macros';

  @override
  String get metricProtein => 'Proteína';

  @override
  String get metricCarbs => 'Carbohidratos';

  @override
  String get metricFat => 'Grasa';

  @override
  String get metricDietaryFiber => 'Fibra dietética';

  @override
  String get metricSugar => 'Azúcar';

  @override
  String get metricEnergyFromFat => 'Calorías de grasa';

  @override
  String get metricMonounsaturatedFat => 'Grasa monoinsaturada';

  @override
  String get metricPolyunsaturatedFat => 'Grasa poliinsaturada';

  @override
  String get metricSaturatedFat => 'Grasa saturada';

  @override
  String get metricTransFat => 'Grasa trans';

  @override
  String get metricUnsaturatedFat => 'Grasa insaturada';

  @override
  String get metricCholesterol => 'Colesterol';

  @override
  String get metricBiotin => 'Biotina';

  @override
  String get metricFolate => 'Folato';

  @override
  String get metricFolicAcid => 'Ácido fólico';

  @override
  String get metricNiacin => 'Niacina';

  @override
  String get metricPantothenicAcid => 'Ácido pantoténico';

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
  String get metricCopper => 'Cobre';

  @override
  String get metricIodine => 'Yodo';

  @override
  String get metricIron => 'Hierro';

  @override
  String get metricMagnesium => 'Magnesio';

  @override
  String get metricManganese => 'Manganeso';

  @override
  String get metricMolybdenum => 'Molibdeno';

  @override
  String get metricPhosphorus => 'Fósforo';

  @override
  String get metricPotassium => 'Potasio';

  @override
  String get metricSelenium => 'Selenio';

  @override
  String get metricSodium => 'Sodio';

  @override
  String get metricZinc => 'Zinc';

  @override
  String get metricCaffeine => 'Cafeína';

  @override
  String get metricRespiratoryRate => 'Frecuencia respiratoria';

  @override
  String get metricAvgRespiratoryRate => 'Frecuencia respiratoria media';

  @override
  String get metricBodyTemp => 'Temp. corporal';

  @override
  String get metricBloodGlucose => 'Glucosa en sangre';

  @override
  String get metricSkinTemperature => 'Temperatura de la piel';

  @override
  String get metricRecordedSpeed => 'Velocidad registrada';

  @override
  String get metricAveragePower => 'Potencia media';

  @override
  String get metricStepsCadence => 'Cadencia de pasos';

  @override
  String get metricCyclingCadence => 'Cadencia de ciclismo';

  @override
  String get unitSteps => 'pasos';

  @override
  String get unitReps => 'reps';

  @override
  String get unitPushes => 'impulsos';

  @override
  String get unitFloors => 'pisos';

  @override
  String get unitDays => 'días';

  @override
  String get unitNights => 'noches';

  @override
  String get unitTests => 'pruebas';

  @override
  String get unitTotal => 'total';

  @override
  String get unitGrams => 'g';

  @override
  String get sectionActivities => 'Actividades';

  @override
  String get sectionActivityTypeStats => 'Por tipo de actividad';

  @override
  String activityTypeStatsActivityCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '# actividades',
      many: '# actividades',
      one: '# actividad',
    );
    return '$_temp0';
  }

  @override
  String get sectionPlannedWorkouts => 'Entrenamientos planificados';

  @override
  String get activitiesFilterActivityTypeLabel => 'Tipo de actividad';

  @override
  String get activitiesFilterAll => 'Todas las actividades';

  @override
  String get activitiesKeyMetrics => 'Métricas clave';

  @override
  String get recoverySleepScore => 'Puntuación de sueño';

  @override
  String get recoverySleepDuration => 'Duración del sueño';

  @override
  String get recoverySleepSchedule => 'Horario de sueño';

  @override
  String get recoveryRemSleep => 'Sueño REM';

  @override
  String get recoveryDeepSleep => 'Sueño profundo';

  @override
  String get recoverySleepEfficiency => 'Eficiencia del sueño';

  @override
  String get sleepScoreConfidenceHigh => 'Alta confianza';

  @override
  String get sleepScoreConfidenceMedium => 'Confianza media';

  @override
  String get sleepScoreConfidenceLow => 'Baja confianza';

  @override
  String get sleepScoreConfidenceNoData => 'Sin datos';

  @override
  String get sleepScoreRatingExcellent => 'Excelente';

  @override
  String get sleepScoreRatingGood => 'Bueno';

  @override
  String get sleepScoreRatingFair => 'Aceptable';

  @override
  String get sleepScoreRatingPoor => 'Bajo';

  @override
  String dashboardSleepScoreSubtitle(String arg0, String arg1) {
    return '$arg0 • $arg1';
  }

  @override
  String get sleepScoreCalculationTitle => 'Cómo se calcula';

  @override
  String get sleepScoreDayNumbersTitle => 'Números de hoy';

  @override
  String get sleepScoreReferencesTitle => 'Enlaces de respaldo';

  @override
  String get sleepScoreCalculationSummary =>
      'OpenVitals puntúa la salud objetiva del sueño a partir de duración, eficiencia, continuidad y regularidad. No diagnostica trastornos del sueño.';

  @override
  String get sleepScoreFormula =>
      'Puntuación = duración 35 + eficiencia 30 + continuidad 20 + regularidad 15';

  @override
  String get sleepScoreFormulaBody =>
      'La duración obtiene todo el crédito entre 7 y 9 h. La eficiencia usa el tiempo total dormido dividido por el tiempo en cama. La continuidad usa el tiempo despierto tras iniciar el sueño. La regularidad compara el punto medio del sueño de hoy con noches recientes.';

  @override
  String get sleepScoreComponentsBody =>
      'Los datos de fases aumentan la confianza, pero REM y sueño profundo no pesan mucho porque las estimaciones de fases de dispositivos de consumo pueden variar. Si falta historial de regularidad, OpenVitals usa un valor neutral y baja la confianza.';

  @override
  String get sleepScoreNotDiagnostic =>
      'Esta puntuación es una guía diaria a partir de registros de Health Connect, no un diagnóstico ni una recomendación de tratamiento.';

  @override
  String get sleepScoreComponentDuration => 'Duración';

  @override
  String get sleepScoreComponentEfficiency => 'Eficiencia';

  @override
  String get sleepScoreComponentContinuity => 'Continuidad';

  @override
  String get sleepScoreComponentRegularity => 'Regularidad';

  @override
  String get sleepScoreTotalSleep => 'Sueño total';

  @override
  String get sleepScoreTimeInBed => 'Tiempo en cama';

  @override
  String get sleepScoreEfficiency => 'Eficiencia';

  @override
  String get sleepScoreWaso => 'Despierto tras dormir';

  @override
  String get sleepScoreRegularity => 'Diferencia horaria';

  @override
  String get sleepScoreBaselineNights => 'Noches base';

  @override
  String get sleepScoreStageRecords => 'Registros de fases';

  @override
  String get sleepScoreQualityNoData =>
      'Datos de sueño insuficientes para calcular una puntuación.';

  @override
  String get sleepScoreQualityStageAwake =>
      'Usa fases de sueño y fases despiertas de Health Connect.';

  @override
  String get sleepScoreQualityStageOnly =>
      'Usa fases de sueño; la continuidad despierta puede estar estimada.';

  @override
  String get sleepScoreQualitySessionOnly =>
      'Usa solo el horario de la sesión de sueño; la confianza es limitada.';

  @override
  String get sleepScoreReferenceAasm => 'Duración de sueño en adultos de AASM';

  @override
  String get sleepScoreReferenceSleepHealth =>
      'Salud del sueño multidimensional';

  @override
  String get sleepScoreReferenceEfficiency =>
      'Definición de eficiencia del sueño';

  @override
  String get sleepScoreReferenceRegularity =>
      'Investigación de regularidad del sueño';

  @override
  String get sleepEfficiencyConfidenceHigh => 'Alta confianza';

  @override
  String get sleepEfficiencyConfidenceLow => 'Baja confianza';

  @override
  String get sleepEfficiencyConfidenceNoData => 'Sin datos';

  @override
  String get sleepEfficiencyCalculationTitle => 'Cómo se calcula';

  @override
  String get sleepEfficiencyDayNumbersTitle => 'Números de hoy';

  @override
  String get sleepEfficiencyReferencesTitle => 'Enlaces de respaldo';

  @override
  String get sleepEfficiencyCalculationSummary =>
      'La eficiencia del sueño es el porcentaje de la ventana de sueño que se pasa dormido. Valores más altos suelen significar menos tiempo despierto en cama.';

  @override
  String get sleepEfficiencyFormula =>
      'Eficiencia del sueño = sueño total / tiempo en cama x 100';

  @override
  String get sleepEfficiencyFormulaBody =>
      'El sueño total es la suma de fases de sueño de Health Connect cuando hay fases disponibles. El tiempo en cama es la ventana de inicio a fin de la sesión principal.';

  @override
  String get sleepEfficiencyDataBody =>
      'Cuando faltan fases de sueño, Health Connect puede proporcionar solo la duración de la sesión. OpenVitals puede mostrar una estimación, pero la confianza es baja porque el tiempo despierto en cama puede estar oculto.';

  @override
  String get sleepEfficiencyNotDiagnostic =>
      'La eficiencia del sueño es una señal de continuidad del sueño, no un diagnóstico. Valores persistentemente bajos pueden merecer consulta clínica.';

  @override
  String get sleepEfficiencyQualityNoData =>
      'Datos de sueño insuficientes para calcular la eficiencia.';

  @override
  String get sleepEfficiencyQualityStageBased =>
      'Usa fases de sueño de Health Connect para el sueño total.';

  @override
  String get sleepEfficiencyQualitySessionOnly =>
      'Usa solo el horario de la sesión; puede faltar tiempo despierto.';

  @override
  String get sleepEfficiencyReferenceDefinition =>
      'Definición de eficiencia del sueño';

  @override
  String get sleepEfficiencyReferenceDenominator =>
      'Investigación sobre el denominador de eficiencia';

  @override
  String get sleepEfficiencyReferenceMethods =>
      'Revisión de métodos de evaluación del sueño';

  @override
  String get cardioLoadConfidenceHigh => 'Alta confianza';

  @override
  String get cardioLoadConfidenceMedium => 'Confianza media';

  @override
  String get cardioLoadConfidenceLow => 'Baja confianza';

  @override
  String get cardioLoadConfidenceNoData => 'Sin datos';

  @override
  String get cardioLoadCalculationTitle => 'Cómo se calcula';

  @override
  String get cardioLoadDayNumbersTitle => 'Números de hoy';

  @override
  String get cardioLoadReferencesTitle => 'Enlaces de respaldo';

  @override
  String get cardioLoadCalculationSummary =>
      'OpenVitals usa TRIMP basado en FC cuando hay datos de frecuencia cardiaca disponibles y solo recurre al movimiento cuando la FC no es utilizable.';

  @override
  String get cardioLoadFormula =>
      'TRIMP = minutos x HRR x 0.64 x e^(1.92 x HRR)';

  @override
  String get cardioLoadFormulaBody =>
      'HRR es la reserva de frecuencia cardiaca: (frecuencia cardiaca - frecuencia en reposo) / (frecuencia máxima - frecuencia en reposo). OpenVitals lo suma en los intervalos de frecuencia cardiaca disponibles del día.';

  @override
  String get cardioLoadMappingBody =>
      'Cuando existen actividades registradas, las muestras de frecuencia cardiaca se asignan por hora a la ventana de inicio y fin de cada actividad. Sin ventanas de actividad, solo cuentan los intervalos con frecuencia cardiaca elevada. Si la FC no es utilizable, el movimiento y las calorías activas se muestran como respaldo de baja confianza.';

  @override
  String get cardioLoadMethod => 'Método';

  @override
  String get cardioLoadTrimpScore => 'Puntuación TRIMP';

  @override
  String get cardioLoadHrCoverage => 'Cobertura FC';

  @override
  String get cardioLoadExpectedCoverage => 'Cobertura esperada';

  @override
  String get cardioLoadRestingHr => 'FC en reposo';

  @override
  String get cardioLoadMaxHr => 'FC máxima';

  @override
  String get cardioLoadHrSamples => 'Muestras FC';

  @override
  String get cardioLoadActivityWindows => 'Ventanas de actividad';

  @override
  String get cardioLoadActivityMinutes => 'Minutos de actividad';

  @override
  String get cardioLoadMovementFallback => 'Respaldo de movimiento';

  @override
  String get cardioLoadMethodActivityWindows => 'TRIMP con FC de actividad';

  @override
  String get cardioLoadMethodElevatedHr => 'TRIMP con FC elevada';

  @override
  String get cardioLoadMethodMovementFallback => 'Respaldo de movimiento';

  @override
  String get cardioLoadMethodNoData => 'Datos insuficientes';

  @override
  String get cardioLoadCalibrationObservedResting => 'FC en reposo observada';

  @override
  String get cardioLoadCalibrationEstimatedResting => 'FC en reposo estimada';

  @override
  String get cardioLoadCalibrationObservedMax => 'FC máxima observada';

  @override
  String get cardioLoadCalibrationEstimatedMax => 'FC máxima estimada';

  @override
  String get cardioLoadReferenceBanister => 'Ecuación TRIMP de Banister';

  @override
  String get cardioLoadReferenceTrainingLoad =>
      'Revisión sobre carga de entrenamiento';

  @override
  String get cardioLoadReferenceHealthConnect =>
      'Asignación de FC en entrenamientos de Health Connect';

  @override
  String get sectionSleepSessions => 'Sesiones de sueño';

  @override
  String get sectionWeight => 'Peso';

  @override
  String get sectionEntries => 'Entradas';

  @override
  String get sectionMeals => 'Comidas';

  @override
  String get sectionSessions => 'Sesiones';

  @override
  String get sectionDailyBreakdown => 'Desglose diario';

  @override
  String get sectionVitals => 'Constantes';

  @override
  String get sectionHeart => 'Corazón';

  @override
  String get sectionCardiovascular => 'Cardiovascular';

  @override
  String get sectionRespiratory => 'Respiratorio';

  @override
  String get sectionRespiratoryRateDailyBreakdown =>
      'Desglose diario de frecuencia respiratoria';

  @override
  String get sectionVo2MaxHistory => 'Historial de VO2 máximo';

  @override
  String get sectionDisplay => 'Visualización';

  @override
  String get sectionPrivacy => 'Privacidad';

  @override
  String get sectionCycleCalendar => 'Calendario del ciclo';

  @override
  String get sectionBasalBodyTemperature => 'Temperatura corporal basal';

  @override
  String get sectionStatistics => 'Estadísticas';

  @override
  String get sectionCalorieTrends => 'Tendencias de calorías';

  @override
  String get sectionNutritionTrends => 'Tendencias de nutrición';

  @override
  String get sectionBodyTrends => 'Tendencias corporales';

  @override
  String get sectionCarbohydrates => 'Carbohidratos';

  @override
  String get sectionFats => 'Grasas';

  @override
  String get sectionVitamins => 'Vitaminas';

  @override
  String get sectionMinerals => 'Minerales';

  @override
  String get sectionOtherNutrients => 'Otros nutrientes';

  @override
  String summaryDailyAverage(String arg0) {
    return 'media diaria de $arg0';
  }

  @override
  String summaryDaysInRange(String arg0) {
    return '$arg0 días en el rango';
  }

  @override
  String summaryEntries(String arg0) {
    return '$arg0 entradas';
  }

  @override
  String summaryReadings(String arg0) {
    return '$arg0 lecturas';
  }

  @override
  String summaryNights(String arg0) {
    return '$arg0 noches';
  }

  @override
  String summaryRecordedStages(String arg0) {
    return '$arg0 fases registradas';
  }

  @override
  String get summaryAverage => 'Media';

  @override
  String summaryAvgValue(String arg0) {
    return 'Media $arg0';
  }

  @override
  String summaryAvgValueRange(String arg0, String arg1, String arg2) {
    return 'Media $arg0 · rango $arg1-$arg2';
  }

  @override
  String summaryValueAvg(String arg0) {
    return '$arg0 de media';
  }

  @override
  String get summaryRange => 'Rango';

  @override
  String get summarySamples => 'Muestras';

  @override
  String summaryRecorded(String arg0, String arg1) {
    return '$arg0-$arg1 registrado';
  }

  @override
  String summaryRestingValue(String arg0) {
    return 'Reposo $arg0';
  }

  @override
  String summaryHrvValue(String arg0) {
    return 'HRV $arg0';
  }

  @override
  String summaryLastUpdate(String arg0) {
    return 'Última actualización $arg0';
  }

  @override
  String get summaryNow => 'Ahora';

  @override
  String summaryToday(String arg0) {
    return '$arg0 hoy';
  }

  @override
  String summaryOnDate(String arg0, String arg1) {
    return '$arg0 el $arg1';
  }

  @override
  String summaryEmptyToday(String arg0) {
    return '$arg0 todavía hoy.';
  }

  @override
  String summaryEmptyDay(String arg0) {
    return '$arg0 en este día.';
  }

  @override
  String get summaryAcrossSelectedPeriod => 'En el periodo seleccionado';

  @override
  String summaryLatestTemperature(String arg0, String arg1) {
    return 'Última $arg0 · $arg1';
  }

  @override
  String summaryTemperatureRange(String arg0, String arg1, String arg2) {
    return 'Rango $arg0-$arg1 · $arg2 lecturas';
  }

  @override
  String get summarySleepEndingToday => 'Sueño que termina hoy';

  @override
  String summarySleepEndingOn(String arg0) {
    return 'Sueño que termina el $arg0';
  }

  @override
  String get statTotal => 'Total';

  @override
  String get statTime => 'Tiempo';

  @override
  String get statActiveDays => 'Días activos';

  @override
  String get statAverage => 'Media';

  @override
  String get statLowest => 'Mínimo';

  @override
  String get statHighest => 'Máximo';

  @override
  String get statReadings => 'Lecturas';

  @override
  String get statDailyAverage => 'Media diaria';

  @override
  String get caloriesStatActiveAverage => 'Media activa';

  @override
  String get caloriesStatBmrReadings => 'Lecturas de BMR';

  @override
  String get statAverageDuration => 'Duración media';

  @override
  String get statTotalIntake => 'Ingesta total';

  @override
  String get statBestDay => 'Mejor día';

  @override
  String get statNightsLogged => 'Noches registradas';

  @override
  String get statLongestSleep => 'Sueño más largo';

  @override
  String get statLongestWorkout => 'Entreno más largo';

  @override
  String get statAverageMovingPace => 'Ritmo medio en movimiento';

  @override
  String get statFastestPace => 'Ritmo más rápido';

  @override
  String get statBestSpeed => 'Mejor velocidad';

  @override
  String get statLongestSession => 'Sesión más larga';

  @override
  String get statBbtReadings => 'Lecturas BBT';

  @override
  String get statGoalStreak => 'Racha de objetivo';

  @override
  String get statLongestGoalStreak => 'Racha más larga';

  @override
  String get statGoalsMet => 'Objetivos cumplidos';

  @override
  String get statSuccessRate => 'Tasa de éxito';

  @override
  String get statAverageGap => 'Diferencia media';

  @override
  String get statVsPreviousDay => 'Vs día anterior';

  @override
  String get statVsPreviousWeek => 'Vs semana anterior';

  @override
  String get statVsPreviousMonth => 'Vs mes anterior';

  @override
  String get statVsPreviousYear => 'Vs año anterior';

  @override
  String get statBaseline => 'Base';

  @override
  String get stat30DayBaseline => 'Media 30 días';

  @override
  String get stat60DayBaseline => 'Media 60 días';

  @override
  String get stat90DayBaseline => 'Media 90 días';

  @override
  String get statUsualRange => 'Rango habitual';

  @override
  String get statBaselineDeviation => 'Desviación base';

  @override
  String get baselineStatusUsual => 'Habitual';

  @override
  String get baselineStatusAbove => 'Por encima';

  @override
  String get baselineStatusBelow => 'Por debajo';

  @override
  String get baselineStatusUnusualHigh => 'Alto inusual';

  @override
  String get baselineStatusUnusualLow => 'Bajo inusual';

  @override
  String get sectionMetricContext => 'Contexto';

  @override
  String get interpretationBpTitle => 'Categoría de presión arterial';

  @override
  String get interpretationBpNormal => 'Normal';

  @override
  String get interpretationBpElevated => 'Elevada';

  @override
  String get interpretationBpStage1 => 'Presión arterial alta etapa 1';

  @override
  String get interpretationBpStage2 => 'Presión arterial alta etapa 2';

  @override
  String get interpretationBpSevere => 'Referencia de rango severo';

  @override
  String interpretationBpBody(String arg0) {
    return 'Esta lectura está en el rango $arg0. Una sola lectura de la app no es un diagnóstico.';
  }

  @override
  String get interpretationBpSevereBody =>
      'Esta lectura está por encima de la referencia de rango severo. Repítela; busca atención urgente si hay síntomas o la lectura sigue muy alta.';

  @override
  String get interpretationBpSource =>
      'Fuente: categorías de presión arterial en adultos de la American Heart Association.';

  @override
  String get interpretationBmiTitle => 'Categoría de IMC';

  @override
  String get interpretationBmiUnderweight => 'Bajo peso';

  @override
  String get interpretationBmiHealthy => 'Peso saludable';

  @override
  String get interpretationBmiOverweight => 'Sobrepeso';

  @override
  String get interpretationBmiObesity1 => 'Obesidad clase 1';

  @override
  String get interpretationBmiObesity2 => 'Obesidad clase 2';

  @override
  String get interpretationBmiObesity3 => 'Obesidad clase 3';

  @override
  String get interpretationBmiBody =>
      'Categoría de cribado de IMC para adultos; el IMC no mide la composición corporal.';

  @override
  String get interpretationBmiSource =>
      'Fuente: categorías de IMC en adultos de los CDC.';

  @override
  String get interpretationFfmiTitle => 'Categoría de FFMI';

  @override
  String get interpretationFfmiBelowAverage => 'Por debajo del promedio';

  @override
  String get interpretationFfmiAverage => 'Promedio';

  @override
  String get interpretationFfmiAboveAverage => 'Por encima del promedio';

  @override
  String get interpretationFfmiExcellent => 'Excelente';

  @override
  String get interpretationFfmiSuperior => 'Superior';

  @override
  String get interpretationFfmiExceptional => 'Excepcional';

  @override
  String get interpretationFfmiElite => 'Élite';

  @override
  String interpretationFfmiBody(String arg0, String arg1) {
    return 'FFMI $arg0; FFMI ajustado $arg1. Usa tu peso, grasa corporal y altura más recientes.';
  }

  @override
  String get interpretationFfmiSource =>
      'Fuente: categorías indicativas de FFMI ajustado de ffmicalculators.com.';

  @override
  String get interpretationSleepTitle => 'Objetivo de sueño';

  @override
  String get interpretationSleepBelow => 'Por debajo del objetivo';

  @override
  String get interpretationSleepNear => 'Cerca del objetivo';

  @override
  String get interpretationSleepMet => 'Objetivo cumplido';

  @override
  String interpretationSleepBelowBody(String arg0) {
    return 'El sueño medio está $arg0 por debajo de tu objetivo configurado.';
  }

  @override
  String interpretationSleepNearBody(String arg0, String arg1) {
    return 'El sueño medio está cerca de tu objetivo configurado: $arg0 frente a $arg1.';
  }

  @override
  String interpretationSleepMetBody(String arg0, String arg1) {
    return 'El sueño medio cumple tu objetivo configurado: $arg0 frente a $arg1.';
  }

  @override
  String get interpretationSleepSource =>
      'Basado en tu objetivo de sueño configurado, no en una evaluación médica del sueño.';

  @override
  String get interpretationMacroTitle => 'Distribución de macros';

  @override
  String get interpretationMacroWithin => 'Dentro de la referencia';

  @override
  String get interpretationMacroOutside => 'Fuera de la referencia';

  @override
  String interpretationMacroBody(String arg0, String arg1, String arg2) {
    return 'Proteína $arg0, carbohidratos $arg1, grasa $arg2 de las calorías registradas de macros.';
  }

  @override
  String get interpretationMacroSource =>
      'Fuente: referencia AMDR para adultos de National Academies; solo macros registrados.';

  @override
  String get interpretationWorkoutTitle => 'Progreso de guía de entrenamiento';

  @override
  String get interpretationWorkoutNone => 'Sin minutos registrados';

  @override
  String get interpretationWorkoutBelow =>
      'Por debajo de la referencia semanal';

  @override
  String get interpretationWorkoutApproaching =>
      'Cerca de la referencia semanal';

  @override
  String get interpretationWorkoutMet => 'Referencia semanal cumplida';

  @override
  String interpretationWorkoutBody(String arg0, String arg1) {
    return 'Registrado $arg0 hacia la referencia adulta de 150 min/semana ($arg1). La intensidad no se verifica.';
  }

  @override
  String interpretationWorkoutBodyWeeklyAverage(String arg0, String arg1) {
    return 'Media semanal $arg0 hacia la referencia adulta de 150 min/semana ($arg1). La intensidad no se verifica.';
  }

  @override
  String get interpretationWorkoutSource =>
      'Fuente: referencia de guía de actividad física para adultos del HHS.';

  @override
  String get interpretationVitalTitle => 'Contexto de constante';

  @override
  String get interpretationVitalWithin =>
      'Dentro de la referencia adulta amplia';

  @override
  String get interpretationVitalBelow =>
      'Por debajo de la referencia adulta amplia';

  @override
  String get interpretationVitalAbove =>
      'Por encima de la referencia adulta amplia';

  @override
  String get interpretationVitalOxygenBelowTypical =>
      'Por debajo del rango típico de oxígeno';

  @override
  String get interpretationVitalOxygenLow => 'Referencia de oxígeno bajo';

  @override
  String get interpretationVitalOxygenVeryLow =>
      'Referencia de oxígeno muy bajo';

  @override
  String get interpretationVitalRestingHrBody =>
      'Solo referencia adulta amplia; forma física, medicación, estrés, enfermedad y momento de medición pueden cambiar lo habitual para ti.';

  @override
  String get interpretationVitalRespiratoryBody =>
      'Solo referencia adulta amplia; actividad, ansiedad, enfermedad y momento de medición pueden afectar la frecuencia respiratoria.';

  @override
  String get interpretationVitalTemperatureBody =>
      'La temperatura varía según la zona de medición y la hora del día; úsala solo como contexto.';

  @override
  String get interpretationVitalOxygenBody =>
      'Las lecturas de pulsioxímetro pueden verse afectadas por dispositivo, piel, circulación, movimiento y condiciones.';

  @override
  String get interpretationVitalSource =>
      'Fuente: referencia de constantes vitales en adultos de MedlinePlus.';

  @override
  String get interpretationOxygenSource =>
      'Fuente: contexto de pulsioxímetro de MedlinePlus y FDA.';

  @override
  String get sectionCrossMetricInsights => 'Insights entre métricas';

  @override
  String get crossMetricPositiveLink => 'Relación positiva';

  @override
  String get crossMetricNegativeLink => 'Relación negativa';

  @override
  String get crossMetricWeakLink => 'Relación débil';

  @override
  String crossMetricCorrelation(String arg0) {
    return '$arg0';
  }

  @override
  String crossMetricPairedDays(int arg0) {
    return '$arg0 días emparejados';
  }

  @override
  String get crossSleepHrvTitle => 'Sueño vs HRV';

  @override
  String get crossSleepHrvPositive =>
      'Más sueño tiende a coincidir con una HRV más alta en este periodo.';

  @override
  String get crossSleepHrvNegative =>
      'Más sueño tiende a coincidir con una HRV más baja en este periodo.';

  @override
  String get crossSleepHrvNeutral =>
      'Sueño y HRV no muestran un patrón claro en este periodo.';

  @override
  String get crossWorkoutRestingHrTitle => 'Entrenos vs frecuencia en reposo';

  @override
  String get crossWorkoutRestingHrPositive =>
      'Más minutos de entreno tienden a coincidir con una frecuencia en reposo más alta en este periodo.';

  @override
  String get crossWorkoutRestingHrNegative =>
      'Más minutos de entreno tienden a coincidir con una frecuencia en reposo más baja en este periodo.';

  @override
  String get crossWorkoutRestingHrNeutral =>
      'Minutos de entreno y frecuencia en reposo no muestran un patrón claro en este periodo.';

  @override
  String get crossHydrationWeightTitle => 'Hidratación vs fluctuación de peso';

  @override
  String get crossHydrationWeightPositive =>
      'Más hidratación tiende a coincidir con mayores cambios de peso en este periodo.';

  @override
  String get crossHydrationWeightNegative =>
      'Más hidratación tiende a coincidir con menores cambios de peso en este periodo.';

  @override
  String get crossHydrationWeightNeutral =>
      'Hidratación y fluctuación de peso no muestran un patrón claro en este periodo.';

  @override
  String get crossMindfulnessSleepTitle => 'Mindfulness vs sueño';

  @override
  String get crossMindfulnessSleepPositive =>
      'Más minutos de mindfulness tienden a coincidir con un sueño más largo en este periodo.';

  @override
  String get crossMindfulnessSleepNegative =>
      'Más minutos de mindfulness tienden a coincidir con un sueño más corto en este periodo.';

  @override
  String get crossMindfulnessSleepNeutral =>
      'Mindfulness y sueño no muestran un patrón claro en este periodo.';

  @override
  String get legendLess => 'Menos';

  @override
  String get legendMore => 'Más';

  @override
  String get dailyGoal => 'Objetivo diario';

  @override
  String goalProgress(int arg0, int arg1) {
    return '$arg0 de $arg1 días registrados cumplidos';
  }

  @override
  String get cdDecreaseDailyGoal => 'Reducir objetivo diario';

  @override
  String get cdIncreaseDailyGoal => 'Aumentar objetivo diario';

  @override
  String get hydrationDailyGoal => 'Objetivo diario';

  @override
  String hydrationGoalProgress(int arg0, int arg1) {
    return '$arg0 de $arg1 días registrados cumplidos';
  }

  @override
  String get hydrationRemindersTitle => 'Recordatorios de hidratación';

  @override
  String get hydrationRemindersSummaryOff =>
      'Desactivados por defecto. Activa recordatorios durante las horas activas hasta alcanzar el objetivo de hidratación de hoy.';

  @override
  String hydrationRemindersSummaryOn(int arg0, String arg1, String arg2) {
    return 'Cada $arg0 min • $arg1-$arg2';
  }

  @override
  String get hydrationRemindersPermissionNeeded =>
      'Concede permiso de notificaciones para activar recordatorios de hidratación.';

  @override
  String get hydrationRemindersInterval => 'Intervalo de recordatorio';

  @override
  String hydrationRemindersIntervalValue(int arg0) {
    return 'Cada $arg0 min';
  }

  @override
  String get hydrationRemindersActiveStart => 'Activo desde';

  @override
  String get hydrationRemindersActiveEnd => 'Activo hasta';

  @override
  String get hydrationRemindersGoalNote =>
      'Los recordatorios se pausan cuando alcanzas el objetivo de hoy y se reanudan mañana.';

  @override
  String get hydrationReminderNotificationChannel =>
      'Recordatorios de hidratación';

  @override
  String get hydrationReminderNotificationChannelDesc =>
      'Recordatorios opcionales para registrar hidratación durante las horas activas.';

  @override
  String get hydrationReminderNotificationTitle =>
      'Recordatorio de hidratación';

  @override
  String hydrationReminderNotificationBody(String arg0, String arg1) {
    return 'Hoy vas por $arg0 de $arg1. Añade una bebida cuando puedas.';
  }

  @override
  String hydrationReminderNotificationProgress(String arg0, String arg1) {
    return '$arg0 / $arg1';
  }

  @override
  String get hydrationTrackerTitle => 'Registrar hidratación';

  @override
  String get hydrationTrackerSubtitle =>
      'Guardado directamente en Health Connect';

  @override
  String get hydrationTrackerPermissionNeeded =>
      'Para el resumen, OpenVitals solo pide permisos de visualización. Para añadir esta entrada manual, necesitamos permiso de escritura. OpenVitals no almacenará estos datos; las entradas se guardan en Health Connect.';

  @override
  String get hydrationNutritionPermissionNeeded =>
      'Concede permiso de escritura de nutrición para guardar los nutrientes de las bebidas en Health Connect.';

  @override
  String get hydrationCustomDrinksTitle => 'Bebidas guardadas';

  @override
  String get hydrationCatalogDrinksTitle => 'Catálogo de bebidas';

  @override
  String get hydrationCatalogSearch => 'Buscar bebidas';

  @override
  String get hydrationCatalogFrequentlyConsumed => 'Bebidas frecuentes';

  @override
  String get hydrationCatalogSavedOutside => 'Bebidas guardadas';

  @override
  String get hydrationCatalogSectionWater => 'Agua';

  @override
  String get hydrationCatalogSectionCoffees => 'Cafés';

  @override
  String get hydrationCatalogSectionEnergyDrinks => 'Bebidas energéticas';

  @override
  String get hydrationCatalogSectionTeas => 'Tés';

  @override
  String get hydrationCatalogSectionChocolateDrinks => 'Bebidas de chocolate';

  @override
  String get hydrationCatalogSectionCarbonatedSoftDrinks => 'Refrescos con gas';

  @override
  String get hydrationCatalogSectionOtherDrinks => 'Otras bebidas';

  @override
  String hydrationCatalogSectionCount(int arg0) {
    return '$arg0 bebidas';
  }

  @override
  String get hydrationNewDrinkAction => 'Nueva bebida';

  @override
  String get hydrationNewDrinkTitle => 'Nueva bebida';

  @override
  String get hydrationEditDrinkTitle => 'Editar bebida';

  @override
  String hydrationLogSavedDrinkTitle(String arg0) {
    return 'Registrar $arg0';
  }

  @override
  String get hydrationCustomDrinkName => 'Nombre';

  @override
  String get hydrationCustomDrinkCategory => 'Categoría';

  @override
  String get hydrationCustomDrinkNoCategory => 'Sin categoría';

  @override
  String get hydrationCustomDrinkHydrationImpact => 'Impacto en hidratación';

  @override
  String get hydrationImpactCountsFully => 'Cuenta completamente';

  @override
  String get hydrationImpactCountsPartially => 'Cuenta parcialmente';

  @override
  String get hydrationImpactDoesNotCount => 'No cuenta';

  @override
  String get hydrationImpactCountsFullyBody =>
      'Todo el volumen de la bebida cuenta para la hidratación.';

  @override
  String get hydrationImpactCountsPartiallyBody =>
      'Usa un porcentaje de esta bebida.';

  @override
  String get hydrationImpactDoesNotCountBody =>
      'Guárdala sin añadir hidratación.';

  @override
  String get hydrationImpactPercentLabel => 'Cuenta como hidratación (%)';

  @override
  String get hydrationImpactInvalidPercent =>
      'Introduce un porcentaje mayor que 0 y menor que 100.';

  @override
  String get hydrationCustomDrinkNutrients => 'Nutrientes';

  @override
  String get hydrationCustomDrinkAddNutrient => 'Añadir nutriente';

  @override
  String get hydrationCustomDrinkLiquidOnly => 'Solo líquido';

  @override
  String hydrationCustomDrinkNutrientCount(int arg0) {
    return 'Nutrientes: $arg0';
  }

  @override
  String hydrationSavedDrinkAmountNoHydration(String arg0) {
    return '$arg0 • No cuenta como hidratación';
  }

  @override
  String hydrationSavedDrinkAmountPartialHydration(String arg0, int arg1) {
    return '$arg0 • Cuenta $arg1% como hidratación';
  }

  @override
  String get hydrationNonHydratingDrinkSavedHint =>
      'Guardada solo como nutrición. No se añadió hidratación.';

  @override
  String get hydrationEntryNutritionOnly => 'Solo nutrición';

  @override
  String get hydrationEntryNoHydration => 'Sin hidratación';

  @override
  String get hydrationCustomDrinkAmountGrams => 'Cantidad (g)';

  @override
  String get hydrationCustomDrinkAmountKcal => 'Cantidad (kcal)';

  @override
  String get hydrationCustomDrinkInvalid =>
      'Introduce un nombre de bebida, una cantidad y cantidades positivas de nutrientes.';

  @override
  String get hydrationInvalidAmount =>
      'Introduce una cantidad mayor que cero y no superior a 100 L.';

  @override
  String hydrationDrinkAmountLabel(String arg0) {
    return 'Cantidad ($arg0)';
  }

  @override
  String hydrationDrinkInvalidAmountRange(String arg0, String arg1) {
    return 'Introduce una cantidad de $arg0 a $arg1.';
  }

  @override
  String hydrationWriteFailed(String arg0) {
    return 'No se pudo guardar la entrada de hidratación: $arg0';
  }

  @override
  String get cdDecreaseHydrationGoal => 'Reducir objetivo de hidratación';

  @override
  String get cdIncreaseHydrationGoal => 'Aumentar objetivo de hidratación';

  @override
  String get cdDecreaseHydrationReminderInterval =>
      'Reducir intervalo de recordatorio de hidratación';

  @override
  String get cdIncreaseHydrationReminderInterval =>
      'Aumentar intervalo de recordatorio de hidratación';

  @override
  String get unitPercentSymbol => '%';

  @override
  String get messageNoDashboardData => 'No hay datos del resumen disponibles.';

  @override
  String get messageMissingPermissionsTitle => 'Faltan algunos permisos';

  @override
  String get messageMissingPermissionsBody =>
      'Concede los permisos que faltan para ver un resumen completo.';

  @override
  String messageHealthConnectRateLimited(int arg0) {
    return 'Se alcanzó el límite de Health Connect. Espera aproximadamente $arg0 min e inténtalo de nuevo.';
  }

  @override
  String get messageNoWorkoutsDay =>
      'No hay entrenamientos registrados en este día.';

  @override
  String get messageNoSleepDay =>
      'No hay ninguna sesión de sueño terminada en este día.';

  @override
  String get messageNoBloodPressure => 'No hay lectura de presión arterial.';

  @override
  String get messageNoOxygen => 'No hay lectura de oxígeno.';

  @override
  String get messageNoVo2Max => 'No hay lectura de VO2 máx.';

  @override
  String get messageNoBloodGlucose => 'No hay lectura de glucosa en sangre.';

  @override
  String get messageNoSkinTemperature =>
      'No hay lectura de temperatura de la piel.';

  @override
  String get messageCycleBrowse => 'Ver calendario y lecturas del ciclo.';

  @override
  String get dashboardAddWidgets => 'Añadir widgets';

  @override
  String get dashboardAllWidgetsAdded =>
      'Todos los widgets ya están en el resumen.';

  @override
  String get dashboardActionLog => 'Registrar';

  @override
  String get dashboardActionStartWorkout => 'Iniciar actividad';

  @override
  String get dashboardActivitiesToday => 'Actividades';

  @override
  String get dashboardSensorStatusTitle => 'Batería de sensores';

  @override
  String dashboardSensorBatteryLowest(int arg0) {
    return '$arg0% mínimo';
  }

  @override
  String get dashboardSensorBatteryUnknown => 'Batería pendiente';

  @override
  String dashboardSensorStatusActiveConnected(int arg0, int arg1) {
    return '$arg0 activos • $arg1 conectados';
  }

  @override
  String get dashboardSensorStatusAllDisabled =>
      'Todos los sensores desactivados';

  @override
  String get dashboardDeleteActivityTitle => '¿Borrar actividad?';

  @override
  String dashboardDeleteActivityMessage(String arg0) {
    return '¿Borrar esta actividad de $arg0 de OpenVitals?';
  }

  @override
  String get dashboardReadinessTitle => 'Preparación diaria';

  @override
  String get dashboardReadinessScore => 'Preparación';

  @override
  String get dashboardReadinessBodyEnergy => 'Energía corporal';

  @override
  String get dashboardReadinessTraining => 'Preparación para entrenar';

  @override
  String get dashboardReadinessHrvStatus => 'Estado de HRV';

  @override
  String get dashboardReadinessIntensityMinutes => 'Minutos de intensidad';

  @override
  String get dashboardReadinessStressLevel => 'Nivel de estrés';

  @override
  String get dashboardReadinessRecommended => 'Recomendado';

  @override
  String get dashboardReadinessAvoid => 'Evitar';

  @override
  String get dashboardReadinessAlternative => 'Alternativa';

  @override
  String get dashboardReadinessStrain => 'Objetivo de esfuerzo';

  @override
  String get dashboardReadinessGoal => 'Objetivo adaptable';

  @override
  String get dashboardReadinessRecoveryMode => 'Modo recuperación';

  @override
  String get dashboardReadinessRecoveryModeBody =>
      'Los objetivos de actividad se reducen para que puedas centrarte en descansar.';

  @override
  String get dashboardReadinessWhy => 'Por qué esta recomendación';

  @override
  String get stressDetailsHowTracked => 'Cómo se calcula';

  @override
  String get stressDetailsHowTrackedBody =>
      'OpenVitals estima el estrés fisiológico localmente con la HRV frente a tu referencia, la frecuencia cardiaca en reposo frente a tu referencia y la frecuencia cardiaca media comparada con la de reposo. Es una estimación de carga, no un diagnóstico ni un detector de estrés mental.';

  @override
  String get stressDetailsScale =>
      'Escala: 0-25 reposo, 26-50 bajo, 51-75 medio, 76-100 alto.';

  @override
  String get stressDetailsInputs => 'Datos usados';

  @override
  String get stressDetailsNoInputs =>
      'No había señales útiles de HRV o referencia de frecuencia cardiaca.';

  @override
  String get stressDetailsDataCoverage => 'Cobertura de datos';

  @override
  String get stressDetailsNoDataCoverage =>
      'No había cobertura de muestras de frecuencia cardiaca o HRV del mismo día.';

  @override
  String get stressDetailsCaveats => 'Advertencias';

  @override
  String get stressDetailsRelaxationPrompt =>
      'Si te parece correcto, prueba una sesión corta de respiración o mindfulness y vuelve a comprobarlo tras un periodo tranquilo.';

  @override
  String get readinessDetailsHowCalculated => 'Cómo se calcula';

  @override
  String get readinessDetailsSignalsUsed => 'Señales usadas';

  @override
  String get readinessDetailsGuidance => 'Qué significa';

  @override
  String get readinessDetailsCaveats => 'Advertencias';

  @override
  String get readinessDetailsCaveatLocal =>
      'Es una estimación local basada en reglas con los datos disponibles en OpenVitals.';

  @override
  String get readinessDetailsCaveatNotMedical =>
      'No es un diagnóstico, consejo médico, entrenamiento personalizado ni predicción de lesiones.';

  @override
  String get readinessDetailsCaveatMissingData =>
      'Los permisos faltantes, muestras escasas o referencias incompletas reducen la confianza.';

  @override
  String get readinessDetailsScoreStrong => 'Fuerte';

  @override
  String get readinessDetailsScoreSteady => 'Estable';

  @override
  String get readinessDetailsScoreLimited => 'Limitado';

  @override
  String get readinessDetailsScoreLow => 'Bajo';

  @override
  String get readinessDetailsScoreNeedsMoreData => 'Necesita más datos';

  @override
  String get bodyEnergyDetailsHowCalculatedBody =>
      'Energía corporal usa señales de recuperación: sueño, estado de HRV, frecuencia cardiaca en reposo, estrés fisiológico, temperatura, hidratación, nutrición y mindfulness. Estima cuánta capacidad de recuperación se ve hoy.';

  @override
  String get bodyEnergyDetailsScale =>
      'Escala: 80-100 fuerte, 60-79 estable, 40-59 limitado, 0-39 bajo.';

  @override
  String get bodyEnergyDetailsSummary =>
      'Una puntuación de recuperación para saber cuánta energía respaldan hoy tus señales corporales.';

  @override
  String get bodyEnergyDetailsNoSignals =>
      'No había señales útiles de recuperación.';

  @override
  String get trainingReadinessDetailsHowCalculatedBody =>
      'Preparación para entrenar usa señales de entrenamiento: sueño, estado de HRV, frecuencia cardiaca en reposo, carga de entrenamiento, minutos de intensidad, estrés fisiológico, temperatura y actividad. Estima si hoy encaja entrenar más fuerte.';

  @override
  String get trainingReadinessDetailsScale =>
      'Escala: 80-100 listo para entrenar fuerte, 60-79 entrenamiento controlado, 40-59 entrenamiento ligero, 0-39 centrado en descanso.';

  @override
  String get trainingReadinessDetailsSummary =>
      'Una puntuación de entrenamiento para saber si la recuperación y la carga de hoy respaldan la intensidad.';

  @override
  String get trainingReadinessDetailsNoSignals =>
      'No había señales útiles de preparación para entrenar.';

  @override
  String dashboardGoalOf(String arg0) {
    return 'de $arg0';
  }

  @override
  String get caloriesEstimatedActiveBmr =>
      'Sin total, estimado con activas + BMR';

  @override
  String caloriesEstimatedValue(String arg0) {
    return 'Est. $arg0';
  }

  @override
  String dashboardWeeklyCardioLoadProgress(int arg0, int arg1) {
    return '$arg0 de $arg1';
  }

  @override
  String dashboardCardioLoadPercentOnly(int arg0) {
    return '$arg0%';
  }

  @override
  String dashboardCardioLoadPercent(int arg0) {
    return '$arg0% carga';
  }

  @override
  String dashboardCardioLoadTodayDelta(int arg0) {
    return '+$arg0% hoy';
  }

  @override
  String get messageNoActivitiesPeriod =>
      'No hay actividades en el periodo seleccionado.';

  @override
  String get plannedWorkoutCompleted => 'Completado';

  @override
  String plannedWorkoutBlocks(int arg0) {
    return '$arg0 bloques';
  }

  @override
  String get messageNoStepUpdates =>
      'No se registraron actualizaciones de pasos';

  @override
  String get messageNoDistanceUpdates =>
      'No se registraron actualizaciones de distancia';

  @override
  String get messageNoCaloriesBurned =>
      'No se registraron datos de calorías totales';

  @override
  String get messageNoFloorsClimbed =>
      'No se registraron datos de pisos subidos';

  @override
  String get messageNoActiveCalories =>
      'No se registraron datos de calorías activas';

  @override
  String get messageNoCalorieDataPeriod =>
      'No hay datos de calorías totales, activas ni BMR en este periodo.';

  @override
  String get messageNoElevation => 'No se registraron datos de elevación';

  @override
  String get messageNoWheelchairPushes =>
      'No se registraron impulsos de silla de ruedas';

  @override
  String get messageNoSleepDaySelected =>
      'No hay datos de sueño para el día seleccionado.';

  @override
  String get messageNoSleepPeriod =>
      'No hay datos de sueño en el periodo seleccionado.';

  @override
  String get messageNoHeartPeriod =>
      'No hay datos de frecuencia cardiaca en el periodo seleccionado.\n\nComprueba que el permiso de frecuencia cardiaca esté concedido y que un dispositivo conectado haya sincronizado datos.';

  @override
  String get messageNoHeartSamplesDay =>
      'No hay muestras de frecuencia cardiaca registradas en este día.';

  @override
  String get messageHeartEmptyHint =>
      'Prueba otra fecha o comprueba que un dispositivo conectado haya sincronizado datos puntuales de frecuencia cardiaca.';

  @override
  String get messageNoWeightPeriod =>
      'No hay datos de peso en el periodo seleccionado.\n\nSincroniza una báscula o un wearable que informe peso a Health Connect.';

  @override
  String get messageNoHydrationPeriod =>
      'No se registraron entradas de hidratación en este periodo.';

  @override
  String get messageNoHydrationAddedPeriod =>
      'No se añadió hidratación en este periodo.';

  @override
  String get messageNoNutritionPeriod =>
      'No se registraron entradas de nutrición en este periodo.';

  @override
  String get messageNoMindfulnessPeriod =>
      'No se registraron sesiones de mindfulness en este periodo.';

  @override
  String get messageNoVitalsPeriod =>
      'No se registraron constantes en este periodo.';

  @override
  String get messageNoReadingsPeriod => 'No hay lecturas en este periodo.';

  @override
  String get messageNoCyclePeriod =>
      'No se registraron datos de ciclo en este periodo.';

  @override
  String get messageNoSegments => 'No hay segmentos registrados.';

  @override
  String get messageNoLaps => 'No hay vueltas registradas.';

  @override
  String get messageNoRoutePoints => 'No hay puntos de ruta registrados.';

  @override
  String get messageRouteConsentRequired =>
      'Los datos de ruta están disponibles, pero aún no se ha concedido acceso a la ruta. Abre los permisos de Health Connect desde Ajustes para activar las vistas previas de ruta.';

  @override
  String get messageNoRouteData => 'No hay datos de ruta registrados.';

  @override
  String get messageNoStages => 'No hay fases registradas.';

  @override
  String get messageNoKcal => 'Sin kcal';

  @override
  String get onboardingTagline => 'Tus datos de salud, en tu dispositivo';

  @override
  String get onboardingPrivacyTitle => 'Privacidad primero';

  @override
  String get onboardingPrivacyBody =>
      'No hace falta cuenta. Los datos permanecen en tu dispositivo. Sin subida a la nube, sin analíticas y sin anuncios.';

  @override
  String get healthDisclaimerTitle => 'Aviso de salud';

  @override
  String get healthDisclaimerBody =>
      'OpenVitals es solo para bienestar general e información. No es un dispositivo médico y no ofrece asesoramiento médico. No diagnostica, trata, cura ni previene ninguna enfermedad o afección médica. Consulta siempre a un profesional sanitario cualificado para recibir asesoramiento, diagnóstico o tratamiento médico.';

  @override
  String get onboardingHealthConnectTitle => 'Con tecnología de Health Connect';

  @override
  String get onboardingHealthConnectBody =>
      'Lee desde el almacén de salud seguro del dispositivo de Android y guarda en Health Connect las entradas que crees. Funciona con todos los datos importados en Health Connect.';

  @override
  String get onboardingPermissionsHeader => 'PERMISOS DE HEALTH CONNECT';

  @override
  String get onboardingGrantCore =>
      'Conceder permisos necesarios de Health Connect';

  @override
  String get onboardingGrantAll =>
      'Conceder permisos necesarios de Health Connect';

  @override
  String get onboardingGrantRemaining =>
      'Conceder permisos disponibles restantes';

  @override
  String get onboardingOpenRequiredPermissions =>
      'Abrir permisos necesarios de Health Connect';

  @override
  String get onboardingUnableOpenPermissions =>
      'No se pueden abrir los permisos de Health Connect.';

  @override
  String get onboardingHealthConnectNotSupported =>
      'Health Connect no es compatible con este dispositivo.';

  @override
  String get onboardingHealthConnectNeedsPlayStore =>
      'Este dispositivo usa Android 13 y tiene instalada la app independiente de Health Connect. Por desgracia, esta versión depende de los servicios de Google Play Store, que no están instalados en este dispositivo, así que Health Connect rechaza las solicitudes antes de que OpenVitals pueda leer tus datos. OpenVitals no puede arreglar ni evitar este problema del dispositivo. La única forma de resolverlo es instalar los servicios de Google Play o actualizar a Android 14 o superior, donde Health Connect forma parte del sistema operativo y no necesita los servicios de Google.';

  @override
  String get onboardingHealthConnectUpdate =>
      'Health Connect debe instalarse o actualizarse para usar esta app.';

  @override
  String get onboardingInstallHealthConnect => 'Instalar Health Connect';

  @override
  String get onboardingStatusNotSupported => 'No compatible';

  @override
  String get onboardingStatusGranted => 'Concedido';

  @override
  String onboardingStatusPartiallyGranted(int arg0, int arg1) {
    return '$arg0/$arg1 concedidos';
  }

  @override
  String get onboardingStatusManual => 'Abrir ajustes';

  @override
  String get onboardingStatusRequired => 'Obligatorio';

  @override
  String get onboardingStatusOptional => 'Opcional';

  @override
  String get onboardingCategoryActivitySleep => 'Actividad y sueño';

  @override
  String get onboardingCategoryActivitySleepDesc =>
      'Health Connect pedirá:\n* Pasos\n* Distancia\n* Ejercicio\n* Sueño';

  @override
  String get onboardingCategoryHeartRecovery => 'Corazón y constantes';

  @override
  String get onboardingCategoryHeartRecoveryDesc =>
      'Health Connect pedirá:\n* Frecuencia cardiaca\n* Frecuencia cardiaca en reposo\n* Variabilidad de la frecuencia cardiaca';

  @override
  String get onboardingCategoryBody => 'Cuerpo';

  @override
  String get onboardingCategoryBodyDesc =>
      'Health Connect pedirá:\n* Peso\n* Altura\n* Grasa corporal\n* Masa corporal magra\n* Tasa metabólica basal\n* Masa ósea\n* Masa de agua corporal';

  @override
  String get onboardingCategoryActivityExtras => 'Extras de actividad';

  @override
  String get onboardingCategoryActivityExtrasDesc =>
      'Health Connect pedirá:\n* Calorías totales quemadas\n* Calorías activas quemadas\n* Pisos subidos\n* Elevación ganada\n* Impulsos en silla de ruedas\n* Velocidad\n* Potencia\n* Cadencia de pasos\n* Cadencia de pedaleo\n* Ejercicio planificado, si es compatible';

  @override
  String get onboardingCategoryNutritionHydration => 'Nutrición e hidratación';

  @override
  String get onboardingCategoryNutritionHydrationDesc =>
      'Health Connect pedirá:\n* Hidratación\n* Nutrición';

  @override
  String get onboardingCategoryManualEntryWrite =>
      'Acceso de escritura para entradas manuales';

  @override
  String get onboardingCategoryManualEntryWriteDesc =>
      'Health Connect pedirá acceso de escritura para:\n* Ejercicio\n* Distancia\n* Elevación ganada\n* Calorías activas quemadas\n* Calorías totales quemadas\n* Ruta de ejercicio\n* Hidratación\n* Peso\n* Altura\n* Grasa corporal\n* Presión arterial\n* Saturación de oxígeno\n* Frecuencia respiratoria\n* Temperatura corporal\n* Mindfulness, si es compatible';

  @override
  String get onboardingCategoryDataImportWrite =>
      'Acceso de escritura para importación de datos';

  @override
  String get onboardingCategoryDataImportWriteDesc =>
      'Health Connect pedirá acceso de escritura para registros importados:\n* Actividad, ejercicio, calorías y distancia\n* Frecuencia cardiaca, frecuencia en reposo y variabilidad de la frecuencia cardiaca\n* Mediciones corporales\n* Hidratación y nutrición\n* Sueño\n* Constantes\n* Mindfulness, si es compatible\n* Registros de seguimiento del ciclo';

  @override
  String get onboardingCategoryMindfulness => 'Mindfulness';

  @override
  String get onboardingCategoryMindfulnessDesc =>
      'Health Connect pedirá:\n* Sesiones de mindfulness';

  @override
  String get onboardingCategoryMindfulnessUnavailable =>
      'Las sesiones de mindfulness requieren una versión más reciente de Health Connect.';

  @override
  String get onboardingCategoryAdditionalDataAccess =>
      'Acceso adicional a datos';

  @override
  String get onboardingCategoryAdditionalDataAccessDesc =>
      'En los permisos de Health Connect, abre OpenVitals > Acceso adicional y configura:\n* Acceso a datos anteriores: Habilitar\n* Acceso a datos en segundo plano: Habilitar\n* Acceso a rutas de ejercicio: Siempre';

  @override
  String onboardingCategoryAdditionalDataAccessManualNote(String arg0) {
    return '$arg0\n\nSi Acceso a rutas de ejercicio no aparece en el diálogo de acceso, abre los ajustes de Health Connect para OpenVitals y configúralo en Acceso adicional.';
  }

  @override
  String get onboardingCategoryVitals => 'Constantes';

  @override
  String get onboardingCategoryVitalsDesc =>
      'Health Connect pedirá:\n* Presión arterial\n* Saturación de oxígeno\n* Frecuencia respiratoria\n* Temperatura corporal\n* VO2 máx.\n* Glucosa en sangre\n* Temperatura de la piel, si es compatible';

  @override
  String get onboardingCategoryCycleTracking => 'Seguimiento del ciclo';

  @override
  String get onboardingCategoryCycleTrackingDesc =>
      'Health Connect pedirá datos sensibles del ciclo:\n* Flujo menstrual\n* Periodos menstruales\n* Pruebas de ovulación\n* Moco cervical\n* Temperatura corporal basal\n* Sangrado intermenstrual\n* Actividad sexual';

  @override
  String get settingsAllRequestableGranted =>
      'Todos los permisos solicitables concedidos';

  @override
  String get settingsManualPermissionsTitle => 'Permisos manuales requeridos';

  @override
  String get settingsManualPermissionsBody =>
      'Algunos permisos de Health Connect no pueden concederse desde el diálogo normal. Abre Health Connect y actívalos para OpenVitals.';

  @override
  String get settingsOpenHealthPermissions =>
      'Abrir permisos de Health Connect';

  @override
  String get settingsDisplayGroupTitle => 'Visualización';

  @override
  String get settingsDisplayGroupBody => 'Idioma, unidades y tema';

  @override
  String get settingsActivitiesGroupTitle => 'Actividades';

  @override
  String get settingsActivitiesGroupBody =>
      'Fechas móviles, actividad favorita, grabación y mapas sin conexión';

  @override
  String get settingsSensorsGroupTitle => 'Sensores y dispositivos';

  @override
  String get settingsSensorsGroupBody =>
      'Sensores de frecuencia cardiaca, cadencia y potencia';

  @override
  String get settingsSensorsEmptyTitle => 'Sin sensores todavía';

  @override
  String get settingsSensorsEmptyBody =>
      'Añade una banda de frecuencia cardiaca, sensor de cadencia, medidor de potencia o footpod.';

  @override
  String get settingsSensorsAddDevice => 'Añadir sensor';

  @override
  String get settingsSensorsEditDevice => 'Editar sensor';

  @override
  String get settingsSensorsRemoveDevice => 'Eliminar sensor';

  @override
  String get settingsSensorsDeviceName => 'Nombre del dispositivo';

  @override
  String get settingsSensorsEnabled => 'Activado';

  @override
  String settingsSensorsBatteryPercent(int arg0) {
    return 'Batería $arg0%';
  }

  @override
  String get settingsSensorsBatteryUnknown => 'Batería pendiente';

  @override
  String get settingsSensorsScanning => 'Buscando sensores…';

  @override
  String get settingsSensorsScanStopped => 'Búsqueda detenida';

  @override
  String get settingsSensorsScanEmpty =>
      'No se encontraron sensores. Asegúrate de que el sensor esté activo y cerca.';

  @override
  String get settingsSensorsShowAllDevices => 'Mostrar todos los dispositivos';

  @override
  String get settingsSensorsOpenBluetooth => 'Abrir ajustes de Bluetooth';

  @override
  String get settingsSensorsDiscovering => 'Detectando capacidades del sensor…';

  @override
  String get settingsSensorsCapabilitiesTitle => 'Capacidades';

  @override
  String get settingsSensorsCapabilityHeartRate => 'Frecuencia cardiaca';

  @override
  String get settingsSensorsCapabilityCyclingCadence => 'Cadencia';

  @override
  String get settingsSensorsCapabilityCyclingPower =>
      'Fuerza de tus piernas al pedalear';

  @override
  String get settingsSensorsCapabilityCyclingSpeed => 'Velocidad';

  @override
  String get settingsSensorsCapabilityRunningSpeedCadence =>
      'Velocidad/cadencia de carrera';

  @override
  String settingsSensorsCapabilityConflict(String arg0, String arg1) {
    return '$arg0 ya está asignado a $arg1';
  }

  @override
  String get settingsSensorsWheelCircumference =>
      'Circunferencia de rueda (mm)';

  @override
  String get activityRecordingSensorsTitle => 'Sensores';

  @override
  String get activityRecordingSensorsAddInSettings =>
      'Añadir sensores en Ajustes';

  @override
  String get activityRecordingSensorsNotConfigured => 'No configurado';

  @override
  String get activityRecordingSensorsConnected => 'Conectado';

  @override
  String get activityRecordingSensorsConnecting => 'Conectando';

  @override
  String get activityRecordingSensorsReconnecting => 'Reconectando';

  @override
  String get activityRecordingSensorsDisabled => 'Desactivado';

  @override
  String get activityRecordingSensorsWaitingForData =>
      'Esperando datos del sensor…';

  @override
  String get activityRecordingSensorsWaitingShort => '—';

  @override
  String get activityRecordingSensorsNoSignalShort => 'Sin señal';

  @override
  String get activityRecordingSensorsGarminBroadcastHint =>
      'Conectado, pero el reloj no transmite la frecuencia cardiaca. En Garmin: Ajustes → Sensores del reloj → Frecuencia cardiaca de muñeca → Transmitir frecuencia cardiaca, luego iniciarlo en el reloj. Desconecta Gadgetbridge primero o usa un chest strap BLE.';

  @override
  String get activityRecordingSensorsRecordedTitle =>
      'Datos del sensor registrados';

  @override
  String get activityRecordingLiveHeartRate => 'Frecuencia cardiaca';

  @override
  String get activityRecordingLiveCadence => 'Cadencia';

  @override
  String get activityRecordingLivePower => 'Potencia';

  @override
  String get activityRecordingLiveSpeed => 'Velocidad';

  @override
  String activityRecordingNotificationHeartRate(String arg0) {
    return 'FC $arg0';
  }

  @override
  String get settingsNutritionGroupTitle => 'Nutrición';

  @override
  String get settingsNutritionGroupBody =>
      'Datos de calorías y personalización de cafeína';

  @override
  String get settingsCaloriesGroupTitle => 'Calorías';

  @override
  String get settingsCaloriesGroupBody => 'Datos de calorías totales';

  @override
  String get settingsCaffeineGroupTitle => 'Cafeína';

  @override
  String get settingsCaffeineGroupBody =>
      'Semivida, hora de dormir, umbral de sueño y personalización.';

  @override
  String get settingsRecoveryGroupTitle => 'Recuperación';

  @override
  String get settingsRecoveryGroupBody =>
      'Rango de sueño y calibración de energía corporal';

  @override
  String get settingsSleepGroupTitle => 'Sueño';

  @override
  String get settingsSleepGroupBody => 'Rango de sueño';

  @override
  String get settingsCycleGroupTitle => 'Ciclo menstrual';

  @override
  String get settingsCycleGroupBody =>
      'Datos del ciclo y permisos de Health Connect';

  @override
  String get settingsDataImportGroupTitle => 'Importadores de datos';

  @override
  String get settingsDataImportGroupBody =>
      'Importar exportaciones de Apple Health, archivos de ruta y archivos FIT';

  @override
  String get settingsPermissionsGroupTitle => 'Permisos';

  @override
  String get settingsPermissionsGroupBody =>
      'Acceso a datos de salud y pasos manuales de permisos';

  @override
  String get settingsHealthConnectGroupTitle => 'Health Connect';

  @override
  String get settingsHealthConnectGroupBody =>
      'Sincronización, permisos, acceso y bloqueo de app';

  @override
  String get settingsDebugDiagnosticsGroupTitle => 'Diagnóstico de depuración';

  @override
  String get settingsDebugDiagnosticsGroupBody =>
      'Guardar registros de diagnóstico saneados para solucionar problemas';

  @override
  String get settingsHealthConnectSyncTitle => 'Sincronizar con Health Connect';

  @override
  String get settingsHealthConnectSyncBody =>
      'Cuando está activado, OpenVitals lee y escribe datos de salud según tus permisos. Cuando está desactivado, la sincronización se pausa sin revocar el acceso.';

  @override
  String get settingsHealthConnectManageAccess => 'Gestionar acceso';

  @override
  String get settingsHealthConnectManageAccessBody =>
      'Abre Health Connect para revisar o cambiar qué datos puede usar OpenVitals.';

  @override
  String get healthConnectAccessInsufficientTitle =>
      'Elige datos para compartir';

  @override
  String get healthConnectAccessInsufficientBody =>
      'OpenVitals necesita acceso a Health Connect para mostrar esta información. Configura los datos que quieres compartir.';

  @override
  String get healthConnectAccessDoubleCancelTitle =>
      'Los permisos requieren atención';

  @override
  String get healthConnectAccessDoubleCancelBody =>
      'No se concedieron los permisos de Health Connect. Abre la configuración de Health Connect para elegir qué datos compartir con OpenVitals.';

  @override
  String get healthConnectSyncPaused =>
      'Sincronización con Health Connect pausada';

  @override
  String get healthConnectSyncInProgress => 'Sincronizando con Health Connect…';

  @override
  String get healthConnectDataSourceManage => 'Gestionar fuentes de datos';

  @override
  String get healthConnectDataSourceManageBody =>
      'Consulta qué apps escriben datos en Health Connect y gestiona su acceso.';

  @override
  String get dashboardHealthConnectPromoTitle => 'Configura tus datos de salud';

  @override
  String get dashboardHealthConnectPromoBody =>
      'Obtén una vista unificada de tu actividad, sueño y frecuencia cardíaca desde las apps y dispositivos que ya usas.';

  @override
  String get dashboardHealthConnectPromoAction => 'Empezar';

  @override
  String get dashboardHealthConnectSyncPausedBody =>
      'Vuelve a activar la sincronización en Ajustes para actualizar tu panel.';

  @override
  String get dashboardHealthConnectInstallAction => 'Instalar Health Connect';

  @override
  String get healthConnectMatchmakingTitle => 'Conecta tus apps';

  @override
  String get healthConnectMatchmakingBody =>
      'Encuentra apps y dispositivos que puedan compartir datos que OpenVitals puede leer.';

  @override
  String get healthConnectMatchmakingAction => 'Buscar fuentes de datos';

  @override
  String get healthConnectPromoteActivityTitle =>
      'Desbloquea información de actividad';

  @override
  String get healthConnectPromoteActivityBody =>
      'Permite datos de actividad para ver pasos, distancia, entrenamientos y tendencias en OpenVitals.';

  @override
  String get healthConnectPromoteActivitiesTitle => 'Ver tus entrenamientos';

  @override
  String get healthConnectPromoteActivitiesBody =>
      'Permite acceso a sesiones de ejercicio para explorar actividades sincronizadas con Health Connect.';

  @override
  String get healthConnectPromoteCaloriesTitle => 'Seguir calorías quemadas';

  @override
  String get healthConnectPromoteCaloriesBody =>
      'Permite datos de calorías para comparar quema activa y total a lo largo del tiempo.';

  @override
  String get healthConnectPromoteSleepTitle => 'Ver tu sueño';

  @override
  String get healthConnectPromoteSleepBody =>
      'Permite datos de sueño para ver fases, duración y tendencias de puntuación de sueño.';

  @override
  String get healthConnectPromoteHeartTitle => 'Monitorizar salud cardíaca';

  @override
  String get healthConnectPromoteHeartBody =>
      'Permite datos de frecuencia cardíaca y VFC para seguir la frecuencia en reposo y la variabilidad.';

  @override
  String get healthConnectPromoteVitalsTitle => 'Desbloquear signos vitales';

  @override
  String get healthConnectPromoteVitalsBody =>
      'Permite datos vitales para ver presión arterial, SpO2 y mediciones relacionadas.';

  @override
  String get healthConnectPromoteBodyTitle => 'Seguir métricas corporales';

  @override
  String get healthConnectPromoteBodyBody =>
      'Permite datos de composición corporal para seguir peso, IMC y tendencias relacionadas.';

  @override
  String get healthConnectPromoteHydrationTitle => 'Seguir hidratación';

  @override
  String get healthConnectPromoteHydrationBody =>
      'Permite datos de hidratación para ver ingesta diaria e historial.';

  @override
  String get healthConnectPromoteNutritionTitle => 'Ver nutrición';

  @override
  String get healthConnectPromoteNutritionBody =>
      'Permite datos de nutrición para revisar calorías y macros de tus fuentes.';

  @override
  String get healthConnectPromoteMindfulnessTitle => 'Seguir mindfulness';

  @override
  String get healthConnectPromoteMindfulnessBody =>
      'Permite datos de sesiones de mindfulness para ver tu práctica a lo largo del tiempo.';

  @override
  String get healthConnectPromoteCycleTitle => 'Seguir datos del ciclo';

  @override
  String get healthConnectPromoteCycleBody =>
      'Permite datos del ciclo menstrual para ver flujo, síntomas y registros relacionados.';

  @override
  String get healthConnectPromoteReadinessTitle =>
      'Mejorar información de preparación';

  @override
  String get healthConnectPromoteReadinessBody =>
      'Permite datos adicionales de Health Connect para refinar las puntuaciones diarias de preparación.';

  @override
  String get healthConnectNewPermissionsTitle => 'Nuevos datos disponibles';

  @override
  String get healthConnectNewPermissionsBody =>
      'OpenVitals ahora puede leer tipos adicionales de datos de salud. Concede acceso para usar las nuevas funciones.';

  @override
  String get healthConnectNewPermissionsAction => 'Revisar permisos';

  @override
  String get privacyReconsentTitle => 'Política de privacidad actualizada';

  @override
  String get privacyReconsentBody =>
      'Nuestra política de privacidad ha cambiado. Revísala y acéptala para continuar sincronizando con Health Connect.';

  @override
  String get privacyReconsentAction => 'Ver política';

  @override
  String get dashboardSummaryToday => 'Hoy';

  @override
  String get settingsDebugLogsTitle => 'Registros de diagnóstico saneados';

  @override
  String get settingsDebugLogsBody =>
      'Comparte o guarda los registros de diagnóstico de OpenVitals como archivo de texto. La exportación elimina u oculta identificadores, ubicaciones, fechas, URI, datos brutos de sensores y registros de otras apps antes de escribir.';

  @override
  String get settingsDebugLogsShare => 'Compartir registros';

  @override
  String get settingsDebugLogsShareChooserTitle =>
      'Compartir registros de diagnóstico';

  @override
  String get settingsDebugLogsShareFailed =>
      'Error al compartir los registros de diagnóstico';

  @override
  String get settingsDebugLogsSave => 'Guardar registros';

  @override
  String get settingsDebugLogsSaved => 'Registros de depuración guardados';

  @override
  String get settingsDebugLogsSaveFailed =>
      'No se pudieron guardar los registros de diagnóstico';

  @override
  String get settingsPrivacyPolicyLink => 'Ver política de privacidad';

  @override
  String get settingsAppLockTitle => 'Bloqueo de la app';

  @override
  String get settingsAppLockBody =>
      'Requerir desbloqueo del dispositivo para abrir OpenVitals.';

  @override
  String get onboardingCoreRequired =>
      'Se necesita acceso a actividad, sueño y frecuencia cardíaca para empezar. Puedes añadir más tipos de datos más tarde desde Ajustes.';

  @override
  String get settingsLanguageTitle => 'Idioma';

  @override
  String get settingsLanguageBody =>
      'Elige el idioma de la app o usa el del sistema.';

  @override
  String get settingsLanguageSystem => 'Sistema';

  @override
  String get settingsLanguageEnglish => 'Inglés';

  @override
  String get settingsLanguageSpanish => 'Español';

  @override
  String get settingsLanguageGerman => 'Alemán';

  @override
  String get settingsLanguageItalian => 'Italiano';

  @override
  String get settingsLanguageEstonian => 'Estonio';

  @override
  String get settingsUnitsTitle => 'Unidades';

  @override
  String get settingsUnitsBody =>
      'Elige cómo se muestran distancias, pesos, hidratación y temperatura.';

  @override
  String get settingsUnitMetric => 'Métrico';

  @override
  String get settingsUnitImperial => 'Imperial';

  @override
  String get settingsThemeTitle => 'Tema';

  @override
  String get settingsThemeBody =>
      'Elige el aspecto de la app de forma independiente del modo oscuro de Android.';

  @override
  String get settingsThemeSystem => 'Sistema';

  @override
  String get settingsThemeLight => 'Claro';

  @override
  String get settingsThemeDark => 'Oscuro';

  @override
  String get settingsThemeAmoled => 'AMOLED';

  @override
  String get settingsDynamicColorTitle => 'Color dinámico (Material You)';

  @override
  String get settingsDynamicColorBody =>
      'Tiñe OpenVitals con tu fondo de pantalla de Android. Desactivado usa la paleta de marca azul y verde azulado de OpenVitals.';

  @override
  String get settingsActivityWeekTitle => 'Fechas móviles';

  @override
  String get settingsActivityWeekBody =>
      'Usa ventanas móviles de 7, 30 y 365 días en lugar de semana, mes y año de calendario.';

  @override
  String get settingsActivityWeekMondayToSunday => 'Calendario';

  @override
  String get settingsActivityWeekLast7Days => 'Móviles';

  @override
  String get settingsFavoriteActivityTitle => 'Actividad favorita';

  @override
  String get settingsFavoriteActivityBody =>
      'Usa por defecto la última actividad grabada o elige un tipo de actividad para preseleccionarlo siempre.';

  @override
  String get settingsFavoriteActivityLatest => 'Usar última';

  @override
  String get settingsActivityRecordingTitle => 'Grabación de actividad';

  @override
  String get settingsActivityRecordingBody =>
      'Ajusta la grabación GPS en vivo sin cambiar el flujo de detalles de la actividad guardada.';

  @override
  String get settingsActivityRecordingKeepScreenOnTitle =>
      'Pantalla siempre encendida';

  @override
  String get settingsActivityRecordingKeepScreenOnBody =>
      'Mantiene la pantalla activa mientras una grabación de actividad está en curso.';

  @override
  String get settingsActivityRecordingAutoIdleTitle => 'Pausa automática';

  @override
  String get settingsActivityRecordingAutoIdleBody =>
      'Detiene el tiempo en movimiento cuando paras más que el tiempo elegido.';

  @override
  String get settingsActivityRecordingIdleTimeoutTitle => 'Tiempo de pausa';

  @override
  String get settingsActivityRecordingAccuracyTitle =>
      'Precisión GPS requerida';

  @override
  String get settingsActivityRecordingRouteGapTitle =>
      'Nuevo segmento tras separación';

  @override
  String get settingsActivityRecordingTimeIntervalTitle =>
      'Intervalo de tiempo de grabación';

  @override
  String get settingsActivityRecordingDistanceIntervalTitle =>
      'Intervalo de distancia de grabación';

  @override
  String get settingsActivityRecordingBarometerTitle => 'Ascenso con barómetro';

  @override
  String get settingsActivityRecordingBarometerBody =>
      'Usa cambios de presión para el ascenso cuando el dispositivo tenga barómetro.';

  @override
  String get settingsActivityRecordingRestBellTitle => 'Campana de descanso';

  @override
  String get settingsActivityRecordingRestBellBody =>
      'Reproduce una campana suave cuando termine la cuenta atrás de descanso entre series.';

  @override
  String get settingsActivityRecordingVoiceTitle => 'Anuncios de voz';

  @override
  String get settingsActivityRecordingVoiceBody =>
      'Lee el progreso, pausa/reanudación y vueltas durante la grabación.';

  @override
  String get settingsActivityRecordingVoiceTimeTitle => 'Anunciar por tiempo';

  @override
  String get settingsActivityRecordingVoiceDistanceTitle =>
      'Anunciar por distancia';

  @override
  String get settingsActivityRecordingVoiceIdleTitle => 'Anuncios de pausa';

  @override
  String get settingsActivityRecordingVoiceIdleBody =>
      'Avisar cuando empieza la pausa automática y cuando se reanuda la grabación.';

  @override
  String get settingsActivityRecordingVoiceLapTitle => 'Anuncios de vuelta';

  @override
  String get settingsActivityRecordingVoiceLapBody =>
      'Lee un resumen cuando marques una vuelta.';

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
  String get settingsActivityRecordingOff => 'Desactivado';

  @override
  String get settingsCalorieDataTitle => 'Datos de calorías totales';

  @override
  String get settingsCalorieDataBody =>
      'Muestra por defecto solo las calorías totales de Health Connect. Activa los cálculos de OpenVitals para completar totales faltantes con calorías activas y BMR.';

  @override
  String get settingsCaffeineTitle => 'Modelo de cafeína';

  @override
  String get settingsCaffeineBody =>
      'Estos valores personalizan el nivel de cafeína, la previsión de hora de dormir y los avisos de sueño seguro. Las entradas permanecen en Health Connect.';

  @override
  String get settingsBodyProfileTitle => 'Perfil corporal';

  @override
  String get settingsBodyProfileBody =>
      'La edad, el peso y la frecuencia cardíaca personalizan las estimaciones de energía corporal y cafeína. Todos los campos son opcionales.';

  @override
  String get settingsBodyProfileWeight => 'Peso';

  @override
  String get settingsSleepRangeTitle => 'Rango de sueño';

  @override
  String get settingsSleepRangeBody =>
      'Elige a qué día se asignan las sesiones de sueño.';

  @override
  String get settingsSleepRangeRolling24h => '24 h móviles';

  @override
  String get settingsSleepRangeNoon => 'Mediodía';

  @override
  String get settingsSleepRangeEvening => '18:00';

  @override
  String get settingsCyclePermissionsTitle => 'Permisos del ciclo';

  @override
  String settingsCyclePermissionsGranted(int arg0, int arg1) {
    return '$arg0/$arg1 permisos de ciclo concedidos.';
  }

  @override
  String get settingsAppleHealthImportTitle => 'Importador de Apple Health';

  @override
  String get settingsAppleHealthImportBody =>
      'Importa registros compatibles desde export.xml o export.zip de Apple Health a Health Connect.';

  @override
  String settingsAppleHealthImportPermissions(int arg0, int arg1) {
    return '$arg0/$arg1 permisos de importación concedidos.';
  }

  @override
  String get settingsAppleHealthImportGrant =>
      'Conceder permisos de importación';

  @override
  String get settingsAppleHealthImportAction =>
      'Importar exportación de Apple Health';

  @override
  String get settingsAppleHealthImportAnalyzeAction =>
      'Analizar exportación de Apple Health';

  @override
  String get settingsAppleHealthImportChooseAnotherAction =>
      'Elegir otra exportación de Apple Health';

  @override
  String get settingsAppleHealthImportSelectedAction =>
      'Importar categorías seleccionadas';

  @override
  String get settingsAppleHealthImportAnalyzing => 'Analizando...';

  @override
  String get settingsAppleHealthImporting => 'Importando...';

  @override
  String get settingsAppleHealthImportProgressQueued => 'En cola';

  @override
  String get settingsAppleHealthImportProgressParsing =>
      'Analizando exportación';

  @override
  String get settingsAppleHealthImportProgressConverting =>
      'Convirtiendo registros';

  @override
  String get settingsAppleHealthImportProgressCheckingDuplicates =>
      'Comprobando duplicados';

  @override
  String get settingsAppleHealthImportProgressWriting =>
      'Escribiendo registros';

  @override
  String get settingsAppleHealthImportProgressFinishing =>
      'Finalizando importación';

  @override
  String get settingsAppleHealthImportProgressBuildingReport =>
      'Creando informe';

  @override
  String get settingsAppleHealthImportProgressComplete => 'Completo';

  @override
  String settingsAppleHealthImportProgress(String arg0, int arg1, int arg2) {
    return '$arg0. $arg1 elementos analizados, $arg2 registros importados.';
  }

  @override
  String settingsAppleHealthImportProgressWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Seleccionados $arg2/$arg3 registros, importados $arg4.';
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
      'La importación continúa en segundo plano si sales de la app.';

  @override
  String get settingsAppleHealthImportNotificationChannel =>
      'Importaciones de Apple Health';

  @override
  String get settingsAppleHealthImportNotificationTitle =>
      'Importando exportación de Apple Health';

  @override
  String settingsAppleHealthImportNotificationText(
    String arg0,
    int arg1,
    int arg2,
  ) {
    return '$arg0. $arg1 analizados, $arg2 importados.';
  }

  @override
  String settingsAppleHealthImportNotificationTextWithPercent(
    int arg0,
    String arg1,
    int arg2,
    int arg3,
    int arg4,
  ) {
    return '$arg0%. $arg1. Seleccionados $arg2/$arg3, importados $arg4.';
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
    return 'Importados $arg0. Duplicados $arg1. No seleccionados $arg2. No compatibles $arg3. Omitidos $arg4. Fallidos $arg5.';
  }

  @override
  String get settingsAppleHealthImportRoutesIncomplete =>
      'Se han importado los registros de salud, pero algunas rutas no estaban disponibles porque el código postal no era correcto. El informe de importación enumera las actividades afectadas para que se puedan recuperar manualmente.';

  @override
  String settingsAppleHealthImportAnalysisResult(
    int arg0,
    int arg1,
    int arg2,
    int arg3,
  ) {
    return '$arg0 elementos analizados. $arg1 registros compatibles encontrados. No compatibles $arg2. Fallidos $arg3.';
  }

  @override
  String get settingsAppleHealthImportChooseCategories =>
      'Elige qué escribir en Health Connect.';

  @override
  String settingsAppleHealthImportCategoryCount(int arg0) {
    return '$arg0 registros';
  }

  @override
  String settingsAppleHealthImportCategoryCountRoutes(int arg0, int arg1) {
    return '$arg0 registros, $arg1 con rutas';
  }

  @override
  String get settingsAppleHealthImportCategoryWorkouts =>
      'Entrenamientos y rutas';

  @override
  String get settingsAppleHealthImportCategoryWorkoutsDesc =>
      'Sesiones de ejercicio y geometría de rutas adjunta.';

  @override
  String get settingsAppleHealthImportCategoryActivity =>
      'Métricas de actividad';

  @override
  String get settingsAppleHealthImportCategoryActivityDesc =>
      'Pasos, distancia, calorías, pisos, elevación, impulsos de silla de ruedas y velocidad.';

  @override
  String get settingsAppleHealthImportCategoryHeart => 'Corazón';

  @override
  String get settingsAppleHealthImportCategoryHeartDesc =>
      'Frecuencia cardiaca y frecuencia cardiaca en reposo.';

  @override
  String get settingsAppleHealthImportCategorySleep => 'Sueño';

  @override
  String get settingsAppleHealthImportCategorySleepDesc =>
      'Sesiones y fases de sueño.';

  @override
  String get settingsAppleHealthImportCategoryBody => 'Medidas corporales';

  @override
  String get settingsAppleHealthImportCategoryBodyDesc =>
      'Peso, altura, grasa corporal, masa magra, BMR, masa ósea y agua corporal.';

  @override
  String get settingsAppleHealthImportCategoryVitals => 'Signos vitales';

  @override
  String get settingsAppleHealthImportCategoryVitalsDesc =>
      'Presión arterial, oxígeno, frecuencia respiratoria, temperatura corporal, glucosa y VO2 máx.';

  @override
  String get settingsAppleHealthImportCategoryNutrition => 'Nutrición';

  @override
  String get settingsAppleHealthImportCategoryNutritionDesc =>
      'Energía, macros, cafeína, minerales y vitaminas.';

  @override
  String get settingsAppleHealthImportCategoryHydration => 'Hidratación';

  @override
  String get settingsAppleHealthImportCategoryHydrationDesc =>
      'Registros de agua.';

  @override
  String get settingsAppleHealthImportCategoryMindfulness => 'Mindfulness';

  @override
  String get settingsAppleHealthImportCategoryMindfulnessDesc =>
      'Sesiones de mindfulness cuando Health Connect las admite.';

  @override
  String get settingsAppleHealthImportCategoryCycle => 'Seguimiento del ciclo';

  @override
  String get settingsAppleHealthImportCategoryCycleDesc =>
      'Menstruación, ovulación, moco cervical, sangrado, temperatura basal y actividad sexual.';

  @override
  String get settingsAppleHealthImportCopyReport => 'Copiar informe';

  @override
  String get settingsAppleHealthImportCopyError => 'Copiar error';

  @override
  String get settingsAppleHealthImportSaveReport => 'Guardar informe';

  @override
  String get settingsAppleHealthImportReportCopied =>
      'Informe de importación copiado.';

  @override
  String get settingsAppleHealthImportErrorCopied =>
      'Error de importación copiado.';

  @override
  String get settingsAppleHealthImportReportSaved =>
      'Informe de importación guardado.';

  @override
  String get settingsAppleHealthImportReportSaveFailed =>
      'No se pudo guardar el informe de importación.';

  @override
  String settingsAppleHealthImportError(String arg0) {
    return 'Error de importación: $arg0';
  }

  @override
  String get settingsAppleHealthImportPermissionDenied =>
      'Se perdió el acceso al archivo seleccionado, por lo que la importación no pudo continuar. Selecciona de nuevo la misma exportación de Apple Health para continuar justo donde lo dejaste.';

  @override
  String get settingsRouteImportTitle => 'Importador GPX/KML/KMZ';

  @override
  String get settingsRouteImportBody =>
      'Importa archivos de ruta GPX, KML o KMZ. Revisa un archivo antes de guardarlo o importa varios archivos directamente en Health Connect.';

  @override
  String settingsRouteImportPermissions(int arg0, int arg1) {
    return '$arg0/$arg1 permisos de importación de rutas concedidos.';
  }

  @override
  String get settingsRouteImportGrant =>
      'Conceder permisos de importación de rutas';

  @override
  String get settingsRouteImportAction => 'Importar archivo GPX/KML/KMZ';

  @override
  String get settingsRouteImportBulkAction =>
      'Importar archivos GPX/KML/KMZ en lote';

  @override
  String get settingsRouteImporting => 'Importando rutas...';

  @override
  String settingsRouteImportProgress(int arg0, int arg1, int arg2, int arg3) {
    return 'Archivo $arg0/$arg1. Importados $arg2, fallidos $arg3.';
  }

  @override
  String settingsRouteImportResult(int arg0, int arg1, int arg2) {
    return 'Importados $arg0. Fallidos $arg1. Seleccionados $arg2.';
  }

  @override
  String settingsRouteImportError(String arg0) {
    return 'Aviso de importación de rutas: $arg0';
  }

  @override
  String get settingsFitImportTitle => 'Importador FIT';

  @override
  String get settingsFitImportBody =>
      'Importa archivos FIT de actividad, curso o entrenamiento, revisa los detalles detectados y elige si guardarlos en Health Connect.';

  @override
  String get settingsFitImportAction => 'Importar archivo FIT';

  @override
  String get settingsOfflineMapsTitle => 'Mapas sin conexión';

  @override
  String get settingsOfflineMapsBody =>
      'Importa paquetes PMTiles o Mapsforge .map/.maps para mapas de actividad completamente sin conexión. Se admiten mapas base PMTiles compatibles con Protomaps y mapas Mapsforge.';

  @override
  String get settingsOfflineMapsEmpty =>
      'Aún no hay mapas sin conexión importados.';

  @override
  String get settingsOfflineMapsFormatPmtiles => 'PMTiles';

  @override
  String get settingsOfflineMapsFormatMapsforge => 'Mapsforge';

  @override
  String get settingsOfflineMapsRenderFormatTitle => 'Formato de renderizado';

  @override
  String settingsOfflineMapsRenderFormatOption(String arg0, int arg1) {
    return '$arg0 ($arg1)';
  }

  @override
  String get settingsOfflineMapsRenderFormatBody =>
      'OpenVitals renderiza juntos todos los paquetes importados del formato seleccionado.';

  @override
  String settingsOfflineMapsPackDetail(String arg0, String arg1, String arg2) {
    return '$arg0 • $arg1 • $arg2';
  }

  @override
  String get settingsOfflineMapsImportAction => 'Importar mapa sin conexión';

  @override
  String get settingsOfflineMapsImporting => 'Importando...';

  @override
  String get settingsOfflineMapsImportProgressQueued => 'En cola';

  @override
  String get settingsOfflineMapsImportProgressCopying => 'Copiando mapa';

  @override
  String get settingsOfflineMapsImportProgressComplete => 'Completo';

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
      'La importación continúa en segundo plano si sales de la app.';

  @override
  String settingsOfflineMapsImportResult(String arg0, String arg1) {
    return 'Importado $arg0 ($arg1).';
  }

  @override
  String settingsOfflineMapsImportError(String arg0) {
    return 'Error al importar mapa: $arg0';
  }

  @override
  String get settingsOfflineMapsImportNotificationChannel =>
      'Importaciones de mapas sin conexión';

  @override
  String get settingsOfflineMapsImportNotificationTitle =>
      'Importando mapa sin conexión';

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
      '¿Quieres aprender a añadir mapas sin conexión? Ve a:';

  @override
  String get settingsOfflineMapsHelpLink => 'Abrir guía de mapas sin conexión';

  @override
  String get sectionSupport => 'Apoyo';

  @override
  String get settingsSupportTitle => 'Apoya OpenVitals';

  @override
  String get settingsSupportBody =>
      'Informa errores, únete a debates de soporte de la comunidad o ayuda a financiar el desarrollo continuo.';

  @override
  String get settingsSupportIssuesAction => 'Informar de un problema';

  @override
  String get settingsSupportDiscussionAction => 'Unirse a debates en Zulip';

  @override
  String get settingsSupportAction => 'Abrir Liberapay';

  @override
  String get crashReportEmailChooserTitle =>
      'Enviar informe de OpenVitals por email';

  @override
  String get crashReportFallbackTitle => 'No se encontró app de email';

  @override
  String crashReportFallbackBody(String arg0) {
    return 'Copia el informe o guárdalo como archivo de texto y envíalo a $arg0 más tarde.';
  }

  @override
  String get crashReportFallbackCopy => 'Copiar informe';

  @override
  String get crashReportFallbackSave => 'Guardar archivo de texto';

  @override
  String get crashReportFallbackCopied => 'Informe copiado.';

  @override
  String get crashReportFallbackSaved => 'Informe guardado.';

  @override
  String get crashReportFallbackSaveFailed => 'No se pudo guardar el informe.';

  @override
  String get crashReportFallbackSaveUnavailable =>
      'No se encontró app para guardar archivos. Informe copiado.';

  @override
  String get crashReportClipboardLabel => 'Informe de OpenVitals';

  @override
  String get settingsPrivacyNoAccount => 'No hace falta cuenta';

  @override
  String get settingsPrivacyNoCloud =>
      'Sin sincronización en la nube de datos de salud';

  @override
  String get settingsPrivacyNoAnalytics => 'Sin SDK de analíticas';

  @override
  String get settingsPrivacyNoAds => 'Sin anuncios ni seguimiento de terceros';

  @override
  String get settingsPrivacyOnDevice =>
      'Los datos permanecen en tu dispositivo';

  @override
  String get settingsPrivacyReadOnly =>
      'Solo lectura salvo las entradas que registres explícitamente';

  @override
  String settingsAppVersion(String arg0, int arg1) {
    return 'Versión $arg0 ($arg1)';
  }

  @override
  String get detailMetrics => 'Métricas';

  @override
  String get detailSessionDetails => 'Detalles de la sesión';

  @override
  String get detailDuration => 'Duración';

  @override
  String get detailMovingTime => 'Tiempo en movimiento';

  @override
  String get detailType => 'Tipo';

  @override
  String get detailStarted => 'Inicio';

  @override
  String get detailEnded => 'Fin';

  @override
  String get detailStartZone => 'Zona inicial';

  @override
  String get detailEndZone => 'Zona final';

  @override
  String get detailRecording => 'Registro';

  @override
  String get detailSourcePackage => 'Paquete de origen';

  @override
  String get detailDeviceType => 'Tipo de dispositivo';

  @override
  String get detailDeviceMaker => 'Fabricante';

  @override
  String get detailDeviceModel => 'Modelo';

  @override
  String get detailLastModified => 'Última modificación';

  @override
  String get detailRecordId => 'Id. de registro';

  @override
  String get detailClientRecordId => 'Id. de registro del cliente';

  @override
  String get detailClientVersion => 'Versión del cliente';

  @override
  String get detailPlannedSessionId => 'Id. de sesión planificada';

  @override
  String get detailNotes => 'Notas';

  @override
  String get detailTitle => 'Título';

  @override
  String get detailTime => 'Hora';

  @override
  String get detailRepetitions => 'Repeticiones';

  @override
  String get detailSet => 'Serie';

  @override
  String get detailLength => 'Longitud';

  @override
  String get detailSegments => 'Segmentos';

  @override
  String get detailLaps => 'Vueltas';

  @override
  String detailLap(int arg0) {
    return 'Vuelta $arg0';
  }

  @override
  String get detailRoute => 'Ruta';

  @override
  String get detailStatus => 'Estado';

  @override
  String get detailStatusAvailable => 'Disponible';

  @override
  String get detailPoints => 'Puntos';

  @override
  String get detailStartPoint => 'Punto inicial';

  @override
  String get detailEndPoint => 'Punto final';

  @override
  String detailAltitude(String arg0) {
    return 'Altitud $arg0';
  }

  @override
  String detailHorizontalAccuracy(String arg0) {
    return 'Precisión horizontal $arg0';
  }

  @override
  String detailVerticalAccuracy(String arg0) {
    return 'Precisión vertical $arg0';
  }

  @override
  String get detailStageEvents => 'Eventos de fases';

  @override
  String get detailStages => 'Fases';

  @override
  String get detailSleepSession => 'Sesión de sueño';

  @override
  String get recordingActivelyRecorded => 'Registrado activamente';

  @override
  String get recordingAutomaticallyRecorded => 'Registrado automáticamente';

  @override
  String get recordingManualEntry => 'Entrada manual';

  @override
  String get recordingUnknown => 'Desconocido';

  @override
  String get deviceWatch => 'Reloj';

  @override
  String get devicePhone => 'Teléfono';

  @override
  String get deviceScale => 'Báscula';

  @override
  String get deviceRing => 'Anillo';

  @override
  String get deviceHeadMounted => 'Montado en la cabeza';

  @override
  String get deviceFitnessBand => 'Pulsera de actividad';

  @override
  String get deviceChestStrap => 'Banda pectoral';

  @override
  String get deviceSmartDisplay => 'Pantalla inteligente';

  @override
  String get sleepStageAwake => 'Despierto';

  @override
  String get sleepStageSleeping => 'Durmiendo';

  @override
  String get sleepStageOutOfBed => 'Fuera de la cama';

  @override
  String get sleepStageLight => 'Ligero';

  @override
  String get sleepStageDeep => 'Profundo';

  @override
  String get sleepStageRem => 'REM';

  @override
  String get sleepStageAwakeInBed => 'Despierto en cama';

  @override
  String get sleepStageUnknown => 'Desconocido';

  @override
  String get sleepStagesShareTitle => 'Porcentaje del tiempo en cama';

  @override
  String get cyclePermissionsMissingTitle => 'Faltan permisos del ciclo';

  @override
  String get cyclePermissionsMissingBody =>
      'Concede permisos de seguimiento del ciclo para mostrar días de periodo, pruebas de ovulación, moco cervical y temperatura basal.';

  @override
  String get cycleObservationMenstruationPeriod => 'Periodo menstrual';

  @override
  String get cycleObservationMenstruationFlow => 'Flujo menstrual';

  @override
  String get cycleObservationOvulationTest => 'Prueba de ovulación';

  @override
  String get cycleObservationCervicalMucus => 'Moco cervical';

  @override
  String get cycleObservationBasalBodyTemperature =>
      'Temperatura corporal basal';

  @override
  String get cycleObservationIntermenstrualBleeding =>
      'Sangrado intermenstrual';

  @override
  String get cycleObservationSexualActivity => 'Actividad sexual';

  @override
  String get cycleProtectionProtected => 'Con protección';

  @override
  String get cycleProtectionUnprotected => 'Sin protección';

  @override
  String get cycleProtectionUnknown => 'Protección desconocida';

  @override
  String cycleBasalTemperatureValue(String arg1) {
    return '%1\$.1f C · $arg1';
  }

  @override
  String cycleDaysValue(int arg0, String arg1) {
    return '$arg0 $arg1';
  }

  @override
  String get cycleDaySingular => 'día';

  @override
  String get cycleDayPlural => 'días';

  @override
  String get cycleFlowLight => 'Ligero';

  @override
  String get cycleFlowMedium => 'Medio';

  @override
  String get cycleFlowHeavy => 'Abundante';

  @override
  String get cycleOvulationPositive => 'Positivo';

  @override
  String get cycleOvulationHigh => 'Alto';

  @override
  String get cycleOvulationNegative => 'Negativo';

  @override
  String get cycleOvulationInconclusive => 'Inconcluso';

  @override
  String get cycleMucusDry => 'Seco';

  @override
  String get cycleMucusSticky => 'Pegajoso';

  @override
  String get cycleMucusCreamy => 'Cremoso';

  @override
  String get cycleMucusWatery => 'Acuoso';

  @override
  String get cycleMucusEggWhite => 'Clara de huevo';

  @override
  String get cycleMucusUnusual => 'Inusual';

  @override
  String get cycleMucusLight => 'leve';

  @override
  String get cycleMucusMedium => 'media';

  @override
  String get cycleMucusHeavy => 'alta';

  @override
  String cycleMucusValue(String arg0, String arg1) {
    return '$arg0, $arg1';
  }

  @override
  String get measurementLocationArmpit => 'Axila';

  @override
  String get measurementLocationFinger => 'Dedo';

  @override
  String get measurementLocationForehead => 'Frente';

  @override
  String get measurementLocationMouth => 'Boca';

  @override
  String get measurementLocationRectum => 'Recto';

  @override
  String get measurementLocationTemporalArtery => 'Arteria temporal';

  @override
  String get measurementLocationToe => 'Dedo del pie';

  @override
  String get measurementLocationEar => 'Oído';

  @override
  String get measurementLocationWrist => 'Muñeca';

  @override
  String get measurementLocationVagina => 'Vagina';

  @override
  String get measurementLocationUnknown => 'Ubicación de medición desconocida';

  @override
  String get weekdayMondayShort => 'L';

  @override
  String get weekdayTuesdayShort => 'M';

  @override
  String get weekdayWednesdayShort => 'X';

  @override
  String get weekdayThursdayShort => 'J';

  @override
  String get weekdayFridayShort => 'V';

  @override
  String get weekdaySaturdayShort => 'S';

  @override
  String get weekdaySundayShort => 'D';

  @override
  String get vitalsPermissionsNeededTitle =>
      'Permisos de constantes necesarios';

  @override
  String get vitalsPermissionsNeededBody =>
      'Concede permisos de presión arterial, saturación de oxígeno, frecuencia respiratoria, temperatura, VO2 máx. y glucosa para completar esta pantalla.';

  @override
  String get vitalsRespiratoryRateReadings =>
      'Lecturas de frecuencia respiratoria';

  @override
  String get vitalsBodyTemperatureReadings =>
      'Lecturas de temperatura corporal';

  @override
  String get heartRateHealthChecksTitle =>
      'Comprobaciones de frecuencia cardiaca';

  @override
  String get heartRateHighTitle => 'Frecuencia cardiaca alta';

  @override
  String get heartRateLowTitle => 'Frecuencia cardiaca baja';

  @override
  String heartRateSamplesAtOrAbove(int arg0) {
    return 'Muestras de $arg0 lpm o más';
  }

  @override
  String heartRateSamplesAtOrBelow(int arg0) {
    return 'Muestras de $arg0 lpm o menos';
  }

  @override
  String heartRateDaysAtOrAbove(int arg0) {
    return 'Días con $arg0 lpm o más';
  }

  @override
  String heartRateDaysAtOrBelow(int arg0) {
    return 'Días con $arg0 lpm o menos';
  }

  @override
  String get cdDecreaseHrThreshold => 'Reducir umbral de frecuencia cardiaca';

  @override
  String get cdIncreaseHrThreshold => 'Aumentar umbral de frecuencia cardiaca';

  @override
  String get mealBreakfast => 'Desayuno';

  @override
  String get mealLunch => 'Almuerzo';

  @override
  String get mealDinner => 'Cena';

  @override
  String get mealSnack => 'Snack';

  @override
  String get mealGeneric => 'Comida';

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
    return 'azúcar ${arg0}g';
  }

  @override
  String get caffeineSectionOverview => 'Resumen';

  @override
  String get caffeineSectionDashboard => 'Panel';

  @override
  String get caffeineSectionAnalytics => 'Analíticas';

  @override
  String get caffeineSectionSleep => 'Impacto en el sueño';

  @override
  String get caffeineSectionSources => 'Fuentes';

  @override
  String get caffeineSectionEntries => 'Entradas';

  @override
  String get caffeineSectionScience => 'Ciencia';

  @override
  String get caffeineSetupTitle => 'Personalizar información de cafeína';

  @override
  String get caffeineSetupBody =>
      'OpenVitals encontró datos de cafeína. La personalización mejora la curva de cafeína y la previsión de hora de dormir.';

  @override
  String get caffeineCurrentTitle => 'Cafeína activa';

  @override
  String get caffeineTodayTotal => 'Total de hoy';

  @override
  String get caffeineTimeToSafe => 'Tiempo hasta seguro';

  @override
  String get caffeineSleepStatusUnlikely => 'Impacto en el sueño improbable';

  @override
  String caffeineSleepStatusUnlikelyBody(String arg0, String arg1) {
    return '$arg0 activa ahora, por debajo de tu umbral de sueño de $arg1.';
  }

  @override
  String get caffeineSleepStatusElevatedNow => 'Elevada ahora';

  @override
  String caffeineSleepStatusElevatedNowBody(
    String arg0,
    String arg1,
    String arg2,
    String arg3,
  ) {
    return '$arg0 activa ahora. Estimada por debajo del umbral en $arg1; la previsión de hora de dormir es $arg2 a las $arg3.';
  }

  @override
  String get caffeineSleepStatusMayAffect => 'Puede afectar al sueño';

  @override
  String caffeineSleepStatusMayAffectBody(
    String arg0,
    String arg1,
    String arg2,
  ) {
    return 'La previsión de hora de dormir es $arg0 a las $arg1, por encima de tu umbral de $arg2.';
  }

  @override
  String get caffeinePeriodTotal => 'Total del periodo';

  @override
  String get caffeineDailyAverage => 'Media diaria';

  @override
  String get caffeineLoggedDays => 'Días registrados';

  @override
  String get caffeinePeakDay => 'Día pico';

  @override
  String caffeinePeakDayValue(String arg0, String arg1) {
    return '$arg0 - $arg1';
  }

  @override
  String get caffeineCurveTitle => 'Curva de cafeína';

  @override
  String caffeineThresholdLine(String arg0) {
    return 'Umbral de sueño $arg0';
  }

  @override
  String get caffeineBedtimeForecast => 'Previsión de hora de dormir';

  @override
  String caffeineBedtimeSummary(String arg0, String arg1) {
    return 'A las $arg0 con umbral $arg1';
  }

  @override
  String get caffeineSafeNights => 'Noches seguras';

  @override
  String get caffeineSafeStreak => 'Racha segura';

  @override
  String get caffeineTopSource => 'Fuente principal';

  @override
  String get caffeineSleepThreshold => 'Umbral de sueño';

  @override
  String get caffeineDailyImpact => 'Impacto diario y al dormir';

  @override
  String get caffeineSafeCalendar => 'Calendario de noches seguras';

  @override
  String get caffeineSources => 'Apps de origen';

  @override
  String get caffeineItems => 'Elementos';

  @override
  String get caffeineInferredCategories => 'Categorías inferidas';

  @override
  String get caffeineTimeOfDay => 'Hora del día';

  @override
  String get caffeineEntry => 'Entrada de cafeína';

  @override
  String caffeineInferredCategory(String arg0) {
    return 'Categoría: $arg0';
  }

  @override
  String caffeineCatalogMatch(String arg0) {
    return 'Catálogo: $arg0';
  }

  @override
  String get caffeineCategory => 'Categoría';

  @override
  String get caffeineCatalog => 'Catálogo';

  @override
  String caffeineCatalogMatchDetail(String arg0, String arg1, String arg2) {
    return '$arg0, típico $arg1, coincidencia $arg2';
  }

  @override
  String get caffeineHealthConnectSourceLabel => 'Fuente';

  @override
  String get caffeineHealthConnectMealLabel => 'Comida';

  @override
  String get caffeineHealthConnectDurationLabel => 'Duración';

  @override
  String caffeineCurrentContribution(String arg0) {
    return '$arg0 activa';
  }

  @override
  String get caffeineCurrentContributionLabel => 'Actual';

  @override
  String get caffeineDose => 'Dosis';

  @override
  String get caffeinePeak => 'Pico';

  @override
  String get caffeinePeakTime => 'Hora pico';

  @override
  String get caffeineContributionCurve => 'Curva de contribución';

  @override
  String get caffeineEmpty =>
      'No hay entradas de cafeína para este periodo. Las bebidas con cafeína añadidas mediante hidratación o nutrición aparecerán aquí cuando Health Connect incluya cafeína.';

  @override
  String get caffeineScienceTitle => 'Cómo funciona la estimación';

  @override
  String get caffeineScienceBody =>
      'OpenVitals lee cafeína de registros de nutrición de Health Connect en miligramos y después estima la absorción durante tu ventana de absorción configurada y la eliminación exponencial según tu semivida personalizada.';

  @override
  String get caffeineScienceMeasurements => 'Mediciones usadas';

  @override
  String get caffeineScienceMeasurementsBody =>
      'La dosis registrada siempre proviene de Health Connect. La hora de inicio/fin, el nombre de la entrada, el tipo de comida y el paquete de origen de datos se usan para horarios, coincidencias y etiquetas de análisis. Las coincidencias del catálogo solo anotan entradas; nunca sustituyen la dosis registrada.';

  @override
  String get caffeineScienceLimits =>
      'Este es un modelo poblacional práctico, no consejo médico. Embarazo, medicamentos, enfermedad hepática, genética, tabaquismo, alcohol, sensibilidad y habituación pueden cambiar la respuesta a la cafeína.';

  @override
  String get caffeineReferencesTitle => 'Investigación y referencias';

  @override
  String get caffeineReferenceDrake => 'Momento de cafeína y sueño, Drake 2013';

  @override
  String get caffeineReferenceNehlig =>
      'Metabolismo individual de la cafeína, Nehlig 2018';

  @override
  String get caffeineReferenceEfsa =>
      'Notas de EFSA sobre seguridad de cafeína y sueño';

  @override
  String get caffeineReferenceHealthConnect =>
      'Campos del registro de nutrición de Health Connect';

  @override
  String get unknownSource => 'Fuente desconocida';

  @override
  String get achievementsLegacyTitle => 'Insignias de actividad heredadas';

  @override
  String achievementsProgressSummary(int arg0, int arg1) {
    return '$arg0 de $arg1 desbloqueadas';
  }

  @override
  String achievementsDataWindow(String arg0, String arg1, String arg2) {
    return '$arg0 a $arg1 · $arg2 días registrados';
  }

  @override
  String get achievementsTrackedDays => 'Días registrados';

  @override
  String get achievementsBestSteps => 'Mejores pasos';

  @override
  String get achievementsTotalDistance => 'Distancia total';

  @override
  String get achievementsBestFloors => 'Mejores pisos';

  @override
  String get achievementsTotalFloors => 'Pisos totales';

  @override
  String get achievementsFilterAll => 'Todo';

  @override
  String get achievementsCategoryDailySteps => 'Pasos diarios';

  @override
  String get achievementsCategoryLifetimeDistance => 'Distancia acumulada';

  @override
  String get achievementsCategoryDailyFloors => 'Pisos diarios';

  @override
  String get achievementsCategoryLifetimeFloors => 'Pisos totales';

  @override
  String achievementsDailyStepsRequirement(String arg0) {
    return '$arg0 pasos en un día';
  }

  @override
  String achievementsLifetimeDistanceRequirement(String arg0) {
    return '$arg0 de distancia total';
  }

  @override
  String achievementsDailyFloorsRequirement(String arg0) {
    return '$arg0 pisos en un día';
  }

  @override
  String achievementsLifetimeFloorsRequirement(String arg0) {
    return '$arg0 pisos totales';
  }

  @override
  String achievementsProgressValue(String arg0, String arg1) {
    return '$arg0 de $arg1';
  }

  @override
  String achievementsAchievedOn(String arg0) {
    return 'Desbloqueada $arg0';
  }

  @override
  String get achievementsEarnedOnce => 'Conseguida';

  @override
  String achievementsEarnedTimes(int arg0) {
    return '$arg0 veces';
  }

  @override
  String get achievementsLocked => 'Bloqueada';

  @override
  String get achievementsNoDataTitle => 'Sin historial de actividad';

  @override
  String get achievementsNoDataBody =>
      'Health Connect no devolvió registros de pasos o distancia. Comprueba que existan datos de actividad y que el acceso al historial esté concedido para registros antiguos.';

  @override
  String get achievementsNoFloorDataTitle => 'Sin datos de pisos';

  @override
  String get achievementsNoFloorDataBody =>
      'Las insignias de pisos se desbloquean cuando Health Connect tiene datos de pisos subidos.';

  @override
  String get achievementsErrorTitle => 'Logros no disponibles';

  @override
  String get dataConfidenceTitle => 'Confianza de los datos';

  @override
  String get dataConfidenceHigh => 'Confianza alta';

  @override
  String get dataConfidenceMedium => 'Confianza media';

  @override
  String get dataConfidenceLow => 'Confianza baja';

  @override
  String dataConfidenceCoverage(int arg0, int arg1, int arg2) {
    return '$arg0 de $arg1 días registrados ($arg2%)';
  }

  @override
  String dataConfidenceSamples(int arg0) {
    return '$arg0 registros';
  }

  @override
  String get dataConfidenceSourceUnavailable =>
      'Detalles de origen no disponibles para este agregado';

  @override
  String dataConfidenceSourceSingle(String arg0) {
    return 'Origen: $arg0';
  }

  @override
  String dataConfidenceSourceMixed(String arg0) {
    return 'Orígenes mixtos: $arg0';
  }

  @override
  String get dataConfidenceKindMeasured =>
      'Registros medidos de Health Connect';

  @override
  String get dataConfidenceKindAggregated =>
      'Agregado desde registros de Health Connect';

  @override
  String get dataConfidenceKindCalculated => 'Calculado por OpenVitals';

  @override
  String get dataConfidenceKindEstimated => 'Valor estimado o derivado';

  @override
  String get dataConfidenceKindMixed => 'Datos medidos y calculados mezclados';

  @override
  String get dataConfidenceWarningLowCoverage =>
      'Los días sin datos pueden debilitar medias y tendencias.';

  @override
  String get dataConfidenceWarningSparse =>
      'Datos escasos: las tendencias y estadísticas pueden ser inestables.';

  @override
  String get dataConfidenceWarningMixedSources =>
      'Los cambios de origen pueden explicar saltos o datos que parecen duplicados.';

  @override
  String get dataConfidenceWarningManual =>
      'Este periodo incluye entradas manuales.';

  @override
  String get dataConfidenceWarningCalculated =>
      'Este valor es derivado, no medido directamente.';

  @override
  String get dataConfidenceWarningNoSources =>
      'Este agregado no expone detalles por origen.';

  @override
  String get settingsBodyEnergyGroupTitle => 'Energía corporal';

  @override
  String get settingsBodyEnergyGroupBody =>
      'Calibración para la energía estimada durante el día y las zonas de esfuerzo.';

  @override
  String get bodyEnergyCalibrationTitle =>
      'Mejorar las estimaciones de energía corporal';

  @override
  String get bodyEnergyCalibrationBody =>
      'OpenVitals estima el desgaste a partir de la intensidad de la frecuencia cardiaca a lo largo del tiempo. La edad, la frecuencia cardiaca máxima, la frecuencia en reposo y las zonas ayudan a clasificar el esfuerzo con más precisión.';

  @override
  String get bodyEnergyCalibrationOptionalBody =>
      'Esto es opcional. Si lo omites, OpenVitals usa estimaciones automáticas de los datos de Health Connect y muestra menor confianza cuando la calibración es incierta. Estos valores permanecen en los ajustes de OpenVitals.';

  @override
  String get bodyEnergyCalibrationBirthYear => 'Año de nacimiento';

  @override
  String get bodyEnergyCalibrationMaxHr => 'Frecuencia cardiaca máxima';

  @override
  String get bodyEnergyCalibrationRestingHr => 'Frecuencia cardiaca en reposo';

  @override
  String get bodyEnergyCalibrationManualZones => 'Zonas cardiacas manuales';

  @override
  String get bodyEnergyCalibrationManualZonesBody =>
      'Zonas 1-5 o límite de frecuencia cardíaca (latidos por minuto).';

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
  String get bodyEnergyCalibrationUseAuto => 'Usar estimaciones automáticas';

  @override
  String get bodyEnergyCalibrationSkip => 'Omitir por ahora';

  @override
  String get bodyEnergyCalibrationSaved =>
      'Calibración de energía corporal guardada';

  @override
  String get bodyEnergyCalibrationReset =>
      'Calibración de energía corporal restablecida a automático';

  @override
  String get bodyEnergyNotSetUp => 'No configurado';

  @override
  String get bodyEnergyTimelineEstimated => 'Valorada por OpenVitals';

  @override
  String get bodyEnergyTimelineCurrent => 'Actual';

  @override
  String get bodyEnergyTimelineStart => 'Inicio';

  @override
  String get bodyEnergyTimelineCharged => 'Cargada';

  @override
  String get bodyEnergyTimelineDrained => 'Consumida';

  @override
  String get bodyEnergyTimelineConfidence => 'Confianza';

  @override
  String get bodyEnergyTimelineNoData =>
      'No se dispone de un historial de la energía corporal utilizable para este período.';

  @override
  String get bodyEnergyTimelineDayTitle => 'Cronología diaria';

  @override
  String get bodyEnergyTimelineLowConfidence =>
      'Algunas categorías son estimaciones, ya que los datos de calibración o de Health Connect están incompletos.';

  @override
  String get bodyEnergyWhyTitle => '¿Qué influyó en esto?';

  @override
  String get bodyEnergyWhyEmpty =>
      'Hasta el momento, ningún factor claro de carga o consumo ha predominado en este día.';

  @override
  String get bodyEnergyInfluenceSleepRecovery => 'Recuperación del sueño';

  @override
  String get bodyEnergyInfluenceQuietRest => 'Descanso tranquilo';

  @override
  String get bodyEnergyInfluenceExertion => 'Esfuerzo';

  @override
  String get bodyEnergyInfluenceElevatedHr => 'Frecuencia cardiaca elevada';

  @override
  String get bodyEnergyInfluenceRecoveryDebt => 'Cansancio acumulado';

  @override
  String get bodyEnergyInfluenceNoData => 'Sin datos';

  @override
  String get bodyEnergyInfluenceSteady => 'Estable';

  @override
  String get bodyEnergyReasonSleepRecoveryDetail =>
      'Los periodos de sueño han incrementado la estimación con respecto al valor anterior.';

  @override
  String get bodyEnergyReasonQuietRestDetail =>
      'Una frecuencia cardíaca baja durante la vigilia proporcionó un pequeño impulso a la recuperación.';

  @override
  String get bodyEnergyReasonExertionDetail =>
      'La intensidad de la frecuencia cardíaca o los entrenamientos registrados redujeron la estimación.';

  @override
  String get bodyEnergyReasonElevatedHrDetail =>
      'La frecuencia cardíaca en estado de vigilia, por encima del nivel en reposo, supuso un desgaste adicional debido al estrés.';

  @override
  String get bodyEnergyReasonRecoveryDebtDetail =>
      'El esfuerzo más intenso de hace poco ha provocado que siga habiendo una pequeña pérdida de líquido después.';

  @override
  String get bodyEnergyReasonNoDataDetail =>
      'Health Connect no proporcionó señal suficiente para este rango.';

  @override
  String get bodyEnergyReasonSteadyDetail =>
      'La estimación se mantuvo prácticamente estable.';

  @override
  String get bodyEnergyInputsTitle => 'Entradas usadas';

  @override
  String bodyEnergyInputsSummary(int arg0, int arg1) {
    return 'Algoritmo v$arg0, tramos de $arg1 minutos';
  }

  @override
  String get bodyEnergyInputHeartRate => 'Muestras de frecuencia cardiaca';

  @override
  String get bodyEnergyInputSleep => 'Sesiones de sueño';

  @override
  String get bodyEnergyInputWorkouts => 'Entrenamientos';

  @override
  String get bodyEnergyInputRestingHr => 'Frecuencia cardiaca en reposo';

  @override
  String get bodyEnergyInputHrBaseline =>
      'Frecuencia cardíaca basal (ritmo cardíaco en reposo)';

  @override
  String get bodyEnergyInputHrv => 'Variabilidad de la frecuencia cardíaca';

  @override
  String get bodyEnergyInputRespiratory => 'Modificador respiratorio';

  @override
  String get bodyEnergyInputPreviousScore => 'Puntuación anterior';

  @override
  String get bodyEnergyInputCalibration => 'Calibración';

  @override
  String get bodyEnergyInputAvailable => 'Disponible';

  @override
  String get bodyEnergyInputMissing => 'Ausente';

  @override
  String get bodyEnergyInputOptional => 'No presente';

  @override
  String bodyEnergyInputRecords(int arg0) {
    return '$arg0 registros';
  }

  @override
  String bodyEnergyInputSessions(int arg0) {
    return '$arg0 sesiones';
  }

  @override
  String bodyEnergyInputWorkoutsValue(int arg0) {
    return '$arg0 entrenamientos';
  }

  @override
  String bodyEnergyInputPreviousScoreValue(String arg0) {
    return '$arg0 inicio';
  }

  @override
  String get bodyEnergyCalibrationModeAuto => 'Estimaciones automáticas';

  @override
  String get bodyEnergyCalibrationModeManualValues => 'Valores manuales';

  @override
  String get bodyEnergyCalibrationModeManualZones => 'Zonas manuales';

  @override
  String get bodyEnergyCalculationTitle =>
      '¿Cómo se calcula la energía corporal?';

  @override
  String get bodyEnergyCalculationBody =>
      'OpenVitals divide el día seleccionado en pequeños intervalos, parte de la puntuación disponible anterior cuando es posible, y a continuación suma la energía obtenida durante el sueño o el descanso tranquilo y resta la energía consumida por el esfuerzo físico, la frecuencia cardíaca elevada en estado de vigilia y la deuda de recuperación tras un esfuerzo más intenso.';

  @override
  String get bodyEnergyCalculationInputsBody =>
      'La frecuencia cardíaca, la frecuencia cardíaca en reposo, las zonas personales, el sueño, los entrenamientos, la variabilidad de la frecuencia cardíaca (VFC) y la frecuencia respiratoria pueden mejorar la estimación. Si faltan datos, la estimación será más conservadora y el nivel de confianza será menor.';

  @override
  String get bodyEnergyCalculationLimitsBody =>
      'Se trata de una estimación del bienestar realizada por el propio dispositivo, no de una medición directa ni de un consejo médico. Los datos introducidos y las razones que se muestran tienen como objetivo permitir revisar y mejorar el método.';

  @override
  String get metricBodyEnergy => 'Energía corporal';

  @override
  String get privacyPolicyTitle => 'Política de privacidad';

  @override
  String get privacyPolicyBody1 =>
      'OpenVitals lee datos de Health Connect para mostrar pasos, entrenamientos, sueño, frecuencia cardiaca, peso, calorías, hidratación, nutrición, mindfulness y constantes en tu dispositivo. Las entradas que registres explícitamente, incluidas las rutas GPX/KML/KMZ importadas y los archivos FIT importados, se escriben en Health Connect.';

  @override
  String get privacyPolicyBody2 =>
      'Esta app no sube tus datos de salud a un servicio en la nube, no incluye anuncios y no comparte datos con terceros.';

  @override
  String get privacyPolicyBody3 =>
      'OpenVitals no es un dispositivo médico y no diagnostica, trata, cura ni previene ninguna enfermedad o afección médica. No sustituye el asesoramiento, el diagnóstico ni el tratamiento médicos proporcionados por un profesional sanitario cualificado.';

  @override
  String get linkCouldNotOpen => 'No se pudo abrir el enlace.';
}
