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
        ).build()

    @Provides
    @Singleton
    fun provideBeverageDao(database: OpenVitalsDatabase): BeverageDao =
        database.beverageDao()

    @Provides
    @Singleton
    fun provideUnitFormatter(preferencesRepository: PreferencesRepository): UnitFormatter =
        UnitFormatter(unitSystemProvider = { preferencesRepository.unitSystem })

    @Provides
    @Singleton
    fun provideDateTimeFormatterProvider(): DateTimeFormatterProvider =
        DateTimeFormatterProvider()
}
