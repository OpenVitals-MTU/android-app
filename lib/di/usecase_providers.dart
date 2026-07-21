
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/repository/dashboard/dashboard_data_loader.dart';
import '../domain/usecase/load_dashboard_day_use_case.dart';
import '../domain/usecase/check_body_write_permission_use_case.dart';
import '../domain/usecase/check_hydration_write_access_use_case.dart';
import '../domain/usecase/check_mindfulness_write_access_use_case.dart';
import '../domain/usecase/check_minimum_health_permissions_use_case.dart';
import '../domain/usecase/check_nutrition_write_permission_use_case.dart';
import '../domain/usecase/check_onboarding_state_use_case.dart';
import '../domain/usecase/check_vitals_write_permission_use_case.dart';
import '../domain/usecase/delete_activity_entry_use_case.dart';
import '../domain/usecase/delete_body_measurement_entry_use_case.dart';
import '../domain/usecase/delete_hydration_entry_use_case.dart';
import '../domain/usecase/delete_mindfulness_session_use_case.dart';
import '../domain/usecase/delete_nutrition_entry_use_case.dart';
import '../domain/usecase/delete_vitals_measurement_entry_use_case.dart';
import '../domain/usecase/discover_ble_device_capabilities_use_case.dart';
import '../domain/usecase/edit_ble_device_registry_use_case.dart';
import '../domain/usecase/edit_custom_hydration_drinks_use_case.dart';
import '../domain/usecase/grant_onboarding_permissions_use_case.dart';
import '../domain/usecase/open_health_connect_settings_use_case.dart';
import '../domain/usecase/read_activity_write_permissions_use_case.dart';
import '../domain/usecase/read_hydration_daily_goal_use_case.dart';
import '../domain/usecase/read_hydration_entry_settings_use_case.dart';
import '../domain/usecase/read_onboarding_permission_catalog_use_case.dart';
import '../domain/usecase/onboard_garmin_watch_use_case.dart';
import '../domain/usecase/read_paired_ble_devices_use_case.dart';
import '../domain/usecase/refresh_ble_device_registry_use_case.dart';
import '../domain/usecase/request_health_permissions_use_case.dart';
import '../domain/usecase/resolve_ble_capability_conflicts_use_case.dart';
import '../domain/usecase/save_body_measurement_use_case.dart';
import '../domain/usecase/save_carbs_entry_use_case.dart';
import '../domain/usecase/save_hydration_container_size_use_case.dart';
import '../domain/usecase/save_hydration_entry_use_case.dart';
import '../domain/usecase/save_last_custom_hydration_amount_use_case.dart';
import '../domain/usecase/save_mindfulness_session_use_case.dart';
import '../domain/usecase/save_vitals_measurement_use_case.dart';
import '../domain/usecase/write_imported_activity_use_case.dart';
import '../domain/usecase/load_achievement_history_use_case.dart';
import '../domain/usecase/load_activities_use_case.dart';
import '../domain/usecase/load_activity_detail_use_case.dart';
import '../domain/usecase/load_heart_rate_recovery_period_use_case.dart';
import '../domain/usecase/load_activity_metric_period_use_case.dart';
import '../domain/usecase/load_body_energy_timeline_use_case.dart';
import '../domain/usecase/load_body_measurement_for_edit_use_case.dart';
import '../domain/usecase/load_body_period_use_case.dart';
import '../domain/usecase/load_caffeine_use_case.dart';
import '../domain/usecase/load_calories_use_case.dart';
import '../domain/usecase/load_cardio_load_detail_use_case.dart';
import '../domain/usecase/load_custom_hydration_drinks_use_case.dart';
import '../domain/usecase/load_cycle_period_use_case.dart';
import '../domain/usecase/load_frequent_hydration_drinks_use_case.dart';
import '../domain/usecase/load_granted_health_permissions_use_case.dart';
import '../domain/usecase/load_heart_period_use_case.dart';
import '../domain/usecase/load_hydration_entry_for_edit_use_case.dart';
import '../domain/usecase/load_hydration_period_use_case.dart';
import '../domain/usecase/load_mindfulness_period_use_case.dart';
import '../domain/usecase/load_mindfulness_session_for_edit_use_case.dart';
import '../domain/usecase/load_nutrition_period_use_case.dart';
import '../domain/usecase/load_recovery_days_use_case.dart';
import '../domain/usecase/load_sleep_detail_use_case.dart';
import '../domain/usecase/load_sleep_period_use_case.dart';
import '../domain/usecase/load_today_hydration_use_case.dart';
import '../domain/usecase/load_vitals_measurement_for_edit_use_case.dart';
import 'data_providers.dart';
/// The domain layer's object graph: the read orchestrator and every use
/// case, each assembled from the repositories it composes.
///
/// Imported through the `providers.dart` barrel; nothing imports this file
/// directly.

