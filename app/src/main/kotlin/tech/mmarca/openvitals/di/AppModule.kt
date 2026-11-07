package tech.mmarca.openvitals.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.presentation.CurrentLocaleStrings
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.local.OpenVitalsDatabase
import tech.mmarca.openvitals.data.local.beverage.BeverageDao
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyTimelineDao
import tech.mmarca.openvitals.data.local.cycle.CycleJournalDao
import tech.mmarca.openvitals.data.local.cycle.PillIntakeDao
import tech.mmarca.openvitals.data.local.food.FoodDao
import tech.mmarca.openvitals.data.local.garmin.GarminSleepMinuteDao
import tech.mmarca.openvitals.data.local.garmin.GarminWellnessDao
import tech.mmarca.openvitals.data.local.heartratecache.HeartRateDayCacheDao
import tech.mmarca.openvitals.data.local.medical.MedicalDocumentDao
import tech.mmarca.openvitals.data.local.scale.ScaleWeighInDao
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginDao
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.preferences.SystemUnitSystemProvider

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider

    @Provides
    @Singleton
    fun provideOpenVitalsDatabase(@ApplicationContext context: Context): OpenVitalsDatabase =
        Room.databaseBuilder(
            context,
            OpenVitalsDatabase::class.java,
            "openvitals.db",
        ).addMigrations(*OpenVitalsDatabase.ALL_MIGRATIONS).build()

    @Provides
    @Singleton
    fun provideBodyEnergyTimelineDao(database: OpenVitalsDatabase): BodyEnergyTimelineDao =
        database.bodyEnergyTimelineDao()

    @Provides
    @Singleton
    fun provideBeverageDao(database: OpenVitalsDatabase): BeverageDao =
        database.beverageDao()

    @Provides
    @Singleton
    fun provideFoodDao(database: OpenVitalsDatabase): FoodDao =
        database.foodDao()

    @Provides
    @Singleton
    fun provideCycleJournalDao(database: OpenVitalsDatabase): CycleJournalDao =
        database.cycleJournalDao()

    @Provides
    @Singleton
    fun providePillIntakeDao(database: OpenVitalsDatabase): PillIntakeDao =
        database.pillIntakeDao()

    @Provides
    @Singleton
    fun provideMedicalDocumentDao(database: OpenVitalsDatabase): MedicalDocumentDao =
        database.medicalDocumentDao()

    @Provides
    @Singleton
    fun provideVitalsDailyCacheDao(database: OpenVitalsDatabase): VitalsDailyCacheDao =
        database.vitalsDailyCacheDao()

    @Provides
    @Singleton
    fun provideGarminWellnessDao(database: OpenVitalsDatabase): GarminWellnessDao =
        database.garminWellnessDao()

    @Provides
    @Singleton
    fun provideScaleWeighInDao(database: OpenVitalsDatabase): ScaleWeighInDao =
        database.scaleWeighInDao()

    @Provides
    @Singleton
    fun provideGarminSleepMinuteDao(database: OpenVitalsDatabase): GarminSleepMinuteDao =
        database.garminSleepMinuteDao()

    @Provides
    @Singleton
    fun provideSyncedRecordOriginDao(database: OpenVitalsDatabase): SyncedRecordOriginDao =
        database.syncedRecordOriginDao()

    @Provides
    @Singleton
    fun provideHeartRateDayCacheDao(database: OpenVitalsDatabase): HeartRateDayCacheDao =
        database.heartRateDayCacheDao()

    @Provides
    @Singleton
    fun provideSystemUnitSystemProvider(): SystemUnitSystemProvider =
        SystemUnitSystemProvider.Default

    @Provides
    @Singleton
    fun provideUnitFormatter(
        @ApplicationContext context: Context,
        preferencesRepository: PreferencesRepository,
    ): UnitFormatter {
        val strings = CurrentLocaleStrings(context)
        return UnitFormatter(
            unitSystemProvider = { preferencesRepository.unitSystem },
            unitOverrideProvider = { preferencesRepository.unitOverride(it) },
            heartRateUnitProvider = { strings.get(R.string.unit_bpm) },
        )
    }

    @Provides
    @Singleton
    fun provideDateTimeFormatterProvider(): DateTimeFormatterProvider =
        DateTimeFormatterProvider()
}
