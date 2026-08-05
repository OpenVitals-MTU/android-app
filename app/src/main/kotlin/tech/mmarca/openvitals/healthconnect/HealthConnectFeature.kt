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
        MANUAL_ENTRY -> manager.requestableWritePermissions
        DATA_IMPORT -> manager.dataImportWritePermissions
        // The CSV importer's gate requires NO permissions: which write
        // permissions are needed is not known until the user has mapped the
        // file's columns, so the confirm step asks for exactly those instead.
        CSV_IMPORT -> emptySet()
        // Same shape as CSV_IMPORT: the report builder asks for exactly the
        // selected metrics' read permissions, and builds fine on a partial
        // grant — ungranted metrics land in the PDF's notice instead.
        HEALTH_REPORT -> emptySet()
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