// ── Read orchestrator + use cases ─────────────────────────────────────────

final dashboardDataLoaderProvider = Provider<DashboardDataLoader>(
  (ref) => DashboardDataLoader(
    ref.watch(healthDataSourceProvider),
    preferencesRepository: ref.watch(preferencesRepositoryProvider),
    bodyEnergyRepository: ref.watch(bodyEnergyRepositoryProvider),
  ),
);

final loadDashboardDayUseCaseProvider = Provider<LoadDashboardDayUseCase>(
  (ref) => LoadDashboardDayUseCase(ref.watch(dashboardDataLoaderProvider)),
);

final loadHeartPeriodUseCaseProvider = Provider<LoadHeartPeriodUseCase>(
  (ref) => LoadHeartPeriodUseCase(
    ref.watch(heartRepositoryProvider),
    ref.watch(vitalsRepositoryProvider),
  ),
);

final loadActivityDetailUseCaseProvider = Provider<LoadActivityDetailUseCase>(
  (ref) => LoadActivityDetailUseCase(
    ref.watch(activityRepositoryProvider),
    ref.watch(heartRepositoryProvider),
  ),
);

final loadHeartRateRecoveryPeriodUseCaseProvider =
    Provider<LoadHeartRateRecoveryPeriodUseCase>(
  (ref) => LoadHeartRateRecoveryPeriodUseCase(
    ref.watch(activityRepositoryProvider),
    ref.watch(heartRepositoryProvider),
  ),
);

final loadActivitiesUseCaseProvider = Provider<LoadActivitiesUseCase>(
  (ref) => LoadActivitiesUseCase(
    ref.watch(activityRepositoryProvider),
    ref.watch(heartRepositoryProvider),
  ),
);

final deleteActivityEntryUseCaseProvider = Provider<DeleteActivityEntryUseCase>(
  (ref) => DeleteActivityEntryUseCase(ref.watch(activityRepositoryProvider)),
);

final loadActivityMetricPeriodUseCaseProvider =
    Provider<LoadActivityMetricPeriodUseCase>(
  (ref) => LoadActivityMetricPeriodUseCase(ref.watch(activityRepositoryProvider)),
);

final loadCaloriesUseCaseProvider = Provider<LoadCaloriesUseCase>(
  (ref) => LoadCaloriesUseCase(
    ref.watch(activityRepositoryProvider),
    ref.watch(bodyRepositoryProvider),
  ),
);

final loadCardioLoadDetailUseCaseProvider =
    Provider<LoadCardioLoadDetailUseCase>(
  (ref) => LoadCardioLoadDetailUseCase(
    ref.watch(activityRepositoryProvider),
    ref.watch(heartRepositoryProvider),
  ),
);

final loadSleepPeriodUseCaseProvider = Provider<LoadSleepPeriodUseCase>(
  (ref) => LoadSleepPeriodUseCase(
    ref.watch(sleepRepositoryProvider),
    ref.watch(heartRepositoryProvider),
  ),
);

final loadAchievementHistoryUseCaseProvider =
    Provider<LoadAchievementHistoryUseCase>(
  (ref) => LoadAchievementHistoryUseCase(ref.watch(activityRepositoryProvider)),
);

final loadBodyPeriodUseCaseProvider = Provider<LoadBodyPeriodUseCase>(
  (ref) => LoadBodyPeriodUseCase(ref.watch(bodyRepositoryProvider)),
);

final deleteBodyMeasurementEntryUseCaseProvider =
    Provider<DeleteBodyMeasurementEntryUseCase>(
  (ref) => DeleteBodyMeasurementEntryUseCase(ref.watch(bodyRepositoryProvider)),
);

