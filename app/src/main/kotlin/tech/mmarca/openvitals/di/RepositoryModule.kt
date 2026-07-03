package tech.mmarca.openvitals.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.ActivityRepositoryImpl
import tech.mmarca.openvitals.data.repository.BodyEnergyRepositoryImpl
import tech.mmarca.openvitals.data.repository.BodyRepositoryImpl
import tech.mmarca.openvitals.data.repository.CaffeineRepositoryImpl
import tech.mmarca.openvitals.data.repository.CycleRepositoryImpl
import tech.mmarca.openvitals.data.repository.HealthRepositoryImpl
import tech.mmarca.openvitals.data.repository.HeartRepositoryImpl
import tech.mmarca.openvitals.data.repository.HydrationRepositoryImpl
import tech.mmarca.openvitals.data.repository.MindfulnessRepositoryImpl
import tech.mmarca.openvitals.data.repository.NutritionRepositoryImpl
import tech.mmarca.openvitals.data.repository.SleepRepositoryImpl
import tech.mmarca.openvitals.data.repository.VitalsRepositoryImpl
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.CaffeineRepository
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSleepRepository(impl: SleepRepositoryImpl): SleepRepository

    @Binds
    @Singleton
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository

    @Binds
    @Singleton
    abstract fun bindHealthRepository(impl: HealthRepositoryImpl): HealthRepository

    @Binds
    @Singleton
    abstract fun bindHeartRepository(impl: HeartRepositoryImpl): HeartRepository

    @Binds
    @Singleton
    abstract fun bindHydrationRepository(impl: HydrationRepositoryImpl): HydrationRepository

    @Binds
    @Singleton
    abstract fun bindBodyRepository(impl: BodyRepositoryImpl): BodyRepository

    @Binds
    @Singleton
    abstract fun bindBodyEnergyRepository(impl: BodyEnergyRepositoryImpl): BodyEnergyRepository

    @Binds
    @Singleton
    abstract fun bindNutritionRepository(impl: NutritionRepositoryImpl): NutritionRepository

    @Binds
    @Singleton
    abstract fun bindCaffeineRepository(impl: CaffeineRepositoryImpl): CaffeineRepository

    @Binds
    @Singleton
    abstract fun bindMindfulnessRepository(impl: MindfulnessRepositoryImpl): MindfulnessRepository

    @Binds
    @Singleton
    abstract fun bindCycleRepository(impl: CycleRepositoryImpl): CycleRepository

    @Binds
    @Singleton
    abstract fun bindVitalsRepository(impl: VitalsRepositoryImpl): VitalsRepository
}
