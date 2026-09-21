package tech.mmarca.openvitals.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import tech.mmarca.openvitals.devices.core.pairing.BleWatchPairing
import tech.mmarca.openvitals.devices.core.pairing.WatchPairingPort
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPort
import tech.mmarca.openvitals.devices.garmin.GarminCounterWatermarkStore
import tech.mmarca.openvitals.devices.garmin.GarminDeviceStateStore
import tech.mmarca.openvitals.devices.garmin.GarminFileStore
import tech.mmarca.openvitals.devices.garmin.GarminGattProbe
import tech.mmarca.openvitals.devices.garmin.GarminGattRadio
import tech.mmarca.openvitals.devices.garmin.GarminRadio
import tech.mmarca.openvitals.devices.garmin.GarminTransportProbe
import tech.mmarca.openvitals.devices.garmin.GarminWatchSyncService
import tech.mmarca.openvitals.devices.garmin.garminFileStore
import tech.mmarca.openvitals.devices.media.AndroidPhoneMediaSource
import tech.mmarca.openvitals.devices.media.PhoneMediaSource

/** Wiring for the `devices/` layer, kept apart so [AppModule] carries no watch knowledge. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DevicesModule {

    @Binds
    @Singleton
    abstract fun bindWatchPairingPort(impl: BleWatchPairing): WatchPairingPort

    @Binds
    @Singleton
    abstract fun bindDeviceSyncPort(impl: GarminWatchSyncService): DeviceSyncPort

    @Binds
    @Singleton
    abstract fun bindPhoneMediaSource(impl: AndroidPhoneMediaSource): PhoneMediaSource

    @Binds
    @Singleton
    abstract fun bindGarminRadio(impl: GarminGattRadio): GarminRadio

    companion object {

        @Provides
        @Singleton
        fun provideGarminTransportProbe(
            @ApplicationContext context: Context,
        ): GarminTransportProbe = GarminGattProbe(context)

        @Provides
        @Singleton
        fun provideGarminDeviceStateStore(
            @ApplicationContext context: Context,
        ): GarminDeviceStateStore = GarminDeviceStateStore(context)

        // One instance: the store finds a file's pending note by the object that saved it.
        @Provides
        @Singleton
        fun provideGarminFileStore(
            @ApplicationContext context: Context,
        ): GarminFileStore = garminFileStore(context)

        @Provides
        @Singleton
        fun provideGarminCounterWatermarkStore(
            @ApplicationContext context: Context,
        ): GarminCounterWatermarkStore = GarminCounterWatermarkStore(context)
    }
}
