package tech.mmarca.openvitals.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.local.OpenVitalsDatabase
import tech.mmarca.openvitals.data.local.beverage.BeverageDao
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyTimelineDao
import tech.mmarca.openvitals.data.local.garmin.GarminWellnessDao
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
        ).addMigrations(
            OpenVitalsDatabase.MIGRATION_1_3,
            OpenVitalsDatabase.MIGRATION_2_3,
            OpenVitalsDatabase.MIGRATION_3_4,
            OpenVitalsDatabase.MIGRATION_4_5,
            OpenVitalsDatabase.MIGRATION_5_6,
            OpenVitalsDatabase.MIGRATION_6_7,
            OpenVitalsDatabase.MIGRATION_7_8,
            OpenVitalsDatabase.MIGRATION_8_9,
        ).build()

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
    fun provideVitalsDailyCacheDao(database: OpenVitalsDatabase): VitalsDailyCacheDao =
        database.vitalsDailyCacheDao()

    @Provides
    @Singleton
    fun provideGarminWellnessDao(database: OpenVitalsDatabase): GarminWellnessDao =
        database.garminWellnessDao()

    @Provides
    @Singleton
    fun provideSyncedRecordOriginDao(database: OpenVitalsDatabase): SyncedRecordOriginDao =
        database.syncedRecordOriginDao()

    @Provides
    @Singleton
    fun provideSystemUnitSystemProvider(): SystemUnitSystemProvider =
        SystemUnitSystemProvider.Default

    @Provides
    @Singleton
    fun provideUnitFormatter(preferencesRepository: PreferencesRepository): UnitFormatter =
        UnitFormatter(
            unitSystemProvider = { preferencesRepository.unitSystem },
            unitOverrideProvider = { preferencesRepository.unitOverride(it) },
        )

    @Provides
    @Singleton
    fun provideDateTimeFormatterProvider(): DateTimeFormatterProvider =
        DateTimeFormatterProvider()
}
