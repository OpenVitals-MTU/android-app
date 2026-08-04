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
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.repository.PreferencesRepository

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
    fun provideUnitFormatter(preferencesRepository: PreferencesRepository): UnitFormatter =
        UnitFormatter(unitSystemProvider = { preferencesRepository.unitSystem })

    @Provides
    @Singleton
    fun provideDateTimeFormatterProvider(): DateTimeFormatterProvider =
        DateTimeFormatterProvider()
}
