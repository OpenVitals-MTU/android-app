package tech.mmarca.openvitals.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.ActivitySplitPreferences
import tech.mmarca.openvitals.data.repository.contract.HealthConnectPreferences
import tech.mmarca.openvitals.data.repository.contract.OnboardingPreferences
import tech.mmarca.openvitals.data.repository.contract.UnitPreferences
import tech.mmarca.openvitals.data.repository.contract.RecordingPreferences
import tech.mmarca.openvitals.data.repository.contract.WidgetOrderPreferences
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyCalibrationPreferences
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.CaffeineModelPreferences
import tech.mmarca.openvitals.data.repository.contract.CalorieDisplayPreferences
import tech.mmarca.openvitals.data.repository.contract.DailyGoalPreferences
import tech.mmarca.openvitals.data.repository.contract.HeartThresholdPreferences
import tech.mmarca.openvitals.data.repository.contract.HydrationGoalPreferences
import tech.mmarca.openvitals.data.repository.contract.MindfulnessTimerPreferences
import tech.mmarca.openvitals.data.repository.contract.NutritionDisplayPreferences
import tech.mmarca.openvitals.data.repository.contract.PeriodPreferences
import tech.mmarca.openvitals.data.repository.contract.SleepWindowPreferences

/**
 * Narrow views of [PreferencesRepository].
 *
 * A screen asks for the handful of settings it uses. That keeps it off the Context the
 * repository needs, so its tests build one constructor with a plain fake.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    abstract fun bindPeriodPreferences(impl: PreferencesRepository): PeriodPreferences

    @Binds
    abstract fun bindDailyGoalPreferences(impl: PreferencesRepository): DailyGoalPreferences

    @Binds
    abstract fun bindBodyProfilePreferences(impl: PreferencesRepository): BodyProfilePreferences

    @Binds
    abstract fun bindCalorieDisplayPreferences(
        impl: PreferencesRepository,
    ): CalorieDisplayPreferences

    @Binds
    abstract fun bindSleepWindowPreferences(impl: PreferencesRepository): SleepWindowPreferences

    @Binds
    abstract fun bindNutritionDisplayPreferences(impl: PreferencesRepository): NutritionDisplayPreferences

    @Binds
    abstract fun bindHeartThresholdPreferences(impl: PreferencesRepository): HeartThresholdPreferences

    @Binds
    abstract fun bindHydrationGoalPreferences(impl: PreferencesRepository): HydrationGoalPreferences

    @Binds
    abstract fun bindBodyEnergyCalibrationPreferences(impl: PreferencesRepository): BodyEnergyCalibrationPreferences

    @Binds
    abstract fun bindCaffeineModelPreferences(impl: PreferencesRepository): CaffeineModelPreferences

    @Binds
    abstract fun bindMindfulnessTimerPreferences(impl: PreferencesRepository): MindfulnessTimerPreferences

    @Binds
    abstract fun bindActivitySplitPreferences(impl: PreferencesRepository): ActivitySplitPreferences

    @Binds
    abstract fun bindWidgetOrderPreferences(impl: PreferencesRepository): WidgetOrderPreferences

    @Binds
    abstract fun bindRecordingPreferences(impl: PreferencesRepository): RecordingPreferences

    @Binds
    abstract fun bindUnitPreferences(impl: PreferencesRepository): UnitPreferences

    @Binds
    abstract fun bindOnboardingPreferences(impl: PreferencesRepository): OnboardingPreferences

    @Binds
    abstract fun bindHealthConnectPreferences(impl: PreferencesRepository): HealthConnectPreferences
}
