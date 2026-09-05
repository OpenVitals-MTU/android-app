package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord

enum class HealthConnectFeature {
    DASHBOARD,
    ACTIVITY,
    ACTIVITIES,
    CALORIES,
    SLEEP,
    HEART,
    HEART_VITALS,
    BODY,
    HYDRATION,
    CAFFEINE,
    NUTRITION,
    MINDFULNESS,
    CYCLE,
    READINESS,
    BODY_ENERGY,
    MANUAL_ENTRY,
    DATA_IMPORT,
    CSV_IMPORT,
    HEALTH_REPORT,
    WORKOUT_PLANS,
    ;

    fun requiredReadPermissions(manager: HealthConnectManager): Set<String> = when (this) {
        DASHBOARD -> manager.minimumOnboardingPermissions
        ACTIVITY -> manager.corePermissions + manager.activityExtrasPermissions
        ACTIVITIES -> manager.corePermissions
        CALORIES -> manager.corePermissions + manager.activityExtrasPermissions
        SLEEP -> setOf(HealthPermission.getReadPermission(SleepSessionRecord::class))
        HEART -> manager.heartPermissions
        HEART_VITALS -> manager.heartPermissions + manager.vitalsPermissions
        BODY -> manager.bodyPermissions
        HYDRATION -> manager.nutritionHydrationPermissions
        CAFFEINE -> setOf(HealthPermission.getReadPermission(NutritionRecord::class))
        NUTRITION -> setOf(HealthPermission.getReadPermission(NutritionRecord::class))
        MINDFULNESS -> manager.mindfulnessPermissions
        CYCLE -> manager.cyclePermissions
        READINESS -> manager.minimumOnboardingPermissions
        BODY_ENERGY -> setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        )
        // The log's grid opens with any write permission; each entry screen asks for its own.
        MANUAL_ENTRY -> emptySet()
        DATA_IMPORT -> manager.dataImportWritePermissions
        // The needed permissions are unknown until the columns are mapped.
        CSV_IMPORT -> emptySet()
        // The report builder asks for the selected metrics' reads and builds on a partial grant.
        HEALTH_REPORT -> emptySet()
        // Empty where Health Connect lacks planned exercise, so the screen shows its own state.
        WORKOUT_PLANS -> manager.plannedExercisePermissions
    }

    fun missingReadPermissions(
        manager: HealthConnectManager,
        grantedPermissions: Set<String>,
    ): Set<String> = requiredReadPermissions(manager) - grantedPermissions

    fun hasMinimumAccess(
        manager: HealthConnectManager,
        grantedPermissions: Set<String>,
    ): Boolean = manager.minimumOnboardingPermissions.all { it in grantedPermissions }
}
