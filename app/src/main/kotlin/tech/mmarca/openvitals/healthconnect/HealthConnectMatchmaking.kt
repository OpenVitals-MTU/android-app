package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.ExperimentalMatchmakingApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.matchmaking.MatchmakingRequest
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import androidx.health.connect.client.records.Record

@OptIn(ExperimentalMatchmakingApi::class)
@Singleton
class HealthConnectMatchmaking @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    private val defaultRecordTypes: Set<KClass<out Record>> = setOf(
        StepsRecord::class,
        SleepSessionRecord::class,
        HeartRateRecord::class,
    )

    suspend fun isFeatureAvailable(): Boolean =
        runCatching {
            client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_MATCHMAKING) ==
                HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        }.getOrDefault(false)

    suspend fun isMatchmakingPossible(): Boolean =
        runCatching {
            client.checkIfMatchmakingIsPossible(matchmakingRequest()).isMatchmakingPossible
        }.getOrDefault(false)

    fun createMatchmakingIntent(): Intent =
        client.createMatchmakingIntent(matchmakingRequest())

    private fun matchmakingRequest(): MatchmakingRequest =
        MatchmakingRequest(recordTypes = defaultRecordTypes)
}
