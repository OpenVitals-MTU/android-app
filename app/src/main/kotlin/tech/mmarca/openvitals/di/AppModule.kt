package tech.mmarca.openvitals.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import tech.mmarca.openvitals.core.performance.AppCoroutineScope
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.cache.MetricSummaryCacheDao
import tech.mmarca.openvitals.data.cache.MetricSummaryCacheStore
import tech.mmarca.openvitals.data.cache.OpenVitalsDatabase
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectQueryCache

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider

    @Provides
    @Singleton
    @AppCoroutineScope
    fun provideAppCoroutineScope(dispatcherProvider: DispatcherProvider): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcherProvider.default)

    @Provides
    @Singleton
    fun provideHealthConnectQueryCache(): HealthConnectQueryCache = HealthConnectQueryCache()

    @Provides
    @Singleton
    fun provideOpenVitalsDatabase(
        @ApplicationContext context: Context,
    ): OpenVitalsDatabase =
        Room.databaseBuilder(
            context,
            OpenVitalsDatabase::class.java,
            "openvitals.db",
        ).build()

    @Provides
    @Singleton
    fun provideMetricSummaryCacheDao(database: OpenVitalsDatabase): MetricSummaryCacheDao =
        database.metricSummaryCacheDao()

    @Provides
    @Singleton
    fun provideMetricSummaryCacheStore(
        dao: MetricSummaryCacheDao,
        dispatcherProvider: DispatcherProvider,
    ): MetricSummaryCacheStore =
        MetricSummaryCacheStore(
            dao = dao,
            dispatchers = dispatcherProvider,
        )

    @Provides
    @Singleton
    fun provideUnitFormatter(preferencesRepository: PreferencesRepository): UnitFormatter =
        UnitFormatter(unitSystemProvider = { preferencesRepository.unitSystem })

    @Provides
    @Singleton
    fun provideDateTimeFormatterProvider(): DateTimeFormatterProvider =
        DateTimeFormatterProvider()
}