final loadBodyEnergyTimelineUseCaseProvider =
    Provider<LoadBodyEnergyTimelineUseCase>(
  (ref) =>
      LoadBodyEnergyTimelineUseCase(ref.watch(bodyEnergyRepositoryProvider)),
);

final loadCaffeineUseCaseProvider = Provider<LoadCaffeineUseCase>(
  (ref) => LoadCaffeineUseCase(ref.watch(caffeineRepositoryProvider)),
);

final deleteHydrationEntryUseCaseProvider =
    Provider<DeleteHydrationEntryUseCase>(
  (ref) => DeleteHydrationEntryUseCase(ref.watch(hydrationRepositoryProvider)),
);

final deleteNutritionEntryUseCaseProvider =
    Provider<DeleteNutritionEntryUseCase>(
  (ref) => DeleteNutritionEntryUseCase(ref.watch(nutritionRepositoryProvider)),
);

final loadCyclePeriodUseCaseProvider = Provider<LoadCyclePeriodUseCase>(
  (ref) => LoadCyclePeriodUseCase(ref.watch(cycleRepositoryProvider)),
);

final loadHydrationPeriodUseCaseProvider = Provider<LoadHydrationPeriodUseCase>(
  (ref) => LoadHydrationPeriodUseCase(
    ref.watch(hydrationRepositoryProvider),
    ref.watch(nutritionRepositoryProvider),
  ),
);

final loadFrequentHydrationDrinksUseCaseProvider =
    Provider<LoadFrequentHydrationDrinksUseCase>(
  (ref) => LoadFrequentHydrationDrinksUseCase(
    ref.watch(hydrationRepositoryProvider),
    ref.watch(nutritionRepositoryProvider),
  ),
);

final loadMindfulnessPeriodUseCaseProvider =
    Provider<LoadMindfulnessPeriodUseCase>(
  (ref) => LoadMindfulnessPeriodUseCase(ref.watch(mindfulnessRepositoryProvider)),
);

final loadNutritionPeriodUseCaseProvider = Provider<LoadNutritionPeriodUseCase>(
  (ref) => LoadNutritionPeriodUseCase(ref.watch(nutritionRepositoryProvider)),
);

final loadRecoveryDaysUseCaseProvider = Provider<LoadRecoveryDaysUseCase>(
  (ref) => LoadRecoveryDaysUseCase(ref.watch(sleepRepositoryProvider)),
);

final loadSleepDetailUseCaseProvider = Provider<LoadSleepDetailUseCase>(
  (ref) => LoadSleepDetailUseCase(ref.watch(sleepRepositoryProvider)),
);

final loadTodayHydrationUseCaseProvider = Provider<LoadTodayHydrationUseCase>(
  (ref) => LoadTodayHydrationUseCase(ref.watch(hydrationRepositoryProvider)),
);

final loadCustomHydrationDrinksUseCaseProvider =
    Provider<LoadCustomHydrationDrinksUseCase>(
  (ref) =>
      LoadCustomHydrationDrinksUseCase(ref.watch(hydrationRepositoryProvider)),
);

final readHydrationDailyGoalUseCaseProvider =
    Provider<ReadHydrationDailyGoalUseCase>(
  (ref) => ReadHydrationDailyGoalUseCase(ref.watch(hydrationRepositoryProvider)),
);

final readHydrationEntrySettingsUseCaseProvider =
    Provider<ReadHydrationEntrySettingsUseCase>(
  (ref) =>
      ReadHydrationEntrySettingsUseCase(ref.watch(hydrationRepositoryProvider)),
);

final discoverBleDeviceCapabilitiesUseCaseProvider =
    Provider<DiscoverBleDeviceCapabilitiesUseCase>(
  (ref) => DiscoverBleDeviceCapabilitiesUseCase(
    ref.watch(bleSensorRepositoryProvider),
    ref.watch(bleDeviceRepositoryProvider),
  ),
);

final onboardGarminWatchUseCaseProvider =
    Provider<OnboardGarminWatchUseCase>(
  (ref) => OnboardGarminWatchUseCase(
    ref.watch(watchPairingPortProvider),
    ref.watch(bleDeviceRepositoryProvider),
    ref.watch(garminTransportProbeProvider),
  ),
);

