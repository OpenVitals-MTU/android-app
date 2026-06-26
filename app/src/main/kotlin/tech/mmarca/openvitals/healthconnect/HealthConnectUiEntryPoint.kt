package tech.mmarca.openvitals.healthconnect

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import tech.mmarca.openvitals.data.repository.PreferencesRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HealthConnectUiEntryPoint {
    fun healthConnectScreenUxCoordinator(): HealthConnectScreenUxCoordinator
    fun preferencesRepository(): PreferencesRepository
}
