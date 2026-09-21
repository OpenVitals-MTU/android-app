package tech.mmarca.openvitals.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.CalorieDisplayPreferences
import tech.mmarca.openvitals.data.repository.contract.DailyGoalPreferences
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
}