final readPairedBleDevicesUseCaseProvider =
    Provider<ReadPairedBleDevicesUseCase>(
  (ref) => ReadPairedBleDevicesUseCase(ref.watch(bleDeviceRepositoryProvider)),
);

final resolveBleCapabilityConflictsUseCaseProvider =
    Provider<ResolveBleCapabilityConflictsUseCase>(
  (ref) => ResolveBleCapabilityConflictsUseCase(
    ref.watch(bleDeviceRepositoryProvider),
  ),
);

final readActivityWritePermissionsUseCaseProvider =
    Provider<ReadActivityWritePermissionsUseCase>(
  (ref) =>
      ReadActivityWritePermissionsUseCase(ref.watch(activityRepositoryProvider)),
);

// ── Permission use cases ──────────────────────────────────────────────────

final checkBodyWritePermissionUseCaseProvider =
    Provider<CheckBodyWritePermissionUseCase>(
  (ref) => CheckBodyWritePermissionUseCase(ref.watch(bodyRepositoryProvider)),
);

final checkVitalsWritePermissionUseCaseProvider =
    Provider<CheckVitalsWritePermissionUseCase>(
  (ref) =>
      CheckVitalsWritePermissionUseCase(ref.watch(vitalsRepositoryProvider)),
);

final checkNutritionWritePermissionUseCaseProvider =
    Provider<CheckNutritionWritePermissionUseCase>(
  (ref) => CheckNutritionWritePermissionUseCase(
    ref.watch(nutritionRepositoryProvider),
  ),
);

final checkMindfulnessWriteAccessUseCaseProvider =
    Provider<CheckMindfulnessWriteAccessUseCase>(
  (ref) => CheckMindfulnessWriteAccessUseCase(
    ref.watch(mindfulnessRepositoryProvider),
  ),
);

final checkHydrationWriteAccessUseCaseProvider =
    Provider<CheckHydrationWriteAccessUseCase>(
  (ref) => CheckHydrationWriteAccessUseCase(
    ref.watch(hydrationRepositoryProvider),
    ref.watch(nutritionRepositoryProvider),
  ),
);

final checkMinimumHealthPermissionsUseCaseProvider =
    Provider<CheckMinimumHealthPermissionsUseCase>(
  (ref) =>
      CheckMinimumHealthPermissionsUseCase(ref.watch(healthRepositoryProvider)),
);

final loadGrantedHealthPermissionsUseCaseProvider =
    Provider<LoadGrantedHealthPermissionsUseCase>(
  (ref) =>
      LoadGrantedHealthPermissionsUseCase(ref.watch(healthRepositoryProvider)),
);

final requestHealthPermissionsUseCaseProvider =
    Provider<RequestHealthPermissionsUseCase>(
  (ref) => RequestHealthPermissionsUseCase(ref.watch(healthRepositoryProvider)),
);

final openHealthConnectSettingsUseCaseProvider =
    Provider<OpenHealthConnectSettingsUseCase>(
  (ref) => OpenHealthConnectSettingsUseCase(ref.watch(healthRepositoryProvider)),
);

final checkOnboardingStateUseCaseProvider =
    Provider<CheckOnboardingStateUseCase>(
  (ref) => CheckOnboardingStateUseCase(ref.watch(healthRepositoryProvider)),
);

final grantOnboardingPermissionsUseCaseProvider =
    Provider<GrantOnboardingPermissionsUseCase>(
  (ref) => GrantOnboardingPermissionsUseCase(ref.watch(healthRepositoryProvider)),
);

final readOnboardingPermissionCatalogUseCaseProvider =
    Provider<ReadOnboardingPermissionCatalogUseCase>(
  (ref) => ReadOnboardingPermissionCatalogUseCase(
    ref.watch(healthRepositoryProvider),
  ),
);

// ── Edit-prefill use cases ────────────────────────────────────────────────

final loadBodyMeasurementForEditUseCaseProvider =
    Provider<LoadBodyMeasurementForEditUseCase>(
  (ref) => LoadBodyMeasurementForEditUseCase(ref.watch(bodyRepositoryProvider)),
);

final loadVitalsMeasurementForEditUseCaseProvider =
    Provider<LoadVitalsMeasurementForEditUseCase>(
  (ref) =>
      LoadVitalsMeasurementForEditUseCase(ref.watch(vitalsRepositoryProvider)),
);

final loadMindfulnessSessionForEditUseCaseProvider =
    Provider<LoadMindfulnessSessionForEditUseCase>(
  (ref) => LoadMindfulnessSessionForEditUseCase(
    ref.watch(mindfulnessRepositoryProvider),
  ),
);

final deleteMindfulnessSessionUseCaseProvider =
    Provider<DeleteMindfulnessSessionUseCase>(
  (ref) =>
      DeleteMindfulnessSessionUseCase(ref.watch(mindfulnessRepositoryProvider)),
);

final loadHydrationEntryForEditUseCaseProvider =
    Provider<LoadHydrationEntryForEditUseCase>(
  (ref) =>
      LoadHydrationEntryForEditUseCase(ref.watch(hydrationRepositoryProvider)),
);

// ── Write use cases ───────────────────────────────────────────────────────

final saveBodyMeasurementUseCaseProvider = Provider<SaveBodyMeasurementUseCase>(
  (ref) => SaveBodyMeasurementUseCase(ref.watch(bodyRepositoryProvider)),
);

final saveVitalsMeasurementUseCaseProvider =
    Provider<SaveVitalsMeasurementUseCase>(
  (ref) => SaveVitalsMeasurementUseCase(ref.watch(vitalsRepositoryProvider)),
);

final saveCarbsEntryUseCaseProvider = Provider<SaveCarbsEntryUseCase>(
  (ref) => SaveCarbsEntryUseCase(ref.watch(nutritionRepositoryProvider)),
);

final saveMindfulnessSessionUseCaseProvider =
    Provider<SaveMindfulnessSessionUseCase>(
  (ref) =>
      SaveMindfulnessSessionUseCase(ref.watch(mindfulnessRepositoryProvider)),
);

final saveHydrationEntryUseCaseProvider = Provider<SaveHydrationEntryUseCase>(
  (ref) => SaveHydrationEntryUseCase(
    ref.watch(hydrationRepositoryProvider),
    ref.watch(nutritionRepositoryProvider),
  ),
);

final editCustomHydrationDrinksUseCaseProvider =
    Provider<EditCustomHydrationDrinksUseCase>(
  (ref) =>
      EditCustomHydrationDrinksUseCase(ref.watch(hydrationRepositoryProvider)),
);

final saveHydrationContainerSizeUseCaseProvider =
    Provider<SaveHydrationContainerSizeUseCase>(
  (ref) =>
      SaveHydrationContainerSizeUseCase(ref.watch(hydrationRepositoryProvider)),
);

final saveLastCustomHydrationAmountUseCaseProvider =
    Provider<SaveLastCustomHydrationAmountUseCase>(
  (ref) => SaveLastCustomHydrationAmountUseCase(
    ref.watch(hydrationRepositoryProvider),
  ),
);

final deleteVitalsMeasurementEntryUseCaseProvider =
    Provider<DeleteVitalsMeasurementEntryUseCase>(
  (ref) =>
      DeleteVitalsMeasurementEntryUseCase(ref.watch(vitalsRepositoryProvider)),
);

final writeImportedActivityUseCaseProvider =
    Provider<WriteImportedActivityUseCase>(
  (ref) => WriteImportedActivityUseCase(ref.watch(activityRepositoryProvider)),
);

final writeImportedActivitiesUseCaseProvider =
    Provider<WriteImportedActivitiesUseCase>(
  (ref) => WriteImportedActivitiesUseCase(ref.watch(activityRepositoryProvider)),
);

final editBleDeviceRegistryUseCaseProvider =
    Provider<EditBleDeviceRegistryUseCase>(
  (ref) => EditBleDeviceRegistryUseCase(ref.watch(bleDeviceRepositoryProvider)),
);

final refreshBleDeviceRegistryUseCaseProvider =
    Provider<RefreshBleDeviceRegistryUseCase>(
  (ref) =>
      RefreshBleDeviceRegistryUseCase(ref.watch(bleDeviceRepositoryProvider)),
);
